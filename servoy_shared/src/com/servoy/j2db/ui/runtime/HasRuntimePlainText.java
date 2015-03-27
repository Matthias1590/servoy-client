/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSFunction;

/**
 * Interface for components with plain text support.
 *
 * @author rgansevles
 *
 *  @since 6.1
 *
 */
public interface HasRuntimePlainText
{
	/**
	 * Gets the plain text for the formatted HTML/RTF text of a specified field element with a design time display property of HTML_AREA/RTF only.
	 *
	 * NOTE: This does not work in Web Client.
	 * 
	 * NOTE: As of Servoy 4.x (and higher) you can also get/set the relative or absolute URL for an HTML_AREA field element. For more detail, see the .URL and .baseURL field element runtime properties earlier in this section.
	 *
	 * NOTE: For information about field element design time properties, see the section on Field elements in the Forms chapter of the Servoy Developer User's Guide.
	 *
	 * @sample var my_text = %%prefix%%%%elementName%%.getAsPlainText();
	 *
	 * @return the plain text
	 */
	@JSFunction
	public String getAsPlainText();
}
