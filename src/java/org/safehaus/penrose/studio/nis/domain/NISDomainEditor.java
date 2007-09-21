package org.safehaus.penrose.studio.nis.domain;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.safehaus.penrose.nis.NISDomain;
import org.safehaus.penrose.studio.nis.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class NISDomainEditor extends FormEditor {

    public Logger log = LoggerFactory.getLogger(getClass());

    NISTool nisTool;
    NISDomain domain;

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        NISDomainEditorInput ei = (NISDomainEditorInput)input;
        nisTool = ei.getNisTool();
        domain = ei.getDomain();

        setSite(site);
        setInput(input);
        setPartName("NIS - "+domain.getName());
    }

    public void addPages() {
        try {
            addPage(new NISDomainMainPage(this));
            addPage(new NISDomainDatabasePage(this));
            addPage(new NISDomainChangeLogPage(this));
            addPage(new NISDomainTablesPage(this));
            addPage(new NISDomainLDAPPage(this));
            addPage(new NISDomainTrackerPage(this));
            addPage(new NISDomainErrorsPage(this));

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

    public NISTool getNisTool() {
        return nisTool;
    }
}
