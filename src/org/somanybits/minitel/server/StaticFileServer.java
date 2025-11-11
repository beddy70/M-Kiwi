/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import org.somanybits.minitel.VTML;

public class StaticFileServer {

    public static final String PAGE_INDEX = "index." + VTML.VTML_EXTENSION;
    public static final String VERSION = "0.2";
    private static String MMODULES_PATH = "/home/eddy/minitel/plugins/mmodules/";
    private static URL[] urls;
    private static HashMap<String, String> paramlist;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java StaticFileServer <DOCUMENT_ROOT> [PORT]");
            System.exit(1);
        }
        Path docRoot = Paths.get(args[0]).toAbsolutePath().normalize();
        if (!Files.isDirectory(docRoot)) {
            System.err.println("Document root invalide: " + docRoot);
            System.exit(2);
        }
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> handleRequest(ex, docRoot));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Minitel Page Server version " + VERSION);
        System.out.println("Serving " + docRoot + " on http://localhost:" + port + "/");

        loadAllMModulePlugins();

    }

    private static void handleRequest(HttpExchange ex, Path docRoot) throws IOException {

        String method = ex.getRequestMethod();

        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            respondText(ex, 405, "Method Not Allowed");
            return;
        }

        // 1) Récupérer et décoder le chemin demandé (sans la query)
        String rawPath = ex.getRequestURI().getRawPath();
        String pathDec = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        // Normaliser les séparateurs (évite backslashes sous Windows)
        pathDec = pathDec.replace('\\', '/');

        System.out.println("rawPath " + rawPath);
        System.out.println("pathDec " + pathDec);

        String query = ex.getRequestURI().getQuery();

        if (query != null) {
            System.out.println("Data:" + query);
            paramlist = new HashMap<>();
            StringTokenizer params = new StringTokenizer(query, "&");

            while (params.hasMoreTokens()) {
                StringTokenizer param = new StringTokenizer(params.nextToken(), "=");
                String pname = (String) param.nextElement();
                String pvalue = null;
                if (param.countTokens() > 0) {
                    pvalue = param.nextToken();
                }
                paramlist.put(pname, pvalue);

            }

//            paramlist.entrySet().forEach(entry -> {
//                System.out.println("name=" + entry.getKey() + " value=" + entry.getValue());
//            });
        }

        if ("mod".equals(rawPath.substring(rawPath.lastIndexOf(".") + 1))) {

            System.out.println("load mmodule " + rawPath.substring(rawPath.lastIndexOf("/") + 1));

            String responsebody = loadMModules(rawPath.substring(rawPath.lastIndexOf("/") + 1).substring(0, rawPath.lastIndexOf(".") - 1), ex, docRoot);
            
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(200, responsebody.length());
        
            byte[] respBytes = responsebody.getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = ex.getResponseBody()) {
                os.write(respBytes);
            } finally {
                ex.close();
            }
            return;
        }

        // 2) Si c'est la racine, tenter index.html
        if (pathDec.equals("/")) {
            pathDec = PAGE_INDEX;
            System.out.println("loading " + PAGE_INDEX);
        }

        // 3) Résoudre sécurisée: docRoot + chemin demandé
        Path target = safeResolve(docRoot, pathDec);
        if (target == null) {
            respondText(ex, 403, "Forbidden");
            return;
        }

        // 4) Si le chemin pointe vers un dossier, tenter index.vtml dedans
        if (Files.isDirectory(target)) {
            Path idx = target.resolve(PAGE_INDEX).normalize();
            if (!idx.startsWith(docRoot) || !Files.exists(idx)) {
                respondText(ex, 403, "Forbidden (no index.html)");
                return;
            }
            target = idx;
        }

        // 5) Vérifier existence fichier
        if (!Files.exists(target) || Files.isDirectory(target)) {
            respondText(ex, 404, "Not Found");
            return;
        }

        // 6) Déterminer le type MIME
        String ctype = Files.probeContentType(target);
        if (ctype == null) {
            ctype = guessContentTypeByExt(target);
        }

        // 7) Répondre (streaming). Pour HEAD, pas de corps.
        long len = Files.size(target);
        ex.getResponseHeaders().set("Content-Type", ctype);
        if ("HEAD".equals(method)) {
            ex.sendResponseHeaders(200, -1);
            ex.close();
            return;
        }

        // Pour gros fichiers, chunked: passer length = 0
        // Ici on envoie avec Content-Length connu (len) pour simplicité.
        ex.sendResponseHeaders(200, len);
        try (OutputStream os = ex.getResponseBody(); InputStream is = Files.newInputStream(target)) {
            is.transferTo(os);
        } finally {
            ex.close();
        }
    }

    /**
     * Empêche les traversées de répertoires. Retourne null si invalide.
     */
    private static Path safeResolve(Path root, String requestPath) {
        // Retire préfixe "/" afin de résoudre en relatif
        String rel = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        Path resolved = root.resolve(rel).normalize();
        // Interdit toute sortie du docRoot
        if (!resolved.startsWith(root)) {
            return null;
        }
        return resolved;
    }

    private static void respondText(HttpExchange ex, int code, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
        ex.close();
    }

    private static String loadMModules(String modname, HttpExchange ex, Path docRoot) throws MalformedURLException {

//        File classesDir = new File(MMODULES_PATH);              // racine des .class
//        URL url = classesDir.toURI().toURL();
        try (URLClassLoader loader = new URLClassLoader(urls,
                Thread.currentThread().getContextClassLoader())) {

            System.out.println(MMODULES_PATH + modname);

            Class<?> loadedClass = Class.forName("org.somanybits.minitel.server.mmodules." + modname, true, loader);

            Class<?> mmoduleClass = Class.forName("org.somanybits.minitel.server.MModule");

            // Vérification du type
            if (!mmoduleClass.isAssignableFrom(loadedClass)) {
                System.err.println(modname + " isn't not a correct MModule");
                return "<minitel>MModule not valid!</minitel>";
            }

            // 5) Récupère le constructeur (HashMap, HttpExchange, Path) avec fallback (Map,...,Path)
            Constructor<?> ctor;
            ctor = loadedClass.getDeclaredConstructor(
                    HashMap.class,
                    HttpExchange.class,
                    Path.class
            ); // Certains modules déclarent l’interface Map plutôt que HashMap

            ctor.setAccessible(true); // si non-public

            Object raw = ctor.newInstance(paramlist, ex, docRoot);
            MModule module = MModule.class.cast(raw);
            return module.getResponse();

        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            System.getLogger(StaticFileServer.class.getName()).log(System.Logger.Level.ERROR, (String) null, e);
        }
        return "";
    }

    private static String guessContentTypeByExt(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = (dot >= 0) ? name.substring(dot + 1) : "";

        return switch (ext) {
            case "mod" ->
                "text/plain; charset=utf-8";
            case "vtml" ->
                "text/plain; charset=utf-8";
            case "html", "htm" ->
                "text/html; charset=utf-8";
            case "css" ->
                "text/css; charset=utf-8";
            case "js" ->
                "application/javascript; charset=utf-8";
            case "json" ->
                "application/json; charset=utf-8";
            case "svg" ->
                "image/svg+xml";
            case "png" ->
                "image/png";
            case "jpg", "jpeg" ->
                "image/jpeg";
            case "gif" ->
                "image/gif";
            case "txt" ->
                "text/plain; charset=utf-8";
            default ->
                "application/octet-stream";

        };
    }

    private static void loadAllMModulePlugins() {

        File dir = new File(MMODULES_PATH);  // contient *.jar
        File[] jars = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));

        System.out.println("Loading mmodules");
        for (File jar : jars) {
            System.out.println("\t" + Arrays.toString(jars));
        }
        System.out.println("all mmodules loaded");
        if (jars == null || jars.length == 0) {
            return;
        }

        urls = Arrays.stream(jars).map(f -> {
            try {
                return f.toURI().toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);
    }

}
