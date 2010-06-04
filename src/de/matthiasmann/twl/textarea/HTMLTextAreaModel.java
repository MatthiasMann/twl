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
package de.matthiasmann.twl.textarea;

import de.matthiasmann.twl.ParameterMap;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.renderer.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * A simple XHTML parser.
 *
 * The following tags are supported:
 * <ul>
 *  <li>{@code a}<br/>Attributes: {@code href}</li>
 *  <li>{@code p}</li>
 *  <li>{@code br}</li>
 *  <li>{@code img}<br/>Attributes: {@code src}, {@code alt}<br/>Styles: {@code float}, {@code display}</li>
 *  <li>{@code span}</li>
 *  <li>{@code div}<br/>Styles: {@code background-image}, {@code float}, {@code width} (required for floating divs)</li>
 *  <li>{@code ul}</li>
 *  <li>{@code li}<br/>Styles: {@code list-style-image}</li>
 *  <li>{@code button}<br/>Attributes: {@code name}, {@code value}, {@code float}, {@code display}</li>
 * </ul>
 *
 * The following generic CSS attributes are supported:
 * <ul>
 *  <li>{@code font}<br/>References a font in the theme - does not behave like HTML</li>
 *  <li>{@code text-align}</li>
 *  <li>{@code text-ident}</li>
 *  <li>{@code margin}</li>
 *  <li>{@code margin-top}</li>
 *  <li>{@code margin-left}</li>
 *  <li>{@code margin-right}</li>
 *  <li>{@code margin-bottom}</li>
 *  <li>{@code clear}</li>
 *  <li>{@code vertical-align}</li>
 *  <li>{@code white-space}<br/>Only {@code normal} and {@code pre}</li>
 * </ul>
 *
 * Numeric values must use on of the following units: {@code em}, {@code ex}, {@code px}, {@code %}
 *
 * @author Matthias Mann
 */
public class HTMLTextAreaModel extends HasCallback implements TextAreaModel {
    
    private final ArrayList<Element> elements;
    private String html;
    private boolean needToParse;

    private final ArrayList<Style> styleStack;
    private final ArrayList<Container> containerStack;
    private final StringBuilder sb;
    private final int[] startLength;

    private boolean paragraphStart;
    private boolean paragraphEnd;
    private String href;

    /**
     * Creates a new {@code HTMLTextAreaModel} without content.
     */
    public HTMLTextAreaModel() {
        this.elements = new ArrayList<Element>();
        this.styleStack = new ArrayList<Style>();
        this.containerStack = new ArrayList<Container>();
        this.sb = new StringBuilder();
        this.startLength = new int[2];
    }

    /**
     * Creates a new {@code HTMLTextAreaModel} and parses the given html.
     * @param html the HTML to parse
     * @see #setHtml(java.lang.String)
     */
    public HTMLTextAreaModel(String html) {
        this();
        setHtml(html);
    }

    /**
     * Creates a new {@code HTMLTextAreaModel} and parses the content of the
     * given {@code Reader}.
     *
     * @see #readHTMLFromStream(java.io.Reader)
     * @param r the reader to parse html from
     * @throws IOException if an error occured while reading
     */
    public HTMLTextAreaModel(Reader r) throws IOException {
        this();
        readHTMLFromStream(r);
    }

    /**
     * Returns the current HTML.
     * @return the current HTML. Can be null.
     */
    public String getHtml() {
        return html;
    }

    /**
     * Sets the a html to parse.
     * 
     * @param html the html.
     */
    public void setHtml(String html) {
        this.html = html;
        this.elements.clear();
        needToParse = true;
        doCallback();
    }

    /**
     * Reads HTML from the given {@code Reader}.
     *
     * @param r the reader to parse html from
     * @throws IOException if an error occured while reading
     * @see #setHtml(java.lang.String)
     */
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

    /**
     * Reads HTML from the given {@code URL}.
     *
     * @param url the URL to parse.
     * @throws IOException if an error occured while reading
     * @see #readHTMLFromStream(java.io.Reader) 
     */
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
            xpp.defineEntityReplacementText("nbsp", "\u00A0");
            xpp.require(XmlPullParser.START_DOCUMENT, null, null);
            xpp.nextTag();

            styleStack.clear();
            styleStack.add(new Style(null, null));
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
                        pushStyle(xpp);
                    }
                    if("img".equals(name)) {
                        finishText();
                        pushStyle(xpp);
                        String src = TextUtil.notNull(xpp.getAttributeValue(null, "src"));
                        String alt = xpp.getAttributeValue(null, "alt");
                        addElement(new ImageElementImpl(getStyle(), src, alt));
                    }
                    if("br".equals(name)) {
                        sb.append("\n");
                    }
                    if("p".equals(name)) {
                        finishText();
                        pushStyle(xpp);
                        paragraphStart = true;
                    }
                    if("button".equals(name)) {
                        finishText();
                        pushStyle(xpp);
                        String btnName = TextUtil.notNull(xpp.getAttributeValue(null, "name"));
                        String btnParam = TextUtil.notNull(xpp.getAttributeValue(null, "value"));
                        addElement(new WidgetElementImpl(getStyle(), btnName, btnParam));
                    }
                    if("li".equals(name)) {
                        finishText();
                        pushStyle(xpp);
                        ListElementImpl lei = new ListElementImpl(getStyle());
                        addElement(lei);
                        containerStack.add(lei);
                        paragraphStart = true;
                    }
                    if("ul".equals(name)) {
                        finishText();
                        pushStyle(xpp);
                    }
                    if("div".equals(name)) {
                        finishText();
                        pushStyle(xpp);
                        BlockElementImpl bei = new BlockElementImpl(getStyle());
                        addElement(bei);
                        containerStack.add(bei);
                        pushStyle(null);
                    }
                    if("a".equals(name)) {
                        finishText();
                        pushStyle(xpp);
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
                        if(!isPre()) {
                            removeBreaks(pos);
                        }
                    }
                    break;
                }
                case XmlPullParser.ENTITY_REF:
                    sb.append(xpp.getText());
                    break;
                }
            }
            finishText();
        } catch(Exception ex) {
             getLogger().log(Level.SEVERE, "Unable to parse XHTML document", ex);
        }
    }

    private boolean isPre() {
        return getStyle().get(StyleAttribute.PREFORMATTED, null);
    }
    
    private Style getStyle() {
        return styleStack.get(styleStack.size()-1);
    }

    private void pushStyle(XmlPullParser xpp) {
        finishText();
        
        Style parent = getStyle();

        String classRef = (xpp != null) ? xpp.getAttributeValue(null, "class") : null;
        String style = (xpp != null) ? xpp.getAttributeValue(null, "style") : null;
        Style newStyle;

        if(style != null) {
            newStyle = new CSSStyle(parent, classRef, style);
        } else {
            newStyle = new Style(parent, classRef);
        }

        styleStack.add(newStyle);
    }

    private void popStyle() {
        if(styleStack.size() > 1) {
            if(getStyle() != styleStack.get(styleStack.size()-2)) {
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

    static class ElementImpl implements Element {
        final Style style;

        ElementImpl(Style style) {
            this.style = style;
        }

        public Style getStyle() {
            return style;
        }
    }

    static class TextElementImpl extends ElementImpl implements TextElement {
        private final String text;
        private final boolean isParagraphStart;
        private final boolean isParagraphEnd;

        TextElementImpl(Style style, String text, boolean isParagraphStart, boolean isParagraphEnd) {
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
    }

    static class LinkElementImpl extends TextElementImpl implements LinkElement {
        private final String href;

        public LinkElementImpl(Style style, String text, boolean isParagraphStart, boolean isParagraphEnd, String href) {
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

        public ImageElementImpl(Style style, String imageSrc, String toolTip) {
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

        WidgetElementImpl(Style style, String widgetName, String widgetParam) {
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

        public Container(Style style) {
            super(style);
            this.elements = new ArrayList<Element>();
        }

        public Iterator<Element> iterator() {
            return elements.iterator();
        }
    }

    static class ListElementImpl extends Container implements ListElement {
        public ListElementImpl(Style style) {
            super(style);
        }
    }

    static class BlockElementImpl extends Container implements BlockElement {
        public BlockElementImpl(Style style) {
            super(style);
        }
    }
}
