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

package com.servoy.j2db.ui.scripting;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;

/**
 * Scriptable combobox component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeDataCombobox extends AbstractRuntimeFormattedValuelistComponent<IFieldComponent> implements IScriptDataComboboxMethods
{
	public RuntimeDataCombobox(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.COMBOBOX;
	}

	@Override
	public void js_setSize(int x, int y)
	{
		if (getComponent() instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)getComponent()).setCachedSize(new Dimension(x, y));
		}
		getComponent().setSize(new Dimension(x, y));
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).validate();
		}
		getChangesRecorder().setSize(x, y, getComponent().getBorder(), getComponent().getMargin(), 0, true, SwingConstants.CENTER);
	}

	@Override
	public void js_setBorder(String spec)
	{
		super.js_setBorder(spec);
		getChangesRecorder().setSize(getComponent().getSize().width, getComponent().getSize().height, getComponent().getBorder(), getComponent().getMargin(),
			0, true, SwingConstants.CENTER);
	}
}
