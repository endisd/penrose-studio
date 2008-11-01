/**
 * Copyright (c) 2000-2006, Identyx Corporation.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.safehaus.penrose.studio.directory;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.safehaus.penrose.directory.EntryConfig;
import org.safehaus.penrose.directory.EntryClient;
import org.safehaus.penrose.directory.DirectoryClient;
import org.safehaus.penrose.partition.PartitionClient;
import org.safehaus.penrose.partition.PartitionManagerClient;
import org.safehaus.penrose.client.PenroseClient;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.PenroseStudioPlugin;
import org.safehaus.penrose.studio.directory.action.MapLDAPTreeAction;
import org.safehaus.penrose.studio.directory.action.NewDynamicEntryAction;
import org.safehaus.penrose.studio.directory.action.NewEntryFromSourceAction;
import org.safehaus.penrose.studio.directory.action.NewEntryAction;
import org.safehaus.penrose.studio.directory.editor.EntryEditorInput;
import org.safehaus.penrose.studio.directory.editor.EntryEditor;
import org.safehaus.penrose.studio.partition.PartitionNode;
import org.safehaus.penrose.studio.partition.PartitionsNode;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.project.ProjectNode;
import org.safehaus.penrose.studio.server.ServersView;
import org.safehaus.penrose.studio.tree.Node;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class EntryNode extends Node {

    Logger log = Logger.getLogger(getClass());

    protected ServersView view;
    protected ProjectNode projectNode;
    protected PartitionsNode partitionsNode;
    protected PartitionNode partitionNode;
    protected DirectoryNode directoryNode;

    private String partitionName;
    private EntryConfig entryConfig;

    public EntryNode(String name, Image image, Object object, Object parent) {
        super(name, image, object, parent);

        if (parent instanceof DirectoryNode) {
            directoryNode = (DirectoryNode)parent;
        } else if (parent instanceof EntryNode) {
            directoryNode = ((EntryNode)parent).getDirectoryNode();
        }

        partitionNode = directoryNode.getPartitionNode();
        partitionsNode = partitionNode.getPartitionsNode();
        projectNode = partitionsNode.getProjectNode();
        view = projectNode.getServersView();
    }

    public void showMenu(IMenuManager manager) throws Exception {

        manager.add(new Action("Open") {
            public void run() {
                try {
                    open();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        manager.add(new Action("Edit sources") {
            public void run() {
                try {
                    editSources();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        manager.add(new Action("Edit ACL") {
            public void run() {
                try {
                    editACL();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(new NewEntryAction(this));
        manager.add(new NewDynamicEntryAction(this));

        showCommercialMenu(manager);

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(new Action("Copy") {
            public void run() {
                try {
                    //copy(connection);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        manager.add(new Action("Paste") {
            public void run() {
                try {
                    //paste(connection);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        manager.add(new Action("Delete", PenroseStudioPlugin.getImageDescriptor(PenroseImage.SIZE_16x16, PenroseImage.DELETE)) {
            public void run() {
                try {
                    remove();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    public void showCommercialMenu(IMenuManager manager) throws Exception {

        //PenroseStudio penroseStudio = PenroseStudio.getInstance();
        //PenroseStudioWorkbenchAdvisor workbenchAdvisor = penroseStudio.getWorkbenchAdvisor();
        //PenroseStudioWorkbenchWindowAdvisor workbenchWindowAdvisor = workbenchAdvisor.getWorkbenchWindowAdvisor();
        //PenroseStudioActionBarAdvisor actionBarAdvisor = workbenchWindowAdvisor.getActionBarAdvisor();

        //if (actionBarAdvisor.getShowCommercialFeaturesAction().isChecked()) {
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            manager.add(new NewEntryFromSourceAction(this));
            manager.add(new MapLDAPTreeAction(this));
        //}

    }

    public void open() throws Exception {

        EntryEditorInput ei = new EntryEditorInput();
        ei.setPartitionName(partitionName);
        ei.setEntryId(entryConfig.getId());
        ei.setProject(projectNode.getProject());

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();

        page.openEditor(ei, EntryEditor.class.getName());
    }

    public void editSources() throws Exception {

        EntryEditorInput ei = new EntryEditorInput();
        ei.setPartitionName(partitionName);
        ei.setEntryId(entryConfig.getId());
        ei.setProject(projectNode.getProject());

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();

        EntryEditor editor = (EntryEditor)page.openEditor(ei, EntryEditor.class.getName());
        editor.showSourcesPage();
    }

    public void editACL() throws Exception {

        EntryEditorInput ei = new EntryEditorInput();
        ei.setPartitionName(partitionName);
        ei.setEntryId(entryConfig.getId());
        ei.setProject(projectNode.getProject());

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();

        EntryEditor editor = (EntryEditor)page.openEditor(ei, EntryEditor.class.getName());
        editor.showACLPage();
    }

    public void remove() throws Exception {

        boolean confirm = MessageDialog.openQuestion(
                view.getSite().getShell(),
                "Confirmation", "Remove selected entries?"
        );

        if (!confirm) return;

        Project project = projectNode.getProject();
        PenroseClient client = project.getClient();
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partitionName);
        DirectoryClient directoryClient = partitionClient.getDirectoryClient();

        //DirectoryConfig directoryConfig = partitionConfig.getDirectoryConfig();

        for (Node node : view.getSelectedNodes()) {
            if (!(node instanceof EntryNode)) continue;

            EntryNode entryNode = (EntryNode) node;

            String id = entryNode.getEntryConfig().getId();
            directoryClient.removeEntry(id);
        }

        //project.save(partitionConfig, directoryConfig);
        partitionClient.store();

        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public boolean hasChildren() throws Exception {
        return !getChildren().isEmpty();
    }

    public Collection<Node> getChildren() throws Exception {

        Collection<Node> children = new ArrayList<Node>();

        Project project = projectNode.getProject();
        PenroseClient client = project.getClient();
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partitionName);
        DirectoryClient directoryClient = partitionClient.getDirectoryClient();

        EntryClient entryClient = directoryClient.getEntryClient(entryConfig.getId());

        log.debug("Getting children:");

        for (String id : entryClient.getChildIds()) {
            log.debug(" - "+id);

            EntryClient childClient = directoryClient.getEntryClient(id);
            EntryConfig childConfig = childClient.getEntryConfig();

            //log.debug(" - childConfig "+childConfig);
            //log.debug(" - childConfig.rdn "+childConfig.getRdn());

            String rdn = childConfig.getRdn() == null ? "Root DSE" : childConfig.getRdn().toString();

            EntryNode entryNode = new EntryNode(
                    rdn,
                    PenroseStudioPlugin.getImage(PenroseImage.FOLDER),
                    childConfig,
                    this
            );

            entryNode.setPartitionName(partitionName);
            entryNode.setEntryConfig(childConfig);

            children.add(entryNode);
        }

        return children;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    public EntryConfig getEntryConfig() {
        return entryConfig;
    }

    public void setEntryConfig(EntryConfig entryConfig) {
        this.entryConfig = entryConfig;
    }

    public ServersView getView() {
        return view;
    }

    public void setView(ServersView view) {
        this.view = view;
    }

    public ProjectNode getProjectNode() {
        return projectNode;
    }

    public void setProjectNode(ProjectNode projectNode) {
        this.projectNode = projectNode;
    }

    public PartitionsNode getPartitionsNode() {
        return partitionsNode;
    }

    public void setPartitionsNode(PartitionsNode partitionsNode) {
        this.partitionsNode = partitionsNode;
    }

    public PartitionNode getPartitionNode() {
        return partitionNode;
    }

    public void setPartitionNode(PartitionNode partitionNode) {
        this.partitionNode = partitionNode;
    }

    public DirectoryNode getDirectoryNode() {
        return directoryNode;
    }

    public void setDirectoryNode(DirectoryNode directoryNode) {
        this.directoryNode = directoryNode;
    }
}
