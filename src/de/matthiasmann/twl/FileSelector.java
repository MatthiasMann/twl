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
import de.matthiasmann.twl.model.BitfieldBooleanModel;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.model.FileSystemTreeModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.LRUListModel;
import de.matthiasmann.twl.model.PersistentIntegerModel;
import de.matthiasmann.twl.model.PersistentLRUListModel;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twl.model.SimpleLRUListModel;
import de.matthiasmann.twl.model.SimpleListModel;
import de.matthiasmann.twl.model.ToggleButtonModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.prefs.Preferences;

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

    private static final int MAX_LRU_SIZE = 10;

    private final IntegerModel flags;
    private final LRUListModel<String> folderLRU;
    private final LRUListModel<FileGroup> filesLRU;

    private final TreeComboBox currentFolder;
    private final FileTable fileTable;
    private final Button btnUp;
    private final Button btnFolderLRU;
    private final Button btnOk;
    private final Button btnCancel;
    private final Button btnRefresh;
    private final Button btnShowFolders;
    private final Button btnShowHidden;
    private final ComboBox fileFilterBox;
    private final FileFiltersModel fileFiltersModel;

    private boolean allowFolderSelection;
    private Callback[] callbacks;
    private NamedFileFilter activeFileFilter;

    private FileSystemModel fsm;
    private FileSystemTreeModel model;

    /**
     * Create a FileSelector without persistent state
     */
    public FileSelector() {
        this(null, null);
    }

    public FileSelector(Preferences prefs, String prefsKey) {
        if((prefs == null) != (prefsKey == null)) {
            throw new IllegalArgumentException("'prefs' and 'prefsKey' must both be valid or both null");
        }

        if(prefs != null) {
            flags     = new PersistentIntegerModel(prefs, prefsKey.concat("_Flags"), 0, 0xFFFF, 0);
            folderLRU = new PersistentLRUListModel<String>(10, String.class, prefs, prefsKey.concat("_foldersLRU"));
            filesLRU  = new PersistentLRUListModel<FileGroup>(10, FileGroup.class, prefs, prefsKey.concat("_filesLRU"));
        } else {
            flags     = new SimpleIntegerModel(0, 0xFFFF, 0);
            folderLRU = new SimpleLRUListModel<String>(10);
            filesLRU  = new SimpleLRUListModel<FileGroup>(10);
        }

        currentFolder = new TreeComboBox();
        currentFolder.setTheme("currentFolder");
        fileTable = new FileTable();
        fileTable.setTheme("fileTable");
        fileTable.addCallback(new FileTable.Callback() {
            public void selectionChanged() {
                FileSelector.this.selectionChanged();
            }
            public void sortingChanged() {
            }
        });

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
                acceptSelection();
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
                acceptSelection();
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
                fileTable.setShowFolders(btnShowFolders.getModel().isSelected());
                fileTable.setShowHidden(btnShowHidden.getModel().isSelected());
                refreshFileTable();
            }
        };

        btnRefresh = new Button();
        btnRefresh.setTheme("buttonRefresh");
        btnRefresh.addCallback(showBtnCallback);
        
        btnShowFolders = new Button(new ToggleButtonModel(new BitfieldBooleanModel(flags, 0), true));
        btnShowFolders.setTheme("buttonShowFolders");
        btnShowFolders.addCallback(showBtnCallback);

        btnShowHidden = new Button(new ToggleButtonModel(new BitfieldBooleanModel(flags, 1), false));
        btnShowHidden.setTheme("buttonShowHidden");
        btnShowHidden.addCallback(showBtnCallback);

        add(labelCurrentFolder);
        add(currentFolder);
        if(btnFolderLRU != null) {
            add(btnFolderLRU);
        }
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
        addActionMapping("acceptSelection", "acceptSelection");
    }

    public FileSystemModel getFileSystemModel() {
        return fsm;
    }

    public void setFileSystemModel(FileSystemModel fsm) {
        this.fsm = fsm;
        if(fsm == null) {
            model = null;
            currentFolder.setModel(null);
            fileTable.setCurrentFolder(null, null);
        } else {
            model = new FileSystemTreeModel(fsm);
            currentFolder.setModel(model);
            currentFolder.setSeparator(fsm.getSeparator());
            setCurrentNode(model);
            if(folderLRU.getNumEntries() > 0) {
                gotoFolderFromLRU(0);
            }
        }
    }

    public boolean getAllowMultiSelection() {
        return fileTable.getAllowMultiSelection();
    }

    public void setAllowMultiSelection(boolean allowMultiSelection) {
        fileTable.setAllowMultiSelection(allowMultiSelection);
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
            setFileFilter(AllFilesFilter);
        }
    }

    public void removeAllFileFilters() {
        fileFiltersModel.removeAll();
        fileFilterBox.setVisible(false);
        setFileFilter(AllFilesFilter);
    }

    public void setFileFilter(NamedFileFilter filter) {
        if(filter == null) {
            throw new NullPointerException("filter");
        }
        activeFileFilter = filter;
        fileTable.setFileFilter(filter.getFileFilter());
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

    public void goOneLevelUp() {
        TreeTableNode node = currentFolder.getCurrentNode();
        TreeTableNode parent = node.getParent();
        if(parent != null) {
            setCurrentNode(parent);
        }
    }

    public void acceptSelection() {
        FileTable.Entry[] selection = fileTable.getSelection();
        if(selection.length == 1) {
            FileTable.Entry entry = selection[0];
            if(entry != null && entry.isFolder) {
                setCurrentFolder(entry.obj);
                return;
            }
        }
        if(callbacks != null) {
            Object[] objects = new Object[selection.length];
            for(int i=0 ; i<selection.length ; i++) {
                FileTable.Entry e = selection[i];
                if(e.isFolder && !allowFolderSelection) {
                    return;
                }
                objects[i] = e.obj;
            }
            addToLRU(selection);
            for(Callback cb : callbacks) {
                cb.filesSelected(objects);
            }
        }
    }

    void selectionChanged() {
        boolean foldersSelected = false;
        boolean filesSelected = false;
        for(FileTable.Entry entry : fileTable.getSelection()) {
            if(entry.isFolder) {
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
        fileTable.setCurrentFolder(fsm, getCurrentFolder());
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
        final ListBox listBox = new ListBox(new FolderListModel(folderLRU));
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
                            gotoFolderFromLRU(idx);
                        }
                    }
                }
            });
        }
    }

    private void addToLRU(FileTable.Entry[] selection) {
        String[] files = new String[selection.length];
        for(int i=0 ; i<files.length ; i++) {
            files[i] = selection[i].name;
        }
        FileGroup group = new FileGroup();
        group.setPath(fsm.getPath(getCurrentFolder()));
        group.setFiles(files);
        group.setTime(System.currentTimeMillis());

        filesLRU.addEntry(group);
        folderLRU.addEntry(group.getPath());
    }

    void gotoFolderFromLRU(int idx) {
        String path = folderLRU.getEntry(idx);
        try {
            TreeTableNode node = resolvePath(path);
            setCurrentNode(node);
        } catch(IllegalArgumentException ex) {
            folderLRU.removeEntry(idx);
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

    public static class FileGroup implements Serializable {
        String path;
        String[] files;
        long time;

        public String[] getFiles() {
            return files;
        }
        public void setFiles(String[] files) {
            this.files = files;
        }
        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public long getTime() {
            return time;
        }
        public void setTime(long time) {
            this.time = time;
        }
    }

    static class FolderListModel extends SimpleListModel<String> {
        final LRUListModel<String> lru;
        public FolderListModel(LRUListModel<String> lru) {
            this.lru = lru;
        }
        public String getEntry(int index) {
            return lru.getEntry(index);
        }
        public int getNumEntries() {
            return lru.getNumEntries();
        }
    }
}
