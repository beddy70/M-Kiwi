package org.somanybits.minitel.components.vtml;

import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Une frame d'animation d'un sprite.
 * Contient les lignes de caractères qui composent cette frame,
 * ainsi que les couleurs optionnelles pour chaque caractère.
 * 
 * @author eddy
 */
public class VTMLSpriteComponent extends ModelMComponent {
    
    private List<String> rows = new ArrayList<>();
    private List<String> colorRows = new ArrayList<>();
    private char[][] data;
    private int[][] colorData;
    private boolean parsingColorsprite = false;
    
    public VTMLSpriteComponent() {
    }
    
    /**
     * Ajoute une ligne au sprite (caractères ou couleurs selon le mode)
     */
    public void addLine(String row) {
        if (parsingColorsprite) {
            colorRows.add(row);
            colorData = null; // Invalider le cache
        } else {
            rows.add(row);
            data = null; // Invalider le cache
        }
    }
    
    /**
     * Active/désactive le mode parsing colorsprite
     */
    public void setParsingColorsprite(boolean parsing) {
        this.parsingColorsprite = parsing;
    }
    
    /**
     * Retourne si on est en mode parsing colorsprite
     */
    public boolean isParsingColorsprite() {
        return parsingColorsprite;
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
    
    /**
     * Retourne les données de couleur sous forme de tableau 2D
     * @return tableau de codes couleur (0-7), -1 = couleur par défaut du sprite
     */
    public int[][] getColorData() {
        if (colorData == null) {
            buildColorData();
        }
        return colorData;
    }
    
    /**
     * Vérifie si ce sprite a des couleurs personnalisées
     */
    public boolean hasColorData() {
        return !colorRows.isEmpty();
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
    
    private void buildColorData() {
        if (colorRows.isEmpty()) {
            colorData = new int[0][0];
            return;
        }
        
        // Utiliser les mêmes dimensions que les données
        char[][] charData = getData();
        int height = charData.length;
        int width = height > 0 ? charData[0].length : 0;
        
        colorData = new int[height][width];
        
        // Initialiser à -1 (couleur par défaut)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                colorData[y][x] = -1;
            }
        }
        
        // Remplir avec les couleurs définies
        for (int y = 0; y < colorRows.size() && y < height; y++) {
            String row = colorRows.get(y);
            for (int x = 0; x < row.length() && x < width; x++) {
                char c = row.charAt(x);
                if (c >= '0' && c <= '7') {
                    colorData[y][x] = c - '0';
                }
                // Espace ou autre = -1 (couleur par défaut du sprite)
            }
        }
    }
    
    @Override
    public byte[] getBytes() {
        // Le sprite ne génère pas de bytes directement
        return new byte[0];
    }
}
