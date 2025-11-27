/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author eddy
 */
public class MFrame extends ModelMComponent {

    private boolean autoHeight = false;
    private ByteArrayOutputStream textFrame = new ByteArrayOutputStream();
    private int currentLine = 0;

    ArrayList<MComponent> clist = new ArrayList();

    public MFrame(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    public boolean isAutoHeight() {
        return autoHeight;
    }

    public void setAutoHeight(boolean autoHeight) {
        this.autoHeight = autoHeight;
    }

    public void addMComponent(MComponent mc) {
        mc.setParent(this);
        this.clist.add(mc);

    }

    @Override
    public void arrange() {
        this.currentLine = 0;
        this.textFrame = new ByteArrayOutputStream();

        for (int i = 0; i < clist.size(); i++) {
            MComponent cc = clist.get(i);
            try {
                textFrame.write(cc.getBytes());
                textFrame.write('\n');
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public byte[] getBytes() {
        return textFrame.toByteArray();
    }

}
