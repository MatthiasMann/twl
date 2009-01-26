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

import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.input.Keyboard;

/**
 * Root of the TWL class hierarchy.
 * 
 * @author Matthias Mann
 */
public class Widget {

    public static final String STATE_KEYBOARD_FOCUS = "keyboardFocus";
    
    private static final int FOCUS_KEY = Keyboard.KEY_TAB;
    
    private Widget parent;
    private int posX;
    private int posY;
    private int width;
    private int height;
    private boolean clip;
    private boolean visible = true;
    private boolean hasOpenPopup;
    private boolean layoutInvalid;
    private String theme;
    private ThemeManager themeManager;
    private Image background;
    private Image overlay;
    private Object tooltipContent;

    private final AnimationState animState;
    private final boolean sharedAnimState;

    private short borderLeft;
    private short borderTop;
    private short borderRight;
    private short borderBottom;

    private short minWidth;
    private short minHeight;
    private short maxWidth;
    private short maxHeight;

    private ArrayList<Widget> children;
    private Widget lastChildMouseOver;
    private Widget focusChild;
    private MouseCursor mouseCursor;

    private boolean focusKeyEnabled = true;
    private boolean canAcceptKeyboardFocus;
    
    public Widget() {
        this(null);
    }

    public Widget(AnimationState animState) {
        // determine the default theme name from the class name of this instance
        // eg class Label => "label"
        Class clazz = getClass();
        do {
            theme = clazz.getSimpleName().toLowerCase();
            clazz = clazz.getSuperclass();
        } while (theme.length() == 0 && clazz != null);

        if(animState == null) {
            this.animState = new AnimationState();
            this.sharedAnimState = false;
        } else {
            this.animState = animState;
            this.sharedAnimState = true;
        }
    }
    
    /**
     * Checks whether this widget or atleast one of it's children
     * owns an open popup.
     * @return true if atleast own open popup is owned (indirectly) by this widget.
     */
    public boolean hasOpenPopups() {
        return hasOpenPopup;
    }
    
    /**
     * Returns the parent of this widget or null if it is the tree root.
     * All coordinates are relative to the root of the widget tree.
     * @return the parent of this widget or null if it is the tree root
     */
    public final Widget getParent() {
        return parent;
    }
    
    /**
     * Returns the root of this widget tree.
     * All coordinates are relative to the root of the widget tree.
     * @return the root of this widget tree
     */
    public final Widget getRootWidget() {
        Widget w = this;
        while(w.parent != null) {
            w = w.parent;
        }
        return w;
    }

    /**
     * Returns the GUI root of this widget tree if it has one.
     * @return the GUI root or null if the root is not a GUI instance.
     */
    public final GUI getGUI() {
        Widget root = getRootWidget();
        if(root instanceof GUI) {
            return (GUI)root;
        }
        return null;
    }
    
    /**
     * Returns the current visibility flag of this widget.
     * This does not check if the widget is cliped or buired behind another widget.
     * @return the current visibility flag of this widget
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Changes the visibility flag of this widget.
     * Widgets are by default visible.
     * Invisible widgets don't receive paint() or handleEvent() calls
     * @param visible the new visibility flag
     */
    public void setVisible(boolean visible) {
        if(this.visible != visible) {
            this.visible = visible;
            if(!visible) {
                GUI gui = getGUI();
                if(gui != null) {
                    gui.widgetHidden(this);
                }
                if(parent != null) {
                    parent.childHidden(this);
                }
            }
        }
    }

    /**
     * Returns the absolute X coordinate of widget in it's tree
     * @return the absolute X coordinate of widget in it's tree
     */
    public final int getX() {
        return posX;
    }


    /**
     * Returns the absolute Y coordinate of widget in it's tree
     * @return the absolute Y coordinate of widget in it's tree
     */
    public final int getY() {
        return posY;
    }
    
    /**
     * Returns the width of this widget
     * @return the width of this widget
     */
    public final int getWidth() {
        return width;
    }

    /**
     * Returns the height of this widget
     * @return the height of this widget
     */
    public final int getHeight() {
        return height;
    }
    
    /**
     * Returns the right X coordinate of this widget
     * @return getX() + getWidth()
     */
    public final int getRight() {
        return posX + width;
    }
    
    /**
     * Returns the bottom Y coordinate of this widget
     * @return getY() + getHeight()
     */
    public final int getBottom() {
        return posY + height;
    }

    /**
     * The inner X position takes the left border into account
     * @return getX() + getBorderLeft()
     */
    public final int getInnerX() {
        return posX + borderLeft;
    }

    /**
     * The inner Y position takes the top border into account
     * @return getY() + getBorderTop()
     */
    public final int getInnerY() {
        return posY + borderTop;
    }
    
    /**
     * The inner width takes the left and right border into account.
     * @return the inner width - never negative
     */
    public final int getInnerWidth() {
        return Math.max(0, width - borderLeft - borderRight);
    }

    /**
     * The inner height takes the top and bottom border into account.
     * @return the inner height - never negative
     */
    public final int getInnerHeight() {
        return Math.max(0, height - borderTop - borderBottom);
    }
    
    /**
     * Returns the right X coordinate while taking the right border into account.
     * @return getInnerX() + getInnerWidth()
     */
    public final int getInnerRight() {
        return posX + Math.max(borderLeft, width - borderRight);
    }
    
    /**
     * Returns the bottom Y coordinate while taking the bottom border into account.
     * @return getInnerY() + getInnerHeight()
     */
    public final int getInnerBottom() {
        return posY + Math.max(borderTop, height - borderBottom);
    }
    
    /**
     * Checks if the given absolute (to this widget's tree) coordinates are inside this widget.
     * 
     * @param x the X coordinate to test
     * @param y the Y coordinate to test
     * @return true if it was inside
     */
    public final boolean isInside(int x, int y) {
        return (x >= posX) && (y >= posY) && (x < posX + width) && (y < posY + height);
    }

    /**
     * Changes the position of this widget.
     * Negative position is allowed.
     * 
     * If position has changed then it updates the position of all children
     * before calling positionChanged.
     * 
     * NOTE: Position is absolute in the widget's tree.
     * 
     * @param x The new x position
     * @param y The new y position
     * @return true if the position was changed, false if new position == old position
     * @see #positionChanged()
     */
    public boolean setPosition(int x, int y) {
        int deltaX = x - posX;
        int deltaY = y - posY;
        if(deltaX != 0 || deltaY != 0) {
            this.posX = x;
            this.posY = y;
            
            if(children != null) {
                for(int i=0,n=children.size() ; i<n ; i++) {
                    adjustChildPosition(children.get(i), deltaX, deltaY);
                }
            }
            
            positionChanged();
            return true;
        }
        return false;
    }
    
    /** 
     * Changes the size of this widget.
     * Zero size is allowed but not negative.
     * Size is not checked against parent widgets.
     * 
     * When the size has changed then
     * - the parent widget's childChangedSize is called
     * - sizeChanged is called
     * 
     * @param width The new width (including border)
     * @param height The new height (including border)
     * @return true if the size was changed, false if new size == old size
     * @throws java.lang.IllegalArgumentException if the size is negative
     * @see #sizeChanged()
     * @see #childChangedSize(Widget)
     */
    public boolean setSize(int width, int height) {
        if(width < 0 || height < 0) {
            throw new IllegalArgumentException("negative size");
        }
        if(this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            if(parent != null) {
                parent.childChangedSize(this);
            }

            sizeChanged();
            return true;
        }
        return false;
    }
    
    /** 
     * Changes the inner size of this widget.
     * Calls setSize after adding the border width/height.
     * 
     * @param width The new width (exclusive border)
     * @param height The new height (exclusive border)
     * @return true if the size was changed, false if new size == old size
     * @see #setSize(int,int)
     */
    public boolean setInnerSize(int width, int height) {
        return setSize(width + borderLeft + borderRight, height + borderTop + borderBottom);
    }

    public short getBorderTop() {
        return borderTop;
    }

    public short getBorderLeft() {
        return borderLeft;
    }

    public short getBorderBottom() {
        return borderBottom;
    }

    public short getBorderRight() {
        return borderRight;
    }

    public int getBorderHorizontal() {
        return borderLeft + borderRight;
    }

    public int getBorderVertical() {
        return borderTop + borderBottom;
    }

    /**
     * Sets a border for this widget.
     * @param top the top border
     * @param left the left border
     * @param bottom the bottom  border
     * @param right the right border
     * @return true if the border values have changed
     * @throws IllegalArgumentException if any of the parameters is negative.
     */
    public boolean setBorderSize(int top, int left, int bottom, int right) {
        if(top < 0 || left < 0 || bottom < 0 || right < 0) {
            throw new IllegalArgumentException("negative border size");
        }
        if(this.borderTop != top ||  this.borderBottom != bottom ||
                this.borderLeft != left || this.borderRight != right) {
            int innerWidth = getInnerWidth();
            int innerHeight = getInnerHeight();
            int deltaLeft = left - this.borderLeft;
            int deltaTop = top - this.borderTop;
            this.borderLeft = (short)left;
            this.borderTop = (short)top;
            this.borderRight = (short)right;
            this.borderBottom = (short)bottom;
            
            // first adjust child position
            if(children != null && (deltaLeft != 0 || deltaTop != 0)) {
                for(int i=0,n=children.size() ; i<n ; i++) {
                    adjustChildPosition(children.get(i), deltaLeft, deltaTop);
                }
            }
            
            // now change size
            setInnerSize(innerWidth, innerHeight);
            borderChanged();
            return true;
        }
        return false;
    }
    
    /**
     * Sets a border for this widget.
     * @param horizontal the border width for left and right
     * @param vertical the border height for top and bottom
     * @return true if the border values have changed
     * @throws IllegalArgumentException if horizontal or vertical is negative.
     */
    public boolean setBorderSize(int horizontal, int vertical) {
        return setBorderSize(vertical, horizontal, vertical, horizontal);
    }
    
    /**
     * Sets a uniform border for this widget.
     * @param border the border width/height on all edges
     * @return true if the border values have changed
     * @throws IllegalArgumentException if border is negative.
     */
    public boolean setBorderSize(int border) {
        return setBorderSize(border, border, border, border);
    }
    
    /**
     * Sets the border width for this widget.
     * @param border the border object or null for no border
     * @return true if the border values have changed
     */
    public boolean setBorderSize(Border border) {
        if(border == null) {
            return setBorderSize(0, 0, 0, 0);
        } else {
            return setBorderSize(border.getBorderTop(), border.getBorderLeft(),
                                    border.getBorderBottom(), border.getBorderRight());
        }
    }

    public int getMinWidth() {
        return Math.max(minWidth, borderLeft + borderRight);
    }

    public int getMinHeight() {
        return Math.max(minHeight, borderTop + borderBottom);
    }

    public void setMinSize(int width, int height) {
        minWidth = (short)Math.min(width, Short.MAX_VALUE);
        minHeight = (short)Math.min(height, Short.MAX_VALUE);
    }

    public int getPreferredInnerWidth() {
        int right = getInnerX();
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                Widget child = children.get(i);
                right = Math.max(right, child.getRight());
            }
        }
        return right - getInnerX();
    }

    /**
     * Returns the preferred width based on it's children and preferred inner width.
     *
     * Subclasses can overwrite this method to compute the preferred size differently.
     *
     * @return the preferred width.
     */
    public int getPreferredWidth() {
        int prefWidth = borderLeft + borderRight + getPreferredInnerWidth();
        Image bg = getBackground();
        if(bg != null) {
            prefWidth = Math.max(prefWidth, bg.getWidth());
        }
        return Math.max(minWidth, prefWidth);
    }

    public int getPreferredInnerHeight() {
        int bottom = getInnerY();
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                Widget child = children.get(i);
                bottom = Math.max(bottom, child.getBottom());
            }
        }
        return bottom - getInnerY();
    }

    /**
     * Returns the preferred height.
     *
     * This method determines the preferred height based on it's children.
     * Subclasses can overwrite this method to compute the preferred size differently.
     *
     * @return the preferred height.
     */
    public int getPreferredHeight() {
        int prefHeight = borderTop + borderBottom + getPreferredInnerHeight();
        Image bg = getBackground();
        if(bg != null) {
            prefHeight = Math.max(prefHeight, bg.getHeight());
        }
        return Math.max(minHeight, prefHeight);
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxSize(int width, int height) {
        maxWidth = (short)Math.min(width, Short.MAX_VALUE);
        maxHeight = (short)Math.min(height, Short.MAX_VALUE);
    }

    public static int computeSize(int min, int width, int max) {
        if(max > 0) {
            width = Math.min(width, max);
        }
        return Math.max(min, width);
    }

    /**
     * Auto adjust the size of this widget based on it's preferred size.
     * 
     * Subclasses can provide more functionality
     */
    public void adjustSize() {
        /*
        System.out.println(this+" minSize="+getMinWidth()+","+getMinHeight()+
                " prefSize="+getPreferredWidth()+","+getPreferredHeight()+
                " maxSize="+getMaxWidth()+","+getMaxHeight());
         * */
        setSize(computeSize(getMinWidth(), getPreferredWidth(), getMaxWidth()),
                computeSize(getMinHeight(), getPreferredHeight(), getMaxHeight()));
        validateLayout();
    }

    /**
     * Called when something has changed which affected the layout of this widget.
     * The default implementation sets a layout invalid flag which causes layout() to be called.
     *
     * Called by the default implementation of sizeChanged, borderChanged and childChangedSize.
     *
     * @see #sizeChanged()
     * @see #borderChanged()
     * @see #childChangedSize(Widget)
     */
    public void invalidateLayout() {
        if(!layoutInvalid) {
            layoutInvalid = true;
            GUI gui = getGUI();
            if(gui != null) {
                gui.hasInvalidLayouts = true;
            }
        }
    }

    /**
     * Calls layout() if the layout is marked invalid.
     * @see #invalidateLayout()
     * @see #layout()
     */
    public void validateLayout() {
        if(layoutInvalid) {
            layout();
        }
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                children.get(i).validateLayout();
            }
        }
        // reset flag after calling layout() to prevent loops caused by childChangedSize(Widget)
        layoutInvalid = false;
    }
    
    /**
     * Returns the current theme name of this widget.
     * The default theme name is the lower case simple class name of this widget.
     * @return the current theme name of this widget
     */
    public String getTheme() {
        return theme;
    }
    
    /**
     * Changes the theme name of this widget - DOES NOT call applyTheme()
     * 
     * @param theme The new theme path element
     * @throws java.lang.NullPointerException if theme is null
     * @see #applyTheme(ThemeManager)
     */
    public void setTheme(String theme) {
        if(theme == null) {
            throw new NullPointerException("theme");
        }
        this.theme = theme;
    }
    
    /**
     * Returns this widget's theme path by concatinating the theme names
     * from all parents separated by '.'
     * @return the effective theme path - can be empty
     */
    public final String getThemePath() {
        return getThemePath(0).toString();
    }

    /**
     * Returns true if paint() is clipped to this widget.
     * @return true if paint() is clipped to this widget
     */
    public boolean isClip() {
        return clip;
    }

    /**
     * Sets whether paint() must be clipped to this Widget or not.
     * 
     * @param clip true if clipping must be used - default is false
     **/
    public void setClip(boolean clip) {
        this.clip = clip;
    }

    /**
     * Returns if this widget will handle the FOCUS_KEY.
     * @return if this widget will handle the FOCUS_KEY.
     */
    public boolean isFocusKeyEnabled() {
        return focusKeyEnabled;
    }

    /**
     * Controls the handling of the FOCUS_KEY.
     * @param focusKeyEnabled if true this widget will handle the focus key.
     */
    public void setFocusKeyEnabled(boolean focusKeyEnabled) {
        this.focusKeyEnabled = focusKeyEnabled;
    }

    /**
     * Returns the current background image or null.
     * @return the current background image or null
     * @see #paintBackground(de.matthiasmann.twl.GUI)
     */
    public Image getBackground() {
        return background;
    }

    /**
     * Sets the background image that should be drawn before drawing this widget
     * @param background the new background image - can be null
     * @see #paintBackground(de.matthiasmann.twl.GUI)
     */
    public void setBackground(Image background) {
        this.background = background;
    }

    /**
     * Returns the current overlay image or null.
     * @return the current overlay image or null.
     * @see #paintOverlay(de.matthiasmann.twl.GUI)
     */
    public Image getOverlay() {
        return overlay;
    }

    /**
     * Sets the overlay image that should be drawn after drawing the children
     * @param overlay the new overlay image - can be null
     * @see #paintOverlay(de.matthiasmann.twl.GUI)
     */
    public void setOverlay(Image overlay) {
        this.overlay = overlay;
    }

    public MouseCursor getMouseCursor() {
        return mouseCursor;
    }

    public void setMouseCursor(MouseCursor mouseCursor) {
        this.mouseCursor = mouseCursor;
    }

    /**
     * Returns the number of children in this widget.
     * @return the number of children in this widget
     */
    public final int getNumChildren() {
        if(children != null) {
            return children.size();
        }
        return 0;
    }

    /**
     * Returns the child at the given index
     * @param index
     * @return the child widget
     * @throws java.lang.IndexOutOfBoundsException if the index is invalid
     */
    public final Widget getChild(int index) throws IndexOutOfBoundsException {
        if(children != null) {
            return children.get(index);
        }
        throw new IndexOutOfBoundsException();
    }

    /**
     * Adds a new child at the end of this widget
     * @param child the child that should be added
     * @throws java.lang.NullPointerException if child is null
     * @throws java.lang.IllegalArgumentException if the child is already in a tree
     */
    public void add(Widget child) {
        insertChild(child, getNumChildren());
    }
    
    /**
     * Inserts a new child into this widget.
     * The position of the child is treated as relative to this widget and adjusted.
     * If a theme was applied to this widget then this theme is also applied to the new child.
     * 
     * @param child the child that should be inserted
     * @param index the index where it should be inserted
     * @throws java.lang.IndexOutOfBoundsException if the index is invalid
     * @throws java.lang.NullPointerException if child is null
     * @throws java.lang.IllegalArgumentException if the child is already in a tree
     */
    public void insertChild(Widget child, int index) throws IndexOutOfBoundsException {
        if(child == null) {
            throw new NullPointerException();
        }
        if(child == this) {
            throw new IllegalArgumentException("can't add to self");
        }
        if(child.parent != null) {
            throw new IllegalArgumentException("child widget already in tree");
        }
        if(children == null) {
            children = new ArrayList<Widget>();
        }
        if(index < 0 || index > children.size()) {
            throw new IndexOutOfBoundsException();
        }
        children.add(index, child);
        child.parent = this;
        adjustChildPosition(child, posX + borderLeft, posY + borderTop);
        GUI gui = getGUI();
        if(gui != null) {
            child.recursivelyAddToGUI(gui);
        }
        if(themeManager != null) {
            child.applyTheme(themeManager);
        }
        childAdded(child);
        // A newly added child can't have open popups
        // because it needs a GUI for this - and it had no parent up to now
    }
    
    /**
     * Returns the index of the specified child in this widget.
     * Uses object identity for comparing.
     * @param child the child which index should be returned
     * @return the index of the child or -1 if it was not found
     */
    public final int getChildIndex(Widget child) {
        if(children != null) {
            // can't use children.indexOf(child) as this uses equals()
            for(int i=0,n=children.size() ; i<n ; i++) {
                if(children.get(i) == child) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Removes the specified child from this widget.
     * Uses object identity for comparing.
     * @param child the child that should be removed.
     * @return true if the child was found and removed.
     */
    public boolean removeChild(Widget child) {
        int idx = getChildIndex(child);
        if(idx >= 0) {
            removeChild(idx);
            return true;
        }
        return false;
    }

    /**
     * Removes the specified child from this widget.
     * The position of the removed child is changed to the relative
     * position to this widget.
     * Calls invalidateLayout after removing the child.
     * 
     * @param index the index of the child
     * @return the removed widget
     * @throws java.lang.IndexOutOfBoundsException if the index is invalid
     * @see #invalidateLayout()
     */
    public Widget removeChild(int index) throws IndexOutOfBoundsException {
        if(children != null) {
            Widget child = children.remove(index);
            unparentChild(child);
            if(lastChildMouseOver == child) {
                lastChildMouseOver = null;
            }
            if(focusChild == child) {
                focusChild = null;
            }
            childRemoved(child);
            return child;
        }
        throw new IndexOutOfBoundsException();
    }

    /**
     * Removes all children of this widget.
     * The position of the all removed children is changed to the relative
     * position to this widget.
     * Calls invalidateLayout after removing all children.
     * 
     * @see #invalidateLayout()
     */
    public void removeAllChildren() {
        if(children != null) {
            focusChild = null;
            lastChildMouseOver = null;
            for(int i=0,n=children.size() ; i<n ; i++) {
                Widget child = children.get(i);
                unparentChild(child);
            }
            children.clear(); // we expect that new children will be added - so keep list
            invalidateLayout();
        }
        if(hasOpenPopup) {
            GUI gui = getGUI();
            assert(gui != null);
            recalcOpenPopups(gui);
        }
    }
    
    /**
     * Clean up GL resources. When overwritten then super method must be called.
     */
    public void destroy() {
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                children.get(i).destroy();
            }
        }
    }

    public boolean canAcceptKeyboardFocus() {
        return canAcceptKeyboardFocus;
    }

    public void setCanAcceptKeyboardFocus(boolean canAcceptKeyboardFocus) {
        this.canAcceptKeyboardFocus = canAcceptKeyboardFocus;
    }

    /**
     * Requests that the keyboard focus is transfered to this widget. Use with care.
     * @return true if keyboard focus was transfered to this widget.
     */
    public boolean requestKeyboardFocus() {
        if(parent != null) {
            if(parent.focusChild == this) {
                return true;
            }
            return parent.requestKeyboardFocus(this);
        }
        return false;
    }
    
    /**
     * Checks if this widget has the keyboard focus
     * @return true if this widget has the keyboard focus
     */
    public boolean hasKeyboardFocus() {
        if(parent != null) {
            return parent.focusChild == this;
        }
        return false;
    }

    public boolean focusNextChild() {
        if(focusChild != null) {
            if(!focusChild.canAcceptKeyboardFocus() && focusChild.focusNextChild()) {
                return true;
            }
        }
        List<Widget> focusList = getKeyboardFocusOrder();
        int focusIdx = focusList.indexOf(focusChild);
        if(focusIdx < 0) {
            focusIdx = 0;
        }

        return focusChildren(focusIdx+1, +1);
    }

    public boolean focusPrevChild() {
        if(focusChild != null) {
            if(!focusChild.canAcceptKeyboardFocus() && focusChild.focusPrevChild()) {
                return true;
            }
        }
        List<Widget> focusList = getKeyboardFocusOrder();
        int focusIdx = focusList.indexOf(focusChild);
        if(focusIdx < 0) {
            focusIdx = focusList.size();
        }

        return focusChildren(focusIdx-1, -1);
    }

    public boolean focusFirstChild() {
        return focusChildren(0, 1);
    }

    public boolean focusLastChild() {
        return focusChildren(-1, -1);
    }

    /**
     * Returns the animation state object.
     * @return the animation state object.
     */
    public AnimationState getAnimationState() {
        return animState;
    }

    /**
     * Automatic tooltip support.
     * This function is called when the mouse is idle over the widget for a certain time.
     *
     * @return the tooltip message or null if no tooltip is desired.
     */
    public Object getTooltipContent() {
        return tooltipContent;
    }

    /**
     * Changes the tooltip context. If the tooltip is currently active then
     * it's refreshed with the new content.
     *
     * @param tooltipContent the new tooltip content.
     */
    public void setTooltipContent(Object tooltipContent) {
        this.tooltipContent = tooltipContent;
        GUI gui = getGUI();
        if(gui != null) {
            gui.requestToolTipUpdate(this);
        }
    }

    /**
     * Returns the visible widget at the specified location.
     * Use this method to locate drag&drop tragets.
     *
     * Subclasses can overwrite this method hide implementation details.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the widget at that location.
     */
    public Widget getWidgetAt(int x, int y) {
        Widget child = getChildAt(x, y);
        if(child != null) {
            return child.getWidgetAt(x, y);
        }
        return this;
    }

    //
    // start of API for derived widgets
    //
    
    /**
     * Apply the given theme
     * 
     * @param themeInfo The theme info for this widget
     */
    protected void applyTheme(ThemeInfo themeInfo) {
        applyThemeBackground(themeInfo);
        applyThemeOverlay(themeInfo);
        applyThemeBorder(themeInfo);
        applyThemeMinSize(themeInfo);
        applyThemeMaxSize(themeInfo);
        applyThemeMouseCursor(themeInfo);
    }

    protected void applyThemeBackground(ThemeInfo themeInfo) {
        setBackground(themeInfo.getImage("background"));
    }

    protected void applyThemeOverlay(ThemeInfo themeInfo) {
        setOverlay(themeInfo.getImage("overlay"));
    }

    protected void applyThemeBorder(ThemeInfo themeInfo) {
        setBorderSize(themeInfo.getBorder());
    }

    protected void applyThemeMinSize(ThemeInfo themeInfo) {
        setMinSize(
                themeInfo.getParameter("minWidth", 0),
                themeInfo.getParameter("minHeight", 0));
    }

    protected void applyThemeMaxSize(ThemeInfo themeInfo) {
        setMaxSize(
                themeInfo.getParameter("maxWidth", Short.MAX_VALUE),
                themeInfo.getParameter("maxHeight", Short.MAX_VALUE));
    }

    protected void applyThemeMouseCursor(ThemeInfo themeInfo) {
        setMouseCursor(themeInfo.getMouseCursor("mouseCursor"));
    }
    
    /**
     * If the widget changed some internal state which may
     * require different theme information then this function
     * can be used to reapply the current theme.
     */
    public void reapplyTheme() {
        if(themeManager != null) {
            applyTheme(themeManager);
        }
    }
    
    /**
     * Checks whether the mouse is inside the widget or not
     * @param evt the mouse event
     * @return true if the widgets wants to claim this mouse event
     */
    protected boolean isMouseInside(Event evt) {
        return isInside(evt.getMouseX(), evt.getMouseY());
    }
    
    /**
     * Called when an event occured that this widget could be interrested in.
     *
     * The default implementation handles only keyboard events and delegates
     * them to child widget which has keyboard focus.
     * If focusKey handling is enabled then this widget cycles the keyboard
     * focus through it's childs.
     *
     * @param evt The event - do not store this object - it may be reused
     * @return true if the widget handled this event
     * @see #setFocusKeyEnabled(boolean)
     */
    protected boolean handleEvent(Event evt) {
        if(children != null) {
            if(evt.isKeyEvent()) {
                return handleKeyEvent(evt);
            }
        }
        return false;
    }
    
    /**
     * Moves the child at index from to index to. This will shift the position
     * of all children in between.
     * 
     * @param from the index of the child that should be moved
     * @param to the new index for the child at from
     * @throws java.lang.IndexOutOfBoundsException if from or to are invalid
     */
    protected void moveChild(int from, int to) {
        if(children == null) {
            throw new IndexOutOfBoundsException();
        }
        if(to < 0 || to >= children.size()) {
            throw new IndexOutOfBoundsException("to");
        }
        if(from < 0 || from >= children.size()) {
            throw new IndexOutOfBoundsException("from");
        }
        Widget child = children.remove(from);
        children.add(to, child);
    }
    
    /**
     * A child requests keyboard focus.
     * Default implementation will grant keyboard focus and
     * request itself keyboard focus.
     *
     * @param child The child that wants keyboard focus
     * @return true if the child received the focus.
     */
    protected boolean requestKeyboardFocus(Widget child) {
        if(child != null && child.parent != this) {
            throw new IllegalArgumentException("not a direct child");
        }
        if(focusChild != child) {
            if(child != null && !requestKeyboardFocus()) {
                return false;
            }
            recursivelyChildFocusLost(focusChild);
            focusChild = child;
            keyboardFocusChildChanged(focusChild);
            if(focusChild != null) {
                if(!focusChild.sharedAnimState) {
                    focusChild.animState.setAnimationState(STATE_KEYBOARD_FOCUS, true);
                }
                focusChild.keyboardFocusGained();
            }
        }
        return focusChild != null;
    }
    
    /**
     * Called when this widget is removed from the GUI tree.
     * After this call getGUI() will return null.
     * 
     * @param gui the GUI object - same as getGUI()
     * @see #getGUI()
     */
    protected void beforeRemoveFromGUI(GUI gui) {
    }
    
    /**
     * Called after this widget has been added to a GUI tree.
     * 
     * @param gui the GUI object - same as getGUI()
     * @see #getGUI()
     */
    protected void afterAddToGUI(GUI gui) {
    }
    
    /**
     * Called from paint() when the layoutInvalid flag is set
     */
    protected void layout() {
    }

    /**
     * Called when the parent container needs to adjust it's layout because
     * the min/pref/max sizes of this widget have changed.
     * 
     * @see #invalidateLayout()
     */
    protected void invalidateParentLayout() {
        if(parent != null) {
            parent.invalidateLayout();
        }
    }

    /**
     * Called when the position of this widget was changed.
     * The default implementation does nothing.
     * 
     * Child positions are already updated to retain the absolute
     * coordinate system. This has the side effect of firing child's
     * positionChanged before the parent's.
     */
    protected void positionChanged() {
    }

    /**
     * Called when the size of this widget has changed.
     * The default implementation calls invalidateLayout.
     * 
     * @see #invalidateLayout()
     */
    protected void sizeChanged() {
        invalidateLayout();
    }

    /**
     * Caleld when the border size has changed.
     * The default implementation calls invalidateLayout.
     * 
     * @see #invalidateLayout()
     */
    protected void borderChanged() {
        invalidateLayout();
    }
    
    /**
     * A child of this widget has changed it's size.
     * The default implementation calls invalidateLayout.
     * 
     * @param child the child that changed size
     * @see #invalidateLayout()
     */
    protected void childChangedSize(Widget child) {
        invalidateLayout();
    }

    /**
     * A new child has been added.
     * The default implementation calls invalidateLayout.
     *
     * @param child the new child
     * @see #invalidateLayout()
     */
    protected void childAdded(Widget child) {
        invalidateLayout();
    }

    /**
     * A child has been removed.
     * The default implementation calls invalidateLayout.
     * 
     * @param exChild the removed widget - no longer a child
     * @see #invalidateLayout()
     */
    protected void childRemoved(Widget exChild) {
        invalidateLayout();
    }

    /**
     * The current keyboard focus child has changed.
     * The default implementation does nothing.
     * 
     * @param child The child which has now the keyboard focus in this hierachy level or null
     */
    protected void keyboardFocusChildChanged(Widget child) {
    }

    protected void keyboardFocusLost() {
    }

    protected void keyboardFocusGained() {
    }

    /**
     * Paints this widget and it's children.
     * A subclass should overwrite paintWidget() instead of this function.
     * 
     * The default implementation calls the following method in order:
     *   paintBackground(gui)
     *   paintWidget(gui)
     *   paintChildren(gui)
     *   paintOverlay(gui)
     *
     * @param gui the GUI object
     * @see #paintBackground(de.matthiasmann.twl.GUI) 
     * @see #paintWidget(de.matthiasmann.twl.GUI)
     * @see #paintChildren(de.matthiasmann.twl.GUI)
     * @see #paintOverlay(de.matthiasmann.twl.GUI)
     */
    protected void paint(GUI gui) {
        paintBackground(gui);
        paintWidget(gui);
        paintChildren(gui);
        paintOverlay(gui);
    }
    
    /**
     * Called by paint() after painting the background and before painting all children.
     * This should be overwritten instead of paint() if normal themeable
     * painting is desired by the subclass.
     * @param gui the GUI object - it's the same as getGUI()
     */
    protected void paintWidget(GUI gui) {
    }
    
    /**
     * Paint the background image of this widget.
     * @param gui the GUI object
     */
    protected void paintBackground(GUI gui) {
        Image bgImage = getBackground();
        if(bgImage != null) {
            bgImage.draw(getAnimationState(), posX, posY, width, height);
        }
    }

    /**
     * Paints the overlay image of this widget.
     * @param gui the GUI object
     */
    protected void paintOverlay(GUI gui) {
        Image ovImage = getOverlay();
        if(ovImage != null) {
            ovImage.draw(getAnimationState(), posX, posY, width, height);
        }
    }

    /**
     * Paints all children in index order. Invisible children are skipped.
     * @param gui the GUI object
     */
    protected void paintChildren(GUI gui) {
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                Widget child = children.get(i);
                if(child.visible) {
                    child.drawWidget(gui);
                }
            }
        }
    }

    /**
     * Paints a specified child. Does not check for visibility.
     *
     * @param gui the GUI object
     * @param child the child Widget
     */
    protected void paintChild(GUI gui, Widget child) {
        if(child.parent != this) {
            throw new IllegalArgumentException("can only render direct children");
        }
        child.drawWidget(gui);
    }

    protected List<Widget> getKeyboardFocusOrder() {
        if(children == null) {
            return Collections.<Widget>emptyList();
        }
        return Collections.unmodifiableList(children);
    }

    protected boolean focusChildren(int start, int dir) {
        List<Widget> focusList = getKeyboardFocusOrder();
        final int n = focusList.size();
        //System.out.println(this+" "+getThemePath()+" n="+n+" start="+start+" dir="+dir);
        if(n == 0) {
            return false;
        }

        if(start < 0) {
            start += n;
        }

        for(int idx=start ; idx>=0 && idx<n ; idx+=dir) {
            Widget child = focusList.get(idx);
            if(child.visible) {
                if(child.canAcceptKeyboardFocus()) {
                    requestKeyboardFocus(child);
                    return true;
                } else if((dir > 0 && child.focusFirstChild()) ||
                        (dir < 0 && child.focusLastChild())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the visible child widget which is at the specified coordinate.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the child widget at that location or null if there is no visible child.
     * @see #getX()
     * @see #getY()
     */
    protected final Widget getChildAt(int x, int y) {
        for(int i=children.size(); i-->0 ;) {
            Widget child = children.get(i);
            if(child.visible && child.isInside(x, y)) {
                return child;
            }
        }
        return null;
    }
    
    //
    // start of internal stuff
    //
    
    private void unparentChild(Widget child) {
        GUI gui = getGUI();
        if(child.hasOpenPopup) { 
            assert(gui != null);
            gui.closePopupFromWidgets(child);
        }
        recursivelyChildFocusLost(this);
        if(gui != null) {
            child.recursivelyRemoveFromGUI(gui);
        }
        child.parent = null;
        child.destroy();
        adjustChildPosition(child, -posX, -posY);
    }

    private void recursivelyAddToGUI(GUI gui) {
        if(layoutInvalid) {
            gui.hasInvalidLayouts = true;
        }
        if(!sharedAnimState) {
            animState.setGUI(gui);
        }
        afterAddToGUI(gui);
        if(children != null) {
            for(int i=children.size() ; i-->0 ;) {
                children.get(i).recursivelyAddToGUI(gui);
            }
        }
    }

    private void recursivelyRemoveFromGUI(GUI gui) {
        if(children != null) {
            for(int i=children.size() ; i-->0 ;) {
                children.get(i).recursivelyRemoveFromGUI(gui);
            }
        }
        focusChild = null;
        if(!sharedAnimState) {
            animState.setGUI(null);
        }
        beforeRemoveFromGUI(gui);
    }

    private void recursivelyChildFocusLost(Widget w) {
        while(w != null) {
            Widget next = w.focusChild;
            if(!w.sharedAnimState) {
                w.animState.setAnimationState(STATE_KEYBOARD_FOCUS, false);
            }
            w.keyboardFocusLost();
            w.focusChild = null;
            w = next;
        }
    }

    private void childHidden(Widget child) {
        if(focusChild == child) {
            recursivelyChildFocusLost(focusChild);
            focusChild = null;
        }
        if(lastChildMouseOver == child) {
            lastChildMouseOver = null;
        }
    }
    
    final void setOpenPopup(GUI gui, boolean hasOpenPopup) {
        if(this.hasOpenPopup != hasOpenPopup) {
            this.hasOpenPopup = hasOpenPopup;
            if(parent != null) {
                if(hasOpenPopup) {
                    parent.setOpenPopup(gui, true);
                } else {
                    parent.recalcOpenPopups(gui);
                }
            }
        }
    }
    
    final void recalcOpenPopups(GUI gui) {
        // 1) check self
        if(gui.hasOpenPopups(this)) {
            setOpenPopup(gui, true);
            return;
        }
        // 2) check children (don't compute, just check the flag)
        if(children != null) {
            for(int i=children.size() ; i-->0 ;) {
                if(children.get(i).hasOpenPopup) {
                    setOpenPopup(gui, true);
                    return;
                }
            }
        }
        setOpenPopup(gui, false);
    }
    
    final void drawWidget(GUI gui) {
        if(clip) {
            gui.clipEnter(posX, posY, width, height);
            try {
                paint(gui);
            } finally {
                gui.clipLeave();
            }
        } else {
            paint(gui);
        }
    }
    
    Widget getWidgetUnderMouse() {
        if(!visible) {
            return null;
        }
        Widget w = this;
        while(w.lastChildMouseOver != null && w.visible) {
            w = w.lastChildMouseOver;
        }
        return w;
    }
    
    private static void adjustChildPosition(Widget child, int deltaX, int deltaY) {
        child.setPosition(child.posX + deltaX, child.posY + deltaY);
    }
    
    void applyTheme(ThemeManager themeManager) {
        this.themeManager = themeManager;
        
        final String themePath = getThemePath();
        if(themePath.length() == 0) {
            if(children != null) {
                for(int i=0,n=children.size() ; i<n ; i++) {
                    children.get(i).applyTheme(themeManager);
                }
            }
            return;
        }
        
        final ThemeInfo themeInfo = themeManager.findThemeInfo(themePath);
        if(themeInfo != null) {
            if(theme.length() > 0) {
                applyTheme(themeInfo);
            }
            applyThemeToChildren(themeManager, themeInfo);
        }
    }

    /**
     * Checks if the given theme name is absolute or relative to it's parent.
     * An absolute theme name starts with a '/'.
     * 
     * @param theme the theme name or path.
     * @return true if the theme is absolute.
     */
    public static boolean isAbsoluteTheme(String theme) {
        return theme.length() > 1 && theme.charAt(0) == '/';
    }

    private void applyThemeImpl(ThemeManager themeManager, ThemeInfo themeInfo) {
        this.themeManager = themeManager;
        if(theme.length() > 0) {
            if(isAbsoluteTheme(theme)) {
                themeInfo = themeManager.findThemeInfo(theme.substring(1));
            } else {
                themeInfo = themeInfo.getChildTheme(theme);
            }
            if(themeInfo == null) {
                return;
            }
            applyTheme(themeInfo);
        }
        applyThemeToChildren(themeManager, themeInfo);
    }
    
    private void applyThemeToChildren(ThemeManager themeManager, ThemeInfo themeInfo) {
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                Widget child = children.get(i);
                child.applyThemeImpl(themeManager, themeInfo);
            }
        }
    }

    private StringBuilder getThemePath(int length) {
        StringBuilder sb;
        length += theme.length();
        boolean abs = isAbsoluteTheme(theme);
        if(parent != null && !abs) {
            sb = parent.getThemePath(length+1);
            if(theme.length() > 0 && sb.length() > 0) {
                sb.append('.');
            }
        } else {
            sb = new StringBuilder(length);
        }
        if(abs) {
            return sb.append(theme.substring(1));
        }
        return sb.append(theme);
    }

    Widget routeMouseEvent(Event evt) {
        assert !evt.isMouseDragEvent();
        if(children != null) {
            for(int i=children.size(); i-->0 ;) {
                Widget child = children.get(i);
                if(child.visible && child.isMouseInside(evt)) {
                    // we send the real event only only if we can transfer the mouse "focus" to this child
                    Widget result = setMouseOverChild(child, evt);
                    if(result != null) {
                        if(evt.getType() == Event.Type.MOUSE_ENTERED ||
                                evt.getType() == Event.Type.MOUSE_EXITED) {
                            return result;
                        }
                        result = result.routeMouseEvent(evt);
                        if(result != null) {
                            return result;
                        }
                        // widget no longer wants mouse events
                    }
                    // found a widget - but it doesn't want mouse events
                    // so assumes it's "invisible" for the mouse
                }
            }
        }
        if(evt.getType() == Event.Type.MOUSE_BTNDOWN && canAcceptKeyboardFocus()) {
            if(focusChild == null) {
                requestKeyboardFocus();
            } else {
                requestKeyboardFocus(null);
            }
        }
        // no child has mouse over
        setMouseOverChild(null, evt);
        if(handleEvent(evt)) {
            return this;
        }
        return null;
    }
    
    boolean routePopupEvent(Event evt) {
        handleEvent(evt);
        for(int i=0,n=children.size() ; i<n ; i++) {
            children.get(i).routePopupEvent(evt);
        }
        return true;
    }

    private boolean handleKeyEvent(Event evt) {
        if(focusKeyEnabled && evt.isKeyEvent() && evt.getKeyCode() == FOCUS_KEY) {
            handleFocusKeyEvent(evt);
            return true;
        }
        if(focusChild != null && focusChild.isVisible()) {
            return focusChild.handleEvent(evt);
        }
        return false;
    }

    private void handleFocusKeyEvent(Event evt) {
        if(evt.getType() == Event.Type.KEY_PRESSED) {
            if((evt.getModifiers() & Event.MODIFIER_SHIFT) != 0) {
                if(!focusPrevChild()) {
                    focusLastChild();
                }
            } else {
                if(!focusNextChild()) {
                    focusFirstChild();
                }
            }
        }
    }

    private Widget setMouseOverChild(Widget child, Event evt) {
        if (lastChildMouseOver != child) {
            Widget result = null;
            if(child != null) {
                result = child.routeMouseEvent(evt.createSubEvent(Event.Type.MOUSE_ENTERED));
                if(result == null) {
                    // this child widget doesn't want mouse events
                    return null;
                }
            }
            if (lastChildMouseOver != null) {
                lastChildMouseOver.routeMouseEvent(evt.createSubEvent(Event.Type.MOUSE_EXITED));
            }
            lastChildMouseOver = child;
        }
        return child;
    }

    void collectLayoutLoop(ArrayList<Widget> result) {
        if(layoutInvalid) {
            result.add(this);
        }
        if(children != null) {
            for(int i=0,n=children.size() ; i<n ; i++) {
                children.get(i).collectLayoutLoop(result);
            }
        }
    }

}
