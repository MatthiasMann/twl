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

import org.lwjgl.input.Keyboard;

/**
 * UI events for Mouse and Keyboard.
 *
 * @author MannMat
 */
public final class Event {

    public enum Type {
        /**
         * The mouse has entered the widget.
         * You need to return true from {@link Widget#handleEvent(de.matthiasmann.twl.Event) } in order to receive further mouse events.
         */
        MOUSE_ENTERED(true, false),
        /**
         * The mouse has moved over the widget - no mouse buttons are pressed.
         * You need to return true from {@link Widget#handleEvent(de.matthiasmann.twl.Event) } in order to receive further mouse events.
         */
        MOUSE_MOVED(true, false),
        /**
         * A mouse button has been pressed. The pressed button is available via {@link Event#getMouseButton() }
         */
        MOUSE_BTNDOWN(true, false),
        /**
         * A mouse button has been released. The released button is available via {@link Event#getMouseButton() }
         */
        MOUSE_BTNUP(true, false),
        /**
         * A click event with the left mouse button. A click is defined by a MOUSE_BTNDOWN event followed
         * by a MOUSE_BTNUP without moving the mouse outside the click distance. The MOUSE_BTNUP event is
         * sent before the MOUSE_CLICKED.
         */
        MOUSE_CLICKED(true, false),
        /**
         * The mouse has moved while at least one mouse button was pressed. The widget automatically
         * captures the mouse when a drag is started, which means that the widgets will receive mouse
         * events from this drag also outside of it's bounds. The drag ends when the last mouse button
         * is released.
         *
         * NOTE: This enum has a typo - because of backwards compatibility it will not be fixed.
         * 
         * @see Event#isMouseDragEvent()
         * @see Event#isMouseDragEnd()
         */
        MOUSE_DRAGED(true, false),
        /**
         * The mouse has left the widget.
         */
        MOUSE_EXITED(true, false),
        /**
         * The mouse wheel has been turned. The amount is available via {@link Event#getMouseWheelDelta() }
         */
        MOUSE_WHEEL(true, false),
        /**
         * A key has been pressed. Not all keys generate characters.
         */
        KEY_PRESSED(false, true),
        /**
         * A key has been released. No character data is available.
         */
        KEY_RELEASED(false, true),
        /**
         * A popup has been opened. Input event delivery will stop until the popup is closed.
         */
        POPUP_OPENED(false, false),
        /**
         * A popup has closed. Input events delivery will resume if no other popups are open.
         */
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
    public static final int MODIFIER_LALT = 512;
    public static final int MODIFIER_RALT = 1024;

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
     * One of the alt/menu keys is pressed
     * @see #getModifiers()
     */
    public static final int MODIFIER_ALT = MODIFIER_LALT | MODIFIER_RALT;

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
    
    Type type;
    int mouseX;
    int mouseY;
    int mouseWheelDelta;
    int mouseButton;
    int mouseClickCount;
    boolean dragEvent;
    boolean keyRepeated;
    char keyChar;
    int keyCode;
    int modifier;
    private Event subEvent;

    Event() {
    }

    /**
     * Returns the type of the event.
     * @return the type of the event.
     */
    public final Type getType() {
        return type;
    }

    /**
     * Returns true for all MOUSE_* event types.
     * @return true if this is a mouse event.
     */
    public final boolean isMouseEvent() {
        return type.isMouseEvent;
    }

    /**
     * Returns true for all MOUSE_* event types except MOUSE_WHEEL.
     * @return true if this is a mouse event but not a mouse wheel event.
     */
    public final boolean isMouseEventNoWheel() {
        return type.isMouseEvent && type != Type.MOUSE_WHEEL;
    }

    /**
     * Returns true for all KEY_* event types.
     * @return true if this is a key event.
     */
    public final boolean isKeyEvent() {
        return type.isKeyEvent;
    }

    /**
     * Returns true if this event is part of a drag operation
     * @return true if this event is part of a drag operation
     */
    public final boolean isMouseDragEvent() {
        return dragEvent;
    }

    /**
     * Returns true if this event ends a drag operation
     * @return true if this event ends a drag operation
     */
    public final boolean isMouseDragEnd() {
        return (modifier & MODIFIER_BUTTON) == 0;
    }

    /**
     * Returns the current mouse X coordinate
     * @return the current mouse X coordinate
     */
    public final int getMouseX() {
        return mouseX;
    }

    /**
     * Returns the current mouse Y coordinate
     * @return the current mouse Y coordinate
     */
    public final int getMouseY() {
        return mouseY;
    }

    /**
     * The mouse button. Only valid for MOUSE_BTNDOWN or MOUSE_BTNUP events
     * @return the mouse button
     * @see Type#MOUSE_BTNDOWN
     * @see Type#MOUSE_BTNUP
     * @see #MOUSE_LBUTTON
     * @see #MOUSE_RBUTTON
     * @see #MOUSE_MBUTTON
     */
    public final int getMouseButton() {
        return mouseButton;
    }

    /**
     * The mouse wheel delta. Only valid for MOUSE_WHEEL events
     * @return the mouse wheel delta
     * @see Type#MOUSE_WHEEL
     */
    public final int getMouseWheelDelta() {
        return mouseWheelDelta;
    }
    
    /**
     * The mouse click count. Only valid for MOUSE_CLICKED events
     * @return the mouse click count
     * @see Type#MOUSE_CLICKED
     */
    public final int getMouseClickCount() {
        return mouseClickCount;
    }
    
    /**
     * Returns the key code. Only valid for KEY_PRESSED or KEY_RELEASED events
     * @see org.lwjgl.input.Keyboard
     * @return the key code
     */
    public final int getKeyCode() {
        return keyCode;
    }

    /**
     * Returns the key character. Only valid if hasKeyChar() returns true.
     * @see #hasKeyChar()
     * @return the key character
     */
    public final char getKeyChar() {
        return keyChar;
    }

    /**
     * Checks if a character is available for theis KEY_PRESSED event
     * @see #getKeyChar()
     * @return true if a character is available
     */
    public final boolean hasKeyChar() {
        return type == Type.KEY_PRESSED && keyChar != Keyboard.CHAR_NONE;
    }

    /**
     * Checks if a characters is available and no keyboard modifiers are
     * active (except these needed to generate that character).
     * 
     * @return true if it's a character without additional modifiers
     */
    public final boolean hasKeyCharNoModifiers() {
        final int MODIFIER_ALTGR = MODIFIER_LCTRL | MODIFIER_RALT;
        return hasKeyChar() && (
                ((modifier & ~MODIFIER_SHIFT) == 0) ||
                ((modifier & ~MODIFIER_ALTGR) == 0));
    }

    /**
     * Returns true if this is a repeated KEY_PRESSED event
     * @return true if this is a repeated KEY_PRESSED event
     */
    public final boolean isKeyRepeated() {
        return type == Type.KEY_PRESSED && keyRepeated;
    }

    /**
     * Returns the current event modifiers
     * @return the current event modifiers
     */
    public final int getModifiers() {
        return modifier;
    }

    final Event createSubEvent(Type newType) {
        if(subEvent == null) {
            subEvent = new Event();
        }
        subEvent.type = newType;
        subEvent.mouseX = mouseX;
        subEvent.mouseY = mouseY;
        subEvent.mouseButton = mouseButton;
        subEvent.mouseWheelDelta = mouseWheelDelta;
        subEvent.mouseClickCount = mouseClickCount;
        subEvent.dragEvent = dragEvent;
        subEvent.keyRepeated = keyRepeated;
        subEvent.keyChar = keyChar;
        subEvent.keyCode = keyCode;
        subEvent.modifier = modifier;
        return subEvent;
    }
    
    void setModifier(int mask, boolean pressed) {
        if(pressed) {
            modifier |= mask;
        } else {
            modifier &= ~mask;
        }
    }
}
