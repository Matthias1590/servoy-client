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

import java.util.Collections;

import org.sablo.IChangeListener;
import org.sablo.specification.property.ArrayGranularChangeKeeper;
import org.sablo.specification.property.ArrayOperation;

/**
 * This class is responsible for monitoring changes to a foundset or foundset linked property's viewport data and
 * being able to update the client with those changes.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public abstract class ViewportDataChangeMonitor<DPT extends ViewportRowDataProvider>
{
	public static final String VIEWPORT_CHANGED = "viewportDataChanged";

	/**
	 * ViewPort bounds and data changed; for example client requested completely new viewPort bounds.
	 */
	protected boolean viewPortCompletelyChanged = false;

	protected ArrayGranularChangeKeeper viewPortChanges = new ArrayGranularChangeKeeper();

	protected final DPT rowDataProvider;

	protected final IChangeListener monitor;

	private FoundsetTypeViewportDataChangeMonitor foundsetTypeViewportDataChangeMonitor;

	public ViewportDataChangeMonitor(IChangeListener monitor, DPT rowDataProvider)
	{
		this.rowDataProvider = rowDataProvider;
		this.monitor = monitor;
	}

	protected void setFoundsetTypeViewportDataChangeMonitor(FoundsetTypeViewportDataChangeMonitor foundsetTypeViewportDataChangeMonitor)
	{
		// ViewportDataChangeMonitor that are used for foundset linked properties are aware of the change monitor of the actual foundset property so that they can
		// trigger sending special updates to client - meant to trigger the client-side foundset listener even if changes happened only in the linked properties, not just on actual foundset property contents
		this.foundsetTypeViewportDataChangeMonitor = foundsetTypeViewportDataChangeMonitor;
	}

	public DPT getRowDataProvider()
	{
		return rowDataProvider;
	}

	public void viewPortCompletelyChanged()
	{
		boolean changed = !viewPortCompletelyChanged;
		viewPortCompletelyChanged = true;
		viewPortChanges.reset(-1, -1);
		if (changed && monitor != null) monitor.valueChanged();
	}

	public boolean shouldSendWholeViewport()
	{
		return viewPortCompletelyChanged;
	}

	public ArrayOperation[] getViewPortChanges()
	{
		return viewPortChanges.getEquivalentSequenceOfOperations();
	}

	public boolean hasViewportChanges()
	{
		return viewPortChanges.hasChanges();
	}

	public void clearChanges()
	{
		viewPortCompletelyChanged = false;
		viewPortChanges.reset(-1, -1);
	}

	public void doneWritingChanges()
	{
		FoundsetDataAdapterList fsDAL = rowDataProvider.getDataAdapterList();
		if (fsDAL != null) fsDAL.resetDALToSelectedIndexQuietly();
	}

	/**
	 * This gets called when rows in the viewport were deleted/inserted/changed.
	 *
	 * @param relativeFirstRow viewPort relative start index for given operation.
	 * @param relativeLastRow viewPort relative end index for given operation (inclusive).
	 * @param operationType can be one of {@link ViewportOperation#DELETE}, {@link ViewportOperation#INSERT} or {@link ViewportOperation#CHANGE}.
	 *
	 * @return true if the operation was queued, false otherwise.
	 */
	public boolean queueOperation(int relativeFirstRow, int relativeLastRow, int oldViewportSize, int operationType)
	{
		if (!rowDataProvider.isReady()) return false;

		if (!shouldSendWholeViewport())
		{
			boolean changed = !viewPortChanges.hasChanges(); // if it doesn't already have changes then it changed
			processOperation(changed, oldViewportSize, new ViewportOperation(relativeFirstRow, relativeLastRow, operationType));

			if (changed && monitor != null) monitor.valueChanged();
			return true;
		}
		return false;
	}

	protected void processOperation(boolean viewportHasNoChangesYet, int oldViewportSize, ViewportOperation op)
	{
		if (viewportHasNoChangesYet) viewPortChanges.reset(0, oldViewportSize - 1);
		viewPortChanges.processOperation(op);
	}

	/**
	 * This gets called when the value of a cell in the viewport was changed. Depending on impl. it might end up calling {@link #queueCellChange(int, int, String)} 0 or more times.
	 *
	 * @param relativeRowIndex viewPort relative row index for given cell-change operation.
	 * @param columnDPName (non-null) the name of the foundset column that changed for the given row index; this is the actual column/dp name
	 * not the cellName that could be impl. specific (based on what rowDataProvider impl. uses as cellNames)
	 *
	 * @return true if the operation was queued, false otherwise.
	 */
	public abstract boolean queueCellChangeDueToColumn(int relativeRowIndex, int oldViewportSize, final String columnDPName);

	/**
	 * This gets called when the value of a cell in the viewport was changed.
	 *
	 * @param relativeRowIndex viewPort relative row index for given cell-change operation.
	 * @param cellName whatever the rowDataProvider uses to identify cells within a row/record. Foundset types will
	 *                 give actual table column name, components will use property names, foundset linked types will
	 *                 probably use something that is not important as they have only 1 cell/row.
	 *
	 * @return true if the operation was queued, false otherwise.
	 */
	public boolean queueCellChange(int relativeRowIndex, int oldViewportSize, final String cellName)
	{
		if (!rowDataProvider.isReady()) return false;

		if (!shouldSendWholeViewport())
		{
			boolean changed = !viewPortChanges.hasChanges(); // if it doesn't already have changes then it changed
			processOperation(changed, oldViewportSize,
				new ViewportOperation(relativeRowIndex, relativeRowIndex, ArrayOperation.CHANGE, Collections.singleton(cellName)));

			// If at least one ViewportDataChangeMonitor sent changes (so the change affected the data in that ViewportDataChangeMonitor)
			// but the foundset property itself was not affected in any way; still, we want the client side (browser) listeners attached to the foundset property
			// notify that an update occurred in this case, even though it didn't actually affect the foundset property itself... but the other foundset linked properties.
			// in the future if we impl. change listeners for foundset linked or foundset linked component types, we could avoid this "queueLinkedPropertyUpdate" completely
			if (foundsetTypeViewportDataChangeMonitor != null)
			{
				foundsetTypeViewportDataChangeMonitor.queueLinkedPropertyUpdate(relativeRowIndex, relativeRowIndex, oldViewportSize, cellName);
			}

			if (changed && monitor != null) monitor.valueChanged();
			return true;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "ViewportDataChangeMonitor [viewPortCompletelyChanged=" + viewPortCompletelyChanged + ", viewPortChanges=" + viewPortChanges + "]";
	}


}
