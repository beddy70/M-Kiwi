package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag {@code <oled>}.
 * Ne génère aucun octet Minitel ; son seul rôle est de transporter
 * les lignes de texte destinées à l'écran OLED SSD1306 128×64.
 *
 * Syntaxe VTML côté serveur (jusqu'à 8 lignes) :
 * <pre>
 *   &lt;oled line1="M-Kiwi" line2="Score: 100" line3="Joueur: Eddy" /&gt;
 * </pre>
 */
public class VTMLOledComponent extends ModelMComponent {

    private final String[] lines;

    public VTMLOledComponent(String[] lines) {
        super();
        this.lines = lines;
    }

    public String[] getLines() {
        return lines;
    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }
}
