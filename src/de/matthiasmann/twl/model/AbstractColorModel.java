/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twl.model;

/**
 *
 * @author Matthias Mann
 */
public abstract class AbstractColorModel implements ColorModel {

    private final String modelName;
    private final String[] names;

    public AbstractColorModel(String modelName, String ... names) {
        this.modelName = modelName;
        this.names = names;
    }

    public String getComponentName(int component) {
        return names[component];
    }

    public String getModelName() {
        return modelName;
    }

    public int getNumComponents() {
        return names.length;
    }

    public float getMinValue(int component) {
        return 0f;
    }

}
