/*
 * Copyright (c) 2008, Matthias Mann
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

import de.matthiasmann.twl.CallbackWithReason;
import java.lang.reflect.Array;

/**
 * Callback list management functions
 *
 * @author Matthias Mann
 */
public class CallbackSupport {

    private CallbackSupport() {
    }

    private static void checkNotNull(Object callback) {
        if(callback == null) {
            throw new NullPointerException("callback");
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] addCallbackToList(T[] curList, T callback, Class<T> clazz) {
        checkNotNull(callback);
        final int curLength = (curList == null) ? 0 : curList.length;
        T[] newList = (T[])Array.newInstance(clazz, curLength + 1);
        if(curLength > 0) {
            System.arraycopy(curList, 0, newList, 0, curLength);
        }
        newList[curLength] = callback;
        return newList;
    }

    public static <T> int findCallbackPosition(T[] list, T callback) {
        checkNotNull(callback);
        for(int i=0,n=list.length ; i<n ; i++) {
            if(list[i] == callback) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeCallbackFromList(T[] curList, int index, Class<T> clazz) {
        final int curLength = curList.length;
        assert(index >= 0 && index < curLength);
        if(curLength == 1) {
            return null;
        }
        final int newLength = curLength - 1;
        T[] newList = (T[])Array.newInstance(clazz, newLength);
        System.arraycopy(curList, 0, newList, 0, index);
        System.arraycopy(curList, index+1, newList, index, newLength-index);
        return newList;
    }

    public static <T> T[] removeCallbackFromList(T[] curList, T callback, Class<T> clazz) {
        int idx = findCallbackPosition(curList, callback);
        if(idx >= 0) {
            curList = removeCallbackFromList(curList, idx, clazz);
        }
        return curList;
    }

    public static void fireCallbacks(Runnable[] callbacks) {
        if(callbacks != null) {
            for(Runnable cb : callbacks) {
                cb.run();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> void fireCallbacks(CallbackWithReason[] callbacks, T reason) {
        if(callbacks != null) {
            for(CallbackWithReason cb : callbacks) {
                ((CallbackWithReason<T>)cb).callback(reason);
            }
        }
    }
}
