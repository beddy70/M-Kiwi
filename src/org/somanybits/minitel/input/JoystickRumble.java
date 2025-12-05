/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.input;

import java.io.File;

/**
 * Contrôle de la vibration (rumble) des manettes USB via Linux Force Feedback.
 * 
 * @author Eddy Briere
 * @version 0.1
 */
public class JoystickRumble {
    
    private String eventDevicePath;
    private boolean supported = false;
    
    public JoystickRumble(String jsDevicePath) {
        this.eventDevicePath = findEventDevice(jsDevicePath);
        this.supported = (eventDevicePath != null && new File(eventDevicePath).canWrite());
        
        if (supported) {
            System.out.println("📳 Rumble: initialisé sur " + eventDevicePath);
        } else {
            System.out.println("📳 Rumble: non disponible pour " + jsDevicePath);
        }
    }
    
    private String findEventDevice(String jsPath) {
        try {
            String jsName = new File(jsPath).getName();
            if (!jsName.startsWith("js")) return null;
            String num = jsName.substring(2);
            
            // Chercher dans /sys/class/input/jsX/device/
            File sysDir = new File("/sys/class/input/" + jsName + "/device");
            if (sysDir.exists()) {
                File parent = sysDir.getCanonicalFile().getParentFile();
                if (parent != null && parent.listFiles() != null) {
                    for (File f : parent.listFiles()) {
                        if (f.getName().startsWith("event")) {
                            String eventPath = "/dev/input/" + f.getName();
                            if (new File(eventPath).exists()) {
                                return eventPath;
                            }
                        }
                    }
                }
            }
            
            // Fallback
            String fallback = "/dev/input/event" + num;
            if (new File(fallback).exists()) return fallback;
            
        } catch (Exception e) {
            System.err.println("📳 Rumble: erreur - " + e.getMessage());
        }
        return null;
    }
    
    public boolean isSupported() {
        return supported;
    }
    
    public void play(int durationMs) {
        play(durationMs, 1.0);
    }
    
    public void play(int durationMs, double intensity) {
        if (!supported) {
            System.out.println("📳 Rumble: non supporté");
            return;
        }
        
        new Thread(() -> {
            try {
                // Essayer d'abord le script Python (plus fiable)
                String scriptPath = System.getProperty("user.dir") + "/scripts/rumble.py";
                File script = new File(scriptPath);
                
                ProcessBuilder pb;
                if (script.exists()) {
                    pb = new ProcessBuilder("python3", scriptPath, 
                        eventDevicePath, 
                        String.valueOf(durationMs), 
                        String.valueOf(intensity));
                } else {
                    // Fallback: utiliser ff-memless via echo
                    // Cette méthode fonctionne sur certains drivers
                    int strong = (int) (Math.min(1.0, Math.max(0.0, intensity)) * 65535);
                    pb = new ProcessBuilder("sh", "-c",
                        String.format(
                            "python3 -c \"" +
                            "import os,struct,time;" +
                            "fd=os.open('%s',os.O_RDWR);" +
                            "e=bytearray(48);" +
                            "struct.pack_into('<Hh',e,0,0x50,-1);" +
                            "struct.pack_into('<HH',e,10,%d,0);" +
                            "struct.pack_into('<HH',e,14,%d,%d);" +
                            "import fcntl;fcntl.ioctl(fd,0x40304580,e);" +
                            "eid=struct.unpack_from('<h',e,2)[0];" +
                            "t=time.time();" +
                            "os.write(fd,struct.pack('<qqHHi',int(t),int((t%%1)*1e6),0x15,eid,1));" +
                            "time.sleep(%f);" +
                            "t=time.time();" +
                            "os.write(fd,struct.pack('<qqHHi',int(t),int((t%%1)*1e6),0x15,eid,0));" +
                            "os.close(fd);" +
                            "print('OK')\"",
                            eventDevicePath, durationMs, strong, strong/2, durationMs/1000.0
                        ));
                }
                
                pb.redirectErrorStream(true);
                Process p = pb.start();
                
                // Lire la sortie
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("📳 Rumble: " + line);
                }
                
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    System.out.println("📳 Rumble: vibration " + durationMs + "ms OK");
                } else {
                    System.err.println("📳 Rumble: échec (code " + exitCode + ")");
                }
                
            } catch (Exception e) {
                System.err.println("📳 Rumble: erreur - " + e.getMessage());
            }
        }, "rumble-thread").start();
    }
    
    public void stop() {
        System.out.println("📳 Rumble: stop");
    }
    
    public static boolean isRumbleAvailable(String jsPath) {
        return new JoystickRumble(jsPath).isSupported();
    }
}
