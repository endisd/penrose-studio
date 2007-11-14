package org.safehaus.penrose.studio.federation.nis.ldap;

import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.apache.log4j.Logger;
import org.safehaus.penrose.studio.federation.nis.NISFederation;
import org.safehaus.penrose.studio.federation.nis.NISDomain;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.ldap.SearchResult;
import org.safehaus.penrose.ldap.SearchRequest;
import org.safehaus.penrose.ldap.SearchResponse;
import org.safehaus.penrose.ldap.Attributes;
import org.safehaus.penrose.management.PartitionClient;
import org.safehaus.penrose.management.SourceClient;
import org.safehaus.penrose.management.PenroseClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

/**
 * @author Endi S. Dewata
 */
public class NISLDAPErrorsPage extends FormPage {

    public DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    NISLDAPEditor editor;
    NISFederation nisFederation;
    NISDomain domain;

    Table table;
    Text descriptionText;
    Text noteText;

    Project project;
    PartitionClient partitionClient;
    SourceClient errors;

    public NISLDAPErrorsPage(NISLDAPEditor editor) throws Exception {
        super(editor, "ERRORS", "  Errors  ");

        this.editor = editor;
        this.project = editor.getProject();
        this.nisFederation = editor.getNisTool();
        this.domain = editor.getDomain();

        PenroseClient penroseClient = project.getClient();
        partitionClient = penroseClient.getPartitionClient(domain.getName());
        errors = partitionClient.getSourceClient("errors");
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("Errors");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section errorsSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        errorsSection.setText("Errors");
        errorsSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control errorsControl = createErrorsControl(errorsSection);
        errorsSection.setClient(errorsControl);

        refresh();
    }

    public Composite createErrorsControl(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(2, false));

        Composite leftPanel = toolkit.createComposite(composite);
        leftPanel.setLayout(new GridLayout());
        leftPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        table = new Table(leftPanel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(100);
        tc.setText("Number");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(150);
        tc.setText("Time");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(300);
        tc.setText("Title");

        table.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                showError();
            }
        });

        toolkit.createLabel(leftPanel, "Description:");

        descriptionText = toolkit.createText(leftPanel, "", SWT.BORDER | SWT.READ_ONLY  | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 150;
        descriptionText.setLayoutData(gd);

        toolkit.createLabel(leftPanel, "Note:");

        noteText = toolkit.createText(leftPanel, "", SWT.BORDER | SWT.READ_ONLY  | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 150;
        noteText.setLayoutData(gd);

        Composite rightPanel = toolkit.createComposite(composite);
        rightPanel.setLayout(new GridLayout());
        gd = new GridData(GridData.FILL_VERTICAL);
        gd.verticalSpan = 2;
        gd.widthHint = 120;
        rightPanel.setLayoutData(gd);

        Button removeButton = new Button(rightPanel, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    if (table.getSelectionCount() == 0) return;

                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Removing Error",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    TableItem[] items = table.getSelection();

                    for (TableItem ti : items) {
                        SearchResult result = (SearchResult)ti.getData();
                        try {
                            errors.delete(result.getDn());

                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }

                    PenroseStudio penroseStudio = PenroseStudio.getInstance();
                    penroseStudio.notifyChangeListeners();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }

                refresh();
            }
        });

        new Label(rightPanel, SWT.NONE);

        Button refreshButton = new Button(rightPanel, SWT.PUSH);
        refreshButton.setText("Refresh");
        refreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        refreshButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                refresh();
            }
        });

        return composite;
    }

    public void refresh() {
        try {
            int[] indices = table.getSelectionIndices();

            table.removeAll();

            SearchRequest request = new SearchRequest();
            SearchResponse response = new SearchResponse();

            errors.search(request, response);

            while (response.hasNext()) {
                SearchResult result = response.next();

                Attributes attributes = result.getAttributes();
                String id = attributes.getValue("id").toString();
                Timestamp time = (Timestamp)attributes.getValue("time");
                String title = (String)attributes.getValue("title");

                TableItem ti = new TableItem(table, SWT.NONE);

                ti.setText(0, id);
                ti.setText(1, df.format(time));
                ti.setText(2, title);

                ti.setData(result);
            }

            table.select(indices);

            showError();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
        }
    }

    public void showError() {

        if (table.getSelectionCount() !=  1) {
            descriptionText.setText("");
            noteText.setText("");
            return;
        }

        TableItem ti = table.getSelection()[0];

        SearchResult result = (SearchResult)ti.getData();
        Attributes attributes = result.getAttributes();

        String description = (String)attributes.getValue("description");
        descriptionText.setText(description == null ? "" : description);

        String note = (String)attributes.getValue("note");
        noteText.setText(note == null ? "" : note);
    }
}