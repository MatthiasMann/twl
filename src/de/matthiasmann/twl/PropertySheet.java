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

import de.matthiasmann.twl.TableBase.CellRenderer;
import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.PropertyList;
import de.matthiasmann.twl.model.SimplePropertyList;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.util.HashMap;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class PropertySheet extends TreeTable {

    public interface PropertyEditor {
        public Widget getWidget();
        public void valueChanged();
        public void preDestroy();
        public void setSelected(boolean selected);
    }

    public interface PropertyEditorFactory<T> {
        public PropertyEditor createEditor(Property<T> property);
    }

    private final SimplePropertyList rootList;
    private final TreeGenerator rootTreeGenerator;
    private final PropertyListCellRenderer subListRenderer;
    private final CellRenderer editorRenderer;
    private final HashMap<Class<?>, PropertyEditorFactory<?>> factories;

    public PropertySheet() {
        this(new Model());
    }

    private PropertySheet(Model model) {
        super(model);
        this.rootList = new SimplePropertyList("<root>");
        this.subListRenderer = new PropertyListCellRenderer();
        this.editorRenderer = new EditorRenderer();
        this.factories = new HashMap<Class<?>, PropertyEditorFactory<?>>();
        this.rootTreeGenerator = new TreeGenerator(rootList, model);
        rootList.addValueChangedCallback(rootTreeGenerator);
        registerPropertyEditorFactory(String.class, new StringEditorFactory());
        setSelectionManager(new TableRowSelectionManager(new TableSingleSelectionModel()));
    }

    public SimplePropertyList getPropertyList() {
        return rootList;
    }

    public<T> void registerPropertyEditorFactory(Class<T> clazz, PropertyEditorFactory<T> factory) {
        if(clazz == null) {
            throw new NullPointerException("clazz");
        }
        if(factory == null) {
            throw new NullPointerException("factory");
        }
        factories.put(clazz, factory);
    }

    @Override
    public void setModel(TreeTableModel model) {
        if(model instanceof Model) {
            super.setModel(model);
        } else {
            throw new UnsupportedOperationException("Do not call this method");
        }
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        applyThemePropertiesSheet(themeInfo);
    }

    protected void applyThemePropertiesSheet(ThemeInfo themeInfo) {
        applyCellRendererTheme(subListRenderer);
        applyCellRendererTheme(editorRenderer);
    }

    @Override
    protected CellRenderer getCellRenderer(int row, int col, TreeTableNode node) {
        if(node == null) {
            node = getNodeFromRow(row);
        }
        if(node instanceof ListNode) {
            if(col == 0) {
                PropertyListCellRenderer cr = subListRenderer;
                NodeState nodeState = getOrCreateNodeState(node);
                cr.setCellData(row, col, node.getData(col), nodeState);
                return cr;
            } else {
                return null;
            }
        } else if(col == 0) {
            return super.getCellRenderer(row, col, node);
        } else {
            CellRenderer cr = editorRenderer;
            cr.setCellData(row, col, node.getData(col));
            return cr;
        }
    }

    TreeTableNode createNode(TreeTableNode parent, Property<?> property) {
        if(property.getType() == PropertyList.class) {
            return new ListNode(parent, property);
        } else {
            PropertyEditorFactory factory = factories.get(property.getType());
            if(factory != null) {
                @SuppressWarnings("unchecked")
                PropertyEditor editor = factory.createEditor(property);
                if(editor != null) {
                    return new LeafNode(parent, property, editor);
                }
            }
            return null;
        }
    }

    interface PSTreeTableNode extends TreeTableNode {
        public void addChild(TreeTableNode parent);
        public void removeAllChildren();
    }

    static abstract class PropertyNode extends AbstractTreeTableNode implements Runnable, PSTreeTableNode {
        protected final Property<?> property;
        public PropertyNode(TreeTableNode parent, Property<?> property) {
            super(parent);
            this.property = property;
            property.addValueChangedCallback(this);
        }
        protected void removeCallback() {
            property.removeValueChangedCallback(this);
        }
        @Override
        public void removeAllChildren() {
            super.removeAllChildren();
        }
        public void addChild(TreeTableNode parent) {
            insertChild(parent, getNumChildren());
        }
    }

    class TreeGenerator implements Runnable {
        private final PropertyList list;
        private final PSTreeTableNode parent;

        public TreeGenerator(PropertyList list, PSTreeTableNode parent) {
            this.list = list;
            this.parent = parent;
        }
        public void run() {
            parent.removeAllChildren();
            addSubProperties();
        }
        void removeChildCallbacks(PSTreeTableNode parent) {
            for(int i=0,n=parent.getNumChildren() ; i<n ; ++i) {
                ((PropertyNode)parent.getChild(i)).removeCallback();
            }
        }
        void addSubProperties() {
            for(int i=0 ; i<list.getNumProperties() ; ++i) {
                TreeTableNode node = createNode(parent, list.getProperty(i));
                if(node != null) {
                    parent.addChild(node);
                }
            }
        }
    }

    static class LeafNode extends PropertyNode {
        private final PropertyEditor editor;

        public LeafNode(TreeTableNode parent, Property<?> property, PropertyEditor editor) {
            super(parent, property);
            this.editor = editor;
            setLeaf(true);
        }
        public Object getData(int column) {
            switch(column) {
            case 0: return property.getName();
            case 1: return editor;
            default: return "???";
            }
        }
        public void run() {
            fireNodeChanged();
        }
    }

    class ListNode extends PropertyNode {
        protected final TreeGenerator treeGenerator;

        public ListNode(TreeTableNode parent, Property<?> property) {
            super(parent, property);
            this.treeGenerator = new TreeGenerator(
                    (PropertyList)property.getValue(), this);
            treeGenerator.run();
        }
        public Object getData(int column) {
            return property.getName();
        }
        public void run() {
            treeGenerator.run();
        }
        @Override
        protected void removeCallback() {
            super.removeCallback();
            treeGenerator.removeChildCallbacks(this);
        }
    }

    class PropertyListCellRenderer extends TreeNodeCellRenderer {
        private final Widget bgRenderer;
        private final Label textRenderer;

        public PropertyListCellRenderer() {
            bgRenderer = new Widget();
            textRenderer = new Label(bgRenderer.getAnimationState());
            bgRenderer.add(textRenderer);
            bgRenderer.setTheme(getTheme());
        }
        @Override
        public int getColumnSpan() {
            return 2;
        }
        @Override
        public Widget getCellRenderWidget(int x, int y, int width, int height, boolean isSelected) {
            bgRenderer.setPosition(x, y);
            bgRenderer.setSize(width, height);
            int indent = getIndentation();
            textRenderer.setPosition(x + indent, y);
            textRenderer.setSize(Math.max(0, width-indent), height);
            bgRenderer.getAnimationState().setAnimationState(STATE_SELECTED, isSelected);
            return bgRenderer;
        }
        @Override
        public void setCellData(int row, int column, Object data, NodeState nodeState) {
            super.setCellData(row, column, data, nodeState);
            textRenderer.setText((String)data);
        }
        @Override
        protected void setSubRenderer(Object colData) {
        }
    }

    static class EditorRenderer implements CellRenderer, TreeTable.CellWidgetCreator {
        private PropertyEditor editor;

        public void applyTheme(ThemeInfo themeInfo) {
        }
        public Widget getCellRenderWidget(int x, int y, int width, int height, boolean isSelected) {
            editor.setSelected(isSelected);
            return null;
        }
        public int getColumnSpan() {
            return 1;
        }
        public int getPreferredHeight() {
            return editor.getWidget().getPreferredHeight();
        }
        public String getTheme() {
            return "PropertyEditorCellRender";
        }
        public void setCellData(int row, int column, Object data) {
            editor = (PropertyEditor)data;
        }
        public Widget updateWidget(Widget existingWidget) {
            return editor.getWidget();
        }
        public void positionWidget(Widget widget, int x, int y, int w, int h) {
            widget.setPosition(x, y);
            widget.setSize(w, h);
        }
    }
    
    static class Model extends AbstractTreeTableModel implements PSTreeTableNode {
        public String getColumnHeaderText(int column) {
            switch(column) {
            case 0: return "Name";
            case 1: return "Value";
            default: return "???";
            }
        }
        public int getNumColumns() {
            return 2;
        }
        @Override
        public void removeAllChildren() {
            super.removeAllChildren();
        }
        public void addChild(TreeTableNode parent) {
            insertChild(parent, getNumChildren());
        }
    }

    static class StringEditor implements PropertyEditor, EditField.Callback {
        private final EditField editField;
        private final Property<String> property;

        public StringEditor(Property<String> property) {
            this.property = property;
            this.editField = new EditField();
            editField.addCallback(this);
            resetValue();
        }
        public Widget getWidget() {
            return editField;
        }
        public void valueChanged() {
            resetValue();
        }
        public void preDestroy() {
            editField.removeCallback(this);
        }
        public void setSelected(boolean selected) {
        }
        public void callback(int key) {
            if(key == Keyboard.KEY_ESCAPE) {
                resetValue();
            } else {
                try {
                    property.setValue(editField.getText());
                    editField.setErrorMessage(null);
                } catch (IllegalArgumentException ex) {
                    editField.setErrorMessage(ex.getMessage());
                }
            }
        }
        private void resetValue() {
            editField.setText(property.getValue());
        }
    }
    static class StringEditorFactory implements PropertyEditorFactory<String> {
        @SuppressWarnings("unchecked")
        public PropertyEditor createEditor(Property<String> property) {
            return new StringEditor(property);
        }
    }
}