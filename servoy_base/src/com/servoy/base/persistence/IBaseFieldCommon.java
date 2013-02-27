/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.persistence;

import com.servoy.base.scripting.annotations.ServoyMobile;


/**
 * Base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 *
 * @since 7.0
 */
public interface IBaseFieldCommon extends IBaseComponentCommon
{
	/**
	 * The dataprovider of the component.
	 */
	@ServoyMobile
	String getDataProviderID();

	void setDataProviderID(String arg);

	/**
	 * The type of display used by the field. Can be one of CALENDAR, CHECKS,
	 * COMBOBOX, HTML_AREA, IMAGE_MEDIA, PASSWORD, RADIOS, RTF_AREA, TEXT_AREA,
	 * TEXT_FIELD, TYPE_AHEAD, LIST_BOX, MULTISELECT_LISTBOX or SPINNER.
	 */
	@ServoyMobile
	int getDisplayType();

	void setDisplayType(int arg);

	/**
	 * The text that is displayed in field when the field doesn't have a text value.
	 */
	@ServoyMobile
	String getPlaceholderText();

	void setPlaceholderText(String arg);

}