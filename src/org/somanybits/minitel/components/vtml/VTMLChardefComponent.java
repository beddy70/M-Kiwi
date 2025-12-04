/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML pour définir un jeu de caractères mosaïques personnalisés.
 * <p>
 * Permet de définir des caractères bitmap 2x3 pixels (format mosaïque Minitel)
 * qui peuvent ensuite être utilisés dans les maps via le tag {@code <putchar>}.
 * </p>
 * 
 * <h2>Syntaxe VTML</h2>
 * <pre>{@code
 * <chardef name="mycharset" type="mosaic">
 *   <char>
 *     <line>##</line>
 *     <line># </line>
 *     <line>##</line>
 *   </char>
 *   <char>
 *     <line>  </line>
 *     <line>##</line>
 *     <line>  </line>
 *   </char>
 * </chardef>
 * }</pre>
 * 
 * <h2>Utilisation dans une map</h2>
 * <pre>{@code
 * <map>
 *   <row><putchar index="0" repeat="40"/></row>
 * </map>
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.3
 */
public class VTMLChardefComponent extends ModelMComponent {
    
    private String charsetName;
    private String type = "mosaic";  // Pour l'instant, seul "mosaic" est supporté
    private List<Character> chars = new ArrayList<>();
    
    // Données temporaires pour le parsing du <char> courant
    private List<String> currentCharLines = new ArrayList<>();
    private boolean parsingChar = false;
    
    public VTMLChardefComponent(String name, String type) {
        this.charsetName = name;
        if (type != null) {
            this.type = type;
        }
    }
    
    public String getCharsetName() {
        return charsetName;
    }
    
    public String getType() {
        return type;
    }
    
    /**
     * Démarre le parsing d'un nouveau caractère
     */
    public void startChar() {
        currentCharLines.clear();
        parsingChar = true;
    }
    
    /**
     * Ajoute une ligne au caractère en cours de parsing
     */
    public void addLine(String line) {
        if (parsingChar) {
            currentCharLines.add(line);
        }
    }
    
    /**
     * Termine le parsing du caractère courant et le convertit en mosaïque
     */
    public void endChar() {
        if (parsingChar && !currentCharLines.isEmpty()) {
            char mosaicChar = convertToMosaic(currentCharLines);
            chars.add(mosaicChar);
            System.out.println("🎨 Chardef '" + charsetName + "': ajout char #" + (chars.size() - 1) + " = 0x" + Integer.toHexString(mosaicChar));
        }
        currentCharLines.clear();
        parsingChar = false;
    }
    
    /**
     * Retourne le caractère mosaïque à l'index donné
     * @param index Index du caractère (0-based)
     * @return Le caractère mosaïque, ou espace si index invalide
     */
    public char getChar(int index) {
        if (index >= 0 && index < chars.size()) {
            return chars.get(index);
        }
        return ' ';
    }
    
    /**
     * Retourne le nombre de caractères définis
     */
    public int getCharCount() {
        return chars.size();
    }
    
    /**
     * Convertit un bitmap 2x3 en caractère mosaïque Minitel
     * <p>
     * Encodage semi-graphique Minitel :
     * <pre>
     * Bit 0 (0x01) = pixel haut-gauche
     * Bit 1 (0x02) = pixel haut-droite
     * Bit 2 (0x04) = pixel milieu-gauche
     * Bit 3 (0x08) = pixel milieu-droite
     * Bit 4 (0x10) = pixel bas-gauche
     * Bit 6 (0x40) = pixel bas-droite
     * Bit 5 (0x20) = toujours à 1 pour les caractères mosaïques
     * </pre>
     */
    private char convertToMosaic(List<String> lines) {
        int semigfx = 0;
        
        // Ligne 0 (haut)
        if (lines.size() > 0) {
            String line = lines.get(0);
            if (getPixel(line, 0)) semigfx |= 0b0100001;  // bit 0 + bit 5
            if (getPixel(line, 1)) semigfx |= 0b0100010;  // bit 1 + bit 5
        }
        
        // Ligne 1 (milieu)
        if (lines.size() > 1) {
            String line = lines.get(1);
            if (getPixel(line, 0)) semigfx |= 0b0100100;  // bit 2 + bit 5
            if (getPixel(line, 1)) semigfx |= 0b0101000;  // bit 3 + bit 5
        }
        
        // Ligne 2 (bas)
        if (lines.size() > 2) {
            String line = lines.get(2);
            if (getPixel(line, 0)) semigfx |= 0b0110000;  // bit 4 + bit 5
            if (getPixel(line, 1)) semigfx |= 0b1100000;  // bit 6 + bit 5
        }
        
        // Cas spéciaux
        if (semigfx == 0b1111111) {
            semigfx = 0b1011111;  // Tous pixels allumés = exception Minitel
        } else if (semigfx == 0) {
            semigfx = 0x20;  // Espace mosaïque (vide mais en mode semi-graphique)
        }
        
        return (char) semigfx;
    }
    
    /**
     * Lit un pixel dans une ligne (# = true, autre = false)
     */
    private boolean getPixel(String line, int x) {
        if (x >= line.length()) {
            return false;
        }
        return line.charAt(x) == '#';
    }
}
