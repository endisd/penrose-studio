package org.safehaus.penrose.studio.federation.nis.consolidation;

import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.apache.log4j.Logger;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.PartitionConfig;
import org.safehaus.penrose.studio.federation.nis.NISDomain;
import org.safehaus.penrose.studio.federation.nis.NISFederation;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.management.PenroseClient;

/**
 * @author Endi Sukma Dewata
 */
public class NISConsolidationPage extends FormPage {

    public Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    NISConsolidationEditor editor;
    NISDomain domain;
    NISFederation nisFederation;

    private Project project;
    private Partition partition;

    public NISConsolidationPage(NISConsolidationEditor editor) throws Exception {
        super(editor, "PARTITION", "  Partition  ");

        this.editor = editor;
        this.partition = editor.getPartition();
        this.domain = editor.getDomain();

        this.nisFederation = editor.getNisFederation();

        project = nisFederation.getProject();
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("Partition");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section section = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Partition");
        section.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control control = createControl(section);
        section.setClient(control);
    }

    public Composite createControl(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Composite left = createLeftPanel(composite);
        left.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite right = createRightPanel(composite);
        right.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        return composite;
    }

    public Composite createLeftPanel(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Label nssSuffixLabel = toolkit.createLabel(composite, "NSS Suffix:");
        GridData gd = new GridData();
        gd.widthHint = 100;
        nssSuffixLabel.setLayoutData(gd);

        Label nssSuffixText = toolkit.createLabel(composite, domain.getNssSuffix());
        nssSuffixText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return composite;
    }

    public Composite createRightPanel(Composite parent) {

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

                    PartitionConfig partitionConfig = nisFederation.createNssPartitionConfig(domain);
                    project.upload("partitions/"+partitionConfig.getName());

                    PenroseClient penroseClient = project.getClient();
                    penroseClient.startPartition(partitionConfig.getName());

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    MessageDialog.openError(editor.getSite().getShell(), "Action Failed", e.getMessage());
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

                    PenroseClient penroseClient = project.getClient();
                    PartitionConfig partitionConfig = nisFederation.getPartitionConfig(domain.getName()+"_nss");
                    penroseClient.stopPartition(partitionConfig.getName());
                    nisFederation.removePartitionConfig(partitionConfig.getName());
                    project.removeDirectory("partitions/"+partitionConfig.getName());

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    MessageDialog.openError(editor.getSite().getShell(), "Action Failed", e.getMessage());
                }
            }
        });

        return composite;
    }

}
