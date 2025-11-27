package org.somanybits.minitel.components.vtml;

import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <script>
 * Exécute du JavaScript côté serveur et peut générer du contenu
 *
 * Exemple d'utilisation:
 * <script>
 *   var message = "Bonjour Minitel!";
 *   output = message.toUpperCase();
 * </script>
 *
 * La variable 'output' sera affichée si définie.
 *
 * @author eddy
 */
public class VTMLScriptComponent extends ModelMComponent {

    private String scriptContent;

    public VTMLScriptComponent() {
        super();
    }

    public VTMLScriptComponent(String scriptContent) {
        super();
        this.scriptContent = scriptContent;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    @Override
    public byte[] getBytes() {
        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            return new byte[0];
        }

        VTMLScriptEngine engine = VTMLScriptEngine.getInstance();
        
        if (!engine.isAvailable()) {
            return "[SCRIPT ERROR: No JavaScript engine available]".getBytes();
        }

        try {
            // Exécuter le script
            engine.execute(scriptContent);
            
            // Récupérer la variable 'output' si elle existe
            Object output = engine.getVariable("output");
            if (output != null) {
                return output.toString().getBytes();
            }
            
            // Pas de sortie
            return new byte[0];
            
        } catch (Exception e) {
            System.err.println("Erreur JavaScript: " + e.getMessage());
            return ("[SCRIPT ERROR: " + e.getMessage() + "]").getBytes();
        }
    }
}
