package org.somanybits.minitel.components;

/**
 * Générateur de QR Code scannable simplifié SANS ZXing
 * Utilise un algorithme basique mais plus correct que QRCodeGenerator
 * 
 * @author eddy
 */
public class SimpleScannableQR {
    
    private static final int QR_SIZE = 21; // Version 1
    
    // Pattern de format info pour masque 0, niveau M
    private static final boolean[] FORMAT_INFO_BITS = {
        true, true, true, false, true, true, false, true, true, false, true, false, true, false, false
    };
    
    /**
     * Génère un QR Code plus correct (mais toujours simplifié)
     * @param text Texte à encoder
     * @return Matrice boolean[][]
     */
    public boolean[][] generateImprovedQR(String text) {
        System.out.println("🔲 Génération QR Code amélioré pour: \"" + text + "\"");
        
        boolean[][] modules = new boolean[QR_SIZE][QR_SIZE];
        boolean[][] reserved = new boolean[QR_SIZE][QR_SIZE];
        
        // 1. Finder patterns
        placeFinderPatterns(modules, reserved);
        
        // 2. Separators
        placeSeparators(modules, reserved);
        
        // 3. Timing patterns
        placeTimingPatterns(modules, reserved);
        
        // 4. Dark module
        placeDarkModule(modules, reserved);
        
        // 5. Format information (bits réels)
        placeFormatInfo(modules, reserved);
        
        // 6. Données (encodage amélioré)
        placeDataImproved(modules, reserved, text);
        
        // 7. Masque (pattern 0)
        applyMask(modules, reserved);
        
        System.out.println("✅ QR Code amélioré généré (plus proche du standard)");
        return modules;
    }
    
    private void placeFinderPatterns(boolean[][] modules, boolean[][] reserved) {
        int[][] positions = {{0, 0}, {QR_SIZE - 7, 0}, {0, QR_SIZE - 7}};
        
        boolean[][] pattern = {
            {true,  true,  true,  true,  true,  true,  true},
            {true,  false, false, false, false, false, true},
            {true,  false, true,  true,  true,  false, true},
            {true,  false, true,  true,  true,  false, true},
            {true,  false, true,  true,  true,  false, true},
            {true,  false, false, false, false, false, true},
            {true,  true,  true,  true,  true,  true,  true}
        };
        
        for (int[] pos : positions) {
            for (int dy = 0; dy < 7; dy++) {
                for (int dx = 0; dx < 7; dx++) {
                    int x = pos[0] + dx;
                    int y = pos[1] + dy;
                    if (x < QR_SIZE && y < QR_SIZE) {
                        modules[y][x] = pattern[dy][dx];
                        reserved[y][x] = true;
                    }
                }
            }
        }
    }
    
    private void placeSeparators(boolean[][] modules, boolean[][] reserved) {
        // Séparateurs blancs autour des finder patterns
        int[][] positions = {{0, 0}, {QR_SIZE - 8, 0}, {0, QR_SIZE - 8}};
        
        for (int[] pos : positions) {
            for (int dy = -1; dy <= 7; dy++) {
                for (int dx = -1; dx <= 7; dx++) {
                    int x = pos[0] + dx;
                    int y = pos[1] + dy;
                    if (x >= 0 && x < QR_SIZE && y >= 0 && y < QR_SIZE) {
                        if (dx == -1 || dx == 7 || dy == -1 || dy == 7) {
                            modules[y][x] = false;
                            reserved[y][x] = true;
                        }
                    }
                }
            }
        }
    }
    
    private void placeTimingPatterns(boolean[][] modules, boolean[][] reserved) {
        // Ligne horizontale (y=6)
        for (int x = 8; x < QR_SIZE - 8; x++) {
            modules[6][x] = ((x - 8) % 2 == 0);
            reserved[6][x] = true;
        }
        
        // Ligne verticale (x=6)
        for (int y = 8; y < QR_SIZE - 8; y++) {
            modules[y][6] = ((y - 8) % 2 == 0);
            reserved[y][6] = true;
        }
    }
    
    private void placeDarkModule(boolean[][] modules, boolean[][] reserved) {
        // Module noir obligatoire en (4*version + 9, 8) = (13, 8)
        modules[13][8] = true;
        reserved[13][8] = true;
    }
    
    private void placeFormatInfo(boolean[][] modules, boolean[][] reserved) {
        // Placer les vrais bits de format info
        // Format: masque 0 (000), niveau M (00), bits de correction
        
        // Positions autour du finder pattern supérieur gauche
        int[] xPositions = {0, 1, 2, 3, 4, 5, 7, 8, 8, 8, 8, 8, 8, 8, 8};
        int[] yPositions = {8, 8, 8, 8, 8, 8, 8, 8, 7, 5, 4, 3, 2, 1, 0};
        
        for (int i = 0; i < FORMAT_INFO_BITS.length && i < xPositions.length; i++) {
            int x = xPositions[i];
            int y = yPositions[i];
            if (x < QR_SIZE && y < QR_SIZE) {
                modules[y][x] = FORMAT_INFO_BITS[i];
                reserved[y][x] = true;
            }
        }
        
        // Positions autour des autres finder patterns (miroir)
        for (int i = 0; i < 7 && i < FORMAT_INFO_BITS.length; i++) {
            // Coin supérieur droit
            modules[8][QR_SIZE - 1 - i] = FORMAT_INFO_BITS[i];
            reserved[8][QR_SIZE - 1 - i] = true;
        }
        
        for (int i = 0; i < 8 && (i + 7) < FORMAT_INFO_BITS.length; i++) {
            // Coin inférieur gauche
            modules[QR_SIZE - 1 - i][8] = FORMAT_INFO_BITS[i + 7];
            reserved[QR_SIZE - 1 - i][8] = true;
        }
    }
    
    private void placeDataImproved(boolean[][] modules, boolean[][] reserved, String text) {
        // Encodage amélioré avec structure plus correcte
        byte[] data = text.getBytes();
        
        // Mode byte (0100) + longueur (8 bits) + données
        boolean[] bits = new boolean[4 + 8 + data.length * 8 + 4]; // +4 pour terminator
        int bitIndex = 0;
        
        // Mode indicator: 0100 (byte mode)
        bits[bitIndex++] = false; bits[bitIndex++] = true; 
        bits[bitIndex++] = false; bits[bitIndex++] = false;
        
        // Character count (8 bits)
        int length = Math.min(data.length, 14); // Limite pour version 1 niveau M
        for (int i = 7; i >= 0; i--) {
            bits[bitIndex++] = ((length >> i) & 1) == 1;
        }
        
        // Data bytes
        for (int i = 0; i < length; i++) {
            for (int j = 7; j >= 0; j--) {
                if (bitIndex < bits.length - 4) {
                    bits[bitIndex++] = ((data[i] >> j) & 1) == 1;
                }
            }
        }
        
        // Terminator (0000)
        for (int i = 0; i < 4 && bitIndex < bits.length; i++) {
            bits[bitIndex++] = false;
        }
        
        // Placer les bits dans la matrice (zigzag)
        placeBitsZigzag(modules, reserved, bits, bitIndex);
    }
    
    private void placeBitsZigzag(boolean[][] modules, boolean[][] reserved, boolean[] bits, int bitCount) {
        int bitIndex = 0;
        boolean up = true;
        
        // Parcours en colonnes de droite à gauche
        for (int col = QR_SIZE - 1; col >= 1; col -= 2) {
            if (col == 6) col--; // Skip timing column
            
            for (int i = 0; i < QR_SIZE; i++) {
                int row = up ? QR_SIZE - 1 - i : i;
                
                // Deux colonnes à la fois
                for (int c = 0; c < 2; c++) {
                    int x = col - c;
                    if (x >= 0 && !reserved[row][x] && bitIndex < bitCount) {
                        modules[row][x] = bits[bitIndex++];
                    }
                }
            }
            up = !up;
        }
    }
    
    private void applyMask(boolean[][] modules, boolean[][] reserved) {
        // Masque pattern 0: (i + j) % 2 == 0
        for (int y = 0; y < QR_SIZE; y++) {
            for (int x = 0; x < QR_SIZE; x++) {
                if (!reserved[y][x] && (x + y) % 2 == 0) {
                    modules[y][x] = !modules[y][x];
                }
            }
        }
    }
    
    /**
     * Affiche le QR Code en ASCII
     */
    public void printQRCode(boolean[][] qr) {
        System.out.println("=== QR Code Amélioré ===");
        for (int y = 0; y < qr.length; y++) {
            for (int x = 0; x < qr[y].length; x++) {
                System.out.print(qr[y][x] ? "██" : "  ");
            }
            System.out.println();
        }
        System.out.println("========================");
    }
}
