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
package com.servoy.j2db.server.headlessclient.jquery;

import java.io.Serializable;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

import com.servoy.j2db.server.headlessclient.MainPage;

/**
 * @author gboros
 * 
 */
@SuppressWarnings("nls")
public class JQueryLoader implements Serializable
{
//	public static final JavascriptResourceReference JS_JQUERY = new JavascriptResourceReference(JQueryLoader.class, "jquery-1.6.4.min.js"); //$NON-NLS-1$
//	public static final JavascriptResourceReference JS_JQUERY_DEBUG = new JavascriptResourceReference(JQueryLoader.class, "jquery-1.6.4.js"); //$NON-NLS-1$


	public static final JavascriptResourceReference JS_JQUERY = new JavascriptResourceReference(MainPage.class, "jquery/jquery-1.6.4.min.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_JQUERY_DEBUG = new JavascriptResourceReference(MainPage.class, "jquery/jquery-1.6.4.js"); //$NON-NLS-	

	private JQueryLoader()
	{
	}

	public static void render(IHeaderResponse response)
	{
		if (Application.exists() && Application.get().getDebugSettings().isAjaxDebugModeEnabled())
		{
			response.renderJavascriptReference(JQueryLoader.JS_JQUERY_DEBUG);
		}
		else
		{
			response.renderJavascriptReference(JQueryLoader.JS_JQUERY);
		}
	}
}
