/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
