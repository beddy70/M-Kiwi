package org.somanybits.minitel.components.vtml;

import java.net.URI;
import java.net.URL;
import org.somanybits.minitel.Teletel;
import org.somanybits.minitel.components.GraphTel;
import org.somanybits.minitel.components.ModelMComponent;
import org.somanybits.minitel.kernel.Config;
import org.somanybits.minitel.kernel.Kernel;

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
    private String style;  // "dithering", "bitmap", ou null (couleur par défaut)
    private String baseUrl; // URL de base pour les chemins relatifs

    public VTMLImgComponent() {
        super();
    }

    public VTMLImgComponent(String src, int left, int top, int width, int height, boolean negative, String style) {
        super();
        this.src = src;
        this.negative = negative;
        this.style = style;
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Résout l'URL de l'image
     * Si src commence par http:// ou https://, utilise tel quel
     * Sinon, construit l'URL avec la config du Kernel (localhost:port)
     */
    private URL resolveImageUrl() throws Exception {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return new URI(src).toURL();
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
        
        return new URI(base + path).toURL();
    }

    @Override
    public byte[] getBytes() {
        try {
            // Résoudre l'URL de l'image
            URL imageUrl = resolveImageUrl();
            
            // Calculer l'espace disponible sur l'écran
            int availableCharsX = Math.min(getWidth(), Teletel.PAGE_WIDTH - getX());
            int availableCharsY = Math.min(getHeight(), Teletel.PAGE_HEIGHT - getY());
            
            // Dimensions en pixels (2 pixels par caractère en X, 3 en Y)
            int pixelWidth = availableCharsX * 2;
            int pixelHeight = availableCharsY * 3;
            
            System.out.println("📷 VTMLImgComponent: src=" + src + 
                              " pos=(" + getX() + "," + getY() + ")" +
                              " requested=" + getWidth() + "x" + getHeight() + " chars" +
                              " available=" + availableCharsX + "x" + availableCharsY + " chars" +
                              " -> " + pixelWidth + "x" + pixelHeight + " pixels" +
                              " style=" + style);
            
            // Créer le GraphTel et charger l'image selon le style
            GraphTel gfx = new GraphTel(pixelWidth, pixelHeight);
            gfx.loadImage(imageUrl, style);

            // Inverser si demandé
            if (negative) {
                gfx.inverseBitmap();
            }

            // Retourner les bytes pour affichage Minitel
            return gfx.getDrawToBytes(getX(), getY());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur chargement image " + src + ": " + e.getMessage());
            return ("[IMG ERROR: " + e.getMessage() + "]").getBytes();
        }
    }
}
