/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Map pour les décors de jeu.
 * <p>
 * Représente une couche de fond dans un {@link VTMLLayersComponent}.
 * Les maps sont empilées et les caractères espace sont transparents,
 * permettant de voir les couches inférieures.
 * </p>
 * 
 * <h2>Types de map</h2>
 * <ul>
 *   <li>{@link MapType#CHAR} - Caractères normaux affichés tels quels</li>
 *   <li>{@link MapType#BITMAP} - Mode semi-graphique (# = pixel allumé)</li>
 * </ul>
 * 
 * <h2>Colormap</h2>
 * <p>
 * Chaque map peut avoir une colormap associée qui définit la couleur du texte (ink)
 * pour chaque caractère. La colormap est définie avec le tag {@code <colormap>}.
 * Les couleurs sont représentées par des caractères :
 * </p>
 * <ul>
 *   <li>' ' (espace) = blanc (7)</li>
 *   <li>'0' = noir</li>
 *   <li>'1' = rouge</li>
 *   <li>'2' = vert</li>
 *   <li>'3' = jaune</li>
 *   <li>'4' = bleu</li>
 *   <li>'5' = magenta</li>
 *   <li>'6' = cyan</li>
 *   <li>'7' = blanc</li>
 * </ul>
 * 
 * <h2>Exemple VTML</h2>
 * <pre>{@code
 * <map type="char">
 *   <row>########################################</row>
 *   <row>#                                      #</row>
 *   <row>########################################</row>
 *   <colormap>
 *     <row>1111111111111111111111111111111111111111</row>
 *     <row>7                                      7</row>
 *     <row>2222222222222222222222222222222222222222</row>
 *   </colormap>
 * </map>
 * }</pre>
 * 
 * <h2>Modification dynamique</h2>
 * <pre>{@code
 * layers.setMapChar(0, x, y, '#');   // Modifier un caractère
 * layers.setMapColor(0, x, y, 1);    // Modifier la couleur (1=rouge)
 * layers.getMapColor(0, x, y);       // Lire la couleur
 * layers.clearMapLine(0, y);         // Effacer une ligne
 * layers.shiftMap(0, "DOWN", 0, 10); // Décaler vers le bas
 * layers.shiftMap(0, "LEFT", 0, 39); // Décaler vers la gauche
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.4
 * @see VTMLLayersComponent
 */
public class VTMLMapComponent extends ModelMComponent {
    
    public enum MapType {
        CHAR,   // Caractères normaux
        BITMAP  // Mode semi-graphique (# = 1, espace = 0)
    }
    
    private MapType type = MapType.CHAR;
    private List<String> rows = new ArrayList<>();
    private List<String> colorRows = new ArrayList<>();
    private List<boolean[]> mosaicRows = new ArrayList<>();  // Flags mosaïque par ligne
    private char[][] data;
    private int[][] colorData;  // Couleur du texte (ink) pour chaque caractère
    private boolean[][] mosaicData;  // true = caractère mosaïque
    private boolean parsingColormap = false;  // Flag pour savoir si on parse une colormap
    
    // Buffer pour accumuler les caractères d'une ligne en cours (pour <putchar>)
    private StringBuilder currentRowBuffer = null;
    private StringBuilder currentRowMosaicFlags = null;  // '1' = mosaïque, '0' = normal
    
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
        addRowWithMosaic(row, null);
    }
    
    /**
     * Ajoute une ligne avec flags mosaïque
     */
    private void addRowWithMosaic(String row, boolean[] mosaic) {
        if (parsingColormap) {
            colorRows.add(row);
        } else {
            rows.add(row);
            mosaicRows.add(mosaic);  // null = pas de mosaïque
        }
        data = null; // Invalider le cache
        colorData = null;
        mosaicData = null;
    }
    
    /**
     * Démarre une nouvelle ligne (pour accumulation avec putchar)
     */
    public void startRow() {
        currentRowBuffer = new StringBuilder();
        currentRowMosaicFlags = new StringBuilder();
    }
    
    /**
     * Ajoute des caractères mosaïques à la ligne en cours
     */
    public void appendMosaicChars(String chars) {
        if (currentRowBuffer == null) {
            currentRowBuffer = new StringBuilder();
            currentRowMosaicFlags = new StringBuilder();
        }
        System.out.println("🎨 appendMosaicChars: '" + chars + "' (len=" + chars.length() + ")");
        currentRowBuffer.append(chars);
        // Marquer tous ces caractères comme mosaïques
        for (int i = 0; i < chars.length(); i++) {
            currentRowMosaicFlags.append('1');
        }
        System.out.println("🎨 currentRowBuffer now: len=" + currentRowBuffer.length());
    }
    
    /**
     * Ajoute des caractères normaux (non-mosaïques) à la ligne en cours
     */
    public void appendTextChars(String chars) {
        if (currentRowBuffer == null) {
            currentRowBuffer = new StringBuilder();
            currentRowMosaicFlags = new StringBuilder();
        }
        System.out.println("📝 appendTextChars: '" + chars + "' (len=" + chars.length() + ")");
        currentRowBuffer.append(chars);
        // Marquer tous ces caractères comme normaux
        for (int i = 0; i < chars.length(); i++) {
            currentRowMosaicFlags.append('0');
        }
    }
    
    /**
     * Termine la ligne en cours et l'ajoute à la map
     */
    public void endRow(int repeat) {
        if (currentRowBuffer != null && currentRowBuffer.length() > 0) {
            String row = currentRowBuffer.toString();
            // Convertir les flags en tableau boolean
            boolean[] mosaic = new boolean[currentRowMosaicFlags.length()];
            for (int i = 0; i < mosaic.length; i++) {
                mosaic[i] = currentRowMosaicFlags.charAt(i) == '1';
            }
            for (int i = 0; i < repeat; i++) {
                addRowWithMosaic(row, mosaic);
            }
            currentRowBuffer = null;
            currentRowMosaicFlags = null;
        }
    }
    
    /**
     * Active/désactive le mode parsing colormap
     */
    public void setParsingColormap(boolean parsing) {
        this.parsingColormap = parsing;
    }
    
    /**
     * Vérifie si on parse actuellement une colormap
     */
    public boolean isParsingColormap() {
        return parsingColormap;
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
     * Les valeurs sont les codes couleur Minitel (0-7)
     */
    public int[][] getColorData() {
        if (colorData == null) {
            buildData();
        }
        return colorData;
    }
    
    /**
     * Retourne les flags mosaïque sous forme de tableau 2D
     * true = caractère mosaïque (nécessite mode semi-graphique)
     */
    public boolean[][] getMosaicData() {
        if (mosaicData == null) {
            buildData();
        }
        return mosaicData;
    }
    
    private void buildData() {
        if (rows.isEmpty()) {
            data = new char[0][0];
            colorData = new int[0][0];
            mosaicData = new boolean[0][0];
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
        colorData = new int[rows.size()][maxWidth];
        mosaicData = new boolean[rows.size()][maxWidth];
        
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            boolean[] rowMosaic = (y < mosaicRows.size()) ? mosaicRows.get(y) : null;
            
            for (int x = 0; x < maxWidth; x++) {
                if (x < row.length()) {
                    data[y][x] = row.charAt(x);
                    // Appliquer le flag mosaïque si défini
                    mosaicData[y][x] = (rowMosaic != null && x < rowMosaic.length && rowMosaic[x]);
                } else {
                    data[y][x] = ' ';
                    mosaicData[y][x] = false;
                }
                // -1 = pas de couleur définie (sera ignoré au rendu)
                colorData[y][x] = -1;
            }
        }
        
        // Appliquer les couleurs de la colormap si définie
        for (int y = 0; y < colorRows.size() && y < colorData.length; y++) {
            String colorRow = colorRows.get(y);
            for (int x = 0; x < colorRow.length() && x < colorData[y].length; x++) {
                colorData[y][x] = parseColorChar(colorRow.charAt(x));
            }
        }
    }
    
    /**
     * Convertit un caractère de colormap en code couleur Minitel
     * @return Code couleur 0-7, ou -1 si espace (pas de couleur)
     */
    private int parseColorChar(char c) {
        if (c >= '0' && c <= '7') {
            return c - '0';
        }
        // Espace = pas de couleur définie
        return -1;
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
    
    /**
     * Modifie la couleur du texte à une position donnée
     * @param x Position X
     * @param y Position Y
     * @param color Code couleur Minitel (0-7)
     */
    public void setColor(int x, int y, int color) {
        if (colorData == null) {
            buildData();
        }
        if (y >= 0 && y < colorData.length && x >= 0 && x < colorData[y].length) {
            colorData[y][x] = color & 0x07;  // Limiter à 0-7
        }
    }
    
    /**
     * Récupère la couleur du texte à une position donnée
     * @param x Position X
     * @param y Position Y
     * @return Code couleur Minitel (0-7), 7 (blanc) par défaut
     */
    public int getColor(int x, int y) {
        if (colorData == null) {
            buildData();
        }
        if (y >= 0 && y < colorData.length && x >= 0 && x < colorData[y].length) {
            return colorData[y][x];
        }
        return 7;  // Blanc par défaut
    }
    
    /**
     * Efface une ligne (caractères et couleurs)
     */
    public void clearLine(int y) {
        if (data == null) {
            buildData();
        }
        if (y >= 0 && y < data.length) {
            for (int x = 0; x < data[y].length; x++) {
                data[y][x] = ' ';
                colorData[y][x] = -1;  // Pas de couleur
            }
        }
    }
    
    /**
     * Décale les lignes vers le bas (caractères et couleurs)
     */
    public void shiftDown(int fromY, int toY) {
        if (data == null) {
            buildData();
        }
        for (int y = toY; y > fromY; y--) {
            if (y >= 0 && y < data.length && y-1 >= 0) {
                System.arraycopy(data[y-1], 0, data[y], 0, data[y].length);
                System.arraycopy(colorData[y-1], 0, colorData[y], 0, colorData[y].length);
            }
        }
        // Vider la ligne du haut
        if (fromY >= 0 && fromY < data.length) {
            for (int x = 0; x < data[fromY].length; x++) {
                data[fromY][x] = ' ';
                colorData[fromY][x] = -1;  // Pas de couleur
            }
        }
    }
    
    /**
     * Décale les lignes vers le haut (caractères et couleurs)
     */
    public void shiftUp(int fromY, int toY) {
        if (data == null) {
            buildData();
        }
        for (int y = fromY; y < toY; y++) {
            if (y >= 0 && y < data.length && y+1 < data.length) {
                System.arraycopy(data[y+1], 0, data[y], 0, data[y].length);
                System.arraycopy(colorData[y+1], 0, colorData[y], 0, colorData[y].length);
            }
        }
        // Vider la ligne du bas
        if (toY >= 0 && toY < data.length) {
            for (int x = 0; x < data[toY].length; x++) {
                data[toY][x] = ' ';
                colorData[toY][x] = -1;  // Pas de couleur
            }
        }
    }
    
    /**
     * Décale les colonnes vers la gauche (caractères et couleurs)
     */
    public void shiftLeft(int fromX, int toX) {
        if (data == null) {
            buildData();
        }
        for (int y = 0; y < data.length; y++) {
            for (int x = fromX; x < toX; x++) {
                if (x >= 0 && x < data[y].length && x+1 < data[y].length) {
                    data[y][x] = data[y][x+1];
                    colorData[y][x] = colorData[y][x+1];
                }
            }
            // Vider la colonne de droite
            if (toX >= 0 && toX < data[y].length) {
                data[y][toX] = ' ';
                colorData[y][toX] = -1;  // Pas de couleur
            }
        }
    }
    
    /**
     * Décale les colonnes vers la droite (caractères et couleurs)
     */
    public void shiftRight(int fromX, int toX) {
        if (data == null) {
            buildData();
        }
        for (int y = 0; y < data.length; y++) {
            for (int x = toX; x > fromX; x--) {
                if (x >= 0 && x < data[y].length && x-1 >= 0) {
                    data[y][x] = data[y][x-1];
                    colorData[y][x] = colorData[y][x-1];
                }
            }
            // Vider la colonne de gauche
            if (fromX >= 0 && fromX < data[y].length) {
                data[y][fromX] = ' ';
                colorData[y][fromX] = -1;  // Pas de couleur
            }
        }
    }
    
    @Override
    public byte[] getBytes() {
        // L'area ne génère pas de bytes directement, c'est le layers qui gère le rendu
        return new byte[0];
    }
}
