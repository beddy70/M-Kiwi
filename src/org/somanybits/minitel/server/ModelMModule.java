/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.server;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Path;
import java.util.HashMap;

/**
 *
 * @author eddy
 */
abstract public class ModelMModule implements MModule{

    protected HashMap<String, String> params = null;
    protected HttpExchange ex =null;
    protected Path docRoot;
    
    public ModelMModule(HashMap params,HttpExchange ex, Path docRoot) {
        this.params=params;
        this.ex=ex;
        this.docRoot=docRoot;
    }
    
    
}
