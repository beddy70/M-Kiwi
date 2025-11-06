# Minitel

Minitel est un programme en Java permettant de créer un serveur Minitel via un Raspberry Pi. Il dispose de deux programme principaux :

- Client MinitelClient.class
- StaticFIleServer.class

## Configuration requise


## Connexion du RPI vers Minitel

### Pour connecter votre Raspberry Pi, veuillez repérer les gpios 5V,GND,TX et RX :

<img src="http://kiwinas:8418/eddy/Minitel-Serveur/raw/branch/main/images/Raspberry%20pi%203%20UART%20pins.png" width="400">

### Ensuite connecter le RX --> TX et TX --> RX du Mintel : 

<img src="http://kiwinas:8418/eddy/Minitel-Serveur/raw/branch/main/images/doc_prise_peri-info.jpeg" width="400">

### Branchement 

<img src="http://kiwinas:8418/eddy/Minitel-Serveur/raw/branch/main/images/montage1-din-serial.png" width="400">

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
# Création d'un page VTML

```
<minitel title="acceuil">

	<div class="frame" left="6" top="2" width="30" height="10">
		<row> __  __ _       _ _       _ </row>
		<row>|  \/  (_)_ __ (_) |_ ___| |</row>
		<row>| |\/| | | '_ \| | __/ _ \ |</row>
		<row>| |  | | | | | | | ||  __/ |</row>
		<row>|_|  |_|_|_| |_|_|\__\___|_|</row>
		<br>
		<row>     LE LIEU TRANQUILLE     </row>
	</div>

	<menu name="main" left="4" top="10" width="30" height="10" keytype="number">
		<item link="Acceuil.mod">Actualité</item>
		<item link="Bar/">Bar</item>	
		<item link="Concerts/">concerts</item>
		<item link="Games/">jeux</item>
		<item link="Wifi/">wifi</item>	
	</menu> 

</minitel>
```

<img src="http://kiwinas:8418/eddy/Minitel-Serveur/raw/branch/main/images/mintel_screen.png" width="400">