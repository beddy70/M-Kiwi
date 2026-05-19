package org.somanybits.minitel.hardware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Gestionnaire de menu OLED pour le Shield M-Kiwi.
 *
 * Affiche un menu arborescent sur l'écran SSD1306 128×64 en police 8×8 px
 * (16 colonnes × 8 lignes). Navigation via 3 boutons GPIO.
 *
 * Ligne 0 : titre du menu courant
 * Ligne 1 : séparateur "----------------"
 * Lignes 2-7 : entrées du menu (curseur ">" devant la sélection)
 */
public class OLEDMenu {

    /** Callbacks implémentés par MinitelClient pour les actions de menu. */
    public interface Actions {
        void onCheckButtons();
        void onCheckLeds();
        void onReboot();
        String getNetworkInfo();
        String getNetworkMac();
        void onRenewDhcp();
        void onNetConfig();
        void onSetEthernet();
        String getCurrentUrl();
        String getSizeHistory();
        void onRestartClient();
        String getServerInfo();
        void onRestartServer();
        String getJoystickInfo();
        void onTestJoystick();
        void onTerminalExit();
        void onSetMinitelMode(int mode);
    }

    // ── Nœud de menu ─────────────────────────────────────────────────────────

    private static class MenuItem {
        final String label;
        final MenuItem[] children;
        final Runnable action;

        MenuItem(String label, MenuItem[] children) {
            this.label    = label;
            this.children = children;
            this.action   = null;
        }

        MenuItem(String label, Runnable action) {
            this.label    = label;
            this.children = null;
            this.action   = action;
        }

        boolean hasChildren() { return children != null && children.length > 0; }
    }

    // ── Champs ────────────────────────────────────────────────────────────────

    private final String  version;
    private final Actions actions;

    private OLEDDisplay display;
    private GPIOLed     leds;
    private GPIOButton  buttons;

    // État de navigation (protégé par synchronized(this))
    private final List<MenuItem[]> menuStack  = new ArrayList<>();
    private final List<Integer>    indexStack = new ArrayList<>();
    private MenuItem[] currentItems;
    private int selectedIndex = 0;
    private int scrollOffset  = 0;

    private static final int MAX_VISIBLE    = 6;
    private static final int ITEMS_ROW_BASE = 2;

    // Écran Network Info
    private boolean inNetworkInfo   = false;
    private String  netIp           = "";
    private String  netMac          = "";
    private volatile int netMacScrollPos = 0;
    private Thread  netMacScrollThread   = null;

    // Réglage luminosité (5 niveaux, index 0-4, défaut 3 = 0xCF)
    private static final int[] BRIGHTNESS_LEVELS = {0x20, 0x60, 0x9F, 0xCF, 0xFF};
    private volatile boolean inBrightnessMenu = false;
    private volatile int     brightnessLevel  = 3;

    // Sélecteur de mode Minitel (4 modes)
    private static final String[] MINITEL_MODE_NAMES  = {"Videotext", "Mixte", "Standard", "Teleinform"};
    private static final int[]    MINITEL_MODE_VALUES = {0x00, 0x01, 0x02, 0x03};
    private volatile boolean inModeSelect   = false;
    private volatile int     selectedModeIdx = 0;

    // Écran About (5 s puis retour auto)
    private volatile boolean inAbout     = false;
    private Thread           aboutThread = null;

    // Test interactif des boutons
    private boolean inButtonTest = false;
    private final boolean[] btnPressed = {false, false, false};

    // Test interactif joysticks
    private volatile boolean inJoyTest  = false;
    private final String[]   joyLabel   = {"", ""};  // dernière touche Joy0, Joy1

    // Réinitialisation par combo BTN1+BTN2 maintenu 3 s
    private volatile boolean terminalModeActive = false;
    private volatile boolean resetPending = false;
    private Thread           resetThread  = null;

    // Test interactif des LEDs (compteur binaire 4 bits)
    private volatile boolean inLedTest     = false;
    private volatile boolean ledTestRunning = false;
    private volatile int     ledTestCounter = 0;
    private final boolean[]  ledTestSaved   = new boolean[4];
    private Thread           ledTestThread  = null;

    // Superposition temporaire (info/résultat d'action)
    private String[] overlayLines = null;
    private long     overlayUntil = 0;

    // Thread de rendu
    private final BlockingQueue<Runnable> renderQueue = new ArrayBlockingQueue<>(8);
    private Thread   renderThread;
    private volatile boolean running = false;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public OLEDMenu(String version, Actions actions) {
        this.version = version;
        this.actions = actions;
    }

    // ── Cycle de vie ─────────────────────────────────────────────────────────

    public boolean init() {
        currentItems = buildMainMenu();

        display = new OLEDDisplay();
        if (!display.init()) display = null;

        leds = new GPIOLed();
        if (!leds.init()) leds = null;

        buttons = new GPIOButton();
        buttons.setListener(new GPIOButton.Listener() {
            @Override public void onPressed(int index)  { handleButtonPressed(index); }
            @Override public void onReleased(int index) { handleButtonReleased(index); }
        });
        buttons.init();

        running = true;

        startRenderThread();
        startHeartbeat();
        scheduleRender();

        return true;
    }

    public void close() {
        running = false;
        if (renderThread != null) renderThread.interrupt();
        if (buttons != null) { buttons.close(); buttons = null; }
        if (leds    != null) { leds.close();    leds    = null; }
        if (display != null) { display.close(); display = null; }
    }

    public void setTerminalModeActive(boolean active) {
        terminalModeActive = active;
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    private void handleButtonPressed(int index) {
        synchronized (this) { if (index < btnPressed.length) btnPressed[index] = true; }

        // Combo BTN1+BTN2 maintenu 3 s → réinitialisation (prioritaire sur tout)
        if ((index == 1 || index == 2) && btnPressed[1] && btnPressed[2]) {
            startResetCountdown();
            return;
        }

        if (terminalModeActive) {
            if (index == 0 && actions != null) actions.onTerminalExit();
            return;
        }
        if (inButtonTest) {
            if (index == 0) exitButtonTest();
            else scheduleRender();
            return;
        }
        if (inLedTest) {
            if (index == 0) exitLedTest();
            return;
        }
        if (inJoyTest) {
            if (index == 0) exitJoyTest();
            return;
        }
        if (inBrightnessMenu) {
            if (index == 0) exitBrightnessMenu();
            else if (index == 1) adjustBrightness(+1);
            else if (index == 2) adjustBrightness(-1);
            return;
        }
        if (inModeSelect) {
            if (index == 0) exitModeSelect();
            else if (index == 1) adjustMinitelMode(-1);
            else if (index == 2) adjustMinitelMode(+1);
            return;
        }
        if (inNetworkInfo) {
            if (index == 0) exitNetworkInfo();
            return;
        }
        if (inAbout) {
            exitAbout();
            return;
        }
        switch (index) {
            case 0 -> enter();
            case 1 -> moveUp();
            case 2 -> moveDown();
        }
    }

    private void handleButtonReleased(int index) {
        synchronized (this) { if (index < btnPressed.length) btnPressed[index] = false; }
        if (index == 1 || index == 2) cancelResetCountdown();
        if (inButtonTest) scheduleRender();
    }

    private void startResetCountdown() {
        if (resetPending) return;
        resetPending = true;
        System.out.println("OLEDMenu: combo UP+DOWN détecté — redémarrage client dans 3 s");
        resetThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                if (resetPending && btnPressed[1] && btnPressed[2]) {
                    System.out.println("OLEDMenu: redémarrage client déclenché");
                    showOverlay("M-Kiwi", "restarting...");
                    Thread.sleep(800);
                    if (actions != null) actions.onRestartClient();
                }
            } catch (InterruptedException ignored) {}
            resetPending = false;
        }, "menu-reset");
        resetThread.setDaemon(true);
        resetThread.start();
    }

    private void cancelResetCountdown() {
        if (!resetPending) return;
        resetPending = false;
        if (resetThread != null) { resetThread.interrupt(); resetThread = null; }
    }



    private synchronized void enterButtonTest() {
        btnPressed[0] = false;
        btnPressed[1] = false;
        btnPressed[2] = false;
        inButtonTest = true;
        scheduleRender();
    }

    private synchronized void exitButtonTest() {
        inButtonTest = false;
        scheduleRender();
    }

    private void enterLedTest() {
        if (leds == null) { showOverlay("LEDs", "non disponibles"); return; }
        for (int i = 0; i < 4; i++) ledTestSaved[i] = leds.getState(i);
        ledTestCounter = 0;
        inLedTest      = true;
        ledTestRunning = true;
        scheduleRender();

        ledTestThread = new Thread(() -> {
            int count = 0;
            while (ledTestRunning) {
                final int c = count;
                for (int i = 0; i < 4; i++) leds.set(i, (c & (1 << i)) != 0);
                ledTestCounter = c;
                scheduleRender();
                count = (count + 1) & 0x0F;
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            }
        }, "led-test");
        ledTestThread.setDaemon(true);
        ledTestThread.start();
    }

    private void exitLedTest() {
        ledTestRunning = false;
        if (ledTestThread != null) {
            ledTestThread.interrupt();
            try { ledTestThread.join(300); } catch (InterruptedException ignored) {}
            ledTestThread = null;
        }
        inLedTest = false;
        if (leds != null) {
            for (int i = 0; i < 4; i++) leds.set(i, ledTestSaved[i]);
        }
        scheduleRender();
    }

    private synchronized void enterJoyTest() {
        joyLabel[0] = "";
        joyLabel[1] = "";
        inJoyTest = true;
        scheduleRender();
    }

    private synchronized void exitJoyTest() {
        inJoyTest = false;
        scheduleRender();
    }

    private synchronized void enterBrightnessMenu() {
        inBrightnessMenu = true;
        scheduleRender();
    }

    private synchronized void exitBrightnessMenu() {
        inBrightnessMenu = false;
        scheduleRender();
    }

    private synchronized void enterModeSelect() {
        inModeSelect = true;
        scheduleRender();
    }

    private synchronized void exitModeSelect() {
        inModeSelect = false;
        scheduleRender();
    }

    private void adjustMinitelMode(int delta) {
        final int newIdx;
        synchronized (this) {
            selectedModeIdx = Math.floorMod(selectedModeIdx + delta, MINITEL_MODE_NAMES.length);
            newIdx = selectedModeIdx;
        }
        if (actions != null) actions.onSetMinitelMode(MINITEL_MODE_VALUES[newIdx]);
        scheduleRender();
    }

    private void adjustBrightness(int delta) {
        final int newLevel;
        synchronized (this) {
            brightnessLevel = Math.max(0, Math.min(BRIGHTNESS_LEVELS.length - 1, brightnessLevel + delta));
            newLevel = brightnessLevel;
        }
        renderQueue.offer(() -> {
            if (display != null) display.setContrast(BRIGHTNESS_LEVELS[newLevel]);
            doRender();
        });
    }

    private void enterNetworkInfo() {
        String ip  = "N/A";
        String mac = "N/A";
        if (actions != null) {
            String raw = actions.getNetworkInfo();
            ip = (raw != null && !raw.isBlank()) ? raw.trim().split("\\s+")[0] : "N/A";
            mac = actions.getNetworkMac();
        }
        synchronized (this) {
            netIp           = ip;
            netMac          = mac;
            netMacScrollPos = 0;
            inNetworkInfo   = true;
        }
        startMacScroll(mac);
        scheduleRender();
    }

    private void exitNetworkInfo() {
        synchronized (this) { inNetworkInfo = false; }
        stopMacScroll();
        scheduleRender();
    }

    private void startMacScroll(String mac) {
        stopMacScroll();
        int maxScroll = Math.max(0, mac.length() - OLEDDisplay.CHARS_PER_LINE_8X8);
        if (maxScroll <= 0) return;
        netMacScrollThread = new Thread(() -> {
            int pos = 0, dir = 1;
            try {
                while (inNetworkInfo && !Thread.currentThread().isInterrupted()) {
                    synchronized (OLEDMenu.this) { netMacScrollPos = pos; }
                    scheduleRender();
                    Thread.sleep((pos == 0 || pos == maxScroll) ? 1500 : 350);
                    pos += dir;
                    if (pos >= maxScroll) { pos = maxScroll; dir = -1; }
                    else if (pos <= 0)   { pos = 0;          dir =  1; }
                }
            } catch (InterruptedException ignored) {}
        }, "mac-scroll");
        netMacScrollThread.setDaemon(true);
        netMacScrollThread.start();
    }

    private void stopMacScroll() {
        if (netMacScrollThread != null) {
            netMacScrollThread.interrupt();
            netMacScrollThread = null;
        }
    }

    private void enterAbout() {
        synchronized (this) { inAbout = true; }
        scheduleRender();
        aboutThread = new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            synchronized (OLEDMenu.this) { inAbout = false; }
            scheduleRender();
        }, "about-timer");
        aboutThread.setDaemon(true);
        aboutThread.start();
    }

    private void exitAbout() {
        synchronized (this) { inAbout = false; }
        if (aboutThread != null) { aboutThread.interrupt(); aboutThread = null; }
        scheduleRender();
    }

    /** Appelé par MinitelClient pour tout événement joystick (bouton ou axe mappé). */
    public void onJoystickEvent(int player, String label) {
        if (!inJoyTest || player < 0 || player >= joyLabel.length) return;
        synchronized (this) {
            if (label.equals(joyLabel[player])) return;
            joyLabel[player] = label;
        }
        scheduleRender();
    }

    private synchronized void enter() {
        MenuItem item = currentItems[selectedIndex];
        if (item.hasChildren()) {
            menuStack.add(currentItems);
            indexStack.add(selectedIndex);
            currentItems  = item.children;
            selectedIndex = 0;
            scrollOffset  = 0;
            scheduleRender();
        } else if (item.action != null) {
            Runnable act = item.action;
            new Thread(act, "menu-action").start();
        }
    }

    private synchronized void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        }
        scheduleRender();
    }

    private synchronized void moveDown() {
        if (selectedIndex < currentItems.length - 1) {
            selectedIndex++;
            if (selectedIndex >= scrollOffset + MAX_VISIBLE)
                scrollOffset = selectedIndex - MAX_VISIBLE + 1;
        }
        scheduleRender();
    }

    private synchronized void goBack() {
        if (!menuStack.isEmpty()) {
            currentItems  = menuStack.remove(menuStack.size() - 1);
            selectedIndex = indexStack.remove(indexStack.size() - 1);
            scrollOffset  = Math.max(0, selectedIndex - MAX_VISIBLE + 1);
        }
        scheduleRender();
    }

    // ── Superposition temporaire ──────────────────────────────────────────────

    private synchronized void showOverlay(String... lines) {
        overlayLines = lines;
        overlayUntil = System.currentTimeMillis() + 3000;
        scheduleRender();
        new Thread(() -> {
            try { Thread.sleep(3100); } catch (InterruptedException ignored) {}
            synchronized (OLEDMenu.this) { overlayLines = null; }
            scheduleRender();
        }, "overlay-clear").start();
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    private void scheduleRender() {
        renderQueue.offer(this::doRender);
    }

    private void startRenderThread() {
        renderThread = new Thread(() -> {
            while (running) {
                try {
                    Runnable task = renderQueue.take();
                    // Vider les doublons accumulés : on ne garde que le dernier
                    Runnable last = task;
                    Runnable next;
                    while ((next = renderQueue.poll()) != null) last = next;
                    last.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "oled-menu-render");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void startHeartbeat() {
        if (leds == null) return;
        Thread hb = new Thread(() -> {
            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    if (!inLedTest && leds != null) {
                        leds.set(3, true);
                        Thread.sleep(100);
                        if (!inLedTest && leds != null) leds.set(3, false);
                    }
                    Thread.sleep(900);
                }
            } catch (InterruptedException ignored) {}
        }, "menu-heartbeat");
        hb.setDaemon(true);
        hb.start();
    }

    private void doRender() {
        if (display == null) return;

        // Capture de l'état sous verrou (I2C hors verrou)
        final MenuItem[] items;
        final int selIdx;
        final int scrOff;
        final String title;
        final String[] overlay;
        final long overlayEnd;
        final boolean btnTest;
        final boolean p1, p2;
        final boolean ledTest;
        final int     ledCount;
        final boolean joyTest;
        final String  joy0, joy1;
        final boolean brightMenu;
        final int     brightLevel;
        final boolean aboutScreen;
        final boolean netInfo;
        final String  nIp, nMac;
        final int     nMacScroll;

        synchronized (this) {
            btnTest     = inButtonTest;
            p1          = btnPressed[1];
            p2          = btnPressed[2];
            ledTest     = inLedTest;
            ledCount    = ledTestCounter;
            joyTest     = inJoyTest;
            joy0        = joyLabel[0];
            joy1        = joyLabel[1];
            brightMenu  = inBrightnessMenu;
            brightLevel = brightnessLevel;
            aboutScreen = inAbout;
            netInfo     = inNetworkInfo;
            nIp         = netIp;
            nMac        = netMac;
            nMacScroll  = netMacScrollPos;
            items      = currentItems;
            selIdx     = selectedIndex;
            scrOff     = scrollOffset;
            overlay    = overlayLines;
            overlayEnd = overlayUntil;
            if (menuStack.isEmpty()) {
                title = "M-Kiwi v" + version;
            } else {
                int    lastIdx    = indexStack.get(indexStack.size() - 1);
                MenuItem[] parent = menuStack.get(menuStack.size() - 1);
                title = parent[lastIdx].label;
            }
        }

        display.clear();

        // Écran test LEDs (compteur binaire 4 bits)
        if (ledTest) {
            String binary = String.format("%4s", Integer.toBinaryString(ledCount)).replace(' ', '0');
            display.drawText8x8(fit("LED Test"), 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8(fit("OK->To Exit"), 0, 2);
            display.drawText8x8(fit("cnt:" + String.format("%2d", ledCount) + " b:" + binary), 0, 3);
            display.drawText8x8("                ", 0, 4);
            display.drawText8x8("                ", 0, 5);
            display.drawText8x8("                ", 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Écran test boutons
        if (btnTest) {
            display.drawText8x8(fit("Button Test"), 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8(fit("OK->To Exit"), 0, 2);
            display.drawText8x8(fit("UP->" + (p1 ? "pressed " : "released")), 0, 3);
            display.drawText8x8(fit("DOWN->" + (p2 ? "pressed " : "released")), 0, 4);
            display.drawText8x8("                ", 0, 5);
            display.drawText8x8("                ", 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Écran test joysticks
        if (joyTest) {
            display.drawText8x8(fit("Joysticks Test"), 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8(fit("OK to Exit"), 0, 2);
            display.drawText8x8(fit("Joy0 -> " + joy0), 0, 3);
            display.drawText8x8(fit("Joy1 -> " + joy1), 0, 4);
            display.drawText8x8("                ", 0, 5);
            display.drawText8x8("                ", 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Écran réglage luminosité
        if (brightMenu) {
            int filled = (brightLevel + 1) * 2;  // 2,4,6,8,10 pour niveaux 0-4
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < 10; i++) bar.append(i < filled ? '#' : ' ');
            bar.append("]");
            display.drawText8x8(fit("Brightness"), 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8(fit("OK to Exit"), 0, 2);
            display.drawText8x8(fit("UP+ / DOWN-"), 0, 3);
            display.drawText8x8(fit(bar.toString()), 0, 4);
            display.drawText8x8(fit("Lv:" + (brightLevel + 1) + " / 5"), 0, 5);
            display.drawText8x8("                ", 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Écran sélection mode Minitel
        if (inModeSelect) {
            final int modeIdx;
            synchronized (this) { modeIdx = selectedModeIdx; }
            display.drawText8x8(fit("Minitel Mode"), 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8(fit("OK to Exit"), 0, 2);
            display.drawText8x8(fit("UP- / DOWN+"), 0, 3);
            display.drawText8x8(fit("Mode:"), 0, 4);
            display.drawText8x8(fit(MINITEL_MODE_NAMES[modeIdx]), 0, 5);
            display.drawText8x8("                ", 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Écran About
        if (aboutScreen) {
            display.drawText8x8("                ", 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8("   Written by   ", 0, 2);
            display.drawText8x8("  Eddy BRIERE   ", 0, 3);
            display.drawText8x8("      2026      ", 0, 4);
            display.drawText8x8("----------------", 0, 5);
            display.drawText8x8("                ", 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Écran Network Info
        if (netInfo) {
            display.drawText8x8(fit("Network Info"), 0, 0);
            display.drawText8x8("----------------", 0, 1);
            display.drawText8x8(fit("OK to Exit"), 0, 2);
            display.drawText8x8("IP:", 0, 3);
            display.drawText8x8(fit(nIp), 0, 4);
            display.drawText8x8("MAC:", 0, 5);
            int macEnd = Math.min(nMac.length(), nMacScroll + OLEDDisplay.CHARS_PER_LINE_8X8);
            String macView = (nMacScroll < nMac.length()) ? nMac.substring(nMacScroll, macEnd) : nMac;
            display.drawText8x8(fit(macView), 0, 6);
            display.drawText8x8("                ", 0, 7);
            display.flush();
            return;
        }

        // Superposition (résultat d'action)
        if (overlay != null && System.currentTimeMillis() < overlayEnd) {
            for (int i = 0; i < overlay.length && i < OLEDDisplay.MAX_LINES; i++) {
                display.drawText8x8(fit(overlay[i]), 0, i);
            }
            display.flush();
            return;
        }

        // Ligne 0 : titre
        display.drawText8x8(fit(title), 0, 0);

        // Ligne 1 : séparateur
        display.drawText8x8("----------------", 0, 1);

        // Lignes 2-7 : entrées du menu
        for (int i = 0; i < MAX_VISIBLE; i++) {
            int itemIdx = scrOff + i;
            int row     = ITEMS_ROW_BASE + i;
            if (itemIdx >= items.length) {
                display.drawText8x8("                ", 0, row);
                continue;
            }
            String prefix = (itemIdx == selIdx) ? ">" : " ";
            String label  = items[itemIdx].label;
            if (label.length() > 15) label = label.substring(0, 15);
            display.drawText8x8(fit(prefix + label), 0, row);
        }

        display.flush();
    }

    private static String fit(String s) {
        int w = OLEDDisplay.CHARS_PER_LINE_8X8;
        if (s == null) s = "";
        if (s.length() > w) return s.substring(0, w);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }

    // ── Arborescence des menus ────────────────────────────────────────────────

    private MenuItem[] buildMainMenu() {
        return new MenuItem[]{
            new MenuItem("System",    buildSystemMenu()),
            new MenuItem("Client",    buildClientMenu()),
            new MenuItem("Server",    buildServerMenu()),
            new MenuItem("NetWork",   buildNetworkMenu()),
            new MenuItem("JoySticks", buildJoysticksMenu()),
            new MenuItem("About",     (Runnable) this::enterAbout),
        };
    }

    private MenuItem[] buildSystemMenu() {
        return new MenuItem[]{
            new MenuItem("Back",         (Runnable) this::goBack),
            new MenuItem("Buttons",      buildButtonsMenu()),
            new MenuItem("LEDs",         buildLedsMenu()),
            new MenuItem("Brightness",   (Runnable) this::enterBrightnessMenu),
            new MenuItem("Minitel Mode", (Runnable) this::enterModeSelect),
            new MenuItem("Reboot",       buildRebootMenu()),
        };
    }

    private MenuItem[] buildButtonsMenu() {
        return new MenuItem[]{
            new MenuItem("Back",  (Runnable) this::goBack),
            new MenuItem("Check", (Runnable) this::enterButtonTest),
        };
    }

    private MenuItem[] buildLedsMenu() {
        return new MenuItem[]{
            new MenuItem("Back",  (Runnable) this::goBack),
            new MenuItem("Check", (Runnable) this::enterLedTest),
        };
    }

    private MenuItem[] buildRebootMenu() {
        return new MenuItem[]{
            new MenuItem("Back",         (Runnable) this::goBack),
            new MenuItem("Yes I'm sure", () -> {
                showOverlay("Rebooting...");
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                if (actions != null) actions.onReboot();
            }),
        };
    }

    private MenuItem[] buildNetworkMenu() {
        return new MenuItem[]{
            new MenuItem("Back",        (Runnable) this::goBack),
            new MenuItem("Info",        (Runnable) this::enterNetworkInfo),
            new MenuItem("WiFi Config", () -> {
                if (actions != null) actions.onNetConfig();
            }),
            new MenuItem("Set Ethernet", () -> {
                if (actions != null) actions.onSetEthernet();
                showOverlay("Interface:", "Ethernet", "saved OK");
            }),
            new MenuItem("Renew DHCP",  () -> {
                showOverlay("DHCP renewing", "Please wait...");
                if (actions != null) actions.onRenewDhcp();
            }),
        };
    }

    private MenuItem[] buildClientMenu() {
        return new MenuItem[]{
            new MenuItem("Back",         (Runnable) this::goBack),
            new MenuItem("Current URL",  () -> {
                String url = (actions != null) ? actions.getCurrentUrl() : "N/A";
                showOverlay(splitToLines(url));
            }),
            new MenuItem("Size History", () -> {
                String h = (actions != null) ? actions.getSizeHistory() : "N/A";
                showOverlay(splitToLines(h));
            }),
            new MenuItem("Restart",      () -> {
                showOverlay("Client", "restarting...");
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                if (actions != null) actions.onRestartClient();
            }),
        };
    }

    private MenuItem[] buildServerMenu() {
        return new MenuItem[]{
            new MenuItem("Back",    (Runnable) this::goBack),
            new MenuItem("Info",    () -> {
                String raw   = (actions != null) ? actions.getServerInfo() : "N/A";
                int    colon = raw.lastIndexOf(':');
                String host  = (colon >= 0) ? raw.substring(0, colon) : raw;
                String port  = (colon >= 0) ? raw.substring(colon + 1) : "";
                showOverlay("Local Server", "----------------",
                            fit("IP:" + host), fit("PORT:" + port));
            }),
            new MenuItem("Restart", () -> {
                showOverlay("Server", "restarting...");
                if (actions != null) actions.onRestartServer();
            }),
        };
    }

    private MenuItem[] buildJoysticksMenu() {
        return new MenuItem[]{
            new MenuItem("Back", (Runnable) this::goBack),
            new MenuItem("Info", () -> {
                String info = (actions != null) ? actions.getJoystickInfo() : "N/A";
                showOverlay(splitToLines(info));
            }),
            new MenuItem("Test", (Runnable) this::enterJoyTest),
        };
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String[] splitToLines(String text) {
        int w = OLEDDisplay.CHARS_PER_LINE_8X8;
        if (text == null || text.isEmpty()) return new String[]{"N/A"};
        List<String> lines = new ArrayList<>();
        while (!text.isEmpty() && lines.size() < OLEDDisplay.MAX_LINES) {
            if (text.length() <= w) { lines.add(text); break; }
            lines.add(text.substring(0, w));
            text = text.substring(w);
        }
        return lines.toArray(new String[0]);
    }
}
