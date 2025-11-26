/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.somanybits.minitel.components;

import java.io.IOException;
import org.somanybits.minitel.Teletel;

/**
 *
 * @author eddy
 */
public class GraphTel implements PageMinitel {

    final public static int DEFAULT_SCREEN_WIDTH = Teletel.PAGE_WIDTH * 2;
    final public static int DEFAULT_SCREEN_HEIGHT = Teletel.PAGE_HEIGHT * 3;
    //private byte pen=Teletel.COLOR_WHITE;

    private boolean pen = true;
    private boolean screenGFX[];
 
    private byte screenColor[];
    private int widthScreen;
    private int heightScreen;
    private byte ink = Teletel.COLOR_WHITE;

    public GraphTel(int w, int h) {
        init(w, h);
    }

    public GraphTel() {
        init(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
    }

    private void init(int w, int h) {
        System.out.println(" size=" + w + "-" + h);
        System.out.println((w % 2) + "-" + (h % 3));
        if ((w % 2) != 0) {
            w = w + (2 - (w % 2));
        }
        if ((h % 3) != 0) {
            h = h + (3 - (h % 3));
        }
        System.out.println("new size=" + w + "-" + h);
        widthScreen = w;
        heightScreen = h;

        screenGFX = new boolean[widthScreen * heightScreen];
        screenColor = new byte[widthScreen * heightScreen];
    }

    public void setInk(byte color) {
        ink = color;
    }

    public void setPen(boolean color) {
        pen = color;
    }

    public void setLine(int x1, int y1, int x2, int y2) {
        //algo built with this link http://fvirtman.free.fr/recueil/02_03_03_line.c.php
//        int dx = (x2 - x1);
//        int dy = (y2 - y1);
//
//        for (int x = x1; x < x2; x++) {
//            int y = y1 + dy * (x - x1) / dx;
//            setPixel(x, y);
//        }
        int x, y;
        int Dx, Dy;
        int xincr, yincr;
        int erreur;
        int i;

        Dx = Math.abs(x2 - x1);
        Dy = Math.abs(y2 - y1);
        if (x1 < x2) {
            xincr = 1;
        } else {
            xincr = -1;
        }
        if (y1 < y2) {
            yincr = 1;
        } else {
            yincr = -1;
        }

        x = x1;
        y = y1;
        if (Dx > Dy) {
            erreur = Dx / 2;
            for (i = 0; i < Dx; i++) {
                x += xincr;
                erreur += Dy;
                if (erreur > Dx) {
                    erreur -= Dx;
                    y += yincr;
                }
                setPixel(x, y);
            }
        } else {
            erreur = Dy / 2;
            for (i = 0; i < Dy; i++) {
                y += yincr;
                erreur += Dx;
                if (erreur > Dy) {
                    erreur -= Dy;
                    x += xincr;
                }
                setPixel(x, y);
            }
        }
        setPixel(x1, y1);
        setPixel(x2, y2);
    }

    public void setCircle(int x1, int y1, int r) {
        //Algo built with this link https://www.wikiwand.com/fr/Algorithme_de_trac%C3%A9_de_cercle_d'Andres
        int x = 0;
        int y = r;
        int d = r - 1;

        while (y >= x) {

            setPixel(x1 + x, y1 + y);
            setPixel(x1 + y, y1 + x);
            setPixel(x1 - x, y1 + y);
            setPixel(x1 - y, y1 + x);
            setPixel(x1 + x, y1 - y);
            setPixel(x1 + y, y1 - x);
            setPixel(x1 - x, y1 - y);
            setPixel(x1 - y, y1 - x);

            if (d >= 2 * x) {
                d -= 2 * x + 1;
                x++;
            } else if (d < 2 * (r - y)) {
                d += 2 * y - 1;
                y--;
            } else {
                d += 2 * (y - x - 1);
                y--;
                x++;
            }
        }
    }

    public void writeBitmap(byte[] bitmap) {

        if (bitmap.length < (widthScreen * heightScreen >> 3)) {
//            throw new IllegalArgumentException("byte[] bitamp is too small (" + (widthScreen * heightScreen >> 3) + " should be good size).");
        }

//         System.out.println("w=" + widthScreen + " h=" + heightScreen);
        for (int j = 0; j < heightScreen; j++) {
//            String line = "";
            for (int i = 0; i < widthScreen; i++) {

                int index = ((j * ((widthScreen + 7) >> 3)) + (i >> 3));
                //System.out.println(" i=" + i + " j=" + j + " index->" + index);

                int bytemap = bitmap[index] & 0xFF;
                int mask = ((0x80) >> ((i % 8))) & 0xFF;

                if ((bytemap & mask) != 0) {
                    setPixel(i, j);
//                    line += "#";
//                     System.out.println("\t\t0x" + String.format("%02X", bytemap) + " pixel=" + true + " mask=0x" + String.format("%02X", mask));

                } else {
//                   System.out.println("\t\t0x" + String.format("%02X", bytemap) + " pixel=" + false + " mask=0x" + String.format("%02X", mask));
//                    line += "_";
                }

            }
//            System.out.println(line);
        }
        debugAscii(bitmap, widthScreen, heightScreen);
    }

    private static void debugAscii(byte[] data, int width, int height) {
        int bytesPerRow = (width + 7) / 8;
        System.out.println("=== Aperçu ASCII ===");
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < width; x++) {
                int byteIndex = y * bytesPerRow + (x / 8);
                int bitIndex = 7 - (x % 8);
                boolean black = ((data[byteIndex] >> bitIndex) & 1) != 0;
                line.append(black ? '#' : '.');
            }
            System.out.println(line);
        }
        System.out.println("====================");
    }

    public void setPixel(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            screenGFX[widthScreen * y + x] = pen;
            screenColor[widthScreen * y + x] = ink;
        }

    }

    public boolean getPixel(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            return screenGFX[widthScreen * y + x];
        }
        return false;
    }

    private byte[] convertToSemiGraph() {
        byte[] data = new byte[(widthScreen / 2) * (heightScreen / 3)];

        int car = 0;
        for (int j = 0; j < heightScreen; j += 3) {

            for (int i = 0; i < widthScreen; i += 2) {
                byte semigfx = 0;
                //line 0
                if (getPixel(i, j)) {
                    semigfx |= 0b0100001;
                }
                if (getPixel(i + 1, j)) {
                    semigfx |= 0b0100010;
                }
                //line 1
                if (getPixel(i, j + 1)) {
                    semigfx |= 0b0100100;
                }
                if (getPixel(i + 1, j + 1)) {
                    semigfx |= 0b0101000;
                }
                //line 2
                if (getPixel(i, j + 2)) {
                    semigfx |= 0b0110000;
                }
                if (getPixel(i + 1, j + 2)) {
                    semigfx |= 0b1100000;
                }
                //full (exception)
                if (semigfx == 0b1111111) {
                    semigfx = 0b1011111;
                } else if (semigfx == 0) {
                    semigfx = 0x20;
                }

                data[car++] = (byte) semigfx;
                //System.out.println(i + ":" + j + "-" + car);
            }

        }
        return data;
    }

//    public void drawClipScreen(Teletel t, int x1, int y1, int width, int length) {
//        byte data[] = convertToSemiGraph();
//       
//    }
//    public void drawScreen(Teletel t) {
//
//    }
    @Override
    public void clear() {

        for (int i = 0; i < screenGFX.length; i++) {
            screenGFX[i] = false;
        }
    }

    @Override
    public void drawToPage(Teletel t, int posx, int posy) throws IOException {
        byte data[] = convertToSemiGraph();
        //t.setCursorHome();

        t.setCursor(posx, posy);

        int wpage = widthScreen / 2;
        int hpage = heightScreen / 3;

        int maxWitdh = posx + wpage;
        if (maxWitdh > Teletel.PAGE_WIDTH) {
            maxWitdh = Teletel.PAGE_WIDTH;
        }

        int maxHeight = posy + hpage;
        if (maxHeight > Teletel.PAGE_HEIGHT) {
            maxHeight = Teletel.PAGE_HEIGHT;
        }

        System.out.println("PAGE_WIDTH=" + Teletel.PAGE_WIDTH + " PAGE_HEIGHT=" + Teletel.PAGE_HEIGHT + " maxWitdh=" + maxWitdh + " maxHeight=" + maxHeight);

        for (int j = posy; j < maxHeight; j++) {
            t.setCursor(posx, j);
            t.setMode(Teletel.MODE_SEMI_GRAPH);
            for (int i = posx; i < maxWitdh; i++) {
                t.getMterm().writeByte(data[wpage * (j - posy) + (i - posx)]);
            }
        }
    }

    @Override
    public void drawToPage(Teletel t) throws IOException {
        drawToPage(t, 0, 0);
    }

    @Override
    public int getNumberLine() {
        return (int) Math.ceil(heightScreen / 3);
    }

    public void inverseBitmap() {
        for (int i = 0; i < screenGFX.length; i++) {
            screenGFX[i]=!screenGFX[i];
            
        }
    }

    /**
     * Génère un QR Code et l'affiche dans le bitmap GraphTel
     * @param text Texte à encoder dans le QR Code
     * @param x Position X du QR Code dans le bitmap
     * @param y Position Y du QR Code dans le bitmap
     * @param scale Facteur d'échelle (1 = 1 pixel par module, 2 = 2x2 pixels par module, etc.)
     */
    public void generateQRCode(String text, int x, int y, int scale) {
        QRCodeGenerator qrGen = new QRCodeGenerator(1); // Version 1 (21x21)
        boolean[][] qrMatrix = qrGen.generateQRCode(text);
        
        drawQRMatrix(qrMatrix, x, y, scale);
        
        // Debug: afficher le QR Code en console
        System.out.println("QR Code généré pour: \"" + text + "\"");
        qrGen.printQRCode(qrMatrix);
    }
    
    /**
     * Génère un QR Code de test avec motif de démonstration
     * @param x Position X du QR Code dans le bitmap
     * @param y Position Y du QR Code dans le bitmap  
     * @param scale Facteur d'échelle
     */
    public void generateTestQRCode(int x, int y, int scale) {
        QRCodeGenerator qrGen = new QRCodeGenerator(1);
        boolean[][] qrMatrix = qrGen.generateTestPattern();
        
        drawQRMatrix(qrMatrix, x, y, scale);
        
        System.out.println("QR Code de test généré");
        qrGen.printQRCode(qrMatrix);
    }
    
    /**
     * Dessine une matrice QR Code dans le bitmap GraphTel
     * @param qrMatrix Matrice du QR Code (true = noir, false = blanc)
     * @param startX Position X de départ
     * @param startY Position Y de départ
     * @param scale Facteur d'échelle
     */
    private void drawQRMatrix(boolean[][] qrMatrix, int startX, int startY, int scale) {
        int qrSize = qrMatrix.length;
        int pixelsDrawn = 0;
        int pixelsSkipped = 0;
        
        // Sauvegarder l'état du pen
        boolean originalPen = pen;
        
        for (int qrY = 0; qrY < qrSize; qrY++) {
            for (int qrX = 0; qrX < qrSize; qrX++) {
                // Définir la couleur du pixel
                setPen(qrMatrix[qrY][qrX]);
                
                // Dessiner le module avec le facteur d'échelle
                for (int sy = 0; sy < scale; sy++) {
                    for (int sx = 0; sx < scale; sx++) {
                        int pixelX = startX + (qrX * scale) + sx;
                        int pixelY = startY + (qrY * scale) + sy; 
                        
                        // Vérifier si le pixel est dans les limites
                        if (pixelX >= 0 && pixelX < widthScreen && pixelY >= 0 && pixelY < heightScreen) {
                            setPixel(pixelX, pixelY);
                            pixelsDrawn++;
                        } else {
                            pixelsSkipped++;
                            if (pixelsSkipped <= 5) { // Limiter les messages
                                System.out.println("⚠️  Pixel hors limites: (" + pixelX + ", " + pixelY + ")");
                            }
                        }
                    }
                }
            }
        }
        
        // Restaurer l'état du pen
        setPen(originalPen);
        
        // Rapport de debug
        System.out.println("📊 Pixels dessinés: " + pixelsDrawn + ", ignorés: " + pixelsSkipped);
        if (pixelsSkipped > 0) {
            System.out.println("❌ ATTENTION: " + pixelsSkipped + " pixels perdus (QR Code tronqué)");
        }
    }
    
    /**
     * Génère un QR Code centré dans le bitmap
     * @param text Texte à encoder
     * @param scale Facteur d'échelle
     */
    public void generateCenteredQRCode(String text, int scale) {
        QRCodeGenerator qrGen = new QRCodeGenerator(1);
        boolean[][] qrMatrix = qrGen.generateQRCode(text);
        
        int qrSize = qrMatrix.length;
        int scaledSize = qrSize * scale;
        
        // Calculer la position pour centrer le QR Code
        int centerX = (widthScreen - scaledSize) / 2;
        int centerY = (heightScreen - scaledSize) / 2;
        
        drawQRMatrix(qrMatrix, centerX, centerY, scale);
        
        System.out.println("QR Code centré généré pour: \"" + text + "\" (taille: " + scaledSize + "x" + scaledSize + ")");
    }
    
    /**
     * Génère un motif visuel centré représentant un QR Code
     * Plus présentable que le QR Code technique
     * @param text Texte à représenter
     * @param scale Facteur d'échelle
     */
    public void generateCenteredVisualQR(String text, int scale) {
        QRCodeGenerator qrGen = new QRCodeGenerator(1);
        boolean[][] qrMatrix = qrGen.generateVisualPattern(text);
        
        int qrSize = qrMatrix.length;
        int scaledSize = qrSize * scale;
        
        // Calculer la position pour centrer
        int centerX = (widthScreen - scaledSize) / 2;
        int centerY = (heightScreen - scaledSize) / 2;
        
        drawQRMatrix(qrMatrix, centerX, centerY, scale);
        
        System.out.println("Motif visuel QR centré pour: \"" + text + "\" (taille: " + scaledSize + "x" + scaledSize + ")");
    }
    
    /**
     * Génère un QR Code amélioré (plus proche du standard, SANS ZXing)
     * Meilleure chance d'être scannable que la version basique
     * @param text Texte à encoder
     * @param scale Facteur d'échelle
     */
    public void generateCenteredImprovedQR(String text, int scale) {
        SimpleScannableQR improvedGen = new SimpleScannableQR();
        boolean[][] qrMatrix = improvedGen.generateImprovedQR(text);
        
        int qrSize = qrMatrix.length;
        int scaledSize = qrSize * scale;
        
        // DEBUG: Vérifier les dimensions
        System.out.println("🔍 DEBUG GraphTel:");
        System.out.println("   Screen: " + widthScreen + "x" + heightScreen + " pixels");
        System.out.println("   QR brut: " + qrSize + "x" + qrSize + " modules");
        System.out.println("   QR scalé: " + scaledSize + "x" + scaledSize + " pixels");
        
        // Vérifier si le QR Code rentre dans l'écran
        if (scaledSize > widthScreen || scaledSize > heightScreen) {
            System.out.println("⚠️  ATTENTION: QR Code trop grand pour l'écran !");
            System.out.println("   Réduisez l'échelle ou augmentez la résolution GraphTel");
            
            // Calculer l'échelle maximale
            int maxScale = Math.min(widthScreen / qrSize, heightScreen / qrSize);
            System.out.println("   Échelle max recommandée: " + maxScale);
            
            if (maxScale > 0) {
                scale = maxScale;
                scaledSize = qrSize * scale;
                System.out.println("   🔧 Auto-ajustement à l'échelle " + scale);
            }
        }
        
        // Calculer la position pour centrer
        int centerX = (widthScreen - scaledSize) / 2;
        int centerY = (heightScreen - scaledSize) / 2;
        
        System.out.println("   Position: (" + centerX + ", " + centerY + ")");
        System.out.println("   Zone QR: (" + centerX + ", " + centerY + ") à (" + (centerX + scaledSize - 1) + ", " + (centerY + scaledSize - 1) + ")");
        
        drawQRMatrix(qrMatrix, centerX, centerY, scale);
        
        System.out.println("✅ QR Code AMÉLIORÉ centré pour: \"" + text + "\" (taille: " + scaledSize + "x" + scaledSize + ")");
    }
    
    /**
     * Génère un QR Code SCANNABLE centré avec ZXing
     * Compatible avec les smartphones (iPhone, Android)
     * @param text Texte à encoder
     * @param scale Facteur d'échelle
     */
    public void generateCenteredScannableQR(String text, int scale) {
        try {
            ScannableQRGenerator scannableGen = new ScannableQRGenerator();
            boolean[][] qrMatrix = scannableGen.generateScannableQR(text, 21); // Version 1
            
            int qrSize = qrMatrix.length;
            int scaledSize = qrSize * scale;
            
            // DEBUG: Vérifier les dimensions
            System.out.println("🔍 DEBUG ZXing QR:");
            System.out.println("   Screen: " + widthScreen + "x" + heightScreen + " pixels");
            System.out.println("   QR ZXing: " + qrSize + "x" + qrSize + " modules");
            System.out.println("   QR scalé: " + scaledSize + "x" + scaledSize + " pixels");
            
            // Calculer la position pour centrer
            int centerX = (widthScreen - scaledSize) / 2;
            int centerY = (heightScreen - scaledSize) / 2;
            
            System.out.println("   Position: (" + centerX + ", " + centerY + ")");
            System.out.println("   Zone QR: (" + centerX + ", " + centerY + ") à (" + (centerX + scaledSize - 1) + ", " + (centerY + scaledSize - 1) + ")");
            
            drawQRMatrix(qrMatrix, centerX, centerY, scale);
            
            System.out.println("✅ QR Code ZXing SCANNABLE pour: \"" + text + "\" (taille: " + scaledSize + "x" + scaledSize + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur QR ZXing, fallback vers amélioré: " + e.getMessage());
            e.printStackTrace();
            // Fallback vers la version améliorée
            generateCenteredImprovedQR(text, scale);
        }
    }

}
