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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Delegates to de.matthiasmann.twl.utils.PNGDecoder.
 * 
 * @author Matthias Mann
 */
public class PNGDecoder extends de.matthiasmann.twl.utils.PNGDecoder {

    public PNGDecoder(InputStream input) throws IOException {
        super(input);
    }
        
    public LWJGLTexture.Format decideTextureFormat(LWJGLTexture.Format fmt) {
        if(fmt == LWJGLTexture.Format.COLOR) {
            fmt = autoColorFormat();
        }
        
        Format pngFormat = super.decideTextureFormat(fmt.getPngFormat());
        if(fmt.pngFormat == pngFormat) {
            return fmt;
        }

        switch(pngFormat) {
            case ALPHA:
                return LWJGLTexture.Format.ALPHA;
            case LUMINANCE:
                return LWJGLTexture.Format.LUMINANCE;
            case LUMINANCE_ALPHA:
                return LWJGLTexture.Format.LUMINANCE_ALPHA;
            case RGB:
                return LWJGLTexture.Format.RGB;
            case RGBA:
                return LWJGLTexture.Format.RGBA;
            case BGRA:
                return LWJGLTexture.Format.BGRA;
            case ABGR:
                return LWJGLTexture.Format.ABGR;
            default:
                throw new UnsupportedOperationException("PNGFormat not handled: " + pngFormat);
        }
    }

    private LWJGLTexture.Format autoColorFormat() {
        if(hasAlpha()) {
            if(isRGB()) {
                return LWJGLTexture.Format.ABGR;
            } else {
                return LWJGLTexture.Format.LUMINANCE_ALPHA;
            }
        } else if(isRGB()) {
            return LWJGLTexture.Format.ABGR;
        } else {
            return LWJGLTexture.Format.LUMINANCE;
        }
    }
    
    public void decode(ByteBuffer buffer, int stride, LWJGLTexture.Format fmt) throws IOException {
        super.decode(buffer, stride, fmt.getPngFormat());
    }
}
