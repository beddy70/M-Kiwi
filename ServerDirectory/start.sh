#!/bin/bash
cd "$(dirname "$0")"
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

if [ "${HTTPS}" = "1" ]; then
    if [ ! -f cert.pem ] || [ ! -f key.pem ]; then
        echo "Erreur : cert.pem / key.pem introuvables."
        echo "Générer un certificat avec : ./gen_cert.sh $(hostname -I | awk '{print $1}')"
        exit 1
    fi
    PORT="${PORT:-8443}"
    echo "Démarrage HTTPS sur port ${PORT}..."
    uvicorn main:app --host 0.0.0.0 --port "${PORT}" \
        --ssl-keyfile key.pem --ssl-certfile cert.pem
else
    PORT="${PORT:-8000}"
    echo "Démarrage HTTP sur port ${PORT}..."
    uvicorn main:app --host 0.0.0.0 --port "${PORT}" --reload
fi
