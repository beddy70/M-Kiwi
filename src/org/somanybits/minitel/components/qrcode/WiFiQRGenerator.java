package org.somanybits.minitel.components.qrcode;

/**
 * Générateur de QR Codes pour connexion WiFi automatique
 * Compatible avec Android et iOS
 * 
 * Format: WIFI:T:<type>;S:<SSID>;P:<password>;H:<hidden>;;
 * 
 * @author eddy
 */
public class WiFiQRGenerator {
    
    /**
     * Types de sécurité WiFi supportés
     */
    public enum SecurityType {
        NONE(""),           // Réseau ouvert
        WEP("WEP"),        // WEP (obsolète)
        WPA("WPA"),        // WPA/WPA2
        WPA2("WPA"),       // WPA2 (utilise WPA dans le QR)
        WPA3("WPA");       // WPA3 (utilise WPA dans le QR)
        
        private final String value;
        
        SecurityType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Génère une chaîne WiFi pour QR Code
     * @param ssid Nom du réseau WiFi
     * @param password Mot de passe (null ou vide pour réseau ouvert)
     * @param security Type de sécurité
     * @param hidden true si le réseau est caché
     * @return Chaîne formatée pour QR Code WiFi
     */
    public static String generateWiFiString(String ssid, String password, SecurityType security, boolean hidden) {
        if (ssid == null || ssid.trim().isEmpty()) {
            throw new IllegalArgumentException("SSID ne peut pas être vide");
        }
        
        StringBuilder wifi = new StringBuilder("WIFI:");
        
        // Type de sécurité
        wifi.append("T:").append(security.getValue()).append(";");
        
        // SSID (échapper les caractères spéciaux)
        wifi.append("S:").append(escapeSpecialChars(ssid)).append(";");
        
        // Mot de passe (seulement si sécurisé)
        if (security != SecurityType.NONE && password != null && !password.isEmpty()) {
            wifi.append("P:").append(escapeSpecialChars(password)).append(";");
        }
        
        // Réseau caché
        if (hidden) {
            wifi.append("H:true;");
        }
        
        // Terminateur obligatoire
        wifi.append(";");
        
        return wifi.toString();
    }
    
    /**
     * Génère un QR Code WiFi simple (réseau ouvert)
     * @param ssid Nom du réseau
     * @return Chaîne WiFi
     */
    public static String generateOpenWiFi(String ssid) {
        return generateWiFiString(ssid, null, SecurityType.NONE, false);
    }
    
    /**
     * Génère un QR Code WiFi WPA/WPA2
     * @param ssid Nom du réseau
     * @param password Mot de passe
     * @return Chaîne WiFi
     */
    public static String generateWPAWiFi(String ssid, String password) {
        return generateWiFiString(ssid, password, SecurityType.WPA, false);
    }
    
    /**
     * Génère un QR Code WiFi WPA avec réseau caché
     * @param ssid Nom du réseau
     * @param password Mot de passe
     * @return Chaîne WiFi
     */
    public static String generateHiddenWPAWiFi(String ssid, String password) {
        return generateWiFiString(ssid, password, SecurityType.WPA, true);
    }
    
    /**
     * Échappe les caractères spéciaux dans SSID et password
     * Caractères à échapper: \ " ; , : < >
     */
    private static String escapeSpecialChars(String input) {
        if (input == null) return "";
        
        return input.replace("\\", "\\\\")  // \ devient \\
                   .replace("\"", "\\\"")   // " devient \"
                   .replace(";", "\\;")     // ; devient \;
                   .replace(",", "\\,")     // , devient \,
                   .replace(":", "\\:")     // : devient \:
                   .replace("<", "\\<")     // < devient \<
                   .replace(">", "\\>");    // > devient \>
    }
    
    /**
     * Valide un SSID WiFi
     * @param ssid SSID à valider
     * @return true si valide
     */
    public static boolean isValidSSID(String ssid) {
        if (ssid == null || ssid.isEmpty()) return false;
        if (ssid.length() > 32) return false;  // SSID max 32 caractères
        return true;
    }
    
    /**
     * Valide un mot de passe WiFi
     * @param password Mot de passe à valider
     * @param security Type de sécurité
     * @return true si valide
     */
    public static boolean isValidPassword(String password, SecurityType security) {
        if (security == SecurityType.NONE) return true;
        if (password == null || password.isEmpty()) return false;
        
        switch (security) {
            case WEP:
                // WEP: 5 ou 13 caractères ASCII, ou 10/26 caractères hex
                int len = password.length();
                return len == 5 || len == 13 || len == 10 || len == 26;
                
            case WPA:
            case WPA2:
            case WPA3:
                // WPA: 8-63 caractères
                return password.length() >= 8 && password.length() <= 63;
                
            default:
                return true;
        }
    }
    
    /**
     * Teste et affiche des exemples de QR Codes WiFi
     */
    public static void printExamples() {
        System.out.println("=== EXEMPLES QR CODES WIFI ===\n");
        
        // Réseau ouvert
        String open = generateOpenWiFi("WiFi_Gratuit");
        System.out.println("📶 Réseau ouvert:");
        System.out.println("   " + open);
        System.out.println();
        
        // Réseau WPA
        String wpa = generateWPAWiFi("MonWiFi", "motdepasse123");
        System.out.println("🔒 Réseau WPA:");
        System.out.println("   " + wpa);
        System.out.println();
        
        // Réseau caché
        String hidden = generateHiddenWPAWiFi("ReseauCache", "supersecret");
        System.out.println("🕵️ Réseau caché:");
        System.out.println("   " + hidden);
        System.out.println();
        
        // Caractères spéciaux
        String special = generateWPAWiFi("WiFi;Spécial", "pass:word,123");
        System.out.println("⚡ Caractères spéciaux:");
        System.out.println("   " + special);
        System.out.println();
        
        System.out.println("📱 Ces chaînes peuvent être encodées en QR Code");
        System.out.println("   Android/iOS se connecteront automatiquement !");
    }
}
