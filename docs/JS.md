# JavaScript VTML — Référence complète

Le moteur JavaScript de M-Kiwi est **Mozilla Rhino** exécuté côté serveur. Les scripts sont écrits dans les balises `<script>` des pages VTML et partagent un **scope global unique** pour toute la session client.

## Table des matières

- [Cycle de vie](#cycle-de-vie)
- [Variables globales](#variables-globales)
- [Fonctions globales](#fonctions-globales)
- [Objet `storage`](#objet-storage) — persistance entre pages
- [Objet `_teletel`](#objet-_teletel) — écriture directe sur le terminal
- [Objet `joystick`](#objet-joystick) — manette USB
- [API Layers](#api-layers) — zone de jeu
  - [API SpriteInstance](#api-spriteinstance) — sprite individuel
- [API DOM](#api-dom) — manipulation des composants VTML
  - [Méthodes par type d'élément](#méthodes-par-type-délément)
- [Classes Java accessibles](#classes-java-accessibles)
- [Exemples](#exemples)
  - [Horloge temps réel](#exemple-horloge-temps-réel)
  - [Score persistant](#exemple-score-persistant)
  - [Navigation conditionnelle](#exemple-navigation-conditionnelle)
  - [DOM dynamique](#exemple-dom-dynamique)
  - [Requête HTTP](#exemple-requête-http)

---

## Cycle de vie

```
Chargement page
     │
     ▼
 <script> exécuté (corps, hors fonctions)
     │
     ▼
 domReady() appelé (si définie)
     │
     ▼
 Boucle : attente événements
     │    ├─ keypad → fonction JS
     │    ├─ <timer> → fonction JS
     │    └─ navigation → nouvelle page
     ▼
 Nouvelle page → resetPageContext()
 (domReady, output, _currentLayers, _currentPage remis à null)
 (storage conservé)
```

**Points clés :**
- Le scope JS est **partagé entre toutes les pages** — les variables déclarées avec `var` survivent à la navigation sauf celles réinitialisées par `resetPageContext()`.
- `storage` est le seul mécanisme de persistance volontaire entre pages.
- Les scripts de jeux (`<timer>`, `<keypad>`) sont synchronisés — pas de concurrence entre appels.

---

## Variables globales

Ces variables sont pré-définies et mises à jour automatiquement par le serveur.

| Variable           | Type              | Description                                              |
|--------------------|-------------------|----------------------------------------------------------|
| `_currentLayers`   | VTMLLayersComponent | Zone de jeu active (null hors jeu)                     |
| `_currentPage`     | Page              | Page courante (accès aux composants)                     |
| `_teletel`         | Teletel           | Instance pour écriture directe sur le terminal           |
| `_joystickMapping` | JoystickMapping   | Configuration du mapping joystick (null si absent)       |
| `_joystickRumble`  | JoystickRumble    | Contrôle du rumble (null si non supporté)                |
| `_pendingNavigation` | string          | URL de navigation en attente (interne, ne pas modifier)  |
| `_pendingFocus`    | string            | Focus en attente (interne, ne pas modifier)              |
| `output`           | string            | Texte injecté dans la page au rendu (remis à null entre pages) |

### Variable `output`

Assigner `output` dans un `<script>` injecte le texte directement dans le flux de la page :

```xml
<script>
  var d = new Date();
  output = "Bonjour, il est " + d.getHours() + "h" + d.getMinutes();
</script>
```

---

## Fonctions globales

Disponibles dans tous les scripts, sans import.

| Fonction                   | Retour   | Description                                                  |
|----------------------------|----------|--------------------------------------------------------------|
| `domReady()`               | —        | **Callback** — appelée après le rendu complet de la page     |
| `debug(msg)`               | —        | Affiche `msg` dans la console Java du serveur                |
| `getElementById(id)`       | composant | Trouve un composant VTML par son attribut `id`              |
| `getElementByName(name)`   | composant | Trouve un composant VTML par son attribut `name`            |
| `gotoPage(url)`            | —        | Déclenche une navigation vers `url` après l'exécution du script |
| `setFocus(componentName)`  | —        | Donne le focus à un composant (menu ou input) par son `name` |
| `enableLineZero(bool)`     | —        | Autorise (`true`) ou interdit (`false`) l'écriture sur la ligne 0 |
| `isLineZeroEnabled()`      | boolean  | Retourne l'état de la protection ligne 0                     |
| `getConfig()`              | Config   | Retourne la configuration du serveur                         |
| `importClass(className)`   | class    | Importe une classe Java par son nom complet                  |

### `domReady()`

Appelée automatiquement après que tous les composants de la page ont été rendus. Idéale pour initialiser les sprites, labels, et autres éléments dépendant du rendu.

```javascript
function domReady() {
  var layers = _currentLayers;
  var player = layers.getSprite("player");
  player.show(0);
  player.move(20, 10);
  layers.setText("score", "Score: 0");
}
```

### `gotoPage(url)`

La navigation est **différée** : la page courante termine son script avant de naviguer.

```javascript
function checkLogin() {
  var user = storage.get("user");
  if (user == null) {
    gotoPage("login.vtml");    // redirige vers login
  } else {
    gotoPage("welcome.vtml");  // redirige vers accueil
  }
}
```

### `debug(msg)`

```javascript
debug("ballX=" + ballX + " ballY=" + ballY);
// → affiche dans la console Java : 🔧 JS: ballX=20 ballY=5
```

---

## Objet `storage`

Persistance de données entre les navigations de pages. Le storage survit pour toute la durée de la session (jusqu'à déconnexion).

| Méthode                          | Retour | Description                                        |
|----------------------------------|--------|----------------------------------------------------|
| `storage.set(key, value)`        | —      | Stocke `value` sous la clé `key`                   |
| `storage.get(key)`               | any    | Lit la valeur, `undefined` si absente              |
| `storage.get(key, defaultValue)` | any    | Lit la valeur, `defaultValue` si absente           |
| `storage.remove(key)`            | —      | Supprime la clé                                    |
| `storage.clear()`                | —      | Vide tout le storage                               |

```javascript
// Page 1 : stocker le nom saisi dans un formulaire
storage.set("username", "Alice");
storage.set("score", 0);

// Page 2 : relire et afficher
var name = storage.get("username", "Inconnu");
var score = storage.get("score", 0);
output = "Bonjour " + name + " — Score: " + score;

// Incrémenter un compteur persistant
storage.set("visits", storage.get("visits", 0) + 1);
```

---

## Objet `_teletel`

Accès direct au terminal Minitel pour écrire des données en dehors du cycle de rendu de page. Principalement utilisé dans les **timers de page** (horloge, animations).

| Méthode                         | Description                                         |
|---------------------------------|-----------------------------------------------------|
| `_teletel.setCursor(x, y)`      | Positionne le curseur à la colonne `x`, ligne `y`   |
| `_teletel.setTextColor(color)`  | Définit la couleur du texte (0–7)                   |
| `_teletel.setBGColor(color)`    | Définit la couleur de fond (0–7)                    |
| `_teletel.writeString(text)`    | Envoie du texte au terminal                         |
| `_teletel.setBlink(bool)`       | Active/désactive le clignotement                    |
| `_teletel.clear()`              | Efface tout l'écran                                 |
| `_teletel.setCursorHome()`      | Positionne le curseur en haut à gauche              |

### Constantes de couleur (`Teletel`)

| Constante              | Valeur | Couleur  |
|------------------------|--------|----------|
| `Teletel.COLOR_BLACK`  | 0      | Noir     |
| `Teletel.COLOR_RED`    | 1      | Rouge    |
| `Teletel.COLOR_GREEN`  | 2      | Vert     |
| `Teletel.COLOR_YELLOW` | 3      | Jaune    |
| `Teletel.COLOR_BLUE`   | 4      | Bleu     |
| `Teletel.COLOR_MAGENTA`| 5      | Magenta  |
| `Teletel.COLOR_CYAN`   | 6      | Cyan     |
| `Teletel.COLOR_WHITE`  | 7      | Blanc    |

**Règle fond coloré** : pour activer la couleur de fond sur le Minitel, commencer la chaîne par un espace (le premier caractère "repeint" la cellule courante).

```javascript
// Horloge dans un timer
function updateClock() {
  var d = new Date();
  var h = ("0" + d.getHours()).slice(-2);
  var m = ("0" + d.getMinutes()).slice(-2);
  var s = ("0" + d.getSeconds()).slice(-2);
  _teletel.setCursor(1, 22);
  _teletel.setTextColor(Teletel.COLOR_YELLOW);
  _teletel.setBGColor(Teletel.COLOR_BLUE);
  _teletel.writeString(" " + h + ":" + m + ":" + s);
}

// Afficher un message clignotant
function showAlert(msg) {
  _teletel.setCursor(0, 0);
  _teletel.setTextColor(Teletel.COLOR_RED);
  _teletel.setBlink(true);
  _teletel.writeString(msg);
  _teletel.setBlink(false);
}
```

---

## Objet `joystick`

Interface pour les manettes de jeu USB. Disponible uniquement si une manette est connectée (`_joystickMapping != null`).

| Méthode                               | Description                                              |
|---------------------------------------|----------------------------------------------------------|
| `joystick.mapButton(button, action)`  | Associe un bouton physique à une action (LEFT, RIGHT…)   |
| `joystick.mapAxis(axis, action)`      | Associe un axe analogique à une action (`"0+"`, `"1-"…`) |
| `joystick.setThreshold(threshold)`    | Seuil de déclenchement des axes (0–32767, défaut 8000)   |
| `joystick.printMapping()`             | Affiche le mapping actuel dans la console Java           |
| `joystick.resetMapping()`             | Remet le mapping par défaut                              |
| `joystick.rumble(durationMs)`         | Vibration pendant `durationMs` ms                        |
| `joystick.rumble(durationMs, intensity)` | Vibration avec intensité (0.0–1.0)                   |
| `joystick.isRumbleSupported()`        | `true` si la manette supporte le rumble                  |

### Actions disponibles

| Action     | Description               |
|------------|---------------------------|
| `LEFT`     | Direction gauche          |
| `RIGHT`    | Direction droite          |
| `UP`       | Direction haut            |
| `DOWN`     | Direction bas             |
| `ACTION1`  | Bouton action 1           |
| `ACTION2`  | Bouton action 2           |
| `ACTION3`  | Bouton action 3           |
| `ACTION4`  | Bouton action 4           |
| `ACTION5`  | Bouton action 5           |
| `ACTION6`  | Bouton action 6           |

```javascript
function domReady() {
  // Mapper le bouton 0 (croix directionnelle gauche) à LEFT
  joystick.mapButton(0, "LEFT");
  joystick.mapButton(1, "RIGHT");
  joystick.mapButton(2, "ACTION1");

  // Mapper les axes analogiques
  joystick.mapAxis("0-", "LEFT");   // axe 0, direction négative
  joystick.mapAxis("0+", "RIGHT");  // axe 0, direction positive
  joystick.mapAxis("1-", "UP");
  joystick.mapAxis("1+", "DOWN");

  // Seuil plus sensible
  joystick.setThreshold(5000);
}

function onCollision() {
  if (joystick.isRumbleSupported()) {
    joystick.rumble(200, 0.8);  // 200ms, intensité 80%
  }
}
```

---

## API Layers

Accès via la variable globale `_currentLayers` (de type `VTMLLayersComponent`). Disponible uniquement dans les pages contenant un tag `<layers>`.

### Dimensions

| Méthode              | Retour | Description                     |
|----------------------|--------|---------------------------------|
| `layers.getWidth()`  | int    | Largeur de la zone de jeu       |
| `layers.getHeight()` | int    | Hauteur de la zone de jeu       |

### Gestion des sprites

| Méthode                               | Retour         | Description                                          |
|---------------------------------------|----------------|------------------------------------------------------|
| `layers.getSprite(id)`                | SpriteInstance | Retourne l'instance d'un sprite par son `id`         |
| `layers.showSprite(id, frameIndex)`   | —              | Affiche le sprite à la frame `frameIndex`            |
| `layers.hideSprite(id)`               | —              | Cache le sprite                                      |
| `layers.moveSprite(id, x, y)`         | —              | Déplace le sprite à la position `(x, y)`             |

### Collisions

| Méthode                                         | Retour | Description                                               |
|-------------------------------------------------|--------|-----------------------------------------------------------|
| `layers.checkCollision(id1, id2)`               | boolean | `true` si les boîtes englobantes se chevauchent          |
| `layers.checkMapCollision(spriteId)`            | int    | Code ASCII du caractère de map touché, ou `0`             |
| `layers.checkMapCollisionAt(spriteId, x, y)`   | int    | Idem, teste à une position hypothétique sans déplacer     |

> En JavaScript, les `char` Java sont convertis en `int` (code ASCII). `'#'` = 35, `' '` = 32, absence = 0.

```javascript
// Tester avant de déplacer (déplacement sécurisé)
function moveRight() {
  var layers = _currentLayers;
  var newX = playerX + 1;
  var hit = layers.checkMapCollisionAt("player", newX, playerY);
  if (hit == 0 || hit == 32) {  // 0=vide, 32=espace
    playerX = newX;
    layers.getSprite("player").move(playerX, playerY);
  }
}
```

### Manipulation des maps

| Méthode                                                    | Retour | Description                                        |
|------------------------------------------------------------|--------|----------------------------------------------------|
| `layers.getMapChar(areaIndex, x, y)`                       | int    | Code ASCII du caractère à `(x, y)` dans la map     |
| `layers.setMapChar(areaIndex, x, y, char)`                 | —      | Pose le caractère `char` à `(x, y)`                |
| `layers.setMapPutchar(areaIndex, x, y, chardefName, index)`| —      | Pose un caractère mosaïque (chardef) à `(x, y)`    |
| `layers.clearMapLine(areaIndex, y)`                        | —      | Efface la ligne `y` (remplace par espaces)          |
| `layers.shiftMap(areaIndex, direction, from, to)`          | —      | Décale le contenu de la map dans une direction      |
| `layers.getMapColor(areaIndex, x, y)`                      | int    | Couleur (0–7) à la position `(x, y)`               |
| `layers.setMapColor(areaIndex, x, y, color)`               | —      | Définit la couleur à `(x, y)`                      |

**Paramètre `areaIndex`** : index de la map (0 = fond, 1 = couche suivante…). Maximum 3 maps.

**Directions pour `shiftMap`** : `"UP"`, `"DOWN"`, `"LEFT"`, `"RIGHT"`.

```javascript
// Tétris : poser un bloc et effacer les lignes complètes
function lockPiece(x, y) {
  var layers = _currentLayers;
  layers.setMapChar(1, x, y, '#');
  layers.setMapColor(1, x, y, 3);  // jaune

  // Vérifier si la ligne est complète (largeur = 10)
  var full = true;
  for (var col = 1; col <= 10; col++) {
    if (layers.getMapChar(1, col, y) == 32) {  // espace = vide
      full = false;
      break;
    }
  }
  if (full) {
    layers.clearMapLine(1, y);
    layers.shiftMap(1, "DOWN", 0, y - 1);  // tout descend d'une ligne
  }
}
```

### Labels

| Méthode                         | Retour  | Description                                     |
|---------------------------------|---------|-------------------------------------------------|
| `layers.setText(id, text)`      | —       | Modifie le texte du label `id`                  |
| `layers.showLabel(id)`          | —       | Rend le label visible                           |
| `layers.hideLabel(id)`          | —       | Cache le label                                  |
| `layers.isLabelVisible(id)`     | boolean | `true` si le label est visible                  |

```javascript
var score = 0;

function addPoints(pts) {
  score += pts;
  _currentLayers.setText("score", "Score: " + score);
}
```

### Son

| Méthode               | Description                             |
|-----------------------|-----------------------------------------|
| `layers.beep()`       | Émet un bip sonore sur le Minitel       |

---

## API SpriteInstance

Objet retourné par `layers.getSprite(id)`.

| Méthode                       | Retour | Description                                          |
|-------------------------------|--------|------------------------------------------------------|
| `sprite.show(frameIndex)`     | —      | Affiche la frame `frameIndex` (0-based)              |
| `sprite.show(frameIndex, color)` | —   | Affiche avec une couleur spécifique (0–7)            |
| `sprite.hide()`               | —      | Cache le sprite                                      |
| `sprite.move(x, y)`           | —      | Déplace et réaffiche à `(x, y)`                      |
| `sprite.setX(x)`              | —      | Modifie uniquement la position X                     |
| `sprite.setY(y)`              | —      | Modifie uniquement la position Y                     |
| `sprite.getX()`               | int    | Position X courante                                  |
| `sprite.getY()`               | int    | Position Y courante                                  |
| `sprite.getWidth()`           | int    | Largeur du sprite (en caractères)                    |
| `sprite.getHeight()`          | int    | Hauteur du sprite (en caractères)                    |
| `sprite.getColor()`           | int    | Couleur courante (0–7)                               |
| `sprite.setColor(color)`      | —      | Définit la couleur sans re-afficher                  |

```javascript
var layers = _currentLayers;
var ball = layers.getSprite("ball");

// Afficher en rouge, frame 0
ball.show(0, 1);

// Déplacer progressivement
ball.move(ball.getX() + 1, ball.getY());

// Changer de couleur en fonction de la vie
var lives = storage.get("lives", 3);
ball.show(0, lives > 1 ? 2 : 1);  // vert si > 1 vie, rouge sinon
```

---

## API DOM

Permet de modifier dynamiquement les composants VTML d'une page.

### Accès aux composants

```javascript
var comp = getElementById("monId");      // par attribut id=""
var comp2 = getElementByName("monNom"); // par attribut name=""
```

### Méthodes communes (tous les composants)

| Méthode                         | Retour     | Description                                        |
|---------------------------------|------------|----------------------------------------------------|
| `comp.getId()`                  | string     | Retourne l'`id` du composant                       |
| `comp.setId(id)`                | —          | Modifie l'`id`                                     |
| `comp.getName()`                | string     | Retourne le `name`                                 |
| `comp.setName(name)`            | —          | Modifie le `name`                                  |
| `comp.getX()`                   | int        | Position X                                         |
| `comp.setX(x)`                  | —          | Modifie la position X                              |
| `comp.getY()`                   | int        | Position Y                                         |
| `comp.setY(y)`                  | —          | Modifie la position Y                              |
| `comp.getWidth()`               | int        | Largeur                                            |
| `comp.setWidth(w)`              | —          | Modifie la largeur                                 |
| `comp.getHeight()`              | int        | Hauteur                                            |
| `comp.setHeight(h)`             | —          | Modifie la hauteur                                 |
| `comp.isVisible()`              | boolean    | `true` si visible                                  |
| `comp.setVisible(bool)`         | —          | Affiche ou cache le composant                      |
| `comp.createElement(tagName)`   | composant  | Crée un enfant et l'ajoute automatiquement         |
| `comp.appendChild(child)`       | —          | Ajoute un composant enfant existant                |
| `comp.removeChild(child)`       | boolean    | Retire un enfant (`true` si trouvé et retiré)      |
| `comp.clearChildren()`          | —          | Retire tous les enfants                            |

> **Important** : `createElement()` ajoute automatiquement l'enfant au composant. Ne pas appeler `appendChild()` en plus, sinon l'élément sera dupliqué.

### Méthodes par type d'élément

| Type      | Méthodes spécifiques                                               |
|-----------|--------------------------------------------------------------------|
| `row`     | `setText(text)`                                                    |
| `div`     | `setX(x)`, `setY(y)`, `setWidth(w)`, `setHeight(h)`               |
| `color`   | `setInk(colorName)`, `getInk()`, `setBackground(colorName)`, `getBackground()`, `setText(text)` |
| `blink`   | `setText(text)`                                                    |
| `label`   | `setText(text)`, `setX(x)`, `setY(y)`                             |

**Valeurs de couleur pour `setInk` / `setBackground`** : `"black"`, `"red"`, `"green"`, `"yellow"`, `"blue"`, `"magenta"`, `"cyan"`, `"white"` (ou équivalents français).

```javascript
function domReady() {
  var container = getElementById("results");

  // Ajouter des lignes dynamiquement
  var items = ["Alice", "Bob", "Charlie"];
  for (var i = 0; i < items.length; i++) {
    var row = container.createElement("row");
    row.setText((i + 1) + ". " + items[i]);
  }

  // Ajouter un texte coloré
  var color = container.createElement("color");
  color.setInk("yellow");
  color.setText("Fin de liste");

  // Modifier la position d'un div
  var panel = getElementByName("panel");
  panel.setY(10);
  panel.setVisible(true);
}
```

---

## Classes Java accessibles

Seules ces classes peuvent être utilisées via `Packages.` ou via les alias pré-définis.

| Alias JS / Classe complète                          | Description                                     |
|-----------------------------------------------------|-------------------------------------------------|
| `Kernel`                                            | Accès au noyau (`Kernel.getInstance()`)         |
| `Config`                                            | Structure de configuration                      |
| `GetTeletelCode`                                    | Génération de codes Vidéotex                    |
| `Teletel`                                           | Constantes de couleur (`Teletel.COLOR_*`)       |
| `java.lang.Math`                                    | Fonctions mathématiques                         |
| `java.util.Date`                                    | Date et heure                                   |
| `java.net.URL`                                      | Requêtes HTTP                                   |
| `java.net.HttpURLConnection`                        | Connexions HTTP avancées                        |
| `java.io.BufferedReader`                            | Lecture de flux texte                           |
| `java.io.InputStreamReader`                         | Conversion de flux en texte                     |
| `org.somanybits.minitel.components.vtml.VTMLFactory`| Création dynamique de composants               |

```javascript
// Accès à la configuration serveur
var config = getConfig();
var port = config.server.port;
debug("Serveur sur le port " + port);

// Constantes de couleur
var rouge = Teletel.COLOR_RED;   // = 1
var bleu  = Teletel.COLOR_BLUE;  // = 4
```

---

## Exemples

### Exemple : horloge temps réel

Utilise un `<timer>` de page et `_teletel` pour mettre à jour l'heure chaque seconde sans bloquer la navigation.

```xml
<minitel title="Accueil">

  <timer event="updateClock" interval="1000"/>

  <script>
    function updateClock() {
      var d = new Date();
      var h = ("0" + d.getHours()).slice(-2);
      var m = ("0" + d.getMinutes()).slice(-2);
      var s = ("0" + d.getSeconds()).slice(-2);
      _teletel.setCursor(1, 22);
      _teletel.setTextColor(Teletel.COLOR_YELLOW);
      _teletel.setBGColor(Teletel.COLOR_BLUE);
      _teletel.writeString(" " + h + ":" + m + ":" + s);
    }
    updateClock();  // affichage immédiat sans attendre 1 seconde
  </script>

</minitel>
```

---

### Exemple : score persistant

Conservation du score entre plusieurs pages de jeu.

```xml
<!-- game.vtml : jeu -->
<script>
  var score = storage.get("score", 0);

  function addPoints(pts) {
    score += pts;
    storage.set("score", score);
    _currentLayers.setText("score", "Score: " + score);
  }

  function gameOver() {
    storage.set("lastScore", score);
    gotoPage("gameover.vtml");
  }
</script>
```

```xml
<!-- gameover.vtml : écran de fin -->
<script>
  var last = storage.get("lastScore", 0);
  var best = storage.get("bestScore", 0);
  if (last > best) {
    storage.set("bestScore", last);
    best = last;
  }
  output = "Score: " + last + "   Record: " + best;
</script>
```

---

### Exemple : navigation conditionnelle

Redirection selon l'état du storage, exécutée dans `domReady`.

```xml
<script>
  function domReady() {
    var user = storage.get("user");
    if (!user) {
      gotoPage("login.vtml");
    }
  }
</script>
```

---

### Exemple : DOM dynamique

Construction d'une liste de résultats à partir d'un tableau de données.

```xml
<div id="liste" left="2" top="5" width="36" height="15">
  <row>== Résultats ==</row>
</div>

<script>
  function domReady() {
    var liste = getElementById("liste");
    liste.clearChildren();

    var data = ["Alice — 1200pts", "Bob — 980pts", "Charlie — 760pts"];
    for (var i = 0; i < data.length; i++) {
      var row = liste.createElement("row");
      row.setText((i + 1) + ". " + data[i]);
    }

    var color = liste.createElement("color");
    color.setInk("cyan");
    color.setText("--- fin ---");
  }
</script>
```

---

### Exemple : requête HTTP

Récupération de données depuis une API externe.

```xml
<script>
  function fetchJson(urlString) {
    var url = new java.net.URL(urlString);
    var conn = url.openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(3000);
    conn.setReadTimeout(3000);

    var reader = new java.io.BufferedReader(
      new java.io.InputStreamReader(conn.getInputStream())
    );
    var result = "";
    var line;
    while ((line = reader.readLine()) != null) {
      result += line;
    }
    reader.close();
    return result;
  }

  // Récupérer la météo (exemple fictif)
  try {
    var data = fetchJson("http://192.168.1.10/api/meteo");
    output = "Météo: " + data;
  } catch (e) {
    output = "Erreur réseau";
    debug("fetchJson error: " + e);
  }
</script>
```
