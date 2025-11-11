/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.kernel;

/**
 *
 * @author eddy
 */
public class Config {

    public Server server = new Server();
    public PathsCfg path = new PathsCfg();

    public static class Server {

        public int port = 8080;
        public String defaultCharset = "utf-8";
    }

    public static class PathsCfg {

        public String root_path = "./root/";
        public String plugins_path = "./plugins/";
    }
}
