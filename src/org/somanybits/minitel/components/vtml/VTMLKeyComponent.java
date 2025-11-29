package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <key>
 * Permet d'associer une touche de fonction à une URL
 * 
 * Exemple:
 * <key name="sommaire" link="index.vtml">
 * <key name="guide" link="aide.vtml">
 *
 * @author eddy
 */
public class VTMLKeyComponent extends ModelMComponent {

    public enum KeyName {
        SOMMAIRE,
        GUIDE,
        TELEPHONE
    }

    private KeyName keyName;
    private String link;

    public VTMLKeyComponent() {
        super();
    }

    public VTMLKeyComponent(KeyName keyName, String link) {
        super();
        this.keyName = keyName;
        this.link = link;
    }

    public KeyName getKeyName() {
        return keyName;
    }

    public void setKeyName(KeyName keyName) {
        this.keyName = keyName;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Parse le nom de touche depuis une chaîne
     */
    public static KeyName parseKeyName(String name) {
        if (name == null) return null;
        
        switch (name.toLowerCase()) {
            case "sommaire":
                return KeyName.SOMMAIRE;
            case "guide":
                return KeyName.GUIDE;
            case "telephone":
                return KeyName.TELEPHONE;
            default:
                return null;
        }
    }

    @Override
    public byte[] getBytes() {
        // Ce composant ne génère pas de données visuelles
        // Il sert uniquement à définir des associations touche -> URL
        return new byte[0];
    }
}
