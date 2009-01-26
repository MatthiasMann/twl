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
 * UI events for Mouse and Keyboard.
 *
 * @author MannMat
 */
public abstract class Event {

    public enum Type {
        MOUSE_ENTERED(true, false),
        MOUSE_MOVED(true, false),
        MOUSE_BTNDOWN(true, false),
        MOUSE_BTNUP(true, false),
        MOUSE_CLICKED(true, false),
        MOUSE_DOUBLE_CLICKED(true, false),
        MOUSE_DRAGED(true, false),
        MOUSE_EXITED(true, false),
        MOUSE_WHEEL(true, false),
        KEY_PRESSED(false, true),
        KEY_RELEASED(false, true),
        CHAR_TYPED(false, true),
        POPUP_OPENED(false, false),
        POPUP_CLOSED(false, false);
        
        final boolean isMouseEvent;
        final boolean isKeyEvent;
        Type(boolean isMouseEvent, boolean isKeyEvent) {
            this.isMouseEvent = isMouseEvent;
            this.isKeyEvent = isKeyEvent;
        }
    };
    
    public static final int MODIFIER_LSHIFT = 1;
    public static final int MODIFIER_LMETA = 2;
    public static final int MODIFIER_LCTRL = 4;
    public static final int MODIFIER_RSHIFT = 8;
    public static final int MODIFIER_RMETA = 16;
    public static final int MODIFIER_RCTRL = 32;
    public static final int MODIFIER_LBUTTON = 64;
    public static final int MODIFIER_RBUTTON = 128;
    public static final int MODIFIER_MBUTTON = 256;
    
    public static final int MODIFIER_SHIFT = MODIFIER_LSHIFT | MODIFIER_RSHIFT;
    public static final int MODIFIER_META = MODIFIER_LMETA | MODIFIER_RMETA;
    public static final int MODIFIER_CTRL = MODIFIER_LCTRL | MODIFIER_RCTRL;
    
    public static final int MOUSE_LBUTTON = 0;
    public static final int MOUSE_RBUTTON = 1;
    public static final int MOUSE_MBUTTON = 2;
    
    public abstract Type getType();
    
    public final boolean isMouseEvent() {
        return getType().isMouseEvent;
    }

    public final boolean isKeyEvent() {
        return getType().isKeyEvent;
    }
    
    public abstract boolean isMouseDragEvent();

    public abstract boolean isMouseDragEnd();
    
    public abstract int getMouseX();
    
    public abstract int getMouseY();
    
    public abstract int getMouseButton();
    
    public abstract int getMouseWheelDelta();
    
    public abstract Event createSubEvent(Type newType);
    
    /**
     * @see org.lwjgl.input.Keyboard
     * @return the key code
     */
    public abstract int getKeyCode();
    
    public abstract char getKeyChar();
    
    public abstract boolean isKeyRepeated();
    
    public abstract int getModifiers();
}
