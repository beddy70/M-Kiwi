/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

/**
 *
 * @author eddy
 */
public class PageManager {

    public static final int MODE_40X25_WIDTH = 40;
    public static final int MODE_40X25_HEIGHT = 25;
    
    int width = MODE_40X25_WIDTH;
    int height = MODE_40X25_HEIGHT;

    private static PageManager pmgr = null;

    public PageManager(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public PageManager() {
    }

    public static PageManager getInstance() {
        if (pmgr == null) {
            pmgr = new PageManager();
        }
        return pmgr;
    }

    int getWidth() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}

