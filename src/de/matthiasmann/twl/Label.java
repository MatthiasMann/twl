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

import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.renderer.Font;

/**
 * A label widget.
 * 
 * @author Matthias Mann
 */
public class Label extends TextWidget {

    public enum CallbackReason {
        CLICK,
        DOUBLE_CLICK
    };
    
    private boolean autoSize = true;
    private Widget labelFor;
    private CallbackWithReason[] callbacks;
    
    public Label() {
        this((AnimationState)null);
    }

    public Label(AnimationState animState) {
        super(animState);
    }

    public Label(String text) {
        this();
        setText(text);
    }

    public void addCallback(CallbackWithReason<CallbackReason> cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, CallbackWithReason.class);
    }

    public void removeCallback(CallbackWithReason<CallbackReason> cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb, CallbackWithReason.class);
    }

    @SuppressWarnings("unchecked")
    protected void doCallback(CallbackReason reason) {
        if(callbacks != null) {
            for(CallbackWithReason cb : callbacks) {
                ((CallbackWithReason<CallbackReason>)cb).callback(reason);
            }
        }
    }

    public boolean isAutoSize() {
        return autoSize;
    }

    public void setAutoSize(boolean autoSize) {
        this.autoSize = autoSize;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if(autoSize) {
            adjustSize();
        }
    }

    @Override
    public String getText() {
        return (String)super.getText();
    }
    
    public void setText(String text) {
        text = TextUtil.notNull(text);
        if(!text.equals(getText())) {
            super.setText(text);
            if(autoSize) {
                adjustSize();
            }
        }
    }

    @Override
    public Object getTooltipContent() {
        Object toolTipContent = super.getTooltipContent();
        if(toolTipContent == null && labelFor != null) {
            return labelFor.getTooltipContent();
        }
        return toolTipContent;
    }

    public Widget getLabelFor() {
        return labelFor;
    }

    public void setLabelFor(Widget labelFor) {
        if(labelFor == this) {
            throw new IllegalArgumentException("labelFor == this");
        }
        this.labelFor = labelFor;
    }

    protected void applyThemeLabel(ThemeInfo themeInfo) {
        Object themeTooltip = themeInfo.getParameterValue("tooltip", false);
        if(themeTooltip != null) {
            setTooltipContent(themeTooltip);
        }
        
        String themeText = themeInfo.getParameterValue("text", false, String.class);
        if(themeText != null) {
            setText(themeText);
        }
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeLabel(themeInfo);
    }

    @Override
    public boolean requestKeyboardFocus() {
        if(labelFor != null) {
            return labelFor.requestKeyboardFocus();
        } else {
            return super.requestKeyboardFocus();
        }
    }

    @Override
    public int getMinWidth() {
        return getPreferedWidth();
    }

    @Override
    public int getMinHeight() {
        return getPreferedHeight();
    }

    @Override
    public boolean handleEvent(Event evt) {
        if(evt.isMouseEvent()) {
            if(evt.getType() == Event.Type.MOUSE_CLICKED) {
                handleClick(false);
            } else if(evt.getType() == Event.Type.MOUSE_DOUBLE_CLICKED) {
                handleClick(true);
            }
            return evt.getType() != Event.Type.MOUSE_WHEEL;
        }
        return false;  
    }
    
    protected void handleClick(boolean doubleClick) {
        doCallback(doubleClick ? CallbackReason.DOUBLE_CLICK : CallbackReason.CLICK);
    }
}
