# Minitel

Minitel est un programme en Java permettant de créer un serveur Minitel via un Raspberry Pi. Il dispose de deux programme principaux :

- Client MinitelClient.class
- StaticFIleServer.class

## Configuration requise


## Connexion du RPI vers Minitel



## Utilisation

Avant tout, vous devz démarrer le serveur comme suit : 

```
$ java -cp Minitel.jar org.somanybits.minitel.server.StaticFileServer [Files Path] [PORT]]
```

Allumer votre Minitel est passez en 9600 Baud en appuyant sur les touches suivantes du Minitel : [ Func + P ] +9

Enfin lancer le client :

```
$  java -jar Minitel.jar [Adresse du serveur de fichier] [PORT]
```
### Exemple : 

Serveur :

```
$ java -cp Minitel.jar org.somanybits.minitel.server.StaticFileServer ./root 8080]
```
Serveur :

```
$ java -jar Minitel.jar localhost 8080
```
