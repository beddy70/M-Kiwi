#!/bin/bash
# Génère un certificat auto-signé valable 1 an
# Usage : ./gen_cert.sh [hostname]
# Exemple : ./gen_cert.sh 192.168.0.207

HOST="${1:-localhost}"

openssl req -x509 \
  -newkey rsa:2048 \
  -keyout key.pem \
  -out cert.pem \
  -days 365 \
  -nodes \
  -subj "/CN=${HOST}" \
  -addext "subjectAltName=IP:${HOST},DNS:${HOST}"

echo ""
echo "Certificat généré pour : ${HOST}"
echo "  cert.pem  (certificat public)"
echo "  key.pem   (clé privée)"
echo ""
echo "Lancer le serveur HTTPS :"
echo "  HTTPS=1 ./start.sh"
