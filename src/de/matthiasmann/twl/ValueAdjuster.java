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

import org.lwjgl.input.Keyboard;

/**
 * Base class for value adjuster widgets.
 * It has a value display/edit widget and 2 buttons.
 *
 * You can adjust the value via drag&drop on the display
 * or by clicking on the display which will open an edit field
 * or using the +/ buttons
 * or using the left/right keys
 * 
 * @author Matthias Mann
 */
public abstract class ValueAdjuster extends Widget {

    private static final int INITIAL_DELAY = 300;
    private static final int REPEAT_DELAY = 75;
    
    private final DraggableButton label;
    private final EditField editField;
    private final Button decButton;
    private final Button incButton;
    private final Runnable timerCallback;
    private Timer timer;

    private String displayPrefix = "";
    private int width;
    
    public ValueAdjuster() {
        this.label = new DraggableButton(new AnimationState(getAnimationState()));
        // EditField always inherits from the passed animation state
        this.editField = new EditField(getAnimationState());
        this.decButton = new Button(new AnimationState(getAnimationState()));
        this.incButton = new Button(new AnimationState(getAnimationState()));
        
        label.setClip(true);
        label.setTheme("valueDisplay");
        editField.setTheme("valueEdit");
        decButton.setTheme("decButton");
        incButton.setTheme("incButton");

        Runnable cbUpdateTimer = new Runnable() {
            public void run() {
                updateTimer();
            }
        };

        timerCallback = new Runnable() {
            public void run() {
                timer.setDelay(REPEAT_DELAY);
                onTimer();
            }
        };

        decButton.getModel().addStateCallback(cbUpdateTimer);
        incButton.getModel().addStateCallback(cbUpdateTimer);
        label.addCallback(new Runnable() {
            public void run() {
                startEdit();
            }
        });
        label.setListener(new DraggableButton.DragListener() {
            public void dragStarted() {
                onDragStart();
            }
            public void dragged(int deltaX, int deltaY) {
                onDragUpdate(deltaX);
            }
            public void dragStopped() {
            }
        });
        
        editField.setVisible(false);
        editField.addCallback(new EditField.Callback() {
            public void callback(int key) {
                handleEditCallback(key);
            }
        });
        
        add(label);
        add(editField);
        add(decButton);
        add(incButton);
        setCanAcceptKeyboardFocus(true);
        setDepthFocusTraversal(false);
    }

    public String getDisplayPrefix() {
        return displayPrefix;
    }

    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
        setDisplayText();
    }

    public void startEdit() {
        if(label.isVisible()) {
            editField.setErrorMessage(null);
            editField.setText(onEditStart());
            editField.setVisible(true);
            editField.requestKeyboardFocus();
            editField.selectAll();
            label.setVisible(false);
        }
    }
    
    public void cancelEdit() {
        if(editField.isVisible()) {
            onEditCanceled();
            label.setVisible(true);
            editField.setVisible(false);
        }
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        width = themeInfo.getParameter("width", 100);
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        minWidth = Math.max(minWidth,
                getBorderHorizontal() +
                decButton.getMinWidth() +
                Math.max(width, label.getMinWidth()) +
                incButton.getMinWidth());
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = label.getMinHeight();
        minHeight = Math.max(minHeight, decButton.getMinHeight());
        minHeight = Math.max(minHeight, incButton.getMinHeight());
        minHeight += getBorderVertical();
        return Math.max(minHeight, super.getMinHeight());
    }

    @Override
    public int getPreferredInnerWidth() {
        int prefWidth = super.getPreferredInnerWidth();
        prefWidth = Math.max(prefWidth,
                decButton.getPreferredWidth() +
                Math.max(width, label.getPreferredWidth()) +
                incButton.getPreferredWidth());
        return prefWidth;
    }

    @Override
    public int getPreferredInnerHeight() {
        int prefHeight = super.getPreferredInnerHeight();
        prefHeight = Math.max(prefHeight, decButton.getPreferredHeight());
        prefHeight = Math.max(prefHeight, label.getPreferredHeight());
        prefHeight = Math.max(prefHeight, incButton.getPreferredHeight());
        return prefHeight;
    }

    @Override
    protected void keyboardFocusLost() {
        cancelEdit();
        label.getAnimationState().setAnimationState(STATE_KEYBOARD_FOCUS, false);
    }

    @Override
    protected void keyboardFocusGained() {
        label.getAnimationState().setAnimationState(STATE_KEYBOARD_FOCUS, true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(!visible) {
            cancelEdit();
        }
    }

    @Override
    protected void widgetDisabled() {
        cancelEdit();
    }

    @Override
    protected void layout() {
        int height = getInnerHeight();
        int y = getInnerY();
        decButton.setPosition(getInnerX(), y);
        decButton.setSize(decButton.getPreferredWidth(), height);
        incButton.setPosition(getInnerRight() - incButton.getPreferredWidth(), y);
        incButton.setSize(incButton.getPreferredWidth(), height);
        int labelX = decButton.getRight();
        int labelWidth = Math.max(0, incButton.getX() - labelX);
        label.setSize(labelWidth, height);
        label.setPosition(labelX, y);
        editField.setSize(labelWidth, height);
        editField.setPosition(labelX, y);
    }
    
    protected void setDisplayText() {
        label.setText(displayPrefix.concat(formatText()));
    }

    protected abstract String formatText();

    void onTimer() {
        if(incButton.getModel().isArmed()) {
            cancelEdit();
            doIncrement();
        } else if(decButton.getModel().isArmed()) {
            cancelEdit();
            doDecrement();
        }
    }

    void updateTimer() {
        if(timer != null) {
            if(incButton.getModel().isArmed() || decButton.getModel().isArmed()) {
                if(!timer.isRunning()) {
                    onTimer();
                    timer.setDelay(INITIAL_DELAY);
                    timer.start();
                }
            } else {
                timer.stop();
            }
        }
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        timer = gui.createTimer();
        timer.setCallback(timerCallback);
        timer.setContinuous(true);
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        super.beforeRemoveFromGUI(gui);
        if(timer != null) {
            timer.stop();
        }
        timer = null;
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(evt.isKeyEvent()) {
            if(!editField.isVisible()) {
                switch(evt.getType()) {
                case KEY_PRESSED:
                    switch(evt.getKeyCode()) {
                    case Keyboard.KEY_RIGHT:
                        doIncrement();
                        return true;
                    case Keyboard.KEY_LEFT:
                        doDecrement();
                        return true;
                    case Keyboard.KEY_RETURN:
                        startEdit();
                        return true;
                    }
                }
                return true;
            }
        } else if(!editField.isVisible() && evt.getType() == Event.Type.MOUSE_WHEEL) {
            if(evt.getMouseWheelDelta() < 0) {
                doDecrement();
            } else if(evt.getMouseWheelDelta() > 0) {
                doIncrement();
            }
            return true;
        }
        return super.handleEvent(evt);
    }
    
    protected abstract String onEditStart();
    protected abstract boolean onEditEnd(String text);
    protected abstract String validateEdit(String text);
    protected abstract void onEditCanceled();
    
    protected abstract void onDragStart();
    protected abstract void onDragUpdate(int dragDelta);
    
    protected abstract void doDecrement();
    protected abstract void doIncrement();
    
    void handleEditCallback(int key) {
        switch(key) {
        case Keyboard.KEY_RETURN:
            if(onEditEnd(editField.getText())) {
                label.setVisible(true);
                editField.setVisible(false);
            }
            break;

        case Keyboard.KEY_ESCAPE:
            cancelEdit();
            break;
            
        default:
            editField.setErrorMessage(validateEdit(editField.getText()));
        }
    }
}
