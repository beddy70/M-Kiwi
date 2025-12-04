/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Keypad pour mapper les touches du clavier aux actions de jeu.
 * <p>
 * Ce composant permet d'associer des touches du clavier Minitel à des
 * fonctions JavaScript. Il supporte deux modes d'utilisation :
 * </p>
 * 
 * <h2>Mode action (compatible joystick)</h2>
 * <pre>{@code
 * <keypad action="UP" key="Z" event="moveUp"/>
 * <keypad action="ACTION1" key=" " event="fire"/>
 * <keypad action="UP" key="O" event="moveUp2" player="1"/>
 * }</pre>
 * <p>Actions disponibles : UP, DOWN, LEFT, RIGHT, ACTION1, ACTION2, ACTION3, ACTION4, ACTION5, ACTION6</p>
 * <p>Le paramètre player (0 ou 1) permet d'associer le keypad à un joueur spécifique
 * pour le support multi-manettes.</p>
 * 
 * <h2>Mode touche directe</h2>
 * <pre>{@code
 * <keypad key="1" event="selectOption1"/>
 * <keypad key="P" event="pauseGame"/>
 * }</pre>
 * <p>Note : Les touches directes ne sont pas accessibles via joystick.</p>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see VTMLLayersComponent
 */
public class VTMLKeypadComponent extends ModelMComponent {
    
    public static final String ACTION_LEFT = "LEFT";
    public static final String ACTION_RIGHT = "RIGHT";
    public static final String ACTION_UP = "UP";
    public static final String ACTION_DOWN = "DOWN";
    public static final String ACTION_ACTION1 = "ACTION1";
    public static final String ACTION_ACTION2 = "ACTION2";
    public static final String ACTION_ACTION3 = "ACTION3";
    public static final String ACTION_ACTION4 = "ACTION4";
    public static final String ACTION_ACTION5 = "ACTION5";
    public static final String ACTION_ACTION6 = "ACTION6";
    
    private String action;  // LEFT, RIGHT, UP, DOWN, ACTION1-ACTION6
    private char key;       // Touche associée
    private String event;   // Nom de la fonction JavaScript à appeler
    private int player;     // 0 = joueur 1, 1 = joueur 2
    
    public VTMLKeypadComponent(String action, char key, String event) {
        this(action, key, event, 0);
    }
    
    public VTMLKeypadComponent(String action, char key, String event, int player) {
        this.action = action != null ? action.toUpperCase() : null;
        this.key = key;
        this.event = event;
        this.player = player;
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
    
    public int getPlayer() {
        return player;
    }
    
    public void setPlayer(int player) {
        this.player = player;
    }
    
    /**
     * Vérifie si l'action est valide (action de jeu prédéfinie)
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
    
    /**
     * Vérifie si ce keypad a une action de jeu (vs touche directe)
     */
    public boolean hasAction() {
        return action != null && !action.isEmpty();
    }
    
    /**
     * Vérifie si ce keypad est une touche directe (sans action de jeu)
     */
    public boolean isDirectKey() {
        return !hasAction() && event != null;
    }
    
    @Override
    public byte[] getBytes() {
        // Le keypad ne génère pas de bytes
        return new byte[0];
    }
}
