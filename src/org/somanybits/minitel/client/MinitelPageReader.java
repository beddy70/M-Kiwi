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
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.kernel.Kernel;
import org.somanybits.minitel.GetTeletelCode;
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
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36)")
                    .timeout(15_000)
                    .get();
        } catch (IOException ex) {
            System.err.println("Error reading page: " + ex.getMessage());
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
                } else if (node instanceof TextNode textNode) {
                    // Capturer le texte entre les tags (ex: entre <putchar> dans un <row>)
                    if (depth >= MINITEL_TAG_DEPTH) {
                        String text = textNode.getWholeText();
                        if (!text.isEmpty() && currentComponent instanceof VTMLRowComponent) {
                            // Chercher la map parente
                            MComponent parent = currentComponent;
                            while (parent != null) {
                                if (parent instanceof VTMLMapComponent map) {
                                    map.appendTextChars(text);
                                    break;
                                }
                                parent = (parent instanceof ModelMComponent m) ? m.getParent() : null;
                            }
                        }
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
            System.out.println("=== ARBRE DES COMPOSANTS ===");
            printComponentTree(rootComponent, 0);
            System.out.println("============================");

            renderComponentTree(rootComponent);
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
            LogManager logmgr = Kernel.getInstance().getLogManager();
            logmgr.addLog(LogManager.ANSI_BOLD_WHITE + depth + ">" + LogManager.ANSI_YELLOW
                    + "  ".repeat(depth - MINITEL_TAG_DEPTH) + "<" + tagname + ">");

            // Créer le composant approprié
            MComponent component = createComponent(tagname, attrs, textContent);

            if (component == null) {
                return; // Tag non reconnu
            }

            // Appliquer les attributs communs id et name
            if (component instanceof ModelMComponent modelComp) {
                String id = attrs.get("id");
                String name = attrs.get("name");
                if (id != null) {
                    modelComp.setId(id);
                }
                if (name != null) {
                    modelComp.setName(name);
                }
                // Enregistrer le composant dans la page
                page.addComponent(modelComp);
            }

            // Ajouter à l'arbre
            if (rootComponent == null && component instanceof VTMLMinitelComponent) {
                // Premier composant = racine
                rootComponent = (VTMLMinitelComponent) component;
                currentComponent = rootComponent;
            } else if (currentComponent != null) {
                // Ajouter comme enfant du composant courant
                currentComponent.addChild(component);

                // Si c'est un formulaire, l'enregistrer dans la page
                if (component instanceof VTMLFormComponent formComponent) {
                    page.setForm(formComponent);
                    System.out.println("📋 Formulaire enregistré dans la page");
                }

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
                || component instanceof VTMLFormComponent
                || component instanceof VTMLLayersComponent
                || component instanceof VTMLMapComponent
                || component instanceof VTMLSpriteDefComponent
                || component instanceof VTMLSpriteComponent
                || component instanceof VTMLColormapComponent
                || component instanceof VTMLColorspriteComponent
                || component instanceof VTMLChardefComponent
                || component instanceof VTMLCharComponent
                || component instanceof VTMLRowComponent;  // row peut contenir des putchar
    }

    /**
     * Remonte au composant parent seulement si le tag fermé était un conteneur
     */
    private void closeCurrentComponent(String tagname) {
        // Désactiver le mode colormap quand on ferme le tag colormap
        if ("colormap".equals(tagname)) {
            if (currentComponent instanceof VTMLColormapComponent) {
                MComponent parent = currentComponent.getParent();
                if (parent instanceof VTMLMapComponent map) {
                    map.setParsingColormap(false);
                    System.out.println("🎨 Colormap fermée");
                }
            }
        }

        // Désactiver le mode colorsprite quand on ferme le tag colorsprite
        if ("colorsprite".equals(tagname)) {
            if (currentComponent instanceof VTMLColorspriteComponent) {
                MComponent parent = currentComponent.getParent();
                if (parent instanceof VTMLSpriteComponent sprite) {
                    sprite.setParsingColorsprite(false);
                    System.out.println("🎨 Colorsprite fermée");
                }
            }
        }
        
        // Terminer le caractère quand on ferme le tag char
        if ("char".equals(tagname)) {
            if (currentComponent instanceof VTMLCharComponent) {
                MComponent parent = currentComponent.getParent();
                if (parent instanceof VTMLChardefComponent chardef) {
                    chardef.endChar();
                    System.out.println("🎨 Char terminé");
                }
            }
        }
        
        // Terminer le row quand on ferme le tag row (pour les putchar)
        if ("row".equals(tagname)) {
            if (currentComponent instanceof VTMLRowComponent row) {
                MComponent parent = currentComponent.getParent();
                if (parent instanceof VTMLMapComponent map) {
                    map.endRow(row.getRepeat());
                    System.out.println("📝 Row terminé avec putchar");
                }
                // Remonter au parent seulement si c'était un VTMLRowComponent
                if (currentComponent.getParent() != null) {
                    currentComponent = currentComponent.getParent();
                }
            }
            // Ne pas remonter si le row n'était pas un conteneur (cas du row avec texte direct)
            return;
        }

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
                || "form".equals(tagname)
                || "layers".equals(tagname)
                || "map".equals(tagname)
                || "spritedef".equals(tagname)
                || "sprite".equals(tagname)
                || "colormap".equals(tagname)
                || "colorsprite".equals(tagname)
                || "chardef".equals(tagname)
                || "char".equals(tagname)
                || "row".equals(tagname);  // row peut contenir des putchar
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
                // Gérer l'attribut repeat (1-100, défaut 1)
                String repeatAttr = attrs.get("repeat");
                int repeat = parseRepeat(repeatAttr);
                if (repeatAttr != null) {
                    System.out.println("🔄 Row repeat attr='" + repeatAttr + "' -> " + repeat);
                }
                
                // Si le parent est une colormap, ajouter la ligne à la map parente
                if (currentComponent instanceof VTMLColormapComponent) {
                    // Remonter à la map parente
                    MComponent parent = currentComponent.getParent();
                    if (parent instanceof VTMLMapComponent map) {
                        System.out.println("🎨 Colormap row: '" + textContent + "' x" + repeat);
                        for (int i = 0; i < repeat; i++) {
                            map.addRow(textContent);  // addRow gère le mode colormap
                        }
                        return null;
                    }
                }
                // Si le parent est une map
                if (currentComponent instanceof VTMLMapComponent map) {
                    // Si le row a du contenu texte direct, l'ajouter immédiatement
                    if (textContent != null && !textContent.isEmpty()) {
                        System.out.println("📝 Map row: '" + textContent + "' x" + repeat);
                        for (int i = 0; i < repeat; i++) {
                            map.addRow(textContent);
                        }
                        return null;
                    }
                    // Sinon, démarrer un row qui sera rempli par des <putchar>
                    map.startRow();
                    System.out.println("📝 Map row démarré (attente putchar)");
                    VTMLRowComponent row = new VTMLRowComponent("");
                    row.setRepeat(repeat);
                    return row;
                }
                return new VTMLRowComponent(textContent);
            }

            case "colormap" -> {
                // Activer le mode colormap sur la map parente
                if (currentComponent instanceof VTMLMapComponent map) {
                    map.setParsingColormap(true);
                    System.out.println("🎨 Colormap détectée");
                }
                return new VTMLColormapComponent();
            }

            case "colorsprite" -> {
                // Activer le mode colorsprite sur le sprite parent
                if (currentComponent instanceof VTMLSpriteComponent sprite) {
                    sprite.setParsingColorsprite(true);
                    System.out.println("🎨 Colorsprite détectée");
                }
                return new VTMLColorspriteComponent();
            }

            case "line" -> {
                // Gérer l'attribut repeat (1-100, défaut 1)
                String repeatAttr = attrs.get("repeat");
                int repeat = parseRepeat(repeatAttr);
                if (repeatAttr != null) {
                    System.out.println("🔄 Line repeat attr='" + repeatAttr + "' -> " + repeat);
                }
                
                // Si le parent est un char (dans chardef), ajouter la ligne au chardef
                if (currentComponent instanceof VTMLCharComponent) {
                    MComponent parent = currentComponent.getParent();
                    if (parent instanceof VTMLChardefComponent chardef) {
                        System.out.println("🎨 Chardef line: '" + textContent + "'");
                        for (int i = 0; i < repeat; i++) {
                            chardef.addLine(textContent);
                        }
                        return null;
                    }
                }
                
                // Si le parent est une colorsprite, ajouter la ligne de couleur
                System.out.println("📝 Data tag - currentComponent=" + currentComponent.getClass().getSimpleName() + ", text='" + textContent + "' x" + repeat);
                if (currentComponent instanceof VTMLColorspriteComponent) {
                    // Remonter au sprite parent
                    MComponent parent = currentComponent.getParent();
                    if (parent instanceof VTMLSpriteComponent sprite) {
                        System.out.println("🎨 Colorsprite line: '" + textContent + "' x" + repeat);
                        for (int i = 0; i < repeat; i++) {
                            sprite.addLine(textContent);  // addLine gère le mode colorsprite
                        }
                        return null;
                    }
                }
                // Si le parent est un sprite, ajouter la ligne
                if (currentComponent instanceof VTMLSpriteComponent sprite) {
                    System.out.println("📝 Sprite data added: '" + textContent + "' x" + repeat);
                    for (int i = 0; i < repeat; i++) {
                        sprite.addLine(textContent);
                    }
                    return null;
                }
                return null;
            }

            case "br" -> {
                return new VTMLBrComponent();
            }

            case "menu" -> {
                String name = attrs.get("name");
                String keytype = attrs.get("keytype");
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);

                VTMLMenuComponent.KeyType type = "alpha".equals(keytype)
                        ? VTMLMenuComponent.KeyType.ALPHA : VTMLMenuComponent.KeyType.NUMBER;

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

                VTMLQRCodeComponent.QRType qrType;
                if ("wpawifi".equals(type)) {
                    qrType = VTMLQRCodeComponent.QRType.WPAWIFI;
                } else if ("vcard".equals(type)) {
                    qrType = VTMLQRCodeComponent.QRType.VCARD;
                } else {
                    qrType = VTMLQRCodeComponent.QRType.URL;
                }

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
                String style = attrs.get("style");  // "dithering", "bitmap", ou null (couleur)

                // baseUrl sera récupéré depuis Kernel.getConfig() si non défini
                return new VTMLImgComponent(src, left, top, width, height, negative, style);
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
                int size = parseInt(attrs.get("size"), 20);
                String label = attrs.get("label");
                VTMLInputComponent input = new VTMLInputComponent(name, left, top, size, label);

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

            case "color" -> {
                String ink = attrs.get("ink");
                String background = attrs.get("background");
                return new VTMLColorComponent(ink, background, textContent);
            }

            case "blink" -> {
                return new VTMLBlinkComponent(textContent);
            }

            case "status" -> {
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                int width = parseInt(attrs.get("width"), 40);
                int height = parseInt(attrs.get("height"), 1);
                VTMLStatusComponent status = new VTMLStatusComponent(left, top, width, height);
                // Enregistrer le status dans la page
                page.setStatus(status);
                return status;
            }

            // ========== TAGS LAYERS (JEUX) ==========
            case "layers" -> {
                int left = parseInt(attrs.get("left"), 0);
                int top = parseInt(attrs.get("top"), 0);
                int width = parseInt(attrs.get("width"), 40);
                int height = parseInt(attrs.get("height"), 24);
                VTMLLayersComponent layers = new VTMLLayersComponent(left, top, width, height);
                // Enregistrer le layers dans la page et vice-versa
                page.setLayers(layers);
                layers.setPage(page);
                return layers;
            }

            case "map" -> {
                String typeStr = attrs.get("type");
                VTMLMapComponent.MapType mapType = "bitmap".equals(typeStr)
                        ? VTMLMapComponent.MapType.BITMAP : VTMLMapComponent.MapType.CHAR;
                VTMLMapComponent map = new VTMLMapComponent(mapType);
                // Ajouter au layers parent
                if (currentComponent instanceof VTMLLayersComponent layers) {
                    layers.addArea(map);
                }
                return map;
            }

            case "spritedef" -> {
                String id = attrs.get("id");
                int width = parseInt(attrs.get("width"), 8);
                int height = parseInt(attrs.get("height"), 8);
                String typeStr = attrs.get("type");
                VTMLSpriteDefComponent.SpriteType spriteType = "bitmap".equals(typeStr)
                        ? VTMLSpriteDefComponent.SpriteType.BITMAP : VTMLSpriteDefComponent.SpriteType.CHAR;
                VTMLSpriteDefComponent spriteDef = new VTMLSpriteDefComponent(width, height, spriteType);
                spriteDef.setId(id); // Définir l'id AVANT d'ajouter au layers
                // Ajouter au layers parent
                if (currentComponent instanceof VTMLLayersComponent layers) {
                    layers.addSpriteDef(spriteDef);
                    System.out.println("🎮 SpriteDef ajouté: id=" + id);
                }
                return spriteDef;
            }

            case "sprite" -> {
                VTMLSpriteComponent sprite = new VTMLSpriteComponent();
                System.out.println("🎮 Sprite frame créé, parent=" + currentComponent.getClass().getSimpleName());
                // Ajouter au spritedef parent
                if (currentComponent instanceof VTMLSpriteDefComponent spriteDef) {
                    spriteDef.addFrame(sprite);
                    System.out.println("🎮 Sprite frame ajouté au spriteDef");
                }
                return sprite;
            }

            case "keypad" -> {
                String action = attrs.get("action");
                String keyStr = attrs.get("key");
                String event = attrs.get("event");
                int player = parseInt(attrs.get("player"), 0);  // 0 = joueur 1, 1 = joueur 2
                char key = (keyStr != null && !keyStr.isEmpty()) ? keyStr.charAt(0) : ' ';
                VTMLKeypadComponent keypad = new VTMLKeypadComponent(action, key, event, player);
                System.out.println("🎮 Keypad: player=" + player + ", action=" + action + ", key=" + key + ", event=" + event);
                // Chercher le layers parent (peut être plus haut dans l'arbre)
                MComponent parent = currentComponent;
                while (parent != null) {
                    if (parent instanceof VTMLLayersComponent layers) {
                        layers.addKeypad(keypad);
                        System.out.println("🎮 Keypad ajouté au layers");
                        break;
                    }
                    parent = (parent instanceof ModelMComponent m) ? m.getParent() : null;
                }
                return keypad;
            }

            case "timer" -> {
                String event = attrs.get("event");
                int interval = parseInt(attrs.get("interval"), 200);
                System.out.println("🎮 Timer: event=" + event + ", interval=" + interval + "ms");
                // Chercher le layers parent
                MComponent parent = currentComponent;
                while (parent != null) {
                    if (parent instanceof VTMLLayersComponent layers) {
                        layers.setTickFunction(event, interval);
                        System.out.println("🎮 Timer configuré sur layers");
                        break;
                    }
                    parent = (parent instanceof ModelMComponent m) ? m.getParent() : null;
                }
                return null;  // Pas de composant visuel
            }

            case "label" -> {
                String id = attrs.get("id");
                int x = parseInt(attrs.get("x"), 0);
                int y = parseInt(attrs.get("y"), 0);
                int labelWidth = parseInt(attrs.get("width"), 10);
                String text = textContent != null ? textContent.trim() : "";
                String visibility = attrs.get("visibility");
                VTMLLabelComponent label = new VTMLLabelComponent(id, x, y, labelWidth, text);
                if ("hidden".equals(visibility)) {
                    label.setVisible(false);
                }
                System.out.println("🏷️ Label: id=" + id + ", pos=(" + x + "," + y + "), width=" + labelWidth + ", text='" + text + "'" + (label.isVisible() ? "" : ", hidden"));
                // Chercher le layers parent
                MComponent parent = currentComponent;
                while (parent != null) {
                    if (parent instanceof VTMLLayersComponent layers) {
                        layers.addLabel(label);
                        System.out.println("🏷️ Label ajouté au layers");
                        break;
                    }
                    parent = (parent instanceof ModelMComponent m) ? m.getParent() : null;
                }
                return label;
            }

            case "chardef" -> {
                String name = attrs.get("name");
                String type = attrs.get("type");
                VTMLChardefComponent chardef = new VTMLChardefComponent(name, type);
                System.out.println("🎨 Chardef créé: name=" + name + ", type=" + type);
                // Enregistrer dans la page pour accès ultérieur
                if (name != null) {
                    page.addChardef(name, chardef);
                }
                return chardef;
            }

            case "char" -> {
                // Démarrer un nouveau caractère dans le chardef parent
                if (currentComponent instanceof VTMLChardefComponent chardef) {
                    chardef.startChar();
                    System.out.println("🎨 Char démarré dans chardef");
                }
                return new VTMLCharComponent();
            }

            case "putchar" -> {
                // Debug: afficher tous les attributs
                System.out.println("🎨 Putchar attrs: " + attrs);
                int index = parseInt(attrs.get("index"), 0);
                int repeat = parseRepeat(attrs.get("repeat"));
                String chardefName = attrs.get("chardef");  // Optionnel
                String codeAttr = attrs.get("code");  // Code direct du caractère semi-graphique
                
                char mosaicChar;
                
                if (codeAttr != null) {
                    // Mode direct : utiliser le code semi-graphique fourni
                    int code = parseInt(codeAttr, 0x20);
                    mosaicChar = (char) code;
                    System.out.println("🎨 Putchar direct: code=0x" + Integer.toHexString(code) + ", repeat=" + repeat);
                } else {
                    // Mode chardef : récupérer le caractère depuis le chardef
                    VTMLChardefComponent chardef = page.getChardef(chardefName);
                    if (chardef == null) {
                        System.err.println("⚠️ putchar: chardef '" + chardefName + "' non trouvé et pas de code direct");
                        return null;
                    }
                    mosaicChar = chardef.getChar(index);
                    System.out.println("🎨 Putchar chardef: index=" + index + ", repeat=" + repeat + ", char=0x" + Integer.toHexString(mosaicChar));
                }
                
                // Générer les caractères
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < repeat; i++) {
                    sb.append(mosaicChar);
                }
                String chars = sb.toString();
                
                // Chercher la map parente (peut être le parent direct ou via un row)
                MComponent parent = currentComponent;
                while (parent != null) {
                    if (parent instanceof VTMLMapComponent map) {
                        map.appendMosaicChars(chars);
                        System.out.println("🎨 Putchar ajouté à la map: '" + chars + "'");
                        return null;
                    }
                    parent = (parent instanceof ModelMComponent m) ? m.getParent() : null;
                }
                System.err.println("⚠️ putchar: pas de map parente trouvée");
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
     * Génère les données Minitel à partir du composant racine Note: Les
     * composants gèrent eux-mêmes le rendu de leurs enfants via getString()
     */
    private void renderComponentTree(MComponent component) throws IOException {
        // 0. Réinitialiser le contexte de page (pas le storage)
        VTMLScriptEngine.getInstance().resetPageContext();
        // 1. D'abord exécuter tous les scripts pour définir les fonctions
        executeAllScripts(component);

        // 2. Définir la page courante pour getElementById/getElementByName
        VTMLScriptEngine.getInstance().setCurrentPage(page);

        // 3. Trouver le layers et le passer au JavaScript
        VTMLLayersComponent layers = findLayers(component);
        if (layers != null) {
            VTMLScriptEngine.getInstance().setCurrentLayers(layers);
        }

        // 4. Appeler domReady() pour initialiser les sprites
        VTMLScriptEngine.getInstance().callDomReady();

        // 5. Générer les données du composant racine (qui inclut ses enfants)
        byte[] data = component.getBytes();
        if (data != null && data.length > 0) {
            page.addData(data);
        }

        // Enregistrer les menus pour la navigation
        registerAllMenuItems(component);
    }

    /**
     * Trouve le premier VTMLLayersComponent dans l'arbre
     */
    private VTMLLayersComponent findLayers(MComponent component) {
        if (component instanceof VTMLLayersComponent layers) {
            return layers;
        }
        if (component instanceof ModelMComponent modelComponent) {
            for (MComponent child : modelComponent.getChilds()) {
                VTMLLayersComponent found = findLayers(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Parcourt l'arbre pour exécuter tous les scripts
     */
    private void executeAllScripts(MComponent component) {
        if (component instanceof VTMLScriptComponent script) {
            String content = script.getScriptContent();
            if (content != null && !content.trim().isEmpty()) {
                try {
                    VTMLScriptEngine.getInstance().execute(content);
                    System.out.println("📜 Script exécuté");
                } catch (Exception e) {
                    System.err.println("Erreur script: " + e.getMessage());
                }
            }
        }

        // Parcourir les enfants
        if (component instanceof ModelMComponent modelComponent) {
            for (MComponent child : modelComponent.getChilds()) {
                executeAllScripts(child);
            }
        }
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
     * Parse l'attribut repeat avec garde-fou (1-100, défaut 1)
     */
    private int parseRepeat(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 1;
        }
        try {
            int repeat = Integer.parseInt(value.trim());
            if (repeat < 1) return 1;
            if (repeat > 100) {
                System.err.println("⚠️ repeat=" + repeat + " trop grand, limité à 100");
                return 100;
            }
            return repeat;
        } catch (NumberFormatException e) {
            return 1;
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
