/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.kernel;

/**
 * Configuration de l'application Minitel-Serveur.
 * <p>
 * Cette classe représente la structure du fichier {@code config.json}.
 * Elle est désérialisée automatiquement par {@link ConfigLoader}.
 * </p>
 * 
 * <h2>Structure du fichier config.json</h2>
 * <pre>{@code
 * {
 *   "server": {
 *     "port": 8080,
 *     "defaultCharset": "utf-8"
 *   },
 *   "path": {
 *     "root_path": "./root/",
 *     "plugins_path": "./plugins/"
 *   },
 *   "client": {
 *     "serial_port": "/dev/serial0",
 *     "serial_baud": 9600,
 *     "joystick_enabled": true
 *   }
 * }
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see ConfigLoader
 * @see Kernel
 */
public class Config {

    public Server server = new Server();
    public PathsCfg path = new PathsCfg();
    public SSLCfg ssl = new SSLCfg();
    public ClientCfg client = new ClientCfg();

    /**
     * Configuration du serveur HTTP.
     */
    public static class Server {
        /** Port d'écoute du serveur HTTP (défaut: 8080) */
        public int port = 8080;
        /** Encodage par défaut des pages (défaut: utf-8) */
        public String defaultCharset = "utf-8";
    }
    
    public static class SSLCfg {
        /** Désactiver la vérification des certificats SSL (pour le développement) */
        public boolean trustAllCertificates = true;
    }

    /**
     * Configuration des chemins de fichiers.
     */
    public static class PathsCfg {
        /** Répertoire racine des pages VTML */
        public String root_path = "./root/";
        /** Répertoire des plugins MModules */
        public String plugins_path = "./plugins/";
    }
    
    /**
     * Configuration du client Minitel.
     * <p>
     * Définit les paramètres de connexion série et le support joystick.
     * Supporte jusqu'à 2 joysticks pour le mode 2 joueurs.
     * </p>
     */
    public static class ClientCfg {
        /** Port série pour la connexion Minitel (ex: /dev/serial0, /dev/ttyUSB0) */
        public String serial_port = "/dev/serial0";
        /** Vitesse du port série en bauds (1200, 4800 ou 9600) */
        public int serial_baud = 9600;
        /** Périphérique joystick Linux pour joueur 0 (ex: /dev/input/js0) */
        public String joystick_device_0 = "/dev/input/js0";
        /** Périphérique joystick Linux pour joueur 1 (ex: /dev/input/js1) */
        public String joystick_device_1 = "/dev/input/js1";
        /** Activer le support joystick USB pour les jeux */
        public boolean joystick_enabled = true;
        /** Mapping des boutons et axes joystick vers actions VTML (joueur 0) */
        public JoystickMapping joystick_mapping_0 = new JoystickMapping();
        /** Mapping des boutons et axes joystick vers actions VTML (joueur 1) */
        public JoystickMapping joystick_mapping_1 = new JoystickMapping();
    }
    
    /**
     * Configuration du mapping joystick.
     * <p>
     * Permet de mapper les boutons et axes du joystick vers les actions
     * de jeu VTML : UP, DOWN, LEFT, RIGHT, ACTION1, ACTION2.
     * </p>
     * 
     * <h3>Format des axes</h3>
     * <ul>
     *   <li>{@code "0+"} - Axe 0, direction positive (droite)</li>
     *   <li>{@code "0-"} - Axe 0, direction négative (gauche)</li>
     *   <li>{@code "1+"} - Axe 1, direction positive (bas)</li>
     *   <li>{@code "1-"} - Axe 1, direction négative (haut)</li>
     * </ul>
     */
    public static class JoystickMapping {
        /** Mapping bouton -> action (clé: numéro du bouton, valeur: action) */
        public java.util.Map<String, String> buttons = new java.util.HashMap<>();
        /** Mapping axe/direction -> action (clé: "axe+/-", valeur: action) */
        public java.util.Map<String, String> axes = new java.util.HashMap<>();
        /** Seuil de déclenchement pour les axes analogiques (0-32767, défaut: 16000) */
        public int axis_threshold = 16000;
        
        /**
         * Crée un mapping par défaut pour un gamepad standard.
         */
        public JoystickMapping() {
            // Mapping par défaut
            buttons.put("0", "ACTION1");  // Bouton A
            buttons.put("1", "ACTION2");  // Bouton B
            buttons.put("2", "ACTION1");  // Bouton X
            buttons.put("3", "ACTION2");  // Bouton Y
            axes.put("0+", "RIGHT");      // Axe X positif
            axes.put("0-", "LEFT");       // Axe X négatif
            axes.put("1+", "DOWN");       // Axe Y positif
            axes.put("1-", "UP");         // Axe Y négatif
        }
    }
}
