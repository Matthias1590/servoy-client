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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;

import com.servoy.base.scripting.api.IJSEvent.EventType;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.util.Utils;


/**
 * This modifier will make sure that, if no event is already registered to "onKeyDown", the event will be consumed when the key is ENTER. This is needed as, in
 * forms, ENTER in text field that do not block ENTER events will lead to onClick on a button from that form (if there is such a button) - unwanted behavior
 * (different from SmartClient).
 * 
 * @author acostescu
 */
public class ConsumeEnterAttributeModifier extends ServoyActionEventBehavior
{

	public ConsumeEnterAttributeModifier(Component component, WebEventExecutor eventExecutor)
	{
		super("onKeyDown", component, eventExecutor, "ConsumeEnter"); //$NON-NLS-2$
	}

	/**
	 * @see wicket.ajax.form.AjaxFormComponentUpdatingBehavior#onUpdate(wicket.ajax.AjaxRequestTarget)
	 */
	@Override
	protected void onUpdate(AjaxRequestTarget target)
	{
		eventExecutor.onEvent(EventType.none, target, getComponent(),
			Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)));
	}

	@Override
	protected IAjaxCallDecorator getAjaxCallDecorator()
	{
		return new AjaxCallDecorator()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public CharSequence decorateScript(CharSequence script)
			{
				return "return testEnterKey(event, function() {event.target.blur();" + script + "});"; //$NON-NLS-1$ //$NON-NLS-2$ 
			}
		};
	}

	@Override
	protected boolean getUpdateModel()
	{
		return false;
	}
}