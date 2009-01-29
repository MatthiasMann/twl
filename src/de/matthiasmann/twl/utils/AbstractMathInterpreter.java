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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Matthias Mann
 */
public abstract class AbstractMathInterpreter implements SimpleMathParser.Interpreter {

    public interface Function {
        public Object execute(Object ... args);
    }
    
    private final ArrayList<Object> stack;
    private final HashMap<String, Function> functions;

    public AbstractMathInterpreter() {
        this.stack = new ArrayList<Object>();
        this.functions = new HashMap<String, Function>();
    }

    public void registerFunction(String name, Function function) {
        if(function == null) {
            throw new NullPointerException("function");
        }
        functions.put(name, function);
    }

    public Number execute(String str) throws ParseException {
        stack.clear();
        SimpleMathParser.interpret(str, this);
        if(stack.size() != 1) {
            throw new IllegalStateException("Expected one return value on the stack");
        }
        return popNumber();
    }
    
    protected void push(Object obj) {
        stack.add(obj);
    }

    protected Object pop() {
        int size = stack.size();
        if(size == 0) {
            throw new IllegalStateException("stack underflow");
        }
        return stack.remove(size-1);
    }

    protected Number popNumber() {
        Object obj = pop();
        if(obj instanceof Number) {
            return (Number)obj;
        }
        throw new IllegalStateException("expected number on stack - found: " +
                ((obj != null) ? obj.getClass() : "null"));
    }

    public void loadConst(Number n) {
        push(n);
    }

    public void add() {
        Number b = popNumber();
        Number a = popNumber();
        boolean isFloat = isFloat(a) || isFloat(b);
        if(isFloat) {
            push(a.floatValue() + b.floatValue());
        } else {
            push(a.intValue() + b.intValue());
        }
    }

    public void sub() {
        Number b = popNumber();
        Number a = popNumber();
        boolean isFloat = isFloat(a) || isFloat(b);
        if(isFloat) {
            push(a.floatValue() - b.floatValue());
        } else {
            push(a.intValue() - b.intValue());
        }
    }

    public void mul() {
        Number b = popNumber();
        Number a = popNumber();
        boolean isFloat = isFloat(a) || isFloat(b);
        if(isFloat) {
            push(a.floatValue() * b.floatValue());
        } else {
            push(a.intValue() * b.intValue());
        }
    }

    public void div() {
        Number b = popNumber();
        Number a = popNumber();
        boolean isFloat = isFloat(a) || isFloat(b);
        if(isFloat) {
            if(Math.abs(b.floatValue()) == 0) {
                throw new IllegalStateException("division by zero");
            }
            push(a.floatValue() / b.floatValue());
        } else {
            if(b.intValue() == 0) {
                throw new IllegalStateException("division by zero");
            }
            push(a.intValue() / b.intValue());
        }
    }

    public void accessArray() {
        Number idx = popNumber();
        Object obj = pop();
        if(obj == null) {
            throw new IllegalStateException("null pointer");
        }
        if(!obj.getClass().isArray()) {
            throw new IllegalStateException("array expected");
        }
        try {
            push(Array.get(obj, idx.intValue()));
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalStateException("array index out of bounds", ex);
        }
    }

    public void accessField(String field) {
        Object obj = pop();
        if(obj == null) {
            throw new IllegalStateException("null pointer");
        }
        Object result;
        try {
            if(obj.getClass().isArray()) {
                if("length".equals(field)) {
                    result = Array.getLength(obj);
                } else {
                    throw new IllegalStateException("unknown field");
                }
            } else {
                PropertyDescriptor pd = new PropertyDescriptor(field, obj.getClass());
                result = pd.getReadMethod().invoke(obj);
            }
        } catch(Exception ex) {
            throw new IllegalStateException("unknown field", ex);
        }
        push(result);
    }

    public void callFunction(String name, int args) {
        Object[] values = new Object[args];
        for(int i=args ; i-->0 ;) {
            values[i] = pop();
        }
        Function function = functions.get(name);
        if(function == null) {
            throw new IllegalArgumentException("Unknown function");
        }
        push(function.execute(values));
    }

    protected static boolean isFloat(Number n) {
        return !(n instanceof Integer);
    }
}
