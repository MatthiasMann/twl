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

import de.matthiasmann.twl.textarea.TextAreaModel.Clear;
import de.matthiasmann.twl.textarea.TextAreaModel.Display;
import de.matthiasmann.twl.textarea.TextAreaModel.FloatPosition;
import de.matthiasmann.twl.textarea.TextAreaModel.HAlignment;
import de.matthiasmann.twl.textarea.TextAreaModel.VAlignment;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public final class StyleAttribute<T> {

    private static final ArrayList<StyleAttribute<?>> attributes = new ArrayList<StyleAttribute<?>>();

    // cascading attributes
    public static final StyleAttribute<HAlignment> HORIZONTAL_ALIGNMENT = new StyleAttribute<HAlignment>(true, HAlignment.class, HAlignment.LEFT);
    public static final StyleAttribute<VAlignment> VERTICAL_ALIGNMENT = new StyleAttribute<VAlignment>(true, VAlignment.class, VAlignment.BOTTOM);
    public static final StyleAttribute<Value> TEXT_IDENT = new StyleAttribute<Value>(true, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<String> FONT_NAME = new StyleAttribute<String>(true, String.class, "default");
    public static final StyleAttribute<String> LIST_STYLE_IMAGE = new StyleAttribute<String>(true, String.class, "ul-bullet");
    public static final StyleAttribute<Boolean> PREFORMATTED = new StyleAttribute<Boolean>(true, Boolean.class, Boolean.FALSE);

    // non cascading attribute
    public static final StyleAttribute<Clear> CLEAR = new StyleAttribute<Clear>(false, Clear.class, Clear.NONE);
    public static final StyleAttribute<Display> DISPLAY = new StyleAttribute<Display>(false, Display.class, Display.INLINE);
    public static final StyleAttribute<FloatPosition> FLOAT_POSITION = new StyleAttribute<FloatPosition>(false, FloatPosition.class, FloatPosition.NONE);
    public static final StyleAttribute<Value> WIDTH = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> HEIGHT = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<String> BACKGROUND_IMAGE = new StyleAttribute<String>(false, String.class, null);
    public static final StyleAttribute<Value> MARGIN_TOP = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> MARGIN_LEFT = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> MARGIN_RIGHT = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> MARGIN_BOTTOM = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> PADDING_TOP = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> PADDING_LEFT = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> PADDING_RIGHT = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);
    public static final StyleAttribute<Value> PADDING_BOTTOM = new StyleAttribute<Value>(false, Value.class, Value.ZERO_PX);

    // boxes
    public static final BoxAttribute MARGIN = new BoxAttribute(MARGIN_TOP, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_BOTTOM);
    public static final BoxAttribute PADDING = new BoxAttribute(PADDING_TOP, PADDING_LEFT, PADDING_RIGHT, PADDING_BOTTOM);
    
    /**
     * A inherited attribute will be looked up in the parent style if it is not set.
     *
     * @return true if this attribute is inherited from the parent.
     */
    public boolean isInherited() {
        return inherited;
    }
    
    public Class<T> getDataType() {
        return dataType;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public int ordinal() {
        return ordinal;
    }

    private final boolean inherited;
    private final Class<T> dataType;
    private final T defaultValue;
    private final int ordinal;

    private StyleAttribute(boolean inherited, Class<T> dataType, T defaultValue) {
        this.inherited = inherited;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.ordinal = attributes.size();
        attributes.add(this);
    }
    
    public static int getNumAttributes() {
        return attributes.size();
    }

    public static StyleAttribute getAttribute(int ordinal) {
        return attributes.get(ordinal);
    }
}
