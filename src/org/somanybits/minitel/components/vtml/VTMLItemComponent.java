package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <item>
 * Inclus dans un tag <menu> avec paramètres link (url) et label
 *
 * @author eddy
 */
public class VTMLItemComponent extends ModelMComponent {

    private String label;
    private String link;

    public VTMLItemComponent() {
        super();
    }

    public VTMLItemComponent(String label, String link) {
        super();
        this.label = label;
        this.link = link;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream divdata = new ByteArrayOutputStream();

             setX(getParent().getX());
            setY(getParent().getY());
            divdata.write(GetTeletelCode.setCursor(getParent().getX(), getParent().getY()));
            parent.setY(getParent().getY() + 1);

            if (getParent() instanceof VTMLMenuComponent) {
                VTMLMenuComponent pmenu = (VTMLMenuComponent) getParent();
                divdata.write((pmenu.getKeyCounter() + " " + getLabel()).getBytes());
            } else {
               divdata.write((getLabel()).getBytes());
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
