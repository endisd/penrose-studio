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
package org.safehaus.penrose.studio.log.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.window.Window;
import org.safehaus.penrose.log.log4j.LoggerConfig;
import org.safehaus.penrose.studio.editor.EditorPage;
import org.safehaus.penrose.studio.log.wizard.LoggerPropertiesWizard;

public class LoggerPropertiesEditorPage extends EditorPage {

    Label nameText;
    Label levelText;
    Button additivityCheckbox;

    LoggerEditor editor;
    LoggerConfig loggerConfig;

    public LoggerPropertiesEditorPage(LoggerEditor editor) {
        super(editor, "PROPERTIES", "Logger Editor", "  Properties  ");

        this.editor = editor;
        this.loggerConfig = editor.getLoggerConfig();
    }

    public void init() throws Exception {

        Composite body = getManagedForm().getForm().getBody();
        body.setLayout(new GridLayout());

        Section propertiesSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        propertiesSection.setText("Properties");
        propertiesSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite propertiesComponent = createPropertiesControl(propertiesSection);
        propertiesSection.setClient(propertiesComponent);
	}

    public Composite createPropertiesControl(final Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(2, false));

        Composite leftControl = createPropertiesLeftControl(composite);
        leftControl.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite rightControl = createPropertiesRightControl(composite);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 100;
        rightControl.setLayoutData(gd);

        return composite;
    }

    public Composite createPropertiesLeftControl(final Composite parent) {

		Composite composite = toolkit.createComposite(parent);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

		Label nameLabel = toolkit.createLabel(composite, "Name:");
        GridData gd = new GridData(GridData.FILL);
        gd.widthHint = 100;
        nameLabel.setLayoutData(gd);

		nameText = toolkit.createLabel(composite, "", SWT.NONE);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		toolkit.createLabel(composite, "Level:");

		levelText = toolkit.createLabel(composite, "", SWT.NONE);
        levelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolkit.createLabel(composite, "Additivity:");

        additivityCheckbox = toolkit.createButton(composite, "", SWT.CHECK);
        additivityCheckbox.setEnabled(false);

        return composite;
    }

    public Composite createPropertiesRightControl(final Composite parent) {

        Composite composite = toolkit.createComposite(parent);

        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        Button editButton = new Button(composite, SWT.PUSH);
		editButton.setText("Edit");
        editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        editButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    LoggerPropertiesWizard wizard = new LoggerPropertiesWizard();
                    wizard.setLoggerConfig(loggerConfig);

                    WizardDialog dialog = new WizardDialog(getSite().getShell(), wizard);
                    dialog.setPageSize(600, 300);
                    int rc = dialog.open();

                    if (rc == Window.CANCEL) return;

                    editor.store();
                    refresh();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });

        return composite;
    }

    public void refresh() {

        String name = loggerConfig.getName();
        nameText.setText(name == null ? "" : name);

        String level = loggerConfig.getLevel();
        levelText.setText(level == null ? "" : level);

        additivityCheckbox.setSelection(loggerConfig.getAdditivity());
    }
}