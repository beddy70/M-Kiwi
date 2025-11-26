#!/bin/bash
# Script de compilation avec ZXing

echo "🔧 Compilation avec ZXing..."

# Variables
ZXING_CP="lib/zxing-core-3.5.1.jar:lib/zxing-javase-3.5.1.jar"
SRC_DIR="src"
BUILD_DIR="build"

# Créer le dossier build
mkdir -p $BUILD_DIR

echo "📦 Compilation des classes avec ZXing..."

# Compiler ScannableQRGenerator
javac -cp "$ZXING_CP:$SRC_DIR" -d $BUILD_DIR \
    src/org/somanybits/minitel/components/ScannableQRGenerator.java

if [ $? -eq 0 ]; then
    echo "✅ ScannableQRGenerator compilé"
else
    echo "❌ Erreur compilation ScannableQRGenerator"
    exit 1
fi

# Compiler GraphTel (avec dépendance ScannableQRGenerator)
javac -cp "$ZXING_CP:$SRC_DIR:$BUILD_DIR" -d $BUILD_DIR \
    src/org/somanybits/minitel/components/GraphTel.java

if [ $? -eq 0 ]; then
    echo "✅ GraphTel compilé"
else
    echo "❌ Erreur compilation GraphTel"
    exit 1
fi

# Compiler le reste du projet
echo "🔨 Compilation du projet complet..."
javac -cp "$ZXING_CP:$SRC_DIR:$BUILD_DIR" -d $BUILD_DIR \
    src/org/somanybits/minitel/**/*.java

if [ $? -eq 0 ]; then
    echo "✅ Projet compilé avec succès"
    
    # Créer le JAR avec ZXing inclus
    echo "📦 Création du JAR avec ZXing..."
    
    # Extraire les JAR ZXing dans build
    cd $BUILD_DIR
    jar xf ../lib/zxing-core-3.5.1.jar
    jar xf ../lib/zxing-javase-3.5.1.jar
    cd ..
    
    # Créer le JAR final
    jar cfm Minitel-with-ZXing.jar manifest.mf -C $BUILD_DIR .
    
    echo "✅ JAR créé: Minitel-with-ZXing.jar"
    echo "🚀 Usage: java -jar Minitel-with-ZXing.jar"
    
else
    echo "❌ Erreur compilation projet"
    exit 1
fi
