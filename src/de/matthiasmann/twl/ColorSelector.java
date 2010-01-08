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
import de.matthiasmann.twl.model.AbstractIntegerModel;
import de.matthiasmann.twl.model.ColorSpace;
import de.matthiasmann.twl.renderer.DynamicImage;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.CallbackSupport;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * A color selector widget
 *
 * @author Matthias Mann
 */
public class ColorSelector extends DialogLayout {

    private static final String[] ARGB_NAMES = {"Red", "Green", "Blue", "Alpha"};

    private ColorSpace colorSpace;
    private float[] colorValues;
    private ColorValueModel[] colorValueModels;
    private boolean useColorArea2D = true;
    private Runnable[] callbacks;
    private int currentColor;
    private ARGBModel[] argbModels;

    public ColorSelector(ColorSpace colorSpace) {
        currentColor = Color.WHITE.toARGB();

        setColorSpace(colorSpace);
    }

    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(ColorSpace colorModel) {
        if(colorModel == null) {
            throw new NullPointerException("colorModel");
        }
        if(this.colorSpace != colorModel) {
            boolean hasColor = this.colorSpace != null;

            this.colorSpace = colorModel;
            this.colorValues = new float[colorModel.getNumComponents()];

            if(hasColor) {
                setColor(currentColor);
            } else {
                setDefaultColor();
            }

            createColorAreas();
        }
    }

    public Color getColor() {
        return new Color(currentColor);
    }

    public void setColor(Color color) {
        setColor(color.toARGB());
    }

    public void setDefaultColor() {
        currentColor = Color.WHITE.toARGB();
        for(int i=0 ; i<colorSpace.getNumComponents() ; i++) {
            colorValues[i] = colorSpace.getDefaultValue(i);
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

    protected void colorChanged() {
        currentColor = (currentColor & (0xFF << 24)) | colorSpace.toRGB(colorValues);
        CallbackSupport.fireCallbacks(callbacks);
        if(argbModels != null) {
            for(ARGBModel m : argbModels) {
                m.fireCallback();
            }
        }
    }

    protected void setColor(int argb) {
        currentColor = argb;
        colorValues = colorSpace.fromRGB(argb & 0xFFFFFF);
        updateAllColorAreas();
    }
    
    protected int getNumComponents() {
        return colorSpace.getNumComponents();
    }

    protected void createColorAreas() {
        removeAllChildren();

        // recreate models to make sure that no callback is left over
        argbModels = new ARGBModel[4];
        argbModels[0] = new ARGBModel(16);
        argbModels[1] = new ARGBModel(8);
        argbModels[2] = new ARGBModel(0);
        argbModels[3] = new ARGBModel(24);

        int numComponents = getNumComponents();

        Group horzAreas = createSequentialGroup().addGap();
        Group vertAreas = createParallelGroup();

        Group horzLabels = createParallelGroup();
        Group horzAdjuster = createParallelGroup();

        Group[] vertAdjuster = new Group[4 + numComponents];
        for(int i=0 ; i<vertAdjuster.length ; i++) {
            vertAdjuster[i] = createParallelGroup();
        }

        colorValueModels = new ColorValueModel[numComponents];
        for(int component=0 ; component<numComponents ; component++) {
            colorValueModels[component] = new ColorValueModel(component);

            Label label = new Label(colorSpace.getComponentName(component));
            ValueAdjusterFloat vaf = new ValueAdjusterFloat(colorValueModels[component]);
            label.setLabelFor(vaf);

            horzLabels.addWidget(label);
            horzAdjuster.addWidget(vaf);
            vertAdjuster[component].addWidget(label).addWidget(vaf);
        }

        for(int i=0 ; i<argbModels.length ; i++) {
            Label label = new Label(ARGB_NAMES[i]);
            ValueAdjusterInt vai = new ValueAdjusterInt(argbModels[i]);
            label.setLabelFor(vai);

            horzLabels.addWidget(label);
            horzAdjuster.addWidget(vai);
            vertAdjuster[numComponents + i].addWidget(label).addWidget(vai);
        }

        int component = 0;

        if(useColorArea2D) {
            for(; component+1 < numComponents ; component+=2) {
                ColorArea2D area = new ColorArea2D(component, component+1);

                horzAreas.addWidget(area);
                vertAreas.addWidget(area);
            }
        }

        for( ; component<numComponents ; component++) {
            ColorArea1D area = new ColorArea1D(component);

            horzAreas.addWidget(area);
            vertAreas.addWidget(area);
        }

        setVerticalGroup(null);
        setHorizontalGroup(createParallelGroup()
                .addGroup(horzAreas.addGap())
                .addGroup(createSequentialGroup(horzLabels, horzAdjuster).addGap()));
        setVerticalGroup(createSequentialGroup()
                .addGroup(vertAreas)
                .addGroups(vertAdjuster).addGap());
    }

    protected void updateAllColorAreas() {
        if(colorValueModels != null) {
            for(ColorValueModel cvm : colorValueModels) {
                cvm.fireCallback();
            }
            colorChanged();
        }
    }

    private static final int IMAGE_SIZE = 64;

    class ColorValueModel extends AbstractFloatModel {
        private final int component;

        public ColorValueModel(int component) {
            this.component = component;
        }

        public float getMaxValue() {
            return colorSpace.getMaxValue(component);
        }

        public float getMinValue() {
            return colorSpace.getMinValue(component);
        }

        public float getValue() {
            return colorValues[component];
        }

        public void setValue(float value) {
            colorValues[component] = value;
            doCallback();
            colorChanged();
        }

        void fireCallback() {
            doCallback();
        }
    }

    class ARGBModel extends AbstractIntegerModel {
        private final int startBit;

        public ARGBModel(int startBit) {
            this.startBit = startBit;
        }

        public int getMaxValue() {
            return 255;
        }

        public int getMinValue() {
            return 0;
        }

        public int getValue() {
            return (currentColor >> startBit) & 255;
        }

        public void setValue(int value) {
            setColor((currentColor & ~(255 << startBit)) | (value << startBit));
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
                float minValue = colorSpace.getMinValue(component);
                float maxValue = colorSpace.getMaxValue(component);
                int pos = (int)((colorValues[component] - maxValue) * (getInnerHeight()-1) / (minValue - maxValue) + 0.5f);
                cursorImage.draw(getAnimationState(), getInnerX(), getInnerY() + pos, getInnerWidth(), 1);
            }
        }

        protected void createImage(GUI gui) {
            img = gui.getRenderer().createDynamicImage(1, IMAGE_SIZE);
        }

        protected void updateImage() {
            final float[] temp = ColorSelector.this.colorValues.clone();

            float x = colorSpace.getMaxValue(component);
            float dx = (colorSpace.getMinValue(component) - x) / (IMAGE_SIZE - 1);

            for(int i=0 ; i<IMAGE_SIZE ; i++) {
                temp[component] = x;
                imgDataInt.put(i, (colorSpace.toRGB(temp) << 8) | 0xFF);
                x += dx;
            }

            img.update(imgData);
            needsUpdate = false;
        }

        @Override
        void handleMouse(int x, int y) {
            float minValue = colorSpace.getMinValue(component);
            float maxValue = colorSpace.getMaxValue(component);
            int innerHeight = getInnerHeight();
            int pos = Math.max(0, Math.min(innerHeight, y));
            float value = maxValue + (minValue - maxValue) * pos / innerHeight;
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
                float minValueX = colorSpace.getMinValue(componentX);
                float maxValueX = colorSpace.getMaxValue(componentX);
                float minValueY = colorSpace.getMinValue(componentY);
                float maxValueY = colorSpace.getMaxValue(componentY);
                int posX = (int)((colorValues[componentX] - maxValueX) * (getInnerWidth()-1) / (minValueX - maxValueX) + 0.5f);
                int posY = (int)((colorValues[componentY] - maxValueY) * (getInnerHeight()-1) / (minValueY - maxValueY) + 0.5f);
                cursorImage.draw(getAnimationState(), getInnerX() + posX, getInnerY() + posY, 1, 1);
            }
        }

        protected void createImage(GUI gui) {
            img = gui.getRenderer().createDynamicImage(IMAGE_SIZE, IMAGE_SIZE);
        }

        protected void updateImage() {
            final float[] temp = ColorSelector.this.colorValues.clone();

            float x0 = colorSpace.getMaxValue(componentX);
            float dx = (colorSpace.getMinValue(componentX) - x0) / (IMAGE_SIZE - 1);

            float y = colorSpace.getMaxValue(componentY);
            float dy = (colorSpace.getMinValue(componentY) - y) / (IMAGE_SIZE - 1);

            for(int i=0,idx=0 ; i<IMAGE_SIZE ; i++) {
                temp[componentY] = y;
                float x = x0;
                for(int j=0 ; j<IMAGE_SIZE ; j++) {
                    temp[componentX] = x;
                    imgDataInt.put(idx++, (colorSpace.toRGB(temp) << 8) | 0xFF);
                    x += dx;
                }
                y += dy;
            }

            img.update(imgData);
            needsUpdate = false;
        }

        @Override
        void handleMouse(int x, int y) {
            float minValueX = colorSpace.getMinValue(componentX);
            float maxValueX = colorSpace.getMaxValue(componentX);
            float minValueY = colorSpace.getMinValue(componentY);
            float maxValueY = colorSpace.getMaxValue(componentY);
            int innerWidtht = getInnerWidth();
            int innerHeight = getInnerHeight();
            int posX = Math.max(0, Math.min(innerWidtht, x));
            int posY = Math.max(0, Math.min(innerHeight, y));
            float valueX = maxValueX + (minValueX - maxValueX) * posX / innerWidtht;
            float valueY = maxValueY + (minValueY - maxValueY) * posY / innerHeight;
            colorValueModels[componentX].setValue(valueX);
            colorValueModels[componentY].setValue(valueY);
        }
    }
}
