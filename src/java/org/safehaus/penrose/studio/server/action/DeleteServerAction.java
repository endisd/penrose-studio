package org.safehaus.penrose.studio.server.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.apache.log4j.Logger;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.studio.server.ServerNode;
import org.safehaus.penrose.studio.server.Server;
import org.safehaus.penrose.studio.server.ServersView;

public class DeleteServerAction extends Action {

    Logger log = Logger.getLogger(getClass());

    public DeleteServerAction() {
        setText("&Delete Server");
        setImageDescriptor(PenroseStudio.getImageDescriptor(PenroseImage.DELETE_LARGE));
        setAccelerator(SWT.CTRL | 'D');
        setToolTipText("Delete Server");
        setId(getClass().getName());
    }

    public void run() {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        try {
            ServersView serversView = ServersView.getInstance();
            ServerNode projectNode = serversView.getSelectedServerNode();
            if (projectNode == null) return;

            Server project = projectNode.getServer();

            boolean confirm = MessageDialog.openQuestion(
                    window.getShell(),
                    "Removing Server", "Are you sure?"
            );

            if (!confirm) return;

            serversView.removeProjectConfig(project.getName());

            PenroseStudio penroseStudio = PenroseStudio.getInstance();
            penroseStudio.notifyChangeListeners();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
        }
    }
}
