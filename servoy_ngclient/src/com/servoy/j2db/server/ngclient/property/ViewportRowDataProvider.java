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
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;

/**
 * This class is responsible for writing data in a foundset property's viewport.
 *
 * @author acostescu
 */
public abstract class ViewportRowDataProvider
{

	protected abstract void populateRowData(IRecordInternal record, String columnName, JSONWriter w, DataConversion clientConversionInfo) throws JSONException;

	protected void writeRowData(int foundsetIndex, String columnName, IFoundSetInternal foundset, JSONWriter w, DataConversion clientConversionInfo)
		throws JSONException
	{
		// write viewport row contents
		IRecordInternal record = foundset.getRecord(foundsetIndex);
		w.object().key(FoundsetTypeSabloValue.ROW_ID_COL_KEY).value(record.getPKHashKey() + "_" + foundsetIndex); // TODO do we really need the "i"?

		populateRowData(record, columnName, w, clientConversionInfo);

		w.endObject();
	}

	protected void writeRowData(int startIndex, int endIndex, IFoundSetInternal foundset, JSONWriter w, DataConversion clientConversionInfo)
		throws JSONException
	{
		writeRowData(startIndex, endIndex, null, foundset, w, clientConversionInfo);
	}

	protected void writeRowData(int startIndex, int endIndex, String columnName, IFoundSetInternal foundset, JSONWriter w, DataConversion clientConversionInfo)
		throws JSONException
	{
		w.array();
		List<Map<String, Object>> rows = new ArrayList<>();
		PropertyDescription rowTypes = null;
		int size = foundset.getSize();
		int end = Math.min(size - 1, endIndex);
		if (startIndex <= end)
		{
			for (int i = startIndex; i <= endIndex; i++)
			{
				clientConversionInfo.pushNode(String.valueOf(i - startIndex));
				writeRowData(i, columnName, foundset, w, clientConversionInfo);
				clientConversionInfo.popNode();
			}
		}
		w.endArray();
	}

}
