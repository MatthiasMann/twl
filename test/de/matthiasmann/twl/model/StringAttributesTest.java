/*
 * Copyright (c) 2008-2011, Matthias Mann
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
package de.matthiasmann.twl.model;

import java.util.HashMap;
import java.lang.reflect.Field;
import java.util.HashSet;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class StringAttributesTest {

    public StringAttributesTest() {
    }

    private static final StateKey BLA  = StateKey.get("bla");
    private static final StateKey BLUB = StateKey.get("blub");
    private static final StateKey HUGO = StateKey.get("hugo");
    private static final StateKey BOB  = StateKey.get("bob");

    @Test
    public void testSimple() {
        StringAttributes sa = new StringAttributes("Hello World");
        assertEquals(11, sa.length());
        sa.setPosition(0);
        assertEquals(0, sa.getPosition());
        check(sa, 11);
    }

    @Test
    public void testSingleLeft() {
        StringAttributes sa = new StringAttributes("Hello World");
        sa.setAnimationState(BLA, 0, 5, true);
        assertEquals(11, sa.length());
        sa.setPosition(0);
        assertEquals(0, sa.getPosition());
        check(sa, 5, BLA);
        check(sa, 11);
    }

    @Test
    public void testSingleCenter() {
        StringAttributes sa = new StringAttributes("Hello World");
        sa.setAnimationState(HUGO, 4, 7, true);
        assertEquals(11, sa.length());
        sa.setPosition(0);
        assertEquals(0, sa.getPosition());
        check(sa, 4);
        check(sa, 7, HUGO);
        check(sa, 11);
    }

    @Test
    public void testSingleRight() {
        StringAttributes sa = new StringAttributes("Hello World");
        sa.setAnimationState(BOB, 6, 11, true);
        assertEquals(11, sa.length());
        sa.setPosition(0);
        assertEquals(0, sa.getPosition());
        check(sa, 6);
        check(sa, 11, BOB);
    }

    @Test
    public void testLeftRight() {
        StringAttributes sa = new StringAttributes("Hello World");
        sa.setAnimationState(BLA, 0, 5, true);
        sa.setAnimationState(BOB, 6, 11, true);
        assertEquals(11, sa.length());
        sa.setPosition(0);
        assertEquals(0, sa.getPosition());
        check(sa, 5, BLA);
        check(sa, 6);
        check(sa, 11, BOB);
    }

    @Test
    public void testOverlapping() {
        StringAttributes sa = new StringAttributes("Hello World");
        sa.setAnimationState(BLA, 0, 5, true);
        sa.setAnimationState(HUGO, 4, 7, true);
        sa.setAnimationState(BOB, 6, 11, true);
        assertEquals(11, sa.length());
        sa.setPosition(0);
        assertEquals(0, sa.getPosition());
        check(sa, 4, BLA);
        check(sa, 5, BLA, HUGO);
        check(sa, 6, HUGO);
        check(sa, 7, HUGO, BOB);
        check(sa, 11, BOB);
    }

    private static void check(StringAttributes sa, int nextPos, StateKey ... activeKeys) {
        HashSet<StateKey> all = getAllStateKeys();
        for(StateKey key : activeKeys) {
            all.remove(key);
            assertTrue(sa.getAnimationState(key));
        }
        for(StateKey key : all) {
            assertFalse(sa.getAnimationState(key));
        }
        assertEquals(nextPos, sa.advance());
        assertEquals(nextPos, sa.getPosition());
    }

    @SuppressWarnings("unchecked")
    private static HashSet<StateKey> getAllStateKeys() {
        try {
            Field f = StateKey.class.getDeclaredField("keys");
            f.setAccessible(true);
            return new HashSet<StateKey>(((HashMap<String, StateKey>)f.get(null)).values());
        } catch(Exception ex) {
            throw new AssertionError();
        }
    }
}