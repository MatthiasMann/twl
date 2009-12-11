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
package de.matthiasmann.twl.utils;

import java.util.Arrays;

/**
 * A 2d sparse grid built using a B+Tree.
 * Rows are the major axis. Operations on column ranges are slower.
 *
 * @author Matthias Mann
 */
public class SparseGrid {

    public interface GridFunction {
        public void apply(int row, int column, Entry e);
    }

    Node root;
    int numLevels;

    public SparseGrid(int pageSize) {
        root = new Node(pageSize);
        numLevels = 1;
    }

    public Entry get(int row, int column) {
        if(root.size > 0) {
            int levels = numLevels;
            Entry e = root;

            do {
                Node node = (Node)e;
                int pos = node.findPos(row, column, node.size);
                if(pos == node.size) {
                    return null;
                }
                e = node.children[pos];
            }while(--levels > 0);

            assert e != null;
            if(e.compare(row, column) == 0) {
                return e;
            }
        }
        return null;
    }

    public void set(int row, int column, Entry entry) {
        entry.row = row;
        entry.column = column;

        if(root.size == 0) {
            root.insertAt(0, entry);
        } else if(!root.insert(entry, numLevels)) {
            splitRoot();
            root.insert(entry, numLevels);
        }
    }

    public Entry remove(int row, int column) {
        if(root.size == 0) {
            return null;
        }
        Entry e = root.remove(row, column, numLevels);
        if(e != null) {
            maybeRemoveRoot();
        }
        return e;
    }

    public void insertRows(int row, int count) {
        if(count > 0) {
            root.insertRows(row, count, numLevels);
        }
    }

    public void insertColumns(int column, int count) {
        if(count > 0) {
            root.insertColumns(column, count, numLevels);
        }
    }

    public void removeRows(int row, int count) {
        if(count > 0) {
            root.removeRows(row, count, numLevels);
            maybeRemoveRoot();
        }
    }

    public void removeColumns(int column, int count) {
        if(count > 0) {
            root.removeColumns(column, count, numLevels);
            maybeRemoveRoot();
        }
    }

    public void iterate(int startRow, int startColumn,
            int endRow, int endColumn, GridFunction func) {
        if(root.size > 0) {
            int levels = numLevels;
            Entry e = root;
            Node node;
            int pos;

            do {
                node = (Node)e;
                pos = node.findPos(startRow, startColumn, node.size-1);
                e = node.children[pos];
            }while(--levels > 0);

            assert e != null;
            if(e.compare(startRow, startColumn) < 0) {
                return;
            }

            do {
                for(int size=node.size ; pos<size ; pos++) {
                    e = node.children[pos];
                    if(e.row > endRow) {
                        return;
                    }
                    if(e.column >= startColumn && e.column <= endColumn) {
                        func.apply(e.row, e.column, e);
                    }
                }
                pos = 0;
                node = node.next;
            } while(node != null);
        }
    }

    public boolean isEmpty() {
        return root.size == 0;
    }

    public void clear() {
        Arrays.fill(root.children, null);
        root.size = 0;
        numLevels = 1;
    }

    private void maybeRemoveRoot() {
        while(numLevels > 1 && root.size == 1) {
            root = (Node)root.children[0];
            root.prev = null;
            root.next = null;
            numLevels--;
        }
    }

    private void splitRoot() {
        Node newNode = root.split();
        Node newRoot = new Node(root.children.length);
        newRoot.children[0] = root;
        newRoot.children[1] = newNode;
        newRoot.size = 2;
        root = newRoot;
        numLevels++;
    }

    static class Node extends Entry {
        final Entry[] children;
        int size;
        Node next;
        Node prev;

        public Node(int size) {
            this.children = new Entry[size];
        }

        boolean insert(Entry e, int levels) {
            if(--levels == 0) {
                return insertLeaf(e);
            }

            for(;;) {
                int pos = findPos(e.row, e.column, size-1);
                assert pos < size;
                Node node = (Node)children[pos];
                if(!node.insert(e, levels)) {
                    if(isFull()) {
                        return false;
                    }
                    Node node2 = node.split();
                    insertAt(pos+1, node2);
                    continue;
                }
                updateRowColumn();
                return true;
            }
        }

        boolean insertLeaf(Entry e) {
            int pos = findPos(e.row, e.column, size);
            if(pos < size) {
                Entry c = children[pos];
                assert c.getClass() != Node.class;
                int cmp = c.compare(e.row, e.column);
                if(cmp == 0) {
                    children[pos] = e;
                    return true;
                }
                assert cmp > 0;
            }

            if(isFull()) {
                return false;
            }
            insertAt(pos, e);
            return true;
        }

        Entry remove(int row, int column, int levels) {
            if(--levels == 0) {
                return removeLeaf(row, column);
            }

            int pos = findPos(row, column, size-1);
            assert pos < size;
            Node node = (Node)children[pos];
            Entry e = node.remove(row, column, levels);
            if(e != null) {
                if(size > 1 && node.isBelowHalf()) {
                    tryMerge(pos);
                }
            }
            updateRowColumn();
            return e;
        }

        Entry removeLeaf(int row, int column) {
            int pos = findPos(row, column, size);
            if(pos == size) {
                return null;
            }

            Entry c = children[pos];
            assert c.getClass() != Node.class;
            int cmp = c.compare(row, column);
            if(cmp == 0) {
                removeAt(pos);
                return c;
            }
            return null;
        }

        int findPos(int row, int column, int high) {
            int low = 0;
            while(low < high) {
                int mid = (low + high) / 2;
                Entry e = children[mid];
                int cmp = e.compare(row, column);
                if(cmp > 0) {
                    high = mid;
                } else if(cmp < 0) {
                    low = mid + 1;
                } else {
                    return mid;
                }
            }
            return low;
        }

        void insertRows(int row, int count, int levels) {
            if(--levels > 0) {
                for(int i=0 ; i<size ; i++) {
                    Node n = (Node)children[i];
                    if(n.row >= row) {
                        n.insertRows(row, count, levels);
                    }
                }
            } else {
                for(int i=0 ; i<size ; i++) {
                    Entry e = children[i];
                    if(e.row >= row) {
                        e.row += count;
                    }
                }
            }
            updateRowColumn();
        }

        void insertColumns(int column, int count, int levels) {
            if(--levels > 0) {
                for(int i=0 ; i<size ; i++) {
                    Node n = (Node)children[i];
                    n.insertColumns(column, count, levels);
                }
            } else {
                for(int i=0 ; i<size ; i++) {
                    Entry e = children[i];
                    if(e.column >= column) {
                        e.column += count;
                    }
                }
            }
            updateRowColumn();
        }

        void removeRows(int row, int count, int levels) {
            if(--levels > 0) {
                boolean needsMerging = false;
                for(int i=size ; i-->0 ;) {
                    Node n = (Node)children[i];
                    if(n.row >= row) {
                        n.removeRows(row, count, levels);
                        needsMerging |= n.isBelowHalf();
                        if(n.size == 0 && size > 1) {
                            removeNodeAt(i);
                        }
                    }
                }
                if(needsMerging) {
                    tryMerge();
                }
            } else {
                for(int i=size ; i-->0 ;) {
                    Entry e = children[i];
                    if(e.row >= row) {
                        e.row -= count;
                        if(e.row < row) {
                            removeAt(i);
                        }
                    }
                }
            }
            updateRowColumn();
        }

        void removeColumns(int column, int count, int levels) {
            if(--levels > 0) {
                boolean needsMerging = false;
                for(int i=size ; i-->0 ;) {
                    Node n = (Node)children[i];
                    n.removeColumns(column, count, levels);
                    needsMerging |= n.isBelowHalf();
                    if(n.size == 0 && size > 1) {
                        removeNodeAt(i);
                    }
                }
                if(needsMerging) {
                    tryMerge();
                }
            } else {
                for(int i=size ; i-->0 ;) {
                    Entry e = children[i];
                    if(e.column >= column) {
                        e.column -= count;
                        if(e.column < column) {
                            removeAt(i);
                        }
                    }
                }
            }
            updateRowColumn();
        }

        void insertAt(int idx, Entry what) {
            System.arraycopy(children, idx, children, idx+1, size-idx);
            children[idx] = what;
            if(idx == size++) {
                updateRowColumn();
            }
        }

        void removeAt(int idx) {
            size--;
            System.arraycopy(children, idx+1, children, idx, size-idx);
            children[size] = null;
            if(idx == size) {
                updateRowColumn();
            }
        }

        void removeNodeAt(int idx) {
            Node n = (Node)children[idx];
            if(n.next != null) {
                n.next.prev = n.prev;
            }
            if(n.prev != null) {
                n.prev.next = n.next;
            }
            n.next = null;
            n.prev = null;
            removeAt(idx);
        }

        void tryMerge() {
            for(int i=0 ; i<size && size>1 ; i++) {
                Node n = (Node)children[i];
                if(n.isBelowHalf()) {
                    tryMerge(i);
                }
            }
        }

        void tryMerge(int pos) {
            if(pos > 0) {
                if(pos+1 < size) {
                    tryMerge3(pos);
                } else {
                    tryMerge2(pos-1);
                }
            } else {
                tryMerge2(pos);
            }
        }

        private void tryMerge2(int pos) {
            Node n1 = (Node)children[pos];
            Node n2 = (Node)children[pos+1];
            if(n1.size + n2.size < children.length) {
                System.arraycopy(n2.children, 0, n1.children, n1.size, n2.size);
                n1.size += n2.size;
                n1.updateRowColumn();
                removeNodeAt(pos+1);
            }
        }

        private void tryMerge3(int pos) {
            Node n0 = (Node)children[pos-1];
            Node n1 = (Node)children[pos];
            Node n2 = (Node)children[pos+1];
            int maxSize = children.length;
            if(n0.size + n1.size + n2.size < 2*maxSize) {
                int s1l = n1.size / 2;
                int s1h = n1.size - s1l;
                if(n0.size + s1l < maxSize && n2.size + s1h < maxSize) {
                    System.arraycopy(n1.children, 0, n0.children, n0.size, s1l);
                    n0.size += s1l;
                    n0.updateRowColumn();

                    System.arraycopy(n1.children, s1l, n1.children, 0, s1h);
                    System.arraycopy(n2.children, 0, n1.children, s1h, n2.size);
                    n1.size = s1h + n2.size;
                    n1.updateRowColumn();

                    removeNodeAt(pos+1);
                }
            }
        }

        boolean isFull() {
            return size == children.length;
        }

        boolean isBelowHalf() {
            return size*2 < children.length;
        }

        Node split() {
            Node newNode = new Node(children.length);
            int size1 = size / 2;
            int size2 = size - size1;
            System.arraycopy(this.children, size1, newNode.children, 0, size2);
            Arrays.fill(this.children, size1, this.size, null);
            newNode.size = size2;
            newNode.updateRowColumn();
            newNode.prev = this;
            newNode.next = this.next;
            this.size = size1;
            this.updateRowColumn();
            this.next = newNode;
            if(newNode.next != null) {
                newNode.next.prev = newNode;
            }
            return newNode;
        }

        void updateRowColumn() {
            if(size > 0) {
                Entry e = children[size-1];
                this.row = e.row;
                this.column = e.column;
            }
        }
    }

    public static class Entry {
        int row;
        int column;

        int compare(int row, int column) {
            int diff = this.row - row;
            if(diff == 0) {
                diff = this.column - column;
            }
            return diff;
        }
    }
}
