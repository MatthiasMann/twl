/*
 * Copyright (c) 2008-2012, Matthias Mann
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

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import java.text.ParseException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class StateExpressionTest {
    
    private static final StateKey[] STATES = {
        StateKey.get("A"),
        StateKey.get("B"),
        StateKey.get("C"),
        StateKey.get("D")
    };
    
    public StateExpressionTest() {
    }

    @Test
    public void testEmpty() {
        try {
            StateExpression expr = StateExpression.parse("", false);
            throw new AssertionError("Should not reach here");
        } catch(ParseException ex) {
            assertEquals("Unexpected end of expression", ex.getMessage());
        }
    }
    
    @Test
    public void testExpr1() throws ParseException {
        StateExpression expr = StateExpression.parse("A", false);
        test(expr, 0xAAAA);
    }

    @Test
    public void testExpr1n() throws ParseException {
        StateExpression expr = StateExpression.parse("A", true);
        test(expr, 0x5555);
    }

    @Test
    public void testExpr2() throws ParseException {
        StateExpression expr = StateExpression.parse("D", false);
        test(expr, 0xFF00);
    }
    
    @Test
    public void testExpr3() throws ParseException {
        StateExpression expr = StateExpression.parse("!A", false);
        test(expr, 0x5555);
    }
    
    @Test
    public void testExpr4() throws ParseException {
        StateExpression expr = StateExpression.parse("A + B", false);
        test(expr, 0x8888);
    }
    
    @Test
    public void testExpr5() throws ParseException {
        StateExpression expr = StateExpression.parse("A + !B", false);
        test(expr, 0x2222);
    }
    
    @Test
    public void testExpr6() throws ParseException {
        StateExpression expr = StateExpression.parse("A + B + C", false);
        test(expr, 0x8080);
    }
    
    @Test
    public void testExpr6n() throws ParseException {
        StateExpression expr = StateExpression.parse("A + B + C", true);
        test(expr, 0x7F7F);
    }
    
    @Test
    public void testExpr7() throws ParseException {
        StateExpression expr = StateExpression.parse("A + D", false);
        test(expr, 0xAA00);
    }
    
    @Test
    public void testExpr8() throws ParseException {
        StateExpression expr = StateExpression.parse("A | B", false);
        test(expr, 0xEEEE);
    }
    
    @Test
    public void testExpr9() throws ParseException {
        StateExpression expr = StateExpression.parse("A | B | C", false);
        test(expr, 0xFEFE);
    }
    
    @Test
    public void testExpr9n() throws ParseException {
        StateExpression expr = StateExpression.parse("A | B | C", true);
        test(expr, 0x0101);
    }
    
    @Test
    public void testExpr10() throws ParseException {
        StateExpression expr = StateExpression.parse("C | D", false);
        test(expr, 0xFFF0);
    }
    
    @Test
    public void testExpr11() throws ParseException {
        StateExpression expr = StateExpression.parse("A ^ B", false);
        test(expr, 0x6666);
    }
    
    @Test
    public void testExpr11n() throws ParseException {
        StateExpression expr = StateExpression.parse("A ^ B", true);
        test(expr, 0x9999);
    }
    
    @Test
    public void testExpr12() throws ParseException {
        StateExpression expr = StateExpression.parse("A ^ B ^ C", false);
        test(expr, 0x9696);
    }
    
    @Test
    public void testExpr12n() throws ParseException {
        StateExpression expr = StateExpression.parse("A ^ B ^ C", true);
        test(expr, 0x6969);
    }
    
    private void test(StateExpression expr, int expResult) {
        AnimationState as = new AnimationState();
        for(int i=0 ; i<(1 << STATES.length) ; i++) {
            for(int j=0 ; j<STATES.length ; j++) {
                as.setAnimationState(STATES[j], (i & (1<<j)) != 0);
            }
            boolean expected = (expResult & (1 << i)) != 0;
            assertEquals(String.format("Testing pattern %04X", i), expected, expr.evaluate(as));
        }
    }
}
