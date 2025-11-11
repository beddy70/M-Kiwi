/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.kernel;

import java.io.IOException;
import java.nio.file.Path;
import org.somanybits.log.LogManager;
import org.somanybits.minitel.server.MModulesManager;

/**
 *
 * @author eddy
 */
public class Kernel {

    private static Kernel kernel;
    private LogManager logmgr;
    private MModulesManager mmodmgr;
    private Config cfg;

    private Kernel() throws IOException {

        logmgr = new LogManager();
        mmodmgr = new MModulesManager();

        cfg = ConfigLoader.load(Path.of("./config.json"));

    }

    static public Kernel getIntance() throws IOException {
        if (Kernel.kernel == null) {
            Kernel.kernel = new Kernel();
        }
        return kernel;
    }

    public LogManager getLogManager() {
        return this.logmgr;
    }

    public Config getConfig() {
        return cfg;
    }

    public MModulesManager getMModulesManager() {
        return mmodmgr;
    }

}
