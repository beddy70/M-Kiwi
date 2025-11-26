package org.somanybits.minitel.server.mmodules;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Path;
import java.util.HashMap;
import org.somanybits.minitel.server.ModelMModule;
import org.somanybits.minitel.components.GraphTel;

/**
 * Module de démonstration des QR Codes sur Minitel
 * 
 * Usage: http://localhost:8080/QRCodeDemo.mod?text=Hello+World&scale=2
 * 
 * @author eddy
 */
public class QRCodeDemo extends ModelMModule {
    
    public QRCodeDemo(HashMap<String, String> params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }
    
    @Override
    public String getResponse() {
        String text = params != null ? params.get("text") : null;
        String scaleStr = params != null ? params.get("scale") : "2";
        
        if (text == null || text.isEmpty()) {
            text = "MINITEL 2024";
        }
        
        int scale = 2;
        try {
            scale = Integer.parseInt(scaleStr);
            if (scale < 1) scale = 1;
            if (scale > 4) scale = 4; // Limiter pour éviter débordement
        } catch (NumberFormatException e) {
            scale = 2;
        }
        
        StringBuilder resp = new StringBuilder();
        resp.append("<minitel title=\"QR Code Demo\">\n");
        resp.append("<div left=\"1\" top=\"1\" width=\"38\" height=\"20\">\n");
        
        resp.append("<row>╔══════════════════════════════════════╗</row>\n");
        resp.append("<row>║          GÉNÉRATEUR QR CODE          ║</row>\n");
        resp.append("<row>╠══════════════════════════════════════╣</row>\n");
        resp.append("<row>║                                      ║</row>\n");
        
        // Simuler la génération du QR Code (pour l'affichage VTML)
        resp.append("<row>║  Texte encodé: ").append(String.format("%-19s", text)).append("║</row>\n");
        resp.append("<row>║  Échelle: ").append(scale).append("x                           ║</row>\n");
        resp.append("<row>║                                      ║</row>\n");
        resp.append("<row>║  QR Code généré en semi-graphique:   ║</row>\n");
        resp.append("<row>║                                      ║</row>\n");
        
        // Zone pour le QR Code (représentation ASCII simplifiée)
        resp.append("<row>║    ████  ██    ██  ██    ████    ║</row>\n");
        resp.append("<row>║    █  █  ██    ██  ██    █  █    ║</row>\n");
        resp.append("<row>║    █▄▄█  ██▄▄  ██▄▄██    █▄▄█    ║</row>\n");
        resp.append("<row>║    ████  ████  ████████  ████    ║</row>\n");
        resp.append("<row>║      ██    ██    ██  ██          ║</row>\n");
        resp.append("<row>║    ████████████████████████████   ║</row>\n");
        resp.append("<row>║                                      ║</row>\n");
        
        resp.append("<row>║  Résolution: 80x75 pixels           ║</row>\n");
        resp.append("<row>║  Format: Semi-graphique Minitel     ║</row>\n");
        resp.append("<row>║                                      ║</row>\n");
        resp.append("<row>╚══════════════════════════════════════╝</row>\n");
        
        resp.append("</div>\n");
        
        // Menu de navigation
        resp.append("<menu name=\"qr_menu\" left=\"2\" top=\"22\" width=\"36\" height=\"3\" keytype=\"number\">\n");
        resp.append("<item link=\"QRCodeDemo.mod?text=HELLO&scale=1\">1. Test HELLO (1x)</item>\n");
        resp.append("<item link=\"QRCodeDemo.mod?text=MINITEL&scale=2\">2. Test MINITEL (2x)</item>\n");
        resp.append("<item link=\"QRCodeDemo.mod?text=https://minitel.fr&scale=3\">3. URL (3x)</item>\n");
        resp.append("<item link=\"index.vtml\">0. Retour menu</item>\n");
        resp.append("</menu>\n");
        
        resp.append("</minitel>\n");
        
        // Log pour debug
        System.out.println("QRCodeDemo: Génération QR Code pour \"" + text + "\" échelle " + scale + "x");
        
        // Ici on pourrait générer le vrai QR Code avec GraphTel
        // et l'envoyer directement au Minitel via des séquences de contrôle
        generateActualQRCode(text, scale);
        
        return resp.toString();
    }
    
    /**
     * Génère un vrai QR Code avec GraphTel (pour test en console)
     */
    private void generateActualQRCode(String text, int scale) {
        try {
            // Créer un GraphTel avec résolution 80x75
            GraphTel gfx = new GraphTel(80, 75);
            
            // Générer le QR Code centré
            gfx.generateCenteredQRCode(text, scale);
            
            System.out.println("QR Code généré avec succès pour: \"" + text + "\"");
            
        } catch (Exception e) {
            System.err.println("Erreur génération QR Code: " + e.getMessage());
        }
    }
    
    @Override
    public String getVersion() {
        return "1.0";
    }
    
    @Override
    public String getContentType() {
        return "text/plain; charset=UTF-8";
    }
}
