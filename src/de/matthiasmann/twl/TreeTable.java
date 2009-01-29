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
import de.matthiasmann.twl.model.ToggleButtonModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.HashEntry;
import de.matthiasmann.twl.utils.SizeSequence;

/**
 * A Tree+Table widget.
 *
 * @author Matthias Mann
 */
public class TreeTable extends TableBase {

    private final ModelChangeListener modelChangeListener;
    private final TreeLeafCellRenderer leafRenderer;
    private final TreeNodeCellRenderer nodeRenderer;

    private NodeState[] nodeStateTable;
    private TreeTableModel model;
    private NodeState rootNodeState;

    public TreeTable() {
        modelChangeListener = new ModelChangeListener();
        nodeStateTable = new NodeState[64];
        leafRenderer = new TreeLeafCellRenderer();
        nodeRenderer = new TreeNodeCellRenderer();
        hasCellWidgetCreators = true;
    }

    public TreeTable(TreeTableModel model) {
        this();
        setModel(model);
        modelAllChanged();
    }

    public void setModel(TreeTableModel model) {
        if(this.model != null) {
            this.model.removeChangeListener(modelChangeListener);
        }
        this.columnHeaderModel = model;
        this.model = model;
        this.nodeStateTable = new NodeState[64];
        if(this.model != null) {
            this.model.addChangeListener(modelChangeListener);
            this.rootNodeState = createNodeState(model);
            this.rootNodeState.level = -1;
            this.rootNodeState.expanded = true;
            this.rootNodeState.initChildSizes();
            this.numRows = getNumRows();
            this.numColumns = model.getNumColumns();
        } else {
            this.rootNodeState = null;
            this.numRows = 0;
            this.numColumns = 0;
        }
        invalidateLayout();
        invalidateParentLayout();
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeTreeTable(themeInfo);
    }

    protected void applyThemeTreeTable(ThemeInfo themeInfo) {
        applyCellRendererTheme(leafRenderer);
        applyCellRendererTheme(nodeRenderer);
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
        NodeState ns = rootNodeState;
        for(;;) {
            int idx;
            if(ns.childSizes == null) {
                idx = Math.min(ns.key.getNumChildren()-1, row);
                row -= idx + 1;
            } else {
                idx = ns.childSizes.getIndex(row);
                row -= ns.childSizes.getPosition(idx) + 1;
            }
            if(row < 0) {
                return ns.key.getChild(idx);
            }
            assert ns.children[idx] != null;
            ns = ns.children[idx];
        }
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
        return node.getData(column);
    }

    @Override
    protected CellRenderer getCellRenderer(int row, int col, TreeTableNode node) {
        if(node == null) {
            node = getNodeFromRow(row);
        }
        if(col == 0) {
            Object data = node.getData(col);
            if(node.isLeaf()) {
                leafRenderer.setCellData(row, col, data, node);
                return leafRenderer;
            }
            NodeState nodeState = getOrCreateNodeState(node);
            nodeRenderer.setCellData(row, col, data, nodeState);
            return nodeRenderer;
        }
        return super.getCellRenderer(row, col, node);
    }

    private boolean updateParentSizes(NodeState ns) {
        while(ns.expanded && ns.parent != null) {
            NodeState parent = ns.parent;
            int idx = parent.key.getChildIndex(ns.key);
            assert parent.childSizes.size() == parent.key.getNumChildren();
            parent.childSizes.setSize(idx, ns.getChildRows() + 1);
            ns = parent;
        }
        numRows = getNumRows();
        return ns.parent == null;
    }
    
    protected void modelNodesAdded(TreeTableNode parent, int idx, int count) {
        NodeState ns = HashEntry.get(nodeStateTable, parent);
        // if ns is null then this node has not yet been displayed
        if(ns != null) {
            if(ns.childSizes != null) {
                assert idx <= ns.childSizes.size();
                ns.childSizes.insert(idx, count);
                assert ns.childSizes.size() == parent.getNumChildren();
            }
            if(ns.children != null) {
                NodeState[] newChilds = new NodeState[parent.getNumChildren()];
                System.arraycopy(ns.children, 0, newChilds, 0, idx);
                System.arraycopy(ns.children, idx, newChilds, idx+count, ns.children.length - idx);
                ns.children = newChilds;
            }
            if(updateParentSizes(ns)) {
                int row = getRowFromNode(parent.getChild(idx));
                assert row < numRows;
                modelRowsInserted(row, count);
            }
        }
    }

    protected void recursiveRemove(NodeState ns) {
        if(ns != null) {
            HashEntry.remove(nodeStateTable, ns);
            if(ns.children != null) {
                for(NodeState nsChild : ns.children) {
                    recursiveRemove(nsChild);
                }
            }
        }
    }

    protected void modelNodesRemoved(TreeTableNode parent, int idx, int count) {
        NodeState ns = HashEntry.get(nodeStateTable, parent);
        // if ns is null then this node has not yet been displayed
        if(ns != null) {
            int rowsBase = getRowFromNode(parent) + 1;
            int rowsStart = rowsBase + idx;
            int rowsEnd = rowsBase + idx + count;
            if(ns.childSizes != null) {
                assert ns.childSizes.size() == parent.getNumChildren() + count;
                rowsStart = rowsBase + ns.childSizes.getPosition(idx);
                rowsEnd = rowsBase + ns.childSizes.getPosition(idx + count);
                ns.childSizes.remove(idx, count);
                assert ns.childSizes.size() == parent.getNumChildren();
            }
            if(ns.children != null) {
                for(int i=0 ; i<count ; i++) {
                    recursiveRemove(ns.children[idx+i]);
                }
                int numChildren = parent.getNumChildren();
                if(numChildren > 0) {
                    NodeState[] newChilds = new NodeState[numChildren];
                    System.arraycopy(ns.children, 0, newChilds, 0, idx);
                    System.arraycopy(ns.children, idx+count, newChilds, idx, newChilds.length - idx);
                    ns.children = newChilds;
                } else {
                    ns.children = null;
                }
            }
            if(updateParentSizes(ns)) {
                modelRowsDeleted(rowsStart, rowsEnd - rowsStart);
            }
        }
    }

    protected boolean isVisible(NodeState ns) {
        while(ns.expanded && ns.parent != null) {
            ns = ns.parent;
        }
        return ns.expanded;
    }
    
    protected void modelNodesChanged(TreeTableNode parent, int idx, int count) {
        NodeState ns = HashEntry.get(nodeStateTable, parent);
        // if ns is null then this node has not yet been displayed
        if(ns != null && isVisible(ns)) {
            int rowsBase = getRowFromNode(parent) + 1;
            int rowsStart = rowsBase + idx;
            int rowsEnd = rowsBase + idx + count;
            if(ns.childSizes != null) {
                rowsStart = rowsBase + ns.childSizes.getPosition(idx);
                rowsEnd = rowsBase + ns.childSizes.getPosition(idx + count);
            }
            modelRowsChanged(rowsStart, rowsEnd - rowsStart);
        }
    }

    protected class ModelChangeListener implements TreeTableModel.ChangeListener {
        public void nodesAdded(TreeTableNode parent, int idx, int count) {
            modelNodesAdded(parent, idx, count);
        }
        public void nodesRemoved(TreeTableNode parent, int idx, int count) {
            modelNodesRemoved(parent, idx, count);
        }
        public void nodesChanged(TreeTableNode parent, int idx, int count) {
            modelNodesChanged(parent, idx, count);
        }
        public void columnInserted(int idx, int count) {
            numColumns = model.getNumColumns();
            modelColumnsInserted(idx, count);
        }
        public void columnDeleted(int idx, int count) {
            numColumns = model.getNumColumns();
            modelColumnsDeleted(idx, count);
        }
        public void columnHeaderChanged(int column) {
            modelColumnHeaderChanged(column);
        }
    }

    protected class NodeState extends HashEntry<TreeTableNode, NodeState> implements BooleanModel {
        final NodeState parent;
        boolean expanded;
        SizeSequence childSizes;
        NodeState[] children;
        int level;

        public NodeState(TreeTableNode key, NodeState parent) {
            super(key);
            this.parent = parent;
            this.level = (parent != null) ? parent.level + 1 : 0;

            if(parent != null) {
                if(parent.children == null) {
                    parent.children = new NodeState[parent.key.getNumChildren()];
                }
                parent.children[parent.key.getChildIndex(key)] = this;
            }
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

    static int getLevel(TreeTableNode node) {
        int level = -2;
        while(node != null) {
            level++;
            node = node.getParent();
        }
        return level;
    }

    class TreeLeafCellRenderer implements CellRenderer {
        protected int treeIndent;
        protected int level;
        protected Dimension treeButtonSize = new Dimension(5, 5);
        protected CellRenderer subRenderer;

        public TreeLeafCellRenderer() {
            setClip(true);
        }

        public void applyTheme(ThemeInfo themeInfo) {
            treeIndent = themeInfo.getParameter("treeIndent", 10);
            treeButtonSize = themeInfo.getParameterValue("treeButtonSize", true, Dimension.class, Dimension.ZERO);
        }

        public String getTheme() {
            return getClass().getSimpleName();
        }
        
        public void setCellData(int row, int column, Object data) {
            throw new UnsupportedOperationException("Don't call this method");
        }

        public void setCellData(int row, int column, Object data, TreeTableNode node) {
            level = getLevel(node);
            setSubRenderer(data);
        }

        protected void setSubRenderer(Object colData) {
            subRenderer = getCellRenderer(colData);
            if(subRenderer != null) {
                subRenderer.setCellData(level, numColumns, colData);
            }
        }

        public int getColumnSpan() {
            return (subRenderer != null) ? subRenderer.getColumnSpan() : 1;
        }

        public int getPreferredHeight() {
            if(subRenderer != null) {
                return Math.max(treeButtonSize.getY(), subRenderer.getPreferredHeight());
            }
            return treeButtonSize.getY();
        }

        public Widget getCellRenderWidget(int x, int y, int width, int height) {
            if(subRenderer != null) {
                int indent = level * treeIndent + treeButtonSize.getX();
                Widget widget = subRenderer.getCellRenderWidget(
                        x + indent, y, Math.max(0, width-indent), height);
                return widget;
            }
            return null;
        }
    }

    class TreeNodeCellRenderer extends TreeLeafCellRenderer implements CachableCellWidgetCreator {
        private NodeState nodeState;

        public Widget updateWidget(Widget existingWidget) {
            ToggleButton tb = (ToggleButton)existingWidget;
            if(tb == null) {
                tb = new ToggleButton();
                tb.setTheme("treeButton");
            }
            ((ToggleButtonModel)tb.getModel()).setModel(nodeState);
            return tb;
        }

        public void positionWidget(Widget widget, int x, int y, int w, int h) {
            int indent = level * treeIndent;
            widget.setPosition(x + indent, y + (h-treeButtonSize.getY())/2);
            widget.setSize(treeButtonSize.getX(), treeButtonSize.getY());
        }

        public void setCellData(int row, int column, Object data, NodeState nodeState) {
            this.nodeState = nodeState;
            setSubRenderer(data);
            level = nodeState.level;
        }

        public String getCacheTag(int row, int column) {
            return "treeButton";
        }
    }
}
