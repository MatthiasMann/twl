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

import de.matthiasmann.twl.model.AbstractFloatModel;
import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.renderer.DynamicImage;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.CallbackSupport;
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
    private ColorValueModel[] colorValueModels;
    private boolean useColorArea2D = true;
    private Runnable[] callbacks;

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
        updateAllColorAreas();
    }

    public void setDefaultColor() {
        alpha = 255;
        for(int i=0 ; i<colorModel.getNumComponents() ; i++) {
            colorValues[i] = colorModel.getDefaultValue(i);
        }
        updateAllColorAreas();
    }

    public boolean isUseColorArea2D() {
        return useColorArea2D;
    }

    public void setUseColorArea2D(boolean useColorArea2D) {
        if(this.useColorArea2D != useColorArea2D) {
            this.useColorArea2D = useColorArea2D;
            createColorAreas();
        }
    }

    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    protected void fireCallbacks() {
        CallbackSupport.fireCallbacks(callbacks);
    }
    
    protected int getNumComponents() {
        return colorModel.getNumComponents();
    }

    protected void createColorAreas() {
        removeAllChildren();

        Group horz = createSequentialGroup().addGap();
        Group vertLabels = createParallelGroup();
        Group vertAreas = createParallelGroup();
        Group vertAdjuster = createParallelGroup();

        int numComponents = getNumComponents();

        colorValueModels = new ColorValueModel[numComponents];
        for(int component=0 ; component<numComponents ; component++) {
            colorValueModels[component] = new ColorValueModel(component);
        }

        int component = 0;

        if(useColorArea2D) {
            for(; component+1 < numComponents ; component+=2) {
                Label label = new Label(colorModel.getComponentName(component) +
                        "/" + colorModel.getComponentName(component+1));
                ColorArea2D area = new ColorArea2D(component, component+1);

                horz.addGroup(createParallelGroup(
                        createSequentialGroup().addGap().addWidget(label).addGap(),
                        createSequentialGroup().addGap().addWidget(area).addGap()));
                vertLabels.addWidget(label);
                vertAreas.addWidget(area);

                label.setLabelFor(area);
            }
        }

        for( ; component<numComponents ; component++) {
            Label label = new Label(colorModel.getComponentName(component));
            ColorArea1D area = new ColorArea1D(component);
            ValueAdjusterFloat vaf = new ValueAdjusterFloat(colorValueModels[component]);

            horz.addGroup(createParallelGroup(
                    createSequentialGroup().addGap().addWidget(label).addGap(),
                    createSequentialGroup().addGap().addWidget(area).addGap(),
                    createSequentialGroup().addGap().addWidget(vaf).addGap()));
            vertLabels.addWidget(label);
            vertAreas.addWidget(area);
            vertAdjuster.addWidget(vaf);

            label.setLabelFor(area);
        }

        setHorizontalGroup(horz.addGap());
        setVerticalGroup(createSequentialGroup().addGap()
                .addGroups(vertLabels).addGroup(vertAreas).addGroup(vertAdjuster).addGap());
    }

    protected void updateAllColorAreas() {
        if(colorValueModels != null) {
            for(ColorValueModel cvm : colorValueModels) {
                cvm.fireCallback();
            }
            fireCallbacks();
        }
    }

    private static final int IMAGE_SIZE = 64;

    class ColorValueModel extends AbstractFloatModel {
        private final int component;

        public ColorValueModel(int component) {
            this.component = component;
        }

        public float getMaxValue() {
            return colorModel.getMaxValue(component);
        }

        public float getMinValue() {
            return colorModel.getMinValue(component);
        }

        public float getValue() {
            return colorValues[component];
        }

        public void setValue(float value) {
            colorValues[component] = value;
            doCallback();
            fireCallbacks();
        }

        void fireCallback() {
            doCallback();
        }
    }

    abstract class ColorArea extends Widget implements Runnable {
        final ByteBuffer imgData;
        final IntBuffer imgDataInt;

        DynamicImage img;
        Image cursorImage;
        boolean needsUpdate;

        public ColorArea(int size) {
            imgData = ByteBuffer.allocateDirect(size);
            imgData.order(ByteOrder.BIG_ENDIAN);
            imgDataInt = imgData.asIntBuffer();
        }

        @Override
        protected void applyTheme(ThemeInfo themeInfo) {
            super.applyTheme(themeInfo);
            cursorImage = themeInfo.getImage("cursor");
        }

        abstract void createImage(GUI gui);
        abstract void updateImage();
        abstract void handleMouse(int x, int y);

        @Override
        protected void paintWidget(GUI gui) {
            if(img == null) {
                createImage(gui);
                needsUpdate = true;
            }
            if(img != null) {
                if(needsUpdate) {
                    updateImage();
                }
                img.draw(getAnimationState(), getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
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

        @Override
        protected boolean handleEvent(Event evt) {
            switch (evt.getType()) {
                case MOUSE_BTNDOWN:
                case MOUSE_DRAGED:
                    handleMouse(evt.getMouseX() - getInnerX(), evt.getMouseY() - getInnerY());
                    return true;
                default:
                    if(evt.isMouseEvent()) {
                        return true;
                    }
                    break;
            }
            return super.handleEvent(evt);
        }

        public void run() {
            needsUpdate = true;
        }
    }
    
    class ColorArea1D extends ColorArea {
        final int component;

        public ColorArea1D(int component) {
            super(IMAGE_SIZE * 4);

            this.component = component;

            for(int i=0,n=getNumComponents() ; i<n ; i++) {
                if(i != component) {
                    colorValueModels[i].addCallback(this);
                }
            }
        }

        @Override
        protected void paintWidget(GUI gui) {
            super.paintWidget(gui);
            if(cursorImage != null) {
                float minValue = colorModel.getMinValue(component);
                float maxValue = colorModel.getMaxValue(component);
                int pos = (int)((colorValues[component] - minValue) * (getInnerHeight()-1) / (maxValue - minValue) + 0.5f);
                cursorImage.draw(getAnimationState(), getInnerX(), getInnerY() + pos, getInnerWidth(), 1);
            }
        }

        protected void createImage(GUI gui) {
            img = gui.getRenderer().createDynamicImage(1, IMAGE_SIZE);
        }

        protected void updateImage() {
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
        void handleMouse(int x, int y) {
            float minValue = colorModel.getMinValue(component);
            float maxValue = colorModel.getMaxValue(component);
            int innerHeight = getInnerHeight();
            int pos = Math.max(0, Math.min(innerHeight, y));
            float value = minValue + (maxValue - minValue) * pos / innerHeight;
            colorValueModels[component].setValue(value);
        }
    }

    class ColorArea2D extends ColorArea {
        private final int componentX;
        private final int componentY;

        public ColorArea2D(int componentX, int componentY) {
            super(IMAGE_SIZE * IMAGE_SIZE * 4);

            this.componentX = componentX;
            this.componentY = componentY;

            for(int i=0,n=getNumComponents() ; i<n ; i++) {
                if(i != componentX && i != componentY) {
                    colorValueModels[i].addCallback(this);
                }
            }
        }
        @Override
        protected void paintWidget(GUI gui) {
            super.paintWidget(gui);
            if(cursorImage != null) {
                float minValueX = colorModel.getMinValue(componentX);
                float maxValueX = colorModel.getMaxValue(componentX);
                float minValueY = colorModel.getMinValue(componentY);
                float maxValueY = colorModel.getMaxValue(componentY);
                int posX = (int)((colorValues[componentX] - minValueX) * (getInnerWidth()-1) / (maxValueX - minValueX) + 0.5f);
                int posY = (int)((colorValues[componentY] - minValueY) * (getInnerHeight()-1) / (maxValueY - minValueY) + 0.5f);
                cursorImage.draw(getAnimationState(), getInnerX() + posX, getInnerY() + posY, 1, 1);
            }
        }

        protected void createImage(GUI gui) {
            img = gui.getRenderer().createDynamicImage(IMAGE_SIZE, IMAGE_SIZE);
        }

        protected void updateImage() {
            final float[] temp = ColorSelector.this.colorValues.clone();

            float x0 = colorModel.getMinValue(componentX);
            float dx = (colorModel.getMaxValue(componentX) - x0) / (IMAGE_SIZE - 1);

            float y = colorModel.getMinValue(componentY);
            float dy = (colorModel.getMaxValue(componentY) - y) / (IMAGE_SIZE - 1);

            for(int i=0,idx=0 ; i<IMAGE_SIZE ; i++) {
                temp[componentY] = y;
                float x = x0;
                for(int j=0 ; j<IMAGE_SIZE ; j++) {
                    temp[componentX] = x;
                    imgDataInt.put(idx++, (colorModel.toRGB(temp) << 8) | 0xFF);
                    x += dx;
                }
                y += dy;
            }

            img.update(imgData);
            needsUpdate = false;
        }

        @Override
        void handleMouse(int x, int y) {
            float minValueX = colorModel.getMinValue(componentX);
            float maxValueX = colorModel.getMaxValue(componentX);
            float minValueY = colorModel.getMinValue(componentY);
            float maxValueY = colorModel.getMaxValue(componentY);
            int innerWidtht = getInnerWidth();
            int innerHeight = getInnerHeight();
            int posX = Math.max(0, Math.min(innerWidtht, x));
            int posY = Math.max(0, Math.min(innerHeight, y));
            float valueX = minValueX + (maxValueX - minValueX) * posX / innerWidtht;
            float valueY = minValueY + (maxValueY - minValueY) * posY / innerHeight;
            colorValueModels[componentX].setValue(valueX);
            colorValueModels[componentY].setValue(valueY);
        }
    }
}
