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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.util.ITransactionConnection;


/**
 * IServer with internal features (only accessible from application server).
 * @author rgansevles
 *
 */
public interface IServerInternal
{
	void flushTables(List<Table> tabelList) throws RepositoryException;

	Table createNewTable(IValidateName validator, String tableName) throws RepositoryException;

	Table createNewTable(IValidateName validator, String nm, boolean testSQL) throws RepositoryException;

	Table createNewTable(IValidateName validator, Table otherServerTable) throws RepositoryException;

	void reloadTables() throws RepositoryException;

	boolean hasTable(String tableName) throws RepositoryException;

	String[] syncTableObjWithDB(Table t, boolean createMissingServoySequences, boolean createMissingDBSequences, Table templateTable)
		throws RepositoryException, SQLException;

	void syncWithExternalTable(String tableName, Table externalTable) throws RepositoryException;

	void syncColumnSequencesWithDB(Table t) throws RepositoryException;

	String getServerURL();

	ServerConfig getConfig();

	String[] removeTable(Table t) throws SQLException, RepositoryException;

	void removeTable(String tableName) throws SQLException, RepositoryException;

	void testConnection(int i) throws Exception;

	void flagValid();

	void flagInvalid();

	int getState();

	void fireStateChanged(int oldState, int state);

	boolean checkIfTableExistsInDatabase(Connection connection, String tableName);

	void duplicateColumnInfo(ColumnInfo sourceColumnInfo, ColumnInfo targetColumnInfo);

	boolean updateColumnInfo(QueryColumn queryColumn) throws RepositoryException;

	void updateAllColumnInfo(Table table) throws RepositoryException;

	void refreshTable(String name) throws RepositoryException;

	void removeTableListener(ITableListener tableListener);

	void addTableListener(ITableListener tableListener);

	Object getNextSequence(String tableName, String columnName) throws RepositoryException;

	boolean supportsSequenceType(int i, Column column) throws Exception;

	String toHTML();

	IServerManagerInternal getServerManager();

	String getName();

	List<String> getViewNames(boolean hideTempViews) throws RepositoryException;

	List<String> getTableNames(boolean hideTempTables) throws RepositoryException;

	List<String> getTableAndViewNames(boolean hideTemporary) throws RepositoryException;

	Table getTable(String tableName) throws RepositoryException;

	IRepository createRepositoryTables() throws RepositoryException;

	IRepository getRepository() throws RepositoryException;

	Table getRepositoryTable(String name) throws RepositoryException;

	Table createNewTable(IValidateName nameValidator, Table selectedTable, String tableName) throws RepositoryException;

	boolean isTableLoaded(String tableName);

	void reloadTableColumnInfo(Table t) throws RepositoryException;

	void reloadServerInfo();

	boolean isValid();

	ITransactionConnection getConnection() throws SQLException, RepositoryException;

	ITransactionConnection getUnmanagedConnection() throws SQLException, RepositoryException;

	QuerySet getSQLQuerySet(ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve, boolean forceQualifyColumns)
		throws RepositoryException;

	boolean hasMissingDBSequences(Table table) throws SQLException, RepositoryException;

	String[] createMissingDBSequences(Table table) throws SQLException, RepositoryException;

	String getDialectClassName();

	DataSource getDataSource() throws Exception;

	String getIndexDropString(Connection connection, Table t, String indexName) throws SQLException;

	String getIndexCreateString(Connection connection, Table t, String indexName, Column[] indexColumns, boolean unique) throws SQLException;

	Connection getRawConnection() throws SQLException, RepositoryException;

	/**
	 * Marks a table as being 'hidden' (or not) in developer.
	 * Hidden tables will not be suggested to the developer and warning problem markers will be created if they are used.
	 * @param tableName the name of the table.
	 * @param hiddenInDeveloper if it should be hidden or not.
	 */
	void setTableMarkedAsHiddenInDeveloper(String tableName, boolean hiddenInDeveloper);

	/**
	 * Tells if a table is marked as 'hidden' in developer.
	 * If the table's structure has not yet been read from DB this will not need to load it.
	 * 
	 * @param tableName the name of the table
	 * @return if it is hidden or not.
	 */
	boolean isTableMarkedAsHiddenInDeveloper(String tableName);

	List<String> getTableAndViewNames(boolean hideTempTables, boolean hideHiddenInDeveloper) throws RepositoryException;

}
