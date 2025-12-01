package org.somanybits.minitel.input;

import java.util.HashMap;
import java.util.Map;

/**
 * Gère le mapping des boutons/axes du joystick vers les actions VTML.
 * Peut être modifié dynamiquement via JavaScript.
 */
public class JoystickMapping {
    
    private final Map<Integer, String> buttonMapping = new HashMap<>();
    private final Map<String, String> axisMapping = new HashMap<>();
    private int axisThreshold = 16000;
    
    public JoystickMapping() {
        // Mapping par défaut
        setDefaultMapping();
    }
    
    public void setDefaultMapping() {
        buttonMapping.clear();
        axisMapping.clear();
        
        // Boutons par défaut
        buttonMapping.put(0, "ACTION1");
        buttonMapping.put(1, "ACTION2");
        buttonMapping.put(2, "ACTION1");
        buttonMapping.put(3, "ACTION2");
        
        // Axes par défaut
        axisMapping.put("0+", "RIGHT");
        axisMapping.put("0-", "LEFT");
        axisMapping.put("1+", "DOWN");
        axisMapping.put("1-", "UP");
    }
    
    /**
     * Charge le mapping depuis la config
     */
    public void loadFromConfig(org.somanybits.minitel.kernel.Config.JoystickMapping cfg) {
        buttonMapping.clear();
        axisMapping.clear();
        
        // Charger les boutons
        for (Map.Entry<String, String> entry : cfg.buttons.entrySet()) {
            try {
                int button = Integer.parseInt(entry.getKey());
                buttonMapping.put(button, entry.getValue());
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Joystick mapping: bouton invalide '" + entry.getKey() + "'");
            }
        }
        
        // Charger les axes
        axisMapping.putAll(cfg.axes);
        
        // Seuil
        axisThreshold = cfg.axis_threshold;
        
        System.out.println("🎮 Joystick mapping chargé: " + buttonMapping.size() + " boutons, " + axisMapping.size() + " axes");
    }
    
    // ========== API pour JavaScript ==========
    
    /**
     * Mapper un bouton vers une action
     * @param button Numéro du bouton (0, 1, 2, ...)
     * @param action Action VTML (UP, DOWN, LEFT, RIGHT, ACTION1, ACTION2)
     */
    public void mapButton(int button, String action) {
        if (action == null || action.isEmpty()) {
            buttonMapping.remove(button);
        } else {
            buttonMapping.put(button, action.toUpperCase());
        }
    }
    
    /**
     * Mapper un axe vers une action
     * @param axis Axe avec direction (ex: "0+", "0-", "1+", "1-")
     * @param action Action VTML
     */
    public void mapAxis(String axis, String action) {
        if (action == null || action.isEmpty()) {
            axisMapping.remove(axis);
        } else {
            axisMapping.put(axis, action.toUpperCase());
        }
    }
    
    /**
     * Définir le seuil pour les axes
     * @param threshold Valeur entre 0 et 32767
     */
    public void setAxisThreshold(int threshold) {
        this.axisThreshold = Math.max(0, Math.min(32767, threshold));
    }
    
    /**
     * Obtenir l'action pour un bouton
     */
    public String getButtonAction(int button) {
        return buttonMapping.get(button);
    }
    
    /**
     * Obtenir l'action pour un axe et une valeur
     */
    public String getAxisAction(int axis, int value) {
        if (Math.abs(value) < axisThreshold) {
            return null;  // Zone morte
        }
        String key = axis + (value > 0 ? "+" : "-");
        return axisMapping.get(key);
    }
    
    public int getAxisThreshold() {
        return axisThreshold;
    }
    
    /**
     * Afficher le mapping actuel (pour debug)
     */
    public void printMapping() {
        System.out.println("🎮 Mapping boutons:");
        for (Map.Entry<Integer, String> e : buttonMapping.entrySet()) {
            System.out.println("   Bouton " + e.getKey() + " -> " + e.getValue());
        }
        System.out.println("🎮 Mapping axes:");
        for (Map.Entry<String, String> e : axisMapping.entrySet()) {
            System.out.println("   Axe " + e.getKey() + " -> " + e.getValue());
        }
    }
}
