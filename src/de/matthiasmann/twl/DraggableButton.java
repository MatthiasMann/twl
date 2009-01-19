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
 * A button which generates drag events.
 * It's used in the ValueAdjuster.
 *
 * @author Matthias Mann
 */
public class DraggableButton extends Button {

    public interface DragListener {
        public void dragStarted();
        public void dragged(int deltaX, int deltaY);
        public void dragStopped();
    }
    
    private int dragStartX;
    private int dragStartY;
    private boolean dragging;
    
    private DragListener listener;
    
    public DraggableButton() {
    }

    public boolean isDragActive() {
        return dragging;
    }

    public DragListener getListener() {
        return listener;
    }

    public void setListener(DragListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean handleEvent(Event evt) {
        if(evt.isMouseEvent() && dragging) {
            if(evt.getType() == Event.Type.MOUSE_DRAGED) {
                if(listener != null) {
                    listener.dragged(evt.getMouseX()-dragStartX, evt.getMouseY()-dragStartY);
                }
            }
            if(evt.isMouseDragEnd()) {
                stopDragging(evt);
            }
            return true;
        }

        switch (evt.getType()) {
        case MOUSE_BTNDOWN:
            dragStartX = evt.getMouseX();
            dragStartY = evt.getMouseY();
            break;
        case MOUSE_DRAGED:
            assert !dragging;
            dragging = true;
            getModel().setArmed(false);
            getModel().setPressed(true);
            if(listener != null) {
                listener.dragStarted();
            }
            return true;
        }
        
        return super.handleEvent(evt);
    }

    private void stopDragging(Event evt) {
        if(listener != null) {
            listener.dragStopped();
        }
        dragging = false;
        getModel().setArmed(false);
        getModel().setPressed(false);
        getModel().setHover(isMouseInside(evt));
    }
}
