#!/bin/bash
# Script de désinstallation des services M-Kiwi
# Usage: sudo ./uninstall_services.sh

set -e

# Configuration
INSTALL_DIR="/opt/mkiwi"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}"
echo "=========================================="
echo "  Désinstallation des services M-Kiwi"
echo "=========================================="
echo -e "${NC}"

# Vérifier les droits root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Erreur: Ce script doit être exécuté en root (sudo)${NC}"
    exit 1
fi

echo -e "${YELLOW}1. Arrêt des services...${NC}"
systemctl stop mkiwi-client 2>/dev/null || true
systemctl stop mkiwi-server 2>/dev/null || true

echo -e "${YELLOW}2. Désactivation des services...${NC}"
systemctl disable mkiwi-client 2>/dev/null || true
systemctl disable mkiwi-server 2>/dev/null || true

echo -e "${YELLOW}3. Suppression des fichiers de service...${NC}"
rm -f /etc/systemd/system/mkiwi-server.service
rm -f /etc/systemd/system/mkiwi-client.service

echo -e "${YELLOW}4. Rechargement de systemd...${NC}"
systemctl daemon-reload

echo ""
read -p "Supprimer aussi les fichiers dans $INSTALL_DIR ? (o/N) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Oo]$ ]]; then
    echo -e "${YELLOW}5. Suppression de $INSTALL_DIR...${NC}"
    rm -rf "$INSTALL_DIR"
    echo -e "${GREEN}Fichiers supprimés.${NC}"
else
    echo -e "${YELLOW}Fichiers conservés dans $INSTALL_DIR${NC}"
fi

echo ""
echo -e "${GREEN}=========================================="
echo "  Désinstallation terminée!"
echo "==========================================${NC}"
echo ""
