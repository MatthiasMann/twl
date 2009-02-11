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

import de.matthiasmann.twl.renderer.AnimationState;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * A class to handle animation state expression
 * 
 * @author Matthias Mann
 */
public abstract class StateExpression {

    public abstract boolean evaluate(AnimationState as);

    public static StateExpression parse(String exp, boolean negate) throws ParseException {
        StateExpression expr = parse(new StringIterator(exp));
        expr.negate ^= negate;
        return expr;
    }

    private static StateExpression parse(StringIterator si) throws ParseException {
        ArrayList<StateExpression> children = new ArrayList<StateExpression>();
        char kind = ' ';
        
        while(si.skipSpaces()) {
            char ch = si.peek();
            boolean negate = ch == '!';
            if(negate) {
                si.pos++;
                if(!si.skipSpaces()) {
                    si.unexpected();
                }
                ch = si.peek();
            }

            StateExpression child = null;
            if(Character.isJavaIdentifierStart(ch)) {
                child = new Check(si.getIdent());
            } else if(ch == '(') {
                si.pos++;
                child = parse(si);
                si.expect(')');
            } else if(ch == ')') {
                break;
            } else {
                si.unexpected();
            }

            child.negate = negate;
            children.add(child);

            if(!si.skipSpaces()) {
                break;
            }

            ch = si.peek();
            if("|+^".indexOf(kind) < 0) {
                break;
            }

            if(children.size() == 1) {
                kind = ch;
            } else if(kind != ch) {
                si.unexpected();
            }
            si.pos++;
        }

        if(children.size() == 0) {
            si.unexpected();
        }
        
        assert kind != ' ' || children.size() == 1;

        if(children.size() == 1) {
            return children.get(0);
        }

        StateExpression[] childArray =
                children.toArray(new StateExpression[children.size()]);
        
        if(kind == '^') {
            return new Xor(childArray);
        } else {
            return new AndOr(kind, childArray);
        }
    }

    static class StringIterator {
        final String str;
        int pos;

        StringIterator(String str) {
            this.str = str;
        }

        boolean hasMore() {
            return pos < str.length();
        }

        char peek() {
            return str.charAt(pos);
        }

        void expect(char what) throws ParseException {
            if(peek() != what) {
                throw new ParseException("Expected '"+what+"' got '"+peek()+"' at " + pos, pos);
            }
            pos++;
        }

        void unexpected() throws ParseException {
            if(pos == str.length()) {
                throw new ParseException("Unexpected end of expression", pos);
            }
            throw new ParseException("Unexpected '"+peek()+"' at " + pos, pos);
        }

        boolean skipSpaces() {
            while(hasMore() && Character.isWhitespace(peek())) {
                pos++;
            }
            return hasMore();
        }

        String getIdent() {
            int start = pos;
            while(hasMore() && Character.isJavaIdentifierPart(peek())) {
                pos++;
            }
            // intern string for faster HashMap lookup
            return str.substring(start, pos).intern();
        }
    }

    protected boolean negate;

    static class AndOr extends StateExpression {
        private final StateExpression[] children;
        private final boolean kind;
        public AndOr(char kind, StateExpression ... children) {
            assert kind == '|' || kind == '+';
            this.children = children;
            this.kind = kind == '|';
        }

        @Override
        public boolean evaluate(AnimationState as) {
            for(StateExpression e : children) {
                if(kind == e.evaluate(as)) {
                    return kind ^ negate;
                }
            }
            return !kind ^ negate;
        }
    }

    static class Xor extends StateExpression {
        private final StateExpression[] children;
        public Xor(StateExpression ... children) {
            this.children = children;
        }

        @Override
        public boolean evaluate(AnimationState as) {
            boolean result = negate;
            for(StateExpression e : children) {
                result ^= e.evaluate(as);
            }
            return result;
        }
    }

    static class Check extends StateExpression {
        private final String state;
        public Check(String state) {
            this.state = state;
        }

        @Override
        public boolean evaluate(AnimationState as) {
            return negate ^ (as != null && as.getAnimationState(state));
        }
    }
}
