/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

/**
 *
 * @author eddy
 */
public class MTitle extends ModelMComponent {

    public static final int TILTE_NUMBER_LINE[] = {2, 1, 1, 1};

    private String title;
    private int level;

    public MTitle(String title, int level) {
        this.title = title;
        this.level = level;
    }

    @Override
    public int getNumberLine() {
        return TILTE_NUMBER_LINE[level];
    }

    @Override
    public String getString() {
  
        return (title.length()<getParent().getWidth()?title:title.substring(0,getParent().getWidth()-1));
    }

}
