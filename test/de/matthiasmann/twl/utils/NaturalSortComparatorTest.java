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
package de.matthiasmann.twl.utils;

import org.junit.Test;
import static org.junit.Assert.*;
import static de.matthiasmann.twl.utils.NaturalSortComparator.*;

/**
 * Tests for NaturalSortComparator
 *
 * @author Matthias Mann
 */
public class NaturalSortComparatorTest {

    public NaturalSortComparatorTest() {
    }

    @Test
    public void testEquality() {
        compareOrder("Test1", "Test1", 0);
        compareOrder("bla", "bla", 0);
        compareOrder(" bla", " bla", 0);
        compareOrder("bla ", "bla ", 0);
        compareOrder("bla  blub", "bla  blub", 0);
    }

    @Test
    public void testMixedCaseEquality() {
        compareOrder("bla", "Bla", 0);
        compareOrder("bla", "bLa", 0);
        compareOrder("bla", "blA", 0);
    }

    @Test
    public void testTextOrder() {
        compareOrder("Affe", "Hund", -1);
        compareOrder("Affe", "Anfang", -1);
        compareOrder("Wohnhaus", "Wohnsiedlung", -1);
        compareOrder("Bla", "Bla ", -1);
        compareOrder("Chuck Norris", "Arni", +1);
    }

    @Test
    public void testNumbersEquality() {
        compareOrder("17", "17", 0);
        compareOrder("3", "3", 0);
        compareOrder("78834", "78834", 0);
    }

    @Test
    public void testNumbersOrder() {
        compareOrder("3", "4", -1);
        compareOrder("007", "42", -1);
        compareOrder("3", "0003", -1);
        compareOrder("78834", "078834", -1);
    }

    @Test
    public void testMixedEquality() {
        compareOrder("Hallo 019 Welt", "Hallo 019 Welt", 0);
        compareOrder("test66.001", "test066.01", 0);
    }

    @Test
    public void testMixedOrder() {
        compareOrder("test1.txt", "test2.txt", -1);
        compareOrder("test007.txt", "test42.txt", -1);
        compareOrder("teSt19.txt", "Test09.txt", +1);
    }

    /**
     * Compares signum(a-b) == result and signum(b-a) == -result
     *
     * @param a First string
     * @param b Second string
     * @param result expected sign
     */
    private void compareOrder(String a, String b, int result) {
        assertEquals( result, Integer.signum(naturalCompare(a, b)));
        assertEquals(-result, Integer.signum(naturalCompare(b, a)));
    }
}
