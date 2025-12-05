# MModules - Modules Dynamiques

Les MModules sont des plugins dynamiques qui génèrent du contenu VTML. Ils sont chargés depuis le répertoire `plugins/mmodules/` sous forme de fichiers JAR.

## Configuration des MModules

Chaque MModule peut avoir son propre fichier de configuration JSON. Le chemin du dossier de configuration est défini dans `config.json` :

```json
{
  "path": {
    "mmodules_config_path": "./mmodules_config/"
  }
}
```

### Convention de nommage

Le fichier de configuration doit porter le **même nom que la classe du MModule** avec l'extension `.json`.

| Classe MModule | Fichier de configuration |
|----------------|--------------------------|
| `ServerScore`  | `mmodules_config/ServerScore.json` |
| `ZXingQRDemo`  | `mmodules_config/ZXingQRDemo.json` |
| `MonModule`    | `mmodules_config/MonModule.json` |

### Exemple de fichier de configuration

`mmodules_config/ServerScore.json` :
```json
{
  "maxRecords": 10,
  "dataPath": "/home/eddy/minitel/.data/scores/"
}
```

### Chemins relatifs vs absolus

Pour les chemins de fichiers dans la configuration, il est recommandé d'utiliser des **chemins absolus** pour éviter toute ambiguïté. Les chemins relatifs (comme `./data/`) sont résolus par rapport au répertoire de travail courant du processus Java, qui peut varier selon comment le serveur est lancé.

```json
{
  "dataPath": "/home/eddy/minitel/.data/scores/"
}
```

Si vous devez utiliser des chemins relatifs, résolvez-les dans votre code par rapport à `docRoot` :

```java
String configPath = config.get("dataPath").asText();
if (configPath.startsWith("./") || !configPath.startsWith("/")) {
    // Résoudre par rapport à la racine du projet
    configPath = docRoot.getParent().resolve(configPath).normalize().toString();
}
```

## Utilisation dans un MModule

Pour lire la configuration, utilisez la méthode `readConfig()` héritée de `ModelMModule` :

```java
public class ServerScore extends ModelMModule {

    public ServerScore(HashMap params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }

    @Override
    public String getResponse() {
        // Charger la configuration depuis mmodules_config/ServerScore.json
        JsonNode config = readConfig();
        
        if (config != null) {
            int maxRecords = config.get("maxRecords").asInt();
            String dataPath = config.get("dataPath").asText();
            
            // Utiliser les valeurs...
        }
        
        return "<minitel>...</minitel>";
    }
}
```

## API de ModelMModule

### Méthodes disponibles

| Méthode | Description |
|---------|-------------|
| `readConfig()` | Lit et retourne le fichier JSON de configuration (`JsonNode`) |
| `getConfigPath()` | Retourne le chemin du fichier de configuration (après appel à `readConfig()`) |

### Attributs protégés

| Attribut | Type | Description |
|----------|------|-------------|
| `params` | `HashMap<String, String>` | Paramètres GET de la requête HTTP |
| `ex` | `HttpExchange` | Objet de la requête HTTP |
| `docRoot` | `Path` | Chemin racine des documents VTML |
| `configPath` | `Path` | Chemin du fichier de configuration |
| `moduleConfig` | `JsonNode` | Configuration JSON chargée |

## Création d'un nouveau MModule

1. Créer une classe qui étend `ModelMModule`
2. Implémenter les méthodes de l'interface `MModule`
3. (Optionnel) Créer un fichier de configuration JSON

```java
package org.somanybits.minitel.server.mmodules;

public class MonModule extends ModelMModule {
    
    public MonModule(HashMap params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }
    
    @Override
    public String getResponse() {
        JsonNode config = readConfig();
        String message = "Hello";
        
        if (config != null && config.has("message")) {
            message = config.get("message").asText();
        }
        
        return "<minitel><div><row>" + message + "</row></div></minitel>";
    }
    
    @Override
    public String getVersion() { 
        return "1.0"; 
    }
    
    @Override
    public String getContentType() { 
        return "text/plain; charset=UTF-8"; 
    }
}
```

## Structure des fichiers

```
Minitel-Serveur/
├── config.json                    # Configuration serveur (mmodules_config_path)
├── mmodules_config/               # Dossier des configurations MModules
│   ├── ServerScore.json
│   ├── ZXingQRDemo.json
│   └── ...
├── plugins/
│   └── mmodules/                  # Fichiers JAR des MModules
│       ├── MModules.jar
│       └── ...
└── src/
    └── org/somanybits/minitel/server/
        ├── MModule.java           # Interface
        ├── ModelMModule.java      # Classe de base abstraite
        ├── MModulesManager.java   # Gestionnaire de chargement
        └── mmodules/              # Implémentations
            ├── ServerScore.java
            └── ...
```
