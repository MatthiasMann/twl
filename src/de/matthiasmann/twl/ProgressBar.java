/*
 * Copyright (c) 2008, Matthias Mann
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

import de.matthiasmann.twl.renderer.Image;

/**
 * A progress bar.
 *
 * @author Matthias Mann
 */
public class ProgressBar extends Widget {

    public static final String STATE_VALUE_CHANGED = "valueChanged";
    
    private Image progressImage;
    private float value;
    private boolean autoSize = true;

    public ProgressBar() {
        getAnimationState().resetAnimationTime(STATE_VALUE_CHANGED);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        if(value < 0) {
            value = 0;
        } else if(value > 1) {
            value = 1;
        }
        if(this.value != value) {
            this.value = value;
            getAnimationState().resetAnimationTime(STATE_VALUE_CHANGED);
        }
    }

    public Image getProgressImage() {
        return progressImage;
    }

    public void setProgressImage(Image progressImage) {
        this.progressImage = progressImage;
    }

    public boolean isAutoSize() {
        return autoSize;
    }

    public void setAutoSize(boolean autoSize) {
        this.autoSize = autoSize;
    }

    protected void applyThemeProgressBar(ThemeInfo themeInfo) {
        setProgressImage(themeInfo.getImage("progressImage"));
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemeProgressBar(themeInfo);
    }

    @Override
    protected void paintWidget(GUI gui) {
        int width = getInnerWidth();
        int height = getInnerHeight();
        if(progressImage != null) {
            int imageWidth = progressImage.getWidth();
            int progressWidth = width - imageWidth;
            int scaledWidth = (int)(progressWidth * value);
            if(scaledWidth < 0) {
                scaledWidth = 0;
            } else if(scaledWidth > progressWidth) {
                scaledWidth = progressWidth;
            }
            progressImage.draw(getAnimationState(), getInnerX(), getInnerY(), imageWidth + scaledWidth, height);
        }
    }

    @Override
    public int getMinWidth() {
        int minWidth = super.getMinWidth();
        Image bg = getBackground();
        if(bg != null) {
            minWidth = Math.max(minWidth, bg.getWidth());
        }
        return minWidth;
    }

    @Override
    public int getMinHeight() {
        int minHeight = super.getMinHeight();
        Image bg = getBackground();
        if(bg != null) {
            minHeight = Math.max(minHeight, bg.getHeight());
        }
        return minHeight;
    }

    @Override
    public int getPreferredInnerWidth() {
        int prefWidth = super.getPreferredInnerWidth();
        if(progressImage != null) {
            prefWidth = Math.max(prefWidth, progressImage.getWidth());
        }
        return prefWidth;
    }

    @Override
    public int getPreferredInnerHeight() {
        int prefHeight = super.getPreferredInnerHeight();
        if(progressImage != null) {
            prefHeight = Math.max(prefHeight, progressImage.getHeight());
        }
        return prefHeight;
    }

}
