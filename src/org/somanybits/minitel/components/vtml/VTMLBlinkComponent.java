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
        return getBytesClipped(Integer.MAX_VALUE);
    }

    public byte[] getBytesClipped(int maxChars) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write(GetTeletelCode.setBlink(true));

            if (text != null && !text.isEmpty()) {
                String t = text.length() > maxChars ? text.substring(0, maxChars) : text;
                out.write(t.getBytes("ISO-8859-1"));
            }

            out.write(GetTeletelCode.setBlink(false));

            return out.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
