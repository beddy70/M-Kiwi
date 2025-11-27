package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <menu>
 * Comme div avec paramètre 'name' et 'keytype' (number/alpha)
 *
 * @author eddy
 */
public class VTMLMenuComponent extends ModelMComponent {

    public enum KeyType {
        NUMBER, ALPHA
    }

    private String name;
    private KeyType keyType;

    private String keyCounter="A";

    public VTMLMenuComponent() {
        super();
        this.keyType = KeyType.NUMBER; // Défaut
    }

    public VTMLMenuComponent(String name, KeyType keyType, int left, int top) {
        super();
        this.name = name;
        this.keyType = keyType;
        setX(left);
        setY(top);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    @Override
    public byte[] getBytes() {
        boolean iteminitialized = false;

        try {
            ByteArrayOutputStream divdata = new ByteArrayOutputStream();

            divdata.write(GetTeletelCode.setCursor(getX(), getY()));

            // Ajouter le contenu des enfants
            for (MComponent child : getChilds()) {

                if (child instanceof VTMLItemComponent && iteminitialized == false) {
                    //VTMLItemComponent item = (VTMLItemComponent) child;
                    iteminitialized = true;
                    switch (keyType) {
                        case ALPHA -> {
                            this.keyCounter = "A";
                        }
                        case NUMBER -> {
                            this.keyCounter = "1";
                        }
                    }
                }

                divdata.write(child.getBytes());

            }

            return divdata.toByteArray();

        } catch (IOException ex) {
            System.getLogger(VTMLDivComponent.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return new byte[0];

    }

    public String getKeyCounter() {
        String curKeyCounter = keyCounter;
        keyCounter = incrementAlphaNum(keyCounter);
        return curKeyCounter;
    }

    public static String incrementAlphaNum(String value) {
        if (value == null || value.length() != 1) {
            throw new IllegalArgumentException("La valeur doit être un caractère unique.");
        }

        char c = value.charAt(0);

        // Cas lettres A-Z
        if (c >= 'A' && c <= 'Z') {
            if (c == 'Z') {
                return null;       // ou retourner "A" si tu veux boucler
            }
            return String.valueOf((char) (c + 1));
        }

        // Cas chiffres 0-9
        if (c >= '0' && c <= '9') {
            if (c == '9') {
                return null;       // ou retourner "0" si tu veux boucler
            }
            return String.valueOf((char) (c + 1));
        }

        // Tout le reste est invalide
        throw new IllegalArgumentException("Caractère non pris en charge : " + value);
    }
}
