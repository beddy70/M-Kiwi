package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant un label de texte modifiable dans un layers
 * Tag: <label id="score" x="30" y="0" width="10">0-0</label>
 */
public class VTMLLabelComponent extends ModelMComponent {
    
    private String text = "";
    
    public VTMLLabelComponent() {
        super();
    }
    
    public VTMLLabelComponent(String id, int x, int y, int width, String text) {
        super();
        setId(id);
        setX(x);
        setY(y);
        setWidth(width);
        this.text = text != null ? text : "";
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text != null ? text : "";
    }
    
    /**
     * Retourne le texte formaté pour l'affichage (padded ou tronqué)
     */
    public String getDisplayText() {
        int width = getWidth();
        if (width <= 0) return text;
        
        if (text.length() > width) {
            return text.substring(0, width);
        } else if (text.length() < width) {
            // Padding avec des espaces
            StringBuilder sb = new StringBuilder(text);
            while (sb.length() < width) {
                sb.append(' ');
            }
            return sb.toString();
        }
        return text;
    }
    
    @Override
    public byte[] getBytes() {
        // Le label ne génère pas de bytes directement, il est géré par le layers
        return new byte[0];
    }
}
