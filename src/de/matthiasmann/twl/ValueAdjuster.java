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

import org.lwjgl.input.Keyboard;

/**
 * Base class for value adjuster widgets.
 * It has a value display/edit widget and 2 buttons.
 *
 * @author Matthias Mann
 */
public abstract class ValueAdjuster extends Widget {

    private final DraggableButton label;
    private final EditField editField;
    private final Button decButton;
    private final Button incButton;

    private int width;
    
    public ValueAdjuster() {
        this.label = new DraggableButton();
        this.editField = new EditField();
        this.decButton = new Button();
        this.incButton = new Button();
        
        label.setClip(true);
        label.setTheme("valueDisplay");
        editField.setTheme("valueEdit");
        decButton.setTheme("decButton");
        incButton.setTheme("incButton");
        
        decButton.addCallback(new Runnable() {
            public void run() {
                doDecrement();
            }
        });
        incButton.addCallback(new Runnable() {
            public void run() {
                doIncrement();
            }
        });
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
    }

    public void startEdit() {
        if(label.isVisible()) {
            editField.setErrorMessage(null);
            editField.setText(onEditStart());
            editField.setVisible(true);
            editField.requestKeyboardFocus();
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
                getBorderLeft() +
                decButton.getMinWidth() +
                Math.max(width, label.getMinWidth()) +
                incButton.getMinWidth() +
                getBorderRight());
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = label.getMinHeight();
        minHeight = Math.max(minHeight, decButton.getMinHeight());
        minHeight = Math.max(minHeight, incButton.getMinHeight());
        minHeight += getBorderTop() + getBorderBottom();
        return Math.max(minHeight, super.getMinHeight());
    }

    @Override
    public int getPreferedInnerWidth() {
        int prefWidth = super.getPreferedInnerWidth();
        prefWidth = Math.max(prefWidth,
                decButton.getPreferedWidth() +
                Math.max(width, label.getPreferedWidth()) +
                incButton.getPreferedWidth());
        return prefWidth;
    }

    @Override
    public int getPreferedInnerHeight() {
        int prefHeight = super.getPreferedInnerHeight();
        prefHeight = Math.max(prefHeight, decButton.getPreferedHeight());
        prefHeight = Math.max(prefHeight, label.getPreferedHeight());
        prefHeight = Math.max(prefHeight, incButton.getPreferedHeight());
        return prefHeight;
    }

    @Override
    protected void keyboardFocusLost() {
        cancelEdit();
    }

    @Override
    protected void keyboardFocusGained() {
        label.requestKeyboardFocus();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(!visible) {
            cancelEdit();
        }
    }

    @Override
    protected void layout() {
        int height = getInnerHeight();
        int y = getInnerY();
        decButton.setPosition(getInnerX(), y);
        decButton.setSize(decButton.getPreferedWidth(), height);
        incButton.setPosition(getInnerRight() - incButton.getPreferedWidth(), y);
        incButton.setSize(incButton.getPreferedWidth(), height);
        int labelX = decButton.getRight();
        int labelWidth = Math.max(0, incButton.getX() - labelX);
        label.setSize(labelWidth, height);
        label.setPosition(labelX, y);
        editField.setSize(labelWidth, height);
        editField.setPosition(labelX, y);
    }
    
    protected void setDisplayText(String text) {
        label.setText(text);
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
