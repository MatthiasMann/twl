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
package de.matthiasmann.twl.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class TextUtilTest {

    @Test
    public void testRomanNumbers1() {
        assertEquals("I", TextUtil.toRomanNumberString(1));
        assertEquals("II", TextUtil.toRomanNumberString(2));
        assertEquals("III", TextUtil.toRomanNumberString(3));
        assertEquals("IV", TextUtil.toRomanNumberString(4));
        assertEquals("V", TextUtil.toRomanNumberString(5));
        assertEquals("VI", TextUtil.toRomanNumberString(6));
        assertEquals("VII", TextUtil.toRomanNumberString(7));
        assertEquals("VIII", TextUtil.toRomanNumberString(8));
        assertEquals("IX", TextUtil.toRomanNumberString(9));
    }

    @Test
    public void testRomanNumbers2() {
        assertEquals("X", TextUtil.toRomanNumberString(10));
        assertEquals("XI", TextUtil.toRomanNumberString(11));
        assertEquals("XII", TextUtil.toRomanNumberString(12));
        assertEquals("XIII", TextUtil.toRomanNumberString(13));
        assertEquals("XIV", TextUtil.toRomanNumberString(14));
        assertEquals("XV", TextUtil.toRomanNumberString(15));
        assertEquals("XX", TextUtil.toRomanNumberString(20));
        assertEquals("XXX", TextUtil.toRomanNumberString(30));
        assertEquals("XL", TextUtil.toRomanNumberString(40));
    }

    @Test
    public void testRomanNumbers3() {
        assertEquals("L", TextUtil.toRomanNumberString(50));
        assertEquals("LI", TextUtil.toRomanNumberString(51));
        assertEquals("LII", TextUtil.toRomanNumberString(52));
        assertEquals("LIII", TextUtil.toRomanNumberString(53));
        assertEquals("LIV", TextUtil.toRomanNumberString(54));
        assertEquals("LV", TextUtil.toRomanNumberString(55));
        assertEquals("LX", TextUtil.toRomanNumberString(60));
        assertEquals("LXX", TextUtil.toRomanNumberString(70));
        assertEquals("LXXX", TextUtil.toRomanNumberString(80));
        assertEquals("XC", TextUtil.toRomanNumberString(90));
        assertEquals("XCI", TextUtil.toRomanNumberString(91));
        assertEquals("XCIV", TextUtil.toRomanNumberString(94));
        assertEquals("XCV", TextUtil.toRomanNumberString(95));
        assertEquals("XCVIII", TextUtil.toRomanNumberString(98));
        assertEquals("XCIX", TextUtil.toRomanNumberString(99));
    }

    @Test
    public void testRomanNumbers4() {
        assertEquals("C", TextUtil.toRomanNumberString(100));
        assertEquals("CI", TextUtil.toRomanNumberString(101));
        assertEquals("CV", TextUtil.toRomanNumberString(105));
        assertEquals("CIX", TextUtil.toRomanNumberString(109));
        assertEquals("CX", TextUtil.toRomanNumberString(110));
        assertEquals("CXX", TextUtil.toRomanNumberString(120));
        assertEquals("CXL", TextUtil.toRomanNumberString(140));
        assertEquals("CL", TextUtil.toRomanNumberString(150));
        assertEquals("CXC", TextUtil.toRomanNumberString(190));
        assertEquals("CXCIX", TextUtil.toRomanNumberString(199));
    }

    @Test
    public void testRomanNumbers5() {
        assertEquals("CC", TextUtil.toRomanNumberString(200));
        assertEquals("CCC", TextUtil.toRomanNumberString(300));
        assertEquals("CD", TextUtil.toRomanNumberString(400));
        assertEquals("D", TextUtil.toRomanNumberString(500));
        assertEquals("DC", TextUtil.toRomanNumberString(600));
        assertEquals("DCC", TextUtil.toRomanNumberString(700));
        assertEquals("DCCC", TextUtil.toRomanNumberString(800));
        assertEquals("CM", TextUtil.toRomanNumberString(900));
        assertEquals("CMX", TextUtil.toRomanNumberString(910));
        assertEquals("CMXL", TextUtil.toRomanNumberString(940));
        assertEquals("CML", TextUtil.toRomanNumberString(950));
        assertEquals("CMLX", TextUtil.toRomanNumberString(960));
        assertEquals("CMXC", TextUtil.toRomanNumberString(990));
        assertEquals("CMXCIV", TextUtil.toRomanNumberString(994));
        assertEquals("CMXCV", TextUtil.toRomanNumberString(995));
        assertEquals("CMXCVI", TextUtil.toRomanNumberString(996));
        assertEquals("CMXCIX", TextUtil.toRomanNumberString(999));
    }

    @Test
    public void testRomanNumbers6() {
        assertEquals("M", TextUtil.toRomanNumberString(1000));
        assertEquals("MM", TextUtil.toRomanNumberString(2000));
        assertEquals("MMM", TextUtil.toRomanNumberString(3000));
        assertEquals("MMMCMXCIX", TextUtil.toRomanNumberString(3999));
        assertEquals("MMM", TextUtil.toRomanNumberString(3000));
        assertEquals("Mↁ", TextUtil.toRomanNumberString(4000));
        assertEquals("\u2181", TextUtil.toRomanNumberString(5000)); // ensure that correct encoding
        assertEquals("ↁM", TextUtil.toRomanNumberString(6000));
        assertEquals("ↁMM", TextUtil.toRomanNumberString(7000));
        assertEquals("ↁMMM", TextUtil.toRomanNumberString(8000));
        assertEquals("Mↂ", TextUtil.toRomanNumberString(9000));
        assertEquals("MↂCM", TextUtil.toRomanNumberString(9900));
        assertEquals("MↂCMXC", TextUtil.toRomanNumberString(9990));
        assertEquals("MↂCMXCIX", TextUtil.toRomanNumberString(9999));
    }

    @Test
    public void testRomanNumbers7() {
        assertEquals("\u2182", TextUtil.toRomanNumberString(10000)); // ensure that correct encoding
        assertEquals("ↂↂ", TextUtil.toRomanNumberString(20000));
        assertEquals("ↂↂↂ", TextUtil.toRomanNumberString(30000));
        assertEquals("ↂↂↂMↂ", TextUtil.toRomanNumberString(39000));
        assertEquals("ↂↂↂMↂCM", TextUtil.toRomanNumberString(39900));
        assertEquals("ↂↂↂMↂCMXC", TextUtil.toRomanNumberString(39990));
        assertEquals("ↂↂↂMↂCMXCVIII", TextUtil.toRomanNumberString(39998));
        assertEquals("ↂↂↂMↂCMXCIX", TextUtil.toRomanNumberString(39999));
    }
}