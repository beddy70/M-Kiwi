package org.somanybits.minitel.input;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecteur de joystick USB via /dev/input/jsX (Linux)
 * Format des événements : 8 octets
 * - 4 octets : timestamp (ms)
 * - 2 octets : valeur (-32767 à 32767 pour axes, 0/1 pour boutons)
 * - 1 octet  : type (1=bouton, 2=axe)
 * - 1 octet  : numéro (index du bouton ou de l'axe)
 */
public class JoystickReader implements Runnable {
    
    public static final int TYPE_BUTTON = 1;
    public static final int TYPE_AXIS = 2;
    public static final int TYPE_INIT = 0x80;
    
    private final String devicePath;
    private volatile boolean running = false;
    private Thread readerThread;
    private final List<JoystickListener> listeners = new ArrayList<>();
    
    // État actuel
    private final int[] axes = new int[8];
    private final boolean[] buttons = new boolean[16];
    
    // Seuil pour considérer un axe comme "pressé"
    private static final int AXIS_THRESHOLD = 16000;
    
    public JoystickReader(String devicePath) {
        this.devicePath = devicePath;
    }
    
    public JoystickReader() {
        this("/dev/input/js0");
    }
    
    public void addListener(JoystickListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(JoystickListener listener) {
        listeners.remove(listener);
    }
    
    public void start() {
        if (running) return;
        running = true;
        readerThread = new Thread(this, "JoystickReader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
    
    public void stop() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void run() {
        System.out.println("🎮 Joystick: tentative ouverture " + devicePath);
        
        try (FileInputStream fis = new FileInputStream(devicePath)) {
            byte[] buffer = new byte[8];
            ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
            
            System.out.println("🎮 Joystick: connecté sur " + devicePath);
            
            while (running) {
                int read = fis.read(buffer);
                if (read != 8) {
                    if (read == -1) break;
                    continue;
                }
                
                bb.rewind();
                int timestamp = bb.getInt();
                short value = bb.getShort();
                byte type = bb.get();
                byte number = bb.get();
                
                // Ignorer les événements d'initialisation
                if ((type & TYPE_INIT) != 0) {
                    type = (byte)(type & ~TYPE_INIT);
                    // Stocker l'état initial
                    if (type == TYPE_AXIS && number < axes.length) {
                        axes[number] = value;
                    } else if (type == TYPE_BUTTON && number < buttons.length) {
                        buttons[number] = (value != 0);
                    }
                    continue;
                }
                
                // Traiter l'événement
                if (type == TYPE_BUTTON && number < buttons.length) {
                    boolean pressed = (value != 0);
                    if (buttons[number] != pressed) {
                        buttons[number] = pressed;
                        fireButtonEvent(number, pressed);
                    }
                } else if (type == TYPE_AXIS && number < axes.length) {
                    int oldValue = axes[number];
                    axes[number] = value;
                    fireAxisEvent(number, value, oldValue);
                }
            }
        } catch (IOException e) {
            System.err.println("🎮 Joystick: erreur - " + e.getMessage());
        }
        
        running = false;
        System.out.println("🎮 Joystick: déconnecté");
    }
    
    private void fireButtonEvent(int button, boolean pressed) {
        for (JoystickListener l : listeners) {
            try {
                l.onButton(button, pressed);
            } catch (Exception e) {
                System.err.println("Erreur listener joystick: " + e.getMessage());
            }
        }
    }
    
    private void fireAxisEvent(int axis, int value, int oldValue) {
        for (JoystickListener l : listeners) {
            try {
                l.onAxis(axis, value);
            } catch (Exception e) {
                System.err.println("Erreur listener joystick: " + e.getMessage());
            }
        }
    }
    
    // Getters pour l'état actuel
    public int getAxis(int index) {
        return (index >= 0 && index < axes.length) ? axes[index] : 0;
    }
    
    public boolean getButton(int index) {
        return (index >= 0 && index < buttons.length) && buttons[index];
    }
    
    /**
     * Vérifie si un joystick est disponible
     */
    public static boolean isAvailable() {
        return isAvailable("/dev/input/js0");
    }
    
    public static boolean isAvailable(String path) {
        java.io.File f = new java.io.File(path);
        return f.exists() && f.canRead();
    }
    
    /**
     * Liste les joysticks disponibles
     */
    public static List<String> listDevices() {
        List<String> devices = new ArrayList<>();
        java.io.File inputDir = new java.io.File("/dev/input");
        if (inputDir.exists() && inputDir.isDirectory()) {
            for (String name : inputDir.list()) {
                if (name.startsWith("js")) {
                    String path = "/dev/input/" + name;
                    if (new java.io.File(path).canRead()) {
                        devices.add(path);
                    }
                }
            }
        }
        return devices;
    }
}
