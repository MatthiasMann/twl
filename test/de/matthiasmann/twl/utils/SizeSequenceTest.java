/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twl.utils;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mam
 */
public class SizeSequenceTest {

    public SizeSequenceTest() {
    }

    @Test
    public void testInsert() {
        final int count = 2000;
        final int initialSize = 50;

        SizeSequence ss = new SizeSequence();
        ss.setDefaultValue(initialSize);
        ss.insert(0, count);

        for(int i=0 ; i<count ; i++) {
            assertEquals(initialSize, ss.getSize(i));
        }
        for(int i=0 ; i<count ; i++) {
            assertEquals(initialSize*i, ss.getPosition(i));
        }

        int maxValue = 0;
        for(int v : ss.table) {
            maxValue = Math.max(maxValue, v);
        }
        System.out.println("maxValue=" + maxValue);
    }

    @Test
    public void testInsert2() {
        final int count = 2000;
        final int initialSize = 50;

        SizeSequence ss = new SizeSequence();
        ss.setDefaultValue(initialSize);
        ss.insert(0, count);

        for(int i=0 ; i<count ; i++) {
            assertEquals(initialSize, ss.getSize(i));
        }
        for(int i=0 ; i<count ; i++) {
            assertEquals(initialSize*i, ss.getPosition(i));
        }

        final int insertIndex = 666;
        final int insertCount = 321;
        final int insertSize = 32;

        ss.setDefaultValue(insertSize);
        ss.insert(insertIndex, insertCount);

        assertEquals(count+insertCount, ss.size);

        int idx=0;
        for(; idx<insertIndex ; idx++) {
            assertEquals(initialSize, ss.getSize(idx));
        }
        for(int i=0 ; i<insertCount ; i++,idx++) {
            assertEquals(insertSize, ss.getSize(idx));
        }
        for(int i=insertIndex ; i<count ; i++,idx++) {
            assertEquals(initialSize, ss.getSize(idx));
        }
        
        int maxValue = 0;
        for(int v : ss.table) {
            maxValue = Math.max(maxValue, v);
        }
        System.out.println("maxValue=" + maxValue);
    }

    @Test
    public void testSetSize() {
        final long seed = 0x12345678;
        final int count = 2000;
        final int initialSize = 50;

        Random r = new Random();

        SizeSequence ss = new SizeSequence();
        ss.setDefaultValue(initialSize);
        ss.insert(0, count);

        for(int i=0 ; i<count ; i++) {
            assertEquals(initialSize, ss.getSize(i));
        }
        for(int i=0 ; i<count ; i++) {
            assertEquals(initialSize*i, ss.getPosition(i));
        }

        r.setSeed(seed);
        for(int i=0 ; i<count ; i++) {
            int size = r.nextInt(initialSize*2);
            ss.setSize(i, size);
        }

        r.setSeed(seed);
        for(int i=0 ; i<count ; i++) {
            int size = r.nextInt(initialSize*2);
            assertEquals(size, ss.getSize(i));
        }

        int maxValue = 0;
        for(int v : ss.table) {
            maxValue = Math.max(maxValue, v);
        }
        System.out.println("maxValue=" + maxValue);
    }

    @Test
    public void testRemove() {
        final long seed = 0x12345678;
        final int count = 2000;
        final int maxSize = 100;

        Random r = new Random();

        SizeSequence ss = new SizeSequence();
        ss.setDefaultValue(0);
        ss.initializeAll(count);

        r.setSeed(seed);
        for(int i=0 ; i<count ; i++) {
            int size = r.nextInt(maxSize);
            ss.setSize(i, size);
        }

        r.setSeed(seed);
        for(int i=0 ; i<count ; i++) {
            int size = r.nextInt(maxSize);
            assertEquals(size, ss.getSize(i));
        }

        final int removeIndex = 1337;
        final int removeCount = 42;

        ss.remove(removeIndex, removeCount);

        assertEquals(count - removeCount, ss.size);

        r.setSeed(seed);

        int idx = 0;
        for(; idx<removeIndex ; idx++) {
            int size = r.nextInt(maxSize);
            assertEquals(size, ss.getSize(idx));
        }
        for(int i=0 ; i<removeCount ; i++) {
            int size = r.nextInt(maxSize);
        }
        for(int i=removeIndex+removeCount ; i<count ; i++,idx++) {
            int size = r.nextInt(maxSize);
            assertEquals(size, ss.getSize(idx));
        }

        int maxValue = 0;
        for(int v : ss.table) {
            maxValue = Math.max(maxValue, v);
        }
        System.out.println("maxValue=" + maxValue);
    }

}