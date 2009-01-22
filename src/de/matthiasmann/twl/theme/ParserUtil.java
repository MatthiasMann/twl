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
package de.matthiasmann.twl.theme;

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A helper class to make XML parsing easier
 *
 * @author Matthias Mann
 */
public class ParserUtil {

    private ParserUtil() {
    }

    static void checkNameNotEmpty(final String name, XmlPullParser xpp) throws XmlPullParserException {
        if(name == null) {
            throw new XmlPullParserException("missing 'name' on '" + xpp.getName() + "'", xpp, null);
        }
        if(name.length() == 0) {
            throw new XmlPullParserException("empty name not allowed", xpp, null);
        }
        if("none".equals(name)) {
            throw new XmlPullParserException("can't use reserved name \"none\"", xpp, null);
        }
        if(name.indexOf('*') >= 0) {
            throw new XmlPullParserException("'*' is not allowed in names", xpp, null);
        }
        if(name.indexOf('/') >= 0) {
            throw new XmlPullParserException("'/' is not allowed in names", xpp, null);
        }
    }

    static<E extends Enum<E>> E parseEnum(XmlPullParser xpp, Class<E> enumClazz, String value) throws XmlPullParserException {
        try {
            return Enum.valueOf(enumClazz, value);
        } catch (IllegalArgumentException ex) {
        }
        try {
            return Enum.valueOf(enumClazz, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
        }
        throw new XmlPullParserException("Unknown enum value \"" + value
                + "\" for enum class " + enumClazz, xpp, null);
    }
    
    static boolean parseBool(XmlPullParser xpp, String value) throws XmlPullParserException {
        if("true".equals(value)) {
            return true;
        } else if("false".equals(value)) {
            return false;
        } else {
            throw new XmlPullParserException("boolean value must be 'true' or 'false'", xpp, null);
        }
    }
    
    static void missingAttribute(XmlPullParser xpp, String attribute) throws XmlPullParserException {
        throw new XmlPullParserException("missing '" + attribute + "' on '" + xpp.getName() + "'", xpp, null);
    }

    static String getAttributeNotNull(XmlPullParser xpp, String attribute) throws XmlPullParserException {
        String value = xpp.getAttributeValue(null, attribute);
        if(value == null) {
            missingAttribute(xpp, attribute);
        }
        return value;
    }
    
    static boolean parseBoolFromAttribute(XmlPullParser xpp, String attribName) throws XmlPullParserException {
        return parseBool(xpp, getAttributeNotNull(xpp, attribName));
    }
    
    static boolean parseBoolFromAttribute(XmlPullParser xpp, String attribName, boolean defaultValue) throws XmlPullParserException {
        String value = xpp.getAttributeValue(null, attribName);
        if(value == null) {
            return defaultValue;
        }
        return parseBool(xpp, value);
    }

    static int parseIntFromAttribute(XmlPullParser xpp, String attribName) throws XmlPullParserException {
        try {
            return Integer.parseInt(getAttributeNotNull(xpp, attribName));
        } catch(NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    ex.getMessage(), xpp, ex).initCause(ex));
        }
    }

    static int parseIntFromAttribute(XmlPullParser xpp, String attribName, int defaultValue) throws XmlPullParserException {
        try {
            String value = xpp.getAttributeValue(null, attribName);
            if(value == null) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        } catch(NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    ex.getMessage(), xpp, ex).initCause(ex));
        }
    }
    
    static Border parseBorderFromAttribute(XmlPullParser xpp, String attribute) throws XmlPullParserException {
        String value = xpp.getAttributeValue(null, attribute);
        if(value == null) {
            return null;
        }
        return parseBorder(xpp, value);
    }

    static Border parseBorder(XmlPullParser xpp, String value) throws XmlPullParserException {
        try {
            int elements = countElements(value);
            int values[] = new int[elements];
            parseIntArray(value, values, 0, elements);

            switch(elements) {
            case 1:
                return new Border(values[0]);
            case 2:
                return new Border(values[0], values[1]);
            case 4:
                return new Border(values[0], values[1], values[2], values[3]);
            default:
                throw new XmlPullParserException("Unsupported border format", xpp, null);
            }
        } catch (NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "Unable to parse border size", xpp, ex).initCause(ex));
        }
    }

    static Color parseColorFromAttribute(XmlPullParser xpp, String attribute, Color defaultColor) throws XmlPullParserException {
        String value = xpp.getAttributeValue(null, attribute);
        if(value == null) {
            return defaultColor;
        }
        return parseColor(xpp, value);
    }

    static Color parseColor(XmlPullParser xpp, String value) throws XmlPullParserException {
        try {
            Color color = Color.parserColor(value);
            if(color == null) {
                throw new XmlPullParserException("Unknown color name: " + value, xpp, null);
            }
            return color;
        } catch(NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "unable to parse color code", xpp, ex).initCause(ex));
        }
    }
    
    static int indexOf(String str, char ch, int idx) {
        idx = str.indexOf(ch, idx);
        if(idx < 0) {
            return str.length();
        }
        return idx;
    }

    static String appendDot(String name) {
        int len = name.length();
        if(len > 0 && name.charAt(len-1) != '.') {
            name = name.concat(".");
        }
        return name;
    }

    static int[] parseIntArrayFromAttribute(XmlPullParser xpp, String attribute) throws XmlPullParserException {
        try {
            String value = getAttributeNotNull(xpp, attribute);
            return parseIntArray(value);
        } catch(NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "Unable to parse", xpp, ex).initCause(ex));
        }
    }

    static int[] parseIntArray(String str) {
        int[] result = new int[countElements(str)];
        parseIntArray(str, result, 0, result.length);
        return result;
    }
    
    static void parseIntArray(String str, int[] result, int offset, int elements) {
        int pos = 0;
        for(int resultIdx=0 ; resultIdx<elements ; resultIdx++) {
            int comma = indexOf(str, ',', pos);
            result[resultIdx+offset] = Integer.parseInt(str.substring(pos, comma));
            pos = comma + 1;
        }
        if(pos < str.length()) {
            throw new NumberFormatException("to many data");
        }
    }

    static int countElements(String str) {
        int count = 0;
        for(int pos=0 ; pos<str.length() ;) {
            count++;
            pos = indexOf(str, ',', pos) + 1;
        }
        return count;
    }

    static<V> SortedMap<String,V> find(SortedMap<String,V> map, String baseName) {
        return map.subMap(baseName, baseName.concat("\uFFFF"));
    }

    static<V> Map<String,V> resolve(SortedMap<String,V> map, String ref, String name) {
        name = ParserUtil.appendDot(name);
        int refLen = ref.length() - 1;
        ref = ref.substring(0, refLen);

        SortedMap<String,V> matched = find(map, ref);
        if(matched.isEmpty()) {
            System.out.println("No match found: " + ref);
            return matched;
        }

        HashMap<String, V> result = new HashMap<String, V>();
        for(Map.Entry<String, V> texEntry : matched.entrySet()) {
            String entryName = texEntry.getKey();
            assert entryName.startsWith(ref);
            result.put(name.concat(entryName.substring(refLen)), texEntry.getValue());
        }

        return result;
    }
}
