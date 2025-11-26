#!/bin/bash
# Script pour télécharger ZXing (bibliothèque QR Code)

echo "Téléchargement de ZXing pour QR Codes scannables..."

# Créer le dossier lib s'il n'existe pas
mkdir -p lib

# Télécharger ZXing Core
wget -O lib/zxing-core-3.5.1.jar \
  "https://repo1.maven.org/maven2/com/google/zxing/core/3.5.1/core-3.5.1.jar"

# Télécharger ZXing JavaSE  
wget -O lib/zxing-javase-3.5.1.jar \
  "https://repo1.maven.org/maven2/com/google/zxing/javase/3.5.1/javase-3.5.1.jar"

echo "✅ ZXing téléchargé dans lib/"
echo "Usage: javac -cp 'lib/*:src' ..."
