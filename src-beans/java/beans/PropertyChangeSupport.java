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
package java.beans;

import de.matthiasmann.twl.utils.CallbackSupport;

/**
 *
 * @author Matthias Mann
 */
public class PropertyChangeSupport {

    private final Object source;
    private PropertyChangeListener[] listeners;

    public PropertyChangeSupport(Object source) {
        this.source = source;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        listeners = CallbackSupport.addCallbackToList(listeners, pcl, PropertyChangeListener.class);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        listeners = CallbackSupport.removeCallbackFromList(listeners, pcl);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
        addPropertyChangeListener(new Filter(propertyName, pcl));
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
        if(listeners != null) {
            for(int i=0,n=listeners.length ; i<n ; i++) {
                PropertyChangeListener l = listeners[i];
                if(l instanceof Filter && ((Filter)l).propertyName.equals(propertyName)) {
                    listeners = CallbackSupport.removeCallbackFromList(listeners, i);
                    return;
                }
            }
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        PropertyChangeListener[] listenersCopy = this.listeners;
        if(listenersCopy != null) {
            for(PropertyChangeListener l : listenersCopy) {
                l.propertyChange(event);
            }
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if(oldValue != newValue) {
            firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if(oldValue != newValue) {
            firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
    }

    static class Filter implements PropertyChangeListener {
        final String propertyName;
        final PropertyChangeListener listener;

        public Filter(String propertyName, PropertyChangeListener listener) {
            this.propertyName = propertyName;
            this.listener = listener;
        }

        public void propertyChange(PropertyChangeEvent event) {
            if(propertyName.equals(event.getPropertyName())) {
                listener.propertyChange(event);
            }
        }
    }
}
