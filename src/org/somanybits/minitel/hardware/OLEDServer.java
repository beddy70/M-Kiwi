package org.somanybits.minitel.hardware;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire OLED côté serveur M-Kiwi.
 *
 * Thread dédié "oled-refresh" (daemon) cadencé à REFRESH_MS.
 * Le flush I2C (1033 octets à 100 kHz ≈ 93 ms) n'est déclenché
 * que si le contenu a réellement changé (flag dirty), ce qui évite
 * de saturer le bus I2C quand le serveur est inactif.
 *
 * Format du fichier IPC /tmp/mkiwi-jstk.txt (écrit par MinitelClient) :
 *   J1:<device>:<bouton>:<timestamp_ms>
 *   J2:<device>:<bouton>:<timestamp_ms>
 */
public class OLEDServer {

    public static final String JOY_STATE_FILE = "/tmp/mkiwi-jstk.txt";

    private static final int  BLINK_MS        = 300;   // durée indicateur RX/TX
    private static final int  REFRESH_MS      = 150;   // cadence du thread
    private static final int  JOY_POLL_CYCLES = 5;     // relire fichier joystick toutes les ~750 ms
    private static final long BTN_DISPLAY_MS  = 2000;  // durée d'affichage du bouton

    private final OLEDDisplay oled;
    private final String serverName;
    private final String version;
    private final int    port;

    // Trafic HTTP
    private volatile long lastRxTime = 0;
    private volatile long lastTxTime = 0;

    // Erreur
    private volatile String errorMessage = null;

    // Joysticks
    private volatile String joyDevice0  = "---";
    private volatile int    joyBtn0     = -1;
    private volatile long   joyBtnTime0 = 0;
    private volatile String joyDevice1  = "---";
    private volatile int    joyBtn1     = -1;
    private volatile long   joyBtnTime1 = 0;

    // Rendu différentiel : flush seulement si le contenu a changé
    private volatile boolean dirty = true;   // true = doit être redessiné
    private boolean prevRx = false;          // état blink précédent (thread oled uniquement)
    private boolean prevTx = false;

    private int refreshCount = 0;

    private ScheduledExecutorService scheduler;

    public OLEDServer(String serverName, String version, int port) {
        this.oled       = new OLEDDisplay();
        this.serverName = serverName;
        this.version    = version;
        this.port       = port;
    }

    public boolean init() {
        if (!oled.init()) return false;
        refresh();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oled-refresh");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::refresh, REFRESH_MS, REFRESH_MS, TimeUnit.MILLISECONDS);
        return true;
    }

    public boolean isAvailable() { return oled.isAvailable(); }

    // ── API publique ──────────────────────────────────────────────────────────

    public void onRX(String path) { lastRxTime = System.currentTimeMillis(); dirty = true; }
    public void onTX()            { lastTxTime = System.currentTimeMillis(); dirty = true; }

    public void showError(String message) { errorMessage = message; dirty = true; }
    public void clearError()              { errorMessage = null;    dirty = true; }

    public void close() {
        if (scheduler != null) scheduler.shutdownNow();
        oled.close();
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    private void readJoystickState() {
        try {
            Path file = Path.of(JOY_STATE_FILE);
            if (!Files.exists(file)) return;
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                String[] p = line.split(":", 4);
                if (p.length < 4) continue;
                int  btn  = parseInt(p[2], -1);
                long time = parseLong(p[3], 0);
                if ("J1".equals(p[0])) {
                    boolean changed = !p[1].equals(joyDevice0) || btn != joyBtn0 || time != joyBtnTime0;
                    if (changed) { joyDevice0 = p[1]; joyBtn0 = btn; joyBtnTime0 = time; dirty = true; }
                } else if ("J2".equals(p[0])) {
                    boolean changed = !p[1].equals(joyDevice1) || btn != joyBtn1 || time != joyBtnTime1;
                    if (changed) { joyDevice1 = p[1]; joyBtn1 = btn; joyBtnTime1 = time; dirty = true; }
                }
            }
        } catch (Exception ignored) {}
    }

    private void refresh() {
        if (!oled.isAvailable()) return;
        try {
            if (refreshCount++ % JOY_POLL_CYCLES == 0) readJoystickState();

            long    now = System.currentTimeMillis();
            boolean rx  = (now - lastRxTime) < BLINK_MS;
            boolean tx  = (now - lastTxTime) < BLINK_MS;

            // Redessiner si : contenu modifié, ou état blink qui change, ou blink actif
            boolean needsFlush = dirty || rx || tx || (rx != prevRx) || (tx != prevTx);
            prevRx = rx;
            prevTx = tx;

            if (!needsFlush) return;   // ← rien à envoyer sur I2C
            dirty = false;

            oled.clear();

            oled.drawText(truncate(serverName, 21), 0, 0);
            oled.drawText(truncate("v" + version + "  port:" + port, 21), 0, 1);
            oled.drawText("---------------------", 0, 2);

            String rxArrow = rx ? ">>>" : "   ";
            String txArrow = tx ? "<<<" : "   ";
            oled.drawText("  " + rxArrow + "RX     TX" + txArrow, 0, 3);

            // Boutons : affichage pendant BTN_DISPLAY_MS après le dernier appui
            // Si le bouton est encore "visible", dirty sera vrai au cycle suivant
            // pour effacer l'indication quand le délai expire.
            boolean btn0visible = joyBtn0 >= 0 && (now - joyBtnTime0) < BTN_DISPLAY_MS;
            boolean btn1visible = joyBtn1 >= 0 && (now - joyBtnTime1) < BTN_DISPLAY_MS;
            if (btn0visible || btn1visible) dirty = true;   // forcer re-rendu à expiration

            oled.drawText(buildJoyLine("J1", joyDevice0, joyBtn0, btn0visible), 0, 4);
            oled.drawText(buildJoyLine("J2", joyDevice1, joyBtn1, btn1visible), 0, 5);

            if (errorMessage != null) {
                oled.drawText(truncate("!" + errorMessage, 21), 0, 7);
            }

            oled.flush();
        } catch (Exception ignored) {}
    }

    private static String buildJoyLine(String label, String device, int btn, boolean showBtn) {
        String dev    = truncate(device, 10);
        String btnStr = showBtn ? "B:" + btn : "";
        return truncate(String.format("%-3s: %-10s%s", label, dev, btnStr), 21);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s);  } catch (Exception e) { return def; }
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }
}
