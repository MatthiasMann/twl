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

import de.matthiasmann.twl.model.TextAreaModel.Element;
import de.matthiasmann.twl.model.TextAreaModel.TextElement;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.model.TextAreaModel;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.FontCache;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.utils.CallbackSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A text area dor rendering complex text. Supports embedded images,
 * bullet point lists, hyper links, multiple fonts, block text and
 * embedded widgets.
 *
 * @author Matthias Mann
 */
public class TextArea extends Widget {

    public interface WidgetResolver {
        public Widget resolveWidget(String name, String param);
    }

    public interface Callback {
        /**
         * Called when a link has been clicked
         * @param href the href of the link
         */
        public void handleLinkClicked(String href);
    }

    public static final String STATE_HOVER = "hover";
    
    private final HashMap<String, Widget> widgets;
    private final HashMap<String, WidgetResolver> widgetResolvers;
    
    private final Runnable modelCB;
    private TextAreaModel model;
    private ParameterMap fonts;
    private ParameterMap images;
    private Font defaultFont;
    private Callback[] callbacks;
    private MouseCursor mouseCursorNormal;
    private MouseCursor mouseCursorLink;

    private final ArrayList<LElement> layout;
    private final ArrayList<LImage> bgImages;
    private boolean inLayoutCode;
    private int lastWidth;
    private int lastHeight;

    private int lastMouseX;
    private int lastMouseY;
    private boolean lastMouseInside;
    private LElement curLElementUnderMouse;

    public TextArea() {
        this.widgets = new HashMap<String, Widget>();
        this.widgetResolvers = new HashMap<String, WidgetResolver>();
        this.layout = new ArrayList<LElement>();
        this.bgImages = new ArrayList<LImage>();
        
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

    public void addCallback(Callback cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Callback.class);
    }

    public void removeCallback(Callback cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeTextArea(themeInfo);
    }

    protected void applyThemeTextArea(ThemeInfo themeInfo) {
        fonts = themeInfo.getParameterMap("fonts");
        images = themeInfo.getParameterMap("images");
        defaultFont = themeInfo.getFont("font");
        mouseCursorNormal = themeInfo.getMouseCursor("mouseCursor");
        mouseCursorLink = themeInfo.getMouseCursor("mouseCursor.link");
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
        return lastHeight;
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

            clearLayout();
            Box box = new Box(0, 0, targetWidth);

            try {
                if(model != null) {
                    layoutElements(box, model);
                    
                    // finish the last line
                    box.nextLine(false);
                    
                    // finish floaters
                    box.clearFloater(TextAreaModel.Clear.BOTH);
                }
                updateMouseHover();
            } finally {
                inLayoutCode = false;
            }

            if(lastHeight != box.curY) {
                lastHeight = box.curY;
                // call outside of inLayoutCode range
                invalidateLayout();
            }
        }
    }

    @Override
    protected void paintWidget(GUI gui) {
        final ArrayList<LElement> ll = layout;
        final ArrayList<LImage> bi = bgImages;
        final int innerX = getInnerX();
        final int innerY = getInnerY();
        final AnimationState as = getAnimationState();
        final LElement hoverElement = curLElementUnderMouse;

        for(int i=0,n=bi.size() ; i<n ; i++) {
            bi.get(i).draw(innerX, innerY, as);
        }

        for(int i=0,n=ll.size() ; i<n ; i++) {
            LElement le = ll.get(i);
            as.setAnimationState(STATE_HOVER, hoverElement == le);
            le.draw(innerX, innerY, as);
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

    @Override
    protected boolean handleEvent(Event evt) {
        if(super.handleEvent(evt)) {
            return true;
        }

        if(evt.isMouseEvent()) {
            lastMouseInside = isMouseInside(evt);
            lastMouseX = evt.getMouseX();
            lastMouseY = evt.getMouseY();
            updateMouseHover();

            if(evt.getType() == Event.Type.MOUSE_WHEEL) {
                return false;
            }

            if(evt.getType() == Event.Type.MOUSE_CLICKED) {
                if(curLElementUnderMouse != null && (curLElementUnderMouse.element instanceof TextAreaModel.LinkElement)) {
                    String href = ((TextAreaModel.LinkElement)curLElementUnderMouse.element).getHREF();
                    if(callbacks != null) {
                        for(Callback l : callbacks) {
                            l.handleLinkClicked(href);
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected Object getTooltipContentAt(int mouseX, int mouseY) {
        if(curLElementUnderMouse != null) {
            if(curLElementUnderMouse.element instanceof TextAreaModel.ImageElement) {
                return ((TextAreaModel.ImageElement)curLElementUnderMouse.element).getToolTip();
            }
        }
        return super.getTooltipContentAt(mouseX, mouseY);
    }

    private LElement findElement(int x, int y) {
        for(LElement le : layout) {
            if(le.isInside(x, y)) {
                return le;
            }
        }
        return null;
    }

    private void updateMouseHover() {
        LElement le = null;
        if(lastMouseInside) {
            le = findElement(lastMouseX - getInnerX(), lastMouseY - getInnerY());
        }
        if(curLElementUnderMouse != le) {
            curLElementUnderMouse = le;
            updateTooltip();
        }
        
        if(le != null && le.element instanceof TextAreaModel.LinkElement) {
            setMouseCursor(mouseCursorLink);
        } else {
            setMouseCursor(mouseCursorNormal);
        }
    }

    void forceRelayout() {
        lastWidth = -1;
        invalidateLayout();
    }
    
    private void clearLayout() {
        for(int i=0,n=layout.size() ; i<n ; i++) {
            layout.get(i).destroy();
        }
        layout.clear();
        bgImages.clear();
        super.removeAllChildren();
    }

    private void layoutElements(Box box, Iterable<TextAreaModel.Element> elements) {
        for(TextAreaModel.Element e : elements) {
            box.clearFloater(e.getClear());
            if(e instanceof TextAreaModel.TextElement) {
                layoutTextElement(box, (TextAreaModel.TextElement)e);
            } else if(e instanceof TextAreaModel.ImageElement) {
                layoutImageElement(box, (TextAreaModel.ImageElement)e);
            } else if(e instanceof TextAreaModel.WidgetElement) {
                layoutWidgetElement(box, (TextAreaModel.WidgetElement)e);
            } else if(e instanceof TextAreaModel.ListElement) {
                layoutListElement(box, (TextAreaModel.ListElement)e);
            } else if(e instanceof TextAreaModel.BlockElement) {
                layoutBlockElement(box, (TextAreaModel.BlockElement)e);
            } else {
                Logger.getLogger(TextArea.class.getName()).log(Level.SEVERE,
                        "Unknown Element subclass: " + e.getClass());
            }
        }
    }
    
    private void layoutImageElement(Box box, TextAreaModel.ImageElement ie) {
        if(images == null) {
            return;
        }
        final Image image = ie.getImage(images);
        if(image == null) {
            return;
        }

        LImage li = new LImage(ie, image);
        layout(box, ie, li, ie.getFloatPosition(), ie.getHorizontalAlignment());
    }

    private void layoutWidgetElement(Box box, TextAreaModel.WidgetElement we) {
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

        super.insertChild(widget, getNumChildren());
        widget.adjustSize();
        
        LWidget lw = new LWidget(we, widget);
        lw.width = widget.getWidth();
        lw.height = widget.getHeight();

        layout(box, we, lw, we.getFloatPosition(), we.getHorizontalAlignment());
    }

    private void layout(Box box, TextAreaModel.Element e, LElement le, TextAreaModel.FloatPosition floatPos, TextAreaModel.HAlignment align) {
        if(floatPos != TextAreaModel.FloatPosition.NONE ||
                align != TextAreaModel.HAlignment.INLINE) {
            box.nextLine(false);
        }

        boolean leftRight = false;

        switch(floatPos) {
            case LEFT:
                leftRight = true;
                le.x = box.lineStartX;
                box.objLeft.add(le);
                break;

            case RIGHT:
                leftRight = true;
                le.x = box.lineStartX + box.lineWidth - le.width;
                box.objRight.add(le);
                break;

            default:
                switch(align) {
                case LEFT:
                    le.x = box.lineStartX;
                    box.nextLine(false);
                    break;

                case RIGHT:
                    le.x = box.lineStartX + box.lineWidth - le.width;
                    box.nextLine(false);
                    break;

                case CENTER:
                    le.x = box.lineStartX + (box.lineWidth - le.width) / 2;
                    box.nextLine(false);
                    break;

                case BLOCK:
                    le.x = box.lineStartX;
                    le.width = box.lineWidth;
                    box.nextLine(false);
                    break;

                case INLINE:
                    if(box.getRemaining() < le.width && !box.isAtStartOfLine()) {
                        box.nextLine(false);
                    }
                    le.x = box.getXAndAdvance(le.width);
                    break;
                }
        }

        layout.add(le);
        if(leftRight) {
            assert box.lineStartIdx == layout.size() - 1;
            box.lineStartIdx++;
            le.y = box.curY;
            le.adjustWidget();
            box.computeMargin();
        }
    }

    static int convertToPX(TextAreaModel.ValueUnit valueUnit, Font font, int full) {
        float value = valueUnit.value;
        switch(valueUnit.unit) {
            case EM:
                value *= font.getEM();
                break;
            case EX:
                value *= font.getEX();
                break;
            case PERCENT:
                value *= full * 0.01f;
                break;
        }
        return Math.round(value);
    }

    private Font selectFont(TextAreaModel.Element e) {
        String fontName = e.getFontName();
        if(fontName != null && fonts != null) {
            Font font = fonts.getFont(fontName);
            if(font != null) {
                return font;
            }
        }
        return defaultFont;
    }
    
    private void layoutTextElement(Box box, TextAreaModel.TextElement te) {
        if(fonts == null) {
            return;
        }
        final String text = te.getText();
        Font font = selectFont(te);
        if(font == null) {
            return;
        }

        box.setupTextParams(te, font);

        int idx = 0;
        while(idx < text.length()) {
            int end = TextUtil.indexOf(text, '\n', idx);
            if(te.isPreformatted()) {
                layoutTextPre(box, te, font, text, idx, end);
            } else {
                layoutText(box, te, font, text, idx, end);
            }
            
            if(end < text.length() && text.charAt(end) == '\n') {
                end++;
                box.nextLine(true);
            }
            idx = end;
        }

        box.resetTextParams(te.isParagraphEnd());
    }

    private void layoutText(Box box, TextAreaModel.TextElement te, Font font,
            String text, int textStart, int textEnd) {
        int idx = textStart;
        // trim start
        while(textStart < textEnd && isSkip(text.charAt(textStart))) {
            textStart++;
        }
        // trim end
        boolean endsWithSpace = false;
        while(textEnd > textStart && isSkip(text.charAt(textEnd-1))) {
            endsWithSpace = true;
            textEnd--;
        }

        // check if we skipped white spaces and the previous element in this
        // row was not a text cell
        if(textStart > idx && box.prevOnLineNotText()) {
            box.curX += font.getSpaceWidth();
        }

        idx = textStart;
        while(idx < textEnd) {
            assert !Character.isSpaceChar(text.charAt(idx));

            int count;
            if(box.textAlignment == TextAreaModel.HAlignment.BLOCK) {
                count = 1;
            } else {
                count = font.computeVisibleGlpyhs(text, idx, textEnd, box.getRemaining());
            }
            
            int end;
            if(box.isAtStartOfLine()) {
                end = idx + Math.max(1, count);
            } else {
                end = idx + count;
            }

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
                if(box.textAlignment != TextAreaModel.HAlignment.BLOCK && box.nextLine(false)) {
                    continue;
                }
                // or we already are at the start of a line
                // just put the word there even if it doesn't fit
                while(end < textEnd && !isBreak(text.charAt(end))) {
                    end++;
                }
            }

            // some characters need to stay at the end of a word
            if(end < textEnd && isPunctuation(text.charAt(end))) {
                end++;
            }

            if(idx < end) {
                LText lt = new LText(te, font, text, idx, end);
                if(box.textAlignment == TextAreaModel.HAlignment.BLOCK && box.getRemaining() < lt.width) {
                    box.nextLine(false);
                }

                int width = lt.width;
                if(end < textEnd && isSkip(text.charAt(end))) {
                    width += font.getSpaceWidth();
                }

                lt.x = box.getXAndAdvance(width);
                layout.add(lt);
            }

            // find the start of the next word
            idx = end;
            while(idx < textEnd && isSkip(text.charAt(idx))) {
                idx++;
            }
        }

        if(!box.isAtStartOfLine() && endsWithSpace) {
            box.curX += font.getSpaceWidth();
        }
    }

    private void layoutTextPre(Box box, TextAreaModel.TextElement te, Font font,
            String text, int textStart, int textEnd) {
        int idx = textStart;
        while(idx < textEnd) {
            box.nextLine(false);

            while(idx < textEnd) {
                if(text.charAt(idx) == '\t') {
                    idx++;
                    int tabX = box.computeNextTabStop(font);
                    if(tabX < box.lineWidth) {
                        box.curX = tabX;
                    } else if(!box.isAtStartOfLine()) {
                        break;
                    }
                }

                int tabIdx = text.indexOf('\t', idx);
                int end = textEnd;
                if(tabIdx >= 0 && tabIdx < textEnd) {
                    end = tabIdx;
                }

                if(end > idx) {
                    int count = font.computeVisibleGlpyhs(text, idx, end, box.getRemaining());
                    if(count == 0 && !box.isAtStartOfLine()) {
                        break;
                    }

                    end = idx + Math.max(1, count);

                    LText lt = new LText(te, font, text, idx, end);
                    lt.x = box.getXAndAdvance(lt.width);
                    layout.add(lt);
                }

                idx = end;
            }
        }
        box.nextLine(false);
    }

    private void layoutListElement(Box box, TextAreaModel.ListElement le) {
        Image image = (images != null) ? le.getBulletImage(images) : null;
        if(image != null) {
            LImage li = new LImage(le, image);
            layout(box, le, li, TextAreaModel.FloatPosition.LEFT, TextAreaModel.HAlignment.LEFT);
            
            int imageHeight = li.height;
            li.height = Short.MAX_VALUE;

            layoutElements(box, le);
            box.nextLine(false);

            li.height = imageHeight;

            box.objLeft.remove(li);
            box.computeMargin();
        } else {
            layoutElements(box, le);
            box.nextLine(false);
        }
    }

    private void layoutBlockElement(Box box, TextAreaModel.BlockElement be) {
        box.nextLine(false);

        Font font = selectFont(be);
        if(font == null && (
                be.getWidth().unit.isFontBased() ||
                be.getMarginTop().unit.isFontBased() ||
                be.getMarginBottom().unit.isFontBased())) {
            return;
        }
        
        LImage bgImage = null;
        Image image = (images != null) ? be.getBackgroundImage(images) : null;
        if(image != null) {
            bgImage = new LImage(be, image);
            bgImages.add(bgImage);
        }

        int bgX = box.lineStartX;
        int bgY = box.curY;
        int bgWidth;
        int bgHeight;

        int marginTop = Math.max(0, convertToPX(be.getMarginTop(), font, 0));
        int marginBottom = Math.max(0, convertToPX(be.getMarginBottom(), font, 0));

        if(be.getFloatPosition() == TextAreaModel.FloatPosition.NONE) {
            bgWidth = box.lineWidth;
            box.curY += marginTop;
            layoutElements(box, be);
            box.nextLine(false);
            box.curY += marginBottom;
            bgHeight = box.curY - bgY;
        } else {
            int remaining = box.getRemaining();
            bgWidth = Math.max(0, Math.min(convertToPX(be.getWidth(), font, remaining), remaining));

            if(be.getFloatPosition() == TextAreaModel.FloatPosition.RIGHT) {
                bgX += box.lineWidth - bgWidth;
            }

            Box blockBox = new Box(bgY + marginTop, bgX, bgWidth);
            layoutElements(blockBox, be);
            blockBox.nextLine(false);
            blockBox.clearFloater(TextAreaModel.Clear.BOTH);
            blockBox.curY += marginBottom;
            bgHeight = blockBox.curY - box.curY;
            
            // sync main box with layout
            box.lineStartIdx = layout.size();

            LElement dummy = new LElement(be);
            dummy.x = bgX;
            dummy.y = bgY;
            dummy.width = bgWidth;
            dummy.height = bgHeight;

            if(be.getFloatPosition() == TextAreaModel.FloatPosition.RIGHT) {
                box.objRight.add(dummy);
            } else {
                box.objLeft.add(dummy);
            }
            box.computeMargin();
        }
        
        if(bgImage != null) {
            bgImage.x = bgX;
            bgImage.y = bgY;
            bgImage.width = bgWidth;
            bgImage.height = bgHeight;
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

    class Box {
        final ArrayList<LElement> objLeft = new ArrayList<LElement>();
        final ArrayList<LElement> objRight = new ArrayList<LElement>();
        final int boxLeft;
        final int boxWidth;
        int curY;
        int curX;
        int lineStartIdx;
        int marginLeft;
        int marginRight;
        int lineStartX;
        int lineWidth;
        int fontLineHeight;
        boolean inParagraph;
        boolean wasAutoBreak;
        TextAreaModel.HAlignment textAlignment;

        public Box(int boxTop, int boxLeft, int boxWidth) {
            this.boxLeft = boxLeft;
            this.boxWidth = boxWidth;
            this.curX = boxLeft;
            this.curY = boxTop;
            this.lineStartIdx = layout.size();
            this.lineStartX = boxLeft;
            this.lineWidth = boxWidth;
            this.textAlignment = TextAreaModel.HAlignment.LEFT;
        }

        void computeMargin() {
            int left = boxLeft;
            int right = boxLeft + boxWidth;

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

        int getRemaining() {
            return Math.max(0, lineWidth - curX + lineStartX);
        }

        int getXAndAdvance(int amount) {
            int x = curX;
            curX = x + amount;
            return x;
        }

        boolean isAtStartOfLine() {
            return lineStartIdx == layout.size();
        }

        boolean prevOnLineNotText() {
            return lineStartIdx < layout.size() && !(layout.get(layout.size()-1) instanceof LText);
        }

        void checkFloaters() {
            removeObjFromList(objLeft);
            removeObjFromList(objRight);
            computeMargin();
            // curX is set by computeMargin()
        }

        void clearFloater(TextAreaModel.Clear clear) {
            if(clear != TextAreaModel.Clear.NONE) {
                int targetY = -1;
                if(clear == TextAreaModel.Clear.LEFT || clear == TextAreaModel.Clear.BOTH) {
                    for(int i=0,n=objLeft.size() ; i<n ; ++i) {
                        LElement le = objLeft.get(i);
                        if(le.height != Short.MAX_VALUE) {  // special case for list elements
                            targetY = Math.max(targetY, le.y + le.height);
                        }
                    }
                }
                if(clear == TextAreaModel.Clear.RIGHT || clear == TextAreaModel.Clear.BOTH) {
                    for(int i=0,n=objRight.size() ; i<n ; ++i) {
                        LElement le = objRight.get(i);
                        targetY = Math.max(targetY, le.y + le.height);
                    }
                }
                if(targetY >= 0) {
                    nextLine(false);
                    if(targetY > curY) {
                        curY = targetY;
                        checkFloaters();
                    }
                }
            }
        }

        boolean nextLine(boolean force) {
            if(isAtStartOfLine()) {
                if(!wasAutoBreak && force) {
                    curY += fontLineHeight;
                    checkFloaters();
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
                switch(le.element.getVerticalAlignment()) {
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

                le.adjustWidget();
            }
            lineStartIdx = layout.size();
            curY += lineHeight;
            checkFloaters();
            // curX is set by computeMargin() inside checkFloaters()
            return true;
        }

        int computeNextTabStop(Font font) {
            int x = curX - lineStartX + font.getSpaceWidth();
            int tabSize = 8 * font.getEM();
            return curX + tabSize - (x % tabSize);
        }

        private void removeObjFromList(ArrayList<LElement> list) {
            for(int i=list.size() ; i-->0 ;) {
                LElement e = list.get(i);
                if(e.y + e.height <= curY) {
                    list.remove(i);
                }
            }
        }

        void endParagraph() {
        }

        void resetTextParams(boolean endParagraph) {
            if(endParagraph) {
                nextLine(false);
                curY += fontLineHeight;
                checkFloaters();
                inParagraph = false;
            }
            
            if(!inParagraph) {
                marginLeft = 0;
                marginRight = 0;
                textAlignment = TextAreaModel.HAlignment.LEFT;
                computeMargin();
            }
        }

        void setupTextParams(TextElement te, Font font) {
            fontLineHeight = font.getLineHeight();

            if(te.isParagraphStart()) {
                nextLine(false);
                inParagraph = true;
            }

            if(te.isParagraphStart() || (!inParagraph && isAtStartOfLine())) {
                int remaining = getRemaining();
                marginLeft = Math.max(0, convertToPX(te.getMarginLeft(), font, remaining));
                marginRight = Math.max(0, convertToPX(te.getMarginRight(), font, remaining));
                textAlignment = te.getHorizontalAlignment();
                computeMargin();
                curX = Math.max(0, lineStartX + convertToPX(te.getTextIndent(), font, remaining));
            }
        }
    }

    static class LElement {
        final TextAreaModel.Element element;
        int x;
        int y;
        int width;
        int height;

        public LElement(TextAreaModel.Element element) {
            this.element = element;
        }

        void adjustWidget() {}
        void draw(int offX, int offY, AnimationState as) {}
        void destroy() {}

        boolean isInside(int x, int y) {
            return (x >= this.x) && (x < this.x + this.width) &&
                    (y >= this.y) && (y < this.y + this.height);
        }
    }

    static class LText extends LElement {
        final Font font;
        final String text;
        final int start;
        final int end;
        FontCache cache;

        public LText(TextAreaModel.Element element, Font font, String text, int start, int end) {
            super(element);
            this.font = font;
            this.text = text;
            this.start = start;
            this.end = end;
            this.cache = font.cacheText(null, text, start, end);
            this.height = font.getLineHeight();

            if(cache != null) {
                this.width = cache.getWidth();
            } else {
                this.width = font.computeTextWidth(text, start, end);
            }
        }

        @Override
        void draw(int offX, int offY, AnimationState as) {
            if(cache != null) {
                cache.draw(as, x+offX, y+offY);
            } else {
                font.drawText(as, x+offX, y+offY, text, start, end);
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
        final Widget widget;

        public LWidget(TextAreaModel.Element element, Widget widget) {
            super(element);
            this.widget = widget;
        }

        @Override
        void adjustWidget() {
            widget.setPosition(x + widget.getParent().getInnerX(), y + widget.getParent().getInnerY());
            widget.setSize(width, height);
        }
    }

    static class LImage extends LElement {
        final Image img;

        public LImage(Element element, Image img) {
            super(element);
            this.img = img;
            this.width = img.getWidth();
            this.height = img.getHeight();
        }
        
        @Override
        void draw(int offX, int offY, AnimationState as) {
            img.draw(as, x+offX, y+offY, width, height);
        }
    }
}
