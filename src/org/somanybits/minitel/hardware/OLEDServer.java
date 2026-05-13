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
 * Affiche en temps réel sur l'écran SSD1306 128×64 :
 *   - Nom, version et port du serveur
 *   - Indicateurs de trafic HTTP (flèches >>> RX / TX <<<) clignotants
 *   - État des joysticks et dernier bouton pressé (pendant 2 s)
 *   - Dernier message d'erreur (module non chargé, etc.)
 *
 * Format du fichier IPC /tmp/mkiwi-jstk.txt (écrit par MinitelClient) :
 *   J1:<device>:<bouton>:<timestamp_ms>
 *   J2:<device>:<bouton>:<timestamp_ms>
 * Exemple :
 *   J1:js0:3:1748000000000
 *   J2:---:-1:0
 */
public class OLEDServer {

    public static final String JOY_STATE_FILE = "/tmp/mkiwi-jstk.txt";

    private static final int  BLINK_MS        = 300;  // durée indicateur RX/TX
    private static final int  REFRESH_MS      = 150;  // cadence de rafraîchissement
    private static final int  JOY_POLL_CYCLES = 5;    // relire le fichier toutes les ~750 ms
    private static final long BTN_DISPLAY_MS  = 2000; // durée d'affichage du bouton

    private final OLEDDisplay oled;
    private final String serverName;
    private final String version;
    private final int    port;

    private volatile long   lastRxTime   = 0;
    private volatile long   lastTxTime   = 0;
    private volatile String errorMessage = null;

    // État joystick J1
    private volatile String joyDevice0  = "---";
    private volatile int    joyBtn0     = -1;
    private volatile long   joyBtnTime0 = 0;

    // État joystick J2
    private volatile String joyDevice1  = "---";
    private volatile int    joyBtn1     = -1;
    private volatile long   joyBtnTime1 = 0;

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

    public void onRX(String path) { lastRxTime = System.currentTimeMillis(); }
    public void onTX()            { lastTxTime = System.currentTimeMillis(); }

    public void showError(String message) { errorMessage = message; }
    public void clearError()              { errorMessage = null; }

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
                String[] parts = line.split(":", 4);
                if (parts.length < 4) continue;
                int    btn  = parseInt(parts[2], -1);
                long   time = parseLong(parts[3], 0);
                if ("J1".equals(parts[0])) {
                    joyDevice0  = parts[1];
                    joyBtn0     = btn;
                    joyBtnTime0 = time;
                } else if ("J2".equals(parts[0])) {
                    joyDevice1  = parts[1];
                    joyBtn1     = btn;
                    joyBtnTime1 = time;
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

            oled.clear();

            oled.drawText(truncate(serverName, 21), 0, 0);
            oled.drawText(truncate("v" + version + "  port:" + port, 21), 0, 1);
            oled.drawText("---------------------", 0, 2);

            String rxArrow = rx ? ">>>" : "   ";
            String txArrow = tx ? "<<<" : "   ";
            oled.drawText("  " + rxArrow + "RX     TX" + txArrow, 0, 3);

            oled.drawText(buildJoyLine("J1", joyDevice0, joyBtn0, joyBtnTime0, now), 0, 4);
            oled.drawText(buildJoyLine("J2", joyDevice1, joyBtn1, joyBtnTime1, now), 0, 5);

            if (errorMessage != null) {
                oled.drawText(truncate("!" + errorMessage, 21), 0, 7);
            }

            oled.flush();
        } catch (Exception ignored) {}
    }

    private static String buildJoyLine(String label, String device, int btn, long btnTime, long now) {
        String dev = truncate(device, 10);
        String btnStr = (btn >= 0 && now - btnTime < BTN_DISPLAY_MS) ? "B:" + btn : "";
        return truncate(String.format("%-3s: %-10s%s", label, dev, btnStr), 21);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }
}
