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

/**
 * A generic popup window.
 * Must not be added as a child to another Widget.
 *
 * While other widgets have a parent/child relationship, popup windows have
 * only have a owner.
 *
 * When a popup window is open it will block all mouse and keyboard events to
 * the UI layer of it's owner. This includes the owner, all it's children, all
 * siblings and parents etc.
 *
 * If the popup window is hidden or disabled it will close instead.
 * 
 * When the owner is hidden (either directly or indirectly) or removed from
 * the GUI tree then the popup is also closed.
 *
 * To use a PopupWindow construct it with your widget as owner and add the
 * content widget. Call {@code openPopup} to make it visible.
 *
 * Only one widget should be added as child to a popup window. This widget
 * will occupy the whole inner area. If more then one widget is added then
 * they will overlap.
 *
 * @author Matthias Mann
 * @see #openPopup()
 * @see #layoutChildrenFullInnerArea()
 */
public class PopupWindow extends Widget {

    private final Widget owner;

    private boolean closeOnClickedOutside = true;
    private boolean closeOnEscape = true;

    /**
     * Creates a new popup window.
     *
     * @param owner The owner of this popup
     */
    public PopupWindow(Widget owner) {
        if(owner == null) {
            throw new NullPointerException("owner");
        }
        this.owner = owner;
    }

    public Widget getOwner() {
        return owner;
    }

    public boolean isCloseOnClickedOutside() {
        return closeOnClickedOutside;
    }

    /**
     * Controls if this popup window should close when a mouse click
     * happens outside of it's area. This is useful for context menus or
     * drop down combo boxes.
     *
     * Default is true.
     *
     * @param closeOnClickedOutside true if it should close on clicks outside it's area
     */
    public void setCloseOnClickedOutside(boolean closeOnClickedOutside) {
        this.closeOnClickedOutside = closeOnClickedOutside;
    }

    public boolean isCloseOnEscape() {
        return closeOnEscape;
    }

    /**
     * Controls if this popup should close when the escape key is pressed.
     *
     * Default is true.
     *
     * @param closeOnEscape true if it should close on escape
     */
    public void setCloseOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
    }

    /**
     * Opens the popup window with it's current size and position.
     * In order for this to work the owner must be part of the GUI tree.
     *
     * When a popup window is shown it is always visible and enabled.
     * 
     * @return true if the popup window could be opened.
     * @see #getOwner() 
     * @see #getGUI()
     */
    public boolean openPopup() {
        GUI gui = owner.getGUI();
        if(gui != null) {
            // a popup can't be invisible or disabled when it should open
            super.setVisible(true);
            super.setEnabled(true);
            // owner's hasOpenPopups flag is handled by GUI
            gui.openPopup(this);
            requestKeyboardFocus();
            focusFirstChild();
            return true;
        }
        return false;
    }

    /**
     * Opens the popup window, calls {@code adjustSize} and centers the popup on
     * the screen.
     *
     * @see #adjustSize() 
     */
    public void openPopupCentered() {
        if(openPopup()) {
            adjustSize();
            setPosition(
                    getParent().getInnerX() + (getParent().getInnerWidth() - getWidth())/2,
                    getParent().getInnerY() + (getParent().getInnerHeight() - getHeight())/2);
        }
    }

    /**
     * Closes this popup window. Keyboard focus is transfered to it's owner.
     */
    public void closePopup() {
        GUI gui = getGUI();
        if(gui != null) {
            // owner's hasOpenPopups flag is handled by GUI
            gui.closePopup(this);
            owner.requestKeyboardFocus();
        }
    }

    /**
     * Checks if theis popup window is currently open
     * @return true if it is open
     */
    public boolean isOpen() {
        return getParent() != null;
    }

    @Override
    public int getPreferredInnerWidth() {
        return BoxLayout.computePreferredWidthVertical(this);
    }

    @Override
    public int getPreferredInnerHeight() {
        return BoxLayout.computePreferredHeightHorizontal(this);
    }

    @Override
    public int getPreferredWidth() {
        int parentWidth = (getParent() != null) ? getParent().getInnerWidth() : Short.MAX_VALUE;
        return Math.min(parentWidth, super.getPreferredWidth());
    }

    @Override
    public int getPreferredHeight() {
        int parentHeight = (getParent() != null) ? getParent().getInnerHeight() : Short.MAX_VALUE;
        return Math.min(parentHeight, super.getPreferredHeight());
    }

    @Override
    protected void layout() {
        layoutChildrenFullInnerArea();
    }
    
    @Override
    protected final boolean handleEvent(Event evt) {
        if(handleEventPopup(evt)) {
            return true;
        }
        if(evt.getType() == Event.Type.MOUSE_CLICKED &&
                !isInside(evt.getMouseX(), evt.getMouseY())) {
            mouseClickedOutside(evt);
            return true;
        }
        if(closeOnEscape &&
                evt.getType() == Event.Type.KEY_PRESSED &&
                evt.getKeyCode() == Event.KEY_ESCAPE) {
            requestPopupClose();
            return true;
        }
        // eat all events
        return true;
    }

    protected boolean handleEventPopup(Event evt) {
        return super.handleEvent(evt);
    }

    @Override
    protected final boolean isMouseInside(Event evt) {
        return true;    // :P
    }

    /**
     * Called when escape if pressed and {@code closeOnEscape} is enabled.
     * Also called by the default implementation of {@code mouseClickedOutside}
     * when {@code closeOnClickedOutside} is active.
     *
     * @see #setCloseOnEscape(boolean)
     * @see #mouseClickedOutside(de.matthiasmann.twl.Event)
     */
    protected void requestPopupClose() {
        closePopup();
    }

    /**
     * Called when a mouse click happened outside the popup window area.
     *
     * The default implementation calls {@code requestPopupClose} when
     * {@code closeOnClickedOutside} is active.
     *
     * @param evt The click event
     * @see #setCloseOnClickedOutside(boolean) 
     */
    protected void mouseClickedOutside(Event evt) {
        if(closeOnClickedOutside) {
            requestPopupClose();
        }
    }
}
