/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import org.somanybits.minitel.kernel.Kernel;

/**
 * Classe de base abstraite pour créer des MModules.
 * <p>
 * Les MModules sont des plugins dynamiques qui génèrent du contenu VTML.
 * Pour créer un nouveau module, étendez cette classe et implémentez
 * les méthodes de l'interface {@link MModule}.
 * </p>
 * 
 * <h2>Exemple de MModule</h2>
 * <pre>{@code
 * public class MonModule extends ModelMModule {
 *     
 *     public MonModule(HashMap params, HttpExchange ex, Path docRoot) {
 *         super(params, ex, docRoot);
 *     }
 *     
 *     @Override
 *     public String getResponse() {
 *         String nom = params.get("nom");
 *         return "<minitel><div><row>Bonjour " + nom + "</row></div></minitel>";
 *     }
 *     
 *     @Override
 *     public String getVersion() { return "1.0"; }
 *     
 *     @Override
 *     public String getContentType() { return "text/plain; charset=UTF-8"; }
 * }
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.4
 * @see MModule
 * @see MModulesManager
 */
public abstract class ModelMModule implements MModule {

    /** Paramètres GET de la requête HTTP */
    protected HashMap<String, String> params = null;
    /** Objet HttpExchange pour accéder aux détails de la requête */
    protected HttpExchange ex = null;
    /** Chemin racine du serveur de documents */
    protected Path docRoot;
    /** Chemin vers le fichier de configuration JSON du module */
    protected Path configPath;
    /** Configuration JSON chargée du module (peut être null si pas de fichier) */
    protected JsonNode moduleConfig;

    /**
     * Constructeur appelé par le MModulesManager lors du chargement du module.
     * 
     * @param params Paramètres GET de l'URL (peut être null)
     * @param ex Objet HttpExchange de la requête
     * @param docRoot Chemin racine des documents VTML
     */
    public ModelMModule(HashMap params, HttpExchange ex, Path docRoot) {
        this.params = params;
        this.ex = ex;
        this.docRoot = docRoot;
    }
    
    /**
     * Lit le fichier de configuration JSON du module.
     * <p>
     * Le fichier est recherché dans le répertoire défini par {@code mmodules_config_path}
     * dans config.json, avec le nom {@code NomDuModule.json}.
     * </p>
     * <p>
     * Exemple: Pour {@code ServerScore}, le fichier sera {@code mmodules_config/ServerScore.json}
     * </p>
     * 
     * @return Le JsonNode contenant la configuration, ou null si le fichier n'existe pas
     */
    protected JsonNode readConfig() {
        try {
            String configDir = Kernel.getInstance().getConfig().path.mmodules_config_path;
            String moduleName = this.getClass().getSimpleName();
            this.configPath = Paths.get(configDir, moduleName + ".json");
            
            File configFile = configPath.toFile();
            System.out.println("[MModule] Config path: " + configFile.getAbsolutePath());
            System.out.println("[MModule] File exists: " + configFile.exists());
            
            if (configFile.exists() && configFile.isFile()) {
                ObjectMapper mapper = new ObjectMapper();
                this.moduleConfig = mapper.readTree(configFile);
                System.out.println("[MModule] Config loaded: " + this.moduleConfig);
                return this.moduleConfig;
            } else {
                System.out.println("[MModule] Config file not found!");
            }
        } catch (IOException e) {
            System.err.println("[MModule] Erreur lors du chargement de la configuration: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Retourne le chemin vers le fichier de configuration JSON du module.
     * 
     * @return Le chemin du fichier de configuration (disponible après appel à readConfig)
     */
    public Path getConfigPath() {
        return this.configPath;
    }
}
