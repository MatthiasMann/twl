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
package de.matthiasmann.twl.renderer;

import de.matthiasmann.twl.Rect;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * TWL Rendering interface
 * 
 * @author Matthias Mann
 */
public interface Renderer {

    /**
     * Returns the elapsed time in milliseconds.
     * @return the elapsed time in milliseconds.
     */
    public long getTimeMillis();
    
    /**
     * Setup rendering for TWL.
     * <p>Must be called before any Font or Image objects is drawn.</p>
     * <p>When this method returned {@code true} then {@link #endRendering()}
     * must be called.</p>
     * @return true if rendering was started, false otherwise
     */
    public boolean startRenderering();
    
    /**
     * Clean up after rendering TWL.
     * Only call this method when {@link #startRenderering()} returned {@code true}
     */
    public void endRendering();
    
    /**
     * Returns the width of the renderable surface
     * @return the width of the renderable surface
     */
    public int getWidth();
    
    /**
     * Returns the height of the renderable surface
     * @return the height of the renderable surface
     */
    public int getHeight();
    
    /**
     * Creates a new cache context.
     * Call setActiveCacheContext to activate it.
     * 
     * @return a new CacheContext
     * @see #setActiveCacheContext(de.matthiasmann.twl.renderer.CacheContext) 
     */
    public CacheContext createNewCacheContext();

    /**
     * Sets the active cache context. It will be used for all future load operations.
     *
     * @param cc The CacheContext object
     * @throws NullPointerException when cc is null
     * @throws IllegalStateException when the CacheContext object is invalid
     */
    public void setActiveCacheContext(CacheContext cc) throws IllegalStateException;
    
    /**
     * Returns the active cache context.
     * If no valid cache context is active then a new one is created and activated.
     * 
     * @return the active CacheContext object
     */
    public CacheContext getActiveCacheContext();
    
    /**
     * Loads a font.
     * 
     * @param baseUrl the base URL that can be used to load font data
     * @param parameter font parameter
     * @param conditionalParameter conditional font paramters - evaluate in order based on AnimationState
     * @return a Font object
     * @throws java.io.IOException if the font could not be loaded
     */
    public Font loadFont(URL baseUrl, Map<String, String> parameter, Collection<FontParameter> conditionalParameter) throws IOException;
    
    /**
     * Loads a texture. Textures are used to create images.
     * 
     * @param url the URL of the texture file
     * @param format a format description - depends on the implementation
     * @param filter how the texture should be filtered - should support "nearest" and linear"
     * @return a Texture object
     * @throws java.io.IOException if the texture could not be loaded
     */
    public Texture loadTexture(URL url, String format, String filter) throws IOException;
    
    /**
     * Returns the line renderer. If line rendering is not supported then this method returns null.
     *
     * This is an optional operation.
     *
     * @return the line renderer or null if not supported.
     */
    public LineRenderer getLineRenderer();

    /**
     * Creates a dynamic image with undefined content.
     * 
     * This is an optional operation.
     * 
     * @param width the width of the image
     * @param height the height of the image
     * @return a new dynamic image or null if the image could not be created
     */
    public DynamicImage createDynamicImage(int width, int height);

    /**
     * Sets the clipping area for all rendering operations.
     * @param rect A rectangle or null to disable clipping.
     */
    public void setClipRect(Rect rect);

    public void setCursor(MouseCursor cursor);

    /**
     * Sets the mouse position for SW mouse cursor rendering
     * 
     * @param mouseX X mouse position
     * @param mouseY Y mouse position
     */
    public void setMousePosition(int mouseX, int mouseY);

    /**
     * Pushes a new tint color on the tint stack. The current tint color is
     * multiplied by the new tint color.
     *
     * For every call of {@code pushGlobalTintColor} a call to {@code popGlobalTintColor}
     * must be made.
     * 
     * @param r red, must be 0.0f &lt;= r &lt;= 1.0f
     * @param g green, must be 0.0f &lt;= g &lt;= 1.0f
     * @param b blue, must be 0.0f &lt;= b &lt;= 1.0f
     * @param a alpha, must be 0.0f &lt;= a &lt;= 1.0f
     */
    public void pushGlobalTintColor(float r, float g, float b, float a);

    public void popGlobalTintColor();
}
