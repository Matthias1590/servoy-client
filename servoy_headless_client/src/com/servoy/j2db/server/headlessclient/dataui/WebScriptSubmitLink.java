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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IScriptTextLabelMethods;
import com.servoy.j2db.util.HtmlUtils;

/**
 * Represents a clickable (has an action event) label in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebScriptSubmitLink extends WebBaseSubmitLink implements IScriptTextLabelMethods
{
	private static final long serialVersionUID = 1L;

	private String i18n;


	/**
	 * @param id
	 */
	public WebScriptSubmitLink(IApplication application, String id)
	{
		super(application, id);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataui.ITextScriptLabel#js_getText()
	 */
	public String js_getText()
	{
		if (i18n != null) return i18n;
		return getText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataui.ITextScriptLabel#js_setText(java.lang.String)
	 */
	public void js_setText(String txt)
	{
		if (txt != null && txt.startsWith("i18n:")) //$NON-NLS-1$
		{
			i18n = txt;
			setText(application.getI18NMessage(txt));
		}
		else
		{
			i18n = null;
			setText(txt);
		}
		jsChangeRecorder.setChanged();
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseSubmitLink#onComponentTagBody(wicket.markup.MarkupStream, wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		CharSequence bodyText = getDefaultModelObjectAsString();

		Object modelObject = getDefaultModelObject();
		if (HtmlUtils.startsWithHtml(modelObject))
		{
			// ignore script/header contributions for now
			bodyText = StripHTMLTagsConverter.convertBodyText(this, bodyText, application.getFlattenedSolution()).getBodyTxt();
		}
		replaceComponentTagBody(markupStream, openTag, bodyText);
	}
}
