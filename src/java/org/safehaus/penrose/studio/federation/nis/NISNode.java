package org.safehaus.penrose.studio.federation.nis;

import org.safehaus.penrose.studio.tree.Node;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.PenroseStudioPlugin;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.federation.FederationDomainNode;
import org.safehaus.penrose.federation.NISFederationClient;
import org.safehaus.penrose.federation.*;
import org.safehaus.penrose.studio.federation.nis.editor.NISEditorInput;
import org.safehaus.penrose.studio.federation.nis.editor.NISEditor;
import org.safehaus.penrose.studio.federation.nis.ownership.NISOwnershipNode;
import org.safehaus.penrose.studio.federation.nis.linking.NISLinkingNode;
import org.safehaus.penrose.studio.federation.nis.conflict.NISConflictsNode;
import org.safehaus.penrose.studio.federation.nis.domain.NISDomainNode;
import org.safehaus.penrose.studio.federation.nis.wizard.AddNISDomainWizard;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchActionConstants;

import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Endi S. Dewata
 */
public class NISNode extends Node {

    private FederationDomainNode federationDomainNode;

    Project project;
    NISFederationClient nisFederation;

    NISLinkingNode linkingNode;
    NISConflictsNode conflictsNode;
    NISOwnershipNode ownershipNode;

    private Collection<Node> children = new ArrayList<Node>();

    public NISNode(String name, FederationDomainNode federationDomainNode) throws Exception {
        super(name, PenroseStudioPlugin.getImage(PenroseImage.FOLDER), null, federationDomainNode);

        this.federationDomainNode = federationDomainNode;

        project = federationDomainNode.getProject();
        nisFederation = new NISFederationClient(federationDomainNode.getFederationClient());

        refresh();
    }

    public void refresh() throws Exception {

        log.debug("Refreshing repository types:");

        children.clear();

        for (FederationRepositoryConfig repository : nisFederation.getRepositories()) {
            NISDomainNode node = new NISDomainNode(
                    repository.getName(),
                    repository,
                    this
            );
            children.add(node);
        }
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

        manager.add(new Action("New NIS Repository...") {
            public void run() {
                try {
                    addNisDomain();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(new Action("Refresh") {
            public void run() {
                try {
                    refresh();

                    PenroseStudio penroseStudio = PenroseStudio.getInstance();
                    penroseStudio.notifyChangeListeners();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    public void open() throws Exception {

        NISEditorInput ei = new NISEditorInput();
        ei.setProject(project);
        ei.setNISFederation(nisFederation);

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        page.openEditor(ei, NISEditor.class.getName());

        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public void addNisDomain() throws Exception {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        AddNISDomainWizard wizard = new AddNISDomainWizard();
        WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
        dialog.setPageSize(600, 300);

        if (dialog.open() == Window.CANCEL) return;

        FederationRepositoryConfig domain = wizard.getRepository();

        nisFederation.addRepository(domain);
        nisFederation.createPartitions(domain.getName());

        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public boolean hasChildren() throws Exception {
        return !children.isEmpty();
    }

    public Collection<Node> getChildren() throws Exception {
        return children;
    }

    public NISFederationClient getNisFederation() {
        return nisFederation;
    }

    public void setNisTool(NISFederationClient nisFederation) {
        this.nisFederation = nisFederation;
    }

    public Project getProject() {
        return project;
    }

    public FederationDomainNode getFederationNode() {
        return federationDomainNode;
    }

    public void setFederationNode(FederationDomainNode federationDomainNode) {
        this.federationDomainNode = federationDomainNode;
    }
}
