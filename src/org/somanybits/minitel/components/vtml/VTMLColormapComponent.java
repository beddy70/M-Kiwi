/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Colormap pour définir les couleurs de texte d'une map.
 * <p>
 * Ce composant est un conteneur pour les lignes de couleur ({@code <row>})
 * qui définissent la couleur du texte (ink) pour chaque caractère de la map parente.
 * </p>
 * 
 * <h2>Codes couleur</h2>
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
 *   <row>########</row>
 *   <row>#      #</row>
 *   <row>########</row>
 *   <colormap>
 *     <row>11111111</row>
 *     <row>7      7</row>
 *     <row>22222222</row>
 *   </colormap>
 * </map>
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.4
 * @see VTMLMapComponent
 */
public class VTMLColormapComponent extends ModelMComponent {
    
    public VTMLColormapComponent() {
        // Constructeur par défaut
    }
    
    @Override
    public byte[] getBytes() {
        // La colormap ne génère pas de bytes directement
        // Les données sont utilisées par la map parente
        return new byte[0];
    }
}
