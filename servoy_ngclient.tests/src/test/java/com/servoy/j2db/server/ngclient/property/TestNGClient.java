/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.dataprocessing.Blob;
import com.servoy.j2db.dataprocessing.BroadcastFilter;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.BufferedDataSetInternal;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IClient;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.ISQLStatement;
import com.servoy.j2db.dataprocessing.ITrackingSQLStatement;
import com.servoy.j2db.dataprocessing.QueryData;
import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITeamRepository;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.QueryString;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IClientManager;
import com.servoy.j2db.server.shared.IPerformanceRegistry;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.server.shared.PerformanceAggregator;
import com.servoy.j2db.server.shared.PerformanceData;
import com.servoy.j2db.server.shared.PerformanceTiming;
import com.servoy.j2db.server.shared.PerformanceTimingAggregate;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * @author Johan
 *
 */
public class TestNGClient extends NGClient
{
	/**
	 *
	 */
	private final TestRepository tr;

	/**
	 * @param wsSession
	 * @param tr
	 */
	TestNGClient(TestRepository tr, NGClientWebsocketSession session) throws Exception
	{
		super(session, null);
		this.tr = tr;
		((NGClientWebsocketSession)getWebsocketSession()).setClient(this);
	}

	public static void initSettings()
	{
		Debug.init();
	}

	@Override
	protected IDataServer createDataServer()
	{
		return new IDataServer()
		{

			private final HashMap<String, IDataSet> dataSetMap = new HashMap<String, IDataSet>();

			@Override
			public void setBroadcastFilters(String clientId, String serverName, BroadcastFilter[] broadcastFilters) throws RemoteException
			{
			}

			@Override
			public BroadcastFilter[] getBroadcastFilters(String clientId, String serverName) throws RemoteException
			{
				return null;
			}

			@Override
			public void clearBroadcastFilters(String clientId) throws RemoteException
			{
			}

			@Override
			public void setServerMaintenanceMode(boolean maintenanceMode) throws RemoteException
			{
			}

			@Override
			public void logMessage(String msg) throws RemoteException
			{
			}

			@Override
			public boolean isInServerMaintenanceMode() throws RemoteException
			{
				return false;
			}

			@Override
			public boolean releaseLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys)
				throws RemoteException, RepositoryException
			{
				return false;
			}

			@Override
			public IDataSet acquireLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys, QuerySelect lockSelect,
				String transaction_id, ArrayList<TableFilter> filters, int chunkSize) throws RemoteException, RepositoryException
			{
				return null;
			}

			@Override
			public String startTransaction(String clientId, String server_name) throws RepositoryException, RemoteException
			{
				return null;
			}

			@Override
			public Object[] performUpdates(String clientId, ISQLStatement[] statements) throws ServoyException, RemoteException
			{
				return statements;
			}

			@Override
			public IDataSet[] performQuery(String client_id, String server_name, String transaction_id, QueryData[] array)
				throws ServoyException, RemoteException
			{
				if (array.length > 0)
				{
					String ds = array[0].getSqlSelect().getTable().getDataSource();
					if ("mem:relatedtest".equals(ds))
					{
						IDataSet set = dataSetMap.get(ds);
						IDataSet[] returnDataSet = new IDataSet[array.length];
						for (int i = 0; i < array.length; i++)
						{
							returnDataSet[i] = new BufferedDataSet();
							for (int k = 0; k < set.getRowCount(); k++)
							{
								QuerySelect sqlSelect = (QuerySelect)array[i].getSqlSelect();
								List<ISQLCondition> conditions = sqlSelect.getCondition().getAllConditions();
								SetCondition setCondition = (SetCondition)conditions.get(0);
								Object[][] value = (Object[][])((Placeholder)setCondition.getValues()).getValue();
								if (set.getRow(k)[1].equals(value[0][0]))
								{
									returnDataSet[i].addRow(new Object[] { set.getRow(k)[0], set.getRow(k)[1], set.getRow(k)[2], set.getRow(k)[3] });
								}
							}
						}
						return returnDataSet;
					}
				}

				return null;
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
				int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
			{
				return dataSetMap.values().iterator().next(); // don't know the
																// datasource,
																// just return
																// the first
																// dataset
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
				ArrayList<TableFilter> filters, boolean distinctInMemory, int startRow, int rowsToRetrieve, boolean updateIdleTimestamp)
				throws ServoyException, RemoteException
			{
				return dataSetMap.get(sqlSelect.getTable().getDataSource());
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
				int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
			{
				return dataSetMap.values().iterator().next(); // don't know the
																// datasource,
																// just return
																// the first
																// dataset
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
				ArrayList<TableFilter> filters,
				boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, ITrackingSQLStatement trackingInfo)
				throws ServoyException, RemoteException
			{
				return dataSetMap.get(sqlSelect.getTable().getDataSource());
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
				ArrayList<TableFilter> filters,
				boolean distinctInMemory, int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
			{
				IDataSet set = dataSetMap.get(sqlSelect.getTable().getDataSource());

				if (sqlSelect instanceof QuerySelect && ((QuerySelect)sqlSelect).getColumns().size() == 1)
				{
					// pk select
					int lastRow = Math.min(set.getRowCount(), startRow + rowsToRetrieve);
					BufferedDataSet ds = BufferedDataSetInternal.createBufferedDataSet(null, null, new SafeArrayList<Object[]>(0), lastRow < set.getRowCount());
					for (int i = startRow; i < lastRow; i++)
					{
						ds.addRow(new Object[] { set.getRow(i)[0] });
					}
					return ds;
				}
				return set;
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
				int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
			{
				return dataSetMap.values().iterator().next(); // don't know the
																// datasource,
																// just return
																// the first
																// dataset
			}

			@Override
			public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
				ArrayList<TableFilter> filters, boolean distinctInMemory, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
			{
				return dataSetMap.get(sqlSelect.getTable().getDataSource());
			}

			@Override
			public IDataSet performCustomQuery(String client_id, String server_name, String driverTableName, String transaction_id, ISQLSelect sqlSelect,
				ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
			{
				return dataSetMap.get(sqlSelect.getTable().getDataSource());
			}

			@Override
			public boolean notifyDataChange(String client_id, String server_name, String tableName, IDataSet pks, int action, String transaction_id)
				throws RemoteException
			{
				return false;
			}

			@Override
			public ITable insertQueryResult(String client_id, String queryServerName, String queryTid, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
				boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, String dataSource, String targetServerName, String targetTableName,
				String targetTid, ColumnType[] columnTypes, String[] pkNames) throws ServoyException, RemoteException
			{
				return null;
			}

			@Override
			public InsertResult insertDataSet(String client_id, IDataSet set, final String dataSource, String serverName, String tableName, String tid,
				ColumnType[] columnTypes, String[] pkNames, HashMap<String, ColumnInfoDef> columnInfoDefinitions) throws ServoyException, RemoteException
			{
				dataSetMap.put(dataSource, set);
				Table table = new Table(serverName, serverName, true, ITable.TABLE, null, null);
				table.setDataSource(dataSource);
				for (int i = 0; i < set.getColumnCount(); i++)
				{
					Column col = new Column(table, set.getColumnNames()[i], set.getColumnTypes()[i], 50, 50, true);
					table.addColumn(col);
					if (Arrays.binarySearch(pkNames, col.getName()) >= 0)
					{
						col.setDatabasePK(true);
					}
				}
				return new InsertResult(table, new Object[0]);
			}

			@Override
			public QuerySet getSQLQuerySet(String serverName, ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve,
				boolean forceQualifyColumns, boolean disableUseArrayForIn) throws RepositoryException, RemoteException
			{
				QuerySet qs = new QuerySet();
				qs.setSelect(new QueryString("select from test", false));
				return qs;
			}

			@Override
			public Object getNextSequence(String serverName, String tableName, String columnName, int columnInfoID, String columnInfoServer)
				throws RepositoryException, RemoteException
			{
				return null;
			}

			@Override
			public Blob getBlob(String clientId, String serverName, ISQLSelect blobSelect, ArrayList<TableFilter> filters, String tid)
				throws RepositoryException, RemoteException
			{
				return null;
			}

			@Override
			public boolean endTransactions(String client_id, String[] transaction_id, boolean commit) throws RepositoryException, RemoteException
			{
				return false;
			}

			@Override
			public void dropTemporaryTable(String client_id, String serverName, String tableName) throws RemoteException, RepositoryException
			{
			}

			@Override
			public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, ISQLUpdate sqlUpdate,
				ArrayList<TableFilter> filters) throws RemoteException
			{
				return null;
			}

			@Override
			public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, String sql,
				Object[] questiondata) throws RemoteException, RepositoryException
			{
				return null;
			}

			@Override
			public void addClientAsTableUser(String client_id, String serverName, String tableName) throws RemoteException, RepositoryException
			{
			}

			@Override
			public IDataSet[] executeProcedure(String clientId, String server_name, String tid, Procedure procedure, Object[] arguments)
				throws RepositoryException, RemoteException
			{
				return null;
			}
		};
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.NGClient#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return Locale.ENGLISH;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ClientState#createRepository()
	 */
	@Override
	protected IRepository createRepository() throws RemoteException
	{
		return tr;
	}

	@Override
	protected boolean startApplicationServerConnection()
	{
		applicationServer = new IApplicationServer()
		{

			@Override
			public ClientLogin login(Credentials credentials) throws RemoteException, RepositoryException
			{
				return null;
			}

			@Override
			public SolutionMetaData[] getSolutionDefinitions(int solutionTypeFilter) throws RemoteException, RepositoryException
			{
				return null;
			}

			@Override
			public SolutionMetaData getSolutionDefinition(String solutionName, int solutionTypeFilter) throws RemoteException, RepositoryException
			{
				return (SolutionMetaData)tr.getRootObjectMetaData(solutionName, IRepository.SOLUTIONS);
			}

			@Override
			public Remote getRemoteService(String cid, String rmiLookupName) throws RemoteException
			{
				return null;
			}

			@Override
			public SolutionMetaData[] getLoginSolutionDefinitions(SolutionMetaData solutionMetaData) throws RemoteException, RepositoryException
			{
				return null;
			}

			@Override
			public Solution getLoginSolution(SolutionMetaData mainSolution, SolutionMetaData loginSolution) throws RemoteException, RepositoryException
			{
				return null;
			}

			@Override
			public String getClientID(String user_uid, String password) throws RemoteException
			{
				return null;
			}

			@Override
			public IClientHost getClientHost() throws RemoteException
			{

				return new IClientHost()
				{
					@Override
					public void unregister(String client_id) throws RemoteException
					{
					}

					@Override
					public Object[] register(IClient c, ClientInfo clientInfo) throws RemoteException
					{
						return new Object[] { "uuid", new Integer(IClientManager.REGISTER_OK) };
					}

					@Override
					public boolean isRegistered(String client_id) throws RemoteException
					{
						return true;
					}

					@Override
					public void pushClientInfo(String clientId, ClientInfo clientInfo) throws RemoteException
					{

					}

					@Override
					public Date getServerTime(String client_id) throws RemoteException
					{
						return null;
					}

					@Override
					public String getServerId() throws RemoteException
					{
						return "";
					}
				};
			}

			@Override
			public IApplicationServerAccess getApplicationServerAccess(String clientId) throws RemoteException
			{
				return new IApplicationServerAccess()
				{

					@Override
					public void logout(String clientId) throws RemoteException, RepositoryException
					{
					}

					@Override
					public IUserManager getUserManager(String clientId) throws RemoteException
					{
						return null;
					}

					@Override
					public ITeamRepository getTeamRepository() throws RemoteException
					{
						return null;
					}

					@Override
					public IRepository getRepository() throws RemoteException
					{
						return null;
					}

					@Override
					public String[] getLicenseNames() throws RemoteException
					{
						return null;
					}

					@Override
					public IPerformanceRegistry getFunctionPerfomanceRegistry() throws RemoteException
					{
						return new IPerformanceRegistry()
						{

							@Override
							public String[] getPerformanceTimingContexts()
							{
								return null;
							}

							@Override
							public PerformanceTimingAggregate[] getPerformanceTiming(String string)
							{
								return null;
							}

							@Override
							public Date getLastCleared(String context)
							{
								return null;
							}

							@Override
							public PerformanceData getPerformanceData(String context)
							{
								return new PerformanceData(this, null, "", new PerformanceAggregator(this));
							}

							@Override
							public Map<String, PerformanceTiming[]> getActiveTimings()
							{
								return null;
							}

							@Override
							public void clearPerformanceData(String context)
							{
							}

							@Override
							public int getMaxNumberOfEntriesPerContext()
							{
								return 500;
							}

							@Override
							public void setMaxNumberOfEntriesPerContext(int maxNumberOfEntriesPerContext)
							{
							}

							@Override
							public String getId()
							{
								return "testPrfReg";
							}

						};
					}

					@Override
					public IDataServer getDataServer() throws RemoteException
					{
						return null;
					}

					@Override
					public int getClientCountForInfo(String info) throws RemoteException
					{
						return 0;
					}

					@Override
					public int getActiveClientCount(int solution_id) throws RemoteException
					{
						return 0;
					}
				};
			}

			@Override
			public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException
			{
				return null;
			}
		};
		return true;
	}

	@Override
	protected void createPluginManager()
	{
	}
}
