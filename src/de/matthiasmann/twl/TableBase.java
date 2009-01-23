/*
 * Copyright (c) 2008-2009, Matthias Mann
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

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.HashEntry;
import de.matthiasmann.twl.utils.SizeSequence;
import de.matthiasmann.twl.utils.SparseGrid;
import de.matthiasmann.twl.utils.SparseGrid.Entry;
import de.matthiasmann.twl.utils.TypeMapping;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public abstract class TableBase extends Widget implements ScrollPane.Scrollable {

    public interface CellRenderer {
        /**
         * Called when the CellRenderer is registered and a theme is applied.
         * @param themeParams the theme parameter object - shared for all CellRenderer
         */
        public void setThemeParameters(ParameterMap themeParams);
        
        public void setCellData(int row, int column, Object data);
        public int getColumnSpan();
        public int getPreferedHeight();
        public Widget getCellRenderWidget(int x, int y, int width, int height);
    }

    public interface CellWidgetCreator extends CellRenderer {
        public Widget updateWidget(Widget existingWidget);
        public void positionWidget(Widget widget, int x, int y, int w, int h);
    }

    public interface CachableCellWidgetCreator extends CellWidgetCreator {
        public String getCacheTag(int row, int column);
    }

    private final StringCellRenderer stringCellRenderer;
    private final RemoveCellWidgets removeCellWidgetsFunction;
    private final InsertCellWidgets insertCellWidgetsFunction;
    private final Widget cellWidgetContainer;
    
    protected final TypeMapping<CellRenderer> cellRenderers;
    protected final SparseGrid widgetGrid;
    protected final SizeSequence columnModel;
    protected SizeSequence rowModel;
    protected boolean hasCellWidgetCreators;
    protected WidgetCache[] widgetCacheTable;
    protected Button[] columnHeaders;

    protected Image imageRowDivider;
    protected Image imageColumnDivider;
    protected ParameterMap cellRendererParameters;
    protected int columnHeaderHeight;

    protected int numRows;
    protected int numColumns;
    protected int rowHeight = 32;
    protected int columnWidth = 256;
    protected boolean autoSizeAllRows;
    protected boolean updateAllCellWidgets;

    protected int scrollPosX;
    protected int scrollPosY;

    protected int firstVisibleRow;
    protected int firstVisibleColumn;
    protected int lastVisibleRow;
    protected int lastVisibleColumn;

    protected TableBase() {
        this.cellRenderers = new TypeMapping<CellRenderer>();
        this.stringCellRenderer = new StringCellRenderer();
        this.widgetGrid = new SparseGrid(32);
        this.removeCellWidgetsFunction = new RemoveCellWidgets();
        this.insertCellWidgetsFunction = new InsertCellWidgets();
        this.columnModel = new SizeSequence();
        this.cellWidgetContainer = new Widget();
        this.cellWidgetContainer.setTheme("");
        this.cellWidgetContainer.setClip(true);

        super.insertChild(cellWidgetContainer, 0);
        setCanAcceptKeyboardFocus(true);
    }

    public boolean isVariableRowHeight() {
        return rowModel != null;
    }

    public void setVaribleRowHeight(boolean varibleRowHeight) {
        if(varibleRowHeight && rowModel == null) {
            rowModel = new RowSizeSequence(numRows);
            autoSizeAllRows = true;
            invalidateLayout();
        } else if(!varibleRowHeight) {
            rowModel = null;
        }
    }

    public int getRowFromPosition(int y) {
        if(y >= 0) {
            if(rowModel != null) {
                return rowModel.getIndex(y);
            }
            return Math.min(numRows-1, y / rowHeight);
        }
        return -1;
    }

    public int getRowStartPosition(int row) {
        if(rowModel != null) {
            return rowModel.getPosition(row);
        } else {
            return row * rowHeight;
        }
    }

    public int getRowHeight(int row) {
        if(rowModel != null) {
            return rowModel.getSize(row);
        } else {
            return rowHeight;
        }
    }

    public int getRowEndPosition(int row) {
        if(rowModel != null) {
            return rowModel.getPosition(row + 1);
        } else {
            return (row+1) * rowHeight;
        }
    }

    public int getColumnFromPosition(int x) {
        if(x >= 0) {
            int column = columnModel.getIndex(x);
            return column;
        }
        return -1;
    }

    public int getColumnStartPosition(int column) {
        return columnModel.getPosition(column);
    }

    public int getColumnWidth(int column) {
        return columnModel.getSize(column);
    }

    public int getColumnEndPosition(int column) {
        return columnModel.getPosition(column + 1);
    }

    @Override
    public int getMinHeight() {
        return Math.max(super.getMinHeight(), columnHeaderHeight);
    }

    @Override
    public int getPreferedInnerWidth() {
        return (numColumns > 0) ? getColumnEndPosition(numColumns-1) : 0;
    }

    @Override
    public int getPreferedInnerHeight() {
        return (numRows > 0) ? getRowEndPosition(numRows-1) : 0;
    }

    public void registerCellRenderer(Class<?> dataClass, CellRenderer cellRenderer) {
        if(dataClass == null) {
            throw new NullPointerException("dataClass");
        }
        cellRenderers.put(dataClass, cellRenderer);

        if(cellRenderer instanceof CellWidgetCreator) {
            hasCellWidgetCreators = true;
        }

        // only call it when we already have a theme
        if(cellRendererParameters != null) {
            cellRenderer.setThemeParameters(cellRendererParameters);
        }
    }

    public void setScrollPosition(int scrollPosX, int scrollPosY) {
        this.scrollPosX = scrollPosX;
        this.scrollPosY = scrollPosY;
        invalidateLayout();
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeTableBase(themeInfo);
        modelAllChanged();
    }

    protected void applyThemeTableBase(ThemeInfo themeInfo) {
        this.imageRowDivider = themeInfo.getImage("rowDivider");
        this.imageColumnDivider = themeInfo.getImage("columnDivider");
        this.rowHeight = themeInfo.getParameter("rowHeight", 32);
        this.columnWidth = themeInfo.getParameter("columnWidth", 256);
        this.columnHeaderHeight = themeInfo.getParameter("columnHeaderHeight", 10);
        this.cellRendererParameters = themeInfo.getParameterMap("cellRendererParameters");
        autoSizeAllRows = true;
        invalidateParentLayout();
        invalidateLayout();

        for(CellRenderer cellRenderer : cellRenderers.getUniqueValues()) {
            cellRenderer.setThemeParameters(cellRendererParameters);
        }
        stringCellRenderer.setThemeParameters(cellRendererParameters);
    }

    @Override
    protected void childChangedSize(Widget child) {
        // ignore
    }

    @Override
    protected void childAdded(Widget child) {
        // ignore
    }

    @Override
    protected void childRemoved(Widget exChild) {
        // ignore
    }


    @Override
    protected void layout() {
        final int innerWidth = getInnerWidth();
        final int innerHeight = Math.max(0, getInnerHeight() - columnHeaderHeight);

        cellWidgetContainer.setPosition(getInnerX(), getInnerY() + columnHeaderHeight);
        cellWidgetContainer.setSize(innerWidth, innerHeight);
        
        if(autoSizeAllRows) {
            autoSizeAllRows();
        }
        if(updateAllCellWidgets) {
            updateAllCellWidgets();
        }

        final int scrollEndX = scrollPosX + innerWidth;
        final int scrollEndY = scrollPosY + innerHeight;

        int startRow = getRowFromPosition(scrollPosY);
        int startColumn = getColumnFromPosition(scrollPosX);
        int endRow = getRowFromPosition(scrollEndY);
        int endColumn = getColumnFromPosition(scrollEndX);

        if(endRow+1 < numRows && getRowEndPosition(endRow) < scrollEndY) {
            endRow++;
        }
        if(endColumn+1 < numColumns && getColumnEndPosition(endColumn) < scrollEndX) {
            endColumn++;
        }

        if(!widgetGrid.isEmpty()) {
            if(startRow > firstVisibleRow) {
                widgetGrid.iterate(firstVisibleRow, 0, startRow-1, numColumns, removeCellWidgetsFunction);
            }
            if(endRow < lastVisibleRow) {
                widgetGrid.iterate(endRow+1, 0, lastVisibleRow, numColumns, removeCellWidgetsFunction);
            }

            widgetGrid.iterate(startRow, 0, endRow, numColumns, insertCellWidgetsFunction);
        }

        firstVisibleRow = startRow;
        firstVisibleColumn = startColumn;
        lastVisibleRow = endRow;
        lastVisibleColumn = endColumn;

        if(numColumns > 0) {
            final int offsetX = getInnerX() - scrollPosX;
            int colStartPos = getColumnStartPosition(0);
            for(int i=0 ; i<numColumns ; i++) {
                int colEndPos = getColumnEndPosition(i);
                Widget w = columnHeaders[i];
                if(w != null) {
                    if(w.getParent() != this) {
                        super.insertChild(w, super.getNumChildren());
                    }
                    w.setPosition(offsetX + colStartPos, getInnerY());
                    w.setSize(colEndPos - colStartPos, columnHeaderHeight);
                }
                colStartPos = colEndPos;
            }
        }
    }

    @Override
    protected void paintWidget(GUI gui) {
        final int innerX = getInnerX();
        final int innerY = getInnerY() + columnHeaderHeight;
        final int innerWidth = getInnerWidth();
        final int innerHeight = getInnerHeight() - columnHeaderHeight;

        int colDivWidth = 0;
        if(imageColumnDivider != null) {
            colDivWidth = imageColumnDivider.getWidth();

            int curX = innerX;
            for(int col=firstVisibleColumn ; col<=lastVisibleColumn ; col++) {
                curX += getColumnWidth(col);
                imageColumnDivider.draw(getAnimationState(), curX, innerY, colDivWidth, innerHeight);
                curX += colDivWidth;
            }
        }

        final int offsetX = innerX - scrollPosX;
        final int offsetY = innerY - scrollPosY;

        int rowStartPos = getRowStartPosition(firstVisibleRow);
        int curY = rowStartPos + offsetY;
        for(int row=firstVisibleRow ; row<=lastVisibleRow ; row++) {
            final int rowEndPos = getRowEndPosition(row);
            final int curRowHeight = rowEndPos - rowStartPos;
            final TreeTableNode rowNode = getNodeFromRow(row);

            int colStartPos = getColumnStartPosition(firstVisibleColumn);
            for(int col=firstVisibleColumn ; col<=lastVisibleColumn ;) {
                int colEndPos = getColumnEndPosition(col);
                final CellRenderer cellRenderer = getCellRenderer(row, col, rowNode);

                int curX = offsetX + colStartPos;
                int colSpan = 1;

                if(cellRenderer != null) {
                    colSpan = cellRenderer.getColumnSpan();
                    if(colSpan > 1) {
                        colEndPos = getColumnEndPosition(Math.max(numColumns-1, col+colSpan-1));
                    }

                    Widget cellRendererWidget = cellRenderer.getCellRenderWidget(
                            curX, curY, colEndPos - colStartPos, curRowHeight);

                    if(cellRendererWidget != null) {
                        if(cellRendererWidget.getParent() != this) {
                            insertCellRenderer(cellRendererWidget);
                        }
                        paintChild(gui, cellRendererWidget);
                    }
                }

                col += Math.max(1, colSpan);
                colStartPos = colEndPos;
            }

            curY += curRowHeight;
            rowStartPos = rowEndPos;

            if(imageRowDivider != null) {
                int rowDivHeight = imageRowDivider.getHeight();
                imageRowDivider.draw(getAnimationState(), innerX, curY, innerWidth, rowDivHeight);
                curY += rowDivHeight;
            }
        }
    }

    protected void insertCellRenderer(Widget widget) {
        int posX = widget.getX();
        int posY = widget.getY();
        widget.setVisible(false);
        super.insertChild(widget, super.getNumChildren());
        widget.setPosition(posX, posY);
    }

    protected abstract TreeTableNode getNodeFromRow(int row);
    protected abstract Object getCellData(int row, int column, TreeTableNode node);

    protected CellRenderer getCellRenderer(Object data) {
        Class<? extends Object> dataClass = data.getClass();
        CellRenderer cellRenderer = cellRenderers.get(dataClass);
        if(cellRenderer == null) {
            cellRenderer = stringCellRenderer;
        }
        return cellRenderer;
    }

    protected CellRenderer getCellRenderer(int row, int col, TreeTableNode node) {
        final Object data = getCellData(row, col, node);
        if(data != null) {
            CellRenderer cellRenderer = getCellRenderer(data);
            cellRenderer.setCellData(row, col, data);
            return cellRenderer;
        }
        return null;
    }

    protected int computeRowHeight(int row) {
        final TreeTableNode rowNode = getNodeFromRow(row);
        int height = 0;
        for(int column = 0; column < numColumns; column++) {
            CellRenderer cellRenderer = getCellRenderer(row, column, rowNode);
            if(cellRenderer != null) {
                height = Math.max(height, cellRenderer.getPreferedHeight());
                column += Math.max(cellRenderer.getColumnSpan() - 1, 0);
            }
        }
        return height;
    }

    protected void autoSizeRow(int row) {
        int height = computeRowHeight(row);
        rowModel.setSize(row, height);
    }

    protected void autoSizeAllRows() {
        if(rowModel != null) {
            rowModel.initializeAll(numRows);
        }
        autoSizeAllRows = false;
    }

    protected WidgetCache getWidgetCache(String tag) {
        if(widgetCacheTable == null) {
            widgetCacheTable = new WidgetCache[16];
        }
        WidgetCache widgetCache = HashEntry.get(widgetCacheTable, tag);
        if(widgetCache == null) {
            widgetCache = new WidgetCache(tag);
            HashEntry.insertEntry(widgetCacheTable, widgetCache);
        }
        return widgetCache;
    }
    
    protected void removeCellWidget(Widget widget) {
        int idx = cellWidgetContainer.getChildIndex(widget);
        if(idx >= 0) {
            cellWidgetContainer.removeChild(idx);
        }
    }

    void insertCellWidget(int row, int column, WidgetEntry widgetEntry) {
        CellWidgetCreator cwc = (CellWidgetCreator)getCellRenderer(row, column, null);
        Widget widget = widgetEntry.widget;

        if(widget == null && widgetEntry.cache != null) {
            widget = widgetEntry.cache.get();
            widget = cwc.updateWidget(widget);
            widgetEntry.widget = widget;
        }
        
        if(widget != null) {
            if(widget.getParent() != cellWidgetContainer) {
                cellWidgetContainer.insertChild(widget, cellWidgetContainer.getNumChildren());
            }

            int x = getColumnStartPosition(column);
            int w = getColumnEndPosition(column) - x;
            int y = getRowStartPosition(row);
            int h = getRowEndPosition(row) - y;

            cwc.positionWidget(widget,
                    x + getInnerX() - scrollPosX,
                    y + getInnerY() - scrollPosY + columnHeaderHeight,
                    w, h);
        }
    }

    protected void updateCellWidget(int row, int column) {
        WidgetEntry we = (WidgetEntry)widgetGrid.get(row, column);
        Widget oldWidget = (we != null) ? we.widget : null;
        Widget newWidget = null;

        TreeTableNode rowNode = getNodeFromRow(row);
        CellRenderer cellRenderer = getCellRenderer(row, column, rowNode);
        if(cellRenderer instanceof CellWidgetCreator) {
            CellWidgetCreator cellWidgetCreator = (CellWidgetCreator)cellRenderer;
            if(we != null && we.creator != cellWidgetCreator) {
                // the cellWidgetCreator has changed for this cell
                // discard the old widget
                oldWidget = null;
            }
            if(cellWidgetCreator instanceof CachableCellWidgetCreator) {
                CachableCellWidgetCreator ccwc = (CachableCellWidgetCreator)cellWidgetCreator;
                if(we == null) {
                    we = new WidgetEntry();
                    widgetGrid.set(row, column, we);
                }
                we.creator = cellWidgetCreator;
                we.cache = getWidgetCache(ccwc.getCacheTag(row, column));
            } else {
                newWidget = cellWidgetCreator.updateWidget(oldWidget);
                if(newWidget != null) {
                    if(we == null) {
                        we = new WidgetEntry();
                        widgetGrid.set(row, column, we);
                    }
                    we.widget = newWidget;
                    we.creator = cellWidgetCreator;
                    we.cache = null;
                }
            }
        }

        if(newWidget == null && we != null && we.cache == null) {
            widgetGrid.remove(row, column);
        }
        
        if(oldWidget != null && newWidget != oldWidget) {
            removeCellWidget(oldWidget);
        }
    }

    protected void updateAllCellWidgets() {
        if(!widgetGrid.isEmpty() || hasCellWidgetCreators) {
            for(int row=0 ; row<numRows ; row++) {
                for(int col=0 ; col<numColumns ; col++) {
                    updateCellWidget(row, col);
                }
            }
        }

        updateAllCellWidgets = false;
    }

    protected void removeAllCellWidgets() {
        cellWidgetContainer.removeAllChildren();
    }

    protected Button createColumnHeader(int column) {
        Button btn = new Button();
        btn.setTheme("columnHeader");
        return btn;
    }

    protected void modelAllChanged() {
        if(!widgetGrid.isEmpty()) {
            removeAllCellWidgets();
            widgetGrid.clear();
        }

        widgetCacheTable = null;

        columnHeaders = new Button[numColumns];
        for(int i=0 ; i<numColumns ; i++) {
            columnHeaders[i] = createColumnHeader(i);
        }

        columnModel.setDefaultValue(columnWidth);
        columnModel.initializeAll(numColumns);
        if(rowModel != null) {
            autoSizeAllRows = true;
        }

        updateAllCellWidgets = true;
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelRowChanged(int row) {
        if(rowModel != null) {
            autoSizeRow(row);
        }
        for(int col=0 ; col<numColumns ; col++) {
            updateCellWidget(row, col);
        }
        invalidateLayout();
    }

    protected void modelRowsChanged(int idx, int count) {
        for(int i=0 ; i<count ; i++) {
            if(rowModel != null) {
                autoSizeRow(idx+i);
            }
            for(int col=0 ; col<numColumns ; col++) {
                updateCellWidget(idx+i, col);
            }
        }
        invalidateLayout();
    }

    protected void modelCellChanged(int row, int column) {
        if(rowModel != null) {
            autoSizeRow(row);
        }
        updateCellWidget(row, column);
        invalidateLayout();
    }

    protected void modelRowsInserted(int row, int count) {
        if(rowModel != null) {
            rowModel.insert(row, count);
        }
        if(!widgetGrid.isEmpty() || hasCellWidgetCreators) {
            removeAllCellWidgets();
            widgetGrid.insertRows(row, count);

            for(int i=0 ; i<count ; i++) {
                for(int col=0 ; col<numColumns ; col++) {
                    updateCellWidget(row+i, col);
                }
            }
        }
        if(row < getRowStartPosition(scrollPosY)) {
            ScrollPane sp = ScrollPane.getContainingScrollPane(this);
            if(sp != null) {
                int rowsStart = getRowStartPosition(row);
                int rowsEnd = getRowEndPosition(row + count - 1);
                sp.setScrollPositionY(scrollPosY + rowsEnd - rowsStart);
            }
        }
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelRowsDeleted(int row, int count) {
        if(row+count <= getRowStartPosition(scrollPosY)) {
            ScrollPane sp = ScrollPane.getContainingScrollPane(this);
            if(sp != null) {
                int rowsStart = getRowStartPosition(row);
                int rowsEnd = getRowEndPosition(row + count - 1);
                sp.setScrollPositionY(scrollPosY - rowsEnd + rowsStart);
            }
        }
        if(rowModel != null) {
            rowModel.remove(row, count);
        }
        if(!widgetGrid.isEmpty()) {
            widgetGrid.iterate(row, 0, row+count-1, numColumns, removeCellWidgetsFunction);
            widgetGrid.removeRows(row, count);
        }
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelColumnsInserted(int column, int count) {
        columnModel.insert(column, count);

        Button[] newColumnHeaders = new Button[numColumns];
        System.arraycopy(columnHeaders, 0, newColumnHeaders, 0, column);
        System.arraycopy(columnHeaders, column, newColumnHeaders, column+count,
                numColumns - (column+count));
        for(int i=0 ; i<count ; i++) {
            newColumnHeaders[column+i] = createColumnHeader(column+i);
        }
        columnHeaders = newColumnHeaders;
        
        if(!widgetGrid.isEmpty() || hasCellWidgetCreators) {
            removeAllCellWidgets();
            widgetGrid.insertColumns(column, count);

            for(int row=0 ; row<numRows ; row++) {
                for(int i=0 ; i<count ; i++) {
                    updateCellWidget(row, column + i);
                }
            }
        }
        if(column < getColumnStartPosition(scrollPosX)) {
            ScrollPane sp = ScrollPane.getContainingScrollPane(this);
            if(sp != null) {
                int columnsStart = getColumnStartPosition(column);
                int columnsEnd = getColumnEndPosition(column + count - 1);
                sp.setScrollPositionX(scrollPosX + columnsEnd - columnsStart);
            }
        }
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelColumnsDeleted(int column, int count) {
        if(column+count <= getColumnStartPosition(scrollPosX)) {
            ScrollPane sp = ScrollPane.getContainingScrollPane(this);
            if(sp != null) {
                int columnsStart = getColumnStartPosition(column);
                int columnsEnd = getColumnEndPosition(column + count - 1);
                sp.setScrollPositionY(scrollPosX - columnsEnd + columnsStart);
            }
        }
        columnModel.remove(column, count);
        if(!widgetGrid.isEmpty()) {
            widgetGrid.iterate(0, column, numRows, column+count-1, removeCellWidgetsFunction);
            widgetGrid.removeColumns(column, count);
        }

        for(int i=0 ; i<count ; i++) {
            int idx = super.getChildIndex(columnHeaders[column+i]);
            if(idx >= 0) {
                super.removeChild(idx);
            }
        }

        Button[] newColumnHeaders = new Button[numColumns];
        System.arraycopy(columnHeaders, 0, newColumnHeaders, 0, column);
        System.arraycopy(columnHeaders, column+count, newColumnHeaders, column, numColumns - count);
        columnHeaders = newColumnHeaders;
        
        invalidateLayout();
        invalidateParentLayout();
    }

    class RowSizeSequence extends SizeSequence {
        public RowSizeSequence(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        protected void initializeSizes(int index, int count) {
            for(int i=0 ; i<count ; i++,index++) {
                table[index] = computeRowHeight(index);
            }
        }
    }

    class RemoveCellWidgets implements SparseGrid.GridFunction {
        public void apply(int row, int column, Entry e) {
            WidgetEntry widgetEntry = (WidgetEntry)e;
            Widget widget = widgetEntry.widget;
            if(widget != null) {
                removeCellWidget(widget);
                if(widgetEntry.cache != null) {
                    widgetEntry.cache.put(widget);
                    widgetEntry.widget = null;
                }
            }
        }
    }

    class InsertCellWidgets implements SparseGrid.GridFunction {
        public void apply(int row, int column, Entry e) {
            insertCellWidget(row, column, (WidgetEntry)e);
        }
    }

    static class WidgetEntry extends SparseGrid.Entry {
        Widget widget;
        CellWidgetCreator creator;
        WidgetCache cache;
    }

    protected static class WidgetCache extends HashEntry<String, WidgetCache> {
        private final ArrayList<Widget> widgets;

        protected WidgetCache(String key) {
            super(key);
            this.widgets = new ArrayList<Widget>();
        }

        public Widget get() {
            int size = widgets.size();
            if(size > 0) {
                return widgets.remove(size - 1);
            }
            return null;
        }

        public void put(Widget widget) {
            widgets.add(widget);
        }
    }

    public static class StringCellRenderer extends TextWidget implements CellRenderer {
        public StringCellRenderer() {
            setCache(false);
            setClip(true);
        }

        public void setThemeParameters(ParameterMap themeParams) {
            setFont(themeParams.getFont("font"));
        }

        public void setCellData(int row, int column, Object data) {
            setText(String.valueOf(data));
        }

        public int getColumnSpan() {
            return 1;
        }

        @Override
        public int getPreferedHeight() {
            Font font = getFont();
            int lineHeight = (font != null) ? font.getLineHeight() : 0;
            return getBorderVertical() + lineHeight * Math.max(1, getNumTextLines());
        }

        public Widget getCellRenderWidget(int x, int y, int width, int height) {
            setPosition(x, y);
            setSize(width, height);
            return this;
        }
    }
}
