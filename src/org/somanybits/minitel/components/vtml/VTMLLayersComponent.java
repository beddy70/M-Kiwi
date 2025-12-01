package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Layers pour créer des jeux Minitel.
 * Gère des zones (areas) empilées et des sprites animés.
 * 
 * @author eddy
 */
public class VTMLLayersComponent extends ModelMComponent {
    
    public static final int MAX_AREAS = 3;
    public static final int MAX_SPRITES = 16;
    
    private int left;
    private int top;
    private int width;
    private int height;
    
    // Buffer interne pour le rendu
    private char[][] buffer;
    private char[][] previousBuffer;
    private boolean[][] mosaicMode;  // true = caractère semi-graphique
    private boolean[][] previousMosaicMode;
    
    // Liste des areas (ordre = z-index, 0 = fond)
    private List<VTMLMapComponent> areas = new ArrayList<>();
    
    // Définitions de sprites
    private Map<String, VTMLSpriteDefComponent> spriteDefs = new HashMap<>();
    
    // Instances de sprites actifs (position, visibilité, frame courante)
    private Map<String, SpriteInstance> spriteInstances = new HashMap<>();
    
    // Mapping des touches (action -> keypad)
    private Map<String, VTMLKeypadComponent> keypads = new HashMap<>();
    
    // Callbacks JavaScript pour les événements (action -> event)
    private Map<String, String> keypadEvents = new HashMap<>();
    
    // Mapping direct des touches (key -> event) pour les touches sans action
    private Map<Character, String> directKeyEvents = new HashMap<>();
    
    // Labels de texte dynamique
    private Map<String, VTMLLabelComponent> labels = new HashMap<>();
    
    // Game loop
    private String tickFunction = null;  // Fonction JS à appeler périodiquement
    private int tickInterval = 200;      // Intervalle en ms (200ms = 5 FPS)
    
    // Flag pour déclencher un beep au prochain rendu
    private boolean pendingBeep = false;
    
    public VTMLLayersComponent(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.buffer = new char[height][width];
        this.previousBuffer = new char[height][width];
        this.mosaicMode = new boolean[height][width];
        this.previousMosaicMode = new boolean[height][width];
        clearBuffer();
    }
    
    private void clearBuffer() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = ' ';
                previousBuffer[y][x] = ' ';
            }
        }
    }
    
    // ========== GESTION DES AREAS ==========
    
    public void addArea(VTMLMapComponent area) {
        if (areas.size() < MAX_AREAS) {
            areas.add(area);
        } else {
            System.err.println("⚠️ Limite de " + MAX_AREAS + " areas atteinte");
        }
    }
    
    public List<VTMLMapComponent> getAreas() {
        return areas;
    }
    
    // ========== GESTION DES SPRITES ==========
    
    public void addSpriteDef(VTMLSpriteDefComponent spriteDef) {
        if (spriteDefs.size() < MAX_SPRITES) {
            spriteDefs.put(spriteDef.getId(), spriteDef);
        } else {
            System.err.println("⚠️ Limite de " + MAX_SPRITES + " sprites atteinte");
        }
    }
    
    public VTMLSpriteDefComponent getSpriteDef(String id) {
        return spriteDefs.get(id);
    }
    
    /**
     * Récupère une instance de sprite (la crée si nécessaire)
     */
    public SpriteInstance getSprite(String id) {
        if (!spriteInstances.containsKey(id)) {
            VTMLSpriteDefComponent def = spriteDefs.get(id);
            if (def != null) {
                spriteInstances.put(id, new SpriteInstance(def));
            }
        }
        return spriteInstances.get(id);
    }
    
    /**
     * Affiche un sprite à une frame donnée
     */
    public void showSprite(String id, int frameIndex) {
        SpriteInstance instance = getSprite(id);
        if (instance != null) {
            instance.setVisible(true);
            instance.setCurrentFrame(frameIndex);
        }
    }
    
    /**
     * Cache un sprite
     */
    public void hideSprite(String id) {
        SpriteInstance instance = spriteInstances.get(id);
        if (instance != null) {
            instance.setVisible(false);
        }
    }
    
    /**
     * Déplace un sprite
     */
    public void moveSprite(String id, int x, int y) {
        SpriteInstance instance = getSprite(id);
        if (instance != null) {
            instance.setX(x);
            instance.setY(y);
        }
    }
    
    /**
     * Vérifie si deux sprites entrent en collision
     */
    public boolean checkCollision(String id1, String id2) {
        SpriteInstance sprite1 = getSprite(id1);
        SpriteInstance sprite2 = getSprite(id2);
        if (sprite1 == null || sprite2 == null) return false;
        return sprite1.collidesWith(sprite2);
    }
    
    /**
     * Vérifie si un sprite entre en collision avec un caractère non-vide de la map.
     * Retourne le caractère touché ou '\0' si pas de collision.
     */
    public char checkMapCollision(String spriteId) {
        SpriteInstance sprite = getSprite(spriteId);
        if (sprite == null || !sprite.isVisible()) return '\0';
        
        int sx = sprite.getX();
        int sy = sprite.getY();
        int sw = sprite.getWidth();
        int sh = sprite.getHeight();
        
        // Parcourir toutes les positions du sprite
        for (int dy = 0; dy < sh; dy++) {
            for (int dx = 0; dx < sw; dx++) {
                int mapX = sx + dx;
                int mapY = sy + dy;
                
                // Vérifier les limites
                if (mapX < 0 || mapX >= width || mapY < 0 || mapY >= height) continue;
                
                // Chercher dans les areas (de bas en haut)
                for (VTMLMapComponent area : areas) {
                    char[][] areaData = area.getData();
                    if (areaData == null) continue;
                    if (mapY >= areaData.length || mapX >= areaData[mapY].length) continue;
                    
                    char c = areaData[mapY][mapX];
                    // Espace = vide, pas de collision
                    if (c != ' ' && c != '\0') {
                        return c;
                    }
                }
            }
        }
        return '\0';  // Pas de collision
    }
    
    /**
     * Vérifie si un sprite toucherait un caractère non-vide à une position donnée.
     * Utile pour tester AVANT de déplacer le sprite.
     */
    public char checkMapCollisionAt(String spriteId, int testX, int testY) {
        SpriteInstance sprite = getSprite(spriteId);
        if (sprite == null) return '\0';
        
        int sw = sprite.getWidth();
        int sh = sprite.getHeight();
        
        // Parcourir toutes les positions du sprite à la position test
        for (int dy = 0; dy < sh; dy++) {
            for (int dx = 0; dx < sw; dx++) {
                int mapX = testX + dx;
                int mapY = testY + dy;
                
                // Vérifier les limites
                if (mapX < 0 || mapX >= width || mapY < 0 || mapY >= height) continue;
                
                // Chercher dans les areas (de bas en haut)
                for (VTMLMapComponent area : areas) {
                    char[][] areaData = area.getData();
                    if (areaData == null) continue;
                    if (mapY >= areaData.length || mapX >= areaData[mapY].length) continue;
                    
                    char c = areaData[mapY][mapX];
                    if (c != ' ' && c != '\0') {
                        return c;
                    }
                }
            }
        }
        return '\0';
    }
    
    /**
     * Modifie un caractère dans une map (appelable depuis JavaScript)
     * @param areaIndex Index de la map (0 = première map)
     * @param x Position X dans la map
     * @param y Position Y dans la map
     * @param c Caractère à placer
     */
    public void setMapChar(int areaIndex, int x, int y, char c) {
        if (areaIndex >= 0 && areaIndex < areas.size()) {
            VTMLMapComponent area = areas.get(areaIndex);
            char[][] data = area.getData();
            if (data != null && y >= 0 && y < data.length && x >= 0 && x < data[y].length) {
                data[y][x] = c;
            }
        }
    }
    
    /**
     * Efface une ligne entière d'une map (pour Tetris)
     */
    public void clearMapLine(int areaIndex, int y) {
        if (areaIndex >= 0 && areaIndex < areas.size()) {
            VTMLMapComponent area = areas.get(areaIndex);
            char[][] data = area.getData();
            if (data != null && y >= 0 && y < data.length) {
                for (int x = 0; x < data[y].length; x++) {
                    data[y][x] = ' ';
                }
            }
        }
    }
    
    /**
     * Décale les lignes d'une map vers le bas (pour Tetris)
     */
    public void shiftMapDown(int areaIndex, int fromY, int toY) {
        if (areaIndex >= 0 && areaIndex < areas.size()) {
            VTMLMapComponent area = areas.get(areaIndex);
            char[][] data = area.getData();
            if (data != null) {
                for (int y = toY; y > fromY; y--) {
                    if (y >= 0 && y < data.length && y-1 >= 0) {
                        System.arraycopy(data[y-1], 0, data[y], 0, data[y].length);
                    }
                }
                // Vider la ligne du haut
                if (fromY >= 0 && fromY < data.length) {
                    for (int x = 0; x < data[fromY].length; x++) {
                        data[fromY][x] = ' ';
                    }
                }
            }
        }
    }
    
    // ========== GESTION DES LABELS ==========
    
    public void addLabel(VTMLLabelComponent label) {
        if (label.getId() != null) {
            labels.put(label.getId(), label);
        }
    }
    
    public VTMLLabelComponent getLabel(String id) {
        return labels.get(id);
    }
    
    /**
     * Modifie le texte d'un label (appelable depuis JavaScript)
     */
    public void setText(String id, String text) {
        VTMLLabelComponent label = labels.get(id);
        if (label != null) {
            label.setText(text);
        }
    }
    
    // ========== SONS ==========
    
    /**
     * Déclenche un beep au prochain rendu (appelable depuis JavaScript)
     */
    public void beep() {
        pendingBeep = true;
    }
    
    /**
     * Vérifie et consomme le flag beep
     */
    public boolean consumeBeep() {
        if (pendingBeep) {
            pendingBeep = false;
            return true;
        }
        return false;
    }
    
    // ========== GESTION DES KEYPADS ==========
    
    public void addKeypad(VTMLKeypadComponent keypad) {
        if (keypad.hasAction()) {
            // Mode classique : action de jeu (UP, DOWN, LEFT, RIGHT, ACTION1, ACTION2)
            keypads.put(keypad.getAction(), keypad);
            if (keypad.getEvent() != null) {
                keypadEvents.put(keypad.getAction(), keypad.getEvent());
            }
        } else if (keypad.isDirectKey()) {
            // Mode direct : touche -> event (sans action de jeu)
            char key = Character.toUpperCase(keypad.getKey());
            directKeyEvents.put(key, keypad.getEvent());
            System.out.println("🎮 Keypad direct: '" + key + "' -> " + keypad.getEvent() + "()");
        }
    }
    
    public VTMLKeypadComponent getKeypad(String action) {
        return keypads.get(action);
    }
    
    public String getKeypadEvent(String action) {
        return keypadEvents.get(action);
    }
    
    /**
     * Trouve l'event direct associé à une touche (sans action de jeu)
     */
    public String getDirectKeyEvent(char key) {
        // Chercher en majuscule et minuscule
        String event = directKeyEvents.get(Character.toUpperCase(key));
        if (event == null) {
            event = directKeyEvents.get(Character.toLowerCase(key));
        }
        return event;
    }
    
    /**
     * Trouve l'action associée à une touche (pour les keypads avec action)
     */
    public String getActionForKey(char key) {
        for (VTMLKeypadComponent keypad : keypads.values()) {
            if (keypad.getKey() == Character.toUpperCase(key) || 
                keypad.getKey() == Character.toLowerCase(key)) {
                return keypad.getAction();
            }
        }
        return null;
    }
    
    // ========== RENDU ==========
    
    /**
     * Compose le buffer à partir des areas et sprites
     */
    public synchronized void compose() {
        // NE PAS sauvegarder ici - c'est getDifferentialBytes() qui gère previousBuffer
        
        // Effacer le buffer et le mode mosaïque
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = ' ';
                mosaicMode[y][x] = false;
            }
        }
        
        // Dessiner les areas de bas en haut
        for (VTMLMapComponent area : areas) {
            drawArea(area);
        }
        
        // Dessiner les sprites visibles par-dessus
        for (String id : spriteInstances.keySet()) {
            SpriteInstance instance = spriteInstances.get(id);
            if (instance.isVisible()) {
                drawSprite(instance);
            }
        }
        
        // Dessiner les labels par-dessus tout
        for (VTMLLabelComponent label : labels.values()) {
            drawLabel(label);
        }
    }
    
    private void drawLabel(VTMLLabelComponent label) {
        String text = label.getDisplayText();
        int lx = label.getX();
        int ly = label.getY();
        
        for (int i = 0; i < text.length(); i++) {
            int bufX = lx + i;
            if (bufX >= 0 && bufX < width && ly >= 0 && ly < height) {
                buffer[ly][bufX] = text.charAt(i);
                mosaicMode[ly][bufX] = false;  // Les labels sont toujours en mode texte
            }
        }
    }
    
    private void drawArea(VTMLMapComponent area) {
        char[][] areaData = area.getData();
        if (areaData == null) return;
        
        for (int y = 0; y < areaData.length && y < height; y++) {
            for (int x = 0; x < areaData[y].length && x < width; x++) {
                char c = areaData[y][x];
                // Ne dessiner que si non-transparent (espace = transparent pour les areas supérieures)
                if (c != ' ' || areas.indexOf(area) == 0) {
                    buffer[y][x] = c;
                }
            }
        }
    }
    
    private void drawSprite(SpriteInstance instance) {
        VTMLSpriteDefComponent def = instance.getDefinition();
        char[][] spriteData = def.getFrameData(instance.getCurrentFrame());
        if (spriteData == null) return;
        
        int sx = instance.getX();
        int sy = instance.getY();
        boolean isBitmap = def.getType() == VTMLSpriteDefComponent.SpriteType.BITMAP;
        
        for (int y = 0; y < spriteData.length; y++) {
            int bufY = sy + y;
            if (bufY < 0 || bufY >= height) continue;
            
            for (int x = 0; x < spriteData[y].length; x++) {
                int bufX = sx + x;
                if (bufX < 0 || bufX >= width) continue;
                
                char c = spriteData[y][x];
                // Pour les sprites, espace = transparent
                if (c != ' ') {
                    buffer[bufY][bufX] = c;
                    mosaicMode[bufY][bufX] = isBitmap;
                }
            }
        }
    }
    
    /**
     * Génère les bytes pour un rendu complet
     */
    @Override
    public byte[] getBytes() {
        System.out.println("🎮 Layers.getBytes() - areas=" + areas.size() + ", spriteDefs=" + spriteDefs.size());
        
        // Debug: afficher le contenu des areas
        for (int i = 0; i < areas.size(); i++) {
            VTMLMapComponent area = areas.get(i);
            char[][] data = area.getData();
            System.out.println("  Area " + i + ": " + (data != null ? data.length + " rows" : "null"));
        }
        
        compose();
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            for (int y = 0; y < height; y++) {
                out.write(GetTeletelCode.setCursor(left, top + y));
                
                for (int x = 0; x < width; x++) {
                    // Pour le rendu initial, on envoie le code de mode avant chaque caractère mosaïque
                    if (mosaicMode[y][x]) {
                        out.write(0x0E);  // Mode semi-graphique
                        out.write(buffer[y][x]);
                        out.write(0x0F);  // Retour mode texte
                    } else {
                        out.write(buffer[y][x]);
                    }
                }
            }
            
            // Copier les buffers pour la prochaine comparaison
            for (int y = 0; y < height; y++) {
                System.arraycopy(buffer[y], 0, previousBuffer[y], 0, width);
                System.arraycopy(mosaicMode[y], 0, previousMosaicMode[y], 0, width);
            }
            
            System.out.println("🎮 Layers output: " + out.size() + " bytes");
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
    
    /**
     * Génère les bytes pour un rendu différentiel (optimisé)
     * Ne redessine que les caractères modifiés
     */
    public synchronized byte[] getDifferentialBytes() {
        compose();
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            int lastX = -1, lastY = -1;
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Redessiner si le caractère OU le mode a changé
                    boolean needsRedraw = buffer[y][x] != previousBuffer[y][x] 
                                       || mosaicMode[y][x] != previousMosaicMode[y][x];
                    
                    if (needsRedraw) {
                        // Positionner le curseur si nécessaire
                        if (lastY != y || lastX != x - 1) {
                            out.write(GetTeletelCode.setCursor(left + x, top + y));
                        }
                        
                        // Pour chaque caractère mosaïque, envoyer mode avant/après
                        boolean needMosaic = mosaicMode[y][x] || (previousMosaicMode[y][x] && buffer[y][x] == ' ');
                        
                        if (needMosaic) {
                            out.write(0x0E);  // Mode semi-graphique
                            out.write(buffer[y][x]);
                            out.write(0x0F);  // Retour mode texte
                        } else {
                            out.write(buffer[y][x]);
                        }
                        
                        lastX = x;
                        lastY = y;
                    }
                }
            }
            
            // Copier tout le buffer dans previousBuffer pour la prochaine comparaison
            for (int y = 0; y < height; y++) {
                System.arraycopy(buffer[y], 0, previousBuffer[y], 0, width);
                System.arraycopy(mosaicMode[y], 0, previousMosaicMode[y], 0, width);
            }
            
            // Ajouter le beep si demandé
            if (consumeBeep()) {
                out.write(0x07);  // BEL - code sonore Minitel
            }
            
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
    
    
    // ========== GAME LOOP ==========
    
    public void setTickFunction(String functionName, int intervalMs) {
        this.tickFunction = functionName;
        this.tickInterval = intervalMs;
    }
    
    public String getTickFunction() { return tickFunction; }
    public int getTickInterval() { return tickInterval; }
    public boolean hasGameLoop() { return tickFunction != null; }
    
    // ========== GETTERS ==========
    
    public int getLeft() { return left; }
    public int getTop() { return top; }
    @Override
    public int getWidth() { return width; }
    @Override
    public int getHeight() { return height; }
    
    // ========== CLASSE INTERNE: INSTANCE DE SPRITE ==========
    
    public static class SpriteInstance {
        private VTMLSpriteDefComponent definition;
        private int x = 0;
        private int y = 0;
        private int currentFrame = 0;
        private boolean visible = false;
        
        public SpriteInstance(VTMLSpriteDefComponent definition) {
            this.definition = definition;
        }
        
        public VTMLSpriteDefComponent getDefinition() { return definition; }
        
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        
        public void move(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public int getCurrentFrame() { return currentFrame; }
        public void setCurrentFrame(int frame) { this.currentFrame = frame; }
        
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
        
        public void show(int frameIndex) {
            this.currentFrame = frameIndex;
            this.visible = true;
        }
        
        public void hide() {
            this.visible = false;
        }
        
        /**
         * Retourne la largeur du sprite en caractères
         */
        public int getWidth() {
            if (definition == null) return 0;
            char[][] data = definition.getFrameData(currentFrame);
            if (data == null || data.length == 0) return 0;
            return data[0].length;
        }
        
        /**
         * Retourne la hauteur du sprite en caractères
         */
        public int getHeight() {
            if (definition == null) return 0;
            char[][] data = definition.getFrameData(currentFrame);
            if (data == null) return 0;
            return data.length;
        }
        
        /**
         * Vérifie si ce sprite entre en collision avec un autre (AABB)
         */
        public boolean collidesWith(SpriteInstance other) {
            if (!this.visible || !other.visible) return false;
            
            int thisLeft = this.x;
            int thisRight = this.x + this.getWidth();
            int thisTop = this.y;
            int thisBottom = this.y + this.getHeight();
            
            int otherLeft = other.x;
            int otherRight = other.x + other.getWidth();
            int otherTop = other.y;
            int otherBottom = other.y + other.getHeight();
            
            return thisLeft < otherRight && thisRight > otherLeft &&
                   thisTop < otherBottom && thisBottom > otherTop;
        }
    }
}
