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
package de.matthiasmann.twl.textarea;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class OrderedListTypeTest {

    public OrderedListTypeTest() {
    }

    @Test
    public void testUpperRomanNumbers1() {
        // only a short test, main test is done by TextUtilTest.java
        assertEquals("-1", OrderedListType.UPPER_ROMAN.format(-1));
        assertEquals("0", OrderedListType.UPPER_ROMAN.format(0));
        assertEquals("I", OrderedListType.UPPER_ROMAN.format(1));
        assertEquals("II", OrderedListType.UPPER_ROMAN.format(2));
        assertEquals("III", OrderedListType.UPPER_ROMAN.format(3));
        assertEquals("IV", OrderedListType.UPPER_ROMAN.format(4));
        assertEquals("ↂↂↂMↂCMXCVIII", OrderedListType.UPPER_ROMAN.format(39998));
        assertEquals("ↂↂↂMↂCMXCIX", OrderedListType.UPPER_ROMAN.format(39999));
        assertEquals("40000", OrderedListType.UPPER_ROMAN.format(40000));
    }

    @Test
    public void testLowerRomanNumbers1() {
        // only a short test, main test is done by TextUtilTest.java
        assertEquals("-1", OrderedListType.LOWER_ROMAN.format(-1));
        assertEquals("0", OrderedListType.LOWER_ROMAN.format(0));
        assertEquals("i", OrderedListType.LOWER_ROMAN.format(1));
        assertEquals("ii", OrderedListType.LOWER_ROMAN.format(2));
        assertEquals("iii", OrderedListType.LOWER_ROMAN.format(3));
        assertEquals("iv", OrderedListType.LOWER_ROMAN.format(4));
        assertEquals("ↂↂↂmↂcmxcviii", OrderedListType.LOWER_ROMAN.format(39998));
        assertEquals("ↂↂↂmↂcmxcix", OrderedListType.LOWER_ROMAN.format(39999));
        assertEquals("40000", OrderedListType.LOWER_ROMAN.format(40000));
    }

    @Test
    public void testUpperLatinNumbers1() {
        assertEquals("-1", OrderedListType.UPPER_ALPHA.format(-1));
        assertEquals("0", OrderedListType.UPPER_ALPHA.format(0));
        assertEquals("A", OrderedListType.UPPER_ALPHA.format(1));
        for(int i=1 ; i<26 ; i++) {
            assertEquals(Character.toString((char)('A' + i - 1)), OrderedListType.UPPER_ALPHA.format(i));
        }
        assertEquals("Z", OrderedListType.UPPER_ALPHA.format(26));
        assertEquals("AA", OrderedListType.UPPER_ALPHA.format(27));
        assertEquals("AB", OrderedListType.UPPER_ALPHA.format(28));
        assertEquals("AY", OrderedListType.UPPER_ALPHA.format(51));
        assertEquals("AZ", OrderedListType.UPPER_ALPHA.format(52));
        assertEquals("BA", OrderedListType.UPPER_ALPHA.format(53));
        assertEquals("BB", OrderedListType.UPPER_ALPHA.format(54));
        assertEquals("BY", OrderedListType.UPPER_ALPHA.format(77));
        assertEquals("BZ", OrderedListType.UPPER_ALPHA.format(78));
        assertEquals("CA", OrderedListType.UPPER_ALPHA.format(79));
        assertEquals("CB", OrderedListType.UPPER_ALPHA.format(80));
        assertEquals("FXSHRXW", OrderedListType.UPPER_ALPHA.format(Integer.MAX_VALUE));
    }

    @Test
    public void testLowerLatinNumbers1() {
        assertEquals("-1", OrderedListType.LOWER_ALPHA.format(-1));
        assertEquals("0", OrderedListType.LOWER_ALPHA.format(0));
        assertEquals("a", OrderedListType.LOWER_ALPHA.format(1));
        for(int i=1 ; i<26 ; i++) {
            assertEquals(Character.toString((char)('a' + i - 1)), OrderedListType.LOWER_ALPHA.format(i));
        }
        assertEquals("fxshrxw", OrderedListType.LOWER_ALPHA.format(Integer.MAX_VALUE));
    }

    @Test
    public void testLowerGreekNumbers1() {
        final String ref =
                "\u03B1\u03B2\u03B3\u03B4\u03B5" + // alpha  beta  gamma  delta  epsilon
                "\u03B6\u03B7\u03B8\u03B9\u03BA" + // zeta   eta   theta  iota   kappa
                "\u03BB\u03BC\u03BD\u03BE\u03BF" + // lamda  mu    nu     xi     omicron
                "\u03C0\u03C1\u03C3\u03C4\u03C5" + // pi     rho   sigma  tau    upsilon
                "\u03C6\u03C7\u03C8\u03C9";        // phi    chi   psi    omega
        // ensure that correct encoding was used to compile OrderedListType.java
        assertEquals(ref, OrderedListType.LOWER_GREEK.characterList);
        for(int i=1 ; i<=ref.length() ; i++) {
            assertEquals(ref.substring(i-1, i), OrderedListType.LOWER_GREEK.format(i));
        }
        assertEquals("-1", OrderedListType.LOWER_GREEK.format(-1));
        assertEquals("0", OrderedListType.LOWER_GREEK.format(0));
    }

    @Test
    public void testLowerGreekNumbers2() {
        assertEquals("α", OrderedListType.LOWER_GREEK.format(1));
        assertEquals("β", OrderedListType.LOWER_GREEK.format(2));
        assertEquals("γ", OrderedListType.LOWER_GREEK.format(3));
        assertEquals("ψ", OrderedListType.LOWER_GREEK.format(23));
        assertEquals("ω", OrderedListType.LOWER_GREEK.format(24));
        assertEquals("αα", OrderedListType.LOWER_GREEK.format(25));
        assertEquals("αβ", OrderedListType.LOWER_GREEK.format(26));
        assertEquals("αγ", OrderedListType.LOWER_GREEK.format(27));
        assertEquals("αδ", OrderedListType.LOWER_GREEK.format(28));
        assertEquals("αε", OrderedListType.LOWER_GREEK.format(29));
        assertEquals("λεππξεη", OrderedListType.LOWER_GREEK.format(Integer.MAX_VALUE));
    }

    @Test
    public void testUpperRussianNumbers1() {
        final String ref =
                "\u0410\u0411\u0412\u0413\u0414" +
                "\u0415\u0416\u0417\u0418\u041A" +
                "\u041B\u041C\u041D\u041E\u041F" +
                "\u0420\u0421\u0422\u0423\u0424" +
                "\u0425\u0426\u0427\u0428\u0429" +
                "\u042D\u042E\u042F";
        // ensure that correct encoding was used to compile OrderedListType.java
        assertEquals(ref, OrderedListType.UPPER_RUSSIAN_SHORT.characterList);
        // assume that toCharListNumber() works as already tested above
    }

    @Test
    public void testLowerRussianNumbers1() {
        final String ref =
                "\u0430\u0431\u0432\u0433\u0434" +
                "\u0435\u0436\u0437\u0438\u043A" +
                "\u043B\u043C\u043D\u043E\u043F" +
                "\u0440\u0441\u0442\u0443\u0444" +
                "\u0445\u0446\u0447\u0448\u0449" +
                "\u044D\u044E\u044F";
        // ensure that correct encoding was used to compile OrderedListType.java
        assertEquals(ref, OrderedListType.LOWER_RUSSIAN_SHORT.characterList);
        // assume that toCharListNumber() works as already tested above
    }
}