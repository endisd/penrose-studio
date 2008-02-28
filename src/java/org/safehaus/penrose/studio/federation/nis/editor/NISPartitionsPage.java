package org.safehaus.penrose.studio.federation.nis.editor;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.IProgressService;
import org.safehaus.penrose.federation.repository.NISDomain;
import org.safehaus.penrose.partition.PartitionConfigs;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.studio.federation.nis.NISFederation;
import org.safehaus.penrose.studio.project.Project;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class NISPartitionsPage extends FormPage {

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    NISEditor editor;
    NISFederation nisFederation;

    Table table;

    public NISPartitionsPage(NISEditor editor, NISFederation nisFederation) {
        super(editor, "PARTITONS", "  Partitions  ");

        this.editor = editor;
        this.nisFederation = nisFederation;
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("Partitions");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section section = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Partitions");
        section.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control sourcesSection = createPartitionsSection(section);
        section.setClient(sourcesSection);

        refresh();
    }

    public Composite createPartitionsSection(Composite parent) {

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
        tc.setText("Name");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(100);
        tc.setText("YP");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(100);
        tc.setText("NIS");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(100);
        tc.setText("NSS");

        Composite links = toolkit.createComposite(leftPanel);
        links.setLayout(new RowLayout());
        links.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Hyperlink selectAllLink = toolkit.createHyperlink(links, "Select All", SWT.NONE);

        selectAllLink.addHyperlinkListener(new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent event) {
                table.selectAll();
            }
        });

        Hyperlink selectNoneLink = toolkit.createHyperlink(links, "Select None", SWT.NONE);

        selectNoneLink.addHyperlinkListener(new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent event) {
                table.deselectAll();
            }
        });

        Composite rightPanel = toolkit.createComposite(composite);
        rightPanel.setLayout(new GridLayout());
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.verticalSpan = 2;
        gd.widthHint = 120;
        rightPanel.setLayoutData(gd);

        Button createButton = new Button(rightPanel, SWT.PUSH);
        createButton.setText("Create");
        createButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    if (table.getSelectionCount() == 0) return;

                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Creating Partition",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    TableItem[] items = table.getSelection();
                    final Collection<NISDomain> domains = new ArrayList<NISDomain>();

                    for (TableItem ti : items) {
                        NISDomain domain = (NISDomain)ti.getData();
                        domains.add(domain);
                    }

                    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();

                    progressService.busyCursorWhile(new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                monitor.beginTask("Creating partitions...", domains.size());

                                for (NISDomain domain : domains) {

                                    monitor.subTask("Creating "+domain.getName()+" partitions.");
                                    nisFederation.createPartitions(domain);

                                    monitor.worked(1);
                                }

                            } catch (Exception e) {
                                throw new InvocationTargetException(e, e.getMessage());
                            }
                        }
                    });

                    PenroseStudio penroseStudio = PenroseStudio.getInstance();
                    penroseStudio.notifyChangeListeners();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }

                refresh();
            }
        });

        Button removeButton = new Button(rightPanel, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    if (table.getSelectionCount() == 0) return;

                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Removing Partition",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    TableItem[] items = table.getSelection();
                    final Collection<NISDomain> domains = new ArrayList<NISDomain>();

                    for (TableItem ti : items) {
                        NISDomain domain = (NISDomain)ti.getData();
                        domains.add(domain);
                    }

                    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();

                    progressService.busyCursorWhile(new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                monitor.beginTask("Removing partitions...", domains.size());

                                for (NISDomain domain : domains) {

                                    monitor.subTask("Removing "+domain.getName()+" partitions.");
                                    nisFederation.removePartitions(domain);

                                    monitor.worked(1);
                                }

                            } catch (Exception e) {
                                throw new InvocationTargetException(e, e.getMessage());
                            }
                        }
                    });

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

            Project project = nisFederation.getProject();
            PartitionConfigs partitionConfigs = project.getPartitionConfigs();

            for (NISDomain domain : nisFederation.getRepositories()) {

                TableItem ti = new TableItem(table, SWT.NONE);
                ti.setText(0, domain.getName());

                boolean ypPartition = partitionConfigs.getPartitionConfig(domain.getName()+"_"+NISFederation.YP) != null;
                String status = domain.isYpEnabled() ? (ypPartition ? "OK" : "Missing") : "Disabled";
                ti.setText(1, status);

                boolean nisPartition = partitionConfigs.getPartitionConfig(domain.getName()+"_"+NISFederation.NIS) != null;
                status = domain.isNisEnabled() ? (nisPartition ? "OK" : "Missing") : "Disabled";
                ti.setText(2, status);

                boolean nssPartition = partitionConfigs.getPartitionConfig(domain.getName()+"_"+NISFederation.NSS) != null;
                status = domain.isNssEnabled() ? (nssPartition ? "OK" : "Missing") : "Disabled";
                ti.setText(3, status);

                ti.setData(domain);
            }

            table.select(indices);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
        }
    }

}
