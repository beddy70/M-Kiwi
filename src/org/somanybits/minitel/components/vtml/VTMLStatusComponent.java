package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <status>
 * Zone dédiée à l'affichage des informations de focus (menu/input actif)
 * 
 * Exemple: <status left="0" top="0" width="40" height="1"/>
 *
 * @author eddy
 */
public class VTMLStatusComponent extends ModelMComponent {

    public VTMLStatusComponent() {
        super();
        setWidth(40);
        setHeight(1);
    }

    public VTMLStatusComponent(int left, int top, int width, int height) {
        super();
        setX(left);
        setY(top);
        setWidth(width);
        setHeight(height);
    }

    /**
     * Efface la zone status
     */
    public byte[] clear() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int row = 0; row < getHeight(); row++) {
                out.write(GetTeletelCode.setCursor(getX(), getY() + row));
                out.write(" ".repeat(getWidth()).getBytes("ISO-8859-1"));
            }
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Affiche un message dans la zone status
     * @param message Le message à afficher (sera tronqué si trop long)
     */
    public byte[] showMessage(String message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Effacer d'abord
            out.write(clear());
            
            // Positionner et écrire le message
            out.write(GetTeletelCode.setCursor(getX(), getY()));
            
            // Tronquer si nécessaire
            String displayMsg = message;
            if (displayMsg.length() > getWidth()) {
                displayMsg = displayMsg.substring(0, getWidth());
            }
            
            out.write(displayMsg.getBytes("ISO-8859-1"));
            
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Affiche l'indicateur de focus menu
     */
    public byte[] showMenuFocus() {
        return showMessage(">> Menu <<");
    }

    /**
     * Affiche l'indicateur de focus input
     */
    public byte[] showInputFocus(String label) {
        return showMessage(">> Saisir: " + label + " <<");
    }

    @Override
    public byte[] getBytes() {
        // Le status ne s'affiche pas au rendu initial, seulement via showMessage
        return new byte[0];
    }
}
