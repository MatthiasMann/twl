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
import de.matthiasmann.twl.utils.XMLParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

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

    void parse(XMLParser xmlp) throws XmlPullParserException, IOException {
        ArrayList<KeyStroke> newStrokes = new ArrayList<KeyStroke>();
        while(!xmlp.isEndTag()) {
            xmlp.require(XmlPullParser.START_TAG, null, "action");
            String name = xmlp.getAttributeNotNull("name");
            String key = xmlp.nextText();
            try {
                KeyStroke ks = KeyStroke.parse(key, name);
                newStrokes.add(ks);
            } catch (IllegalArgumentException ex) {
                throw xmlp.error("can't parse Keystroke", ex);
            }
            xmlp.require(XmlPullParser.END_TAG, null, "action");
            xmlp.nextTag();
        }
        addMappings(newStrokes);
    }

    /**
     * Parses a stand alone &lt;inputMapDef&gt; XML file
     *
     * @param url the URL ton the XML file
     * @throws XmlPullParserException if a parse error occured
     * @throws IOException if an IO related error occured
     */
    public void parse(URL url) throws XmlPullParserException, IOException {
        XMLParser xmlp = new XMLParser(url);
        try {
            xmlp.require(XmlPullParser.START_DOCUMENT, null, null);
            xmlp.nextTag();
            xmlp.require(XmlPullParser.START_TAG, null, "inputMapDef");
            xmlp.nextTag();
            parse(xmlp);
            xmlp.require(XmlPullParser.END_TAG, null, "inputMapDef");
        } finally {
            xmlp.close();
        }
    }

    public void writeXML(OutputStream os) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlSerializer serializer = factory.newSerializer();
        serializer.setOutput(os, "UTF8");
        serializer.startDocument("UTF8", Boolean.TRUE);
        serializer.text("\n");
        serializer.startTag(null, "inputMapDef");
        for(KeyStroke ks : keyStrokes) {
            serializer.text("\n    ");
            serializer.startTag(null, "action");
            serializer.attribute(null, "name", ks.getAction());
            serializer.text(ks.getKeyString());
            serializer.endTag(null, "action");
        }
        serializer.text("\n");
        serializer.endTag(null, "inputMapDef");
        serializer.endDocument();
    }
}
