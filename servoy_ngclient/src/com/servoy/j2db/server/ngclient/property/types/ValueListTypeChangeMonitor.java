/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.IChangeListener;

import com.servoy.j2db.util.Debug;

/**
 * @author acostescu
 */
public class ValueListTypeChangeMonitor
{

	private IChangeListener changeNotifier;
	private boolean changed;

	public void markFullyChanged(boolean notify)
	{
		if (!changed)
		{
			changed = true;
			if (notify && changeNotifier != null) changeNotifier.valueChanged();
		}
	}

	public void clearChanges()
	{
		changed = false;
	}

	public boolean isChanged()
	{
		return changed;
	}

	public void notifyOfChange()
	{
		Debug.warn("RF valuelisttypechangemonitor " + changed + " " + changeNotifier);
		if (changed && changeNotifier != null) changeNotifier.valueChanged();
	}

	/**
	 * @param changeNotifier
	 */
	public void setChangeNotifier(IChangeListener changeNotifier)
	{
		this.changeNotifier = changeNotifier;
	}

}
