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


import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormManager;

/**
 * @author jcompagner
*/
public class CmdNextRecord extends AbstractCmd
{
	public CmdNextRecord(IApplication app)
	{
		super(
			app,
			"CmdNextRecord", app.getI18NMessage("servoy.menuitem.nextRecord"), "servoy.menuitem.nextRecord", app.getI18NMessage("servoy.menuitem.nextRecord.mnemonic").charAt(0)/*
																																												 * ,app
																																												 * .
																																												 * loadImage
																																												 * (
																																												 * "newrecord.gif"
																																												 * )
																																												 */); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		setActionCommand("nextrec"); //$NON-NLS-1$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_MASK));
	}

	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		IFormManager fm = application.getFormManager();
		final FormController fp = (FormController)fm.getCurrentForm();
		fp.selectNextRecord();
		return null;
	}
}