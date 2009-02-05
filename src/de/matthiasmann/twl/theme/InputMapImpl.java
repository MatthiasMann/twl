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
import de.matthiasmann.twl.InputMap;
import java.util.Collection;

/**
 *
 * @author Matthias Mann
 */
public class InputMapImpl implements InputMap {

    private static final KeyStroke[] EMPTY_MAP = {};

    private KeyStroke[] keyStrokes;

    InputMapImpl() {
        keyStrokes = EMPTY_MAP;
    }

    InputMapImpl(InputMapImpl base) {
        keyStrokes = base.keyStrokes;
    }

    void addMappings(Collection<KeyStroke> strokes) {
        int size = strokes.size();
        KeyStroke[] newStrokes = new KeyStroke[keyStrokes.length + size];
        strokes.toArray(newStrokes);
        System.arraycopy(keyStrokes, 0, newStrokes, size, keyStrokes.length);
        keyStrokes = newStrokes;
    }

    public String mapEvent(Event event) {
        if(event.isKeyEvent()) {
            int mappedEventModifiers = KeyStroke.convertModifier(event);
            for(KeyStroke ks : keyStrokes) {
                if(ks.match(event, mappedEventModifiers)) {
                    return ks.getAction();
                }
            }
        }
        return null;
    }

}
