package org.somanybits.minitel.components.vtml;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Moteur JavaScript pour l'exécution de scripts dans les pages VTML
 * Utilise directement l'API Mozilla Rhino
 *
 * @author eddy
 */
public class VTMLScriptEngine {

    private static VTMLScriptEngine instance;
    private Scriptable scope;
    private boolean available = false;

    // Liste blanche des classes Java autorisées
    private static final java.util.Set<String> ALLOWED_CLASSES = java.util.Set.of(
        // Classes Minitel
        "org.somanybits.minitel.kernel.Kernel",
        "org.somanybits.minitel.kernel.Config",
        "org.somanybits.minitel.GetTeletelCode",
        "org.somanybits.minitel.Teletel",
        // Classes Java de base (lecture seule)
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.lang.Math",
        "java.util.Date"
    );

    private VTMLScriptEngine() {
        try {
            // Initialiser Rhino
            Context cx = Context.enter();
            
            // Activer le ClassShutter pour filtrer les classes accessibles
            cx.setClassShutter(new ClassShutter() {
                @Override
                public boolean visibleToScripts(String className) {
                    // Autoriser seulement les classes dans la liste blanche
                    return ALLOWED_CLASSES.contains(className);
                }
            });
            
            scope = cx.initStandardObjects();
            
            // Exposer des classes Java utiles au JavaScript
            exposeJavaClasses(cx);
            
            available = true;
            System.out.println("✅ Moteur JavaScript Rhino initialisé (mode sécurisé)");
            Context.exit();
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation Rhino: " + e.getMessage());
            available = false;
        }
    }
    
    /**
     * Expose des classes Java au contexte JavaScript
     */
    private void exposeJavaClasses(Context cx) {
        try {
            // Définir les classes Java accessibles via JavaScript
            String initScript = 
                "var Kernel = Packages.org.somanybits.minitel.kernel.Kernel;\n" +
                "var GetTeletelCode = Packages.org.somanybits.minitel.GetTeletelCode;\n" +
                "var Teletel = Packages.org.somanybits.minitel.Teletel;\n" +
                "var Config = Packages.org.somanybits.minitel.kernel.Config;\n" +
                "\n" +
                "// Helper pour importer une classe\n" +
                "function importClass(className) {\n" +
                "  return Packages[className];\n" +
                "}\n" +
                "\n" +
                "// Helper pour obtenir la config\n" +
                "function getConfig() {\n" +
                "  return Kernel.getIntance().getConfig();\n" +
                "}\n";
            
            cx.evaluateString(scope, initScript, "init", 1, null);
            
        } catch (Exception e) {
            System.err.println("Erreur exposition classes Java: " + e.getMessage());
        }
    }

    public static VTMLScriptEngine getInstance() {
        if (instance == null) {
            instance = new VTMLScriptEngine();
        }
        return instance;
    }

    /**
     * Vérifie si le moteur JavaScript est disponible
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Définit une variable accessible depuis les scripts
     */
    public void setVariable(String name, Object value) {
        if (scope != null) {
            Context cx = Context.enter();
            try {
                Object wrappedValue = Context.javaToJS(value, scope);
                ScriptableObject.putProperty(scope, name, wrappedValue);
            } finally {
                Context.exit();
            }
        }
    }

    /**
     * Récupère une variable depuis le contexte des scripts
     */
    public Object getVariable(String name) {
        if (scope != null) {
            Context cx = Context.enter();
            try {
                Object value = ScriptableObject.getProperty(scope, name);
                if (value == Scriptable.NOT_FOUND) {
                    return null;
                }
                return Context.jsToJava(value, Object.class);
            } finally {
                Context.exit();
            }
        }
        return null;
    }

    /**
     * Exécute un script JavaScript et retourne le résultat
     */
    public Object execute(String script) throws Exception {
        if (!available) {
            throw new Exception("Moteur JavaScript non disponible");
        }
        
        Context cx = Context.enter();
        try {
            Object result = cx.evaluateString(scope, script, "script", 1, null);
            return Context.jsToJava(result, Object.class);
        } finally {
            Context.exit();
        }
    }

    /**
     * Exécute un script et retourne le résultat sous forme de String
     */
    public String executeAsString(String script) throws Exception {
        Object result = execute(script);
        return result != null ? result.toString() : "";
    }

    /**
     * Réinitialise le contexte (nouveau scope)
     */
    public void resetContext() {
        Context cx = Context.enter();
        try {
            scope = cx.initStandardObjects();
        } finally {
            Context.exit();
        }
    }
}
