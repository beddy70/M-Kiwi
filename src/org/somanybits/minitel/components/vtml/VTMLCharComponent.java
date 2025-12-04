/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant un caractère mosaïque dans un {@code <chardef>}.
 * <p>
 * Ce composant est un conteneur pour les lignes {@code <line>} qui définissent
 * le bitmap 2x3 du caractère.
 * </p>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see VTMLChardefComponent
 */
public class VTMLCharComponent extends ModelMComponent {
    
    public VTMLCharComponent() {
        // Composant vide, les lignes sont ajoutées via le parent VTMLChardefComponent
    }
}
