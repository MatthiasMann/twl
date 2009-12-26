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

import de.matthiasmann.twl.model.AutoCompletionDataSource;
import de.matthiasmann.twl.model.AutoCompletionResult;
import de.matthiasmann.twl.model.SimpleListModel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class EditFieldAutoCompletionWindow extends InfoWindow {

    private final ResultListModel listModel;
    private final ListBox listBox;

    private boolean captureKeys;
    private AutoCompletionDataSource dataSource;
    private ExecutorService executorService;
    private FutureTask<AutoCompletionResult> future;

    public EditFieldAutoCompletionWindow(EditField editField) {
        super(editField);

        this.listModel = new ResultListModel();
        this.listBox = new ListBox(listModel);

        add(listBox);

        editField.addCallback(new EditField.Callback() {
            public void callback(int key) {
                if(key == Keyboard.KEY_NONE) {
                    updateAutoCompletion();
                } else {
                    stopAutoCompletion();
                }
            }
        });
    }

    public EditField getEditField() {
        return (EditField)getOwner();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public AutoCompletionDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(AutoCompletionDataSource dataSource) {
        if(this.dataSource != dataSource) {
            this.dataSource = dataSource;
            if(isOpen()) {
                updateAutoCompletion();
            }
        }
    }

    public void updateAutoCompletion() {
        AutoCompletionResult result = null;
        if(dataSource != null) {
            EditField ef = getEditField();
            GUI gui = getGUI();
            int cursorPos = ef.getCursorPos();
            if(cursorPos > 0 && cursorPos == ef.getTextLength()) {
                String text = ef.getText();
                if(executorService != null && gui != null) {
                    if(future != null) {
                        future.cancel(true);
                    }
                    future = new FutureTask<AutoCompletionResult>(new AsyncQuery(gui, dataSource, text, listModel.result));
                } else {
                    result = dataSource.collectSuggestions(text, listModel.result);
                }
            }
        }
        updateAutoCompletion(result);
    }

    protected void updateAutoCompletion(AutoCompletionResult results) {
        listModel.setResult(results);
        captureKeys = false;
        installAutoCompletion();
    }

    void checkFuture() {
        if(future != null) {
            if(future.isDone()) {
                AutoCompletionResult result = null;
                try {
                    result = future.get();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();   // should never happen
                } catch (ExecutionException ex) {
                    Logger.getLogger(EditFieldAutoCompletionWindow.class.getName()).log(Level.SEVERE, "Exception while collection auto completion results", ex);
                }
                future = null;
                updateAutoCompletion(result);
            }
        }
    }
    
    @Override
    protected boolean handleEvent(Event evt) {
        if(evt.isKeyEvent()) {
            if(captureKeys) {
                if(evt.getType() == Event.Type.KEY_PRESSED) {
                    switch (evt.getKeyCode()) {
                        case Keyboard.KEY_RETURN:
                            if(listBox.getSelected() >= 0) {
                                getEditField().setText(listModel.getEntry(listBox.getSelected()));
                                stopAutoCompletion();
                            }
                            break;

                        case Keyboard.KEY_ESCAPE:
                            stopAutoCompletion();
                            break;

                        case Keyboard.KEY_UP:
                        case Keyboard.KEY_DOWN:
                            listBox.handleEvent(evt);
                            break;

                        default:
                            if(evt.hasKeyChar()) {
                                stopAutoCompletion();
                                return false;
                            }
                            break;
                    }
                }
                return true;
            } else {
                switch (evt.getKeyCode()) {
                    case Keyboard.KEY_UP:
                    case Keyboard.KEY_DOWN:
                        listBox.handleEvent(evt);
                        startCapture();
                        return true;
                    default:
                        return false;
                }
            }
        }
        
        return super.handleEvent(evt);
    }

    public void stopAutoCompletion() {
        listModel.setResult(null);
        installAutoCompletion();
    }

    private void startCapture() {
        captureKeys = true;
        installAutoCompletion();
    }

    private void installAutoCompletion() {
        if(listModel.getNumEntries() > 0) {
            getEditField().setAutoCompletionWindow(this, captureKeys);
        } else {
            captureKeys = false;
            getEditField().setAutoCompletionWindow(null, false);
        }
    }

    static class ResultListModel extends SimpleListModel<String> {
        AutoCompletionResult result;

        public void setResult(AutoCompletionResult result) {
            this.result = result;
            fireAllChanged();
        }

        public int getNumEntries() {
            return (result == null) ? 0 : result.getNumResults();
        }

        public String getEntry(int index) {
            return result.getResult(index);
        }
    }

    class AsyncQuery implements Callable<AutoCompletionResult>, Runnable {
        private final GUI gui;
        private final AutoCompletionDataSource dataSource;
        private final String text;
        private final AutoCompletionResult prevResult;

        public AsyncQuery(GUI gui, AutoCompletionDataSource dataSource, String text, AutoCompletionResult prevResult) {
            this.gui = gui;
            this.dataSource = dataSource;
            this.text = text;
            this.prevResult = prevResult;
        }

        public AutoCompletionResult call() throws Exception {
            AutoCompletionResult acr = dataSource.collectSuggestions(text, prevResult);
            gui.invokeLater(this);
            return acr;
        }

        public void run() {
            checkFuture();
        }
    }
}
