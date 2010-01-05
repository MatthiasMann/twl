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
import de.matthiasmann.twl.renderer.AnimationState;
import de.matthiasmann.twl.renderer.DynamicImage;
import de.matthiasmann.twl.renderer.Image;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public class LWJGLDynamicImage extends TextureAreaBase implements DynamicImage {

    private final LWJGLRenderer renderer;
    private final int target;
    private final Color tintColor;
    private int id;
    
    public LWJGLDynamicImage(LWJGLRenderer renderer, int target, int id, int width, int height, Color tintColor) {
        super(0, 0, width, height, width, height);
        
        this.renderer = renderer;
        this.tintColor = tintColor;
        this.target = target;
        this.id = id;
    }

    public void destroy() {
        if(id != 0) {
            renderer.glDeleteTexture(id);
            renderer.dynamicImages.remove(this);
        }
    }

    public void update(ByteBuffer data) {
        update(0, 0, width, height, data);
    }

    public void update(int xoffset, int yoffset, int width, int height, ByteBuffer data) {
        if(xoffset < 0 || yoffset < 0 || getWidth() <= 0 || getHeight() <= 0) {
            throw new IllegalArgumentException();
        }
        if(xoffset >= getWidth() || yoffset >= getHeight()) {
            throw new IllegalArgumentException();
        }
        if(width > getWidth() - xoffset || height > getHeight() - yoffset) {
            throw new IllegalArgumentException();
        }
        if(data.remaining() < width*height*4) {
            throw new IllegalArgumentException();
        }
        bind();
        GL11.glTexSubImage2D(target, 0, xoffset, yoffset, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
    }

    public Image createTintedVersion(Color color) {
        if(color == null) {
            throw new NullPointerException("color");
        }
        Color newTintColor = tintColor.multiply(color);
        if(newTintColor.equals(tintColor)) {
            return this;
        }
        return new LWJGLDynamicImage(renderer, target, id, getWidth(), getHeight(), newTintColor);
    }

    public void draw(AnimationState as, int x, int y) {
        draw(as, x, y, width, height);
    }

    public void draw(AnimationState as, int x, int y, int width, int height) {
        bind();
        renderer.tintState.setColor(tintColor);
        if(target != GL11.GL_TEXTURE_2D) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(target);
        }
        GL11.glBegin(GL11.GL_QUADS);
        drawQuad(x, y, width, height);
        GL11.glEnd();
        if(target != GL11.GL_TEXTURE_2D) {
            GL11.glDisable(target);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    private void bind() {
        if(id == 0) {
            throw new IllegalStateException("destroyed");
        }
        GL11.glBindTexture(target, id);
    }

}
