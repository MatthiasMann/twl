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
package de.matthiasmann.twl.model;

import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public abstract class AbstractTreeTableNode implements TreeTableNode {

    private final TreeTableNode parent;
    private ArrayList<TreeTableNode> childs;
    private boolean leaf;

    protected AbstractTreeTableNode(TreeTableNode parent) {
        if(parent == null) {
            throw new NullPointerException("parent");
        }
        this.parent = parent;
        assert getTreeTableModel() != null;
    }

    public TreeTableNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public int getNumChildren() {
        return (childs != null) ? childs.size() : 0;
    }

    public TreeTableNode getChild(int idx) {
        return childs.get(idx);
    }

    public int getChildIndex(TreeTableNode child) {
        if(childs != null) {
            for(int i=0,n=childs.size() ; i<n ; i++) {
                if(childs.get(i) == child) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    protected void insertChild(TreeTableNode node, int idx) {
        assert getChildIndex(node) < 0;
        assert node.getParent() == this;
        if(childs == null) {
            childs = new ArrayList<TreeTableNode>();
        }
        childs.add(idx, node);
        getTreeTableModel().fireNodesAdded(this, idx, 1);
    }

    protected void removeChild(int idx) {
        childs.remove(idx);
        getTreeTableModel().fireNodesRemoved(this, idx, 1);
    }

    protected AbstractTreeTableModel getTreeTableModel() {
        TreeTableNode n = parent;
        for(;;) {
            TreeTableNode p = n.getParent();
            if(p == null) {
                return (AbstractTreeTableModel)n;
            }
            n = p;
        }
    }
}
