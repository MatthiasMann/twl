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

import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class ScrollPane extends Widget {

    public static final String STATE_DOWNARROW_ARMED = "downArrowArmed";
    public static final String STATE_RIGHTARROW_ARMED = "rightArrowArmed";
    public static final String STATE_HORIZONTAL_SCROLLBAR_VISIBLE = "horizontalScrollbarVisible";
    public static final String STATE_VERTICAL_SCROLLBAR_VISIBLE = "verticalScrollbarVisible";

    public enum Fixed {
        NONE,
        HORIZONTAL,
        VERTICAL
    }

    public interface Scrollable {
        public void setScrollPosition(int scrollPosX, int scrollPosY);
    }
    
    private final Scrollbar scrollbarH;
    private final Scrollbar scrollbarV;
    private final ContentArea contentArea;
    private DraggableButton dragButton;
    private Widget content;
    private Fixed fixed = Fixed.NONE;
    private int hscrollbarOffsetY;
    private int vscrollbarOffsetX;

    public ScrollPane() {
        this(null);
    }

    public ScrollPane(Widget content) {
        this.scrollbarH = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
        this.scrollbarV = new Scrollbar(Scrollbar.Orientation.VERTICAL);
        this.contentArea = new ContentArea();

        Runnable cb = new Runnable() {
            public void run() {
                scrollContent();
            }
        };

        scrollbarH.addCallback(cb);
        scrollbarH.setVisible(false);
        scrollbarV.addCallback(cb);
        scrollbarV.setVisible(false);
        
        super.insertChild(contentArea, 0);
        super.insertChild(scrollbarH, 1);
        super.insertChild(scrollbarV, 2);
        setContent(content);
        setCanAcceptKeyboardFocus(true);
    }

    public Fixed getFixed() {
        return fixed;
    }

    public void setFixed(Fixed fixed) {
        if(this.fixed != fixed) {
            this.fixed = fixed;
            invalidateLayout();
        }
    }

    public Widget getContent() {
        return content;
    }

    public void setContent(Widget content) {
        if(this.content != null) {
            contentArea.removeAllChildren();
            this.content = null;
        }
        if(content != null) {
            contentArea.add(content);
            this.content = content;
        }
    }

    public void updateScrollbarSizes() {
        invalidateLayout();
        validateLayout();
    }

    public int getScrollPositionX() {
        return scrollbarH.getValue();
    }

    public int getMaxScrollPosX() {
        return scrollbarH.getMaxValue();
    }

    public void setScrollPositionX(int pos) {
        scrollbarH.setValue(pos);
    }

    public int getScrollPositionY() {
        return scrollbarV.getValue();
    }

    public int getMaxScrollPosY() {
        return scrollbarV.getMaxValue();
    }

    public void setScrollPositionY(int pos) {
        scrollbarV.setValue(pos);
    }

    public int getContentAreaWidth() {
        return contentArea.getWidth();
    }

    public int getContentAreaHeight() {
        return contentArea.getHeight();
    }

    public static ScrollPane getContainingScrollPane(Widget widget) {
        Widget ca = widget.getParent();
        if(ca != null && ca.getClass() == ContentArea.class) {
            return (ScrollPane)ca.getParent();
        }
        return null;
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        int border = getBorderHorizontal();
        //minWidth = Math.max(minWidth, scrollbarH.getMinWidth() + border);
        if(fixed == Fixed.HORIZONTAL && content != null) {
            minWidth = Math.max(minWidth, content.getMinWidth() +
                    border + scrollbarV.getMinWidth());
        }
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = super.getMinHeight();
        int border = getBorderVertical();
        //minHeight = Math.max(minHeight, scrollbarV.getMinHeight() + border);
        if(fixed == Fixed.VERTICAL && content != null) {
            minHeight = Math.max(minHeight, content.getMinHeight() +
                    border + scrollbarH.getMinHeight());
        }
        return minHeight;
    }

    @Override
    public int getPreferredInnerWidth() {
        if(content != null) {
            switch(fixed) {
            case HORIZONTAL:
                int prefWidth = computeSize(
                        content.getMinWidth(),
                        content.getPreferredWidth(),
                        content.getMaxWidth());
                if(scrollbarV.isVisible()) {
                    prefWidth += scrollbarV.getPreferredWidth();
                }
                return prefWidth;
            case VERTICAL:
                return content.getPreferredWidth();
            }
        }
        return 0;
    }

    @Override
    public int getPreferredInnerHeight() {
        if(content != null) {
            switch(fixed) {
            case HORIZONTAL:
                return content.getPreferredHeight();
            case VERTICAL:
                int prefHeight = computeSize(
                        content.getMinHeight(),
                        content.getPreferredHeight(),
                        content.getMaxHeight());
                if(scrollbarH.isVisible()) {
                    prefHeight += scrollbarH.getPreferredHeight();
                }
                return prefHeight;
            }
        }
        return 0;
    }

    @Override
    public void insertChild(Widget child, int index) {
        throw new UnsupportedOperationException("use setContent");
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException("use setContent");
    }

    @Override
    public Widget removeChild(int index) {
        throw new UnsupportedOperationException("use setContent");
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeScrollPane(themeInfo);
    }

    protected void applyThemeScrollPane(ThemeInfo themeInfo) {
        hscrollbarOffsetY = themeInfo.getParameter("hscrollbarOffsetY", 0);
        vscrollbarOffsetX = themeInfo.getParameter("vscrollbarOffsetX", 0);
        boolean hasDragButton = themeInfo.getParameter("hasDragButton", false);
        if(hasDragButton && dragButton == null) {
            dragButton = new DraggableButton();
            dragButton.setTheme("dragButton");
            dragButton.setListener(new DraggableButton.DragListener() {
                public void dragStarted() {
                    scrollbarH.externalDragStart();
                    scrollbarV.externalDragStart();
                }
                public void dragged(int deltaX, int deltaY) {
                    scrollbarH.externalDragged(deltaX, deltaY);
                    scrollbarV.externalDragged(deltaX, deltaY);
                }
                public void dragStopped() {
                    scrollbarH.externalDragStopped();
                    scrollbarV.externalDragStopped();
                }
            });
            super.insertChild(dragButton, 3);
        } else if(!hasDragButton && dragButton != null) {
            assert super.getChild(3) == dragButton;
            super.removeChild(3);
            dragButton = null;
        }
        invalidateLayout();
    }

    @Override
    protected void layout() {
        if(content != null) {
            int availWidth = getInnerWidth();
            int availHeight = getInnerHeight();
            int requiredWidth;
            int requiredHeight;
            boolean repeat;
            boolean visibleH = false;
            boolean visibleV = false;

            switch(fixed) {
            case HORIZONTAL:
                requiredWidth = availWidth;
                requiredHeight = content.getPreferredHeight();
                break;
            case VERTICAL:
                requiredWidth = content.getPreferredWidth();
                requiredHeight = availHeight;
                break;
            default:
                requiredWidth = content.getPreferredWidth();
                requiredHeight = content.getPreferredHeight();
                break;
            }

            //System.out.println("required="+requiredWidth+","+requiredHeight+" avail="+availWidth+","+availHeight);

            // don't add scrollbars if we have zero size
            if(availWidth > 0 && availHeight > 0) {
                do{
                    repeat = false;

                    if(fixed != Fixed.HORIZONTAL) {
                        scrollbarH.setMinMaxValue(0, Math.max(0, requiredWidth - availWidth));
                        if(scrollbarH.getMaxValue() > 0) {
                            repeat |= !visibleH;
                            visibleH = true;
                            availHeight = Math.max(0, getInnerHeight() -
                                    scrollbarH.getPreferredHeight() + hscrollbarOffsetY);
                        }
                    } else {
                        scrollbarH.setMinMaxValue(0, 0);
                        requiredWidth = availWidth;
                    }

                    if(fixed != Fixed.VERTICAL) {
                        scrollbarV.setMinMaxValue(0, Math.max(0, requiredHeight - availHeight));
                        if(scrollbarV.getMaxValue() > 0) {
                            repeat |= !visibleV;
                            visibleV = true;
                            availWidth = Math.max(0, getInnerWidth() -
                                    scrollbarV.getPreferredWidth() + vscrollbarOffsetX);
                        }
                    } else {
                        scrollbarV.setMinMaxValue(0, 0);
                        requiredHeight = availHeight;
                    }
                }while(repeat);
            }

            if(visibleH != scrollbarH.isVisible() || visibleV != scrollbarV.isVisible()) {
                invalidateParentLayout();
            }
            scrollbarH.setVisible(visibleH);
            scrollbarH.setSize(availWidth, getInnerHeight() - availHeight);
            scrollbarH.setPosition(getInnerX(), getInnerY() + availHeight);
            scrollbarH.setPageSize(Math.max(1, availWidth));
            scrollbarH.setStepSize(Math.max(1, availWidth / 10));
            scrollbarV.setVisible(visibleV);
            scrollbarV.setSize(getInnerWidth() - availWidth, availHeight);
            scrollbarV.setPosition(getInnerX() + availWidth, getInnerY());
            scrollbarV.setPageSize(Math.max(1, availHeight));
            scrollbarV.setStepSize(Math.max(1, availHeight / 10));

            if(dragButton != null) {
                dragButton.setVisible(visibleH && visibleV);
                dragButton.setSize(getInnerWidth() - availWidth, getInnerHeight() - availHeight);
                dragButton.setPosition(getInnerX() + availWidth, getInnerY() + availHeight);
            }
            
            contentArea.setPosition(getInnerX(), getInnerY());
            contentArea.setSize(availWidth, availHeight);
            if(content instanceof Scrollable) {
                content.setPosition(contentArea.getX(), contentArea.getY());
                content.setSize(availWidth, availHeight);
            } else {
                content.setSize(requiredWidth, requiredHeight);
            }

            AnimationState animationState = getAnimationState();
            animationState.setAnimationState(STATE_HORIZONTAL_SCROLLBAR_VISIBLE, visibleH);
            animationState.setAnimationState(STATE_VERTICAL_SCROLLBAR_VISIBLE, visibleV);

            scrollContent();
        } else {
            scrollbarH.setVisible(false);
            scrollbarV.setVisible(false);
        }
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(super.handleEvent(evt)) {
            return true;
        }
        switch(evt.getType()) {
        case KEY_PRESSED:
        case KEY_RELEASED:
            if(evt.getKeyCode() == Keyboard.KEY_LEFT ||
                    evt.getKeyCode() == Keyboard.KEY_RIGHT) {
                return scrollbarH.handleEvent(evt);
            }
            if(evt.getKeyCode() == Keyboard.KEY_UP ||
                    evt.getKeyCode() == Keyboard.KEY_DOWN) {
                return scrollbarV.handleEvent(evt);
            }
            break;
        case MOUSE_WHEEL:
            if(scrollbarV.isVisible()) {
                return scrollbarV.handleEvent(evt);
            }
            break;
        }
        return evt.isMouseEvent() && contentArea.isMouseInside(evt);
    }

    @Override
    protected void paint(GUI gui) {
        if(dragButton != null) {
            AnimationState as = dragButton.getAnimationState();
            as.setAnimationState(STATE_DOWNARROW_ARMED, scrollbarV.isDownRightButtonArmed());
            as.setAnimationState(STATE_RIGHTARROW_ARMED, scrollbarH.isDownRightButtonArmed());
        }
        super.paint(gui);
    }

    void scrollContent() {
       if(content instanceof Scrollable) {
            Scrollable scrollable = (Scrollable)content;
            scrollable.setScrollPosition(scrollbarH.getValue(), scrollbarV.getValue());
       } else {
            content.setPosition(
                    contentArea.getX()-scrollbarH.getValue(),
                    contentArea.getY()-scrollbarV.getValue());
       }
    }

    class ContentArea extends Widget {
        ContentArea() {
            setClip(true);
            setTheme("");
        }

        @Override
        protected void childChangedSize(Widget child) {
            ScrollPane.this.childChangedSize(child);
        }

        @Override
        public void invalidateLayout() {
            ScrollPane.this.updateScrollbarSizes();
        }

        @Override
        protected void sizeChanged() {
        }
    }
}
