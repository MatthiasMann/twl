/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twl.textarea;

import java.io.StringReader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Mann
 */
public class StyleSheetTest {

    public StyleSheetTest() {
    }

    @Test
    public void testParse1() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader(""));
    }

    @Test
    public void testParse2() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { font-family: link }"));
    }

    @Test
    public void testParse3() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("a { font-family: link; }"));
    }

    @Test
    public void testParse4() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("div a { font-family: link; }"));
    }

    @Test
    public void testParse5() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("div > a { font-family: link; }"));
    }

    @Test
    public void testParse6() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("div, img { margin-right: 5px; margin-left: 5px; }"));
    }

    @Test
    public void testParse7() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader(".bla { height: 5% }"));
    }

    @Test
    public void testParse8() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("*.bla { height: 5% }"));
    }

    @Test
    public void testParse9() throws Exception {
        StyleSheet ss = new StyleSheet();
        ss.parse(new StringReader("*.bla blub { height: 5% }"));
    }
}