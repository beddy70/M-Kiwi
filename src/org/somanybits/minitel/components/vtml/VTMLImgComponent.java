package org.somanybits.minitel.components.vtml;

import java.net.URL;
import org.somanybits.minitel.components.GraphTel;
import org.somanybits.minitel.components.ModelMComponent;
import org.somanybits.minitel.kernel.Config;
import org.somanybits.minitel.kernel.Kernel;
import tools.ImageTo1bpp;

/**
 * Composant VTML représentant le tag <img>
 * Affiche une image convertie en semi-graphique Minitel
 * Le src peut être une URL complète ou un chemin relatif (localhost par défaut)
 *
 * @author eddy
 */
public class VTMLImgComponent extends ModelMComponent {

    private String src;
    private boolean negative;
    private String baseUrl; // URL de base pour les chemins relatifs

    public VTMLImgComponent() {
        super();
    }

    public VTMLImgComponent(String src, int left, int top, int width, int height, boolean negative) {
        super();
        this.src = src;
        this.negative = negative;
        setX(left);
        setY(top);
        setWidth(width);
        setHeight(height);
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Résout l'URL de l'image
     * Si src commence par http:// ou https://, utilise tel quel
     * Sinon, construit l'URL avec la config du Kernel (localhost:port)
     */
    private URL resolveImageUrl() throws Exception {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return new URL(src);
        }
        
        // Chemin relatif - utiliser la config du Kernel
        String base;
        if (baseUrl != null && !baseUrl.isEmpty()) {
            base = baseUrl;
        } else {
            // Récupérer le port depuis la configuration
            Config cfg = Kernel.getIntance().getConfig();
            base = "http://localhost:" + cfg.server.port;
        }
        
        // S'assurer que le chemin commence par / pour l'URL
        String path = !src.startsWith("/") ? "/" + src : src;
        
        return new URL(base + path);
    }

    @Override
    public byte[] getBytes() {
        try {
            // Résoudre l'URL de l'image
            URL imageUrl = resolveImageUrl();
            
            // Charger et convertir l'image en 1bpp depuis l'URL
            ImageTo1bpp img = new ImageTo1bpp(imageUrl, getWidth(), getHeight());

            // Créer le GraphTel avec les dimensions de l'image
            GraphTel gfx = new GraphTel(img.getWidth(), img.getHeight());
            gfx.writeBitmap(img.getBitmap());

            // Inverser si demandé
            if (negative) {
                gfx.inverseBitmap();
            }

            // Retourner les bytes pour affichage Minitel
            return gfx.getDrawToBytes(getX(), getY());

        } catch (Exception e) {
            System.err.println("Erreur chargement image " + src + ": " + e.getMessage());
            return ("[IMG ERROR: " + e.getMessage() + "]").getBytes();
        }
    }
}
