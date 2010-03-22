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

import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.model.ButtonModel;
import de.matthiasmann.twl.model.SimpleButtonModel;
import org.lwjgl.input.Keyboard;

/**
 * A generic button. Behavior is defined by a ButtonModel.
 * 
 * @author Matthias Mann
 */
public class Button extends TextWidget {
    
    public static final String STATE_ARMED = "armed";
    public static final String STATE_PRESSED = "pressed";
    public static final String STATE_SELECTED = "selected";

    private final Runnable stateChangedCB;
    private ButtonModel model;
    private String themeText;
    private String text;
    private String themeTooltip;

    public Button() {
        this(null, false, null);
    }

    public Button(ButtonModel model) {
        this(null, false, model);
    }

    /**
     * Creates a Button with a shared animation state
     *
     * @param animState the animation state to share, can be null
     */
    public Button(AnimationState animState) {
        this(animState, false, null);
    }

    /**
     * Creates a Button with a shared or inherited animation state
     *
     * @param animState the animation state to share or inherit, can be null
     * @param inherit true if the animation state should be inherited false for sharing
     */
    public Button(AnimationState animState, boolean inherit) {
        this(animState, inherit, null);
    }

    public Button(String text) {
        this(null, false, null);
        setText(text);
    }

    /**
     * Creates a Button with a shared animation state
     *
     * @param animState the animation state to share, can be null
     * @param model the button behavior model, if null a SimpleButtonModel is created
     */
    public Button(AnimationState animState, ButtonModel model) {
        this(animState, false, model);
    }

    /**
     * Creates a Button with a shared or inherited animation state
     *
     * @param animState the animation state to share or inherit, can be null
     * @param inherit true if the animation state should be inherited false for sharing
     * @param model the button behavior model, if null a SimpleButtonModel is created
     */
    public Button(AnimationState animState, boolean inherit, ButtonModel model) {
        super(animState, inherit);
        this.stateChangedCB = new Runnable() {
            public void run() {
                modelStateChanged();
            }
        };
        if(model == null) {
            model = new SimpleButtonModel();
        }
        setModel(model);
        setCanAcceptKeyboardFocus(true);
    }

    public ButtonModel getModel() {
        return model;
    }

    public void setModel(ButtonModel model) {
        if(model == null) {
            throw new NullPointerException("model");
        }
        boolean isConnected = getGUI() != null;
        if(this.model != null) {
            if(isConnected) {
                this.model.disconnect();
            }
            this.model.removeStateCallback(stateChangedCB);
        }
        this.model = model;
        this.model.addStateCallback(stateChangedCB);
        if(isConnected) {
            this.model.connect();
        }
        modelStateChanged();
    }

    @Override
    protected void widgetDisabled() {
        disarm();
    }

    @Override
    public void setEnabled(boolean enabled) {
        model.setEnabled(enabled);
    }

    public void addCallback(Runnable callback) {
        model.addActionCallback(callback);
    }

    public void removeCallback(Runnable callback) {
        model.removeActionCallback(callback);
    }

    public boolean hasCallbacks() {
        return model.hasActionCallbacks();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        updateText();
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeButton(themeInfo);
        invalidateLayoutTree();
    }

    protected void applyThemeButton(ThemeInfo themeInfo) {
        themeText = themeInfo.getParameterValue("text", false, String.class);
        themeTooltip = themeInfo.getParameterValue("tooltip", false, String.class);
        updateText();
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        if(model != null) {
            model.connect();
        }
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        if(model != null) {
            model.disconnect();
        }
        super.beforeRemoveFromGUI(gui);
    }

    @Override
    public Object getTooltipContent() {
        Object tooltipContent = super.getTooltipContent();
        return (tooltipContent == null) ? themeTooltip : tooltipContent;
    }

    @Override
    public int getMinWidth() {
        return Math.max(super.getMinWidth(), getPreferredWidth());
    }

    @Override
    public int getMinHeight() {
        return Math.max(super.getMinHeight(), getPreferredHeight());
    }

    protected final void doCallback() {
        getModel().fireActionCallback();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(!visible) {
            disarm();
        }
    }

    protected void disarm() {
        // disarm first to not fire a callback
        model.setHover(false);
        model.setArmed(false);
        model.setPressed(false);
    }

    void modelStateChanged() {
        super.setEnabled(model.isEnabled());
        AnimationState as = getAnimationState();
        as.setAnimationState(STATE_SELECTED, model.isSelected());
        as.setAnimationState(STATE_HOVER, model.isHover());
        as.setAnimationState(STATE_ARMED, model.isArmed());
        as.setAnimationState(STATE_PRESSED, model.isPressed());
    }

    void updateText() {
        if(text == null) {
            super.setCharSequence(TextUtil.notNull(themeText));
        } else {
            super.setCharSequence(text);
        }
    }

    @Override
    public boolean handleEvent(Event evt) {
        if(evt.isMouseEvent()) {
            boolean hover = (evt.getType() != Event.Type.MOUSE_EXITED) && isMouseInside(evt);
            model.setHover(hover);
            model.setArmed(hover && model.isPressed());
        }
        if(!model.isEnabled()) {
            // don't process event for a disabled button (except hover above)
            return false;
        }
        switch (evt.getType()) {
        case MOUSE_BTNDOWN:
            if(evt.getMouseButton() == Event.MOUSE_LBUTTON) {
                model.setPressed(true);
                model.setArmed(true);
            }
            break;
        case MOUSE_BTNUP:
            if(evt.getMouseButton() == Event.MOUSE_LBUTTON) {
                model.setPressed(false);
                model.setArmed(false);
            }
            break;
        case KEY_PRESSED:
            switch (evt.getKeyCode()) {
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_SPACE:
                if(!evt.isKeyRepeated()) {
                    model.setPressed(true);
                    model.setArmed(true);
                }
                return true;
            }
            break;
        case KEY_RELEASED:
            switch (evt.getKeyCode()) {
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_SPACE:
                model.setPressed(false);
                model.setArmed(false);
                return true;
            }
            break;
        case POPUP_OPENED:
            model.setHover(false);
            break;
        case MOUSE_WHEEL:
            // ignore mouse wheel
            return false;
        }
        // eat all mouse events
        return evt.isMouseEvent();
    }

}
