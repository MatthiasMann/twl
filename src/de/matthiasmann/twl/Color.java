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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * @author MannMat
 */
public class Color {

    public static final Color BLACK = new Color(0xFF000000);
    public static final Color SILVER = new Color(0xFFC0C0C0);
    public static final Color GRAY = new Color(0xFF808080);
    public static final Color WHITE = new Color(0xFFFFFFFF);
    public static final Color MAROON = new Color(0xFF800000);
    public static final Color RED = new Color(0xFFFF0000);
    public static final Color PURPLE = new Color(0xFF800080);
    public static final Color FUCHSIA = new Color(0xFFFF00FF);
    public static final Color GREEN = new Color(0xFF008000);
    public static final Color LIME = new Color(0xFF00FF00);
    public static final Color OLVIVE = new Color(0xFF808000);
    public static final Color ORANGE = new Color(0xFFFFA500);
    public static final Color YELLOW = new Color(0xFFFFFF00);
    public static final Color NAVY = new Color(0xFF000080);
    public static final Color BLUE = new Color(0xFF0000FF);
    public static final Color TEAL = new Color(0xFF008080);
    public static final Color AQUA = new Color(0xFF00FFFF);
    
    private final byte r;
    private final byte g;
    private final byte b;
    private final byte a;

    public Color(byte r, byte g, byte b, byte a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
    
    public Color(int rgba) {
        this.r = (byte)(rgba >> 16);
        this.g = (byte)(rgba >>  8);
        this.b = (byte)(rgba      );
        this.a = (byte)(rgba >> 24);
    }

    public byte getR() {
        return r;
    }

    public byte getG() {
        return g;
    }

    public byte getB() {
        return b;
    }

    public byte getA() {
        return a;
    }

    public float getRedFloat() {
        return (r & 255) / 255f;
    }

    public float getGreenFloat() {
        return (g & 255) / 255f;
    }

    public float getBlueFloat() {
        return (b & 255) / 255f;
    }

    public float getAlphaFloat() {
        return (a & 255) / 255f;
    }

    public void getFloats(float[] dst, int off) {
        dst[off+0] = getRedFloat();
        dst[off+1] = getGreenFloat();
        dst[off+2] = getBlueFloat();
        dst[off+3] = getAlphaFloat();
    }

    public static Color getColorByName(String name) {
        name = name.toUpperCase();
        try {
            Field f = Color.class.getField(name);
            if(Modifier.isStatic(f.getModifiers()) && f.getType() == Color.class) {
                return (Color)f.get(null);
            }
        } catch (Throwable ex) {
            // ignore
        }
        return null;
    }

    public static Color parserColor(String value) throws NumberFormatException {
        if(value.length() == 4 && value.charAt(0) == '#') {
            int rgb4 = Integer.parseInt(value.substring(1), 16);
            int r = ((rgb4 >> 8) & 0xF) * 0x11;
            int g = ((rgb4 >> 4) & 0xF) * 0x11;
            int b = ((rgb4     ) & 0xF) * 0x11;
            return new Color(0xFF000000 | (r << 16) | (g << 8) | b);
        }
        if(value.length() == 5 && value.charAt(0) == '#') {
            int rgb4 = Integer.parseInt(value.substring(1), 16);
            int a = ((rgb4 >> 12) & 0xF) * 0x11;
            int r = ((rgb4 >>  8) & 0xF) * 0x11;
            int g = ((rgb4 >>  4) & 0xF) * 0x11;
            int b = ((rgb4      ) & 0xF) * 0x11;
            return new Color((a << 24) | (r << 16) | (g << 8) | b);
        }
        if(value.length() == 7 && value.charAt(0) == '#') {
            return new Color(0xFF000000 | Integer.parseInt(value.substring(1), 16));
        }
        if(value.length() == 9 && value.charAt(0) == '#') {
            return new Color((int)Long.parseLong(value.substring(1), 16));
        }
        return Color.getColorByName(value);
    }

    public Color multiply(Color other) {
        return new Color(
                mul(r, other.r),
                mul(g, other.g),
                mul(b, other.b),
                mul(a, other.a));
    }

    private byte mul(byte a, byte b) {
        return (byte)(((a & 255) * (b & 255)) / 255);
    }
}
