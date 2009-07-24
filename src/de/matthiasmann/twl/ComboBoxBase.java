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

/**
 * base class for drop down comboboxes.
 *
 * Manages layout of label and button and opening the popup.
 * Subclasses have to create and add the label, and add the popup content.
 *
 * @author Matthias Mann
 */
public abstract class ComboBoxBase extends Widget {

    public static final String STATE_COMBOBOX_KEYBOARD_FOCUS = "comboboxKeyboardFocus";
    
    protected final Button button;
    protected final PopupWindow popup;
    
    protected ComboBoxBase() {
        this.button = new Button(getAnimationState());
        this.popup = new PopupWindow(this);

        button.addCallback(new Runnable() {
            public void run() {
                openPopup();
            }
        });

        add(button);
        setCanAcceptKeyboardFocus(true);
        setDepthFocusTraversal(false);
    }
    
    protected abstract Widget getLabel();

    protected boolean openPopup() {
        if(popup.openPopup()) {
            int minHeight = popup.getMinHeight();
            int popupHeight = computeSize(minHeight,
                    popup.getPreferredHeight(),
                    popup.getMaxHeight());
            int popupMaxBottom = popup.getParent().getInnerBottom();
            if(getBottom() + minHeight > popupMaxBottom) {
                popup.setPosition(getX(), popupMaxBottom - minHeight);
            } else {
                popup.setPosition(getX(), getBottom());
            }
            popupHeight = Math.min(popupHeight, popupMaxBottom - popup.getY());
            popup.setSize(getWidth(), popupHeight);
            return true;
        }
        return false;
    }

    @Override
    public int getPreferredInnerWidth() {
        return getLabel().getPreferredWidth() + button.getPreferredWidth();
    }

    @Override
    public int getPreferredInnerHeight() {
        return Math.max(getLabel().getPreferredHeight(), button.getPreferredHeight());
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        minWidth = Math.max(minWidth, getLabel().getMinWidth() + button.getMinWidth());
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = super.getMinHeight();
        minHeight = Math.max(minHeight, getLabel().getMinHeight());
        minHeight = Math.max(minHeight, button.getMinHeight());
        return minHeight;
    }

    @Override
    protected void layout() {
        int btnWidth = button.getPreferredWidth();
        int innerHeight = getInnerHeight();
        button.setPosition(getInnerRight() - btnWidth, getInnerY());
        button.setSize(btnWidth, innerHeight);
        getLabel().setSize(Math.max(0, button.getX() - getInnerX()), innerHeight);
    }

    private void setRecursive(Widget w, String what, boolean state) {
        w.getAnimationState().setAnimationState(what, state);
        for(int i=0 ; i<w.getNumChildren() ; ++i) {
            Widget child = w.getChild(i);
            setRecursive(child, what, state);
        }
    }

    @Override
    protected void keyboardFocusGained() {
        super.keyboardFocusGained();
        setRecursive(getLabel(), STATE_COMBOBOX_KEYBOARD_FOCUS, true);
    }

    @Override
    protected void keyboardFocusLost() {
        super.keyboardFocusLost();
        setRecursive(getLabel(), STATE_COMBOBOX_KEYBOARD_FOCUS, false);
    }

}
