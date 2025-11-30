package org.somanybits.minitel.components.vtml;

import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Map pour les layers.
 * Représente une zone de fond avec du contenu texte ou bitmap.
 * 
 * @author eddy
 */
public class VTMLMapComponent extends ModelMComponent {
    
    public enum MapType {
        CHAR,   // Caractères normaux
        BITMAP  // Mode semi-graphique (# = 1, espace = 0)
    }
    
    private MapType type = MapType.CHAR;
    private List<String> rows = new ArrayList<>();
    private char[][] data;
    
    public VTMLMapComponent(MapType type) {
        this.type = type;
    }
    
    public MapType getType() {
        return type;
    }
    
    public void setType(MapType type) {
        this.type = type;
    }
    
    /**
     * Ajoute une ligne de contenu
     */
    public void addRow(String row) {
        rows.add(row);
        data = null; // Invalider le cache
    }
    
    /**
     * Retourne les données sous forme de tableau 2D
     */
    public char[][] getData() {
        if (data == null) {
            buildData();
        }
        return data;
    }
    
    private void buildData() {
        if (rows.isEmpty()) {
            data = new char[0][0];
            return;
        }
        
        // Trouver la largeur max
        int maxWidth = 0;
        for (String row : rows) {
            if (row.length() > maxWidth) {
                maxWidth = row.length();
            }
        }
        
        data = new char[rows.size()][maxWidth];
        
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            for (int x = 0; x < maxWidth; x++) {
                if (x < row.length()) {
                    data[y][x] = row.charAt(x);
                } else {
                    data[y][x] = ' ';
                }
            }
        }
    }
    
    /**
     * Modifie un caractère à une position donnée
     */
    public void setChar(int x, int y, char c) {
        if (data == null) {
            buildData();
        }
        if (y >= 0 && y < data.length && x >= 0 && x < data[y].length) {
            data[y][x] = c;
        }
    }
    
    /**
     * Récupère un caractère à une position donnée
     */
    public char getChar(int x, int y) {
        if (data == null) {
            buildData();
        }
        if (y >= 0 && y < data.length && x >= 0 && x < data[y].length) {
            return data[y][x];
        }
        return ' ';
    }
    
    @Override
    public byte[] getBytes() {
        // L'area ne génère pas de bytes directement, c'est le layers qui gère le rendu
        return new byte[0];
    }
}
