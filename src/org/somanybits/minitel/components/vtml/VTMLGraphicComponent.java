/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import java.io.IOException;
import org.somanybits.minitel.components.GraphTel;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML {@code <graphic>} — zone de dessin semi-graphique Minitel.
 *
 * <h2>Tag VTML</h2>
 * <pre>{@code
 * <graphic id="canvas" left="0" top="2" width="20" height="10">
 *   <setcolor value="7"/>
 *   <drawrect x="0" y="0" w="39" h="29"/>
 *   <drawline x1="0" y1="0" x2="39" y2="29" color="3"/>
 * </graphic>
 * }</pre>
 *
 * <h2>Dimensions</h2>
 * {@code width} et {@code height} sont en <b>caractères Minitel</b>.
 * En interne, GraphTel travaille en pixels : 1 char = 2px × 3px.
 *
 * <h2>API JavaScript</h2>
 * <pre>{@code
 * var c = getElementById("canvas");
 * c.setColor(3);              // encre jaune
 * c.setBgColor(0);            // fond noir
 * c.setPixel(10, 5);
 * c.drawLine(0, 0, 19, 9);
 * c.drawRect(2, 2, 15, 7);
 * c.fillRect(2, 2, 15, 7);
 * c.drawCircle(10, 5, 4);
 * c.fillCircle(10, 5, 4);
 * c.clear();
 * c.repaintAll();             // force full redraw au prochain cycle
 * }</pre>
 *
 * <h2>Mise à jour différentielle</h2>
 * Chaque appel de dessin marque les cellules (blocs 2×3) affectées comme
 * "dirty". {@link #getDifferentialBytes()} n'émet que ces cellules, puis les
 * efface. {@link #getBytes()} fait un rendu complet et remet tout à zéro.
 */
public class VTMLGraphicComponent extends ModelMComponent {

    private final GraphTel gfx;

    /**
     * @param left        Position X en caractères Minitel
     * @param top         Position Y en caractères Minitel
     * @param widthChars  Largeur en caractères (1 char = 2 pixels)
     * @param heightChars Hauteur en caractères (1 char = 3 pixels)
     */
    public VTMLGraphicComponent(int left, int top, int widthChars, int heightChars) {
        gfx = new GraphTel(widthChars * 2, heightChars * 3);
        setX(left);
        setY(top);
        setWidth(widthChars);
        setHeight(heightChars);
    }

    // ========== API DE DESSIN (proxy vers GraphTel) ==========

    /** Définit la couleur d'encre (foreground, 0-7) */
    public void setColor(int color) {
        gfx.setInk((byte) color);
    }

    /** Définit la couleur de fond (background, 0-7) */
    public void setBgColor(int color) {
        gfx.setBGColor((byte) color);
    }

    /** Active (true) ou désactive (false) le mode dessin */
    public void setPen(boolean on) {
        gfx.setPen(on);
    }

    /** Place un pixel aux coordonnées pixel données */
    public void setPixel(int x, int y) {
        gfx.setPixel(x, y);
    }

    /** Trace une ligne (coordonnées pixel) */
    public void drawLine(int x1, int y1, int x2, int y2) {
        gfx.setLine(x1, y1, x2, y2);
    }

    /** Dessine le contour d'un rectangle (coordonnées pixel, taille en pixels) */
    public void drawRect(int x, int y, int w, int h) {
        gfx.drawRect(x, y, w, h);
    }

    /** Dessine un rectangle plein */
    public void fillRect(int x, int y, int w, int h) {
        gfx.fillRect(x, y, w, h);
    }

    /** Dessine le contour d'un cercle */
    public void drawCircle(int x, int y, int r) {
        gfx.setCircle(x, y, r);
    }

    /** Dessine un cercle plein */
    public void fillCircle(int x, int y, int r) {
        gfx.fillCircle(x, y, r);
    }

    /** Efface toute la zone (tous les pixels à fond noir) et marque tout dirty */
    public void clear() {
        gfx.clear();
    }

    /** Force un prochain getDifferentialBytes() à tout renvoyer (full repaint) */
    public void repaintAll() {
        gfx.markAllDirty();
    }

    /** @return true si des cellules ont été modifiées depuis le dernier rendu */
    public boolean hasDirtyPixels() {
        return gfx.hasDirty();
    }

    // ========== RENDU ==========

    private final java.io.ByteArrayOutputStream pendingOutput = new java.io.ByteArrayOutputStream();

    /**
     * Enregistre les cellules dirty dans le buffer pending.
     * Appelé explicitement depuis JS via {@code c.update()}.
     * Les données sont envoyées au terminal par {@link #consumePendingBytes()}.
     */
    public void update() {
        try {
            byte[] diff = gfx.getDifferentialBytes(getX(), getY());
            if (diff.length > 0) {
                pendingOutput.write(diff);
            }
        } catch (IOException e) {
            System.err.println("❌ VTMLGraphicComponent.update: " + e.getMessage());
        }
    }

    /**
     * Retourne et vide le buffer pending accumulé par {@link #update()}.
     * Appelé par MinitelClient après chaque tick JS.
     */
    public byte[] consumePendingBytes() {
        byte[] data = pendingOutput.toByteArray();
        pendingOutput.reset();
        return data;
    }

    /**
     * Rendu complet de la zone. Appelé lors du chargement initial de la page.
     * Remet les flags dirty et le buffer pending à zéro après l'émission.
     */
    @Override
    public byte[] getBytes() {
        try {
            byte[] data = gfx.getDrawToBytes(getX(), getY());
            gfx.clearDirty();
            pendingOutput.reset();
            return data;
        } catch (IOException e) {
            System.err.println("❌ VTMLGraphicComponent.getBytes: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Rendu différentiel : émet uniquement les cellules (blocs 2×3) modifiées
     * depuis le dernier appel à cette méthode ou à {@link #getBytes()}.
     * Retourne un tableau vide si rien n'a changé.
     */
    public byte[] getDifferentialBytes() {
        try {
            return gfx.getDifferentialBytes(getX(), getY());
        } catch (IOException e) {
            System.err.println("❌ VTMLGraphicComponent.getDifferentialBytes: " + e.getMessage());
            return new byte[0];
        }
    }
}
