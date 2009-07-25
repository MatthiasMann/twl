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

import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;

/**
 * A drop down combo box which shows a TreeTable and has a TreePathDisplay as label
 *
 * @author Matthias Mann
 */
public class TreeComboBox extends ComboBoxBase {

    public interface Callback {
        /**
         * The selected node has changed
         *
         * @param node the new selected node
         * @param previousChildNode if the new selected node is a parent of the previous node,
         *      then previousChildNode is a child of node which was selected before otherwise it's null
         */
        public void selectedNodeChanged(TreeTableNode node, TreeTableNode previousChildNode);
    }

    public interface PathResolver {
        /**
         * Tries to resolve the given string to a node
         *
         * @param model the tree model
         * @param path the path to resolve
         * @return A node - MUST NOT BE NULL
         * @throws IllegalArgumentException when the path can't be resolved, the message is displayed
         */
        public TreeTableNode resolvePath(TreeTableModel model, String path) throws IllegalArgumentException;
    }

    private final TableSingleSelectionModel selectionModel;
    private final TreePathDisplay display;
    private final TreeTable table;

    private TreeTableModel model;
    private Callback[] callbacks;
    private PathResolver pathResolver;

    public TreeComboBox() {
        selectionModel = new TableSingleSelectionModel();
        display = new TreePathDisplay();
        display.setTheme("display");
        table = new TreeTable();
        table.setSelectionManager(new TableRowSelectionManager(selectionModel) {
            @Override
            protected void handleMouseClick(int row, int column, boolean isShift, boolean isCtrl) {
                super.handleMouseClick(row, column, isShift, isCtrl);
                if(!isShift && !isCtrl && row >= 0 && row < getNumRows()) {
                    popup.closePopup();
                }
            }
        });

        display.addCallback(new TreePathDisplay.Callback() {
            public void pathElementClicked(TreeTableNode node, TreeTableNode child) {
                fireSelectedNodeChanged(node, child);
            }

            public boolean resolvePath(String path) {
                return TreeComboBox.this.resolvePath(path);
            }
        });

        selectionModel.addSelectionChangeListener(new Runnable() {
            public void run() {
                int row = selectionModel.getFirstSelected();
                if(row >= 0) {
                    nodeChanged(table.getNodeFromRow(row));
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(table);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);
        
        add(display);
        popup.setTheme("treecomboboxPopup");
        popup.add(scrollPane);
    }

    public TreeComboBox(TreeTableModel model) {
        this();
        setModel(model);
    }

    public TreeTableModel getModel() {
        return model;
    }

    public void setModel(TreeTableModel model) {
        if(this.model != model) {
            this.model = model;
            table.setModel(model);
            display.setCurrentNode(model);
        }
    }
    
    public void setCurrentNode(TreeTableNode node) {
        if(node == null) {
            throw new NullPointerException("node");
        }
        display.setCurrentNode(node);
    }

    public TreeTableNode getCurrentNode() {
        return display.getCurrentNode();
    }

    public void setSeparator(String separator) {
        display.setSeparator(separator);
    }

    public String getSeparator() {
        return display.getSeparator();
    }

    public PathResolver getPathResolver() {
        return pathResolver;
    }

    public void setPathResolver(PathResolver pathResolver) {
        this.pathResolver = pathResolver;
        display.setAllowEdit(pathResolver != null);
    }

    public void addCallback(Callback callback) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, callback, Callback.class);
    }
    
    public void removeCallback(Callback callback) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, callback);
    }

    @Override
    protected Widget getLabel() {
        return display;
    }

    void fireSelectedNodeChanged(TreeTableNode node, TreeTableNode child) {
        if(callbacks != null) {
            for(Callback cb : callbacks) {
                cb.selectedNodeChanged(node, child);
            }
        }
    }

    boolean resolvePath(String path) {
        if(pathResolver != null) {
            try {
                TreeTableNode node = pathResolver.resolvePath(model, path);
                assert node != null;
                nodeChanged(node);
                return true;
            } catch (IllegalArgumentException ex) {
                display.setEditErrorMessage(ex.getMessage());
            }
        }
        return false;
    }

    void nodeChanged(TreeTableNode node) {
        TreeTableNode oldNode = display.getCurrentNode();
        display.setCurrentNode(node);
        fireSelectedNodeChanged(node, getChildOf(node, oldNode));
    }

    private TreeTableNode getChildOf(TreeTableNode parent, TreeTableNode node) {
        while(node != null && node != parent) {
            node = node.getParent();
        }
        return node;
    }

    @Override
    protected boolean openPopup() {
        if(super.openPopup()) {
            table.collapseAll();
            int idx = table.getRowFromNodeExpand(display.getCurrentNode());
            selectionModel.setSelection(idx, idx);
            table.scrollToRow(Math.max(0, idx));
            return true;
        }
        return false;
    }
    
}
