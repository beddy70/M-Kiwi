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

            int parentWidth = getParent().getWidth();
            int budget = parentWidth > 0 ? parentWidth : Integer.MAX_VALUE;

            if (text != null && !text.isEmpty()) {
                String t = (budget < Integer.MAX_VALUE && text.length() > budget)
                        ? text.substring(0, budget) : text;
                budget = (budget == Integer.MAX_VALUE) ? Integer.MAX_VALUE : budget - t.length();
                divdata.write(t.getBytes("ISO-8859-1"));
            }

            for (var child : getChilds()) {
                if (budget <= 0) break;
                if (child instanceof VTMLColorComponent cc) {
                    divdata.write(cc.getBytesClipped(budget));
                    String ct = cc.getText();
                    if (ct != null && budget < Integer.MAX_VALUE)
                        budget -= Math.min(ct.length(), budget);
                } else if (child instanceof VTMLBlinkComponent bc) {
                    divdata.write(bc.getBytesClipped(budget));
                    String bt = bc.getText();
                    if (bt != null && budget < Integer.MAX_VALUE)
                        budget -= Math.min(bt.length(), budget);
                } else {
                    divdata.write(child.getBytes());
                }
            }

            return divdata.toByteArray();
        } catch (IOException ex) {
            System.getLogger(VTMLRowComponent.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return new byte[0];
    }

}
