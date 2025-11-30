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

            divdata.write(GetTeletelCode.setCursor(getX(), getY()));
            // Ne pas écrire le textContent s'il ne contient que des espaces/retours
            String text = getTextContent();
            if (text != null && !text.trim().isEmpty()) {
                divdata.write(text.getBytes());
            }

            // Ajouter le contenu des enfants
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
