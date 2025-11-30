/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package org.somanybits.minitel.client;

//import jssc.SerialPortException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.MinitelConnection;
import org.somanybits.minitel.Teletel;
import org.somanybits.minitel.components.vtml.VTMLFormComponent;
import org.somanybits.minitel.components.vtml.VTMLInputComponent;
import org.somanybits.minitel.components.vtml.VTMLStatusComponent;
import org.somanybits.minitel.events.CodeSequenceListener;
import org.somanybits.minitel.events.CodeSequenceSentEvent;
import org.somanybits.minitel.events.KeyPressedEvent;
import org.somanybits.minitel.events.KeyPressedListener;
import org.somanybits.minitel.kernel.Kernel;

/**
 *
 * @author eddy
 */
public class MinitelClient implements KeyPressedListener, CodeSequenceListener {

    public final static String URL_NEWS = "https://lestranquilles.fr/nos-actualites/";
    private static final String VERSION = "0.1";
    private static LogManager logmgr;

//    private Thread rxThread;
//    private volatile boolean running = false;
    MinitelConnection mc;
    MinitelPageReader mtr;

    Teletel t;
    
    // Système de focus pour les formulaires
    private VTMLFormComponent currentForm = null;
    private VTMLStatusComponent currentStatus = null;
    private boolean formHasFocus = false;  // true = focus sur form/inputs, false = focus sur menu

    public static void main(String[] args) throws Exception {

        logmgr = Kernel.getIntance().getLogManager();
        logmgr.setPrefix("> ");

        if (args.length == 0) {
            logmgr.addLog("Usage:  Minitel <DOCUMENT_ROOT> [PORT]", LogManager.MSG_TYPE_ERROR);
            System.exit(1);
        }
        String server = args[0];
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 8080;

        logmgr.addLog(LogManager.ANSI_BOLD_GREEN + "Minitel Client  version" + VERSION);
        logmgr.addLog(LogManager.ANSI_WHITE + "Connection to " + server + ":" + port + "/");
        new MinitelClient(server, port);
    }
    // private Page currentpage;

    public MinitelClient(String server, int port) throws IOException, InterruptedException {

        PageManager pmgr = Kernel.getIntance().getPageManager();
        mc = new MinitelConnection("/dev/serial0", MinitelConnection.BAUD_9600);

        mc.open();

        // BufferedImage buffImg = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        //inti Events
        mc.addKeyPressedEvent(this);
        mc.addCodeSequenceEvent(this);

        t = new Teletel(mc);
        t.clear();
        t.clearLineZero();

        t.setScreenMode(Teletel.MODE_VIDEOTEXT);
        
   

//        t.setBGColor(Teletel.COLOR_GREEN);
//        t.setTextColor(Teletel.COLOR_WHITE);
//        t.writeString(" ");
        t.setTextColor(Teletel.COLOR_RED);
        t.setCursor(0, 1);
        t.writeString("VTML 1.0");
        t.setCursor(0, 3);
        t.writeString("Client version " + VERSION + " (C) 2025");
        t.setCursor(0, 4);
        t.writeString("Author : Eddy BRIERE");
        t.setCursor(0, 5);
        t.writeString("email  : peassembler@yahoo.fr");
        t.setCursor(0, 7);
        t.setTextColor(Teletel.COLOR_WHITE);
        t.setBlink(true);
        t.writeString("Initialisation ! Waiting please... ");
        t.setBlink(false);



//        byte[] bitmap= {
//            (byte)0b11111111, (byte)0b11111111,
//            (byte)0b11000001, (byte)0b10000011,
//            (byte)0b11111111, (byte)0b11111111,
//            (byte)0b01111111, (byte)0b11111110,
//            (byte)0b00100000, (byte)0b10000100,
//            (byte)0b00010000, (byte)0b11111000
//            
//        };
//        GraphTel gfx = new GraphTel(16, 6);
//        gfx.writeBitmap(bitmap);
//        ImageTo1bpp img = new ImageTo1bpp("images_src/groupe.jpg", 80, 69);
//        
//        GraphTel gfx = new GraphTel(img.getWidth(), img.getHeight());
//        //gfx.setLine(0, 0, img.getWidth(), img.getHeight());
//        gfx.writeBitmap(img.getBitmap());
//        gfx.inverseBitmap();
//        gfx.drawToPage(t, 0, 1);

        // try {
        //     Thread.sleep(1000); // pause de 1000 millisecondes = 1 seconde
        // } catch (InterruptedException e) {
        //     t.setMode(Teletel.MODE_TEXT);
        //     Thread.currentThread().interrupt(); // bonne pratique
        //     System.err.println("Pause interrompue");
        // }

        mtr = new MinitelPageReader(server, port);

        //currentpage = mtr.get("");
        pmgr.navigate("");

        mc.writeBytes(pmgr.getCurrentPage().getData());
        
        // Initialiser le système de focus pour la première page
        updateCurrentForm(pmgr.getCurrentPage());

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
        // Mode interactif: pipe stdin -> série, RX affiché en continu
        System.out.println("Mode interactif: tape du texte (Ctrl-C pour quitter).");
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

        try {
            String keyvalue = null;

            System.out.println("event keypressed=" + event.getKeyCode() + " type=" + event.getType());
            PageManager pmgr = Kernel.getIntance().getPageManager();

            switch (event.getType()) {
                case KeyPressedEvent.TYPE_KEY_MENU_EVENT:
                    switch (event.getKeyCode()) {
                        case KeyPressedEvent.KEY_SOMMAIRE:
                            // Naviguer vers l'URL associée à SOMMAIRE si définie
                            if (pmgr.getCurrentPage() != null && pmgr.getCurrentPage().hasFunctionKey("SOMMAIRE")) {
                                String link = pmgr.getCurrentPage().getFunctionKeyLink("SOMMAIRE");
                                pmgr.navigate(link);
                                mc.writeBytes(pmgr.getCurrentPage().getData());
                                updateCurrentForm(pmgr.getCurrentPage());
                            }
                            keyvalue = "SOMMAIRE";
                            break;
                        case KeyPressedEvent.KEY_ANNULATION:
                            // Effacer tous les champs du formulaire
                            if (currentForm != null && currentForm.hasInputs()) {
                                mc.writeBytes(currentForm.clearAllInputs());
                                // Remettre le focus sur le menu
                                formHasFocus = false;
                                showStatusMessage(">> Menu <<");
                            }
                            keyvalue = "ANNULATION";
                            break;
                        case KeyPressedEvent.KEY_RETOUR:
                            pmgr.back();
                            mc.writeBytes(pmgr.getCurrentPage().getData());
                            updateCurrentForm(pmgr.getCurrentPage());
                            keyvalue = "RETOUR";
                            break;
                        case KeyPressedEvent.KEY_REPETITION:
                            pmgr.reload();
                            mc.writeBytes(pmgr.getCurrentPage().getData());
                            updateCurrentForm(pmgr.getCurrentPage());
                            keyvalue = "REPETITION";
                            break;
                        case KeyPressedEvent.KEY_GUIDE:
                            // Naviguer vers l'URL associée à GUIDE si définie
                            if (pmgr.getCurrentPage() != null && pmgr.getCurrentPage().hasFunctionKey("GUIDE")) {
                                String link = pmgr.getCurrentPage().getFunctionKeyLink("GUIDE");
                                pmgr.navigate(link);
                                mc.writeBytes(pmgr.getCurrentPage().getData());
                                updateCurrentForm(pmgr.getCurrentPage());
                            }
                            keyvalue = "GUIDE";
                            break;
                        case KeyPressedEvent.KEY_CORRECTION:
                            // Supprimer le dernier caractère si on est en mode saisie
                            if (formHasFocus && currentForm != null) {
                                VTMLInputComponent currentInput = currentForm.getCurrentInput();
                                if (currentInput != null) {
                                    mc.writeBytes(currentInput.deleteChar());
                                }
                            }
                            keyvalue = "CORRECTION";
                            break;
                        case KeyPressedEvent.KEY_SUITE:
                            pmgr.forward();
                            mc.writeBytes(pmgr.getCurrentPage().getData());
                            updateCurrentForm(pmgr.getCurrentPage());
                            keyvalue = "SUITE";
                            break;
                        case KeyPressedEvent.KEY_ENVOI:
                            // Soumettre le formulaire si présent
                            if (currentForm != null && currentForm.hasInputs()) {
                                String actionUrl = currentForm.buildActionUrl();
                                System.out.println("📤 ENVOI -> " + actionUrl);
                                pmgr.navigate(actionUrl);
                                mc.writeBytes(pmgr.getCurrentPage().getData());
                                updateCurrentForm(pmgr.getCurrentPage());
                            }
                            keyvalue = "ENVOI";
                            break;
                        case KeyPressedEvent.KEY_TELEPHONE:
                            // Naviguer vers l'URL associée à TELEPHONE si définie
                            if (pmgr.getCurrentPage() != null && pmgr.getCurrentPage().hasFunctionKey("TELEPHONE")) {
                                String link = pmgr.getCurrentPage().getFunctionKeyLink("TELEPHONE");
                                pmgr.navigate(link);
                                mc.writeBytes(pmgr.getCurrentPage().getData());
                                updateCurrentForm(pmgr.getCurrentPage());
                            }
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
                    
                    // Touche Entrée = basculer entre form et menu
                    if (car == 0x0D || car == 0x0A) {
                        if (currentForm != null && currentForm.hasInputs()) {
                            if (formHasFocus) {
                                // On est dans le form : passer à l'input suivant ou sortir
                                VTMLInputComponent currentInput = currentForm.getCurrentInput();
                                int currentIdx = currentForm.getCurrentInputIndex();
                                int totalInputs = currentForm.getFocusableInputs().size();
                                
                                if (currentIdx < totalInputs - 1) {
                                    // Pas encore au dernier input : passer au suivant
                                    if (currentInput != null) {
                                        mc.writeBytes(currentInput.onFocusLost());
                                    }
                                    currentInput = currentForm.nextInput();
                                    if (currentInput != null) {
                                        mc.writeBytes(currentInput.onFocusGained());
                                        showStatusMessage(">> " + currentInput.getFocusLabel() + " <<");
                                    }
                                } else {
                                    // Dernier input : sortir du form, passer au menu
                                    if (currentInput != null) {
                                        mc.writeBytes(currentInput.onFocusLost());
                                    }
                                    formHasFocus = false;
                                    // Afficher indicateur menu dans la zone status
                                    showStatusMessage(">> Menu <<");
                                    System.out.println("🔄 Focus -> MENU");
                                }
                            } else {
                                // On est sur le menu : revenir au premier input du form
                                formHasFocus = true;
                                currentForm.setInputIndex(0);
                                VTMLInputComponent firstInput = currentForm.getCurrentInput();
                                if (firstInput != null) {
                                    mc.writeBytes(firstInput.onFocusGained());
                                    showStatusMessage(">> " + firstInput.getFocusLabel() + " <<");
                                }
                               
                                System.out.println("🔄 Focus -> FORM (input 0)");
                            }
                        }
                        break;
                    }
                    
                    // Si focus sur form : capturer les caractères pour l'input
                    if (currentForm != null && formHasFocus && currentForm.hasInputs()) {
                        VTMLInputComponent currentInput = currentForm.getCurrentInput();
                        if (currentInput != null) {
                            // Backspace / Correction
                            if (car == 0x08 || car == 0x7F) {
                                mc.writeBytes(currentInput.deleteChar());
                                break;
                            } else if (car >= 0x20 && car < 0x7F) {
                                // Caractère imprimable -> saisie dans l'input
                                mc.writeBytes(currentInput.appendChar(car));
                                break;
                            }
                        }
                    }
                    
                    // Focus sur menu (ou pas de form) : comportement navigation
                    Page currentpage = pmgr.getCurrentPage();
                    if (currentpage != null) {
                        if (currentpage.getLink("" + car) != null) {
                            System.out.println("key=" + ((char) car) + " link=" + currentpage.getLink((char) car + ""));
                            try {
                                currentpage = pmgr.navigate(currentpage.getLink((char) car + ""));
                                mc.writeBytes(currentpage.getData());
                                // Mettre à jour le formulaire si la nouvelle page en a un
                                updateCurrentForm(currentpage);
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
        } catch (IOException ex) {
             
            System.getLogger(MinitelClient.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * Met à jour le formulaire courant à partir de la page
     * Cherche un VTMLFormComponent dans la page et initialise le focus
     * Par défaut, le focus commence sur le menu (formHasFocus = false)
     */
    private void updateCurrentForm(Page page) {
        currentForm = page.getForm();
        currentStatus = page.getStatus();
        
        // Par défaut : curseur masqué (mode menu)
        // Note: setEcho désactivé car génère des caractères parasites sur certains Minitel
        try {
            mc.writeBytes(GetTeletelCode.showCursor(false));
            // mc.writeBytes(GetTeletelCode.setEcho(false));
        } catch (IOException e) {
            System.err.println("Erreur init curseur: " + e.getMessage());
        }
        
        if (currentForm != null && currentForm.hasInputs()) {
            // Par défaut, on commence sur le menu
            formHasFocus = false;
            currentForm.setInputIndex(0);
            System.out.println("📋 Formulaire détecté avec " + currentForm.getFocusableInputs().size() + " inputs (focus: MENU)");
            
            // Afficher indicateur menu si zone status définie
            showStatusMessage(">> Menu <<");
        } else {
            currentForm = null;
            formHasFocus = false;
        }
    }
    
    /**
     * Affiche un message dans la zone status si elle est définie
     * Puis repositionne le curseur sur l'input courant si on est en mode form
     */
    private void showStatusMessage(String message) {
        if (currentStatus != null) {
            try {
                mc.writeBytes(currentStatus.showMessage(message));
                
                // Repositionner le curseur sur l'input si on est en mode form
                if (formHasFocus && currentForm != null) {
                    VTMLInputComponent currentInput = currentForm.getCurrentInput();
                    if (currentInput != null) {
                        int cursorX = currentInput.getAbsoluteX() + 
                                     (currentInput.getValue() != null ? currentInput.getValue().length() : 0);
                        int cursorY = currentInput.getAbsoluteY();
                        mc.writeBytes(org.somanybits.minitel.GetTeletelCode.setCursor(cursorX, cursorY));
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur affichage status: " + e.getMessage());
            }
        }
        // Si pas de status défini, on n'affiche rien
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
