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
package org.safehaus.penrose.studio.ldap.source.editor;

import org.safehaus.penrose.studio.source.editor.SourceEditor;
import org.safehaus.penrose.studio.source.editor.SourceBrowsePage;
import org.safehaus.penrose.studio.source.editor.SourcePropertiesPage;
import org.safehaus.penrose.studio.source.editor.SourceFieldsPage;
import org.safehaus.penrose.studio.config.editor.ParametersPage;

public class LDAPSourceEditor extends SourceEditor {

    ParametersPage parametersPage;

    public void addPages() {
        try {
            addPage(new SourcePropertiesPage(this));
            addPage(new LDAPSourcePropertyPage(this));
            addPage(new LDAPSourceFieldsPage(this));

            parametersPage = new ParametersPage(this, "Source Editor");
            parametersPage.setParameters(sourceConfig.getParameters());
            addPage(parametersPage);

            addPage(new SourceBrowsePage(this));

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}