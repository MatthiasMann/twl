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
package de.matthiasmann.twl.renderer.lwjgl;

import java.nio.ByteBuffer;

/**
 * A utility class to implement the GL11 based renderer
 * 
 * @author Matthias Mann
 */
final class OGLUtil {

    public static final int PAGE_SIZE = 4096;
    
    private OGLUtil() {
    }
    
    public static int divRoundUp(int value, int denum) {
        return (value + denum - 1) / denum;
    }
    
    public static int roundUpPOT(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value-1));
    }
    
    public static int align(int value, int what) {
        return (value + what - 1) & ~(what-1);
    }
    
    public static int align4(int value) {
        return (value + 3) & ~3;
    }
    
    public static int alignPage(int value) {
        return (value + PAGE_SIZE - 1) & ~(PAGE_SIZE-1);
    }
    
    public static ByteBuffer slice(ByteBuffer buf, int size) {
        size = alignPage(size);
        int oldLimit = buf.limit();
        buf.limit(buf.position() + size);
        ByteBuffer result = buf.slice();
        buf.position(buf.limit()).limit(oldLimit);
        return result;
    }
}
