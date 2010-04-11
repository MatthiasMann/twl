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
package de.matthiasmann.twl.model;

import de.matthiasmann.twl.ParameterMap;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.ParameterStringParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * A simple HTML parser.
 * @author Matthias Mann
 */
public class HTMLTextAreaModel extends HasCallback implements TextAreaModel {
    
    private final ArrayList<Element> elements;
    private String html;
    private boolean needToParse;

    private final ArrayList<StyleInfo> styleStack;
    private final ArrayList<Container> containerStack;
    private final StringBuilder sb;
    private final int[] startLength;

    private boolean paragraphStart;
    private boolean paragraphEnd;
    private String href;

    public HTMLTextAreaModel() {
        this.elements = new ArrayList<Element>();
        this.styleStack = new ArrayList<StyleInfo>();
        this.containerStack = new ArrayList<Container>();
        this.sb = new StringBuilder();
        this.startLength = new int[2];
    }

    public HTMLTextAreaModel(String html) {
        this();
        setHtml(html);
    }

    public HTMLTextAreaModel(Reader r) throws IOException {
        this();
        readHTMLFromStream(r);
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
        this.elements.clear();
        needToParse = true;
        doCallback();
    }

    public void readHTMLFromStream(Reader r) throws IOException {
        char[] buf = new char[1024];
        int read, off = 0;
        while((read=r.read(buf, off, buf.length-off)) > 0) {
            off += read;
            if(off == buf.length) {
                char[] newBuf = new char[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, off);
                buf = newBuf;
            }
        }
        setHtml(new String(buf, 0, off));
    }

    public void readHTMLFromURL(URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            readHTMLFromStream(new InputStreamReader(in));
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(HTMLTextAreaModel.class.getName()).log(
                        Level.SEVERE, "Exception while closing InputStream", ex);
            }
        }
    }

    public Iterator<Element> iterator() {
        if(needToParse) {
            parseHTML();
            needToParse = false;
        }
        return elements.iterator();
    }

    private void addElement(Element e) {
        int numOpenContainer = containerStack.size();
        if(numOpenContainer > 0) {
            containerStack.get(numOpenContainer-1).elements.add(e);
        } else {
            elements.add(e);
        }
    }
    
    private void parseHTML() {
        try {
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            xppf.setNamespaceAware(false);
            xppf.setValidating(false);
            XmlPullParser xpp = xppf.newPullParser();
            xpp.setInput(new CompositeReader("<html>", html, "</html>"));
            xpp.require(XmlPullParser.START_DOCUMENT, null, null);
            xpp.nextTag();

            styleStack.clear();
            styleStack.add(new StyleInfo());
            containerStack.clear();
            sb.setLength(0);
            paragraphStart = false;
            paragraphEnd = false;
            href = null;

            int type;
            while((type=xpp.nextToken()) != XmlPullParser.END_DOCUMENT) {
                switch(type) {
                case XmlPullParser.START_TAG: {
                    String name = xpp.getName();
                    if("span".equals(name)) {
                        pushStyle(xpp, false);
                    }
                    if("img".equals(name)) {
                        finishText();
                        pushStyle(xpp, true);
                        String src = TextUtil.notNull(xpp.getAttributeValue(null, "src"));
                        String alt = xpp.getAttributeValue(null, "alt");
                        addElement(new ImageElementImpl(getStyle(), src, alt));
                    }
                    if("br".equals(name)) {
                        sb.append("\n");
                    }
                    if("p".equals(name)) {
                        finishText();
                        pushStyle(xpp, false);
                        paragraphStart = true;
                    }
                    if("button".equals(name)) {
                        finishText();
                        pushStyle(xpp, true);
                        String btnName = TextUtil.notNull(xpp.getAttributeValue(null, "name"));
                        String btnParam = TextUtil.notNull(xpp.getAttributeValue(null, "value"));
                        addElement(new WidgetElementImpl(getStyle(), btnName, btnParam));
                    }
                    if("li".equals(name)) {
                        finishText();
                        pushStyle(xpp, false);
                        ListElementImpl lei = new ListElementImpl(getStyle());
                        addElement(lei);
                        containerStack.add(lei);
                        paragraphStart = true;
                    }
                    if("ul".equals(name)) {
                        finishText();
                        pushStyle(xpp, false);
                    }
                    if("div".equals(name)) {
                        finishText();
                        pushStyle(xpp, false);
                        BlockElementImpl bei = new BlockElementImpl(getStyle());
                        addElement(bei);
                        containerStack.add(bei);
                    }
                    if("a".equals(name)) {
                        finishText();
                        pushStyle(xpp, true);
                        href = xpp.getAttributeValue(null, "href");
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    String name = xpp.getName();
                    if("span".equals(name)) {
                        popStyle();
                    }
                    if("img".equals(name)) {
                        popStyle();
                    }
                    if("p".equals(name)) {
                        paragraphEnd = true;
                        finishText();
                        popStyle();
                    }
                    if("button".equals(name)) {
                        popStyle();
                    }
                    if("li".equals(name)) {
                        paragraphEnd = true;
                        finishText();
                        popStyle();
                        containerStack.remove(containerStack.size()-1);
                    }
                    if("ul".equals(name)) {
                        popStyle();
                    }
                    if("div".equals(name)) {
                        finishText();
                        popStyle();
                        containerStack.remove(containerStack.size()-1);
                    }
                    if("a".equals(name)) {
                        finishText();
                        popStyle();
                        href = null;
                    }
                    break;
                }
                case XmlPullParser.TEXT: {
                    char[] buf = xpp.getTextCharacters(startLength);
                    if(startLength[1] > 0) {
                        int pos = sb.length();
                        sb.append(buf, startLength[0], startLength[1]);
                        if(!getStyle().pre) {
                            removeBreaks(pos);
                        }
                    }
                    break;
                }
                case XmlPullParser.ENTITY_REF: {
                    int pos = sb.length();
                    sb.append(xpp.getText());
                    if(!getStyle().pre) {
                        removeBreaks(pos);
                    }
                }
                }
            }
            finishText();
        } catch(Exception ex) {
             getLogger().log(Level.SEVERE, "Unable to parse XHTML document", ex);
        }
    }

    private StyleInfo getStyle() {
        return styleStack.get(styleStack.size()-1);
    }

    private void pushStyle(XmlPullParser xpp, boolean img) {
        StyleInfo newStyle = new StyleInfo(getStyle());
        if(img) {
            newStyle.halignment = HAlignment.INLINE;
            newStyle.changed = true;
        }

        if(newStyle.floatPosition != FloatPosition.NONE) {
            newStyle.floatPosition = FloatPosition.NONE;
            newStyle.changed = true;
        }
        
        String style = xpp.getAttributeValue(null, "style");
        if(style != null) {
           finishText();
           ParameterStringParser psp = new ParameterStringParser(style, ';', ':');
           psp.setTrim(true);
           while(psp.next()) {
               try {
                   if(parseCSSAttribute(newStyle, psp.getKey(), psp.getValue())) {
                       newStyle.changed = true;
                   }
               } catch (IllegalArgumentException ex) {
                    getLogger().log(Level.SEVERE, "Unable to parse CSS attribute: " + psp.getKey() + "=" + psp.getValue(), ex);
               }
           }
        }

        styleStack.add(newStyle);
    }

    private boolean parseCSSAttribute(StyleInfo style, String key, String value) {
        if("margin-top".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.marginTop = vu;
                return true;
            }
        }
        if("margin-left".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.marginLeft = vu;
                return true;
            }
        }
        if("margin-right".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.marginRight = vu;
                return true;
            }
        }
        if("margin-bottom".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.marginBottom = vu;
                return true;
            }
        }
        if("margin".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.marginTop = vu;
                style.marginLeft = vu;
                style.marginRight = vu;
                style.marginBottom = vu;
                return true;
            }
        }
        if("text-indent".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.textIndent = vu;
                return true;
            }
        }
        if("font".equals(key)) {
            style.fontName = value;
            return true;
        }
        if("text-align".equals(key)) {
            HAlignment alignment = HALIGN.get(value);
            if(alignment != null) {
                style.halignment = alignment;
                return true;
            }
        }
        if("vertical-align".equals(key)) {
            VAlignment alignment = VALIGN.get(value);
            if(alignment != null) {
                style.valignment = alignment;
                return true;
            }
        }
        if("white-space".equals(key)) {
            if("pre".equals(value)) {
                style.pre = true;
                return true;
            }
            if("normal".equals(value)) {
                style.pre = false;
                return true;
            }
        }
        if("list-style-image".equals(key)) {
            style.listStyleImage = parseURL(value);
            return true;
        }
        if("clear".equals(key)) {
            Clear clear = CLEAR.get(value);
            if(clear != null) {
                style.clear = clear;
                return true;
            }
        }
        if("float".equals(key)) {
            FloatPosition floatPos = FLOAT.get(value);
            if(floatPos != null) {
                style.floatPosition = floatPos;
                return true;
            }
        }
        if("width".equals(key)) {
            ValueUnit vu = parseValueUnit(value);
            if(vu != null) {
                style.width = vu;
                return true;
            }
        }
        if("background-image".equals(key)) {
            style.backgroundImage = parseURL(value);
            return true;
        }
        return false;
    }

    private ValueUnit parseValueUnit(String value) {
        Unit unit;
        int suffixLength = 2;
        if(value.endsWith("px")) {
            unit = Unit.PX;
        } else if(value.endsWith("em")) {
            unit = Unit.EM;
        } else if(value.endsWith("ex")) {
            unit = Unit.EX;
        } else if(value.endsWith("%")) {
            suffixLength = 1;
            unit = Unit.PERCENT;
        } else {
            return null;
        }
        try {
            String numberPart = value.substring(0, value.length()-suffixLength).trim();
            return new ValueUnit(Float.parseFloat(numberPart), unit);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(HTMLTextAreaModel.class.getName()).log(Level.WARNING,
                    "Invalid numeric value: " + value, ex);
            return null;
        }
    }

    private String parseURL(String value) {
        if(value.startsWith("url(") && value.endsWith(")")) {
            return value.substring(4, value.length()-1);
        }
        return value;
    }

    private static final HashMap<String, HAlignment> HALIGN = new HashMap<String, HAlignment>();
    private static final HashMap<String, VAlignment> VALIGN = new HashMap<String, VAlignment>();
    private static final HashMap<String, Clear> CLEAR = new HashMap<String, Clear>();
    private static final HashMap<String, FloatPosition> FLOAT = new HashMap<String, FloatPosition>();
    static {
        HALIGN.put("left", HAlignment.LEFT);
        HALIGN.put("right", HAlignment.RIGHT);
        HALIGN.put("center", HAlignment.CENTER);
        HALIGN.put("justify", HAlignment.BLOCK);

        VALIGN.put("top", VAlignment.TOP);
        VALIGN.put("middle", VAlignment.CENTER);
        VALIGN.put("bottom", VAlignment.BOTTOM);

        CLEAR.put("none", Clear.NONE);
        CLEAR.put("left", Clear.LEFT);
        CLEAR.put("right", Clear.RIGHT);
        CLEAR.put("both", Clear.BOTH);

        FLOAT.put("none", FloatPosition.NONE);
        FLOAT.put("left", FloatPosition.LEFT);
        FLOAT.put("right", FloatPosition.RIGHT);
    }

    private void popStyle() {
        if(styleStack.size() > 1) {
            if(getStyle().changed) {
                finishText();
            }
            styleStack.remove(styleStack.size()-1);
        }
    }

    private void finishText() {
        if(sb.length() > 0 || paragraphStart || paragraphEnd) {
            TextElementImpl e;
            if(href != null) {
                e = new LinkElementImpl(getStyle(), sb.toString(), paragraphStart, paragraphEnd, href);
            } else {
                e = new TextElementImpl(getStyle(), sb.toString(), paragraphStart, paragraphEnd);
            }
            addElement(e);
            sb.setLength(0);
            paragraphStart = false;
            paragraphEnd = false;
        }
    }

    private void removeBreaks(int pos) {
        // strip special characters of the new added block
        for(int idx=sb.length() ; idx-->pos ;) {
            char ch = sb.charAt(idx);
            if(Character.isWhitespace(ch) || Character.isISOControl(ch)) {
                sb.setCharAt(idx, ' ');
            }
        }
        // HTML treats consecutive spaces as one space - so remove them
        if(pos > 0) {
            pos--;
        }
        boolean wasSpace = false;
        for(int idx=sb.length() ; idx-->pos ;) {
            boolean isSpace = sb.charAt(idx) == ' ';
            if(isSpace && wasSpace) {
                sb.deleteCharAt(idx);
            }
            wasSpace = isSpace;
        }
    }

    Logger getLogger() {
        return Logger.getLogger(HTMLTextAreaModel.class.getName());
    }
    
    static class CompositeReader extends Reader {
        private final String[] strings;
        private int nr;
        private int pos;

        public CompositeReader(String ... strings) {
            this.strings = strings;
            while(strings[nr].length() == 0) {
                nr++;
            }
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if(nr == strings.length) {
                return -1;
            }
            String cur = strings[nr];
            int remain = cur.length() - pos;
            if(len > remain) {
                len = remain;
            }
            cur.getChars(pos, pos+len, cbuf, off);
            pos += len;
            if(pos == cur.length()) {
                pos = 0;
                do {
                    nr++;
                } while(nr < strings.length && strings[nr].length() == 0);
            }
            return len;
        }

        @Override
        public void close() {
        }
    }

    static class StyleInfo {
        ValueUnit marginTop;
        ValueUnit marginLeft;
        ValueUnit marginRight;
        ValueUnit marginBottom;
        ValueUnit textIndent;
        ValueUnit width;
        HAlignment halignment;
        VAlignment valignment;
        Clear clear;
        FloatPosition floatPosition;
        String fontName;
        String listStyleImage;
        String backgroundImage;
        boolean pre;
        boolean changed;

        StyleInfo() {
            marginTop = TextAreaModel.ZERO_PX;
            marginLeft = TextAreaModel.ZERO_PX;
            marginRight = TextAreaModel.ZERO_PX;
            marginBottom = TextAreaModel.ZERO_PX;
            textIndent = TextAreaModel.ZERO_PX;
            width = new ValueUnit(100f/3f, Unit.PERCENT);
            halignment = HAlignment.LEFT;
            valignment = VAlignment.BOTTOM;
            clear = Clear.NONE;
            floatPosition = FloatPosition.NONE;
            fontName = "default";
            listStyleImage = "ul-bullet";
            backgroundImage = "";
        }

        StyleInfo(StyleInfo src) {
            this.marginTop = src.marginTop;
            this.marginLeft = src.marginLeft;
            this.marginRight = src.marginRight;
            this.marginBottom = src.marginBottom;
            this.textIndent = src.textIndent;
            this.width = src.width;
            this.halignment = src.halignment;
            this.valignment = src.valignment;
            this.clear = src.clear;
            this.floatPosition = src.floatPosition;
            this.fontName = src.fontName;
            this.listStyleImage = src.listStyleImage;
            this.backgroundImage = src.backgroundImage;
            this.pre = src.pre;
        }
    }

    static class ElementImpl implements Element {
        final StyleInfo style;

        ElementImpl(StyleInfo style) {
            this.style = style;
        }

        public ValueUnit getMarginLeft() {
            return style.marginLeft;
        }

        public ValueUnit getMarginRight() {
            return style.marginRight;
        }

        public HAlignment getHorizontalAlignment() {
            return style.halignment;
        }

        public VAlignment getVerticalAlignment() {
            return style.valignment;
        }

        public Clear getClear() {
            return style.clear;
        }

        public FloatPosition getFloatPosition() {
            return style.floatPosition;
        }

        public String getFontName() {
            return style.fontName;
        }
    }

    static class TextElementImpl extends ElementImpl implements TextElement {
        private final String text;
        private final boolean isParagraphStart;
        private final boolean isParagraphEnd;

        TextElementImpl(StyleInfo style, String text, boolean isParagraphStart, boolean isParagraphEnd) {
            super(style);
            this.text = text;
            this.isParagraphStart = isParagraphStart;
            this.isParagraphEnd = isParagraphEnd;
        }

        public String getText() {
            return text;
        }

        public boolean isParagraphStart() {
            return isParagraphStart;
        }

        public boolean isParagraphEnd() {
            return isParagraphEnd;
        }

        public ValueUnit getTextIndent() {
            return style.textIndent;
        }

        public boolean isPreformatted() {
            return style.pre;
        }
    }

    static class LinkElementImpl extends TextElementImpl implements LinkElement {
        private final String href;

        public LinkElementImpl(StyleInfo style, String text, boolean isParagraphStart, boolean isParagraphEnd, String href) {
            super(style, text, isParagraphStart, isParagraphEnd);
            this.href = href;
        }

        public String getHREF() {
            return href;
        }
    }

    static class ImageElementImpl extends ElementImpl implements ImageElement {
        private final String imageSrc;
        private final String toolTip;

        public ImageElementImpl(StyleInfo style, String imageSrc, String toolTip) {
            super(style);
            this.imageSrc = imageSrc;
            this.toolTip = toolTip;
        }

        public Image getImage(ParameterMap style) {
            return style.getImage(imageSrc);
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    static class WidgetElementImpl extends ElementImpl implements WidgetElement {
        private final String widgetName;
        private final String widgetParam;

        WidgetElementImpl(StyleInfo style, String widgetName, String widgetParam) {
            super(style);
            this.widgetName = widgetName;
            this.widgetParam = widgetParam;
        }

        public String getWidgetName() {
            return widgetName;
        }

        public String getWidgetParam() {
            return widgetParam;
        }
    }

    static class Container extends ElementImpl {
        final ArrayList<Element> elements;

        public Container(StyleInfo style) {
            super(style);
            this.elements = new ArrayList<Element>();
        }

        public Iterator<Element> iterator() {
            return elements.iterator();
        }
    }

    static class ListElementImpl extends Container implements ListElement {
        public ListElementImpl(StyleInfo style) {
            super(style);
        }

        public Image getBulletImage(ParameterMap styleMap) {
            return styleMap.getImage(style.listStyleImage);
        }
    }

    static class BlockElementImpl extends Container implements BlockElement {
        public BlockElementImpl(StyleInfo style) {
            super(style);
        }

        public Image getBackgroundImage(ParameterMap styleMap) {
            if(style.backgroundImage.length() > 0) {
                return styleMap.getImage(style.backgroundImage);
            }
            return null;
        }

        public ValueUnit getWidth() {
            return style.width;
        }

        public ValueUnit getMarginTop() {
            return style.marginTop;
        }

        public ValueUnit getMarginBottom() {
            return style.marginBottom;
        }
    }
}
