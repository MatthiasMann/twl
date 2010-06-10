/*
 * Copyright (c) 2008-2010, Matthias Mann
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
package de.matthiasmann.twl.textarea;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 *
 * @author Matthias Mann
 */
public class StyleSheet implements StyleSheetResolver {

    private final ArrayList<Selector> rules;
    private final IdentityHashMap<Style, Object> cache;

    public StyleSheet() {
        this.rules = new ArrayList<Selector>();
        this.cache = new IdentityHashMap<Style, Object>();
    }

    public void parse(URL url) throws IOException {
        InputStream is = url.openStream();
        try {
            parse(new InputStreamReader(is, "UTF8"));
        } finally {
            is.close();
        }
    }
    
    public void parse(Reader r) throws IOException {
        Parser parser = new Parser(r);
        Selector parent = null;
        boolean directChild = false;
        ArrayList<Selector> selectors = new ArrayList<Selector>();
        for(;;) {
            String element = null;
            switch(parser.next()) {
                case Parser.EOF:
                    return;
                case Parser.GT:
                    directChild = true;
                    break;
                case Parser.IDENT:
                    element = parser.yytext();
                case Parser.STAR: {
                    String className = null;
                    if(parser.peek() == Parser.DOT) {
                        parser.next();
                        parser.expect(Parser.IDENT);
                        className = parser.yytext();
                    }
                    parent = new Selector(element, className, directChild, parent);
                    directChild = false;
                    break;
                }
                case Parser.DOT: {
                    parser.expect(Parser.IDENT);
                    String className = parser.yytext();
                    parent = new Selector(null, className, directChild, parent);
                    directChild = false;
                    break;
                }
                case Parser.COMMA:
                    if(parent != null) {
                        selectors.add(parent);
                        parent = null;
                    }
                    break;
                case Parser.STYLE_BEGIN: {
                    if(parent != null) {
                        selectors.add(parent);
                        parent = null;
                    }
                    
                    CSSStyle style = new CSSStyle();
                    
                    int what;
                    while((what=parser.next()) != Parser.STYLE_END) {
                        if(what != Parser.IDENT) {
                            parser.unexpected();
                        }
                        String key = parser.yytext();
                        parser.expect(Parser.COLON);
                        what = parser.next();
                        if(what != Parser.SEMICOLON && what != Parser.STYLE_END) {
                            parser.unexpected();
                        }
                        String value = parser.value();
                        try {
                            style.parseCSSAttribute(key, value);
                        } catch (IllegalArgumentException ex) {
                        }
                        if(what == Parser.STYLE_END) {
                            break;
                        }
                    }

                    for(int i=0,n=selectors.size() ; i<n ; i++) {
                        Selector selector = selectors.get(i);
                        rules.add(selector);
                        for(Selector s=selector ; s!=null ; s=s.parent) {
                            s.style = style;
                        }
                    }

                    selectors.clear();
                    break;
                }
            }
        }
    }

    public void layoutFinished() {
        cache.clear();
    }

    public void startLayout() {
        cache.clear();
    }

    public Style resolve(Style style) {
        StyleSheetKey styleSheetKey;
        while((styleSheetKey = style.getStyleSheetKey()) == null) {
            style = style.getParent();
            if(style == null) {
                return null;
            }
        }

        Object cacheData = cache.get(style);
        if(cacheData != null) {
            return (cacheData == this) ? null : (Style)cacheData;
        }

        ArrayList<Selector> candidates = new ArrayList<Selector>();
        for(int i=0,n=rules.size() ; i<n ; i++) {
            Selector selector = rules.get(i);
            if(selector.matches(styleSheetKey)) {
                candidates.add(selector);
            }
        }

        Style parent = style;
        while(!candidates.isEmpty() && (parent=parent.getParent()) != null) {
            styleSheetKey = parent.getStyleSheetKey();
            for(int i=candidates.size() ; i-->0 ;) {
                Selector selector = candidates.get(i);
                if(selector.parent != null) {
                    if(selector.parent.matches(styleSheetKey)) {
                        candidates.set(i, selector.parent);
                    } else if(selector.directChild) {
                        candidates.remove(i);
                    }
                }
            }
        }

        Style result = null;
        boolean copy = true;
        
        for(int i=0,n=candidates.size() ; i<n ; i++) {
            Selector selector = candidates.get(i);
            if(selector.parent == null) {
                Style ruleStyle = selector.style;
                if(result == null) {
                    result = ruleStyle;
                } else {
                    if(copy) {
                        result = new Style(result);
                        copy = false;
                    }
                    result.putAll(ruleStyle);
                }
            }
        }

        cache.put(style, (result == null) ? this : result);
        return result;
    }

    static class Selector extends StyleSheetKey {
        final boolean directChild;
        final Selector parent;
        CSSStyle style;

        public Selector(String element, String className, boolean directChild, Selector parent) {
            super(element, className);
            this.directChild = directChild;
            this.parent = parent;
        }
    }
}
