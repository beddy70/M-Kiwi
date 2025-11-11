/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.kernel;

/**
 *
 * @author eddy
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParseException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigLoader {

    //private static String MMODULES_PATH = "/home/eddy/minitel/plugins/mmodules/";
    //private ConfigLoader() {}

    public static Config load(Path file) throws IOException {
        
        ObjectMapper om = new ObjectMapper();
        
        Config cfg;
        try {
            cfg = om.readValue(file.toFile(), Config.class);
        } catch (JsonParseException e) {
            throw new IOException("JSON invalide dans " + file + " : " + e.getOriginalMessage(), e);
        }

        // Defaults déjà posés dans les POJO, on normalise et on valide
        validatePort(cfg.server.port);
        validateCharset(cfg.server.defaultCharset);

        // Normalisation des chemins (résolution + nettoyage)
        cfg.path.root_path    = normalize(cfg.path.root_path);
        cfg.path.plugins_path = normalize(cfg.path.plugins_path);

        return cfg;
    }

    private static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port invalide: " + port);
        }
    }

    private static void validateCharset(String cs) {
        if (cs == null || cs.isBlank() || !Charset.isSupported(cs)) {
            throw new IllegalArgumentException("Charset invalide ou non supporté: " + cs);
        }
    }

    private static String normalize(String p) {
        if (p == null || p.isBlank()) return Paths.get(".").toAbsolutePath().normalize().toString();
        return Paths.get(p).toAbsolutePath().normalize().toString() + (p.endsWith("/") ? "/" : "");
    }
}
