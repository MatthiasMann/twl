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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.Resource;
import de.matthiasmann.twl.renderer.Texture;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.EXTAbgr;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.Util;

/**
 * Simple texture implementation for TWL using LWJGL.
 *
 * @author Matthias Mann
 */
public class LWJGLTexture implements Texture, Resource {

    public enum Format {
        ALPHA(GL11.GL_ALPHA, GL11.GL_ALPHA8, 1),
        LUMINANCE(GL11.GL_LUMINANCE, GL11.GL_LUMINANCE8, 1),
        LUMINANCE_ALPHA(GL11.GL_LUMINANCE_ALPHA, GL11.GL_LUMINANCE8_ALPHA8, 2),
        RGB(GL11.GL_RGB, GL11.GL_RGB8, 3),
        RGB_SMALL(GL11.GL_RGB, GL11.GL_RGB5_A1, 3),
        RGBA(GL11.GL_RGBA, GL11.GL_RGBA8, 4),
        BGRA(GL12.GL_BGRA, GL11.GL_RGBA8, 4),
        ABGR(EXTAbgr.GL_ABGR_EXT, GL11.GL_RGBA8, 4);

        final int glFormat;
        final int glInternalFormat;
        final int pixelSize;
        Format(int fmt, int ifmt, int ps) {
            this.glFormat = fmt;
            this.glInternalFormat = ifmt;
            this.pixelSize = ps;
        }

        public int getPixelSize() {
            return pixelSize;
        }
    }

    public enum Filter {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR);

        final int glValue;
        Filter(int value) {
            this.glValue = value;
        }
    }

    final LWJGLRenderer renderer;
    private int id;
    private final int width;
    private final int height;
    private final int texWidth;
    private final int texHeight;
    private ByteBuffer texData;
    private Format texDataFmt;
    private ArrayList<LWJGLCursor> cursors;

    public LWJGLTexture(LWJGLRenderer renderer, int width, int height,
            ByteBuffer buf, Format fmt, Filter filter) {
        this.renderer = renderer;

        if(width <= 0 || height <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }

        id = renderer.glGenTexture();
        if(id == 0) {
            throw new OpenGLException("failed to allocate texture ID");
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        if(GLContext.getCapabilities().OpenGL12) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter.glValue);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter.glValue);

        this.texWidth = OGLUtil.roundUpPOT(width);
        this.texHeight = OGLUtil.roundUpPOT(height);

        if(texWidth != width || texHeight != height) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,
                    fmt.glInternalFormat, texWidth, texHeight,
                    0, fmt.glFormat, GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer)null);
            Util.checkGLError();
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                    0, 0, width, height, fmt.glFormat,
                    GL11.GL_UNSIGNED_BYTE, buf);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,
                    fmt.glInternalFormat, texWidth, texHeight,
                    0, fmt.glFormat, GL11.GL_UNSIGNED_BYTE, buf);
        }

        Util.checkGLError();

        this.width = width;
        this.height = height;
        this.texData = buf;
        this.texDataFmt = fmt;
    }

    public void destroy() {
        if(id != 0) {
            // make sure that our texture is not bound when we try to delete it
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            renderer.glDeleteTexture(id);
            id = 0;
        }
        if(cursors != null) {
            for(LWJGLCursor cursor : cursors) {
                cursor.destroy();
            }
            cursors.clear();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTexWidth() {
        return texWidth;
    }

    public int getTexHeight() {
        return texHeight;
    }

    boolean bind(Color color) {
        if(id != 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            renderer.tintState.setColor(color);
            return true;
        }
        return false;
    }

    boolean bind() {
        if(id != 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            return true;
        }
        return false;
    }

    public Image getImage(int x, int y, int width, int height, Color tintColor, boolean tiled) {
        if(x < 0 || x >= getWidth()) {
            throw new IllegalArgumentException("x");
        }
        if(y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("y");
        }
        if(x + Math.abs(width) > getWidth()) {
            throw new IllegalArgumentException("width");
        }
        if(y + Math.abs(height) > getHeight()) {
            throw new IllegalArgumentException("height");
        }
        if(tiled && (width <= 0 || height <= 0)) {
            throw new IllegalArgumentException("Tiled rendering requires positive width & height");
        }
        return new TextureArea(this, x, y, width, height, tintColor, tiled);
    }

    public MouseCursor createCursor(int x, int y, int width, int height, int hotSpotX, int hotSpotY) {
        if(renderer.isUseSWMouseCursors()) {
            return new SWCursor(this, x, y, width, height, hotSpotX, hotSpotY);
        }
        if(texData != null) {
            LWJGLCursor cursor = new LWJGLCursor(texData, texDataFmt,
                    texDataFmt.getPixelSize() * this.width, x, y, width, height, hotSpotX, hotSpotY);

            if(cursors == null) {
                cursors = new ArrayList<LWJGLCursor>();
            }
            cursors.add(cursor);

            return cursor;
        }
        return null;
    }

    public void themeLoadingDone() {
        // don't clear this to enable theme switching
        // this.texData = null;
    }
}
