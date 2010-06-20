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

import java.util.Collections;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class InputMapTest {

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF8\" standalone=\"yes\"?>\n";

    public InputMapTest() {
    }

    @Test
    public void test1() throws Exception {
        final String input = XML_HEADER
                + "<inputMapDef>\n"
                + "    <action name=\"openInventory\">I</action>\n"
                + "    <action name=\"openQuestLog\">Q</action>\n"
                + "</inputMapDef>";
        InputMap im = parse(input);
        String result = toString(im);
        assertEquals(input, result);
    }

    @Test
    public void test2() throws Exception {
        final String input = XML_HEADER
                + "<inputMapDef>\n"
                + "    <action name=\"openInventory\">shift I</action>\n"
                + "    <action name=\"openQuestLog\">cmd Q</action>\n"
                + "    <action name=\"quit\">meta X</action>\n"
                + "    <action name=\"run\">LSHIFT</action>\n"
                + "    <action name=\"console\">typed î</action>\n"
                + "</inputMapDef>";
        InputMap im = parse(input);
        String result = toString(im);
        assertEquals(input, result);
    }

    @Test
    public void testMerge() throws Exception {
        final String input = XML_HEADER
                + "<inputMapDef>\n"
                + "    <action name=\"openInventory\">I</action>\n"
                + "    <action name=\"openQuestLog\">Q</action>\n"
                + "</inputMapDef>";

        InputMap im1 = parse(input);
        InputMap im2 = im1.addKeyStroke(KeyStroke.parse("Q", "quit"));

        final String expected = XML_HEADER
                + "<inputMapDef>\n"
                + "    <action name=\"quit\">Q</action>\n"
                + "    <action name=\"openInventory\">I</action>\n"
                + "</inputMapDef>";

        String result = toString(im2);
        assertEquals(expected, result);
    }

    @Test
    public void testRemove() throws Exception {
        final String input = XML_HEADER
                + "<inputMapDef>\n"
                + "    <action name=\"openInventory\">I</action>\n"
                + "    <action name=\"openQuestLog\">Q</action>\n"
                + "</inputMapDef>";

        InputMap im1 = parse(input);
        InputMap im2 = im1.removeKeyStrokes(Collections.singleton(im1.getKeyStrokes()[0]));

        final String expected = XML_HEADER
                + "<inputMapDef>\n"
                + "    <action name=\"openQuestLog\">Q</action>\n"
                + "</inputMapDef>";

        String result = toString(im2);
        assertEquals(expected, result);
    }

    private static String toString(InputMap imi) throws XmlPullParserException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imi.writeXML(baos);
        return baos.toString("UTF8");
    }
    private static InputMap parse(final String content) throws XmlPullParserException, IOException {
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {
                    }
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(content.getBytes("UTF8"));
                    }
                };
            }
        };
        return InputMap.parse(new URL("test", "localhost", 80, "data", handler));
    }
}