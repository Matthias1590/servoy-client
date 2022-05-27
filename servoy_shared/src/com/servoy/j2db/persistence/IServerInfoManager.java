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
package com.servoy.j2db.persistence;

import java.util.HashMap;

import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * Interface for repository component that manages the column info and server settings.
 *
 * @author jblok
 *
 */
public interface IServerInfoManager extends IColumnInfoProvider
{
	void createNewColumnInfo(Column c, boolean createMissingServoySequence) throws RepositoryException;

	void removeColumnInfo(Column c) throws RepositoryException;

	void removeAllColumnInfo(ITable t) throws RepositoryException;

	void updateAllColumnInfo(ITable t) throws RepositoryException;

	void setTableColumnInfos(ITable t, HashMap<String, ColumnInfoDef> columnInfoDefinitions) throws RepositoryException;

	// Server methods

	ServerSettings loadServerSettings(String serverName) throws RepositoryException;

	void updateServerSettings(String serverName, ServerSettings serverSettings) throws RepositoryException;
}
