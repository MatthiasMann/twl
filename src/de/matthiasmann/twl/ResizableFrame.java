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
package de.matthiasmann.twl;

import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.renderer.Renderer;

/**
 * A resizable frame
 *
 * @author Matthias Mann
 */
public class ResizableFrame extends Widget {

    public static final String STATE_FADE = "fade";

    public enum ResizableAxis {
        NONE(false, false),
        HORIZONTAL(true, false),
        VERTICAL(false, true),
        BOTH(true, true);

        final boolean allowX;
        final boolean allowY;
        private ResizableAxis(boolean allowX, boolean allowY) {
            this.allowX = allowX;
            this.allowY = allowY;
        }
    };
    
    private enum DragMode {
        NONE("mouseCursor"),
        EDGE_LEFT("mouseCursor.left"),
        EDGE_TOP("mouseCursor.top"),
        EDGE_RIGHT("mouseCursor.right"),
        EDGE_BOTTOM("mouseCursor.bottom"),
        CORNER_TL("mouseCursor.top-left"),
        CORNER_TR("mouseCursor.top-right"),
        CORNER_BR("mouseCursor.bottom-right"),
        CORNER_BL("mouseCursor.bottom-left"),
        POSITION("mouseCursor.all");

        final String cursorName;
        DragMode(String cursorName) {
            this.cursorName = cursorName;
        }
    }

    private String title;
    
    private final MouseCursor[] cursors;
    private ResizableAxis resizableAxis = ResizableAxis.BOTH;
    private DragMode dragMode = DragMode.NONE;
    private int dragStartX;
    private int dragStartY;
    private int dragInitialLeft;
    private int dragInitialTop;
    private int dragInitialRight;
    private int dragInitialBottom;

    private Image backgroundActive;
    private Image backgroundInactive;

    private Color fadeColorInactive;
    private int fadeDurationActivate;
    private int fadeDurationDeactivate;
    private int fadeDurationShow;
    private int fadeDurationHide;

    private TextWidget titleWidget;
    private int titleAreaTop;
    private int titleAreaLeft;
    private int titleAreaRight;
    private int titleAreaBottom;

    private boolean hasCloseButton;
    private Button closeButton;
    private int closeButtonX;
    private int closeButtonY;

    private boolean hasResizeHandle;
    private Widget resizeHandle;
    private int resizeHandleX;
    private int resizeHandleY;
    private DragMode resizeHandleDragMode;

    private int fadeDuration;
    private boolean fadeActive;
    private boolean hasTint;
    private float[] currentTint;

    public ResizableFrame() {
        cursors = new MouseCursor[DragMode.values().length];
        setCanAcceptKeyboardFocus(true);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        if(titleWidget != null) {
            titleWidget.setText(title);
        }
    }

    public ResizableAxis getResizableAxis() {
        return resizableAxis;
    }

    public void setResizableAxis(ResizableAxis resizableAxis) {
        if(resizableAxis == null) {
            throw new NullPointerException("resizableAxis");
        }
        this.resizableAxis = resizableAxis;
    }

    public boolean hasTitleBar() {
        return titleWidget != null && titleWidget.getParent() == this;
    }

    public void addCloseCallback(Runnable cb) {
        if(closeButton == null) {
            closeButton = new Button();
            closeButton.setTheme("closeButton");
            closeButton.setCanAcceptKeyboardFocus(false);
            add(closeButton);
            layoutCloseButton();
        }
        closeButton.setVisible(hasCloseButton);
        closeButton.addCallback(cb);
    }

    public void removeCloseCallback(Runnable cb) {
        if(closeButton != null) {
            closeButton.removeCallback(cb);
            closeButton.setVisible(closeButton.hasCallbacks());
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if(visible) {
            if(hasTint || !super.isVisible()) {
                fadeTo(hasKeyboardFocus() ? Color.WHITE : fadeColorInactive, fadeDurationShow);
            }
        } else if(super.isVisible()) {
            fadeToHide(fadeDurationHide);
        }
    }

    protected void applyThemeResizableFrame(ThemeInfo themeInfo) {
        for(DragMode m : DragMode.values()) {
            if(m.cursorName != null) {
                cursors[m.ordinal()] = themeInfo.getMouseCursor(m.cursorName);
            } else {
                cursors[m.ordinal()] = null;
            }
        }
        titleAreaTop = themeInfo.getParameter("titleAreaTop", 0);
        titleAreaLeft = themeInfo.getParameter("titleAreaLeft", 0);
        titleAreaRight = themeInfo.getParameter("titleAreaRight", 0);
        titleAreaBottom = themeInfo.getParameter("titleAreaBottom", 0);
        closeButtonX = themeInfo.getParameter("closeButtonX", 0);
        closeButtonY = themeInfo.getParameter("closeButtonY", 0);
        hasCloseButton = themeInfo.getParameter("hasCloseButton", false);
        hasResizeHandle = themeInfo.getParameter("hasResizeHandle", false);
        resizeHandleX = themeInfo.getParameter("resizeHandleX", 0);
        resizeHandleY = themeInfo.getParameter("resizeHandleY", 0);
        fadeColorInactive = themeInfo.getParameter("fadeColorInactive", Color.WHITE);
        fadeDurationActivate = themeInfo.getParameter("fadeDurationActivate", 0);
        fadeDurationDeactivate = themeInfo.getParameter("fadeDurationDeactivate", 0);
        fadeDurationShow = themeInfo.getParameter("fadeDurationShow", 0);
        fadeDurationHide = themeInfo.getParameter("fadeDurationHide", 0);
        invalidateLayout();
        layoutTitle();
        layoutCloseButton();
        layoutResizeHandle();

        if(super.isVisible() && !hasKeyboardFocus() &&
                (currentTint != null || !Color.WHITE.equals(fadeColorInactive))) {
            fadeTo(fadeColorInactive, 0);
        }
    }

    @Override
    protected void applyThemeBackground(ThemeInfo themeInfo) {
        backgroundActive = themeInfo.getImage("background-active");
        backgroundInactive = themeInfo.getImage("background-inactive");
        setBackground(hasKeyboardFocus() ? backgroundActive : backgroundInactive);
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeResizableFrame(themeInfo);
    }

    @Override
    protected void paint(GUI gui) {
        if(hasTint) {
            paintWithTint(gui);
        } else {
            super.paint(gui);
        }
    }

    protected void paintWithTint(GUI gui) {
        if(fadeActive) {
            doTintFade();
        }
        float[] tint = currentTint;
        Renderer renderer = gui.getRenderer();
        renderer.pushGlobalTintColor(tint[0], tint[1], tint[2], tint[3]);
        try {
            super.paint(gui);
        } finally {
            renderer.popGlobalTintColor();
        }
    }

    private static final float ZERO_EPSILON = 1e-3f;
    private static final float ONE_EPSILON = 1f - ZERO_EPSILON;
    
    private void doTintFade() {
        int time = getAnimationState().getAnimationTime(STATE_FADE);
        float t = Math.min(time, fadeDuration) / (float)fadeDuration;
        float[] tint = currentTint;
        for(int i=0 ; i<4 ; i++) {
            tint[i] = tint[i+4] + t * tint[i+8];
        }
        if(time >= fadeDuration) {
            fadeActive = false;
            if(currentTint[3] <= ZERO_EPSILON) {
                super.setVisible(false);
            }
            // disable tinted rendering if we have full WHITE as tint
            hasTint =
                    (currentTint[0] < ONE_EPSILON) ||
                    (currentTint[1] < ONE_EPSILON) ||
                    (currentTint[2] < ONE_EPSILON) ||
                    (currentTint[3] < ONE_EPSILON);
        }
    }

    protected void fadeTo(Color color, int duration) {
        //System.out.println("Start fade to " + color + " over " + duration + " ms");
        allocateTint();
        // get destination color
        color.getFloats(currentTint, 8);
        // finish fadeTo computation
        fadeTo(duration);
    }

    protected void fadeToHide(int duration) {
        allocateTint();
        // get current tint as destination value but with 0 alpha
        System.arraycopy(currentTint, 0, currentTint, 8, 3);
        currentTint[11] = 0f;
        // finish fadeTo computation
        fadeTo(duration);
    }

    private void allocateTint() {
        if(currentTint == null) {
            currentTint = new float[12];
            // we start with WHITE tint color
            Color.WHITE.getFloats(currentTint, 0);
        }
        // get current tint as start value
        System.arraycopy(currentTint, 0, currentTint, 4, 4);
    }

    private void fadeTo(int duration) {
        // currentTint[8..11] contain the destination color
        // if destination alpha is > 0 then make frame visible
        if(currentTint[11] >= ZERO_EPSILON) {
            super.setVisible(true);
        }
        // convert destination into deltas
        for(int i=0 ; i<4 ; i++) {
            currentTint[i+8] -= currentTint[i+4];
        }
        // start fade, flags will be set correctly at end of fade
        hasTint = true;
        fadeActive = true;
        fadeDuration = Math.max(1, duration);
        getAnimationState().resetAnimationTime(STATE_FADE);
    }

    protected boolean isFrameElement(Widget widget) {
        return widget == titleWidget || widget == closeButton || widget == resizeHandle;
    }

    @Override
    protected void layout() {
        int minWidth = getMinWidth();
        int minHeight = getMinHeight();
        if(getWidth() < minWidth || getHeight() < minHeight) {
            int width = Math.max(getWidth(), minWidth);
            int height = Math.max(getHeight(), minHeight);
            if(getParent() != null) {
                int x = Math.min(getX(), getParent().getInnerRight() - width);
                int y = Math.min(getY(), getParent().getInnerBottom() - height);
                setPosition(x, y);
            }
            setSize(width, height);
        }

        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            Widget child = getChild(i);
            if(!isFrameElement(child)) {
                child.setPosition(getInnerX(), getInnerY());
                child.setSize(getInnerWidth(), getInnerHeight());
            }
        }

        layoutTitle();
        layoutCloseButton();
        layoutResizeHandle();
    }

    protected void layoutTitle() {
        setArea(getY(), getX(), getRight(), getBottom());
        
        int titleX = getTitleX(titleAreaLeft);
        int titleY = getTitleY(titleAreaTop);
        int titleWidth = Math.max(0, getTitleX(titleAreaRight) - titleX);
        int titleHeight = Math.max(0, getTitleY(titleAreaBottom) - titleY);

        if(titleAreaLeft != titleAreaRight && titleAreaTop != titleAreaBottom) {
            if(titleWidget == null) {
                titleWidget = new TextWidget();
                titleWidget.setTheme("title");
                titleWidget.setMouseCursor(cursors[DragMode.POSITION.ordinal()]);
                titleWidget.setText(title);
                titleWidget.setClip(true);
            }
            if(titleWidget.getParent() == null) {
                insertChild(titleWidget, 0);
            }

            titleWidget.setPosition(titleX, titleY);
            titleWidget.setSize(titleWidth, titleHeight);
        } else if(titleWidget != null && titleWidget.getParent() == this) {
            titleWidget.destroy();
            removeChild(titleWidget);
        }
    }

    protected void layoutCloseButton() {
        if(closeButton != null) {
            closeButton.adjustSize();
            closeButton.setPosition(
                    getTitleX(closeButtonX),
                    getTitleY(closeButtonY));
            closeButton.setVisible(closeButton.hasCallbacks() && hasCloseButton);
        }
    }

    protected void layoutResizeHandle() {
        if(hasResizeHandle && resizeHandle == null) {
            resizeHandle = new Widget(getAnimationState());
            resizeHandle.setTheme("resizeHandle");
            super.insertChild(resizeHandle, 0);
        }
        if(resizeHandle != null) {
            if(resizeHandleX > 0) {
                if(resizeHandleY > 0) {
                    resizeHandleDragMode = DragMode.CORNER_TL;
                } else {
                    resizeHandleDragMode = DragMode.CORNER_TR;
                }
            } else if(resizeHandleY > 0) {
                resizeHandleDragMode = DragMode.CORNER_BL;
            } else {
                resizeHandleDragMode = DragMode.CORNER_BR;
            }

            resizeHandle.adjustSize();
            resizeHandle.setPosition(
                    getTitleX(resizeHandleX),
                    getTitleY(resizeHandleY));
            resizeHandle.setVisible(hasResizeHandle);
        } else {
            resizeHandleDragMode = DragMode.NONE;
        }
    }

    @Override
    protected void keyboardFocusGained() {
        if(getBackground() != backgroundActive) {
            setBackground(backgroundActive);
        }
        fadeTo(Color.WHITE, fadeDurationActivate);
    }

    @Override
    protected void keyboardFocusLost() {
        if(!hasOpenPopups()) {
            if(getBackground() != backgroundInactive) {
                setBackground(backgroundInactive);
            }
            if(super.isVisible()) {
                fadeTo(fadeColorInactive, fadeDurationDeactivate);
            }
        }
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            Widget child = getChild(i);
            if(!isFrameElement(child)) {
                minWidth = Math.max(minWidth, child.getMinWidth() + getBorderHorizontal());
            }
        }
        if(hasTitleBar() && titleAreaRight < 0) {
            minWidth = Math.max(minWidth, titleWidget.getPreferredWidth() + titleAreaLeft - titleAreaRight);
        }
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = super.getMinHeight();
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            Widget child = getChild(i);
            if(!isFrameElement(child)) {
                minHeight = Math.max(minHeight, child.getMinHeight() + getBorderVertical());
            }
        }
        return minHeight;
    }

    @Override
    public int getPreferredInnerWidth() {
        int prefWidth = 0;
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            Widget child = getChild(i);
            if(!isFrameElement(child)) {
                prefWidth = Math.max(prefWidth, child.getPreferredWidth());
            }
        }
        return prefWidth;
    }

    @Override
    public int getPreferredWidth() {
        int prefWidth = super.getPreferredWidth();
        if(hasTitleBar() && titleAreaRight < 0) {
            prefWidth = Math.max(prefWidth, titleWidget.getPreferredWidth() + titleAreaLeft - titleAreaRight);
        }
        return prefWidth;
    }

    @Override
    public int getPreferredInnerHeight() {
        int prefHeight = 0;
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            Widget child = getChild(i);
            if(!isFrameElement(child)) {
                prefHeight = Math.max(prefHeight, child.getPreferredHeight());
            }
        }
        return prefHeight;
    }

    @Override
    public void adjustSize() {
        layoutTitle();
        super.adjustSize();
    }

    private int getTitleX(int offset) {
        return (offset < 0) ? getRight() + offset : getX() + offset;
    }

    private int getTitleY(int offset) {
        return (offset < 0) ? getBottom() + offset : getY() + offset;
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(dragMode != DragMode.NONE) {
            if(evt.isMouseDragEnd()) {
                dragMode = DragMode.NONE;
            } else if(evt.getType() == Event.Type.MOUSE_DRAGED) {
                handleMouseDrag(evt);
            }
            return true;
        }

        DragMode cursorMode = getDragMode(evt.getMouseX(), evt.getMouseY());
        MouseCursor cursor = cursors[cursorMode.ordinal()];
        setMouseCursor(cursor);

        if(!evt.isMouseDragEvent()) {
            if(evt.getType() == Event.Type.MOUSE_BTNDOWN &&
                    evt.getMouseButton() == Event.MOUSE_LBUTTON &&
                    handleMouseDown(evt)) {
                setMouseCursor(cursors[dragMode.ordinal()]);
                return true;
            }
        }

        if(super.handleEvent(evt)) {
            return true;
        }

        return evt.isMouseEvent();
    }

    private DragMode getDragMode(int mx, int my) {
        boolean left = mx < getInnerX();
        boolean right = mx >= getInnerRight();

        boolean top = my < getInnerY();
        boolean bot = my >= getInnerBottom();

        if(titleWidget != null && titleWidget.getParent() == this) {
            if(titleWidget.isInside(mx, my)) {
                return DragMode.POSITION;
            }
            top = my < titleWidget.getY();
        }

        if(closeButton != null && closeButton.isVisible() && closeButton.isInside(mx, my)) {
            return DragMode.NONE;
        }

        if(resizableAxis == ResizableAxis.NONE) {
            return DragMode.NONE;
        }
        
        if(resizeHandle != null && resizeHandle.isVisible() && resizeHandle.isInside(mx, my)) {
            return resizeHandleDragMode;
        }

        if(!resizableAxis.allowX) {
            left = false;
            right = false;
        }
        if(!resizableAxis.allowY) {
            top = false;
            bot = false;
        }
        
        if(left) {
            if(top) {
                return DragMode.CORNER_TL;
            }
            if(bot) {
                return DragMode.CORNER_BL;
            }
            return DragMode.EDGE_LEFT;
        }
        if(right) {
            if(top) {
                return DragMode.CORNER_TR;
            }
            if(bot) {
                return DragMode.CORNER_BR;
            }
            return DragMode.EDGE_RIGHT;
        }
        if(top) {
            return DragMode.EDGE_TOP;
        }
        if(bot) {
            return DragMode.EDGE_BOTTOM;
        }
        return DragMode.NONE;
    }

    private boolean handleMouseDown(Event evt) {
        final int mx = evt.getMouseX();
        final int my = evt.getMouseY();

        dragStartX = mx;
        dragStartY = my;
        dragInitialLeft = getX();
        dragInitialTop = getY();
        dragInitialRight = getRight();
        dragInitialBottom = getBottom();

        dragMode = getDragMode(mx, my);
        return dragMode != DragMode.NONE;
    }

    private void handleMouseDrag(Event evt) {
        final int dx = evt.getMouseX() - dragStartX;
        final int dy = evt.getMouseY() - dragStartY;
        
        int left = dragInitialLeft;
        int top = dragInitialTop;
        int right = dragInitialRight;
        int bottom = dragInitialBottom;

        switch(dragMode) {
        case CORNER_BL:
        case CORNER_TL:
        case EDGE_LEFT:
            left = Math.min(left + dx, right - getMinWidth());
            break;
        case CORNER_BR:
        case CORNER_TR:
        case EDGE_RIGHT:
            right = Math.max(right + dx, left + getMinWidth());
            break;
        case POSITION:
            if(getParent() != null) {
                int minX = getParent().getInnerX();
                int maxX = getParent().getInnerRight();
                int width = dragInitialRight - dragInitialLeft;
                left = Math.max(minX, Math.min(maxX - width, left + dx));
                right = Math.min(maxX, Math.max(minX + width, right + dx));
            } else {
                left += dx;
                right += dx;
            }
            break;
        }

        switch(dragMode) {
        case CORNER_TL:
        case CORNER_TR:
        case EDGE_TOP:
            top = Math.min(top + dy, bottom - getMinHeight());
            break;
        case CORNER_BL:
        case CORNER_BR:
        case EDGE_BOTTOM:
            bottom = Math.max(bottom + dy, top + getMinHeight());
            break;
        case POSITION:
            if(getParent() != null) {
                int minY = getParent().getInnerY();
                int maxY = getParent().getInnerHeight();
                int height = dragInitialBottom - dragInitialTop;
                top = Math.max(minY, Math.min(maxY - height, top + dy));
                bottom = Math.min(maxY, Math.max(minY + height, bottom + dy));
            } else {
                top += dy;
                bottom += dy;
            }
            break;
        }

        setArea(top, left, right, bottom);
    }

    private void setArea(int top, int left, int right, int bottom) {
        Widget p = getParent();
        if(p != null) {
            top = Math.max(top, p.getInnerY());
            left = Math.max(left, p.getInnerX());
            right = Math.min(right, p.getInnerRight());
            bottom = Math.min(bottom, p.getInnerBottom());
        }

        setPosition(left, top);
        setSize(Math.max(getMinWidth(), right-left),
                Math.max(getMinHeight(), bottom-top));
    }
}
