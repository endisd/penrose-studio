/**
 * Copyright 2009 Red Hat, Inc.
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
package org.safehaus.penrose.studio.welcome.action;

import org.eclipse.jface.action.Action;
import org.safehaus.penrose.studio.welcome.SplashShell;
import org.safehaus.penrose.studio.PenroseStudio;

public class AboutAction extends Action {

	public AboutAction() {
        setText("&About "+ PenroseStudio.PRODUCT_NAME);
        setId(getClass().getName());
	}

	public void run() {
		SplashShell shell = new SplashShell();
		shell.open();
	}

}