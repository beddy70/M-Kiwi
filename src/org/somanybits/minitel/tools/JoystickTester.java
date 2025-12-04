/*
 * Minitel-Serveur - Utilitaire de test des joysticks
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utilitaire pour tester les joysticks USB.
 * Affiche les événements (boutons et axes) en temps réel.
 * 
 * Usage: java JoystickTester [device1] [device2]
 * Exemple: java JoystickTester /dev/input/js0 /dev/input/js1
 */
public class JoystickTester {
    
    // Structure d'un événement joystick Linux (8 octets)
    // uint32_t time;    // timestamp en millisecondes
    // int16_t value;    // valeur (-32767 à 32767 pour axes, 0/1 pour boutons)
    // uint8_t type;     // type d'événement
    // uint8_t number;   // numéro du bouton ou de l'axe
    
    private static final int JS_EVENT_BUTTON = 0x01;
    private static final int JS_EVENT_AXIS = 0x02;
    private static final int JS_EVENT_INIT = 0x80;
    
    public static void main(String[] args) {
        String device0 = args.length > 0 ? args[0] : "/dev/input/js0";
        String device1 = args.length > 1 ? args[1] : "/dev/input/js1";
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║           TESTEUR DE JOYSTICKS USB                     ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║  Appuyez sur les boutons ou bougez les axes            ║");
        System.out.println("║  Ctrl+C pour quitter                                   ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Lancer un thread pour chaque joystick
        Thread t0 = new Thread(() -> readJoystick(device0, 0), "Joystick-0");
        Thread t1 = new Thread(() -> readJoystick(device1, 1), "Joystick-1");
        
        t0.setDaemon(true);
        t1.setDaemon(true);
        
        t0.start();
        t1.start();
        
        // Attendre indéfiniment (Ctrl+C pour quitter)
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Arrêt...");
        }
    }
    
    private static void readJoystick(String device, int playerNum) {
        System.out.println("[J" + playerNum + "] Ouverture de " + device + "...");
        
        try (FileInputStream fis = new FileInputStream(device)) {
            byte[] buffer = new byte[8];
            ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
            
            System.out.println("[J" + playerNum + "] ✅ Connecté à " + device);
            System.out.println();
            
            while (true) {
                int bytesRead = fis.read(buffer);
                if (bytesRead != 8) {
                    continue;
                }
                
                bb.rewind();
                int time = bb.getInt();        // 4 octets - timestamp
                short value = bb.getShort();   // 2 octets - valeur
                byte type = bb.get();          // 1 octet - type
                byte number = bb.get();        // 1 octet - numéro
                
                // Ignorer les événements d'initialisation
                if ((type & JS_EVENT_INIT) != 0) {
                    continue;
                }
                
                String playerLabel = "[J" + playerNum + "]";
                
                if ((type & JS_EVENT_BUTTON) != 0) {
                    String state = value == 1 ? "PRESSÉ" : "RELÂCHÉ";
                    System.out.println(playerLabel + " 🔘 BOUTON " + number + " " + state);
                    
                    // Suggestion de mapping
                    if (value == 1) {
                        System.out.println(playerLabel + "    → Config: \"" + number + "\": \"ACTION1\" ou \"ACTION2\"");
                    }
                }
                
                if ((type & JS_EVENT_AXIS) != 0) {
                    String direction = "";
                    if (value > 16000) {
                        direction = "(+) →";
                    } else if (value < -16000) {
                        direction = "(-) ←";
                    } else {
                        direction = "(centre)";
                    }
                    
                    System.out.println(playerLabel + " 🕹️  AXE " + number + " = " + value + " " + direction);
                    
                    // Suggestion de mapping pour les axes significatifs
                    if (Math.abs(value) > 16000) {
                        String sign = value > 0 ? "+" : "-";
                        String action = "";
                        if (number == 0) {
                            action = value > 0 ? "RIGHT" : "LEFT";
                        } else if (number == 1) {
                            action = value > 0 ? "DOWN" : "UP";
                        }
                        if (!action.isEmpty()) {
                            System.out.println(playerLabel + "    → Config: \"" + number + sign + "\": \"" + action + "\"");
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("[J" + playerNum + "] ❌ Erreur: " + e.getMessage());
            System.err.println("[J" + playerNum + "] Vérifiez que " + device + " existe et est accessible.");
            System.err.println("[J" + playerNum + "] Essayez: ls -la " + device);
        }
    }
}
