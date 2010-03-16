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

import de.matthiasmann.twl.model.BooleanModel;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Matthias Mann
 */
public class Menu extends MenuElement implements Iterable<MenuElement> {

    public static final String STATE_HAS_OPEN_MENUS = "hasOpenMenus";
    
    private final ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
    private String popupTheme;

    public Menu() {
    }

    public Menu(String name) {
        super(name);
    }

    public String getPopupTheme() {
        return popupTheme;
    }

    public void setPopupTheme(String popupTheme) {
        String oldPopupTheme = this.popupTheme;
        this.popupTheme = popupTheme;
        firePropertyChange("popupTheme", oldPopupTheme, this.popupTheme);
    }

    public Iterator<MenuElement> iterator() {
        return elements.iterator();
    }

    public MenuElement get(int index) {
        return elements.get(index);
    }

    public int getNumElements() {
        return elements.size();
    }
    
    public void clear() {
        elements.clear();
    }

    public Menu add(MenuElement e) {
        elements.add(e);
        return this;
    }

    public Menu add(String name, Runnable cb) {
        return add(new MenuAction(name, cb));
    }

    public Menu add(String name, BooleanModel model) {
        return add(new MenuCheckbox(name, model));
    }
    
    public Menu addSpacer() {
        return add(new MenuSpacer());
    }
    
    @Override
    protected Widget createMenuWidget(MenuManager mm, int level) {
        SubMenuBtn smb = new SubMenuBtn(mm, level);
        smb.setEnabled(isEnabled());
        setWidgetTheme(smb, "submenu");
        return smb;
    }

    public void createMenuBar(Widget container) {
        MenuManager mm = new MenuManager(container, true);
        for(Widget w : createWidgets(mm, 0)) {
            container.add(w);
        }
    }

    public Widget createMenuBar() {
        DialogLayout l = new DialogLayout();
        setWidgetTheme(l, "menubar");

        MenuManager mm = new MenuManager(l, true);
        Widget[] widgets = createWidgets(mm, 0);

        l.setHorizontalGroup(l.createSequentialGroup().addWidgetsWithGap("menuitem", widgets));
        l.setVerticalGroup(l.createParallelGroup(widgets));

        l.getHorizontalGroup().addGap();
        return l;
    }

    public MenuManager openPopupMenu(Widget parent) {
        MenuManager mm = new MenuManager(parent, false);
        mm.openSubMenu(0, this, parent, true);
        return mm;
    }
    
    public MenuManager openPopupMenu(Widget parent, int x, int y) {
        MenuManager mm = new MenuManager(parent, false);
        Widget popup = mm.openSubMenu(0, this, parent, false);
        if(popup != null) {
            popup.setPosition(x, y);
        }
        return mm;
    }

    private Widget[] createWidgets(MenuManager mm, int level) {
        Widget[] widgets = new Widget[elements.size()];
        for(int i=0,n=elements.size() ; i<n ; i++) {
            MenuElement e = elements.get(i);
            widgets[i] = e.createMenuWidget(mm, level);
        }
        return widgets;
    }

    DialogLayout createPopup(MenuManager mm, int level, Widget btn) {
        Widget[] widgets = createWidgets(mm, level);
        MenuPopup popup = new MenuPopup(btn);
        if(popupTheme != null) {
            popup.setTheme(popupTheme);
        }
        popup.setHorizontalGroup(popup.createParallelGroup(widgets));
        popup.setVerticalGroup(popup.createSequentialGroup().addWidgetsWithGap("menuitem", widgets));
        return popup;
    }

    static class MenuPopup extends DialogLayout {
        private final Widget btn;

        MenuPopup(Widget btn) {
            this.btn = btn;
        }

        @Override
        protected void afterAddToGUI(GUI gui) {
            super.afterAddToGUI(gui);
            btn.getAnimationState().setAnimationState(STATE_HAS_OPEN_MENUS, true);
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            btn.getAnimationState().setAnimationState(STATE_HAS_OPEN_MENUS, false);
            super.beforeRemoveFromGUI(gui);
        }
    }
    
    class SubMenuBtn extends MenuBtn implements Runnable {
        private final MenuManager mm;
        private final int level;
        private Timer timer;

        public SubMenuBtn(MenuManager mm, int level) {
            this.mm = mm;
            this.level = level;
            
            addCallback(this);
        }

        @Override
        public boolean handleEvent(Event evt) {
            if(evt.getType() == Event.Type.MOUSE_ENTERED) {
                if((level > 0 || mm.isOpen()) && !mm.isSubMenuOpen(Menu.this)) {
                    startTimer();
                }
            }
            if(evt.getType() == Event.Type.MOUSE_EXITED) {
                stopTimer();
            }
            return super.handleEvent(evt);
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            stopTimer();
            timer = null;
            super.beforeRemoveFromGUI(gui);
        }

        private void startTimer() {
            if(timer == null) {
                GUI gui = getGUI();
                if(gui == null) {
                    return;
                }
                timer = gui.createTimer();
            }
            timer.setCallback(this);
            timer.setDelay(300);
            timer.start();
        }

        private void stopTimer() {
            if(timer != null) {
                timer.stop();
            }
        }

        public void run() {
            stopTimer();
            mm.openSubMenu(level, Menu.this, this, true);
        }
    }
}
