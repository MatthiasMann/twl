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

import java.text.ParseException;

/**
 *
 * @author Matthias Mann
 */
public class SimpleMathParser {

    public interface Interpreter {
        public void accessVariable(String name);
        public void accessField(String field);
        public void accessArray();
        public void loadConst(Number n);
        public void add();
        public void sub();
        public void mul();
        public void div();
        public void callFunction(String name, int args);
    }

    final String str;
    final Interpreter interpreter;
    int pos;

    private SimpleMathParser(String str, Interpreter interpreter) {
        this.str = str;
        this.interpreter = interpreter;
    }

    public static void interpret(String str, Interpreter interpreter) throws ParseException {
        new SimpleMathParser(str, interpreter).parse();
    }

    private void parse() throws ParseException {
        try {
            parseAddSub();
            int ch = peek();
            if(ch != -1) {
                unexpected(ch);
            }
        } catch (ParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw (ParseException)(new ParseException("Unable to execute", pos).initCause(ex));
        }
    }
    
    private void parseAddSub() throws ParseException {
        parseMulDiv();
        for(;;) {
            int ch = peek();
            switch (ch) {
                case '+':
                    pos++;
                    parseMulDiv();
                    interpreter.add();
                    break;
                case '-':
                    pos++;
                    parseMulDiv();
                    interpreter.sub();
                    break;
                default:
                    return;
            }
        }
    }

    private void parseMulDiv() throws ParseException {
        parseIdentOrConst();
        for(;;) {
            int ch = peek();
            switch (ch) {
                case '*':
                    pos++;
                    parseIdentOrConst();
                    interpreter.mul();
                    break;
                case '/':
                    pos++;
                    parseIdentOrConst();
                    interpreter.div();
                    break;
                default:
                    return;
            }
        }
    }

    private void parseIdentOrConst() throws ParseException {
        int ch = peek();
        if(Character.isJavaIdentifierStart((char)ch)) {
            String ident = parseIdent();
            ch = peek();
            if(ch == '(') {
                pos++;
                parseCall(ident);
                return;
            }
            interpreter.accessVariable(ident);
            while(ch == '.' || ch == '[') {
                pos++;
                if(ch == '.') {
                    String field = parseIdent();
                    interpreter.accessField(field);
                } else {
                    parseIdentOrConst();
                    expect(']');
                    interpreter.accessArray();
                }
                ch = peek();
            }
        } else if(ch == '.' || ch == '-' || ch == '+' || Character.isDigit((char)ch)) {
            parseConst();
        } else if(ch == '(') {
            pos++;
            parseAddSub();
            expect(')');
        } else {
            unexpected(ch);
        }
    }

    private void parseCall(String name) throws ParseException {
        int count = 1;
        parseAddSub();
        for(;;) {
            int ch = peek();
            if(ch == ')') {
                pos++;
                interpreter.callFunction(name, count);
                return;
            }
            if(ch == ',') {
                pos++;
                count++;
                parseAddSub();
            } else {
                unexpected(ch);
            }
        }
    }

    private void parseConst() throws ParseException {
        final int len = str.length();
        int start = pos;
        Number n;
        if(str.charAt(pos) == '+') {
            start = ++pos;
        } else if(str.charAt(pos) == '-') {
            pos++;
        }
        while(pos < len && Character.isDigit(str.charAt(pos))) {
            pos++;
        }
        if(pos < len && str.charAt(pos) == '.') {
            pos++;
            while(pos < len && Character.isDigit(str.charAt(pos))) {
                pos++;
            }
            if(pos - start <= 1) {
                unexpected(-1);
            }
            n = Float.valueOf(str.substring(start, pos));
        } else {
            n = Integer.valueOf(str.substring(start, pos));
        }
        interpreter.loadConst(n);
    }
    
    private boolean skipSpaces() {
        for(;;) {
            if(pos == str.length()) {
                return false;
            }
            if(!Character.isWhitespace(str.charAt(pos))) {
                return true;
            }
            pos++;
        }
    }

    private int peek() {
        if(skipSpaces()) {
            return str.charAt(pos);
        }
        return -1;
    }

    private String parseIdent() {
        int start = pos;
        while(pos < str.length() && Character.isJavaIdentifierPart(str.charAt(pos))) {
            pos++;
        }
        return str.substring(start, pos);
    }

    private void expect(int what) throws ParseException {
        int ch = peek();
        if(ch != what) {
            unexpected(ch);
        } else {
            pos++;
        }
    }
    
    private void unexpected(int ch) throws ParseException {
        if(ch < 0) {
            throw new ParseException("Unexpected end of string", pos);
        }
        throw new ParseException("Unexpected character '"+(char)ch+"' at " + pos, pos);
    }
}
