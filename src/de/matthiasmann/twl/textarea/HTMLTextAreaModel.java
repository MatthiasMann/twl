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

import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.utils.TextUtil;
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
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * A simple XHTML parser.
 *
 * The following tags are supported:
 * <ul>
 *  <li>{@code a}<br/>Attributes: {@code href}</li>
 *  <li>{@code p}</li>
 *  <li>{@code br}</li>
 *  <li>{@code img}<br/>Attributes: {@code src}, {@code alt}<br/>Styles: {@code float}, {@code display}, {@code width}, {@code height}</li>
 *  <li>{@code span}</li>
 *  <li>{@code div}<br/>Styles: {@code background-image}, {@code float}, {@code width}<br/>Required styles for floating blocks: {@code width}<br/>Optional styles for floating blocks: {@code height}</li>
 *  <li>{@code ul}</li>
 *  <li>{@code li}<br/>Styles: {@code list-style-image}</li>
 *  <li>{@code button}<br/>Attributes: {@code name}, {@code value}<br/>Styles: {@code float}, {@code display}, {@code width}, {@code height}</li>
 * </ul>
 *
 * The following generic CSS attributes are supported:
 * <ul>
 *  <li>{@code font-family}<br/>References a font in the theme</li>
 *  <li>{@code text-align}</li>
 *  <li>{@code text-ident}</li>
 *  <li>{@code margin}</li>
 *  <li>{@code margin-top}</li>
 *  <li>{@code margin-left}</li>
 *  <li>{@code margin-right}</li>
 *  <li>{@code margin-bottom}</li>
 *  <li>{@code padding}</li>
 *  <li>{@code padding-top}</li>
 *  <li>{@code padding-left}</li>
 *  <li>{@code padding-right}</li>
 *  <li>{@code padding-bottom}</li>
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
    private final StringBuilder sb;
    private final int[] startLength;

    private ContainerElement curContainer;
    private boolean paragraphStart;
    private boolean paragraphEnd;
    private String href;

    private static final Style DEFAULTS_P_END = new Style().with(StyleAttribute.MARGIN_BOTTOM, new Value(1.0f, Value.Unit.EM));

    /**
     * Creates a new {@code HTMLTextAreaModel} without content.
     */
    public HTMLTextAreaModel() {
        this.elements = new ArrayList<Element>();
        this.styleStack = new ArrayList<Style>();
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
        if(curContainer != null) {
            curContainer.add(e);
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
            curContainer = null;
            sb.setLength(0);

            parseMain(xpp);
            finishText();
        } catch(Exception ex) {
             getLogger().log(Level.SEVERE, "Unable to parse XHTML document", ex);
        }
    }

    private void parseContainer(XmlPullParser xpp, ContainerElement container) throws XmlPullParserException, IOException {
        ContainerElement prevContainer = curContainer;
        curContainer = container;
        pushStyle(null);
        parseMain(xpp);
        popStyle();
        curContainer = prevContainer;
    }

    private void parseMain(XmlPullParser xpp) throws XmlPullParserException, IOException {
        paragraphStart = false;
        paragraphEnd = false;
        href = null;

        int level = 1;
        int type;
        while(level > 0 && (type=xpp.nextToken()) != XmlPullParser.END_DOCUMENT) {
            switch(type) {
            case XmlPullParser.START_TAG: {
                ++level;
                String name = xpp.getName();
                if("br".equals(name)) {
                    sb.append("\n");
                    break;
                }
                finishText();
                pushStyle(xpp);
                if("img".equals(name)) {
                    String src = TextUtil.notNull(xpp.getAttributeValue(null, "src"));
                    String alt = xpp.getAttributeValue(null, "alt");
                    addElement(new ImageElement(getStyle(), src, alt));
                }
                if("p".equals(name)) {
                    paragraphStart = true;
                }
                if("button".equals(name)) {
                    String btnName = TextUtil.notNull(xpp.getAttributeValue(null, "name"));
                    String btnParam = TextUtil.notNull(xpp.getAttributeValue(null, "value"));
                    addElement(new WidgetElement(getStyle(), btnName, btnParam));
                }
                if("li".equals(name)) {
                    ListElement lei = new ListElement(getStyle());
                    parseContainer(xpp, lei);
                    addElement(lei);
                    paragraphStart = true;
                    --level;
                }
                if("div".equals(name)) {
                    BlockElement bei = new BlockElement(getStyle());
                    addElement(bei);
                    parseContainer(xpp, bei);
                    --level;
                }
                if("a".equals(name)) {
                    href = xpp.getAttributeValue(null, "href");
                }
                if("table".equals(name)) {
                    parseTable(xpp);
                    --level;
                }
                break;
            }
            case XmlPullParser.END_TAG: {
                --level;
                String name = xpp.getName();
                if("br".equals(name)) {
                    break;
                }
                if("p".equals(name)) {
                    paragraphEnd = true;
                }
                if("li".equals(name)) {
                    paragraphEnd = true;
                }
                finishText();
                popStyle();
                if("a".equals(name)) {
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
    }

    private void parseTable(XmlPullParser xpp) throws XmlPullParserException, IOException {
        ArrayList<TableCellElement> cells = new ArrayList<TableCellElement>();
        int numColumns = 0;
        int cellSpacing = parseInt(xpp, "cellspacing", 0);
        int cellPadding = parseInt(xpp, "cellpadding", 0);
        Style tableStyle = getStyle();

        for(;;) {
            switch (xpp.nextTag()) {
                case XmlPullParser.START_TAG: {
                    pushStyle(xpp);
                    String name = xpp.getName();
                    if("td".equals(name) || "th".equals(name)) {
                        int colspan = parseInt(xpp, "colspan", 1);
                        int rowspan = parseInt(xpp, "rowspan", 1);
                        TableCellElement cell = new TableCellElement(getStyle(), colspan, rowspan);
                        parseContainer(xpp, cell);

                        cells.add(cell);
                        for(int col=1 ; col<colspan ; col++) {
                            cells.add(null);
                        }
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    popStyle();
                    String name = xpp.getName();
                    if("tr".equals(name)) {
                        if(numColumns == 0) {
                            numColumns = cells.size();
                        }
                    }
                    if("table".equals(name)) {
                        int numRows = (numColumns > 0) ? (cells.size() + numColumns - 1) / numColumns : 0;
                        TableElement tableElement = new TableElement(tableStyle, numColumns, numRows, cellSpacing, cellPadding);
                        if(numColumns > 0) {
                            for(int i=0 ; i<cells.size() ; i++) {
                                TableCellElement cell = cells.get(i);
                                tableElement.setSell(i / numColumns, i % numColumns, cell);
                            }
                        }
                        addElement(tableElement);
                        return;
                    }
                }
            }
        }
    }

    private static int parseInt(XmlPullParser xpp, String attribute, int defaultValue) {
        String value = xpp.getAttributeValue(null, attribute);
        if(value == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(value);
        }
    }
    
    private boolean isPre() {
        return getStyle().get(StyleAttribute.PREFORMATTED, null);
    }
    
    private Style getStyle() {
        return styleStack.get(styleStack.size()-1);
    }

    private void pushStyle(XmlPullParser xpp) {
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
        int stackSize = styleStack.size();
        if(stackSize > 1) {
            styleStack.remove(stackSize-1);
        }
    }

    private void finishText() {
        if(sb.length() > 0 || paragraphStart || paragraphEnd) {
            Style style = getStyle();
            if(paragraphEnd) {
                style = style.withAlternateDefaults(DEFAULTS_P_END);
            }
            TextElement e;
            if(href != null) {
                e = new LinkElement(style, sb.toString(), paragraphStart, paragraphEnd, href);
            } else {
                e = new TextElement(style, sb.toString(), paragraphStart, paragraphEnd);
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
}
