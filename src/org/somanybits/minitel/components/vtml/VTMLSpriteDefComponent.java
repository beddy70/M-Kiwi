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
     * Retourne les données d'une frame sous forme de tableau 2D
     */
    public char[][] getFrameData(int frameIndex) {
        VTMLSpriteComponent frame = getFrame(frameIndex);
        if (frame == null) return null;
        return frame.getData();
    }
    
    @Override
    public byte[] getBytes() {
        // Le sprite ne génère pas de bytes directement
        return new byte[0];
    }
}
