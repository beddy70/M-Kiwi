#!/bin/bash
# Script de déploiement Minitel-Serveur
# Copie les fichiers nécessaires vers un dossier cible

SCRIPT_NAME=$(basename "$0")
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Déploiement Minitel-Serveur ===${NC}"
echo ""

# Demander le dossier cible
read -p "Dossier cible de déploiement: " TARGET_DIR

# Vérifier que le dossier cible est spécifié
if [ -z "$TARGET_DIR" ]; then
    echo -e "${RED}Erreur: Aucun dossier cible spécifié.${NC}"
    exit 1
fi

# Créer le dossier cible s'il n'existe pas
if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${YELLOW}Création du dossier cible: $TARGET_DIR${NC}"
    mkdir -p "$TARGET_DIR"
    if [ $? -ne 0 ]; then
        echo -e "${RED}Erreur: Impossible de créer le dossier cible.${NC}"
        exit 1
    fi
fi

echo ""
echo "Déploiement de $PROJECT_DIR vers $TARGET_DIR"
echo ""

# 1. Copier le JAR depuis dist/
if [ -d "$PROJECT_DIR/dist" ]; then
    echo -e "${GREEN}[1/5]${NC} Copie du JAR (dist/)..."
    cp -v "$PROJECT_DIR/dist/"*.jar "$TARGET_DIR/" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}  Aucun JAR trouvé dans dist/${NC}"
    fi
else
    echo -e "${YELLOW}[1/5] Dossier dist/ non trouvé${NC}"
fi

# 2. Copier le dossier root/
if [ -d "$PROJECT_DIR/root" ]; then
    echo -e "${GREEN}[2/5]${NC} Copie du dossier root/..."
    cp -rv "$PROJECT_DIR/root" "$TARGET_DIR/"
else
    echo -e "${YELLOW}[2/5] Dossier root/ non trouvé${NC}"
fi

# 3. Copier le dossier lib/
if [ -d "$PROJECT_DIR/lib" ]; then
    echo -e "${GREEN}[3/5]${NC} Copie du dossier lib/..."
    cp -rv "$PROJECT_DIR/lib" "$TARGET_DIR/"
else
    echo -e "${YELLOW}[3/5] Dossier lib/ non trouvé${NC}"
fi

# 4. Copier le dossier plugins/
if [ -d "$PROJECT_DIR/plugins" ]; then
    echo -e "${GREEN}[4/5]${NC} Copie du dossier plugins/..."
    cp -rv "$PROJECT_DIR/plugins" "$TARGET_DIR/"
else
    echo -e "${YELLOW}[4/5] Dossier plugins/ non trouvé${NC}"
fi

# 5. Copier les scripts shell (sauf deploy.sh)
echo -e "${GREEN}[5/5]${NC} Copie des scripts shell..."
for script in "$PROJECT_DIR"/*.sh; do
    if [ -f "$script" ]; then
        script_name=$(basename "$script")
        if [ "$script_name" != "$SCRIPT_NAME" ]; then
            cp -v "$script" "$TARGET_DIR/"
            chmod +x "$TARGET_DIR/$script_name"
        fi
    fi
done

# 6. Copier le fichier de configuration
if [ -f "$PROJECT_DIR/config.json" ]; then
    echo -e "${GREEN}[+]${NC} Copie de config.json..."
    cp -v "$PROJECT_DIR/config.json" "$TARGET_DIR/"
fi

echo ""
echo -e "${GREEN}=== Déploiement terminé ===${NC}"
echo "Fichiers déployés dans: $TARGET_DIR"
echo ""
echo "Pour démarrer:"
echo "  cd $TARGET_DIR"
echo "  ./startserver.sh   # Démarrer le serveur"
echo "  ./startclient.sh   # Démarrer le client"
