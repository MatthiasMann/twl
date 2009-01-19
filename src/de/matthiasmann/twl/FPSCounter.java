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

/**
 * A simple FPS counter.
 * Measures the time required to render a specified number of
 * frames (default 100) using System.nanoTime.
 *
 * @see System.nanoTime()
 * @author Matthias Mann
 */
public class FPSCounter extends Label {

    private long startTime;
    private int frames;
    private int framesToCount = 100;
    
    private final char[] fmtBuffer;
    
    public FPSCounter() {
        startTime = System.nanoTime();
        fmtBuffer = new char[16];
    }

    public int getFramesToCount() {
        return framesToCount;
    }

    public void setFramesToCount(int framesToCount) {
        if(framesToCount < 1) {
            throw new IllegalArgumentException("framesToCount < 1");
        }
        this.framesToCount = framesToCount;
    }

    @Override
    protected void paintWidget(GUI gui) {
        if(++frames >= framesToCount) {
            updateFPS();
        }
        super.paintWidget(gui);
    }

    private int format(char[] buf, int value, int decimalPoint) {
        int pos = buf.length;
        while(pos > 0) {
            buf[--pos] = (char)('0' + (value % 10));
            value /= 10;
            if(--decimalPoint == 0) {
                buf[--pos] = '.';
            } else if(value == 0) {
                break;
            }
        }
        return pos;
    }

    private void updateFPS() {
        long curTime = System.nanoTime();
        long elapsed = curTime - startTime;
        startTime = curTime;
        
        int fpsX100 = (int)((frames * (long)1e11) / elapsed);
        int len = format(fmtBuffer, fpsX100, 2);
        
        setText(new String(fmtBuffer, fmtBuffer.length-len, len));
        
        frames = 0;
    }

}
