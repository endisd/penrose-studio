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
package org.safehaus.penrose.studio.properties;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.client.PenroseClient;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.project.Project;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Endi S. Dewata
 */
public class SystemPropertiesEditor extends MultiPageEditorPart {

    Logger log = Logger.getLogger(getClass());

    boolean dirty;

    Project project;

    Map<String,String> origProperties;
    Map<String,String> properties = new TreeMap<String,String>();

    SystemPropertiesPage propertyPage;

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        try {
            SystemPropertiesEditorInput ei = (SystemPropertiesEditorInput)input;
            project = ei.getProject();

            setSite(site);
            setInput(input);
            setPartName("System Properties");

            PenroseClient client = project.getClient();
            PenroseConfig penroseConfig = client.getPenroseConfig();
            
            origProperties = penroseConfig.getSystemProperties();
            properties.putAll(origProperties);

        } catch (Exception e) {
            throw new PartInitException(e.getMessage(), e);
        }
    }

    protected void createPages() {
        try {
            propertyPage = new SystemPropertiesPage(this);
            addPage(propertyPage.createControl());
            setPageText(0, "  Properties  ");

            load();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Composite getParent() {
        return getContainer();
    }

    public void dispose() {
        propertyPage.dispose();
        super.dispose();
    }

    public void load() throws Exception {
        propertyPage.load();
    }

    public void doSave(IProgressMonitor iProgressMonitor) {
        try {
            store();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void doSaveAs() {
    }

    public void store() throws Exception {

        origProperties.clear();
        origProperties.putAll(properties);
        
        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();

        checkDirty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void checkDirty() {
        try {
            dirty = false;

            if (!origProperties.equals(properties)) {
                dirty = true;
                return;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            firePropertyChange(PROP_DIRTY);
        }
    }
}
