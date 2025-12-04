# M-Kiwi : Redonner vie au Minitel 🇫🇷🥝

## Le Minitel, c'est quoi ?

Le Minitel était le précurseur d'Internet en France. Dès les années 80, des millions de Français consultaient l'annuaire électronique, réservaient des billets de train, ou discutaient sur des messageries... 20 ans avant le Web ! Ce petit terminal beige avec son écran vert était révolutionnaire.

Aujourd'hui, ces machines dorment dans les greniers. **Et si on leur redonnait vie ?**

---

## L'idée du projet

**M-Kiwi** transforme un Raspberry Pi en serveur Minitel moderne, exactement comme on créerait un site web aujourd'hui, mais pour ce terminal rétro.

### Un serveur web... pour Minitel

Imaginez : vous créez des pages comme on fait du HTML, mais adaptées à l'écran du Minitel (40 colonnes, 25 lignes, graphismes en mosaïque). Le serveur les envoie au terminal via un simple câble série.

```
┌─────────────────┐         ┌─────────────────┐
│   Raspberry Pi  │ ──────► │    Minitel      │
│   (M-Kiwi)      │  série  │   (terminal)    │
└─────────────────┘         └─────────────────┘
```

### La puissance du JavaScript

C'est là que ça devient intéressant : on peut intégrer du **JavaScript** dans les pages ! Le même langage qui fait tourner les sites web modernes. Cela permet de créer des pages interactives, des formulaires intelligents, et surtout... **des jeux**.

---

## Des jeux sur Minitel !

M-Kiwi permet de créer des jeux vidéo jouables sur Minitel :

- 🐍 **Snake** - Le classique du serpent qui grandit
- 🧱 **Tetris** - Les pièces qui tombent
- 🏓 **Pong** - Le tennis de table rétro
- 👾 **Space Invaders** - Défendre la Terre contre les aliens
- 🧱 **Breakout** - Casser des briques avec une balle

### Comment ça marche ?

On définit des **sprites** (petits dessins), des **zones de jeu**, et des **contrôles**. Le JavaScript gère la logique : déplacements, collisions, score...

Et le plus beau : on peut brancher une **manette USB** ! Le Raspberry Pi la détecte et traduit les mouvements en actions dans le jeu. On peut même jouer **à deux** avec deux manettes.

---

## Pourquoi ce projet ?

### 🎓 Pédagogique
Le Minitel est un excellent support pour apprendre la programmation : contraintes techniques simples, résultats visuels immédiats, et un côté "vintage" qui fascine.

### 🎨 Créatif
Créer avec des contraintes (40×25 caractères, 8 couleurs) pousse à l'inventivité. C'est l'esprit du pixel art appliqué au texte.

### 🕹️ Ludique
Jouer à Snake sur un vrai Minitel des années 80, avec une manette USB moderne, c'est une expérience unique qui mélange nostalgie et technologie.

### ♻️ Écologique
Plutôt que de jeter ces machines, on leur offre une seconde vie. Le Minitel devient une console de jeux rétro ou un terminal d'affichage original.

---

## En résumé

**M-Kiwi** est un pont entre deux époques :

| Années 80 | Aujourd'hui |
|-----------|-------------|
| Minitel | Raspberry Pi |
| Vidéotex | JavaScript |
| Clavier à membrane | Manettes USB |
| Services 3615 | Pages VTML |

---

## À propos

**M-Kiwi** est un projet personnel développé par **Eddy Brière**, passionné par le rétro-computing et l'histoire numérique française.

Le développement a été accompagné par **Claude** (Anthropic), une intelligence artificielle qui a assisté sur :
- La rédaction de la documentation
- La création des jeux exploitant les capacités du client M-Kiwi
- L'architecture et l'optimisation du code

Cette collaboration homme-machine illustre comment l'IA peut accompagner les développeurs solo dans leurs projets ambitieux, en apportant une aide à la fois technique et créative.

<i>Remercîments à mon ami Daniel Da Cunha pour ses précieux conseils.</i>   

---

**Le Minitel n'est pas mort. Il joue à Tetris.** 🎮🥝
