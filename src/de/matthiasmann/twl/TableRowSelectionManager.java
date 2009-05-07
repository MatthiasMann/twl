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

import de.matthiasmann.twl.model.DefaultTableSelectionModel;
import de.matthiasmann.twl.model.TableSelectionModel;

/**
 *
 * @author Matthias Mann
 */
public class TableRowSelectionManager implements TableSelectionManager {

    protected final ActionMap actionMap;
    protected final TableSelectionModel selectionModel;

    protected TableBase tableBase;

    public TableRowSelectionManager(TableSelectionModel selectionModel) {
        if(selectionModel == null) {
            throw new NullPointerException("selectionModel");
        }
        this.selectionModel = selectionModel;
        this.actionMap = new ActionMap();

        actionMap.addMapping(this);
    }

    public TableRowSelectionManager() {
        this(new DefaultTableSelectionModel());
    }

    public void setAssociatedTable(TableBase base) {
        if(tableBase != base) {
            if(tableBase != null && base != null) {
                throw new IllegalStateException("selection manager still in use");
            }
            this.tableBase = base;
            modelChanged();
        }
    }

    public SelectionGranularity getSelectionGranularity() {
        return SelectionGranularity.ROWS;
    }

    public boolean handleKeyStrokeAction(String action, Event event) {
        return actionMap.invoke(action, event);
    }

    public boolean handleMouseEvent(int row, int column, Event event) {
        if(event.getType() == Event.Type.MOUSE_BTNDOWN && event.getMouseButton() == Event.MOUSE_LBUTTON) {
            boolean isShift = (event.getModifiers() & Event.MODIFIER_SHIFT) != 0;
            boolean isCtrl = (event.getModifiers() & Event.MODIFIER_CTRL) != 0;
            handleMouseClick(row, column, isShift, isCtrl);
            return true;
        }
        return false;
    }

    public boolean isRowSelected(int row) {
        return selectionModel.isSelected(row);
    }

    public boolean isCellSelected(int row, int column) {
        return false;
    }

    public int getLeadRow() {
        return selectionModel.getLeadIndex();
    }

    public int getLeadColumn() {
        return -1;
    }

    public void modelChanged() {
        selectionModel.clearSelection();
        selectionModel.setAnchorIndex(-1);
        selectionModel.setLeadIndex(-1);
    }

    public void rowsInserted(int index, int count) {
        selectionModel.rowsInserted(index, count);
    }

    public void rowsDeleted(int index, int count) {
        selectionModel.rowsDeleted(index, count);
    }

    public void columnInserted(int index, int count) {
    }

    public void columnsDeleted(int index, int count) {
    }

    @ActionMap.Action
    public void selectNextRow() {
        handleRelativeAction(1, SET);
    }

    @ActionMap.Action
    public void selectPreviousRow() {
        handleRelativeAction(-1, SET);
    }

    @ActionMap.Action
    public void selectNextPage() {
        handleRelativeAction(getPageSize(), SET);
    }

    @ActionMap.Action
    public void selectPreviousPage() {
        handleRelativeAction(-getPageSize(), SET);
    }

    @ActionMap.Action
    public void selectFirstRow() {
        int numRows = getNumRows();
        if(numRows > 0) {
            handleAbsoluteAction(0, SET);
        }
    }

    @ActionMap.Action
    public void selectLastRow() {
        int numRows = getNumRows();
        if(numRows > 0) {
            handleRelativeAction(numRows-1, SET);
        }
    }

    @ActionMap.Action
    public void extendSelectionToNextRow() {
        handleRelativeAction(1, EXTEND);
    }

    @ActionMap.Action
    public void extendSelectionToPreviousRow() {
        handleRelativeAction(-1, EXTEND);
    }

    @ActionMap.Action
    public void extendSelectionToNextPage() {
        handleRelativeAction(getPageSize(), EXTEND);
    }

    @ActionMap.Action
    public void extendSelectionToPreviousPage() {
        handleRelativeAction(-getPageSize(), EXTEND);
    }

    @ActionMap.Action
    public void extendSelectionToFirstRow() {
        int numRows = getNumRows();
        if(numRows > 0) {
            handleAbsoluteAction(0, EXTEND);
        }
    }

    @ActionMap.Action
    public void extendSelectionToLastRow() {
        int numRows = getNumRows();
        if(numRows > 0) {
            handleRelativeAction(numRows-1, EXTEND);
        }
    }

    @ActionMap.Action
    public void moveLeadToNextRow() {
        handleRelativeAction(1, MOVE);
    }

    @ActionMap.Action
    public void moveLeadToPreviousRow() {
        handleRelativeAction(-1, MOVE);
    }

    @ActionMap.Action
    public void moveLeadToNextPage() {
        handleRelativeAction(getPageSize(), MOVE);
    }

    @ActionMap.Action
    public void moveLeadToPreviousPage() {
        handleRelativeAction(-getPageSize(), MOVE);
    }

    @ActionMap.Action
    public void moveLeadToFirstRow() {
        int numRows = getNumRows();
        if(numRows > 0) {
            handleAbsoluteAction(0, MOVE);
        }
    }

    @ActionMap.Action
    public void moveLeadToLastRow() {
        int numRows = getNumRows();
        if(numRows > 0) {
            handleAbsoluteAction(numRows-1, MOVE);
        }
    }

    @ActionMap.Action
    public void toggleSelectionOnLeadRow() {
        int leadIndex = selectionModel.getLeadIndex();
        if(leadIndex > 0) {
            selectionModel.invertSelection(leadIndex, leadIndex);
        }
    }

    @ActionMap.Action
    public void selectAll() {
        int numRows = getNumRows();
        if(numRows > 0) {
            selectionModel.setSelection(0, numRows-1);
        }
    }

    @ActionMap.Action
    public void selectNone() {
        selectionModel.clearSelection();
    }

    protected static final int TOGGLE = 0;
    protected static final int EXTEND = 1;
    protected static final int SET = 2;
    protected static final int MOVE = 3;

    protected void handleRelativeAction(int delta, int mode) {
        int numRows = getNumRows();
        if(numRows > 0) {
            int leadIndex = Math.max(0, selectionModel.getLeadIndex());
            int index = Math.max(0, Math.min(numRows-1, leadIndex + delta));

            handleAbsoluteAction(index, mode);
        }
    }

    protected void handleAbsoluteAction(int index, int mode) {
        if(tableBase != null) {
            tableBase.adjustScrollPosition(index);
        }

        switch (mode) {
            case MOVE:
                selectionModel.setLeadIndex(index);
                break;
            case EXTEND:
                int anchorIndex = Math.max(0, selectionModel.getAnchorIndex());
                selectionModel.setSelection(anchorIndex, index);
                break;
            case TOGGLE:
                selectionModel.invertSelection(index, index);
                break;
            default:
                selectionModel.setSelection(index, index);
                break;
        }
    }

    protected void handleMouseClick(int row, int column, boolean isShift, boolean isCtrl) {
        if(row < 0) {
            if(!isShift) {
                selectionModel.clearSelection();
            }
        } else {
            tableBase.adjustScrollPosition(row);
            int anchorIndex = selectionModel.getAnchorIndex();
            boolean anchorSelected;
            if(anchorIndex == -1) {
                anchorIndex = 0;
                anchorSelected = false;
            } else {
                anchorSelected = selectionModel.isSelected(anchorIndex);
            }

            if(isCtrl) {
                if(isShift) {
                    if(anchorSelected) {
                        selectionModel.addSelection(anchorIndex, row);
                    } else {
                        selectionModel.removeSelection(anchorIndex, row);
                    }
                } else if(selectionModel.isSelected(row)) {
                    selectionModel.removeSelection(row, row);
                } else {
                    selectionModel.addSelection(row, row);
                }
            } else if(isShift) {
                selectionModel.setSelection(anchorIndex, row);
            } else {
                selectionModel.setSelection(row, row);
            }
        }
    }

    protected int getNumRows() {
        if(tableBase != null) {
            return tableBase.getNumRows();
        }
        return 0;
    }

    protected int getPageSize() {
        if(tableBase != null) {
            return Math.max(1, tableBase.getNumVisibleRows());
        }
        return 1;
    }
}