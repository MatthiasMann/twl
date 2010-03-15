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
package de.matthiasmann.twl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Matthias Mann
 */
public abstract class MenuElement {

    private String name;
    private String theme;
    private boolean enabled = true;
    private PropertyChangeSupport pcs;

    public MenuElement() {
    }

    public MenuElement(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        firePropertyChange("name", oldName, name);
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        String oldTheme = this.theme;
        this.theme = theme;
        firePropertyChange("theme", oldTheme, theme);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean oldEnabled = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldEnabled, enabled);
    }

    protected abstract Widget createMenuWidget(MenuManager mm, int level);

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if(pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if(pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if(pcs != null) {
            pcs.removePropertyChangeListener(propertyName, listener);
        }
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if(pcs != null) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if(pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if(pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if(pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    void setWidgetTheme(Widget w) {
        if(theme != null) {
            w.setTheme(theme);
        }
    }
}
