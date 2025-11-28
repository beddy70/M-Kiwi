package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <input>
 * Champ de saisie texte pour Minitel
 * 
 * Exemple:
 * <input name="recherche" left="5" top="10" width="20" label="Recherche:">
 *
 * @author eddy
 */
public class VTMLInputComponent extends ModelMComponent {

    private String name;
    private String value = "";
    private String label;
    private String placeholder;

    public VTMLInputComponent() {
        super();
        setWidth(20);
        setHeight(1);
    }

    public VTMLInputComponent(String name, int left, int top, int width) {
        super();
        this.name = name;
        setX(left);
        setY(top);
        setWidth(width);
        setHeight(1);
    }

    public VTMLInputComponent(String name, int left, int top, int width, String label) {
        this(name, left, top, width);
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream inputdata = new ByteArrayOutputStream();

            // Position absolue basée sur le parent (form) + position relative
            int absX = getX();
            int absY = getY();
            if (getParent() != null) {
                absX += getParent().getX();
                absY += getParent().getY();
            }

            // Afficher le label si présent
            if (label != null && !label.isEmpty()) {
                inputdata.write(GetTeletelCode.setCursor(absX, absY));
                inputdata.write(label.getBytes());
                absX += label.length() + 1;
            }

            // Positionner le curseur pour le champ de saisie
            inputdata.write(GetTeletelCode.setCursor(absX, absY));

            // Zone de saisie : fond inversé pour la rendre visible
            inputdata.write(GetTeletelCode.setInverse(true));
            
            // Afficher le placeholder ou des espaces pour la zone de saisie
            String displayText;
            if (value != null && !value.isEmpty()) {
                displayText = value;
            } else if (placeholder != null && !placeholder.isEmpty()) {
                displayText = placeholder;
            } else {
                displayText = "";
            }
            
            // Remplir la zone avec des espaces jusqu'à la largeur
            StringBuilder field = new StringBuilder(displayText);
            while (field.length() < getWidth()) {
                field.append(" ");
            }
            if (field.length() > getWidth()) {
                field.setLength(getWidth());
            }
            
            inputdata.write(field.toString().getBytes());
            inputdata.write(GetTeletelCode.setInverse(false));

            return inputdata.toByteArray();
        } catch (IOException ex) {
            System.err.println("Erreur VTMLInputComponent: " + ex.getMessage());
            return new byte[0];
        }
    }
}
