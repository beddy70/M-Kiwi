/*
 * Minitel-Serveur - Serveur Minitel moderne
 * Copyright (c) 2024 Eddy Briere
 */
package org.somanybits.minitel.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.kernel.Kernel;

/**
 * Gestionnaire de modules dynamiques (MModules).
 * <p>
 * Cette classe charge et exécute les plugins MModule depuis le répertoire
 * {@code plugins/mmodules/}. Les MModules sont des fichiers JAR contenant
 * des classes qui implémentent l'interface {@link MModule}.
 * </p>
 * 
 * <h2>Fonctionnement</h2>
 * <ol>
 *   <li>Au démarrage, tous les JAR dans {@code plugins/mmodules/} sont chargés</li>
 *   <li>Quand une URL {@code *.mod} est demandée, le module correspondant est instancié</li>
 *   <li>La méthode {@link MModule#getResponse()} génère le contenu VTML</li>
 * </ol>
 * 
 * <h2>Exemple d'URL</h2>
 * <pre>{@code
 * http://localhost:8080/ServerStatus.mod?param1=value1
 * }</pre>
 * 
 * @author Eddy Briere
 * @version 0.3
 * @see MModule
 * @see ModelMModule
 */
public class MModulesManager {

    private String mmodulePath;
    private URL[] urls;

    private LogManager logmgr;

    public String loadMModules(String modname, HttpExchange ex, Path docRoot, HashMap paramlist) throws MalformedURLException {

//        File classesDir = new File(MMODULES_PATH);              // racine des .class
//        URL url = classesDir.toURI().toURL();
        try (URLClassLoader loader = new URLClassLoader(urls,
                Thread.currentThread().getContextClassLoader())) {

            System.out.println(this.mmodulePath + modname);

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
            //System.getLogger(StaticFileServer.class.getName()).log(System.Logger.Level.ERROR, (String) null, e);
            
        }
        return null;
    }

    public void loadAllMModulePlugins() throws IOException {

        this.logmgr = Kernel.getInstance().getLogManager();
        this.mmodulePath = Kernel.getInstance().getConfig().path.plugins_path + "/mmodules/";

        File dir = new File(this.mmodulePath);  // contient *.jar
        File[] jars = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));

        logmgr.addLog(LogManager.ANSI_BOLD_YELLOW + "Loading mmodules");

        for (File jar : jars) {
            logmgr.addLog(LogManager.ANSI_WHITE + "\t" + Arrays.toString(jars));
        }
        logmgr.addLog(LogManager.ANSI_BOLD_YELLOW + "All mmodules loaded");
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
