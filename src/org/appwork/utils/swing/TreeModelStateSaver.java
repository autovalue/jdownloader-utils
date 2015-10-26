/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.swing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.JTree;
import javax.swing.tree.TreePath;



/**
 * This class is used to save the selection states and expanded states of each
 * tree node before rebuilding the tree. After rebuilding, the model is able to
 * restore these states.
 * 
 * @author $Author: unknown$
 * 
 */
public class TreeModelStateSaver {

    protected JTree                        tree;
    /**
     * Stores for each node the expanded state
     */
    private final HashMap<Object, Boolean> expandCache;

    /**
     * treePath for internal use
     */
    private TreePath                       treePath;

    /**
     * Stores all selected Paths
     */
    protected TreePath[]                   selectedPaths;

    /**
     * @param tree
     */
    public TreeModelStateSaver(final JTree tree) {
        this.tree = tree;
        this.expandCache = new HashMap<Object, Boolean>();
    }

    /**
     * @return the expandCache
     */
    public HashMap<Object, Boolean> getExpandCache() {
        return this.expandCache;
    }

    /**
     * @return the {@link TreeModelStateSaver#selectedPaths}
     * @see TreeModelStateSaver#selectedPaths
     */
    public TreePath[] getSelectedPaths() {
        return this.selectedPaths;
    }

    /**
     * Restore the saved tree state
     */
    public void restore() {

        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                try {
                    if (TreeModelStateSaver.this.tree.getModel() == null) {

                    return null; }
                    ;

                    TreeModelStateSaver.this.restoreState(TreeModelStateSaver.this.tree.getModel().getRoot(), new ArrayList<Object>());

                    final TreePath[] selectedPathes = TreeModelStateSaver.this.getSelectedPaths();
                    if (selectedPathes != null && selectedPathes.length > 0) {
                        TreeModelStateSaver.this.tree.getSelectionModel().clearSelection();
                        TreeModelStateSaver.this.tree.getSelectionModel().setSelectionPaths(selectedPathes);

                    }
                } catch (final Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
                return null;
            }

        }.start();
    }

    protected void restoreState(final Object node, final java.util.List<Object> path) {
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                if (node == null) { return null; }
                path.add(node);

                TreeModelStateSaver.this.treePath = new TreePath(path.toArray(new Object[] {}));
                final Boolean bo = TreeModelStateSaver.this.expandCache.get(node);
                try {
                    if (bo != null && bo.booleanValue()) {
                        TreeModelStateSaver.this.tree.expandPath(TreeModelStateSaver.this.treePath);
                    }
                } catch (final Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }

                for (int i = 0; i < TreeModelStateSaver.this.tree.getModel().getChildCount(node); i++) {
                    try {
                        TreeModelStateSaver.this.restoreState(TreeModelStateSaver.this.tree.getModel().getChild(node, i), new ArrayList<Object>(path));
                    } catch (final Throwable e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                }
                return null;
            }

        }.start();

    }

    /**
     * Save the current state of the tree
     */
    public void save() {
        if (this.tree.getModel() != null) {
            this.saveState(this.tree.getModel().getRoot(), new ArrayList<Object>());
        }
        this.selectedPaths = this.tree.getSelectionPaths();
    }

    /**
     * Saves the expaned states of each node to cacheMap runs rekursive
     * 
     * @param root
     */
    private void saveState(final Object node, final java.util.List<Object> path) {
        path.add(node);
        try {
            this.treePath = new TreePath(path.toArray(new Object[] {}));
            this.expandCache.put(node, this.tree.isExpanded(this.treePath));
        } catch (final Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);

        }

        final int max = this.tree.getModel().isLeaf(node) ? 0 : this.tree.getModel().getChildCount(node);
        for (int i = 0; i < max; i++) {
            try {
                this.saveState(this.tree.getModel().getChild(node, i), new ArrayList<Object>(path));
            } catch (final Exception e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            this.tree.getModel().getChildCount(node);
        }
    }

    /**
     * @param selectedPathes
     *            the selectedPathes to set
     */
    public void setSelectedPaths(final TreePath[] selectedPathes) {
        this.selectedPaths = selectedPathes;
    }

}
