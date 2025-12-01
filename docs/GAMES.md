# Créer des jeux Minitel avec VTML

Ce guide explique comment créer des jeux interactifs pour Minitel en utilisant le système de **layers** de VTML.

## Table des matières

1. [Architecture d'un jeu](#architecture-dun-jeu)
2. [Le composant Layers](#le-composant-layers)
3. [Les Maps (décors)](#les-maps-décors)
4. [Les Sprites](#les-sprites)
5. [Animations de sprites](#animations-de-sprites)
6. [Contrôles clavier](#contrôles-clavier)
7. [Joystick USB](#joystick-usb)
8. [Game Loop](#game-loop)
9. [Collisions](#collisions)
10. [Interface utilisateur](#interface-utilisateur)
11. [Exemples complets](#exemples-complets)

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

// Obtenir les dimensions du sprite
var w = player.getWidth();
var h = player.getHeight();

// Obtenir la position actuelle
var x = player.getX();
var y = player.getY();
```

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

Le `<keypad>` associe des touches à des fonctions JavaScript.

### Syntaxe

```xml
<keypad action="UP"    key="Z" event="moveUp"/>
<keypad action="DOWN"  key="S" event="moveDown"/>
<keypad action="LEFT"  key="Q" event="moveLeft"/>
<keypad action="RIGHT" key="D" event="moveRight"/>
<keypad action="ACTION1" key=" " event="fire"/>
```

### Actions disponibles

| Action | Description |
|--------|-------------|
| `UP` | Haut |
| `DOWN` | Bas |
| `LEFT` | Gauche |
| `RIGHT` | Droite |
| `ACTION1` | Action principale (tir, saut...) |
| `ACTION2` | Action secondaire |

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

// Effacer une ligne entière (pour Tetris)
layers.clearMapLine(mapIndex, y);

// Décaler les lignes vers le bas (pour Tetris)
// Décale les lignes de fromY à toY d'une position vers le bas
layers.shiftMapDown(mapIndex, fromY, toY);
```

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
```

```javascript
var score = 0;

function addPoints(points) {
  score += points;
  layers.setText("score", "Score: " + score);
}

function gameOver() {
  layers.setText("score", "GAME OVER!");
  layers.beep();
}
```

### Son

```javascript
// Émettre un bip Minitel
layers.beep();
```

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
- **Pas de son complexe** : Seulement le bip Minitel (`layers.beep()`)
- **40x25 caractères** : Résolution fixe du Minitel (ligne 0 = ligne d'info)

---

## Ressources

- [Documentation VTML complète](VTML.md)
- [Exemple Snake](../pages/games/snake.vtml)
- [Exemple Pong](../pages/games/pong.vtml)
- [Exemple Tetris](../pages/games/tetris.vtml)
