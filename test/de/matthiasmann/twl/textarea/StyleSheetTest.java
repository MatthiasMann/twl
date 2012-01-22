/*
 * Copyright (c) 2008-2012, Matthias Mann
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

import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.*;
import static de.matthiasmann.twl.textarea.StyleAttribute.*;
import static de.matthiasmann.twl.textarea.Value.Unit.*;
import de.matthiasmann.twl.utils.StringList;

/**
 *
 * @author Matthias Mann
 */
public class StyleSheetTest {

    public StyleSheetTest() {
    }

    @Test
    public void testParse1() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader(""));
    }

    @Test
    public void testParse2() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { font: link }"));
        test(ss, FONT_FAMILIES, new StringList("link"), new StyleSheetKey("a", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("td", null, null));
    }

    @Test
    public void testParse3() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { font: link; }"));
        test(ss, FONT_FAMILIES, new StringList("link"), new StyleSheetKey("a", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("td", null, null));
    }

    @Test
    public void testParse4() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("div a { font: link; }"));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("a", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("div", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("td", null, null));
        test(ss, FONT_FAMILIES, new StringList("link"), new StyleSheetKey("div", null, null), new StyleSheetKey("a", null, null));
        test(ss, FONT_FAMILIES, new StringList("link"), new StyleSheetKey("div", null, null), new StyleSheetKey("p", null, null), new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse5() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("div > a { font: link; }"));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("a", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("div", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("td", null, null));
        test(ss, FONT_FAMILIES, new StringList("link"), new StyleSheetKey("div", null, null), new StyleSheetKey("a", null, null));
        test(ss, FONT_FAMILIES, new StringList("default"), new StyleSheetKey("div", null, null), new StyleSheetKey("p", null, null), new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse6() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("div, img { margin-right: 6px; margin-left: 5px; }"));
        test(ss, MARGIN_LEFT, new Value(5, PX), new StyleSheetKey("div", null, null));
        test(ss, MARGIN_LEFT, new Value(5, PX), new StyleSheetKey("img", null, null));
        test(ss, MARGIN_LEFT, Value.ZERO_PX, new StyleSheetKey("p", null, null));
        test(ss, MARGIN_RIGHT, new Value(6, PX), new StyleSheetKey("div", null, null));
        test(ss, MARGIN_RIGHT, new Value(6, PX), new StyleSheetKey("img", null, null));
        test(ss, MARGIN_RIGHT, Value.ZERO_PX, new StyleSheetKey("p", null, null));
    }

    @Test
    public void testParse7() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader(".bla { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bla", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse8() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("*.bla { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bla", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse9() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("*.bla blub { height: 5% }"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bla", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("blub", null, null));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bla", null), new StyleSheetKey("blub", null, null));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bla", null), new StyleSheetKey("p", null, null), new StyleSheetKey("blub", null, null));
    }

    @Test
    public void testParse9a() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("*.bla > blub { height: 5% }"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bla", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("blub", null, null));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bla", null), new StyleSheetKey("blub", null, null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bla", null), new StyleSheetKey("p", null, null), new StyleSheetKey("blub", null, null));
    }

    @Test
    public void testParse9b() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("blub .bla { height: 5% }"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bla", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("blub", null, null));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("blub", null, null), new StyleSheetKey("a", "bla", null));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("blub", null, null), new StyleSheetKey("p", null, null), new StyleSheetKey("a", "bla", null));
    }

    @Test
    public void testParse10() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("#id12 { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse11() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("p#id12{ height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("p", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", null, null));
    }

    @Test
    public void testParse12() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("#id12.bar { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse12a() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("#id12 .bar { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("p", null, "id12"), new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse13() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader(".bar#id12 { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("a", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse13a() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader(".bar #id12 { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("p", "bar", null), new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
    }

    @Test
    public void testParse14() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("p.bar#id12 { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("p", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", null, null));
    }

    @Test
    public void testParse15() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("p#id12.bar { height: 5% }"));
        test(ss, HEIGHT, new Value(5, PERCENT), new StyleSheetKey("p", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("a", null, null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", null, "id12"));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", "bar", null));
        test(ss, HEIGHT, Value.AUTO, new StyleSheetKey("p", null, null));
    }
    
    @Test
    public void testParse16() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { text-indent: 10px; width: 100px }"));
        StyleSheetKey keyA = new StyleSheetKey("a", null, null);
        StyleSheetKey keyImg = new StyleSheetKey("img", null, null);
        test(ss, TEXT_INDENT, new Value(10, PX), keyA);
        test(ss, TEXT_INDENT, new Value(10, PX), keyA, null);
        test(ss, TEXT_INDENT, Value.ZERO_PX, keyImg);
        test(ss, WIDTH, new Value(100, PX), keyA);
        test(ss, WIDTH, Value.AUTO, keyA, null);
    }
    
    @Test
    public void testParse17() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { text-indent: 10px }"));
        StyleSheetKey keyA = new StyleSheetKey("a", null, null);
        
        Value P10 = new Value(10, PX);
        Value P42 = new Value(42, PX);
        Style styleA = makeStyle(keyA);
        Style styleA_42 = makeStyle(keyA, null).with(TEXT_INDENT, P42);
        
        test(ss, TEXT_INDENT, P10, styleA);
        test(ss, TEXT_INDENT, P42, styleA.with(TEXT_INDENT, P42));
        test(ss, TEXT_INDENT, P10, keyA, null);
        test(ss, TEXT_INDENT, P42, styleA_42);
        test(ss, TEXT_INDENT, P42, new Style(styleA_42, null));
    }
    
    @Test
    public void testParse18() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { width: 66px }"));
        StyleSheetKey keyA = new StyleSheetKey("a", null, null);
        
        Value P66 = new Value(66, PX);
        Value P42 = new Value(42, PX);
        Style styleA = makeStyle(keyA);
        Style styleA_42 = makeStyle(keyA, null).with(WIDTH, P42);
        
        test(ss, WIDTH, P66, styleA);
        test(ss, WIDTH, P42, styleA.with(WIDTH, P42));
        test(ss, WIDTH, Value.AUTO, keyA, null);
        test(ss, WIDTH, P42, styleA_42);
        test(ss, WIDTH, Value.AUTO, new Style(styleA_42, null));
    }

    private Style makeStyle(StyleSheetKey ... keys) {
        Style style = null;
        for(StyleSheetKey key : keys) {
            style = new Style(style, key);
        }
        return style;
    }
    
    private<T> void test(StyleSheet ss, StyleAttribute<T> attrib, T expected, StyleSheetKey ... keys) {
        test(ss, attrib, expected, makeStyle(keys));
    }
    
    private<T> void test(StyleSheet ss, StyleAttribute<T> attrib, T expected, Style style) {
        Object value = style.get(attrib, ss);
        assertEquals(expected, value);
    }
}