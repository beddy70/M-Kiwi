/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel;

import java.io.IOException;

/**
 * API haut niveau pour contrôler l'affichage Videotex.
 * <p>
 * Cette classe fournit des méthodes pour manipuler l'affichage du Minitel :
 * positionnement du curseur, couleurs, modes graphiques, etc.
 * </p>
 * 
 * <h2>Constantes d'écran</h2>
 * <ul>
 *   <li>{@link #PAGE_WIDTH} - Largeur de l'écran : 40 caractères</li>
 *   <li>{@link #PAGE_HEIGHT} - Hauteur de l'écran : 24 lignes</li>
 * </ul>
 * 
 * <h2>Couleurs disponibles</h2>
 * <p>8 couleurs : noir, rouge, vert, jaune, bleu, magenta, cyan, blanc</p>
 * 
 * <h2>Modes d'affichage</h2>
 * <ul>
 *   <li>{@link #MODE_TEXT} - Mode texte normal</li>
 *   <li>{@link #MODE_SEMI_GRAPH} - Mode semi-graphique (mosaïque)</li>
 * </ul>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see MinitelConnection
 * @see GetTeletelCode
 */
public class Teletel {

    /** Largeur de l'écran Minitel en caractères */
    public static final int PAGE_WIDTH = 40;
    /** Hauteur de l'écran Minitel en lignes */
    public static final int PAGE_HEIGHT = 24;

    static public final int COLOR_BLACK = 0x00;
    static public final int COLOR_RED = 0x01;
    static public final int COLOR_GREEN = 0x02;
    static public final int COLOR_YELLOW = 0x03;
    static public final int COLOR_BLUE = 0x04;
    static public final int COLOR_MAGENTA = 0x05;
    static public final int COLOR_CYAN = 0x06;
    static public final int COLOR_WHITE = 0x07;

    static public final int MODE_TEXT = 0x01;
    static public final int MODE_SEMI_GRAPH = 0x00;

    static public final byte MODE_VIDEOTEXT = 0x00;
    static public final byte MODE_MIXTE = 0x01;
    static public final byte MODE_STANDARD = 0x02;
    private int screenmode = MODE_VIDEOTEXT;

    private final MinitelConnection mterm;

    public Teletel(MinitelConnection mt) {
        mterm = mt;
    }

    public MinitelConnection getMterm() {
        return mterm;
    }

    public void writeString(String text) {

        try {
            mterm.writeText(text);
        } catch (IOException ex) {
            System.getLogger(Teletel.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

    public int getGrey(int level) {
        level &= 0x7;
        int color = 0;

        switch (level) {
            case 0:
                color = COLOR_BLACK; //0%
                break;
            case 1:
                color = COLOR_BLUE; //40%
                break;
            case 2:
                color = COLOR_RED; //50%
                break;
            case 3:
                color = COLOR_MAGENTA; //60%
                break;
            case 4:
                color = COLOR_GREEN; //70%
                break;
            case 5:
                color = COLOR_CYAN; //80%
                break;
            case 6:
                color = COLOR_YELLOW; //90%
                break;
            case 7:
                color = COLOR_WHITE; //100%
                break;

        }
        return color;
    }

    public void setTextColor(int color) throws IOException {
        mterm.writeByte((byte) (0x1b));
        mterm.writeByte((byte) (0x40 + (byte) color));
    }

    public void setBGColor(int color) throws IOException {
        mterm.writeByte((byte) (0x1b));
        mterm.writeByte((byte) (0x50 + (byte) color));

    }

    /**
     * Positionne le curseur aux coordonnées spécifiées.
     * Par défaut, la ligne 0 est protégée et le curseur sera placé en ligne 1.
     * Utilisez {@link GetTeletelCode#enableLineZero(boolean)} pour autoriser l'écriture en ligne 0.
     * 
     * @param x Position horizontale (0-39)
     * @param y Position verticale (0-24, mais 0 est protégée par défaut)
     */
    public void setCursor(int x, int y) throws IOException {
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        
        // Protection ligne 0 : utiliser la config globale de GetTeletelCode
        if (!GetTeletelCode.isLineZeroEnabled() && y == 0) {
            y = 1;
        }
        
        mterm.writeByte((byte) (0x1f));
        mterm.writeByte((byte) (y + 0x40));
        mterm.writeByte((byte) (x + 0x40 + 1));
    }

    public void setCursorHome() throws IOException {
        mterm.writeByte((byte) (0x1e));

    }

    public void moveCursorXY(int x, int y) throws IOException {  // Voir p.95
        mterm.writeByte((byte) (0x1B));
        mterm.writeByte((byte) (0x5B));
        mterm.writeByteP(y);   // Pr : Voir section Private ci-dessous
        mterm.writeByte((byte) (0x3B));
        mterm.writeByteP(x);   // Pc : Voir section Private ci-dessous
        mterm.writeByte((byte) (0x48));
    }

  

    public void clearLineZero() throws IOException {
        this.setCursor(0, 0);
        this.writeString(" ".repeat(40));
    }

    public void clear() throws IOException {

        if (screenmode != MODE_VIDEOTEXT) {
            mterm.writeByte((byte) (0x0c));
        } else {
            mterm.writeByte((byte) (0x1b));
            mterm.writeByte((byte) (0x5B));
            mterm.writeByte((byte) (0x32));
            mterm.writeByte((byte) (0x4A));
            mterm.writeByte((byte) (0x1b));
            mterm.writeByte((byte) (0x5B));
            mterm.writeByte((byte) (0x48));
        }

    }
// ECHO OFF

    public void setEcho(boolean flag) throws IOException {

        mterm.writeByte((byte) (0x1b));
        mterm.writeByte((byte) (0x3B));

        if (flag) {
            mterm.writeByte((byte) (0x61)); //ON
        } else {
            mterm.writeByte((byte) (0x60)); //OFF
        }
        mterm.writeByte((byte) (0x58));
        mterm.writeByte((byte) (0x51));

    }

    public void setBlink(boolean flag) throws IOException {
        mterm.writeByte((byte) (0x1b));
        if (flag) {
            mterm.writeByte((byte) (0x48));
        } else {
            mterm.writeByte((byte) (0x49));

        }
    }

    public void setMode(int mode) throws IOException {
        mterm.writeByte((byte) (0x0e + mode));
    }

    public void setScreenMode(int screenmode) throws IOException {
        this.screenmode = screenmode;
        switch (screenmode) {
            case MODE_MIXTE:
                mterm.writeByte((byte) (0x1B));
                mterm.writeByte((byte) (0x3A));
                mterm.writeByte((byte) (0x32));
                mterm.writeByte((byte) (0x7D));
                break;
            case MODE_STANDARD:
                mterm.writeByte((byte) (0x1B));
                mterm.writeByte((byte) (0x3A));
                mterm.writeByte((byte) (0x31));
                mterm.writeByte((byte) (0x7D));
                break;
            case MODE_VIDEOTEXT:
            default:
                mterm.writeByte((byte) (0x1B));
                mterm.writeByte((byte) (0x3A));
                mterm.writeByte((byte) (0x32));
                mterm.writeByte((byte) (0x7E));

        }
    }
}
