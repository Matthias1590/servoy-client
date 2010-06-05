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
package com.servoy.j2db.dataprocessing;


import javax.swing.text.Document;

import com.servoy.j2db.util.ITagResolver;

/**
 * Convenient interface to tie a implementing jComponent easily to a dataAdapter
 * 
 * @author jblok
 */
public interface IDisplayData extends IDisplay//for a dataprovider!
{
	public Object getValueObject();

	public void setValueObject(Object data);

	public boolean needEditListner();

	/**
	 * This makes a display no longer updateable,needFocusListner should return false used by display which uses multiple values to display information typical
	 * use a dataprovider with a letter which contains %%tags%% where tags are other dataprovider names, the tags are replaced with the values
	 */
	public boolean needEntireState();

	public void setNeedEntireState(boolean needEntireState);

	public void addEditListener(IEditListener editListener);

	public String getDataProviderID();

	public void setDataProviderID(String dataProviderID);

	public Document getDocument();//for undo listner

	public String getFormat();//needed to be able to format searches

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal);//to trigger onChangeMethod (not all displays have own property change impl)

	public void setValueValid(boolean valid, Object oldVal); //should request focus and again during stopedit if valid == false

	public boolean isValueValid();

	public void setTagResolver(ITagResolver resolver);
}
