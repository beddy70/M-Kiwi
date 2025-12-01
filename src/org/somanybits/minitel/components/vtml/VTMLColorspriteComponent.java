/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant Colorsprite pour définir les couleurs de chaque caractère d'un sprite.
 * <p>
 * Ce composant est un conteneur pour les lignes de couleur ({@code <line>})
 * qui définissent la couleur du texte (ink) pour chaque caractère du sprite parent.
 * Fonctionne de manière similaire à {@link VTMLColormapComponent} pour les maps.
 * </p>
 * 
 * <h2>Codes couleur</h2>
 * <ul>
 *   <li>' ' (espace) = couleur par défaut du sprite</li>
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
 * <spritedef id="alien" width="3" height="2" type="char">
 *   <sprite>
 *     <line>/O\</line>
 *     <line>\_/</line>
 *   </sprite>
 *   <colorsprite>
 *     <line>121</line>
 *     <line>333</line>
 *   </colorsprite>
 * </spritedef>
 * }</pre>
 * 
 * <p>Dans cet exemple, le sprite "alien" aura :</p>
 * <ul>
 *   <li>'/' en rouge (1), 'O' en vert (2), '\' en rouge (1)</li>
 *   <li>'\_/' tout en jaune (3)</li>
 * </ul>
 * 
 * @author Eddy Briere
 * @version 0.4
 * @see VTMLSpriteComponent
 * @see VTMLSpriteDefComponent
 * @see VTMLColormapComponent
 */
public class VTMLColorspriteComponent extends ModelMComponent {
    
    public VTMLColorspriteComponent() {
        // Constructeur par défaut
    }
    
    @Override
    public byte[] getBytes() {
        // La colorsprite ne génère pas de bytes directement
        // Les données sont utilisées par le sprite parent
        return new byte[0];
    }
}
