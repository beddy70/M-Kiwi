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
public class MList  extends ModelMComponent {

    ArrayList<MComponent> clist = new ArrayList();
    private int width;
    private int height;

    public MList(int witdh, int height) {
        setWidth(width);
        setHeight(height);
    }


    @Override
    public int getNumberLine() {
        int numLine = 0;
        for (int i = 0; i < clist.size(); i++) {
            numLine += clist.get(i).getNumberLine();
        }
        return numLine;
    }

}
