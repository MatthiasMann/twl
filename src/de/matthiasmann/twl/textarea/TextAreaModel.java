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
package de.matthiasmann.twl.textarea;

import de.matthiasmann.twl.ParameterMap;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.textarea.Style;

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
        JUSTIFY
    }

    public enum Display {
        INLINE,
        BLOCK
    }
    
    public enum VAlignment {
        TOP,
        MIDDLE,
        BOTTOM,
        FILL
    }

    public enum Clear {
        NONE,
        LEFT,
        RIGHT,
        BOTH
    }

    public enum FloatPosition {
        NONE,
        LEFT,
        RIGHT
    }
    
    public interface Element {
        /**
         * Returns the style associated with this element
         * @return the style associated with this element. Must not be null.
         */
        public Style getStyle();
    }

    public interface TextElement extends Element {
        /**
         * Returns ths text.
         * @return the text.
         */
        public String getText();

        /**
         * Returns true if this element starts a new paragraph.
         *
         * Causes this element to start on a new line.
         * 
         * @return true if this element starts a new paragraph.
         */
        public boolean isParagraphStart();

        /**
         * Returns true if this element ends the current paragraph.
         *
         * A blank line is inserted after this element.
         * 
         * @return true if this element ends the current paragraph.
         */
        public boolean isParagraphEnd();
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
    }

    public interface BlockElement extends Element, Iterable<Element> {
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
