/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.input;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Surveillance des joysticks USB pour le plug & play.
 * <p>
 * Scanne périodiquement /dev/input/ pour détecter les connexions
 * et déconnexions de manettes.
 * </p>
 * 
 * @author Eddy Briere
 * @version 0.1
 */
public class JoystickWatcher {
    
    public interface JoystickConnectionListener {
        void onJoystickConnected(int index, String devicePath);
        void onJoystickDisconnected(int index, String devicePath);
    }
    
    private final ScheduledExecutorService scheduler;
    private final Map<String, Boolean> deviceStatus = new HashMap<>();
    private final String[] watchedDevices;
    private JoystickConnectionListener listener;
    private boolean running = false;
    
    // Intervalle de scan en secondes
    private static final int SCAN_INTERVAL = 2;
    
    /**
     * Crée un watcher pour les périphériques spécifiés.
     * @param devices Liste des chemins à surveiller (ex: /dev/input/js0, /dev/input/js1)
     */
    public JoystickWatcher(String... devices) {
        this.watchedDevices = devices;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JoystickWatcher");
            t.setDaemon(true);
            return t;
        });
        
        // Initialiser l'état actuel
        for (String device : devices) {
            deviceStatus.put(device, new File(device).exists());
        }
    }
    
    /**
     * Définit le listener pour les événements de connexion.
     */
    public void setListener(JoystickConnectionListener listener) {
        this.listener = listener;
    }
    
    /**
     * Démarre la surveillance.
     */
    public void start() {
        if (running) return;
        running = true;
        
        System.out.println("🔌 JoystickWatcher: démarrage surveillance");
        
        scheduler.scheduleAtFixedRate(this::scanDevices, 
            SCAN_INTERVAL, SCAN_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Arrête la surveillance.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        System.out.println("�� JoystickWatcher: arrêt");
    }
    
    /**
     * Scanne les périphériques pour détecter les changements.
     */
    private void scanDevices() {
        for (int i = 0; i < watchedDevices.length; i++) {
            String device = watchedDevices[i];
            boolean wasConnected = deviceStatus.getOrDefault(device, false);
            boolean isConnected = new File(device).exists() && new File(device).canRead();
            
            if (isConnected != wasConnected) {
                deviceStatus.put(device, isConnected);
                
                if (isConnected) {
                    System.out.println("🔌 Joystick connecté: " + device);
                    if (listener != null) {
                        listener.onJoystickConnected(i, device);
                    }
                } else {
                    System.out.println("🔌 Joystick déconnecté: " + device);
                    if (listener != null) {
                        listener.onJoystickDisconnected(i, device);
                    }
                }
            }
        }
    }
    
    /**
     * Vérifie si un périphérique est actuellement connecté.
     */
    public boolean isConnected(String device) {
        return deviceStatus.getOrDefault(device, false);
    }
    
    /**
     * Vérifie si un périphérique est actuellement connecté par index.
     */
    public boolean isConnected(int index) {
        if (index < 0 || index >= watchedDevices.length) return false;
        return isConnected(watchedDevices[index]);
    }
}
