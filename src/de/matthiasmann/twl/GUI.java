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

import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.theme.ThemeManager;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Root of a UI tree. Handles timing, mouse and keyboard events, popups, tooltips etc.
 * 
 * @author Matthias Mann
 */
public final class GUI extends Widget {

    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    public interface MouseIdleListener {
        public void mouseEnterIdle();
        public void mouseExitIdle();
    }
    
    private static final int DRAG_DIST = 3;
    private static final int DBLCLICK_TIME = 500;   // ms
    private static final int KEYREPEAT_INITIAL_DELAY = 250; // ms
    private static final int KEYREPEAT_INTERVAL_DELAY = 1000/30;    // ms
    private static final int NO_REPEAT = 0;
    
    private static final int TOOLTIP_OFFSET_X = 0;
    private static final int TOOLTIP_OFFSET_Y = 0;
    private static final int TOOLTIP_DELAY = 1000;  // 1 sec in ms
    
    private final Renderer renderer;
    
    long curTime;
    private int deltaTime;
    
    private Widget rootPane;
    boolean hasInvalidLayouts;

    final EventImpl event;
    private boolean wasInside;
    private boolean dragActive;
    private int mouseClickCount;
    private int dragButton;
    private int mouseDownX;
    private int mouseDownY;
    private int mouseLastX;
    private int mouseLastY;
    private int mouseClickedX;
    private int mouseClickedY;
    private long mouseEventTime;
    private long mouseClickedTime;
    private long keyEventTime;
    private int keyRepeatDelay;
    private boolean popupEventOccured;
    private Widget lastMouseDownWidget;
    private Widget lastMouseClickWidget;
    
    private int mouseIdleTime = 60;
    private boolean mouseIdleState;
    private MouseIdleListener mouseIdleListener;
    
    private Rect[] clipRects;
    private int numClipRects;
    
    private final TooltipWindow tooltipWindow;
    private final Label tooltipLabel;
    private Widget tooltipOwner;
    
    private final ArrayList<TimerImpl> activeTimers;
    
    public GUI(Renderer renderer) {
        this(new Widget(), renderer);
        rootPane.setTheme("");
        rootPane.setFocusKeyEnabled(false);
    }
    
    public GUI(Widget rootPane, Renderer renderer) {
        if(rootPane == null) {
            throw new NullPointerException("rootPane");
        }
        if(renderer == null) {
            throw new NullPointerException("renderer");
        }
        
        this.renderer = renderer;
        this.event = new EventImpl();
        this.rootPane = rootPane;
        this.rootPane.setFocusKeyEnabled(false);
        this.clipRects = new Rect[8];

        this.tooltipLabel = new Label();
        this.tooltipWindow = new TooltipWindow();
        this.tooltipWindow.add(tooltipLabel);
        this.tooltipWindow.setVisible(false);
        
        this.activeTimers = new ArrayList<TimerImpl>();
        
        setTheme("");
        setFocusKeyEnabled(false);
        setSize();
        
        super.insertChild(rootPane, 0);
        super.insertChild(tooltipWindow, 1);
        
        resyncTimerAfterPause();
    }
    
    /**
     * Applies the active theme to this widget and it's children.
     * If a widget in the tree has an empty theme name then it
     * is omitted from this process but it children are still processed.
     * 
     * @param themeManager the theme manager that should be used
     * @throws java.lang.NullPointerException if themeManager is null
     */
    @Override
    public void applyTheme(ThemeManager themeManager) {
        if(themeManager == null) {
            throw new NullPointerException("themeManager");
        }
        
        super.applyTheme(themeManager);
    }

    public Widget getRootPane() {
        return rootPane;
    }

    public void setRootPane(Widget rootPane) {
        if(rootPane == null) {
            throw new NullPointerException("rootPane");
        }
        this.rootPane = rootPane;
        super.removeChild(0);
        super.insertChild(rootPane, 0);
    }

    public Renderer getRenderer() {
        return renderer;
    }
    
    public MouseSensitiveRectangle createMouseSenitiveRectangle() {
        return new MouseSensitiveRectangle() {
            @Override
            public boolean isMouseOver() {
                // use last event's mouse position
                return isInside(event.mouseX, event.mouseY);
            }
        };
    }
    
    public Timer createTimer() {
        return new TimerImpl();
    }

    public long getCurrentTime() {
        return curTime;
    }
    
    public boolean requestToolTip(Widget widget, int x, int y,
            Object content, Alignment alignment) {
        if(alignment == null) {
            throw new NullPointerException("alignment");
        }
        if(widget == getWidgetUnderMouse()) {
            setTooltip(x, y, widget, content, alignment);
            return true;
        }
        return false;
    }

    public void requestToolTipUpdate(Widget widget) {
        if(tooltipOwner == widget) {
            tooltipOwner = null;
        }
    }

    public MouseIdleListener getMouseIdleListener() {
        return mouseIdleListener;
    }

    public void setMouseIdleListener(MouseIdleListener mouseIdleListener) {
        this.mouseIdleListener = mouseIdleListener;
        callMouseIdleListener();
    }

    public int getMouseIdleTime() {
        return mouseIdleTime;
    }

    public void setMouseIdleTime(int mouseIdleTime) {
        if(mouseIdleTime < 1) {
            throw new IllegalArgumentException("mouseIdleTime < 1");
        }
        this.mouseIdleTime = mouseIdleTime;
    }
    
    @Override
    public boolean setPosition(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertChild(Widget child, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Widget removeChild(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void adjustSize() {
        rootPane.adjustSize();
    }

    @Override
    protected void sizeChanged() {
        rootPane.setSize(getWidth(), getHeight());
    }

    @Override
    public void validateLayout() {
        int count = 1000;
        while(hasInvalidLayouts && count > 0) {
            hasInvalidLayouts = false;
            super.validateLayout();
            count--;
        }
        if(count == 0) {
            debugLayoutLoop();
        }
    }

    /**
     * Sets the size of the GUI based on the OpenGL viewport.
     */
    public void setSize() {
        setSize(renderer.getWidth(), renderer.getHeight());
    }
    
    /**
     * Handles keyboard, mouse, timers and executes draw()
     * 
     * This is the easiest method to use this GUI
     */
    public void update() {
        setSize();
        updateTime();
        handleKeyboardInputLWJGL();
        handleMouseInputLWJGL();
        updateTimers();
        validateLayout();
        draw();
        setCursor();
    }

    /**
     * when calls to updateTime where stoped then this method should be called
     * before calling updateTime again to prevent a large delta jump.
     * This allows the UI timer to be suspended.
     */
    public void resyncTimerAfterPause() {
        this.curTime = getTimeMillis();
        this.deltaTime = 0;
    }
    
    public void updateTime() {
        long newTime = getTimeMillis();
        deltaTime = Math.max(0, (int)(newTime - curTime));
        curTime = newTime;
    }

    public long getTimeMillis() {
        long res = Sys.getTimerResolution();
        long time = Sys.getTime();
        if(res != 1000) {
            time = (time * 1000) / res;
        }
        return time;
    }
    
    public void updateTimers() {
        for(int i=0 ; i<activeTimers.size() ;) {
            if(!activeTimers.get(i).tick(deltaTime)) {
                activeTimers.remove(i);
            } else {
                i++;
            }
        }
        deltaTime = 0;
    }
    
    public void draw() {
        numClipRects = 0;
        
        renderer.startRenderering();
        try {
            drawWidget(this);
        } finally {
            renderer.endRendering();
        }
    }

    public void setCursor() {
        Widget widget = getWidgetUnderMouse();
        if(widget != null) {
            MouseCursor cursor = widget.getMouseCursor();
            renderer.setCursor(cursor);
        }
    }
    
    public void handleKeyboardInputLWJGL() {
        if(Keyboard.isCreated()) {
            while(Keyboard.next()) {
                handleKey(
                        Keyboard.getEventKey(),
                        Keyboard.getEventCharacter(),
                        Keyboard.getEventKeyState());
            }
            
            handleKeyRepeat();
        }
    }
    
    public void handleMouseInputLWJGL() {
        if(Mouse.isCreated()) {
            while(Mouse.next()) {
                handleMouse(Mouse.getEventX(), getHeight() - Mouse.getEventY(),
                        Mouse.getEventButton(), Mouse.getEventButtonState());

                int wheelDelta = Mouse.getEventDWheel();
                if(wheelDelta != 0) {
                    handleMouseWheel(wheelDelta / 120);
                }
            }
            
            handleTooltips();
        }
    }
    
    /**
     * Mouse has moved / button was pressed or released.
     * 
     * @param mouseX the new mouse X coordinate
     * @param mouseY the new mouse Y coordinate
     * @param button the button that has been pressed/released or -1 if no button changed
     * @param pressed true if the button was pressed. Ignored if button is -1.
     */
    public final void handleMouse(int mouseX, int mouseY, int button, boolean pressed) {
        mouseEventTime = curTime;
        event.mouseButton = button;

        // only the previously pressed mouse button
        int prevButtonState = event.modifier & Event.MODIFIER_BUTTON;
        
        int buttonMask = 0;
        switch (button) {
        case Event.MOUSE_LBUTTON:
            buttonMask = Event.MODIFIER_LBUTTON;
            break;
        case Event.MOUSE_RBUTTON:
            buttonMask = Event.MODIFIER_RBUTTON;
            break;
        case Event.MOUSE_MBUTTON:
            buttonMask = Event.MODIFIER_MBUTTON;
            break;
        }
        event.setModifier(buttonMask, pressed);
        boolean wasPressed = (prevButtonState & buttonMask) != 0;

        // don't send new mouse coords when still in drag area
        if(dragActive || prevButtonState == 0) {
            event.mouseX = mouseX;
            event.mouseY = mouseY;
        } else {
            event.mouseX = mouseDownX;
            event.mouseY = mouseDownY;
        }

        if(!isInside(mouseX, mouseY)) {
            pressed = false;
            mouseClickCount = 0;
            if(wasInside) {
                sendMouseEvent(Event.Type.MOUSE_EXITED, null);
                wasInside = false;
            }
        } else if(!wasInside) {
            wasInside = true;
            sendMouseEvent(Event.Type.MOUSE_ENTERED, null);
        }
        
        if(mouseX != mouseLastX || mouseY != mouseLastY) {
            mouseLastX = mouseX;
            mouseLastY = mouseY;

            if(prevButtonState != 0 && !dragActive) {
                if(Math.abs(mouseX - mouseDownX) > DRAG_DIST ||
                    Math.abs(mouseY - mouseDownY) > DRAG_DIST) {
                    dragActive = true;
                    mouseClickCount = 0;
                }
            }
            
            if(dragActive) {
                // send MOUSE_DRAGGED only to the widget which received the MOUSE_BTNDOWN
                if(lastMouseDownWidget != null) {
                    sendMouseEvent(Event.Type.MOUSE_DRAGED, lastMouseDownWidget);
                }
            } else if(prevButtonState == 0) {
                sendMouseEvent(Event.Type.MOUSE_MOVED, null);
            }
        }

        if(buttonMask != 0 && pressed != wasPressed) {
            if(pressed) {
                if(dragButton < 0) {
                    mouseDownX = mouseX;
                    mouseDownY = mouseY;
                    dragButton = button;
                    lastMouseDownWidget = sendMouseEvent(Event.Type.MOUSE_BTNDOWN, null);
                } else if(lastMouseDownWidget != null) {
                    // if another button is pressed while one button is already
                    // pressed then route the second button to the widget which
                    // received the first press
                    sendMouseEvent(Event.Type.MOUSE_BTNDOWN, lastMouseDownWidget);
                }
            } else if(dragButton >= 0) {
                // send MOUSE_BTNUP only to the widget which received the MOUSE_BTNDOWN
                if(lastMouseDownWidget != null) {
                    sendMouseEvent(Event.Type.MOUSE_BTNUP, lastMouseDownWidget);
                }
            }

            if(button == Event.MOUSE_LBUTTON && !popupEventOccured) {
                if(!pressed && !dragActive) {
                    if(mouseClickCount == 0 ||
                            curTime - mouseClickedTime > DBLCLICK_TIME ||
                            lastMouseClickWidget != lastMouseDownWidget) {
                        mouseClickedX = mouseX;
                        mouseClickedY = mouseY;
                        lastMouseClickWidget = lastMouseDownWidget;
                        mouseClickCount = 0;
                        mouseClickedTime = curTime;
                    }
                    if(Math.abs(mouseX - mouseClickedX) < DRAG_DIST &&
                            Math.abs(mouseY - mouseClickedY) < DRAG_DIST) {
                        // ensure same click target as first
                        event.mouseX = mouseClickedX;
                        event.mouseY = mouseClickedY;
                        event.mouseClickCount = ++mouseClickCount;
                        mouseClickedTime = curTime;
                        if(lastMouseClickWidget != null) {
                            sendMouseEvent(Event.Type.MOUSE_CLICKED, lastMouseClickWidget);
                        }
                    } else {
                        lastMouseClickWidget = null;
                    }
                }
            }
        }

        if(event.isMouseDragEnd()) {
            dragActive = false;
            dragButton = -1;
        }
    }
    
    /**
     * Mouse wheel has been turned. Must be called after handleMouse.
     * 
     * @param wheelDelta the normalized wheel delta
     */
    public final void handleMouseWheel(int wheelDelta) {
        event.mouseWheelDelta = wheelDelta;
        sendEvent(Event.Type.MOUSE_WHEEL);
    }
    
    public final void handleKey(int keyCode, char keyChar, boolean pressed) {
        event.keyCode = keyCode;
        event.keyChar = keyChar;
        event.keyRepeated = false;

        keyEventTime = curTime;
        if(event.keyCode != Keyboard.KEY_NONE) {
            setModifiers(event.keyCode, pressed);

            if(pressed) {
                keyRepeatDelay = KEYREPEAT_INITIAL_DELAY;
                sendEvent(Event.Type.KEY_PRESSED);
            } else {
                keyRepeatDelay = NO_REPEAT;
                sendEvent(Event.Type.KEY_RELEASED);
            }
        } else {
            keyRepeatDelay = NO_REPEAT;
        }
        if(event.keyChar != Keyboard.CHAR_NONE) {
            sendEvent(Event.Type.CHAR_TYPED);
        }
    }
    
    /**
     * Must be called after calling handleKey().
     * @see #handleKey(int, char, boolean) 
     */
    public final void handleKeyRepeat() {
        if(keyRepeatDelay != NO_REPEAT) {
            long keyDeltaTime = curTime - keyEventTime;
            if(keyDeltaTime > keyRepeatDelay) {
                keyEventTime = curTime;
                keyRepeatDelay = KEYREPEAT_INTERVAL_DELAY;
                event.keyRepeated = true;
                sendEvent(Event.Type.KEY_PRESSED);  // refire last key event
                if(event.keyChar != Keyboard.CHAR_NONE) {
                    sendEvent(Event.Type.CHAR_TYPED);
                }
            }
        }
    }
    
    /**
     * Must be called after calling handleMouse or handleMouseWheel
     * @see #handleMouse(int, int, int, boolean) 
     * @see #handleMouseWheel(int)
     */
    public final void handleTooltips() {
        Widget widgetUnderMouse = getWidgetUnderMouse();
        if(widgetUnderMouse != tooltipOwner) {
            if(widgetUnderMouse != null && (curTime-mouseEventTime) > TOOLTIP_DELAY) {
                setTooltip(
                        event.mouseX + TOOLTIP_OFFSET_X,
                        event.mouseY + TOOLTIP_OFFSET_Y,
                        widgetUnderMouse,
                        widgetUnderMouse.getTooltipContent(),
                        Alignment.BOTTOMLEFT);
            } else {
                hideTooltip();
            }
        }

        boolean mouseIdle = (curTime - mouseEventTime) > mouseIdleTime;
        if(mouseIdleState != mouseIdle) {
            mouseIdleState = mouseIdle;
            callMouseIdleListener();
        }
    }

    private Widget getTopPane() {
        // don't use potential overwritten methods
        return super.getChild(super.getNumChildren()-2);
    }
    
    @Override
    Widget getWidgetUnderMouse() {
        return getTopPane().getWidgetUnderMouse();
    }

    private Widget sendMouseEvent(Event.Type type, Widget target) {
        assert type.isMouseEvent;
        popupEventOccured = false;
        event.type = type;
        event.dragEvent = dragActive;

        if(target != null) {
            target.handleEvent(event);
            return target;
        } else {
            assert !dragActive;
            return getTopPane().routeMouseEvent(event);
        }
    }

    private void sendEvent(Event.Type type) {
        assert !type.isMouseEvent;
        popupEventOccured = false;
        event.type = type;
        event.dragEvent = false;
        getTopPane().handleEvent(event);
    }

    private void sendPopupEvent(Event.Type type) {
        assert type == Event.Type.POPUP_OPENED || type == Event.Type.POPUP_CLOSED;
        popupEventOccured = false;
        event.type = type;
        event.dragEvent = false;
        getTopPane().routePopupEvent(event);
    }

    void openPopup(PopupWindow popup) {
        if(popup.getParent() == this) {
            closePopup(popup);
        } else if(popup.getParent() != null) {
            throw new IllegalArgumentException("popup must not be added anywhere");
        }
        hideTooltip();
        sendPopupEvent(Event.Type.POPUP_OPENED);
        super.insertChild(popup, getNumChildren()-1);
        popup.getOwner().setOpenPopup(this, true);
        super.requestKeyboardFocus(popup);
        popupEventOccured = true;
    }
    
    void closePopup(PopupWindow popup) {
        int idx = getChildIndex(popup);
        if(idx > 0) {
            super.removeChild(idx);
        }
        requestKeyboardFocus(null);
        popup.getOwner().recalcOpenPopups(this);
        sendPopupEvent(Event.Type.POPUP_CLOSED);
        popupEventOccured = true;
        popup.getOwner().requestKeyboardFocus();
    }

    boolean hasOpenPopups(Widget owner) {
        for(int i=getNumChildren()-1 ; i-->1 ;) {
            PopupWindow popup = (PopupWindow)getChild(i);
            if(popup.getOwner() == popup.getOwner()) {
                return true;
            }
        }
        return false;
    }
    
    void closePopupFromWidgets(Widget widget) {
        for(int i=getNumChildren()-1 ; i-->1 ;) {
            PopupWindow popup = (PopupWindow)getChild(i);
            Widget owner = popup.getOwner();
            while(owner != null && owner != widget) {
                owner = owner.getParent();
            }
            if(owner == widget) {
                closePopup(popup);
            }
        }
    }

    void widgetHidden(Widget widget) {
        closePopupFromWidgets(widget);
        Widget to = tooltipOwner;
        while(to != null && to != widget) {
            to = to.getParent();
        }
        if(to != null) {
            hideTooltip();
        }
    }

    @Override
    public boolean requestKeyboardFocus() {
        // GUI always has the keyboard focus
        return true;
    }
    
    @Override
    protected boolean requestKeyboardFocus(Widget child) {
        if(child != null) {
            if(child != getTopPane()) {
                return false;
            }
        }
        return super.requestKeyboardFocus(child);
    }

    private void debugLayoutLoop() {
        ArrayList<Widget> widgetsInLoop = new ArrayList<Widget>();
        collectLayoutLoop(widgetsInLoop);
        System.err.println("WARNING: layout loop detected - printing");
        for(int i=0,n=widgetsInLoop.size() ; i<n ; i++) {
            System.err.println(i+": "+widgetsInLoop.get(i));
        }
    }

    private void hideTooltip() {
        tooltipWindow.setVisible(false);
        tooltipOwner = null;
    }

    private void setTooltip(int x, int y, Widget widget, Object content,
            Alignment alignment) throws IllegalArgumentException {
        if(content == null) {
            hideTooltip();
            return;
        }
        
        if(content instanceof String) {
            String text = (String)content;
            if(text.length() == 0) {
                hideTooltip();
                return;
            }
            tooltipLabel.setBackground(null);
            tooltipLabel.setText(text);
            tooltipWindow.adjustSize();
        } else {
            throw new IllegalArgumentException("Unsupported data type");
        }
        
        int ttWidth = tooltipWindow.getWidth();
        int ttHeight = tooltipWindow.getHeight();
        
        switch(alignment) {
        case TOP:
        case CENTER:
        case BOTTOM:
            x -= ttWidth / 2;
            break;
        case TOPRIGHT:
        case RIGHT:
        case BOTTOMRIGHT:
            x -= ttWidth;
            break;
        }
        
        switch(alignment) {
        case LEFT:
        case CENTER:
        case RIGHT:
            y -= ttHeight / 2;
            break;
        case BOTTOMLEFT:
        case BOTTOM:
        case BOTTOMRIGHT:
            y -= ttHeight;
            break;
        }
        
        if(x + ttWidth > getWidth()) {
            x = getWidth() - ttWidth;
        }
        if(y + ttHeight > getHeight()) {
            y = getHeight() - ttHeight;
        }
        if(x < 0) {
            x = 0;
        }
        if(y < 0) {
            y = 0;
        }
        
        tooltipOwner = widget;
        tooltipWindow.setPosition(x, y);
        tooltipWindow.setVisible(true);
    }

    void clipEnter(int x, int y, int w, int h) {
        Rect rect;
        if(numClipRects == clipRects.length) {
            Rect[] newRects = new Rect[numClipRects*2];
            System.arraycopy(clipRects, 0, newRects, 0, numClipRects);
            clipRects = newRects;
        }
        if((rect = clipRects[numClipRects]) == null) {
            rect = new Rect();
            clipRects[numClipRects] = rect;
        }
        rect.setXYWH(x, y, w, h);
        if(numClipRects > 0) {
            rect.intersect(clipRects[numClipRects-1]);
        }
        renderer.setClipRect(rect);
        numClipRects++;
    }
    
    void clipLeave() {
        numClipRects--;
        if(numClipRects == 0) {
            renderer.setClipRect(null);
        } else {
            renderer.setClipRect(clipRects[numClipRects-1]);
        }
    }

    private void setModifiers(int keyCode, boolean pressed) {
        switch(keyCode) {
            case Keyboard.KEY_LSHIFT:
                event.setModifier(Event.MODIFIER_LSHIFT, pressed);
                break;
            case Keyboard.KEY_LMETA:
                event.setModifier(Event.MODIFIER_LMETA, pressed);
                break;
            case Keyboard.KEY_LCONTROL:
                event.setModifier(Event.MODIFIER_LCTRL, pressed);
                break;
            case Keyboard.KEY_RSHIFT:
                event.setModifier(Event.MODIFIER_RSHIFT, pressed);
                break;
            case Keyboard.KEY_RMETA:
                event.setModifier(Event.MODIFIER_RMETA, pressed);
                break;
            case Keyboard.KEY_RCONTROL:
                event.setModifier(Event.MODIFIER_RCTRL, pressed);
                break;
        }
    }
    
    private void callMouseIdleListener() {
        if(mouseIdleListener != null) {
            if(mouseIdleState) {
                mouseIdleListener.mouseEnterIdle();
            } else {
                mouseIdleListener.mouseExitIdle();
            }
        }
    }

    static class TooltipWindow extends Widget {
        public static final String STATE_FADE = "fade";
        private int fadeInTime;

        @Override
        protected void applyTheme(ThemeInfo themeInfo) {
            super.applyTheme(themeInfo);
            fadeInTime = themeInfo.getParameter("fadeInTime", 0);
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            getAnimationState().resetAnimationTime(STATE_FADE);
        }

        @Override
        protected void paint(GUI gui) {
            int time = getAnimationState().getAnimationTime(STATE_FADE);
            if(time < fadeInTime) {
                float alpha = time / (float)fadeInTime;
                gui.getRenderer().pushGlobalTintColor(1f, 1f, 1f, alpha);
                try {
                    super.paint(gui);
                } finally {
                    gui.getRenderer().popGlobalTintColor();
                }
            } else {
                super.paint(gui);
            }
        }

        @Override
        protected void layout() {
            for(int i=0,n=getNumChildren() ; i<n ; i++) {
                Widget c = getChild(i);
                c.setSize(getInnerWidth(), getInnerHeight());
                c.setPosition(getInnerX(), getInnerY());
            }
        }
    }
    
    static class EventImpl extends Event {
        Type type;
        int mouseX;
        int mouseY;
        int mouseWheelDelta;
        int mouseButton;
        int mouseClickCount;
        boolean dragEvent;
        boolean keyRepeated;
        char keyChar;
        int keyCode;
        int modifier;
        EventImpl subEvent;

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public boolean isMouseDragEvent() {
            return dragEvent;
        }

        @Override
        public boolean isMouseDragEnd() {
            return (modifier & MODIFIER_BUTTON) == 0;
        }

        @Override
        public int getMouseX() {
            return mouseX;
        }

        @Override
        public int getMouseY() {
            return mouseY;
        }

        @Override
        public int getMouseButton() {
            return mouseButton;
        }

        @Override
        public int getMouseWheelDelta() {
            return mouseWheelDelta;
        }

        @Override
        public int getMouseClickCount() {
            return mouseClickCount;
        }

        @Override
        public char getKeyChar() {
            return keyChar;
        }

        @Override
        public int getKeyCode() {
            return keyCode;
        }

        @Override
        public boolean isKeyRepeated() {
            return keyRepeated;
        }

        @Override
        public int getModifiers() {
            return modifier;
        }

        @Override
        public Event createSubEvent(Type newType) {
            if(subEvent == null) {
                subEvent = new EventImpl();
            }
            subEvent.type = newType;
            subEvent.mouseX = mouseX;
            subEvent.mouseY = mouseY;
            subEvent.mouseButton = mouseButton;
            subEvent.mouseWheelDelta = mouseWheelDelta;
            subEvent.mouseClickCount = mouseClickCount;
            subEvent.dragEvent = dragEvent;
            subEvent.keyRepeated = keyRepeated;
            subEvent.keyChar = keyChar;
            subEvent.keyCode = keyCode;
            subEvent.modifier = modifier;
            return subEvent;
        }

        void setModifier(int mask, boolean pressed) {
            if(pressed) {
                modifier |= mask;
            } else {
                modifier &= ~mask;
            }
        }
    }
    
    private static final int TIMER_COUNTER_IN_CALLBACK = -1;
    private static final int TIMER_COUNTER_DO_START = -2;
    private static final int TIMER_COUNTER_DO_STOP = -3;
    
    class TimerImpl implements Timer {
        int counter;
        int delay = 10;
        boolean continuous;
        Runnable callback;

        public boolean isRunning() {
            return counter > 0;
        }

        public void setDelay(int delay) {
            if(delay < 1) {
                throw new IllegalArgumentException("delay < 1");
            }
            this.delay = delay;
        }

        public void start() {
            if(counter == 0) {
                counter = delay;
                activeTimers.add(this);
            } else if(counter < 0) {
                counter = TIMER_COUNTER_DO_START;
            }
        }

        public void stop() {
            if(counter > 0) {
                counter = 0;
                activeTimers.remove(this);
            } else if(counter < 0) {
                counter = TIMER_COUNTER_DO_STOP;
            }
        }

        public void setCallback(Runnable callback) {
            this.callback = callback;
        }

        public boolean isContinuous() {
            return continuous;
        }

        public void setContinuous(boolean continuous) {
            this.continuous = continuous;
        }
        
        boolean tick(int delta) {
            int newCounter = counter - delta;
            if(newCounter <= 0) {
                boolean doStop = !continuous;
                counter = TIMER_COUNTER_IN_CALLBACK;
                doCallback();
                if(doStop && counter != TIMER_COUNTER_DO_START) {
                    counter = 0;
                    return false;
                } else {
                    // timer is already running
                    counter = Math.max(1, newCounter + delay);
                }
            } else {
                counter = newCounter;
            }
            return true;
        }
        
        private void doCallback() {
            if(callback != null) {
                try {
                    callback.run();
                } catch (Throwable ex) {
                    logger.log(Level.SEVERE, "Exception in callback", ex);
                }
            }
        }
    }
}
