/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twl.model;

/**
 *
 * @author mam
 */
public class FileSystemTreeModel extends AbstractTreeTableModel {

    private final FileSystemModel fsm;

    public FileSystemTreeModel(FileSystemModel fsm) {
        this.fsm = fsm;

        insertRoots();
    }

    public int getNumColumns() {
        return 1;
    }

    public String getColumnHeaderText(int column) {
        return "Folder";
    }

    public FileSystemModel getFileSystemModel() {
        return fsm;
    }

    public void insertRoots() {
        removeAllChildren();

        for(Object root : fsm.listRoots()) {
            insertChild(new FolderNode(this, fsm, root), getNumChildren());
        }
    }

    public FolderNode getNodeForFolder(Object obj) {
        Object parent = fsm.getParent(obj);
        TreeTableNode parentNode;
        if(parent == null) {
            parentNode = this;
        } else {
            parentNode = getNodeForFolder(parent);
        }
        if(parentNode != null) {
            for(int i=0 ; i<parentNode.getNumChildren() ; i++) {
                FolderNode node = (FolderNode)parentNode.getChild(i);
                if(fsm.equals(node.folder, obj)) {
                    return node;
                }
            }
        }
        return null;
    }

    static final FolderNode[] NO_CHILDREN = new FolderNode[0];
    
    public static class FolderNode implements TreeTableNode {
        private final TreeTableNode parent;
        private final FileSystemModel fsm;
        private final Object folder;
        FolderNode[] children;

        public FolderNode(TreeTableNode parent, FileSystemModel fsm, Object folder) {
            this.parent = parent;
            this.fsm = fsm;
            this.folder = folder;
        }

        public Object getFolder() {
            return folder;
        }

        public Object getData(int column) {
            return fsm.getName(folder);
        }

        public TreeTableNode getChild(int idx) {
            return children[idx];
        }

        public int getChildIndex(TreeTableNode child) {
            for(int i=0,n=children.length ; i<n ; i++) {
                if(children[i] == child) {
                    return i;
                }
            }
            return -1;
        }

        public int getNumChildren() {
            if(children == null) {
                collectChilds();
            }
            return children.length;
        }

        public TreeTableNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return false;
        }

        private void collectChilds() {
            children = NO_CHILDREN;
            try {
                Object[] subFolder = fsm.listFolder(folder, FolderFilter.instance);
                if(subFolder != null && subFolder.length > 0) {
                    FolderNode[] newChildren = new FolderNode[subFolder.length];
                    for(int i=0 ; i<subFolder.length ; i++) {
                        newChildren[i] = new FolderNode(this, fsm, subFolder[i]);
                    }
                    children = newChildren;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    static class FolderFilter implements FileSystemModel.FileFilter {
        static final FolderFilter instance = new FolderFilter();
        
        public boolean accept(FileSystemModel model, Object file) {
            return model.isFolder(file);
        }
    };
}
