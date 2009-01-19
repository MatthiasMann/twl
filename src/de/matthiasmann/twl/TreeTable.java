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

import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.TableModel;
import de.matthiasmann.twl.model.ToggleButtonModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.HashEntry;
import de.matthiasmann.twl.utils.SizeSequence;

/**
 *
 * @author Matthias Mann
 */
public class TreeTable extends TableBase {

    private NodeState[] nodeStateTable;
    private TreeTableModel model;
    private NodeState rootNodeState;

    public TreeTable() {
        nodeStateTable = new NodeState[64];
        super.registerCellRenderer(TreeTableNode.class, new TreeLeafCellRenderer());
        super.registerCellRenderer(NodeState.class, new TreeNodeCellRenderer());
    }

    public TreeTable(TreeTableModel model) {
        this();
        setModel(model);
        modelAllChanged();
    }

    public void setModel(TableModel model) {
        throw new UnsupportedOperationException();
    }

    public void setModel(TreeTableModel model) {
        this.model = model;
        this.nodeStateTable = new NodeState[64];
        this.rootNodeState = createNodeState(model);
        this.rootNodeState.initChildSizes();
        this.numRows = getNumRows();
        this.numColumns = model.getNumColumns();
    }

    public int getRowFromNode(TreeTableNode node) {
        int position = -1;
        TreeTableNode parent = node.getParent();
        while(parent != null) {
            NodeState ns = HashEntry.get(nodeStateTable, parent);
            int idx = parent.getChildIndex(node);
            if(ns.childSizes != null) {
                idx = ns.childSizes.getPosition(idx);
            }
            position += idx + 1;
            node = parent;
            parent = node.getParent();
        }
        return position;
    }

    public TreeTableNode getNodeFromRow(int row) {
        TreeTableNode node = model;
        row++;
        while(row > 0) {
            NodeState ns = HashEntry.get(nodeStateTable, node);
            if(ns.childSizes != null) {
                int idx = ns.childSizes.getIndex(row - 1);
                row -= ns.childSizes.getPosition(idx) + 1;
                node = ns.key.getChild(idx);
            } else {
                node = ns.key.getChild(row - 1);
                row  = 0;
            }
        }
        return node;
    }

    protected NodeState getOrCreateNodeState(TreeTableNode node) {
        NodeState ns = HashEntry.get(nodeStateTable, node);
        if(ns == null) {
            ns = createNodeState(node);
        }
        return ns;
    }

    protected NodeState createNodeState(TreeTableNode node) {
        TreeTableNode parent = node.getParent();
        NodeState nsParent = null;
        if(parent != null) {
            nsParent = HashEntry.get(nodeStateTable, parent);
            assert nsParent != null;
        }
        NodeState newNS = new NodeState(node, nsParent);
        HashEntry.insertEntry(nodeStateTable, newNS);
        return newNS;
    }

    protected void expandedChanged(NodeState ns) {
        TreeTableNode node = ns.key;
        int count = ns.getChildRows();
        int size = ns.expanded ? count : 0;
        
        TreeTableNode parent = node.getParent();
        while(parent != null) {
            NodeState nsParent = HashEntry.get(nodeStateTable, parent);
            if(nsParent.childSizes == null) {
                nsParent.initChildSizes();
            }
            
            int idx = nsParent.key.getChildIndex(node);
            nsParent.childSizes.setSize(idx, size + 1);
            size = nsParent.childSizes.getEndPosition();

            node = parent;
            parent = node.getParent();
        }

        numRows = getNumRows();
        int row = getRowFromNode(ns.key);
        if(ns.expanded) {
            modelRowsInserted(row+1, count);
        } else {
            modelRowsDeleted(row+1, count);
        }
    }

    protected int getNumRows() {
        return rootNodeState.childSizes.getEndPosition();
    }

    @Override
    protected Object getCellData(int row, int column, TreeTableNode node) {
        if(node == null) {
            node = getNodeFromRow(row);
        }
        if(column == 0) {
            if(node.isLeaf()) {
                return node;
            }
            return getOrCreateNodeState(node);
        }
        return node.getData(column);
    }

    protected class NodeState extends HashEntry<TreeTableNode, NodeState> implements BooleanModel {
        final NodeState parent;
        boolean expanded;
        SizeSequence childSizes;
        int level;

        public NodeState(TreeTableNode key, NodeState parent) {
            super(key);
            this.parent = parent;
            this.level = (parent != null) ? parent.level + 1 : 0;
        }

        public void addCallback(Runnable callback) {
        }

        public void removeCallback(Runnable callback) {
        }

        public boolean getValue() {
            return expanded;
        }

        public void setValue(boolean value) {
            if(this.expanded != value) {
                this.expanded = value;
                expandedChanged(this);
            }
        }

        void initChildSizes() {
            childSizes = new SizeSequence();
            childSizes.setDefaultValue(1);
            childSizes.initializeAll(key.getNumChildren());
        }

        int getChildRows() {
            if(childSizes != null) {
                return childSizes.getEndPosition();
            }
            return key.getNumChildren();
        }
    }

    static class TreeLeafCellRenderer extends TextWidget implements CellRenderer {
        protected int treeIndent;
        protected int level;
        protected Dimension treeButtonSize = new Dimension(5, 5);

        public TreeLeafCellRenderer() {
            setClip(true);
        }
        
        @Override
        protected int computeTextX() {
            return super.computeTextX() + level * treeIndent + treeButtonSize.getX();
        }

        public void setCellData(int row, int column, Object data) {
            TreeTableNode node = (TreeTableNode)data;
            Object colData = node.getData(column);
            setText(String.valueOf(colData));
            level = getLevel(node);
        }

        public void setThemeParameters(ParameterMap themeParams) {
            treeIndent = themeParams.getParameter("treeIndent", 10);
            treeButtonSize = themeParams.getParameterValue("treeButtonSize", true, Dimension.class);
        }

        private static int getLevel(TreeTableNode node) {
            int level = -1;
            while(node != null) {
                level++;
                node = node.getParent();
            }
            return level;
        }
    }
    static class TreeNodeCellRenderer extends TreeLeafCellRenderer implements CellWidgetCreator {
        public Widget updateWidget(int row, int column, Object data, Widget existingWidget) {
            ToggleButton tb = (ToggleButton)existingWidget;
            if(tb == null) {
                tb = new ToggleButton();
                tb.setTheme("treeButton");
            }
            ((ToggleButtonModel)tb.getModel()).setModel((NodeState)data);
            return tb;
        }

        public void positionWidget(int row, int column, Object data, Widget widget, int x, int y, int w, int h) {
            int indent = ((NodeState)data).level * treeIndent;
            widget.setPosition(x + indent, y + (h-treeButtonSize.getY())/2);
            widget.setSize(treeButtonSize.getX(), treeButtonSize.getY());
        }

        @Override
        public void setCellData(int row, int column, Object data) {
            NodeState node = (NodeState)data;
            Object colData = node.key.getData(column);
            setText(String.valueOf(colData));
            level = node.level;
        }
    }
}
