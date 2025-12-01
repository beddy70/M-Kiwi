/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.kernel;

/**
 *
 * @author eddy
 */
public class Config {

    public Server server = new Server();
    public PathsCfg path = new PathsCfg();
    public SSLCfg ssl = new SSLCfg();
    public ClientCfg client = new ClientCfg();

    public static class Server {

        public int port = 8080;
        public String defaultCharset = "utf-8";
    }
    
    public static class SSLCfg {
        /** Désactiver la vérification des certificats SSL (pour le développement) */
        public boolean trustAllCertificates = true;
    }

    public static class PathsCfg {

        public String root_path = "./root/";
        public String plugins_path = "./plugins/";
    }
    
    public static class ClientCfg {
        /** Port série pour la connexion Minitel */
        public String serial_port = "/dev/serial0";
        /** Vitesse du port série */
        public int serial_baud = 9600;
        /** Périphérique joystick */
        public String joystick_device = "/dev/input/js0";
        /** Activer le support joystick */
        public boolean joystick_enabled = true;
        /** Mapping des boutons joystick vers actions VTML */
        public JoystickMapping joystick_mapping = new JoystickMapping();
    }
    
    public static class JoystickMapping {
        /** Mapping bouton -> action (ex: "0": "ACTION1") */
        public java.util.Map<String, String> buttons = new java.util.HashMap<>();
        /** Mapping axe/direction -> action (ex: "0+": "RIGHT", "0-": "LEFT") */
        public java.util.Map<String, String> axes = new java.util.HashMap<>();
        /** Seuil pour les axes (0-32767) */
        public int axis_threshold = 16000;
        
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
