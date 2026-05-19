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
 * GraphTel - Gestionnaire de graphiques semi-graphiques pour Minitel
 * 
 * <h2>Description</h2>
 * Cette classe gère un buffer graphique en mémoire et le convertit en caractères
 * semi-graphiques Minitel. Le Minitel utilise des caractères spéciaux où chaque
 * caractère représente un bloc de 2×3 pixels (6 pixels par caractère).
 * 
 * <h2>Système de coordonnées</h2>
 * <ul>
 *   <li><b>Pixels</b> : Coordonnées internes (widthScreen × heightScreen)</li>
 *   <li><b>Caractères</b> : Coordonnées écran Minitel (40×25 max)</li>
 *   <li>Conversion : 1 caractère = 2 pixels en X, 3 pixels en Y</li>
 * </ul>
 * 
 * <h2>Palette de couleurs</h2>
 * Le Minitel supporte 8 couleurs :
 * <pre>
 * 0 = Noir    (BLACK)     4 = Bleu    (BLUE)
 * 1 = Rouge   (RED)       5 = Magenta (MAGENTA)
 * 2 = Vert    (GREEN)     6 = Cyan    (CYAN)
 * 3 = Jaune   (YELLOW)    7 = Blanc   (WHITE)
 * </pre>
 * 
 * <h2>Modes de conversion d'images</h2>
 * <ul>
 *   <li><b>Couleur</b> : Conversion vers les 8 couleurs Minitel</li>
 *   <li><b>Dithering</b> : Tramage Floyd-Steinberg pour meilleur rendu des dégradés</li>
 *   <li><b>Bitmap</b> : Noir et blanc uniquement (seuil de luminance)</li>
 * </ul>
 * 
 * <h2>Exemple d'utilisation</h2>
 * <pre>{@code
 * // Créer un GraphTel de 40×24 caractères (80×72 pixels)
 * GraphTel gfx = new GraphTel(80, 72);
 * 
 * // Dessiner des primitives
 * gfx.setPen(true);
 * gfx.setLine(0, 0, 79, 71);
 * gfx.setCircle(40, 36, 20);
 * 
 * // Charger une image
 * gfx.loadImage(new URL("http://example.com/image.png"), "dithering");
 * 
 * // Obtenir les bytes pour envoi au Minitel
 * byte[] data = gfx.getDrawToBytes(0, 0);
 * }</pre>
 * 
 * @author eddy
 * @see Teletel
 * @see GetTeletelCode
 */
public class GraphTel implements PageMinitel {

    /** Largeur par défaut en pixels (80 = 40 caractères × 2) */
    final public static int DEFAULT_SCREEN_WIDTH = Teletel.PAGE_WIDTH * 2;
    
    /** Hauteur par défaut en pixels (75 = 25 caractères × 3) */
    final public static int DEFAULT_SCREEN_HEIGHT = Teletel.PAGE_HEIGHT * 3;

    /** État du stylo (true = dessine, false = efface) */
    private boolean pen = true;

    /** Buffer des pixels (true = allumé, false = éteint) */
    private boolean screenGFX[];

    /** Buffer des couleurs par pixel (0-7) */
    private byte screenColor[];

    /** Flags de cellules modifiées depuis le dernier rendu différentiel (1 flag par bloc 2×3) */
    private boolean[] dirty;

    /** Largeur en pixels */
    private int widthScreen;

    /** Hauteur en pixels */
    private int heightScreen;

    /** Couleur d'encre courante (foreground) */
    private byte ink = GetTeletelCode.COLOR_WHITE;

    /** Couleur de fond courante (background) */
    private byte bgcolor = GetTeletelCode.COLOR_BLACK;

    /**
     * Constructeur avec dimensions personnalisées
     * @param w Largeur en pixels (sera arrondie au multiple de 2 supérieur)
     * @param h Hauteur en pixels (sera arrondie au multiple de 3 supérieur)
     */
    public GraphTel(int w, int h) {
        init(w, h);
    }

    /**
     * Constructeur par défaut (plein écran : 80×75 pixels = 40×25 caractères)
     */
    public GraphTel() {
        init(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
    }

    /**
     * Définit la couleur d'encre (foreground) pour les prochains dessins
     * @param color Code couleur Minitel (0-7)
     */
    public void setInk(byte color) {
        ink = color;
    }

    /**
     * Définit la couleur de fond (background) pour les prochains dessins
     * @param bgcolor Code couleur Minitel (0-7)
     */
    public void setBGColor(byte bgcolor) {
        this.bgcolor = bgcolor;
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
        dirty = new boolean[(widthScreen / 2) * (heightScreen / 3)];

        intColor();
    }

    private void intColor() {
        for (int i = 0; i < screenColor.length; i++) {
            screenColor[i] = (byte) (bgcolor << 8 | ink);
        }
    }

    /**
     * Définit l'état du stylo
     * @param color true = dessine (allume les pixels), false = efface (éteint les pixels)
     */
    public void setPen(boolean color) {
        pen = color;
    }

    // ========== PRIMITIVES DE DESSIN ==========
    
    /**
     * Trace une ligne entre deux points (algorithme de Bresenham)
     * @param x1 Coordonnée X du point de départ (en pixels)
     * @param y1 Coordonnée Y du point de départ (en pixels)
     * @param x2 Coordonnée X du point d'arrivée (en pixels)
     * @param y2 Coordonnée Y du point d'arrivée (en pixels)
     */
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

    /**
     * Trace un cercle (algorithme d'Andres)
     * @param x1 Coordonnée X du centre (en pixels)
     * @param y1 Coordonnée Y du centre (en pixels)
     * @param r Rayon du cercle (en pixels)
     */
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

    // ========== GESTION DES BITMAPS ==========
    
    /**
     * Écrit un bitmap (format byte array) dans le GraphTel
     * @param bitmap Tableau d'octets représentant le bitmap (1 bit par pixel, MSB first)
     */
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

    /**
     * Affiche un aperçu ASCII du bitmap dans la console (debug)
     */
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

    // ========== MANIPULATION DES PIXELS ==========
    
    /**
     * Allume ou éteint un pixel selon l'état du stylo (pen)
     * @param x Coordonnée X (en pixels)
     * @param y Coordonnée Y (en pixels)
     */
    public void setPixel(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            screenGFX[widthScreen * y + x] = pen;
            screenColor[widthScreen * y + x] = pen ? ink : bgcolor;
            dirty[(y / 3) * (widthScreen / 2) + (x / 2)] = true;
        }
    }

    /**
     * Retourne l'état d'un pixel
     * @param x Coordonnée X (en pixels)
     * @param y Coordonnée Y (en pixels)
     * @return true si allumé, false si éteint ou hors limites
     */
    public boolean getPixel(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            return screenGFX[widthScreen * y + x];
        }
        return false;
    }

    /**
     * Retourne la couleur d'un pixel
     * @param x Coordonnée X (en pixels)
     * @param y Coordonnée Y (en pixels)
     * @return Code couleur Minitel (0-7) ou 0 si hors limites
     */
    public byte getColor(int x, int y) {
        if ((x < widthScreen && x >= 0) && (y < heightScreen && y >= 0)) {
            return screenColor[widthScreen * y + x];
        }
        return 0;
    }

    /**
     * Retourne la couleur d'encre (foreground) pour un bloc semi-graphique (2x3 pixels)
     * La couleur d'encre est la couleur dominante parmi TOUS les pixels du bloc
     * @param charX Position X en caractères (0-39)
     * @param charY Position Y en caractères (0-24)
     * @return Couleur d'encre (0-7), blanc par défaut
     */
    public byte getBlockColor(int charX, int charY) {
        int pixelX = charX * 2;
        int pixelY = charY * 3;
        
        // Compter les occurrences de chaque couleur dans le bloc 2x3
        int[] colorCount = new int[8];
        
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                int px = pixelX + dx;
                int py = pixelY + dy;
                if (px < widthScreen && py < heightScreen) {
                    int index = widthScreen * py + px;
                    byte color = (byte) (screenColor[index] & 0x07);
                    colorCount[color]++;
                }
            }
        }
        
        // Trouver la couleur dominante (ignorer noir car c'est le fond)
        int maxCount = 0;
        byte dominantColor = GetTeletelCode.COLOR_WHITE;  // Blanc par défaut
        
        for (int c = 1; c < 8; c++) {  // Ignorer noir (0)
            if (colorCount[c] > maxCount) {
                maxCount = colorCount[c];
                dominantColor = (byte) c;
            }
        }
        
        return dominantColor;
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
            }

        }
        return data;
    }

    // ========== MÉTHODES D'AFFICHAGE ==========
    
    /**
     * Efface le buffer graphique (tous les pixels à false)
     */
    @Override
    public void clear() {
        for (int i = 0; i < screenGFX.length; i++) {
            screenGFX[i] = false;
            screenColor[i] = bgcolor;
        }
        java.util.Arrays.fill(dirty, true);
    }

    /**
     * Dessine le GraphTel sur un terminal Minitel à la position spécifiée
     * @param t Terminal Minitel
     * @param posx Position X en caractères (0-39)
     * @param posy Position Y en caractères (0-24)
     */
    @Override
    public void drawToPage(Teletel t, int posx, int posy) throws IOException {
        byte data[] = convertToSemiGraph();

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

    /**
     * Retourne les données graphiques sous forme de String (pour debug)
     * @param posx Position X en caractères
     * @param posy Position Y en caractères
     * @return String contenant les codes Vidéotex
     */
    public String getDrawToString(int posx, int posy) throws IOException {
        return new String(getDrawToBytes(posx, posy));
    }

    /**
     * Génère les codes Vidéotex pour afficher le GraphTel
     * <p>
     * Cette méthode convertit le buffer graphique en séquence d'octets
     * prête à être envoyée au Minitel. Elle gère :
     * <ul>
     *   <li>Le positionnement du curseur</li>
     *   <li>Les changements de couleur (encre et fond)</li>
     *   <li>La conversion en caractères semi-graphiques</li>
     * </ul>
     * 
     * @param posx Position X de départ en caractères (0-39)
     * @param posy Position Y de départ en caractères (0-24)
     * @return Tableau d'octets contenant les codes Vidéotex
     */
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

        byte lastFgColor = -1;  // Couleur d'encre précédente
        byte lastBgColor = -1;  // Couleur de fond précédente
        
        for (int j = posy; j < maxHeight; j++) {
            fulldraw.write(GetTeletelCode.setCursor(posx, j));
            fulldraw.write(GetTeletelCode.setMode(Teletel.MODE_SEMI_GRAPH));
            
            // Réinitialiser les couleurs à chaque ligne (le Minitel les reset)
            lastFgColor = -1;
            lastBgColor = -1;

            for (int i = posx; i < maxWitdh; i++) {
                // Coordonnées relatives dans le GraphTel (pas sur l'écran)
                int localX = i - posx;
                int localY = j - posy;
                
                // Récupérer les couleurs du bloc semi-graphique
                byte[] colors = getBlockColors(localX, localY);
                byte fgColor = colors[0];  // Couleur d'encre (pixels allumés)
                byte bgColor = colors[1];  // Couleur de fond (pixels éteints)
                
                // Changer la couleur de fond si différente
                if (bgColor != lastBgColor) {
                    fulldraw.write(GetTeletelCode.setBGColor(bgColor));
                    // Écrire un espace pour appliquer la couleur de fond
                    fulldraw.write(' ');
                    // Repositionner le curseur
                    fulldraw.write(GetTeletelCode.setCursor(i, j));
                    fulldraw.write(GetTeletelCode.setMode(Teletel.MODE_SEMI_GRAPH));
                    lastBgColor = bgColor;
                }
                
                // Changer la couleur d'encre si différente
                if (fgColor != lastFgColor) {
                    fulldraw.write(GetTeletelCode.setTextColor(fgColor));
                    lastFgColor = fgColor;
                }

                fulldraw.write(data[wpage * localY + localX]);
            }
        }
        fulldraw.write(GetTeletelCode.setMode(Teletel.MODE_TEXT));

        return fulldraw.toByteArray();
    }
    
    /**
     * Retourne les couleurs d'encre et de fond pour un bloc semi-graphique
     * @param charX Position X en caractères
     * @param charY Position Y en caractères
     * @return byte[2] : [0]=foreground (encre), [1]=background (fond)
     */
    private byte[] getBlockColors(int charX, int charY) {
        int pixelX = charX * 2;
        int pixelY = charY * 3;
        
        // Compter les couleurs des pixels allumés et éteints séparément
        int[] fgColorCount = new int[8];  // Couleurs des pixels allumés
        int[] bgColorCount = new int[8];  // Couleurs des pixels éteints
        
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                int px = pixelX + dx;
                int py = pixelY + dy;
                if (px < widthScreen && py < heightScreen) {
                    int index = widthScreen * py + px;
                    byte color = (byte) (screenColor[index] & 0x07);
                    
                    if (screenGFX[index]) {
                        // Pixel allumé -> compte pour foreground
                        fgColorCount[color]++;
                    } else {
                        // Pixel éteint -> compte pour background
                        bgColorCount[color]++;
                    }
                }
            }
        }
        
        // Trouver la couleur dominante pour l'encre (ignorer noir)
        int maxFg = 0;
        byte fgColor = GetTeletelCode.COLOR_WHITE;
        for (int c = 1; c < 8; c++) {
            if (fgColorCount[c] > maxFg) {
                maxFg = fgColorCount[c];
                fgColor = (byte) c;
            }
        }
        
        // Trouver la couleur dominante pour le fond (inclure noir)
        int maxBg = 0;
        byte bgColor = GetTeletelCode.COLOR_BLACK;
        for (int c = 0; c < 8; c++) {
            if (bgColorCount[c] > maxBg) {
                maxBg = bgColorCount[c];
                bgColor = (byte) c;
            }
        }
        
        return new byte[] { fgColor, bgColor };
    }

    @Override
    public void drawToPage(Teletel t) throws IOException {
        drawToPage(t, 0, 0);
    }

    @Override
    public int getNumberLine() {
        return (int) Math.ceil(heightScreen / 3);
    }

    /**
     * Inverse tous les pixels du buffer (négatif)
     */
    public void inverseBitmap() {
        for (int i = 0; i < screenGFX.length; i++) {
            screenGFX[i] = !screenGFX[i];
        }
        java.util.Arrays.fill(dirty, true);
    }

    /**
     * Écrit un bitmap booléen dans le GraphTel avec mise à l'échelle et centrage
     * @param mbits Tableau de bits carrés (true = pixel allumé, false = éteint)
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

    // ========== DIRTY TRACKING ==========

    /** Efface tous les flags dirty (appelé après un rendu complet) */
    public void clearDirty() {
        java.util.Arrays.fill(dirty, false);
    }

    /** Marque toutes les cellules dirty pour forcer un prochain full repaint */
    public void markAllDirty() {
        java.util.Arrays.fill(dirty, true);
    }

    /** @return true si au moins une cellule est dirty */
    public boolean hasDirty() {
        for (boolean d : dirty) {
            if (d) return true;
        }
        return false;
    }

    /**
     * Génère les codes Vidéotex uniquement pour les cellules modifiées depuis
     * le dernier appel. Efface les flags dirty après l'émission.
     *
     * @param posx Position X de départ en caractères
     * @param posy Position Y de départ en caractères
     * @return Bytes Vidéotex (vide si rien n'a changé)
     */
    public byte[] getDifferentialBytes(int posx, int posy) throws IOException {
        byte[] data = convertToSemiGraph();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int wpage = widthScreen / 2;
        int hpage = heightScreen / 3;
        int maxWidth  = Math.min(posx + wpage,  Teletel.PAGE_WIDTH);
        int maxHeight = Math.min(posy + hpage, Teletel.PAGE_HEIGHT);

        int lastCharX = -1, lastCharY = -1;
        byte lastFgColor = -1, lastBgColor = -1;
        boolean inSemiGraph = false;

        for (int j = posy; j < maxHeight; j++) {
            int localY = j - posy;
            for (int i = posx; i < maxWidth; i++) {
                int localX = i - posx;
                int cellIdx = localY * wpage + localX;

                if (!dirty[cellIdx]) continue;

                // Repositionnement si non-adjacent
                if (lastCharY != j || lastCharX != i - 1) {
                    if (inSemiGraph) {
                        out.write(GetTeletelCode.setMode(Teletel.MODE_TEXT));
                        inSemiGraph = false;
                    }
                    out.write(GetTeletelCode.setCursor(i, j));
                    lastFgColor = -1;
                    lastBgColor = -1;
                }

                byte[] colors = getBlockColors(localX, localY);
                byte fgColor = colors[0];
                byte bgColor = colors[1];

                // Changement de couleur de fond (nécessite sortie du mode semi-graph)
                if (bgColor != lastBgColor) {
                    if (inSemiGraph) {
                        out.write(GetTeletelCode.setMode(Teletel.MODE_TEXT));
                        inSemiGraph = false;
                    }
                    out.write(GetTeletelCode.setBGColor(bgColor));
                    out.write(' ');
                    out.write(GetTeletelCode.setCursor(i, j));
                    lastBgColor = bgColor;
                    lastFgColor = -1;
                }

                if (!inSemiGraph) {
                    out.write(GetTeletelCode.setMode(Teletel.MODE_SEMI_GRAPH));
                    inSemiGraph = true;
                }

                if (fgColor != lastFgColor) {
                    out.write(GetTeletelCode.setTextColor(fgColor));
                    lastFgColor = fgColor;
                }

                out.write(data[cellIdx]);
                dirty[cellIdx] = false;

                lastCharX = i;
                lastCharY = j;
            }
        }

        if (inSemiGraph) {
            out.write(GetTeletelCode.setMode(Teletel.MODE_TEXT));
        }

        return out.toByteArray();
    }

    // ========== NOUVELLES PRIMITIVES DE DESSIN ==========

    /**
     * Dessine le contour d'un rectangle (pixels)
     * @param x Coin supérieur gauche X
     * @param y Coin supérieur gauche Y
     * @param w Largeur en pixels
     * @param h Hauteur en pixels
     */
    public void drawRect(int x, int y, int w, int h) {
        for (int i = x; i <= x + w; i++) {
            setPixel(i, y);
            setPixel(i, y + h);
        }
        for (int j = y + 1; j < y + h; j++) {
            setPixel(x, j);
            setPixel(x + w, j);
        }
    }

    /**
     * Dessine un rectangle plein (pixels)
     * @param x Coin supérieur gauche X
     * @param y Coin supérieur gauche Y
     * @param w Largeur en pixels
     * @param h Hauteur en pixels
     */
    public void fillRect(int x, int y, int w, int h) {
        for (int j = y; j <= y + h; j++) {
            for (int i = x; i <= x + w; i++) {
                setPixel(i, j);
            }
        }
    }

    /**
     * Dessine un cercle plein (pixels)
     * @param cx Centre X
     * @param cy Centre Y
     * @param r  Rayon en pixels
     */
    public void fillCircle(int cx, int cy, int r) {
        for (int dy = -r; dy <= r; dy++) {
            int halfWidth = (int) Math.sqrt((double) r * r - (double) dy * dy);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                setPixel(cx + dx, cy + dy);
            }
        }
    }

    // ========== GETTERS ==========

    /** @return Largeur du buffer en pixels */
    public int getWidthScreen() {
        return widthScreen;
    }

    /** @return Hauteur du buffer en pixels */
    public int getHeightScreen() {
        return heightScreen;
    }

    /** @return État actuel du stylo */
    public boolean getPen() {
        return pen;
    }

    // ========== CHARGEMENT D'IMAGES ==========
    /**
     * Palette des 8 couleurs Minitel
     */
    private static final int[][] MINITEL_PALETTE = {
        {0, 0, 0}, // 0 = BLACK
        {255, 0, 0}, // 1 = RED
        {0, 255, 0}, // 2 = GREEN
        {255, 255, 0}, // 3 = YELLOW
        {0, 0, 255}, // 4 = BLUE
        {255, 0, 255}, // 5 = MAGENTA
        {0, 255, 255}, // 6 = CYAN
        {255, 255, 255} // 7 = WHITE
    };

    /**
     * Charge une image depuis un fichier et la convertit en semi-graphique
     * Minitel
     *
     * @param file Fichier image (PNG, JPEG, etc.)
     */
    public void loadImage(File file) throws IOException {
        loadImage(file, false);
    }
    
    public void loadImage(File file, boolean dithering) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Impossible de lire l'image: " + file.getPath());
        }
        if (dithering) {
            convertImageWithDithering(img);
        } else {
            convertImage(img);
        }
    }

    /**
     * Charge une image depuis une URL et la convertit en semi-graphique Minitel
     *
     * @param url URL de l'image
     */
    public void loadImage(URL url) throws IOException {
        loadImage(url, (String) null);
    }
    
    public void loadImage(URL url, boolean dithering) throws IOException {
        loadImage(url, dithering ? "dithering" : null);
    }
    
    public void loadImage(URL url, String style) throws IOException {
        BufferedImage img = loadImageFromUrl(url);
        if (img == null) {
            throw new IOException("Impossible de lire l'image: " + url);
        }
        convertImageWithStyle(img, style);
    }
    
    /**
     * Charge une image depuis une URL avec des headers HTTP simulant un navigateur.
     * Cela permet de contourner les protections hotlinking de certains sites.
     * 
     * @param url URL de l'image
     * @return BufferedImage ou null si échec
     */
    private BufferedImage loadImageFromUrl(URL url) throws IOException {
        java.net.URLConnection conn = url.openConnection();
        
        // Simuler un navigateur pour éviter les blocages hotlinking
        conn.setRequestProperty("User-Agent", 
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", 
            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setRequestProperty("Referer", url.getProtocol() + "://" + url.getHost() + "/");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        
        try (InputStream is = conn.getInputStream()) {
            return ImageIO.read(is);
        }
    }

    /**
     * Charge une image depuis un InputStream et la convertit en semi-graphique
     * Minitel
     *
     * @param is InputStream de l'image
     */
    public void loadImage(InputStream is) throws IOException {
        loadImage(is, false);
    }
    
    public void loadImage(InputStream is, boolean dithering) throws IOException {
        BufferedImage img = ImageIO.read(is);
        if (img == null) {
            throw new IOException("Impossible de lire l'image depuis le stream");
        }
        if (dithering) {
            convertImageWithDithering(img);
        } else {
            convertImage(img);
        }
    }

    /**
     * Convertit une BufferedImage selon le style spécifié
     * @param img Image source
     * @param style "dithering", "bitmap", ou null (couleur par défaut)
     */
    private void convertImageWithStyle(BufferedImage img, String style) {
        if ("dithering".equalsIgnoreCase(style)) {
            convertImageWithDithering(img);
        } else if ("bitmap".equalsIgnoreCase(style)) {
            convertImageBW(img);
        } else {
            convertImage(img);
        }
    }

    /**
     * Convertit une BufferedImage en pixels Minitel
     * L'image est étirée pour remplir tout le GraphTel
     *
     * @param img Image source
     */
    public void convertImage(BufferedImage img) {
        System.out.println("🖼️ Conversion image " + img.getWidth() + "x" + img.getHeight()
                + " -> GraphTel " + widthScreen + "x" + heightScreen);

        // Effacer l'écran
        clear();

        // Calculer les ratios pour étirer l'image (pas de conservation du ratio)
        double scaleX = (double) img.getWidth() / widthScreen;
        double scaleY = (double) img.getHeight() / heightScreen;

        System.out.println("   Échelle X: " + String.format("%.2f", scaleX)
                + ", Échelle Y: " + String.format("%.2f", scaleY));

        // Parcourir chaque pixel de l'écran GraphTel
        for (int screenY = 0; screenY < heightScreen; screenY++) {
            for (int screenX = 0; screenX < widthScreen; screenX++) {
                // Calculer la position correspondante dans l'image source
                int imgX = (int) (screenX * scaleX);
                int imgY = (int) (screenY * scaleY);

                // Limiter aux bornes de l'image
                imgX = Math.min(imgX, img.getWidth() - 1);
                imgY = Math.min(imgY, img.getHeight() - 1);

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
                screenColor[index] = minitelColor;
            }
        }

        System.out.println("✅ Image convertie");
    }

    /**
     * Convertit une BufferedImage en pixels Minitel avec dithering Floyd-Steinberg
     * L'image est étirée pour remplir tout le GraphTel
     *
     * @param img Image source
     */
    public void convertImageWithDithering(BufferedImage img) {
        System.out.println("🖼️ Conversion image avec dithering " + img.getWidth() + "x" + img.getHeight()
                + " -> GraphTel " + widthScreen + "x" + heightScreen);

        // Effacer l'écran
        clear();

        // Calculer les ratios pour étirer l'image
        double scaleX = (double) img.getWidth() / widthScreen;
        double scaleY = (double) img.getHeight() / heightScreen;

        // Créer un buffer pour les erreurs de quantification (float pour précision)
        float[][] errorR = new float[heightScreen][widthScreen];
        float[][] errorG = new float[heightScreen][widthScreen];
        float[][] errorB = new float[heightScreen][widthScreen];

        // Pré-calculer les couleurs de l'image redimensionnée
        int[][] imgColors = new int[heightScreen][widthScreen];
        for (int screenY = 0; screenY < heightScreen; screenY++) {
            for (int screenX = 0; screenX < widthScreen; screenX++) {
                int imgX = Math.min((int) (screenX * scaleX), img.getWidth() - 1);
                int imgY = Math.min((int) (screenY * scaleY), img.getHeight() - 1);
                imgColors[screenY][screenX] = img.getRGB(imgX, imgY);
            }
        }

        // Appliquer Floyd-Steinberg dithering
        for (int screenY = 0; screenY < heightScreen; screenY++) {
            for (int screenX = 0; screenX < widthScreen; screenX++) {
                int rgb = imgColors[screenY][screenX];
                
                // Couleur originale + erreur accumulée
                float oldR = ((rgb >> 16) & 0xFF) + errorR[screenY][screenX];
                float oldG = ((rgb >> 8) & 0xFF) + errorG[screenY][screenX];
                float oldB = (rgb & 0xFF) + errorB[screenY][screenX];
                
                // Limiter aux bornes 0-255
                oldR = Math.max(0, Math.min(255, oldR));
                oldG = Math.max(0, Math.min(255, oldG));
                oldB = Math.max(0, Math.min(255, oldB));

                // Trouver la couleur Minitel la plus proche
                byte minitelColor = findClosestMinitelColor((int) oldR, (int) oldG, (int) oldB);
                
                // Couleur Minitel choisie
                int newR = MINITEL_PALETTE[minitelColor][0];
                int newG = MINITEL_PALETTE[minitelColor][1];
                int newB = MINITEL_PALETTE[minitelColor][2];

                // Calculer l'erreur de quantification
                float errR = oldR - newR;
                float errG = oldG - newG;
                float errB = oldB - newB;

                // Distribuer l'erreur aux pixels voisins (Floyd-Steinberg)
                // Pixel à droite: 7/16
                if (screenX + 1 < widthScreen) {
                    errorR[screenY][screenX + 1] += errR * 7 / 16;
                    errorG[screenY][screenX + 1] += errG * 7 / 16;
                    errorB[screenY][screenX + 1] += errB * 7 / 16;
                }
                // Pixel en bas à gauche: 3/16
                if (screenY + 1 < heightScreen && screenX - 1 >= 0) {
                    errorR[screenY + 1][screenX - 1] += errR * 3 / 16;
                    errorG[screenY + 1][screenX - 1] += errG * 3 / 16;
                    errorB[screenY + 1][screenX - 1] += errB * 3 / 16;
                }
                // Pixel en bas: 5/16
                if (screenY + 1 < heightScreen) {
                    errorR[screenY + 1][screenX] += errR * 5 / 16;
                    errorG[screenY + 1][screenX] += errG * 5 / 16;
                    errorB[screenY + 1][screenX] += errB * 5 / 16;
                }
                // Pixel en bas à droite: 1/16
                if (screenY + 1 < heightScreen && screenX + 1 < widthScreen) {
                    errorR[screenY + 1][screenX + 1] += errR * 1 / 16;
                    errorG[screenY + 1][screenX + 1] += errG * 1 / 16;
                    errorB[screenY + 1][screenX + 1] += errB * 1 / 16;
                }

                // Déterminer si le pixel est "allumé" (pas noir)
                boolean pixelOn = (minitelColor != GetTeletelCode.COLOR_BLACK);

                // Stocker le pixel
                int index = widthScreen * screenY + screenX;
                screenGFX[index] = pixelOn;
                screenColor[index] = minitelColor;
            }
        }

        System.out.println("✅ Image convertie avec dithering");
    }

    /**
     * Trouve la couleur Minitel la plus proche d'une couleur RGB Utilise la
     * distance euclidienne dans l'espace RGB
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
     *
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
     * L'image est étirée pour remplir tout le GraphTel
     *
     * @param img Image source
     */
    public void convertImageBW(BufferedImage img) {
        System.out.println("🖼️ Conversion image N&B " + img.getWidth() + "x" + img.getHeight()
                + " -> GraphTel " + widthScreen + "x" + heightScreen);

        clear();

        // Calculer les ratios pour étirer l'image (pas de conservation du ratio)
        double scaleX = (double) img.getWidth() / widthScreen;
        double scaleY = (double) img.getHeight() / heightScreen;

        System.out.println("   Échelle X: " + String.format("%.2f", scaleX)
                + ", Échelle Y: " + String.format("%.2f", scaleY));

        for (int screenY = 0; screenY < heightScreen; screenY++) {
            for (int screenX = 0; screenX < widthScreen; screenX++) {
                int imgX = (int) (screenX * scaleX);
                int imgY = (int) (screenY * scaleY);

                // Limiter aux bornes de l'image
                imgX = Math.min(imgX, img.getWidth() - 1);
                imgY = Math.min(imgY, img.getHeight() - 1);

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
            }
        }

        System.out.println("✅ Image N&B convertie");
    }

}
