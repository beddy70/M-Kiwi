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
import java.util.List;
import java.util.Map;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;
import org.somanybits.minitel.components.vtml.VTMLFormComponent;
import org.somanybits.minitel.components.vtml.VTMLLayersComponent;
import org.somanybits.minitel.components.vtml.VTMLStatusComponent;

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

    // ========== SYSTÈME DE FORMULAIRES ==========
    
    private VTMLFormComponent form = null;
    
    /**
     * Définit le formulaire de la page
     */
    public void setForm(VTMLFormComponent form) {
        this.form = form;
    }
    
    /**
     * Retourne le formulaire de la page (ou null si pas de formulaire)
     */
    public VTMLFormComponent getForm() {
        return form;
    }
    
    /**
     * Vérifie si la page contient un formulaire
     */
    public boolean hasForm() {
        return form != null;
    }

    // ========== ZONE STATUS ==========
    
    private VTMLStatusComponent status = null;
    
    /**
     * Définit la zone status de la page
     */
    public void setStatus(VTMLStatusComponent status) {
        this.status = status;
    }
    
    /**
     * Retourne la zone status de la page (ou null si non définie)
     */
    public VTMLStatusComponent getStatus() {
        return status;
    }
    
    /**
     * Vérifie si la page a une zone status
     */
    public boolean hasStatus() {
        return status != null;
    }

    // ========== ZONE LAYERS (JEUX) ==========
    
    private VTMLLayersComponent layers = null;
    
    /**
     * Définit le layers de la page
     */
    public void setLayers(VTMLLayersComponent layers) {
        this.layers = layers;
    }
    
    /**
     * Retourne le layers de la page (ou null si non défini)
     */
    public VTMLLayersComponent getLayers() {
        return layers;
    }
    
    /**
     * Vérifie si la page a un layers
     */
    public boolean hasLayers() {
        return layers != null;
    }

    // ========== LISTE DES COMPOSANTS ==========
    
    private List<ModelMComponent> components = new ArrayList<>();
    
    /**
     * Ajoute un composant à la page
     */
    public void addComponent(ModelMComponent component) {
        components.add(component);
    }
    
    /**
     * Retourne tous les composants de la page
     */
    public List<ModelMComponent> getComponents() {
        return components;
    }
    
    /**
     * Recherche un composant par son ID
     * @param id L'ID du composant
     * @return Le composant ou null si non trouvé
     */
    public ModelMComponent getComponentById(String id) {
        if (id == null) return null;
        return findComponentById(components, id);
    }
    
    private ModelMComponent findComponentById(List<ModelMComponent> list, String id) {
        for (ModelMComponent comp : list) {
            if (id.equals(comp.getId())) {
                return comp;
            }
            // Recherche récursive dans les enfants
            if (!comp.getChilds().isEmpty()) {
                List<ModelMComponent> children = new ArrayList<>();
                for (MComponent child : comp.getChilds()) {
                    if (child instanceof ModelMComponent) {
                        children.add((ModelMComponent) child);
                    }
                }
                ModelMComponent found = findComponentById(children, id);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    /**
     * Recherche un composant par son nom
     * @param name Le nom du composant
     * @return Le composant ou null si non trouvé
     */
    public ModelMComponent getComponentByName(String name) {
        if (name == null) return null;
        return findComponentByName(components, name);
    }
    
    private ModelMComponent findComponentByName(List<ModelMComponent> list, String name) {
        for (ModelMComponent comp : list) {
            if (name.equals(comp.getName())) {
                return comp;
            }
            // Recherche récursive dans les enfants
            if (!comp.getChilds().isEmpty()) {
                List<ModelMComponent> children = new ArrayList<>();
                for (MComponent child : comp.getChilds()) {
                    if (child instanceof ModelMComponent) {
                        children.add((ModelMComponent) child);
                    }
                }
                ModelMComponent found = findComponentByName(children, name);
                if (found != null) return found;
            }
        }
        return null;
    }

}

