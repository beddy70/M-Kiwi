package org.somanybits.minitel.input;

/**
 * Interface pour recevoir les événements joystick
 */
public interface JoystickListener {
    
    /**
     * Appelé quand un bouton est pressé ou relâché
     * @param button Numéro du bouton (0-15)
     * @param pressed true si pressé, false si relâché
     */
    void onButton(int button, boolean pressed);
    
    /**
     * Appelé quand un axe change de valeur
     * @param axis Numéro de l'axe (0=X, 1=Y, etc.)
     * @param value Valeur (-32767 à 32767)
     */
    void onAxis(int axis, int value);
}
