package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <row>
 * Contient une ligne de texte (implique un saut de ligne après)
 *
 * @author eddy
 */
public class VTMLRowComponent extends ModelMComponent {

    private String text;
    private int repeat = 1;  // Pour les rows avec putchar

    public VTMLRowComponent() {
        super();
    }

    public VTMLRowComponent(String text) {
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public int getRepeat() {
        return repeat;
    }
    
    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream divdata = new ByteArrayOutputStream();

            setX(getParent().getX());
            setY(getParent().getY());
            divdata.write(GetTeletelCode.setCursor(getParent().getX(), getParent().getY()));
            parent.setY(getParent().getY() + 1);
            
            divdata.write(text.getBytes());

            return divdata.toByteArray();
        } catch (IOException ex) {
            System.getLogger(VTMLRowComponent.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return new byte[0];
    }

}
