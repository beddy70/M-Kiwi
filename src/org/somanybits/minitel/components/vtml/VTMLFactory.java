/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.MComponent;

/**
 * Factory pour créer des composants VTML dynamiquement depuis JavaScript.
 * <p>
 * Permet d'utiliser createElement("tagName") dans les scripts VTML
 * pour créer des éléments dynamiquement, similaire au DOM HTML.
 * </p>
 * 
 * <h2>Exemple d'utilisation en JavaScript</h2>
 * <pre>
 * var row = createElement("row");
 * row.setText("Mon texte");
 * getElementById("container").appendChild(row);
 * </pre>
 * 
 * @author Eddy Briere
 * @version 0.1
 * @see VTMLScriptEngine
 */
public class VTMLFactory {
    
    /**
     * Crée un composant VTML à partir de son nom de tag.
     * 
     * @param tagName Le nom du tag VTML (row, br, div, color, blink, label)
     * @return Le composant créé, ou null si le type est inconnu
     */
    public static MComponent create(String tagName) {
        if (tagName == null) {
            return null;
        }
        
        return switch (tagName.toLowerCase()) {
            case "row" -> new VTMLRowComponent();
            case "br" -> new VTMLBrComponent();
            case "div" -> new VTMLDivComponent();
            case "color" -> new VTMLColorComponent();
            case "blink" -> new VTMLBlinkComponent();
            case "label" -> new VTMLLabelComponent();
            default -> {
                System.err.println("⚠️ createElement: type d'élément inconnu: " + tagName);
                yield null;
            }
        };
    }
}
