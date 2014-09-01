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

package com.servoy.j2db.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ISupportExtendsID;

/**
 * Utility methods that can be used related to persists.
 *
 * @author acostescu
 */
public class PersistUtils
{

	/**
	 * Similar to {@link AbstractBase#getPropertiesMap()}, but takes into account persist inheritance.
	 * @param extendable a persist which could be inherit from another.
	 * @return the map of property values collected from the persist's hierarchy chain.
	 */
	public static Map<String, Object> getFlattenedPropertiesMap(ISupportExtendsID extendable)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		List<AbstractBase> hierarchy = PersistHelper.getOverrideHierarchy(extendable);
		for (int i = hierarchy.size() - 1; i >= 0; i--)
		{
			map.putAll(hierarchy.get(i).getPropertiesMap());
		}
		return map;
	}

}
