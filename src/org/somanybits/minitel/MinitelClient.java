/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package org.somanybits.minitel;

//import jssc.SerialPortException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.somanybits.minitel.components.PageManager;
import org.somanybits.minitel.events.CodeSequenceListener;
import org.somanybits.minitel.events.CodeSequenceSentEvent;
import org.somanybits.minitel.events.KeyPressedEvent;
import org.somanybits.minitel.events.KeyPressedListener;
import org.somanybits.minitel.client.MinitelPageReader;
import org.somanybits.minitel.client.Page;

/**
 *
 * @author eddy
 */
public class MinitelClient implements KeyPressedListener, CodeSequenceListener {

    public final static String URL_NEWS = "https://lestranquilles.fr/nos-actualites/";
    private static final String VERSION = "0.1";

    private Thread rxThread;
    private volatile boolean running = false;

    MinitelConnection mc;
    MinitelPageReader mtr;
    Teletel t;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage:  Minitel <DOCUMENT_ROOT> [PORT]");
            System.exit(1);
        }
        String server = args[0];
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 8080;

        System.out.println("Minitel Client  version" + VERSION);
        System.out.println("Read " + server + ":" + port + "/");
        new MinitelClient(server, port);
    }
    private Page currentpage;

    public MinitelClient(String server, int port) throws IOException, InterruptedException {
        PageManager pmgr = PageManager.getInstance();

        mc = new MinitelConnection("/dev/serial0", MinitelConnection.BAUD_9600);

        mc.open();

        BufferedImage buffImg = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);

        //inti Events
        mc.addKeyPressedEvent(this);
        mc.addCodeSequenceEvent(this);

        t = new Teletel(mc);
        t.clear();

        t.setScreenMode(Teletel.MODE_VIDEOTEXT);

//            GraphTel gt = new GraphTel(20, 15);
//            gt.setCircle(10, 7, 7);
//            gt.drawToPage(t, 10, 14);
        t.setTextColor(Teletel.COLOR_RED);
        t.setCursor(6, 4);
        t.writeString(" __  __ _       _ _       _ ");
        t.setCursor(6, 5);
        t.writeString("|  \\/  (_)_ __ (_) |_ ___| |");
        t.setCursor(6, 6);
        t.writeString("| |\\/| | | '_ \\| | __/ _ \\ |");
        t.setCursor(6, 7);
        t.writeString("| |  | | | | | | | ||  __/ |");
        t.setCursor(6, 8);
        t.writeString("|_|  |_|_|_| |_|_|\\__\\___|_|");

        t.setCursor(6, 10);
        t.setTextColor(Teletel.COLOR_WHITE);
        t.writeString("Initialisation ! Waiting please... ");

//            t.setCursor(6, 12);
//            t.setTextColor(Teletel.COLOR_CYAN);
//            t.setBlink(true);
//            t.writeString("Loading news");
//            t.setBlink(false);
//            t.setBGColor(Teletel.PAGE_WIDTH);
        mtr = new MinitelPageReader(server, port);

        currentpage = mtr.get("");
        mc.writeBytes(currentpage.getData());

//            ReadNews rnews = new ReadNews(URL_NEWS); 
//
//            MFrame frame = rnews.readPage();
//            frame.arrange();
//
//            System.out.println(frame.getString());
//            t.clear();
//            t.setScreenMode(Teletel.SCREEN_MODE_80_COL);
//            
//            t.writeString("toto\r\n");
//            t.writeString("titi\r\n");
        t.setEcho(false);
        //t.setCursor(0, 1);
//            t.writeString(frame.getString());
//
//            
//            t.setCursor(6, 12);
//            t.setblink(true);
//            t.setTextColor(Teletel.COLOR_BLUE);
//            t.writeString("HTML Loaded!");
        // Mode interactif: pipe stdin -> série, RX affiché en continu
        System.out.println("Mode interactif: tape du texte (Ctrl-D pour quitter).");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Envoi avec CRLF (classique en série)
                mc.writeText(line + "\r\n");
            }
        }

//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("\nDépannage:\n"
//                    + "- Vérifie que le chemin est bien /dev/serial0 (pas /edv/serial0).\n"
//                    + "- Ajoute ton user au groupe 'dialout' puis reconnecte la session.\n"
//                    + "- Désactive le getty/console série et libère le port.\n"
//                    + "- Assure la config côté cible: 1200 bauds, 7E1, mêmes contrôles de flux.\n"
//                    + "- Sur Raspberry Pi: privilégie le PL011 (ttyAMA0) via dtoverlay=disable-bt.");
//        }
    }

    @Override
    public void keyPressed(KeyPressedEvent event) {

        String keyvalue = null;

        System.out.println("event keypressed=" + event.getKeyCode() + " type=" + event.getType());

        switch (event.getType()) {
            case KeyPressedEvent.TYPE_KEY_MENU_EVENT:
                switch (event.getKeyCode()) {
                    case KeyPressedEvent.KEY_SOMMAIRE:
                        keyvalue = "SOMMAIRE";
                        break;
                    case KeyPressedEvent.KEY_ANNULATION:
                        keyvalue = "ANNULATION";
                        break;
                    case KeyPressedEvent.KEY_RETOUR:
                        keyvalue = "RETOUR";
                        break;
                    case KeyPressedEvent.KEY_REPETITION:
                        keyvalue = "REPETITION";
                        break;
                    case KeyPressedEvent.KEY_GUIDE:
                        keyvalue = "GUIDE";
                        break;
                    case KeyPressedEvent.KEY_CORRECTION:
                        keyvalue = "CORRECTION";
                        break;
                    case KeyPressedEvent.KEY_SUITE:
                        keyvalue = "SUITE";
                        break;
                    case KeyPressedEvent.KEY_ENVOI:
                        keyvalue = "ENVOI";
                        break;
                    case KeyPressedEvent.KEY_TELEPHONE:
                        keyvalue = "TELEPHONE";
                        break;
                    case KeyPressedEvent.KEY_CONNEXION_FIN:
                        keyvalue = "CONNEXION/FIN";
                        break;
                    default:
                        keyvalue = "Menu (" + event.getKeyCode() + ") unkown !!";
                }
                break;
            case KeyPressedEvent.TYPE_KEY_CHAR_EVENT:
                keyvalue = event.getKeyCode() + "";

                char car = (char) event.getKeyCode();
                if (currentpage != null) {
                    if (currentpage.getLink("" + car) != null) {
                        System.out.println("key=" + ((char) car) + " link=" + currentpage.getLink((char) car + ""));
                        try {
                            currentpage = mtr.get(currentpage.getLink((char) car + ""));
                            mc.writeBytes(currentpage.getData());
                        } catch (IOException ex) {
                            System.getLogger(MinitelClient.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                        }

                    }
                }

                break;

            case KeyPressedEvent.TYPE_KEY_DIRECTION_EVENT:
                switch (event.getKeyCode()) {
                    case KeyPressedEvent.KEY_UP:
                        keyvalue = "UP";
                        break;
                    case KeyPressedEvent.KEY_DOWN:
                        keyvalue = "DOWN";
                        break;
                    case KeyPressedEvent.KEY_LEFT:
                        keyvalue = "LEFT";
                        break;
                    case KeyPressedEvent.KEY_RIGHT:
                        keyvalue = "RIGHT";
                        break;
                }
                break;
            default:
                keyvalue = "Key (" + event.getKeyCode() + ") unkwon !!!";
        }

        System.out.println(">" + keyvalue);
    }

    @Override
    public void SequenceSent(CodeSequenceSentEvent event) {
        byte[] sequence = event.getSequenceCode();

        switch (sequence.length) {
            case 2:
                int seq16 = (int) (sequence[0] << 8 | sequence[1]);
                switch (seq16) {
                    case CodeSequenceSentEvent.SEQ_40_COL_MODE:
                        System.out.println("Switch in 40 col mode");
                        break;
                    case CodeSequenceSentEvent.SEQ_80_COL_MODE:
                        System.out.println("Switch in 80 col mode");
                        break;
                }
                break;
            default:

                System.out.println("#" + MinitelConnection.toHex(event.getSequenceCode()));
        }
    }

}
