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
package de.matthiasmann.twl;

import de.matthiasmann.twl.Event.Type;
import de.matthiasmann.twl.utils.ClassUtils;
import de.matthiasmann.twl.utils.HashEntry;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class ActionMap {

    public static final int FLAG_ON_PRESSED = 1;
    public static final int FLAG_ON_RELEASE = 2;
    public static final int FLAG_ON_REPEAT = 4;

    private Mapping[] mappings;
    private int numMappings;

    public ActionMap() {
        mappings = new Mapping[16];
    }

    public boolean invoke(String action, Event event) {
        Mapping mapping = HashEntry.get(mappings, action);
        if(mapping != null) {
            mapping.call(event);
            return true;
        }
        return false;
    }

    public void addMapping(String action, Object target, String methodName, Object[] params, int flags) {
        for(Method m : target.getClass().getMethods()) {
            if(m.getName().equals(methodName)) {
                if(ClassUtils.isParamsCompatible(m.getParameterTypes(), params)) {
                    addMappingImpl(action, target, m, params, flags);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Can't find matching method");
    }

    public void addMapping(String action, Object target, Method method, Object[] params, int flags) {
        if(method.getDeclaringClass().isInstance(target)) {
            throw new IllegalArgumentException("method does not belong to target");
        }
        if(!ClassUtils.isParamsCompatible(method.getParameterTypes(), params)) {
            throw new IllegalArgumentException("Paramters don't match method");
        }
        addMappingImpl(action, target, method, params, flags);
    }

    public void addMapping(Object target) {
        for(Method m : target.getClass().getMethods()) {
            Action action = m.getAnnotation(Action.class);
            if(action != null) {
                if(m.getParameterTypes().length > 0) {
                    throw new UnsupportedOperationException("automatic binding of actions not supported for methods with parameters");
                }
                String name = m.getName();
                if(action.name().length() > 0) {
                    name = action.name();
                }
                int flags =
                        (action.onPressed() ? FLAG_ON_PRESSED : 0) |
                        (action.onRelease() ? FLAG_ON_RELEASE : 0) |
                        (action.onRepeat() ? FLAG_ON_REPEAT : 0);
                addMappingImpl(name, target, m, null, flags);
            }
        }
    }

    public void addMappingImpl(String action, Object target, Method method, Object[] params, int flags) {
        mappings = HashEntry.maybeResizeTable(mappings, numMappings);
        HashEntry.insertEntry(mappings, new Mapping(action, target, method, params, flags));
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Action {
        String name() default "";
        boolean onPressed() default true;
        boolean onRelease() default false;
        boolean onRepeat() default true;
    }

    static class Mapping extends HashEntry<String, Mapping> {
        final Object target;
        final Method method;
        final Object[] params;
        final int flags;

        Mapping(String key, Object target, Method method, Object[] params, int flags) {
            super(key);
            this.target = target;
            this.method = method;
            this.params = params;
            this.flags = flags;
        }

        void call(Event e) {
            Type type = e.getType();
            if((type == Event.Type.KEY_RELEASED && ((flags & FLAG_ON_RELEASE) != 0)) ||
                    (type == Event.Type.KEY_PRESSED && ((flags & FLAG_ON_PRESSED) != 0) &&
                    (!e.isKeyRepeated() || ((flags & FLAG_ON_REPEAT) != 0)))) {
                try {
                    method.invoke(target, params);
                } catch (Exception ex) {
                    Logger.getLogger(ActionMap.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
