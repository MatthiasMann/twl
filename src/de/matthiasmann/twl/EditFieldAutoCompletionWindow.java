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
import java.util.concurrent.Future;
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
    private Future<AutoCompletionResult> future;

    /**
     * Creates an EditFieldAutoCompletionWindow associated with the specified
     * EditField. It will register itself as callback.
     *
     * Auto completion will start to work once a data source is set
     *
     * @param editField the EditField to which auto completion should be applied
     */
    public EditFieldAutoCompletionWindow(EditField editField) {
        super(editField);

        this.listModel = new ResultListModel();
        this.listBox = new ListBox(listModel);
        
        add(listBox);

        Callbacks cb = new Callbacks();
        listBox.addCallback(cb);
        editField.addCallback(cb);
    }

    /**
     * Creates an EditFieldAutoCompletionWindow associated with the specified
     * EditField. It will register itself as callback.
     *
     * Auto completion is operational with the given data source (when it's not null)
     *
     * @param editField the EditField to which auto completion should be applied
     * @param dataSource the data source used for auto completion - can be null
     */
    public EditFieldAutoCompletionWindow(EditField editField, AutoCompletionDataSource dataSource) {
        this(editField);
        this.dataSource = dataSource;
    }

    /**
     * Creates an EditFieldAutoCompletionWindow associated with the specified
     * EditField. It will register itself as callback.
     *
     * Auto completion is operational with the given data source (when it's not null)
     *
     * @see #setExecutorService(executorService)
     *
     * @param editField the EditField to which auto completion should be applied
     * @param dataSource the data source used for auto completion - can be null
     * @param executorService the executorService used to execute the data source queries
     */
    public EditFieldAutoCompletionWindow(EditField editField,
            AutoCompletionDataSource dataSource,
            ExecutorService executorService) {
        this(editField);
        this.dataSource = dataSource;
        this.executorService = executorService;
    }

    /**
     * Returns the EditField to which this EditFieldAutoCompletionWindow is attached
     * @return the EditField
     */
    public EditField getEditField() {
        return (EditField)getOwner();
    }

    /**
     * Returns the current ExecutorService
     * @return the current ExecutorService
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Sets the ExecutorService which is used to perform async queries on the
     * AutoCompletionDataSource.
     *
     * If it is null then all queries are done synchronously from the EditField
     * callback. This is good as long as data source is very fast (eg small in
     * memory tables).
     *
     * When the data source quries take too long they will impact the UI
     * responsiveness. To prevent that the queries can be executed in another
     * thread. This requires the data source and results to be thread save.
     *
     * @param executorService the ExecutorService or null
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        cancelFuture();
    }

    /**
     * Returns the current data source
     * @return the current data source
     */
    public AutoCompletionDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets a new data source.
     *
     * If the info window is currently open, then the displayed auto completion
     * will be refreshed. If you also need to change the ExecutorService then
     * it's adviced to do that first.
     *
     * @param dataSource the new AutoCompletionDataSource - can be null
     */
    public void setDataSource(AutoCompletionDataSource dataSource) {
        this.dataSource = dataSource;
        cancelFuture();
        if(isOpen()) {
            updateAutoCompletion();
        }
    }

    /**
     * This will update the auto completion and open the info window when results
     * are available
     */
    public void updateAutoCompletion() {
        cancelFuture();
        AutoCompletionResult result = null;
        if(dataSource != null) {
            EditField ef = getEditField();
            int cursorPos = ef.getCursorPos();
            if(cursorPos > 0 && cursorPos == ef.getTextLength()) {
                String text = ef.getText();
                GUI gui = ef.getGUI();
                if(listModel.result != null) {
                    result = listModel.result.refine(text);
                }
                if(result == null) {
                    if(executorService != null && gui != null) {
                        future = executorService.submit((Callable<AutoCompletionResult>)
                                new AsyncQuery(gui, dataSource, text, listModel.result));
                    } else {
                        try {
                            result = dataSource.collectSuggestions(text, listModel.result);
                        } catch (Exception ex) {
                            reportQueryException(ex);
                        }
                    }
                }
            }
        }
        updateAutoCompletion(result);
    }

    /**
     * Stops the auto completion.
     * 
     * Closes the infow window and discards the collected results.
     */
    public void stopAutoCompletion() {
        listModel.setResult(null);
        installAutoCompletion();
    }

    @Override
    protected void infoWindowClosed() {
        stopAutoCompletion();
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
                    // set the interrupted state again
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    reportQueryException(ex.getCause());
                }
                future = null;
                updateAutoCompletion(result);
            }
        }
    }

    void cancelFuture() {
        if(future != null) {
            future.cancel(true);
            future = null;
        }
    }

    protected void reportQueryException(Throwable ex) {
        Logger.getLogger(EditFieldAutoCompletionWindow.class.getName()).log(
                Level.SEVERE, "Exception while collecting auto completion results", ex);
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(evt.isKeyEvent()) {
            if(captureKeys) {
                if(evt.getType() == Event.Type.KEY_PRESSED) {
                    switch (evt.getKeyCode()) {
                        case Keyboard.KEY_RETURN:
                            acceptAutoCompletion();
                            break;

                        case Keyboard.KEY_ESCAPE:
                            stopAutoCompletion();
                            break;

                        case Keyboard.KEY_UP:
                        case Keyboard.KEY_DOWN:
                        case Keyboard.KEY_PRIOR:
                        case Keyboard.KEY_NEXT:
                        case Keyboard.KEY_HOME:
                        case Keyboard.KEY_END:
                            listBox.handleEvent(evt);
                            break;

                        default:
                            if(evt.hasKeyChar() || evt.getKeyCode() == Keyboard.KEY_BACK) {
                                if(!acceptAutoCompletion()) {
                                    stopAutoCompletion();
                                }
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

    boolean acceptAutoCompletion() {
        int selected = listBox.getSelected();
        if(selected >= 0) {
            getEditField().setText(listModel.getEntry(selected));
            stopAutoCompletion();
            return true;
        }
        return false;
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

    class Callbacks implements EditField.Callback, CallbackWithReason<ListBox.CallbackReason> {
        public void callback(int key) {
            if(key == Keyboard.KEY_NONE) {
                updateAutoCompletion();
            } else {
                stopAutoCompletion();
            }
        }

        public void callback(ListBox.CallbackReason reason) {
            switch(reason) {
                case MOUSE_DOUBLE_CLICK:
                    acceptAutoCompletion();
                    break;
            }
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
