/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twl.textarea;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the styles which should be applied to a certain element.
 * 
 * @author Matthias Mann
 */
public class Style {

    private final Style parent;
    private final String classRef;
    private final Object[] values;
    private final Style altDefaults;

    /**
     * Creates an empty Style without a parent, class reference and no attributes
     */
    public Style() {
        this(null, null);
    }

    /**
     * Creates an Style with the given parent and class reference.
     *
     * @param parent the parent style. Can be null.
     * @param classRef the class reference. Can be null.
     */
    public Style(Style parent, String classRef) {
        this.parent = parent;
        this.classRef = classRef;
        this.values = new Object[StyleAttribute.getNumAttributes()];
        this.altDefaults = null;
    }

    /**
     * Creates an Style with the given parent and class reference and copies the
     * given attributes.
     *
     * @param parent the parent style. Can be null.
     * @param classRef the class reference. Can be null.
     * @param values a map with attributes for this Style. Can be null.
     */
    public Style(Style parent, String classRef, Map<StyleAttribute<?>, Object> values) {
        this(parent, classRef);
        
        if(values != null) {
            putAll(values);
        }
    }

    private Style(Style parent, String classRef, Object[] values, Style altDefaults) {
        this.parent = parent;
        this.classRef = classRef;
        this.values = values.clone();
        this.altDefaults = altDefaults;
    }

    /**
     * Resolves the Style in which the specified attribute is defined.
     *
     * If a attribute does not cascade then this method does nothing.
     *
     * If a StyleClassResolverm is specified then this method will treat class
     * Styles referenced by classRef as if they are part of a Style in this chain.
     * 
     * @param attribute The attribute to lookup.
     * @param resolver A StyleClassResolver to resolve the classRef. Can be null.
     * @return The Style which defined the specified attribute, will never return null.
     * @see StyleAttribute#isInherited()
     * @see #getParent()
     */
    public Style resolve(StyleAttribute<?> attribute, StyleClassResolver resolver) {
        if(!attribute.isInherited()) {
            return this;
        }

        return doResolve(this, attribute.ordinal(), resolver);
    }

    private static Style doResolve(Style style, int ord, StyleClassResolver resolver) {
        for(;;) {
            if(style.values[ord] != null) {
                return style;
            }
            if(resolver != null && style.classRef != null && style.classRef.length() > 0) {
                Style classStyle = resolver.resolve(style.classRef);
                if(classStyle != null && classStyle.values[ord] != null) {
                    // return main style here because class style has no parent chain
                    return style;
                }
            }
            if(style.parent == null) {
                return style;
            }
            style = style.parent;
        }

    }

    /**
     * Retrives the value of the specified attribute without resolving the style.
     *
     * If the attribute is not set in this Style and a StyleClassResolver was
     * specified then the Style referenced by the classRef will be used instead.
     *
     * @param <V> The data type of the attribute
     * @param attribute The attribute to lookup.
     * @param resolver A StyleClassResolver to resolve the classRef. Can be null.
     * @return The attribute value if it was set, or the default value of the attribute.
     */
    public<V> V getNoResolve(StyleAttribute<V> attribute, StyleClassResolver resolver) {
        Object value = values[attribute.ordinal()];
        if(value == null && resolver != null && classRef != null && classRef.length() > 0) {
            Style classStyle = resolver.resolve(classRef);
            if(classStyle != null) {
                value = classStyle.values[attribute.ordinal()];
            }
        }
        if(value == null) {
            if(altDefaults != null) {
                return altDefaults.get(attribute, resolver);
            } else {
                return attribute.getDefaultValue();
            }
        }
        return attribute.getDataType().cast(value);
    }

    /**
     * Retrives the value of the specified attribute from the resolved style.
     *
     * @param <V> The data type of the attribute
     * @param attribute The attribute to lookup.
     * @param resolver A StyleClassResolver to resolve the classRef. Can be null.
     * @return The attribute value if it was set, or the default value of the attribute.
     * @see #resolve(de.matthiasmann.twl.textarea.StyleAttribute, de.matthiasmann.twl.textarea.StyleClassResolver)
     * @see #getNoResolve(de.matthiasmann.twl.textarea.StyleAttribute, de.matthiasmann.twl.textarea.StyleClassResolver)
     */
    public<V> V get(StyleAttribute<V> attribute, StyleClassResolver resolver) {
        return resolve(attribute, resolver).getNoResolve(attribute, resolver);
    }

    /**
     * Returns the parent of this Style or null. The parent is used to lookup
     * attributes which can be inherited and are not specified in this Style.
     * 
     * @return the parent of this Style or null.
     * @see StyleAttribute#isInherited()
     */
    public Style getParent() {
        return parent;
    }

    /**
     * Returns the class reference for this Style or null. A class style is used
     * to lookup attributes which are not set in this Style.
     * 
     * @return the class reference for this Style or null.
     */
    public String getClassRef() {
        return classRef;
    }

    /**
     * Creates a copy of this Style and sets the specified attributes.
     *
     * It is possible to set a attribute to null to 'unset' it.
     *
     * @param values The attributes to set in the new Style.
     * @return a new Style with the same parent, classRef and modified attributes.
     */
    public Style with(Map<StyleAttribute<?>, Object> values) {
        Style newStyle = new Style(parent, classRef, this.values, this.altDefaults);
        newStyle.putAll(values);
        return newStyle;
    }

    /**
     * Creates a copy of this Style and sets the specified attributes.
     *
     * It is possible to set a attribute to null to 'unset' it.
     * 
     * @param <V> The data type of the attribute
     * @param attribute The attribute to set.
     * @param value The new value of that attribute. Can be null.
     * @return a new Style with the same parent, classRef and modified attribute.
     */
    public<V> Style with(StyleAttribute<V> attribute, V value) {
        Style newStyle = new Style(parent, classRef, this.values, this.altDefaults);
        newStyle.put(attribute, value);
        return newStyle;
    }

    /**
     * Creates a copy of this Style with alternate default values.
     * This will replace existing alternate defaults with the new ones.
     *
     * @param altDefaults The new alternate defaults. Can be null.
     * @return a new Style with the same parent, classRef, values and the specified alternate defaults
     */
    public Style withAlternateDefaults(Style altDefaults) {
        return new Style(parent, classRef, this.values, altDefaults);
    }

    protected void put(StyleAttribute<?> attribute, Object value) {
        if(attribute == null) {
            throw new IllegalArgumentException("attribute is null");
        }
        if(value != null && !attribute.getDataType().isInstance(value)) {
            throw new IllegalArgumentException("value is a " + value.getClass() +
                    " but must be a " + attribute.getDataType());
        }

        values[attribute.ordinal()] = value;
    }

    protected void putAll(Map<StyleAttribute<?>, Object> values) {
        for(Map.Entry<StyleAttribute<?>, Object> e : values.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Creates a map which will contain all set attributes of this Style.
     * Changes to that map have no impact on this Style.
     * @return a map which will contain all set attributes of this Style.
     */
    public Map<StyleAttribute<?>, Object> toMap() {
        HashMap<StyleAttribute<?>, Object> result = new HashMap<StyleAttribute<?>, Object>();
        for(int ord=0 ; ord<values.length ; ord++) {
            Object value = values[ord];
            if(value != null) {
                result.put(StyleAttribute.getAttribute(ord), value);
            }
        }
        return result;
    }
}
