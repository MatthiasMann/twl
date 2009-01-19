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
 *
 * @author Matthias Mann
 */
public class BoxLayout extends Widget {

    public static final int NOT_SET = -1;
    
    public enum Direction {
        HORIZONTAL,
        VERTICAL
    };
    
    private Direction direction;
    private int spacing;
    private boolean autoSize = true;
    private boolean scroll;
    private int maxComponentSize = NOT_SET;
    private Alignment alignment = Alignment.TOP;
    
    public BoxLayout() {
        this(Direction.HORIZONTAL);
    }
    
    public BoxLayout(Direction direction) {
        this.direction = direction;
    }

    public int getSpacing() {
        return spacing;
    }

    public void setSpacing(int spacing) {
        if(this.spacing != spacing) {
            this.spacing = spacing;
            invalidateLayout();
        }
    }

    public boolean isAutoSize() {
        return autoSize;
    }

    public void setAutoSize(boolean autoSize) {
        if(this.autoSize != autoSize) {
            this.autoSize = autoSize;
            invalidateLayout();
        }
    }

    public boolean isScroll() {
        return scroll;
    }

    public void setScroll(boolean scroll) {
        if(this.scroll != scroll) {
            this.scroll = scroll;
            invalidateLayout();
        }
    }

    public int getMaxComponentSize() {
        return maxComponentSize;
    }

    public void setMaxComponentSize(int maxComponentSize) {
        if(maxComponentSize < 0 && maxComponentSize != NOT_SET) {
            throw new IllegalArgumentException("maxComponentSize");
        }
        if(this.maxComponentSize != maxComponentSize) {
            this.maxComponentSize = maxComponentSize;
            invalidateLayout();
        }
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        if(alignment == null) {
            throw new NullPointerException("alignment");
        }
        if(this.alignment != alignment) {
            this.alignment = alignment;
            invalidateLayout();
        }
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        if(direction == null) {
            throw new NullPointerException("direction");
        }
        if(this.direction != direction) {
            this.direction = direction;
            invalidateLayout();
        }
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        setSpacing(themeInfo.getParameter("spacing", 0));
        setAlignment(themeInfo.getParameter("alignment", Alignment.TOP));
    }

    protected void layoutHorizontal() {
        int x = getInnerX();
        int y = getInnerY();
        int width = 0;
        int height = 0;
        final int numChildren = getNumChilds();

        // pass 1: get needed size and limit component size
        for(int idx=0 ; idx<numChildren ; idx++) {
            Widget child = getChild(idx);
            int childWidth = child.getWidth();
            int childHeight = child.getHeight();

            if(maxComponentSize != NOT_SET && childWidth > maxComponentSize) {
                child.setSize(maxComponentSize, childHeight);
                child.setClip(true);// activate clip because we reduce it's size
                childWidth = maxComponentSize;  // don't ask child to prevent layout bugs
            }
            x += childWidth + spacing;
            height = Math.max(height, child.getHeight());
        }

        width = x - spacing - getInnerX();
        x = getInnerX();

        if(!autoSize && scroll && width > getInnerWidth()) {
            x -= width - getInnerWidth();
        }

        // pass 2: position childs
        for(int idx=0 ; idx<numChildren ; idx++) {
            Widget child = getChild(idx);
            int yoff = 0;
            switch (alignment) {
            case LEFT:
            case CENTER:
            case RIGHT:
                yoff = (height - child.getHeight())/2;
                break;
            case BOTTOMLEFT:
            case BOTTOM:
            case BOTTOMRIGHT:
                yoff = height - child.getHeight();
                break;
            default:
                break;
            }
            child.setPosition(x, y + yoff);
            int childWidth = child.getWidth();
            if(maxComponentSize > NOT_SET && childWidth > maxComponentSize) {
                childWidth = maxComponentSize;
            }
            x += childWidth + spacing;
        }

        width = x - spacing - getInnerX();
        setInnerSize(autoSize ? width : getInnerWidth(), height);
    }

    protected void layoutVertical() {
        int x = getInnerX();
        int y = getInnerY();
        int width = 0;
        int height = 0;
        final int numChildren = getNumChilds();

        // pass 1: get needed size and limit component size
        for(int idx=0 ; idx<numChildren ; idx++) {
            Widget child = getChild(idx);
            int childWidth = child.getWidth();
            int childHeight = child.getHeight();

            if(maxComponentSize != NOT_SET && childHeight > maxComponentSize) {
                child.setSize(childWidth, maxComponentSize);
                child.setClip(true);// activate clip because we reduce it's size
                childHeight = maxComponentSize;  // don't ask child to prevent layout bugs
            }
            y += childHeight + spacing;
            width = Math.max(width, child.getWidth());
        }

        height = y - spacing - getInnerY();
        y = getInnerY();

        if(!autoSize && scroll && height > getInnerHeight()) {
            y -= height - getInnerHeight();
        }

        // pass 2: position childs
        for(int idx=0 ; idx<numChildren ; idx++) {
            Widget child = getChild(idx);
            int xoff = 0;
            switch (alignment) {
            case TOP:
            case CENTER:
            case BOTTOM:
                xoff = (width - child.getWidth())/2;
                break;
            case TOPRIGHT:
            case RIGHT:
            case BOTTOMRIGHT:
                xoff = width - child.getWidth();
                break;
            default:
                break;
            }
            child.setPosition(x + xoff, y);
            int childHeight = child.getHeight();
            if(maxComponentSize > NOT_SET && childHeight > maxComponentSize) {
                childHeight = maxComponentSize;
            }
            y += childHeight + spacing;
        }

        height = y - spacing - getInnerY();
        setInnerSize(width, autoSize ? height : getInnerHeight());
    }

    @Override
    protected void layout() {
        if(getNumChilds() > 0) {
            switch(direction) {
            case HORIZONTAL:
                layoutHorizontal();
                break;
                
            case VERTICAL:
                layoutVertical();
                break;
                
            default:
                throw new IllegalStateException(direction.toString());
            }
        }
    }

}
