# VTML - Videotex Terminal Markup Language

VTML est un langage de balisage inspiré de HTML, conçu pour créer des pages Minitel. Il permet de structurer le contenu et de générer automatiquement les codes Vidéotex.

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

Affiche une image convertie en semi-graphique Minitel (1 bit par pixel).

| Attribut   | Type    | Défaut | Description                      |
|------------|---------|--------|----------------------------------|
| `src`      | string  | -      | Chemin de l'image (local ou URL) |
| `left`     | int     | 0      | Position X                       |
| `top`      | int     | 0      | Position Y                       |
| `width`    | int     | 32     | Largeur en pixels                |
| `height`   | int     | 32     | Hauteur en pixels                |
| `negative` | boolean | false  | Inverser les couleurs            |

```xml
<img src="images/logo.jpg" left="4" top="2" width="32" height="32">
<img src="images/photo.png" left="0" top="10" width="64" height="48" negative="true">
```

**Formats supportés** : JPG, PNG, GIF, BMP

**Note** : L'image est automatiquement redimensionnée et convertie en noir et blanc.

---

### `<qrcode>`

Génère un QR code affichable sur Minitel.

| Attribut  | Type   | Défaut | Description                        |
|-----------|--------|--------|------------------------------------|
| `type`    | string | `url`  | Type : `url` ou `wpawifi`          |
| `message` | string | -      | Contenu du QR code                 |
| `scale`   | int    | 1      | Facteur d'échelle (1-3)            |
| `left`    | int    | 0      | Position X                         |
| `top`     | int    | 0      | Position Y                         |

```xml
<!-- QR code URL -->
<qrcode type="url" message="https://example.com" scale="2" left="10" top="5">

<!-- QR code WiFi -->
<qrcode type="wpawifi" message="ssid:'MonSSID', password:'MonMotDePasse'" scale="1" left="5" top="10">
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
| `width`       | int    | 20     | Largeur du champ               |
| `label`       | string | -      | Label affiché avant le champ   |
| `placeholder` | string | -      | Texte indicatif                |
| `value`       | string | -      | Valeur par défaut              |

```xml
<input name="email" left="0" top="0" width="25" label="Email: " placeholder="exemple@mail.com">
```

**Affichage** : Le champ de saisie est affiché en vidéo inverse pour être visible.

---

### `<key>`

Associe une touche de fonction Minitel à une URL de navigation.

| Attribut | Type   | Défaut | Description                              |
|----------|--------|--------|------------------------------------------|
| `name`   | string | -      | Nom de la touche : `sommaire` ou `guide` |
| `link`   | string | -      | URL cible                                |

```xml
<!-- Touche SOMMAIRE -> page d'accueil -->
<key name="sommaire" link="index.vtml">

<!-- Touche GUIDE -> page d'aide -->
<key name="guide" link="aide.vtml">
```

**Touches supportées** :
- `sommaire` - Touche SOMMAIRE
- `guide` - Touche GUIDE

**Note** : Ce tag ne génère pas d'affichage, il définit uniquement le comportement des touches.

---

## Exemple complet

```xml
<minitel title="Page d'accueil">
  
  <!-- En-tête avec image -->
  <div left="0" top="0">
    <img src="images/banner.jpg" left="0" top="0" width="80" height="24">
  </div>
  
  <!-- Titre -->
  <div left="0" top="3">
    <row>        BIENVENUE SUR MINITEL        </row>
  </div>
  
  <!-- Date dynamique -->
  <div left="0" top="5">
    <script>
      var d = new Date();
      output = "Date: " + d.getDate() + "/" + (d.getMonth()+1) + "/" + d.getFullYear();
    </script>
  </div>
  
  <!-- Menu principal -->
  <menu name="main" keytype="numeric" left="5" top="8">
    <item link="services.vtml">1. Nos services</item>
    <item link="contact.vtml">2. Contact</item>
    <item link="about.vtml">3. À propos</item>
  </menu>
  
  <!-- QR code -->
  <div left="25" top="15">
    <qrcode type="url" message="https://example.com" scale="1" left="0" top="0">
  </div>
  
  <!-- Touches de fonction -->
  <key name="sommaire" link="index.vtml">
  <key name="guide" link="aide.vtml">
  
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
