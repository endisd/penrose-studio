package org.safehaus.penrose.studio.federation.global.editor;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.safehaus.penrose.federation.GlobalRepository;
import org.safehaus.penrose.federation.FederationClient;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.studio.project.Project;

/**
 * @author Endi S. Dewata
 */
public class GlobalRepositoryPartitionsPage extends FormPage {

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    GlobalEditor editor;

    Label suffixText;
    Label templateText;

    FederationClient federation;
    Project project;

    public GlobalRepositoryPartitionsPage(GlobalEditor editor) {
        super(editor, "PARTITIONS", "  Partitions  ");

        this.editor = editor;
        this.federation = editor.federation;
        this.project = editor.project;
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("Global Repository");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section partitionSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        partitionSection.setText("Partition");
        partitionSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control partitionControl = createPartitionControl(partitionSection);
        partitionSection.setClient(partitionControl);

        refresh();
    }

    public Composite createPartitionControl(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Composite left = createPartitionLeftPanel(composite);
        left.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite right = createPartitionRightPanel(composite);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 100;
        right.setLayoutData(gd);

        return composite;
    }

    public Composite createPartitionLeftPanel(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Label suffixLabel = toolkit.createLabel(composite, "Suffix:");
        GridData gd = new GridData();
        gd.widthHint = 100;
        suffixLabel.setLayoutData(gd);

        suffixText = toolkit.createLabel(composite, "");
        suffixText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolkit.createLabel(composite, "Template:");

        templateText = toolkit.createLabel(composite, "");
        templateText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return composite;
    }

    public Composite createPartitionRightPanel(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Button createButton = toolkit.createButton(composite, "Create", SWT.PUSH);
        createButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Creating Partition",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    federation.createPartitions(FederationClient.GLOBAL);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }
            }
        });

        Button removeButton = toolkit.createButton(composite, "Remove", SWT.PUSH);
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Removing Partition",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    federation.removePartitions(FederationClient.GLOBAL);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }
            }
        });

        return composite;
    }

    public void refresh() {
        try {
            GlobalRepository globalRepository = federation.getGlobalRepository();
            if (globalRepository == null) return;

            String suffix = globalRepository.getParameter(GlobalRepository.SUFFIX);
            suffixText.setText(suffix == null ? "" : suffix);

            String template = globalRepository.getParameter(GlobalRepository.TEMPLATE);
            templateText.setText(template == null ? "" : template);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
        }
    }

}