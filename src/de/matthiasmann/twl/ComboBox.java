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

import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.Label.CallbackReason;
import de.matthiasmann.twl.model.ListModel;
import org.lwjgl.input.Keyboard;

/**
 * A drop down combobox. It creates a popup containing a Listbox.
 *
 * @author Matthias Mann
 */
public class ComboBox<T> extends ComboBoxBase {

    private static final int INVALID_WIDTH = -1;
    
    private final ComboboxLabel label;
    private final ListBox<T> listbox;

    private Runnable[] selectionChangedListeners;

    private ListModel.ChangeListener modelChangeListener;
    private boolean computeWidthFromModel;
    private int modelWidth = INVALID_WIDTH;
    
    public ComboBox(ListModel<T> model) {
        this();
        setModel(model);
    }
    
    public ComboBox() {
        this.label = new ComboboxLabel(getAnimationState());
        this.listbox = new ComboboxListbox<T>();
        
        label.addCallback(new CallbackWithReason<Label.CallbackReason>() {
            public void callback(CallbackReason reason) {
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
    }

    public void addCallback(Runnable cb) {
        selectionChangedListeners = CallbackSupport.addCallbackToList(selectionChangedListeners, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        selectionChangedListeners = CallbackSupport.removeCallbackFromList(selectionChangedListeners, cb);
    }

    private void doCallback() {
        CallbackSupport.fireCallbacks(selectionChangedListeners);
    }

    public void setModel(ListModel<T> model) {
        unregisterModelChangeListener();
        listbox.setModel(model);
        if(computeWidthFromModel) {
            registerModelChangeListener();
        }
    }

    public ListModel<T> getModel() {
        return listbox.getModel();
    }

    public void setSelected(int selected) {
        listbox.setSelected(selected);
        updateLabel();
    }

    public int getSelected() {
        return listbox.getSelected();
    }

    public boolean isComputeWidthFromModel() {
        return computeWidthFromModel;
    }

    public void setComputeWidthFromModel(boolean computeWidthFromModel) {
        if(this.computeWidthFromModel != computeWidthFromModel) {
            this.computeWidthFromModel = computeWidthFromModel;
            if(computeWidthFromModel) {
                registerModelChangeListener();
            } else {
                unregisterModelChangeListener();
            }
        }
    }

    private void registerModelChangeListener() {
        final ListModel<?> model = getModel();
        if(model != null) {
            modelWidth = INVALID_WIDTH;
            if(modelChangeListener == null) {
                modelChangeListener = new ModelChangeListener();
            }
            model.addChangeListener(modelChangeListener);
        }
    }

    private void unregisterModelChangeListener() {
        if(modelChangeListener != null) {
            final ListModel<T> model = getModel();
            if(model != null) {
                model.removeChangeListener(modelChangeListener);
            }
        }
    }

    @Override
    protected boolean openPopup() {
        if(super.openPopup()) {
            popup.validateLayout();
            listbox.scrollToSelected();
            return true;
        }
        return false;
    }
    
    protected void listBoxSelectionChanged(boolean close) {
        updateLabel();
        if(close) {
            popup.closePopup();
        }
        doCallback();
    }

    protected String getModelData(int idx) {
        return String.valueOf(getModel().getEntry(idx));
    }

    protected Widget getLabel() {
        return label;
    }

    protected void updateLabel() {
        int selected = getSelected();
        if(selected == ListBox.NO_SELECTION) {
            label.setText("");
        } else {
            label.setText(getModelData(selected));
        }
        if(!computeWidthFromModel) {
            invalidateLayoutTree();
        }
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        modelWidth = INVALID_WIDTH;
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

    void invalidateModelWidth() {
        if(computeWidthFromModel) {
            modelWidth = INVALID_WIDTH;
            invalidateLayoutTree();
        }
    }

    void updateModelWidth() {
        if(computeWidthFromModel) {
            modelWidth = 0;
            updateModelWidth(0, getModel().getNumEntries()-1);
        }
    }
    
    void updateModelWidth(int first, int last) {
        if(computeWidthFromModel) {
            int newModelWidth = modelWidth;
            for(int idx=first ; idx<=last ; idx++) {
                newModelWidth = Math.max(newModelWidth, computeEntryWidth(idx));
            }
            if(newModelWidth > modelWidth) {
                modelWidth = newModelWidth;
                invalidateLayoutTree();
            }
        }
    }

    protected int computeEntryWidth(int idx) {
        int width = label.getBorderHorizontal();
        Font font = label.getFont();
        if(font != null) {
            width += font.computeMultiLineTextWidth(getModelData(idx));
        }
        return width;
    }

    class ComboboxLabel extends Label {
        public ComboboxLabel(AnimationState animState) {
            super(animState);
            setAutoSize(false);
            setClip(true);
            setTheme("display");
        }

        @Override
        public int getPreferredInnerWidth() {
            if(computeWidthFromModel && getModel() != null) {
                if(modelWidth == INVALID_WIDTH) {
                    updateModelWidth();
                }
                return modelWidth;
            } else {
                return super.getPreferredInnerWidth();
            }
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

    class ModelChangeListener implements ListModel.ChangeListener {
        public void entriesInserted(int first, int last) {
            updateModelWidth(first, last);
        }
        public void entriesDeleted(int first, int last) {
            invalidateModelWidth();
        }
        public void entriesChanged(int first, int last) {
            invalidateModelWidth();
        }
        public void allChanged() {
            invalidateModelWidth();
        }
    }

    static class ComboboxListbox<T> extends ListBox<T> {
        public ComboboxListbox() {
            setTheme("listbox");
        }

        @Override
        protected ListBoxDisplay createDisplay() {
            return new ComboboxListboxLabel();
        }
    }

    static class ComboboxListboxLabel extends ListBox.ListBoxLabel {
        @Override
        protected boolean handleListBoxEvent(Event evt) {
            if(evt.getType() == Event.Type.MOUSE_CLICKED) {
                doListBoxCallback(ListBox.CallbackReason.MOUSE_CLICK);
                return true;
            }
            if(evt.getType() == Event.Type.MOUSE_BTNDOWN) {
                doListBoxCallback(ListBox.CallbackReason.SET_SELECTED);
                return true;
            }
            return false;
        }
    }
    
}
