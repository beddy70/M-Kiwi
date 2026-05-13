package org.somanybits.minitel.hardware;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire OLED côté serveur M-Kiwi.
 *
 * Affiche en temps réel sur l'écran SSD1306 128×64 :
 *   - Nom, version et port du serveur
 *   - Indicateurs de trafic HTTP (flèches >>> RX / TX <<<) clignotants
 *   - Dernière page VTML servie
 *   - Dernier message d'erreur (module non chargé, etc.)
 *
 * Utilisation depuis n'importe quel MModule :
 * <pre>
 *   OLEDServer oled = Kernel.getInstance().getOledServer();
 *   if (oled != null) oled.showError("Module introuvable");
 * </pre>
 */
public class OLEDServer {

    private static final int BLINK_MS   = 300;   // durée de l'indicateur RX/TX
    private static final int REFRESH_MS = 150;   // cadence de rafraîchissement

    private final OLEDDisplay oled;
    private final String serverName;
    private final String version;
    private final int    port;

    private volatile long   lastRxTime    = 0;
    private volatile long   lastTxTime    = 0;
    private volatile String lastPath      = "";
    private volatile String errorMessage  = null;

    private ScheduledExecutorService scheduler;

    public OLEDServer(String serverName, String version, int port) {
        this.oled       = new OLEDDisplay();
        this.serverName = serverName;
        this.version    = version;
        this.port       = port;
    }

    /**
     * Initialise l'écran et démarre la boucle de rafraîchissement.
     * @return true si l'écran est disponible, false sinon (silencieux)
     */
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

    /** Appeler à chaque requête HTTP reçue. */
    public void onRX(String path) {
        lastRxTime = System.currentTimeMillis();
        lastPath   = path != null ? path : "";
    }

    /** Appeler à chaque réponse HTTP envoyée. */
    public void onTX() {
        lastTxTime = System.currentTimeMillis();
    }

    /** Affiche un message d'erreur sur la dernière ligne (persistant). */
    public void showError(String message) {
        errorMessage = message;
    }

    /** Efface le message d'erreur. */
    public void clearError() {
        errorMessage = null;
    }

    /** Libère les ressources. */
    public void close() {
        if (scheduler != null) scheduler.shutdownNow();
        oled.close();
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    private void refresh() {
        if (!oled.isAvailable()) return;
        try {
            long now   = System.currentTimeMillis();
            boolean rx = (now - lastRxTime) < BLINK_MS;
            boolean tx = (now - lastTxTime) < BLINK_MS;

            oled.clear();

            // Ligne 0 : nom du serveur (tronqué à 21 chars)
            oled.drawText(truncate(serverName, 21), 0, 0);

            // Ligne 1 : version + port
            oled.drawText(truncate("v" + version + "  port:" + port, 21), 0, 1);

            // Ligne 2 : séparateur
            oled.drawText("---------------------", 0, 2);

            // Ligne 3 : indicateurs trafic
            //   ">>>RX     TX<<<" (15 chars, centré dans 21)
            String rxArrow = rx ? ">>>" : "   ";
            String txArrow = tx ? "<<<" : "   ";
            oled.drawText("  " + rxArrow + "RX     TX" + txArrow, 0, 3);

            // Ligne 4 : dernière page servie
            if (!lastPath.isEmpty()) {
                oled.drawText(truncate(lastPath, 21), 0, 4);
            }

            // Ligne 7 : erreur (si présente)
            if (errorMessage != null) {
                oled.drawText(truncate("!" + errorMessage, 21), 0, 7);
            }

            oled.flush();
        } catch (Exception ignored) {
            // Ne pas crasher le serveur pour un problème d'affichage
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
