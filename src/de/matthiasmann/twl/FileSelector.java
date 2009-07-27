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

import de.matthiasmann.twl.ListBox.CallbackReason;
import de.matthiasmann.twl.TableBase.ColumnHeader;
import de.matthiasmann.twl.model.AbstractTableModel;
import de.matthiasmann.twl.model.DefaultTableSelectionModel;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.model.FileSystemTreeModel;
import de.matthiasmann.twl.model.SimpleListModel;
import de.matthiasmann.twl.model.TableModel;
import de.matthiasmann.twl.model.TableSelectionModel;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.ToggleButtonModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twl.utils.NaturalSortComparator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.prefs.Preferences;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

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

    public static class NamedFileFilter {
        private final String name;
        private final FileSystemModel.FileFilter fileFilter;

        public NamedFileFilter(String name, FileFilter fileFilter) {
            this.name = name;
            this.fileFilter = fileFilter;
        }
        public String getDisplayName() {
            return name;
        }
        public FileSystemModel.FileFilter getFileFilter() {
            return fileFilter;
        }
    }

    public static NamedFileFilter AllFilesFilter = new NamedFileFilter("All files", null);

    public static final String STATE_SORT_ASCENDING  = "sortAscending";
    public static final String STATE_SORT_DESCENDING = "sortDescending";

    private static final int MAX_LRU_SIZE = 10;

    private final PersistentState persistentState;
    private final TreeComboBox currentFolder;
    private final FileTableModel fileTableModel;
    private final FileTable fileTable;
    private final Runnable selectionChangedListener;
    private final Button btnUp;
    private final Button btnFolderLRU;
    private final Button btnOk;
    private final Button btnCancel;
    private final Button btnRefresh;
    private final Button btnShowFolders;
    private final Button btnShowHidden;
    private final ComboBox fileFilterBox;
    private final FileFiltersModel fileFiltersModel;

    private TableSelectionModel fileTableSelectionModel;
    private boolean allowMultiSelection;
    private boolean allowFolderSelection;
    private Callback[] callbacks;
    private NamedFileFilter activeFileFilter;

    private FileSystemModel fsm;
    private FileSystemTreeModel model;
    private Comparator<Entry> currentSorting;
    private int sortColumn;
    private boolean sortDescending;

    private Preferences prefs;
    private String prefsKey;

    public FileSelector(FileSystemModel fsm) {
        this();
        setFileSystemModel(fsm);
    }

    public FileSelector() {
        persistentState = new PersistentState();
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
        
        btnUp = new Button();
        btnUp.setTheme("buttonUp");
        btnUp.addCallback(new Runnable() {
            public void run() {
                goOneLevelUp();
            }
        });

        btnFolderLRU = new Button();
        btnFolderLRU.setTheme("buttonLRU");
        btnFolderLRU.addCallback(new Runnable() {
            public void run() {
                showFolderLRU();
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

        activeFileFilter = AllFilesFilter;
        fileFiltersModel = new FileFiltersModel();
        fileFilterBox = new ComboBox(fileFiltersModel);
        fileFilterBox.setTheme("fileFiltersBox");
        fileFilterBox.setComputeWidthFromModel(true);
        fileFilterBox.setVisible(false);
        fileFilterBox.addCallback(new Runnable() {
            public void run() {
                int idx = fileFilterBox.getSelected();
                if(idx >= 0) {
                    setFileFilter(fileFiltersModel.getFileFilter(idx));
                }
            }
        });
        
        Label labelCurrentFolder = new Label("Folder");
        labelCurrentFolder.setLabelFor(currentFolder);

        ScrollPane scrollPane = new ScrollPane(fileTable);

        Runnable showBtnCallback = new Runnable() {
            public void run() {
                refreshFileTable();
            }
        };

        btnRefresh = new Button();
        btnRefresh.setTheme("buttonRefresh");
        btnRefresh.addCallback(showBtnCallback);
        
        btnShowFolders = new Button(new PersistentStateButtonModel(PersistentState.FLAG_SHOW_FOLDERS));
        btnShowFolders.setTheme("buttonShowFolders");
        btnShowFolders.addCallback(showBtnCallback);

        btnShowHidden = new Button(new PersistentStateButtonModel(PersistentState.FLAG_SHOW_HIDDEN));
        btnShowHidden.setTheme("buttonShowHidden");
        btnShowHidden.addCallback(showBtnCallback);

        add(labelCurrentFolder);
        add(currentFolder);
        add(btnFolderLRU);
        add(btnUp);
        add(scrollPane);
        add(fileFilterBox);
        add(btnOk);
        add(btnCancel);
        add(btnRefresh);
        add(btnShowFolders);
        add(btnShowHidden);
        
        Group hCurrentFolder = createSequentialGroup()
                .addWidget(labelCurrentFolder)
                .addWidget(currentFolder)
                .addWidget(btnFolderLRU)
                .addWidget(btnUp);
        Group vCurrentFolder = createParallelGroup()
                .addWidget(labelCurrentFolder)
                .addWidget(currentFolder)
                .addWidget(btnFolderLRU)
                .addWidget(btnUp);

        Group hButtonGroup = createSequentialGroup()
                .addWidget(fileFilterBox)
                .addGap()
                .addWidget(btnOk)
                .addGap(20)
                .addWidget(btnCancel)
                .addGap(20);
        Group vButtonGroup = createParallelGroup()
                .addWidget(fileFilterBox)
                .addWidget(btnOk)
                .addWidget(btnCancel);

        Group hShowBtns = createParallelGroup()
                .addWidget(btnRefresh)
                .addWidget(btnShowFolders)
                .addWidget(btnShowHidden);
        Group vShowBtns = createSequentialGroup()
                .addWidget(btnRefresh)
                .addGap(MEDIUM_GAP)
                .addWidget(btnShowFolders)
                .addWidget(btnShowHidden)
                .addGap();

        Group hMainGroup = createSequentialGroup()
                .addGroup(hShowBtns)
                .addWidget(scrollPane);
        Group vMainGroup = createParallelGroup()
                .addGroup(vShowBtns)
                .addWidget(scrollPane);

        setHorizontalGroup(createParallelGroup()
                .addGroup(hCurrentFolder)
                .addGroup(hMainGroup)
                .addGroup(hButtonGroup));
        setVerticalGroup(createSequentialGroup()
                .addGroup(vCurrentFolder)
                .addGroup(vMainGroup)
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

    public void addFileFilter(NamedFileFilter filter) {
        if(filter == null) {
            throw new NullPointerException("filter");
        }
        fileFiltersModel.addFileFilter(filter);
        fileFilterBox.setVisible(fileFiltersModel.getNumEntries() > 0);
        if(fileFilterBox.getSelected() < 0) {
            fileFilterBox.setSelected(0);
        }
    }

    public void removeFileFilter(NamedFileFilter filter) {
        if(filter == null) {
            throw new NullPointerException("filter");
        }
        fileFiltersModel.removeFileFilter(filter);
        if(fileFiltersModel.getNumEntries() == 0) {
            fileFilterBox.setVisible(false);
            setFileFilter(null);
        }
    }

    public void removeAllFileFilters() {
        fileFiltersModel.removeAll();
        fileFilterBox.setVisible(false);
        setFileFilter(null);
    }

    public void setFileFilter(NamedFileFilter filter) {
        if(filter == null) {
            throw new NullPointerException("filter");
        }
        activeFileFilter = filter;
        refreshFileTable();
    }

    public NamedFileFilter getFileFilter() {
        return activeFileFilter;
    }

    public boolean getShowFolders() {
        return btnShowFolders.getModel().isSelected();
    }

    public void setShowFolders(boolean showFolders) {
        btnShowFolders.getModel().setSelected(showFolders);
    }

    public boolean getShowHidden() {
        return btnShowHidden.getModel().isSelected();
    }

    public void setShowHidden(boolean showHidden) {
        btnShowHidden.getModel().setSelected(showHidden);
    }

    public void setPersistentStorage(Preferences prefs, String key) {
        if(prefs != null) {
            if(key == null) {
                throw new NullPointerException("key");
            }
            this.prefs = prefs;
            this.prefsKey = key;
            byte[] arr = prefs.getByteArray(key, null);
            if(arr != null) {
                try {
                    persistentState.decode(arr);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            btnShowFolders.modelStateChanged();
            btnShowHidden.modelStateChanged();
        } else {
            this.prefs = null;
            this.prefsKey = null;
        }
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
        if((!allowFolderSelection || !fireCallback) && selection.length == 1) {
            Entry entry = fileTableModel.getEntry(selection[0]);
            if(entry != null && entry.isFolder) {
                setCurrentFolder(entry.obj);
                return;
            }
        }
        if(fireCallback && callbacks != null) {
            Object[] objects = new Object[selection.length];
            for(int i=0 ; i<selection.length ; i++) {
                Entry e = fileTableModel.getEntry(selection[i]);
                if(e.isFolder && !allowFolderSelection) {
                    return;
                }
                objects[i] = e.obj;
            }
            if(persistentState != null) {
                addToLRU(selection);
                savePersistentState();
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
        refreshFileTable();
    }

    void refreshFileTable() {
        final TreeTableNode node = currentFolder.getCurrentNode();
        Object[] objs;
        if(node == model) {
            objs = fsm.listRoots();
        } else {
            Object folder = getCurrentFolder();
            FileFilter filter = activeFileFilter.getFileFilter();
            if(filter != null || !getShowFolders() || !getShowHidden()) {
                filter = new FileFilterWrapper(filter, getShowFolders(), getShowHidden());
            }
            objs = fsm.listFolder(folder,filter);
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
            setLeadIndex(null);
        }
    }

    private void sortFilesAndUpdateModel(Entry[] entries, int numFolders) {
        Entry leadEntry = fileTableModel.getEntry(fileTableSelectionModel.getLeadIndex());
        Entry[] selectedEntries = fileTableModel.getEntries(fileTableSelectionModel.getSelection());
        Comparator<Entry> c = currentSorting;
        if(sortDescending) {
            c = Collections.reverseOrder(c);
        }
        Arrays.sort(entries, numFolders, entries.length, c);
        fileTableModel.setData(entries, numFolders);
        for(Entry e : selectedEntries) {
            int idx = fileTableModel.findEntry(e);
            if(idx >= 0) {
                fileTableSelectionModel.addSelection(idx, idx);
            }
        }
        setLeadIndex(leadEntry);
    }
    
    private void setLeadIndex(Entry entry) {
        int index = Math.max(0, fileTableModel.findEntry(entry));
        fileTableSelectionModel.setLeadIndex(index);
        fileTableSelectionModel.setAnchorIndex(index);
        fileTable.scrollToRow(index);
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

    void showFolderLRU() {
        final PopupWindow popup = new PopupWindow(this);
        final ListBox listBox = new ListBox(new FolderListModel(persistentState));
        popup.setTheme("fileselector-folderLRUpopup");
        popup.add(listBox);
        if(popup.openPopup()) {
            popup.setInnerSize(getInnerWidth()*2/3, getInnerHeight()*2/3);
            popup.setPosition(btnFolderLRU.getX() - popup.getWidth(), btnFolderLRU.getY());
            listBox.addCallback(new CallbackWithReason<ListBox.CallbackReason>() {
                public void callback(CallbackReason reason) {
                    if(reason.actionRequested()) {
                        popup.closePopup();
                        int idx = listBox.getSelected();
                        if(idx >= 0) {
                            String path = persistentState.groups[idx].path;
                            try {
                                TreeTableNode node = resolvePath(path);
                                setCurrentNode(node);
                            } catch(IllegalArgumentException ex) {
                                persistentState.removeGroup(idx);
                                savePersistentState();
                            }
                        }
                    }
                }
            });
        }
    }

    private void addToLRU(int[] selection) {
        String[] files = new String[selection.length];
        for(int i=0 ; i<files.length ; i++) {
            Entry e = fileTableModel.getEntry(selection[i]);
            files[i] = e.name;
        }
        PersistentState.FileGroup group = new PersistentState.FileGroup(
                fsm.getPath(getCurrentFolder()), files);
        persistentState.addGroup(group);
    }

    private void savePersistentState() {
        assert(persistentState != null);
        assert(prefs != null);
        assert(prefsKey != null);
        persistentState.setFlag(PersistentState.FLAG_SHOW_FOLDERS, btnShowFolders.getModel().isSelected());
        persistentState.setFlag(PersistentState.FLAG_SHOW_HIDDEN, btnShowHidden.getModel().isSelected());
        try {
            byte[] arr = persistentState.encode();
            if(arr != null) {
                prefs.putByteArray(prefsKey, arr);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
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

        int findEntry(Entry entry) {
            for(int i=0 ; i<entries.length ; i++) {
                if(entries[i] == entry) {
                    return i;
                }
            }
            return -1;
        }

        Entry[] getEntries(int[] selection) {
            final int count = selection.length;
            if(count == 0) {
                return EMPTY;
            }
            Entry[] result = new Entry[count];
            for(int i=0 ; i<count ; i++) {
                result[i] = entries[selection[i]];
            }
            return result;
        }
        
        static final DateFormat dateFormat = DateFormat.getDateInstance();
        static String SIZE_UNITS[] = {" MB", " KB", " B"};
        static long SIZE_FACTORS[] = {1024*1024, 1024, 1};

        private String formatFileSize(long size) {
            if(size <= 0) {
                return "0 B";
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

    class FileFiltersModel extends SimpleListModel<String> {
        private final ArrayList<NamedFileFilter> filters = new ArrayList<NamedFileFilter>();
        public NamedFileFilter getFileFilter(int index) {
            return filters.get(index);
        }
        public String getEntry(int index) {
            NamedFileFilter filter = getFileFilter(index);
            return filter.getDisplayName();
        }
        public int getNumEntries() {
            return filters.size();
        }
        public void addFileFilter(NamedFileFilter filter) {
            int index = filters.size();
            filters.add(filter);
            fireEntriesInserted(index, 1);
        }
        public void removeFileFilter(NamedFileFilter filter) {
            int idx = filters.indexOf(filter);
            if(idx >= 0) {
                filters.remove(idx);
                fireEntriesDeleted(idx, 1);
            }
        }
        private void removeAll() {
            filters.clear();
            fireAllChanged();
        }
    }

    private static class FileFilterWrapper implements FileFilter {
        private final FileFilter base;
        private final boolean showFolder;
        private final boolean showHidden;
        public FileFilterWrapper(FileFilter base, boolean showFolder, boolean showHidden) {
            this.base = base;
            this.showFolder = showFolder;
            this.showHidden = showHidden;
        }
        public boolean accept(FileSystemModel fsm, Object file) {
            if(showHidden || !fsm.isHidden(file)) {
                if(fsm.isFolder(file)) {
                    return showFolder;
                }
                return (base == null) || base.accept(fsm, file);
            }
            return false;
        }
    }

    static class PersistentState {
        private static final int MAGIC = 0x965A4D07;

        static class FileGroup {
            final String path;
            final String[] files;
            final long time;

            public FileGroup(String path, String[] files) {
                this.path = path;
                this.files = files;
                this.time = System.currentTimeMillis();
            }
            public FileGroup(DataInputStream dis) throws IOException {
                this.path = dis.readUTF();
                this.time = dis.readLong();
                int count = dis.readUnsignedByte();
                this.files = new String[count];
                for(int i=0 ; i<count ; ++i) {
                    files[i] = dis.readUTF();
                }
            }

            void encode(DataOutputStream dos) throws IOException {
                // ensure that count can fit into a byte, also the available space is limited
                int count = Math.min(100, files.length);
                dos.writeUTF(path);
                dos.writeLong(time);
                dos.writeByte((byte)count);
                for(int i=0 ; i<count ; i++) {
                    dos.writeUTF(files[i]);
                }
            }
        }

        static final int FLAG_SHOW_FOLDERS = 1;
        static final int FLAG_SHOW_HIDDEN  = 2;

        final FileGroup[] groups = new FileGroup[MAX_LRU_SIZE];
        int numGroups;
        int flags = FLAG_SHOW_FOLDERS;

        void setFlag(int flag, boolean value) {
            if(value) {
                flags |= flag;
            } else {
                flags &= ~flag;
            }
        }
        boolean getFlag(int flag) {
            return (flags & flag) == flag;
        }
        
        void addGroup(FileGroup g) {
            System.arraycopy(groups, 0, groups, 1, MAX_LRU_SIZE-1);
            groups[0] = g;
            numGroups = Math.min(numGroups+1, MAX_LRU_SIZE);
        }
        void removeGroup(int idx) {
            assert idx < numGroups;
            System.arraycopy(groups, idx+1, groups, idx, (MAX_LRU_SIZE-1)-idx);
            groups[MAX_LRU_SIZE-1] = null;
            numGroups--;
        }
        void moveGroupToFront(int idx) {
            assert idx < numGroups;
            FileGroup g = groups[idx];
            System.arraycopy(groups, 0, groups, 1, idx-1);
            groups[0] = g;
        }

        byte[] encode() throws IOException {
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(boas, new Deflater(9));
            DataOutputStream dos = new DataOutputStream(deflaterOutputStream);
            dos.writeInt(MAGIC);
            dos.writeShort((short)flags);
            dos.writeByte(numGroups);
            for(int i=0 ; i<numGroups ; ++i) {
                groups[i].encode(dos);
            }
            dos.close();
            return boas.toByteArray();
        }
        
        void decode(byte[] arr) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(arr);
            InflaterInputStream iis = new InflaterInputStream(bais);
            DataInputStream dis = new DataInputStream(iis);
            if(dis.readInt() != MAGIC) {
                throw new IOException("Invalid MAGIC");
            }
            flags = dis.readUnsignedShort();
            numGroups = 0;
            int count = Math.min(MAX_LRU_SIZE, dis.readUnsignedByte());
            for(int i=0 ; i<count ; i++) {
                groups[i] = new FileGroup(dis);
                numGroups = i+1;  // number of groups sucessfully parsed
            }
        }
    }

    class PersistentStateButtonModel extends ToggleButtonModel {
        final int flag;

        PersistentStateButtonModel(int flag) {
            this.flag = flag;
        }
        @Override
        public boolean isSelected() {
            return persistentState.getFlag(flag);
        }
        @Override
        public void setSelected(boolean selected) {
            if(selected != isSelected()) {
                persistentState.setFlag(flag, selected);
                savePersistentState();
                fireStateCallback();
            }
        }
    }

    static class FolderListModel extends SimpleListModel<String> {
        final PersistentState persistentState;
        public FolderListModel(PersistentState persistentState) {
            this.persistentState = persistentState;
        }
        public String getEntry(int index) {
            return persistentState.groups[index].path;
        }
        public int getNumEntries() {
            return persistentState.numGroups;
        }
    }
}
