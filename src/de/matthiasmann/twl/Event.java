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
        MOUSE_DRAGED(true, false),
        MOUSE_EXITED(true, false),
        MOUSE_WHEEL(true, false),
        KEY_PRESSED(false, true),
        KEY_RELEASED(false, true),
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

    /**
     * One of the shift keys is pressed
     * @see #getModifiers()
     */
    public static final int MODIFIER_SHIFT = MODIFIER_LSHIFT | MODIFIER_RSHIFT;

    /**
     * One of the meta keys (ALT on Windows) is pressed
     * @see #getModifiers()
     */
    public static final int MODIFIER_META = MODIFIER_LMETA | MODIFIER_RMETA;

    /**
     * One of the control keys is pressed
     * @see #getModifiers()
     */
    public static final int MODIFIER_CTRL = MODIFIER_LCTRL | MODIFIER_RCTRL;

    /**
     * One of the mouse buttons is pressed
     * @see #getModifiers()
     */
    public static final int MODIFIER_BUTTON = MODIFIER_LBUTTON | MODIFIER_MBUTTON | MODIFIER_RBUTTON;

    /**
     * Left mouse button - this is the primary mouse button
     * @see #getMouseButton()
     */
    public static final int MOUSE_LBUTTON = 0;

    /**
     * Right mouse button - this is for context menus
     * @see #getMouseButton()
     */
    public static final int MOUSE_RBUTTON = 1;

    /**
     * Middle mouse button
     * @see #getMouseButton()
     */
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

    /**
     * The mouse button. Only valid for MOUSE_BTNDOWN or MOUSE_BTNUP events
     * @return the mouse button
     * @see Type#MOUSE_BTNDOWN
     * @see Type#MOUSE_BTNUP
     * @see #MOUSE_LBUTTON
     * @see #MOUSE_RBUTTON
     * @see #MOUSE_MBUTTON
     */
    public abstract int getMouseButton();

    /**
     * The mouse wheel delta. Only valid for MOUSE_WHEEL events
     * @return the mouse wheel delta
     * @see Type#MOUSE_WHEEL
     */
    public abstract int getMouseWheelDelta();
    
    /**
     * The mouse click count. Only valid for MOUSE_CLICKED events
     * @return the mouse click count
     * @see Type#MOUSE_CLICKED
     */
    public abstract int getMouseClickCount();

    public abstract Event createSubEvent(Type newType);
    
    /**
     * Returns the key code. Only valid for KEY_PRESSED or KEY_RELEASED events
     * @see org.lwjgl.input.Keyboard
     * @return the key code
     */
    public abstract int getKeyCode();

    /**
     * Returns the key character. Only valid if hasKeyChar() returns true.
     * @see #hasKeyChar()
     * @return the key character
     */
    public abstract char getKeyChar();

    /**
     * Checks if a character is available for theis KEY_PRESSED event
     * @see #getKeyChar()
     * @return true if a character is available
     */
    public abstract boolean hasKeyChar();
    
    public abstract boolean isKeyRepeated();
    
    public abstract int getModifiers();
}
