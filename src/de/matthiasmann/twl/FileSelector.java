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

import de.matthiasmann.twl.TableBase.ColumnHeader;
import de.matthiasmann.twl.model.AbstractTableModel;
import de.matthiasmann.twl.model.DefaultTableSelectionModel;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemTreeModel;
import de.matthiasmann.twl.model.TableModel;
import de.matthiasmann.twl.model.TableSelectionModel;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.utils.NaturalSortComparator;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * A File selector widget using FileSystemModel
 *
 * @author Matthias Mann
 */
public class FileSelector extends DialogLayout {

    public interface Callback {
        public void filesSelected(Object[] files);
        public void canceled();
    }

    public static final String STATE_SORT_ASCENDING  = "sortAscending";
    public static final String STATE_SORT_DESCENDING = "sortDescending";

    private final TreeComboBox currentFolder;
    private final FileTableModel fileTableModel;
    private final FileTable fileTable;
    private final Runnable selectionChangedListener;
    private final Button btnOk;
    private final Button btnCancel;

    private TableSelectionModel fileTableSelectionModel;
    private boolean allowMultiSelection;
    private boolean allowFolderSelection;
    private Callback[] callbacks;

    private FileSystemModel fsm;
    private FileSystemTreeModel model;
    private Comparator<Entry> currentSorting;
    private int sortColumn;
    private boolean sortDescending;

    public FileSelector(FileSystemModel fsm) {
        this();
        setFileSystemModel(fsm);
    }

    public FileSelector() {
        currentFolder = new TreeComboBox();
        currentFolder.setTheme("currentFolder");
        fileTableModel = new FileTableModel();
        fileTable = new FileTable(fileTableModel);
        selectionChangedListener = new Runnable() {
            public void run() {
                selectionChanged();
            }
        };

        currentSorting = NameComparator.instance;
        
        Button btnUp = new Button();
        btnUp.setTheme("buttonUp");
        btnUp.addCallback(new Runnable() {
            public void run() {
                goOneLevelUp();
            }
        });

        btnOk = new Button();
        btnOk.setTheme("buttonOk");
        btnOk.addCallback(new Runnable() {
            public void run() {
                acceptSelection(true);
            }
        });

        btnCancel = new Button();
        btnCancel.setTheme("buttonCancel");
        btnCancel.addCallback(new Runnable() {
            public void run() {
                if(callbacks != null) {
                    for(Callback cb : callbacks) {
                        cb.canceled();
                    }
                }
            }
        });

        currentFolder.setPathResolver(new TreeComboBox.PathResolver() {
            public TreeTableNode resolvePath(TreeTableModel model, String path) throws IllegalArgumentException {
                return FileSelector.this.resolvePath(path);
            }
        });
        currentFolder.addCallback(new TreeComboBox.Callback() {
            public void selectedNodeChanged(TreeTableNode node, TreeTableNode previousChildNode) {
                setCurrentNode(node);
            }
        });

        setAllowMultiSelection(true);
        fileTable.addCallback(new TableBase.Callback() {
            public void mouseDoubleClicked(int row, int column) {
                acceptSelection(false);
            }
        });
        
        Label labelCurrentFolder = new Label("Folder");
        labelCurrentFolder.setLabelFor(currentFolder);

        ScrollPane scrollPane = new ScrollPane(fileTable);

        add(labelCurrentFolder);
        add(currentFolder);
        add(btnUp);
        add(scrollPane);
        add(btnOk);
        add(btnCancel);
        
        Group hCurrentFolder = createSequentialGroup()
                .addWidget(labelCurrentFolder)
                .addWidget(currentFolder)
                .addWidget(btnUp);
        Group vCurrentFolder = createParallelGroup()
                .addWidget(labelCurrentFolder)
                .addWidget(currentFolder)
                .addWidget(btnUp);

        Group hButtonGroup = createSequentialGroup()
                .addGap()
                .addWidget(btnOk)
                .addGap(20)
                .addWidget(btnCancel)
                .addGap(20);
        Group vButtonGroup = createParallelGroup()
                .addWidget(btnOk)
                .addWidget(btnCancel);

        setHorizontalGroup(createParallelGroup()
                .addGroup(hCurrentFolder)
                .addWidget(scrollPane)
                .addGroup(hButtonGroup));
        setVerticalGroup(createSequentialGroup()
                .addGroup(vCurrentFolder)
                .addWidget(scrollPane)
                .addGroup(vButtonGroup));

        addActionMapping("goOneLevelUp", "goOneLevelUp");
        addActionMapping("acceptSelection", "acceptSelection", false);
    }

    public FileSystemModel getFileSystemModel() {
        return fsm;
    }

    public void setFileSystemModel(FileSystemModel fsm) {
        this.fsm = fsm;
        if(fsm == null) {
            model = null;
            currentFolder.setModel(null);
            fileTableModel.setData(EMPTY, 0);
        } else {
            model = new FileSystemTreeModel(fsm);
            currentFolder.setModel(model);
            currentFolder.setSeparator(fsm.getSeparator());
            setCurrentNode(model);
        }
    }

    public boolean getAllowMultiSelection() {
        return allowMultiSelection;
    }

    public void setAllowMultiSelection(boolean allowMultiSelection) {
        this.allowMultiSelection = allowMultiSelection;
        if(fileTableSelectionModel != null) {
            fileTableSelectionModel.removeSelectionChangeListener(selectionChangedListener);
        }
        if(allowMultiSelection) {
            fileTableSelectionModel = new DefaultTableSelectionModel();
        } else {
            fileTableSelectionModel = new TableSingleSelectionModel();
        }
        fileTableSelectionModel.addSelectionChangeListener(selectionChangedListener);
        fileTable.setSelectionManager(new TableRowSelectionManager(fileTableSelectionModel));
        selectionChanged();
    }

    public boolean getAllowFolderSelection() {
        return allowFolderSelection;
    }

    public void setAllowFolderSelection(boolean allowFolderSelection) {
        this.allowFolderSelection = allowFolderSelection;
        selectionChanged();
    }

    public void addCallback(Callback callback) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, callback, Callback.class);
    }

    public void removeCallback(Callback callback) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, callback);
    }

    public Object getCurrentFolder() {
        Object node = currentFolder.getCurrentNode();
        if(node instanceof FileSystemTreeModel.FolderNode) {
            return ((FileSystemTreeModel.FolderNode)node).getFolder();
        } else {
            return null;
        }
    }

    public boolean setCurrentFolder(Object folder) {
        FileSystemTreeModel.FolderNode node = model.getNodeForFolder(folder);
        if(node != null) {
            setCurrentNode(node);
            return true;
        }
        return false;
    }

    public void goOneLevelUp() {
        TreeTableNode node = currentFolder.getCurrentNode();
        TreeTableNode parent = node.getParent();
        if(parent != null) {
            setCurrentNode(parent);
        }
    }

    public void acceptSelection(boolean fireCallback) {
        int[] selection = fileTableSelectionModel.getSelection();
        if(!fireCallback || selection.length == 1) {
            Entry entry = fileTableModel.getEntry(selection[0]);
            if(entry != null && entry.isFolder && (!allowFolderSelection || !fireCallback)) {
                setCurrentFolder(entry.obj);
                return;
            }
        }
        if(callbacks != null) {
            Object[] objects = new Object[selection.length];
            for(int i=0 ; i<selection.length ; i++) {
                Entry e = fileTableModel.getEntry(selection[i]);
                if(e.isFolder && !allowFolderSelection) {
                    return;
                }
                objects[i] = e.obj;
            }
            for(Callback cb : callbacks) {
                cb.filesSelected(objects);
            }
        }
    }

    void setSortColumn(int column) {
        if(sortColumn == column) {
            sortDescending = !sortDescending;
        }
        this.sortColumn = column;
        switch(column) {
        case 0: currentSorting = NameComparator.instance; break;
        case 1: currentSorting = ExtensionComparator.instance; break;
        case 2: currentSorting = SizeComparator.instance; break;
        case 3: currentSorting = LastModifiedComparator.instance; break;
        }
        Entry[] entries = fileTableModel.entries.clone();
        sortFilesAndUpdateModel(entries, fileTableModel.numFolders);
    }

    void selectionChanged() {
        boolean foldersSelected = false;
        boolean filesSelected = false;
        for(int row : fileTableSelectionModel.getSelection()) {
            if(fileTableModel.getEntry(row).isFolder) {
                foldersSelected = true;
            } else {
                filesSelected = true;
            }
        }
        if(allowFolderSelection) {
            btnOk.setEnabled(filesSelected ||  foldersSelected);
        } else {
            btnOk.setEnabled(filesSelected && !foldersSelected);
        }
    }

    protected void setCurrentNode(TreeTableNode node) {
        currentFolder.setCurrentNode(node);
        Object[] objs;
        if(node == model) {
            objs = fsm.listRoots();
        } else {
            Object folder = getCurrentFolder();
            objs = fsm.listFolder(folder, null);
        }
        if(objs != null) {
            int lastFileIdx = objs.length;
            Entry[] entries = new Entry[lastFileIdx];
            int numFolders = 0;
            for(int i=0 ; i<objs.length ; i++) {
                Entry e = new Entry(fsm, objs[i]);
                if(e.isFolder) {
                    entries[numFolders++] = e;
                } else {
                    entries[--lastFileIdx] = e;
                }
            }
            Arrays.sort(entries, 0, numFolders, NameComparator.instance);
            sortFilesAndUpdateModel(entries, numFolders);
        } else {
            fileTableModel.setData(EMPTY, 0);
        }
        fileTableSelectionModel.setAnchorIndex(0);
        fileTableSelectionModel.setLeadIndex(0);
    }

    private void sortFilesAndUpdateModel(Entry[] entries, int numFolders) {
        Comparator<Entry> c = currentSorting;
        if(sortDescending) {
            c = Collections.reverseOrder(c);
        }
        Arrays.sort(entries, numFolders, entries.length, c);
        fileTableModel.setData(entries, numFolders);
    }

    TreeTableNode resolvePath(String path) throws IllegalArgumentException {
        Object obj = fsm.getFile(path);
        if(obj != null) {
            FileSystemTreeModel.FolderNode node = model.getNodeForFolder(obj);
            if(node != null) {
                return node;
            }
        }
        throw new IllegalArgumentException("Could not resolve: " + path);
    }

    static class Entry {
        public final Object obj;
        public final String name;
        public final boolean isFolder;
        public final long size;
        public final Date lastModified;

        public Entry(FileSystemModel fsm, Object obj) {
            this.obj = obj;
            this.name = fsm.getName(obj);
            this.isFolder = fsm.isFolder(obj);
            this.lastModified = new Date(fsm.getLastModified(obj));
            if(isFolder) {
                this.size = 0;
            } else {
                this.size = fsm.getSize(obj);
            }
        }

        public String getExtension() {
            int idx = name.lastIndexOf('.');
            if(idx >= 0) {
                return name.substring(idx+1);
            } else {
                return "";
            }
        }
    }
    
    static Entry[] EMPTY = new Entry[0];

    static class FileTableModel extends AbstractTableModel {
        private Entry[] entries = EMPTY;
        private int numFolders;

        public void setData(Entry[] entries, int numFolders) {
            fireRowsDeleted(0, getNumRows());
            this.entries = entries;
            this.numFolders = numFolders;
            fireRowsInserted(0, getNumRows());
        }

        static String COLUMN_HEADER[] = {"File name", "Type", "Size", "Last modified"};

        public String getColumnHeaderText(int column) {
            return COLUMN_HEADER[column];
        }

        public int getNumColumns() {
            return COLUMN_HEADER.length;
        }

        public Object getCell(int row, int column) {
            Entry e = entries[row];
            if(e.isFolder) {
                switch(column) {
                case 0: return "["+e.name+"]";
                case 1: return "Folder";
                case 2: return "";
                case 3: return dateFormat.format(e.lastModified);
                default: return "??";
                }
            } else {
                switch(column) {
                case 0: return e.name;
                case 1: {
                    String ext = e.getExtension();
                    return (ext.length() == 0) ? "File" : ext+"-file";
                }
                case 2: return formatFileSize(e.size);
                case 3: return dateFormat.format(e.lastModified);
                default: return "??";
                }
            }
        }

        public int getNumRows() {
            return entries.length;
        }

        Entry getEntry(int row) {
            if(row >= 0 && row < entries.length) {
                return entries[row];
            } else {
                return null;
            }
        }
        
        static final DateFormat dateFormat = DateFormat.getDateInstance();
        static String SIZE_UNITS[] = {"MB", "KB", "B"};
        static long SIZE_FACTORS[] = {1024*1024, 1024, 1};

        private String formatFileSize(long size) {
            if(size <= 0) {
                return "";
            } else {
                for(int i=0 ;; ++i) {
                    if(size >= SIZE_FACTORS[i]) {
                        long value = (size*10) / SIZE_FACTORS[i];
                        return Long.toString(value / 10) + '.' +
                                Character.forDigit((int)(value % 10), 10) +
                                SIZE_UNITS[i];
                    }
                }
            }
        }
    }

    static class NameComparator implements Comparator<Entry> {
        static final NameComparator instance = new NameComparator();
        public int compare(Entry o1, Entry o2) {
            return NaturalSortComparator.naturalCompare(o1.name, o2.name);
        }
    }

    static class ExtensionComparator implements Comparator<Entry> {
        static final ExtensionComparator instance = new ExtensionComparator();
        public int compare(Entry o1, Entry o2) {
            return NaturalSortComparator.naturalCompare(o1.getExtension(), o2.getExtension());
        }
    }

    static class SizeComparator implements Comparator<Entry> {
        static final SizeComparator instance = new SizeComparator();
        public int compare(Entry o1, Entry o2) {
            return Long.signum(o1.size - o2.size);
        }
    }

    static class LastModifiedComparator implements Comparator<Entry> {
        static final LastModifiedComparator instance = new LastModifiedComparator();
        public int compare(Entry o1, Entry o2) {
            return o1.lastModified.compareTo(o2.lastModified);
        }
    }

    private class FileTable extends Table {
        public FileTable(TableModel model) {
            super(model);
            setTheme("fileTable");
        }

        @Override
        protected ColumnHeader createColumnHeader(final int column) {
            ColumnHeader columnHeader = super.createColumnHeader(column);
            columnHeader.addCallback(new Runnable() {
                public void run() {
                    setSortColumn(column);
                    setSortArrows();
                }
            });
            return columnHeader;
        }

        @Override
        protected void updateColumnHeaderNumbers() {
            super.updateColumnHeaderNumbers();
            setSortArrows();
        }

        protected void setSortArrows() {
            for(int i = 0 ; i < numColumns ; i++) {
                AnimationState animState = columnHeaders[i].getAnimationState();
                animState.setAnimationState(STATE_SORT_ASCENDING, (i == sortColumn) && !sortDescending);
                animState.setAnimationState(STATE_SORT_DESCENDING, (i == sortColumn) && sortDescending);
            }
        }
    }
}
