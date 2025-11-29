package org.somanybits.minitel.components.vtml;

/**
 * Interface pour les composants qui peuvent recevoir le focus dans un formulaire.
 * Implémentée par VTMLMenuComponent et VTMLInputComponent.
 * 
 * @author eddy
 */
public interface Focusable {
    
    /**
     * Type de focus pour déterminer le comportement des touches
     */
    enum FocusType {
        /** Menu : attend une seule touche pour action immédiate */
        MENU,
        /** Input : accumule les touches jusqu'à Entrée */
        INPUT
    }
    
    /**
     * Retourne le type de focus de ce composant
     */
    FocusType getFocusType();
    
    /**
     * Retourne le nom/label du composant pour affichage ligne 0
     */
    String getFocusLabel();
    
    /**
     * Appelé quand le composant reçoit le focus
     * @return Bytes à envoyer au Minitel (ex: inversion vidéo)
     */
    byte[] onFocusGained();
    
    /**
     * Appelé quand le composant perd le focus
     * @return Bytes à envoyer au Minitel (ex: retour normal)
     */
    byte[] onFocusLost();
    
    /**
     * Position X du composant (pour le curseur)
     */
    int getX();
    
    /**
     * Position Y du composant (pour le curseur)
     */
    int getY();
}
