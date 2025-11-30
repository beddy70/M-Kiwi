# VTML - Videotex Terminal Markup Language

VTML est un langage de balisage inspiré de HTML, conçu pour créer des pages Minitel. Il permet de structurer le contenu et de générer automatiquement les codes Vidéotex.

## Attributs communs

Tous les tags VTML supportent les attributs suivants :

| Attribut | Type   | Défaut | Description                                      |
|----------|--------|--------|--------------------------------------------------|
| `id`     | string | -      | Identifiant unique du composant                  |
| `name`   | string | -      | Nom du composant (pour recherche par nom)        |

Ces attributs permettent de retrouver un composant via JavaScript :

```javascript
var comp = pageManager.getComponentById("monId");
var comp2 = pageManager.getComponentByName("monNom");
```

---

## Structure de base

```xml
<minitel title="Ma Page">
  <div left="0" top="1" width="40" height="24">
    <row>Contenu de la page</row>
  </div>
</minitel>
```

---

## Tags disponibles

### `<minitel>`

Élément racine obligatoire. Définit une page Minitel.

| Attribut | Type   | Défaut | Description          |
|----------|--------|--------|----------------------|
| `title`  | string | -      | Titre de la page     |

```xml
<minitel title="Accueil">
  ...
</minitel>
```

---

### `<div>`

Conteneur de positionnement. Définit une zone rectangulaire sur l'écran.

| Attribut | Type | Défaut | Description                    |
|----------|------|--------|--------------------------------|
| `left`   | int  | 0      | Position X (0-39)              |
| `top`    | int  | 0      | Position Y (0-24)              |
| `width`  | int  | 40     | Largeur en caractères          |
| `height` | int  | 25     | Hauteur en lignes              |

```xml
<div left="5" top="3" width="30" height="10">
  <row>Contenu centré</row>
</div>
```

---

### `<row>`

Ligne de texte. Le texte est affiché à la position courante.

```xml
<row>Bonjour Minitel!</row>
<row>Deuxième ligne</row>
```

**Note** : Les espaces multiples sont préservés.

---

### `<br>`

Saut de ligne simple.

```xml
<row>Ligne 1</row>
<br>
<row>Ligne 3 (après saut)</row>
```

---

### `<menu>`

Conteneur de menu interactif avec navigation par touches.

| Attribut  | Type   | Défaut | Description                           |
|-----------|--------|--------|---------------------------------------|
| `name`    | string | -      | Nom du menu                           |
| `keytype` | string | -      | Type de touches (`numeric`, `alpha`)  |
| `left`    | int    | 0      | Position X                            |
| `top`     | int    | 0      | Position Y                            |

```xml
<menu name="principal" keytype="numeric" left="2" top="5">
  <item link="page1.vtml">Option 1</item>
  <item link="page2.vtml">Option 2</item>
  <item link="page3.vtml">Option 3</item>
</menu>
```

---

### `<item>`

Élément de menu. Doit être enfant de `<menu>`.

| Attribut | Type   | Défaut | Description                |
|----------|--------|--------|----------------------------|
| `link`   | string | -      | Page cible (fichier .vtml) |

```xml
<item link="services.vtml">Nos services</item>
```

---

### `<img>`

Affiche une image convertie en semi-graphique Minitel.

| Attribut   | Type    | Défaut | Description                              |
|------------|---------|--------|------------------------------------------|
| `src`      | string  | -      | Chemin de l'image (local ou URL)         |
| `left`     | int     | 0      | Position X (en caractères, 0-39)         |
| `top`      | int     | 0      | Position Y (en caractères, 0-24)         |
| `width`    | int     | 32     | Largeur en caractères                    |
| `height`   | int     | 32     | Hauteur en caractères                    |
| `negative` | boolean | false  | Inverser les couleurs                    |
| `style`    | string  | -      | Mode de rendu : `dithering` ou `bitmap`  |

#### Modes de rendu (attribut `style`)

| Valeur      | Description                                           |
|-------------|-------------------------------------------------------|
| *(aucun)*   | Couleur 8 couleurs Minitel (par défaut)               |
| `dithering` | Couleur avec tramage Floyd-Steinberg (meilleur rendu) |
| `bitmap`    | Noir et blanc uniquement                              |

```xml
<!-- Image en couleur (8 couleurs Minitel) -->
<img src="images/logo.jpg" left="4" top="2" width="32" height="20">

<!-- Image avec dithering (meilleur pour les photos et dégradés) -->
<img src="images/photo.png" left="0" top="0" width="40" height="24" style="dithering">

<!-- Image en noir et blanc -->
<img src="images/icon.png" left="10" top="5" width="20" height="15" style="bitmap">

<!-- Image inversée -->
<img src="images/logo.png" left="0" top="10" width="40" height="14" negative="true">
```

**Formats supportés** : JPG, PNG, GIF, BMP

**Notes** :
- Les dimensions sont en **caractères** (1 caractère = 2×3 pixels semi-graphiques)
- L'image est automatiquement redimensionnée pour remplir la zone spécifiée
- Le mode `dithering` utilise l'algorithme Floyd-Steinberg pour simuler plus de couleurs

---

### `<qrcode>`

Génère un QR code affichable sur Minitel.

| Attribut  | Type   | Défaut | Description                          |
|-----------|--------|--------|--------------------------------------|
| `type`    | string | `url`  | Type : `url`, `wpawifi` ou `vcard`   |
| `message` | string | -      | Contenu du QR code                   |
| `scale`   | int    | 1      | Facteur d'échelle (1-3)              |
| `left`    | int    | 0      | Position X                           |
| `top`     | int    | 0      | Position Y                           |

```xml
<!-- QR code URL -->
<qrcode type="url" message="https://example.com" scale="2" left="10" top="5">

<!-- QR code WiFi -->
<qrcode type="wpawifi" message="ssid:'MonSSID', password:'MonMotDePasse'" scale="1" left="5" top="10">

<!-- QR code vCard -->
<qrcode type="vcard" message="name:'Dupont;Marc', tel:'+33663190308', email:'contact@dupont.org', org:'Dupont Cie', title:'PDG', url:'marc@dupont.org'" left="11" top="7" scale="1"/>
```

**Champs vCard supportés** :
- `name:'...'` - Nom complet
- `tel:'...'` - Téléphone
- `email:'...'` - Email
- `org:'...'` - Organisation
- `title:'...'` - Titre/Fonction
- `url:'...'` - Site web

---

## Tags Layers (Jeux)

Le système de layers permet de créer des jeux interactifs sur Minitel avec des zones de fond (maps) et des sprites animés.

### `<layers>`

Conteneur principal pour un jeu. Définit une zone de jeu avec des maps et des sprites.

| Attribut | Type   | Défaut | Description                    |
|----------|--------|--------|--------------------------------|
| `id`     | string | -      | Identifiant unique (requis pour JS) |
| `left`   | int    | 0      | Position X                     |
| `top`    | int    | 0      | Position Y                     |
| `width`  | int    | 40     | Largeur en caractères          |
| `height` | int    | 24     | Hauteur en lignes              |

**Limites** :
- Maximum 3 `<map>`
- Maximum 8 `<spritedef>`

```xml
<layers id="game" left="0" top="1" width="40" height="20">
  <!-- maps, sprites, keypads et timer ici -->
</layers>
```

---

### `<map>`

Zone de fond dans un layers. Les maps sont empilées (la première = fond, la dernière = dessus).
Les caractères espace dans les maps supérieures sont transparents.

| Attribut | Type   | Défaut | Description                           |
|----------|--------|--------|---------------------------------------|
| `id`     | string | -      | Identifiant unique                    |
| `type`   | string | `char` | Type : `char` ou `bitmap`             |

```xml
<map id="terrain" type="char">
  <row>########################################</row>
  <row>#                                      #</row>
  <row>#                                      #</row>
  <row>########################################</row>
</map>
```

---

### `<spritedef>`

Définition d'un sprite avec ses frames d'animation.

| Attribut | Type   | Défaut | Description                           |
|----------|--------|--------|---------------------------------------|
| `id`     | string | -      | Identifiant unique du sprite          |
| `width`  | int    | 8      | Largeur du sprite                     |
| `height` | int    | 8      | Hauteur du sprite                     |
| `type`   | string | `char` | Type de rendu (voir ci-dessous)       |

#### Types de sprites

| Type     | Description                                                    |
|----------|----------------------------------------------------------------|
| `char`   | Caractères texte affichés tels quels                           |
| `bitmap` | Mode semi-graphique : `#` = pixel allumé, ` ` (espace) = pixel éteint |

**Mode `char`** : Chaque caractère est affiché directement. Idéal pour les sprites textuels.

```xml
<spritedef id="player" width="3" height="2" type="char">
  <sprite>
    <line> O </line>
    <line>/|\</line>
  </sprite>
</spritedef>
```

**Mode `bitmap`** : Utilise les caractères semi-graphiques Minitel (mosaïque 2×3).
- `#` = pixel allumé (1)
- ` ` (espace) = pixel éteint (0)

```xml
<!-- Sprite bitmap 6x6 pixels (2 caractères × 2 caractères) -->
<spritedef id="ball" width="2" height="2" type="bitmap">
  <sprite>
    <line> #### </line>
    <line>######</line>
    <line>######</line>
    <line>######</line>
    <line>######</line>
    <line> #### </line>
  </sprite>
</spritedef>
```

**Note** : En mode `bitmap`, la largeur/hauteur sont en **caractères** (1 caractère = 2×3 pixels semi-graphiques).

---

### `<sprite>`

Une frame d'animation dans un `<spritedef>`. Contient des `<line>` pour définir l'apparence.

```xml
<sprite>
  <line>###</line>
  <line># #</line>
  <line>###</line>
</sprite>
```

---

### `<line>`

Ligne de données dans un `<sprite>`. Définit une ligne de l'apparence du sprite.

```xml
<line>###</line>
```

**Note** : Utilisez `<line>` dans les sprites et `<row>` dans les maps.

---

### `<keypad>`

Associe une touche du clavier à une action de jeu.

| Attribut | Type   | Défaut | Description                           |
|----------|--------|--------|---------------------------------------|
| `action` | string | -      | Action : LEFT, RIGHT, UP, DOWN, ACTION1, ACTION2 |
| `key`    | string | -      | Touche du clavier (une lettre)        |
| `event`  | string | -      | Nom de la fonction JavaScript à appeler |

```xml
<keypad action="LEFT"    key="Q" event="moveLeft"/>
<keypad action="RIGHT"   key="D" event="moveRight"/>
<keypad action="UP"      key="Z" event="moveUp"/>
<keypad action="DOWN"    key="S" event="moveDown"/>
<keypad action="ACTION1" key=" " event="jump"/>
<keypad action="ACTION2" key="E" event="action"/>
```

**Note** : L'attribut `event` contient le nom de la fonction sans les parenthèses.

---

### `<timer>`

Définit une boucle de jeu (game loop) qui appelle une fonction JavaScript à intervalle régulier.

| Attribut   | Type   | Défaut | Description                           |
|------------|--------|--------|---------------------------------------|
| `event`    | string | -      | Nom de la fonction JavaScript à appeler |
| `interval` | int    | 200    | Intervalle en millisecondes           |

```xml
<timer event="moveBall" interval="300"></timer>
```

**Note** : Utilisez la syntaxe avec balise fermante `</timer>` (pas auto-fermante).

---

### Exemple complet de jeu (Pong)

```xml
<minitel title="Pong Minitel">
  
  <layers id="game" left="0" top="1" width="40" height="22">
    
    <!-- Fond du terrain -->
    <map id="terrain" type="char">
      <row>########################################</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>#                                      #</row>
      <row>########################################</row>
    </map>
    
    <!-- Sprite de la raquette -->
    <spritedef id="paddle" width="1" height="4" type="char">
      <sprite>
        <line>|</line>
        <line>|</line>
        <line>|</line>
        <line>|</line>
      </sprite>
    </spritedef>
    
    <!-- Sprite de la balle -->
    <spritedef id="ball" width="1" height="1" type="char">
      <sprite>
        <line>O</line>
      </sprite>
    </spritedef>
    
    <!-- Mapping des touches -->
    <keypad action="UP"   key="Z" event="moveUp"/>
    <keypad action="DOWN" key="S" event="moveDown"/>
    
    <!-- Timer pour animer la balle -->
    <timer event="moveBall" interval="300"></timer>
    
  </layers>
  
  <div left="0" top="24" width="40" height="1">
    <row>[Z]=Haut [S]=Bas</row>
  </div>
  
  <script>
    var paddleY = 10;
    var ballX = 20;
    var ballY = 10;
    var ballDX = 1;
    var ballDY = 1;

    function getLayers() {
      return _currentLayers;
    }

    function domReady() {
      var layers = getLayers();
      if (layers != null) {
        var paddle = layers.getSprite("paddle");
        if (paddle != null) {
          paddle.show(0);
          paddle.move(2, paddleY);
        }
        
        var ball = layers.getSprite("ball");
        if (ball != null) {
          ball.show(0);
          ball.move(ballX, ballY);
        }
      }
    }

    function moveUp() {
      var layers = getLayers();
      if (layers != null && paddleY > 1) {
        paddleY--;
        var paddle = layers.getSprite("paddle");
        if (paddle != null) {
          paddle.move(2, paddleY);
        }
      }
    }

    function moveDown() {
      var layers = getLayers();
      if (layers != null && paddleY < 16) {
        paddleY++;
        var paddle = layers.getSprite("paddle");
        if (paddle != null) {
          paddle.move(2, paddleY);
        }
      }
    }

    function moveBall() {
      var layers = getLayers();
      if (layers == null) return;
      
      ballX = ballX + ballDX;
      ballY = ballY + ballDY;
      
      // Rebond murs haut/bas
      if (ballY <= 1 || ballY >= 19) {
        ballDY = -ballDY;
      }
      
      // Rebond mur droit
      if (ballX >= 38) {
        ballDX = -ballDX;
      }
      
      // Rebond raquette ou perdu
      if (ballX <= 3) {
        if (ballY >= paddleY && ballY < paddleY + 4) {
          ballDX = -ballDX;
        } else if (ballX <= 1) {
          // Reset au centre
          ballX = 20;
          ballY = 10;
          ballDX = 1;
        }
      }
      
      var ball = layers.getSprite("ball");
      if (ball != null) {
        ball.move(ballX, ballY);
      }
    }
  </script>
  
</minitel>
```

---

### API JavaScript pour les jeux

#### Variable globale

| Variable          | Description                                      |
|-------------------|--------------------------------------------------|
| `_currentLayers`  | Référence au VTMLLayersComponent courant         |

#### Fonction spéciale

| Fonction     | Description                                           |
|--------------|-------------------------------------------------------|
| `domReady()` | Appelée automatiquement après le chargement de la page |
| `debug(msg)` | Affiche un message dans la console Java               |

#### API Sprite

```javascript
// Récupérer le layers courant
var layers = _currentLayers;

// Récupérer un sprite par son id
var sprite = layers.getSprite("player");

// Afficher le sprite (frame 0)
sprite.show(0);

// Déplacer le sprite à la position (x, y)
sprite.move(10, 5);

// Cacher le sprite
sprite.hide();
```

#### Exemple de fonction de déplacement

```javascript
var playerX = 10;
var playerY = 5;

function moveRight() {
  var layers = _currentLayers;
  if (layers != null && playerX < 38) {
    playerX++;
    var player = layers.getSprite("player");
    if (player != null) {
      player.move(playerX, playerY);
    }
  }
}
```

---

### `<script>`

Exécute du JavaScript côté serveur. Permet de générer du contenu dynamique.

```xml
<script>
  var date = new Date();
  var heure = date.getHours() + ":" + date.getMinutes();
  output = "Il est " + heure;
</script>
```

**Variable spéciale** : `output` - son contenu est affiché dans la page.

#### Classes Java accessibles

| Classe          | Description                    |
|-----------------|--------------------------------|
| `Kernel`        | Accès à la configuration       |
| `Config`        | Structure de configuration     |
| `GetTeletelCode`| Génération de codes Vidéotex   |
| `Teletel`       | Constantes Minitel             |
| `Math`          | Fonctions mathématiques        |
| `Date`          | Date et heure                  |

```xml
<script>
  // Accès à la configuration
  var config = Kernel.getIntance().getConfig();
  output = "Port: " + config.server.port;
</script>

<script>
  // Calculs
  output = "Pi = " + Math.PI.toFixed(4);
</script>
```

**Sécurité** : L'accès aux classes Java est restreint à une liste blanche. Les opérations fichiers, réseau et système sont bloquées.

---

### `<form>`

Conteneur de formulaire pour la saisie utilisateur. Utilise la méthode GET.

| Attribut | Type   | Défaut | Description                    |
|----------|--------|--------|--------------------------------|
| `action` | string | -      | URL cible (page .vtml)         |
| `method` | string | `GET`  | Méthode HTTP (GET uniquement)  |
| `left`   | int    | 0      | Position X                     |
| `top`    | int    | 0      | Position Y                     |
| `width`  | int    | 40     | Largeur                        |
| `height` | int    | 25     | Hauteur                        |

```xml
<form action="search.vtml" left="2" top="5">
  <input name="query" left="0" top="0" width="20" label="Recherche: ">
  <input name="ville" left="0" top="2" width="15" label="Ville: ">
</form>
```

---

### `<input>`

Champ de saisie texte. Doit être enfant de `<form>`.

| Attribut      | Type   | Défaut | Description                    |
|---------------|--------|--------|--------------------------------|
| `name`        | string | -      | Nom du paramètre GET           |
| `left`        | int    | 0      | Position X (relative au form)  |
| `top`         | int    | 0      | Position Y (relative au form)  |
| `size`        | int    | 20     | Taille du champ en caractères  |
| `label`       | string | -      | Label affiché avant le champ   |
| `placeholder` | string | -      | Texte indicatif                |
| `value`       | string | -      | Valeur par défaut              |

```xml
<input name="email" left="0" top="0" size="25" label="Email: " placeholder="exemple@mail.com">
```

**Affichage** : Le champ de saisie est affiché en vidéo inverse pour être visible.

---

### `<key>`

Associe une touche de fonction Minitel à une URL de navigation.

| Attribut | Type   | Défaut | Description                                          |
|----------|--------|--------|------------------------------------------------------|
| `name`   | string | -      | Nom de la touche : `sommaire`, `guide` ou `telephone` |
| `link`   | string | -      | URL cible                                            |

```xml
<!-- Touche SOMMAIRE -> page d'accueil -->
<key name="sommaire" link="index.vtml">

<!-- Touche GUIDE -> page d'aide -->
<key name="guide" link="aide.vtml">

<!-- Touche TELEPHONE -> page contact -->
<key name="telephone" link="contact.vtml">
```

**Touches supportées** :
- `sommaire` - Touche SOMMAIRE
- `guide` - Touche GUIDE
- `telephone` - Touche TELEPHONE

**Note** : Ce tag ne génère pas d'affichage, il définit uniquement le comportement des touches.

---

### `<color>`

Définit les couleurs d'encre (texte) et de fond pour le contenu suivant.

| Attribut     | Type   | Défaut | Description                    |
|--------------|--------|--------|--------------------------------|
| `ink`        | string | -      | Couleur du texte (encre)       |
| `background` | string | -      | Couleur de fond                |

#### Valeurs de couleur

| Nom (FR)  | Nom (EN)  | Valeur |
|-----------|-----------|--------|
| `noir`    | `black`   | 0      |
| `rouge`   | `red`     | 1      |
| `vert`    | `green`   | 2      |
| `jaune`   | `yellow`  | 3      |
| `bleu`    | `blue`    | 4      |
| `magenta` | `magenta` | 5      |
| `cyan`    | `cyan`    | 6      |
| `blanc`   | `white`   | 7      |

```xml
<!-- Texte jaune sur fond bleu -->
<color ink="yellow" background="blue">
<row>Texte en jaune sur fond bleu</row>

<!-- Texte rouge (fond inchangé) -->
<color ink="red">
<row>Texte en rouge</row>

<!-- Utilisation des valeurs numériques -->
<color ink="7" background="4">
<row>Blanc sur bleu</row>

<!-- Noms en français -->
<color ink="blanc" background="noir">
<row>Texte blanc sur fond noir</row>
```

**Note** : Les couleurs restent actives jusqu'au prochain changement de couleur.

---

### `<blink>`

Fait clignoter le texte contenu.

```xml
<!-- Texte clignotant -->
<blink>ATTENTION!</blink>

<!-- Combiné avec couleur -->
<color ink="red"><blink>ALERTE IMPORTANTE</blink></color>

<!-- Dans une ligne -->
<row>Status: <blink>EN COURS</blink></row>
```

**Note** : Le clignotement est automatiquement désactivé après le texte pour ne pas affecter le reste de la page.

---

### `<status>`

Zone dédiée à l'affichage des informations de focus (menu/input actif). Ce tag est utilisé conjointement avec `<form>` pour indiquer à l'utilisateur quel élément a le focus.

| Attribut | Type | Défaut | Description                    |
|----------|------|--------|--------------------------------|
| `left`   | int  | 0      | Position X (0-39)              |
| `top`    | int  | 0      | Position Y (0-24)              |
| `width`  | int  | 40     | Largeur en caractères          |
| `height` | int  | 1      | Hauteur en lignes              |

```xml
<!-- Zone status en bas de l'écran -->
<status left="0" top="24" width="40" height="1"/>
```

**Comportement** :
- Affiche `>> Menu <<` quand le focus est sur la navigation menu
- Affiche `>> Saisir: [label] <<` quand le focus est sur un champ de saisie
- Si ce tag n'est pas défini, aucune information de focus n'est affichée

**Cycle de focus** (touche Entrée) :
1. Menu → Input 1 → Input 2 → ... → Input N → Menu → ...

**Important** : Utiliser la syntaxe auto-fermante `<status ... />` (pas `<status></status>`).

---

## Exemple complet

```xml
<minitel title="Page d'accueil">
  
  <!-- Zone status pour afficher le focus -->
  <status left="0" top="24" width="40" height="1"/>
  
  <!-- En-tête avec image -->
  <div left="0" top="0">
    <img src="images/banner.jpg" left="0" top="0" width="40" height="6">
  </div>
  
  <!-- Titre -->
  <div left="0" top="7">
    <row>        BIENVENUE SUR MINITEL        </row>
  </div>
  
  <!-- Date dynamique -->
  <div left="0" top="9">
    <script>
      var d = new Date();
      output = "Date: " + d.getDate() + "/" + (d.getMonth()+1) + "/" + d.getFullYear();
    </script>
  </div>
  
  <!-- Menu principal -->
  <menu name="main" keytype="numeric" left="5" top="11">
    <item link="services.vtml">1. Nos services</item>
    <item link="contact.vtml">2. Contact</item>
    <item link="about.vtml">3. À propos</item>
  </menu>
  
  <!-- Formulaire de recherche -->
  <form action="search.vtml" left="0" top="17">
    <input name="query" left="0" top="0" size="20" label="Recherche: "/>
    <input name="ville" left="0" top="2" size="15" label="Ville: "/>
  </form>
  
  <!-- QR code -->
  <div left="25" top="17">
    <qrcode type="url" message="https://example.com" scale="1" left="0" top="0"/>
  </div>
  
  <!-- Touches de fonction -->
  <key name="sommaire" link="index.vtml"/>
  <key name="guide" link="aide.vtml"/>
  
</minitel>
```

---

## Dimensions écran Minitel

| Constante       | Valeur | Description           |
|-----------------|--------|-----------------------|
| `PAGE_WIDTH`    | 40     | Largeur en caractères |
| `PAGE_HEIGHT`   | 25     | Hauteur en lignes     |

**Coordonnées** :
- X : 0 à 39 (gauche à droite)
- Y : 0 à 24 (haut en bas)

---

## Fichiers

Les pages VTML sont des fichiers texte avec l'extension `.vtml` placés dans le répertoire `root/` du serveur.

```
root/
├── index.vtml      # Page d'accueil
├── services.vtml
├── contact.vtml
└── images/
    ├── logo.jpg
    └── banner.png
```
