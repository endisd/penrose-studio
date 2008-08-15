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
package org.safehaus.penrose.studio.directory.wizard;

import org.apache.log4j.Logger;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.safehaus.penrose.acl.ACI;
import org.safehaus.penrose.directory.EntryConfig;
import org.safehaus.penrose.ldap.DN;
import org.safehaus.penrose.ldap.RDN;
import org.safehaus.penrose.management.PenroseClient;
import org.safehaus.penrose.partition.PartitionClient;
import org.safehaus.penrose.partition.PartitionManagerClient;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.studio.mapping.wizard.AttributeValueWizardPage;
import org.safehaus.penrose.studio.directory.wizard.ObjectClassWizardPage;
import org.safehaus.penrose.studio.directory.wizard.StaticEntryDNWizardPage;
import org.safehaus.penrose.studio.project.Project;

import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class RootEntryWizard extends Wizard {

    Logger log = Logger.getLogger(getClass());

    private Project project;
    private String partitionName;
    private EntryConfig entryConfig;

    public StaticEntryDNWizardPage dnPage;
    public ObjectClassWizardPage ocPage;
    public AttributeValueWizardPage attrPage;

    public RootEntryWizard(Project project, String partitionName) {
        this.project = project;
        this.partitionName = partitionName;

        dnPage = new StaticEntryDNWizardPage();
        ocPage = new ObjectClassWizardPage(project);
        attrPage = new AttributeValueWizardPage(project, partitionName);
        
        setWindowTitle("Adding root entry");
    }

    public boolean canFinish() {
        if (!dnPage.isPageComplete()) return false;
        if (!ocPage.isPageComplete()) return false;
        if (!attrPage.isPageComplete()) return false;
        return true;
    }

    public void addPages() {
        addPage(dnPage);
        addPage(ocPage);
        addPage(attrPage);
    }

    public IWizardPage getNextPage(IWizardPage page) {
        try {
            if (ocPage == page) {
                Collection objectClasses = ocPage.getSelectedObjectClasses();
                attrPage.setObjectClasses(objectClasses);

                if (!objectClasses.isEmpty()) {
                    DN dn = new DN(dnPage.getDn());
                    RDN rdn = dn.getRdn();
                    attrPage.setRdn(rdn);
                }
            }

            return super.getNextPage(page);
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean performFinish() {
        try {
            entryConfig = new EntryConfig();
            entryConfig.setDn(dnPage.getDn());
            entryConfig.setEntryClass(dnPage.getClassName());
            entryConfig.addObjectClasses(ocPage.getSelectedObjectClasses());
            entryConfig.addAttributeConfigs(attrPage.getAttributeMappings());

            entryConfig.addACI(new ACI("rs"));
/*
            DirectoryConfig directoryConfig = partitionConfig.getDirectoryConfig();
            directoryConfig.addEntryConfig(entryConfig);
            project.save(partitionConfig, directoryConfig);
*/
            PenroseClient client = project.getClient();
            PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
            PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partitionName);
            partitionClient.createEntry(entryConfig);
            partitionClient.store();
            
            return true;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
            return false;
        }
    }

    public boolean needsPreviousAndNextButtons() {
        return true;
    }

    public EntryConfig getEntryConfig() {
        return entryConfig;
    }

    public void setEntryConfig(EntryConfig entryConfig) {
        this.entryConfig = entryConfig;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
