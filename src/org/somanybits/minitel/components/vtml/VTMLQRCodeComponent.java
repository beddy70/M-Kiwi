package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.Teletel;
import org.somanybits.minitel.components.GraphTel;
import org.somanybits.minitel.components.ModelMComponent;
import org.somanybits.minitel.components.qrcode.ScannableQRGenerator;
import org.somanybits.minitel.components.qrcode.WiFiQRGenerator;

/**
 * Composant VTML représentant le tag <qrcode>
 * Génère des QR codes avec types url ou wpawifi
 *
 * @author eddy
 */
public class VTMLQRCodeComponent extends ModelMComponent {

    public enum QRType {
        URL, WPAWIFI
    }

    private QRType type;
    private String message;
    private int scale;

    public VTMLQRCodeComponent() {
        super();
        this.type = QRType.URL;
        this.scale = 1;
    }

    public VTMLQRCodeComponent(QRType type, String message, int scale) {
        super();
        this.type = type;
        this.message = message;
        this.scale = scale;
    }

    public QRType getType() {
        return type;
    }

    public void setType(QRType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    /**
     * Parse le message WiFi au format: ssid:'nom', password:'motdepasse'
     */
    private String parseWiFiMessage(String message) {
        try {
            String ssid = null;
            String password = null;

            if (message.contains("ssid:")) {
                int ssidStart = message.indexOf("ssid:'") + 6;
                int ssidEnd = message.indexOf("'", ssidStart);
                if (ssidEnd > ssidStart) {
                    ssid = message.substring(ssidStart, ssidEnd);
                }
            }

            if (message.contains("password:")) {
                int passStart = message.indexOf("password:'") + 10;
                int passEnd = message.indexOf("'", passStart);
                if (passEnd > passStart) {
                    password = message.substring(passStart, passEnd);
                }
            }

            if (ssid != null) {
                if (password != null && !password.trim().isEmpty()) {
                    return WiFiQRGenerator.generateWPAWiFi(ssid, password);
                } else {
                    return WiFiQRGenerator.generateOpenWiFi(ssid);
                }
            }

            return message; // Fallback
        } catch (Exception e) {
            return message; // Fallback
        }
    }

    @Override
    public byte[] getBytes() {
        try {
            // Générer le contenu QR selon le type
            String qrContent = message;
            if (type == QRType.WPAWIFI && message != null) {
                qrContent = parseWiFiMessage(message);
            }

            // Générer le QR Code avec ZXing
            ScannableQRGenerator scannableGen = new ScannableQRGenerator();
            boolean[][] qrMatrix = scannableGen.generateScannableQR(qrContent, 21);

            // Appliquer le scale si > 1
            if (scale > 1) {
                qrMatrix = scaleMatrix(qrMatrix, scale);
            }

            boolean[] bitmap1D = ScannableQRGenerator.matrixTo1D(qrMatrix);

            GraphTel gfx = new GraphTel(qrMatrix.length, qrMatrix.length);
            gfx.writeBitmap(bitmap1D);
            gfx.inverseBitmap();

            return gfx.getDrawToBytes(getX(), getY());

        } catch (Exception e) {
            return ("[QR ERROR: " + e.getMessage() + "]\n").getBytes();
        }
    }

    /**
     * Agrandit la matrice QR selon le facteur d'échelle
     */
    private boolean[][] scaleMatrix(boolean[][] original, int scaleFactor) {
        int originalSize = original.length;
        int newSize = originalSize * scaleFactor;
        boolean[][] scaled = new boolean[newSize][newSize];
        
        for (int y = 0; y < originalSize; y++) {
            for (int x = 0; x < originalSize; x++) {
                boolean value = original[y][x];
                // Remplir le bloc scaleFactor x scaleFactor
                for (int dy = 0; dy < scaleFactor; dy++) {
                    for (int dx = 0; dx < scaleFactor; dx++) {
                        scaled[y * scaleFactor + dy][x * scaleFactor + dx] = value;
                    }
                }
            }
        }
        
        return scaled;
    }
}
