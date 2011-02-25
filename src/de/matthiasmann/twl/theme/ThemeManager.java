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
import de.matthiasmann.twl.DebugHook;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.InputMap;
import de.matthiasmann.twl.KeyStroke;
import de.matthiasmann.twl.ListBox;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.PositionAnimatedPanel;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.FontParameter;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.utils.AbstractMathInterpreter;
import de.matthiasmann.twl.utils.StateExpression;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.utils.XMLParser;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The theme manager
 *
 * @author Matthias Mann
 */
public class ThemeManager {

    private static final HashMap<String, Class<? extends Enum<?>>> enums =
            new HashMap<String, Class<? extends Enum<?>>>();
    
    static {
        registerEnumType("alignment", Alignment.class);
        registerEnumType("direction", PositionAnimatedPanel.Direction.class);
    }

    static final Object NULL = new Object();
    
    private final Renderer renderer;
    private final CacheContext cacheContext;
    private final ImageManager imageManager;
    private final HashMap<String, Font> fonts;
    private final HashMap<String, ThemeInfoImpl> themes;
    private final HashMap<String, InputMap> inputMaps;
    final HashMap<String, Object> constants;
    private final MathInterpreter mathInterpreter;
    private Font defaultFont;
    private Font firstFont;

    final ParameterMapImpl emptyMap;
    final ParameterListImpl emptyList;

    private ThemeManager(Renderer renderer, CacheContext cacheContext) throws XmlPullParserException, IOException {
        this.renderer = renderer;
        this.cacheContext = cacheContext;
        this.imageManager = new ImageManager(renderer);
        this.fonts  = new HashMap<String, Font>();
        this.themes = new HashMap<String, ThemeInfoImpl>();
        this.inputMaps = new HashMap<String, InputMap>();
        this.constants = new HashMap<String, Object>();
        this.emptyMap = new ParameterMapImpl(this, null);
        this.emptyList = new ParameterListImpl(this, null);
        this.mathInterpreter = new MathInterpreter();
    }

    /**
     * Returns the associated cache context which was used to load this theme.
     * @return the cache context
     * @see #createThemeManager(java.net.URL, de.matthiasmann.twl.renderer.Renderer, de.matthiasmann.twl.renderer.CacheContext) 
     */
    public CacheContext getCacheContext() {
        return cacheContext;
    }

    /**
     * Destroys the CacheContext and releases all OpenGL resources
     * @see CacheContext#destroy()
     * @see #getCacheContext()
     */
    public void destroy() {
        for(Font font : fonts.values()) {
            font.destroy();
        }
        cacheContext.destroy();
    }
    
    public Font getDefaultFont() {
        return defaultFont;
    }

    /**
     * Loads the specified theme using the provided renderer and a new cache context.
     *
     * This is equivalent to calling {@code createThemeManager(url, renderer, renderer.createNewCacheContext())}
     *
     * @param url The URL of the theme
     * @param renderer The renderer which is used to load and render the resources
     * @return a new ThemeManager
     * @throws IOException if an error occured while loading
     * @throws NullPointerException if one of the passed parameters is {@code null}
     * @see #createThemeManager(java.net.URL, de.matthiasmann.twl.renderer.Renderer, de.matthiasmann.twl.renderer.CacheContext)
     * @see #destroy() 
     */
    public static ThemeManager createThemeManager(URL url, Renderer renderer) throws IOException {
        if(url == null) {
            throw new IllegalArgumentException("url is null");
        }
        if(renderer == null) {
            throw new IllegalArgumentException("renderer is null");
        }
        return createThemeManager(url, renderer, renderer.createNewCacheContext());
    }

    /**
     * Loads the specified theme using the provided renderer and cache context.
     *
     * The provided {@code cacheContext} is set active in the provided {@code renderer}.
     *
     * The cache context is stored inside the created ThemeManager. Calling
     * {@code destroy()} on the returned ThemeManager instance will also destroy
     * the cache context.
     *
     * @param url The URL of the theme
     * @param renderer The renderer which is used to load and render the resources
     * @param cacheContext The cache context into which the resources are loaded
     * @return a new ThemeManager
     * @throws IOException if an error occured while loading
     * @throws NullPointerException if one of the passed parameters is {@code null}
     * @see Renderer#setActiveCacheContext(de.matthiasmann.twl.renderer.CacheContext)
     * @see #destroy() 
     */
    public static ThemeManager createThemeManager(URL url, Renderer renderer, CacheContext cacheContext) throws IOException {
        if(url == null) {
            throw new IllegalArgumentException("url is null");
        }
        if(renderer == null) {
            throw new IllegalArgumentException("renderer is null");
        }
        if(cacheContext == null) {
            throw new IllegalArgumentException("cacheContext is null");
        }
        try {
            renderer.setActiveCacheContext(cacheContext);
            ThemeManager tm = new ThemeManager(renderer, cacheContext);
            tm.insertDefaultConstants();
            tm.parseThemeFile(url);
            if(tm.defaultFont == null) {
                tm.defaultFont = tm.firstFont;
            }
            return tm;
        } catch (XmlPullParserException ex) {
            throw (IOException)(new IOException().initCause(ex));
        }
    }
    
    public static<E extends Enum<E>> void registerEnumType(String name, Class<E> enumClazz) {
        if(!enumClazz.isEnum()) {
            throw new IllegalArgumentException("not an enum class");
        }
        Class<?> curClazz = enums.get(name);
        if(curClazz != null && curClazz != enumClazz) {
            throw new IllegalArgumentException("Enum type name \"" + name +
                    "\" is already in use by " + curClazz);
        }
        enums.put(name, enumClazz);
    }

    public ThemeInfo findThemeInfo(String themePath) {
        return findThemeInfo(themePath, true);
    }

    private ThemeInfo findThemeInfo(String themePath, boolean warn) {
        int start = TextUtil.indexOf(themePath, '.', 0);
        ThemeInfo info = themes.get(themePath.substring(0, start));
        while(info != null && ++start < themePath.length()) {
            int next = TextUtil.indexOf(themePath, '.', start);
            info = info.getChildTheme(themePath.substring(start, next));
            start = next;
        }
        if(info == null && warn) {
            DebugHook.getDebugHook().missingTheme(themePath);
        }
        return info;
    }
    
    public Image getImageNoWarning(String name) {
        return imageManager.getImage(name);
    }

    public Image getImage(String name) {
        Image img = imageManager.getImage(name);
        if(img == null) {
            DebugHook.getDebugHook().missingImage(name);
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

    private void parseThemeFile(URL url) throws XmlPullParserException, IOException {
        try {
            XMLParser xmlp = new XMLParser(url);
            try {
                xmlp.setLoggerName(ThemeManager.class.getName());
                xmlp.require(XmlPullParser.START_DOCUMENT, null, null);
                xmlp.nextTag();
                parseThemeFile(xmlp, url);
            } finally {
                xmlp.close();
            }
        } catch (Exception ex) {
            throw (IOException)(new IOException("while parsing Theme XML: " + url).initCause(ex));
        }
    }

    private void parseThemeFile(XMLParser xmlp, URL baseUrl) throws XmlPullParserException, IOException {
        xmlp.require(XmlPullParser.START_TAG, null, "themes");
        xmlp.nextTag();

        while(!xmlp.isEndTag()) {
            xmlp.require(XmlPullParser.START_TAG, null, null);
            final String tagName = xmlp.getName();
            if("images".equals(tagName) || "textures".equals(tagName)) {
                imageManager.parseImages(xmlp, baseUrl);
            } else if("include".equals(tagName)) {
                String fontFileName = xmlp.getAttributeNotNull("filename");
                parseThemeFile(new URL(baseUrl, fontFileName));
                xmlp.nextTag();
            } else {
                final String name = xmlp.getAttributeNotNull("name");
                if("theme".equals(tagName)) {
                    if(themes.containsKey(name)) {
                        throw xmlp.error("theme \"" + name + "\" already defined");
                    }
                    themes.put(name, parseTheme(xmlp, name, null, baseUrl));
                } else if("inputMapDef".equals(tagName)) {
                    if(inputMaps.containsKey(name)) {
                        throw xmlp.error("inputMap \"" + name + "\" already defined");
                    }
                    inputMaps.put(name, parseInputMap(xmlp, name, null));
                } else if("fontDef".equals(tagName)) {
                    if(fonts.containsKey(name)) {
                        throw xmlp.error("font \"" + name + "\" already defined");
                    }
                    boolean makeDefault = xmlp.parseBoolFromAttribute("default", false);
                    Font font = parseFont(xmlp, baseUrl);
                    fonts.put(name, font);
                    if(firstFont == null) {
                        firstFont = font;
                    }
                    if(makeDefault) {
                        if(defaultFont != null) {
                            throw xmlp.error("default font already set");
                        }
                        defaultFont = font;
                    }
                } else if("constantDef".equals(tagName)) {
                    Map<String, ?> value = parseParam(xmlp, baseUrl, "constantDef", null);
                    if(value.size() != 1) {
                        throw xmlp.error("constant definitions must define exactly 1 value");
                    }
                    insertConstant(name, value.values().iterator().next());
                } else {
                    throw xmlp.unexpected();
                }
            }
            xmlp.require(XmlPullParser.END_TAG, null, tagName);
            xmlp.nextTag();
        }
        xmlp.require(XmlPullParser.END_TAG, null, "themes");
    }

    private InputMap getInputMap(XMLParser xmlp, String name) throws XmlPullParserException {
        InputMap im = inputMaps.get(name);
        if(im == null) {
            throw xmlp.error("Undefined input map: " + name);
        }
        return im;
    }

    private InputMap parseInputMap(XMLParser xmlp, String name, ThemeInfoImpl parent) throws XmlPullParserException, IOException {
        InputMap base = InputMap.empty();
        if(xmlp.parseBoolFromAttribute("merge", false)) {
            if(parent == null) {
                throw xmlp.error("Can't merge on top level");
            }
            Object o = parent.params.get(name);
            if(o instanceof InputMap) {
                base = (InputMap)o;
            } else if(o != null) {
                throw xmlp.error("Can only merge with inputMap - found a " + o.getClass().getSimpleName());
            }
        }
        String baseName = xmlp.getAttributeValue(null, "ref");
        if(baseName != null) {
            base = base.addKeyStrokes(getInputMap(xmlp, baseName));
        }

        xmlp.nextTag();

        LinkedHashSet<KeyStroke> keyStrokes = InputMap.parseBody(xmlp);
        InputMap im = base.addKeyStrokes(keyStrokes);
        return im;
    }

    private Font parseFont(XMLParser xmlp, URL baseUrl) throws XmlPullParserException, IOException {
        Map<String, String> params = xmlp.getUnusedAttributes();
        ArrayList<FontParameter> fontParams = new ArrayList<FontParameter>();
        params.remove("name");
        params.remove("default");
        xmlp.nextTag();
        while(!xmlp.isEndTag()) {
            xmlp.require(XmlPullParser.START_TAG, null, "fontParam");
            StateExpression cond = ParserUtil.parseCondition(xmlp);
            if(cond == null) {
                throw xmlp.error("Condition required");
            }
            Map<String, String> condParams = xmlp.getUnusedAttributes();
            condParams.remove("if");
            condParams.remove("unless");
            fontParams.add(new FontParameter(cond, condParams));
            xmlp.nextTag();
            xmlp.require(XmlPullParser.END_TAG, null, "fontParam");
            xmlp.nextTag();
        }
        return renderer.loadFont(baseUrl, params, fontParams);
    }

    private void parseThemeWildcardRef(XMLParser xmlp, ThemeInfoImpl parent) throws IOException, XmlPullParserException {
        String ref = xmlp.getAttributeValue(null, "ref");
        if(parent == null) {
            throw xmlp.error("Can't declare wildcard themes on top level");
        }
        if(ref == null) {
            throw xmlp.error("Reference required for wildcard theme");
        }
        if(!ref.endsWith("*")) {
            throw xmlp.error("Wildcard reference must end with '*'");
        }
        String refPath = ref.substring(0, ref.length()-1);
        if(refPath.length() > 0 && !refPath.endsWith(".")) {
            throw xmlp.error("Wildcard must end with \".*\" or be \"*\"");
        }
        parent.wildcardImportPath = refPath;
        xmlp.nextTag();
    }
    
    private ThemeInfoImpl parseTheme(XMLParser xmlp, String themeName, ThemeInfoImpl parent, URL baseUrl) throws IOException, XmlPullParserException {
        ParserUtil.checkNameNotEmpty(themeName, xmlp);
        if(themeName.indexOf('.') >= 0 || themeName.indexOf('*') >= 0) {
            throw xmlp.error("name must not contain '.' or '*'");
        }
        ThemeInfoImpl ti = new ThemeInfoImpl(this, themeName, parent);
        ThemeInfoImpl oldEnv = mathInterpreter.setEnv(ti);
        try {
            if(xmlp.parseBoolFromAttribute("merge", false)) {
                if(parent == null) {
                    throw xmlp.error("Can't merge on top level");
                }
                ThemeInfoImpl tiPrev = parent.children.get(themeName);
                if(tiPrev != null) {
                    ti.copy(tiPrev);
                }
            }
            String ref = xmlp.getAttributeValue(null, "ref");
            if(ref != null) {
                ThemeInfoImpl tiRef = (ThemeInfoImpl)findThemeInfo(ref);
                if(tiRef == null) {
                    throw xmlp.error("referenced theme info not found: " + ref);
                }
                ti.copy(tiRef);
            }
            ti.maybeUsedFromWildcard = xmlp.parseBoolFromAttribute("allowWildcard", false);
            xmlp.nextTag();
            while(!xmlp.isEndTag()) {
                xmlp.require(XmlPullParser.START_TAG, null, null);
                final String tagName = xmlp.getName();
                final String name = xmlp.getAttributeNotNull("name");
                if("param".equals(tagName)) {
                    Map<String, ?> entries = parseParam(xmlp, baseUrl, "param", ti);
                    ti.params.putAll(entries);
                } else if("theme".equals(tagName)) {
                    if(name.length() == 0) {
                        parseThemeWildcardRef(xmlp, ti);
                    } else {
                        ThemeInfoImpl tiChild = parseTheme(xmlp, name, ti, baseUrl);
                        ti.children.put(name, tiChild);
                    }
                } else {
                    throw xmlp.unexpected();
                }
                xmlp.require(XmlPullParser.END_TAG, null, tagName);
                xmlp.nextTag();
            }
        } finally {
            mathInterpreter.setEnv(oldEnv);
        }
        return ti;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> parseParam(XMLParser xmlp, URL baseUrl, String tagName, ThemeInfoImpl parent) throws XmlPullParserException, IOException {
        try {
            xmlp.require(XmlPullParser.START_TAG, null, tagName);
            String name = xmlp.getAttributeNotNull("name");
            xmlp.nextTag();
            String valueTagName = xmlp.getName();
            Object value = parseValue(xmlp, valueTagName, name, baseUrl, parent);
            xmlp.require(XmlPullParser.END_TAG, null, valueTagName);
            xmlp.nextTag();
            xmlp.require(XmlPullParser.END_TAG, null, tagName);
            if(value instanceof Map<?,?>) {
                return (Map<String, ?>)value;
            }
            ParserUtil.checkNameNotEmpty(name, xmlp);
            return Collections.singletonMap(name, value);
        } catch (NumberFormatException ex) {
            throw xmlp.error("unable to parse value", ex);
        }
    }

    private ParameterListImpl parseList(XMLParser xmlp, URL baseUrl, ThemeInfoImpl parent) throws XmlPullParserException, IOException {
        ParameterListImpl result = new ParameterListImpl(this, parent);
        xmlp.nextTag();
        while(xmlp.isStartTag()) {
            String tagName = xmlp.getName();
            Object obj = parseValue(xmlp, tagName, null, baseUrl, parent);
            xmlp.require(XmlPullParser.END_TAG, null, tagName);
            result.params.add(obj);
            xmlp.nextTag();
        }
        return result;
    }
    
    private ParameterMapImpl parseMap(XMLParser xmlp, URL baseUrl, String name, ThemeInfoImpl parent) throws XmlPullParserException, IOException, NumberFormatException {
        ParameterMapImpl result = new ParameterMapImpl(this, parent);
        if(xmlp.parseBoolFromAttribute("merge", false)) {
            if(parent == null) {
                throw xmlp.error("Can't merge on top level");
            }
            Object obj = parent.params.get(name);
            if(obj instanceof ParameterMapImpl) {
                ParameterMapImpl base = (ParameterMapImpl)obj;
                result.params.putAll(base.params);
            } else if(obj != null) {
                throw xmlp.error("Can only merge with map - found a " + obj.getClass().getSimpleName());
            }
        }
        String ref = xmlp.getAttributeValue(null, "ref");
        if(ref != null) {
            Object obj = parent.params.get(ref);
            if(obj == null) {
                obj = constants.get(ref);
                if(obj == null) {
                    throw new IOException("Referenced map not found: " + ref);
                }
            }
            if(obj instanceof ParameterMapImpl) {
                ParameterMapImpl base = (ParameterMapImpl)obj;
                result.params.putAll(base.params);
            } else {
                throw new IOException("Expected a map got a " + obj.getClass().getSimpleName());
            }
        }
        xmlp.nextTag();
        while(xmlp.isStartTag()) {
            String tagName = xmlp.getName();
            Map<String, ?> params = parseParam(xmlp, baseUrl, "param", parent);
            xmlp.require(XmlPullParser.END_TAG, null, tagName);
            result.addParameters(params);
            xmlp.nextTag();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object parseValue(XMLParser xmlp, String tagName, String wildcardName, URL baseUrl, ThemeInfoImpl parent) throws XmlPullParserException, IOException, NumberFormatException {
        try {
            if("list".equals(tagName)) {
                return parseList(xmlp, baseUrl, parent);
            }
            if("map".equals(tagName)) {
                return parseMap(xmlp, baseUrl, wildcardName, parent);
            }
            if("inputMapDef".equals(tagName)) {
                return parseInputMap(xmlp, wildcardName, parent);
            }
            if("fontDef".equals(tagName)) {
                return parseFont(xmlp, baseUrl);
            }
            if("enum".equals(tagName)) {
                String enumType = xmlp.getAttributeNotNull("type");
                Class<? extends Enum> enumClazz = enums.get(enumType);
                if(enumClazz == null) {
                    throw xmlp.error("enum type \"" + enumType + "\" not registered");
                }
                return xmlp.parseEnumFromText(enumClazz);
            }
            if("bool".equals(tagName)) {
                return xmlp.parseBoolFromText();
            }

            String value = xmlp.nextText();

            if("color".equals(tagName)) {
                return ParserUtil.parseColor(xmlp, value);
            }
            if("float".equals(tagName)) {
                return parseMath(xmlp, value).floatValue();
            }
            if("int".equals(tagName)) {
                return parseMath(xmlp, value).intValue();
            }
            if("string".equals(tagName)) {
                return value;
            }
            if("font".equals(tagName)) {
                Font font = fonts.get(value);
                if(font == null) {
                    throw xmlp.error("Font \"" + value + "\" not found");
                }
                return font;
            }
            if("border".equals(tagName)) {
                return parseObject(xmlp, value, Border.class);
            }
            if("dimension".equals(tagName)) {
                return parseObject(xmlp, value, Dimension.class);
            }
            if("gap".equals(tagName) || "size".equals(tagName)) {
                return parseObject(xmlp, value, DialogLayout.Gap.class);
            }
            if("constant".equals(tagName)) {
                Object result = constants.get(value);
                if(result == null) {
                    throw xmlp.error("Unknown constant: " + value);
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
                return imageManager.getReferencedImage(xmlp, value);
            }
            if("cursor".equals(tagName)) {
                if(value.endsWith(".*")) {
                    if(wildcardName == null) {
                        throw new IllegalArgumentException("Wildcard's not allowed");
                    }
                    return imageManager.getCursors(value, wildcardName);
                }
                return imageManager.getReferencedCursor(xmlp, value);
            }
            if("inputMap".equals(tagName)) {
                return getInputMap(xmlp, value);
            }
            throw xmlp.error("Unknown type \"" + tagName + "\" specified");
        } catch (NumberFormatException ex) {
            throw xmlp.error("unable to parse value", ex);
        }
    }

    private Number parseMath(XMLParser xmlp, String str) throws XmlPullParserException {
        try {
            return mathInterpreter.execute(str);
        } catch(ParseException ex) {
            throw xmlp.error("unable to evaluate", ex);
        }
    }

    private<T> T parseObject(XMLParser xmlp, String str, Class<T> type) throws XmlPullParserException {
        try {
            return mathInterpreter.executeCreateObject(str, type);
        } catch(ParseException ex) {
            throw xmlp.error("unable to evaluate", ex);
        }
    }

    ThemeInfo resolveWildcard(String base, String name) {
        assert(base.length() == 0 || base.endsWith("."));
        String fullPath = base.concat(name);
        ThemeInfo info = findThemeInfo(fullPath, false);
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
