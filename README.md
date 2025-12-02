# Minitel-Serveur 🖥️

**Serveur Minitel moderne** - Connectez votre Minitel historique à l'Internet moderne via Raspberry Pi

Minitel-Serveur est une plateforme Java innovante qui transforme un terminal Minitel en navigateur web moderne grâce au format **VTML** (Videotex Markup Language). Le projet crée un pont technologique entre le protocole Videotex historique et les services web contemporains.

## Table des matières

- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#%EF%B8%8F-architecture)
- [Prérequis](#-prérequis)
- [Installation et Configuration](#-installation-et-configuration)
- [Démarrage Rapide](#-démarrage-rapide)
- [Format VTML](#-format-vtml-videotex-markup-language)
- [Modules Dynamiques (MModules)](#-modules-dynamiques-mmodules)
- [Composants Graphiques](#-composants-graphiques)
- [Navigation et Événements](#-navigation-et-événements)
- [Développement et Debug](#%EF%B8%8F-développement-et-debug)
- [Ressources Techniques](#-ressources-techniques)
- [Contribution](#-contribution)

---

## 🚀 Fonctionnalités

- **🌐 Serveur HTTP** avec support du format VTML
- **📱 Client Minitel** avec communication série optimisée
- **🔧 Modules dynamiques** (MModules) extensibles
- **🎨 Composants graphiques** (texte, images bitmap, menus)
- **⚡ Communication haute vitesse** (9600 bauds sur Minitel 2)
- **🔗 Navigation interactive** avec liens et menus

## 🏗️ Architecture

```
┌─────────────────┐    GPIO/Série      ┌──────────────────┐
│   Minitel       │ ◄────────────────► │  Raspberry Pi    │
│   (Terminal)    │    7E1 Protocol    │                  │
└─────────────────┘                    │  ┌─────────────┐ │    Ethernet
                                       │  │ Java Server │ │ ◄──────────► Internet
                                       │  │   + Client  │ │
                                       │  └─────────────┘ │
                                       └──────────────────┘
```

### Composants Principaux

- **`StaticFileServer`** : Serveur HTTP servant les pages VTML
- **`MinitelClient`** : Interface avec le terminal Minitel physique  
- **`MinitelConnection`** : Gestion communication série (1200/4800/9600 bauds)
- **`Teletel`** : API haut niveau pour contrôle d'affichage Videotex
- **`MModulesManager`** : Système de plugins dynamiques

## 📋 Prérequis

### Matériel
- **Raspberry Pi** (2, 3, 4 ou Zero)
- **Terminal Minitel** (Minitel 1 ou 2)
- **Câble série** DIN → GPIO ou USB-Série
- **Connexion Ethernet/WiFi**

### Logiciel
- **Java 11+** (OpenJDK recommandé)
- **Raspberry Pi OS** ou distribution Linux
- **Accès GPIO** (`/dev/serial0` configuré)

## 🔧 Installation et Configuration

### 1. Connexion Matérielle

**GPIO Raspberry Pi :**
```
Pin 6  (GND)  ──► Minitel GND
Pin 8  (TX)   ──► Minitel RX  
Pin 10 (RX)   ──► Minitel TX
Pin 2  (5V)   ──► Minitel 5V (si nécessaire)
```
Attention !!! Le Minitel ne possède pas de sortie 5v. La broche 5 du Minitel fournit des tensions supérieurs (entre 8,5v à 12v en fonction du modèle).

### 2. Configuration Série

Activez l'UART et désactivez la console série :
```bash
# Dans /boot/config.txt
enable_uart=1
dtoverlay=disable-bt

# Désactiver getty
sudo systemctl disable serial-getty@ttyAMA0.service
```

### 3. Basculement 9600 Bauds (Minitel 2)

Utilisez le script fourni pour basculer en haute vitesse :
```bash
chmod +x switchTo9600b.sh
./switchTo9600b.sh
```

### 4. Fichier de Configuration

Le fichier `config.json` contient toute la configuration du serveur et du client :

```json
{
  "server": {
    "port": 8080,
    "defaultCharset": "utf-8"
  },
  "path": {
    "root_path": "./root/",
    "plugins_path": "./plugins/"
  },
  "client": {
    "serial_port": "/dev/serial0",
    "serial_baud": 9600,
    "joystick_device": "/dev/input/js0",
    "joystick_enabled": true,
    "joystick_mapping": {
      "buttons": {
        "0": "ACTION1",
        "1": "ACTION2"
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

#### Options de configuration

| Section | Clé | Description |
|---------|-----|-------------|
| `server.port` | int | Port HTTP du serveur (défaut: 8080) |
| `server.defaultCharset` | string | Encodage des pages (défaut: utf-8) |
| `path.root_path` | string | Répertoire des pages VTML |
| `path.plugins_path` | string | Répertoire des MModules |
| `client.serial_port` | string | Port série du Minitel |
| `client.serial_baud` | int | Vitesse: 1200, 4800 ou 9600 |
| `client.joystick_enabled` | bool | Activer le support joystick USB |
| `client.joystick_device` | string | Périphérique joystick Linux |
| `client.joystick_mapping` | object | Mapping des boutons/axes |

## 🚀 Démarrage Rapide

### 1. Compilation
```bash
# Compiler le projet
javac -cp "lib/*:src" -d build src/org/somanybits/minitel/**/*.java

# Créer le JAR
jar cfm Minitel.jar manifest.mf -C build .
```

### 2. Lancement du Serveur
```bash
# Démarrer le serveur (utilise config.json)
java -cp Minitel.jar org.somanybits.minitel.server.StaticFileServer
```

### 3. Lancement du Client
```bash
# Connecter le client au serveur
java -jar Minitel.jar localhost 8080
```

## 📝 Format VTML (Videotex Markup Language)

VTML est un langage de markup spécialement conçu pour les contraintes du Minitel (40×25 caractères).

> 📖 **Documentation complète** : [docs/VTML.md](docs/VTML.md)

### Structure de Base

**Fichier : `root/index.vtml`**
```xml
<minitel title="Accueil">
    <!-- Zone de texte positionnée -->
    <div class="frame" left="6" top="2" width="30" height="10">
        <row> __  __ _       _ _       _ </row>
        <row>|  \/  (_)_ __ (_) |_ ___| |</row>
        <row>| |\/| | | '_ \| | __/ _ \ |</row>
        <row>| |  | | | | | | | ||  __/ |</row>
        <row>|_|  |_|_|_| |_|_|\__\___|_|</row>
        <br>
        <row>     LE LIEU TRANQUILLE     </row>
    </div>

    <!-- Menu interactif -->
    <menu name="main" left="4" top="10" width="30" height="10" keytype="number">
        <item link="actualites.vtml">1. Actualités</item>
        <item link="bar/">2. Bar</item>	
        <item link="concerts/">3. Concerts</item>
        <item link="ServerStatus.mod?val1=69&val2=hello">4. Info Serveur</item>
        <item link="wifi/">5. WiFi</item>	
    </menu> 
</minitel>
```

### Tags VTML Supportés

| Tag | Description | Attributs |
|-----|-------------|-----------|
| `<minitel>` | Conteneur principal | `title` |
| `<div>` | Zone de texte positionnée | `left`, `top`, `width`, `height`, `class` |
| `<row>` | Ligne de texte dans une div | - |
| `<menu>` | Menu interactif | `name`, `left`, `top`, `width`, `height`, `keytype` |
| `<item>` | Élément de menu | `link` |
| `<br>` | Saut de ligne | - |

### Système de Coordonnées

```
┌─────────────────────────────────────────┐ ← 40 caractères
│ (0,0)                            (39,0) │
│                                         │
│                                         │ ← 25 lignes
│                                         │
│ (0,24)                          (39,24) │
└─────────────────────────────────────────┘
```

## 🔧 Modules Dynamiques (MModules)

Les **MModules** sont des plugins Java qui étendent les fonctionnalités du serveur, similaires aux CGI ou modules PHP.

### Fonctionnement

- **Chargement automatique** : Tous les fichiers `.jar` dans `plugins/mmodules/` sont chargés au démarrage
- **Activation via URL** : `http://localhost:8080/NomModule.mod`
- **Paramètres GET** : Support des variables via query string
- **Réponse VTML** : Les modules génèrent du contenu VTML dynamique

### Structure d'un MModule

```java
package org.somanybits.minitel.server.mmodules;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.somanybits.minitel.server.ModelMModule;
import org.somanybits.minitel.server.StaticFileServer;

public class ServerStatus extends ModelMModule {
    
    public ServerStatus(HashMap params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }
    
    @Override
    public String getResponse() {
        String resp = "<minitel><div>\n";
        resp += "<row>Module: " + this.getClass().getSimpleName() + "</row>\n";
        resp += "<row>Version: " + getVersion() + "</row>\n";
        resp += "<row>Serveur: " + StaticFileServer.VERSION + "</row>\n";
        resp += "<row>Adresse: " + ex.getLocalAddress() + "</row>\n";
        
        // Affichage des paramètres GET
        if (params != null) {
            resp += "<row>Paramètres:</row>\n";
            for (Map.Entry<String, String> entry : params.entrySet()) {
                resp += "<row>  " + entry.getKey() + " = " + entry.getValue() + "</row>\n";
            }
        }
        
        resp += "</div></minitel>\n";
        return resp;
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

### Utilisation avec Paramètres

**URL avec paramètres :**
```
http://localhost:8080/ServerStatus.mod?utilisateur=eddy&niveau=admin
```

**Récupération dans le module :**
```java
String utilisateur = params.get("utilisateur");  // "eddy"
String niveau = params.get("niveau");            // "admin"
```

### Exemples de MModules

- **`ServerStatus.mod`** : Informations système
- **`QRCodeDemo.mod`** : Générateur de QR codes interactif
- **`WiFiQRDemo.mod`** : Génération de QR Codes WiFi pour connexion automatique
- **`Meteo.mod`** : Données météorologiques via API
- **`News.mod`** : Flux RSS adapté pour Minitel
- **`Chat.mod`** : Système de messagerie simple
- **`IoT.mod`** : Monitoring capteurs domestiques

## 🎨 Composants Graphiques

### GraphTel - Affichage Bitmap et QR Codes

Le système **GraphTel** permet d'afficher des images bitmap et de générer des QR codes sur Minitel :

#### **Affichage d'Images**
```java
// Conversion d'image en bitmap 1bpp
ImageTo1bpp img = new ImageTo1bpp("images_src/photo.jpg", 80, 69);

// Création du composant graphique
GraphTel gfx = new GraphTel(img.getWidth(), img.getHeight());
gfx.writeBitmap(img.getBitmap());
gfx.inverseBitmap();  // Inversion noir/blanc si nécessaire
gfx.drawToPage(teletel, 0, 1);  // Affichage à la position (0,1)
```

#### **Génération de QR Codes**
```java
// Génération de QR Code centré avec échelle 2
graphtel.generateCenteredQRCode("https://example.com", 2);

// QR Code SCANNABLE avec ZXing (iPhone/Android compatible)
graphtel.generateCenteredScannableQR("https://eddy-briere.com", 2);

// QR Code WiFi pour connexion automatique
graphtel.generateWiFiWPA("MonWiFi", "motdepasse123", 2);
graphtel.generateWiFiOpen("WiFi_Gratuit", 2);

// QR Code de test avec motif de vérification
graphtel.generateTestQRCode(10, 10, 3);

// Motif visuel décoratif
graphtel.generateCenteredVisualQR("MINITEL 2024", 3);
```

**Caractéristiques QR Code :**
- **Résolution** : 80×75 pixels en semi-graphique
- **Versions supportées** : QR Code Version 1 (21×21 modules)
- **Facteurs d'échelle** : 1x à 4x (recommandé : 2x ou 3x)
- **Encodage** : Texte simple, URLs, données courtes

## 🔗 Navigation et Événements

### Gestion des Touches

Le client gère automatiquement :
- **Touches numériques** : Navigation dans les menus
- **Touches directionnelles** : Déplacement curseur (mode 80 colonnes)
- **Touches fonction** : SOMMAIRE, RETOUR, SUITE, ENVOI, etc.
- **Touches spéciales** : CORRECTION, RÉPÉTITION, GUIDE

### Liens et Navigation

```xml
<!-- Lien vers page statique -->
<item link="actualites.vtml">Actualités</item>

<!-- Lien vers dossier (cherche index.vtml) -->
<item link="services/">Services</item>

<!-- Lien vers module avec paramètres -->
<item link="meteo.mod?ville=paris&format=simple">Météo Paris</item>
```

## 🛠️ Développement et Debug

### Logs Système

Le système de logs intégré permet le suivi :
```java
LogManager logmgr = Kernel.getInstance().getLogManager();
logmgr.addLog("Message d'information", LogManager.MSG_TYPE_INFO);
logmgr.addLog("Erreur détectée", LogManager.MSG_TYPE_ERROR);
```

### Tests et Émulation

Pour développer sans Minitel physique :
- Utilisez un **émulateur Minitel** (MiniTel, WinTel)
- **Connexion TCP** au lieu de série pour tests
- **Mode debug** avec affichage console des séquences

## 📚 Ressources Techniques

### Protocole Videotex
- **Format série** : 7E1 (7 bits, parité paire, 1 stop bit)
- **Vitesses** : 1200 bauds (standard), 4800/9600 bauds (Minitel 2)
- **Séquences d'échappement** : Compatible norme française

### Références
- [Spécifications Videotex CNET](https://www.minitel.org)
- [Documentation technique Minitel](https://github.com/cquest/minitel)
- [Émulateurs Minitel](http://minitel.3615.org)

---

## 🤝 Contribution

Ce projet est ouvert aux contributions ! N'hésitez pas à :
- Signaler des bugs
- Proposer des améliorations
- Créer de nouveaux MModules
- Améliorer la documentation

**Auteur** : Eddy BRIERE (peassembler@yahoo.fr)  
**Version** : 0.4  
**Licence** : Open Source
