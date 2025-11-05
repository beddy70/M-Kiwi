/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
 *
 * @author eddy
 */
public class MinitelConnection implements Closeable {

    public static int BAUD_1200 = 1200;
    public static int BAUD_4800 = 4800;
    public static int BAUD_9600 = 9600;

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
