package org.somanybits.minitel.components;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.HashMap;
import java.util.Map;

/**
 * Générateur de QR Codes scannables avec ZXing
 * Produit des QR codes conformes au standard ISO/IEC 18004
 * 
 * @author eddy
 */
public class ScannableQRGenerator {
    
    private final QRCodeWriter writer;
    private final Map<EncodeHintType, Object> hints;
    
    public ScannableQRGenerator() {
        writer = new QRCodeWriter();
        hints = new HashMap<>();
        
        // Configuration pour QR codes optimisés
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // ~15% correction
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // Bordure minimale pour Minitel
    }
    
    /**
     * Génère un QR Code scannable
     * @param text Texte à encoder
     * @param size Taille du QR Code (21, 25, 29, etc.)
     * @return Matrice boolean[][] (true = noir, false = blanc)
     */
    public boolean[][] generateScannableQR(String text, int size) {
        try {
            System.out.println("🔲 Génération QR Code scannable pour: \"" + text + "\"");
            
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            
            boolean[][] result = new boolean[matrix.getHeight()][matrix.getWidth()];
            
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    result[y][x] = matrix.get(x, y);
                }
            }
            
            System.out.println("✅ QR Code scannable généré (" + matrix.getWidth() + "x" + matrix.getHeight() + ")");
            return result;
            
        } catch (WriterException e) {
            System.err.println("❌ Erreur génération QR Code: " + e.getMessage());
            
            // Fallback vers le générateur visuel
            System.out.println("🔄 Fallback vers motif visuel...");
            QRCodeGenerator fallback = new QRCodeGenerator(1);
            return fallback.generateVisualPattern(text);
        }
    }
    
    /**
     * Génère un QR Code avec niveau d'erreur spécifique
     * @param text Texte à encoder
     * @param size Taille du QR Code
     * @param errorLevel Niveau de correction (L, M, Q, H)
     * @return Matrice boolean[][]
     */
    public boolean[][] generateScannableQR(String text, int size, ErrorCorrectionLevel errorLevel) {
        Map<EncodeHintType, Object> customHints = new HashMap<>(hints);
        customHints.put(EncodeHintType.ERROR_CORRECTION, errorLevel);
        
        try {
            System.out.println("🔲 QR Code " + errorLevel + " pour: \"" + text + "\"");
            
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, customHints);
            
            boolean[][] result = new boolean[matrix.getHeight()][matrix.getWidth()];
            
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    result[y][x] = matrix.get(x, y);
                }
            }
            
            System.out.println("✅ QR Code " + errorLevel + " généré (" + matrix.getWidth() + "x" + matrix.getHeight() + ")");
            return result;
            
        } catch (WriterException e) {
            System.err.println("❌ Erreur QR Code " + errorLevel + ": " + e.getMessage());
            return generateScannableQR(text, size); // Fallback vers config par défaut
        }
    }
    
    /**
     * Teste la capacité d'encodage pour différentes tailles
     * @param text Texte à tester
     */
    public void testCapacity(String text) {
        System.out.println("=== TEST CAPACITÉ QR CODE ===");
        System.out.println("Texte: \"" + text + "\" (" + text.length() + " caractères)");
        
        int[] sizes = {21, 25, 29, 33, 37}; // Versions 1-5
        String[] versions = {"V1", "V2", "V3", "V4", "V5"};
        
        for (int i = 0; i < sizes.length; i++) {
            try {
                BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, sizes[i], sizes[i], hints);
                System.out.println("✅ " + versions[i] + " (" + sizes[i] + "x" + sizes[i] + "): OK");
            } catch (WriterException e) {
                System.out.println("❌ " + versions[i] + " (" + sizes[i] + "x" + sizes[i] + "): " + e.getMessage());
            }
        }
        System.out.println("===============================");
    }
    
    /**
     * Affiche un QR Code en ASCII pour debug
     */
    public void printQRCode(boolean[][] qr) {
        System.out.println("=== QR Code Scannable ===");
        for (int y = 0; y < qr.length; y++) {
            for (int x = 0; x < qr[y].length; x++) {
                System.out.print(qr[y][x] ? "██" : "  ");
            }
            System.out.println();
        }
        System.out.println("=========================");
    }
}
