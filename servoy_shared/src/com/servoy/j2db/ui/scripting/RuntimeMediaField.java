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
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JViewport;

import com.servoy.base.scripting.annotations.ServoyMobileFilterOut;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeImageMedia;

/**
 * Scriptable media component.
 * 
 * @author lvostinar
 * @since 6.0
 */
@ServoyMobileFilterOut
public class RuntimeMediaField extends AbstractRuntimeField<IFieldComponent> implements IRuntimeImageMedia
{
	private JComponent jComponent;

	public RuntimeMediaField(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	/**
	 * @param jComponent the jComponent to set
	 */
	public void setjComponent(JComponent jComponent)
	{
		this.jComponent = jComponent;
	}

	public String getElementType()
	{
		return IRuntimeComponent.IMAGE_MEDIA;
	}

	public void setScroll(int x, int y)
	{
		if (jComponent != null)
		{
			Rectangle rect = new Rectangle(x, y, getComponent().getSize().width, getComponent().getSize().height);
			if (jComponent.getParent() instanceof JViewport)
			{
				// you cannot ask for a region bigger then the actual view extent size to be visible - that would have no effect in some cases;
				// but if you want x and y to be the coordinates where the visible area starts (if that is possible) then a rectangle the same size as the visible area must be used
				Dimension s = ((JViewport)jComponent.getParent()).getExtentSize();
				rect.width = s.width;
				rect.height = s.height;
			}
			jComponent.scrollRectToVisible(rect);
		}
	}

	public int getScrollX()
	{
		if (jComponent != null)
		{
			return jComponent.getVisibleRect().x;
		}
		return 0;
	}

	public int getScrollY()
	{
		if (jComponent != null)
		{
			return jComponent.getVisibleRect().y;
		}
		return 0;
	}

	public boolean isEditable()
	{
		return getComponent().isEditable();
	}

	public void setEditable(boolean b)
	{
		if (isEditable() != b)
		{
			getComponent().setEditable(b);
			getChangesRecorder().setChanged();
		}
	}
}
