package org.safehaus.penrose.studio.federation.nis.ownership;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.safehaus.penrose.federation.NISDomain;
import org.safehaus.penrose.federation.NISFederationClient;
import org.safehaus.penrose.studio.federation.nis.editor.NISHostsPage;
import org.safehaus.penrose.studio.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NISFilesEditor extends FormEditor {

    public Logger log = LoggerFactory.getLogger(getClass());

    Project project;
    NISFederationClient nisFederation;
    NISDomain domain;

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        NISFilesEditorInput ei = (NISFilesEditorInput)input;
        project = ei.getProject();
        nisFederation = ei.getNisTool();
        domain = ei.getDomain();

        setSite(site);
        setInput(input);
        setPartName(ei.getName());
    }

    public void addPages() {
        try {
            addPage(new NISHostsPage(this));
            addPage(new NISFilesPage(this));
            addPage(new NISScriptsPage(this));

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void doSave(IProgressMonitor iProgressMonitor) {
    }

    public void doSaveAs() {
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public NISDomain getDomain() {
        return domain;
    }

    public void setDomain(NISDomain domain) {
        this.domain = domain;
    }

    public NISFederationClient getNisTool() {
        return nisFederation;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
