package org.somanybits.minitel.hardware;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

/**
 * Pilote SSD1306 OLED 128x64 via I2C (Pi4J v2 / linuxfs).
 *
 * Pi4J v2 avec le plugin linuxfs accède directement à /dev/i2c-1
 * SANS dépendance WiringPi — compatible avec toutes les versions de Raspberry Pi OS.
 *
 * Branchement RPi : GPIO2=SDA (pin 3), GPIO3=SCL (pin 5)  →  /dev/i2c-1
 *
 * Syntaxe VTML côté serveur (jusqu'à 8 lignes, 21 caractères chacune) :
 * <pre>
 *   &lt;oled line1="M-Kiwi" line2="Score: 100" line3="Joueur: Eddy" /&gt;
 * </pre>
 *
 * Graceful degradation : si Pi4J ou l'I2C ne sont pas disponibles,
 * {@link #isAvailable()} retourne false et aucune exception n'est propagée.
 */
public class OLEDDisplay  {

    public static final int DEFAULT_BUS     = 1;       // /dev/i2c-1
    public static final int DEFAULT_ADDRESS = 0x3C;    // SSD1306 adresse courante

    private static final int WIDTH  = 128;
    private static final int HEIGHT = 64;
    private static final int PAGES  = HEIGHT / 8;      // 8 pages de 8 pixels

    private static final int FONT_WIDTH  = 6;           // 5px glyphe + 1px espace
    public static final int  CHARS_PER_LINE = WIDTH / FONT_WIDTH;   // 21
    public static final int  MAX_LINES      = PAGES;                 // 8

    public static final int  CHAR_SIZE_8X8     = 8;
    public static final int  CHARS_PER_LINE_8X8 = WIDTH / CHAR_SIZE_8X8; // 16

    private final byte[] buffer = new byte[WIDTH * PAGES];  // 1024 octets

    // Mode headless requis pour Graphics2D sur Raspberry Pi sans écran
    static { System.setProperty("java.awt.headless", "true"); }

    private Context pi4j;
    private I2C i2c;
    private boolean available = false;

    private final int busNumber;
    private final int address;

    public OLEDDisplay() {
        this(DEFAULT_BUS, DEFAULT_ADDRESS);
    }

    public OLEDDisplay(int busNumber, int address) {
        this.busNumber = busNumber;
        this.address   = address;
    }

    /**
     * Initialise Pi4J v2 et le SSD1306. Retourne true si l'écran est prêt.
     * Ne lève pas d'exception — log l'erreur et retourne false si indisponible.
     */
    public boolean init() {
        try {
            pi4j = Pi4J.newAutoContext();
            I2CConfig config = I2C.newConfigBuilder(pi4j)
                    .id("ssd1306-oled")
                    .bus(busNumber)
                    .device(address)
                    .build();
            i2c = pi4j.create(config);
            initSSD1306();
            clear();
            flush();
            available = true;
            System.out.println("OLED SSD1306 initialisé — I2C bus "
                    + busNumber + ", adresse 0x" + Integer.toHexString(address));
        } catch (Exception | Error e) {
            System.out.println("OLED non disponible ("
                    + e.getClass().getSimpleName() + "): " + e.getMessage());
            available = false;
            if (pi4j != null) {
                try { pi4j.shutdown(); } catch (Exception ignored) {}
                pi4j = null;
            }
        }
        return available;
    }

    public boolean isAvailable() { return available; }

    // ── Envoi commandes SSD1306 ───────────────────────────────────────────────

    private void cmd(int b) {
        i2c.write(new byte[]{0x00, (byte) b}, 0, 2);
    }

    private void initSSD1306() throws InterruptedException {
        // Pause après power-up : le SSD1306 a besoin de ~100ms avant d'accepter des commandes
        Thread.sleep(100);

        cmd(0xAE);              // Display OFF (reset state)
        cmd(0xD5); cmd(0xF0);  // Clock divide / oscillateur freq
        cmd(0xA8); cmd(0x3F);  // Multiplex ratio = 64
        cmd(0xD3); cmd(0x00);  // Display offset = 0
        cmd(0x40);              // Display start line = 0
        cmd(0x8D); cmd(0x14);  // Charge pump ON
        cmd(0x20); cmd(0x00);  // Horizontal addressing mode
        cmd(0xA1);              // Segment remap (col 127 = SEG0)
        cmd(0xC8);              // COM scan direction flipped (haut→bas)
        cmd(0xDA); cmd(0x12);  // COM pins hardware config (128x64)
        cmd(0x81); cmd(0xCF);  // Contrast
        cmd(0xD9); cmd(0xF1);  // Pre-charge period
        cmd(0xDB); cmd(0x40);  // VCOMH deselect level
        cmd(0xA4);              // Output follows RAM content
        cmd(0xA6);              // Normal display (non inversé)
        cmd(0xAF);              // Display ON — la charge pump a eu le temps de monter
    }

    // ── Buffer ────────────────────────────────────────────────────────────────

    /** Efface le buffer interne (ne touche pas encore l'écran). */
    public void clear() {
        java.util.Arrays.fill(buffer, (byte) 0);
    }

    /** Envoie le buffer complet (1024 octets) vers l'écran. */
    public void flush() {
        if (!available) return;
        // Positionner curseur : colonnes 0-127, pages 0-7
        i2c.write(new byte[]{0x00, 0x21, 0x00, (byte) 0x7F}, 0, 4);
        i2c.write(new byte[]{0x00, 0x22, 0x00, 0x07},         0, 4);
        // Envoyer les pixels (control byte 0x40 suivi des 1024 octets)
        byte[] payload = new byte[buffer.length + 1];
        payload[0] = 0x40;
        System.arraycopy(buffer, 0, payload, 1, buffer.length);
        i2c.write(payload, 0, payload.length);
    }

    // ── Rendu texte ───────────────────────────────────────────────────────────

    /**
     * Dessine un caractère ASCII dans le buffer.
     * @param c   caractère (0x20-0x7E)
     * @param col position horizontale en caractères (0-20)
     * @param row ligne de texte (0-7)
     */
    public void drawChar(char c, int col, int row) {
        if (c < 0x20 || c > 0x7E) c = ' ';
        byte[] glyph = FONT5X7[c - 0x20];
        int px   = col * FONT_WIDTH;
        int base = row * WIDTH + px;
        if (px + 5 > WIDTH || row >= PAGES) return;
        for (int i = 0; i < 5; i++) buffer[base + i] = glyph[i];
        buffer[base + 5] = 0;   // espace inter-caractère
    }

    /**
     * Dessine un caractère dans une grille 8×8 px (col 0-15, row 0-7).
     * Réutilise la police 5×7 avec 1px de marge gauche et 2px droite.
     */
    public void drawChar8x8(char c, int col, int row) {
        if (c < 0x20 || c > 0x7E) c = ' ';
        byte[] g = FONT5X7[c - 0x20];
        int px   = col * CHAR_SIZE_8X8;
        int base = row * WIDTH + px;
        if (px + CHAR_SIZE_8X8 > WIDTH || row >= PAGES) return;
        buffer[base]   = 0;
        buffer[base+1] = g[0];
        buffer[base+2] = g[1];
        buffer[base+3] = g[2];
        buffer[base+4] = g[3];
        buffer[base+5] = g[4];
        buffer[base+6] = 0;
        buffer[base+7] = 0;
    }

    /** Dessine une chaîne en grille 8×8 px depuis la colonne {@code col} sur la ligne {@code row}. */
    public void drawText8x8(String text, int col, int row) {
        if (text == null) return;
        for (int i = 0; i < text.length(); i++) {
            if (col + i >= CHARS_PER_LINE_8X8) break;
            drawChar8x8(text.charAt(i), col + i, row);
        }
    }

    /** Dessine une chaîne depuis la colonne {@code col} sur la ligne {@code row}. */
    public void drawText(String text, int col, int row) {
        if (text == null) return;
        for (int i = 0; i < text.length(); i++) {
            if (col + i >= CHARS_PER_LINE) break;
            drawChar(text.charAt(i), col + i, row);
        }
    }

    /**
     * Efface l'écran et affiche les lignes de texte (0 à 8 éléments).
     * C'est la méthode appelée par le client à chaque changement de page VTML.
     */
    public void displayLines(String[] lines) {
        if (!available) return;
        clear();
        if (lines != null) {
            for (int i = 0; i < lines.length && i < MAX_LINES; i++) {
                if (lines[i] != null && !lines[i].isEmpty()) {
                    drawText(lines[i], 0, i);
                }
            }
        }
        flush();
    }

    /** Efface complètement l'écran (affiche noir). */
    public void clearScreen() {
        if (!available) return;
        clear();
        flush();
    }

    /**
     * Règle le contraste (luminosité) de l'écran.
     * @param value 0x00 (minimum) … 0xFF (maximum); défaut hardware : 0xCF
     */
    public void setContrast(int value) {
        if (!available) return;
        cmd(0x81);
        cmd(value & 0xFF);
    }

    // ── Pixels et images ─────────────────────────────────────────────────────

    /**
     * Allume ou éteint un pixel individuel dans le buffer.
     * Le buffer SSD1306 est organisé en pages de 8 lignes : chaque byte
     * représente une colonne de 8 pixels (bit 0 = ligne du haut de la page).
     *
     * @param x  colonne (0–127)
     * @param y  ligne en pixels (0–63)
     * @param on true = pixel allumé
     */
    public void drawPixel(int x, int y, boolean on) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        int idx = (y / 8) * WIDTH + x;
        if (on) buffer[idx] = (byte)(buffer[idx] |  (1 << (y % 8)));
        else    buffer[idx] = (byte)(buffer[idx] & ~(1 << (y % 8)));
    }

    /**
     * Dessine un {@link java.awt.image.BufferedImage} dans le buffer.
     * L'image est redimensionnée à {@code width × height} pixels, puis binarisée
     * par seuillage de luminance (ITU-R BT.601 : 0.299R + 0.587G + 0.114B).
     *
     * @param src       image source (tout format AWT)
     * @param x         colonne de départ (0–127)
     * @param y         ligne de départ en pixels (0–63)
     * @param width     largeur cible en pixels
     * @param height    hauteur cible en pixels
     * @param threshold seuil de binarisation 0–255 ; luminance ≥ seuil → pixel allumé.
     *                  128 convient aux logos ; baisser pour les images claires.
     */
    public void drawImage(java.awt.image.BufferedImage src,
                          int x, int y, int width, int height, int threshold) {
        if (src == null || width <= 0 || height <= 0) return;

        java.awt.image.BufferedImage scaled =
            new java.awt.image.BufferedImage(width, height,
                                             java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                           java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int sx = x + px, sy = y + py;
                if (sx >= WIDTH || sy >= HEIGHT) continue;
                int rgb = scaled.getRGB(px, py);
                int lum = ((rgb >> 16 & 0xFF) * 77
                         + (rgb >>  8 & 0xFF) * 150
                         + ( rgb      & 0xFF) * 29) >> 8;
                drawPixel(sx, sy, lum >= threshold);
            }
        }
    }

    /** Variante avec seuil par défaut (128). */
    public void drawImage(java.awt.image.BufferedImage src,
                          int x, int y, int width, int height) {
        drawImage(src, x, y, width, height, 128);
    }

    /**
     * Charge un fichier image (PNG, GIF, JPEG, BMP…) et le dessine dans le buffer.
     *
     * @param path      chemin vers le fichier
     * @param x         colonne de départ (0–127)
     * @param y         ligne de départ en pixels (0–63)
     * @param width     largeur cible en pixels
     * @param height    hauteur cible en pixels
     * @param threshold seuil de binarisation (0–255)
     * @return true si chargé et dessiné avec succès
     */
    public boolean drawImageFile(String path, int x, int y,
                                 int width, int height, int threshold) {
        try {
            java.awt.image.BufferedImage img =
                javax.imageio.ImageIO.read(new java.io.File(path));
            if (img == null) {
                System.out.println("OLEDDisplay: format non reconnu — " + path);
                return false;
            }
            drawImage(img, x, y, width, height, threshold);
            return true;
        } catch (java.io.IOException e) {
            System.out.println("OLEDDisplay: erreur chargement — " + e.getMessage());
            return false;
        }
    }

    /** Variante avec seuil par défaut (128). */
    public boolean drawImageFile(String path, int x, int y, int width, int height) {
        return drawImageFile(path, x, y, width, height, 128);
    }

    /**
     * Charge une image depuis un flux (ressource JAR, réseau…) et la dessine.
     *
     * @param is        flux d'entrée (PNG, GIF, JPEG…)
     * @param x         colonne de départ (0–127)
     * @param y         ligne de départ en pixels (0–63)
     * @param width     largeur cible en pixels
     * @param height    hauteur cible en pixels
     * @param threshold seuil de binarisation (0–255)
     * @return true si lu et dessiné avec succès
     */
    public boolean drawImageStream(java.io.InputStream is, int x, int y,
                                   int width, int height, int threshold) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
            if (img == null) {
                System.out.println("OLEDDisplay: format non reconnu depuis le flux");
                return false;
            }
            drawImage(img, x, y, width, height, threshold);
            return true;
        } catch (java.io.IOException e) {
            System.out.println("OLEDDisplay: erreur lecture flux — " + e.getMessage());
            return false;
        }
    }

    /** Variante avec seuil par défaut (128). */
    public boolean drawImageStream(java.io.InputStream is,
                                   int x, int y, int width, int height) {
        return drawImageStream(is, x, y, width, height, 128);
    }

    /** Libère les ressources Pi4J. */
    public void close() {
        available = false;
        if (pi4j != null) {
            try { pi4j.shutdown(); } catch (Exception ignored) {}
            pi4j = null;
        }
    }

    // ── Police 5×7 (Adafruit GFX), ASCII 0x20 → 0x7E ────────────────────────
    // Chaque entrée = 5 octets (colonnes gauche→droite).
    // Bit 0 = ligne du haut, bit 6 = ligne du bas.

    private static final byte[][] FONT5X7 = {
        {0x00,0x00,0x00,0x00,0x00}, // 0x20 ' '
        {0x00,0x00,0x5F,0x00,0x00}, // 0x21 '!'
        {0x00,0x07,0x00,0x07,0x00}, // 0x22 '"'
        {0x14,0x7F,0x14,0x7F,0x14}, // 0x23 '#'
        {0x24,0x2A,0x7F,0x2A,0x12}, // 0x24 '$'
        {0x23,0x13,0x08,0x64,0x62}, // 0x25 '%'
        {0x36,0x49,0x55,0x22,0x50}, // 0x26 '&'
        {0x00,0x05,0x03,0x00,0x00}, // 0x27 '\''
        {0x00,0x1C,0x22,0x41,0x00}, // 0x28 '('
        {0x00,0x41,0x22,0x1C,0x00}, // 0x29 ')'
        {0x08,0x2A,0x1C,0x2A,0x08}, // 0x2A '*'
        {0x08,0x08,0x3E,0x08,0x08}, // 0x2B '+'
        {0x00,0x50,0x30,0x00,0x00}, // 0x2C ','
        {0x08,0x08,0x08,0x08,0x08}, // 0x2D '-'
        {0x00,0x60,0x60,0x00,0x00}, // 0x2E '.'
        {0x20,0x10,0x08,0x04,0x02}, // 0x2F '/'
        {0x3E,0x51,0x49,0x45,0x3E}, // 0x30 '0'
        {0x00,0x42,0x7F,0x40,0x00}, // 0x31 '1'
        {0x42,0x61,0x51,0x49,0x46}, // 0x32 '2'
        {0x21,0x41,0x45,0x4B,0x31}, // 0x33 '3'
        {0x18,0x14,0x12,0x7F,0x10}, // 0x34 '4'
        {0x27,0x45,0x45,0x45,0x39}, // 0x35 '5'
        {0x3C,0x4A,0x49,0x49,0x30}, // 0x36 '6'
        {0x01,0x71,0x09,0x05,0x03}, // 0x37 '7'
        {0x36,0x49,0x49,0x49,0x36}, // 0x38 '8'
        {0x06,0x49,0x49,0x29,0x1E}, // 0x39 '9'
        {0x00,0x36,0x36,0x00,0x00}, // 0x3A ':'
        {0x00,0x56,0x36,0x00,0x00}, // 0x3B ';'
        {0x08,0x14,0x22,0x41,0x00}, // 0x3C '<'
        {0x14,0x14,0x14,0x14,0x14}, // 0x3D '='
        {0x00,0x41,0x22,0x14,0x08}, // 0x3E '>'
        {0x02,0x01,0x51,0x09,0x06}, // 0x3F '?'
        {0x32,0x49,0x79,0x41,0x3E}, // 0x40 '@'
        {0x7E,0x11,0x11,0x11,0x7E}, // 0x41 'A'
        {0x7F,0x49,0x49,0x49,0x36}, // 0x42 'B'
        {0x3E,0x41,0x41,0x41,0x22}, // 0x43 'C'
        {0x7F,0x41,0x41,0x22,0x1C}, // 0x44 'D'
        {0x7F,0x49,0x49,0x49,0x41}, // 0x45 'E'
        {0x7F,0x09,0x09,0x01,0x01}, // 0x46 'F'
        {0x3E,0x41,0x41,0x51,0x32}, // 0x47 'G'
        {0x7F,0x08,0x08,0x08,0x7F}, // 0x48 'H'
        {0x00,0x41,0x7F,0x41,0x00}, // 0x49 'I'
        {0x20,0x40,0x41,0x3F,0x01}, // 0x4A 'J'
        {0x7F,0x08,0x14,0x22,0x41}, // 0x4B 'K'
        {0x7F,0x40,0x40,0x40,0x40}, // 0x4C 'L'
        {0x7F,0x02,0x04,0x02,0x7F}, // 0x4D 'M'
        {0x7F,0x04,0x08,0x10,0x7F}, // 0x4E 'N'
        {0x3E,0x41,0x41,0x41,0x3E}, // 0x4F 'O'
        {0x7F,0x09,0x09,0x09,0x06}, // 0x50 'P'
        {0x3E,0x41,0x51,0x21,0x5E}, // 0x51 'Q'
        {0x7F,0x09,0x19,0x29,0x46}, // 0x52 'R'
        {0x46,0x49,0x49,0x49,0x31}, // 0x53 'S'
        {0x01,0x01,0x7F,0x01,0x01}, // 0x54 'T'
        {0x3F,0x40,0x40,0x40,0x3F}, // 0x55 'U'
        {0x1F,0x20,0x40,0x20,0x1F}, // 0x56 'V'
        {0x3F,0x40,0x38,0x40,0x3F}, // 0x57 'W'
        {0x63,0x14,0x08,0x14,0x63}, // 0x58 'X'
        {0x03,0x04,0x78,0x04,0x03}, // 0x59 'Y'
        {0x61,0x51,0x49,0x45,0x43}, // 0x5A 'Z'
        {0x00,0x7F,0x41,0x41,0x00}, // 0x5B '['
        {0x02,0x04,0x08,0x10,0x20}, // 0x5C '\'
        {0x00,0x41,0x41,0x7F,0x00}, // 0x5D ']'
        {0x04,0x02,0x01,0x02,0x04}, // 0x5E '^'
        {0x40,0x40,0x40,0x40,0x40}, // 0x5F '_'
        {0x00,0x01,0x02,0x04,0x00}, // 0x60 '`'
        {0x20,0x54,0x54,0x54,0x78}, // 0x61 'a'
        {0x7F,0x48,0x44,0x44,0x38}, // 0x62 'b'
        {0x38,0x44,0x44,0x44,0x20}, // 0x63 'c'
        {0x38,0x44,0x44,0x48,0x7F}, // 0x64 'd'
        {0x38,0x54,0x54,0x54,0x18}, // 0x65 'e'
        {0x08,0x7E,0x09,0x01,0x02}, // 0x66 'f'
        {0x08,0x54,0x54,0x54,0x3C}, // 0x67 'g'
        {0x7F,0x08,0x04,0x04,0x78}, // 0x68 'h'
        {0x00,0x44,0x7D,0x40,0x00}, // 0x69 'i'
        {0x20,0x40,0x44,0x3D,0x00}, // 0x6A 'j'
        {0x7F,0x10,0x28,0x44,0x00}, // 0x6B 'k'
        {0x00,0x41,0x7F,0x40,0x00}, // 0x6C 'l'
        {0x7C,0x04,0x18,0x04,0x78}, // 0x6D 'm'
        {0x7C,0x08,0x04,0x04,0x78}, // 0x6E 'n'
        {0x38,0x44,0x44,0x44,0x38}, // 0x6F 'o'
        {0x7C,0x14,0x14,0x14,0x08}, // 0x70 'p'
        {0x08,0x14,0x14,0x18,0x7C}, // 0x71 'q'
        {0x7C,0x08,0x04,0x04,0x08}, // 0x72 'r'
        {0x48,0x54,0x54,0x54,0x20}, // 0x73 's'
        {0x04,0x3F,0x44,0x40,0x20}, // 0x74 't'
        {0x3C,0x40,0x40,0x20,0x7C}, // 0x75 'u'
        {0x1C,0x20,0x40,0x20,0x1C}, // 0x76 'v'
        {0x3C,0x40,0x30,0x40,0x3C}, // 0x77 'w'
        {0x44,0x28,0x10,0x28,0x44}, // 0x78 'x'
        {0x0C,0x50,0x50,0x50,0x3C}, // 0x79 'y'
        {0x44,0x64,0x54,0x4C,0x44}, // 0x7A 'z'
        {0x00,0x08,0x36,0x41,0x00}, // 0x7B '{'
        {0x00,0x00,0x7F,0x00,0x00}, // 0x7C '|'
        {0x00,0x41,0x36,0x08,0x00}, // 0x7D '}'
        {0x08,0x08,0x2A,0x1C,0x08}, // 0x7E '~'
    };
}
