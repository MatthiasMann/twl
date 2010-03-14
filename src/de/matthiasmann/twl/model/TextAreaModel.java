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
package de.matthiasmann.twl.model;

import de.matthiasmann.twl.ParameterMap;
import de.matthiasmann.twl.renderer.Image;

/**
 * Data model for the TextArea widget.
 * 
 * @author Matthias Mann
 */
public interface TextAreaModel extends Iterable<TextAreaModel.Element> {

    public enum HAlignment {
        LEFT,
        RIGHT,
        CENTER,
        BLOCK,
        INLINE
    }

    public enum VAlignment {
        TOP,
        CENTER,
        BOTTOM,
        FILL
    }

    public interface Element {
        /**
         * Returns the desired horizontal alignment for this element.
         * @return the desired horizontal alignment for this element.
         */
        public HAlignment getHorizontalAlignment();

        /**
         * Returns the desired vertical alignment for this element.
         * @return the desired vertical alignment for this element.
         */
        public VAlignment getVerticalAlignment();

        /**
         * Returns the margin to the left border.
         * @return the margin to the left border.
         */
        public int getMarginLeft();

        /**
         * Returns the margin to the right border.
         * @return the margin to the right border.
         */
        public int getMarginRight();
    }

    public interface TextElement extends Element {
        /**
         * Returns the font name. The font name is mapped to a font using the themeInfo.
         * @return the font name.
         */
        public String getFontName();

        /**
         * Returns ths text.
         * @return the text.
         */
        public String getText();

        /**
         * Returns the indentation for the first line of the text.
         * @return the indentation for the first line of the text.
         */
        public int getTextIndent();

        /**
         * Returns true if this element starts a new paragraph.
         * @return true if this element starts a new paragraph.
         */
        public boolean isParagraphStart();

        /**
         * Returns true if this element ends the current paragraph.
         * @return true if this element ends the current paragraph.
         */
        public boolean isParagraphEnd();

        /**
         * Returns true if this element is a preformatted text (&lt;pre/&gt;)
         * @return true if this element is a preformatted text
         */
        public boolean isPreformatted();
    }

    public interface LinkElement extends TextElement {
        /**
         * Returns the href of the link.
         * @return the href of the link.
         */
        public String getHREF();
    }

    public interface ImageElement extends Element {
        /**
         * Returns the image for this image element.
         * @param style a ParameterMap which can be used to lookup images.
         * @return the image object.
         */
        public Image getImage(ParameterMap style);

        /**
         * Returns the tooltip or null for this image.
         * @return the tooltip or null for this image.
         */
        public String getToolTip();
    }

    public interface WidgetElement extends Element {
        public String getWidgetName();
        public String getWidgetParam();
    }

    public interface ListElement extends Element, Iterable<Element> {
        /**
         * Returns the image used for the bullet.
         * 
         * @param style a ParameterMap which can be used to lookup images.
         * @return the image object for the bullet.
         */
        public Image getBulletImage(ParameterMap style);
    }

    /**
     * Adds a model change callback which is called when the model is modified.
     * @param cb the callback - must not be null.
     */
    public void addCallback(Runnable cb);

    /**
     * Removes the specific callback.
     * @param cb the callback that should be removed.
     */
    public void removeCallback(Runnable cb);
    
}
