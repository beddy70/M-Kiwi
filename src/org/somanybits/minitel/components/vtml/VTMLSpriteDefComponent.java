package org.somanybits.minitel.components.vtml;

import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Définition d'un sprite avec plusieurs frames d'animation.
 * 
 * @author eddy
 */
public class VTMLSpriteDefComponent extends ModelMComponent {
    
    public enum SpriteType {
        CHAR,   // Caractères normaux
        BITMAP  // Mode semi-graphique (# = 1, espace = 0)
    }
    
    private int spriteWidth;
    private int spriteHeight;
    private SpriteType type = SpriteType.CHAR;
    private List<VTMLSpriteComponent> frames = new ArrayList<>();
    
    public VTMLSpriteDefComponent(int width, int height, SpriteType type) {
        this.spriteWidth = width;
        this.spriteHeight = height;
        this.type = type;
    }
    
    public int getSpriteWidth() {
        return spriteWidth;
    }
    
    public int getSpriteHeight() {
        return spriteHeight;
    }
    
    public SpriteType getType() {
        return type;
    }
    
    /**
     * Ajoute une frame d'animation
     */
    public void addFrame(VTMLSpriteComponent frame) {
        frames.add(frame);
    }
    
    /**
     * Retourne le nombre de frames
     */
    public int getFrameCount() {
        return frames.size();
    }
    
    /**
     * Retourne une frame par son index
     */
    public VTMLSpriteComponent getFrame(int index) {
        if (index >= 0 && index < frames.size()) {
            return frames.get(index);
        }
        return null;
    }
    
    /**
     * Vérifie si une frame a des couleurs personnalisées
     */
    public boolean hasFrameColorData(int frameIndex) {
        VTMLSpriteComponent frame = getFrame(frameIndex);
        return frame != null && frame.hasColorData();
    }
    
    /**
     * Retourne les données de couleur d'une frame sous forme de tableau 2D
     * @param frameIndex Index de la frame
     * @return tableau de codes couleur (0-7), -1 = couleur par défaut du sprite
     */
    public int[][] getFrameColorData(int frameIndex) {
        VTMLSpriteComponent frame = getFrame(frameIndex);
        if (frame == null || !frame.hasColorData()) return null;
        
        int[][] rawColorData = frame.getColorData();
        
        if (type == SpriteType.CHAR) {
            return rawColorData;
        }
        
        // Mode BITMAP : adapter les couleurs aux caractères semi-graphiques
        // Chaque caractère semi-graphique = 2×3 pixels
        int pixelHeight = rawColorData.length;
        int pixelWidth = rawColorData.length > 0 ? rawColorData[0].length : 0;
        
        // Calculer les dimensions en caractères
        int charHeight = (pixelHeight + 2) / 3;
        int charWidth = (pixelWidth + 1) / 2;
        
        int[][] result = new int[charHeight][charWidth];
        
        // Pour chaque bloc 2×3, prendre la couleur dominante (ou la première non-default)
        for (int cy = 0; cy < charHeight; cy++) {
            for (int cx = 0; cx < charWidth; cx++) {
                int px = cx * 2;
                int py = cy * 3;
                
                // Chercher la première couleur définie dans le bloc
                int color = -1;
                for (int dy = 0; dy < 3 && color < 0; dy++) {
                    for (int dx = 0; dx < 2 && color < 0; dx++) {
                        int y = py + dy;
                        int x = px + dx;
                        if (y < pixelHeight && x < pixelWidth && rawColorData[y][x] >= 0) {
                            color = rawColorData[y][x];
                        }
                    }
                }
                result[cy][cx] = color;
            }
        }
        
        return result;
    }
    
    /**
     * Retourne les données d'une frame sous forme de tableau 2D
     * En mode BITMAP, convertit les # et espaces en caractères semi-graphiques
     */
    public char[][] getFrameData(int frameIndex) {
        VTMLSpriteComponent frame = getFrame(frameIndex);
        if (frame == null) return null;
        
        char[][] rawData = frame.getData();
        
        if (type == SpriteType.CHAR) {
            return rawData;
        }
        
        // Mode BITMAP : convertir en semi-graphique
        // Chaque caractère semi-graphique = 2×3 pixels
        // rawData contient les pixels (# = allumé, espace = éteint)
        int pixelHeight = rawData.length;
        int pixelWidth = rawData.length > 0 ? rawData[0].length : 0;
        
        // Calculer les dimensions en caractères
        int charHeight = (pixelHeight + 2) / 3;  // Arrondi supérieur
        int charWidth = (pixelWidth + 1) / 2;    // Arrondi supérieur
        
        char[][] result = new char[charHeight][charWidth];
        
        for (int cy = 0; cy < charHeight; cy++) {
            for (int cx = 0; cx < charWidth; cx++) {
                int semigfx = 0;
                
                // Lire les 6 pixels du bloc 2×3
                int px = cx * 2;
                int py = cy * 3;
                
                // Encodage semi-graphique Minitel (comme GraphTel)
                // Chaque pixel allumé ajoute son bit + le bit 5 (0x20)
                // Ligne 0
                if (getPixel(rawData, px, py)) semigfx |= 0b0100001;      // bit 0 + bit 5
                if (getPixel(rawData, px + 1, py)) semigfx |= 0b0100010;  // bit 1 + bit 5
                // Ligne 1
                if (getPixel(rawData, px, py + 1)) semigfx |= 0b0100100;  // bit 2 + bit 5
                if (getPixel(rawData, px + 1, py + 1)) semigfx |= 0b0101000;  // bit 3 + bit 5
                // Ligne 2
                if (getPixel(rawData, px, py + 2)) semigfx |= 0b0110000;  // bit 4 + bit 5
                if (getPixel(rawData, px + 1, py + 2)) semigfx |= 0b1100000;  // bit 6 + bit 5
                
                // Cas spéciaux
                if (semigfx == 0b1111111) {
                    semigfx = 0b1011111;  // Tous pixels allumés = exception
                } else if (semigfx == 0) {
                    semigfx = ' ';  // Espace normal = transparent (sera ignoré par drawSprite)
                }
                
                result[cy][cx] = (char) semigfx;
            }
        }
        
        return result;
    }
    
    /**
     * Lit un pixel dans les données brutes (# = true, autre = false)
     */
    private boolean getPixel(char[][] data, int x, int y) {
        if (y >= data.length || x >= (data.length > 0 ? data[0].length : 0)) {
            return false;
        }
        return data[y][x] == '#';
    }
    
    @Override
    public byte[] getBytes() {
        // Le sprite ne génère pas de bytes directement
        return new byte[0];
    }
}
