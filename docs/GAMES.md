# Créer des jeux Minitel avec VTML

Ce guide explique comment créer des jeux interactifs pour Minitel en utilisant le système de **layers** de VTML.

## Table des matières

1. [Architecture d'un jeu](#architecture-dun-jeu)
2. [Le composant Layers](#le-composant-layers)
3. [Les Maps (décors)](#les-maps-décors)
   - [Colormap (couleurs)](#colormap-couleurs-de-texte)
4. [Les Sprites](#les-sprites)
   - [Colorsprite (couleurs)](#colorsprite-couleurs-par-caractère)
5. [Animations de sprites](#animations-de-sprites)
6. [Contrôles clavier](#contrôles-clavier)
7. [Joystick USB](#joystick-usb)
8. [Game Loop](#game-loop)
9. [Collisions](#collisions)
10. [Interface utilisateur](#interface-utilisateur)
11. [Exemples complets](#exemples-complets)
12. [Conseils de performance](#conseils-de-performance)
13. [Limitations](#limitations)
14. [Ressources](#ressources)

**Annexes**
- [Annexe A : Référence des fonctions JavaScript](#annexe-a--référence-des-fonctions-javascript)
- [Annexe B : Référence des attributs VTML](#annexe-b--référence-des-attributs-vtml)
- [Annexe C : Codes de référence](#annexe-c--codes-de-référence)

---

## Architecture d'un jeu

Un jeu VTML est composé de :

```
┌─────────────────────────────────────┐
│           <minitel>                 │
│  ┌───────────────────────────────┐  │
│  │         <layers>              │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │   Map 0 (décor fond)    │  │  │
│  │  ├─────────────────────────┤  │  │
│  │  │   Map 1 (éléments)      │  │  │
│  │  ├─────────────────────────┤  │  │
│  │  │   Sprites (par-dessus)  │  │  │
│  │  ├─────────────────────────┤  │  │
│  │  │   Labels (texte)        │  │  │
│  │  └─────────────────────────┘  │  │
│  │  + Keypad (contrôles)         │  │
│  │  + Timer (game loop)          │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │         <script>              │  │
│  │   Logique JavaScript du jeu   │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Structure de base

```xml
<?xml version="1.0" encoding="UTF-8"?>
<minitel>
  <!-- Titre du jeu -->
  <div left="0" top="0" width="40" height="1">
    <row>         MON SUPER JEU</row>
  </div>
  
  <!-- Zone de jeu -->
  <layers id="game" left="0" top="1" width="40" height="22">
    <!-- Maps, sprites, contrôles ici -->
  </layers>
  
  <!-- Instructions -->
  <div left="0" top="23" width="40" height="1">
    <row>[Z]Haut [S]Bas [Q]&lt; [D]&gt;</row>
  </div>
  
  <!-- Code du jeu -->
  <script>
    // JavaScript ici
  </script>
</minitel>
```

---

## Le composant Layers

Le `<layers>` est le conteneur principal pour les jeux. Il gère :
- L'empilement des maps (jusqu'à 3)
- Les sprites animés (jusqu'à 16)
- Les labels de texte dynamique
- Le rendu différentiel optimisé

### Attributs

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `id`     | string | - | Identifiant pour JavaScript |
| `left`   | int | 0 | Position X |
| `top`    | int | 0 | Position Y |
| `width`  | int | 40 | Largeur en caractères |
| `height` | int | 24 | Hauteur en lignes |

### Accès depuis JavaScript

```javascript
function getLayers() {
  return _currentLayers;  // Variable globale définie automatiquement
}

function domReady() {
  var layers = getLayers();
  // Initialiser le jeu ici
}
```

---

## Les Maps (décors)

Les maps sont des grilles de caractères qui forment le décor du jeu.

### Syntaxe

```xml
<map>
  <row>########################################</row>
  <row>#                                      #</row>
  <row>#                                      #</row>
  <row>########################################</row>
</map>
```

### Empilement de maps

Vous pouvez empiler jusqu'à 3 maps. Les caractères espaces sont transparents.

```xml
<!-- Map 0 : Fond (bordures) -->
<map>
  <row>########</row>
  <row>#      #</row>
  <row>########</row>
</map>

<!-- Map 1 : Éléments dynamiques -->
<map>
  <row>        </row>
  <row>  XX    </row>
  <row>        </row>
</map>
```

### Modification dynamique

Vous pouvez modifier les caractères d'une map en JavaScript :

```javascript
// Placer un caractère
// mapIndex: 0 = première map, 1 = deuxième, etc.
layers.setMapChar(mapIndex, x, y, '#');

// Effacer une ligne entière (remplace par des espaces)
layers.clearMapLine(mapIndex, y);

// Décaler les lignes vers le bas (pour Tetris)
// Décale les lignes de fromY à toY, vide la ligne fromY
layers.shiftMapDown(mapIndex, fromY, toY);
```

**Note** : L'index de map correspond à l'ordre de déclaration dans le VTML (0 = première `<map>`).

### Colormap (couleurs de texte)

Chaque map peut avoir une **colormap** associée qui définit la couleur du texte (ink) pour chaque caractère. La colormap fonctionne en mode différentiel : seules les couleurs définies sont appliquées.

#### Codes couleur

| Caractère | Couleur | Code |
|-----------|---------|------|
| `0` | Noir | 0 |
| `1` | Rouge | 1 |
| `2` | Vert | 2 |
| `3` | Jaune | 3 |
| `4` | Bleu | 4 |
| `5` | Magenta | 5 |
| `6` | Cyan | 6 |
| `7` ou espace | Blanc | 7 |

#### Syntaxe VTML

```xml
<map type="char">
  <row>########################################</row>
  <row>#                                      #</row>
  <row>########################################</row>
  <colormap>
    <row>1111111111111111111111111111111111111111</row>
    <row>7                                      7</row>
    <row>2222222222222222222222222222222222222222</row>
  </colormap>
</map>
```

Dans cet exemple :
- La première ligne de `#` sera en **rouge** (1)
- Les bordures de la deuxième ligne seront en **blanc** (7)
- La dernière ligne sera en **vert** (2)

#### Modification dynamique des couleurs

```javascript
// Lire la couleur à une position
var color = layers.getMapColor(mapIndex, x, y);

// Modifier la couleur à une position
layers.setMapColor(mapIndex, x, y, 1);  // 1 = rouge

// Exemple : placer un bloc coloré
layers.setMapChar(0, x, y, '#');
layers.setMapColor(0, x, y, 3);  // Jaune
```

**Note** : Les fonctions `clearMapLine()` et `shiftMapDown()` gèrent automatiquement les couleurs (remise à blanc pour les lignes effacées, décalage des couleurs avec les caractères).

---

## Les Sprites

Les sprites sont des éléments graphiques mobiles, définis séparément de la map.

### Définition d'un sprite

```xml
<spritedef id="player" width="1" height="1" type="char">
  <sprite>
    <line>@</line>
  </sprite>
</spritedef>
```

### Attributs de spritedef

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `id`     | string | - | Identifiant unique |
| `width`  | int | 1 | Largeur en caractères |
| `height` | int | 1 | Hauteur en lignes |
| `type`   | string | "char" | "char" ou "bitmap" |

### Types de sprites

**Type "char"** : Caractères ASCII normaux
```xml
<spritedef id="ball" width="1" height="1" type="char">
  <sprite><line>O</line></sprite>
</spritedef>
```

**Type "bitmap"** : Caractères semi-graphiques Minitel (mosaïque)
```xml
<spritedef id="paddle" width="1" height="3" type="bitmap">
  <sprite>
    <line>}</line>
    <line>}</line>
    <line>}</line>
  </sprite>
</spritedef>
```

### Manipulation en JavaScript

```javascript
var layers = getLayers();

// Récupérer un sprite (le crée si nécessaire)
var player = layers.getSprite("player");

// Afficher le sprite (frame 0)
player.show(0);

// Déplacer le sprite
player.move(x, y);

// Cacher le sprite
player.hide();

// Définir la couleur du sprite (0-7)
player.setColor(1);  // 1 = rouge

// Obtenir la couleur actuelle
var color = player.getColor();

// Afficher avec une couleur spécifique
player.show(0, 6);  // Frame 0, couleur cyan

// Obtenir les dimensions du sprite
var w = player.getWidth();
var h = player.getHeight();

// Obtenir la position actuelle
var x = player.getX();
var y = player.getY();
```

### Couleurs des sprites

Chaque sprite peut avoir sa propre couleur de texte (ink) :

| Code | Couleur |
|------|---------|
| 0 | Noir |
| 1 | Rouge |
| 2 | Vert |
| 3 | Jaune |
| 4 | Bleu |
| 5 | Magenta |
| 6 | Cyan |
| 7 | Blanc (défaut) |

```javascript
// Exemple : pièces de Tetris colorées
var piece = layers.getSprite("block");
piece.setColor(6);  // Cyan pour la pièce I
piece.show(0);
piece.move(x, y);
```

### Colorsprite (couleurs par caractère)

Chaque sprite peut avoir des couleurs différentes pour chaque caractère grâce à `<colorsprite>`.
Fonctionne de manière similaire à `<colormap>` pour les maps.

#### Codes couleur

| Caractère | Couleur |
|-----------|---------|
| `0` | Noir |
| `1` | Rouge |
| `2` | Vert |
| `3` | Jaune |
| `4` | Bleu |
| `5` | Magenta |
| `6` | Cyan |
| `7` ou espace | Couleur par défaut du sprite |

#### Mode `char`

En mode `char`, la colorsprite a les mêmes dimensions que le sprite :

```xml
<spritedef id="alien" width="3" height="2" type="char">
  <sprite>
    <line>/O\</line>
    <line>\_/</line>
    <colorsprite>
      <line>121</line>
      <line>333</line>
    </colorsprite>
  </sprite>
</spritedef>
```

Dans cet exemple :
- `/` en **rouge** (1), `O` en **vert** (2), `\` en **rouge** (1)
- `\_/` tout en **jaune** (3)

#### Mode `bitmap`

En mode `bitmap`, la colorsprite correspond aux **caractères semi-graphiques** (2×3 pixels), pas aux pixels.

| Pixels (largeur × hauteur) | Caractères colorsprite |
|----------------------------|------------------------|
| 10×1, 10×2, 10×3           | 5×1                    |
| 10×4, 10×5, 10×6           | 5×2                    |
| 11×4                       | 6×2                    |

Formule : `largeur = ceil(pixels_largeur / 2)`, `hauteur = ceil(pixels_hauteur / 3)`

```xml
<!-- Sprite bitmap 6×6 pixels = 3×2 caractères -->
<spritedef id="ball" width="3" height="2" type="bitmap">
  <sprite>
    <line> #### </line>
    <line>######</line>
    <line>######</line>
    <line>######</line>
    <line>######</line>
    <line> #### </line>
    <colorsprite>
      <line>666</line>
      <line>666</line>
    </colorsprite>
  </sprite>
</spritedef>
```

**Note** : Si un caractère de `<colorsprite>` est un espace ou non défini, la couleur par défaut du sprite (définie via `sprite.setColor()`) sera utilisée.

---

## Animations de sprites

Un sprite peut avoir plusieurs frames d'animation.

### Définition multi-frames

```xml
<spritedef id="explosion" width="3" height="3" type="char">
  <!-- Frame 0 : petite -->
  <sprite>
    <line>   </line>
    <line> * </line>
    <line>   </line>
  </sprite>
  
  <!-- Frame 1 : moyenne -->
  <sprite>
    <line> * </line>
    <line>***</line>
    <line> * </line>
  </sprite>
  
  <!-- Frame 2 : grande -->
  <sprite>
    <line>***</line>
    <line>***</line>
    <line>***</line>
  </sprite>
</spritedef>
```

### Animation en JavaScript

```javascript
var frameIndex = 0;
var sprite = layers.getSprite("explosion");

function animate() {
  sprite.show(frameIndex);
  frameIndex = (frameIndex + 1) % 3;  // Boucle sur 3 frames
}
```

### Exemple : Personnage qui marche

```xml
<spritedef id="hero" width="1" height="2" type="char">
  <!-- Frame 0 : repos -->
  <sprite>
    <line>O</line>
    <line>|</line>
  </sprite>
  
  <!-- Frame 1 : marche gauche -->
  <sprite>
    <line>O</line>
    <line>/</line>
  </sprite>
  
  <!-- Frame 2 : marche droite -->
  <sprite>
    <line>O</line>
    <line>\</line>
  </sprite>
</spritedef>
```

```javascript
var walkFrame = 0;

function walk() {
  var hero = layers.getSprite("hero");
  walkFrame = (walkFrame == 1) ? 2 : 1;
  hero.show(walkFrame);
}

function stop() {
  var hero = layers.getSprite("hero");
  hero.show(0);  // Frame repos
}
```

---

## Contrôles clavier

Le `<keypad>` associe des touches à des fonctions JavaScript. Deux modes sont disponibles.

### Mode action (avec joystick)

```xml
<keypad action="UP"    key="Z" event="moveUp"/>
<keypad action="DOWN"  key="S" event="moveDown"/>
<keypad action="LEFT"  key="Q" event="moveLeft"/>
<keypad action="RIGHT" key="D" event="moveRight"/>
<keypad action="ACTION1" key=" " event="fire"/>
```

Ces actions sont aussi déclenchées automatiquement par un joystick USB.

### Actions disponibles

| Action | Description |
|--------|-------------|
| `UP` | Haut |
| `DOWN` | Bas |
| `LEFT` | Gauche |
| `RIGHT` | Droite |
| `ACTION1` | Action principale (tir, saut...) |
| `ACTION2` | Action secondaire |

### Mode touche directe

Pour des raccourcis clavier personnalisés (sans joystick) :

```xml
<!-- Touches numériques -->
<keypad key="1" event="selectWeapon1"/>
<keypad key="2" event="selectWeapon2"/>

<!-- Touches de contrôle -->
<keypad key="P" event="pauseGame"/>
<keypad key="R" event="resetGame"/>
<keypad key="M" event="toggleMusic"/>
```

**Note** : Les touches directes ne sont pas accessibles via joystick.

### Fonctions JavaScript

```javascript
function moveUp() {
  playerY--;
  updatePlayer();
}

function moveDown() {
  playerY++;
  updatePlayer();
}

function pauseGame() {
  gamePaused = !gamePaused;
  layers.setText("status", gamePaused ? "PAUSE" : "");
}

function fire() {
  // Tirer un projectile
}
```

---

## Joystick USB

Sur Raspberry Pi, un joystick USB est automatiquement détecté et utilisable.

### Fonctionnement

Le système lit `/dev/input/js0` (ou autre joystick disponible) et traduit les événements en appels aux mêmes fonctions que le `<keypad>`.

**Aucune configuration nécessaire** : si vous avez défini des `<keypad>` pour UP/DOWN/LEFT/RIGHT et ACTION1/ACTION2, le joystick les déclenchera automatiquement.

### Mapping par défaut

| Joystick | Action VTML |
|----------|-------------|
| Axe X gauche | `LEFT` |
| Axe X droite | `RIGHT` |
| Axe Y haut | `UP` |
| Axe Y bas | `DOWN` |
| Bouton 0 (A/X) | `ACTION1` |
| Bouton 1 (B/O) | `ACTION2` |
| Bouton 2 | `ACTION1` |
| Bouton 3 | `ACTION2` |

### Exemple

Avec cette configuration clavier :

```xml
<keypad action="UP" key="Z" event="moveUp"/>
<keypad action="DOWN" key="S" event="moveDown"/>
<keypad action="ACTION1" key=" " event="fire"/>
```

Le joystick appellera automatiquement :
- `moveUp()` quand on pousse le stick vers le haut
- `moveDown()` quand on pousse vers le bas
- `fire()` quand on appuie sur le bouton A

### Configuration via config.json

Le mapping peut être personnalisé dans `config.json` :

```json
{
  "client": {
    "joystick_device": "/dev/input/js0",
    "joystick_enabled": true,
    "joystick_mapping": {
      "buttons": {
        "0": "ACTION1",
        "1": "ACTION2",
        "2": "UP",
        "3": "DOWN"
      },
      "axes": {
        "0+": "RIGHT",
        "0-": "LEFT",
        "1+": "DOWN",
        "1-": "UP"
      },
      "axis_threshold": 16000
    }
  }
}
```

**Format des axes** : `"axe+direction"` où direction est `+` (positif) ou `-` (négatif).

### Configuration via JavaScript

Le mapping peut aussi être modifié dynamiquement en JavaScript :

```javascript
function domReady() {
  // Remapper le bouton 0 sur UP
  joystick.mapButton(0, "UP");
  
  // Remapper l'axe 0 positif sur ACTION1
  joystick.mapAxis("0+", "ACTION1");
  
  // Changer le seuil de détection des axes
  joystick.setThreshold(20000);
  
  // Afficher le mapping actuel (debug)
  joystick.printMapping();
  
  // Réinitialiser le mapping par défaut
  joystick.resetMapping();
}
```

**API JavaScript disponible** :

| Méthode | Description |
|---------|-------------|
| `joystick.mapButton(button, action)` | Mapper un bouton vers une action |
| `joystick.mapAxis(axis, action)` | Mapper un axe vers une action |
| `joystick.setThreshold(value)` | Définir le seuil des axes (0-32767) |
| `joystick.printMapping()` | Afficher le mapping actuel |
| `joystick.resetMapping()` | Réinitialiser le mapping par défaut |

### Vérification

Au démarrage, le serveur affiche :
```
🎮 Joystick mapping chargé: 4 boutons, 4 axes
🎮 Joystick: utilisation de /dev/input/js0
```

Si aucun joystick n'est branché :
```
🎮 Joystick: périphérique /dev/input/js0 non disponible
```

---

## Game Loop

Le `<timer>` appelle une fonction à intervalle régulier.

### Syntaxe

```xml
<timer event="gameLoop" interval="200"></timer>
```

### Exemple de game loop

```javascript
var gameOver = false;

function gameLoop() {
  if (gameOver) return;
  
  var layers = getLayers();
  if (layers == null) return;
  
  // 1. Mettre à jour la logique
  updatePhysics();
  
  // 2. Vérifier les collisions
  checkCollisions(layers);
  
  // 3. Mettre à jour l'affichage
  updateDisplay(layers);
}
```

### Vitesse variable

Pour un jeu qui accélère (comme Tetris) :

```javascript
var speed = 500;  // ms

function levelUp() {
  speed = Math.max(100, speed - 50);
  // Note: l'intervalle du timer est fixe,
  // gérez la vitesse dans votre logique
}
```

---

## Collisions

### Collision sprite vs sprite

```javascript
if (layers.checkCollision("ball", "paddle")) {
  // La balle touche la raquette
  ballDX = -ballDX;
  layers.beep();
}
```

### Collision sprite vs map

```javascript
// Vérifier si le sprite touche un caractère non-vide
// Retourne le code ASCII du caractère touché (0 = pas de collision)
var hit = layers.checkMapCollision("player");
if (hit != 0 && hit != 32) {  // 32 = espace
  // Collision avec le décor
}

// Vérifier une position spécifique AVANT de déplacer
var char = layers.checkMapCollisionAt("player", newX, newY);
if (char == 35) {  // 35 = '#'
  // Collision avec un mur
}
```

### Modification dynamique des maps

```javascript
// Modifier un caractère dans une map
layers.setMapChar(mapIndex, x, y, '#');

// Modifier la couleur du texte (ink) à une position
layers.setMapColor(mapIndex, x, y, 1);  // 1 = rouge

// Lire la couleur à une position
var color = layers.getMapColor(mapIndex, x, y);

// Lire un caractère à une position (retourne le code ASCII)
var charCode = layers.getMapChar(mapIndex, x, y);
if (charCode != 0 && charCode != 32) {  // Pas vide ni espace
  // Il y a quelque chose à cette position
}

// Effacer une ligne entière (pour Tetris) - efface aussi les couleurs
layers.clearMapLine(mapIndex, y);

// Décaler les lignes vers le bas (pour Tetris) - décale aussi les couleurs
layers.shiftMapDown(mapIndex, fromY, toY);
```

### Codes couleur

| Code | Couleur |
|------|---------|
| 0 | Noir |
| 1 | Rouge |
| 2 | Vert |
| 3 | Jaune |
| 4 | Bleu |
| 5 | Magenta |
| 6 | Cyan |
| 7 | Blanc |

### Codes de caractères courants

| Caractère | Code |
|-----------|------|
| Espace | 32 |
| `#` | 35 |
| `*` | 42 |
| `@` | 64 |
| `O` | 79 |

---

## Interface utilisateur

### Labels dynamiques

```xml
<label id="score" x="1" y="0" width="12">Score: 0</label>
<label id="lives" x="30" y="0" width="10">Vies: 3</label>
<!-- Label caché par défaut, affiché uniquement en fin de partie -->
<label id="gameover" x="12" y="10" width="16" visibility="hidden">GAME OVER!</label>
```

#### Attributs des labels

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `id` | string | - | Identifiant pour JavaScript |
| `x` | int | 0 | Position X |
| `y` | int | 0 | Position Y |
| `width` | int | 10 | Largeur (texte tronqué ou paddé) |
| `visibility` | string | `visible` | `visible` ou `hidden` |

#### API JavaScript des labels

```javascript
// Modifier le texte d'un label
layers.setText("score", "Score: " + score);

// Afficher un label caché
layers.showLabel("gameover");

// Cacher un label
layers.hideLabel("gameover");

// Vérifier si un label est visible
if (layers.isLabelVisible("gameover")) {
  // ...
}
```

#### Exemple : Message de fin de partie

```javascript
function gameOver() {
  gameRunning = false;
  layers.setText("gameover", "GAME OVER!");
  layers.showLabel("gameover");  // Affiche le label caché
  layers.beep();
}

function victory() {
  gameRunning = false;
  layers.setText("gameover", "VICTOIRE!");
  layers.showLabel("gameover");
}
```

### Son

```javascript
// Émettre un bip Minitel
layers.beep();
```

**⚠️ Attention** : Le beep est **bloquant**. Pendant la durée du bip, les entrées clavier et joystick sont ignorées. Utilisez-le avec parcimonie pour ne pas gêner la jouabilité.

---

## Exemples complets

### Snake minimal

```xml
<layers id="game" left="0" top="1" width="40" height="22">
  <map>
    <row>########################################</row>
    <row>#                                      #</row>
    <!-- ... lignes du milieu ... -->
    <row>########################################</row>
  </map>
  
  <spritedef id="head" width="1" height="1" type="char">
    <sprite><line>@</line></sprite>
  </spritedef>
  
  <spritedef id="apple" width="1" height="1" type="char">
    <sprite><line>X</line></sprite>
  </spritedef>
  
  <keypad action="UP" key="Z" event="goUp"/>
  <keypad action="DOWN" key="S" event="goDown"/>
  <keypad action="LEFT" key="Q" event="goLeft"/>
  <keypad action="RIGHT" key="D" event="goRight"/>
  
  <timer event="gameLoop" interval="200"></timer>
</layers>

<script>
  var headX = 20, headY = 10;
  var direction = 0;  // 0=droite, 1=bas, 2=gauche, 3=haut
  
  function domReady() {
    var layers = getLayers();
    var head = layers.getSprite("head");
    head.show(0);
    head.move(headX, headY);
  }
  
  function goUp()    { if (direction != 1) direction = 3; }
  function goDown()  { if (direction != 3) direction = 1; }
  function goLeft()  { if (direction != 0) direction = 2; }
  function goRight() { if (direction != 2) direction = 0; }
  
  function gameLoop() {
    var layers = getLayers();
    
    // Déplacer
    if (direction == 0) headX++;
    else if (direction == 1) headY++;
    else if (direction == 2) headX--;
    else if (direction == 3) headY--;
    
    // Collision mur
    var hit = layers.checkMapCollisionAt("head", headX, headY);
    if (hit == 35) {
      layers.setText("score", "GAME OVER!");
      return;
    }
    
    // Mettre à jour
    var head = layers.getSprite("head");
    head.move(headX, headY);
  }
</script>
```

### Pong minimal

```xml
<layers id="game" left="0" top="1" width="40" height="22">
  <spritedef id="ball" width="1" height="1" type="char">
    <sprite><line>O</line></sprite>
  </spritedef>
  
  <spritedef id="paddle" width="1" height="3" type="char">
    <sprite>
      <line>#</line>
      <line>#</line>
      <line>#</line>
    </sprite>
  </spritedef>
  
  <keypad action="UP" key="Z" event="paddleUp"/>
  <keypad action="DOWN" key="S" event="paddleDown"/>
  
  <timer event="gameLoop" interval="100"></timer>
</layers>

<script>
  var ballX = 20, ballY = 10;
  var ballDX = 1, ballDY = 1;
  var paddleY = 10;
  
  function domReady() {
    var layers = getLayers();
    layers.getSprite("ball").show(0);
    layers.getSprite("paddle").show(0);
    updatePositions(layers);
  }
  
  function paddleUp()   { if (paddleY > 1) paddleY--; }
  function paddleDown() { if (paddleY < 18) paddleY++; }
  
  function updatePositions(layers) {
    layers.getSprite("ball").move(ballX, ballY);
    layers.getSprite("paddle").move(2, paddleY);
  }
  
  function gameLoop() {
    var layers = getLayers();
    
    // Déplacer la balle
    ballX += ballDX;
    ballY += ballDY;
    
    // Rebonds
    if (ballY <= 0 || ballY >= 21) ballDY = -ballDY;
    if (ballX >= 39) ballDX = -ballDX;
    
    // Collision raquette
    if (ballX == 3 && ballY >= paddleY && ballY < paddleY + 3) {
      ballDX = -ballDX;
      layers.beep();
    }
    
    // Balle perdue
    if (ballX < 0) {
      ballX = 20;
      ballY = 10;
    }
    
    updatePositions(layers);
  }
</script>
```

---

## Conseils de performance

1. **Minimisez les sprites** : Chaque sprite consomme de la mémoire
2. **Utilisez les maps** : Pour les éléments statiques ou nombreux
3. **Rendu différentiel** : Le système ne redessine que ce qui change
4. **Intervalle raisonnable** : 100-200ms minimum pour le game loop

## Limitations

- **16 sprites maximum** par layers (définis avec `<spritedef>`)
- **3 maps maximum** empilées
- **Pas de son complexe** : Seulement le bip Minitel (`layers.beep()`), qui est bloquant
- **40x25 caractères** : Résolution fixe du Minitel (ligne 0 = ligne d'info)

---

## Ressources

- [Documentation VTML complète](VTML.md)
- [Exemple Snake](../pages/games/snake.vtml)
- [Exemple Pong](../pages/games/pong.vtml)
- [Exemple Tetris](../pages/games/tetris.vtml)
- [Exemple Tetris Couleur](../pages/games/tetris_color.vtml) - Utilise les colormaps
- [Exemple Breakout](../pages/games/breakout.vtml) - Casse-briques avec briques colorées
- [Exemple Space Invaders](../pages/games/invaders.vtml) - Envahisseurs colorés avec tirs

---

## Annexe A : Référence des fonctions JavaScript

### Fonctions globales

| Fonction | Description |
|----------|-------------|
| `getLayers()` | Retourne l'objet `_currentLayers` (conteneur principal) |
| `domReady()` | Callback appelé automatiquement quand la page est prête |

### API Layers

| Méthode | Paramètres | Retour | Description |
|---------|------------|--------|-------------|
| `getSprite(id)` | `id`: string | Sprite | Récupère un sprite par son ID |
| `setText(id, text)` | `id`: string, `text`: string | void | Modifie le texte d'un label |
| `showLabel(id)` | `id`: string | void | Affiche un label caché |
| `hideLabel(id)` | `id`: string | void | Cache un label |
| `isLabelVisible(id)` | `id`: string | boolean | Vérifie si un label est visible |
| `beep()` | - | void | Émet un bip Minitel (bloquant) |
| `checkCollision(id1, id2)` | `id1`: string, `id2`: string | boolean | Collision entre deux sprites |
| `checkMapCollision(id)` | `id`: string | int | Collision sprite/map (retourne code ASCII) |
| `checkMapCollisionAt(id, x, y)` | `id`: string, `x`: int, `y`: int | int | Collision à une position donnée |
| `setMapChar(map, x, y, char)` | `map`: int, `x`: int, `y`: int, `char`: string | void | Place un caractère dans une map |
| `getMapChar(map, x, y)` | `map`: int, `x`: int, `y`: int | int | Lit un caractère (code ASCII) |
| `setMapColor(map, x, y, color)` | `map`: int, `x`: int, `y`: int, `color`: int | void | Définit la couleur à une position |
| `getMapColor(map, x, y)` | `map`: int, `x`: int, `y`: int | int | Lit la couleur à une position |
| `clearMapLine(map, y)` | `map`: int, `y`: int | void | Efface une ligne (caractères + couleurs) |
| `shiftMapDown(map, from, to)` | `map`: int, `from`: int, `to`: int | void | Décale les lignes vers le bas |

### API Sprite

| Méthode | Paramètres | Retour | Description |
|---------|------------|--------|-------------|
| `show(frame)` | `frame`: int | void | Affiche le sprite à la frame donnée |
| `show(frame, color)` | `frame`: int, `color`: int | void | Affiche avec une couleur spécifique |
| `hide()` | - | void | Cache le sprite |
| `move(x, y)` | `x`: int, `y`: int | void | Déplace le sprite |
| `setColor(color)` | `color`: int (0-7) | void | Définit la couleur du sprite |
| `getColor()` | - | int | Retourne la couleur actuelle |
| `getX()` | - | int | Position X actuelle |
| `getY()` | - | int | Position Y actuelle |
| `getWidth()` | - | int | Largeur du sprite |
| `getHeight()` | - | int | Hauteur du sprite |

### API Joystick

| Méthode | Paramètres | Retour | Description |
|---------|------------|--------|-------------|
| `joystick.mapButton(btn, action)` | `btn`: int, `action`: string | void | Mappe un bouton vers une action |
| `joystick.mapAxis(axis, action)` | `axis`: string, `action`: string | void | Mappe un axe (ex: "0+", "1-") |
| `joystick.setThreshold(value)` | `value`: int (0-32767) | void | Seuil de détection des axes |
| `joystick.printMapping()` | - | void | Affiche le mapping actuel (debug) |
| `joystick.resetMapping()` | - | void | Réinitialise le mapping par défaut |

---

## Annexe B : Référence des attributs VTML

### Élément `<layers>`

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `id` | string | - | Identifiant pour JavaScript |
| `left` | int | 0 | Position X |
| `top` | int | 0 | Position Y |
| `width` | int | 40 | Largeur en caractères |
| `height` | int | 24 | Hauteur en lignes |

### Élément `<spritedef>`

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `id` | string | - | Identifiant unique |
| `width` | int | 1 | Largeur en caractères |
| `height` | int | 1 | Hauteur en lignes |
| `type` | string | "char" | `char` ou `bitmap` |

### Élément `<label>`

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `id` | string | - | Identifiant pour JavaScript |
| `x` | int | 0 | Position X |
| `y` | int | 0 | Position Y |
| `width` | int | 10 | Largeur (texte tronqué ou paddé) |
| `visibility` | string | `visible` | `visible` ou `hidden` |

### Élément `<keypad>`

| Attribut | Type | Description |
|----------|------|-------------|
| `key` | string | Touche clavier associée |
| `event` | string | Nom de la fonction JavaScript à appeler |
| `action` | string | Action joystick : `UP`, `DOWN`, `LEFT`, `RIGHT`, `ACTION1`, `ACTION2` |

### Élément `<timer>`

| Attribut | Type | Description |
|----------|------|-------------|
| `event` | string | Nom de la fonction JavaScript à appeler |
| `interval` | int | Intervalle en millisecondes |

---

## Annexe C : Codes de référence

### Codes couleur

| Code | Couleur | Utilisation |
|------|---------|-------------|
| 0 | Noir | `setColor(0)`, `setMapColor(map, x, y, 0)` |
| 1 | Rouge | `setColor(1)`, `setMapColor(map, x, y, 1)` |
| 2 | Vert | `setColor(2)`, `setMapColor(map, x, y, 2)` |
| 3 | Jaune | `setColor(3)`, `setMapColor(map, x, y, 3)` |
| 4 | Bleu | `setColor(4)`, `setMapColor(map, x, y, 4)` |
| 5 | Magenta | `setColor(5)`, `setMapColor(map, x, y, 5)` |
| 6 | Cyan | `setColor(6)`, `setMapColor(map, x, y, 6)` |
| 7 | Blanc | `setColor(7)`, `setMapColor(map, x, y, 7)` |

### Codes ASCII courants

| Caractère | Code | Utilisation |
|-----------|------|-------------|
| Espace | 32 | `if (char == 32)` → vide |
| `#` | 35 | Murs, obstacles |
| `*` | 42 | Étoiles, projectiles |
| `@` | 64 | Joueur |
| `O` | 79 | Balles |
| `X` | 88 | Cibles, items |

### Actions joystick

| Action | Description | Mapping clavier typique |
|--------|-------------|-------------------------|
| `UP` | Direction haut | Z, W, ↑ |
| `DOWN` | Direction bas | S, ↓ |
| `LEFT` | Direction gauche | Q, A, ← |
| `RIGHT` | Direction droite | D, → |
| `ACTION1` | Action principale | Espace, Entrée |
| `ACTION2` | Action secondaire | Shift, Ctrl |
