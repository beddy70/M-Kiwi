/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.somanybits.minitel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @author eddy
 */
public class TeletelCode {

    static public final byte PAGE_WIDTH       = 40;
    static public final byte PAGE_HEIGHT     = 24;

    static public final byte COLOR_BLACK     = 0x00;
    static public final byte COLOR_RED       = 0x01;
    static public final byte COLOR_GREEN     = 0x02;
    static public final byte COLOR_YELLOW    = 0x03;
    static public final byte COLOR_BLUE      = 0x04;
    static public final byte COLOR_MAGENTA   = 0x05;
    static public final byte COLOR_CYAN      = 0x06;
    static public final byte COLOR_WHITE     = 0x07;

    static public final byte MODE_TEXT       = 0x01;
    static public final byte MODE_SEMI_GRAPH = 0x00;

    static public final byte MODE_VIDEOTEXT = 0x00;
    static public final byte MODE_MIXTE     = 0x01;
    static public final byte MODE_STANDARD  = 0x02;
    private int screenmode = MODE_VIDEOTEXT;

    ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);

    public int getGrey(int level) {
        level &= 0x7;
        int color = 0;

        switch (level) {
            case 0 ->
                color = COLOR_BLACK;   //0%
            case 1 ->
                color = COLOR_BLUE;    //40%
            case 2 ->
                color = COLOR_RED;     //50%
            case 3 ->
                color = COLOR_MAGENTA; //60%
            case 4 ->
                color = COLOR_GREEN;   //70%
            case 5 ->
                color = COLOR_CYAN;    //80%
            case 6 ->
                color = COLOR_YELLOW;  //90%
            case 7 ->
                color = COLOR_WHITE;   //100%

        }
        return color;
    }

    public byte[] setTextColor(int color) throws IOException {
        buf.reset();
        buf.write((byte) (0x1b));
        buf.write((byte) (0x40 + (byte) color));

        return buf.toByteArray();
    }

    public byte[] setBGColor(int color) throws IOException {
        buf.reset();
        buf.write((byte) (0x1b));
        buf.write((byte) (0x50 + (byte) color));
        return buf.toByteArray();
    }

    public byte[] setCursor(int x, int y) throws IOException {
        buf.reset();
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        buf.write((byte) (0x1f));
        buf.write((byte) (y + 0x40));
        buf.write((byte) (x + 0x40 + 1));

        return buf.toByteArray();
    }

    public byte[] setCursorHome() throws IOException {
        buf.reset();
        buf.write((byte) (0x1e));
        return buf.toByteArray();
    }

    public byte[] clear() throws IOException {
        buf.reset();

        if (screenmode != MODE_MIXTE) {
            buf.write((byte) (0x0c));
        } else {
            buf.write((byte) (0x1b));
            buf.write((byte) (0x5B));
            buf.write((byte) (0x32));
            buf.write((byte) (0x4A));
            buf.write((byte) (0x1b));
            buf.write((byte) (0x5B));
            buf.write((byte) (0x48));
        }
        return buf.toByteArray();
    }
// ECHO OFF

    public byte[] setEcho(boolean flag) throws IOException {
        buf.reset();

        buf.write((byte) (0x1b));
        buf.write((byte) (0x3B));

        if (flag) {
            buf.write((byte) (0x61)); //ON
        } else {
            buf.write((byte) (0x60)); //OFF
        }
        buf.write((byte) (0x58));
        buf.write((byte) (0x51));
        return buf.toByteArray();
    }

    public byte[] setBlink(boolean flag) throws IOException {
        buf.reset();
        buf.write((byte) (0x1b));
        if (flag) {
            buf.write((byte) (0x48));
        } else {
            buf.write((byte) (0x49));

        }
        return buf.toByteArray();
    }

    public byte[] setMode(int mode) throws IOException {
        buf.reset();
        buf.write((byte) (0x0e + mode));
        return buf.toByteArray();
    }

    public byte[] setScreenMode(int screenmode) throws IOException {
        buf.reset();
        this.screenmode = screenmode;
        switch (screenmode) {
            case MODE_MIXTE:
                buf.write((byte) (0x1B));
                buf.write((byte) (0x3A));
                buf.write((byte) (0x32));
                buf.write((byte) (0x7D));
                break;
            case MODE_STANDARD:
                buf.write((byte) (0x1B));
                buf.write((byte) (0x3A));
                buf.write((byte) (0x31));
                buf.write((byte) (0x7D));
                break;
            case MODE_VIDEOTEXT:
            default:
                buf.write((byte) (0x1B));
                buf.write((byte) (0x3A));
                buf.write((byte) (0x32));
                buf.write((byte) (0x7E));

        }
        return buf.toByteArray();
    }

}
