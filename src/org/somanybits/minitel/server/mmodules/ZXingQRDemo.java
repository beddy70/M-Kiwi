package org.somanybits.minitel.server.mmodules;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Path;
import java.util.HashMap;
import org.somanybits.minitel.server.ModelMModule;
import org.somanybits.minitel.components.qrcode.ScannableQRGenerator;
import org.somanybits.minitel.components.qrcode.WiFiQRGenerator;
import org.somanybits.minitel.components.qrcode.WiFiQRGenerator.SecurityType;

/**
 * Module de démonstration ZXing - QR Codes 100% scannables
 * 
 * Usage:
 * - /ZXingQRDemo.mod?text=Hello+World
 * - /ZXingQRDemo.mod?wifi=true&ssid=MonWiFi&password=secret&security=WPA
 * 
 * @author eddy
 */
public class ZXingQRDemo extends ModelMModule {
    
    public ZXingQRDemo(HashMap<String, String> params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }
    
    @Override
    public String getResponse() {
        boolean isWiFi = "true".equalsIgnoreCase(getParameter("wifi", "false"));
        
        if (isWiFi) {
            return generateWiFiQRResponse();
        } else {
            return generateTextQRResponse();
        }
    }
    
    private String generateTextQRResponse() {
        String text = getParameter("text", "Hello ZXing!");
        
        // Générer QR Code ZXing (pour la console)
        try {
            ScannableQRGenerator scannableGen = new ScannableQRGenerator();
            boolean[][] qrMatrix = scannableGen.generateScannableQR(text, 21);
            System.out.println("✅ QR Code ZXing généré pour: \"" + text + "\"");
        } catch (Exception e) {
            System.err.println("❌ Erreur ZXing: " + e.getMessage());
        }
        
        // Retourner page VTML
        StringBuilder vtml = new StringBuilder();
        vtml.append("\\f\\c"); // Effacer écran
        vtml.append("\\j\\b\\r"); // Jaune sur bleu, double hauteur
        vtml.append("   QR CODE ZXING   \\n");
        vtml.append("\\g\\a\\s"); // Vert sur noir, taille normale
        vtml.append("\\n");
        vtml.append("Texte: ").append(text).append("\\n\\n");
        vtml.append("\\b\\r"); // Bleu, double hauteur
        vtml.append("QR Code ZXing genere !\\n");
        vtml.append("\\g\\s"); // Vert, taille normale
        vtml.append("100% scannable iPhone/Android\\n\\n");
        
        vtml.append("\\j"); // Jaune
        vtml.append("EXEMPLES:\\n");
        vtml.append("\\g"); // Vert
        vtml.append("?text=Bonjour+Monde\\n");
        vtml.append("?wifi=true&ssid=MonWiFi&password=secret\\n");
        
        vtml.append("\\n\\r\\j[RETOUR] pour menu");
        
        return vtml.toString();
    }
    
    private String generateWiFiQRResponse() {
        String ssid = getParameter("ssid", "MonWiFi");
        String password = getParameter("password", "");
        String securityStr = getParameter("security", "WPA").toUpperCase();
        boolean hidden = "true".equalsIgnoreCase(getParameter("hidden", "false"));
        
        // Convertir le type de sécurité
        SecurityType security;
        try {
            security = SecurityType.valueOf(securityStr);
        } catch (IllegalArgumentException e) {
            security = SecurityType.WPA;
        }
        
        // Générer la chaîne WiFi
        String wifiString = WiFiQRGenerator.generateWiFiString(ssid, 
                                                               password.isEmpty() ? null : password, 
                                                               security, 
                                                               hidden);
        
        // Générer QR Code ZXing (pour la console)
        try {
            ScannableQRGenerator scannableGen = new ScannableQRGenerator();
            boolean[][] qrMatrix = scannableGen.generateScannableQR(wifiString, 21);
            System.out.println("✅ QR Code WiFi ZXing généré pour: " + ssid);
        } catch (Exception e) {
            System.err.println("❌ Erreur ZXing WiFi: " + e.getMessage());
        }
        
        // Retourner page VTML
        StringBuilder vtml = new StringBuilder();
        vtml.append("\\f\\c"); // Effacer écran
        vtml.append("\\j\\b\\r"); // Jaune sur bleu, double hauteur
        vtml.append("  QR CODE WIFI ZXING \\n");
        vtml.append("\\g\\a\\s"); // Vert sur noir, taille normale
        vtml.append("\\n");
        vtml.append("SSID: ").append(ssid).append("\\n");
        vtml.append("Securite: ").append(security).append("\\n");
        vtml.append("Cache: ").append(hidden ? "Oui" : "Non").append("\\n\\n");
        
        vtml.append("\\b\\r"); // Bleu, double hauteur
        vtml.append("QR WiFi ZXing genere !\\n");
        vtml.append("\\g\\s"); // Vert, taille normale
        vtml.append("Connexion automatique\\n");
        vtml.append("iPhone/Android compatible\\n\\n");
        
        vtml.append("\\m"); // Magenta
        vtml.append("Chaine: ").append(wifiString).append("\\n\\n");
        
        vtml.append("\\n\\r\\j[RETOUR] pour menu");
        
        return vtml.toString();
    }
    
    private String getParameter(String key, String defaultValue) {
        String value = params != null ? params.get(key) : null;
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    @Override
    public String getVersion() {
        return "1.0-ZXing";
    }
    
    @Override
    public String getContentType() {
        return "text/plain; charset=utf-8";
    }
}
