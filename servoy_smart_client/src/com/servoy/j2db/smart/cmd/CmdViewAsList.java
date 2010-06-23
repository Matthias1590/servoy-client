/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.smart.cmd;


import javax.swing.ButtonModel;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.util.FixedToggleButtonModel;

/**
 * Menu view as list action
 * @author jblok
 */
public class CmdViewAsList extends AbstractCmd
{
	public CmdViewAsList(IApplication app)
	{
		super(
			app,
			"CmdViewAsList", app.getI18NMessage("servoy.menuitem.viewAsList"), "servoy.menuitem.viewAsList", app.getI18NMessage("servoy.menuitem.viewAsList.mnemonic").charAt(0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		application.getFormManager().getCurrentForm().setView(IForm.LIST_VIEW);
		return null;
	}

	@Override
	protected ButtonModel createButtonModel()
	{
		return new FixedToggleButtonModel();
	}
}