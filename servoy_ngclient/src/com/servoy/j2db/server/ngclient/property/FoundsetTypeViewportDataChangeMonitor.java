/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.util.Collections;

import org.sablo.IChangeListener;
import org.sablo.specification.property.ArrayOperation;

/**
 * A ViewportDataChangeMonitor that is for the foundset property type itself.
 *
 * @author acostescu
 */
public class FoundsetTypeViewportDataChangeMonitor extends ViewportDataChangeMonitor<FoundsetTypeRowDataProvider>
{

	private final FoundsetTypeSabloValue foundsetPropertyValue;

	public FoundsetTypeViewportDataChangeMonitor(IChangeListener monitor, FoundsetTypeRowDataProvider rowDataProvider,
		FoundsetTypeSabloValue foundsetPropertyValue)
	{
		super(monitor, rowDataProvider);
		this.foundsetPropertyValue = foundsetPropertyValue;
	}

	/**
	 * This gets called when other properties that are linked to this foundset properties do send CHANGE updates for a row but the foundset property itself
	 * has nothing to change (so for example only one column in one row changed, but that column is not used in the foundset property, just in a config object with a dataprovider
	 * linked to the foundset). In this case we still want to let the foundset property know about this so it can trigger the browser-side foundset listener properly - as components are interested in this situation as well.
	 *
	 * @param relativeFirstRow viewPort relative start index for given operation.
	 * @param relativeLastRow viewPort relative end index for given operation (inclusive).
	 */
	public boolean queueLinkedPropertyUpdate(int firstRelativeRowIndex, int lastRelativeRowIndex, int oldViewportSize, String columnName)
	{
		if (!rowDataProvider.isReady()) return false;

		boolean changed = !viewPortChanges.hasChanges(); // if it doesn't already have changes then it changed
		processOperation(changed, oldViewportSize,
			new ViewportOperation(firstRelativeRowIndex, lastRelativeRowIndex, ArrayOperation.CHANGE, Collections.singleton(columnName)));

		if (changed && monitor != null) monitor.valueChanged();
		return changed;
	}

	@Override
	public boolean queueCellChangeDueToColumn(int relativeRowIndex, int oldViewportSize, String columnDPName)
	{
		// if the col is a PK then yes, we do sent it to client as part of the ROW_ID_COL_KEY (see above)
		// else see if this column is used directly by the foundset property on client
		if (columnDPName == null || foundsetPropertyValue.isOneOfTheFollowingAPk(Collections.singleton(columnDPName)) ||
			foundsetPropertyValue.getClientIDForColumnName(columnDPName, false) != null)
		{
			return queueCellChange(relativeRowIndex, oldViewportSize, columnDPName);
		}
		return false;
	}

}