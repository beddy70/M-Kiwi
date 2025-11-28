/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.client;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.kernel.Kernel;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;
import org.somanybits.minitel.components.vtml.*;

/**
 *
 * @author eddy
 */
public class MinitelPageReader {

    private int port = 0;
    private String domain;
    private Page page;

    // Structure de composants VTML (nouvelle architecture)
    private VTMLMinitelComponent rootComponent = null;
    private MComponent currentComponent = null;

    public static final int MINITEL_TAG_DEPTH = 3;

    public MinitelPageReader(String domain, int port) {
        this.port = port;
        this.domain = domain;
    }

    /**
     * Charge une page VTML et retourne les données Minitel
     */
    public Page get(String url) throws IOException {

        if (!isHttpHostOrHostPort(url)) {
            url = "http://" + domain + ":" + Integer.toString(port) + "/" + url;
        }

        page = new Page(Page.MODE_40_COL);
        
        // Initialiser la structure de composants
        rootComponent = null;
        currentComponent = null;

        // READ LINK
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Minitel/5.0 (Java JSoup)")
                    .timeout(15_000)
                    .get();
        } catch (IOException ex) {
            page.addData(("Error:" + ex.getMessage()).getBytes());
            return page;
        }

        // Parcourir le document et construire l'arbre de composants
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof Element el) {
                    if (depth >= MINITEL_TAG_DEPTH) {
                        // Récupérer les attributs
                        Map<String, String> attrs = new LinkedHashMap<>();
                        for (Attribute a : el.attributes()) {
                            attrs.put(a.getKey(), a.getValue());
                        }
                        
                        // Récupérer le texte direct de l'élément (préserver les espaces multiples)
                        // Pour les scripts, utiliser data() qui préserve le contenu brut
                        String textContent;
                        if ("script".equals(el.normalName())) {
                            textContent = el.data();
                        } else {
                            textContent = el.wholeOwnText();
                        }
                        
                        // Créer le composant et l'ajouter à l'arbre
                        buildComponentFromElement(el.normalName(), depth, attrs, textContent);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el) {
                    if (depth >= MINITEL_TAG_DEPTH) {
                        // Remonter au parent seulement si c'était un conteneur
                        closeCurrentComponent(el.normalName());
                    }
                }
            }
        }, doc);

        // Générer les données Minitel à partir de l'arbre de composants
        if (rootComponent != null) {
            // Afficher l'arbre des composants sur la console
           
            
            renderComponentTree(rootComponent);

             printComponentTree(rootComponent, 0);
            System.out.println();
        }

        return page;
    }

    /**
     * Affiche l'arbre des MComponent sur la console
     */
    private void printComponentTree(MComponent component, int depth) {
        String indent = "  ".repeat(depth);
        String className = component.getClass().getSimpleName();
        
        // Construire les infos du composant
        StringBuilder info = new StringBuilder();
        info.append(indent);
        info.append("\u001B[33m").append(className).append("\u001B[0m"); // Jaune pour le nom de classe
        
        // Toujours afficher les coordonnées
        info.append(" \u001B[36m(x=").append(component.getX())
            .append(", y=").append(component.getY()).append(")\u001B[0m");
        
        // Ajouter le contenu texte si présent
        String textContent = component.getTextContent();
        if (textContent != null && !textContent.isEmpty()) {
            String preview = textContent.length() > 30 ? textContent.substring(0, 30) + "..." : textContent;
            info.append(" \u001B[32m\"").append(preview).append("\"\u001B[0m");
        }
        
        System.out.println(info.toString());
        
        // Afficher les enfants récursivement
        if (component instanceof ModelMComponent modelComponent) {
            for (MComponent child : modelComponent.getChilds()) {
                printComponentTree(child, depth + 1);
            }
        }
    }

    /**
     * Construit un composant à partir d'un élément HTML et l'ajoute à l'arbre
     */
    private void buildComponentFromElement(String tagname, int depth, Map<String, String> attrs, String textContent) {
        try {
            LogManager logmgr = Kernel.getIntance().getLogManager();
            logmgr.addLog(LogManager.ANSI_BOLD_WHITE + depth + ">" + LogManager.ANSI_YELLOW + 
                         "  ".repeat(depth - MINITEL_TAG_DEPTH) + "<" + tagname + ">");

            // Créer le composant approprié
            MComponent component = createComponent(tagname, attrs, textContent);
            
            if (component == null) {
                return; // Tag non reconnu
            }

            // Ajouter à l'arbre
            if (rootComponent == null && component instanceof VTMLMinitelComponent) {
                // Premier composant = racine
                rootComponent = (VTMLMinitelComponent) component;
                currentComponent = rootComponent;
            } else if (currentComponent != null) {
                // Ajouter comme enfant du composant courant
                currentComponent.addChild(component);
                
                // Descendre dans le composant seulement s'il peut avoir des enfants
                // (pas pour les composants "feuilles" comme qrcode, br, row, item)
                if (isContainerComponent(component)) {
                    currentComponent = component;
                }
            }

        } catch (IOException ex) {
            System.err.println("Erreur construction composant: " + ex.getMessage());
        }
    }

    /**
     * Vérifie si un composant peut contenir des enfants
     */
    private boolean isContainerComponent(MComponent component) {
        return component instanceof VTMLMinitelComponent
            || component instanceof VTMLDivComponent
            || component instanceof VTMLMenuComponent
            || component instanceof VTMLFormComponent;
    }

    /**
     * Remonte au composant parent seulement si le tag fermé était un conteneur
     */
    private void closeCurrentComponent(String tagname) {
        // Ne remonter que si c'était un tag conteneur
        if (isContainerTag(tagname)) {
            if (currentComponent != null && currentComponent.getParent() != null) {
                currentComponent = currentComponent.getParent();
            }
        }
    }

    /**
     * Vérifie si un tag est un conteneur (peut avoir des enfants)
     */
    private boolean isContainerTag(String tagname) {
        return "minitel".equals(tagname)
            || "div".equals(tagname)
            || "menu".equals(tagname)
            || "form".equals(tagname);
    }

    /**
     * Crée un composant VTML à partir du nom de tag et des attributs
     */
    private MComponent createComponent(String tagname, Map<String, String> attrs, String textContent) {
        switch (tagname) {
            case "minitel" -> {
                String title = attrs.get("title");
                VTMLMinitelComponent minitel = new VTMLMinitelComponent(title);
                minitel.setTextContent(textContent);
                page.setTile(title); // Définir le titre de la page
                return minitel;
            }
            
            case "div" -> {
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                int width = parseInt(attrs.get("width"), 40);
                int height = parseInt(attrs.get("height"), 25);
        
                return new VTMLDivComponent(left, top, width, height, textContent);
            }
            
            case "row" -> {
                return new VTMLRowComponent(textContent);
            }
            
            case "br" -> {
                return new VTMLBrComponent();
            }
            
            case "menu" -> {
                String name = attrs.get("name");
                String keytype = attrs.get("keytype");
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                
                VTMLMenuComponent.KeyType type = "alpha".equals(keytype) ? 
                    VTMLMenuComponent.KeyType.ALPHA : VTMLMenuComponent.KeyType.NUMBER;
                
                return new VTMLMenuComponent(name, type, left, top);
            }
            
            case "item" -> {
                String link = attrs.get("link");
                return new VTMLItemComponent(textContent, link);
            }
            
            case "qrcode" -> {
                String type = attrs.get("type");
                String message = attrs.get("message");
                int scale = parseInt(attrs.get("scale"), 1);
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                
                VTMLQRCodeComponent.QRType qrType = "wpawifi".equals(type) ? 
                    VTMLQRCodeComponent.QRType.WPAWIFI : VTMLQRCodeComponent.QRType.URL;
                
                VTMLQRCodeComponent qrComponent = new VTMLQRCodeComponent(qrType, message, scale);
                qrComponent.setX(left);
                qrComponent.setY(top);
                return qrComponent;
            }
            
            case "img" -> {
                String src = attrs.get("src");
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                int width = parseInt(attrs.get("width"), 32);
                int height = parseInt(attrs.get("height"), 32);
                boolean negative = "true".equalsIgnoreCase(attrs.get("negative"));
                
                // baseUrl sera récupéré depuis Kernel.getConfig() si non défini
                return new VTMLImgComponent(src, left, top, width, height, negative);
            }
            
            case "script" -> {
                // Le contenu du script est dans textContent
                return new VTMLScriptComponent(textContent);
            }
            
            case "form" -> {
                String action = attrs.get("action");
                String method = attrs.get("method");
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                int width = parseInt(attrs.get("width"), 40);
                int height = parseInt(attrs.get("height"), 25);
                return new VTMLFormComponent(action, method, left, top, width, height);
            }
            
            case "input" -> {
                String name = attrs.get("name");
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                int width = parseInt(attrs.get("width"), 20);
                String label = attrs.get("label");
                VTMLInputComponent input = new VTMLInputComponent(name, left, top, width, label);
                
                String placeholder = attrs.get("placeholder");
                if (placeholder != null) {
                    input.setPlaceholder(placeholder);
                }
                
                String value = attrs.get("value");
                if (value != null) {
                    input.setValue(value);
                }
                
                return input;
            }
            
            case "key" -> {
                String name = attrs.get("name");
                String link = attrs.get("link");
                
                VTMLKeyComponent.KeyName keyName = VTMLKeyComponent.parseKeyName(name);
                if (keyName != null && link != null) {
                    // Enregistrer l'association dans la page
                    page.addFunctionKey(keyName.name(), link);
                    return new VTMLKeyComponent(keyName, link);
                }
                System.out.println("⚠️ Tag <key> invalide: name=" + name + ", link=" + link);
                return null;
            }
            
            default -> {
                // Tag non reconnu
                System.out.println("⚠️ Tag VTML non reconnu: " + tagname);
                return null;
            }
        }
    }

    /**
     * Génère les données Minitel à partir du composant racine
     * Note: Les composants gèrent eux-mêmes le rendu de leurs enfants via getString()
     */
    private void renderComponentTree(MComponent component) throws IOException {
        // Générer les données du composant racine (qui inclut ses enfants)
        byte[] data = component.getBytes();
        if (data != null && data.length > 0) {
            page.addData(data);
        }
        
        // Enregistrer les menus pour la navigation
        registerAllMenuItems(component);
    }
    
    /**
     * Parcourt l'arbre pour enregistrer tous les items de menu
     */
    private void registerAllMenuItems(MComponent component) {
        if (component instanceof VTMLMenuComponent menu) {
            registerMenuItems(menu);
        }
        
        // Parcourir les enfants pour trouver d'autres menus
        if (component instanceof ModelMComponent modelComponent) {
            for (MComponent child : modelComponent.getChilds()) {
                registerAllMenuItems(child);
            }
        }
    }

    /**
     * Enregistre les items de menu dans la page
     */
    private void registerMenuItems(VTMLMenuComponent menu) {
        char key = menu.getKeyType() == VTMLMenuComponent.KeyType.ALPHA ? 'A' : '1';
        
        for (MComponent child : ((ModelMComponent) menu).getChilds()) {
            if (child instanceof VTMLItemComponent item) {
                page.addMenu(String.valueOf(key), item.getLink());
                key++;
            }
        }
    }

    /**
     * Parse un entier avec valeur par défaut
     */
    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Retourne le composant racine VTML
     */
    public VTMLMinitelComponent getRootComponent() {
        return rootComponent;
    }

    private static final Pattern HTTP_HOST_PORT = Pattern.compile(
            "^http://(?:"
            + "localhost"
            + "|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(?:\\.(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?))*"
            + "|\\d{1,3}(?:\\.\\d{1,3}){3}"
            + ")"
            + "(?::(6553[0-5]|655[0-3]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}))?"
            + "/?$"
    );

    public static boolean isHttpHostOrHostPort(String s) {
        return HTTP_HOST_PORT.matcher(s).matches();
    }
}
