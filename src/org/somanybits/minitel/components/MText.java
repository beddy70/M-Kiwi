/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.components;

import java.math.BigDecimal;

/**
 *
 * @author eddy
 */
public class MText extends ModelMComponent {

    private String text;

    public MText(String text) {
        this.text = text;

    }

    @Override
    public int getNumberLine() {

        BigDecimal line= BigDecimal.valueOf(((double)text.length()) / ((double)getParent().getWidth()));
        return line.intValue();

    }
    @Override
    public byte[] getBytes(){
        return text.getBytes();
    }

}
