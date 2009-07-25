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

import de.matthiasmann.twl.model.TableColumnHeaderModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.utils.CallbackSupport;
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

    public interface Callback {
        public void mouseDoubleClicked(int row, int column);
    }
    
    public interface CellRenderer {
        /**
         * Called when the CellRenderer is registered and a theme is applied.
         * @param themeInfo the theme object
         */
        public void applyTheme(ThemeInfo themeInfo);

        /**
         * The theme name for this CellRenderer. Must be relative to the Table.
         * @return the theme name.
         */
        public String getTheme();

        /**
         * This method sets the row, column and the cell data.
         * It is called before any other cell related method is called.
         * @param row the table row
         * @param column the table column
         * @param data the cell data
         */
        public void setCellData(int row, int column, Object data);

        /**
         * Returns how many columns this cell spans. Must be >= 1.
         * Is called after setCellData.
         * @return the column span.
         * @see #setCellData(int, int, java.lang.Object)
         */
        public int getColumnSpan();

        /**
         * Returns the preferred cell height in variable row height mode.
         * It is not called at all in fixed row height mode.
         * @return the preferred cell height
         * @see #setCellData(int, int, java.lang.Object)
         * @see TableBase#setVaribleRowHeight(boolean)
         */
        public int getPreferredHeight();

        /**
         * Returns the widget used to render the cell or null if no rendering
         * should happen. This widget should not be added to any widget. It
         * will be managed by the Table.
         * TableBase uses a stamping approch for cell rendering. This method
         * must not create a new widget each time.
         *
         * This method is responsible to call setPosition and setSize on the
         * returned widget.
         *
         * @param x the left edge of the cell
         * @param y the top edge of the cell
         * @param width the width of the cell
         * @param height the height of the cell
         * @param isSelected the selected state of this cell
         * @return the widget used for cell rendering or null.
         * @see #setCellData(int, int, java.lang.Object)
         */
        public Widget getCellRenderWidget(int x, int y, int width, int height, boolean isSelected);
    }

    public interface CellWidgetCreator extends CellRenderer {
        public Widget updateWidget(Widget existingWidget);
        public void positionWidget(Widget widget, int x, int y, int w, int h);
    }

    public interface CachableCellWidgetCreator extends CellWidgetCreator {
        public String getCacheTag(int row, int column);
    }

    public static final String STATE_FIRST_COLUMNHEADER = "firstColumnHeader";
    public static final String STATE_LAST_COLUMNHEADER = "lastColumnHeader";
    public static final String STATE_ROW_SELECTED = "rowSelected";
    public static final String STATE_LEAD_ROW = "leadRow";
    public static final String STATE_SELECTED = "selected";

    private final StringCellRenderer stringCellRenderer;
    private final RemoveCellWidgets removeCellWidgetsFunction;
    private final InsertCellWidgets insertCellWidgetsFunction;
    private final Widget cellWidgetContainer;
    
    protected final TypeMapping<CellRenderer> cellRenderers;
    protected final SparseGrid widgetGrid;
    protected final ColumnSizeSequence columnModel;
    protected TableColumnHeaderModel columnHeaderModel;
    protected SizeSequence rowModel;
    protected boolean hasCellWidgetCreators;
    protected WidgetCache[] widgetCacheTable;
    protected ColumnHeader[] columnHeaders;
    protected TableSelectionManager selectionManager;
    protected Callback[] callbacks;

    protected Image imageColumnDivider;
    protected Image imageRowBackground;
    protected Image imageRowOverlay;
    protected ThemeInfo tableBaseThemeInfo;
    protected int columnHeaderHeight;
    protected int columnDividerDragableDistance;
    protected MouseCursor columnResizeCursor;
    protected MouseCursor normalCursor;

    protected int numRows;
    protected int numColumns;
    protected int rowHeight = 32;
    protected int defaultColumnWidth = 256;
    protected boolean autoSizeAllRows;
    protected boolean updateAllCellWidgets;
    protected boolean updateAllColumnWidth;

    protected int scrollPosX;
    protected int scrollPosY;

    protected int firstVisibleRow;
    protected int firstVisibleColumn;
    protected int lastVisibleRow;
    protected int lastVisibleColumn;
    protected boolean firstRowPartialVisible;
    protected boolean lastRowPartialVisible;

    protected TableBase() {
        this.cellRenderers = new TypeMapping<CellRenderer>();
        this.stringCellRenderer = new StringCellRenderer();
        this.widgetGrid = new SparseGrid(32);
        this.removeCellWidgetsFunction = new RemoveCellWidgets();
        this.insertCellWidgetsFunction = new InsertCellWidgets();
        this.columnModel = new ColumnSizeSequence();
        this.cellWidgetContainer = new Widget();
        this.cellWidgetContainer.setTheme("");
        this.cellWidgetContainer.setClip(true);

        super.insertChild(cellWidgetContainer, 0);
        setCanAcceptKeyboardFocus(true);
    }

    public TableSelectionManager getSelectionManager() {
        return selectionManager;
    }

    public void setSelectionManager(TableSelectionManager selectionManager) {
        if(this.selectionManager != selectionManager) {
            if(this.selectionManager != null) {
                this.selectionManager.setAssociatedTable(null);
            }
            this.selectionManager = selectionManager;
            if(this.selectionManager != null) {
                this.selectionManager.setAssociatedTable(this);
            }
        }
    }

    public void setDefaultSelectionManager() {
        setSelectionManager(new TableRowSelectionManager());
    }

    public void addCallback(Callback callback) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, callback, Callback.class);
    }

    public void removeCallback(Callback callback) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, callback);
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

    public int getNumRows() {
        return numRows;
    }

    public int getNumColumns() {
        return numColumns;
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
        checkRowIndex(row);
        if(rowModel != null) {
            return rowModel.getPosition(row);
        } else {
            return row * rowHeight;
        }
    }

    public int getRowHeight(int row) {
        checkRowIndex(row);
        if(rowModel != null) {
            return rowModel.getSize(row);
        } else {
            return rowHeight;
        }
    }

    public int getRowEndPosition(int row) {
        checkRowIndex(row);
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
        checkColumnIndex(column);
        return columnModel.getPosition(column);
    }

    public int getColumnWidth(int column) {
        checkColumnIndex(column);
        return columnModel.getSize(column);
    }

    public int getColumnEndPosition(int column) {
        checkColumnIndex(column);
        return columnModel.getPosition(column + 1);
    }

    public void setColumnWidth(int column, int width) {
        checkColumnIndex(column);
        columnHeaders[column].setColumnWidth(width);    // store passed width
        if(columnModel.update(column)) {
            invalidateLayout();
            invalidateParentLayout();
        }
    }

    public void scrollToRow(int row) {
        ScrollPane scrollPane = ScrollPane.getContainingScrollPane(this);
        if(scrollPane != null) {
            int rowStart = getRowStartPosition(row);
            if(rowStart < scrollPosY) {
                scrollPane.setScrollPositionY(rowStart);
            } else {
                int rowEnd = getRowEndPosition(row);
                int innerHeight = Math.max(0, getInnerHeight() - columnHeaderHeight);
                if(rowEnd > scrollPosY + innerHeight) {
                    scrollPane.setScrollPositionY(rowEnd - innerHeight);
                }
            }
        }
    }

    public int getNumVisibleRows() {
        int rows = lastVisibleRow - firstVisibleRow;
        if(!lastRowPartialVisible) {
            rows++;
        }
        return rows;
    }
    
    @Override
    public int getMinHeight() {
        return Math.max(super.getMinHeight(), columnHeaderHeight);
    }

    @Override
    public int getPreferredInnerWidth() {
        return (numColumns > 0) ? getColumnEndPosition(numColumns-1) : 0;
    }

    @Override
    public int getPreferredInnerHeight() {
        return columnHeaderHeight + 
                ((numRows > 0) ? getRowEndPosition(numRows-1) : 0);
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
        if(tableBaseThemeInfo != null) {
            applyCellRendererTheme(cellRenderer);
        }
    }

    public void setScrollPosition(int scrollPosX, int scrollPosY) {
        this.scrollPosX = scrollPosX;
        this.scrollPosY = scrollPosY;
        invalidateLayout();
    }

    public void adjustScrollPosition(int row) {
        checkRowIndex(row);
        ScrollPane scrollPane = ScrollPane.getContainingScrollPane(this);
        int numVisibleRows = getNumVisibleRows();
        if(numVisibleRows >= 1 && scrollPane != null) {
            if(row < firstVisibleRow || (row == firstVisibleRow && firstRowPartialVisible)) {
                int pos = getRowStartPosition(row);
                scrollPane.setScrollPositionY(pos);
            } else if(row > lastVisibleRow || (row == lastVisibleRow && lastRowPartialVisible)) {
                int innerHeight = Math.max(0, getInnerHeight() - columnHeaderHeight);
                int pos = getRowEndPosition(row);
                pos = Math.max(0, pos - innerHeight);
                scrollPane.setScrollPositionY(pos);
            }
        }
    }

    public boolean isFixedWidthMode() {
        ScrollPane scrollPane = ScrollPane.getContainingScrollPane(this);
        if(scrollPane != null) {
            if(scrollPane.getFixed() != ScrollPane.Fixed.HORIZONTAL) {
                return false;
            }
        }
        return true;
    }
    
    protected final void checkRowIndex(int row) {
        if(row < 0 || row >= numRows) {
            throw new IndexOutOfBoundsException("row");
        }
    }

    protected final void checkColumnIndex(int column) {
        if(column < 0 || column >= numColumns) {
            throw new IndexOutOfBoundsException("column");
        }
    }
    
    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeTableBase(themeInfo);
        updateAll();
    }

    protected void applyThemeTableBase(ThemeInfo themeInfo) {
        this.tableBaseThemeInfo = themeInfo;
        this.imageColumnDivider = themeInfo.getImage("columnDivider");
        this.imageRowBackground = themeInfo.getImage("row.background");
        this.imageRowOverlay = themeInfo.getImage("row.overlay");
        this.rowHeight = themeInfo.getParameter("rowHeight", 32);
        this.defaultColumnWidth = themeInfo.getParameter("columnHeaderWidth", 256);
        this.columnHeaderHeight = themeInfo.getParameter("columnHeaderHeight", 10);
        this.columnDividerDragableDistance = themeInfo.getParameter("columnDividerDragableDistance", 3);
        
        for(CellRenderer cellRenderer : cellRenderers.getUniqueValues()) {
            applyCellRendererTheme(cellRenderer);
        }
        applyCellRendererTheme(stringCellRenderer);
        updateAllColumnWidth = true;
    }

    @Override
    protected void applyThemeMouseCursor(ThemeInfo themeInfo) {
        this.columnResizeCursor = themeInfo.getMouseCursor("columnResizeCursor");
        this.normalCursor = themeInfo.getMouseCursor("mouseCursor");
    }
    
    protected void applyCellRendererTheme(CellRenderer cellRenderer) {
        String childThemeName = cellRenderer.getTheme();
        assert !isAbsoluteTheme(childThemeName);
        ThemeInfo childTheme = tableBaseThemeInfo.getChildTheme(childThemeName);
        if(childTheme != null) {
            cellRenderer.applyTheme(childTheme);
        }
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException();
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

    protected int getOffsetX() {
        return getInnerX() - scrollPosX;
    }

    protected int getOffsetY() {
        return getInnerY() - scrollPosY + columnHeaderHeight;
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        if(isFixedWidthMode()) {
            updateAllColumnWidth = true;
        }
    }

    @Override
    protected void layout() {
        final int innerWidth = getInnerWidth();
        final int innerHeight = Math.max(0, getInnerHeight() - columnHeaderHeight);

        cellWidgetContainer.setPosition(getInnerX(), getInnerY() + columnHeaderHeight);
        cellWidgetContainer.setSize(innerWidth, innerHeight);

        if(updateAllColumnWidth) {
            updateAllColumnWidth();
        }
        if(autoSizeAllRows) {
            autoSizeAllRows();
        }
        if(updateAllCellWidgets) {
            updateAllCellWidgets();
        }

        final int scrollEndX = scrollPosX + innerWidth;
        final int scrollEndY = scrollPosY + innerHeight;

        int startRow = Math.min(numRows-1, Math.max(0, getRowFromPosition(scrollPosY)));
        int startColumn = Math.min(numColumns-1, Math.max(0, getColumnFromPosition(scrollPosX)));
        int endRow = Math.min(numRows-1, Math.max(startRow, getRowFromPosition(scrollEndY)));
        int endColumn = Math.min(numColumns-1, Math.max(startColumn, getColumnFromPosition(scrollEndX)));

        if(numRows > 0) {
            firstRowPartialVisible = getRowStartPosition(startRow) < scrollPosY;
            lastRowPartialVisible = getRowEndPosition(endRow) > scrollEndY;
        } else {
            firstRowPartialVisible = false;
            lastRowPartialVisible = false;
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
            final int offsetX = getOffsetX();
            int colStartPos = getColumnStartPosition(0);
            for(int i=0 ; i<numColumns ; i++) {
                int colEndPos = getColumnEndPosition(i);
                Widget w = columnHeaders[i];
                if(w != null) {
                    assert w.getParent() == this;
                    w.setPosition(offsetX + colStartPos +
                            columnDividerDragableDistance, getInnerY());
                    w.setSize(Math.max(0, colEndPos - colStartPos -
                            2*columnDividerDragableDistance), columnHeaderHeight);
                    w.setVisible(columnHeaderHeight > 0);
                    AnimationState animationState = w.getAnimationState();
                    animationState.setAnimationState(STATE_FIRST_COLUMNHEADER, i == 0);
                    animationState.setAnimationState(STATE_LAST_COLUMNHEADER, i == numColumns-1);
                }
                colStartPos = colEndPos;
            }
        }
    }

    @Override
    protected void paintWidget(GUI gui) {
        if(firstVisibleRow < 0 || firstVisibleRow >= numRows) {
            return;
        }
        
        final int innerX = getInnerX();
        final int innerY = getInnerY() + columnHeaderHeight;
        final int innerWidth = getInnerWidth();
        final int innerHeight = getInnerHeight() - columnHeaderHeight;
        final int offsetX = getOffsetX();
        final int offsetY = getOffsetY();

        gui.clipEnter(innerX, innerY, innerWidth, innerHeight);
        try {
            final AnimationState animState = getAnimationState();
            final int leadRow;
            final int leadColumn;
            final boolean isCellSelection;

            if(selectionManager != null) {
                leadRow = selectionManager.getLeadRow();
                leadColumn = selectionManager.getLeadColumn();
                isCellSelection = selectionManager.getSelectionGranularity() ==
                        TableSelectionManager.SelectionGranularity.CELLS;
            } else {
                leadRow = -1;
                leadColumn = -1;
                isCellSelection = false;
            }

            if(imageRowBackground != null) {
                paintRowImage(imageRowBackground, leadRow);
            }

            if(imageColumnDivider != null) {
                animState.setAnimationState(STATE_ROW_SELECTED, false);
                for(int col=firstVisibleColumn ; col<=lastVisibleColumn ; col++) {
                    int colEndPos = getColumnEndPosition(col);
                    int curX = offsetX + colEndPos;
                    imageColumnDivider.draw( animState, curX,innerY, 1, innerHeight);
                }
            }

            int rowStartPos = getRowStartPosition(firstVisibleRow);
            for(int row=firstVisibleRow ; row<=lastVisibleRow ; row++) {
                final int rowEndPos = getRowEndPosition(row);
                final int curRowHeight = rowEndPos - rowStartPos;
                final int curY = offsetY + rowStartPos;
                final TreeTableNode rowNode = getNodeFromRow(row);
                final boolean isRowSelected = !isCellSelection && isRowSelected(row);
                
                int colStartPos = getColumnStartPosition(firstVisibleColumn);
                for(int col=firstVisibleColumn ; col<=lastVisibleColumn ;) {
                    int colEndPos = getColumnEndPosition(col);
                    final CellRenderer cellRenderer = getCellRenderer(row, col, rowNode);
                    final boolean isCellSelected = isRowSelected || isCellSelected(row, col);

                    int curX = offsetX + colStartPos;
                    int colSpan = 1;

                    if(cellRenderer != null) {
                        colSpan = cellRenderer.getColumnSpan();
                        if(colSpan > 1) {
                            colEndPos = getColumnEndPosition(Math.max(numColumns-1, col+colSpan-1));
                        }

                        Widget cellRendererWidget = cellRenderer.getCellRenderWidget(
                                curX, curY, colEndPos - colStartPos, curRowHeight, isCellSelected);

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

                rowStartPos = rowEndPos;
            }

            if(imageRowOverlay != null) {
                paintRowImage(imageRowOverlay, leadRow);
            }
        } finally {
            gui.clipLeave();
        }
    }

    private void paintRowImage(Image img, int leadRow) {
        final AnimationState animState = getAnimationState();
        final int x = getOffsetX();
        final int width = columnModel.getEndPosition();
        final int offsetY = getOffsetY();
        
        int rowStartPos = getRowStartPosition(firstVisibleRow);
        for(int row=firstVisibleRow ; row<=lastVisibleRow ; row++) {
            final int rowEndPos = getRowEndPosition(row);
            final int curRowHeight = rowEndPos - rowStartPos;
            final int curY = offsetY + rowStartPos;

            animState.setAnimationState(STATE_ROW_SELECTED, isRowSelected(row));
            animState.setAnimationState(STATE_LEAD_ROW, row == leadRow);
            img.draw(animState, x, curY, width, curRowHeight);

            rowStartPos = rowEndPos;
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

    protected boolean isRowSelected(int row) {
        if(selectionManager != null) {
            return selectionManager.isRowSelected(row);
        }
        return false;
    }

    protected boolean isCellSelected(int row, int column) {
        if(selectionManager != null) {
            return selectionManager.isCellSelected(row, column);
        }
        return false;
    }

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
                height = Math.max(height, cellRenderer.getPreferredHeight());
                column += Math.max(cellRenderer.getColumnSpan() - 1, 0);
            }
        }
        return height;
    }

    protected int computeColumnWidth(int index) {
        if(isFixedWidthMode()) {
            int preferredWidth = computePreferredColumnsWidth();
            return computeFixedColumnWidth(index, preferredWidth);
        }
        return Math.max(2*columnDividerDragableDistance+1,
                columnHeaders[index].getPreferredWidth());
    }

    protected int computeFixedColumnWidth(int index, int preferredWidth) {
        int columnWidth = Math.max(2*columnDividerDragableDistance+1,
                columnHeaders[index].getPreferredWidth());
        return Math.max(2*columnDividerDragableDistance+1,
                getInnerWidth() * columnWidth / preferredWidth);
    }

    protected int computePreferredColumnsWidth() {
        int preferredWidth = 0;
        for(int i=0,n=columnHeaders.length ; i<n ; i++) {
            preferredWidth += Math.max(2*columnDividerDragableDistance+1,
                    columnHeaders[i].getPreferredWidth());
        }
        return preferredWidth;
    }

    protected boolean autoSizeRow(int row) {
        int height = computeRowHeight(row);
        return rowModel.setSize(row, height);
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

            cwc.positionWidget(widget, x + getOffsetX(), y + getOffsetY(), w, h);
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
                we.widget = null;
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

    protected int getDefaultColumnWidth(int column) {
        int width = 0;
        if(tableBaseThemeInfo != null) {
            ParameterMap columnWidthMap = tableBaseThemeInfo.getParameterMap("columnWidths");
            Object obj = columnWidthMap.getParameterValue(Integer.toString(column), false);
            if(obj instanceof Integer) {
                width = ((Integer)obj).intValue();
            }
        }
        if(width > 0) {
            return width;
        }
        return defaultColumnWidth;
    }

    protected ColumnHeader createColumnHeader(int column) {
        ColumnHeader btn = new ColumnHeader();
        btn.setTheme("columnHeader");
        btn.setCanAcceptKeyboardFocus(false);
        super.insertChild(btn, super.getNumChildren());
        return btn;
    }

    protected void updateColumnHeader(int column) {
        Button columnHeader = columnHeaders[column];
        columnHeader.setText(columnHeaderModel.getColumnHeaderText(column));
        String[] states = columnHeaderModel.getColumnHeaderStates();
        if(states.length > 0) {
            AnimationState animationState = columnHeader.getAnimationState();
            for(int i=0 ; i<states.length ; i++) {
                animationState.setAnimationState(states[i],
                        columnHeaderModel.getColumnHeaderState(column, i));
            }
        }
    }

    private void updateColumnNumbers() {
        for(int i=0 ; i<columnHeaders.length ; i++) {
            columnHeaders[i].column = i;
        }
    }
    
    private void removeColumnHeaders(int column, int count) throws IndexOutOfBoundsException {
        for(int i = 0 ; i < count ; i++) {
            int idx = super.getChildIndex(columnHeaders[column + i]);
            if(idx >= 0) {
                super.removeChild(idx);
            }
        }
    }

    protected boolean isMouseInColumnHeader(int y) {
        y -= getInnerY();
        return y >= 0 && y < columnHeaderHeight;
    }

    protected int getColumnSeparatorUnderMouse(int x) {
        x -= getOffsetX();
        x += columnDividerDragableDistance;
        int col = columnModel.getIndex(x);
        int dist = x - columnModel.getPosition(col);
        if(dist < 2*columnDividerDragableDistance) {
            return col - 1;
        }
        return -1;
    }

    protected int getRowUnderMouse(int y) {
        y -= getOffsetY();
        int row = getRowFromPosition(y);
        return row;
    }

    protected int getColumnUnderMouse(int x) {
        x -= getOffsetX();
        int col = columnModel.getIndex(x);
        return col;
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(super.handleEvent(evt)) {
            return true;
        }

        if(evt.isMouseEvent()) {
            return handleMouseEvent(evt);
        }
        
        return false;
    }

    @Override
    protected boolean handleKeyStrokeAction(String action, Event event) {
        if(!super.handleKeyStrokeAction(action, event)) {
            if(selectionManager == null) {
                return false;
            }
            if(!selectionManager.handleKeyStrokeAction(action, event)) {
                return false;
            }
        }
        // remove focus from childs
        requestKeyboardFocus(null);
        return true;
    }

    protected boolean dragActive;
    protected int dragColumn;
    protected int dragStartX;

    protected boolean handleMouseEvent(Event evt) {
        if(dragActive) {
            if(dragColumn >= 0) {
                setColumnWidth(dragColumn, Math.max(1, evt.getMouseX() - dragStartX));
            }
            if(evt.isMouseDragEnd()) {
                dragActive = false;
            }
            return true;
        }

        boolean inHeader = isMouseInColumnHeader(evt.getMouseY());
        if(inHeader) {
            int column = getColumnSeparatorUnderMouse(evt.getMouseX());
            if(column >= 0 && (column < getNumColumns()-1 || !isFixedWidthMode())) {
                setMouseCursor(columnResizeCursor);

                if(evt.getType() == Event.Type.MOUSE_BTNDOWN) {
                    dragColumn = column;
                    dragStartX = evt.getMouseX() - getColumnWidth(column);
                }
                if(evt.isMouseDragEvent()) {
                    dragActive = true;
                }
                return true;
            }
        } else {
            int row = getRowUnderMouse(evt.getMouseY());
            int column = getColumnUnderMouse(evt.getMouseX());
            if(selectionManager != null) {
                selectionManager.handleMouseEvent(row, column, evt);
            }
            if(evt.getType() == Event.Type.MOUSE_CLICKED && evt.getMouseClickCount() > 1) {
                if(callbacks != null) {
                    for(Callback cb : callbacks) {
                        cb.mouseDoubleClicked(row, column);
                    }
                }
            }
        }

        setMouseCursor(normalCursor);

        // let ScrollPane handle mouse wheel
        return evt.getType() != Event.Type.MOUSE_WHEEL;
    }


    protected void updateAllColumnWidth() {
        columnModel.initializeAll(numColumns);
        updateAllColumnWidth = false;
    }

    protected void updateAll() {
        if(!widgetGrid.isEmpty()) {
            removeAllCellWidgets();
            widgetGrid.clear();
        }
        widgetCacheTable = null;

        if(rowModel != null) {
            autoSizeAllRows = true;
        }

        updateAllCellWidgets = true;
        updateAllColumnWidth = true;
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelAllChanged() {
        if(columnHeaders != null) {
            removeColumnHeaders(0, columnHeaders.length);
        }

        columnHeaders = new ColumnHeader[numColumns];
        for(int i=0 ; i<numColumns ; i++) {
            columnHeaders[i] = createColumnHeader(i);
            updateColumnHeader(i);
        }
        updateColumnNumbers();

        if(selectionManager != null) {
            selectionManager.modelChanged();
        }
        
        updateAll();
    }

    protected void modelRowChanged(int row) {
        if(rowModel != null) {
            if(autoSizeRow(row)) {
                invalidateParentLayout();
            }
        }
        for(int col=0 ; col<numColumns ; col++) {
            updateCellWidget(row, col);
        }
        invalidateLayout();
    }

    protected void modelRowsChanged(int idx, int count) {
        boolean rowHeightChanged = false;
        for(int i=0 ; i<count ; i++) {
            if(rowModel != null) {
                rowHeightChanged |= autoSizeRow(idx+i);
            }
            for(int col=0 ; col<numColumns ; col++) {
                updateCellWidget(idx+i, col);
            }
        }
        invalidateLayout();
        if(rowHeightChanged) {
            invalidateParentLayout();
        }
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
        if(row < getRowFromPosition(scrollPosY)) {
            ScrollPane sp = ScrollPane.getContainingScrollPane(this);
            if(sp != null) {
                int rowsStart = getRowStartPosition(row);
                int rowsEnd = getRowEndPosition(row + count - 1);
                sp.setScrollPositionY(scrollPosY + rowsEnd - rowsStart);
            }
        }
        if(selectionManager != null) {
            selectionManager.rowsInserted(row, count);
        }
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelRowsDeleted(int row, int count) {
        if(row+count <= getRowFromPosition(scrollPosY)) {
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
        if(selectionManager != null) {
            selectionManager.rowsDeleted(row, count);
        }
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelColumnsInserted(int column, int count) {
        ColumnHeader[] newColumnHeaders = new ColumnHeader[numColumns];
        System.arraycopy(columnHeaders, 0, newColumnHeaders, 0, column);
        System.arraycopy(columnHeaders, column, newColumnHeaders, column+count,
                numColumns - (column+count));
        for(int i=0 ; i<count ; i++) {
            newColumnHeaders[column+i] = createColumnHeader(column+i);
        }
        columnHeaders = newColumnHeaders;
        updateColumnNumbers();

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

        removeColumnHeaders(column, count);

        ColumnHeader[] newColumnHeaders = new ColumnHeader[numColumns];
        System.arraycopy(columnHeaders, 0, newColumnHeaders, 0, column);
        System.arraycopy(columnHeaders, column+count, newColumnHeaders, column, numColumns - count);
        columnHeaders = newColumnHeaders;
        updateColumnNumbers();
        
        invalidateLayout();
        invalidateParentLayout();
    }

    protected void modelColumnHeaderChanged(int column) {
        updateColumnHeader(column);
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

    protected class ColumnSizeSequence extends SizeSequence {
        @Override
        protected void initializeSizes(int index, int count) {
            if(isFixedWidthMode()) {
                int preferredWidth = computePreferredColumnsWidth();
                for(int i=0 ; i<count ; i++,index++) {
                    table[index] = computeFixedColumnWidth(index, preferredWidth);
                }
            } else {
                for(int i=0 ; i<count ; i++,index++) {
                    table[index] = computeColumnWidth(index);
                }
            }
        }
        protected boolean update(int index) {
            int width = computeColumnWidth(index);
            return setSize(index, width);
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

    protected class ColumnHeader extends Button {
        private int column;
        private int columnWidth;

        public int getColumnWidth() {
            return columnWidth;
        }

        public void setColumnWidth(int columnWidth) {
            this.columnWidth = columnWidth;
        }

        @Override
        public int getPreferredWidth() {
            if(columnWidth > 0) {
                return columnWidth;
            }
            int prefWidth = super.getPreferredWidth();
            return Math.max(prefWidth, getDefaultColumnWidth(column));
        }

        @Override
        public void adjustSize() {
            // don't do anything
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

        @Override
        public void applyTheme(ThemeInfo themeInfo) {
            super.applyTheme(themeInfo);
        }

        public void setCellData(int row, int column, Object data) {
            setText(String.valueOf(data));
        }

        public int getColumnSpan() {
            return 1;
        }

        public Widget getCellRenderWidget(int x, int y, int width, int height, boolean isSelected) {
            setPosition(x, y);
            setSize(width, height);
            getAnimationState().setAnimationState(STATE_SELECTED, isSelected);
            return this;
        }
    }
}
