package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag &lt;fillchar&gt;.
 * Remplit une zone rectangulaire d'un caractère avec couleurs optionnelles.
 *
 * Règle background : si la couleur de fond est non-nulle (non-noir), chaque
 * ligne commence par un espace pour activer la couleur de fond Minitel ; les
 * (width-1) caractères suivants sont remplis avec {@code char}.
 */
public class VTMLFillcharComponent extends ModelMComponent {

    private final String fillChar;
    private final String ink;
    private final String background;

    public VTMLFillcharComponent(int left, int top, int width, int height,
                                  String fillChar, String ink, String background) {
        setX(left);
        setY(top);
        setWidth(width);
        setHeight(height);
        this.fillChar   = (fillChar != null && !fillChar.isEmpty()) ? fillChar : " ";
        this.ink        = ink;
        this.background = background;
    }

    private byte parseColor(String color) {
        if (color == null || color.isEmpty()) return -1;
        try {
            int v = Integer.parseInt(color.trim());
            if (v >= 0 && v <= 7) return (byte) v;
        } catch (NumberFormatException ignored) {}
        return switch (color.toLowerCase().trim()) {
            case "black",   "noir"   -> GetTeletelCode.COLOR_BLACK;
            case "red",     "rouge"  -> GetTeletelCode.COLOR_RED;
            case "green",   "vert"   -> GetTeletelCode.COLOR_GREEN;
            case "yellow",  "jaune"  -> GetTeletelCode.COLOR_YELLOW;
            case "blue",    "bleu"   -> GetTeletelCode.COLOR_BLUE;
            case "magenta"           -> GetTeletelCode.COLOR_MAGENTA;
            case "cyan"              -> GetTeletelCode.COLOR_CYAN;
            case "white",   "blanc"  -> GetTeletelCode.COLOR_WHITE;
            default -> -1;
        };
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte inkColor = parseColor(ink);
            byte bgColor  = parseColor(background);
            // La règle espace-initial s'applique uniquement si fond non-noir/non-absent
            boolean needLeadingSpace = bgColor > 0;

            char ch    = fillChar.charAt(0);
            int width  = getWidth();
            int height = getHeight();
            int left   = getX();
            int top    = getY();

            for (int row = 0; row < height; row++) {
                out.write(GetTeletelCode.setCursor(left, top + row));
                if (inkColor >= 0) out.write(GetTeletelCode.setTextColor(inkColor));
                if (bgColor  >= 0) out.write(GetTeletelCode.setBGColor(bgColor));

                if (needLeadingSpace) {
                    out.write(' ');
                    int fillCount = Math.max(0, width - 1);
                    out.write(String.valueOf(ch).repeat(fillCount).getBytes("ISO-8859-1"));
                } else {
                    out.write(String.valueOf(ch).repeat(width).getBytes("ISO-8859-1"));
                }
            }

            return out.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
