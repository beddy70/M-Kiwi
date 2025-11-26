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

}
