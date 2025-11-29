package org.somanybits.minitel.util;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Utilitaire pour gérer les connexions SSL/HTTPS
 * Permet de désactiver la vérification des certificats (pour le développement)
 * 
 * @author eddy
 */
public class SSLHelper {
    
    private static boolean initialized = false;
    
    /**
     * Désactive la vérification des certificats SSL pour toutes les connexions HTTPS.
     * ATTENTION : À utiliser uniquement en développement !
     * 
     * Cela permet de :
     * - Accepter les certificats auto-signés
     * - Ignorer les erreurs de chaîne de certification
     * - Contourner les problèmes de SNI
     */
    public static synchronized void disableCertificateValidation() {
        if (initialized) {
            return; // Déjà initialisé
        }
        
        try {
            // Créer un TrustManager qui accepte tous les certificats
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Accepter tous les certificats client
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Accepter tous les certificats serveur
                    }
                }
            };
            
            // Installer le TrustManager permissif
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            // Désactiver la vérification du hostname
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            
            initialized = true;
            System.out.println("⚠️ SSL: Vérification des certificats désactivée (mode développement)");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la désactivation SSL: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vérifie si la validation SSL est désactivée
     */
    public static boolean isValidationDisabled() {
        return initialized;
    }
}
