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
    private String url;

    private ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);

    public Page(int mode) {
        this.mode = mode;
        keylinklist = new LinkedHashMap<>();
    }

    public Page(int mode, String url) {
        this(mode);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Réinitialise les données de la page (pour reload)
     */
    public void reset() {
        buf.reset();
        keylinklist.clear();
    }

    public void setTile(String title) {
        this.title = title;

    }

    public String getTitle() {
        return title;
    }

    public void addData(byte[] data) throws IOException {
        // Affichage hexadécimal sur la console (style hex editor)
        System.out.println("=== Page.addData (" + data.length + " bytes) ===");
        
        for (int offset = 0; offset < data.length; offset += 16) {
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();
            
            for (int i = 0; i < 16; i++) {
                if (offset + i < data.length) {
                    int b = data[offset + i] & 0xFF;
                    // Partie hex
                    if (b == 0x1B) {
                        // ESC en rouge
                        hexPart.append("\u001B[31m").append(String.format("%02X ", b)).append("\u001B[0m");
                    } else if (b == 0x1F) {
                        // Cursor position en cyan
                        hexPart.append("\u001B[36m").append(String.format("%02X ", b)).append("\u001B[0m");
                    } else {
                        hexPart.append(String.format("%02X ", b));
                    }
                    // Partie ASCII (affichable: 0x20-0x7E)
                    if (b >= 0x20 && b <= 0x7E) {
                        asciiPart.append((char) b);
                    } else {
                        asciiPart.append('.');
                    }
                } else {
                    hexPart.append("   ");
                    asciiPart.append(' ');
                }
            }
            
            System.out.printf("%08X  %s |%s|\n", offset, hexPart.toString(), asciiPart.toString());
        }
        
        System.out.println("================================");
        
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

    // Associations touches de fonction -> URL
    private Map<String, String> functionKeyLinks = new LinkedHashMap<>();

    /**
     * Associe une touche de fonction à une URL
     * @param keyName Nom de la touche (sommaire, guide)
     * @param link URL cible
     */
    public void addFunctionKey(String keyName, String link) {
        functionKeyLinks.put(keyName.toUpperCase(), link);
    }

    /**
     * Récupère l'URL associée à une touche de fonction
     * @param keyName Nom de la touche (SOMMAIRE, GUIDE)
     * @return URL ou null si non définie
     */
    public String getFunctionKeyLink(String keyName) {
        return functionKeyLinks.get(keyName.toUpperCase());
    }

    /**
     * Vérifie si une touche de fonction a une URL associée
     */
    public boolean hasFunctionKey(String keyName) {
        return functionKeyLinks.containsKey(keyName.toUpperCase());
    }

    /**
     * Hérite des touches de fonction d'une page précédente
     * Les touches déjà définies dans cette page ne sont pas écrasées
     * @param previousPage Page dont on hérite les touches
     */
    public void inheritFunctionKeys(Page previousPage) {
        if (previousPage == null) return;
        
        // Pour chaque touche de la page précédente
        for (Map.Entry<String, String> entry : previousPage.functionKeyLinks.entrySet()) {
            String keyName = entry.getKey();
            // Hériter seulement si cette page n'a pas redéfini la touche
            if (!functionKeyLinks.containsKey(keyName)) {
                functionKeyLinks.put(keyName, entry.getValue());
            }
        }
    }

}

