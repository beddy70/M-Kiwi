/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

import java.util.ArrayList;

/**
 *
 * @author eddy
 */
public class ModelMComponent implements MComponent {

    private int width;
    private int height;
    private int x;
    private int y;
    private String textContent;
    private String id;
    private String name;
    protected MComponent parent;
    protected ArrayList<MComponent> childs = new ArrayList<MComponent>();
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

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

    @Override
    public void setParent(MComponent parent) {
        this.parent = parent;
    }

    @Override
    public void arrange() {

    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public void addChild(MComponent child) {
        childs.add(child);
        child.setParent(this);
    }

    public ArrayList<MComponent> getChilds() {
        return childs;
    }

    @Override
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    @Override
    public String getTextContent() {
        return textContent;
    }

}
