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

import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.ListModel.ChangeListener;
import org.lwjgl.input.Keyboard;

/**
 * A list box. Supports single and multiple columns.
 *
 * @author Matthias Mann
 */
public class ListBox extends Widget {

    public static final int NO_SELECTION = -1;
    public static final int DEFAULT_CELL_HEIGHT = 20;
    public static final int SINGLE_COLUMN = -1;
    
    public enum CallbackReason {
        MODEL_CHANGED(false),
        SET_SELECTED(false),
        MOUSE_CLICK(false),
        MOUSE_DOUBLE_CLICK(true),
        KEYBOARD(false),
        KEYBOARD_RETURN(true);
        
        final boolean forceCallback;
        private CallbackReason(boolean forceCallback) {
            this.forceCallback = forceCallback;
        }
        
        public boolean actionRequested() {
            return forceCallback;
        }
    };
    
    private static final ListBoxDisplay EMPTY_LABELS[] = {};
    
    private final ChangeListener modelCallback;
    private final Scrollbar scrollbar;
    private ListBoxDisplay[] labels;
    private ListModel model;
    private int cellHeight = DEFAULT_CELL_HEIGHT;
    private int cellWidth = SINGLE_COLUMN;
    private boolean rowMajor = true;
    private boolean fixedCellWidth;
    private boolean fixedCellHeight;

    private int numCols = 1;
    private int firstVisible;
    private int selected = NO_SELECTION;
    private boolean needUpdate;
    private CallbackWithReason[] callbacks;
    
    public ListBox() {
        modelCallback = new ChangeListenerImpl();
        scrollbar = new Scrollbar();
        labels = EMPTY_LABELS;
        
        scrollbar.addCallback(new Runnable() {
            public void run() {
                setFirstVisible(scrollbar.getValue() * numCols);
            }
        });
        
        add(scrollbar);
        
        setSize(200, 300);
        setCanAcceptKeyboardFocus(true);
        setDepthFocusTraversal(false);
    }

    public ListBox(ListModel model) {
        this();
        setModel(model);
    }

    public ListModel getModel() {
        return model;
    }

    public void setModel(ListModel model) {
        if(this.model != model) {
            if(this.model != null) {
                this.model.removeChangeListener(modelCallback);
            }
            this.model = model;
            if(model != null) {
                model.addChangeListener(modelCallback);
            }
            modelCallback.allChanged();
        }
    }

    public void addCallback(CallbackWithReason<CallbackReason> cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, CallbackWithReason.class);
    }

    public void removeCallback(CallbackWithReason<CallbackReason> cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb, CallbackWithReason.class);
    }

    @SuppressWarnings("unchecked")
    private void doCallback(CallbackReason reason) {
        if(callbacks != null) {
            for(CallbackWithReason cb : callbacks) {
                ((CallbackWithReason<CallbackReason>)cb).callback(reason);
            }
        }
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public void setCellHeight(int cellHeight) {
        if(cellHeight < 1) {
            throw new IllegalArgumentException("cellHeight < 1");
        }
        this.cellHeight = cellHeight;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public void setCellWidth(int cellWidth) {
        if(cellWidth < 1 && cellWidth != SINGLE_COLUMN) {
            throw new IllegalArgumentException("cellWidth < 1");
        }
        this.cellWidth = cellWidth;
    }

    public boolean isFixedCellHeight() {
        return fixedCellHeight;
    }

    public void setFixedCellHeight(boolean fixedCellHeight) {
        this.fixedCellHeight = fixedCellHeight;
    }

    public boolean isFixedCellWidth() {
        return fixedCellWidth;
    }

    public void setFixedCellWidth(boolean fixedCellWidth) {
        this.fixedCellWidth = fixedCellWidth;
    }

    public boolean isRowMajor() {
        return rowMajor;
    }

    public void setRowMajor(boolean rowMajor) {
        this.rowMajor = rowMajor;
    }

    public int getFirstVisible() {
        return firstVisible;
    }

    public int getLastVisible() {
        return getFirstVisible() + labels.length - 1;
    }
    
    public void setFirstVisible(int firstVisible) {
        firstVisible = Math.max(0, Math.min(firstVisible, getNumEntries() - 1));
        if(this.firstVisible != firstVisible) {
            this.firstVisible = firstVisible;
            scrollbar.setValue(firstVisible / numCols);
            needUpdate = true;
        }
    }

    public int getSelected() {
        return selected;
    }
    
    public void setSelected(int selected) {
        setSelected(selected, true, CallbackReason.SET_SELECTED);
    }
    
    public void setSelected(int selected, boolean scroll) {
        setSelected(selected, scroll, CallbackReason.SET_SELECTED);
    }
    
    void setSelected(int selected, boolean scroll, CallbackReason reason) {
        if(selected < NO_SELECTION || selected >= getNumEntries()) {
            throw new IllegalArgumentException();
        }
        if(scroll) {
            validateLayout();
            if(selected == NO_SELECTION) {
                setFirstVisible(0);
            } else {
                int delta = getFirstVisible() - selected;
                if(delta > 0) {
                    int deltaRows = (delta + numCols - 1) / numCols;
                    setFirstVisible(getFirstVisible() - deltaRows * numCols);
                } else {
                    delta = selected - getLastVisible();
                    if(delta > 0) {
                        int deltaRows = (delta + numCols - 1) / numCols;
                        setFirstVisible(getFirstVisible() + deltaRows * numCols);
                    }
                }
            }
        }
        if(this.selected != selected) {
            this.selected = selected;
            needUpdate = true;
            doCallback(reason);
        } else if(reason.actionRequested() || reason == CallbackReason.MOUSE_CLICK) {
            doCallback(reason);
        }
    }

    public void scrollToSelected() {
        setSelected(selected, true, CallbackReason.SET_SELECTED);
    }

    public int getNumEntries() {
        if(model != null) {
            return model.getNumEntries();
        }
        return 0;
    }
    
    public int getNumRows() {
        return (getNumEntries() + numCols - 1) / numCols;
    }
    
    public int findEntryByName(String prefix) {
        int numEntries = getNumEntries();
        for(int i=selected+1 ; i<numEntries ; i++) {
            if(model.matchPrefix(i, prefix)) {
                return i;
            }
        }
        for(int i=0 ; i<selected ; i++) {
            if(model.matchPrefix(i, prefix)) {
                return i;
            }
        }
        return NO_SELECTION;
    }

    /**
     * The method always return this.
     * Use getEntryAt(x, y) to locate the listbox entry at the specific coordinates.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @return this.
     */
    @Override
    public Widget getWidgetAt(int x, int y) {
        return this;
    }

    /**
     * Returns the entry at the specific coordinates or -1 if there is no entry.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the index of the entry or -1.
     */
    public int getEntryAt(int x, int y) {
        int n = Math.max(labels.length, getNumEntries() - firstVisible);
        for(int i=0 ; i<n ; i++) {
            if(labels[i].getWidget().isInside(x, y)) {
                return firstVisible + i;
            }
        }
        return -1;
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        setCellHeight(themeInfo.getParameter("cellHeight", DEFAULT_CELL_HEIGHT));
        setCellWidth(themeInfo.getParameter("cellWidth", SINGLE_COLUMN));
        setRowMajor(themeInfo.getParameter("rowMajor", true));
        setFixedCellWidth(themeInfo.getParameter("fixedCellWidth", false));
        setFixedCellHeight(themeInfo.getParameter("fixedCellHeight", false));
    }

    protected void goKeyboard(int dir) {
        int newPos = selected + dir;
        if(newPos >= 0 && newPos < getNumEntries()) {
            setSelected(newPos, true, CallbackReason.KEYBOARD);
        }
    }
    
    protected boolean isSearchChar(char ch) {
        return (ch != Keyboard.CHAR_NONE) && Character.isLetterOrDigit(ch);
    }

    @Override
    protected void keyboardFocusGained() {
        setLabelFocused(true);
    }

    @Override
    protected void keyboardFocusLost() {
        setLabelFocused(false);
    }

    private void setLabelFocused(boolean focused) {
        int idx = selected - firstVisible;
        if(idx >= 0 && idx < labels.length) {
            labels[idx].setFocused(focused);
        }
    }

    @Override
    public boolean handleEvent(Event evt) {
        switch (evt.getType()) {
        case MOUSE_WHEEL:
            scrollbar.scroll(-evt.getMouseWheelDelta());
            return true;
        case KEY_PRESSED:
            switch (evt.getKeyCode()) {
            case Keyboard.KEY_UP:
                goKeyboard(-numCols);
                break;
            case Keyboard.KEY_DOWN:
                goKeyboard(numCols);
                break;
            case Keyboard.KEY_LEFT:
                goKeyboard(-1);
                break;
            case Keyboard.KEY_RIGHT:
                goKeyboard(1);
                break;
            case Keyboard.KEY_PRIOR:
                if(getNumEntries() > 0) {
                    setSelected(Math.max(0, selected-labels.length),
                        true, CallbackReason.KEYBOARD);
                }
                break;
            case Keyboard.KEY_NEXT:
                setSelected(Math.min(getNumEntries()-1, selected+labels.length),
                        true, CallbackReason.KEYBOARD);
                break;
            case Keyboard.KEY_HOME:
                if(getNumEntries() > 0) {
                    setSelected(0, true, CallbackReason.KEYBOARD);
                }
                break;
            case Keyboard.KEY_END:
                setSelected(getNumEntries()-1, true, CallbackReason.KEYBOARD);
                break;
            case Keyboard.KEY_RETURN:
                setSelected(selected, false, CallbackReason.KEYBOARD_RETURN);
                break;
            default:
                if(evt.hasKeyChar() && isSearchChar(evt.getKeyChar())) {
                    int idx = findEntryByName(Character.toString(evt.getKeyChar()));
                    if(idx != NO_SELECTION) {
                        setSelected(idx, true, CallbackReason.KEYBOARD);
                    }
                    return true;
                }
                break;
            }
            return true;
        case KEY_RELEASED:
            return true;
        }
        // delegate to children (listbox, displays, etc...)
        if(super.handleEvent(evt)) {
            return true;
        }
        // eat all mouse events
        return evt.isMouseEvent();
    }

    @Override
    protected void childChangedSize(Widget child) {
        if(!(child instanceof ListBoxDisplay)) {
            super.childChangedSize(child);
        }
    }

    @Override
    public int getMinWidth() {
        return Math.max(super.getMinWidth(), scrollbar.getMinWidth());
    }

    @Override
    public int getMinHeight() {
        return Math.max(super.getMinHeight(), scrollbar.getMinHeight());
    }

    @Override
    public int getPreferredInnerWidth() {
        return Math.max(super.getPreferredInnerWidth(), scrollbar.getPreferredWidth());
    }

    @Override
    public int getPreferredInnerHeight() {
        return Math.max(getNumRows() * getCellHeight(), scrollbar.getPreferredHeight());
    }

    @Override
    protected void paint(GUI gui) {
        if(needUpdate) {
            updateDisplay();
        }
        // always update  scrollbar
        int maxFirstVisibleRow = Math.max(0, getNumEntries() - labels.length);
        scrollbar.setMinMaxValue(0, maxFirstVisibleRow);
        scrollbar.setValue(firstVisible / numCols);

        super.paint(gui);
    }

    private void updateDisplay() {
        needUpdate = false;
        
        int numEntries = getNumEntries();
        if(selected >= numEntries) {
            selected = NO_SELECTION;
        }
        
        int maxFirstVisibleRow = Math.max(0, numEntries - labels.length);
        maxFirstVisibleRow = (maxFirstVisibleRow + numCols + 1) / numCols;
        int maxFirstVisible = maxFirstVisibleRow * numCols;
        if(firstVisible > maxFirstVisible) {
            firstVisible = Math.max(0, maxFirstVisible);
        }

        boolean hasFocus = hasKeyboardFocus();

        for(int i=0 ; i<labels.length ; i++) {
            ListBoxDisplay label = labels[i];
            int cell = i + firstVisible;
            if(cell < numEntries) {
                label.setData(model.getEntry(cell));
                label.setTooltipContent(model.getEntryTooltip(cell));
            } else {
                label.setData(null);
                label.setTooltipContent(null);
            }
            label.setSelected(cell == selected);
            label.setFocused(cell == selected && hasFocus);
        }
    }

    @Override
    protected void layout() {
        scrollbar.setSize(scrollbar.getPreferredWidth(), getInnerHeight());
        scrollbar.setPosition(getInnerRight() - scrollbar.getWidth(), getInnerY());
        
        int numRows = Math.max(1, getInnerHeight() / cellHeight);
        if(cellWidth != SINGLE_COLUMN) {
            numCols = Math.max(1, (scrollbar.getX() - getInnerX()) / cellWidth);
        } else {
            numCols = 1;
        }
        setVisibleCells(numRows);
        
        needUpdate = true;
    }

    private void setVisibleCells(int numRows) {
        int visibleCells = numRows * numCols;
        assert visibleCells >= 1;
        
        scrollbar.setPageSize(visibleCells);
        
        int curVisible = labels.length;
        for(int i = visibleCells; i < curVisible; i++) {
            removeChild(labels[i].getWidget());
        }

        ListBoxDisplay[] newLabels = new ListBoxDisplay[visibleCells];
        System.arraycopy(labels, 0, newLabels, 0, Math.min(visibleCells, labels.length));
        labels = newLabels;
        
        for(int i = curVisible; i < visibleCells; i++) {
            final int cellOffset = i;
            ListBoxDisplay lbd = createDisplay();
            lbd.addListBoxCallback(new CallbackWithReason<CallbackReason>() {
                public void callback(CallbackReason reason) {
                    int cell = getFirstVisible() + cellOffset;
                    if(cell < getNumEntries()) {
                        setSelected(cell, false, reason);
                    }
                }
            });
            add(lbd.getWidget());
            labels[i] = lbd;
        }
        
        int innerWidth = scrollbar.getX() - getInnerX();
        int innerHeight = getInnerHeight();
        for(int i=0 ; i<visibleCells ; i++) {
            int row, col;
            if(rowMajor) {
                row = i / numCols;
                col = i % numCols;
            } else {
                row = i % numRows;
                col = i / numRows;
            }
            int x, y, w, h;
            if(fixedCellHeight) {
                y = row * cellHeight;
                h = cellHeight;
            } else {
                y = row * innerHeight / numRows;
                h = (row+1) * innerHeight / numRows - y;
            }
            if(fixedCellWidth && cellWidth != SINGLE_COLUMN) {
                x = col * cellWidth;
                w = cellWidth;
            } else {
                x = col * innerWidth / numCols;
                w = (col+1) * innerWidth / numCols - x;
            }
            Widget cell = (Widget)labels[i];
            cell.setSize(Math.max(0, w), Math.max(0, h));
            cell.setPosition(x + getInnerX(), y + getInnerY());
        }
    }
    
    protected ListBoxDisplay createDisplay() {
        return new ListBoxLabel();
    }
    
    protected static class ListBoxLabel extends TextWidget implements ListBoxDisplay {
        public static final String STATE_SELECTED = "selected";

        private boolean selected;
        private CallbackWithReason[] callbacks;

        public ListBoxLabel() {
            setClip(true);
            setTheme("display");
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            if(this.selected != selected) {
                this.selected = selected;
                getAnimationState().setAnimationState(STATE_SELECTED, selected);
            }
        }

        public boolean isFocused() {
            return getAnimationState().getAnimationState(STATE_KEYBOARD_FOCUS);
        }

        public void setFocused(boolean focused) {
            getAnimationState().setAnimationState(STATE_KEYBOARD_FOCUS, focused);
        }

        public void setData(Object data) {
            setText((data == null) ? "" : data.toString());
        }

        public Widget getWidget() {
            return this;
        }

        public void addListBoxCallback(CallbackWithReason<ListBox.CallbackReason> cb) {
            callbacks = CallbackSupport.addCallbackToList(callbacks, cb, CallbackWithReason.class);
        }

        public void removeListBoxCallback(CallbackWithReason<ListBox.CallbackReason> cb) {
            callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb, CallbackWithReason.class);
        }

        @SuppressWarnings("unchecked")
        protected void doListBoxCallback(ListBox.CallbackReason reason) {
            if(callbacks != null) {
                for(CallbackWithReason cb : callbacks) {
                    ((CallbackWithReason<ListBox.CallbackReason>)cb).callback(reason);
                }
            }
        }

        protected boolean handleListBoxEvent(Event evt) {
            switch(evt.getType()) {
            case MOUSE_BTNDOWN:
                if(!selected) {
                    doListBoxCallback(CallbackReason.MOUSE_CLICK);
                }
                return true;
            case MOUSE_CLICKED:
                if(selected && evt.getMouseClickCount() > 1) {
                    doListBoxCallback(CallbackReason.MOUSE_DOUBLE_CLICK);
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean handleEvent(Event evt) {
            if(!evt.isMouseDragEvent()) {
                if(handleListBoxEvent(evt)) {
                    return true;
                }
            }
            if(super.handleEvent(evt)) {
                return true;
            }
            return evt.isMouseEvent();
        }

    }

    private class ChangeListenerImpl implements ChangeListener {
        public void entriesInserted(int first, int last) {
            int delta = last - first + 1;
            int fv = getFirstVisible();
            if(fv >= first) {
                fv += delta;
                setFirstVisible(fv);
            }
            int s = getSelected();
            if(s >= first) {
                setSelected(s + delta, false, CallbackReason.MODEL_CHANGED);
            }
            if(first <= getLastVisible() && last >= fv) {
                needUpdate = true;
            }
        }

        public void entriesDeleted(int first, int last) {
            int delta = last - first + 1;
            int fv = getFirstVisible();
            int lv = getLastVisible();
            if(fv > last) {
                setFirstVisible(fv - delta);
            } else if(fv <= last && lv >= first) {
                setFirstVisible(first);
            }
            int s = getSelected();
            if(s > last) {
                setSelected(s - delta, false, CallbackReason.MODEL_CHANGED);
            } else if(s >= first && s <= last) {
                setSelected(NO_SELECTION, false, CallbackReason.MODEL_CHANGED);
            }
        }

        public void entriesChanged(int first, int last) {
            int fv = getFirstVisible();
            int lv = getLastVisible();
            if(fv <= last && lv >= first) {
                needUpdate = true;
            }
        }

        public void allChanged() {
            setSelected(NO_SELECTION, false, CallbackReason.MODEL_CHANGED);
            setFirstVisible(0);
            needUpdate = true;
        }
    }
}
