/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author eddy
 */
public class Page {

    static final public int MODE_40_COL = 0;
    static final public int MODE_80_COL = 1;

    private Map<String, String> keylinklist;

    private int mode;
    private String title;

    private ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);

    public Page(int mode) {
        this.mode = mode;
        keylinklist = new LinkedHashMap<>();
    }

    public void setTile(String title) {
        this.title = title;

    }

    public String getTitle() {
        return title;
    }

    public void addData(byte[] data) throws IOException {
        buf.write(data);
    }

    public byte[] getData() {
        return buf.toByteArray();
    }

    public String getLink(String key) {
        return keylinklist.get((String) key);
    }

    public void addMenu(String key, String link) {
        keylinklist.put(key, link);
    }

}
