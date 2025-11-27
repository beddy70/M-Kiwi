/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

/**
 *
 * @author eddy
 */
public interface MComponent {

    public int getNumberLine();

    public int getWidth();

    public void setWidth(int width);

    public int getHeight();

    public void setHeight(int height);

    public int getX();

    public void setX(int x);

    public int getY();

    public void setY(int y);

    public MComponent getParent();

    public String getTextContent();

    public void setTextContent(String textContent);

    public void arrange();

    public byte[] getBytes();

    public void setParent(MComponent parent);

    public void addChild(MComponent child);
}
