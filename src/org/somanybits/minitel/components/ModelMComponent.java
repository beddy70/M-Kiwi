/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

/**
 *
 * @author eddy
 */
public class ModelMComponent implements MComponent {
    

    private int width;
    private int height;
    private int x;
    private int y;
    protected MComponent parent;

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getNumberLine() {
        return 1;
    }

    @Override
    public MComponent getParent() {
       return parent;
    }

    public void setParent(MComponent parent) {
        this.parent = parent;
    }

    @Override
    public void arrange() {
        
    }

    @Override
    public String getString() {
        return "";
    }

}
