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

import com.servoy.base.scripting.annotations.ServoyMobileFilterOut;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.RenderableWrapper;
import com.servoy.j2db.ui.runtime.IRuntimeCalendar;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.Utils;

/**
 * Scriptable calendar component.
 * 
 * @author lvostinar
 * @since 6.0
 */
@ServoyMobileFilterOut
public class RuntimeDataCalendar extends AbstractRuntimeField<IFieldComponent> implements IRuntimeCalendar, IFormatScriptComponent
{
	private ComponentFormat componentFormat;

	public RuntimeDataCalendar(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String getElementType()
	{
		return IRuntimeComponent.CALENDAR;
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

	public void setFormat(String formatString)
	{
		if (!Utils.safeEquals(formatString, getFormat()))
		{
			setComponentFormat(new ComponentFormat(FormatParser.parseFormatProperty(application.getI18NMessageIfPrefixed(formatString)),
				componentFormat == null ? IColumnTypes.TEXT : componentFormat.dpType, componentFormat == null ? IColumnTypes.TEXT : componentFormat.uiType));
			getChangesRecorder().setChanged();

			RenderEventExecutor renderEventExecutor = getRenderEventExecutor();
			IScriptRenderMethods renderable = getRenderable();
			if (renderEventExecutor != null && !renderEventExecutor.isOnRenderExecuting() && renderable instanceof RenderableWrapper)
			{
				((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_FORMAT);
			}
			fireOnRender();
		}
	}

	public String getFormat()
	{
		return componentFormat == null ? null : componentFormat.parsedFormat.getFormatString();
	}

	public void setComponentFormat(ComponentFormat componentFormat)
	{
		this.componentFormat = componentFormat;
		if (componentFormat != null && getComponent() instanceof IFormattingComponent)
		{
			((IFormattingComponent)getComponent()).installFormat(componentFormat);
		}
	}

	public ComponentFormat getComponentFormat()
	{
		return componentFormat;
	}


}
