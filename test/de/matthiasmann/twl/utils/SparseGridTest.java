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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit test for SparseGrid
 *
 * @author Matthias Mann
 */
public class SparseGridTest {

    public SparseGridTest() {
    }

    @Test
    public void testRandom1() {
        Helper h = new Helper(64);
        Random r = new Random(0x12345678);

        h.doRandomInsertsDeletes(r, 500000, 2000, 17);
        h.dumpStats();
    }

    @Test
    public void testRandom2() {
        Helper h = new Helper(16);
        Random r = new Random(0x87654321);

        h.doRandomInsertsDeletes(r, 500000, 5000, 50);
        h.dumpStats();
    }

    @Test
    public void testRandom3() {
        Helper h = new Helper(8);
        Random r = new Random(0x87654321);

        h.doRandomInsertsDeletes(r, 500000, 5000, 50);
        h.dumpStats();
    }

    static class Coord implements Comparable<Coord> {
        final int row;
        final int column;

        public Coord(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int compareTo(Coord o) {
            int diff = row - o.row;
            if(diff == 0) {
                diff = column - o.column;
            }
            return diff;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj != null && obj.getClass() == getClass()) {
                final Coord other = (Coord)obj;
                return this.row == other.row && this.column == other.column;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return row * 137 + column;
        }
    }

    static class Entry extends SparseGrid.Entry {
        final int nr;

        public Entry(int nr) {
            this.nr = nr;
        }
    }

    static class Helper {
        final SparseGrid sg;
        HashMap<Coord, Entry> map;
        int currentNr;
        int maxSize;
        int maxLevels;
        int numInsertRows;
        int numInsertCols;
        int numRemoveRows;
        int numRemoveCols;

        public Helper(int pageSize) {
            this.sg = new SparseGrid(pageSize);
            this.map = new HashMap<Coord, Entry>();
        }

        public void insert(int row, int column) {
            checkSize();
            check(row, column);
            Entry e = new Entry(currentNr++);
            sg.set(row, column, e);
            map.put(new Coord(row, column), e);
            maxSize = Math.max(maxSize, map.size());
            maxLevels = Math.max(maxLevels, sg.numLevels);
        }

        public void check(int row, int column) {
            try {
                Entry eSG = (Entry)sg.get(row, column);
                Entry eMap = map.get(new Coord(row, column));
                assertSame(eMap, eSG);
            } catch (AssertionError ex) {
                sg.get(row, column);
                throw ex;
            }
        }

        public void remove(int row, int column) {
            checkSize();
            check(row, column);
            Entry eSG = (Entry)sg.remove(row, column);
            Coord c = new Coord(row, column);
            Entry eMap = map.remove(c);
            assertSame(eMap, eSG);
            //checkAll();
        }

        private void rewriteMap(int rowStart, int rowOffset, int colStart, int colOffset) {
            HashMap<Coord, Entry> newMap = new HashMap<Coord, Entry>();
            for(Map.Entry<Coord, Entry> e : map.entrySet()) {
                Coord c = e.getKey();
                if(c.row < rowStart && c.column < colStart) {
                    newMap.put(c, e.getValue());
                } else {
                    int row = c.row;
                    int col = c.column;
                    if(row >= rowStart) {
                        row += rowOffset;
                        if(row < rowStart) {
                            continue;
                        }
                    }
                    if(col >= colStart) {
                        col += colOffset;
                        if(col < colStart) {
                            continue;
                        }
                    }
                    newMap.put(new Coord(row, col), e.getValue());
                }
            }
            map = newMap;
        }

        private void checkLinks(SparseGrid.Node node, SparseGrid.Node prev, SparseGrid.Node next, int levels) {
            assertTrue("empty node", node.size > 0);
            if(--levels == 0) {
                return;
            }
            SparseGrid.Node n = null;
            for(int i=0 ; i<node.size ; i++) {
                n = (SparseGrid.Node)node.children[i];
                assertSame(prev, n.prev);
                //assertFalse("below half", n.isBelowHalf() || node.size == 1);
                if(prev != null) {
                    assertSame(n, prev.next);
                }
                prev = n;
            }
            assertSame(next, n.next);

            if(levels > 1) {
                prev = null;
                if(node.prev != null) {
                    prev = getLast(node.prev);
                }
                for(int i=0 ; i<node.size ; i++) {
                    n = (SparseGrid.Node)node.children[i];
                    if(i+1 < node.size) {
                        next = (SparseGrid.Node)node.children[i+1];
                    } else {
                        next = getFirst(node.next);
                    }
                    checkLinks(n, getLast(prev), getFirst(next), levels);
                    prev = n;
                }
            }
        }
        private SparseGrid.Node getLast(SparseGrid.Node node) {
            if(node != null && node.size > 0) {
                return (SparseGrid.Node)node.children[node.size-1];
            }
            return null;
        }
        private SparseGrid.Node getFirst(SparseGrid.Node node) {
            if(node != null && node.size > 0) {
                return (SparseGrid.Node)node.children[0];
            }
            return null;
        }
        private void checkLinks() {
            if(!sg.isEmpty()) {
                checkLinks(sg.root, null, null, sg.numLevels);
            }
        }
        private int getSize(SparseGrid.Node n, int level) {
            if(--level==0) {
                return n.size;
            }
            int size = 0;
            for(int i=0 ; i<n.size ; ++i) {
                size += getSize((SparseGrid.Node)n.children[i], level);
            }
            return size;
        }
        private int getSize() {
            return getSize(sg.root, sg.numLevels);
        }

        private void checkSize() {
            int curSize = getSize();
            assertEquals(map.size(), curSize);
        }
        private void checkAll() {
            checkSize();
            checkLinks();
            for(Map.Entry<Coord, Entry> e : map.entrySet()) {
                Coord c = e.getKey();
                Entry eSG = (Entry)sg.get(c.row, c.column);
                assertSame(e.getValue(), eSG);
            }
        }
        
        public void insertRows(int row, int count) {
            //checkAll();
            sg.insertRows(row, count);
            rewriteMap(row, count, Integer.MAX_VALUE, 0);
            checkAll();
            numInsertRows++;
        }

        public void removeRows(int row, int count) {
            //checkAll();
            int oldSize = getSize();
            assertEquals(map.size(), oldSize);
            sg.removeRows(row, count);
            int newSize = getSize();
            rewriteMap(row, -count, Integer.MAX_VALUE, 0);
            assertEquals(map.size(), newSize);
            checkAll();
            numRemoveRows++;
        }

        public void insertColumns(int column, int count) {
            //checkAll();
            sg.insertColumns(column, count);
            rewriteMap(Integer.MAX_VALUE, 0, column, count);
            checkAll();
            numInsertCols++;
        }

        public void removeColumns(int column, int count) {
            //checkAll();
            sg.removeColumns(column, count);
            rewriteMap(Integer.MAX_VALUE, 0, column, -count);
            checkAll();
            numRemoveCols++;
        }

        public void iterate(int startRow, int startColumn, int endRow, int endColumn) {
            //checkAll();
            final HashMap<Coord, Entry> results = new HashMap<Coord, Entry>();
            for(Map.Entry<Coord, Entry> e : map.entrySet()) {
                Coord c = e.getKey();
                if(c.row >= startRow && c.column >= startColumn &&
                        c.row <= endRow && c.column <= endColumn) {
                    results.put(c, e.getValue());
                }
            }
            sg.iterate(startRow, startColumn, endRow, endColumn, new SparseGrid.GridFunction() {
                public void apply(int row, int column, SparseGrid.Entry eSG) {
                    Entry eMap = results.remove(new Coord(row, column));
                    assertSame(eMap, eSG);
                }
            });
            for(Map.Entry<Coord, Entry> e : results.entrySet()) {
                Coord c = e.getKey();
                Entry eSG = (Entry)sg.get(c.row, c.column);
                if(eSG != null) {
                    System.out.println("Missed "+c.row+" "+c.column);
                }
                assertNull(eSG);
            }
            assertTrue(results.isEmpty());
        }

        static final int[] chances = createChanceTable(600,300,2,2,1,1,1);
        public void doRandomInsertsDeletes(Random r, int count, int maxRows, int maxColumns) {
            for(int i=0 ; i<count ; i++) {
                int row = r.nextInt(maxRows);
                int col = r.nextInt(maxColumns);
                switch(chances[r.nextInt(chances.length)]) {
                case 0:
                    insert(row, col);
                    break;
                case 1:
                    remove(row, col);
                    break;
                case 2:
                    insertRows(row, r.nextInt(maxRows/10)+1);
                    break;
                case 3:
                    removeRows(row, r.nextInt(maxRows/10)+1);
                    break;
                case 4:
                    insertColumns(col, r.nextInt(maxColumns/10)+1);
                    break;
                case 5:
                    removeColumns(col, r.nextInt(maxColumns/10)+1);
                    break;
                case 6:
                    iterate(row, col,
                            row + r.nextInt(maxRows/10),
                            col + r.nextInt(maxColumns/10));
                    break;
                default:
                    throw new UnsupportedOperationException();
                }
            }
        }

        public void dumpStats() {
            System.out.println("maxSize="+maxSize+" maxLevels="+maxLevels+
                    " currentNr="+currentNr+
                    " numInserts="+numInsertRows+","+numInsertCols+
                    " numRemoves="+numRemoveRows+","+numRemoveCols);
        }

        private static int[] createChanceTable(int ... weights) {
            int sum = 0;
            for(int weight : weights) {
                sum += weight;
            }
            int[] result = new int[sum];
            int idx = 0;
            int pos = 0;
            for(int weight : weights) {
                Arrays.fill(result, idx, idx+weight, pos++);
                idx += weight;
            }
            return result;
        }
    }
}