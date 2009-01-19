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
 * A timer interface for UI timing - like auto scrolling.
 * 
 * @author Matthias Mann
 */
public abstract interface Timer {

    /**
     * Returns true if the timer is already running.
     * @return true if the timer is already running.
     */
    public boolean isRunning();
    
    /**
     * Sets the delay in ms till next expiration
     * @param delay in ms
     */
    public void setDelay(int delay);
    
    /**
     * Starts the timer. If it is already running then this method does nothing.
     */
    public void start();
    
    /**
     * Stops the timer. If the timer is not running then this method does nothing.
     */
    public void stop();
    
    /**
     * Sets the callback that should be executed once the timer expires.
     * @param callback the callback.
     */
    public void setCallback(Runnable callback);

    /**
     * Returns true if the timer is a continous firing timer.
     * @return true if the timer is a continous firing timer.
     */
    public boolean isContinuous();

    /**
     * Sets the timer continous mode. A timer in continous mode must be stopped manually.
     * @param continuous true if the timer should auto restart after firing.
     */
    public void setContinuous(boolean continuous);
    
}
