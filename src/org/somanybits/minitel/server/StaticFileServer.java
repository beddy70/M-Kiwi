/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.kernel.Kernel;
import org.somanybits.minitel.VTMLContants;

public class StaticFileServer {
    
    public static final String PAGE_INDEX = "index." + VTMLContants.VTML_EXTENSION;
    public static final String VERSION = "0.3";
    
    private static HashMap<String, String> paramlist;
    private static LogManager logmgr;
    
    public static void main(String[] args) throws Exception {
        
        Path docRoot = Paths.get(Kernel.getIntance().getConfig().path.root_path).toAbsolutePath().normalize();
        int port = Kernel.getIntance().getConfig().server.port;
        
        logmgr = Kernel.getIntance().getLogManager();
        logmgr.setPrefix("> ");
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> handleRequest(ex, docRoot));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        
        logmgr.addLog(LogManager.ANSI_BOLD_GREEN + "Minitel Page Server version " + VERSION);
        logmgr.addLog(LogManager.ANSI_WHITE + "Serving " + docRoot + " on http://localhost:" + port + "/");
        
        Kernel.getIntance().getMModulesManager().loadAllMModulePlugins();
        
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

            //System.out.println("load mmodule " + rawPath.substring(rawPath.lastIndexOf("/") + 1));
            MModulesManager mmodmgr = Kernel.getIntance().getMModulesManager();
            
            String mmodname = rawPath.substring(rawPath.lastIndexOf("/") + 1).substring(0, rawPath.lastIndexOf(".") - 1);
            String response = mmodmgr.loadMModules(mmodname, ex, docRoot, paramlist);
            
            if (response == null) {
                response = "<minitel><div><row>MModule " + mmodname + " not found :(</row></div></minitel>";
            }
            
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=" + Kernel.getIntance().getConfig().server.defaultCharset);
            ex.sendResponseHeaders(200, response.length());
            
            byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
            
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
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=" + Kernel.getIntance().getConfig().server.defaultCharset);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
        ex.close();
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
    
}
