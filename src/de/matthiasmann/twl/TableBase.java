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
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.SizeSequence;
import de.matthiasmann.twl.utils.SparseGrid;
import de.matthiasmann.twl.utils.SparseGrid.Entry;
import de.matthiasmann.twl.utils.TypeMapping;

/**
 *
 * @author Matthias Mann
 */
public abstract class TableBase extends Widget implements ScrollPane.Scrollable {

    public interface CellRenderer {
        public void setCellData(int row, int column, Object data);
        public int getPreferedHeight();
        public void setThemeParameters(ParameterMap themeParams);
    }

    public interface CellWidgetCreator extends CellRenderer {
        public Widget updateWidget(int row, int column, Object data, Widget existingWidget);
        public void positionWidget(int row, int column, Object data, Widget widget, int x, int y, int w, int h);
    }

    public interface CachableCellWidgetCreator extends CellWidgetCreator {
        public String getCacheTag(int row, int column);
    }

    private final StringCellRenderer stringCellRenderer;
    private final RemoveCellWidgets removeCellWidgetsFunction;
    private final InsertCellWidgets insertCellWidgetsFunction;
    
    protected final TypeMapping<CellRenderer> cellRenderers;
    protected final SparseGrid widgetGrid;
    protected final SizeSequence columnModel;
    protected SizeSequence rowModel;
    protected boolean hasCellWidgetCreators;

    protected Image imageRowDivider;
    protected Image imageColumnDivider;
    protected ParameterMap cellRendererParameters;

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

        registerCellRenderer(String.class, stringCellRenderer);
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
        if(cellRenderer instanceof Widget) {
            Widget w = (Widget)cellRenderer;
            if(w.getParent() != null) {
                throw new IllegalArgumentException("cellRenderer already in use");
            }
            w.setVisible(false);
            super.insertChild(w, super.getNumChilds());
        } else if(!(cellRenderer instanceof CellWidgetCreator)) {
            throw new IllegalArgumentException("cellRenderer must be atleast a Widget or a CellWidgetCreator");
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
        applyThemeTable(themeInfo);
    }

    protected void applyThemeTable(ThemeInfo themeInfo) {
        this.imageRowDivider = themeInfo.getImage("rowDivider");
        this.imageColumnDivider = themeInfo.getImage("columnDivider");
        this.rowHeight = themeInfo.getParameter("rowHeight", 32);
        this.columnWidth = themeInfo.getParameter("columnWidth", 256);
        this.cellRendererParameters = themeInfo.getParameterMap("cellRendererParameters");
        autoSizeAllRows = true;
        invalidateParentLayout();
        invalidateLayout();

        for(CellRenderer cellRenderer : cellRenderers.getUniqueValues()) {
            cellRenderer.setThemeParameters(cellRendererParameters);
        }
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
        final int innerHeight = getInnerHeight();

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
    }

    @Override
    protected void paintWidget(GUI gui) {
        final int innerX = getInnerX();
        final int innerY = getInnerY();
        final int innerWidth = getInnerWidth();
        final int innerHeight = getInnerHeight();

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

        final int startX = innerX + getColumnStartPosition(firstVisibleColumn) - scrollPosX;
        final int offsetY = innerY - scrollPosY;

        int rowStartPos = getRowStartPosition(firstVisibleRow);
        int curY = rowStartPos + offsetY;
        for(int row=firstVisibleRow ; row<=lastVisibleRow ; row++) {
            final int rowEndPos = getRowEndPosition(row);
            final int curRowHeight = rowEndPos - rowStartPos;
            final TreeTableNode rowNode = getNodeFromRow(row);

            int curX = startX;
            for(int col=firstVisibleColumn ; col<=lastVisibleColumn ; col++) {
                final int curColWidth = getColumnWidth(col);

                final CellRenderer cellRenderer = getCellRenderer(row, col, rowNode);
                if(cellRenderer instanceof Widget) {
                    Widget cellRendererWidget = (Widget)cellRenderer;
                    cellRendererWidget.setPosition(curX, curY);
                    cellRendererWidget.setSize(curColWidth, curRowHeight);
                    paintChild(gui, cellRendererWidget);
                }
                
                curX += curColWidth + colDivWidth;
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

    protected void removeCellWidget(Widget widget) {
        int idx = super.getChildIndex(widget);
        if(idx >= 0) {
            super.removeChild(idx);
        }
    }

    protected void insertCellWidget(int row, int col, Widget widget, CellWidgetCreator creator) {
        if(widget.getParent() != this) {
            super.insertChild(widget, getNumChilds());
        }

        int x = getColumnStartPosition(col);
        int w = getColumnEndPosition(col) - x;
        int y = getRowStartPosition(row);
        int h = getRowEndPosition(row) - y;

        creator.positionWidget(row, col,
                getCellData(row, col, null), widget,
                x + getInnerX() - scrollPosX,
                y + getInnerY() - scrollPosY,
                w, h);
    }

    protected void updateCellWidget(int row, int col) {
        WidgetEntry we = (WidgetEntry)widgetGrid.get(row, col);
        Widget oldWidget = (we != null) ? we.widget : null;
        Widget newWidget = null;

        final Object data = getCellData(row, col, null);
        CellWidgetCreator cellWidgetCreator = null;
        if(data != null) {
            CellRenderer cellRenderer = getCellRenderer(data);
            if(cellRenderer instanceof CellWidgetCreator) {
                cellWidgetCreator = (CellWidgetCreator)cellRenderer;
                if(we != null && we.creator != cellWidgetCreator) {
                    // the cellWidgetCreator has changed for this cell
                    // discard the old widget
                    oldWidget = null;
                }
                newWidget = cellWidgetCreator.updateWidget(row, col, data, oldWidget);
                if(newWidget != null) {
                    if(we == null) {
                        we = new WidgetEntry();
                        widgetGrid.set(row, col, we);
                    }
                    we.widget = newWidget;
                    we.creator = cellWidgetCreator;
                }
            }
        }

        if(newWidget == null && we != null) {
            widgetGrid.remove(row, col);
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

    protected void addAllCellRenderers() {
        for(CellRenderer cellRenderer : cellRenderers.getUniqueValues()) {
            Widget w = (Widget)cellRenderer;
            if(w.getParent() != this) {
                super.insertChild(w, super.getNumChilds());
            }
        }
    }
    
    protected void removeAllCellWidgets() {
        super.removeAllChilds();
        addAllCellRenderers();
    }

    protected void modelAllChanged() {
        if(!widgetGrid.isEmpty()) {
            removeAllCellWidgets();
            widgetGrid.clear();
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
            if(widgetEntry.widget != null) {
                removeCellWidget(widgetEntry.widget);
            }
        }
    }

    class InsertCellWidgets implements SparseGrid.GridFunction {
        public void apply(int row, int column, Entry e) {
            WidgetEntry widgetEntry = (WidgetEntry)e;
            if(widgetEntry.widget != null) {
                insertCellWidget(row, column, widgetEntry.widget, widgetEntry.creator);
            }
        }
    }

    static class WidgetEntry extends SparseGrid.Entry {
        Widget widget;
        CellWidgetCreator creator;
    }

    static class StringCellRenderer extends TextWidget implements CellRenderer {
        public StringCellRenderer() {
            setCache(false);
            setClip(true);
        }

        public void setCellData(int row, int column, Object data) {
            setText((data == null) ? "null" : data.toString());
        }

        public void setThemeParameters(ParameterMap themeParams) {
        }
    }
}
