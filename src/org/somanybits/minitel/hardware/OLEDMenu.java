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
        void onRenewDhcp();
        String getCurrentUrl();
        String getSizeHistory();
        void onRestartClient();
        String getServerInfo();
        void onRestartServer();
        String getJoystickInfo();
        void onTestJoystick();
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
            @Override public void onPressed(int index)  { handleButton(index); }
            @Override public void onReleased(int index) {}
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

    // ── Boutons ───────────────────────────────────────────────────────────────

    private void handleButton(int index) {
        switch (index) {
            case 0 -> enter();
            case 1 -> moveUp();
            case 2 -> moveDown();
        }
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
                    if (leds != null) leds.set(3, true);
                    Thread.sleep(100);
                    if (leds != null) leds.set(3, false);
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

        synchronized (this) {
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
            new MenuItem("Server",    buildServerMenu()),
            new MenuItem("JoySticks", buildJoysticksMenu()),
            new MenuItem("NetWork",   buildNetworkMenu()),
            new MenuItem("Client",    buildClientMenu()),
        };
    }

    private MenuItem[] buildSystemMenu() {
        return new MenuItem[]{
            new MenuItem("Back",    (Runnable) this::goBack),
            new MenuItem("Buttons", buildButtonsMenu()),
            new MenuItem("LEDs",    buildLedsMenu()),
            new MenuItem("Reboot",  buildRebootMenu()),
        };
    }

    private MenuItem[] buildButtonsMenu() {
        return new MenuItem[]{
            new MenuItem("Back",  (Runnable) this::goBack),
            new MenuItem("Check", () -> {
                if (actions != null) actions.onCheckButtons();
                showOverlay("Button test", "Press buttons", "to test LEDs");
            }),
        };
    }

    private MenuItem[] buildLedsMenu() {
        return new MenuItem[]{
            new MenuItem("Back",  (Runnable) this::goBack),
            new MenuItem("Check", () -> {
                if (actions != null) actions.onCheckLeds();
                showOverlay("LED test", "Checking LEDs...");
            }),
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
            new MenuItem("Back",  (Runnable) this::goBack),
            new MenuItem("Info",  () -> {
                String info = (actions != null) ? actions.getNetworkInfo() : "N/A";
                showOverlay(splitToLines(info));
            }),
            new MenuItem("Renew", () -> {
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
                String info = (actions != null) ? actions.getServerInfo() : "N/A";
                showOverlay(splitToLines(info));
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
            new MenuItem("Test", () -> {
                showOverlay("Joystick test", "Move sticks", "or press BTNs");
                if (actions != null) actions.onTestJoystick();
            }),
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
