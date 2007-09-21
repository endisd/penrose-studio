package org.safehaus.penrose.studio.nis.domain;

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
import org.safehaus.penrose.studio.nis.NISTool;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.nis.NISDomain;
import org.safehaus.penrose.management.PenroseClient;
import org.safehaus.penrose.management.PartitionClient;
import org.safehaus.penrose.management.SchedulerClient;
import org.safehaus.penrose.management.JobClient;
import org.safehaus.penrose.partition.Partitions;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.source.Source;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

/**
 * @author Endi S. Dewata
 */
public class NISDomainTrackerPage extends FormPage {

    public DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    NISDomainEditor editor;
    NISTool nisTool;
    NISDomain domain;

    Table trackerTable;

    public NISDomainTrackerPage(NISDomainEditor editor) {
        super(editor, "TRACKER", "  Tracker  ");

        this.editor = editor;
        this.nisTool = editor.getNisTool();
        this.domain = editor.getDomain();
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("Tracker");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section trackerSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        trackerSection.setText("Tracker");
        trackerSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control trackerControl = createTrackerControl(trackerSection);
        trackerSection.setClient(trackerControl);

        refreshTracker();
    }

    public Composite createTrackerControl(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(2, false));

        Composite leftPanel = toolkit.createComposite(composite);
        leftPanel.setLayout(new GridLayout());
        leftPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        trackerTable = new Table(leftPanel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        trackerTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        trackerTable.setHeaderVisible(true);
        trackerTable.setLinesVisible(true);

        TableColumn tc = new TableColumn(trackerTable, SWT.NONE);
        tc.setWidth(150);
        tc.setText("Change Number");

        tc = new TableColumn(trackerTable, SWT.NONE);
        tc.setWidth(350);
        tc.setText("Change Time");

        Composite rightPanel = toolkit.createComposite(composite);
        rightPanel.setLayout(new GridLayout());
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.verticalSpan = 2;
        gd.widthHint = 120;
        rightPanel.setLayoutData(gd);

        Button removeButton = new Button(rightPanel, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    if (trackerTable.getSelectionCount() == 0) return;

                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Removing Tracker",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    TableItem[] items = trackerTable.getSelection();

                    Project project = nisTool.getProject();
                    PenroseClient client = project.getClient();
                    PartitionClient partitionClient = client.getPartitionClient(domain.getName());
                    SchedulerClient schedulerClient = partitionClient.getSchedulerClient();

                    JobClient jobClient = schedulerClient.getJobClient("LDAPSync");

                    for (TableItem ti : items) {
                        SearchResult searchResult = (SearchResult)ti.getData();
                        Attributes attributes = searchResult.getAttributes();
                        Long changeNumber = Long.parseLong(attributes.getValue("changeNumber").toString());

                        jobClient.invoke("removeTracker", new Object[] { changeNumber }, new String[] { Long.class.getName() });
                    }

                    PenroseStudio penroseStudio = PenroseStudio.getInstance();
                    penroseStudio.notifyChangeListeners();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    MessageDialog.openError(editor.getSite().getShell(), "Action Failed", e.getMessage());
                }

                refreshTracker();
            }
        });

        new Label(rightPanel, SWT.NONE);

        Button refreshButton = new Button(rightPanel, SWT.PUSH);
        refreshButton.setText("Refresh");
        refreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        refreshButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                refreshTracker();
            }
        });

        return composite;
    }

    public void refreshTracker() {
        try {
            int[] indices = trackerTable.getSelectionIndices();

            trackerTable.removeAll();

            Partitions partitions = nisTool.getPartitions();
            Partition partition = partitions.getPartition(domain.getName());
            Source tracker = partition.getSource("tracker");

            SearchRequest request = new SearchRequest();
            SearchResponse response = new SearchResponse() {
                public void add(SearchResult result) {

                    Attributes attributes = result.getAttributes();
                    String changeNumber = attributes.getValue("changeNumber").toString();
                    String changeTimestamp = df.format((Timestamp)attributes.getValue("changeTimestamp"));

                    TableItem ti = new TableItem(trackerTable, SWT.NONE);

                    ti.setText(0, changeNumber);
                    ti.setText(1, changeTimestamp);

                    ti.setData(result);
                }
            };

            tracker.search(request, response);

            trackerTable.select(indices);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            MessageDialog.openError(editor.getSite().getShell(), "Action Failed", e.getMessage());
        }
    }
}
