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

import de.matthiasmann.twl.renderer.CacheContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GLContext;

/**
 *
 * @author Matthias Mann
 */
public class LWJGLCacheContext implements CacheContext {

    final LWJGLRenderer renderer;
    final HashMap<String, LWJGLTexture> textures;
    final HashMap<String, BitmapFont> fontCache;
    boolean valid;

    protected LWJGLCacheContext(LWJGLRenderer renderer) {
        this.renderer = renderer;
        this.textures = new HashMap<String, LWJGLTexture>();
        this.fontCache = new HashMap<String, BitmapFont>();
        valid = true;
    }

    LWJGLTexture loadTexture(URL url, LWJGLTexture.Format fmt, LWJGLTexture.Filter filter) throws IOException {
        String urlString = url.toString();
        LWJGLTexture texture = textures.get(urlString);
        if(texture == null) {
            texture = createTexture(url, fmt, filter);
            textures.put(urlString, texture);
        }
        return texture;
    }

    private LWJGLTexture createTexture(URL textureUrl, LWJGLTexture.Format fmt, LWJGLTexture.Filter filter) throws IOException {
        InputStream is = textureUrl.openStream();
        try {
            PNGDecoder dec = new PNGDecoder(is);
            fmt = dec.decideTextureFormat(fmt);

            if(GLContext.getCapabilities().GL_EXT_abgr) {
                if(fmt == LWJGLTexture.Format.RGBA) {
                    fmt = LWJGLTexture.Format.ABGR;
                }
            } else if(fmt == LWJGLTexture.Format.ABGR) {
                fmt = LWJGLTexture.Format.RGBA;
            }

            int stride = dec.getWidth() * fmt.getPixelSize();
            ByteBuffer buf = BufferUtils.createByteBuffer(stride * dec.getHeight());
            dec.decode(buf, stride, fmt);
            buf.flip();

            return new LWJGLTexture(renderer, dec.getWidth(), dec.getHeight(), buf, fmt, filter);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
            }
        }
    }

    BitmapFont loadBitmapFont(URL url) throws IOException {
        String urlString = url.toString();
        BitmapFont bmFont = fontCache.get(urlString);
        if(bmFont == null) {
            bmFont = BitmapFont.loadFont(renderer, url);
            fontCache.put(urlString, bmFont);
        }
        return bmFont;
    }

    public boolean isValid() {
        return valid;
    }

    public void destroy() {
        try {
            for(LWJGLTexture t : textures.values()) {
                t.destroy();
            }
            for(BitmapFont f : fontCache.values()) {
                f.destroy();
            }
        } finally {
            textures.clear();
            fontCache.clear();
            valid = false;
        }
    }

}
