/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.somanybits.minitel.components;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.Teletel;

/**
 *
 * @author eddy
 */
public class GraphTel implements PageMinitel {

    final public static int DEFAULT_SCREEN_WIDTH = Teletel.PAGE_WIDTH * 2;
    final public static int DEFAULT_SCREEN_HEIGHT = Teletel.PAGE_HEIGHT * 3;
    // private byte pen=Teletel.COLOR_WHITE;

    private boolean pen = true;
    private boolean screenGFX[];

    private byte screenColor[];
    private int widthScreen;
    private int heightScreen;
    private byte ink = GetTeletelCode.COLOR_WHITE;
    private byte bgcolor = GetTeletelCode.COLOR_BLACK;

    public GraphTel(int w, int h) {
        init(w, h);

    }

    public GraphTel() {
        init(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
    }

    public void setInk(byte color) {
        ink = color;
    }

    public void setBGColor(byte bgcolor) {
        bgcolor = bgcolor;
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

        intColor();
    }

    private void intColor() {
        for (int i = 0; i < screenColor.length; i++) {
            screenColor[i] = (byte) (bgcolor << 8 | ink);
        }
    }

    public void setPen(boolean color) {
        pen = color;
    }

    public void setLine(int x1, int y1, int x2, int y2) {
        // algo built with this link http://fvirtman.free.fr/recueil/02_03_03_line.c.php
        // int dx = (x2 - x1);
        // int dy = (y2 - y1);
        //
        // for (int x = x1; x < x2; x++) {
        // int y = y1 + dy * (x - x1) / dx;
        // setPixel(x, y);
        // }
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
        // Algo built with this link
        // https://www.wikiwand.com/fr/Algorithme_de_trac%C3%A9_de_cercle_d'Andres
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
        // Calculer les dimensions réelles du bitmap à partir de sa taille
        int bytesPerRow = (widthScreen + 7) >> 3;
        int bitmapHeight = bitmap.length / bytesPerRow;

        // Limiter aux dimensions réelles du bitmap
        int maxHeight = Math.min(heightScreen, bitmapHeight);
        int maxWidth = widthScreen;

        for (int j = 0; j < maxHeight; j++) {
            for (int i = 0; i < maxWidth; i++) {

                int index = (j * bytesPerRow) + (i >> 3);

                // Vérifier que l'index est valide
                if (index >= bitmap.length) {
                    continue;
                }

                int bytemap = bitmap[index] & 0xFF;
                int mask = ((0x80) >> ((i % 8))) & 0xFF;

                if ((bytemap & mask) != 0) {
                    setPixel(i, j);
                }
            }
        }
        debugAscii(bitmap, maxWidth, maxHeight);
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
            screenColor[widthScreen * y + x] = (byte) (bgcolor << 8 | ink);

        }

    }

    public boolean getPixel(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            return screenGFX[widthScreen * y + x];
        }
        return false;
    }

    public byte getColor(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            return screenColor[widthScreen * y + x];
        }
        return 0;
    }

    private byte[] convertToSemiGraph() {
        byte[] data = new byte[(widthScreen / 2) * (heightScreen / 3)];

        int car = 0;
        for (int j = 0; j < heightScreen; j += 3) {

            for (int i = 0; i < widthScreen; i += 2) {
                byte semigfx = 0;
                // line 0
                if (getPixel(i, j)) {
                    semigfx |= 0b0100001;
                }
                if (getPixel(i + 1, j)) {
                    semigfx |= 0b0100010;
                }
                // line 1
                if (getPixel(i, j + 1)) {
                    semigfx |= 0b0100100;
                }
                if (getPixel(i + 1, j + 1)) {
                    semigfx |= 0b0101000;
                }
                // line 2
                if (getPixel(i, j + 2)) {
                    semigfx |= 0b0110000;
                }
                if (getPixel(i + 1, j + 2)) {
                    semigfx |= 0b1100000;
                }
                // full (exception)
                if (semigfx == 0b1111111) {
                    semigfx = 0b1011111;
                } else if (semigfx == 0) {
                    semigfx = 0x20;
                }

                data[car++] = (byte) semigfx;
                // System.out.println(i + ":" + j + "-" + car);
            }

        }
        return data;
    }

    // public void drawClipScreen(Teletel t, int x1, int y1, int width, int length)
    // {
    // byte data[] = convertToSemiGraph();
    //
    // }
    // public void drawScreen(Teletel t) {
    //
    // }
    @Override
    public void clear() {

        for (int i = 0; i < screenGFX.length; i++) {
            screenGFX[i] = false;
        }
    }

    @Override
    public void drawToPage(Teletel t, int posx, int posy) throws IOException {
        byte data[] = convertToSemiGraph();
        // t.setCursorHome();

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

        System.out.println("PAGE_WIDTH=" + Teletel.PAGE_WIDTH + " PAGE_HEIGHT=" + Teletel.PAGE_HEIGHT + " maxWitdh="
                + maxWitdh + " maxHeight=" + maxHeight);

        for (int j = posy; j < maxHeight; j++) {
            t.setCursor(posx, j);
            t.setMode(Teletel.MODE_SEMI_GRAPH);
            for (int i = posx; i < maxWitdh; i++) {
                t.getMterm().writeByte(data[wpage * (j - posy) + (i - posx)]);
            }
        }
        t.setMode(Teletel.MODE_TEXT);
    }

    public String getDrawToString(int posx, int posy) throws IOException {
        return new String(getDrawToBytes(posx, posy));
    }

    public byte[] getDrawToBytes(int posx, int posy) throws IOException {
        byte data[] = convertToSemiGraph();

        ByteArrayOutputStream fulldraw = new ByteArrayOutputStream();

        fulldraw.write(GetTeletelCode.setCursor(posx, posy));

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

        System.out.println("PAGE_WIDTH=" + Teletel.PAGE_WIDTH + " PAGE_HEIGHT=" + Teletel.PAGE_HEIGHT + " maxWitdh="
                + maxWitdh + " maxHeight=" + maxHeight);

        for (int j = posy; j < maxHeight; j++) {
            fulldraw.write(GetTeletelCode.setCursor(posx, j));
            fulldraw.write(GetTeletelCode.setMode(Teletel.MODE_SEMI_GRAPH));

            byte currentcolor = GetTeletelCode.COLOR_BLACK;
            byte curreentbgcolor = currentcolor;
            for (int i = posx; i < maxWitdh; i++) {

                currentcolor = (byte) (getColor(i, j) & 0x0F);
                if (ink != currentcolor) {
                    ink = currentcolor;
                    fulldraw.write(GetTeletelCode.setTextColor(ink));
                }
                curreentbgcolor = (byte) ((getColor(i, j) >> 8) & 0x0F);
                if (bgcolor != curreentbgcolor) {
                    bgcolor = curreentbgcolor;
                    fulldraw.write(GetTeletelCode.setBGColor(bgcolor));
                }

                fulldraw.write(data[wpage * (j - posy) + (i - posx)]);
            }
        }
        fulldraw.write(GetTeletelCode.setMode(Teletel.MODE_TEXT));

        return fulldraw.toByteArray();
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
            screenGFX[i] = !screenGFX[i];

        }
    }

    /**
     * Écrit un bitmap dans le GraphTel
     *
     * @param mbits Tableau de bits (true = noir, false = blanc)
     */
    public void writeBitmap(boolean[] mbits) {
        if (mbits == null) {
            System.err.println("❌ Bitmap null fourni à writeBitmap");
            return;
        }

        // Calculer les dimensions du bitmap source
        int bitmapSize = (int) Math.sqrt(mbits.length);
        if (bitmapSize * bitmapSize != mbits.length) {
            System.err.println("❌ Bitmap doit être carré. Taille: " + mbits.length);
            return;
        }

        System.out.println("📝 Écriture bitmap " + bitmapSize + "x" + bitmapSize + " dans GraphTel " + widthScreen + "x"
                + heightScreen);

        // Effacer l'écran
        clear();

        // Calculer le facteur d'échelle pour centrer
        int scaleX = widthScreen / bitmapSize;
        int scaleY = heightScreen / bitmapSize;
        int scale = Math.min(scaleX, scaleY);

        if (scale < 1) {
            scale = 1;
        }

        // Calculer la position de centrage
        int scaledWidth = bitmapSize * scale;
        int scaledHeight = bitmapSize * scale;
        int offsetX = (widthScreen - scaledWidth) / 2;
        int offsetY = (heightScreen - scaledHeight) / 2;

        System.out.println("   Échelle: " + scale + "x, Position: (" + offsetX + ", " + offsetY + ")");

        // Sauvegarder l'état du pen
        boolean originalPen = pen;

        // Copier le bitmap avec mise à l'échelle
        for (int y = 0; y < bitmapSize; y++) {
            for (int x = 0; x < bitmapSize; x++) {
                boolean pixelValue = mbits[y * bitmapSize + x];
                setPen(pixelValue);

                // Dessiner le pixel avec facteur d'échelle
                for (int sy = 0; sy < scale; sy++) {
                    for (int sx = 0; sx < scale; sx++) {
                        int screenX = offsetX + (x * scale) + sx;
                        int screenY = offsetY + (y * scale) + sy;

                        if (screenX >= 0 && screenX < widthScreen
                                && screenY >= 0 && screenY < heightScreen) {
                            setPixel(screenX, screenY);
                        }
                    }
                }
            }
        }

        // Restaurer l'état du pen
        setPen(originalPen);

        System.out.println("✅ Bitmap écrit avec succès");
    }

    // Getters pour QRCodeDisplay
    public int getWidthScreen() {
        return widthScreen;
    }

    public int getHeightScreen() {
        return heightScreen;
    }

    public boolean getPen() {
        return pen;
    }

    // ========== CHARGEMENT D'IMAGES ==========

    /**
     * Palette des 8 couleurs Minitel
     */
    private static final int[][] MINITEL_PALETTE = {
        {0, 0, 0},       // 0 = BLACK
        {255, 0, 0},     // 1 = RED
        {0, 255, 0},     // 2 = GREEN
        {255, 255, 0},   // 3 = YELLOW
        {0, 0, 255},     // 4 = BLUE
        {255, 0, 255},   // 5 = MAGENTA
        {0, 255, 255},   // 6 = CYAN
        {255, 255, 255}  // 7 = WHITE
    };

    /**
     * Charge une image depuis un fichier et la convertit en semi-graphique Minitel
     * @param file Fichier image (PNG, JPEG, etc.)
     */
    public void loadImage(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Impossible de lire l'image: " + file.getPath());
        }
        convertImage(img);
    }

    /**
     * Charge une image depuis une URL et la convertit en semi-graphique Minitel
     * @param url URL de l'image
     */
    public void loadImage(URL url) throws IOException {
        BufferedImage img = ImageIO.read(url);
        if (img == null) {
            throw new IOException("Impossible de lire l'image: " + url);
        }
        convertImage(img);
    }

    /**
     * Charge une image depuis un InputStream et la convertit en semi-graphique Minitel
     * @param is InputStream de l'image
     */
    public void loadImage(InputStream is) throws IOException {
        BufferedImage img = ImageIO.read(is);
        if (img == null) {
            throw new IOException("Impossible de lire l'image depuis le stream");
        }
        convertImage(img);
    }

    /**
     * Convertit une BufferedImage en pixels Minitel
     * @param img Image source
     */
    public void convertImage(BufferedImage img) {
        System.out.println("🖼️ Conversion image " + img.getWidth() + "x" + img.getHeight() + 
                          " -> GraphTel " + widthScreen + "x" + heightScreen);

        // Effacer l'écran
        clear();

        // Calculer le ratio pour adapter l'image
        double scaleX = (double) widthScreen / img.getWidth();
        double scaleY = (double) heightScreen / img.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (img.getWidth() * scale);
        int scaledHeight = (int) (img.getHeight() * scale);
        int offsetX = (widthScreen - scaledWidth) / 2;
        int offsetY = (heightScreen - scaledHeight) / 2;

        System.out.println("   Échelle: " + String.format("%.2f", scale) + 
                          ", Taille: " + scaledWidth + "x" + scaledHeight +
                          ", Offset: (" + offsetX + ", " + offsetY + ")");

        // Parcourir chaque pixel de l'écran GraphTel
        for (int screenY = 0; screenY < heightScreen; screenY++) {
            for (int screenX = 0; screenX < widthScreen; screenX++) {
                // Calculer la position correspondante dans l'image source
                int imgX = (int) ((screenX - offsetX) / scale);
                int imgY = (int) ((screenY - offsetY) / scale);

                // Vérifier si on est dans les limites de l'image
                if (imgX >= 0 && imgX < img.getWidth() && imgY >= 0 && imgY < img.getHeight()) {
                    int rgb = img.getRGB(imgX, imgY);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Trouver la couleur Minitel la plus proche
                    byte minitelColor = findClosestMinitelColor(r, g, b);

                    // Déterminer si le pixel est "allumé" (pas noir)
                    boolean pixelOn = (minitelColor != GetTeletelCode.COLOR_BLACK);

                    // Stocker le pixel
                    int index = widthScreen * screenY + screenX;
                    screenGFX[index] = pixelOn;
                    // Pour l'instant, on stocke juste la couleur du pixel (ink)
                    // Le fond sera géré lors du rendu
                    screenColor[index] = minitelColor;
                } else {
                    // Hors de l'image = noir
                    int index = widthScreen * screenY + screenX;
                    screenGFX[index] = false;
                    screenColor[index] = GetTeletelCode.COLOR_BLACK;
                }
            }
        }

        System.out.println("✅ Image convertie");
    }

    /**
     * Trouve la couleur Minitel la plus proche d'une couleur RGB
     * Utilise la distance euclidienne dans l'espace RGB
     */
    private byte findClosestMinitelColor(int r, int g, int b) {
        int bestColor = 0;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < MINITEL_PALETTE.length; i++) {
            int dr = r - MINITEL_PALETTE[i][0];
            int dg = g - MINITEL_PALETTE[i][1];
            int db = b - MINITEL_PALETTE[i][2];
            int distance = dr * dr + dg * dg + db * db;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestColor = i;
            }
        }

        return (byte) bestColor;
    }

    /**
     * Charge une image en noir et blanc (sans couleurs)
     * @param file Fichier image
     */
    public void loadImageBW(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Impossible de lire l'image: " + file.getPath());
        }
        convertImageBW(img);
    }

    /**
     * Convertit une image en noir et blanc
     * @param img Image source
     */
    public void convertImageBW(BufferedImage img) {
        System.out.println("🖼️ Conversion image N&B " + img.getWidth() + "x" + img.getHeight() + 
                          " -> GraphTel " + widthScreen + "x" + heightScreen);

        clear();

        double scaleX = (double) widthScreen / img.getWidth();
        double scaleY = (double) heightScreen / img.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (img.getWidth() * scale);
        int scaledHeight = (int) (img.getHeight() * scale);
        int offsetX = (widthScreen - scaledWidth) / 2;
        int offsetY = (heightScreen - scaledHeight) / 2;

        for (int screenY = 0; screenY < heightScreen; screenY++) {
            for (int screenX = 0; screenX < widthScreen; screenX++) {
                int imgX = (int) ((screenX - offsetX) / scale);
                int imgY = (int) ((screenY - offsetY) / scale);

                if (imgX >= 0 && imgX < img.getWidth() && imgY >= 0 && imgY < img.getHeight()) {
                    int rgb = img.getRGB(imgX, imgY);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Luminance (formule standard)
                    int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                    // Seuil à 128 pour noir/blanc
                    boolean pixelOn = (luminance > 128);

                    int index = widthScreen * screenY + screenX;
                    screenGFX[index] = pixelOn;
                    screenColor[index] = (byte) (pixelOn ? GetTeletelCode.COLOR_WHITE : GetTeletelCode.COLOR_BLACK);
                } else {
                    int index = widthScreen * screenY + screenX;
                    screenGFX[index] = false;
                    screenColor[index] = GetTeletelCode.COLOR_BLACK;
                }
            }
        }

        System.out.println("✅ Image N&B convertie");
    }

}
