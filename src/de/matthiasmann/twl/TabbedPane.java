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
import de.matthiasmann.twl.model.HasCallback;
import java.util.ArrayList;

/**
 * A simple tabbed pane
 * 
 * @author Matthias Mann
 */
public class TabbedPane extends Widget {

    public static final String STATE_FIRST_TAB = "firstTab";
    public static final String STATE_LAST_TAB = "lastTab";
    
    public enum TabPosition {
        TOP(true),
        LEFT(false),
        RIGHT(true),
        BOTTOM(false);

        final boolean horz;
        private TabPosition(boolean horz) {
            this.horz = horz;
        }
    }
    
    private final ArrayList<Tab> tabs;
    private final BoxLayout tabBox;
    final Container container;

    TabPosition tabPosition;
    Tab activeTab;

    public TabbedPane() {
        this.tabs = new ArrayList<Tab>();
        this.tabBox = new BoxLayout();
        this.container = new Container();
        this.tabPosition = TabPosition.TOP;

        tabBox.setTheme("tabbox");
        
        super.insertChild(container, 0);
        super.insertChild(tabBox, 1);

        addActionMapping("nextTab", "cycleTabs", +1);
        addActionMapping("prevTab", "cycleTabs", -1);
    }

    public TabPosition getTabPosition() {
        return tabPosition;
    }

    public void setTabPosition(TabPosition tabPosition) {
        if(tabPosition == null) {
            throw new NullPointerException("tabPosition");
        }
        if(this.tabPosition != tabPosition) {
            this.tabPosition = tabPosition;
            tabBox.setDirection(tabPosition.horz
                    ? BoxLayout.Direction.HORIZONTAL
                    : BoxLayout.Direction.VERTICAL);
            invalidateLayout();
        }
    }

    public Tab addTab(String title, Widget pane) {
        Tab tab = new Tab();
        tab.setTitle(title);
        tab.setPane(pane);
        tabBox.add(tab.button);
        tabs.add(tab);

        if(tabs.size() == 1) {
            setActiveTab(tab);
        }
        updateTabStates();
        return tab;
    }

    public Tab getActiveTab() {
        return activeTab;
    }
    
    public void setActiveTab(Tab tab) {
        if(tab != null) {
            validateTab(tab);
        }
        
        if(activeTab != tab) {
            Tab prevTab = activeTab;
            activeTab = tab;

            if(prevTab != null) {
                prevTab.doCallback();
            }
            if(tab != null) {
                tab.doCallback();
            }
        }
    }

    public void removeTab(Tab tab) {
        validateTab(tab);

        int idx = (tab == activeTab) ? tabs.indexOf(tab) : -1;
        tab.setPane(null);
        tabBox.removeChild(tab.button);
        tabs.remove(tab);

        if(idx >= 0 && !tabs.isEmpty()) {
            setActiveTab(tabs.get(Math.min(tabs.size()-1, idx)));
        }
        updateTabStates();
    }

    public void cycleTabs(int direction) {
        if(!tabs.isEmpty()) {
            int idx = tabs.indexOf(activeTab);
            if(idx < 0) {
                idx = 0;
            } else {
                idx += direction;
                idx %= tabs.size();
                idx += tabs.size();
                idx %= tabs.size();
            }
            setActiveTab(tabs.get(idx));
        }
    }

    @Override
    public int getMinWidth() {
        int minWidth;
        if(tabPosition.horz) {
            minWidth = Math.max(container.getMinWidth(), tabBox.getMinWidth());
        } else {
            minWidth = container.getMinWidth() + tabBox.getMinWidth();
        }
        return Math.max(super.getMinWidth(), minWidth + getBorderHorizontal());
    }

    @Override
    public int getMinHeight() {
        int minHeight;
        if(tabPosition.horz) {
            minHeight = container.getMinHeight() + tabBox.getMinHeight();
        } else {
            minHeight = Math.max(container.getMinHeight(), tabBox.getMinHeight());
        }
        return Math.max(super.getMinHeight(), minHeight + getBorderVertical());
    }

    @Override
    public int getPreferredInnerWidth() {
        if(tabPosition.horz) {
            return Math.max(container.getPreferredWidth(), tabBox.getPreferredWidth());
        } else {
            return container.getPreferredWidth() + tabBox.getPreferredWidth();
        }
    }

    @Override
    public int getPreferredInnerHeight() {
        if(tabPosition.horz) {
            return container.getPreferredHeight() + tabBox.getPreferredHeight();
        } else {
            return Math.max(container.getPreferredHeight(), tabBox.getPreferredHeight());
        }
    }

    @Override
    protected void layout() {
        tabBox.adjustSize();
        
        switch(tabPosition) {
            case TOP:
                tabBox.setPosition(getInnerX(), getInnerY());
                container.setSize(getInnerWidth(), getInnerHeight() - tabBox.getHeight());
                container.setPosition(getInnerX(), tabBox.getBottom());
                break;

            case LEFT:
                tabBox.setPosition(getInnerX(), getInnerY());
                container.setSize(getInnerWidth() - tabBox.getWidth(), getInnerHeight());
                container.setPosition(tabBox.getRight(), getInnerY());
                break;

            case RIGHT:
                tabBox.setPosition(getInnerX() - tabBox.getWidth(), getInnerY());
                container.setSize(getInnerWidth() - tabBox.getWidth(), getInnerHeight());
                container.setPosition(getInnerX(), getInnerY());
                break;

            case BOTTOM:
                tabBox.setPosition(getInnerX(), getInnerY() - tabBox.getHeight());
                container.setSize(getInnerWidth(), getInnerHeight() - tabBox.getHeight());
                container.setPosition(getInnerX(), getInnerY());
                break;
        }
    }

    @Override
    public void insertChild(Widget child, int index) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("use addTab/removeTab");
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException("use addTab/removeTab");
    }

    @Override
    public Widget removeChild(int index) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("use addTab/removeTab");
    }

    protected void updateTabStates() {
        for(int i=0,n=tabs.size() ; i<n ; i++) {
            Tab tab = tabs.get(i);
            AnimationState animationState = tab.button.getAnimationState();
            animationState.setAnimationState(STATE_FIRST_TAB, i == 0);
            animationState.setAnimationState(STATE_LAST_TAB, i == n-1);
        }
    }
    
    private void validateTab(Tab tab) {
        if(tab.button.getParent() != tabBox) {
            throw new IllegalArgumentException("Invalid tab");
        }
    }

    public class Tab extends HasCallback implements BooleanModel {
        private final ToggleButton button;
        private Widget pane;

        Tab() {
            button = new ToggleButton(this);
            button.setTheme("tabbutton");
        }

        public boolean getValue() {
            return activeTab == this;
        }

        public void setValue(boolean value) {
            if(value) {
                setActiveTab(this);
            }
        }

        public Widget getPane() {
            return pane;
        }

        public void setPane(Widget pane) {
            if(this.pane != pane) {
                if(this.pane != null) {
                    container.removeChild(this.pane);
                }
                this.pane = pane;
                if(pane != null) {
                    pane.setVisible(getValue());
                    container.add(pane);
                }
            }
        }

        public Tab setTitle(String title) {
            button.setText(title);
            return this;
        }

        public Tab setTheme(String theme) {
            button.setTheme(theme);
            return this;
        }

        @Override
        protected void doCallback() {
            if(pane != null) {
                pane.setVisible(getValue());
            }
            super.doCallback();
        }
    }

    private static class Container extends Widget {
        public Container() {
            setClip(true);
        }

        @Override
        public int getMinWidth() {
            int minWidth = 0;
            for(int i=0,n=getNumChildren() ; i<n ; i++) {
                minWidth = Math.max(minWidth, getChild(i).getMinWidth());
            }
            return Math.max(super.getMinWidth(), minWidth + getBorderHorizontal());
        }

        @Override
        public int getMinHeight() {
            int minHeight = 0;
            for(int i=0,n=getNumChildren() ; i<n ; i++) {
                minHeight = Math.max(minHeight, getChild(i).getMinHeight());
            }
            return Math.max(super.getMinHeight(), minHeight + getBorderVertical());
        }

        @Override
        public int getPreferredInnerWidth() {
            int prefWidth = 0;
            for(int i=0,n=getNumChildren() ; i<n ; i++) {
                prefWidth = Math.max(prefWidth, getChild(i).getPreferredWidth());
            }
            return prefWidth;
        }

        @Override
        public int getPreferredInnerHeight() {
            int prefHeight = 0;
            for(int i=0,n=getNumChildren() ; i<n ; i++) {
                prefHeight = Math.max(prefHeight, getChild(i).getPreferredHeight());
            }
            return prefHeight;
        }

        @Override
        protected void layout() {
            layoutChildrenFullInnerArea();
        }
    }
}
