package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag &lt;refresh seconds="N"/&gt;.
 * Indique au client Minitel de recharger la page courante toutes les N secondes.
 * Ne produit aucun octet de rendu — son effet est enregistré sur la Page.
 */
public class VTMLRefreshComponent extends ModelMComponent {

    private final int seconds;

    public VTMLRefreshComponent(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() { return seconds; }

    @Override
    public byte[] getBytes() { return new byte[0]; }
}
