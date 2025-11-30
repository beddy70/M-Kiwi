package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <input>
 * Champ de saisie texte pour Minitel
 * Implémente Focusable pour le système de focus des formulaires.
 * 
 * Exemple:
 * <input name="recherche" left="5" top="10" width="20" label="Recherche:">
 *
 * @author eddy
 */
public class VTMLInputComponent extends ModelMComponent implements Focusable {

    private String name;
    private String value = "";
    private String label;
    private String placeholder;
    public static final char DEFAULT_FILL_CHAR = '.';

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
            //inputdata.write(GetTeletelCode.setInverse(true));
            
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
                field.append(DEFAULT_FILL_CHAR);
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
    
    /**
     * Calcule la position X absolue (incluant le parent et le label)
     */
    public int getAbsoluteX() {
        int absX = getX();
        if (getParent() != null) {
            absX += getParent().getX();
        }
        if (label != null && !label.isEmpty()) {
            absX += label.length() + 1;
        }
        return absX;
    }
    
    /**
     * Calcule la position Y absolue (incluant le parent)
     */
    public int getAbsoluteY() {
        int absY = getY();
        if (getParent() != null) {
            absY += getParent().getY();
        }
        return absY;
    }
    
    // ========== IMPLÉMENTATION FOCUSABLE ==========
    
    @Override
    public FocusType getFocusType() {
        return FocusType.INPUT;
    }
    
    @Override
    public String getFocusLabel() {
        if (label != null && !label.isEmpty()) {
            return "Saisir: " + label;
        }
        return name != null ? "Saisir: " + name : "Saisir";
    }
    
    @Override
    public byte[] onFocusGained() {
        // Quand l'input reçoit le focus :
        // 1. Positionner le curseur dans le champ
        // 2. Activer le curseur clignotant
        // Note: l'écho reste désactivé, on gère l'affichage nous-mêmes dans appendChar
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Positionner le curseur au début du champ (après la valeur actuelle)
            int cursorX = getAbsoluteX() + (value != null ? value.length() : 0);
            int cursorY = getAbsoluteY();
            out.write(GetTeletelCode.setCursor(cursorX, cursorY));
            
            // Activer le curseur visible
            out.write(GetTeletelCode.showCursor(true));
            
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
    
    @Override
    public byte[] onFocusLost() {
        // Quand l'input perd le focus :
        // 1. Masquer le curseur
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(GetTeletelCode.showCursor(false));
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
    
    /**
     * Ajoute un caractère à la valeur (appelé lors de la saisie)
     * @return Les bytes (vide si OK, bip + correction si champ plein)
     */
    public byte[] appendChar(char c) {
        if (value == null) {
            value = "";
        }
        if (value.length() < getWidth()) {
            value += c;
            // Ne rien renvoyer : le Minitel affiche déjà via écho local
            return new byte[0];
        }
        // Champ plein : bip + backspace + espace + backspace pour annuler l'écho local
        return new byte[] { 0x07, 0x08, ' ', 0x08 };
    }
    
    /**
     * Supprime le dernier caractère (backspace)
     * @return Les bytes pour mettre à jour l'affichage
     */
    public byte[] deleteChar() {
        if (value != null && value.length() > 0) {
            value = value.substring(0, value.length() - 1);
            // Reculer, écrire un espace, reculer
            return new byte[] { 0x08, DEFAULT_FILL_CHAR, 0x08 };
        }
        return new byte[0];
    }
    
    /**
     * Efface complètement la valeur et réaffiche la zone vide
     * @return Les bytes pour mettre à jour l'affichage
     */
    public byte[] clearValue() {
        value = "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Positionner au début de la zone de saisie
            out.write(GetTeletelCode.setCursor(getAbsoluteX(), getAbsoluteY()));
            
            // Réafficher la zone vide en inverse
            //out.write(GetTeletelCode.setInverse(true));
            out.write((DEFAULT_FILL_CHAR + "").repeat(getWidth()).getBytes("ISO-8859-1"));
            out.write(GetTeletelCode.setInverse(false));
            
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
