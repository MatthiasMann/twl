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
package de.matthiasmann.twl;

import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.model.TextAreaModel;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.FontCache;
import de.matthiasmann.twl.renderer.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class TextArea extends Widget {

    public interface WidgetResolver {
        public Widget resolveWidget(String name, String param);
    }

    private final HashMap<String, Widget> widgets;
    private final HashMap<String, WidgetResolver> widgetResolvers;
    
    private final Runnable modelCB;
    private TextAreaModel model;
    private ParameterMap fonts;
    private ParameterMap images;
    private Font defaultFont;

    private final ArrayList<LElement> layout;
    private final ArrayList<LElement> objLeft;
    private final ArrayList<LElement> objRight;
    private int curY;
    private int curX;
    private int lineStartIdx;
    private int marginLeft;
    private int marginRight;
    private int lineStartX;
    private int lineWidth;
    private int fontLineHeight;
    private boolean inParagraph;
    private boolean wasAutoBreak;
    private boolean inLayoutCode;
    private TextAreaModel.HAlignment textAlignment;
    private int lastWidth;

    public TextArea() {
        this.widgets = new HashMap<String, Widget>();
        this.widgetResolvers = new HashMap<String, WidgetResolver>();
        this.layout = new ArrayList<LElement>();
        this.objLeft = new ArrayList<LElement>();
        this.objRight = new ArrayList<LElement>();
        
        this.modelCB = new Runnable() {
            public void run() {
                forceRelayout();
            }
        };
    }

    public TextArea(TextAreaModel model) {
        this();
        setModel(model);
    }

    public TextAreaModel getModel() {
        return model;
    }

    public void setModel(TextAreaModel model) {
        if(this.model != null) {
            this.model.removeCallback(modelCB);
        }
        this.model = model;
        if(model != null) {
            model.addCallback(modelCB);
        }
        forceRelayout();
    }

    public void registerWidget(String name, Widget widget) {
        if(name == null) {
            throw new NullPointerException("name");
        }
        if(widget.getParent() != null) {
            throw new IllegalArgumentException("Widget must not have a parent");
        }
        if(widgets.containsKey(name) || widgetResolvers.containsKey(name)) {
            throw new IllegalArgumentException("widget name already in registered");
        }
        if(widgets.containsValue(widget)) {
            throw new IllegalArgumentException("widget already registered");
        }
        widgets.put(name, widget);
    }

    public void registerWidgetResolver(String name, WidgetResolver resolver) {
        if(name == null) {
            throw new NullPointerException("name");
        }
        if(resolver == null) {
            throw new NullPointerException("resolver");
        }
        if(widgets.containsKey(name) || widgetResolvers.containsKey(name)) {
            throw new IllegalArgumentException("widget name already in registered");
        }
        widgetResolvers.put(name, resolver);
    }

    public void unregisterWidgetResolver(String name) {
        if(name == null) {
            throw new NullPointerException("name");
        }
        widgetResolvers.remove(name);
    }

    public void unregisterWidget(String name) {
        if(name == null) {
            throw new NullPointerException("name");
        }
        Widget w = widgets.get(name);
        if(w != null) {
            int idx = getChildIndex(w);
            if(idx > 0) {
                super.removeChild(idx);
                forceRelayout();
            }
        }
    }

    public void unregisterAllWidgets() {
        widgets.clear();
        super.removeAllChildren();
        forceRelayout();
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        fonts = themeInfo.getParameterMap("fonts");
        images = themeInfo.getParameterMap("images");
        defaultFont = themeInfo.getFont("font");
        forceRelayout();
    }

    @Override
    public void insertChild(Widget child, int index) {
        throw new UnsupportedOperationException("use registerWidget");
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException("use registerWidget");
    }

    @Override
    public Widget removeChild(int index) {
        throw new UnsupportedOperationException("use registerWidget");
    }

    @Override
    public int getPreferredInnerWidth() {
        return getInnerWidth();
    }

    @Override
    public int getPreferredInnerHeight() {
        validateLayout();
        return curY;
    }
    
    @Override
    public int getPreferredWidth() {
        int maxWidth = getMaxWidth();
        if(maxWidth > 0) {
            return maxWidth;
        }
        return computeSize(getMinWidth(), super.getPreferredWidth(), maxWidth);
    }

    @Override
    public void setMaxSize(int width, int height) {
        if(width != getMaxWidth()) {
            invalidateLayout();
        }
        super.setMaxSize(width, height);
    }

    @Override
    public void setMinSize(int width, int height) {
        if(width != getMinWidth()) {
            invalidateLayout();
        }
        super.setMinSize(width, height);
    }
    
    @Override
    protected void layout() {
        int targetWidth = computeSize(getMinWidth(), getWidth(), getMaxWidth());
        targetWidth -= getBorderHorizontal();

        //System.out.println(this+" minWidth="+getMinWidth()+" width="+getWidth()+" maxWidth="+getMaxWidth());
        
        // only recompute the layout when it has changed
        if(lastWidth != targetWidth) {
            this.lastWidth = targetWidth;
            this.inLayoutCode = true;
            int lastCurY = curY;

            try {
                clearLayout();
                if(model != null) {
                    for(TextAreaModel.Element e : model) {
                        if(e instanceof TextAreaModel.TextElement) {
                            layout((TextAreaModel.TextElement)e);
                        } else if(e instanceof  TextAreaModel.ImageElement) {
                            layout((TextAreaModel.ImageElement)e);
                        } else if(e instanceof  TextAreaModel.WidgetElement) {
                            layout((TextAreaModel.WidgetElement)e);
                        }
                    }
                    // finish the last line
                    nextLine(false);

                    for(int i=0,n=layout.size() ; i<n ; i++) {
                        layout.get(i).adjustWidget();
                    }
                }
            } finally {
                inLayoutCode = false;
                objLeft.clear();
                objRight.clear();
            }

            if(lastCurY != curY) {
                // call outside of inLayoutCode range
                invalidateParentLayout();
            }
        }
    }

    @Override
    protected void paintWidget(GUI gui) {
        final int innerX = getInnerX();
        final int innerY = getInnerY();

        for(int i=0,n=layout.size() ; i<n ; i++) {
            LElement le = layout.get(i);
            le.draw(innerX, innerY);
        }
    }

    @Override
    protected void childChangedSize(Widget child) {
        // ignore while in layout code
        if(!inLayoutCode) {
            forceRelayout();
        }
    }

    @Override
    protected void sizeChanged() {
        if(!inLayoutCode) {
            invalidateLayout();
        }
    }

    @Override
    protected void childAdded(Widget child) {
        // always ignore
    }

    @Override
    protected void childRemoved(Widget exChild) {
        // always ignore
    }

    @Override
    public void destroy() {
        super.destroy();
        clearLayout();
        forceRelayout();
    }

    void forceRelayout() {
        lastWidth = -1;
        invalidateLayout();
    }
    
    private void clearLayout() {
        for(int i=0,n=layout.size() ; i<n ; i++) {
            layout.get(i).destroy();
        }
        objLeft.clear();
        objRight.clear();
        super.removeAllChildren();

        curY = 0;
        curX = 0;
        marginLeft = 0;
        marginRight = 0;
        lineStartIdx = 0;
        fontLineHeight = 0;
        inParagraph = false;
        wasAutoBreak = false;
        textAlignment = TextAreaModel.HAlignment.LEFT;
        layout.clear();
        computeMargin();
    }

    private void computeMargin() {
        int right = lastWidth;
        int left = 0;

        for(int i=0,n=objLeft.size() ; i<n ; i++) {
            LElement e = objLeft.get(i);
            left = Math.max(left, e.x + e.width);
        }

        for(int i=0,n=objRight.size() ; i<n ; i++) {
            LElement e = objRight.get(i);
            right = Math.min(right, e.x);
        }

        left += marginLeft;
        right -= marginRight;

        lineStartX = left;
        lineWidth = Math.max(0, right - left);

        if(isAtStartOfLine()) {
            curX = lineStartX;
        }
    }

    private int getRemaining() {
        return Math.max(0, lineWidth - curX);
    }

    private boolean isAtStartOfLine() {
        return lineStartIdx == layout.size();
    }

    private boolean nextLine(boolean force) {
        if(isAtStartOfLine()) {
            if(!wasAutoBreak && force) {
                curY += fontLineHeight;
                wasAutoBreak = false;
                return true;
            }
            return false;
        }
        wasAutoBreak = !force;
        int lineHeight = 0;
        for(int idx=lineStartIdx ; idx<layout.size() ; idx++) {
            LElement le = layout.get(idx);
            lineHeight = Math.max(lineHeight, le.height);
        }

        LElement lastElement = layout.get(layout.size() - 1);
        int remaining = (lineStartX + lineWidth) - (lastElement.x + lastElement.width);
        
        switch(textAlignment) {
        case RIGHT: {
            for(int idx=lineStartIdx ; idx<layout.size() ; idx++) {
                LElement le = layout.get(idx);
                le.x += remaining;
            }
            break;
        }
        case CENTER: {
            int offset = remaining / 2;
            for(int idx=lineStartIdx ; idx<layout.size() ; idx++) {
                LElement le = layout.get(idx);
                le.x += offset;
            }
            break;
        }
        case BLOCK:
            if(remaining < lineWidth / 4) {
                int num = layout.size() - lineStartIdx;
                for(int i=1 ; i<num ; i++) {
                    LElement le = layout.get(lineStartIdx + i);
                    int offset = remaining * i / (num-1);
                    le.x += offset;
                }
            }
            break;
        }
        
        for(int idx=lineStartIdx ; idx<layout.size() ; idx++) {
            LElement le = layout.get(idx);
            switch(le.valign) {
            case BOTTOM:
                le.y = curY + lineHeight - le.height;
                break;
            case TOP:
                le.y = curY;
                break;
            case CENTER:
                le.y = curY + (lineHeight - le.height)/2;
                break;
            case FILL:
                le.y = curY;
                le.height = lineHeight;
                break;
            }
        }
        lineStartIdx = layout.size();
        curY += lineHeight;

        removeObjFromList(objLeft);
        removeObjFromList(objRight);
        computeMargin();
        // curX is set by computeMargin()
        
        return true;
    }

    private void removeObjFromList(ArrayList<LElement> list) {
        for(int i=list.size() ; i-->0 ;) {
            LElement e = list.get(i);
            if(e.y + e.height < curY) {
                list.remove(i);
            }
        }
    }

    private void layout(TextAreaModel.ImageElement ie) {
        if(images == null) {
            return;
        }
        final Image image = ie.getImage(images);
        if(image == null) {
            return;
        }

        LImage li = new LImage(image, ie.getToolTip());
        layout(ie, li);
    }

    private void layout(TextAreaModel.WidgetElement we) {
        Widget widget = widgets.get(we.getWidgetName());
        if(widget == null) {
            WidgetResolver resolver = widgetResolvers.get(we.getWidgetName());
            if(resolver != null) {
                widget = resolver.resolveWidget(we.getWidgetName(), we.getWidgetParam());
            }
            if(widget == null) {
                return;
            }
        }

        if(widget.getParent() != null) {
            Logger.getLogger(TextArea.class.getName()).log(Level.SEVERE, "Widget already added: " + widget);
            return;
        }

        LWidget lw = new LWidget(widget);
        layout(we, lw);
    }

    private void layout(TextAreaModel.Element e, LWidget lw) {
        final TextAreaModel.HAlignment align = e.getHorizontalAlignment();
        if(align != TextAreaModel.HAlignment.INLINE) {
            nextLine(false);
        }

        super.insertChild(lw.widget, getNumChildren());
        lw.valign = e.getVerticalAlignment();
        lw.widget.adjustSize();
        lw.width = lw.widget.getWidth();
        lw.height = lw.widget.getHeight();

        boolean leftRight = false;
        switch(align) {
        case LEFT:
            leftRight = true;
            lw.x = lineStartX;
            objLeft.add(lw);
            break;

        case RIGHT:
            leftRight = true;
            lw.x = lineStartX + lineWidth - lw.width;
            objRight.add(lw);
            break;

        case CENTER:
            lw.x = lineStartX + (lineWidth - lw.width) / 2;
            nextLine(false);
            break;

        case BLOCK:
            lw.x = lineStartX;
            lw.width = lineWidth;
            nextLine(false);
            break;

        case INLINE:
            if(getRemaining() < lw.width && !isAtStartOfLine()) {
                nextLine(false);
            }
            lw.x = curX;
            curX += lw.width;
            break;
        }

        layout.add(lw);
        if(leftRight) {
            assert lineStartIdx == layout.size() - 1;
            lineStartIdx++;
            lw.y = curY;
            computeMargin();
        }
    }

    private void layout(TextAreaModel.TextElement te) {
        if(fonts == null) {
            return;
        }
        final String text = te.getText();
        Font font;
        if(te.getFontName() == null || (font = fonts.getFont(te.getFontName())) == null) {
            if(defaultFont == null) {
                return;
            }
            font = defaultFont;
        }
        fontLineHeight = font.getLineHeight();

        if(te.isParagraphStart()) {
            nextLine(false);
            inParagraph = true;
        }

        if(te.isParagraphStart() || (!inParagraph && isAtStartOfLine())) {
            marginLeft = Math.max(0, te.getMarginLeft());
            marginRight = Math.max(0, te.getMarginRight());
            textAlignment = te.getHorizontalAlignment();
            computeMargin();
            curX = Math.max(0, lineStartX + te.getTextIndent());
        }

        int idx = 0;
        while(idx < text.length()) {
            int end = TextUtil.indexOf(text, '\n', idx);
            layoutText(te, font, text, idx, end);
            
            if(end < text.length() && text.charAt(end) == '\n') {
                end++;
                nextLine(true);
            }
            idx = end;
        }

        if(te.isParagraphEnd()) {
            nextLine(false);
            curY += font.getLineHeight();
            inParagraph = false;
        }
        if(!inParagraph) {
            marginLeft = 0;
            marginRight = 0;
            textAlignment = TextAreaModel.HAlignment.LEFT;
            computeMargin();
        }
    }

    private void layoutText(TextAreaModel.TextElement te, Font font,
            String text, int textStart, int textEnd) {
        // trim start
        while(textStart < textEnd && isSkip(text.charAt(textStart))) {
            textStart++;
        }
        // trim end
        while(textEnd > textStart && isSkip(text.charAt(textEnd-1))) {
            textEnd--;
        }

        int idx = textStart;
        while(idx < textEnd) {
            assert !Character.isSpaceChar(text.charAt(idx));

            int count;
            if(textAlignment == TextAreaModel.HAlignment.BLOCK) {
                count = 1;
            } else {
                count = font.computeVisibleGlpyhs(text, idx, textEnd, getRemaining());
            }
            int end = idx + Math.max(1, count);

            // if we are not at the end of this text element
            // and the next character is not a space
            if(end < textEnd && !isBreak(text.charAt(end))) {
                // then we walk backwards until we find spaces
                // this prevents the line ending in the middle of a word
                while(end > idx && !isBreak(text.charAt(end-1))) {
                    end--;
                }
            }

            // now walks backwards until we hit the end of the previous word
            while(end > idx && isSkip(text.charAt(end-1))) {
                end--;
            }

            // if we found no word that fits
            if(end == idx) {
                // we may need a new line
                if(textAlignment != TextAreaModel.HAlignment.BLOCK && nextLine(false)) {
                    continue;
                }
                // or we already are at the start of a line
                // just put the word there even if it doesn't fit
                while(end < textEnd && !isBreak(text.charAt(end))) {
                    end++;
                }
                // some characters need to stay at the end of a word
                if(end < textEnd && isPunctuation(text.charAt(end))) {
                    end++;
                }
            }

            if(idx < end) {
                LText lt = new LText(font, text, idx, end, te.getVerticalAlignment());
                if(textAlignment == TextAreaModel.HAlignment.BLOCK && getRemaining() < lt.width) {
                    nextLine(false);
                }
                
                lt.x = curX;
                curX += lt.width + font.getSpaceWidth();
                layout.add(lt);
            }

            // find the start of the next word
            idx = end;
            while(idx < textEnd && isSkip(text.charAt(idx))) {
                idx++;
            }
        }
    }

    private boolean isSkip(char ch) {
        return Character.isWhitespace(ch);
    }

    private boolean isPunctuation(char ch) {
        return ":;,.-!?".indexOf(ch) >= 0;
    }

    private boolean isBreak(char ch) {
        return Character.isWhitespace(ch) || isPunctuation(ch);
    }

    static abstract class LElement {
        int x;
        int y;
        int width;
        int height;
        TextAreaModel.VAlignment valign;

        void draw(int offX, int offY) {}
        void destroy() {}
        void adjustWidget() {}
    }

    class LText extends LElement {
        Font font;
        String text;
        int start;
        int end;
        FontCache cache;

        public LText(Font font, String text, int start, int end,
                TextAreaModel.VAlignment valign) {
            this.font = font;
            this.text = text;
            this.start = start;
            this.end = end;
            this.cache = font.cacheText(null, text, start, end);
            this.height = font.getLineHeight();
            this.valign = valign;

            if(cache != null) {
                this.width = cache.getWidth();
            } else {
                this.width = font.computeTextWidth(text, start, end);
            }
        }

        @Override
        void draw(int offX, int offY) {
            if(cache != null) {
                cache.draw(getAnimationState(), x+offX, y+offY);
            } else {
                font.drawText(getAnimationState(), x+offX, y+offY, text);
            }
        }

        @Override
        void destroy() {
            if(cache != null) {
                cache.destroy();
                cache = null;
            }
        }
    }

    static class LWidget extends LElement {
        Widget widget;

        LWidget() {
        }

        LWidget(Widget widget) {
            this.widget = widget;
            this.widget.adjustSize();
        }

        @Override
        void adjustWidget() {
            widget.setPosition(x + widget.getParent().getInnerX(), y + widget.getParent().getInnerY());
            widget.setSize(width, height);
        }
    }

    static class LImage extends LWidget {
        LImage(Image img, String toolTip) {
            this.widget = new Label() {
                @Override
                protected void applyThemeBackground(ThemeInfo themeInfo) {
                    // don't load the background image
                }
            };
            widget.setTheme("image");
            widget.setBackground(img);
            widget.setTooltipContent(toolTip);
        }
    }
}
