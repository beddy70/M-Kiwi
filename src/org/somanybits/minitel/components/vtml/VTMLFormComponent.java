package org.somanybits.minitel.components.vtml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.somanybits.minitel.GetTeletelCode;
import org.somanybits.minitel.components.MComponent;
import org.somanybits.minitel.components.ModelMComponent;

/**
 * Composant VTML représentant le tag <form>
 * Conteneur pour les champs de saisie <input>
 * 
 * Exemple:
 * <form action="search.vtml" method="GET" left="2" top="5">
 *   <input name="query" left="0" top="0" width="20">
 *   <input name="ville" left="0" top="2" width="15">
 * </form>
 *
 * @author eddy
 */
public class VTMLFormComponent extends ModelMComponent {

    private String action;
    private String method = "GET";
    private List<VTMLInputComponent> inputs = new ArrayList<>();
    
    // Système de focus pour les inputs
    private List<VTMLInputComponent> focusableInputs = new ArrayList<>();
    private int currentInputIndex = 0;

    public VTMLFormComponent() {
        super();
    }

    public VTMLFormComponent(String action, String method, int left, int top, int width, int height) {
        super();
        this.action = action;
        this.method = method != null ? method.toUpperCase() : "GET";
        setX(left);
        setY(top);
        setWidth(width);
        setHeight(height);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Récupère tous les inputs du formulaire
     */
    public List<VTMLInputComponent> getInputs() {
        inputs.clear();
        for (MComponent child : getChilds()) {
            if (child instanceof VTMLInputComponent) {
                inputs.add((VTMLInputComponent) child);
            }
        }
        return inputs;
    }

    /**
     * Construit l'URL avec les paramètres GET
     */
    public String buildActionUrl() {
        StringBuilder url = new StringBuilder(action);
        List<VTMLInputComponent> formInputs = getInputs();
        
        if (!formInputs.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (VTMLInputComponent input : formInputs) {
                if (!first) {
                    url.append("&");
                }
                url.append(input.getName());
                url.append("=");
                url.append(input.getValue() != null ? input.getValue() : "");
                first = false;
            }
        }
        return url.toString();
    }

    // ========== SYSTÈME DE FOCUS POUR LES INPUTS ==========
    
    /**
     * Collecte tous les inputs du formulaire
     */
    public void collectInputs() {
        focusableInputs.clear();
        collectInputsRecursive(this);
        System.out.println("📋 Form: " + focusableInputs.size() + " inputs trouvés");
    }
    
    private void collectInputsRecursive(MComponent component) {
        if (component instanceof VTMLInputComponent) {
            focusableInputs.add((VTMLInputComponent) component);
        }
        if (component instanceof ModelMComponent) {
            for (MComponent child : ((ModelMComponent) component).getChilds()) {
                collectInputsRecursive(child);
            }
        }
    }
    
    /**
     * Retourne la liste des inputs
     */
    public List<VTMLInputComponent> getFocusableInputs() {
        if (focusableInputs.isEmpty()) {
            collectInputs();
        }
        return focusableInputs;
    }
    
    /**
     * Retourne l'input qui a actuellement le focus
     */
    public VTMLInputComponent getCurrentInput() {
        if (focusableInputs.isEmpty()) {
            collectInputs();
        }
        if (focusableInputs.isEmpty()) {
            return null;
        }
        return focusableInputs.get(currentInputIndex);
    }
    
    /**
     * Passe à l'input suivant (cycle)
     * @return Le nouvel input focusé
     */
    public VTMLInputComponent nextInput() {
        if (focusableInputs.isEmpty()) {
            collectInputs();
        }
        if (focusableInputs.isEmpty()) {
            return null;
        }
        currentInputIndex = (currentInputIndex + 1) % focusableInputs.size();
        System.out.println("🔄 Focus input -> " + currentInputIndex + "/" + focusableInputs.size());
        return getCurrentInput();
    }
    
    /**
     * Retourne l'index de l'input actuel
     */
    public int getCurrentInputIndex() {
        return currentInputIndex;
    }
    
    /**
     * Définit le focus sur un input spécifique
     */
    public void setInputIndex(int index) {
        if (index >= 0 && index < focusableInputs.size()) {
            currentInputIndex = index;
        }
    }
    
    /**
     * Vérifie si le formulaire a des inputs
     */
    public boolean hasInputs() {
        if (focusableInputs.isEmpty()) {
            collectInputs();
        }
        return !focusableInputs.isEmpty();
    }

    @Override
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream formdata = new ByteArrayOutputStream();

            // Positionner le curseur au début du formulaire
            formdata.write(GetTeletelCode.setCursor(getX(), getY()));

            // Rendre les enfants (inputs et autres)
            for (MComponent child : getChilds()) {
                formdata.write(child.getBytes());
            }
            
            // Collecter les inputs après le rendu
            collectInputs();

            return formdata.toByteArray();
        } catch (IOException ex) {
            System.err.println("Erreur VTMLFormComponent: " + ex.getMessage());
            return new byte[0];
        }
    }
}
