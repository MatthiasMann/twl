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

import de.matthiasmann.twl.Alignment;
import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.ListBox;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.PositionAnimatedPanel;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.renderer.FontParameter;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.utils.AbstractMathInterpreter;
import de.matthiasmann.twl.utils.StateExpression;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * The theme manager
 *
 * @author Matthias Mann
 */
public class ThemeManager {

    private static final Logger logger = Logger.getLogger(ThemeManager.class.getName());
    private static final HashMap<String, Class<? extends Enum>> enums =
            new HashMap<String, Class<? extends Enum>>();
    
    static {
        registerEnumType("alignment", Alignment.class);
        registerEnumType("direction", PositionAnimatedPanel.Direction.class);
    }

    static final Object NULL = new Object();
    
    private final Renderer renderer;
    private final ImageManager imageManager;
    private final HashMap<String, Font> fonts;
    private final HashMap<String, ThemeInfoImpl> themes;
    private final HashMap<String, InputMapImpl> inputMaps;
    private final HashMap<String, Object> constants;
    private final MathInterpreter mathInterpreter;
    private Font defaultFont;
    private Font firstFont;

    final ParameterMapImpl emptyMap;
    final ParameterListImpl emptyList;
    
    private ThemeManager(Renderer renderer) throws XmlPullParserException, IOException {
        this.renderer = renderer;
        this.imageManager = new ImageManager(renderer);
        this.fonts  = new HashMap<String, Font>();
        this.themes = new HashMap<String, ThemeInfoImpl>();
        this.inputMaps = new HashMap<String, InputMapImpl>();
        this.constants = new HashMap<String, Object>();
        this.emptyMap = new ParameterMapImpl(this);
        this.emptyList = new ParameterListImpl(this);
        this.mathInterpreter = new MathInterpreter();

        insertDefaultConstants();
    }


    /**
     * Releases all OpenGL resources
     */
    public void destroy() {
        imageManager.destroy();
        for(Font font : fonts.values()) {
            font.destroy();
        }
    }
    
    public Font getDefaultFont() {
        return defaultFont;
    }
    
    public static ThemeManager createThemeManager(URL url, Renderer renderer) throws IOException {
        if(url == null) {
            throw new NullPointerException("url");
        }
        if(renderer == null) {
            throw new NullPointerException("renderer");
        }
        try {
            ThemeManager tm = new ThemeManager(renderer);
            tm.parseThemeFile(url);
            if(tm.defaultFont == null) {
                tm.defaultFont = tm.firstFont;
            }
            return tm;
        } catch (XmlPullParserException ex) {
            throw (IOException)(new IOException().initCause(ex));
        }
    }
    
    public static<E extends Enum> void registerEnumType(String name, Class<E> enumClazz) {
        if(!enumClazz.isEnum()) {
            throw new IllegalArgumentException("not an enum class");
        }
        Class curClazz = enums.get(name);
        if(curClazz != null && curClazz != enumClazz) {
            throw new IllegalArgumentException("Enum type name \"" + name +
                    "\" is already in use by " + curClazz);
        }
        enums.put(name, enumClazz);
    }
    
    public ThemeInfo findThemeInfo(String themePath) {
        int start = ParserUtil.indexOf(themePath, '.', 0);
        ThemeInfo info = themes.get(themePath.substring(0, start));
        while(info != null && ++start < themePath.length()) {
            int next = ParserUtil.indexOf(themePath, '.', start);
            info = info.getChildTheme(themePath.substring(start, next));
            start = next;
        }
        if(info == null) {
            logger.warning("Could not find theme: " + themePath);
        }
        return info;
    }
    
    public Image getImageNoWarning(String name) {
        return imageManager.getImage(name);
    }

    public Image getImage(String name) {
        Image img = imageManager.getImage(name);
        if(img == null) {
            logger.warning("Could not find image: " + name);
        }
        return img;
    }

    public Object getCursor(String name) {
        return imageManager.getCursor(name);
    }

    public final void insertConstant(String name, Object value) {
        if(constants.containsKey(name)) {
            throw new IllegalArgumentException("Constant '"+name+"' already declared");
        }
        if(value == null) {
            value = NULL;
        }
        constants.put(name, value);
    }
    
    protected void insertDefaultConstants() {
        insertConstant("SINGLE_COLUMN", ListBox.SINGLE_COLUMN);
    }

    private static final Class[] XPP_CLASS = {XmlPullParser.class};
    private void parseThemeFile(URL url) throws XmlPullParserException, IOException {
        XmlPullParser xpp = null;
        InputStream is = null;

        try {
            xpp = (XmlPullParser)url.getContent(XPP_CLASS);
        } catch (IOException ex) {
            // ignore
        }

        try {
            if(xpp == null) {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                factory.setValidating(false);
                xpp = factory.newPullParser();
                is = url.openStream();
                if(is == null) {
                    throw new FileNotFoundException(url.toString());
                }
                xpp.setInput(is, "UTF8");
            }

            xpp.require(XmlPullParser.START_DOCUMENT, null, null);
            xpp.nextTag();
            parseThemeFile(xpp, url);
        } catch (Exception ex) {
            throw (IOException)(new IOException("while parsing Theme XML: " + url).initCause(ex));
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }

    private void parseThemeFile(XmlPullParser xpp, URL baseUrl) throws XmlPullParserException, IOException {
        xpp.require(XmlPullParser.START_TAG, null, "themes");
        xpp.nextTag();

        while(xpp.getEventType() != XmlPullParser.END_TAG) {
            xpp.require(XmlPullParser.START_TAG, null, null);
            final String tagName = xpp.getName();
            if("textures".equals(tagName)) {
                imageManager.parseTextures(xpp, baseUrl);
            } else if("include".equals(tagName)) {
                String fontFileName = ParserUtil.getAttributeNotNull(xpp, "filename");
                parseThemeFile(new URL(baseUrl, fontFileName));
                xpp.nextTag();
            } else {
                final String name = ParserUtil.getAttributeNotNull(xpp, "name");
                if("theme".equals(tagName)) {
                    if(themes.containsKey(name)) {
                        throw new XmlPullParserException("theme \"" + name + "\" already defined", xpp, null);
                    }
                    themes.put(name, parseTheme(xpp, name, null, baseUrl));
                } else if("inputMapDef".equals(tagName)) {
                    if(inputMaps.containsKey(name)) {
                        throw new XmlPullParserException("inputMap \"" + name + "\" already defined", xpp, null);
                    }
                    inputMaps.put(name, parseInputMap(xpp));
                } else if("fontDef".equals(tagName)) {
                    if(fonts.containsKey(name)) {
                        throw new XmlPullParserException("font \"" + name + "\" already defined", xpp, null);
                    }
                    boolean makeDefault = ParserUtil.parseBoolFromAttribute(xpp, "default", false);
                    Font font = parseFont(xpp, baseUrl);
                    fonts.put(name, font);
                    if(firstFont == null) {
                        firstFont = font;
                    }
                    if(makeDefault) {
                        if(defaultFont != null) {
                            throw new XmlPullParserException("default font already set", xpp, null);
                        }
                        defaultFont = font;
                    }
                } else if("constantDef".equals(tagName)) {
                    insertConstant(name, parseParam(xpp, baseUrl));
                } else {
                    throw new XmlPullParserException("Unexpected '"+tagName+"'", xpp, null);
                }
            }
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            xpp.nextTag();
        }
        xpp.require(XmlPullParser.END_TAG, null, "themes");
    }

    private InputMapImpl getInputMap(XmlPullParser xpp, String name) throws XmlPullParserException {
        InputMapImpl im = inputMaps.get(name);
        if(im == null) {
            throw new XmlPullParserException("Undefined input map: " + name, xpp, null);
        }
        return im;
    }

    private InputMapImpl parseInputMap(XmlPullParser xpp) throws XmlPullParserException, IOException {
        InputMapImpl base = null;
        String baseName = xpp.getAttributeValue(null, "ref");
        if(baseName != null) {
            base = getInputMap(xpp, baseName);
        }

        ArrayList<KeyStroke> keyStrokes = new ArrayList<KeyStroke>();
        xpp.nextTag();
        while(xpp.getEventType() != XmlPullParser.END_TAG) {
            xpp.require(XmlPullParser.START_TAG, null, "action");
            String name = ParserUtil.getAttributeNotNull(xpp, "name");
            String key = xpp.nextText();
            try {
                KeyStroke ks = KeyStroke.parse(key, name);
                keyStrokes.add(ks);
            } catch (IllegalArgumentException ex) {
                throw (XmlPullParserException)(new XmlPullParserException(
                        ex.getMessage(), xpp, ex).initCause(ex));
            }
            xpp.require(XmlPullParser.END_TAG, null, "action");
            xpp.nextTag();
        }

        InputMapImpl im;
        if(base != null) {
            if(keyStrokes.isEmpty()) {
                return base;
            }
            im = new InputMapImpl(base);
        } else {
            im = new InputMapImpl();
        }
        im.addMappings(keyStrokes);
        return im;
    }

    private Font parseFont(XmlPullParser xpp, URL baseUrl) throws XmlPullParserException, IOException {
        Map<String, String> params = ParserUtil.getAttributeMap(xpp);
        ArrayList<FontParameter> fontParams = new ArrayList<FontParameter>();
        params.remove("name");
        params.remove("default");
        xpp.nextTag();
        while(xpp.getEventType() != XmlPullParser.END_TAG) {
            xpp.require(XmlPullParser.START_TAG, null, "fontParam");
            StateExpression cond = ParserUtil.parseCondition(xpp);
            if(cond == null) {
                throw new XmlPullParserException("Condition required", xpp, null);
            }
            Map<String, String> condParams = ParserUtil.getAttributeMap(xpp);
            condParams.remove("if");
            condParams.remove("unless");
            fontParams.add(new FontParameter(cond, condParams));
            xpp.nextTag();
            xpp.require(XmlPullParser.END_TAG, null, "fontParam");
            xpp.nextTag();
        }
        return renderer.loadFont(baseUrl, params, fontParams);
    }

    private void parseThemeWildcardRef(XmlPullParser xpp, ThemeInfoImpl parent) throws IOException, XmlPullParserException {
        String ref = xpp.getAttributeValue(null, "ref");
        if(parent == null) {
            throw new XmlPullParserException("Can't declare wildcard themes on top level", xpp, null);
        }
        if(ref == null) {
            throw new XmlPullParserException("Reference required for wildcard theme", xpp, null);
        }
        if(!ref.endsWith("*")) {
            throw new XmlPullParserException("Wildcard reference must end with '*'", xpp, null);
        }
        String refPath = ref.substring(0, ref.length()-1);
        if(refPath.length() > 0 && !refPath.endsWith(".")) {
            throw new XmlPullParserException("Wildcard must end with \".*\" or be \"*\"", xpp, null);
        }
        parent.wildcardImportPath = refPath;
        xpp.nextTag();
    }
    
    private ThemeInfoImpl parseTheme(XmlPullParser xpp, String themeName, ThemeInfoImpl parent, URL baseUrl) throws IOException, XmlPullParserException {
        ParserUtil.checkNameNotEmpty(themeName, xpp);
        if(themeName.indexOf('.') >= 0 || themeName.indexOf('*') >= 0) {
            throw new XmlPullParserException("name must not contain '.' or '*'", xpp, null);
        }
        ThemeInfoImpl ti = new ThemeInfoImpl(this, themeName, parent);
        ThemeInfoImpl oldEnv = mathInterpreter.setEnv(ti);
        try {
            if(ParserUtil.parseBoolFromAttribute(xpp, "merge", false)) {
                if(parent == null) {
                    throw new XmlPullParserException("Can't merge on top level", xpp, null);
                }
                ThemeInfoImpl tiPrev = parent.children.get(themeName);
                if(tiPrev != null) {
                    ti.copy(tiPrev);
                }
            }
            String ref = xpp.getAttributeValue(null, "ref");
            if(ref != null) {
                ThemeInfoImpl tiRef = (ThemeInfoImpl)findThemeInfo(ref);
                if(tiRef == null) {
                    throw new XmlPullParserException("referenced theme info not found: " + ref, xpp, null);
                }
                ti.copy(tiRef);
            }
            ti.maybeUsedFromWildcard = ParserUtil.parseBoolFromAttribute(xpp, "allowWildcard", false);
            xpp.nextTag();
            while(xpp.getEventType() != XmlPullParser.END_TAG) {
                xpp.require(XmlPullParser.START_TAG, null, null);
                final String tagName = xpp.getName();
                final String name = xpp.getAttributeValue(null, "name");
                if("param".equals(tagName)) {
                    Map<String, ?> entries = parseParam(xpp, baseUrl);
                    ti.params.putAll(entries);
                } else if("theme".equals(tagName)) {
                    if(name.length() == 0) {
                        parseThemeWildcardRef(xpp, ti);
                    } else {
                        ThemeInfoImpl tiChild = parseTheme(xpp, name, ti, baseUrl);
                        ti.children.put(name, tiChild);
                    }
                } else {
                    throw new XmlPullParserException("Unexpected '"+tagName+"'", xpp, null);
                }
                xpp.require(XmlPullParser.END_TAG, null, tagName);
                xpp.nextTag();
            }
        } finally {
            mathInterpreter.setEnv(oldEnv);
        }
        return ti;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> parseParam(XmlPullParser xpp, URL baseUrl) throws XmlPullParserException, IOException {
        try {
            xpp.require(XmlPullParser.START_TAG, null, "param");
            String name = xpp.getAttributeValue(null, "name");
            xpp.nextTag();
            String tagName = xpp.getName();
            Object value = parseValue(xpp, tagName, name, baseUrl);
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            xpp.nextTag();
            xpp.require(XmlPullParser.END_TAG, null, "param");
            if(value instanceof Map) {
                return (Map<String, ?>)value;
            }
            ParserUtil.checkNameNotEmpty(name, xpp);
            return Collections.singletonMap(name, value);
        } catch (NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "unable to parse value", xpp, ex).initCause(ex));
        }
    }

    private ParameterListImpl parseList(XmlPullParser xpp, URL baseUrl) throws XmlPullParserException, IOException {
        ParameterListImpl result = new ParameterListImpl(this);
        xpp.nextTag();
        while(xpp.getEventType() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            Object obj = parseValue(xpp, tagName, null, baseUrl);
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            result.params.add(obj);
            xpp.nextTag();
        }
        return result;
    }
    
    private ParameterMapImpl parseMap(XmlPullParser xpp, URL baseUrl) throws XmlPullParserException, IOException, NumberFormatException {
        ParameterMapImpl result = new ParameterMapImpl(this);
        xpp.nextTag();
        while(xpp.getEventType() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            Map<String, ?> params = parseParam(xpp, baseUrl);
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            result.addParameters(params);
            xpp.nextTag();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object parseValue(XmlPullParser xpp, String tagName, String wildcardName, URL baseUrl) throws XmlPullParserException, IOException, NumberFormatException {
        try {
            if("list".equals(tagName)) {
                return parseList(xpp, baseUrl);
            }
            if("map".equals(tagName)) {
                return parseMap(xpp, baseUrl);
            }
            if("inputMapDef".equals(tagName)) {
                return parseInputMap(xpp);
            }
            if("fontDef".equals(tagName)) {
                return parseFont(xpp, baseUrl);
            }

            String enumType = xpp.getAttributeValue(null, "type");
            String value = xpp.nextText();

            if("color".equals(tagName)) {
                return ParserUtil.parseColor(xpp, value);
            }
            if("float".equals(tagName)) {
                return parseMath(xpp, value).floatValue();
            }
            if("int".equals(tagName)) {
                return parseMath(xpp, value).intValue();
            }
            if("string".equals(tagName)) {
                return value;
            }
            if("font".equals(tagName)) {
                Font font = fonts.get(value);
                if(font == null) {
                    throw new XmlPullParserException("Font \"" + value + "\" not found", xpp, null);
                }
                return font;
            }
            if("enum".equals(tagName)) {
                if(enumType == null) {
                    ParserUtil.missingAttribute(xpp, "type");
                }
                Class<? extends Enum> enumClazz = enums.get(enumType);
                if(enumClazz == null) {
                    throw new XmlPullParserException("enum type \"" + enumType + "\" not registered", xpp, null);
                }
                return ParserUtil.parseEnum(xpp, enumClazz, value);
            }
            if("bool".equals(tagName)) {
                return ParserUtil.parseBool(xpp, value);
            }
            if("border".equals(tagName)) {
                return parseObject(xpp, value, Border.class);
            }
            if("dimension".equals(tagName)) {
                return parseObject(xpp, value, Dimension.class);
            }
            if("gap".equals(tagName)) {
                return parseObject(xpp, value, DialogLayout.Gap.class);
            }
            if("constant".equals(tagName)) {
                Object result = constants.get(value);
                if(result == null) {
                    throw new XmlPullParserException("Unknown constant: " + value, xpp, null);
                }
                if(result == NULL) {
                    result = null;
                }
                return result;
            }
            if("image".equals(tagName)) {
                if(value.endsWith(".*")) {
                    if(wildcardName == null) {
                        throw new IllegalArgumentException("Wildcard's not allowed");
                    }
                    return imageManager.getImages(value, wildcardName);
                }
                return imageManager.getReferencedImage(xpp, value);
            }
            if("cursor".equals(tagName)) {
                if(value.endsWith(".*")) {
                    if(wildcardName == null) {
                        throw new IllegalArgumentException("Wildcard's not allowed");
                    }
                    return imageManager.getCursors(value, wildcardName);
                }
                return imageManager.getReferencedCursor(xpp, value);
            }
            if("inputMap".equals(tagName)) {
                return getInputMap(xpp, value);
            }
            throw new XmlPullParserException("Unknown type \"" + tagName + "\" specified", xpp, null);
        } catch (NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "unable to parse value", xpp, ex).initCause(ex));
        }
    }

    private Number parseMath(XmlPullParser xpp, String str) throws XmlPullParserException {
        try {
            return mathInterpreter.execute(str);
        } catch(ParseException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "unable to evaluate", xpp, ex).initCause(ex));
        }
    }

    private<T> T parseObject(XmlPullParser xpp, String str, Class<T> type) throws XmlPullParserException {
        try {
            return mathInterpreter.executeCreateObject(str, type);
        } catch(ParseException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "unable to evaluate", xpp, ex).initCause(ex));
        }
    }

    ThemeInfo resolveWildcard(String base, String name) {
        assert(base.length() == 0 || base.endsWith("."));
        String fullPath = base.concat(name);
        ThemeInfo info = findThemeInfo(fullPath);
        if(info != null && ((ThemeInfoImpl)info).maybeUsedFromWildcard) {
            return info;
        }
        return null;
    }

    class MathInterpreter extends AbstractMathInterpreter {
        private ThemeInfoImpl env;

        public ThemeInfoImpl setEnv(ThemeInfoImpl env) {
            ThemeInfoImpl oldEnv = this.env;
            this.env = env;
            return oldEnv;
        }

        public void accessVariable(String name) {
            if(env != null) {
                Object obj = env.getParameterValue(name, false);
                if(obj != null) {
                    push(obj);
                    return;
                }
                obj = env.getChildTheme(name);
                if(obj != null) {
                    push(obj);
                    return;
                }
            }
            Object obj = constants.get(name);
            if(obj != null) {
                push(obj);
                return;
            }
            throw new IllegalArgumentException("variable not found: " + name);
        }

        @Override
        protected Object accessField(Object obj, String field) {
            if(obj instanceof ParameterMapImpl) {
                Object result = ((ParameterMapImpl)obj).getParameterValue(field, false);
                if(result == null) {
                    throw new IllegalArgumentException("field not found: " + field);
                }
                return result;
            }
            if((obj instanceof Image) && "border".equals(field)) {
                Border border = null;
                if(obj instanceof HasBorder) {
                    border = ((HasBorder)obj).getBorder();
                }
                return (border != null) ? border : Border.ZERO;
            }
            return super.accessField(obj, field);
        }
    }
}
