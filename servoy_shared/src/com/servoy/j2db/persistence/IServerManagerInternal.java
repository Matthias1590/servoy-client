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

import java.io.File;
import java.sql.Driver;


/**
 * IServerManager interface extended with internal methods.
 */
public interface IServerManagerInternal extends IServerManager
{

	IServer deleteServer(ServerConfig oldServerConfig);

	IServer createServer(ServerConfig newServerConfig);

	String validateServerConfig(String oldServerName, ServerConfig serverConfig);

	void addServerConfigListener(IServerConfigListener serverConfigSyncer);

	void removeServerConfigListener(IServerConfigListener logServerListener);

	ISequenceProvider getGlobalSequenceProvider();

	void reloadServersTables() throws RepositoryException;

	File getDriverDir();

	Driver loadDriver(String driverClassName, String url) throws Exception;

	IServer getServer(String string);

	IServer getLogServer();

	String getLogServerName();

	void testServerConfigConnection(ServerConfig serverConfig, int i) throws Exception;

	void setLogServerName(String currentServerName);

	void saveServerConfig(String oldServerName, ServerConfig serverConfig) throws RepositoryException;

	String[] getKnownDriverClassNames();

	ServerConfig[] getServerConfigs();

	boolean logTableExists();

	void removeServerListener(IServerListener serverListener);

	void addServerListener(IServerListener serverListener);

	ISequenceProvider getSequenceProvider(String name);

	IColumnInfoManager[] getColumnInfoManagers(String name);

	ServerConfig getServerConfig(String name);

	IServer getRepositoryServer() throws RepositoryException;

	void addGlobalColumnInfoProvider(IColumnInfoProvider cip);

	void removeGlobalColumnInfoProvider(IColumnInfoProvider cip);

	void setGlobalColumnInfoProviders(IServerInternal server, IDeveloperRepository rep, String clientId);

	void setGlobalSequenceProvider(ISequenceProvider sm);
}
