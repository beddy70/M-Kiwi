package org.somanybits.minitel.components.vtml;

import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Une frame d'animation d'un sprite.
 * Contient les lignes de caractères qui composent cette frame.
 * 
 * @author eddy
 */
public class VTMLSpriteComponent extends ModelMComponent {
    
    private List<String> rows = new ArrayList<>();
    private char[][] data;
    
    public VTMLSpriteComponent() {
    }
    
    /**
     * Ajoute une ligne au sprite
     */
    public void addLine(String row) {
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
    
    @Override
    public byte[] getBytes() {
        // Le sprite ne génère pas de bytes directement
        return new byte[0];
    }
}
