/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.server;

/**
 * Interface pour les modules dynamiques MModule.
 * <p>
 * Les MModules peuvent avoir leur propre fichier de configuration JSON
 * situé dans le répertoire défini par {@code mmodules_config_path} dans config.json.
 * Le fichier de configuration doit être nommé {@code NomDuModule.json}.
 * Utilisez {@code readConfig()} dans {@link ModelMModule} pour charger la configuration.
 * </p>
 * 
 * @author Eddy Briere
 * @version 0.4
 * @see ModelMModule
 */
interface MModule {
    
    /**
     * Génère la réponse VTML du module.
     * @return Le contenu VTML à renvoyer au client
     */
    public String getResponse();
    
    /**
     * Retourne la version du module.
     * @return La version sous forme de chaîne (ex: "1.0")
     */
    public String getVersion();
    
    /**
     * Retourne le type de contenu de la réponse.
     * @return Le Content-Type HTTP (ex: "text/plain; charset=UTF-8")
     */
    public String getContentType();

}
