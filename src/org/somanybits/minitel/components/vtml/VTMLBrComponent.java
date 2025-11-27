package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <br>
 * Saut de ligne simple
 *
 * @author eddy
 */
public class VTMLBrComponent extends ModelMComponent {

    public VTMLBrComponent() {
        super();
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream divdata = new ByteArrayOutputStream();

            divdata.write(GetTeletelCode.setCursor(getParent().getX(), getParent().getY()));
            parent.setY(getParent().getY() + 1);

            return divdata.toByteArray();
        } catch (IOException ex) {
            System.getLogger(VTMLBrComponent.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return new byte[0];
    }
}
