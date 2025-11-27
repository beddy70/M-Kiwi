package org.somanybits.minitel.server.mmodules;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Path;
import java.util.HashMap;
import org.somanybits.minitel.server.ModelMModule;

/**
 * Module de démonstration du tag <qrcode> VTML
 * 
 * Usage: /QRCodeVTMLDemo.mod
 * 
 * @author eddy
 */
public class QRCodeVTMLDemo extends ModelMModule {
    
    public QRCodeVTMLDemo(HashMap<String, String> params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }
    
    @Override
    public String getResponse() {
        // Générer une page VTML avec des exemples de QR codes
        StringBuilder vtml = new StringBuilder();
        
        vtml.append("<!DOCTYPE html>\n");
        vtml.append("<html>\n");
        vtml.append("<head><title>Demo QR Code VTML</title></head>\n");
        vtml.append("<body>\n");
        vtml.append("    <minitel title=\"QR Code VTML Demo\">\n");
        
        // En-tête
        vtml.append("        <div left=\"0\" top=\"1\">\n");
        vtml.append("            <row>=== QR CODE VTML DEMO ===</row>\n");
        vtml.append("            <br/>\n");
        vtml.append("        </div>\n");
        
        // QR Code URL
        vtml.append("        <div left=\"0\" top=\"4\">\n");
        vtml.append("            <row>1. Site Web:</row>\n");
        vtml.append("        </div>\n");
        vtml.append("        <qrcode type=\"url\" message=\"https://eddy-briere.com\" left=\"2\" top=\"6\"/>\n");
        
        // QR Code WiFi WPA
        vtml.append("        <div left=\"0\" top=\"18\">\n");
        vtml.append("            <row>2. WiFi WPA:</row>\n");
        vtml.append("        </div>\n");
        vtml.append("        <qrcode type=\"wpawifi\" message=\"ssid:'Labo Game', password:'Girafe1970'\" left=\"2\" top=\"20\"/>\n");
        
        // QR Code WiFi Ouvert
        vtml.append("        <div left=\"0\" top=\"32\">\n");
        vtml.append("            <row>3. WiFi Ouvert:</row>\n");
        vtml.append("        </div>\n");
        vtml.append("        <qrcode type=\"wpawifi\" message=\"ssid:'WiFi_Gratuit'\" left=\"2\" top=\"34\"/>\n");
        
        // QR Code texte simple
        vtml.append("        <div left=\"0\" top=\"46\">\n");
        vtml.append("            <row>4. Texte simple:</row>\n");
        vtml.append("        </div>\n");
        vtml.append("        <qrcode type=\"url\" message=\"Hello VTML QR Code!\" left=\"2\" top=\"48\"/>\n");
        
        // Pied de page
        vtml.append("        <div left=\"0\" top=\"60\">\n");
        vtml.append("            <row>Demo terminee !</row>\n");
        vtml.append("            <row>Syntaxe: &lt;qrcode type=\"url|wpawifi\" message=\"...\" /&gt;</row>\n");
        vtml.append("        </div>\n");
        
        vtml.append("    </minitel>\n");
        vtml.append("</body>\n");
        vtml.append("</html>\n");
        
        return vtml.toString();
    }
    
    @Override
    public String getVersion() {
        return "1.0-VTML";
    }
    
    @Override
    public String getContentType() {
        return "text/html; charset=utf-8";
    }
}
