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
    private boolean visible = true;  // Visibilité du composant (par défaut visible)
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
    
    /**
     * Alias pour addChild - compatibilité DOM JavaScript
     * @param child Le composant enfant à ajouter
     */
    public void appendChild(MComponent child) {
        addChild(child);
    }
    
    /**
     * Crée un élément VTML et l'ajoute comme enfant - API DOM JavaScript
     * @param tagName Le nom du tag (row, br, div, color, blink, label)
     * @return Le composant créé, ou null si le type est inconnu
     */
    public MComponent createElement(String tagName) {
        MComponent child = org.somanybits.minitel.components.vtml.VTMLFactory.create(tagName);
        if (child != null) {
            addChild(child);
        }
        return child;
    }
    
    /**
     * Retire un enfant du composant - API DOM JavaScript
     * @param child Le composant enfant à retirer
     * @return true si l'enfant a été retiré, false sinon
     */
    public boolean removeChild(MComponent child) {
        if (child != null && childs.remove(child)) {
            child.setParent(null);
            return true;
        }
        return false;
    }
    
    /**
     * Retire tous les enfants du composant - API DOM JavaScript
     */
    public void clearChildren() {
        for (MComponent child : childs) {
            child.setParent(null);
        }
        childs.clear();
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
    
    /**
     * Retourne la visibilité du composant
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Définit la visibilité du composant
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
