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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A layout manager similar to Swing's GroupLayout
 *
 * @author Matthias Mann
 */
public class DialogLayout extends Widget {

    /**
     * Symbolic constant to refer to "small gap".
     * @see #getSmallGap()
     * @see Group#addGap(int)
     * @see Group#addGap(int, int, int)
     */
    public static final int SMALL_GAP   = -1;

    /**
     * Symbolic constant to refer to "medium gap".
     * @see #getMediumGap()
     * @see Group#addGap(int)
     * @see Group#addGap(int, int, int)
     */
    public static final int MEDIUM_GAP  = -2;

    /**
     * Symbolic constant to refer to "large gap".
     * @see #getLargeGap()
     * @see Group#addGap(int)
     * @see Group#addGap(int, int, int)
     */
    public static final int LARGE_GAP   = -3;

    /**
     * Symbolic constant to refer to "default gap".
     * The default gap is added (when enabled) between widgets.
     *
     * @see #getDefaultGap()
     * @see #setAddDefaultGaps(boolean)
     * @see #isAddDefaultGaps()
     * @see Group#addGap(int)
     * @see Group#addGap(int, int, int)
     */
    public static final int DEFAULT_GAP = -4;

    protected Dimension smallGap;
    protected Dimension mediumGap;
    protected Dimension largeGap;
    protected Dimension defaultGap;

    protected boolean addDefaultGaps = true;
    protected boolean redoDefaultGaps;

    protected Group horz;
    protected Group vert;
    private final HashMap<Widget, WidgetSpring> widgetSprings;

    public DialogLayout() {
        widgetSprings = new HashMap<Widget, WidgetSpring>();
    }

    public Group getHorizontalGroup() {
        return horz;
    }

    /**
     * The horizontal group control the position and size of all child
     * widgets along the X axis.
     * @param g the group used for the X axis
     */
    public void setHorizontalGroup(Group g) {
        if(g != null) {
            g.checkGroup(this);
        }
        this.horz = g;
        this.redoDefaultGaps = true;
    }

    public Group getVerticalGroup() {
        return vert;
    }

    /**
     * The vertical group control the position and size of all child
     * widgets along the Y axis.
     * @param g the group used for the Y axis
     */
    public void setVerticalGroup(Group g) {
        if(g != null) {
            g.checkGroup(this);
        }
        this.vert = g;
        this.redoDefaultGaps = true;
    }

    public Dimension getSmallGap() {
        return smallGap;
    }

    public void setSmallGap(Dimension smallGap) {
        this.smallGap = smallGap;
        invalidateLayout();
    }

    public Dimension getMediumGap() {
        return mediumGap;
    }

    public void setMediumGap(Dimension mediumGap) {
        this.mediumGap = mediumGap;
        invalidateLayout();
    }

    public Dimension getLargeGap() {
        return largeGap;
    }

    public void setLargeGap(Dimension largeGap) {
        this.largeGap = largeGap;
        invalidateLayout();
    }

    public Dimension getDefaultGap() {
        return defaultGap;
    }

    public void setDefaultGap(Dimension defaultGap) {
        this.defaultGap = defaultGap;
        invalidateLayout();
    }

    public boolean isAddDefaultGaps() {
        return addDefaultGaps;
    }

    /**
     * Determine whether default gaps should be added from the theme or not.
     * 
     * @param addDefaultGaps if true then default gaps are added.
     */
    public void setAddDefaultGaps(boolean addDefaultGaps) {
        this.addDefaultGaps = addDefaultGaps;
    }

    /**
     * removes all default gaps from all groups.
     */
    public void removeDefaultGaps() {
        if(horz != null && vert != null) {
            horz.removeDefaultGaps();
            vert.removeDefaultGaps();
            invalidateLayout();
        }
    }

    /**
     * Adds theme dependant default gaps to all groups.
     */
    public void addDefaultGaps() {
        if(horz != null && vert != null) {
            horz.addDefaultGap();
            vert.addDefaultGap();
            invalidateLayout();
        }
    }

    protected void applyThemeDialogLayout(ThemeInfo themeInfo) {
        setSmallGap(themeInfo.getParameterValue("smallGap", true, Dimension.class));
        setMediumGap(themeInfo.getParameterValue("mediumGap", true, Dimension.class));
        setLargeGap(themeInfo.getParameterValue("largeGap", true, Dimension.class));
        setDefaultGap(themeInfo.getParameterValue("defaultGap", true, Dimension.class));
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeDialogLayout(themeInfo);
    }

    @Override
    public int getMinWidth() {
        if(horz != null) {
            prepare();
            return horz.getMinSize(AXIS_X) + getBorderHorizontal();
        }
        return super.getMinWidth();
    }

    @Override
    public int getMinHeight() {
        if(vert != null) {
            prepare();
            return vert.getMinSize(AXIS_Y) + getBorderVertical();
        }
        return super.getMinHeight();
    }

    @Override
    public int getPreferedInnerWidth() {
        if(horz != null) {
            prepare();
            return horz.getPrefSize(AXIS_X);
        }
        return super.getPreferedInnerWidth();
    }

    @Override
    public int getPreferedInnerHeight() {
        if(vert != null) {
            prepare();
            return vert.getPrefSize(AXIS_Y);
        }
        return super.getPreferedInnerHeight();
    }

    @Override
    public void adjustSize() {
        if(horz != null && vert != null) {
            prepare();
            int minWidth = horz.getMinSize(AXIS_X);
            int minHeight = vert.getMinSize(AXIS_Y);
            int prefWidth = horz.getPrefSize(AXIS_X);
            int prefHeight = vert.getPrefSize(AXIS_Y);
            int maxWidth = getMaxWidth();
            int maxHeight = getMaxHeight();
            setInnerSize(
                    computeSize(minWidth, prefWidth, maxWidth),
                    computeSize(minHeight, prefHeight, maxHeight));
            doLayout();
        }
    }

    @Override
    public void layout() {
        if(horz != null && vert != null) {
            prepare();
            doLayout();
        }
    }
    
    protected void prepare() {
        if(redoDefaultGaps) {
            if(addDefaultGaps) {
                addDefaultGaps();
            }
            redoDefaultGaps = false;
        }
        for(WidgetSpring s : widgetSprings.values()) {
            s.prepare();
        }
    }

    protected void doLayout() {
        horz.setSize(AXIS_X, getInnerX(), getInnerWidth());
        vert.setSize(AXIS_Y, getInnerY(), getInnerHeight());
        for(WidgetSpring s : widgetSprings.values()) {
            s.apply();
        }
        if((getWidth() < horz.getMinSize(AXIS_X) || getHeight() < vert.getMinSize(AXIS_Y))) {
            invalidateParentLayout();
        }
    }

    /**
     * Creates a new parallel group.
     * All childs in a parallel group share the same position and size of it's axis.
     *
     * @return the new parallel Group.
     */
    public Group createParallelGroup() {
        return new ParallelGroup();
    }

    /**
     * Creates a new sequential group.
     * All childs in a sequential group are ordered with increasing coordinates
     * along it's axis in the order they are added to the group. The available
     * size is distributed among the childs depending on their min/prefered/max
     * sizes.
     * 
     * @return a new sequential Group.
     */
    public Group createSequentialGroup() {
        return new SequentialGroup();
    }

    @Override
    public void insertChild(Widget child, int index) throws IndexOutOfBoundsException {
        super.insertChild(child, index);
        widgetSprings.put(child, new WidgetSpring(child));
    }

    @Override
    public void removeAllChilds() {
        super.removeAllChilds();
        widgetSprings.clear();
        if(horz != null) {
            horz.recheckWidgets();
        }
        if(vert != null) {
            vert.recheckWidgets();
        }
    }

    @Override
    public Widget removeChild(int index) throws IndexOutOfBoundsException {
        final Widget widget = super.removeChild(index);
        widgetSprings.remove(widget);
        if(horz != null) {
            horz.recheckWidgets();
        }
        if(vert != null) {
            vert.recheckWidgets();
        }
        return widget;
    }

    private static final int AXIS_X = 0;
    private static final int AXIS_Y = 1;

    private static abstract class Spring {
        abstract int getMinSize(int axis);
        abstract int getPrefSize(int axis);
        abstract int getMaxSize(int axis);
        abstract void setSize(int axis, int pos, int size);

        Spring() {
        }
        
        void collectAllSprings(HashSet<Spring> result) {
            result.add(this);
        }
    }

    private static class WidgetSpring extends Spring {
        final Widget w;
        int x;
        int y;
        int width;
        int height;
        int prefWidth;
        int prefHeight;

        WidgetSpring(Widget w) {
            this.w = w;
        }

        void prepare() {
            this.x = w.getX();
            this.y = w.getY();
            this.width = w.getWidth();
            this.height = w.getHeight();
            this.prefWidth = computeSize(w.getMinWidth(), w.getPreferedWidth(), w.getMaxWidth());
            this.prefHeight = computeSize(w.getMinHeight(), w.getPreferedHeight(), w.getMaxHeight());
        }

        @Override
        int getMinSize(int axis) {
            switch(axis) {
            case AXIS_X: return w.getMinWidth();
            case AXIS_Y: return w.getMinHeight();
            default: throw new IllegalArgumentException("axis");
            }
        }

        @Override
        int getPrefSize(int axis) {
            switch(axis) {
            case AXIS_X: return prefWidth;
            case AXIS_Y: return prefHeight;
            default: throw new IllegalArgumentException("axis");
            }
        }

        @Override
        int getMaxSize(int axis) {
            switch(axis) {
            case AXIS_X: return w.getMaxWidth();
            case AXIS_Y: return w.getMaxHeight();
            default: throw new IllegalArgumentException("axis");
            }
        }

        @Override
        void setSize(int axis, int pos, int size) {
            switch(axis) {
            case AXIS_X:
                this.x = pos;
                this.width = size;
                break;
            case AXIS_Y:
                this.y = pos;
                this.height = size;
                break;
            default:
                throw new IllegalArgumentException("axis");
            }
        }

        void apply() {
            w.setPosition(x, y);
            w.setSize(width, height);
        }
    }

    private class GapSpring extends Spring {
        final int min;
        final int pref;
        final int max;
        final boolean isDefault;

        GapSpring(int min, int pref, int max, boolean isDefault) {
            convertConstant(AXIS_X, min);
            convertConstant(AXIS_X, pref);
            convertConstant(AXIS_X, max);
            this.min = min;
            this.pref = pref;
            this.max = max;
            this.isDefault = isDefault;
        }

        @Override
        int getMinSize(int axis) {
            return convertConstant(axis, min);
        }

        @Override
        int getPrefSize(int axis) {
            return convertConstant(axis, pref);
        }

        @Override
        int getMaxSize(int axis) {
            return convertConstant(axis, max);
        }

        @Override
        void setSize(int axis, int pos, int size) {
        }

        private int convertConstant(int axis, int value) {
            if(value >= 0) {
                return value;
            }
            Dimension dim = null;
            switch(value) {
            case SMALL_GAP:
                dim = smallGap;
                break;
            case MEDIUM_GAP:
                dim = mediumGap;
                break;
            case LARGE_GAP:
                dim = largeGap;
                break;
            case DEFAULT_GAP:
                dim = defaultGap;
                break;
            default:
                throw new IllegalArgumentException("Invalid gap size: " + value);
            }
            if(dim == null) {
                return 0;
            } else if(axis == AXIS_X) {
                return dim.getX();
            } else {
                return dim.getY();
            }
        }
    }

    public abstract class Group extends Spring {
        final ArrayList<Spring> springs = new ArrayList<Spring>();
        boolean alreadyAdded;

        void checkGroup(DialogLayout owner) {
            if(getDialogLayout() != owner) {
                throw new IllegalArgumentException("Can't add group from different layout");
            }
            if(alreadyAdded) {
                throw new IllegalArgumentException("Group already added to another group");
            }
        }

        /**
         * Adds another group. A group can only be added once.
         *
         * WARNING: No check is made to prevent cycles.
         * 
         * @param g the child Group
         * @return this Group
         */
        public Group addGroup(Group g) {
            g.checkGroup(getDialogLayout());
            g.alreadyAdded = true;
            addSpring(g);
            return this;
        }

        /**
         * Addsa  widget to this group. The widget is automaticly added as child widget.
         *
         * @param w the child widget.
         * @return this Group
         */
        public Group addWidget(Widget w) {
            if(w.getParent() != getDialogLayout()) {
                getDialogLayout().add(w);
            }
            WidgetSpring s = widgetSprings.get(w);
            if(s == null) {
                throw new IllegalStateException("WidgetSpring for Widget not found: " + w);
            }
            addSpring(s);
            return this;
        }

        /**
         * Adds a generic gap. Can use symbolic gap names.
         *
         * @param min the minimum size in pixels or a symbolic constant
         * @param pref the prefered size in pixels or a symbolic constant
         * @param max the maximum size in pixels or a symbolic constant
         * @return this Group
         * @see DialogLayout#SMALL_GAP
         * @see DialogLayout#MEDIUM_GAP
         * @see DialogLayout#LARGE_GAP
         * @see DialogLayout#DEFAULT_GAP
         */
        public Group addGap(int min, int pref, int max) {
            addSpring(new GapSpring(min, pref, max, false));
            return this;
        }

        /**
         * Adds a fixed sized gap. Can use symbolic gap names.
         *
         * @param size the size in pixels or a symbolic constant
         * @return this Group
         * @see DialogLayout#SMALL_GAP
         * @see DialogLayout#MEDIUM_GAP
         * @see DialogLayout#LARGE_GAP
         * @see DialogLayout#DEFAULT_GAP
         */
        public Group addGap(int size) {
            addSpring(new GapSpring(size, size, size, false));
            return this;
        }

        /**
         * Adds a gap with minimum size. Can use symbolic gap names.
         *
         * @param minSize the minimum size in pixels or a symbolic constant
         * @return this Group
         * @see DialogLayout#SMALL_GAP
         * @see DialogLayout#MEDIUM_GAP
         * @see DialogLayout#LARGE_GAP
         * @see DialogLayout#DEFAULT_GAP
         */
        public Group addMinGap(int minSize) {
            addSpring(new GapSpring(minSize, minSize, Short.MAX_VALUE, false));
            return this;
        }

        /**
         * Adds a flexible gap with no minimum size.
         *
         * @return this Group
         */
        public Group addGap() {
            addSpring(new GapSpring(0, 0, Short.MAX_VALUE, false));
            return this;
        }

        /**
         * Remove all default gaps from this and child groups
         */
        public void removeDefaultGaps() {
            for(int i=springs.size() ; i-->0 ;) {
                Spring s = springs.get(i);
                if(s instanceof GapSpring) {
                    if(((GapSpring)s).isDefault) {
                        springs.remove(i);
                    }
                } else if(s instanceof Group) {
                    ((Group)s).removeDefaultGaps();
                }
            }
        }

        /**
         * Add a default gap between all childs except if the neighbour is already a Gap.
         */
        public void addDefaultGap() {
            for(int i=0 ; i<springs.size() ; i++) {
                Spring s = springs.get(i);
                if(s instanceof Group) {
                    ((Group)s).addDefaultGap();
                }
            }
        }

        private void addSpring(Spring s) {
            springs.add(s);
            getDialogLayout().redoDefaultGaps = true;
        }

        protected DialogLayout getDialogLayout() {
            return DialogLayout.this;
        }

        void recheckWidgets() {
            for(int i=springs.size() ; i-->0 ;) {
                Spring s = springs.get(i);
                if(s instanceof WidgetSpring) {
                    if(!widgetSprings.containsKey(((WidgetSpring)s).w)) {
                        springs.remove(i);
                    }
                } else if(s instanceof Group) {
                    ((Group)s).recheckWidgets();
                }
            }
        }
    }

    static class SpringDelta implements Comparable<SpringDelta> {
        final int idx;
        final int delta;

        SpringDelta(int idx, int delta) {
            this.idx = idx;
            this.delta = delta;
        }

        public int compareTo(SpringDelta o) {
            return delta - o.delta;
        }
    }

    class SequentialGroup extends Group {
        SequentialGroup() {
        }

        @Override
        int getMinSize(int axis) {
            int size = 0;
            for(int i=0,n=springs.size() ; i<n ; i++) {
                size += springs.get(i).getMinSize(axis);
            }
            return size;
        }

        @Override
        int getPrefSize(int axis) {
            int size = 0;
            for(int i=0,n=springs.size() ; i<n ; i++) {
                size += springs.get(i).getPrefSize(axis);
            }
            return size;
        }

        @Override
        int getMaxSize(int axis) {
            return 0;
        }
        
        /**
         * Add a default gap between all childs except if the neighbour is already a Gap.
         */
        @Override
        public void addDefaultGap() {
            if(springs.size() > 1) {
                boolean wasGap = true;
                for(int i=0 ; i<springs.size() ; i++) {
                    Spring s = springs.get(i);
                    boolean isGap = s instanceof GapSpring;
                    if(!isGap && !wasGap) {
                        springs.add(i++, new GapSpring(DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP, true));
                    }
                    wasGap = isGap;
                }
            }
            super.addDefaultGap();
        }

        @Override
        void setSize(int axis, int pos, int size) {
            int prefSize = getPrefSize(axis);
            if(size == prefSize) {
                for(Spring s : springs) {
                    int spref = s.getPrefSize(axis);
                    s.setSize(axis, pos, spref);
                    pos += spref;
                }
            } else if(springs.size() == 1) {
                Spring s = springs.get(0);
                s.setSize(axis, pos, size);
            } else if(springs.size() > 1) {
                setSizeNonPref(axis, pos, size, prefSize);
            }
        }

        private void setSizeNonPref(int axis, int pos, int size, int prefSize) {
            int delta = size - prefSize;
            boolean useMin = delta < 0;
            if(useMin) {
                delta = -delta;
            }

            SpringDelta[] deltas = new SpringDelta[springs.size()];
            int resizeable = 0;
            for(int i=0 ; i<springs.size() ; i++) {
                Spring s = springs.get(i);
                int sdelta = useMin
                        ? s.getPrefSize(axis) - s.getMinSize(axis)
                        : s.getMaxSize(axis) - s.getPrefSize(axis);
                if(sdelta > 0)  {
                    deltas[resizeable++] = new SpringDelta(i, sdelta);
                }
            }
            if(resizeable > 0) {
                Arrays.sort(deltas, 0, resizeable);
                int sdelta = delta / resizeable;
                int rest = delta - sdelta * resizeable;
                int sizes[] = new int[springs.size()];

                for(int i=0 ; i<resizeable ; i++) {
                    SpringDelta d = deltas[i];
                    if(i+1 == resizeable) {
                        // last one gets all
                        sdelta += rest;
                    }
                    int ddelta = Math.min(d.delta, sdelta);
                    delta -= ddelta;
                    if(ddelta != sdelta && i+1 < resizeable) {
                        int remaining = resizeable - i - 1;
                        sdelta = delta / remaining;
                        rest = delta - sdelta * remaining;
                    }
                    if(useMin) {
                        ddelta = -ddelta;
                    }
                    sizes[d.idx] = ddelta;
                }

                for(int i=0 ; i<springs.size() ; i++) {
                    Spring s = springs.get(i);
                    int ssize = s.getPrefSize(axis) + sizes[i];
                    s.setSize(axis, pos, ssize);
                    pos += ssize;
                }
            } else {
                for(Spring s : springs) {
                    int ssize;
                    if(useMin) {
                        ssize = s.getMinSize(axis);
                    } else {
                        ssize = s.getMaxSize(axis);
                        if(ssize == 0) {
                            ssize = s.getPrefSize(axis);
                        }
                    }
                    s.setSize(axis, pos, ssize);
                    pos += ssize;
                }
            }
        }
    }

    class ParallelGroup extends Group {
        ParallelGroup() {
        }

        @Override
        int getMinSize(int axis) {
            int size = 0;
            for(int i=0,n=springs.size() ; i<n ; i++) {
                size = Math.max(size, springs.get(i).getMinSize(axis));
            }
            return size;
        }

        @Override
        int getPrefSize(int axis) {
            int size = 0;
            for(int i=0,n=springs.size() ; i<n ; i++) {
                size = Math.max(size, springs.get(i).getPrefSize(axis));
            }
            return size;
        }

        @Override
        int getMaxSize(int axis) {
            int size = 0;
            for(int i=0,n=springs.size() ; i<n ; i++) {
                size = Math.max(size, springs.get(i).getMaxSize(axis));
            }
            return size;
        }

        @Override
        void setSize(int axis, int pos, int size) {
            for(int i=0,n=springs.size() ; i<n ; i++) {
                springs.get(i).setSize(axis, pos, size);
            }
        }
    }
}
