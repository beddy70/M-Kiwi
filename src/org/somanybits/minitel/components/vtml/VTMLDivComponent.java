package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <div>
 * Contient une zone de texte avec paramètres top, left, width, height
 *
 * @author eddy
 */
public class VTMLDivComponent extends ModelMComponent {

    public VTMLDivComponent() {
        super();
    }

    public VTMLDivComponent(int left, int top, int width, int height, String textContent) {
        super();
        setX(left);
        setY(top);
        setWidth(width);
        setHeight(height);
        setTextContent(textContent);
    }
    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream divdata = new ByteArrayOutputStream();

            int left  = getX();
            int top   = getY();
            int width = getWidth();

            String text = getTextContent();
            if (text != null && !text.trim().isEmpty()) {
                if (width > 0) {
                    // Wrap text at width, capped at height lines when height > 0
                    int height = getHeight();
                    int row = 0;
                    for (int pos = 0; pos < text.length() && (height <= 0 || row < height); pos += width) {
                        String chunk = text.substring(pos, Math.min(pos + width, text.length()));
                        divdata.write(GetTeletelCode.setCursor(left, top + row));
                        divdata.write(chunk.getBytes("ISO-8859-1"));
                        row++;
                    }
                    setY(top + row); // children (<row>) start after the text
                } else {
                    divdata.write(GetTeletelCode.setCursor(left, top));
                    divdata.write(text.getBytes("ISO-8859-1"));
                }
            } else {
                divdata.write(GetTeletelCode.setCursor(left, top));
            }

            for (MComponent child : getChilds()) {
                divdata.write(child.getBytes());
            }

            return divdata.toByteArray();

        } catch (IOException ex) {
            System.getLogger(VTMLDivComponent.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return new byte[0];
    }
}
