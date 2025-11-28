package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <color>
 * Définit les couleurs d'encre (ink) et de fond (background)
 *
 * Couleurs disponibles : black, red, green, yellow, blue, magenta, cyan, white
 * ou valeurs numériques 0-7
 *
 * @author eddy
 */
public class VTMLColorComponent extends ModelMComponent {

    private String ink;        // Couleur d'encre (texte)
    private String background; // Couleur de fond
    private String text;       // Texte inline optionnel

    public VTMLColorComponent() {
        super();
    }

    public VTMLColorComponent(String ink, String background) {
        super();
        this.ink = ink;
        this.background = background;
    }

    public VTMLColorComponent(String ink, String background, String text) {
        super();
        this.ink = ink;
        this.background = background;
        this.text = text;
    }

    public String getInk() {
        return ink;
    }

    public void setInk(String ink) {
        this.ink = ink;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * Convertit un nom de couleur ou une valeur numérique en code couleur Minitel
     */
    private byte parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return -1; // Pas de changement
        }
        
        // Essayer de parser comme nombre
        try {
            int value = Integer.parseInt(color.trim());
            if (value >= 0 && value <= 7) {
                return (byte) value;
            }
        } catch (NumberFormatException e) {
            // Pas un nombre, essayer comme nom
        }
        
        // Parser comme nom de couleur
        return switch (color.toLowerCase().trim()) {
            case "black", "noir" -> GetTeletelCode.COLOR_BLACK;
            case "red", "rouge" -> GetTeletelCode.COLOR_RED;
            case "green", "vert" -> GetTeletelCode.COLOR_GREEN;
            case "yellow", "jaune" -> GetTeletelCode.COLOR_YELLOW;
            case "blue", "bleu" -> GetTeletelCode.COLOR_BLUE;
            case "magenta" -> GetTeletelCode.COLOR_MAGENTA;
            case "cyan" -> GetTeletelCode.COLOR_CYAN;
            case "white", "blanc" -> GetTeletelCode.COLOR_WHITE;
            default -> -1;
        };
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Définir la couleur de fond si spécifiée
            byte bgColor = parseColor(background);
            if (bgColor >= 0) {
                out.write(GetTeletelCode.setBGColor(bgColor));
            }
            
            // Définir la couleur d'encre si spécifiée
            byte inkColor = parseColor(ink);
            if (inkColor >= 0) {
                out.write(GetTeletelCode.setTextColor(inkColor));
            }
            
            // Ajouter le texte inline si présent
            if (text != null && !text.isEmpty()) {
                out.write(text.getBytes("ISO-8859-1"));
            }
            
            return out.toByteArray();
            
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
