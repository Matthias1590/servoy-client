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


import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A relation (between 2 tables on one or more key column pairs)
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Relation extends AbstractBase implements ISupportChilds, ISupportUpdateableName, ISupportHTMLToolTipText, ISupportContentEquals, ICloneable,
	IRelation
{
	/*
	 * All 1-n providers for this class
	 */
	private transient IDataProvider[] primary;
	private transient Column[] foreign;
	private transient int[] operators;

	/**
	 * Constructor I
	 */
	Relation(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.RELATIONS, parent, element_id, uuid);
	}

	public boolean isRuntimeReadonly()
	{
		return primary != null || foreign != null || operators != null;
	}

	public void setChanged(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		isChanged = true;
		IDataProvider[] dataproviders = getPrimaryDataProviders(dataProviderHandler);
		int[] ops = getOperators();
		Column[] columns = getForeignColumns();
		createNewRelationItems(dataproviders, ops, columns);

	}

	/*
	 * _____________________________________________________________ Methods from IPersist
	 */

	/*
	 * _____________________________________________________________ Methods for relation column handling
	 */

	public boolean checkIfRelationItemsValid(IDataProvider[] primaryDataProvider, Column[] foreignColumns) throws RepositoryException
	{
		if (primaryDataProvider == null || primaryDataProvider.length == 0 || foreignColumns == null || foreignColumns.length == 0)
		{
			throw new RepositoryException("one of the arguments is null or is an empty array"); //$NON-NLS-1$
		}
		for (Column column : foreignColumns)
		{
			if (!column.getTable().getName().equalsIgnoreCase(getForeignTableName()))
			{
				throw new RepositoryException("one of the arguments has another tablename than the ones defined on creation of the relations"); //$NON-NLS-1$
			}
		}
		return true;
	}

	public void createNewRelationItems(IDataProvider[] primaryDataProvider, int[] ops, Column[] foreignColumns) throws RepositoryException
	{
		if (!isParentRef()) checkIfRelationItemsValid(primaryDataProvider, foreignColumns);
		List<IPersist> allobjects = getAllObjectsAsList();

		int i = 0;
		if (primaryDataProvider != null)
		{
			for (; i < primaryDataProvider.length; i++)
			{
				RelationItem obj = null;
				if (i < allobjects.size())
				{
					obj = (RelationItem)allobjects.get(i);
				}
				else
				{
					obj = (RelationItem)getRootObject().getChangeHandler().createNewObject(this, IRepository.RELATION_ITEMS);
					addChild(obj);
				}

				//set all the required properties
				obj.setPrimaryDataProviderID(primaryDataProvider[i].getDataProviderID());
				obj.setOperator(ops[i]);
				obj.setForeignColumnName(foreignColumns[i].getName());
			}
		}

		//delete the once which are not used anymore
		if (i < allobjects.size())
		{
			IPersist[] remainder = allobjects.subList(i, allobjects.size()).toArray(new IPersist[allobjects.size() - i]);
			for (IPersist p : remainder)
			{
				((IDeveloperRepository)p.getRootObject().getRepository()).deleteObject(p);
			}
		}


		//if (relation_items.size() != 0) makeColumns(relation_items); //slow
		primary = primaryDataProvider; //faster
		foreign = foreignColumns; //faster
		operators = ops; //faster
		isGlobal = null;
		valid = null;
	}


	public RelationItem createNewRelationItem(IFoundSetManagerInternal foundSetManager, IDataProvider primaryDataProvider, int ops, Column foreignColumn)
		throws RepositoryException
	{
		if (!foreignColumn.getTable().equals(foundSetManager.getTable(getForeignDataSource())))
		{
			throw new RepositoryException("one of the arguments has another tablename than the ones defined on creation of the relations"); //$NON-NLS-1$
		}
		RelationItem obj = null;
		if (primaryDataProvider != null)
		{
			obj = (RelationItem)getRootObject().getChangeHandler().createNewObject(this, IRepository.RELATION_ITEMS);
			//set all the required properties
			obj.setPrimaryDataProviderID(primaryDataProvider.getDataProviderID());
			obj.setOperator(ops);
			obj.setForeignColumnName(foreignColumn.getName());
			addChild(obj);
		}

		primary = null;
		foreign = null;
		operators = null;
		isGlobal = null;
		valid = null;

		return obj;
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	//the repository element id can differ!
	public boolean contentEquals(Object obj)
	{
		if (obj instanceof Relation)
		{
			Relation other = (Relation)obj;
			try
			{
				List<IPersist> allobjects = getAllObjectsAsList();
				if (other.getAllObjectsAsList().size() != allobjects.size()) return false;
				for (int pos = 0; pos < allobjects.size(); pos++)
				{
					RelationItem ri = (RelationItem)allobjects.get(pos);
					RelationItem ori = (RelationItem)other.getAllObjectsAsList().get(pos);
					if (!ri.contentEquals(ori))
					{
						return false;
					}
				}

				if (!isGlobal() && (!getPrimaryTableName().equals(other.getPrimaryTableName()) || !getForeignTableName().equals(other.getForeignTableName())))
				{
					return false;
				}
				return (getName().equals(other.getName()) && getDeleteRelatedRecords() == other.getDeleteRelatedRecords() && getAllowCreationRelatedRecords() == other.getAllowCreationRelatedRecords());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		String name = Utils.toEnglishLocaleLowerCase(arg);
		validator.checkName(name, getID(), new ValidatorSearchContext(IRepository.RELATIONS), true);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, name);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * Set the name
	 * 
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, Utils.toEnglishLocaleLowerCase(arg));
	}

	/**
	 * The name of the relation. 
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the primary data source
	 */
	public void setPrimaryDataSource(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PRIMARYDATASOURCE, arg);
	}

	/**
	 * Qualified name of the primary data source. Contains both the name of the primary server
	 * and the name of the primary table.
	 */
	public String getPrimaryDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PRIMARYDATASOURCE);
	}

	/**
	 * Set the foreign data source
	 */
	public void setForeignDataSource(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FOREIGNDATASOURCE, arg);
	}

	/**
	 * Qualified name of the foreign data source. Contains both the name of the foreign
	 * server and the name of the foreign table.
	 */
	public String getForeignDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FOREIGNDATASOURCE);
	}

	/**
	 * Set the serverName1
	 * 
	 * @param arg the serverName1
	 */
	public void setPrimaryServerName(String arg)
	{
		setPrimaryDataSource(DataSourceUtils.createDBTableDataSource(arg, getPrimaryTableName()));
	}

	public String getPrimaryServerName()
	{
		String primaryDataSource = getPrimaryDataSource();
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(primaryDataSource);
		if (stn != null)
		{
			return stn[0];
		}

		// data source is not a server/table combi
		Table primaryTable = null;
		try
		{
			primaryTable = getPrimaryTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return primaryTable == null ? null : primaryTable.getServerName();
	}

	/**
	 * Set the foreignServerName
	 * 
	 * @param arg the foreignServerName
	 */
	public void setForeignServerName(String arg)
	{
		setForeignDataSource(DataSourceUtils.createDBTableDataSource(arg, getForeignTableName()));
	}

	public String getForeignServerName()
	{
		String foreignDataSource = getForeignDataSource();
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return stn[0];
		}

		// data source is not a server/table combi
		Table foreignTable = null;
		try
		{
			foreignTable = getForeignTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return foreignTable == null ? null : foreignTable.getServerName();
	}

	/**
	 * Set the tableName1
	 * 
	 * @param arg the tableName1
	 */
	public void setPrimaryTableName(String arg)
	{
		setPrimaryDataSource(DataSourceUtils.createDBTableDataSource(getPrimaryServerName(), arg));
	}

	public String getPrimaryTableName()
	{
		String primaryDataSource = getPrimaryDataSource();
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(primaryDataSource);
		if (stn != null)
		{
			return stn[1];
		}

		// data source is not a server/table combi
		Table primaryTable = null;
		try
		{
			primaryTable = getPrimaryTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return primaryTable == null ? null : primaryTable.getName();
	}

	public Table getPrimaryTable() throws RepositoryException
	{
		return getTable(getPrimaryDataSource());
	}

	public IServer getPrimaryServer() throws RepositoryException, RemoteException
	{
		String primaryDataSource = getPrimaryDataSource();
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(primaryDataSource);
		if (stn != null)
		{
			return getRootObject().getServer(stn[0]);
		}

		ITable primaryTable = getPrimaryTable();
		if (primaryTable != null)
		{
			return getRootObject().getServer(primaryTable.getServerName());
		}
		return null;
	}

	/**
	 * Set the foreignTableName
	 * 
	 * @param arg the foreignTableName
	 */
	public void setForeignTableName(String arg)
	{
		setForeignDataSource(DataSourceUtils.createDBTableDataSource(getForeignServerName(), arg));
	}

	public String getForeignTableName()
	{
		String foreignDataSource = getForeignDataSource();
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return stn[1];
		}

		// data source is not a server/table combi
		Table foreignTable = null;
		try
		{
			foreignTable = getForeignTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return foreignTable == null ? null : foreignTable.getName();
	}

	/**
	 * A String which specified a set of sort options for the initial sorting of data
	 * retrieved through this relation.
	 * 
	 * Has the form "column_name asc, another_column_name desc, ...".
	 */
	public String getInitialSort()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT);
	}

	/**
	 * Sets the sortOptions.
	 * 
	 * @param initialSort The sortOptions to set
	 */
	public void setInitialSort(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT, arg);
	}


	/**
	 * Gets the duplicateRelatedRecords.
	 * 
	 * @return Returns a boolean
	 */
	public boolean getDuplicateRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DUPLICATERELATEDRECORDS).booleanValue();
	}

	/**
	 * Sets the duplicateRelatedRecords.
	 * 
	 * @param duplicateRelatedRecords The options to set
	 */
	public void setDuplicateRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DUPLICATERELATEDRECORDS, arg);
	}

	/**
	 * Set the deleteRelatedRecords
	 * 
	 * @param arg the deleteRelatedRecords
	 */
	public void setDeleteRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DELETERELATEDRECORDS, arg);
	}

	/**
	 * Flag that tells if related records should be deleted or not when a parent record is deleted.
	 * 
	 * The default value of this flag is "false".
	 */
	public boolean getDeleteRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DELETERELATEDRECORDS).booleanValue();
	}

	/**
	 * Set the existsInDB
	 * 
	 * @param arg the existsInDB
	 */
	public void setExistsInDB(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXISTSINDB, arg);
	}

	/**
	 * Get the existsInDB
	 * 
	 * @return the existsInDB
	 */
	public boolean getExistsInDB()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_EXISTSINDB).booleanValue();
	}

	public void setAllowCreationRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWCREATIONRELATEDRECORDS, arg);
	}

	/**
	 * Flag that tells if related records can be created through this relation.
	 * 
	 * The default value of this flag is "false".
	 */
	public boolean getAllowCreationRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWCREATIONRELATEDRECORDS).booleanValue();
	}

	public void setAllowParentDeleteWhenHavingRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS, arg);
	}

	/**
	 * Flag that tells if the parent record can be deleted while it has related records.
	 * 
	 * The default value of this flag is "true".
	 */
	public boolean getAllowParentDeleteWhenHavingRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS).booleanValue();
	}

	public int getItemCount()
	{
		return getAllObjectsAsList().size();
	}

	public IDataProvider[] getPrimaryDataProviders(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary == null)
		{
			makePrimaryDataProviders(dataProviderHandler);
		}
		return primary;
	}

	public Column[] getForeignColumns() throws RepositoryException
	{
		if (foreign == null)
		{
			makeForeignColumns();
		}
		return foreign;
	}

	private Table getTable(String dataSource) throws RepositoryException
	{
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			try
			{
				IServer server = getRootObject().getServer(stn[0]);
				if (server == null)
				{
					valid = Boolean.FALSE;
					throw new RepositoryException(Messages.getString("servoy.exception.serverNotFound", new Object[] { stn[0] })); //$NON-NLS-1$
				}
				return (Table)server.getTable(stn[1]);
			}
			catch (RemoteException e)
			{
				Debug.error(e);
				return null;
			}
		}

		// not a server/table combi, ask the current clients foundset manager
		if (J2DBGlobals.getServiceProvider() != null)
		{
			return (Table)J2DBGlobals.getServiceProvider().getFoundSetManager().getTable(dataSource);
		}

		// developer
		return null;
	}

	public Table getForeignTable() throws RepositoryException
	{
		return getTable(getForeignDataSource());
	}

	public IServer getForeignServer() throws RepositoryException, RemoteException
	{
		String foreignDataSource = getForeignDataSource();
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return getRootObject().getServer(stn[0]);
		}

		ITable foreignTable = getForeignTable();
		if (foreignTable != null)
		{
			return getRootObject().getServer(foreignTable.getServerName());
		}
		return null;
	}

	public boolean isUsableInSort()
	{
		if (!isUsableInSearch())
		{
			return false;
		}
		if (getJoinType() != ISQLJoin.INNER_JOIN)
		{ // outer joins icw or-null modifiers do not work (oracle) or looses outer join (ansi)
			for (int operator : getOperators())
			{
				if ((operator & ISQLCondition.ORNULL_MODIFIER) != 0)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isUsableInSearch()
	{
		return isValid() && !isMultiServer() && !isGlobal();
	}

	public boolean isMultiServer()
	{
		String primaryServerName = getPrimaryServerName();
		return (primaryServerName == null || !primaryServerName.equals(getForeignServerName()));
	}

	//creates real object relations also does some checks
	private void makePrimaryDataProviders(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary != null) return;

		List<IPersist> allobjects = getAllObjectsAsList();
		IDataProvider[] p = new IDataProvider[allobjects.size()];
		Table pt = null;
		RepositoryException exception = null;

		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			String pdp = ri.getPrimaryDataProviderID();
			IDataProvider pc = null;

			if (pdp.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				pc = dataProviderHandler.getGlobalDataProvider(pdp);
				if (pc != null)
				{
					p[pos] = pc;
				}
				else
				{
					if (exception == null) exception = new RepositoryException(Messages.getString(
						"servoy.relation.error.dataproviderDoesntExist", new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() })); //$NON-NLS-1$
				}
			}
			else
			{
				if (pt == null)
				{
					pt = getPrimaryTable();
				}

				if (pt != null)
				{
					pc = dataProviderHandler.getDataProviderForTable(pt, pdp);
					if (pc != null)
					{
						p[pos] = pc;
					}
					else
					{
						if (exception == null) exception = new RepositoryException(
							Messages.getString(
								"servoy.relation.error.dataproviderDoesntExist", new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() })); //$NON-NLS-1$
					}
				}
				else
				{
					if (exception == null) exception = new RepositoryException(Messages.getString(
						"servoy.relation.error.tableDoesntExist", new Object[] { getPrimaryTableName(), getForeignTableName(), getName() })); //$NON-NLS-1$
				}
			}
		}
		primary = p;

		if (exception != null)
		{
			valid = Boolean.FALSE;
			throw exception;
		}

	}

	private void makeForeignColumns() throws RepositoryException
	{
		if (foreign != null) return;

		List<IPersist> allobjects = getAllObjectsAsList();
		Column[] f = new Column[allobjects.size()];
		Table ft = null;
		RepositoryException exception = null;

		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			if (ft == null)
			{
				ft = getTable(getForeignDataSource());
			}

			if (ft != null)
			{
				Column fc = ft.getColumn(ri.getForeignColumnName());
				if (fc != null)
				{
					f[pos] = fc;
				}
				else
				{
					if (exception == null) exception = new RepositoryException(Messages.getString("servoy.relation.error.dataproviderDoesntExist", //$NON-NLS-1$
						new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() }));
				}
			}
			else
			{
				if (exception == null) exception = new RepositoryException(Messages.getString("servoy.relation.error.tableDoesntExist", //$NON-NLS-1$
					new Object[] { getPrimaryTableName(), getForeignTableName(), getName() }));
			}
		}
		foreign = f;

		if (exception != null)
		{
			valid = Boolean.FALSE;
			throw exception;
		}
	}

	public boolean isValid()
	{
		if (valid == null && getForeignDataSource() != null)
		{
			try
			{
				IServer server = getForeignServer();
				valid = server != null && server.isValid() && getForeignTable() != null ? Boolean.TRUE : Boolean.FALSE;
			}
			catch (Exception e)
			{
				valid = Boolean.FALSE;
			}
		}
		// default to true
		if (valid == null) return true;
		return valid.booleanValue();
	}

	public Boolean valid = null;

	public void setValid(boolean b)
	{
		valid = b ? Boolean.TRUE : Boolean.FALSE;
		if (b)//clear so they are checked again
		{
			primary = null;
			foreign = null;
			operators = null;
			isGlobal = null;
		}
	}

	public int[] getOperators()
	{
		if (operators == null)
		{
			List<IPersist> allobjects = getAllObjectsAsList();
			int size = 0;
			if (primary != null)
			{
				size = primary.length;
			}
			else
			{
				size = allobjects.size();
			}
			operators = new int[size];
			if (allobjects != null)
			{
				for (int pos = 0; pos < allobjects.size(); pos++)
				{
					RelationItem ri = (RelationItem)allobjects.get(pos);
					operators[pos] = ri.getOperator();
				}
			}
		}
		return operators;
	}

	public boolean isParentRef()
	{
		String primaryDataSource = getPrimaryDataSource();
		return primaryDataSource != null && primaryDataSource.equals(getForeignDataSource()) && getAllObjectsAsList().size() == 0;
	}

	/**
	 * Does the relation always relate to the same record?
	 * 
	 * @return true if the relation is a FK->PK relation on the same data source.
	 * @throws RepositoryException
	 */
	public boolean isExactPKRef(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		String primaryDataSource = getPrimaryDataSource();
		return primaryDataSource != null && primaryDataSource.equals(getForeignDataSource()) // same data source
			&& isFKPKRef() // FK to itself
			&& Arrays.equals(getPrimaryDataProviders(dataProviderHandler), getForeignColumns());
	}

	/**
	 * Does the relation define a FK->PK relation?
	 * 
	 * @throws RepositoryException
	 */
	public boolean isFKPKRef() throws RepositoryException
	{
		getForeignColumns();
		if (foreign == null || foreign.length == 0)
		{
			return false;
		}

		getOperators();
		for (int element : operators)
		{
			if (element != ISQLCondition.EQUALS_OPERATOR)
			{
				return false;
			}
		}

		return Arrays.equals(foreign[0].getTable().getRowIdentColumns().toArray(), foreign);
	}

	public String checkKeyTypes(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary == null)
		{
			//make sure they are loaded
			getPrimaryDataProviders(dataProviderHandler);
			getForeignColumns();
		}

		if (primary != null && foreign != null)
		{
			for (int i = 0; i < primary.length; i++)
			{
				if (primary[i] == null || foreign[i] == null)
				{
					return Messages.getString("servoy.relation.error"); //$NON-NLS-1$
				}

				int primaryType = Column.mapToDefaultType(primary[i].getDataProviderType());
				int foreignType = Column.mapToDefaultType(foreign[i].getDataProviderType());
				if (primaryType == IColumnTypes.INTEGER && foreignType == IColumnTypes.NUMBER)
				{
					continue; //allow integer to number mappings
				}
				if (primaryType == IColumnTypes.NUMBER && foreignType == IColumnTypes.INTEGER)
				{
					continue; //allow number to integer mappings
				}
				if (foreignType == IColumnTypes.INTEGER && primary[i] instanceof AbstractBase &&
					"Boolean".equals(((AbstractBase)primary[i]).getSerializableRuntimeProperty(IScriptProvider.TYPE))) //$NON-NLS-1$
				{
					continue; //allow boolean var to number mappings
				}
				if (primaryType != foreignType)
				{
					return Messages.getString(
						"servoy.relation.error.typeDoesntMatch", new Object[] { primary[i].getDataProviderID(), foreign[i].getDataProviderID() }); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	private transient Boolean isGlobal;

	public boolean isGlobal()
	{
		if (isGlobal == null)
		{
			isGlobal = Boolean.valueOf(isGlobalEx());
		}
		return isGlobal.booleanValue();
	}

	/**
	 * @return true if entirely global
	 */
	private boolean isGlobalEx()
	{
		if (!isValid()) return false;//don't know

		List<IPersist> allobjects = getAllObjectsAsList();
		if (allobjects.size() == 0) return false;
		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			String pdp = ri.getPrimaryDataProviderID();
			if (!pdp.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				return false;
			}
		}
		return true;
	}

	public String toHTML()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>Name(solution): <b>"); //$NON-NLS-1$
		sb.append(getName());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(getRootObject().getName());
		sb.append(')');
		if (isGlobal())
		{
			sb.append("</b><br>Global relation <b>"); //$NON-NLS-1$
		}
		sb.append("</b><br><br>From: <b>"); //$NON-NLS-1$
		sb.append(getPrimaryServerName());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(getPrimaryTableName());
		sb.append("</b><br>To: <b>"); //$NON-NLS-1$
		sb.append(getForeignServerName());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(getForeignTableName());
		sb.append("</b><br>"); //$NON-NLS-1$
		sb.append("<br>"); //$NON-NLS-1$
		List<IPersist> allobjects = getAllObjectsAsList();
		int size = allobjects.size();
		for (int i = 0; i < size; i++)
		{
			RelationItem ri = (RelationItem)allobjects.get(i);
			sb.append(ri.getPrimaryDataProviderID());
			sb.append(" <font color=\"red\">"); //$NON-NLS-1$
			sb.append(RelationItem.getOperatorAsString(ri.getOperator()).replaceAll("<", "&lt;")); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("</font> "); //$NON-NLS-1$
			sb.append(ri.getForeignColumnName());
			if (i < size - 1) sb.append("<br>"); //$NON-NLS-1$
		}
		sb.append("</html>"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * The join type that is performed between the primary table and the foreign table.
	 * Can be "inner join" or "left outer join".
	 */
	public int getJoinType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_JOINTYPE).intValue();
	}

	public void setJoinType(int JoinType)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JOINTYPE, JoinType);
	}
}
