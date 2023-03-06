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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.util.Debug;

/**
 * A data adapter list that can be used to work with records of a foundset typed property.<br/>
 * As one would normally change/go over records in data adapter list to generate contents for each record,
 * this object keep track of when such record changes occur. This can be useful when for example you would not want to interpret
 * properties as changed/dirty only because you are changing the current record of the DAL to retrieve values.
 *
 * @author acostescu
 */
public class FoundsetDataAdapterList extends DataAdapterList
{

	private boolean keepQuiet = false;
	private Object onlyFireListenersForPropertyValue;
	private List<IDataLinkedPropertyRegistrationListener> dataLinkedPropertyRegistrationListeners;
	private final FoundsetTypeSabloValue foundsetTypeSabloValue;
	private Set<String> globalDataproviders;

	public FoundsetDataAdapterList(IWebFormController formController, FoundsetTypeSabloValue foundsetTypeSabloValue)
	{
		super(formController);
		this.foundsetTypeSabloValue = foundsetTypeSabloValue;
	}

	public void setRecordQuietly(IRecord record)
	{
		setRecordQuietly(record, false);
	}

	public void setRecordQuietly(IRecord record, boolean skipIfAlreadySet)
	{
		if (skipIfAlreadySet && record == getRecord()) return;

		keepQuiet = true;
		try
		{
			super.setRecord(record, false);
		}
		finally
		{
			keepQuiet = false;
		}
	}

	@Override
	protected boolean shouldIgnoreRecordChange(IRecord oldRecord, IRecord newRecord)
	{
		return false;
	}

	public boolean isQuietRecordChangeInProgress()
	{
		return keepQuiet;
	}

	@Override
	public void addDataLinkedProperty(IDataLinkedPropertyValue propertyValue, TargetDataLinks targetDataLinks)
	{
		super.addDataLinkedProperty(propertyValue, targetDataLinks);
		if (dataLinkedPropertyRegistrationListeners != null)
		{
			for (IDataLinkedPropertyRegistrationListener l : dataLinkedPropertyRegistrationListeners)
			{
				l.dataLinkedPropertyRegistered(propertyValue, targetDataLinks);
			}
		}
	}

	@Override
	public void removeDataLinkedProperty(IDataLinkedPropertyValue propertyValue)
	{
		super.removeDataLinkedProperty(propertyValue);
		if (dataLinkedPropertyRegistrationListeners != null)
		{
			for (IDataLinkedPropertyRegistrationListener l : dataLinkedPropertyRegistrationListeners)
			{
				l.dataLinkedPropertyUnregistered(propertyValue);
			}
		}
	}

	public void addDataLinkedPropertyRegistrationListener(IDataLinkedPropertyRegistrationListener listener)
	{
		if (dataLinkedPropertyRegistrationListeners == null) dataLinkedPropertyRegistrationListeners = new ArrayList<IDataLinkedPropertyRegistrationListener>();
		if (!dataLinkedPropertyRegistrationListeners.contains(listener)) dataLinkedPropertyRegistrationListeners.add(listener);
	}

	public void removeDataLinkedPropertyRegistrationListener(IDataLinkedPropertyRegistrationListener listener)
	{
		if (dataLinkedPropertyRegistrationListeners != null)
		{
			dataLinkedPropertyRegistrationListeners.remove(listener);
			if (dataLinkedPropertyRegistrationListeners.size() == 0) dataLinkedPropertyRegistrationListeners = null;
		}
	}

	public void resetDALToSelectedIndexQuietly()
	{
		// see https://support.servoy.com/browse/SVY-11537; DataproviderTypeSabloValues do listen to related data but only on the row in the foundset DAL
		IFoundSetInternal foundset;
		if (getRecord() != null) foundset = getRecord().getParentFoundSet();
		else foundset = null;

		if (foundset != null && foundset.getSize() > 0)
		{
			IRecord selectedRecord = foundset.getRecord(foundset.getSelectedIndex());
			setRecordQuietly(selectedRecord, true);
		}
		else
		{
			setRecordQuietly(null, true); // to make sure DAL is not listening to records that are no longer in the foundset
		}
	}

	@Override
	public void valueChanged(ModificationEvent e)
	{
		// if this is a global modification event and we need to react on that one
		// then just mark the foundset as fully changed.
		if (globalDataproviders != null && e.getName() != null && globalDataproviders.contains(e.getName()))
		{
			foundsetTypeSabloValue.changeMonitor.viewPortCompletelyChanged();
			// it would be better if we could just mark the actual full column viewport as changed..
			// then we need to have here or be abe to get the ViewportDataChangeMonitor of the FoundsetLinkedTypeSabloValue
			// but then the clients also need to react on that specific change..
			// or we call queuCellChange for every cell of that column (so for every row)
		}
		else
		{
			IRecordInternal r = getRecord();
			// if the current record is already not in the foundset anymore, just ignore this change.
			// this is because the row of the record could still fire a ModificationEvent change at the moment the record is removed from the foundset.
			if (r == null || r.getParentFoundSet().getRecordIndex(r) != -1)
			{
				super.valueChanged(e);
			}
		}
	}

	@Override
	protected void setupModificationListener(String dataprovider)
	{
		super.setupModificationListener(dataprovider);

		if (isGlobalDataprovider(dataprovider))
		{
			if (globalDataproviders == null) globalDataproviders = new HashSet<String>(3);
			globalDataproviders.add(dataprovider);
		}
	}

	public void onlyFireListenersForProperty(Object propertyValue)
	{
		this.onlyFireListenersForPropertyValue = propertyValue;
	}

	public void resumeNormalListeners()
	{
		this.onlyFireListenersForPropertyValue = null;
	}

	@Override
	protected Object getValueObjectForTagResolver(IRecord recordToUse, String dataProviderId)
	{
		Object valueObject = super.getValueObjectForTagResolver(recordToUse, dataProviderId);
		// log if the name is not valid for the foundset, valueObject is set to null when an invalid relation is used
		if ((valueObject == Scriptable.NOT_FOUND || valueObject == null) && recordToUse != null)
		{
			boolean validDataprovider = false;
			if (valueObject == null)
			{
				validDataprovider = ((IFoundSetInternal)recordToUse.getParentFoundSet()).isValidRelation(dataProviderId);
			}
			if (!validDataprovider)
			{
				Debug.warn("Invalid dataprovider " + dataProviderId + " set for foundset property type " + this.foundsetTypeSabloValue);
			}
		}
		return valueObject;
	}

	@Override
	protected boolean canFirePropertyValueListener(IDataLinkedPropertyValue propertyValue)
	{
		return this.onlyFireListenersForPropertyValue == null || this.onlyFireListenersForPropertyValue == propertyValue;
	}

	@Override
	public String toString()
	{
		return "FoundsetDAL[form:" + getForm() + ",foundsettype:" + this.foundsetTypeSabloValue + "]";
	}
}
