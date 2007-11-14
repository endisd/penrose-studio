package org.safehaus.penrose.studio.federation.nis.conflict;

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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.apache.log4j.Logger;
import org.safehaus.penrose.studio.federation.nis.NISDomain;
import org.safehaus.penrose.studio.federation.nis.NISFederation;
import org.safehaus.penrose.studio.federation.Federation;
import org.safehaus.penrose.studio.nis.dialog.NISUserDialog;
import org.safehaus.penrose.studio.nis.action.*;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.PartitionConfigs;
import org.safehaus.penrose.partition.PartitionConfig;
import org.safehaus.penrose.source.Source;
import org.safehaus.penrose.source.SourceConfig;
import org.safehaus.penrose.jdbc.Assignment;
import org.safehaus.penrose.jdbc.QueryResponse;
import org.safehaus.penrose.jdbc.connection.JDBCConnection;
import org.safehaus.penrose.management.PenroseClient;
import org.safehaus.penrose.management.PartitionClient;
import org.safehaus.penrose.management.SourceClient;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.sql.ResultSet;

/**
 * @author Endi S. Dewata
 */
public class NISUserScriptsPage extends FormPage {

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    Combo actionCombo;

    Label messageLabel;
    Table usersTable;
    Table conflictsTable;
    Table matchesTable;

    NISUsersEditor editor;
    NISDomain domain;
    NISFederation nisFederation;

    Map<String,Collection<Conflict>> conflicts = new TreeMap<String,Collection<Conflict>>();

    public NISUserScriptsPage(NISUsersEditor editor) {
        super(editor, "SCRIPTS", "  Scripts  ");

        this.editor = editor;
        domain = editor.getDomain();
        nisFederation = editor.getNisTool();
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("NIS User Scripts");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section section = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Action");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control sourcesSection = createActionSection(section);
        section.setClient(sourcesSection);

        section = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Results");
        section.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control resultsSection = createResultsSection(section);
        section.setClient(resultsSection);

        init();
    }

    public void init() {
        try {
            actionCombo.removeAll();


            actionCombo.add("Conflicting UID Finder");
            actionCombo.setData("Conflicting UID Finder", ConflictingUIDFinderAction.class.getName());

            actionCombo.add("Inconsistent UID Finder");
            actionCombo.setData("Inconsistent UID Finder", InconsistentUIDFinderAction.class.getName());

/*
            SearchRequest request = new SearchRequest();
            request.setFilter("(type=users)");

            SearchResponse response = new SearchResponse() {
                public void add(SearchResult result) throws Exception {
                    Attributes attributes = result.getAttributes();
                    String actionName = (String) attributes.getValue("name");
                    String className = (String) attributes.getValue("className");

                    actionCombo.add(actionName);
                    actionCombo.setData(actionName, className);
                }
            };

            nisFederation.getActions().search(request, response);
*/
            actionCombo.select(0);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
        }
    }

    public Composite createActionSection(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(3, false));

        Label actionLabel = toolkit.createLabel(composite, "Action:");
        GridData gd = new GridData();
        gd.widthHint = 100;
        actionLabel.setLayoutData(gd);

        actionCombo = new Combo(composite, SWT.READ_ONLY);
        actionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button runButton = new Button(composite, SWT.PUSH);
        runButton.setText("Run");
        gd = new GridData();
        gd.horizontalAlignment = GridData.END;
        gd.widthHint = 80;
        runButton.setLayoutData(gd);

        runButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    run();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }
            }
        });

        return composite;
    }

    public Composite createResultsSection(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(2, false));

        messageLabel = toolkit.createLabel(composite, "");
        messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label conflictsLabel = toolkit.createLabel(composite, "Conflicts:");
        conflictsLabel.setLayoutData(new GridData());

        usersTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.verticalSpan = 3;
        usersTable.setLayoutData(gd);

        usersTable.setHeaderVisible(true);
        usersTable.setLinesVisible(true);

        usersTable.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent focusEvent) {
                conflictsTable.deselectAll();
                matchesTable.deselectAll();
            }
        });

        TableColumn tc = new TableColumn(usersTable, SWT.NONE);
        tc.setText("User");
        tc.setWidth(100);

        tc = new TableColumn(usersTable, SWT.NONE);
        tc.setText("UID");
        tc.setWidth(80);

        usersTable.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    if (usersTable.getSelectionCount() == 0) return;

                    TableItem item = usersTable.getSelection()[0];

                    Attributes attributes = (Attributes)item.getData();

                    showConflicts(attributes);
                    showMatches(attributes);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }
            }
        });

        conflictsTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        conflictsTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        conflictsTable.setHeaderVisible(true);
        conflictsTable.setLinesVisible(true);

        conflictsTable.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent focusEvent) {
                matchesTable.deselectAll();
            }
        });

        tc = new TableColumn(conflictsTable, SWT.NONE);
        tc.setText("Domain");
        tc.setWidth(120);

        tc = new TableColumn(conflictsTable, SWT.NONE);
        tc.setText("User");
        tc.setWidth(100);

        tc = new TableColumn(conflictsTable, SWT.NONE);
        tc.setText("UID");
        tc.setWidth(80);

        Label matchesLabel = toolkit.createLabel(composite, "Matches:");
        matchesLabel.setLayoutData(new GridData());

        matchesTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        matchesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        matchesTable.setHeaderVisible(true);
        matchesTable.setLinesVisible(true);

        matchesTable.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent focusEvent) {
                conflictsTable.deselectAll();
            }
        });

        tc = new TableColumn(matchesTable, SWT.NONE);
        tc.setText("Domain");
        tc.setWidth(120);

        tc = new TableColumn(matchesTable, SWT.NONE);
        tc.setText("User");
        tc.setWidth(100);

        tc = new TableColumn(matchesTable, SWT.NONE);
        tc.setText("UID");
        tc.setWidth(80);

        Button editButton = new Button(composite, SWT.PUSH);
        editButton.setText("Edit");
        gd = new GridData();
        gd.widthHint = 80;
        editButton.setLayoutData(gd);

        editButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    TableItem item;

                    if (conflictsTable.getSelectionCount() > 0) {
                        item = conflictsTable.getSelection()[0];

                    } else if (matchesTable.getSelectionCount() > 0) {
                        item = matchesTable.getSelection()[0];

                    } else if (usersTable.getSelectionCount() > 0) {
                        item = usersTable.getSelection()[0];

                    } else {
                        return;
                    }

                    Attributes attributes = (Attributes)item.getData();
                    String domain = (String)attributes.getValue("domain");
                    String uid = (String)attributes.getValue("uid");
                    Integer origUidNumber = (Integer)attributes.getValue("origUidNumber");

                    edit(domain, uid, origUidNumber);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }
            }
        });

        return composite;
    }

    public void showConflicts(Attributes attributes) throws Exception {

        conflictsTable.removeAll();

        String uid1 = (String) attributes.getValue("uid");
        Collection<Conflict> list = conflicts.get(uid1);

        if (list == null) return;

        for (Conflict conflict : list) {

            Attributes attributes2 = conflict.getAttributes2();

            String domain2 = (String) attributes2.getValue("domain");
            String uid2 = (String) attributes2.getValue("uid");
            Integer uidNumber2 = (Integer) attributes2.getValue("uidNumber");
            if (uidNumber2 == null) uidNumber2 = (Integer) attributes2.getValue("origUidNumber");

            TableItem ti = new TableItem(conflictsTable, SWT.NONE);
            ti.setText(0, domain2);
            ti.setText(1, "" + uid2);
            ti.setText(2, "" + uidNumber2);

            ti.setData(attributes2);
        }
    }

    public void showMatches(Attributes attributes) throws Exception {

        matchesTable.removeAll();

        String uid = (String) attributes.getValue("uid");
        Integer uidNumber = (Integer) attributes.getValue("uidNumber");
        if (uidNumber == null) uidNumber = (Integer) attributes.getValue("origUidNumber");

        Project project = nisFederation.getProject();
        PartitionConfigs partitionConfigs = project.getPartitionConfigs();

        Partition partition = nisFederation.getPartition();
        JDBCConnection connection = (JDBCConnection)partition.getConnection(Federation.JDBC);

        for (NISDomain repository : nisFederation.getRepositories()) {
            final String name = repository.getName();
            if (domain.getName().equals(name)) continue;

            PartitionConfig partitionConfig = partitionConfigs.getPartitionConfig(name+"_"+NISFederation.YP);
            SourceConfig sourceConfig = partitionConfig.getSourceConfigs().getSourceConfig(NISFederation.CACHE_USERS);

            String table = connection.getTableName(sourceConfig);

            String sql = "select a.uid, a.uidNumber, b.uidNumber" +
                    " from "+table+" a"+
                    " left join "+ NISFederation.NIS_TOOL +".users b on b.domain=? and a.uid=b.uid"+
                    " where a.uid = ? and (b.uidNumber is null and a.uidNumber = ? or b.uidNumber = ?)"+
                    " order by a.uid";

            Collection<Assignment> assignments = new ArrayList<Assignment>();
            assignments.add(new Assignment(name));
            assignments.add(new Assignment(uid));
            assignments.add(new Assignment(uidNumber));
            assignments.add(new Assignment(uidNumber));

            QueryResponse queryResponse = new QueryResponse() {
                public void add(Object object) throws Exception {
                    ResultSet rs = (ResultSet)object;

                    String uid2 = rs.getString(1);
                    Integer origUidNumber2 = (Integer)rs.getObject(2);
                    Integer uidNumber2 = (Integer)rs.getObject(3);
                    if (uidNumber2 == null) uidNumber2 = origUidNumber2;

                    Attributes attributes = new Attributes();
                    attributes.setValue("domain", name);
                    attributes.setValue("uid", uid2);
                    attributes.setValue("origUidNumber", origUidNumber2);
                    attributes.setValue("uidNumber", uidNumber2);

                    TableItem ti = new TableItem(matchesTable, SWT.NONE);
                    ti.setText(0, name);
                    ti.setText(1, "" + uid2);
                    ti.setText(2, "" + uidNumber2);

                    ti.setData(attributes);
                }
            };

            connection.executeQuery(sql, assignments, queryResponse);
        }
    }

    public void run() throws Exception {

        usersTable.removeAll();
        conflictsTable.removeAll();
        matchesTable.removeAll();
        conflicts.clear();

        String actionName = actionCombo.getText();
        String className = (String) actionCombo.getData(actionName);

        Class clazz = Class.forName(className);
        NISAction action = (NISAction) clazz.newInstance();
        action.setNisTool(nisFederation);

        NISActionRequest request = new NISActionRequest();
        request.setDomain(domain.getName());

        for (NISDomain repository : nisFederation.getRepositories()) {
            String name = repository.getName();
            request.addDomain(name);
        }

        NISActionResponse response = new NISActionResponse() {
            public void add(Object object) {
                Conflict conflict = (Conflict)object;

                Attributes attributes1 = conflict.getAttributes1();
                String uid = (String) attributes1.getValue("uid");

                Collection<Conflict> list = conflicts.get(uid);
                if (list == null) {
                    list = new ArrayList<Conflict>();
                    conflicts.put(uid, list);
                }

                list.add(conflict);
            }
        };

        action.execute(request, response);

        for (Collection<Conflict> list : conflicts.values()) {

            Conflict conflict = list.iterator().next();

            Attributes attributes1 = conflict.getAttributes1();
            String uid = (String) attributes1.getValue("uid");
            Integer uidNumber = (Integer)attributes1.getValue("uidNumber");
            if (uidNumber == null) uidNumber = (Integer)attributes1.getValue("origUidNumber");

            TableItem ti = new TableItem(usersTable, SWT.NONE);
            ti.setText(0, uid);
            ti.setText(1, "" + uidNumber);

            ti.setData(attributes1);
        }

        messageLabel.setText("Found " + conflicts.size() + " user(s).");
    }

    public void edit(
            String domainName,
            String uid,
            Integer origUidNumber
    ) throws Exception {

        PenroseClient penroseClient = nisFederation.getProject().getClient();

        PartitionClient partitionClient = penroseClient.getPartitionClient(domainName+"_"+NISFederation.YP);
        SourceClient sourceClient = partitionClient.getSourceClient(NISFederation.CHANGE_USERS);

        Partition partition = nisFederation.getPartitions().getPartition(domainName);

        RDNBuilder rb = new RDNBuilder();
        rb.set("domain", domainName);
        rb.set("uid", uid);
        DN dn = new DN(rb.toRdn());

        NISUserDialog dialog = new NISUserDialog(getSite().getShell(), SWT.NONE);
        dialog.setDomain(domainName);
        dialog.setUid(uid);
        dialog.setOrigUidNumber(origUidNumber);

        Source penroseUsers = partition.getSource(NISFederation.CHANGE_USERS);

        SearchRequest request = new SearchRequest();
        request.setDn(dn);

        SearchResponse response = new SearchResponse();

        sourceClient.search(request, response);
        penroseUsers.search(request, response);

        if (response.hasNext()) {
            SearchResult result = response.next();
            Attributes attributes = result.getAttributes();
            dialog.setNewUidNumber((Integer)attributes.getValue("uidNumber"));
        }

        dialog.open();

        int action = dialog.getAction();

        if (action == NISUserDialog.CANCEL) return;

        Integer uidNumber = dialog.getUidNumber();
        String message = dialog.getMessage();

        if (action == NISUserDialog.SET) {

            if (!origUidNumber.equals(uidNumber)) checkUidNumber(uid, uidNumber);

            Attributes attrs = new Attributes();
            attrs.setValue("domain", domainName);
            attrs.setValue("uid", uid);
            attrs.setValue("origUidNumber", origUidNumber);
            attrs.setValue("uidNumber", uidNumber);
            attrs.setValue("message", message);

            penroseUsers.add(dn, attrs);

        } else if (action == NISUserDialog.CHANGE) {

            if (!origUidNumber.equals(uidNumber)) checkUidNumber(uid, uidNumber);

            Collection<Modification> modifications = new ArrayList<Modification>();
            modifications.add(new Modification(Modification.REPLACE, new Attribute("uidNumber", uidNumber)));
            modifications.add(new Modification(Modification.REPLACE, new Attribute("message", message)));

            penroseUsers.modify(dn, modifications);

        } else { // if (action == NISUserDialog.REMOVE) {

            penroseUsers.delete(dn);
        }
    }

    public void checkUidNumber(String uid, Integer uidNumber) throws Exception {

        Project project = nisFederation.getProject();
        PenroseClient client = project.getClient();

        PartitionClient partitionClient = client.getPartitionClient(Federation.PARTITION);
        //Partition partition = nisFederation.getPartition();

        SearchRequest request = new SearchRequest();
        request.setFilter("(uidNumber=" + uidNumber + ")");

        SearchResponse response = new SearchResponse();

        SourceClient users = partitionClient.getSourceClient(NISFederation.CHANGE_USERS);
        //Source users = partition.getSource(NISFederation.CHANGE_USERS);
        users.search(request, response);

        while (response.hasNext()) {
            SearchResult result = response.next();
            Attributes attributes = result.getAttributes();

            String domainName = (String)attributes.getValue("domain");
            String uid2 = (String)attributes.getValue("uid");
            if (uid.equals(uid2)) continue;

            throw new Exception("UID number "+uidNumber+" is already allocated for user "+uid2+" in domain "+domainName);
        }

        for (NISDomain repository : nisFederation.getRepositories()) {
            String name = repository.getName();

            PartitionClient partitionClient2 = client.getPartitionClient(name+"_"+NISFederation.YP);
            //Partition partition2 = nisFederation.getPartitions().getPartition(name);

            response = new SearchResponse();

            SourceClient users2 = partitionClient.getSourceClient(NISFederation.CACHE_USERS);
            //Source users2 = partition2.getSource(NISFederation.CACHE_USERS);
            users2.search(request, response);

            while (response.hasNext()) {
                SearchResult result = response.next();
                Attributes attrs = result.getAttributes();

                String uid2 = (String)attrs.getValue("uid");
                if (uid.equals(uid2)) continue;

                throw new Exception("UID number "+uidNumber+" is used by user "+uid2+" in domain "+name);
            }
        }
    }
}