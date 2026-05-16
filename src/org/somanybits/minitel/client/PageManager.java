package org.somanybits.minitel.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Gestionnaire de navigation et d'historique des pages Minitel
 * 
 * @author eddy
 */
public class PageManager {

    private List<Page> history;
    private int currentIndex;
    private String domain;
    private int port;

    public PageManager(String domain, int port) {
        history = new ArrayList<>();
        currentIndex = -1;
        this.domain = domain;
        this.port = port;
    }

    /**
     * Charge une nouvelle page et l'ajoute à l'historique
     * @param url URL de la page à charger (peut être relative ou absolue)
     * @return La page chargée
     */
    public Page navigate(String url) throws IOException {
        // Construire l'URL complète si nécessaire
        String fullUrl = buildFullUrl(url);
        
        // Récupérer la page courante pour hériter des touches
        Page previousPage = getCurrentPage();
        
        // Créer un nouveau reader pour chaque requête
        MinitelPageReader pageReader = new MinitelPageReader(domain, port);
        Page page = pageReader.get(url);
        page.setUrl(fullUrl);  // Stocker l'URL complète
        
        // Hériter des touches de fonction de la page précédente
        // (sauf si la nouvelle page les redéfinit)
        if (previousPage != null) {
            page.inheritFunctionKeys(previousPage);
        }
        
        System.out.println("📄 navigate: " + fullUrl);
        
        // Si on n'est pas à la fin de l'historique, supprimer les pages suivantes
        if (currentIndex < history.size() - 1) {
            history = new ArrayList<>(history.subList(0, currentIndex + 1));
        }
        
        // Ajouter à l'historique
        history.add(page);
        currentIndex = history.size() - 1;
        
        return page;
    }

    /**
     * Construit l'URL complète à partir d'une URL relative ou absolue
     */
    private String buildFullUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "index.vtml";  // Page par défaut
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;  // URL absolue
        }
        return url;  // URL relative
    }

    /**
     * Recharge la page courante
     * @return La page rechargée
     */
    public Page reload() throws IOException {
        System.out.println("🔄 PageManager.reload() - currentIndex=" + currentIndex + ", historySize=" + history.size());
        
        if (currentIndex < 0 || currentIndex >= history.size()) {
            System.out.println("❌ reload: index invalide");
            return null;
        }
        
        Page currentPage = history.get(currentIndex);
        String url = currentPage.getUrl();
        
        System.out.println("🔄 reload: URL=" + url);
        
        if (url == null || url.isEmpty()) {
            System.out.println("❌ reload: URL vide ou null");
            return currentPage;
        }
        
        // Créer un nouveau reader et recharger
        System.out.println("🔄 reload: chargement depuis " + domain + ":" + port + "/" + url);
        MinitelPageReader pageReader = new MinitelPageReader(domain, port);
        Page newPage = pageReader.get(url);
        newPage.setUrl(url);
        
        // Hériter des touches de fonction de la page précédente dans l'historique
        if (currentIndex > 0) {
            Page previousPage = history.get(currentIndex - 1);
            newPage.inheritFunctionKeys(previousPage);
        }
        
        System.out.println("✅ reload: page rechargée, " + newPage.getData().length + " bytes");
        
        // Remplacer dans l'historique
        history.set(currentIndex, newPage);
        
        return newPage;
    }

    /**
     * Retourne à la page précédente
     * @return La page précédente ou null si pas d'historique
     */
    public Page back() {
        if (currentIndex > 0) {
            currentIndex--;
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Avance à la page suivante (si on a fait back)
     * @return La page suivante ou null si pas de page suivante
     */
    public Page forward() {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Retourne la page courante
     */
    public Page getCurrentPage() {
        if (currentIndex >= 0 && currentIndex < history.size()) {
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Retourne l'URL de la page courante
     */
    public String getCurrentUrl() {
        Page current = getCurrentPage();
        return current != null ? current.getUrl() : null;
    }

    /**
     * Vérifie si on peut revenir en arrière
     */
    public boolean canGoBack() {
        return currentIndex > 0;
    }

    /**
     * Vérifie si on peut avancer
     */
    public boolean canGoForward() {
        return currentIndex < history.size() - 1;
    }

    /**
     * Retourne la taille de l'historique
     */
    public int getHistorySize() {
        return history.size();
    }

    /**
     * Retourne l'index courant dans l'historique
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Retourne l'historique complet (lecture seule)
     */
    public List<Page> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Efface l'historique
     */
    public void clearHistory() {
        history.clear();
        currentIndex = -1;
    }

    /**
     * Change le serveur cible et efface l'historique.
     */
    public void setServer(String domain, int port) {
        this.domain = domain;
        this.port = port;
        clearHistory();
    }

    /**
     * Navigue vers un lien de la page courante (via touche menu)
     * @param key La touche pressée
     * @return La nouvelle page ou null si le lien n'existe pas
     */
    public Page navigateToLink(String key) throws IOException {
        Page current = getCurrentPage();
        if (current == null) {
            return null;
        }
        
        String link = current.getLink(key);
        if (link != null && !link.isEmpty()) {
            return navigate(link);
        }
        
        return null;
    }
    
    // ========== RECHERCHE DE COMPOSANTS ==========
    
    /**
     * Recherche un composant par son ID dans la page courante
     * @param id L'ID du composant
     * @return Le composant ou null si non trouvé
     */
    public ModelMComponent getComponentById(String id) {
        Page current = getCurrentPage();
        if (current == null) return null;
        return current.getComponentById(id);
    }
    
    /**
     * Recherche un composant par son nom dans la page courante
     * @param name Le nom du composant
     * @return Le composant ou null si non trouvé
     */
    public ModelMComponent getComponentByName(String name) {
        Page current = getCurrentPage();
        if (current == null) return null;
        return current.getComponentByName(name);
    }
}
