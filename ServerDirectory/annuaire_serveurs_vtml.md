# Annuaire de serveurs VTML

## Objectif

Créer un outil permettant de gérer un annuaire de serveurs VTML utilisable par le client Minitel **M-Kiwi**.

L’annuaire doit permettre :

- d’enregistrer des serveurs VTML ;
- de rechercher des serveurs par nom, description, mot-clé ou catégorie ;
- de consulter les informations techniques d’un serveur ;
- d’utiliser l’annuaire directement depuis le client M-Kiwi via une page Minitel dédiée.

Le code de l’annuaire devra être ajouté dans le dossier suivant, à la racine du projet :

```text
ServerDirectory/
```

---

## Architecture générale

Le serveur d’annuaire sera composé de trois éléments principaux :

```text
Client M-Kiwi
    ↓ requêtes HTTP / JSON
Serveur FastAPI
    ↓ lecture / écriture
Base de données légère
```

### Composants

| Composant | Rôle |
|---|---|
| **FastAPI** | Backend HTTP exposant les données de l’annuaire |
| **Base de données légère** | Stockage des serveurs VTML |
| **Interface admin HTML/JavaScript** | Administration de l’annuaire |
| **Client M-Kiwi** | Consultation et recherche de serveurs VTML |

---

## Backend serveur

### Technologie proposée

Pour la partie serveur, la solution proposée est :

```text
FastAPI + JSON
```

Une base SQLite pourra être utilisée si le projet évolue et nécessite des requêtes plus structurées.

### Formats de stockage possibles

| Format | Avantage | Inconvénient |
|---|---|---|
| **JSON** | Simple, lisible, facile à modifier | Moins adapté aux recherches complexes |
| **XML** | Structuré, compatible avec certains anciens outils | Verbeux |
| **SQLite** | Solide, requêtes SQL, évolutif | Nécessite une couche d’accès DB |

Pour une première version, le format recommandé est :

```text
JSON
```

Puis migration éventuelle vers :

```text
SQLite
```

---

## Structure d’un serveur VTML

Un serveur VTML enregistré dans l’annuaire doit contenir les propriétés suivantes :

| Champ | Type | Description |
|---|---|---|
| `id` | entier | Identifiant unique du serveur |
| `url` | chaîne | URL ou adresse IP du serveur |
| `port` | entier | Port du serveur, `80` par défaut |
| `name` | chaîne | Nom du serveur |
| `description` | chaîne | Description courte du serveur |
| `keywords` | liste | Liste de mots-clés pour la recherche |
| `admin_email` | chaîne | Email de l’administrateur |
| `website` | chaîne | Site web, GitHub, Facebook ou autre lien |
| `vtml_version` | chaîne | Version VTML supportée |
| `mkiwi_server_version` | chaîne | Version serveur M-Kiwi supportée |
| `categories` | liste | Liste de catégories associées |
| `enabled` | booléen | Indique si le serveur doit être visible dans l’annuaire |

### Exemple JSON

```json
{
  "id": 1,
  "url": "somanybits.com",
  "port": 80,
  "name": "Somanybits",
  "description": "Serveur VTML de démonstration",
  "keywords": ["minitel", "vtml", "demo", "retro"],
  "admin_email": "admin@somanybits.com",
  "website": "https://somanybits.com",
  "vtml_version": "1.0",
  "mkiwi_server_version": "1.0",
  "categories": ["demo", "retro", "public"],
  "enabled": true
}
```

---

## API FastAPI

Le serveur FastAPI devra exposer des routes permettant au client M-Kiwi et à l’interface d’administration d’interagir avec l’annuaire.

### Routes proposées

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/servers` | Liste des serveurs VTML |
| `GET` | `/api/servers/{id}` | Détail d’un serveur |
| `GET` | `/api/search?q=...` | Recherche dans l’annuaire |
| `POST` | `/api/servers` | Ajout d’un serveur |
| `PUT` | `/api/servers/{id}` | Modification d’un serveur |
| `DELETE` | `/api/servers/{id}` | Suppression d’un serveur |
| `GET` | `/api/health` | État du service |

### Paramètres de pagination

La route `/api/servers` pourra accepter les paramètres suivants :

| Paramètre | Description | Valeur par défaut |
|---|---|---|
| `page` | Numéro de page | `1` |
| `limit` | Nombre de résultats par page | `10` |
| `q` | Recherche texte optionnelle | vide |
| `category` | Filtrage par catégorie | vide |

Exemple :

```text
GET /api/servers?page=1&limit=10&q=retro
```

### Exemple de réponse

```json
{
  "page": 1,
  "limit": 10,
  "total": 2,
  "pages": 1,
  "items": [
    {
      "id": 1,
      "url": "192.168.0.155",
      "port": 8080,
      "name": "M-Kiwi",
      "description": "Serveur VTML local",
      "categories": ["local", "test"]
    },
    {
      "id": 2,
      "url": "somanybits.com",
      "port": 80,
      "name": "Somanybits",
      "description": "Serveur public VTML",
      "categories": ["public", "demo"]
    }
  ]
}
```

---

## Interface d’administration

Une interface web en HTML/JavaScript permettra d’administrer l’annuaire.

### Fonctionnalités attendues

- afficher la liste des serveurs VTML ;
- ajouter un nouveau serveur ;
- modifier un serveur existant ;
- supprimer un serveur ;
- rechercher par nom, mot-clé ou catégorie ;
- activer ou désactiver un serveur ;
- tester l’accessibilité d’un serveur VTML ;
- visualiser les informations détaillées d’un serveur.

### Emplacement proposé

```text
ServerDirectory/
├── main.py
├── requirements.txt
├── README.md
├── data/
│   └── servers.json
├── static/
│   ├── admin.html
│   ├── admin.js
│   └── style.css
└── tests/
    └── test_api.py
```

---

## Lien entre le client M-Kiwi et l’annuaire VTML

### Principe

Le client M-Kiwi se connecte au serveur d’annuaire via des requêtes HTTP.

Les données sont récupérées au format JSON depuis FastAPI.

L’implémentation pourra s’appuyer sur l’exemple présent dans :

```text
docs/JS.md
```

Chapitre :

```text
Exemple : requête HTTP
```

Ces requêtes permettront de construire une page Minitel de recherche et de navigation dans l’annuaire.

---

## URL spéciale côté client

Le client M-Kiwi pourra ouvrir l’annuaire via une URL spécifique :

```text
mkiwi:server_directory
```

Cette URL affichera une page Minitel dédiée à la recherche de serveurs VTML.

---

## Configuration du serveur d’annuaire

Le serveur d’annuaire utilisé par défaut pourra être défini dans le fichier de configuration du client :

```text
config.json
```

Exemple :

```json
{
  "server_directory": "http://somanybits.com"
}
```

L’utilisateur pourra également modifier temporairement l’adresse du serveur d’annuaire depuis l’interface Minitel, via le champ :

```text
Serveur
```

---

## Interface client Minitel

### Écran de recherche

Exemple d’écran Minitel pour :

```text
mkiwi:server_directory
```

```text
#########################################
# Annuaire M-Kiwi                       #
#                                       #
# Serveur : somanybits.com________      #
#                                       #
# Chercher : ______________________     #
#                                       #
# Liste des serveurs :                  #
#                                       #
#      1 192.168.0.155:8080             #
#      2 M-Kiwi                         #
#      3 MO5                            #
#      4 GameKiwi                       #
#                                       #
#                                       #
#                                       #
#                                       #
#      [RETOUR] Page 1/1 [SUITE]        #
#                                       #
#                                       #
#                                       #
#                                       #
#                                       #
#                                       #
#                                       #
#                                       #
#########################################
```

> Note : les caractères `#` représentent les limites de l’écran Minitel.  
> Ils ne doivent pas être affichés à l’écran.

---

## Champs de l’écran

### Serveur

Le champ `Serveur` permet de définir l’adresse du serveur d’annuaire utilisé.

Exemples :

```text
somanybits.com
```

```text
192.168.0.155:8080
```

Cette valeur peut provenir :

- du fichier `config.json` ;
- d’une saisie manuelle de l’utilisateur ;
- d’une valeur par défaut intégrée au client.

---

### Chercher

Le champ `Chercher` permet de rechercher un serveur VTML.

La recherche doit pouvoir porter sur :

- le nom du serveur ;
- sa description ;
- ses mots-clés ;
- ses catégories ;
- son URL.

Exemples :

```text
retro
```

```text
jeu
```

```text
MO5
```

---

### Liste des serveurs VTML

La zone `Liste des serveurs` affiche les résultats retournés par le serveur d’annuaire.

La liste doit fonctionner comme un menu numérique.

Exemple :

```text
1 192.168.0.155:8080
2 M-Kiwi
3 MO5
4 GameKiwi
```

L’utilisateur peut sélectionner un serveur en appuyant sur le numéro correspondant.

---

## Pagination

La liste affiche par défaut les **10 premiers résultats**.

Si plus de 10 serveurs sont disponibles, la navigation se fait par pages.

Commandes proposées :

| Commande | Action |
|---|---|
| `SUITE` | Afficher les 10 résultats suivants |
| `RETOUR` | Afficher les 10 résultats précédents |
| `1` à `10` | Sélectionner un serveur |

Exemple :

```text
[RETOUR] Page 1/3 [SUITE]
```

---

## Sélection d’un serveur

Lorsqu’un utilisateur sélectionne un serveur dans la liste, le client M-Kiwi peut :

1. afficher une fiche détaillée du serveur ;
2. proposer une connexion directe ;
3. enregistrer le serveur dans les favoris ;
4. revenir à la liste.

### Exemple de fiche serveur

```text
#########################################
# Serveur VTML                          #
#                                       #
# Nom : M-Kiwi                          #
# URL : 192.168.0.155                   #
# Port: 8080                            #
#                                       #
# Description :                         #
# Serveur local de test VTML            #
#                                       #
# Version VTML : 1.0                    #
# Version M-Kiwi : 1.0                  #
#                                       #
# 1 Connexion                           #
# 2 Favoris                             #
# 3 Retour                              #
#                                       #
#########################################
```

---

## Comportement attendu

### Au lancement de `mkiwi:server_directory`

Le client doit :

1. charger l’adresse du serveur d’annuaire depuis `config.json` ;
2. appeler l’API FastAPI ;
3. récupérer la liste des serveurs au format JSON ;
4. afficher les résultats sur l’écran Minitel ;
5. permettre la recherche, la pagination et la sélection.

---

## Gestion des erreurs

Le client doit gérer les cas suivants :

| Erreur | Comportement attendu |
|---|---|
| Serveur d’annuaire inaccessible | Afficher un message d’erreur |
| Réponse JSON invalide | Afficher une erreur de format |
| Aucun résultat | Afficher `Aucun serveur trouvé` |
| Timeout HTTP | Proposer de réessayer |
| URL invalide | Demander une nouvelle adresse |
| Serveur VTML désactivé | Ne pas l’afficher dans la liste publique |

### Exemple d’erreur

```text
#########################################
# Annuaire M-Kiwi                       #
#                                       #
# Erreur : serveur inaccessible         #
#                                       #
# Vérifiez l'adresse du serveur         #
# d'annuaire.                           #
#                                       #
# [RETOUR]                              #
#########################################
```

---

## Sécurité et validation

### Côté serveur

Le backend devra vérifier :

- que l’URL est valide ;
- que le port est compris entre `1` et `65535` ;
- que l’email administrateur est valide ;
- que les champs obligatoires sont présents ;
- que les chaînes ne dépassent pas une longueur raisonnable ;
- que les catégories et mots-clés ne contiennent pas de caractères problématiques.

### Côté interface admin

L’interface d’administration devra :

- valider les champs avant envoi ;
- afficher les erreurs retournées par l’API ;
- empêcher l’envoi de formulaires incomplets ;
- confirmer les suppressions.

---

## Format minimal d’un serveur

Pour la V1, seuls les champs suivants sont strictement nécessaires :

```json
{
  "id": 1,
  "url": "192.168.0.155",
  "port": 8080,
  "name": "M-Kiwi",
  "description": "Serveur local de test VTML",
  "keywords": ["local", "test"],
  "categories": ["local"],
  "enabled": true
}
```

Les autres champs peuvent être optionnels.

---

## Résumé technique

### Serveur

```text
FastAPI
JSON ou SQLite
Interface admin HTML/JavaScript
Dossier : ServerDirectory/
```

### Client

```text
URL spéciale : mkiwi:server_directory
Requêtes HTTP JSON
Affichage Minitel
Recherche
Pagination
Sélection d’un serveur
```

### Format recommandé pour la V1

```text
Backend : FastAPI
Base : JSON
Interface admin : HTML + JavaScript
Réponse API : JSON
Pagination : 10 résultats par page
```

---

## Plan de développement proposé

### Étape 1 — Backend minimal

- créer le dossier `ServerDirectory/` ;
- créer `main.py` ;
- créer `data/servers.json` ;
- exposer `GET /api/servers` ;
- exposer `GET /api/search?q=...`.

### Étape 2 — Interface admin

- créer `static/admin.html` ;
- afficher les serveurs ;
- ajouter un formulaire de création ;
- gérer modification et suppression.

### Étape 3 — Intégration M-Kiwi

- ajouter la route spéciale `mkiwi:server_directory` ;
- récupérer le serveur d’annuaire depuis `config.json` ;
- appeler l’API HTTP ;
- afficher les résultats sur l’écran Minitel.

### Étape 4 — Pagination et recherche

- limiter à 10 résultats par page ;
- ajouter `SUITE` et `RETOUR` ;
- gérer la sélection numérique ;
- afficher la fiche détail d’un serveur.

### Étape 5 — Stabilisation

- validation des champs ;
- gestion des erreurs réseau ;
- tests API ;
- documentation d’installation ;
- choix définitif entre JSON et SQLite.

---

## Notes de conception

- Le client ne doit jamais exécuter de SQL directement.
- Le client doit uniquement consommer des endpoints HTTP.
- Le format JSON est recommandé pour les échanges entre M-Kiwi et l’annuaire.
- Le backend peut changer de stockage interne sans modifier le protocole client.
- La pagination est indispensable pour respecter les contraintes d’affichage Minitel.
- Les noms affichés doivent être courts pour rester lisibles sur écran Minitel.
