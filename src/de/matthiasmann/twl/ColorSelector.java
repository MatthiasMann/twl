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
package de.matthiasmann.twl;

import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.renderer.DynamicImage;
import de.matthiasmann.twl.renderer.Image;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 *
 * @author Matthias Mann
 */
public class ColorSelector extends DialogLayout {

    private ColorModel colorModel;
    private float[] colorValues;
    private int alpha;
    private ColorArea1D[] areas;

    public ColorSelector(ColorModel colorModel) {
        alpha = 255;
        
        setColorModel(colorModel);
    }

    public ColorModel getColorModel() {
        return colorModel;
    }

    public void setColorModel(ColorModel colorModel) {
        if(colorModel == null) {
            throw new NullPointerException("colorModel");
        }
        if(this.colorModel != colorModel) {
            Color color = null;
            if(this.colorModel != null) {
                color = getColor();
            }

            this.colorModel = colorModel;
            this.colorValues = new float[colorModel.getNumComponents()];

            if(color != null) {
                setColor(color);
            } else {
                setDefaultColor();
            }

            createColorAreas();
        }
    }

    public Color getColor() {
        return new Color((alpha << 24) | colorModel.toRGB(colorValues));
    }

    public void setColor(Color color) {
        alpha = color.getA() & 255;
        colorValues = colorModel.fromRGB(color.toARGB() & 0xFFFFFF);
        updateAllColorAreas(-1);
    }

    public void setDefaultColor() {
        alpha = 255;
        for(int i=0 ; i<colorModel.getNumComponents() ; i++) {
            colorValues[i] = colorModel.getDefaultValue(i);
        }
        updateAllColorAreas(-1);
    }
    
    protected void createColorAreas() {
        removeAllChildren();

        Group horz = createSequentialGroup().addGap();
        Group vertLabels = createParallelGroup();
        Group vertAreas = createParallelGroup();

        areas = new ColorArea1D[colorModel.getNumComponents()];
        
        for(int component=0 ; component<colorModel.getNumComponents() ; component++) {
            Label label = new Label(colorModel.getComponentName(component));
            ColorArea1D area = new ColorArea1D(component);

            areas[component] = area;
            
            horz.addGroup(createParallelGroup(
                    createSequentialGroup().addGap().addWidget(label).addGap(),
                    createSequentialGroup().addGap().addWidget(area).addGap()));
            vertLabels.addWidget(label);
            vertAreas.addWidget(area);

            label.setLabelFor(area);
        }

        setHorizontalGroup(horz.addGap());
        setVerticalGroup(createSequentialGroup().addGap().addGroup(vertLabels).addGroup(vertAreas).addGap());
    }

    protected void updateAllColorAreas(int exclude) {
        if(areas != null) {
            for(int i=0 ; i<areas.length ; i++) {
                if(i != exclude) {
                    areas[i].needsUpdate = true;
                }
            }
        }
    }

    private static final int IMAGE_SIZE = 64;

    class ColorArea1D extends Widget {
        private final ByteBuffer imgData;
        private final IntBuffer imgDataInt;
        private final int component;

        private DynamicImage img;
        private Image cursorImage;
        boolean needsUpdate;

        public ColorArea1D(int component) {
            imgData = ByteBuffer.allocateDirect(IMAGE_SIZE * 4);
            imgData.order(ByteOrder.BIG_ENDIAN);
            imgDataInt = imgData.asIntBuffer();
            
            this.component = component;
            this.needsUpdate = true;
        }

        @Override
        protected void applyTheme(ThemeInfo themeInfo) {
            super.applyTheme(themeInfo);
            cursorImage = themeInfo.getImage("cursor");
        }

        @Override
        protected void paintWidget(GUI gui) {
            if(img == null) {
                img = gui.getRenderer().createDynamicImage(1, IMAGE_SIZE);
            }
            if(img != null) {
                if(needsUpdate) {
                    updateImage();
                }
                img.draw(getAnimationState(), getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
            }
            if(cursorImage != null) {
                float minValue = colorModel.getMinValue(component);
                float maxValue = colorModel.getMaxValue(component);
                int pos = (int)((colorValues[component] - minValue) * (getInnerHeight()-1) / (maxValue - minValue) + 0.5f);
                cursorImage.draw(getAnimationState(), getInnerX(), getInnerY() + pos, getInnerWidth(), 1);
            }
        }

        @Override
        public void destroy() {
            super.destroy();
            if(img != null) {
                img.destroy();
                img = null;
            }
        }

        private void updateImage() {
            final float[] temp = ColorSelector.this.colorValues.clone();

            float x = colorModel.getMinValue(component);
            float dx = (colorModel.getMaxValue(component) - x) / (IMAGE_SIZE - 1);

            for(int i=0 ; i<IMAGE_SIZE ; i++) {
                temp[component] = x;
                imgDataInt.put(i, (colorModel.toRGB(temp) << 8) | 0xFF);
                x += dx;
            }

            img.update(imgData);
            needsUpdate = false;
        }

        @Override
        protected boolean handleEvent(Event evt) {
            switch (evt.getType()) {
                case MOUSE_BTNDOWN:
                case MOUSE_DRAGED: {
                    float minValue = colorModel.getMinValue(component);
                    float maxValue = colorModel.getMaxValue(component);
                    int innerHeight = getInnerHeight();
                    int pos = Math.max(0, Math.min(innerHeight, evt.getMouseY() - getInnerY()));
                    colorValues[component] = minValue + (maxValue - minValue) * pos / innerHeight;
                    updateAllColorAreas(component);
                    return true;
                }
                default:
                    if(evt.isMouseEvent()) {
                        return true;
                    }
                    break;
            }
            return super.handleEvent(evt);
        }
    }
}
