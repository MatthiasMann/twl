/*
 * Copyright (c) 2008, Matthias Mann
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

/**
 * A special toggle button which changes the open/closed state of AnimatedWindow
 *
 * @author Matthias Mann
 */
public class DialogButton extends ToggleButton {

    private final AnimatedWindow window;

    protected AnimatedWindow[] exclusiveList;
    
    public DialogButton(AnimatedWindow window) {
        this.window = window;
        
        window.addCallback(new Runnable() {
            public void run() {
                windowStateChanged();
            }
        });
        
        addCallback(new Runnable() {
            public void run() {
                buttonAction();
            }
        });
    }

    public AnimatedWindow[] getExclusiveList() {
        return exclusiveList;
    }

    public void setExclusiveList(AnimatedWindow[] exclusiveList) {
        this.exclusiveList = exclusiveList;
    }

    protected void buttonAction() {
        if(exclusiveList != null && isActive()) {
            for(AnimatedWindow w : exclusiveList) {
                if(w != window) {
                    w.setState(false);
                }
            }
        }
        
        window.setState(isActive());
    }
    
    protected void windowStateChanged() {
        if(window.isOpen() || window.isOpening()) {
            setActive(true);
            if(hasKeyboardFocus()) {
                window.requestKeyboardFocus();
            }
        } else {
            setActive(false);
            if(window.hasKeyboardFocus()) {
                requestKeyboardFocus();
            }
        }
    }
    
}
