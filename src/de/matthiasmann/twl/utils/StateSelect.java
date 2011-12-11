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

import de.matthiasmann.twl.renderer.AnimationState;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;

/**
 *
 * @author Matthias Mann
 */
public class StateSelect {
    
    private static boolean useOptimizer = false;
    
    private final StateExpression[] expressions;
    private final StateKey[] programKeys;
    private final short[] programCodes;

    public StateSelect(StateExpression ... expressions) {
        this.expressions = expressions;
        
        StateSelectOptimizer sso = useOptimizer
                ? StateSelectOptimizer.optimize(expressions)
                : null;
        
        if(sso != null) {
            programKeys = sso.getProgramKeys();
            programCodes = sso.getProgramCodes();
        } else {
            programKeys = null;
            programCodes = null;
        }
    }

    public static boolean isUseOptimizer() {
        return useOptimizer;
    }

    /**
     * Controls the use of the StateSelectOptimizer.
     * 
     * @param useOptimizer true if the StateSelectOptimizer should be used
     */
    public static void setUseOptimizer(boolean useOptimizer) {
        StateSelect.useOptimizer = useOptimizer;
    }
    
    public int getNumExpressions() {
        return expressions.length;
    }
    
    public StateExpression getExpression(int idx) {
        return expressions[idx];
    }
    
    public int evaluate(AnimationState as, int noMatchIndex) {
        if(programKeys != null) {
            return evaluateProgram(as, noMatchIndex);
        }
        return evaluateExpr(as, noMatchIndex);
    }
    
    private int evaluateExpr(AnimationState as, int noMatchIndex) {
        for(int i=0,n=expressions.length ; i<n ; i++) {
            if(expressions[i].evaluate(as)) {
                return i;
            }
        }
        return noMatchIndex;
    }
    
    private int evaluateProgram(AnimationState as, int noMatchIndex) {
        int pos = 0;
        do {
            if(!as.getAnimationState(programKeys[pos >> 1])) {
                pos++;
            }
            pos = programCodes[pos];
        } while(pos >= 0);
        
        if(pos == -1) {
            return noMatchIndex;
        }
        return pos & CODE_MASK;
    }

    static final int CODE_RESULT = 0x8000;
    static final int CODE_MASK   = 0x7FFF;
}
