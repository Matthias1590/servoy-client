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

import java.io.IOException;
import java.util.List;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.InfoChannel;


/**
 * The <code>IXMLExportUserChannel</code> interface is used to send send info messages and to delegate any decisions needed during the export to the user.
 * 
 */
public interface IXMLExportUserChannel extends InfoChannel
{
	/**
	 * Return the modules names which must be included in the export. The argument is a (sorted) list of all modules which the solution references. The result
	 * should be a subset of the supplied list.
	 * 
	 * @param allModules names of all modules of exported solution. This list may be altered and returned.
	 * @return the modules names which must be included in the export.
	 */
	public <T extends List<String>> T getModuleIncludeList(T allModules);

	/**
	 * Return a protection password for export.
	 * 
	 * @return a protection password for export.
	 */
	public String getProtectionPassword();

	public boolean getExportAllTablesFromReferencedServers();

	public String getTableMetaData(ITable table) throws IOException;
}
