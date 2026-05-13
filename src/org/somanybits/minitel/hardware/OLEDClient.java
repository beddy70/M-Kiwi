package org.somanybits.minitel.hardware;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire OLED côté client M-Kiwi.
 *
 * Affiche sur l'écran SSD1306 128×64 :
 *   - Version et connexion serveur
 *   - Page VTML courante
 *   - État des joysticks USB + dernier bouton pressé (2 s)
 *
 * Toutes les mises à jour sont sérialisées dans un unique thread daemon
 * "oled-client", ce qui évite les conflits I2C et ne bloque jamais
 * les threads appelants (joystick, navigation, etc.).
 */
public class OLEDClient {

    private static final long BTN_DISPLAY_MS = 2000;

    private final OLEDDisplay oled;
    private final String      version;
    private final String      server;
    private final int         port;

    private volatile String currentUrl = "";
    private volatile String joyDevice0 = "---";
    private volatile String joyDevice1 = "---";
    private volatile int    joyBtn0    = -1;
    private volatile int    joyBtn1    = -1;

    private final ScheduledExecutorService renderThread;

    public OLEDClient(String version, String server, int port) {
        this.oled    = new OLEDDisplay();
        this.version = version;
        this.server  = server;
        this.port    = port;
        this.renderThread = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oled-client");
            t.setDaemon(true);
            return t;
        });
    }

    /** Initialise l'écran. Silencieux si l'écran est absent. */
    public boolean init() {
        boolean ok = oled.init();
        if (ok) renderThread.submit(this::render);
        return ok;
    }

    public boolean isAvailable() { return oled.isAvailable(); }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Appeler à chaque changement de page VTML. */
    public void onNavigate(String url) {
        currentUrl = url != null ? url : "";
        renderThread.submit(this::render);
    }

    /** Appeler quand un joystick se connecte ou se déconnecte. */
    public void onJoystick(int idx, String device, boolean connected) {
        String name = (connected && device != null)
                ? device.replace("/dev/input/", "") : "---";
        if (idx == 0) joyDevice0 = name;
        else          joyDevice1 = name;
        renderThread.submit(this::render);
    }

    /** Appeler à chaque appui de bouton joystick. */
    public void onButton(int idx, int button) {
        if (idx == 0) joyBtn0 = button;
        else          joyBtn1 = button;
        renderThread.submit(this::render);
        // Effacer l'indicateur bouton après BTN_DISPLAY_MS
        renderThread.schedule(() -> {
            if (idx == 0) joyBtn0 = -1;
            else          joyBtn1 = -1;
            render();
        }, BTN_DISPLAY_MS, TimeUnit.MILLISECONDS);
    }

    /** Libère les ressources. */
    public void close() {
        renderThread.shutdownNow();
        oled.close();
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    private void render() {
        if (!oled.isAvailable()) return;
        try {
            oled.clear();
            oled.drawText(truncate("M-Kiwi v" + version, 21),  0, 0);
            oled.drawText(truncate(server + ":" + port, 21),    0, 1);
            oled.drawText(truncate(currentUrl, 21),              0, 2);
            oled.drawText("---------------------",               0, 3);
            oled.drawText(buildJoyLine("J1", joyDevice0, joyBtn0), 0, 4);
            oled.drawText(buildJoyLine("J2", joyDevice1, joyBtn1), 0, 5);
            oled.flush();
        } catch (Exception ignored) {}
    }

    private static String buildJoyLine(String label, String device, int btn) {
        String dev    = truncate(device, 10);
        String btnStr = btn >= 0 ? "B:" + btn : "";
        return truncate(String.format("%-3s: %-10s%s", label, dev, btnStr), 21);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
