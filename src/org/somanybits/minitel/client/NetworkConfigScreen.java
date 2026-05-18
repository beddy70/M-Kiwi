package org.somanybits.minitel.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.Teletel;
import org.somanybits.minitel.events.KeyPressedEvent;

/**
 * Écran de configuration réseau pour le client M-Kiwi.
 * Activé par mkiwi:netconfig.
 *
 * Étape 1 — INTERFACE : choisir Ethernet ou WiFi.
 * Étape 2 — WIFI_LIST : scanner et sélectionner un réseau.
 * Étape 3 — WIFI_PASS : saisir le mot de passe et se connecter.
 */
public class NetworkConfigScreen {

    public enum ActionResult { NONE, CANCEL, APPLY_ETH, APPLY_WIFI }

    private enum Step { INTERFACE, WIFI_LIST, WIFI_PASS }

    private static final int MAX_NETWORKS = 9;
    private static final int PASS_MAX     = 32;

    private final Teletel t;
    private Step step = Step.INTERFACE;

    private final String currentPrimary;
    private final String currentSsid;

    private final List<WifiNetwork> networks = new ArrayList<>();
    private WifiNetwork selectedNetwork = null;
    private final StringBuilder passwordBuf = new StringBuilder();

    private String pendingSsid;
    private String pendingPassword;

    public NetworkConfigScreen(Teletel t, String currentPrimary, String currentSsid) {
        this.t = t;
        this.currentPrimary = currentPrimary != null ? currentPrimary : "auto";
        this.currentSsid    = currentSsid    != null ? currentSsid    : "";
    }

    public String getPendingSsid()     { return pendingSsid; }
    public String getPendingPassword() { return pendingPassword; }

    public void show() throws IOException {
        drawInterfaceScreen();
    }

    public ActionResult handleKey(KeyPressedEvent event) throws IOException {
        switch (step) {
            case INTERFACE: return handleInterfaceKey(event);
            case WIFI_LIST: return handleWifiListKey(event);
            case WIFI_PASS: return handleWifiPassKey(event);
            default:        return ActionResult.CANCEL;
        }
    }

    // ── Étape 1 : choix d'interface ────────────────────────────────────────────

    private ActionResult handleInterfaceKey(KeyPressedEvent event) throws IOException {
        if (event.getType() == KeyPressedEvent.TYPE_KEY_CHAR_EVENT) {
            char car = (char) event.getKeyCode();
            if (car == '1') return ActionResult.APPLY_ETH;
            if (car == '2') {
                step = Step.WIFI_LIST;
                drawWifiListScreen();
                loadNetworks(false);
                return ActionResult.NONE;
            }
        } else if (event.getType() == KeyPressedEvent.TYPE_KEY_MENU_EVENT
                && event.getKeyCode() == KeyPressedEvent.KEY_RETOUR) {
            return ActionResult.CANCEL;
        }
        return ActionResult.NONE;
    }

    private void drawInterfaceScreen() throws IOException {
        t.clear();
        drawTitle("CONFIG RESEAU  M-Kiwi");

        t.setCursor(2, 3);
        t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("Interface principale :");

        boolean ethActive  = !"wifi".equals(currentPrimary);
        boolean wifiActive = "wifi".equals(currentPrimary);

        t.setCursor(2, 5);
        t.setTextColor(ethActive ? Teletel.COLOR_GREEN : Teletel.COLOR_WHITE);
        t.writeString("[1] ETHERNET" + (ethActive ? "  < actuelle" : ""));

        t.setCursor(2, 6);
        t.setTextColor(wifiActive ? Teletel.COLOR_GREEN : Teletel.COLOR_WHITE);
        t.writeString("[2] WiFi" + (wifiActive ? "      < actuelle" : ""));

        if (!currentSsid.isEmpty()) {
            t.setCursor(2, 8);
            t.setTextColor(Teletel.COLOR_CYAN);
            t.writeString("SSID : ");
            t.setTextColor(Teletel.COLOR_WHITE);
            t.writeString(trunc(currentSsid, 31));
        }

        drawFooter("1/2=choisir  RETOUR=annuler");
    }

    // ── Étape 2 : liste des réseaux WiFi ──────────────────────────────────────

    private ActionResult handleWifiListKey(KeyPressedEvent event) throws IOException {
        if (event.getType() == KeyPressedEvent.TYPE_KEY_CHAR_EVENT) {
            char car = (char) event.getKeyCode();
            if (car >= '1' && car <= '9') {
                int idx = car - '1';
                if (idx < networks.size()) {
                    selectedNetwork = networks.get(idx);
                    passwordBuf.setLength(0);
                    step = Step.WIFI_PASS;
                    drawWifiPassScreen();
                }
                return ActionResult.NONE;
            }
            if (car == '*') {
                loadNetworks(true);
                return ActionResult.NONE;
            }
        } else if (event.getType() == KeyPressedEvent.TYPE_KEY_MENU_EVENT
                && event.getKeyCode() == KeyPressedEvent.KEY_RETOUR) {
            step = Step.INTERFACE;
            drawInterfaceScreen();
            return ActionResult.NONE;
        }
        return ActionResult.NONE;
    }

    private void drawWifiListScreen() throws IOException {
        t.clear();
        drawTitle("CONFIG WIFI  M-Kiwi");

        t.setCursor(2, 3);
        t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("Reseaux disponibles :");

        clearArea(5, MAX_NETWORKS);
        drawFooter("1-9=choisir  *=scanner  RETOUR=retour");
    }

    private void drawNetworkList() throws IOException {
        clearArea(5, MAX_NETWORKS);
        if (networks.isEmpty()) {
            t.setCursor(2, 8);
            t.setTextColor(Teletel.COLOR_YELLOW);
            t.writeString("Aucun reseau trouve.");
            t.setCursor(2, 9);
            t.writeString("Appuyez sur * pour scanner.");
            return;
        }
        int count = Math.min(networks.size(), MAX_NETWORKS);
        for (int i = 0; i < count; i++) {
            WifiNetwork nw = networks.get(i);
            t.setCursor(0, 5 + i);
            t.setTextColor(Teletel.COLOR_CYAN);
            t.writeString(" " + (i + 1) + " ");
            t.setTextColor(currentSsid.equals(nw.ssid) ? Teletel.COLOR_GREEN : Teletel.COLOR_WHITE);
            t.writeString(padRight(trunc(nw.ssid, 30), 30) + " " + nw.signalBars());
        }
    }

    private void loadNetworks(boolean rescan) {
        try {
            t.setCursor(2, 7);
            t.setTextColor(Teletel.COLOR_YELLOW);
            t.writeString("Scan en cours...          ");
        } catch (IOException ignored) {}

        // Mode multiline : chaque champ sur sa propre ligne "CHAMP:valeur"
        // Evite l'ambiguïté du séparateur ':' dans les SSIDs (format terse)
        // --rescan auto : NM rescanne si le cache est périmé (évite résultat partiel)
        String[] cmd = rescan
            ? new String[]{"nmcli", "-t", "-m", "multiline", "-f", "SSID,SIGNAL", "dev", "wifi", "list", "--rescan", "yes"}
            : new String[]{"nmcli", "-t", "-m", "multiline", "-f", "SSID,SIGNAL", "dev", "wifi", "list", "--rescan", "auto"};

        networks.clear();
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            boolean finished = p.waitFor(rescan ? 15 : 10, java.util.concurrent.TimeUnit.SECONDS);
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.err.println("[WiFi] finished=" + finished + " exitCode=" + (finished ? p.exitValue() : -1));
            System.err.println("[WiFi] raw output (" + output.length() + " chars):\n" + output);

            // Parsing multiline : SSID et SIGNAL sur lignes séparées
            String parsedSsid = null;
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.startsWith("SSID:")) {
                    parsedSsid = line.substring(5).trim();
                } else if (line.startsWith("SIGNAL:") && parsedSsid != null) {
                    String signalStr = line.substring(7).trim();
                    try {
                        int signal = Integer.parseInt(signalStr);
                        if (!parsedSsid.isEmpty() && !parsedSsid.equals("--")) {
                            boolean dup = false;
                            for (WifiNetwork existing : networks) {
                                if (existing.ssid.equals(parsedSsid)) {
                                    if (signal > existing.signal) existing.signal = signal;
                                    dup = true;
                                    break;
                                }
                            }
                            if (!dup && networks.size() < MAX_NETWORKS) {
                                networks.add(new WifiNetwork(parsedSsid, signal));
                                System.err.println("[WiFi] added: " + parsedSsid + " signal=" + signal);
                            } else if (dup) {
                                System.err.println("[WiFi] dup: " + parsedSsid);
                            }
                        } else {
                            System.err.println("[WiFi] skip (empty/--): ssid='" + parsedSsid + "'");
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("[WiFi] skip (parseInt failed): " + line);
                    }
                    parsedSsid = null;
                }
            }
            System.err.println("[WiFi] total networks: " + networks.size());
            networks.sort((a, b) -> b.signal - a.signal);
        } catch (Exception e) {
            System.err.println("WiFi scan: " + e.getMessage());
        }

        try { drawNetworkList(); } catch (IOException ignored) {}
    }

    // ── Étape 3 : saisie du mot de passe ──────────────────────────────────────

    private ActionResult handleWifiPassKey(KeyPressedEvent event) throws IOException {
        if (event.getType() == KeyPressedEvent.TYPE_KEY_CHAR_EVENT) {
            char car = (char) event.getKeyCode();
            if ((car == 0x08 || car == 0x7F) && passwordBuf.length() > 0) {
                passwordBuf.deleteCharAt(passwordBuf.length() - 1);
                drawPasswordField();
            } else if (car >= 0x20 && car < 0x7F && passwordBuf.length() < PASS_MAX) {
                passwordBuf.append(car);
                drawPasswordField();
            }
            return ActionResult.NONE;
        }
        if (event.getType() == KeyPressedEvent.TYPE_KEY_MENU_EVENT) {
            switch (event.getKeyCode()) {
                case KeyPressedEvent.KEY_ENVOI:
                    pendingSsid     = selectedNetwork.ssid;
                    pendingPassword = passwordBuf.toString();
                    return ActionResult.APPLY_WIFI;
                case KeyPressedEvent.KEY_RETOUR:
                    step = Step.WIFI_LIST;
                    drawWifiListScreen();
                    drawNetworkList();
                    return ActionResult.NONE;
                case KeyPressedEvent.KEY_CORRECTION:
                    if (passwordBuf.length() > 0) {
                        passwordBuf.deleteCharAt(passwordBuf.length() - 1);
                        drawPasswordField();
                    }
                    return ActionResult.NONE;
                default:
                    break;
            }
        }
        return ActionResult.NONE;
    }

    private void drawWifiPassScreen() throws IOException {
        t.clear();
        drawTitle("CONFIG WIFI  M-Kiwi");

        t.setCursor(2, 3);
        t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("Reseau : ");
        t.setTextColor(Teletel.COLOR_WHITE);
        t.writeString(trunc(selectedNetwork.ssid, 29));

        t.setCursor(2, 5);
        t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("Mot de passe :");

        drawPasswordField();

        t.setCursor(2, 8);
        t.setTextColor(Teletel.COLOR_YELLOW);
        t.writeString("Laisser vide si reseau ouvert");

        drawFooter("ENVOI=connecter  RETOUR=retour");
    }

    private void drawPasswordField() throws IOException {
        t.setCursor(0, 6);
        t.setBGColor(Teletel.COLOR_BLUE);
        t.setTextColor(Teletel.COLOR_WHITE);
        String masked = "*".repeat(passwordBuf.length());
        StringBuilder line = new StringBuilder(" > ").append(masked);
        while (line.length() < 40) line.append(' ');
        t.writeString(line.substring(0, 40));
        t.setBGColor(Teletel.COLOR_BLACK);
    }

    // ── Utilitaires d'affichage ───────────────────────────────────────────────

    private void drawTitle(String text) throws IOException {
        t.setCursor(0, 1);
        t.setBGColor(Teletel.COLOR_BLUE);
        t.setTextColor(Teletel.COLOR_WHITE);
        t.writeString(padCenter(text, 40));
        t.setBGColor(Teletel.COLOR_BLACK);
    }

    private void drawFooter(String help) throws IOException {
        t.setCursor(0, 21);
        t.setTextColor(Teletel.COLOR_YELLOW);
        t.writeString("----------------------------------------");
        t.setCursor(0, 22);
        t.setTextColor(Teletel.COLOR_GREEN);
        t.writeString(trunc(help, 40));
    }

    private void clearArea(int startRow, int rows) throws IOException {
        for (int i = 0; i < rows; i++) {
            t.setCursor(0, startRow + i);
            t.setTextColor(Teletel.COLOR_WHITE);
            t.writeString("                                        ");
        }
    }

    private String padCenter(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        int pad = (len - s.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(' ');
        sb.append(s);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    private String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ── WifiNetwork ───────────────────────────────────────────────────────────

    private static class WifiNetwork {
        String ssid;
        int    signal;

        WifiNetwork(String ssid, int signal) {
            this.ssid   = ssid;
            this.signal = signal;
        }

        String signalBars() {
            if (signal >= 75) return "****";
            if (signal >= 50) return "*** ";
            if (signal >= 25) return "**  ";
            return "*   ";
        }
    }
}
