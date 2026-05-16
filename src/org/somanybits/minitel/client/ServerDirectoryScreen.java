package org.somanybits.minitel.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.somanybits.minitel.Teletel;
import org.somanybits.minitel.events.KeyPressedEvent;

/**
 * Écran annuaire de serveurs VTML pour le client M-Kiwi.
 * Activé par l'URL spéciale mkiwi:server_directory.
 */
public class ServerDirectoryScreen {

    public enum ActionResult { NONE, CANCEL, NAVIGATE }

    private enum FocusField { SERVER, SEARCH, LIST }

    private static final int MAX_RESULTS = 10;
    private static final int FIELD_MAX   = 30;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Terminal ───────────────────────────────────────────────────────────────
    private final Teletel t;

    // ── Champs de saisie ──────────────────────────────────────────────────────
    private StringBuilder serverField;
    private final StringBuilder searchField = new StringBuilder();
    private FocusField focus = FocusField.SEARCH;

    // ── Résultats ─────────────────────────────────────────────────────────────
    private int currentPage  = 1;
    private int totalPages   = 1;
    private final List<Entry> results = new ArrayList<>();

    // ── Vue détail ────────────────────────────────────────────────────────────
    private boolean showingDetail = false;
    private Entry   detailEntry   = null;

    // ── Résultat de navigation ────────────────────────────────────────────────
    private String pendingHost;
    private int    pendingPort;

    // ── Constructeur ──────────────────────────────────────────────────────────

    public ServerDirectoryScreen(Teletel t, String directoryUrl) {
        this.t  = t;
        String addr = (directoryUrl != null && !directoryUrl.isEmpty()) ? directoryUrl : "http://localhost:8000";
        this.serverField = new StringBuilder(addr);
    }

    public String getPendingHost() { return pendingHost; }
    public int    getPendingPort() { return pendingPort; }

    // ── Affichage initial ─────────────────────────────────────────────────────

    public void show() throws IOException {
        drawListScreen();
        fetchResults();
    }

    // ── Gestion clavier ───────────────────────────────────────────────────────

    public ActionResult handleKey(KeyPressedEvent event) throws IOException {
        return showingDetail ? handleDetailKey(event) : handleListKey(event);
    }

    private ActionResult handleListKey(KeyPressedEvent event) throws IOException {
        switch (event.getType()) {

            case KeyPressedEvent.TYPE_KEY_CHAR_EVENT: {
                char car = (char) event.getKeyCode();

                // Sélection par numéro (1-9 = indices 0-8, 0 = indice 9)
                if (car >= '1' && car <= '9') {
                    int idx = car - '1';
                    if (idx < results.size()) showDetail(results.get(idx));
                    return ActionResult.NONE;
                }
                if (car == '0') {
                    if (9 < results.size()) showDetail(results.get(9));
                    return ActionResult.NONE;
                }

                // Pagination N = suivant, P = précédent
                if (car == 'n' || car == 'N') {
                    if (currentPage < totalPages) { currentPage++; fetchResults(); }
                    return ActionResult.NONE;
                }
                if (car == 'p' || car == 'P') {
                    if (currentPage > 1) { currentPage--; fetchResults(); }
                    return ActionResult.NONE;
                }

                // Entrée = champ suivant
                if (car == 0x0D || car == 0x0A) {
                    cycleFocus();
                    return ActionResult.NONE;
                }

                // Backspace
                if (car == 0x08 || car == 0x7F) {
                    if (focus == FocusField.SERVER && serverField.length() > 0) {
                        serverField.deleteCharAt(serverField.length() - 1);
                        drawField(serverField, 10, 3, true);
                    } else if (focus == FocusField.SEARCH && searchField.length() > 0) {
                        searchField.deleteCharAt(searchField.length() - 1);
                        drawField(searchField, 10, 5, true);
                    }
                    return ActionResult.NONE;
                }

                // Caractère imprimable
                if (car >= 0x20 && car < 0x7F) {
                    if (focus == FocusField.SERVER && serverField.length() < FIELD_MAX) {
                        serverField.append(car);
                        drawField(serverField, 10, 3, true);
                    } else if (focus == FocusField.SEARCH && searchField.length() < FIELD_MAX) {
                        searchField.append(car);
                        drawField(searchField, 10, 5, true);
                    }
                }
                return ActionResult.NONE;
            }

            case KeyPressedEvent.TYPE_KEY_MENU_EVENT:
                switch (event.getKeyCode()) {
                    case KeyPressedEvent.KEY_ENVOI:
                        currentPage = 1;
                        fetchResults();
                        break;
                    case KeyPressedEvent.KEY_RETOUR:
                        return ActionResult.CANCEL;
                    case KeyPressedEvent.KEY_CORRECTION:
                        if (focus == FocusField.SERVER && serverField.length() > 0) {
                            serverField.deleteCharAt(serverField.length() - 1);
                            drawField(serverField, 10, 3, true);
                        } else if (focus == FocusField.SEARCH && searchField.length() > 0) {
                            searchField.deleteCharAt(searchField.length() - 1);
                            drawField(searchField, 10, 5, true);
                        }
                        break;
                    default:
                        break;
                }
                return ActionResult.NONE;

            case KeyPressedEvent.TYPE_KEY_DIRECTION_EVENT:
                if (event.getKeyCode() == KeyPressedEvent.KEY_UP) {
                    if (focus == FocusField.SEARCH) setFocus(FocusField.SERVER);
                    else if (focus == FocusField.LIST) setFocus(FocusField.SEARCH);
                } else if (event.getKeyCode() == KeyPressedEvent.KEY_DOWN) {
                    if (focus == FocusField.SERVER) setFocus(FocusField.SEARCH);
                    else if (focus == FocusField.SEARCH) setFocus(FocusField.LIST);
                }
                return ActionResult.NONE;

            default:
                return ActionResult.NONE;
        }
    }

    private ActionResult handleDetailKey(KeyPressedEvent event) throws IOException {
        if (event.getType() == KeyPressedEvent.TYPE_KEY_CHAR_EVENT) {
            char car = (char) event.getKeyCode();
            if (car == '1') {
                pendingHost = detailEntry.url;
                pendingPort = detailEntry.port;
                return ActionResult.NAVIGATE;
            }
            if (car == '3') {
                returnToList();
                return ActionResult.NONE;
            }
        } else if (event.getType() == KeyPressedEvent.TYPE_KEY_MENU_EVENT
                && event.getKeyCode() == KeyPressedEvent.KEY_RETOUR) {
            returnToList();
        }
        return ActionResult.NONE;
    }

    private void returnToList() throws IOException {
        showingDetail = false;
        drawListScreen();
        drawResults();
    }

    // ── Focus ──────────────────────────────────────────────────────────────────

    private void cycleFocus() throws IOException {
        switch (focus) {
            case SERVER: setFocus(FocusField.SEARCH); break;
            case SEARCH: setFocus(FocusField.LIST);   break;
            case LIST:   setFocus(FocusField.SERVER); break;
        }
    }

    private void setFocus(FocusField newFocus) throws IOException {
        FocusField old = focus;
        focus = newFocus;
        if (old == FocusField.SERVER || newFocus == FocusField.SERVER)
            drawField(serverField, 10, 3, focus == FocusField.SERVER);
        if (old == FocusField.SEARCH || newFocus == FocusField.SEARCH)
            drawField(searchField, 10, 5, focus == FocusField.SEARCH);
    }

    // ── Dessin liste ───────────────────────────────────────────────────────────

    private void drawListScreen() throws IOException {
        t.clear();

        // Titre
        t.setCursor(0, 1);
        t.setBGColor(Teletel.COLOR_BLUE);
        t.setTextColor(Teletel.COLOR_WHITE);
        t.writeString(padCenter("ANNUAIRE M-KIWI", 40));
        t.setBGColor(Teletel.COLOR_BLACK);

        // Champ Serveur
        t.setCursor(0, 3);
        t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("Serveur:  ");
        drawField(serverField, 10, 3, focus == FocusField.SERVER);

        // Champ Chercher
        t.setCursor(0, 5);
        t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("Chercher: ");
        drawField(searchField, 10, 5, focus == FocusField.SEARCH);

        // Séparateur haut
        t.setCursor(0, 7);
        t.setTextColor(Teletel.COLOR_YELLOW);
        t.writeString("----------------------------------------");

        // Zone résultats vide
        for (int i = 1; i <= MAX_RESULTS; i++) {
            t.setCursor(0, 7 + i);
            t.setTextColor(Teletel.COLOR_WHITE);
            t.writeString("                                        ");
        }

        // Séparateur bas
        t.setCursor(0, 18);
        t.setTextColor(Teletel.COLOR_YELLOW);
        t.writeString("----------------------------------------");

        // Aide
        t.setCursor(0, 21);
        t.setTextColor(Teletel.COLOR_GREEN);
        t.writeString("ENVOI=chercher  ENTREE=champ suivant");
        t.setCursor(0, 22);
        t.writeString("1-9/0=selectionner  N=suiv  P=prec");
    }

    private void drawField(StringBuilder field, int col, int row, boolean active) throws IOException {
        t.setCursor(col, row);
        t.setBGColor(active ? Teletel.COLOR_BLUE : Teletel.COLOR_BLACK);
        t.setTextColor(Teletel.COLOR_WHITE);
        String content = field.toString();
        // Afficher la fin si trop long
        String visible = content.length() > FIELD_MAX
                ? content.substring(content.length() - FIELD_MAX)
                : content;
        t.writeString(padRight(visible, FIELD_MAX));
        t.setBGColor(Teletel.COLOR_BLACK);
    }

    private void drawResults() throws IOException {
        // Effacer la zone
        for (int i = 0; i < MAX_RESULTS; i++) {
            t.setCursor(0, 8 + i);
            t.setTextColor(Teletel.COLOR_WHITE);
            t.writeString("                                        ");
        }

        if (results.isEmpty()) {
            t.setCursor(2, 12);
            t.setTextColor(Teletel.COLOR_YELLOW);
            t.writeString("Aucun serveur trouve.");
        } else {
            for (int i = 0; i < results.size() && i < MAX_RESULTS; i++) {
                Entry e = results.get(i);
                int num = i + 1;
                t.setCursor(0, 8 + i);
                t.setTextColor(Teletel.COLOR_CYAN);
                t.writeString(num < 10 ? " " + num + " " : "10 ");
                t.setTextColor(Teletel.COLOR_WHITE);
                t.writeString(formatEntry(e, 37));
            }
        }

        drawPagination();
    }

    private String formatEntry(Entry e, int maxLen) {
        String name = e.name != null ? e.name : "?";
        String addr = e.url != null ? e.url : "";
        if (name.length() + 1 + addr.length() <= maxLen) {
            return padRight(name, maxLen - addr.length() - 1) + " " + addr;
        }
        return padRight(name.length() > maxLen ? name.substring(0, maxLen) : name, maxLen);
    }

    private void drawPagination() throws IOException {
        t.setCursor(0, 19);
        t.setTextColor(Teletel.COLOR_GREEN);
        String line;
        String pageInfo = "Page " + currentPage + "/" + totalPages;
        if (totalPages <= 1) {
            line = padCenter(pageInfo, 40);
        } else if (currentPage <= 1) {
            line = padRight(padCenter(pageInfo, 34) + " [N]", 40);
        } else if (currentPage >= totalPages) {
            line = padRight("[P] " + padCenter(pageInfo, 27), 40);
        } else {
            line = "[P] " + padCenter(pageInfo, 32) + " [N]";
        }
        t.writeString(padRight(line, 40));
    }

    // ── Dessin détail ─────────────────────────────────────────────────────────

    private void showDetail(Entry entry) throws IOException {
        detailEntry  = entry;
        showingDetail = true;
        drawDetailScreen();
    }

    private void drawDetailScreen() throws IOException {
        t.clear();

        t.setCursor(0, 1);
        t.setBGColor(Teletel.COLOR_BLUE);
        t.setTextColor(Teletel.COLOR_WHITE);
        t.writeString(padCenter("SERVEUR VTML", 40));
        t.setBGColor(Teletel.COLOR_BLACK);

        t.setCursor(0, 3); t.setTextColor(Teletel.COLOR_CYAN);  t.writeString("Nom  : ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString(trunc(detailEntry.name, 33));

        t.setCursor(0, 4); t.setTextColor(Teletel.COLOR_CYAN);  t.writeString("URL  : ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString(trunc(detailEntry.url, 33));

        t.setCursor(0, 5); t.setTextColor(Teletel.COLOR_CYAN);  t.writeString("Port : ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString(String.valueOf(detailEntry.port));

        if (detailEntry.description != null && !detailEntry.description.isEmpty()) {
            t.setCursor(0, 7); t.setTextColor(Teletel.COLOR_CYAN);
            t.writeString("Description :");
            String desc = detailEntry.description;
            t.setCursor(0, 8); t.setTextColor(Teletel.COLOR_WHITE);
            t.writeString(trunc(desc, 40));
            if (desc.length() > 40) {
                t.setCursor(0, 9); t.writeString(trunc(desc.substring(40), 40));
            }
        }

        t.setCursor(0, 11); t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("VTML   : ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString(trunc(detailEntry.vtmlVersion, 15));

        t.setCursor(0, 12); t.setTextColor(Teletel.COLOR_CYAN);
        t.writeString("M-Kiwi : ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString(trunc(detailEntry.mkiwiVersion, 15));

        if (detailEntry.categories != null && !detailEntry.categories.isEmpty()) {
            t.setCursor(0, 13); t.setTextColor(Teletel.COLOR_CYAN);
            t.writeString("Categ. : ");
            t.setTextColor(Teletel.COLOR_WHITE);
            t.writeString(trunc(String.join(", ", detailEntry.categories), 31));
        }

        t.setCursor(0, 15); t.setTextColor(Teletel.COLOR_YELLOW);
        t.writeString("----------------------------------------");

        t.setCursor(0, 16); t.setTextColor(Teletel.COLOR_CYAN);   t.writeString(" 1 ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString("Connexion");

        t.setCursor(0, 17); t.setTextColor(Teletel.COLOR_CYAN);   t.writeString(" 3 ");
        t.setTextColor(Teletel.COLOR_WHITE); t.writeString("Retour a la liste");
    }

    // ── Affichage erreur ──────────────────────────────────────────────────────

    public void showError(String msg) throws IOException {
        t.setCursor(0, 20);
        t.setTextColor(Teletel.COLOR_RED);
        t.writeString(padRight(trunc(msg, 40), 40));
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private String getBaseUrl() {
        String addr = serverField.toString().trim();
        if (addr.startsWith("http://") || addr.startsWith("https://")) return addr;
        return "http://" + addr;
    }

    private void fetchResults() throws IOException {
        String q = URLEncoder.encode(searchField.toString(), StandardCharsets.UTF_8);
        String url = getBaseUrl() + "/api/servers?page=" + currentPage + "&limit=" + MAX_RESULTS
                + (searchField.length() > 0 ? "&q=" + q : "");

        t.setCursor(2, 12);
        t.setTextColor(Teletel.COLOR_YELLOW);
        t.writeString("Chargement...                        ");

        try {
            String json = httpGet(url);
            parseResults(json);
        } catch (IOException e) {
            showListError("Annuaire inaccessible.");
        }
    }

    private void parseResults(String json) throws IOException {
        try {
            JsonNode root = MAPPER.readTree(json);
            totalPages = root.path("pages").asInt(1);
            results.clear();
            for (JsonNode item : root.path("items")) {
                Entry e = new Entry();
                e.id          = item.path("id").asInt();
                e.name        = item.path("name").asText("?");
                e.url         = item.path("url").asText("");
                e.port        = item.path("port").asInt(80);
                e.description = item.path("description").asText("");
                e.vtmlVersion = item.path("vtml_version").asText("1.0");
                e.mkiwiVersion= item.path("mkiwi_server_version").asText("1.0");
                e.categories  = new ArrayList<>();
                for (JsonNode c : item.path("categories")) e.categories.add(c.asText());
                results.add(e);
            }
            drawResults();
        } catch (Exception e) {
            showListError("Reponse invalide du serveur.");
        }
    }

    private void showListError(String msg) throws IOException {
        for (int i = 0; i < MAX_RESULTS; i++) {
            t.setCursor(0, 8 + i);
            t.writeString("                                        ");
        }
        t.setCursor(2, 11); t.setTextColor(Teletel.COLOR_RED);   t.writeString(trunc(msg, 36));
        t.setCursor(2, 12); t.setTextColor(Teletel.COLOR_YELLOW); t.writeString("Verifiez l'adresse du serveur.");
        drawPagination();
    }

    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ── Utilitaires texte ─────────────────────────────────────────────────────

    private String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
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

    private String trunc(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    // ── Classe interne Entry ──────────────────────────────────────────────────

    public static class Entry {
        public int    id;
        public String name;
        public String url;
        public int    port;
        public String description;
        public String vtmlVersion;
        public String mkiwiVersion;
        public List<String> categories;
    }
}
