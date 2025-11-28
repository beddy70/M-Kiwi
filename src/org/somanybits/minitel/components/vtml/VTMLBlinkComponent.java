package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <blink>
 * Active le clignotement pour le texte contenu
 *
 * @author eddy
 */
public class VTMLBlinkComponent extends ModelMComponent {

    private String text;  // Texte à faire clignoter

    public VTMLBlinkComponent() {
        super();
    }

    public VTMLBlinkComponent(String text) {
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            // Activer le clignotement
            out.write(GetTeletelCode.setBlink(true));
            
            // Ajouter le texte
            if (text != null && !text.isEmpty()) {
                out.write(text.getBytes("ISO-8859-1"));
            }
            
            // Désactiver le clignotement (retour à fixe)
            out.write(GetTeletelCode.setBlink(false));
            
            return out.toByteArray();
            
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
