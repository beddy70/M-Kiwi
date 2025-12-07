# Installation de M-Kiwi en tant que services Linux

> ⚠️ **ATTENTION** : Ces scripts sont en cours de développement et n'ont pas encore été testés en production. Utilisez-les à vos risques et périls.

Ce document explique comment installer le serveur et le client M-Kiwi en tant que services systemd sur Raspberry Pi.

## Table des matières

- [Avantages](#avantages)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Désinstallation](#désinstallation)
- [Commandes de gestion](#commandes-de-gestion)
- [Structure des fichiers](#structure-des-fichiers)
- [Configuration des services](#configuration-des-services)
- [Dépannage](#dépannage)

---

## Avantages

L'installation en tant que services offre plusieurs avantages :

- **Démarrage automatique** : Les services démarrent automatiquement au boot du Raspberry Pi
- **Gestion simplifiée** : Démarrer/arrêter/redémarrer avec `systemctl`
- **Logs centralisés** : Accès aux logs via `journalctl`
- **Redémarrage automatique** : En cas de crash, le service redémarre automatiquement
- **Dépendances** : Le client attend que le serveur soit démarré

---

## Prérequis

- Raspberry Pi avec Raspberry Pi OS
- Java 17+ installé (`sudo apt install openjdk-17-jdk`)
- Projet M-Kiwi compilé (JAR disponible dans `dist/` ou à la racine)
- Accès root (sudo)

---

## Installation

### 1. Copier le projet sur le Raspberry Pi

```bash
# Depuis votre machine de développement
scp -r /chemin/vers/Minitel-Serveur pi@raspberrypi:/home/pi/
```

### 2. Exécuter le script d'installation

```bash
cd /home/pi/Minitel-Serveur
sudo ./install_services.sh
```

Le script effectue les opérations suivantes :

1. Vérifie les prérequis (root, Java, JAR)
2. Crée le répertoire `/opt/mkiwi/`
3. Copie les fichiers nécessaires :
   - `Minitel.jar`
   - `lib/*.jar`
   - `root/` (pages VTML)
   - `plugins/`
   - `mmodules_config/`
   - `config.json`
4. Configure les permissions (utilisateur `pi`)
5. Crée les fichiers de service systemd
6. Active les services au démarrage

### 3. Démarrer les services

```bash
sudo systemctl start mkiwi-server
sudo systemctl start mkiwi-client
```

---

## Désinstallation

```bash
sudo ./uninstall_services.sh
```

Le script :
1. Arrête les services
2. Désactive les services
3. Supprime les fichiers de service
4. Propose de supprimer `/opt/mkiwi/` (optionnel)

---

## Commandes de gestion

### Démarrer les services

```bash
sudo systemctl start mkiwi-server
sudo systemctl start mkiwi-client
```

### Arrêter les services

```bash
sudo systemctl stop mkiwi-client
sudo systemctl stop mkiwi-server
```

### Redémarrer les services

```bash
sudo systemctl restart mkiwi-server
sudo systemctl restart mkiwi-client
```

### Voir le status

```bash
sudo systemctl status mkiwi-server
sudo systemctl status mkiwi-client
```

### Voir les logs

```bash
# Logs du serveur (temps réel)
sudo journalctl -u mkiwi-server -f

# Logs du client (temps réel)
sudo journalctl -u mkiwi-client -f

# Dernières 100 lignes
sudo journalctl -u mkiwi-server -n 100

# Logs depuis le dernier boot
sudo journalctl -u mkiwi-server -b
```

### Activer/Désactiver au démarrage

```bash
# Activer
sudo systemctl enable mkiwi-server
sudo systemctl enable mkiwi-client

# Désactiver
sudo systemctl disable mkiwi-server
sudo systemctl disable mkiwi-client
```

---

## Structure des fichiers

### Après installation

```
/opt/mkiwi/
├── Minitel.jar              # Application principale
├── config.json              # Configuration
├── lib/                     # Librairies Java
│   ├── jackson-*.jar
│   ├── jsoup-*.jar
│   ├── jssc-*.jar
│   ├── rhino-*.jar
│   └── zxing-*.jar
├── root/                    # Pages VTML
│   ├── index.vtml
│   ├── games/
│   └── ...
├── plugins/                 # MModules externes
│   └── mmodules/
└── mmodules_config/         # Configuration des MModules
    └── ServerScore.json

/etc/systemd/system/
├── mkiwi-server.service     # Service serveur
└── mkiwi-client.service     # Service client
```

---

## Configuration des services

### mkiwi-server.service

```ini
[Unit]
Description=M-Kiwi Minitel Server
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/opt/mkiwi
ExecStart=/usr/bin/java -cp "Minitel.jar:lib/*" org.somanybits.minitel.server.StaticFileServer
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### mkiwi-client.service

```ini
[Unit]
Description=M-Kiwi Minitel Client
After=mkiwi-server.service
Requires=mkiwi-server.service

[Service]
Type=simple
User=pi
WorkingDirectory=/opt/mkiwi
ExecStart=/usr/bin/java -jar Minitel.jar localhost 8080
Restart=on-failure
RestartSec=10
SupplementaryGroups=dialout

[Install]
WantedBy=multi-user.target
```

### Personnalisation

Pour modifier les services après installation :

```bash
# Éditer le service
sudo nano /etc/systemd/system/mkiwi-server.service

# Recharger systemd
sudo systemctl daemon-reload

# Redémarrer le service
sudo systemctl restart mkiwi-server
```

---

## Dépannage

### Le service ne démarre pas

```bash
# Vérifier le status détaillé
sudo systemctl status mkiwi-server -l

# Voir les logs d'erreur
sudo journalctl -u mkiwi-server --no-pager
```

### Erreur "Java not found"

```bash
# Vérifier l'installation de Java
java -version

# Installer Java si nécessaire
sudo apt install openjdk-17-jdk
```

### Erreur de permission sur le port série

```bash
# Ajouter l'utilisateur au groupe dialout
sudo usermod -a -G dialout pi

# Redémarrer
sudo reboot
```

### Le client ne se connecte pas au serveur

```bash
# Vérifier que le serveur est démarré
sudo systemctl status mkiwi-server

# Vérifier que le port 8080 est ouvert
netstat -tlnp | grep 8080
```

### Modifier le port ou l'adresse

Éditez `/opt/mkiwi/config.json` puis redémarrez les services :

```bash
sudo nano /opt/mkiwi/config.json
sudo systemctl restart mkiwi-server mkiwi-client
```

---

## Notes

- Les services sont configurés pour redémarrer automatiquement en cas d'échec
- Le client dépend du serveur : il attend que le serveur soit démarré
- L'utilisateur `pi` doit avoir accès au port série (`/dev/serial0`)
- Les logs sont conservés par journald selon la configuration système

---

**Documentation M-Kiwi** 🥝
