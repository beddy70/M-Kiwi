#!/bin/bash
# Script pour télécharger toutes les librairies Java du projet M-Kiwi

echo "=========================================="
echo "  Téléchargement des librairies M-Kiwi"
echo "=========================================="

# Créer le dossier lib s'il n'existe pas
mkdir -p lib
cd lib

# Base URL Maven Central
MAVEN="https://repo1.maven.org/maven2"

# ----------------------------------------
# ZXing - Génération de QR Codes
# ----------------------------------------
echo ""
echo "📦 ZXing (QR Codes)..."
wget -q --show-progress -O zxing-core-3.5.1.jar \
  "$MAVEN/com/google/zxing/core/3.5.1/core-3.5.1.jar"
wget -q --show-progress -O zxing-javase-3.5.1.jar \
  "$MAVEN/com/google/zxing/javase/3.5.1/javase-3.5.1.jar"

# ----------------------------------------
# Jackson - Parsing JSON
# ----------------------------------------
echo ""
echo "📦 Jackson (JSON)..."
wget -q --show-progress -O jackson-core-2.17.2.jar \
  "$MAVEN/com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar"
wget -q --show-progress -O jackson-databind-2.17.2.jar \
  "$MAVEN/com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar"
wget -q --show-progress -O jackson-annotations-2.17.2.jar \
  "$MAVEN/com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar"

# ----------------------------------------
# Jsoup - Parsing HTML/XML
# ----------------------------------------
echo ""
echo "📦 Jsoup (HTML Parser)..."
wget -q --show-progress -O jsoup-1.18.1.jar \
  "$MAVEN/org/jsoup/jsoup/1.18.1/jsoup-1.18.1.jar"

# ----------------------------------------
# JSSC - Communication série (port COM/tty)
# ----------------------------------------
echo ""
echo "📦 JSSC (Serial Port)..."
wget -q --show-progress -O jssc-2.8.0.jar \
  "$MAVEN/org/scream3r/jssc/2.8.0/jssc-2.8.0.jar"

# ----------------------------------------
# Rhino - Moteur JavaScript
# ----------------------------------------
echo ""
echo "📦 Rhino (JavaScript Engine)..."
wget -q --show-progress -O rhino-1.7.14.jar \
  "$MAVEN/org/mozilla/rhino/1.7.14/rhino-1.7.14.jar"

# ----------------------------------------
# Résumé
# ----------------------------------------
echo ""
echo "=========================================="
echo "✅ Téléchargement terminé!"
echo "=========================================="
echo ""
echo "Librairies installées:"
ls -lh *.jar 2>/dev/null | awk '{print "  - " $9 " (" $5 ")"}'
echo ""
echo "Usage: javac -cp 'lib/*:src' ..."
