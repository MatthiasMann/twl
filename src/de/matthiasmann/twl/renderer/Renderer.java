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
     * Setup rendering for TWL.
     * Must be called before any Font or Image objects is drawn.
     */
    public void startRenderering();
    
    /**
     * Clean up after rendering TWL.
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
     * Returns the line renderer
     * @return the line renderer
     */
    public LineRenderer getLineRenderer();

    /**
     * Sets the clipping area for all rendering operations.
     * @param rect A rectangle or null to disable clipping.
     */
    public void setClipRect(Rect rect);

    public void setCursor(MouseCursor cursor);

    public void pushGlobalTintColor(float r, float g, float b, float a);

    public void popGlobalTintColor();
}
