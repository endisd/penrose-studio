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
package org.safehaus.penrose.studio.jdbc.connection;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.safehaus.penrose.partition.*;
import org.safehaus.penrose.studio.source.wizard.SourceWizardPage;
import org.safehaus.penrose.studio.jdbc.source.JDBCPrimaryKeyWizardPage;
import org.safehaus.penrose.studio.jdbc.source.JDBCFieldWizardPage;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.jdbc.JDBCClient;
import org.safehaus.penrose.source.SourceConfig;
import org.safehaus.penrose.source.FieldConfig;
import org.safehaus.penrose.source.TableConfig;
import org.safehaus.penrose.source.SourceConfigs;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class JDBCSourceWizard extends Wizard {

    Logger log = Logger.getLogger(getClass());

    private Project project;
    private PartitionConfig partitionConfig;
    private ConnectionConfig connectionConfig;
    private TableConfig tableConfig;
    private SourceConfig sourceConfig;

    public SourceWizardPage propertyPage;
    public JDBCFieldWizardPage fieldsPage;
    public JDBCPrimaryKeyWizardPage primaryKeyPage = new JDBCPrimaryKeyWizardPage();

    public JDBCSourceWizard(PartitionConfig partition, ConnectionConfig connectionConfig, TableConfig tableConfig) {
        this.partitionConfig = partition;
        this.connectionConfig = connectionConfig;
        this.tableConfig = tableConfig;

        propertyPage = new SourceWizardPage(tableConfig.getName().toLowerCase());
        fieldsPage = new JDBCFieldWizardPage();

        setWindowTitle(connectionConfig.getName()+" - New Source");
    }

    public boolean canFinish() {
        if (!propertyPage.isPageComplete()) return false;
        if (!fieldsPage.isPageComplete()) return false;
        if (!primaryKeyPage.isPageComplete()) return false;

        return true;
    }

    public boolean performFinish() {
        try {
            sourceConfig = new SourceConfig();
            sourceConfig.setName(propertyPage.getSourceName());
            sourceConfig.setConnectionName(connectionConfig.getName());

            String catalog = tableConfig.getCatalog();
            String schema = tableConfig.getSchema();
            String table = tableConfig.getName();

            sourceConfig.setParameter(JDBCClient.CATALOG, catalog);
            sourceConfig.setParameter(JDBCClient.SCHEMA, schema);
            sourceConfig.setParameter(JDBCClient.TABLE, table);

            String filter = fieldsPage.getFilter();
            if (filter != null) {
                sourceConfig.setParameter(JDBCClient.FILTER, filter);
            }

            System.out.println("Saving fields :");
            Collection<FieldConfig> fields = primaryKeyPage.getFields();
            if (fields.isEmpty()) {
                fields = fieldsPage.getSelectedFieldConfigs();
            }

            for (FieldConfig field : fields) {
                System.out.println(" - " + field.getName() + " " + field.isPrimaryKey());
                sourceConfig.addFieldConfig(field);
            }

            SourceConfigs sourceConfigs = partitionConfig.getSourceConfigs();
            sourceConfigs.addSourceConfig(sourceConfig);
            project.save(partitionConfig, sourceConfigs);

            return true;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public SourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(SourceConfig connection) {
        this.sourceConfig = connection;
    }

    public void addPages() {
        addPage(propertyPage);
        addPage(fieldsPage);
        addPage(primaryKeyPage);
    }

    public IWizardPage getNextPage(IWizardPage page) {
        if (propertyPage == page) {
            fieldsPage.setTableConfig(connectionConfig, tableConfig);

        } else if (fieldsPage == page) {
            Collection<FieldConfig> selectedFields = fieldsPage.getSelectedFieldConfigs();
            primaryKeyPage.setFieldConfigs(selectedFields);
        }

        return super.getNextPage(page);
    }

    public boolean needsPreviousAndNextButtons() {
        return true;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public PartitionConfig getPartitionConfig() {
        return partitionConfig;
    }

    public void setPartitionConfig(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}