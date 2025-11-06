# Minitel

Minitel est un programme en Java permettant de créer un serveur Minitel via un Raspberry Pi. Il dispose de deux programme principaux :

- Client MinitelClient.class
- StaticFIleServer.class

## Configuration requise


## Connexion du RPI vers Minitel

Pour connecter votre Raspberry Pi, veuillez repérer les gpios 5V,GND,TX et RX :

![GPIO Raspberry Pi](http://kiwinas:8418/eddy/Minitel-Serveur/raw/branch/main/images/Raspberry%20pi%203%20UART%20pins.png)

Ensuite connecter le RX --> TX et TX --> RX du Mintel : 



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
