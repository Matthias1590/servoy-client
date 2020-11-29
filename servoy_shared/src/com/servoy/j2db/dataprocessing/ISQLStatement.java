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


import java.io.Serializable;

import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;

/**
 * Object that must be Serializable and contains the insert or update info.
 * @author		jblok
 */
public interface ISQLStatement extends Serializable, ISQLActionTypes
{
	public int getAction();

	/**
	 * Get the server name.
	 * @return String
	 */
	public String getServerName();

	public void setServerName(String name);

	/**
	 * Get the table name.
	 * @return String
	 */
	public String getTableName();

	/**
	 * Get the pk columns.
	 * @return the pks
	 */
	public IDataSet[] getPKs();


	public default IDataSet getPKs(int index)
	{
		return getPKs() == null ? null : getPKs()[index];
	}

	/**
	 * Get the update.
	 * @return ISQLUpdate update
	 */
	public ISQLUpdate getUpdate();

	/**
	 * Get the transactionID.
	 * @return String the id ,null if none
	 */
	public String getTransactionID();

	/**
	 * @return
	 */
	public boolean usedIdentity();

	public boolean isOracleFixTrackingData();

	/**
	 * Set the update count for checking
	 */
	public void setExpectedUpdateCount(int expectedUpdateCount);

	public ISQLSelect getRequerySelect();

	public static int REGULAR_DATA_TYPE = 0;
	public static int I18N_DATA_TYPE = 1;

	public int getDataType();
}
