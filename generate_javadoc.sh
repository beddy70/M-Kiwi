#!/bin/bash
# Script de génération de la Javadoc pour Minitel-Serveur
# Nécessite les dépendances dans lib/ : jsoup, jackson-databind, zxing, rhino

CLASSPATH="lib/*"
OUTPUT_DIR="docs/javadoc"
SOURCE_DIR="src"

echo "🔧 Génération de la Javadoc Minitel-Serveur..."

# Créer le répertoire de sortie
mkdir -p "$OUTPUT_DIR"

# Générer la Javadoc
javadoc \
    -d "$OUTPUT_DIR" \
    -sourcepath "$SOURCE_DIR" \
    -classpath "$CLASSPATH" \
    -subpackages org.somanybits \
    -encoding UTF-8 \
    -charset UTF-8 \
    -doctitle "Minitel-Serveur API Documentation" \
    -windowtitle "Minitel-Serveur API" \
    -header "<b>Minitel-Serveur</b>" \
    -footer "Copyright © 2024 Eddy Briere" \
    -author \
    -version \
    -use \
    -Xdoclint:none \
    --allow-script-in-comments

if [ $? -eq 0 ]; then
    echo "✅ Javadoc générée avec succès dans $OUTPUT_DIR/"
    echo "   Ouvrir: file://$(pwd)/$OUTPUT_DIR/index.html"
else
    echo "❌ Erreur lors de la génération de la Javadoc"
    echo ""
    echo "Dépendances requises dans lib/:"
    echo "  - jsoup-*.jar (parsing HTML)"
    echo "  - jackson-databind-*.jar (JSON)"
    echo "  - jackson-core-*.jar (JSON)"
    echo "  - zxing-core-*.jar (QR codes)"
    echo "  - zxing-javase-*.jar (QR codes)"
    echo "  - rhino-*.jar (JavaScript)"
    exit 1
fi
