# M-Kiwi (Minitel-Serveur) 🖥️

**Serveur Minitel moderne** - Connectez votre Minitel historique à l'Internet moderne via Raspberry Pi

Minitel-Serveur est une plateforme Java innovante qui transforme un terminal Minitel en navigateur web moderne grâce au format **VTML** (Videotex Markup Language). Le projet crée un pont technologique entre le protocole Videotex historique et les services web contemporains.

## Table des matières

- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#%EF%B8%8F-architecture)
  - [Composants Principaux](#composants-principaux)
- [Prérequis](#-prérequis)
  - [Matériel](#matériel)
  - [Logiciel](#logiciel)
- [Installation et Configuration](#-installation-et-configuration)
  - [1. Connexion Matérielle](#1-connexion-matérielle)
  - [2. Shield M-Kiwi (carte fille GPIO)](#2-shield-m-kiwi-carte-fille-gpio-optionnelle)
  - [3. Configuration Série](#3-configuration-série)
  - [4. Auto-négociation de Vitesse](#4-auto-négociation-de-vitesse)
  - [5. Désactiver le mode ECHO](#5-désactiver-le-mode-echo)
  - [6. Fichier de Configuration](#6-fichier-de-configuration)
- [Démarrage Rapide](#-démarrage-rapide)
  - [Déploiement](#déploiement)
  - [1. Compilation](#1-compilation)
  - [2. Lancement du Serveur](#2-lancement-du-serveur)
  - [3. Lancement du Client](#3-lancement-du-client)
  - [4. Test des Joysticks USB](#4-test-des-joysticks-usb)
  - [5. Installation en tant que services Linux](#5-installation-en-tant-que-services-linux)
- [Format VTML](#-format-vtml-videotex-markup-language)
  - [Structure de Base](#structure-de-base)
  - [Tags VTML Supportés](#tags-vtml-supportés)
  - [Système de Coordonnées](#système-de-coordonnées)
- [Modules Dynamiques (MModules)](#-modules-dynamiques-mmodules)
  - [Fonctionnement](#fonctionnement)
  - [Structure d'un MModule](#structure-dun-mmodule)
  - [Utilisation avec Paramètres](#utilisation-avec-paramètres)
  - [Configuration avec readConfig()](#configuration-des-mmodules-avec-readconfig)
  - [Exemples de MModules](#exemples-de-mmodules)
  - [Module natif : ServerScore](#module-natif--serverscore)
- [Composants Graphiques](#-composants-graphiques)
  - [GraphTel - Affichage Bitmap et QR Codes](#graphtel---affichage-bitmap-et-qr-codes)
- [Navigation et Événements](#-navigation-et-événements)
  - [Gestion des Touches](#gestion-des-touches)
  - [Liens et Navigation](#liens-et-navigation)
- [Développement et Debug](#%EF%B8%8F-développement-et-debug)
  - [Logs Système](#logs-système)
  - [Tests et Émulation](#tests-et-émulation)
- [Ressources Techniques](#-ressources-techniques)
  - [Protocole Videotex](#protocole-videotex)
  - [Protection Ligne 0](#protection-ligne-0)
  - [Références](#références)
- [Contribution](#-contribution)

---

## Documentations techniques

- [Hardware.md](docs/Hardware.md) — Package `org.somanybits.minitel.hardware` : GPIOLed, GPIOButton, OLEDDisplay, OLEDClient
- [PCB_HAT.md](docs/PCB_HAT.md) — Schéma et BOM du Shield M-Kiwi
- [VTML.md](docs/VTML.md) — Référence du langage VTML
- [MModules.md](docs/MModules.md) — Création de modules dynamiques
- [GraphTel.md](docs/GraphTel.md) — Affichage bitmap et QR codes
- [SERVICES.md](docs/SERVICES.md) — Installation en tant que services Linux

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
- **Java 17+** (OpenJDK 17 recommandé)
- **Raspberry Pi OS** ou distribution Linux
- **Accès GPIO** (`/dev/serial0` configuré)

#### Installation de Java 17 sur Raspberry Pi

```bash
# Mise à jour du système
sudo apt update && sudo apt upgrade -y

# Installation d'OpenJDK 17
sudo apt install openjdk-17-jdk -y

# Vérification de l'installation
java -version
```

> **Note** : Sur Raspberry Pi OS Lite, vous pouvez aussi utiliser `openjdk-17-jre` si vous n'avez pas besoin de compiler.

#### Téléchargement des librairies Java

Un script est fourni pour télécharger automatiquement toutes les dépendances Java depuis Maven Central :

```bash
cd lib
./download_libs.sh
```

Librairies téléchargées :
- **ZXing** 3.5.1 - Génération de QR Codes
- **Jackson** 2.17.2 - Parsing JSON
- **Jsoup** 1.18.1 - Parsing HTML/XML (VTML)
- **JSSC** 2.8.0 - Communication série
- **Rhino** 1.7.14 - Moteur JavaScript

## 🔧 Installation et Configuration

### 1. Connexion Matérielle

**GPIO Raspberry Pi :**
```
Pin 6  (GND)  ──► Minitel GND
Pin 8  (TX)   ──► Minitel RX  
Pin 10 (RX)   ──► Minitel TX
Pin 2  (5V)   ──► Minitel 5V (si nécessaire)
```

**Connexion avec le Minitel :**

Le Minitel fonctionne en logique 5V tandis que le Raspberry Pi utilise du 3.3V. Un **level shifter bidirectionnel** est nécessaire pour la conversion des niveaux logiques. Un pull-up de 2.2kΩ sur la ligne RX (côté 3.3V) améliore la stabilité de réception clavier.

```
                RASPBERRY PI                          LEVEL SHIFTER                           MINITEL
                (3.3V Logic)                         (Bidirectionnel)                        (5V Logic)
                                                
                ┌─────────────┐                  ┌─────────────────────┐                  ┌─────────────┐
                │             │                  │                     │                  │             │
   3.3V (Pin 1) ┼──────┬──────┼──────────────────┤ LV            HV    ├──────────────────┼── 5V        │
                │      │      │                  │                     │                  │             │
   GND (Pin 6) ─┼──────┼──────┼──────────────────┤ GND          GND    ├──────────────────┼── GND       │
                │      │      │                  │                     │                  │  (DIN 2)    │
                │      │      │                  │                     │                  │             │
   Pin 8 (TX) ──┼──────┼──────┼──────────────────┤ LV1          HV1    ├──────────────────┼──► RX       │
   GPIO14       │      │      │                  │                     │                  │  (DIN 1)    │
                │      │      │                  │                     │                  │             │
                │    2.2kΩ    │                  │                     │                  │             │
                │      └┬┐    │                  │                     │                  │             │
                │       ││    │                  │                     │                  │             │
                │      ┌┴┘    │                  │                     │                  │             │
                │      │      │                  │                     │                  │             │
   Pin 10 (RX) ◄┼──────┴──────┼──────────────────┤ LV2          HV2    ├──────────────────┼─── TX       │
   GPIO15       │             │                  │                     │                  │  (DIN 3)    │
                │             │                  │                     │                  │             │
                └─────────────┘                  └─────────────────────┘                  └─────────────┘
                                                
                Pull-up 2.2kΩ vers 3.3V
                sur ligne RX uniquement
                                                
                     DÉTAIL CONNECTEUR DIN 5 BROCHES (vue face soudure)
                                                
                                      ┌───────────┐
                                     /  1     2    \
                                    │      ●       │
                                     \  3     4   /
                                      └────5─────┘
                                                
                                 Pin │ Signal      │ Connexion
                                 ────┼─────────────┼────────────────
                                  1  │ RX (entrée) │ ← HV1 (depuis RPi TX)
                                  2  │ GND         │ → GND commun
                                  3  │ TX (sortie) │ → HV2 (vers RPi RX)
                                  4  │ Device RDY  │ NON CONNECTÉ
                                  5  │ 8-12V       │ ⚠️ NON CONNECTÉ
```

| Raspberry Pi | Level Shifter LV | Level Shifter HV | Minitel DIN |
|--------------|------------------|------------------|-------------|
| 3.3V (Pin 1) | LV | HV | - |
| GND (Pin 6) | GND | GND | Pin 2    |
| TX (Pin 8) | LV1 | HV1 | Pin 1 (RX) |
| RX (Pin 10) + **pull-up 2.2kΩ → 3.3V** | LV2 | HV2 | Pin 3 (TX) |

**⚠️ Attention !** Ne jamais connecter la broche 4 du DIN (8-12V) au Raspberry Pi !

> **Info** : Exemple de level shifter compatible : [Gebildet 3.3V-5V 8 Channels Bi-Directional](https://www.amazon.fr/Gebildet-3-3V-5V-Channels-Converter-Bi-Directional/dp/B07RY15XMJ)

### 2. Shield M-Kiwi (carte fille GPIO, optionnelle)

> Remerciement particulier à **Michael Jouannigot** pour l'élaboration et l'amélioration du PCB.
> Site web : [electrosens.fr](https://www.electrosens.fr)

Ces composants sont gérés par le **client** (`MinitelClient`) via l'accès direct au sysfs GPIO (Linux 6.x, offset `gpiochip512`).

#### Prérequis

```bash
sudo apt-get install raspi-gpio   # pull-up boutons
sudo usermod -aG gpio $USER       # droits sysfs (reconnexion requise)
```

#### OLED SSD1306 (I2C)

Affiche en temps réel : version, serveur:port, URL courante, état des joysticks USB.

| Signal | GPIO  | Pin Header |
|--------|-------|------------|
| SDA    | GPIO2 | Pin 3      |
| SCL    | GPIO3 | Pin 5      |
| VCC    | 3.3V  | Pin 1      |
| GND    | GND   | Pin 9      |

Activation I2C :
```bash
sudo raspi-config   # Interface Options → I2C → Enable
sudo i2cdetect -y 1  # doit afficher 0x3c
```

#### LEDs (4× active-high)

| Index | GPIO BCM | Pin Header |
|-------|----------|------------|
| LED 0 | GPIO 5   | Pin 29     |
| LED 1 | GPIO 6   | Pin 31     |
| LED 2 | GPIO 12  | Pin 32     |
| LED 3 | GPIO 13  | Pin 33     |

Câblage : `GPIO -- [R 330ohm] -- LED(+) -- LED(-) -- GND` (active-high)

#### Boutons-poussoirs (3× active-low, pull-up interne)

| Index | GPIO BCM | Pin Header | Fonction                    |
|-------|----------|------------|-----------------------------|
| BTN 0 | GPIO 20  | Pin 38     | Sélectionne la LED (0→1→2→3)|
| BTN 1 | GPIO 21  | Pin 40     | Éteint la LED sélectionnée  |
| BTN 2 | GPIO 26  | Pin 37     | Allume la LED sélectionnée  |

Câblage : GPIO → [BTN] → GND. Le pull-up interne est activé automatiquement au démarrage via `raspi-gpio set <BCM> ip pu`.

### 3. Configuration Série

Activez l'UART et désactivez la console série :
```bash
# Dans /boot/config.txt
enable_uart=1
dtoverlay=disable-bt

# Désactiver getty
sudo systemctl disable serial-getty@ttyAMA0.service
```

### 4. Auto-négociation de Vitesse

**Important** : Le Minitel démarre toujours en **1200 bauds** à sa mise sous tension.

Le client intègre un système d'**auto-négociation automatique** de la vitesse série :

1. Si le client est configuré en 9600 bauds (ou 4800) dans `config.json` mais que le Minitel est en 1200 bauds, le client détecte la désynchronisation (réception de séries de `0x00`)
2. Le client bascule temporairement en 1200 bauds
3. Il envoie la séquence PRO2 pour forcer le Minitel à la vitesse cible
4. Il reconfigure le port série à la vitesse cible
5. La communication reprend normalement

**Séquences de changement de vitesse (PRO2) :**
| Vitesse | Séquence HEX |
|---------|--------------|
| 1200 bauds | `1B 3A 64 7F` |
| 4800 bauds | `1B 3A 6A 7F` |
| 9600 bauds | `1B 3A 6B 7F` |

> **Note** : Le script `switchTo9600b.sh` permet de forcer manuellement le passage en 9600 bauds si nécessaire :
> ```bash
> chmod +x switchTo9600b.sh
> ./switchTo9600b.sh
> ```

### 5. Désactiver le mode ECHO

Afin de réaliser le bon fonctionnement du client VTML, il est important de désactiver le mode ECHO du Minitel. Pour cela, réaliser la combinaison de touches suivante :

```
[Fnct] + [T]  puis  [E]
```
Si lorsque vous tapé des touches au clavier et que ces dernières apparaissent, alors le mode ECHO (OFF) n'est pas activé. Recommencez la combinaison de touches ci-dessus. 

> **Note** : Cette manipulation doit être effectuée après chaque mise sous tension du Minitel.

### 6. Fichier de Configuration

Le fichier `config.json` contient toute la configuration du serveur et du client :

```json
{
  "server": {
    "port": 8080,
    "defaultCharset": "utf-8"
  },
  "path": {
    "root_path": "./root/",
    "plugins_path": "./plugins/",
    "mmodules_config_path": "./mmodules_config/"
  },
  "client": {
    "serial_port": "/dev/serial0",
    "serial_baud": 9600,
    "serial_databits": "cs7",
    "serial_parity": "parenb",
    "serial_parity_odd": "-parodd",
    "serial_stopbits": "-cstopb",
    "serial_flow_hw": "-crtscts",
    "serial_flow_sw": "-ixon -ixoff",
    "serial_echo": "-echo",
    "serial_icanon": "-icanon",
    "serial_opost": "-opost",
    "serial_chunk_size": 128,
    "serial_chunk_delay_ms": 15,
    "joystick_device_0": "/dev/input/js0",
    "joystick_device_1": "/dev/input/js1",
    "joystick_enabled": true,
    "joystick_mapping_0": {
      "buttons": { "0": "ACTION1", "1": "ACTION2" },
      "axes": { "0+": "RIGHT", "0-": "LEFT", "1+": "DOWN", "1-": "UP" },
      "axis_threshold": 16000
    },
    "joystick_mapping_1": {
      "buttons": { "0": "ACTION1", "1": "ACTION2" },
      "axes": { "0+": "RIGHT", "0-": "LEFT", "1+": "DOWN", "1-": "UP" },
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
| `path.mmodules_config_path` | string | Répertoire des configurations MModules |
| `client.serial_port` | string | Port série du Minitel |
| `client.serial_baud` | int | Vitesse: 1200, 4800 ou 9600 |
| `client.serial_databits` | string | Bits de données: cs5, cs6, cs7, cs8 (défaut: cs7) |
| `client.serial_parity` | string | Parité: parenb (activée) ou -parenb (désactivée) |
| `client.serial_parity_odd` | string | Parité impaire: parodd ou -parodd (défaut: -parodd = paire) |
| `client.serial_stopbits` | string | Stop bits: cstopb (2) ou -cstopb (1, défaut) |
| `client.serial_flow_hw` | string | Flow control matériel: crtscts ou -crtscts (défaut) |
| `client.serial_flow_sw` | string | Flow control logiciel: "ixon ixoff" ou "-ixon -ixoff" (défaut) |
| `client.serial_echo` | string | Echo local: echo ou -echo (défaut) |
| `client.serial_icanon` | string | Mode canonique: icanon ou -icanon (défaut) |
| `client.serial_opost` | string | Post-processing sortie: opost ou -opost (défaut) |
| `client.serial_chunk_size` | int | Taille des blocs d'envoi en bytes (défaut: 128) |
| `client.serial_chunk_delay_ms` | int | Délai entre blocs en ms (défaut: 15, utile pour Minitel Philips) |
| `client.joystick_enabled` | bool | Activer le support joystick USB |
| `client.joystick_device_0` | string | Périphérique joystick joueur 0 |
| `client.joystick_device_1` | string | Périphérique joystick joueur 1 |
| `client.joystick_mapping_0` | object | Mapping boutons/axes joueur 0 |
| `client.joystick_mapping_1` | object | Mapping boutons/axes joueur 1 |

#### Configuration série pour Minitel

La configuration par défaut est **7E1** (7 bits, parité paire, 1 stop bit), compatible avec tous les Minitel :

| Paramètre | Valeur | Description |
|-----------|--------|-------------|
| `serial_databits` | `cs7` | 7 bits de données (protocole Videotex) |
| `serial_parity` | `parenb` | Parité activée |
| `serial_parity_odd` | `-parodd` | Parité paire (Even) |
| `serial_stopbits` | `-cstopb` | 1 stop bit |

#### Compatibilité Minitel Philips

Les Minitel 2 Philips sont plus sensibles au débit que les Alcatel. Si vous rencontrez des problèmes d'affichage (caractères manquants, corruption), ajustez le throttling :

```json
"serial_chunk_size": 64,
"serial_chunk_delay_ms": 20
```

## 🚀 Démarrage Rapide

### Déploiement

Un script de déploiement est fourni pour copier les fichiers nécessaires vers un dossier cible (ex: Raspberry Pi) :

```bash
./deploy.sh
```

Le script copie :
- Le JAR compilé (`dist/`)
- Les pages VTML (`root/`)
- Les librairies (`lib/`)
- Les plugins (`plugins/`)
- Les scripts de démarrage (`*.sh`)
- Le fichier de configuration (`config.json`)

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

# Ou utiliser le script fourni
./startserver.sh
```

### 3. Lancement du Client
```bash
# Connecter le client au serveur
java -jar Minitel.jar localhost 8080

# Ou utiliser le script fourni
./startclient.sh
```

### 4. Test des Joysticks USB

Un utilitaire de diagnostic permet de tester les joysticks et d'identifier les numéros de boutons/axes :

```bash
# Lancer l'utilitaire de test
./test_joystick.sh
```

L'utilitaire propose de tester :
- **0** : Joystick 0 uniquement (`/dev/input/js0`)
- **1** : Joystick 1 uniquement (`/dev/input/js1`)
- **2** : Les deux joysticks simultanément

**Exemple de sortie :**
```
[J0] 🔘 BOUTON 0 PRESSÉ
[J0]    → Config: "0": "ACTION1" ou "ACTION2"
[J0] 🕹️  AXE 0 = 32767 (+) →
[J0]    → Config: "0+": "RIGHT"
```

Utilisez ces informations pour configurer le mapping dans `config.json`.

### 5. Installation en tant que services Linux

Des scripts sont fournis pour installer M-Kiwi en tant que services systemd sur Raspberry Pi :

```bash
# Installation
sudo ./install_services.sh

# Désinstallation
sudo ./uninstall_services.sh
```

Une fois installés, les services peuvent être gérés avec `systemctl` :

```bash
sudo systemctl start mkiwi-server    # Démarrer le serveur
sudo systemctl start mkiwi-client    # Démarrer le client
sudo systemctl status mkiwi-server   # Voir le status
sudo journalctl -u mkiwi-client -f   # Voir les logs
```

> ⚠️ **Non testé** : Ces scripts sont en cours de développement. Voir [docs/SERVICES.md](docs/SERVICES.md) pour plus de détails.

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

### Configuration des MModules avec `readConfig()`

Chaque MModule peut avoir son propre fichier de configuration JSON. Le fichier doit être placé dans le répertoire `mmodules_config/` (défini par `mmodules_config_path` dans `config.json`) et nommé `NomDuModule.json`.

**Exemple de fichier de configuration** (`mmodules_config/MonModule.json`) :
```json
{
  "apiKey": "ma-cle-secrete",
  "maxResults": 50,
  "dataPath": "/chemin/vers/donnees/"
}
```

**Utilisation dans le module :**
```java
public class MonModule extends ModelMModule {
    
    public MonModule(HashMap params, HttpExchange ex, Path docRoot) {
        super(params, ex, docRoot);
    }
    
    @Override
    public String getResponse() {
        // Charger la configuration
        JsonNode config = readConfig();
        
        if (config != null) {
            // Lire les valeurs
            String apiKey = config.get("apiKey").asText();
            int maxResults = config.get("maxResults").asInt();
            String dataPath = config.get("dataPath").asText();
            
            // Utiliser les valeurs...
        }
        
        return "<minitel>...</minitel>";
    }
}
```

**Méthodes disponibles :**

| Méthode | Description |
|---------|-------------|
| `readConfig()` | Charge et retourne le fichier JSON de configuration (`JsonNode`), ou `null` si absent |
| `getConfigPath()` | Retourne le chemin du fichier de configuration (après appel à `readConfig()`) |

### Exemples de MModules

- **`ServerStatus.mod`** : Informations système
- **`QRCodeDemo.mod`** : Générateur de QR codes interactif
- **`WiFiQRDemo.mod`** : Génération de QR Codes WiFi pour connexion automatique
- **`Meteo.mod`** : Données météorologiques via API
- **`News.mod`** : Flux RSS adapté pour Minitel
- **`Chat.mod`** : Système de messagerie simple
- **`IoT.mod`** : Monitoring capteurs domestiques

### Module natif : ServerScore

**ServerScore** est un module natif de M-Kiwi permettant de gérer des tableaux de scores pour les jeux. Il offre une API HTTP simple pour créer, enregistrer et consulter les meilleurs scores.

```
http://localhost:8080/ServerScore.mod?mode=read&gameid={GameId}&fields=score,name
```

Modes disponibles :
- `create` : Créer un nouveau tableau de scores
- `write` : Enregistrer un score
- `read` : Lire tous les scores
- `top1` : Meilleur score
- `top10` : 10ème meilleur score

📖 Documentation complète : [docs/ServerScore.md](docs/ServerScore.md)

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

### Protection Ligne 0

La ligne 0 du Minitel est la ligne de status système. Y écrire peut causer des problèmes d'affichage. Par défaut, l'écriture sur la ligne 0 est **interdite** et le curseur est automatiquement déplacé en ligne 1.

**En JavaScript :**
```javascript
// Autoriser l'écriture sur la ligne 0
enableLineZero(true);

// Vérifier si la ligne 0 est accessible
if (isLineZeroEnabled()) {
  // ...
}

// Réactiver la protection
enableLineZero(false);
```

**En Java :**
```java
GetTeletelCode.enableLineZero(true);  // Autoriser
GetTeletelCode.enableLineZero(false); // Protéger (défaut)
```

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
**Version** : Client 0.7.2, Serveur 0.4  
**Licence** : Open Source
