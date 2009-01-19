/*
 * Copyright (c) 2008-2009, Matthias Mann
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

import de.matthiasmann.twl.utils.HashEntry;

/**
 *
 * @author Matthias Mann
 */
public class AnimationState implements de.matthiasmann.twl.renderer.AnimationState {

    private State[] stateTable;
    private int stateTableSize;
    private GUI gui;

    public AnimationState() {
        this.stateTable = new State[16];
    }

    public void setGUI(GUI gui) {
        this.gui = gui;
        
        long curTime = getCurrentTime();
        for(State s1 : stateTable) {
            for(State s=s1 ; s!=null ; s=s.next()) {
                s.lastChangedTime = curTime;
            }
        }
    }

    /**
     * Returns the time since the specified state has changed in ms.
     * If the specified state was never changed then a free running time is returned.
     *
     * @param stateName the state name.
     * @return time since last state change is ms.
     * @see #getAnimationTime()
     */
    public int getAnimationTime(String stateName) {
        State state = HashEntry.get(stateTable, stateName);
        if(state != null) {
            return (int)Math.min(Integer.MAX_VALUE, getCurrentTime() - state.lastChangedTime);
        }
        return (int)getCurrentTime() & ((1<<31)-1);
    }

    /**
     * Checks if the given state is active.
     *
     * @param stateName the state name.
     * @return true if the state is set
     */
    public boolean getAnimationState(String stateName) {
        State state = HashEntry.get(stateTable, stateName);
        if(state != null) {
            return state.active;
        }
        return false;
    }

    public void setAnimationState(String stateName, boolean active) {
        State state = getOrCreate(stateName);
        if(state.active != active) {
            state.active = active;
            state.lastChangedTime = getCurrentTime();
        }
    }

    public void resetAnimationTime(String stateName) {
        State state = getOrCreate(stateName);
        state.lastChangedTime = getCurrentTime();
    }

    private State getOrCreate(String stateName) {
        State state = HashEntry.get(stateTable, stateName);
        if(state == null) {
            state = createState(stateName, state);
        }
        return state;
    }

    private State createState(String stateName, State state) {
        state = new State(stateName);
        state.lastChangedTime = getCurrentTime();
        stateTable = HashEntry.maybeResizeTable(stateTable, stateTableSize);
        HashEntry.insertEntry(stateTable, state);
        stateTableSize++;
        return state;
    }

    private long getCurrentTime() {
        return (gui != null) ? gui.curTime : 0;
    }

    static class State extends HashEntry<String, State> {
        long lastChangedTime;
        boolean active;

        public State(String key) {
            super(key);
        }
    }
}
