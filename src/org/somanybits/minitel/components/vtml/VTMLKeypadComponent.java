package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Keypad pour mapper les touches du clavier aux actions de jeu.
 * 
 * Actions supportées : LEFT, RIGHT, UP, DOWN, ACTION1, ACTION2
 * 
 * @author eddy
 */
public class VTMLKeypadComponent extends ModelMComponent {
    
    public static final String ACTION_LEFT = "LEFT";
    public static final String ACTION_RIGHT = "RIGHT";
    public static final String ACTION_UP = "UP";
    public static final String ACTION_DOWN = "DOWN";
    public static final String ACTION_ACTION1 = "ACTION1";
    public static final String ACTION_ACTION2 = "ACTION2";
    
    private String action;  // LEFT, RIGHT, UP, DOWN, ACTION1, ACTION2
    private char key;       // Touche associée
    private String event;   // Nom de la fonction JavaScript à appeler
    
    public VTMLKeypadComponent(String action, char key, String event) {
        this.action = action != null ? action.toUpperCase() : null;
        this.key = key;
        this.event = event;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action != null ? action.toUpperCase() : null;
    }
    
    public char getKey() {
        return key;
    }
    
    public void setKey(char key) {
        this.key = key;
    }
    
    public String getEvent() {
        return event;
    }
    
    public void setEvent(String event) {
        this.event = event;
    }
    
    /**
     * Vérifie si l'action est valide
     */
    public static boolean isValidAction(String action) {
        if (action == null) return false;
        String upper = action.toUpperCase();
        return ACTION_LEFT.equals(upper) || 
               ACTION_RIGHT.equals(upper) || 
               ACTION_UP.equals(upper) || 
               ACTION_DOWN.equals(upper) || 
               ACTION_ACTION1.equals(upper) || 
               ACTION_ACTION2.equals(upper);
    }
    
    @Override
    public byte[] getBytes() {
        // Le keypad ne génère pas de bytes
        return new byte[0];
    }
}
