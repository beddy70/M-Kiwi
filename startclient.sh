#!/bin/bash
cd "$(dirname "$0")"

while true; do
    java -jar Minitel.jar "$@"
    EXIT_CODE=$?
    echo "Client arrêté (code $EXIT_CODE). Redémarrage dans 3 s..."
    sleep 3
done
