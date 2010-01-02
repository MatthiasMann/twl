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
package de.matthiasmann.twl.utils;

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.Renderer;

/**
 * A utility class to animate tint colors for widgets
 *
 * @author Matthias Mann
 */
public class TintAnimator {

    /**
     * A time source for the fade animation
     */
    public interface TimeSource {
        /**
         * Restarts the time from 0 for a new fade animation
         */
        public void resetTime();
        /**
         * Returns the current time (since last reset) in milliseconds.
         * @return current time in ms
         */
        public int getTime();
    }

    private static final float ZERO_EPSILON = 1e-3f;
    private static final float ONE_EPSILON = 1f - ZERO_EPSILON;

    private final TimeSource timeSource;
    private final float[] currentTint;
    private int fadeDuration;
    private boolean fadeActive;
    private boolean hasTint;

    /**
     * Creates a new TintAnimator which starts in the specified color
     *
     * @param timeSource the time source for the fade animation
     * @param color the starting color
     */
    public TintAnimator(TimeSource timeSource, Color color) {
        if(timeSource == null) {
            throw new NullPointerException("timeSource");
        }
        if(color == null) {
            throw new NullPointerException("color");
        }
        this.timeSource = timeSource;
        this.currentTint = new float[12];
        setColor(color);
    }

    /**
     * Creates a new TintAnimator which starts with Color.WHITE
     */
    public TintAnimator(TimeSource timeSource) {
        this(timeSource, Color.WHITE);
    }

    /**
     * Sets the current color without a fade. Any active fade is stopped.
     * The time source is also reset even so no animation is started.
     * 
     * @param color the new color
     */
    public void setColor(Color color) {
        color.getFloats(currentTint, 0);
        color.getFloats(currentTint, 4);
        hasTint = !Color.WHITE.equals(color);
        fadeActive = false;
        fadeDuration = 0;
        timeSource.resetTime();
    }

    /**
     * Fade the current color to the specified color
     *
     * @param color the destination color of the fade
     * @param fadeDuration the fade time in miliseconds
     */
    public void fadeTo(Color color, int fadeDuration) {
        if(fadeDuration <= 0) {
            setColor(color);
        } else {
            color.getFloats(currentTint, 8);
            System.arraycopy(currentTint, 0, currentTint, 4, 4);
            this.fadeActive = true;
            this.fadeDuration = fadeDuration;
            this.hasTint = true;
            timeSource.resetTime();
        }
    }

    /**
     * Fade the current color to alpha 0.0f
     *
     * @param fadeDuration the fade time in miliseconds
     */
    public void fadeToHide(int fadeDuration) {
        if(fadeDuration <= 0) {
            currentTint[3] = 0.0f;
            this.fadeActive = false;
            this.fadeDuration = 0;
            this.hasTint = true;
        } else {
            System.arraycopy(currentTint, 0, currentTint, 4, 8);
            currentTint[11] = 0.0f;
            this.fadeActive = !isZeroAlpha();
            this.fadeDuration = fadeDuration;
            this.hasTint = true;
            timeSource.resetTime();
        }
    }

    /**
     * Updates the fade to the specified time. The time must go from 0 to
     * fadeDuration.
     */
    public void update() {
        if(fadeActive) {
            int time = timeSource.getTime();
            float t = Math.min(time, fadeDuration) / (float)fadeDuration;
            float tm1 = 1.0f - t;
            float[] tint = currentTint;
            for(int i=0 ; i<4 ; i++) {
                tint[i] = tm1 * tint[i+4] + t * tint[i+8];
            }
            if(time >= fadeDuration) {
                fadeActive = false;
                // disable tinted rendering if we have full WHITE as tint
                hasTint =
                        (currentTint[0] < ONE_EPSILON) ||
                        (currentTint[1] < ONE_EPSILON) ||
                        (currentTint[2] < ONE_EPSILON) ||
                        (currentTint[3] < ONE_EPSILON);
            }
        }
    }

    /**
     * Returns true when a fade is active
     * @return true when a fade is active
     */
    public boolean isFadeActive() {
        return fadeActive;
    }

    /**
     * Returns true when the current tint color is not Color.WHITE
     * @return true when the current tint color is not Color.WHITE
     */
    public boolean hasTint() {
        return hasTint;
    }

    /**
     * Returns true is the current alpha value is 0.0f
     * @return true is the current alpha value is 0.0f
     */
    public boolean isZeroAlpha() {
        return currentTint[3] <= ZERO_EPSILON;
    }
    
    /**
     * Calls {@code renderer.pushGlobalTintColor} with the current tint color.
     * It is important to call {@code renderer.popGlobalTintColor} after this
     * method.
     *
     * @param renderer The renderer
     *
     * @see Renderer#pushGlobalTintColor(float, float, float, float)
     * @see Renderer#popGlobalTintColor()
     */
    public void paintWithTint(Renderer renderer) {
        float[] tint = this.currentTint;
        renderer.pushGlobalTintColor(tint[0], tint[1], tint[2], tint[3]);
    }

    /**
     * A time source which uses the GUI object of the specified widget
     */
    public static class GUITimeSource implements TimeSource {
        private final Widget owner;
        private long startTime;

        public GUITimeSource(Widget owner) {
            if(owner == null) {
                throw new NullPointerException("owner");
            }
            this.owner = owner;
            resetTime();
        }

        public int getTime() {
            return (int)(getCurrentTime() - startTime);
        }

        public void resetTime() {
            startTime = getCurrentTime();
        }

        private long getCurrentTime() {
            GUI gui = gui = owner.getGUI();
            return (gui != null) ? gui.getTimeMillis() : 0;
        }
    }

    /**
     * A time source which uses a specified animation state as time source.
     */
    public static class AnimationStateTimeSource implements TimeSource {
        private final AnimationState animState;
        private final String animStateKey;

        public AnimationStateTimeSource(AnimationState animState, String animStateKey) {
            if(animState == null) {
                throw new NullPointerException("animState");
            }
            if(animStateKey == null) {
                throw new NullPointerException("animStateKey");
            }
            this.animState = animState;
            this.animStateKey = animStateKey;
        }

        public int getTime() {
            return animState.getAnimationTime(animStateKey);
        }

        /**
         * Calls resetAnimationTime on the animation state
         * @see AnimationState#resetAnimationTime(java.lang.String)
         */
        public void resetTime() {
            animState.resetAnimationTime(animStateKey);
        }
    }
}
