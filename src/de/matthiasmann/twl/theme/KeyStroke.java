/*
 * Copyright (c) 2008-2009, Matthias Mann
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
package de.matthiasmann.twl.theme;

import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.utils.TextUtil;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class KeyStroke {

    private static final int SHIFT = 1;
    private static final int CTRL = 2;
    private static final int META = 4;
    
    private final int modifier;
    private final int keyCode;
    private final char keyChar;
    private final String action;

    private KeyStroke(int modifier, int keyCode, char keyChar, String action) {
        this.modifier = modifier;
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.action = action;
    }

    String getAction() {
        return action;
    }

    boolean match(Event e, int mappedEventModifiers) {
        assert e.isKeyEvent();
        if(mappedEventModifiers != modifier) {
            return false;
        }
        if(keyCode != Keyboard.KEY_NONE && keyCode != e.getKeyCode()) {
            return false;
        }
        if(keyChar != Keyboard.CHAR_NONE && (!e.hasKeyChar() || keyChar != e.getKeyChar())) {
            return false;
        }
        return true;
    }

    static KeyStroke parse(String s, String action) {
        int idx = TextUtil.skipSpaces(s, 0);
        int modifers = 0;
        char keyChar = Keyboard.CHAR_NONE;
        int keyCode = Keyboard.KEY_NONE;
        boolean typed = false;
        boolean end = false;

        while(idx < s.length()) {
            int endIdx = TextUtil.indexOf(s, ' ', idx);
            String part = s.substring(idx, endIdx);

            if(end) {
                throw new IllegalArgumentException("Unexpected: " + part);
            }
            
            if(typed) {
                if(part.length() != 1) {
                    throw new IllegalArgumentException("Expected single character after 'typed'");
                }
                keyChar = part.charAt(0);
                if(keyChar == Keyboard.CHAR_NONE) {
                    throw new IllegalArgumentException("Unknown character: " + part);
                }
                end = true;
            } else if("ctrl".equalsIgnoreCase(part) || "control".equalsIgnoreCase(part)) {
                modifers |= CTRL;
            } else if("shift".equalsIgnoreCase(part)) {
                modifers |= SHIFT;
            } else if("meta".equalsIgnoreCase(part)) {
                modifers |= META;
            } else if("typed".equalsIgnoreCase(part)) {
                typed = true;
            } else {
                keyCode = Keyboard.getKeyIndex(part.toUpperCase());
                if(keyCode == Keyboard.KEY_NONE) {
                    throw new IllegalArgumentException("Unknown key: " + part);
                }
                end = true;
            }

            idx = TextUtil.skipSpaces(s, endIdx+1);
        }

        if(!end) {
            throw new IllegalArgumentException("Unexpected end of string");
        }

        return new KeyStroke(modifers, keyCode, keyChar, action);
    }

    static int convertModifier(Event event) {
        int eventModifiers = event.getModifiers();
        int modifiers = 0;
        if((eventModifiers & Event.MODIFIER_SHIFT) != 0) {
            modifiers |= SHIFT;
        }
        if((eventModifiers & Event.MODIFIER_CTRL) != 0) {
            modifiers |= CTRL;
        }
        if((eventModifiers & Event.MODIFIER_META) != 0) {
            modifiers |= META;
        }
        return modifiers;
    }
}
