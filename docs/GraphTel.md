# GraphTel - Gestionnaire de graphiques semi-graphiques pour Minitel

## Table des matières

- [Description](#description)
- [Système de coordonnées](#système-de-coordonnées)
- [Palette de couleurs](#palette-de-couleurs)
- [Constructeurs](#constructeurs)
- [Méthodes principales](#méthodes-principales)
  - [Gestion des couleurs](#gestion-des-couleurs)
  - [Primitives de dessin](#primitives-de-dessin)
  - [Manipulation du buffer](#manipulation-du-buffer)
  - [Chargement d'images](#chargement-dimages)
- [Modes de conversion d'images](#modes-de-conversion-dimages)
  - [Algorithme Floyd-Steinberg (Dithering)](#algorithme-floyd-steinberg-dithering)
- [Affichage](#affichage)
- [Getters](#getters)
- [Exemple complet](#exemple-complet)
- [Utilisation avec VTML](#utilisation-avec-vtml)
- [Notes techniques](#notes-techniques)
  - [Caractères semi-graphiques](#caractères-semi-graphiques)
  - [Gestion des couleurs par bloc](#gestion-des-couleurs-par-bloc)
- [Voir aussi](#voir-aussi)

---

## Description

`GraphTel` est une classe qui gère un buffer graphique en mémoire et le convertit en caractères semi-graphiques Minitel. Le Minitel utilise des caractères spéciaux où chaque caractère représente un bloc de **2×3 pixels** (6 pixels par caractère).

---

## Système de coordonnées

| Type | Description | Exemple |
|------|-------------|---------|
| **Pixels** | Coordonnées internes du buffer | 80×72 pixels |
| **Caractères** | Coordonnées écran Minitel | 40×24 caractères |

**Conversion** : 1 caractère = 2 pixels en X, 3 pixels en Y

```
Écran Minitel : 40 colonnes × 25 lignes (caractères)
Buffer GraphTel : 80 × 75 pixels (par défaut)
```

---

## Palette de couleurs

Le Minitel supporte 8 couleurs :

| Code | Nom (EN) | Nom (FR) | RGB |
|------|----------|----------|-----|
| 0 | BLACK | Noir | (0, 0, 0) |
| 1 | RED | Rouge | (255, 0, 0) |
| 2 | GREEN | Vert | (0, 255, 0) |
| 3 | YELLOW | Jaune | (255, 255, 0) |
| 4 | BLUE | Bleu | (0, 0, 255) |
| 5 | MAGENTA | Magenta | (255, 0, 255) |
| 6 | CYAN | Cyan | (0, 255, 255) |
| 7 | WHITE | Blanc | (255, 255, 255) |

---

## Constructeurs

### `GraphTel()`
Crée un GraphTel plein écran (80×75 pixels = 40×25 caractères).

```java
GraphTel gfx = new GraphTel();
```

### `GraphTel(int w, int h)`
Crée un GraphTel avec dimensions personnalisées en pixels.

```java
// 40×24 caractères = 80×72 pixels
GraphTel gfx = new GraphTel(80, 72);
```

> **Note** : Les dimensions sont automatiquement arrondies au multiple supérieur (×2 pour la largeur, ×3 pour la hauteur).

---

## Méthodes principales

### Gestion des couleurs

| Méthode | Description |
|---------|-------------|
| `setInk(byte color)` | Définit la couleur d'encre (foreground) pour les prochains dessins |
| `setBGColor(byte color)` | Définit la couleur de fond (background) |
| `setPen(boolean state)` | Active (`true`) ou désactive (`false`) le stylo |

```java
gfx.setInk(GetTeletelCode.COLOR_RED);
gfx.setBGColor(GetTeletelCode.COLOR_BLACK);
gfx.setPen(true);  // Le stylo dessine
```

---

### Primitives de dessin

#### `setPixel(int x, int y)`
Allume ou éteint un pixel selon l'état du stylo.

```java
gfx.setPen(true);
gfx.setPixel(10, 20);  // Allume le pixel (10, 20)
```

#### `setLine(int x1, int y1, int x2, int y2)`
Trace une ligne entre deux points (algorithme de Bresenham).

```java
gfx.setLine(0, 0, 79, 71);  // Diagonale
```

#### `setCircle(int cx, int cy, int r)`
Trace un cercle (algorithme d'Andres).

```java
gfx.setCircle(40, 36, 20);  // Cercle centré, rayon 20
```

---

### Manipulation du buffer

| Méthode | Description |
|---------|-------------|
| `clear()` | Efface le buffer (tous les pixels à `false`) |
| `inverseBitmap()` | Inverse tous les pixels (effet négatif) |
| `getPixel(int x, int y)` | Retourne l'état d'un pixel (`true`/`false`) |
| `getColor(int x, int y)` | Retourne la couleur d'un pixel (0-7) |

---

### Chargement d'images

#### Depuis un fichier
```java
gfx.loadImage(new File("image.png"));
gfx.loadImage(new File("image.png"), true);  // Avec dithering
```

#### Depuis une URL
```java
gfx.loadImage(new URL("http://example.com/image.png"));
gfx.loadImage(new URL("http://example.com/image.png"), "dithering");
gfx.loadImage(new URL("http://example.com/image.png"), "bitmap");
```

#### Depuis un InputStream
```java
gfx.loadImage(inputStream);
gfx.loadImage(inputStream, true);  // Avec dithering
```

---

## Modes de conversion d'images

| Mode | Paramètre | Description |
|------|-----------|-------------|
| **Couleur** | `null` ou aucun | Conversion vers les 8 couleurs Minitel |
| **Dithering** | `"dithering"` | Tramage Floyd-Steinberg pour meilleur rendu des dégradés |
| **Bitmap** | `"bitmap"` | Noir et blanc uniquement (seuil de luminance à 128) |

### Algorithme Floyd-Steinberg (Dithering)

Le dithering distribue l'erreur de quantification aux pixels voisins :

```
         X    7/16
  3/16  5/16  1/16
```

Cela permet de simuler plus de nuances avec seulement 8 couleurs.

---

## Affichage

### Sur un terminal Minitel (direct)
```java
gfx.drawToPage(teletel, 0, 0);  // Position (0, 0)
gfx.drawToPage(teletel);         // Position par défaut (0, 0)
```

### Génération de bytes (pour envoi différé)
```java
byte[] data = gfx.getDrawToBytes(0, 0);
// Envoyer data au Minitel plus tard
```

### Génération de String (debug)
```java
String str = gfx.getDrawToString(0, 0);
```

---

## Getters

| Méthode | Description |
|---------|-------------|
| `getWidthScreen()` | Largeur du buffer en pixels |
| `getHeightScreen()` | Hauteur du buffer en pixels |
| `getPen()` | État actuel du stylo |
| `getNumberLine()` | Nombre de lignes en caractères |

---

## Exemple complet

```java
// Créer un GraphTel de 40×24 caractères
GraphTel gfx = new GraphTel(80, 72);

// Dessiner un cadre
gfx.setPen(true);
gfx.setInk(GetTeletelCode.COLOR_WHITE);
gfx.setLine(0, 0, 79, 0);    // Haut
gfx.setLine(0, 71, 79, 71);  // Bas
gfx.setLine(0, 0, 0, 71);    // Gauche
gfx.setLine(79, 0, 79, 71);  // Droite

// Dessiner un cercle au centre
gfx.setInk(GetTeletelCode.COLOR_CYAN);
gfx.setCircle(40, 36, 20);

// Charger une image avec dithering
gfx.loadImage(new URL("http://example.com/logo.png"), "dithering");

// Obtenir les bytes pour envoi au Minitel
byte[] data = gfx.getDrawToBytes(0, 0);
```

---

## Utilisation avec VTML

Dans un fichier VTML, utilisez le tag `<img>` qui utilise `GraphTel` en interne :

```xml
<!-- Image couleur (défaut) -->
<img src="images/photo.png" left="0" top="0" width="40" height="24">

<!-- Image avec dithering -->
<img src="images/photo.png" left="0" top="0" width="40" height="24" style="dithering">

<!-- Image noir et blanc -->
<img src="images/photo.png" left="0" top="0" width="40" height="24" style="bitmap">
```

---

## Notes techniques

### Caractères semi-graphiques

Chaque caractère semi-graphique Minitel encode 6 pixels (2×3) dans un octet :

```
+---+---+
| 1 | 2 |  Bits: 0, 1
+---+---+
| 3 | 4 |  Bits: 2, 3
+---+---+
| 5 | 6 |  Bits: 4, 5
+---+---+
```

Le bit 6 est toujours à 1 pour identifier un caractère semi-graphique.

### Gestion des couleurs par bloc

Chaque bloc 2×3 ne peut avoir qu'une seule couleur d'encre et une seule couleur de fond. `GraphTel` détermine automatiquement :
- **Couleur d'encre** : Couleur dominante des pixels allumés
- **Couleur de fond** : Couleur dominante des pixels éteints

### Reset des couleurs

Le Minitel réinitialise les couleurs à chaque repositionnement du curseur. `GraphTel` gère cela automatiquement en réémettant les codes couleur à chaque ligne.

---

## Voir aussi

- [VTML.md](VTML.md) - Documentation du langage VTML
- `GetTeletelCode` - Codes Vidéotex
- `Teletel` - Interface terminal Minitel
