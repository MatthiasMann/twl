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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A layout manager similar to Swing's GroupLayout
 *
 * This layout manager uses two independant layout groups:
 *   one for the horizontal axis
 *   one for the vertical axis.
 * Every widget must be added to both the horizontal and the vertical group.
 *
 * When a widget is added to a group it will also be added as a child widget
 * if it was not already added. You can add widgets to DialogLayout before
 * adding them to a group to set the focus order.
 *
 * There are two kinds of groups:
 *   a sequential group which which behaves similar to BoxLayout
 *   a parallel group which alignes the start and size of each child
 *
 * Groups can be cascaded as a tree without restrictions.
 *
 * It is also possible to add widgets to DialogLayout without adding them
 * to the layout groups. These widgets are then not touched by DialogLayout's
 * layout system.
 *
 * When a widget is only added to either the horizontal or vertical groups
 * and not both, then an IllegalStateException exception is created on layout.
 *
 * To help debugging the group construction you can set the system property
 * "debugLayoutGroups" to "true" which will collect additional stack traces
 * to help locate the source of the error.
 *
 * @author Matthias Mann
 * @see #createParallelGroup() 
 * @see #createSequentialGroup()
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

    private static final boolean DEBUG_LAYOUT_GROUPS = Boolean.getBoolean("debugLayoutGroups");
    
    protected Dimension smallGap;
    protected Dimension mediumGap;
    protected Dimension largeGap;
    protected Dimension defaultGap;
    protected ParameterMap namedGaps;

    protected boolean addDefaultGaps = true;
    protected boolean redoDefaultGaps;

    protected Group horz;
    protected Group vert;

    /**
     * Debugging aid. Captures the stack trace where one of the group was last assigned.
     */
    Throwable debugStackTrace;

    final HashMap<Widget, WidgetSpring> widgetSprings;

    public DialogLayout() {
        widgetSprings = new HashMap<Widget, WidgetSpring>();
        collectDebugStack();
    }

    public Group getHorizontalGroup() {
        return horz;
    }

    /**
     * The horizontal group control the position and size of all child
     * widgets along the X axis.
     *
     * Every widget must be part of both horizontal and vertical group.
     * Otherwise a IllegalStateException is thrown at layout time.
     *
     * @param g the group used for the X axis
     */
    public void setHorizontalGroup(Group g) {
        if(g != null) {
            g.checkGroup(this);
        }
        this.horz = g;
        this.redoDefaultGaps = true;
        collectDebugStack();
    }

    public Group getVerticalGroup() {
        return vert;
    }

    /**
     * The vertical group control the position and size of all child
     * widgets along the Y axis.
     *
     * Every widget must be part of both horizontal and vertical group.
     * Otherwise a IllegalStateException is thrown at layout time.
     *
     * @param g the group used for the Y axis
     */
    public void setVerticalGroup(Group g) {
        if(g != null) {
            g.checkGroup(this);
        }
        this.vert = g;
        this.redoDefaultGaps = true;
        collectDebugStack();
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

    private void collectDebugStack() {
        if(DEBUG_LAYOUT_GROUPS) {
            debugStackTrace = new Throwable("DialogLayout created/used here").fillInStackTrace();
        }
    }

    protected void applyThemeDialogLayout(ThemeInfo themeInfo) {
        setSmallGap(themeInfo.getParameterValue("smallGap", true, Dimension.class, Dimension.ZERO));
        setMediumGap(themeInfo.getParameterValue("mediumGap", true, Dimension.class, Dimension.ZERO));
        setLargeGap(themeInfo.getParameterValue("largeGap", true, Dimension.class, Dimension.ZERO));
        setDefaultGap(themeInfo.getParameterValue("defaultGap", true, Dimension.class, Dimension.ZERO));
        namedGaps = themeInfo.getParameterMap("namedGaps");
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
    public int getPreferredInnerWidth() {
        if(horz != null) {
            prepare();
            return horz.getPrefSize(AXIS_X);
        }
        return super.getPreferredInnerWidth();
    }

    @Override
    public int getPreferredInnerHeight() {
        if(vert != null) {
            prepare();
            return vert.getPrefSize(AXIS_Y);
        }
        return super.getPreferredInnerHeight();
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
        try{
            for(WidgetSpring s : widgetSprings.values()) {
                s.apply();
            }
        }catch(IllegalStateException ex) {
            if(debugStackTrace != null && ex.getCause() == null) {
                ex.initCause(debugStackTrace);
            }
            throw ex;
        }
        if((getWidth() < horz.getMinSize(AXIS_X) || getHeight() < vert.getMinSize(AXIS_Y))) {
            invalidateParentLayout();
        }
    }

    /**
     * Creates a new parallel group.
     * All children in a parallel group share the same position and size of it's axis.
     *
     * @return the new parallel Group.
     */
    public Group createParallelGroup() {
        return new ParallelGroup();
    }

    /**
     * Creates a parallel group and adds the specified widgets.
     *
     * @see #createParallelGroup()
     * @param widgets the widgets to add
     * @return a new parallel Group.
     */
    public Group createParallelGroup(Widget ... widgets) {
        return createParallelGroup().addWidgets(widgets);
    }

    /**
     * Creates a parallel group and adds the specified groups.
     *
     * @see #createParallelGroup()
     * @param groups the groups to add
     * @return a new parallel Group.
     */
    public Group createParallelGroup(Group ... groups) {
        return createParallelGroup().addGroups(groups);
    }

    /**
     * Creates a new sequential group.
     * All children in a sequential group are ordered with increasing coordinates
     * along it's axis in the order they are added to the group. The available
     * size is distributed among the children depending on their min/preferred/max
     * sizes.
     * 
     * @return a new sequential Group.
     */
    public Group createSequentialGroup() {
        return new SequentialGroup();
    }

    /**
     * Creates a sequential group and adds the specified widgets.
     *
     * @see #createSequentialGroup()
     * @param widgets the widgets to add
     * @return a new sequential Group.
     */
    public Group createSequentialGroup(Widget ... widgets) {
        return createSequentialGroup().addWidgets(widgets);
    }

    @Override
    public void insertChild(Widget child, int index) throws IndexOutOfBoundsException {
        super.insertChild(child, index);
        widgetSprings.put(child, new WidgetSpring(child));
    }

    @Override
    public void removeAllChildren() {
        super.removeAllChildren();
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

    public static class Gap {
        public final int min;
        public final int preferred;
        public final int max;

        public Gap() {
            this(0,0,32767);
        }
        public Gap(int size) {
            this(size, size, size);
        }
        public Gap(int min, int preferred) {
            this(min, preferred, 32767);
        }
        public Gap(int min, int preferred, int max) {
            if(min < 0) {
                throw new IllegalArgumentException("min");
            }
            if(preferred < min) {
                throw new IllegalArgumentException("preferred");
            }
            if(max < 0 || (max > 0 && max < preferred)) {
                throw new IllegalArgumentException("max");
            }
            this.min = min;
            this.preferred = preferred;
            this.max = max;
        }
    }
    
    static final int AXIS_X = 0;
    static final int AXIS_Y = 1;

    static abstract class Spring {
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
        int flags;

        WidgetSpring(Widget w) {
            this.w = w;
        }

        void prepare() {
            this.x = w.getX();
            this.y = w.getY();
            this.width = w.getWidth();
            this.height = w.getHeight();
            this.prefWidth = computeSize(w.getMinWidth(), w.getPreferredWidth(), w.getMaxWidth());
            this.prefHeight = computeSize(w.getMinHeight(), w.getPreferredHeight(), w.getMaxHeight());
            this.flags = 0;
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
            this.flags |= 1 << axis;
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
            if(flags != 3) {
                invalidState();
            }
            w.setPosition(x, y);
            w.setSize(width, height);
        }

        void invalidState() {
            StringBuilder sb = new StringBuilder();
            sb.append("Widget ").append(w)
                    .append(" with theme ").append(w.getTheme())
                    .append(" is not part of the following groups:");
            if((flags & (1 << AXIS_X)) == 0) {
                sb.append(" horizontal");
            }
            if((flags & (1 << AXIS_Y)) == 0) {
                sb.append(" vertical");
            }
            throw new IllegalStateException(sb.toString());
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
            Dimension dim;
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

    private static final Gap NO_GAP = new Gap(0,0,32767);

    private class NamedGapSpring extends Spring {
        final String name;

        public NamedGapSpring(String name) {
            this.name = name;
        }

        @Override
        int getMaxSize(int axis) {
            return getGap().max;
        }

        @Override
        int getMinSize(int axis) {
            return getGap().min;
        }

        @Override
        int getPrefSize(int axis) {
            return getGap().preferred;
        }

        @Override
        void setSize(int axis, int pos, int size) {
        }

        private Gap getGap() {
            if(namedGaps != null) {
                return namedGaps.getParameterValue(name, true, Gap.class, NO_GAP);
            }
            return NO_GAP;
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
         * Adds several groups. A group can only be added once.
         *
         * WARNING: No check is made to prevent cycles.
         *
         * @param groups the groups to add
         * @return this Group
         */
        public Group addGroups(Group ... groups) {
            for(Group g : groups) {
                addGroup(g);
            }
            return this;
        }

        /**
         * Adds a widget to this group. The widget is automaticly added as child widget.
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
         * Adds several widgets to this group. The widget is automaticly added as child widget.
         * 
         * @param widgets The widgets which should be added.
         * @return this Group
         */
        public Group addWidgets(Widget ... widgets) {
            for(Widget w : widgets) {
                addWidget(w);
            }
            return this;
        }

        /**
         * Adds a generic gap. Can use symbolic gap names.
         *
         * @param min the minimum size in pixels or a symbolic constant
         * @param pref the preferred size in pixels or a symbolic constant
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

        public Group addGap(String name) {
            if(name.length() == 0) {
                throw new IllegalArgumentException("name");
            }
            addSpring(new NamedGapSpring(name));
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
         * Add a default gap between all children except if the neighbour is already a Gap.
         */
        public void addDefaultGap() {
            for(int i=0 ; i<springs.size() ; i++) {
                Spring s = springs.get(i);
                if(s instanceof Group) {
                    ((Group)s).addDefaultGap();
                }
            }
        }

        void addSpring(Spring s) {
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
         * Add a default gap between all children except if the neighbour is already a Gap.
         */
        @Override
        public void addDefaultGap() {
            if(springs.size() > 1) {
                boolean wasGap = true;
                for(int i=0 ; i<springs.size() ; i++) {
                    Spring s = springs.get(i);
                    boolean isGap = (s instanceof GapSpring) || (s instanceof NamedGapSpring);
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
                if(resizeable > 1) {
                    Arrays.sort(deltas, 0, resizeable);
                }
                
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
