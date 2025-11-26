/*
 * QR Code Generator for Minitel Semi-Graphic Display
 * Generates QR codes optimized for 80x75 pixel resolution
 * 
 * @author eddy
 */
package org.somanybits.minitel.components;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de QR Code simplifié pour affichage Minitel
 * Implémente une version basique du standard QR Code
 */
public class QRCodeGenerator {
    
    // Tailles de QR Code supportées
    public static final int QR_VERSION_1 = 21; // 21x21 modules
    public static final int QR_VERSION_2 = 25; // 25x25 modules  
    public static final int QR_VERSION_3 = 29; // 29x29 modules
    
    // Patterns de positionnement (7x7)
    private static final boolean[][] FINDER_PATTERN = {
        {true,  true,  true,  true,  true,  true,  true},
        {true,  false, false, false, false, false, true},
        {true,  false, true,  true,  true,  false, true},
        {true,  false, true,  true,  true,  false, true},
        {true,  false, true,  true,  true,  false, true},
        {true,  false, false, false, false, false, true},
        {true,  true,  true,  true,  true,  true,  true}
    };
    
    // Note: Les séparateurs sont gérés directement dans placeFinderPattern()
    
    private int size;
    private boolean[][] modules;
    private boolean[][] reserved;
    
    public QRCodeGenerator(int version) {
        switch (version) {
            case 1: size = QR_VERSION_1; break;
            case 2: size = QR_VERSION_2; break;
            case 3: size = QR_VERSION_3; break;
            default: size = QR_VERSION_1;
        }
        
        modules = new boolean[size][size];
        reserved = new boolean[size][size];
        
        // Initialiser les patterns fixes
        initializeFixedPatterns();
    }
    
    /**
     * Génère un QR Code simple avec le texte donné
     * @param text Texte à encoder
     * @return Matrice de pixels (true = noir, false = blanc)
     */
    public boolean[][] generateQRCode(String text) {
        // Réinitialiser
        clearModules();
        initializeFixedPatterns();
        
        // Encoder le texte (version simplifiée)
        encodeText(text);
        
        // Appliquer un masque simple
        applyMask();
        
        return modules;
    }
    
    /**
     * Génère un QR Code de démonstration avec motif de test
     */
    public boolean[][] generateTestPattern() {
        clearModules();
        initializeFixedPatterns();
        
        // Créer un motif de test simple
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!reserved[y][x]) {
                    // Motif en damier pour test
                    modules[y][x] = ((x + y) % 2 == 0);
                }
            }
        }
        
        return modules;
    }
    
    private void clearModules() {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                modules[y][x] = false;
                reserved[y][x] = false;
            }
        }
    }
    
    private void initializeFixedPatterns() {
        // Finder patterns aux 3 coins
        placeFinderPattern(0, 0);                    // Coin supérieur gauche
        placeFinderPattern(size - 7, 0);             // Coin supérieur droit  
        placeFinderPattern(0, size - 7);             // Coin inférieur gauche
        
        // Timing patterns (lignes de synchronisation)
        placeTimingPatterns();
        
        // Format information (version simplifiée)
        placeFormatInfo();
    }
    
    private void placeFinderPattern(int x, int y) {
        // Placer le finder pattern 7x7
        for (int dy = 0; dy < 7; dy++) {
            for (int dx = 0; dx < 7; dx++) {
                if (x + dx < size && y + dy < size) {
                    modules[y + dy][x + dx] = FINDER_PATTERN[dy][dx];
                    reserved[y + dy][x + dx] = true;
                }
            }
        }
        
        // Placer le séparateur 8x8 (bordure blanche)
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && px < size && py >= 0 && py < size) {
                    if (dx == -1 || dx == 7 || dy == -1 || dy == 7) {
                        modules[py][px] = false;
                        reserved[py][px] = true;
                    }
                }
            }
        }
    }
    
    private void placeTimingPatterns() {
        // Ligne horizontale (y=6)
        for (int x = 8; x < size - 8; x++) {
            modules[6][x] = (x % 2 == 0);
            reserved[6][x] = true;
        }
        
        // Ligne verticale (x=6)  
        for (int y = 8; y < size - 8; y++) {
            modules[y][6] = (y % 2 == 0);
            reserved[y][6] = true;
        }
    }
    
    private void placeFormatInfo() {
        // Version simplifiée - juste marquer les zones réservées
        // Format info autour du finder pattern supérieur gauche
        for (int i = 0; i < 9; i++) {
            if (i != 6) { // Skip timing pattern
                reserved[8][i] = true;
                reserved[i][8] = true;
            }
        }
        
        // Format info autour des autres finder patterns
        for (int i = 0; i < 8; i++) {
            reserved[8][size - 1 - i] = true;
            reserved[size - 1 - i][8] = true;
        }
    }
    
    private void encodeText(String text) {
        // Encodage très simplifié - juste pour démonstration
        // Dans un vrai QR Code, il faudrait implémenter Reed-Solomon, etc.
        
        byte[] data = text.getBytes();
        List<Boolean> bits = new ArrayList<>();
        
        // Mode indicator (4 bits) - Mode Byte = 0100
        bits.add(false); bits.add(true); bits.add(false); bits.add(false);
        
        // Character count (8 bits pour version 1)
        int length = Math.min(data.length, 17); // Limite pour version 1
        for (int i = 7; i >= 0; i--) {
            bits.add(((length >> i) & 1) == 1);
        }
        
        // Data
        for (int i = 0; i < length; i++) {
            for (int j = 7; j >= 0; j--) {
                bits.add(((data[i] >> j) & 1) == 1);
            }
        }
        
        // Padding si nécessaire
        while (bits.size() % 8 != 0) {
            bits.add(false);
        }
        
        // Placer les données dans la matrice (zigzag pattern simplifié)
        placeBits(bits);
    }
    
    private void placeBits(List<Boolean> bits) {
        int bitIndex = 0;
        boolean up = true;
        
        // Parcours en colonnes de droite à gauche
        for (int col = size - 1; col >= 1; col -= 2) {
            if (col == 6) col--; // Skip timing column
            
            for (int i = 0; i < size; i++) {
                int row = up ? size - 1 - i : i;
                
                // Deux colonnes à la fois
                for (int c = 0; c < 2; c++) {
                    int x = col - c;
                    if (!reserved[row][x] && bitIndex < bits.size()) {
                        modules[row][x] = bits.get(bitIndex++);
                    }
                }
            }
            up = !up;
        }
    }
    
    private void applyMask() {
        // Masque simple : pattern 0 = (i + j) % 2 == 0
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!reserved[y][x] && (x + y) % 2 == 0) {
                    modules[y][x] = !modules[y][x];
                }
            }
        }
    }
    
    public int getSize() {
        return size;
    }
    
    /**
     * Affiche le QR Code en ASCII pour debug
     */
    public void printQRCode(boolean[][] qr) {
        System.out.println("=== QR Code ===");
        for (int y = 0; y < qr.length; y++) {
            for (int x = 0; x < qr[y].length; x++) {
                System.out.print(qr[y][x] ? "██" : "  ");
            }
            System.out.println();
        }
        System.out.println("===============");
    }
}
