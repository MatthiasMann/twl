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
import java.util.Arrays;
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
        ArrayList<Selector> selectors = new ArrayList<Selector>();
        int what;
        while((what=parser.yylex()) != Parser.EOF) {
            Selector parent = null;
            boolean directChild = false;

            selectorloop: for(;;) {
                String element;
                String className = null;
                switch (what) {
                    default:
                        parser.unexpected();    // throws exception
                        // fall though will not happen but keeps compiler quite
                    case Parser.STAR:
                    case Parser.DOT:
                        element = null;
                        break;
                    case Parser.IDENT:
                        element = parser.yytext();
                        break;
                }
                if(what == Parser.DOT || ((what = parser.yylex()) == Parser.DOT)) {
                    parser.expect(Parser.IDENT);
                    className = parser.yytext();
                    what = parser.yylex();
                }
                parent = new Selector(element, className, directChild, parent);
                switch (what) {
                    case Parser.GT:
                        directChild = true;
                        what = parser.yylex();
                        break;
                    case Parser.COMMA:
                    case Parser.STYLE_BEGIN:
                        break selectorloop;
                }
            }

            selectors.add(parent);

            switch (what) {
                default:
                    parser.unexpected();    // throws exception
                    // fall though will not happen but keeps compiler quite
                    
                case Parser.STYLE_BEGIN: {
                    CSSStyle style = new CSSStyle();

                    while((what=parser.yylex()) != Parser.STYLE_END) {
                        if(what != Parser.IDENT) {
                            parser.unexpected();
                        }
                        String key = parser.yytext();
                        parser.expect(Parser.COLON);
                        what = parser.yylex();
                        if(what != Parser.SEMICOLON && what != Parser.STYLE_END) {
                            parser.unexpected();
                        }
                        String value = parser.sb.toString().trim();
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
                        int score = 0;
                        for(Selector s=selector ; s!=null ; s=s.parent) {
                            if(s.directChild) {
                                score += 0x1;
                            }
                            if(s.className != null) {
                                score += 0x100;
                            }
                            if(s.element != null) {
                                score += 0x10000;
                            }
                        }
                        for(Selector s=selector ; s!=null ; s=s.parent) {
                            s.score = score;
                            s.style = style;
                        }
                    }

                    selectors.clear();
                    break;
                }

                case Parser.COMMA:
                    break;
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

        Selector[] candidates = new Selector[rules.size()];
        int numCandidates = 0;
        
        // find all possible candidates
        for(int i=0,n=rules.size() ; i<n ; i++) {
            Selector selector = rules.get(i);
            if(selector.matches(styleSheetKey)) {
                candidates[numCandidates++] = selector;
            }
        }

        // follow the parent chain and check multi selector rules
        Style parent = style;
        while(numCandidates > 0 && (parent=parent.getParent()) != null) {
            styleSheetKey = parent.getStyleSheetKey();
            if(styleSheetKey != null) {
                for(int i=numCandidates ; i-->0 ;) {
                    Selector selector = candidates[i];
                    if(selector.parent != null) {
                        if(selector.parent.matches(styleSheetKey)) {
                            candidates[i] = selector.parent;
                        } else if(selector.directChild) {
                            System.arraycopy(candidates, i+1, candidates, i, --numCandidates - i);
                        }
                    }
                }
            }
        }

        // remove not fully matched rules
        for(int i=numCandidates ; i-->0 ;) {
            if(candidates[i].parent != null) {
                System.arraycopy(candidates, i+1, candidates, i, --numCandidates - i);
            }
        }

        if(numCandidates > 1) {
            Arrays.sort(candidates, 0, numCandidates);
        }
        
        Style result = null;
        boolean copy = true;
        
        for(int i=0,n=numCandidates ; i<n ; i++) {
            Style ruleStyle = candidates[i].style;
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

        cache.put(style, (result == null) ? this : result);
        return result;
    }

    static class Selector extends StyleSheetKey implements Comparable<Selector> {
        final boolean directChild;
        final Selector parent;
        CSSStyle style;
        int score;

        public Selector(String element, String className, boolean directChild, Selector parent) {
            super(element, className);
            this.directChild = directChild;
            this.parent = parent;
        }

        public int compareTo(Selector other) {
            return this.score - other.score;
        }
    }
}
