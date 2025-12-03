# ServerScore - Module de gestion des scores

Le module **ServerScore** permet de gérer des tableaux de scores pour les jeux M-Kiwi. Il offre une API HTTP simple pour enregistrer, lire et interroger les meilleurs scores.

---

## Principe de fonctionnement

Chaque jeu est identifié par un **GameId** unique (généré manuellement). Le module stocke les scores avec des champs personnalisables (score, nom, date, etc.) et limite le nombre d'enregistrements.

```
┌─────────────────┐           ┌─────────────────┐
│   Jeu Minitel   │ ──HTTP──► │  ServerScore    │
│   (client)      │ ◄──────── │   (module)      │
└─────────────────┘           └─────────────────┘
```

---

## Configuration

Lors de la création d'un tableau de scores, vous définissez :

| Paramètre | Description |
|-----------|-------------|
| **GameId** | Identifiant unique du jeu (ex: `tetris_038ad74e-772c-43c5-8c5b-d719be30f487`) |
| **Champs** | Liste des champs à stocker (ex: `score`, `name`, `date`) |
| **Max scores** | Nombre maximum de scores conservés |

---

## API HTTP

### URL de base

```
http://[HOST]:8080/ServerScore.mod
```

### Modes disponibles

| Mode | Description |
|------|-------------|
| `write` | Enregistrer un nouveau score |
| `read` | Lire tous les scores enregistrés |
| `top1` | Récupérer le meilleur score |
| `top10` | Récupérer le 10ème meilleur score (seuil) |

---

## Mode `write` - Enregistrer un score

Enregistre un nouveau score dans le tableau.

### Requête

```
GET /ServerScore.mod?mode=write&gameid={GameId}&values={valeur1},{valeur2},...
```

### Paramètres

| Paramètre | Description |
|-----------|-------------|
| `mode` | `write` |
| `gameid` | Identifiant unique du jeu |
| `values` | Valeurs séparées par des virgules, dans l'ordre des champs définis |

### Exemple

Pour un jeu avec les champs `score,name` :

```
http://192.168.0.119:8080/ServerScore.mod?mode=write&gameid=tetris_038ad74e-772c-43c5-8c5b-d719be30f487&values=Paul,1400
```

---

## Mode `read` - Lire les scores

Récupère la liste des scores enregistrés, triés selon l'ordre des champs spécifiés.

### Requête

```
GET /ServerScore.mod?mode=read&gameid={GameId}&fields={champ1},{champ2},...
```

### Paramètres

| Paramètre | Description |
|-----------|-------------|
| `mode` | `read` |
| `gameid` | Identifiant unique du jeu |
| `fields` | Champs à retourner, séparés par des virgules. **Le premier champ définit le tri.** |

### Exemple

```
http://192.168.0.119:8080/ServerScore.mod?mode=read&gameid=tetris_038ad74e-772c-43c5-8c5b-d719be30f487&fields=score,name
```

### Réponse

Les scores sont retournés sous forme de texte, séparés par `&` :

```
400000,lirililarila&11000,Pauline&10000,Hugo&9750,Gwen&9500,Eddy&8800,Malika&8760,Mamidou&7000,Savannah&4050,Paul&3780,PAT
```

**Format** : `{score},{name}&{score},{name}&...`

---

## Mode `top1` - Meilleur score

Retourne uniquement le meilleur score enregistré.

### Requête

```
GET /ServerScore.mod?mode=top1&gameid={GameId}
```

### Réponse

```
400000,lirililarila
```

---

## Mode `top10` - 10ème meilleur score

Retourne le 10ème meilleur score (utile pour vérifier si un nouveau score entre dans le top 10).

### Requête

```
GET /ServerScore.mod?mode=top10&gameid={GameId}
```

### Réponse

```
3780,PAT
```

---

## Exemple d'utilisation en VTML

### Afficher le tableau des scores

```xml
<minitel title="Meilleurs Scores">
  <div name="lstscore" left="8" top="6" width="36" height="20">
    <row>== MEILLEURS SCORES ==</row>
    <br/>
    
    <script>
      var serverscore = "http://192.168.0.119:8080/";
      var GameId = "tetris_038ad74e-772c-43c5-8c5b-d719be30f487";
      
      function domReady() {
        var data = fetchUrl(serverscore + "ServerScore.mod?mode=read&gameid=" + GameId + "&fields=score,name");
        var items = data.split("&");
        var container = getElementByName("lstscore");
        
        for (var i = 0; i < items.length; i++) {
          var values = items[i].split(",");
          var myrow = container.createElement("row");
          myrow.setText((i+1) + " -> " + values[0] + " " + values[1]);
        }
      }
      
      function fetchUrl(urlString) {
        var url = new java.net.URL(urlString);
        var connection = url.openConnection();
        connection.setRequestMethod("GET");
        
        var reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(connection.getInputStream())
        );
        
        var response = "";
        var line;
        while ((line = reader.readLine()) != null) {
          response += line;
        }
        reader.close();
        return response;
      }
    </script>
  </div>
</minitel>
```

### Enregistrer un score depuis un jeu

```javascript
function saveScore(playerName, score) {
  var url = serverscore + "ServerScore.mod?mode=write&gameid=" + GameId + "&values=" + playerName + "," + score;
  fetchUrl(url);
}

// Après game over
saveScore("Eddy", 9500);
```

### Vérifier si le score entre dans le top 10

```javascript
function isHighScore(score) {
  var top10 = fetchUrl(serverscore + "ServerScore.mod?mode=top10&gameid=" + GameId);
  var minScore = parseInt(top10.split(",")[0]);
  return score > minScore;
}

// Afficher "NEW HIGH SCORE!" si le joueur entre dans le classement
if (isHighScore(playerScore)) {
  // Demander le nom et sauvegarder
}
```

---

## Bonnes pratiques

1. **GameId unique** : Utilisez un UUID pour éviter les collisions entre jeux
2. **Validation** : Vérifiez le score côté serveur si possible
3. **Stockage du GameId** : Utilisez `storage.set("GameId", ...)` pour le partager entre pages
4. **Tri** : Placez le champ de tri en premier dans `fields` (généralement `score`)

---

## Notes techniques

- Les scores sont triés par ordre décroissant (meilleur en premier)
- Le séparateur entre enregistrements est `&`
- Le séparateur entre champs est `,`
- Les espaces dans les valeurs sont conservés

---

**Module développé pour M-Kiwi** 🥝🎮
