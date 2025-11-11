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

Nom du fichier : **index.vtml**

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

## Tag minitel
## Tag div
## Tag row
## Tag menu
## Tag item

# Le serveur

Comme un serveur web, le Serveur Minitel retourne des pages demandées par le client Minitel sous forme d'URL. 

    http://localhost:8080/ ou http://localhost:8080/menu.vtml

## Les MModules

Les MModules archives jar contenant une ou plusieurs classes hérités de la classe  :

    org.somanybits.minitel.server.ModelMModule 

Elles sont activées par le client via une URL comme suit : 

    http://localhost:8080/ServerStatus.mod

 Les MModules se trouvent dans le dossier (coté serveur) suivant :

    plugins/mmodules/
    
L'ensemble des fichiers jar présent dans le dossiers **plugins/mmodules/** sont chargés au lancement du serveur.
    
### Exemple d'écriture dans MModule

    package org.somanybits.minitel.server.mmodules;
    
    import com.sun.net.httpserver.HttpExchange;
    import java.nio.file.Path;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.function.Consumer;
    import org.somanybits.minitel.server.ModelMModule;
    import org.somanybits.minitel.server.StaticFileServer;

    /**
     *
     * @author eddy
     */
	    public class ServerStatus extends ModelMModule {
    
        public ServerStatus(HashMap params, HttpExchange ex, Path docRoot) {
            super(params, ex, docRoot);
        }
    
        @Override
        public String getResponse() {
    
            String method = ex.getRequestMethod();
    
            String resp = "<minitel><div>\n";
            resp += "<row>MModules Name=" + this.getClass().getName() + "\n</row>";
            resp += "<row>MModules Version=" + getVersion() + "\n</row>";
            resp += "<row>Server Version=" + StaticFileServer.VERSION + "\n</row>";
            resp += "<row>Local Address=" + ex.getLocalAddress() + "\n</row>";
            resp += "<row>Request Method=" + ex.getRequestMethod() + "\n</row>";
    
            
            if (params != null) {

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    resp += "<row>\tname=" + entry.getKey() + " value=" + entry.getValue() + "\n</row>";
                }
            }
       
    
            resp += "</div></minitel>\n";
            return resp;
        }
    
        @Override
        public String getVersion() {
            return "1.0";
        }
    
        @Override
        public String getContentType() {
            return "Content-Type, text/plain; charset=UTF-8";
        }
    
    }

## Les query dans les MModules

Comme pour une requête de type GET, vous pouvez envoyer des variables sous forme de "query" :

    http://localhost:8080/MyMModole.mod?val1=12@val2=Hello

Du coté du MModule vous pouvez récupérer l'ensembles des variables via une table HashMap :

      if (params != null) {  
	      for (Map.Entry<String, String> entry : params.entrySet()) {
	          resp += "<row>\tname=" + entry.getKey() + " value=" + entry.getValue() + "\n</row>";
	      }   
      }

ou simplement : 

    String value1 = params.get("val1");
    String value2 = params.get("val2");
