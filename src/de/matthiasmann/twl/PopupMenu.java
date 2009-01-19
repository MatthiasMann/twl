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

/**
 * A popup menu.
 *
 * @author Matthias Mann
 */
public class PopupMenu extends PopupWindow {

    private final Runnable closeMenuCallback;
    
    public PopupMenu(Widget owner) {
        super(owner);
        this.closeMenuCallback = new Runnable() {
            public void run() {
                closeAllPopupMenus();
            }
        };
    }

    public boolean showPopup(int x, int y) {
        setPosition(x, y);
        if(openPopup()) {
            layout();
            return true;
        }
        return false;
    }

    public void addSpacer() {
        Widget w = new Widget();
        w.setTheme("spacer");
        add(w);
    }
    
    @Override
    public void insertChild(Widget child, int index) throws IndexOutOfBoundsException {
        if((child instanceof Button) && !(child instanceof SubMenu)) {
            ((Button)child).addCallback(closeMenuCallback);
        }
        super.insertChild(child, index);
    }

    @Override
    public void removeAllChilds() {
        for(int i=getNumChilds() ; i-->0 ;) {
            final Widget child = getChild(i);
            removeCallbackFromWidget(child);
        }
        super.removeAllChilds();
    }

    @Override
    public Widget removeChild(int idx) {
        Widget child = super.removeChild(idx);
        removeCallbackFromWidget(child);
        return child;
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        adjustSize();
        //layout();
    }

    @Override
    protected void sizeChanged() {
        //layout();
    }

    @Override
    protected void borderChanged() {
        //layout();
    }

    @Override
    protected void mouseClickedOutside(Event evt) {
        PopupMenu m = this;
        while(!m.isInside(evt.getMouseX(), evt.getMouseY())) {
            m.closePopup();
            Widget o = m.getOwner();
            if(o instanceof SubMenu) {
                m = ((SubMenu)o).getParentPopupMenu();
            } else {
                break;
            }
        }
    }

    @Override
    protected void layout() {
        int minWidth = 0;
        int minHeight = 0;
        for(int i=0 ; i<getNumChilds() ; i++) {
            Widget child = getChild(i);
            minWidth = Math.max(minWidth, child.getWidth());
            minHeight += child.getHeight();
        }
        if(getBackground() != null) {
            minWidth = Math.max(minWidth, getBackground().getWidth());
            minHeight = Math.max(minHeight, getBackground().getHeight());
        }
        int x = getInnerX();
        int y = getInnerY();
        for(int i=0 ; i<getNumChilds() ; i++) {
            Widget child = getChild(i);
            int childHeight = child.getHeight();
            child.setSize(minWidth, childHeight);
            child.setPosition(x, y);
            y += childHeight;
        }
        setInnerSize(minWidth, y - getInnerY());
    }
    
    void closeAllPopupMenus() {
        PopupMenu m = this;
        for(;;) {
            m.closePopup();
            Widget o = m.getOwner();
            if(o instanceof SubMenu) {
                m = ((SubMenu)o).getParentPopupMenu();
            } else {
                break;
            }
        }
    }

    private void removeCallbackFromWidget(final Widget child) {
        if(child instanceof Button) {
            ((Button)child).removeCallback(closeMenuCallback);
        }
    }
}
