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
import de.matthiasmann.twl.renderer.Image;
import org.lwjgl.input.Keyboard;

/**
 * A simple one line edit field
 * 
 * @author Matthias Mann
 */
public class EditField extends Widget {

    public interface Callback {
        public void callback(int key);
    }

    private final StringBuilder editBuffer;
    private TextRenderer textRenderer;
    private PasswordMasker passwordMasking;

    private int cursorPos;
    private int scrollPos;
    private int selectionStart;
    private int selectionEnd;
    private int maxTextLength = Short.MAX_VALUE;

    private int desiredWidthinCharacters = 5;
    private Image cursorImage;
    private Image selectionImage;
    private Color errorColor;
    private Color normalColor;
    private Color selectionColor;
    private char passwordChar;
    private Object errorMsg;
    private Callback[] callbacks;
    private PopupMenu popupMenu;
    
    public EditField() {
        this.editBuffer = new StringBuilder();
        this.textRenderer = new TextRenderer(getAnimationState());
        this.passwordChar = '*';

        textRenderer.setTheme("renderer");
        textRenderer.setClip(true);
        
        add(textRenderer);
        setCanAcceptKeyboardFocus(true);
    }

    public void addCallback(Callback cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Callback.class);
    }

    public void removeCallback(Callback cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb, Callback.class);
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

    public void setText(String text) {
        text = TextUtil.limitStringLength(text, maxTextLength);
        editBuffer.replace(0, editBuffer.length(), text);
        cursorPos = editBuffer.length();
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

    public void insertText(String str) {
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
        cursorImage = themeInfo.getImage("cursor");
        selectionImage = themeInfo.getImage("selection");
        errorColor = themeInfo.getParameter("textColorError", Color.RED);
        normalColor = themeInfo.getParameter("textColor", Color.WHITE);
        selectionColor = themeInfo.getParameter("selectionColor", Color.WHITE);
        setPasswordChar((char)themeInfo.getParameter("passwordChar", '*'));
        setErrorMessage(errorMsg);  // update color
    }

    @Override
    protected void layout() {
        textRenderer.setPosition(getInnerX(), getInnerY());
        textRenderer.setSize(getInnerWidth(), getInnerHeight());
    }

    @Override
    public int getPreferedInnerWidth() {
        int prefWidth = 0;
        Font font = textRenderer.getFont();
        if(font != null) {
            int lineHeight = font.getLineHeight();
            prefWidth = Math.max(prefWidth, lineHeight*desiredWidthinCharacters/2);
        }
        return prefWidth;
    }

    @Override
    public int getPreferedInnerHeight() {
        int prefHeight = super.getPreferedInnerHeight();
        Font font = textRenderer.getFont();
        if(font != null) {
            int lineHeight = font.getLineHeight();
            prefHeight = Math.max(prefHeight, lineHeight);
        }
        return prefHeight;
    }

    @Override
    public int getMinWidth() {
        return getPreferedWidth();
    }

    @Override
    public int getMinHeight() {
        return getPreferedHeight();
    }

    public void setErrorMessage(Object errorMsg) {
        if(errorMsg != null) {
            textRenderer.setTextColor(errorColor);
        } else {
            textRenderer.setTextColor(normalColor);
        }
        if(this.errorMsg != errorMsg) {
            this.errorMsg = errorMsg;
            GUI gui = getGUI();
            if(gui != null) {
                gui.requestToolTipUpdate(this);
            }
        }
    }

    @Override
    public Object getTooltipContent() {
        if(errorMsg != null) {
            return errorMsg;
        }
        return super.getTooltipContent();
    }

    @Override
    public boolean handleEvent(Event evt) {
        boolean selectPressed = (evt.getModifiers() & Event.MODIFIER_SHIFT) != 0;

        if(evt.isMouseDragEvent()) {
            if(evt.getType() == Event.Type.MOUSE_DRAGED &&
                    (evt.getModifiers() & Event.MODIFIER_LBUTTON) != 0) {
                int newPos = textRenderer.getCursorPosFromMouse(evt.getMouseX());
                setCursorPos(newPos, true);
            }
            return true;
        }

        switch (evt.getType()) {
        case CHAR_TYPED:
            insertChar(evt.getKeyChar());
            return true;
            
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
            }
            break;

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
        }
        
        if(super.handleEvent(evt)) {
            return true;
        }

        return evt.isMouseEvent();
    }

    protected void showPopupMenu(Event evt) {
        if(popupMenu == null) {
            popupMenu = createPopupMenu();
        }
        if(popupMenu != null) {
            popupMenu.showPopup(evt.getMouseX(), evt.getMouseY());
        }
    }

    protected PopupMenu createPopupMenu() {
        Button btnCopy = new Button("copy");
        btnCopy.addCallback(new Runnable() {
            public void run() {
                copyToClipboard();
            }
        });

        Button btnClear = new Button("clear");
        btnClear.addCallback(new Runnable() {
            public void run() {
                setText("");
            }
        });

        Button btnPaste = new Button("paste");
        btnPaste.addCallback(new Runnable() {
            public void run() {
                pasteFromClipboard();
            }
        });

        PopupMenu menu = new PopupMenu(this);
        menu.add(btnCopy);
        menu.add(btnClear);
        menu.add(btnPaste);
        return menu;
    }

    private void updateText() {
        textRenderer.setText(passwordMasking != null ? passwordMasking : editBuffer);
        scrollToCursor(false);
        doCallback(Keyboard.KEY_NONE);
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
        if(!Character.isISOControl(ch)) {
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
        if(hasSelection()) {
            deleteSelection();
            updateText();
        } else if(cursorPos > 0) {
            --cursorPos;
            deleteNext();
        }
    }

    protected void deleteNext() {
        if(hasSelection()) {
            deleteSelection();
            updateText();
        } else if(cursorPos < editBuffer.length()) {
            editBuffer.deleteCharAt(cursorPos);
            updateText();
        }
    }

    protected void deleteSelection() {
        editBuffer.delete(selectionStart, selectionEnd);
        selectionEnd = selectionStart;
        setCursorPos(selectionStart, false);
    }

    protected class TextRenderer extends TextWidget {
        int lastTextX;

        protected TextRenderer(AnimationState animState) {
            super(animState);
        }

        @Override
        protected void paintWidget(GUI gui) {
            boolean paintText = true;
            lastTextX = computeTextX();
            if(hasSelection()) {
                if(selectionImage != null) {
                    int xpos0 = lastTextX + computeRelativeCursorPositionX(selectionStart);
                    int xpos1 = lastTextX + computeRelativeCursorPositionX(selectionEnd);
                    selectionImage.draw(getAnimationState(), xpos0, computeTextY(),
                            xpos1 - xpos0, getFont().getLineHeight());
                }
                if(selectionColor != null && !selectionColor.equals(getTextColor())) {
                    paintWithSelection(selectionStart, selectionEnd, selectionColor);
                    paintText = false;
                }
            }
            if(paintText) {
                paintLabelText(getTextColor());
            }
            if(getParent().hasKeyboardFocus() && cursorImage != null) {
                int xpos = lastTextX + computeRelativeCursorPositionX(cursorPos);
                cursorImage.draw(getAnimationState(), xpos, computeTextY(),
                        cursorImage.getWidth(), getFont().getLineHeight());
            }
        }

        @Override
        protected void sizeChanged() {
            scrollToCursor(true);
        }

        @Override
        protected int computeTextX() {
            if(getParent().hasKeyboardFocus()) {
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
