package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.Teletel;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant racine VTML représentant le tag <minitel>
 *
 * @author eddy
 */
public class VTMLMinitelComponent extends ModelMComponent {

    private String title;

    public VTMLMinitelComponent() {
        super();
    }

    public VTMLMinitelComponent(String title) {
        super();
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream divdata = new ByteArrayOutputStream();
            
            // Passer en mode texte (G0) et effacer l'écran
            divdata.write(GetTeletelCode.setMode(GetTeletelCode.MODE_TEXT));
            divdata.write(GetTeletelCode.clear());
            divdata.write(getTextContent().getBytes());
            //divdata.write(GetTeletelCode.clearLineZero());

     
            // Ajouter le contenu des enfants
            for (var child : getChilds()) {
                divdata.write(child.getBytes());
            }
            //divdata.write(GetTeletelCode.setEcho(true));
            return divdata.toByteArray();

        } catch (IOException ex) {
            System.getLogger(VTMLMinitelComponent.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return new byte[0];
    }
}
