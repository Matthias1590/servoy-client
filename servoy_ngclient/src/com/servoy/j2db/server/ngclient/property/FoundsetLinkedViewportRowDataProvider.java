/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.util.Pair;

/**
 * Writes values for each record of the foundset viewport according to a {@link FoundsetLinkedPropertyType}'s wrapped type value.
 *
 * @author acostescu
 */
public class FoundsetLinkedViewportRowDataProvider<YF, YT> extends ViewportRowDataProvider
{

	private final FoundsetDataAdapterList dal;
	private final PropertyDescription pd;
	private final FoundsetLinkedTypeSabloValue<YF, YT> sabloValue;
	private IBrowserConverterContext browserConverterContext;
	private final Pair<String/* forColumn */, JSONString/* type */> reuseablePairForCellType = new Pair<>(null, null); // just to avoid creating lots of objects when writing viewport to JSON

	public FoundsetLinkedViewportRowDataProvider(FoundsetDataAdapterList dal, PropertyDescription pd, FoundsetLinkedTypeSabloValue<YF, YT> sabloValue)
	{
		this.dal = dal;
		this.pd = pd;
		this.sabloValue = sabloValue;
	}

	@Override
	protected void populateRowData(IRecordInternal record, String columnNameAlwaysNullSoIgnore, JSONWriter w, String generatedRowId,
		ViewportClientSideTypes types) throws JSONException
	{
		// TODO we should change the order in which rows are populated for a foundset; the foundset itself should do dal.setRecordQuietly(record) then call all ViewportRowDataProvider to populate their data somehow
		dal.setRecordQuietly(record);
		IJSONStringWithClientSideType jsonValueRepresentationForWrappedValue = JSONUtils.FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(
			sabloValue.getWrappedValue(), pd,
			browserConverterContext, false);

		w.value(jsonValueRepresentationForWrappedValue); // write it even if it's null (we can't have undefined in a json array)
		reuseablePairForCellType.setRight(jsonValueRepresentationForWrappedValue != null ? jsonValueRepresentationForWrappedValue.getClientSideType() : null);
		types.registerClientSideType(Arrays.asList(reuseablePairForCellType));
	}

	@Override
	protected boolean shouldGenerateRowIds()
	{
		return false;
	}

	@Override
	protected boolean containsColumn(String columnName)
	{
		return sabloValue.usesFoundsetLinkedDataprovider(columnName);
	}

	/**
	 * We need this as a separate reference and not only passed when doing toJSON on main property because on foundset changes, the foundset property
	 * will write at once the changes and keep them as strings... So then we need access to the context at that time.
	 */
	public void initializeIfNeeded(IBrowserConverterContext context)
	{
		if (browserConverterContext == null) browserConverterContext = context;
	}

	@Override
	protected boolean isReady()
	{
		return browserConverterContext != null;
	}

	@Override
	protected FoundsetDataAdapterList getDataAdapterList()
	{
		return dal;
	}
}
