/*
 * Copyright (c) 2008-2013, Matthias Mann
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

import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class HTMLTextAreaModelTest {

    public HTMLTextAreaModelTest() {
    }

    @Test
    public void testSimpleText() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo Welt");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo Welt");
        assertFalse(i.hasNext());
    }

    @Test
    public void testNewLine() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo Welt\nNot a new line");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo Welt\nNot a new line");
        assertFalse(i.hasNext());
    }

    @Test
    public void testEmptySpan() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo<span/>Welt");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo");
        assertText(i, "default", "Welt");
        assertFalse(i.hasNext());
    }

    @Test
    public void testEmptyDiv() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo<div/>Welt");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo");
        TextAreaModel.BlockElement be = next(i, TextAreaModel.BlockElement.class);
        assertEquals(TextAreaModel.FloatPosition.NONE, be.getStyle().get(StyleAttribute.FLOAT_POSITION, null));
        assertFalse(be.iterator().hasNext());
        assertText(i, "default", "Welt");
        assertFalse(i.hasNext());
    }

    @Test
    public void testBR() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo Welt<br/>A new line");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo Welt");
        assertLineBreak(i);
        assertText(i, "default", "A new line");
        assertFalse(i.hasNext());
    }

    @Test
    public void testBREmptySpan() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo Welt<br/><span/>A new line");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo Welt");
        assertLineBreak(i);
        assertText(i, "default", "A new line");
        assertFalse(i.hasNext());
    }

    @Test
    public void testBREmptySpan2() {
        HTMLTextAreaModel m = new HTMLTextAreaModel("Hallo Welt<span/><br/>A new line");
        Iterator<TextAreaModel.Element> i1 = m.iterator();
        Iterator<TextAreaModel.Element> i = assertBlock(i1);
        assertText(i, "default", "Hallo Welt");
        assertLineBreak(i);
        assertText(i, "default", "A new line");
        assertFalse(i.hasNext());
    }

    private<T extends TextAreaModel.Element> T next(Iterator<TextAreaModel.Element> i, Class<T> clazz) {
        assertTrue(i.hasNext());
        TextAreaModel.Element e = i.next();
        assertTrue(clazz.isInstance(e));
        return clazz.cast(e);
    }

    private void assertText(Iterator<TextAreaModel.Element> i, String style, String text) {
        TextAreaModel.TextElement te = next(i, TextAreaModel.TextElement.class);
        assertEquals(text, te.getText());
    }

    private Iterator<TextAreaModel.Element> assertBlock(Iterator<TextAreaModel.Element> i) {
        TextAreaModel.BlockElement be = next(i, TextAreaModel.BlockElement.class);
        return be.iterator();
    }
    
    private void assertLineBreak(Iterator<TextAreaModel.Element> i) {
        TextAreaModel.LineBreakElement br = next(i, TextAreaModel.LineBreakElement.class);
    }
}