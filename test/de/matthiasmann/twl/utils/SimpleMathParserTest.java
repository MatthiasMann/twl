/*
 * Copyright (c) 2008-2011, Matthias Mann
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

import java.text.ParseException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class SimpleMathParserTest {

    public SimpleMathParserTest() {
    }

    @Test
    public void test1() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("0", ti);
        assertEquals("loadConst 0\n", ti.toString());
    }

    @Test
    public void test2() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("font.lineHeight + 4", ti);
        assertEquals(
                "accessVariable font\n" +
                "accessField lineHeight\n" +
                "loadConst 4\n" +
                "add\n", ti.toString());
    }

    @Test
    public void test3() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("7 + 3 * 2 - 1", ti);
        assertEquals(
                "loadConst 7\n" +
                "loadConst 3\n" +
                "loadConst 2\n" +
                "mul\n" +
                "add\n" +
                "loadConst 1\n" +
                "sub\n", ti.toString());
    }

    @Test
    public void test4() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("3 + max(9, 5)", ti);
        assertEquals(
                "loadConst 3\n" +
                "loadConst 9\n" +
                "loadConst 5\n" +
                "callFunction max 2\n" +
                "add\n", ti.toString());
    }

    @Test
    public void test5() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("-3 / 2", ti);
        assertEquals(
                "loadConst 3\n" +
                "negate\n" +
                "loadConst 2\n" +
                "div\n", ti.toString());
    }

    @Test
    public void test6() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("(7 + 3) * 2 - 1", ti);
        assertEquals(
                "loadConst 7\n" +
                "loadConst 3\n" +
                "add\n" +
                "loadConst 2\n" +
                "mul\n" +
                "loadConst 1\n" +
                "sub\n", ti.toString());
    }

    @Test
    public void test7() throws ParseException {
        TestInterpreter ti = new TestInterpreter();
        SimpleMathParser.interpret("9 - -1", ti);
        assertEquals(
                "loadConst 9\n" +
                "loadConst 1\n" +
                "negate\n" +
                "sub\n", ti.toString());
    }
    
    @Test
    public void test8() {
        try {
            TestInterpreter ti = new TestInterpreter();
            SimpleMathParser.interpret("0x 9", ti);
            throw new AssertionError("Should have thrown an exception");
        } catch(ParseException ex) {
            assertEquals("Unexpected character ' ' at 2", ex.getMessage());
            assertEquals(2, ex.getErrorOffset());
        }
    }
    
    @Test
    public void test9() {
        try {
            TestInterpreter ti = new TestInterpreter();
            SimpleMathParser.interpret("0x123456789", ti);
            throw new AssertionError("Should have thrown an exception");
        } catch(ParseException ex) {
            assertEquals("Number to large at 11", ex.getMessage());
            assertEquals(11, ex.getErrorOffset());
        }
    }
    
    @Test
    public void test10() {
        try {
            TestInterpreter ti = new TestInterpreter();
            SimpleMathParser.interpret("0x", ti);
            throw new AssertionError("Should have thrown an exception");
        } catch(ParseException ex) {
            assertEquals("Unexpected end of string", ex.getMessage());
            assertEquals(2, ex.getErrorOffset());
        }
    }

    static class TestInterpreter implements SimpleMathParser.Interpreter {
        final StringBuilder sb = new StringBuilder();

        @Override
        public String toString() {
            return sb.toString();
        }

        public void accessVariable(String name) {
            sb.append("accessVariable ").append(name).append('\n');
        }

        public void accessField(String field) {
            sb.append("accessField ").append(field).append('\n');
        }

        public void accessArray() {
            sb.append("accessArray\n");
        }

        public void loadConst(Number n) {
            sb.append("loadConst ").append(n).append('\n');
        }

        public void add() {
            sb.append("add\n");
        }

        public void sub() {
            sb.append("sub\n");
        }

        public void mul() {
            sb.append("mul\n");
        }

        public void div() {
            sb.append("div\n");
        }

        public void callFunction(String name, int args) {
            sb.append("callFunction ").append(name).append(' ').append(args).append('\n');
        }

        public void negate() {
            sb.append("negate\n");
        }
    }
}