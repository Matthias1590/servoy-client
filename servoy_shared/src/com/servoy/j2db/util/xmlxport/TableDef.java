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

package com.servoy.j2db.util.xmlxport;

import java.io.Serializable;
import java.util.ArrayList;

import com.servoy.j2db.persistence.ITable;

public class TableDef implements Serializable, DBIDefinition
{

	public static final String PROP_TABLE_TYPE = "tableType"; //$NON-NLS-1$
	public static final String PROP_COLUMNS = "columns"; //$NON-NLS-1$
	public static final String HIDDEN_IN_DEVELOPER = "hiddenInDeveloper"; //$NON-NLS-1$
	public static final String IS_META_DATA = "isMetaData"; //$NON-NLS-1$

	public String name = null;
	public String createScript = null;
	public String primaryKey = null;
	public boolean hiddenInDeveloper = false;
	public Boolean isMetaData = Boolean.FALSE;
	public ArrayList<ColumnInfoDef> columnInfoDefSet = new ArrayList<ColumnInfoDef>(); // this should be a list, otherwise column creation order is broken
	public int tableType = ITable.TABLE;

	public String dbiFileContents = null;

	/**
	 * @return the dbiFileContents
	 */
	public String getDbiFileContents()
	{
		return dbiFileContents;
	}
}
