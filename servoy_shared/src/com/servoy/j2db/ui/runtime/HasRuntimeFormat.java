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
package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;


/**
 * Interface for components with formatting support.
 * 
 * @author jcompagner
 * @since 6.1
 */
public interface HasRuntimeFormat
{
	/**
	 * Gets or sets the display formatting of an element for number and text values; does not affect the actual value stored in the database column.
	 *
	 * <p>It only returns it's correct value if it was explicitly set, otherwise null.
	 * 
	 * @sample
	 * //sets the display formatting of the field
	 * %%prefix%%%%elementName%%.format = '###';
	 * 
	 * //gets the display formatting of the field
	 * var format = %%prefix%%%%elementName%%.format;
	 */
	@JSGetter
	public String getFormat();

	@JSSetter
	public void setFormat(String format);
}
