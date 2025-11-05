/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.somanybits.minitel.components;

import java.io.IOException;
import org.somanybits.minitel.Teletel;

/**
 *
 * @author eddy
 */
interface PageMinitel {

    public void clear();

    public void drawToPage(Teletel t, int posx, int posy) throws IOException;

    public void drawToPage(Teletel t) throws IOException;
    
    public int getNumberLine();

}
