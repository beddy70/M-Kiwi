/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.server;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Path;
import java.util.HashMap;

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
 * @version 0.3
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
}
