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

import de.matthiasmann.twl.model.AbstractTableModel;
import de.matthiasmann.twl.model.DefaultTableSelectionModel;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemTreeModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.text.DateFormat;
import java.util.Date;

/**
 *
 * @author Matthias Mann
 */
public class FileSelector extends DialogLayout {

    private final TreeComboBox currentFolder;
    private final FileTableModel fileTableModel;
    private final DefaultTableSelectionModel fileTableSelectionModel;
    private final Table fileTable;

    private FileSystemModel fsm;
    private FileSystemTreeModel model;

    public FileSelector(FileSystemModel fsm) {
        this();
        setFileSystemModel(fsm);
    }

    public FileSelector() {
        currentFolder = new TreeComboBox();
        currentFolder.setTheme("currentFolder");
        fileTableModel = new FileTableModel();
        fileTable = new Table(fileTableModel);
        fileTable.setTheme("fileTable");

        Button btnUp = new Button();
        btnUp.setTheme("buttonUp");
        btnUp.addCallback(new Runnable() {
            public void run() {
                goOneLevelUp();
            }
        });

        Button btnOk = new Button();
        btnOk.setTheme("buttonOk");

        Button btnCancel = new Button();
        btnCancel.setTheme("buttonCancel");

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

        fileTableSelectionModel = new DefaultTableSelectionModel();
        fileTable.setSelectionManager(new TableRowSelectionManager(fileTableSelectionModel));
        fileTable.addCallback(new TableBase.Callback() {
            public void mouseDoubleClicked(int row, int column) {
                acceptSelection();
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

        setCanAcceptKeyboardFocus(true);
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
            fileTableModel.setData(null, EMPTY, EMPTY);
        } else {
            model = new FileSystemTreeModel(fsm);
            currentFolder.setModel(model);
            currentFolder.setSeparator(fsm.getSeparator());
            setCurrentNode(model);
        }
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

    public void acceptSelection() {
        int[] selection = fileTableSelectionModel.getSelection();
        if(selection.length == 1) {
            int row = selection[0];
            FileTableModel m = (FileTableModel)fileTable.getModel();
            Object file = m.getFile(row);
            if(file != null && fsm.isFolder(file)) {
                setCurrentFolder(file);
            }
        }
    }

    protected void setCurrentNode(TreeTableNode node) {
        currentFolder.setCurrentNode(node);
        Object[] files;
        if(node == model) {
            files = fsm.listRoots();
        } else {
            Object folder = getCurrentFolder();
            files = fsm.listFolder(folder, null);
        }
        Object[] folders;
        if(files == null) {
            files = folders = new Object[0];
        } else {
            folders = new Object[files.length];
            int numFolders = 0;
            int numFiles = 0;
            for(int i=0 ; i<files.length ; i++) {
                Object obj = files[i];
                if(fsm.isFolder(obj)) {
                    folders[numFolders++] = obj;
                } else {
                    files[numFiles++] = obj;
                }
            }
            folders = copyOf(folders, numFolders);
            files = copyOf(files, numFiles);
        }
        fileTableModel.setData(fsm, folders, files);
        fileTableSelectionModel.setAnchorIndex(0);
        fileTableSelectionModel.setLeadIndex(0);
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

    private Object[] copyOf(Object[] arr, int count) {
        Object[] tmp = new Object[count];
        System.arraycopy(arr, 0, tmp, 0, count);
        return tmp;
    }

    static Object[] EMPTY = new Object[0];

    static class FileTableModel extends AbstractTableModel {
        private FileSystemModel fsm;
        private Object[] folders = EMPTY;
        private Object[] files = EMPTY;

        public void setData(FileSystemModel fsm, Object[] folders, Object[] files) {
            fireRowsDeleted(0, getNumRows());
            this.fsm = fsm;
            this.folders = folders;
            this.files = files;
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
            boolean isFolder = row < folders.length;
            if(isFolder) {
                Object folder = folders[row];
                switch(column) {
                case 0: return "["+fsm.getName(folder)+"]";
                case 1: return "Folder";
                case 2: return "";
                case 3: return dateFormat.format(new Date(fsm.getLastModified(folder)));
                default: return "??";
                }
            } else {
                Object file = files[row - folders.length];
                switch(column) {
                case 0: return fsm.getName(file);
                case 1: return "File";
                case 2: return formatFileSize(fsm.getSize(file));
                case 3: return dateFormat.format(new Date(fsm.getLastModified(file)));
                default: return "??";
                }
            }
        }

        public int getNumRows() {
            return folders.length + files.length;
        }

        Object getFile(int row) {
            if(row < 0) {
                return null;
            } else if(row < folders.length) {
                return folders[row];
            } else {
                row -= folders.length;
                if(row < files.length) {
                    return files[row];
                } else {
                    return null;
                }
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
}
