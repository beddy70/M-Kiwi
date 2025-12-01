/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.kernel;

import java.io.IOException;
import java.nio.file.Path;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.client.PageManager;
import org.somanybits.minitel.server.MModulesManager;
import org.somanybits.minitel.util.SSLHelper;

/**
 * Noyau central de l'application Minitel-Serveur.
 * <p>
 * Cette classe implémente le pattern Singleton et fournit un point d'accès
 * centralisé à tous les composants principaux du système :
 * </p>
 * <ul>
 *   <li>{@link LogManager} - Gestion des logs</li>
 *   <li>{@link MModulesManager} - Gestion des modules dynamiques</li>
 *   <li>{@link Config} - Configuration de l'application</li>
 *   <li>{@link PageManager} - Gestion de la navigation et des pages VTML</li>
 * </ul>
 * 
 * <h2>Exemple d'utilisation</h2>
 * <pre>{@code
 * Kernel kernel = Kernel.getInstance();
 * Config config = kernel.getConfig();
 * int port = config.server.port;
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see Config
 * @see PageManager
 */
public class Kernel {

    private static Kernel kernel;
    private LogManager logmgr;
    private MModulesManager mmodmgr;
    private Config cfg;
    private PageManager pageManager;

    private Kernel() throws IOException {

        logmgr = new LogManager();
        mmodmgr = new MModulesManager();

        cfg = ConfigLoader.load(Path.of("./config.json"));
        
        // Désactiver la vérification SSL si configuré (pour le développement)
        if (cfg.ssl.trustAllCertificates) {
            SSLHelper.disableCertificateValidation();
        }
        
        // Initialiser le PageManager avec la config
        pageManager = new PageManager("localhost", cfg.server.port);

    }

    /**
     * Retourne l'instance unique du Kernel (Singleton).
     * <p>
     * Si le Kernel n'existe pas encore, il est créé et initialisé
     * avec la configuration depuis {@code config.json}.
     * </p>
     * 
     * @return L'instance unique du Kernel
     * @throws IOException Si le fichier de configuration ne peut pas être lu
     */
    public static Kernel getInstance() throws IOException {
        if (Kernel.kernel == null) {
            Kernel.kernel = new Kernel();
        }
        return kernel;
    }

    /**
     * Retourne le gestionnaire de logs.
     * 
     * @return Le {@link LogManager} pour enregistrer des messages de log
     */
    public LogManager getLogManager() {
        return this.logmgr;
    }

    /**
     * Retourne la configuration de l'application.
     * 
     * @return L'objet {@link Config} contenant tous les paramètres
     */
    public Config getConfig() {
        return cfg;
    }

    /**
     * Retourne le gestionnaire de modules dynamiques (MModules).
     * 
     * @return Le {@link MModulesManager} pour charger et exécuter des plugins
     */
    public MModulesManager getMModulesManager() {
        return mmodmgr;
    }

    /**
     * Retourne le gestionnaire de pages et de navigation.
     * 
     * @return Le {@link PageManager} pour naviguer entre les pages VTML
     */
    public PageManager getPageManager() {
        return pageManager;
    }

}
