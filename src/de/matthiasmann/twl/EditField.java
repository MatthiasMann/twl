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

import de.matthiasmann.twl.model.StringModel;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.Image;
import org.lwjgl.input.Keyboard;

/**
 * A simple one line edit field
 * 
 * @author Matthias Mann
 */
public class EditField extends Widget {

    public static final String STATE_ERROR = "error";
    public static final String STATE_READONLY = "readonly";
    public static final String STATE_HOVER = "hover";
    
    public interface Callback {
        /**
         * Gets called for any change in the edit field, or when ESCAPE or RETURN was pressed
         *
         * @param key One of KEY_NONE, KEY_ESCAPE, KEY_RETURN
         * @see Keyboard#KEY_NONE
         * @see Keyboard#KEY_ESCAPE
         * @see Keyboard#KEY_RETURN
         */
        public void callback(int key);
    }

    private final StringBuilder editBuffer;
    private final TextRenderer textRenderer;
    private PasswordMasker passwordMasking;
    private Runnable modelChangeListener;
    private StringModel model;
    private boolean readOnly;

    private int cursorPos;
    private int scrollPos;
    private int selectionStart;
    private int selectionEnd;
    private int maxTextLength = Short.MAX_VALUE;

    private int columns = 5;
    private Image cursorImage;
    private Image selectionImage;
    private char passwordChar;
    private Object errorMsg;
    private Callback[] callbacks;
    private Menu popupMenu;
    private boolean textLongerThenWidget;

    private InfoWindow autoCompletionWindow;
    private boolean autoCompletionWantKeys;
    private int autoCompletionHeight = 100;

    private InfoWindow errorInfoWindow;
    private Label errorInfoLabel;

    /**
     * Creates a new EditField with an optional parent animation state.
     *
     * Unlike other widgets which use the passed animation state directly,
     * the EditField always creates it's animation state with the passed
     * one as parent.
     *
     * @param parentAnimationState
     * @see AnimationState#AnimationState(de.matthiasmann.twl.AnimationState) 
     */
    public EditField(AnimationState parentAnimationState) {
        super(parentAnimationState, true);
        
        this.editBuffer = new StringBuilder();
        this.textRenderer = new TextRenderer(getAnimationState());
        this.passwordChar = '*';

        textRenderer.setTheme("renderer");
        textRenderer.setClip(true);
        
        add(textRenderer);
        setCanAcceptKeyboardFocus(true);
        setDepthFocusTraversal(false);

        addActionMapping("cut", "cutToClipboard");
        addActionMapping("copy", "copyToClipboard");
        addActionMapping("paste", "pasteFromClipboard");
        addActionMapping("selectAll", "selectAll");
    }

    public EditField() {
        this(null);
    }

    public void addCallback(Callback cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Callback.class);
    }

    public void removeCallback(Callback cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    protected void doCallback(int key) {
        if(callbacks != null) {
            for(Callback cb : callbacks) {
                cb.callback(key);
            }
        }
    }

    public boolean isPasswordMasking() {
        return passwordMasking != null;
    }

    public void setPasswordMasking(boolean passwordMasking) {
        if(passwordMasking != isPasswordMasking()) {
            if(passwordMasking) {
                this.passwordMasking = new PasswordMasker(editBuffer, passwordChar);
            } else {
                this.passwordMasking = null;
            }
            updateText();
        }
    }

    public char getPasswordChar() {
        return passwordChar;
    }

    public void setPasswordChar(char passwordChar) {
        this.passwordChar = passwordChar;
        if(passwordMasking != null) {
            passwordMasking = new PasswordMasker(editBuffer, passwordChar);
            updateText();
        }
    }

    public int getColumns() {
        return columns;
    }

    /**
     * This is used to determine the desired width of the EditField based on
     * it's font and the character 'X'
     * 
     * @param columns number of characters
     * @throws IllegalArgumentException if columns < 0
     */
    public void setColumns(int columns) {
        if(columns < 0) {
            throw new IllegalArgumentException("columns");
        }
        this.columns = columns;
    }

    public StringModel getModel() {
        return model;
    }

    public void setModel(StringModel model) {
        if(this.model != null) {
            this.model.removeCallback(modelChangeListener);
        }
        this.model = model;
        if(this.model != null) {
            if(modelChangeListener == null) {
                modelChangeListener = new ModelChangeListener();
            }
            this.model.addCallback(modelChangeListener);
            modelChanged();
        }
    }

    public void setText(String text) {
        text = TextUtil.limitStringLength(text, maxTextLength);
        editBuffer.replace(0, editBuffer.length(), text);
        cursorPos = editBuffer.length();
        selectionStart = 0;
        selectionEnd = 0;
        updateText();
        scrollToCursor(true);
    }

    public String getText() {
        return editBuffer.toString();
    }
    
    public String getSelectedText() {
        return editBuffer.substring(selectionStart, selectionEnd);
    }

    public boolean hasSelection() {
        return selectionStart != selectionEnd;
    }

    public int getCursorPos() {
        return cursorPos;
    }

    public int getTextLength() {
        return editBuffer.length();
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        getAnimationState().setAnimationState(STATE_READONLY, readOnly);
    }

    public void insertText(String str) {
        if(!readOnly) {
            boolean update = false;
            if(hasSelection()) {
                deleteSelection();
                update = true;
            }
            int insertLength = Math.min(str.length(), maxTextLength - editBuffer.length());
            if(insertLength > 0) {
                editBuffer.insert(cursorPos, str, 0, insertLength);
                cursorPos += insertLength;
                update = true;
            }
            if(update) {
                updateText();
            }
        }
    }

    public void pasteFromClipboard() {
        String cbText = Clipboard.getClipboard();
        if(cbText != null) {
            cbText = TextUtil.stripNewLines(cbText);
            insertText(cbText);
        }
    }

    public void copyToClipboard() {
        String text;
        if(hasSelection()) {
            text = getSelectedText();
        } else {
            text = getText();
        }
        if(isPasswordMasking()) {
            text = TextUtil.createString(passwordChar, text.length());
        }
        Clipboard.setClipboard(text);
    }

    public void cutToClipboard() {
        String text;
        if(hasSelection()) {
            text = getSelectedText();
            if(!readOnly) {
                deleteSelection();
                updateText();
            }
        } else {
            text = getText();
            if(!readOnly) {
                setText("");
            }
        }
        if(isPasswordMasking()) {
            text = TextUtil.createString(passwordChar, text.length());
        }
        Clipboard.setClipboard(text);
    }

    public int getMaxTextLength() {
        return maxTextLength;
    }

    public void setMaxTextLength(int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applayThemeEditField(themeInfo);
    }

    protected void applayThemeEditField(ThemeInfo themeInfo) {
        cursorImage = themeInfo.getImage("cursor");
        selectionImage = themeInfo.getImage("selection");
        autoCompletionHeight = themeInfo.getParameter("autocompletion-height", 100);
        columns = themeInfo.getParameter("columns", 5);
        setPasswordChar((char)themeInfo.getParameter("passwordChar", '*'));
    }

    @Override
    protected void layout() {
        layoutChildFullInnerArea(textRenderer);
        checkTextWidth();
        if(autoCompletionWindow != null) {
            layoutAutocompletionWindow();
        }
        if(errorInfoWindow != null) {
            layoutErrorInfoWindow();
        }
    }

    private void layoutAutocompletionWindow() {
        autoCompletionWindow.setPosition(getX(), getBottom());
        autoCompletionWindow.setSize(getWidth(), autoCompletionHeight);
    }

    private int computeInnerWidth() {
        if(columns > 0) {
            Font font = textRenderer.getFont();
            if(font != null) {
                return font.computeTextWidth("X")*columns;
            }
        }
        return 0;
    }

    private int computeInnerHeight() {
        Font font = textRenderer.getFont();
        if(font != null) {
            return font.getLineHeight();
        }
        return 0;
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        minWidth = Math.max(minWidth, computeInnerWidth() + getBorderHorizontal());
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = super.getMinHeight();
        minHeight = Math.max(minHeight, computeInnerHeight() + getBorderVertical());
        return minHeight;
    }

    @Override
    public int getPreferredInnerWidth() {
        return computeInnerWidth();
    }

    @Override
    public int getPreferredInnerHeight() {
        return computeInnerHeight();
    }

    public void setErrorMessage(Object errorMsg) {
        getAnimationState().setAnimationState(STATE_ERROR, errorMsg != null);
        if(this.errorMsg != errorMsg) {
            this.errorMsg = errorMsg;
            GUI gui = getGUI();
            if(gui != null) {
                gui.requestToolTipUpdate(this);
            }
        }
        if(errorMsg != null) {
            if(hasKeyboardFocus()) {
                openErrorInfoWindow();
            }
        } else if(errorInfoWindow != null) {
            errorInfoWindow.closeInfo();
        }
    }

    @Override
    public Object getTooltipContent() {
        if(errorMsg != null) {
            return errorMsg;
        }
        Object tooltip = super.getTooltipContent();
        if(tooltip == null && !isPasswordMasking() && textLongerThenWidget) {
            tooltip = getText();
        }
        return tooltip;
    }

    public void setAutoCompletionWindow(InfoWindow window, boolean wantKeys) {
        if(window == null) {
            if(autoCompletionWindow != null) {
                autoCompletionWindow.closeInfo();
                autoCompletionWindow = null;
            }
            autoCompletionWantKeys = false;
        } else {
            autoCompletionWindow = window;
            autoCompletionWantKeys = wantKeys;
            if(autoCompletionWindow.openInfo()) {
                layoutAutocompletionWindow();
            }
        }
    }

    @Override
    public boolean handleEvent(Event evt) {
        boolean selectPressed = (evt.getModifiers() & Event.MODIFIER_SHIFT) != 0;

        if(evt.isMouseEvent()) {
            boolean hover = (evt.getType() != Event.Type.MOUSE_EXITED) && isMouseInside(evt);
            getAnimationState().setAnimationState(STATE_HOVER, hover);
        }

        if(evt.isMouseDragEvent()) {
            if(evt.getType() == Event.Type.MOUSE_DRAGED &&
                    (evt.getModifiers() & Event.MODIFIER_LBUTTON) != 0) {
                int newPos = textRenderer.getCursorPosFromMouse(evt.getMouseX());
                setCursorPos(newPos, true);
            }
            return true;
        }

        if(super.handleEvent(evt)) {
            return true;
        }

        if(evt.isKeyEvent() && autoCompletionWantKeys) {
            if(autoCompletionWindow.handleEvent(evt)) {
                return true;
            }
        }
        
        switch (evt.getType()) {
        case KEY_PRESSED:
            switch (evt.getKeyCode()) {
            case Keyboard.KEY_BACK:
                deletePrev();
                return true;
            case Keyboard.KEY_DELETE:
                deleteNext();
                return true;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_ESCAPE:
                doCallback(evt.getKeyCode());
                return true;
            case Keyboard.KEY_HOME:
                setCursorPos(0, selectPressed);
                return true;
            case Keyboard.KEY_END:
                setCursorPos(editBuffer.length(), selectPressed);
                return true;
            case Keyboard.KEY_LEFT:
                moveCursor(-1, selectPressed);
                return true;
            case Keyboard.KEY_RIGHT:
                moveCursor(+1, selectPressed);
                return true;
            case Keyboard.KEY_UP:
            case Keyboard.KEY_DOWN:
                if(!autoCompletionWantKeys && autoCompletionWindow != null) {
                    return autoCompletionWindow.handleEvent(evt);
                }
                return false;
            default:
                // the only allowed modifier is shift
                if(evt.hasKeyChar() && ((evt.getModifiers() & ~Event.MODIFIER_SHIFT) == 0)) {
                    insertChar(evt.getKeyChar());
                    return true;
                }
                return false;
            }

        case KEY_RELEASED:
            switch (evt.getKeyCode()) {
            case Keyboard.KEY_BACK:
            case Keyboard.KEY_DELETE:
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_ESCAPE:
            case Keyboard.KEY_HOME:
            case Keyboard.KEY_END:
            case Keyboard.KEY_LEFT:
            case Keyboard.KEY_RIGHT:
                return true;
            case Keyboard.KEY_UP:
            case Keyboard.KEY_DOWN:
                if(!autoCompletionWantKeys && autoCompletionWindow != null) {
                    return autoCompletionWindow.handleEvent(evt);
                }
                return false;
            default:
                return evt.hasKeyChar() && ((evt.getModifiers() & ~Event.MODIFIER_SHIFT) == 0);
            }

        case MOUSE_BTNUP:
            if(evt.getMouseButton() == Event.MOUSE_RBUTTON && isMouseInside(evt)) {
                showPopupMenu(evt);
                return true;
            }
            break;

        case MOUSE_BTNDOWN:
            if(evt.getMouseButton() == Event.MOUSE_LBUTTON && isMouseInside(evt)) {
                int newPos = textRenderer.getCursorPosFromMouse(evt.getMouseX());
                setCursorPos(newPos, selectPressed);
                return true;
            }
            break;

        case MOUSE_CLICKED:
            if(evt.getMouseClickCount() == 2) {
                int newPos = textRenderer.getCursorPosFromMouse(evt.getMouseX());
                selectWordFromMouse(newPos);
                return true;
            }
            if(evt.getMouseClickCount() == 3) {
                selectAll();
                return true;
            }
            break;

        case MOUSE_WHEEL:
            return false;
        }

        return evt.isMouseEvent();
    }

    protected void showPopupMenu(Event evt) {
        if(popupMenu == null) {
            popupMenu = createPopupMenu();
        }
        if(popupMenu != null) {
            popupMenu.openPopupMenu(this, evt.getMouseX(), evt.getMouseY());
        }
    }

    protected Menu createPopupMenu() {
        Menu menu = new Menu();
        menu.add("cut", new Runnable() {
            public void run() {
                cutToClipboard();
            }
        });
        menu.add("copy", new Runnable() {
            public void run() {
                copyToClipboard();
            }
        });
        menu.add("paste", new Runnable() {
            public void run() {
                pasteFromClipboard();
            }
        });
        menu.addSpacer();
        menu.add("clear", new Runnable() {
            public void run() {
                if(!isReadOnly()) {
                    setText("");
                }
            }
        });
        return menu;
    }

    private void updateText() {
        if(model != null) {
            model.setValue(getText());
        }
        textRenderer.setCharSequence(passwordMasking != null ? passwordMasking : editBuffer);
        checkTextWidth();
        scrollToCursor(false);
        doCallback(Keyboard.KEY_NONE);
    }

    private void checkTextWidth() {
        textLongerThenWidget = textRenderer.getPreferredWidth() > textRenderer.getWidth();
    }

    protected void moveCursor(int dir, boolean select) {
        setCursorPos(cursorPos + dir, select);
    }

    protected void setCursorPos(int pos, boolean select) {
        pos = Math.max(0, Math.min(editBuffer.length(), pos));
        if(!select) {
            selectionStart = pos;
            selectionEnd = pos;
        }
        if(this.cursorPos != pos) {
            if(select) {
                if(hasSelection()) {
                    if(cursorPos == selectionStart) {
                        selectionStart = pos;
                    } else {
                        selectionEnd = pos;
                    }
                } else {
                    selectionStart = cursorPos;
                    selectionEnd = pos;
                }
                if(selectionStart > selectionEnd) {
                    int t = selectionStart;
                    selectionStart = selectionEnd;
                    selectionEnd = t;
                }
            }

            this.cursorPos = pos;
            scrollToCursor(false);
        }
    }

    public void selectAll() {
        selectionStart = 0;
        selectionEnd = editBuffer.length();
    }

    protected void selectWordFromMouse(int index) {
        selectionStart = index;
        selectionEnd = index;
        while(selectionStart > 0 && !Character.isWhitespace(editBuffer.charAt(selectionStart-1))) {
            selectionStart--;
        }
        while(selectionEnd < editBuffer.length() && !Character.isWhitespace(editBuffer.charAt(selectionEnd))) {
            selectionEnd++;
        }
    }

    protected void scrollToCursor(boolean force) {
        int xpos = textRenderer.computeRelativeCursorPositionX(cursorPos);
        int renderWidth = textRenderer.getWidth() - 5;
        if(xpos < scrollPos + 5) {
            scrollPos = Math.max(0, xpos - 5);
        } else if(force || xpos - scrollPos > renderWidth) {
            scrollPos = Math.max(0, xpos - renderWidth);
        }
    }
    
    protected void insertChar(char ch) {
        // don't add control characters
        if(!readOnly && !Character.isISOControl(ch)) {
            boolean update = false;
            if(hasSelection()) {
                deleteSelection();
                update = true;
            }
            if(editBuffer.length() < maxTextLength) {
                editBuffer.insert(cursorPos, ch);
                cursorPos++;
                update = true;
            }
            if(update) {
                updateText();
            }
        }
    }

    protected void deletePrev() {
        if(!readOnly) {
            if(hasSelection()) {
                deleteSelection();
                updateText();
            } else if(cursorPos > 0) {
                --cursorPos;
                deleteNext();
            }
        }
    }

    protected void deleteNext() {
        if(!readOnly) {
            if(hasSelection()) {
                deleteSelection();
                updateText();
            } else if(cursorPos < editBuffer.length()) {
                editBuffer.deleteCharAt(cursorPos);
                updateText();
            }
        }
    }

    protected void deleteSelection() {
        editBuffer.delete(selectionStart, selectionEnd);
        selectionEnd = selectionStart;
        setCursorPos(selectionStart, false);
    }

    protected void modelChanged() {
        String modelText = model.getValue();
        if(editBuffer.length() != modelText.length() || !getText().equals(modelText)) {
            setText(modelText);
        }
    }

    protected boolean hasFocusOrPopup() {
        return hasKeyboardFocus() || hasOpenPopups();
    }
    
    @Override
    protected void paintOverlay(GUI gui) {
        if(cursorImage != null && hasFocusOrPopup()) {
            int xpos = textRenderer.lastTextX + textRenderer.computeRelativeCursorPositionX(cursorPos);
            cursorImage.draw(getAnimationState(), xpos, textRenderer.computeTextY(),
                    cursorImage.getWidth(), textRenderer.getFont().getLineHeight());
        }
        super.paintOverlay(gui);
    }

    private void openErrorInfoWindow() {
        if(autoCompletionWindow == null || !autoCompletionWindow.isOpen()) {
            if(errorInfoWindow == null) {
                errorInfoLabel = new Label();
                errorInfoWindow = new InfoWindow(this);
                errorInfoWindow.setTheme("editfield-errorinfowindow");
                errorInfoWindow.add(errorInfoLabel);
            }
            errorInfoLabel.setText(errorMsg.toString());
            errorInfoWindow.openInfo();
            layoutErrorInfoWindow();
        }
    }

    private void layoutErrorInfoWindow() {
        errorInfoWindow.setSize(getWidth(), errorInfoWindow.getPreferredHeight());
        errorInfoWindow.setPosition(getX(), getBottom());
    }

    @Override
    protected void keyboardFocusGained() {
        if(errorMsg != null) {
            openErrorInfoWindow();
        }
    }

    @Override
    protected void keyboardFocusLost() {
        super.keyboardFocusLost();
        if(errorInfoWindow != null) {
            errorInfoWindow.closeInfo();
        }
    }

    protected class ModelChangeListener implements Runnable {
        public void run() {
            modelChanged();
        }
    }

    protected class TextRenderer extends TextWidget {
        int lastTextX;

        protected TextRenderer(AnimationState animState) {
            super(animState);
        }

        @Override
        protected void paintWidget(GUI gui) {
            lastTextX = computeTextX();
            if(hasSelection() && hasFocusOrPopup()) {
                if(selectionImage != null) {
                    int xpos0 = lastTextX + computeRelativeCursorPositionX(selectionStart);
                    int xpos1 = lastTextX + computeRelativeCursorPositionX(selectionEnd);
                    selectionImage.draw(getAnimationState(), xpos0, computeTextY(),
                            xpos1 - xpos0, getFont().getLineHeight());
                }
                paintWithSelection(getAnimationState(), selectionStart, selectionEnd);
            } else {
                paintLabelText(getAnimationState());
            }
        }

        @Override
        protected void sizeChanged() {
            scrollToCursor(true);
        }

        @Override
        protected int computeTextX() {
            if(hasFocusOrPopup()) {
                return getInnerX() - scrollPos;
            } else {
                return getInnerX();
            }
        }

        protected int getCursorPosFromMouse(int x) {
            if(getFont() != null) {
                x += getFont().getSpaceWidth() / 2;
                return getFont().computeVisibleGlpyhs(
                        getText(), 0, editBuffer.length(),
                        x - lastTextX);
            } else {
                return 0;
            }
        }
    }

    static class PasswordMasker implements CharSequence {
        private final CharSequence base;
        private final char maskingChar;

        public PasswordMasker(CharSequence base, char maskingChar) {
            this.base = base;
            this.maskingChar = maskingChar;
        }

        public int length() {
            return base.length();
        }

        public char charAt(int index) {
            return maskingChar;
        }

        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

}
