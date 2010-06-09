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
package de.matthiasmann.twl.textarea;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Data model for the TextArea widget.
 * 
 * @author Matthias Mann
 */
public interface TextAreaModel extends Iterable<TextAreaModel.Element> {

    public enum HAlignment {
        LEFT,
        RIGHT,
        CENTER,
        JUSTIFY
    }

    public enum Display {
        INLINE,
        BLOCK
    }
    
    public enum VAlignment {
        TOP,
        MIDDLE,
        BOTTOM,
        FILL
    }

    public enum Clear {
        NONE,
        LEFT,
        RIGHT,
        BOTH
    }

    public enum FloatPosition {
        NONE,
        LEFT,
        RIGHT
    }
    
    public abstract class Element {
        private final Style style;

        public Element(Style style) {
            this.style = style;
        }

        /**
         * Returns the style associated with this element
         * @return the style associated with this element. Must not be null.
         */
        public Style getStyle() {
            return style;
        }
    }

    public class TextElement extends Element {
        private final String text;
        private final boolean paragraphStart;
        private final boolean paragraphEnd;

        public TextElement(Style style, String text, boolean paragraphStart, boolean paragraphEnd) {
            super(style);
            this.text = text;
            this.paragraphStart = paragraphStart;
            this.paragraphEnd = paragraphEnd;
        }

        public TextElement(Style style, String text) {
            this(style, text, false, false);
        }

        /**
         * Returns ths text.
         * @return the text.
         */
        public String getText() {
            return text;
        }

        /**
         * Returns true if this element starts a new paragraph.
         *
         * Causes this element to start on a new line.
         * 
         * @return true if this element starts a new paragraph.
         */
        public boolean isParagraphStart() {
            return paragraphStart;
        }

        /**
         * Returns true if this element ends the current paragraph.
         *
         * A blank line is inserted after this element.
         * 
         * @return true if this element ends the current paragraph.
         */
        public boolean isParagraphEnd() {
            return paragraphEnd;
        }
    }

    public class LinkElement extends TextElement {
        private final String href;

        public LinkElement(Style style, String text, boolean paragraphStart, boolean paragraphEnd, String href) {
            super(style, text, paragraphStart, paragraphEnd);
            this.href = href;
        }

        public LinkElement(Style style, String text, String href) {
            this(style, text, false, false, href);
        }

        /**
         * Returns the href of the link.
         * @return the href of the link.
         */
        public String getHREF() {
            return href;
        }
    }

    public class ImageElement extends Element {
        private final String imageName;
        private final String tooltip;

        public ImageElement(Style style, String imageName, String tooltip) {
            super(style);
            this.imageName = imageName;
            this.tooltip = tooltip;
        }

        public ImageElement(Style style, String imageName) {
            this(style, imageName, null);
        }

        /**
         * Returns the image name for this image element.
         * @return the image name for this image element.
         */
        public String getImageName() {
            return imageName;
        }

        /**
         * Returns the tooltip or null for this image.
         * @return the tooltip or null for this image.
         */
        public String getToolTip() {
            return tooltip;
        }
    }

    public class WidgetElement extends Element {
        private final String widgetName;
        private final String widgetParam;

        public WidgetElement(Style style, String widgetName, String widgetParam) {
            super(style);
            this.widgetName = widgetName;
            this.widgetParam = widgetParam;
        }

        public String getWidgetName() {
            return widgetName;
        }

        public String getWidgetParam() {
            return widgetParam;
        }
    }

    public abstract class ContainerElement extends Element implements Iterable<Element> {
        protected final ArrayList<Element> children;

        public ContainerElement(Style style) {
            super(style);
            this.children = new ArrayList<Element>();
        }

        public Iterator<Element> iterator() {
            return children.iterator();
        }

        public void add(Element element) {
            this.children.add(element);
        }
    }

    public class ListElement extends ContainerElement {
        public ListElement(Style style) {
            super(style);
        }
    }

    public class BlockElement extends ContainerElement {
        public BlockElement(Style style) {
            super(style);
        }
    }

    public class TableCellElement extends ContainerElement {
        private final int colspan;

        public TableCellElement(Style style) {
            this(style, 1, 1);
        }

        public TableCellElement(Style style, int colspan, int rowspan) {
            super(style);
            this.colspan = colspan;
        }

        public int getColspan() {
            return colspan;
        }
    }
    
    public class TableElement extends Element {
        private final int numColumns;
        private final int numRows;
        private final int cellSpacing;
        private final int cellPadding;
        private final TableCellElement[] cells;

        public TableElement(Style style, int numColumns, int numRows, int cellSpacing, int cellPadding) {
            super(style);
            if(numColumns < 0 ) {
                throw new IllegalArgumentException("numColumns");
            }
            if(numRows < 0) {
                throw new IllegalArgumentException("numRows");
            }

            this.numColumns = numColumns;
            this.numRows = numRows;
            this.cellSpacing = cellSpacing;
            this.cellPadding = cellPadding;
            this.cells = new TableCellElement[numRows * numColumns];
        }

        public int getNumColumns() {
            return numColumns;
        }

        public int getNumRows() {
            return numRows;
        }

        public int getCellPadding() {
            return cellPadding;
        }

        public int getCellSpacing() {
            return cellSpacing;
        }

        public TableCellElement getCell(int row, int column) {
            if(column < 0 || column >= numColumns) {
                throw new IndexOutOfBoundsException("column");
            }
            if(row < 0 || row >= numRows) {
                throw new IndexOutOfBoundsException("row");
            }
            return cells[row * numColumns + column];
        }

        public void setSell(int row, int column, TableCellElement cell) {
            if(column < 0 || column >= numColumns) {
                throw new IndexOutOfBoundsException("column");
            }
            if(row < 0 || row >= numRows) {
                throw new IndexOutOfBoundsException("row");
            }
            cells[row * numColumns + column] = cell;
        }
    }

    /**
     * Adds a model change callback which is called when the model is modified.
     * @param cb the callback - must not be null.
     */
    public void addCallback(Runnable cb);

    /**
     * Removes the specific callback.
     * @param cb the callback that should be removed.
     */
    public void removeCallback(Runnable cb);
    
}
