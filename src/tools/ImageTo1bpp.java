/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tools;

/**
 *
 * @author eddy
 */
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ImageTo1bpp {

    private byte[] bitmap = null;
    private int width;
    private int height;

    /**
     * Constructeur depuis un fichier local
     */
    public ImageTo1bpp(String filename, int targetW, int targetH) throws IOException {
        File file = new File(filename);
        BufferedImage src = ImageIO.read(file);
        if (src == null) {
            throw new IOException("Impossible de lire l'image: " + file);
        }
        processImage(src, targetW, targetH);
    }

    /**
     * Constructeur depuis une URL
     */
    public ImageTo1bpp(URL url, int targetW, int targetH) throws IOException {
        BufferedImage src = ImageIO.read(url);
        if (src == null) {
            throw new IOException("Impossible de lire l'image: " + url);
        }
        processImage(src, targetW, targetH);
    }

    /**
     * Traitement commun de l'image
     */
    private void processImage(BufferedImage src, int targetW, int targetH) {

        // Redimensionnement vers targetW x targetH
        BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();

        width = img.getWidth();
        height = img.getHeight();

        int bytesPerRow = (width + 7) / 8;
        bitmap = new byte[bytesPerRow * height];

        int threshold = 128; // luminosité

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = img.getRGB(x, y);

                int r = (argb >> 16) & 0xFF;
                int gg = (argb >> 8) & 0xFF;
                int b = (argb) & 0xFF;

                int lum = (int) (0.299 * r + 0.587 * gg + 0.114 * b);

                // Noir si en dessous du seuil
                boolean isBlack = lum < threshold;

                int byteIndex = y * bytesPerRow + (x / 8);
                int bitIndex = 7 - (x % 8);      // pixel 0 -> bit 7

                if (isBlack) {
                    bitmap[byteIndex] |= (1 << bitIndex);
                }
            }
        }

        // Debug visuel ASCII
        debugAscii(bitmap, width, height);

        // Dump en tableau Java
        //dumpAsJavaArray(bitmap, bytesPerRow, width, height);
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

    public byte[] getBitmap() {
        return bitmap;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private static void dumpAsJavaArray(byte[] data, int bytesPerRow, int width, int height) {
        System.out.println("// width = " + width + ", height = " + height);
        System.out.println("// bytesPerRow = " + bytesPerRow + ", total bytes = " + data.length);
        System.out.println("byte[] BITMAP = {");
        for (int i = 0; i < data.length; i++) {
            if (i % bytesPerRow == 0) {
                System.out.print("    ");
            }
            int v = data[i] & 0xFF;
            System.out.printf("(byte)0x%02X", v);
            if (i < data.length - 1) {
                System.out.print(", ");
            }
            if ((i + 1) % bytesPerRow == 0) {
                System.out.println();
            }
        }
        System.out.println("};");
    }
}
