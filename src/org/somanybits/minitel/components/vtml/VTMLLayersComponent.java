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
    public static final int MAX_SPRITES = 8;
    
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
    
    // Mapping des touches
    private Map<String, VTMLKeypadComponent> keypads = new HashMap<>();
    
    // Callbacks JavaScript pour les événements
    private Map<String, String> keypadEvents = new HashMap<>();
    
    // Game loop
    private String tickFunction = null;  // Fonction JS à appeler périodiquement
    private int tickInterval = 200;      // Intervalle en ms (200ms = 5 FPS)
    
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
    
    // ========== GESTION DES KEYPADS ==========
    
    public void addKeypad(VTMLKeypadComponent keypad) {
        keypads.put(keypad.getAction(), keypad);
        if (keypad.getEvent() != null) {
            keypadEvents.put(keypad.getAction(), keypad.getEvent());
        }
    }
    
    public VTMLKeypadComponent getKeypad(String action) {
        return keypads.get(action);
    }
    
    public String getKeypadEvent(String action) {
        return keypadEvents.get(action);
    }
    
    /**
     * Trouve l'action associée à une touche
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
        //System.out.println("🎮 compose() - spriteInstances=" + spriteInstances.size());
        for (String id : spriteInstances.keySet()) {
            SpriteInstance instance = spriteInstances.get(id);
            //System.out.println("  Sprite '" + id + "': visible=" + instance.isVisible() + ", pos=(" + instance.getX() + "," + instance.getY() + ")");
            if (instance.isVisible()) {
                drawSprite(instance);
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
    }
}
