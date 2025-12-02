/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import org.somanybits.minitel.events.CodeSequenceListener;
import org.somanybits.minitel.events.CodeSequenceSentEvent;
import org.somanybits.minitel.events.KeyPressedEvent;
import org.somanybits.minitel.events.KeyPressedListener;

/**
 * Connexion série vers un terminal Minitel.
 * <p>
 * Cette classe gère la communication bas niveau avec le Minitel via le port série.
 * Elle configure automatiquement le port en mode 7E1 (7 bits, parité paire, 1 stop bit)
 * conformément au protocole Videotex.
 * </p>
 * 
 * <h2>Vitesses supportées</h2>
 * <ul>
 *   <li>{@link #BAUD_1200} - Minitel 1 standard</li>
 *   <li>{@link #BAUD_4800} - Minitel 2 vitesse moyenne</li>
 *   <li>{@link #BAUD_9600} - Minitel 2 haute vitesse</li>
 * </ul>
 * 
 * <h2>Exemple d'utilisation</h2>
 * <pre>{@code
 * MinitelConnection mc = new MinitelConnection("/dev/serial0", MinitelConnection.BAUD_9600);
 * mc.open();
 * mc.addKeyPressedListener(myListener);
 * mc.writeBytes(GetTeletelCode.clearScreen());
 * mc.close();
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see Teletel
 * @see KeyPressedListener
 */
public class MinitelConnection implements Closeable {

    /** Vitesse standard Minitel 1 : 1200 bauds */
    public static final int BAUD_1200 = 1200;
    /** Vitesse moyenne Minitel 2 : 4800 bauds */
    public static final int BAUD_4800 = 4800;
    /** Haute vitesse Minitel 2 : 9600 bauds */
    public static final int BAUD_9600 = 9600;

    private String device = "/dev/serial0";
    private int baud = BAUD_1200;

    private final boolean rtscts = false;
    private final boolean xonxoff = false;

    private RandomAccessFile raf;
    private InputStream in;
    private OutputStream out;
    private Thread rxThread;
    private volatile boolean running = false;
    Process p;

    private ArrayList<KeyPressedListener> listkeypressedlistener = new ArrayList<>();
    private ArrayList<CodeSequenceListener> listcodesequencelistener = new ArrayList<>();
    
    // Détection de désynchronisation de vitesse
    private int consecutiveZeroCount = 0;
    private static final int ZERO_THRESHOLD = 5;  // Nombre de 0 consécutifs pour déclencher la resync

    public MinitelConnection(String device, int baud) {
        this.device = device;
        this.baud = baud;

    }

    public MinitelConnection(String device) {
        this.device = device;
        this.baud = BAUD_1200;
    }

    public void open() throws IOException, InterruptedException {

        configureLine_7E1();

        this.raf = new RandomAccessFile(device, "rw");
        this.in = new BufferedInputStream(new FileInputStream(raf.getFD()));
        this.out = new BufferedOutputStream(new FileOutputStream(raf.getFD()));
        startReader();
    }
    
    /**
     * Force la vitesse du Minitel en envoyant la séquence PRO2 appropriée.
     * Le Minitel doit être en 1200 bauds par défaut, donc on reconfigure
     * temporairement le port série en 1200 bauds pour envoyer la commande.
     * 
     * @throws IOException si erreur de communication
     * @throws InterruptedException si le processus stty est interrompu
     */
    public void forceMinitelSpeed() throws IOException, InterruptedException {
        if (baud == BAUD_1200) {
            System.out.println("⚡ Vitesse déjà à 1200 bauds, pas de changement nécessaire");
            return;
        }
        
        System.out.println("⚡ Détection désynchronisation - Forçage vitesse Minitel vers " + baud + " bauds");
        
        // 1. Arrêter le reader
        running = false;
        if (rxThread != null) {
            rxThread.interrupt();
            try { rxThread.join(500); } catch (InterruptedException e) { }
        }
        
        // 2. Fermer les streams actuels
        try { if (in != null) in.close(); } catch (Exception e) { }
        try { if (out != null) out.close(); } catch (Exception e) { }
        try { if (raf != null) raf.close(); } catch (Exception e) { }
        
        // 3. Reconfigurer en 1200 bauds (vitesse par défaut du Minitel)
        int targetBaud = this.baud;
        this.baud = BAUD_1200;
        configureLine_7E1();
        
        // 4. Rouvrir le port
        this.raf = new RandomAccessFile(device, "rw");
        this.out = new BufferedOutputStream(new FileOutputStream(raf.getFD()));
        
        // 5. Envoyer la séquence de changement de vitesse
        byte[] speedCmd = getSpeedChangeSequence(targetBaud);
        System.out.println("⚡ Envoi séquence vitesse: " + toHex(speedCmd));
        out.write(speedCmd);
        out.flush();
        
        // 6. Attendre que le Minitel traite la commande
        Thread.sleep(200);
        
        // 7. Fermer et reconfigurer à la vitesse cible
        try { out.close(); } catch (Exception e) { }
        try { raf.close(); } catch (Exception e) { }
        
        this.baud = targetBaud;
        configureLine_7E1();
        
        // 8. Rouvrir complètement
        this.raf = new RandomAccessFile(device, "rw");
        this.in = new BufferedInputStream(new FileInputStream(raf.getFD()));
        this.out = new BufferedOutputStream(new FileOutputStream(raf.getFD()));
        
        // 9. Reset du compteur et redémarrage du reader
        consecutiveZeroCount = 0;
        startReader();
        
        System.out.println("⚡ Vitesse Minitel forcée à " + baud + " bauds - OK");
    }
    
    /**
     * Détecte si les données reçues indiquent une désynchronisation de vitesse.
     * Une série de 0x00 consécutifs est typique d'une mauvaise vitesse.
     * 
     * @param frame les données reçues
     * @return true si désynchronisation détectée
     */
    private boolean detectSpeedMismatch(byte[] frame) {
        for (byte b : frame) {
            if (b == 0x00) {
                consecutiveZeroCount++;
                if (consecutiveZeroCount >= ZERO_THRESHOLD) {
                    return true;
                }
            } else {
                // Reset si on reçoit un octet non-nul
                consecutiveZeroCount = 0;
            }
        }
        return false;
    }
    
    /**
     * Retourne la séquence PRO2 pour changer la vitesse du Minitel.
     * Séquences: ESC PRO2 PROG vitesse
     * - 4800 bauds: 1B 3A 6A 7F
     * - 9600 bauds: 1B 3A 6B 7F
     */
    private byte[] getSpeedChangeSequence(int targetBaud) {
        switch (targetBaud) {
            case BAUD_4800:
                return new byte[] { 0x1B, 0x3A, 0x6A, 0x7F };
            case BAUD_9600:
                return new byte[] { 0x1B, 0x3A, 0x6B, 0x7F };
            default:
                // 1200 bauds par défaut (pas de changement)
                return new byte[] { 0x1B, 0x3A, 0x64, 0x7F };
        }
    }

    private void configureLine_7E1() throws IOException, InterruptedException {
        String flowHw = rtscts ? "crtscts" : "-crtscts";
        String flowSw = xonxoff ? "ixon ixoff" : "-ixon -ixoff";

        // IMPORTANT: ne pas utiliser 'raw' (forcerait cs8 -parenb)
        String cmd = String.format(
                "stty -F %s %d cs7 parenb -parodd -cstopb %s %s -echo -icanon -opost",
                device, baud, flowHw, flowSw
        );

        p = new ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start();

        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        int rc = p.waitFor();

        if (rc != 0) {
            throw new InterruptedException("stty failed (" + rc + "): " + out);
        }
    }

    public void writeBytes(byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    public void writeByte(byte data) throws IOException {
        out.write(data);
        out.flush();
    }

    void writeByteP(int n) throws IOException {
        // Pn, Pr, Pc : Voir remarques p.95 et 96
        if (n <= 9) {
            writeByte((byte) (0x30 + n));
        } else {
            writeByte((byte) (0x30 + n / 10));
            writeByte((byte) (0x30 + n % 10));
        }
    }

    public void writeText(String s) throws IOException {
        writeBytes(s.getBytes(StandardCharsets.US_ASCII)); // 7-bit friendly
    }

    private void startReader() throws InterruptedException {
        running = true;
        rxThread = new Thread(() -> {
            byte[] buf = new byte[512];
            try {
                while (running) {
                    int n = in.read(buf);
                    if (n < 0) {
                        break;
                    }
                    if (n == 0) {
                        continue;
                    }

                    byte[] frame = Arrays.copyOf(buf, n);
                    
                    // Détection de désynchronisation: série de 0x00
                    if (detectSpeedMismatch(frame)) {
                        System.out.println("⚠️ Désynchronisation détectée (" + consecutiveZeroCount + " zéros)");
                        try {
                            forceMinitelSpeed();
                        } catch (InterruptedException e) {
                            System.err.println("Erreur resync: " + e.getMessage());
                        }
                        continue;  // Reprendre la lecture après resync
                    }

                    switch (frame.length) {
                        case 1:
                            // Key char pressed

                            this.sendKeyPressedEvent((int) frame[0], KeyPressedEvent.TYPE_KEY_CHAR_EVENT);
//                            System.out.println("1)RX HEX: " + toHex(frame));
//                            System.out.println("1)RX TXT: " + sanitizeAscii(frame));
                            break;
                        case 2:
                            // Menu Key pressed supposed 
                            if (((frame[0] & 0xFF) == 0x13) && (((frame[1] & 0xFF) >= 0x41 && (frame[1] & 0xFF) <= 0x49) || ((frame[1] & 0xFF) == 0x5B))) {
                                this.sendKeyPressedEvent((int) (frame[0] << 8 | frame[1]), KeyPressedEvent.TYPE_KEY_MENU_EVENT);
//                                System.out.println("2) Menu Pressed");
                            } else {
                                this.sendCodeSequenceEvent(frame);
//                                System.out.println("2) Sequence ");
                            }
//                            System.out.println("2)RX HEX: " + toHex(frame));
//                            System.out.println("2)RX TXT: " + sanitizeAscii(frame));
                            break;
                        default:
                            int seq16 = (int) (frame[0] << 8 | frame[1]);
                            if (frame.length == 3) {

                                // check if direction key is pressed (only in 80 Col Mode)
                                if (seq16 == 0x1B5B) {
                                    if (frame[2] == 0x41 || frame[2] == 0x42 || frame[2] == 0x43 || frame[2] == 0x44) {
                                        this.sendKeyPressedEvent((int) frame[2] | 0x80, KeyPressedEvent.TYPE_KEY_DIRECTION_EVENT);
                                        break;
                                    }
                                }

                            }

                            this.sendCodeSequenceEvent(frame);
//                            System.out.println("3)RX HEX: " + toHex(frame));
//                            System.out.println("3)RX TXT: " + sanitizeAscii(frame));
                        // Affichage double: HEX + ASCII (7 bits utiles)

                    }
                }

            } catch (IOException ignore) {
            } finally {
                running = false;

            }
        }, "serial-rx");

        rxThread.setDaemon(true);
        rxThread.start();

    }

    // ---------- Utils ----------
    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static String sanitizeAscii(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            int v = b & 0x7F; // 7 bits utiles
            if (v >= 32 && v < 127) {
                sb.append((char) v);
            } else if (v == 10) {
                sb.append('\n');
            } else if (v == 13) {
                sb.append('\r');
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        running = false;
        try {
            if (rxThread != null) {
                rxThread.interrupt();
            }
        } catch (Exception ignore) {
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ignore) {
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ignore) {
        }
        if (raf != null) {
            raf.close();
        }
    }

    //Events Key Code
    public boolean addKeyPressedEvent(KeyPressedListener kpevt) {
        return this.listkeypressedlistener.add(kpevt);
    }

    public boolean removeKeyPressedEvent(KeyPressedListener kpevt) {
        return this.listkeypressedlistener.remove(kpevt);
    }

    public void sendKeyPressedEvent(int keycode, int type) {
        KeyPressedEvent event = new KeyPressedEvent(keycode, type);

        for (int i = 0; i < this.listkeypressedlistener.size(); i++) {
            this.listkeypressedlistener.get(i).keyPressed(event);
        }
    }

    //Events Code Sequence
    public boolean addCodeSequenceEvent(CodeSequenceListener csevt) {
        return this.listcodesequencelistener.add(csevt);
    }

    public boolean removeCodeSequenceEvent(CodeSequenceListener csevt) {
        return this.listcodesequencelistener.remove(csevt);
    }

    public void sendCodeSequenceEvent(byte[] seqcode) {
        CodeSequenceSentEvent event = new CodeSequenceSentEvent(seqcode);

        for (int i = 0; i < this.listcodesequencelistener.size(); i++) {
            this.listcodesequencelistener.get(i).SequenceSent(event);
        }
    }

}
