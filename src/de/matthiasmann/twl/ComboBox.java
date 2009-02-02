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

import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.Label.CallbackReason;
import de.matthiasmann.twl.model.ListModel;
import org.lwjgl.input.Keyboard;

/**
 * A drop down combobox. It creates a popup containing a Listbox.
 *
 * @author Matthias Mann
 */
public class ComboBox extends Widget {

    private final ComboboxLabel label;
    private final Button button;
    private final PopupWindow popup;
    private final ListBox listbox;

    private Runnable[] selectionChangedListeners;
    
    public ComboBox(ListModel model) {
        this();
        setModel(model);
    }
    
    public ComboBox() {
        this.label = new ComboboxLabel(getAnimationState());
        this.button = new Button(getAnimationState());
        this.popup = new PopupWindow(this);
        this.listbox = new ListBox() {
            @Override
            protected ListBoxDisplay createDisplay() {
                return new ListBoxLabel() {
                    @Override
                    protected boolean handleListBoxEvent(Event evt) {
                        if(evt.getType() == Event.Type.MOUSE_CLICKED) {
                            doListBoxCallback(CallbackReason.MOUSE_CLICK);
                            return true;
                        }
                        if(evt.getType() == Event.Type.MOUSE_BTNDOWN) {
                            doListBoxCallback(CallbackReason.SET_SELECTED);
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
        
        label.addCallback(new CallbackWithReason<Label.CallbackReason>() {
            public void callback(CallbackReason reason) {
                openPopup();
            }
        });
        button.addCallback(new Runnable() {
            public void run() {
                openPopup();
            }
        });
        
        listbox.addCallback(new CallbackWithReason<ListBox.CallbackReason>() {
            public void callback(ListBox.CallbackReason reason) {
                switch (reason) {
                case KEYBOARD_RETURN:
                case MOUSE_CLICK:
                case MOUSE_DOUBLE_CLICK:
                    listBoxSelectionChanged(true);
                    break;
                default:
                    listBoxSelectionChanged(false);
                    break;
                }
            }
        });
        
        popup.setTheme("comboboxPopup");
        popup.add(listbox);
        
        add(label);
        add(button);
        setCanAcceptKeyboardFocus(true);
    }

    public void addCallback(Runnable cb) {
        selectionChangedListeners = CallbackSupport.addCallbackToList(selectionChangedListeners, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        selectionChangedListeners = CallbackSupport.removeCallbackFromList(selectionChangedListeners, cb, Runnable.class);
    }

    private void doCallback() {
        if(selectionChangedListeners != null) {
            for(Runnable cb : selectionChangedListeners) {
                cb.run();
            }
        }
    }

    public void setModel(ListModel model) {
        listbox.setModel(model);
    }

    public ListModel getModel() {
        return listbox.getModel();
    }

    public void setSelected(int selected) {
        listbox.setSelected(selected);
        updateLabel();
    }

    public int getSelected() {
        return listbox.getSelected();
    }
    
    protected void openPopup() {
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
            listbox.setSize(popup.getInnerWidth(), popup.getInnerHeight());
            listbox.scrollToSelected();
        }
    }
    
    protected void listBoxSelectionChanged(boolean close) {
        updateLabel();
        if(close) {
            popup.closePopup();
        }
        doCallback();
    }

    protected void updateLabel() {
        int selected = getSelected();
        if(selected == ListBox.NO_SELECTION) {
            label.setText("");
        } else {
            label.setText(getModel().getEntry(selected).toString());
        }
        invalidateParentLayout();
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(super.handleEvent(evt)) {
            return true;
        }
        if(evt.getType() == Event.Type.KEY_PRESSED) {
            switch (evt.getKeyCode()) {
            case Keyboard.KEY_UP:
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_HOME:
            case Keyboard.KEY_END:
                // let the listbox handle this :)
                listbox.handleEvent(evt);
                return true;
            case Keyboard.KEY_SPACE:
            case Keyboard.KEY_RETURN:
                openPopup();
                return true;
            }
        }
        return false;
    }

    @Override
    public int getPreferredInnerWidth() {
        return label.getPreferredWidth() + button.getPreferredWidth();
    }

    @Override
    public int getPreferredInnerHeight() {
        return Math.max(label.getPreferredHeight(), button.getPreferredHeight());
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        minWidth = Math.max(minWidth, label.getMinWidth() + button.getMinWidth());
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = super.getMinHeight();
        minHeight = Math.max(minHeight, label.getMinHeight());
        minHeight = Math.max(minHeight, button.getMinHeight());
        return minHeight;
    }

    @Override
    protected void layout() {
        int btnWidth = button.getPreferredWidth();
        int innerHeight = getInnerHeight();
        button.setPosition(getInnerRight() - btnWidth, getInnerY());
        button.setSize(btnWidth, innerHeight);
        label.setSize(Math.max(0, button.getX() - getInnerX()), innerHeight);
    }
    
    static class ComboboxLabel extends Label {
        public ComboboxLabel(AnimationState animState) {
            super(animState);
            setAutoSize(false);
            setClip(true);
            setTheme("display");
        }

        @Override
        public int getPreferredInnerHeight() {
            int prefHeight = super.getPreferredInnerHeight();
            if(getFont() != null) {
                prefHeight = Math.max(prefHeight, getFont().getLineHeight());
            }
            return prefHeight;
        }
    }
}
