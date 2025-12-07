/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
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
import org.somanybits.minitel.components.vtml.VTMLKeypadComponent;
import org.somanybits.minitel.components.vtml.VTMLLayersComponent;
import org.somanybits.minitel.components.vtml.VTMLScriptEngine;
import org.somanybits.minitel.components.vtml.VTMLStatusComponent;
import org.somanybits.minitel.events.CodeSequenceListener;
import org.somanybits.minitel.events.CodeSequenceSentEvent;
import org.somanybits.minitel.events.KeyPressedEvent;
import org.somanybits.minitel.events.KeyPressedListener;
import org.somanybits.minitel.input.JoystickListener;
import org.somanybits.minitel.input.JoystickMapping;
import org.somanybits.minitel.input.JoystickReader;
import org.somanybits.minitel.input.JoystickRumble;
import org.somanybits.minitel.input.JoystickWatcher;
import org.somanybits.minitel.kernel.Config;
import org.somanybits.minitel.kernel.Kernel;

/**
 * Client Minitel principal.
 * <p>
 * Cette classe gère l'interface entre le terminal Minitel physique et le serveur.
 * Elle implémente les listeners pour les événements clavier et les séquences de contrôle.
 * </p>
 * 
 * <h2>Fonctionnalités principales</h2>
 * <ul>
 *   <li>Connexion série au Minitel (1200/4800/9600 bauds)</li>
 *   <li>Gestion des touches fonction (SOMMAIRE, RETOUR, ENVOI, etc.)</li>
 *   <li>Navigation entre pages VTML</li>
 *   <li>Gestion des formulaires et saisie utilisateur</li>
 *   <li>Support des jeux avec layers, sprites et game loop</li>
 *   <li>Support joystick USB pour les jeux</li>
 * </ul>
 * 
 * <h2>Utilisation</h2>
 * <pre>{@code
 * java -jar Minitel.jar localhost 8080
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.5
 * @see MinitelConnection
 * @see MinitelPageReader
 * @see VTMLLayersComponent
 */
public class MinitelClient implements KeyPressedListener, CodeSequenceListener {

    public final static String URL_NEWS = "https://lestranquilles.fr/nos-actualites/";
    private static final String VERSION = "0.7.1";
    private static LogManager logmgr;

//    private Thread rxThread;
//    private volatile boolean running = false;
    MinitelConnection mc;
    MinitelPageReader mtr;

    Teletel t;
    
    // Système de focus pour les formulaires
    private VTMLFormComponent currentForm = null;
    private VTMLStatusComponent currentStatus = null;
    private VTMLLayersComponent currentLayers = null;
    private boolean formHasFocus = false;  // true = focus sur form/inputs, false = focus sur menu
    private boolean layersHasFocus = false; // true = focus sur layers (mode jeu)
    
    // Game loop
    private Thread gameLoopThread = null;
    private volatile boolean gameLoopRunning = false;
    
    // Verrou pour synchroniser l'accès au script engine et au layers
    private final Object scriptLock = new Object();
    
    // Joystick USB - Support 2 joueurs avec plug & play
    private JoystickReader joystick = null;      // Joueur 1
    private JoystickReader joystick2 = null;     // Joueur 2
    private JoystickRumble joystickRumble = null;  // Rumble joueur 1
    private JoystickMapping joystickMapping = new JoystickMapping();   // Mapping joueur 1
    private JoystickMapping joystickMapping2 = new JoystickMapping();  // Mapping joueur 2
    private String[] lastAxisAction = new String[8];   // Pour éviter les répétitions d'axes (joueur 1)
    private String[] lastAxisAction2 = new String[8];  // Pour éviter les répétitions d'axes (joueur 2)
    private JoystickWatcher joystickWatcher = null;    // Surveillance plug & play

    public static void main(String[] args) throws Exception {

        logmgr = Kernel.getInstance().getLogManager();
        logmgr.setPrefix("> ");

        if (args.length == 0) {
            logmgr.addLog("Usage:  Minitel <DOCUMENT_ROOT> [PORT]", LogManager.MSG_TYPE_ERROR);
            System.exit(1);
        }
        String server = args[0];
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 8080;

        logmgr.addLog(LogManager.ANSI_BOLD_GREEN + "M-Kiwi Client  version " + VERSION);
        logmgr.addLog(LogManager.ANSI_WHITE + "Connection to " + server + ":" + port + "/");
        new MinitelClient(server, port);
    }
    // private Page currentpage;

    public MinitelClient(String server, int port) throws IOException, InterruptedException {

        PageManager pmgr = Kernel.getInstance().getPageManager();
        Config cfg = Kernel.getInstance().getConfig();
        
        // Utiliser la config pour le port série
        String serialPort = cfg.client.serial_port;
        int serialBaud = cfg.client.serial_baud;
        System.out.println("📋 Config: Serial " + serialPort + " @ " + serialBaud + " baud");
        
        mc = new MinitelConnection(serialPort, serialBaud);
        
        // Appliquer la configuration stty depuis config.json
        mc.setSttyConfig(
            cfg.client.serial_databits,
            cfg.client.serial_parity,
            cfg.client.serial_parity_odd,
            cfg.client.serial_stopbits,
            cfg.client.serial_flow_hw,
            cfg.client.serial_flow_sw,
            cfg.client.serial_echo,
            cfg.client.serial_icanon,
            cfg.client.serial_opost
        );
        
        // Appliquer le throttling pour compatibilité Minitel Philips
        mc.setThrottling(cfg.client.serial_chunk_size, cfg.client.serial_chunk_delay_ms);
        System.out.println("📋 Throttling: " + cfg.client.serial_chunk_size + " bytes, " + cfg.client.serial_chunk_delay_ms + "ms delay");

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
t.setEcho(false);
        mc.writeBytes(pmgr.getCurrentPage().getData());
        
        // Initialiser le système de focus pour la première page
        updateCurrentForm(pmgr.getCurrentPage());
        
        // Initialiser le joystick USB si disponible
        initJoystick();

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

            //System.out.println("event keypressed=" + event.getKeyCode() + " type=" + event.getType());
            PageManager pmgr = Kernel.getInstance().getPageManager();

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
                    
                    // Si focus sur layers : capturer les touches pour le jeu
                    if (layersHasFocus && currentLayers != null) {
                        // Synchroniser avec le game loop et les actions joystick
                        synchronized (scriptLock) {
                            // 1. Vérifier d'abord les touches directes (sans action de jeu)
                            String directEvent = currentLayers.getDirectKeyEvent(car);
                            if (directEvent != null) {
                                System.out.println("🎮 Touche directe: '" + car + "' -> " + directEvent + "()");
                                try {
                                    VTMLScriptEngine.getInstance().execute(directEvent + "()");
                                    byte[] update = currentLayers.getDifferentialBytes();
                                    mc.writeBytes(update);
                                    // Vérifier si une navigation ou un focus a été demandé
                                    checkPendingNavigation(pmgr);
                                    checkPendingFocus();
                                } catch (Exception e) {
                                    System.err.println("Erreur JS: " + e.getMessage());
                                }
                                break;
                            }
                            
                            // 2. Sinon, vérifier les actions de jeu (UP, DOWN, LEFT, RIGHT, ACTION1, ACTION2)
                            // Trouver le keypad correspondant à cette touche pour avoir le bon joueur
                            VTMLKeypadComponent keypad = currentLayers.getKeypadForKey(car);
                            if (keypad != null && keypad.hasAction()) {
                                int player = keypad.getPlayer();
                                String action = keypad.getAction();
                                String eventFunc = currentLayers.getKeypadEvent(player, action);
                                System.out.println("🎮 Player " + player + " Action: " + action + " -> " + eventFunc);
                                if (eventFunc != null) {
                                    try {
                                        // Appeler la fonction JavaScript
                                        VTMLScriptEngine.getInstance().execute(eventFunc + "()");
                                        // Rafraîchir l'affichage du layers
                                        byte[] update = currentLayers.getDifferentialBytes();
                                        System.out.println("🎮 Update: " + update.length + " bytes");
                                        mc.writeBytes(update);
                                        // Vérifier si une navigation ou un focus a été demandé
                                        checkPendingNavigation(pmgr);
                                        checkPendingFocus();
                                    } catch (Exception e) {
                                        System.err.println("Erreur JS: " + e.getMessage());
                                    }
                                }
                            }
                        }
                        // Touche non reconnue : rien à faire, l'écho est désactivé
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
        // Arrêter le game loop précédent avant de changer de page
        stopGameLoop();
        
        currentForm = page.getForm();
        currentStatus = page.getStatus();
        currentLayers = page.getLayers();
        
        // Si la page a un layers, activer le mode jeu
        if (currentLayers != null) {
            layersHasFocus = true;
            formHasFocus = false;
            System.out.println("🎮 Layers détecté - Mode jeu activé");
            // Note: L'écho doit être désactivé manuellement avec Fnct+T E
            showStatusMessage(">> Jeu <<");
            
            // Démarrer le game loop si configuré
            if (currentLayers.hasGameLoop()) {
                startGameLoop();
            }
            return;
        }
        
        layersHasFocus = false;
        
        // Masquer le curseur pour les pages normales (mode menu)
        try {
            mc.writeBytes(GetTeletelCode.showCursor(false));
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
    
    // ========== GAME LOOP ==========
    
    private void startGameLoop() {
        if (gameLoopRunning || currentLayers == null || !currentLayers.hasGameLoop()) {
            return;
        }
        
        gameLoopRunning = true;
        String tickFunc = currentLayers.getTickFunction();
        int interval = currentLayers.getTickInterval();
        
        System.out.println("🎮 Démarrage game loop: " + tickFunc + "() toutes les " + interval + "ms");
        
        gameLoopThread = new Thread(() -> {
            while (gameLoopRunning && currentLayers != null) {
                try {
                    // Synchroniser avec les actions joystick et les touches clavier
                    synchronized (scriptLock) {
                        // Appeler la fonction JavaScript
                        VTMLScriptEngine.getInstance().execute(tickFunc + "()");
                        
                        // Rafraîchir l'affichage
                        byte[] update = currentLayers.getDifferentialBytes();
                        if (update.length > 0) {
                            synchronized (mc) {
                                mc.writeBytes(update);
                            }
                        }
                        
                        // Gérer le beep si demandé
                        if (currentLayers.consumeBeep()) {
                            synchronized (mc) {
                                mc.writeBytes(GetTeletelCode.beep());
                            }
                        }
                    }
                    
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("❌ Erreur game loop: " + e.getMessage());
                }
            }
        }, "GameLoop");
        
        gameLoopThread.start();
    }
    
    private void stopGameLoop() {
        if (gameLoopRunning) {
            gameLoopRunning = false;
            if (gameLoopThread != null) {
                gameLoopThread.interrupt();
                gameLoopThread = null;
            }
            System.out.println("🎮 Arrêt game loop demandé");
        }
    }
    
    // ========== JOYSTICK USB ==========
    
    private void initJoystick() {
        Config cfg;
        try {
            cfg = Kernel.getInstance().getConfig();
        } catch (IOException e) {
            System.err.println("🎮 Joystick: erreur config - " + e.getMessage());
            return;
        }
        
        // Vérifier si le joystick est activé dans la config
        if (!cfg.client.joystick_enabled) {
            System.out.println("🎮 Joystick: désactivé dans la config");
            return;
        }
        
        String device0 = cfg.client.joystick_device_0;
        String device1 = cfg.client.joystick_device_1;
        
        // Charger les mappings
        joystickMapping.loadFromConfig(cfg.client.joystick_mapping_0);
        joystickMapping2.loadFromConfig(cfg.client.joystick_mapping_1);
        VTMLScriptEngine.getInstance().setVariable("_joystickMapping", joystickMapping);
        VTMLScriptEngine.getInstance().setVariable("_joystickMapping2", joystickMapping2);
        
        // Initialiser les joysticks déjà connectés
        connectJoystick(0, device0);
        connectJoystick(1, device1);
        
        // Démarrer la surveillance plug & play
        joystickWatcher = new JoystickWatcher(device0, device1);
        joystickWatcher.setListener(new JoystickWatcher.JoystickConnectionListener() {
            @Override
            public void onJoystickConnected(int index, String devicePath) {
                connectJoystick(index, devicePath);
            }
            
            @Override
            public void onJoystickDisconnected(int index, String devicePath) {
                disconnectJoystick(index);
            }
        });
        joystickWatcher.start();
    }
    
    /**
     * Connecte un joystick à l'index spécifié.
     */
    private void connectJoystick(int index, String device) {
        if (!JoystickReader.isAvailable(device)) {
            System.out.println("🎮 Joystick " + index + ": " + device + " non disponible");
            return;
        }
        
        // Déconnecter l'ancien si présent
        disconnectJoystick(index);
        
        System.out.println("🎮 Joystick " + index + ": connexion à " + device);
        
        JoystickReader reader = new JoystickReader(device);
        final int playerIndex = index;
        
        reader.addListener(new JoystickListener() {
            @Override
            public void onButton(int button, boolean pressed) {
                System.out.println("🎮 [P" + playerIndex + "] Bouton " + button + " = " + pressed);
                if (!pressed) return;
                handleJoystickButton(playerIndex, button);
            }
            
            @Override
            public void onAxis(int axis, int value) {
                if (Math.abs(value) > 10000) {
                    System.out.println("🎮 [P" + playerIndex + "] Axe " + axis + " = " + value);
                }
                handleJoystickAxis(playerIndex, axis, value);
            }
        });
        
        reader.start();
        
        if (index == 0) {
            joystick = reader;
            // Initialiser le rumble
            joystickRumble = new JoystickRumble(device);
            VTMLScriptEngine.getInstance().setVariable("_joystickRumble", joystickRumble);
        } else {
            joystick2 = reader;
        }
        
        System.out.println("🎮 Joystick " + index + ": connecté ✓");
    }
    
    /**
     * Déconnecte un joystick.
     */
    private void disconnectJoystick(int index) {
        if (index == 0 && joystick != null) {
            joystick.stop();
            joystick = null;
            joystickRumble = null;
            VTMLScriptEngine.getInstance().setVariable("_joystickRumble", null);
            System.out.println("🎮 Joystick 0: déconnecté");
        } else if (index == 1 && joystick2 != null) {
            joystick2.stop();
            joystick2 = null;
            System.out.println("🎮 Joystick 1: déconnecté");
        }
    }
    
    private void handleJoystickButton(int player, int button) {
        if (!layersHasFocus || currentLayers == null) return;
        
        // Utiliser le mapping configurable selon le joueur
        JoystickMapping mapping = (player == 0) ? joystickMapping : joystickMapping2;
        String action = mapping.getButtonAction(button);
        if (action == null) return;
        
        triggerJoystickAction(player, action);
    }
    
    private void handleJoystickAxis(int player, int axis, int value) {
        if (!layersHasFocus || currentLayers == null) return;
        
        String[] lastActions = (player == 0) ? lastAxisAction : lastAxisAction2;
        if (axis < 0 || axis >= lastActions.length) return;
        
        // Utiliser le mapping configurable selon le joueur
        JoystickMapping mapping = (player == 0) ? joystickMapping : joystickMapping2;
        String action = mapping.getAxisAction(axis, value);
        
        // Éviter les répétitions: ne déclencher que si l'action change
        String lastAction = lastActions[axis];
        if (action == null) {
            lastActions[axis] = null;
            return;
        }
        
        if (action.equals(lastAction)) {
            return;
        }
        
        lastActions[axis] = action;
        triggerJoystickAction(player, action);
    }
    
    private void triggerJoystickAction(int player, String action) {
        if (currentLayers == null) return;
        String event = currentLayers.getKeypadEvent(player, action);
        if (event != null) {
            // Exécuter dans un thread séparé pour ne jamais bloquer le thread joystick
            new Thread(() -> {
                // Synchroniser avec le game loop et les touches clavier
                synchronized (scriptLock) {
                    try {
                        VTMLScriptEngine.getInstance().execute(event + "()");
                        refreshLayersDisplay();
                    } catch (Throwable t) {
                        System.err.println("❌ Erreur joystick event: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                    }
                }
            }, "JoystickAction").start();
        }
    }
    
    /**
     * Obtenir le mapping joystick pour modification via JavaScript
     */
    public JoystickMapping getJoystickMapping() {
        return joystickMapping;
    }
    
    private void refreshLayersDisplay() {
        if (currentLayers == null) return;
        try {
            byte[] update = currentLayers.getDifferentialBytes();
            if (update.length > 0) {
                synchronized (mc) {
                    mc.writeBytes(update);
                }
            }
            // Gérer le beep si demandé
            if (currentLayers.consumeBeep()) {
                mc.writeBytes(GetTeletelCode.beep());
            }
        } catch (Throwable t) {
            // Catch Throwable pour ne jamais bloquer (ArrayIndexOutOfBounds, etc.)
            System.err.println("❌ Erreur refresh display: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }
    
    /**
     * Vérifie si une navigation a été demandée via gotoPage() et l'exécute
     */
    private void checkPendingNavigation(PageManager pmgr) {
        String pendingUrl = VTMLScriptEngine.getInstance().consumePendingNavigation();
        if (pendingUrl != null) {
            try {
                System.out.println("🔀 Navigation vers: " + pendingUrl);
                Page newPage = pmgr.navigate(pendingUrl);
                mc.writeBytes(newPage.getData());
                updateCurrentForm(newPage);
            } catch (IOException e) {
                System.err.println("Erreur navigation gotoPage: " + e.getMessage());
            }
        }
    }
    
    /**
     * Vérifie si un focus a été demandé via setFocus() et l'applique
     */
    private void checkPendingFocus() {
        String componentName = VTMLScriptEngine.getInstance().consumePendingFocus();
        if (componentName != null) {
            try {
                PageManager pmgr = Kernel.getInstance().getPageManager();
                Page currentPage = pmgr.getCurrentPage();
                if (currentPage == null) return;
                
                // Chercher le composant par nom
                Object component = currentPage.getComponentByName(componentName);
                if (component == null) {
                    System.err.println("⚠️ Composant non trouvé pour focus: " + componentName);
                    return;
                }
                
                // Gérer le focus selon le type de composant
                if (component instanceof VTMLFormComponent) {
                    VTMLFormComponent form = (VTMLFormComponent) component;
                    currentForm = form;
                    formHasFocus = true;
                    layersHasFocus = false;
                    form.setInputIndex(0);
                    VTMLInputComponent firstInput = form.getCurrentInput();
                    if (firstInput != null) {
                        mc.writeBytes(firstInput.onFocusGained());
                        showStatusMessage(">> " + firstInput.getFocusLabel() + " <<");
                    }
                    System.out.println("🎯 Focus sur form: " + componentName);
                } else if (component instanceof VTMLLayersComponent) {
                    VTMLLayersComponent layers = (VTMLLayersComponent) component;
                    currentLayers = layers;
                    layersHasFocus = true;
                    formHasFocus = false;
                    showStatusMessage(">> Jeu <<");
                    System.out.println("🎯 Focus sur layers: " + componentName);
                } else if (component instanceof VTMLInputComponent) {
                    // Focus sur un input spécifique dans un form
                    VTMLInputComponent input = (VTMLInputComponent) component;
                    // Trouver le form parent
                    if (currentForm != null) {
                        java.util.List<VTMLInputComponent> inputs = currentForm.getFocusableInputs();
                        for (int i = 0; i < inputs.size(); i++) {
                            if (inputs.get(i) == input || 
                                (input.getName() != null && input.getName().equals(inputs.get(i).getName()))) {
                                currentForm.setInputIndex(i);
                                formHasFocus = true;
                                layersHasFocus = false;
                                mc.writeBytes(input.onFocusGained());
                                showStatusMessage(">> " + input.getFocusLabel() + " <<");
                                System.out.println("🎯 Focus sur input: " + componentName);
                                break;
                            }
                        }
                    }
                } else {
                    System.out.println("⚠️ Type de composant non focusable: " + component.getClass().getSimpleName());
                }
            } catch (IOException e) {
                System.err.println("Erreur setFocus: " + e.getMessage());
            }
        }
    }

}
