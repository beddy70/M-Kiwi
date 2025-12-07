#!/bin/bash
# Script d'installation des services M-Kiwi pour Raspberry Pi
# Usage: sudo ./install_services.sh

set -e

# Configuration
INSTALL_DIR="/opt/mkiwi"
SERVICE_USER="pi"
JAVA_PATH="/usr/bin/java"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}"
echo "=========================================="
echo "  Installation des services M-Kiwi"
echo "=========================================="
echo -e "${NC}"

# Vérifier les droits root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Erreur: Ce script doit être exécuté en root (sudo)${NC}"
    exit 1
fi

# Vérifier que Java est installé
if ! command -v java &> /dev/null; then
    echo -e "${RED}Erreur: Java n'est pas installé${NC}"
    echo "Installez Java avec: sudo apt install openjdk-17-jdk"
    exit 1
fi

# Vérifier que le JAR existe
if [ ! -f "dist/Minitel.jar" ] && [ ! -f "Minitel.jar" ]; then
    echo -e "${RED}Erreur: Minitel.jar non trouvé${NC}"
    echo "Compilez d'abord le projet avec NetBeans ou ant"
    exit 1
fi

# Déterminer le chemin du JAR
if [ -f "dist/Minitel.jar" ]; then
    JAR_SOURCE="dist/Minitel.jar"
else
    JAR_SOURCE="Minitel.jar"
fi

echo -e "${YELLOW}1. Création du répertoire d'installation...${NC}"
mkdir -p "$INSTALL_DIR"
mkdir -p "$INSTALL_DIR/lib"
mkdir -p "$INSTALL_DIR/root"
mkdir -p "$INSTALL_DIR/plugins"
mkdir -p "$INSTALL_DIR/mmodules_config"

echo -e "${YELLOW}2. Copie des fichiers...${NC}"
cp "$JAR_SOURCE" "$INSTALL_DIR/Minitel.jar"
cp -r lib/*.jar "$INSTALL_DIR/lib/" 2>/dev/null || true
cp -r root/* "$INSTALL_DIR/root/" 2>/dev/null || true
cp -r plugins/* "$INSTALL_DIR/plugins/" 2>/dev/null || true
cp -r mmodules_config/* "$INSTALL_DIR/mmodules_config/" 2>/dev/null || true
cp config.json "$INSTALL_DIR/"

echo -e "${YELLOW}3. Configuration des permissions...${NC}"
chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR"
chmod -R 755 "$INSTALL_DIR"

echo -e "${YELLOW}4. Création du service mkiwi-server...${NC}"
cat > /etc/systemd/system/mkiwi-server.service << EOF
[Unit]
Description=M-Kiwi Minitel Server
Documentation=https://github.com/beddy70/M-Kiwi
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=$JAVA_PATH -cp "Minitel.jar:lib/*" org.somanybits.minitel.server.StaticFileServer
Restart=on-failure
RestartSec=5
StandardOutput=journal
StandardError=journal

# Sécurité
NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=$INSTALL_DIR

[Install]
WantedBy=multi-user.target
EOF

echo -e "${YELLOW}5. Création du service mkiwi-client...${NC}"
cat > /etc/systemd/system/mkiwi-client.service << EOF
[Unit]
Description=M-Kiwi Minitel Client
Documentation=https://github.com/beddy70/M-Kiwi
After=mkiwi-server.service
Requires=mkiwi-server.service

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=$JAVA_PATH -jar Minitel.jar localhost 8080
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

# Accès au port série
SupplementaryGroups=dialout

[Install]
WantedBy=multi-user.target
EOF

echo -e "${YELLOW}6. Rechargement de systemd...${NC}"
systemctl daemon-reload

echo -e "${YELLOW}7. Activation des services au démarrage...${NC}"
systemctl enable mkiwi-server
systemctl enable mkiwi-client

echo ""
echo -e "${GREEN}=========================================="
echo "  Installation terminée!"
echo "==========================================${NC}"
echo ""
echo "Fichiers installés dans: $INSTALL_DIR"
echo ""
echo -e "${YELLOW}Commandes utiles:${NC}"
echo ""
echo "  # Démarrer les services"
echo "  sudo systemctl start mkiwi-server"
echo "  sudo systemctl start mkiwi-client"
echo ""
echo "  # Arrêter les services"
echo "  sudo systemctl stop mkiwi-client"
echo "  sudo systemctl stop mkiwi-server"
echo ""
echo "  # Redémarrer"
echo "  sudo systemctl restart mkiwi-server"
echo "  sudo systemctl restart mkiwi-client"
echo ""
echo "  # Voir le status"
echo "  sudo systemctl status mkiwi-server"
echo "  sudo systemctl status mkiwi-client"
echo ""
echo "  # Voir les logs en temps réel"
echo "  sudo journalctl -u mkiwi-server -f"
echo "  sudo journalctl -u mkiwi-client -f"
echo ""
echo "  # Désactiver au démarrage"
echo "  sudo systemctl disable mkiwi-server"
echo "  sudo systemctl disable mkiwi-client"
echo ""
echo -e "${GREEN}Pour démarrer maintenant:${NC}"
echo "  sudo systemctl start mkiwi-server && sudo systemctl start mkiwi-client"
echo ""
