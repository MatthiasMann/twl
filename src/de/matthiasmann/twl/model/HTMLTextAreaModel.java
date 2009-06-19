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
package de.matthiasmann.twl.model;

import de.matthiasmann.twl.ParameterMap;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.ParameterStringParser;
import java.io.IOException;
import java.io.Reader;
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
    
    static final Logger logger = Logger.getLogger(HTMLTextAreaModel.class.getName());

    private final ArrayList<Element> elements;
    private String html;
    private boolean needToParse;

    private final ArrayList<StyleInfo> styleStack;
    private final StringBuilder sb;
    private final int[] startLength;

    private boolean paragraphStart;
    private boolean paragraphEnd;

    public HTMLTextAreaModel() {
        this.elements = new ArrayList<Element>();
        this.styleStack = new ArrayList<StyleInfo>();
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

    public Iterator<Element> iterator() {
        if(needToParse) {
            parseHTML();
            needToParse = false;
        }
        return elements.iterator();
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
            sb.delete(0, sb.length());
            paragraphStart = false;
            paragraphEnd = false;

            int type;
            while((type=xpp.nextToken()) != XmlPullParser.END_DOCUMENT) {
                switch(type) {
                case XmlPullParser.START_TAG: {
                    String name = xpp.getName();
                    if("div".equals(name)) {
                        pushStyle(xpp, false);
                    }
                    if("img".equals(name)) {
                        finishText();
                        pushStyle(xpp, true);
                        String src = TextUtil.notNull(xpp.getAttributeValue(null, "src"));
                        String alt = xpp.getAttributeValue(null, "alt");
                        elements.add(new ImageElementImpl(getStyle(), src, alt));
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
                        elements.add(new WidgetElementImpl(getStyle(), btnName, btnParam));
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    String name = xpp.getName();
                    if("div".equals(name)) {
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
                    break;
                }
                case XmlPullParser.TEXT: {
                    char[] buf = xpp.getTextCharacters(startLength);
                    if(startLength[1] > 0) {
                        int pos = sb.length();
                        sb.append(buf, startLength[0], startLength[1]);
                        removeBreaks(pos);
                    }
                    break;
                }
                case XmlPullParser.ENTITY_REF: {
                    int pos = sb.length();
                    sb.append(xpp.getText());
                    removeBreaks(pos);
                }
                }
            }
            finishText();
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Unable to parse XHTML document", ex);
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
                   logger.log(Level.SEVERE, null, ex);
               }
           }
        }

        styleStack.add(newStyle);
    }

    private boolean parseCSSAttribute(StyleInfo style, String key, String value) {
        if("margin-left".equals(key)) {
            style.marginLeft = parsePXValue(value);
            return true;
        }
        if("margin-right".equals(key)) {
            style.marginRight = parsePXValue(value);
            return true;
        }
        if("text-indent".equals(key)) {
            style.textIndent = parsePXValue(value);
            return true;
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
        return false;
    }

    private int parsePXValue(String value) {
        if(value.endsWith("px")) {
            value = value.substring(0, value.length()-2).trim();
        }
        return Integer.parseInt(value, 10);
    }

    private static final HashMap<String, HAlignment> HALIGN = new HashMap<String, HAlignment>();
    private static final HashMap<String, VAlignment> VALIGN = new HashMap<String, VAlignment>();
    static {
        HALIGN.put("left", HAlignment.LEFT);
        HALIGN.put("right", HAlignment.RIGHT);
        HALIGN.put("center", HAlignment.CENTER);
        HALIGN.put("justify", HAlignment.BLOCK);

        VALIGN.put("top", VAlignment.TOP);
        VALIGN.put("middle", VAlignment.CENTER);
        VALIGN.put("bottom", VAlignment.BOTTOM);
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
            elements.add(new TextElementImpl(getStyle(), sb.toString(), paragraphStart, paragraphEnd));
            sb.delete(0, sb.length());
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
        int marginLeft;
        int marginRight;
        int textIndent;
        HAlignment halignment;
        VAlignment valignment;
        String fontName;
        boolean changed;

        StyleInfo() {
            halignment = HAlignment.LEFT;
            valignment = VAlignment.BOTTOM;
        }

        StyleInfo(StyleInfo src) {
            this.marginLeft = src.marginLeft;
            this.marginRight = src.marginRight;
            this.textIndent = src.textIndent;
            this.halignment = src.halignment;
            this.valignment = src.valignment;
            this.fontName = src.fontName;
        }
    }

    static class ElementImpl implements Element {
        final StyleInfo style;

        ElementImpl(StyleInfo style) {
            this.style = style;
        }

        public int getMarginLeft() {
            return style.marginLeft;
        }

        public int getMarginRight() {
            return style.marginRight;
        }

        public int getTextIndent() {
            return style.textIndent;
        }

        public HAlignment getHorizontalAlignment() {
            return style.halignment;
        }

        public VAlignment getVerticalAlignment() {
            return style.valignment;
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

        public String getFontName() {
            return style.fontName;
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


}
