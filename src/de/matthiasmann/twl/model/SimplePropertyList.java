/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twl.model;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple property list property. Used to create sub properties in the PropertySheet.
 *
 * @author Matthias Mann
 */
public class SimplePropertyList extends AbstractProperty<PropertyList> implements PropertyList {

    private final String name;
    private final ArrayList<Property<?>> properties;

    public SimplePropertyList(String name) {
        this.name = name;
        this.properties = new ArrayList<Property<?>>();
    }

    public SimplePropertyList(String name, Property<?>... properties) {
        this(name);
        this.properties.addAll(Arrays.asList(properties));
    }

    public String getName() {
        return name;
    }

    public boolean isReadOnly() {
        return true;
    }

    public PropertyList getValue() {
        return this;
    }

    public void setValue(PropertyList value) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported");
    }

    public Class<PropertyList> getType() {
        return PropertyList.class;
    }

    public int getNumProperties() {
        return properties.size();
    }

    public Property<?> getProperty(int idx) {
        return properties.get(idx);
    }

    public void addProperty(Property<?> property) {
        properties.add(property);
        fireValueChangedCallback();
    }

    public void addProperty(int idx, Property<?> property) {
        properties.add(idx, property);
        fireValueChangedCallback();
    }

    public void removeProperty(int idx) {
        properties.remove(idx);
        fireValueChangedCallback();
    }
}
