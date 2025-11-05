/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.events;

/**
 *
 * @author eddy
 */
public class KeyPressedEvent {

    static final public int KEY_SOMMAIRE = 0x1346;
    static final public int KEY_ANNULATION = 0x1345;
    static final public int KEY_RETOUR = 0x1342;
    static final public int KEY_REPETITION = 0x1343;
    static final public int KEY_GUIDE = 0x1344;
    static final public int KEY_CORRECTION = 0x1347;
    static final public int KEY_SUITE = 0x1348;
    static final public int KEY_ENVOI = 0x1341;
    static final public int KEY_CONNEXION_FIN = 0x1349;
    static final public int KEY_TELEPHONE = 0x135B;

    // Only in 80 Col mode
    static final public int KEY_UP = 0xC1;
    static final public int KEY_DOWN = 0xC2;
    static final public int KEY_LEFT = 0xC3;
    static final public int KEY_RIGHT = 0xC4;

    private int keycode = 0;

    static final public int TYPE_KEY_CHAR_EVENT = 0x01;
    static final public int TYPE_KEY_MENU_EVENT = 0x02;
    static final public int TYPE_KEY_DIRECTION_EVENT = 0x03;

    private int type = 0;

    public KeyPressedEvent(int keycode, int type) {
        this.keycode = keycode;
        this.type = type;
    }

    public int getKeyCode() {
        return this.keycode;
    }

    public int getType() {
        return this.type;
    }

}
