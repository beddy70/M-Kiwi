# HTTPS — Annuaire VTML

## Architecture

```
Internet → https://services.eddy-briere.com:4444
              ↓ port forwarding UDM (4444 → 8000)
           Ubuntu (ServerDirectory) — uvicorn HTTPS :8000
```

L'UDM fait un **port forwarding brut** (pas de terminaison TLS).  
C'est uvicorn qui gère le SSL directement.

---

## Certificat Let's Encrypt

### Première installation

```bash
sudo apt install -y certbot

# Validation par DNS (port 80 non requis)
sudo certbot certonly --manual --preferred-challenges dns -d services.eddy-briere.com
```

Certbot affiche une valeur TXT à ajouter chez votre registrar DNS :

| Type | Sous-domaine | Valeur |
|------|-------------|--------|
| TXT  | `_acme-challenge.services` | `<valeur affichée>` |

Vérifier la propagation avant d'appuyer sur Entrée :  
https://toolbox.googleapps.com/apps/dig/#TXT/_acme-challenge.services.eddy-briere.com

### Copier les certs dans le projet

```bash
sudo cp /etc/letsencrypt/live/services.eddy-briere.com/fullchain.pem ~/ServerDirectory/cert.pem
sudo cp /etc/letsencrypt/live/services.eddy-briere.com/privkey.pem  ~/ServerDirectory/key.pem
sudo chown eddy:eddy ~/ServerDirectory/cert.pem ~/ServerDirectory/key.pem
```

---

## Lancement

```bash
cd ~/ServerDirectory
HTTPS=1 PORT=8000 ./start.sh
```

Sans `HTTPS=1`, le serveur démarre en HTTP sur le port 8000 (mode développement local).

---

## Renouvellement (tous les 90 jours)

```bash
sudo certbot renew

# Recopier les certs renouvelés
sudo cp /etc/letsencrypt/live/services.eddy-briere.com/fullchain.pem ~/ServerDirectory/cert.pem
sudo cp /etc/letsencrypt/live/services.eddy-briere.com/privkey.pem  ~/ServerDirectory/key.pem
sudo chown eddy:eddy ~/ServerDirectory/cert.pem ~/ServerDirectory/key.pem
```

---

## Configuration client M-Kiwi

Dans `config.json` :

```json
"server_directory": "https://services.eddy-briere.com:4444"
```

Le certificat Let's Encrypt étant signé par une CA reconnue, le client Java l'accepte nativement.
