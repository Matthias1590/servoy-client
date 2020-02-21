/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.query;

import com.servoy.base.query.BaseAbstractBaseQuery;
import com.servoy.base.query.IQueryValues;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Container for ANY or ALL conditions with values.
 *
 * @author rgansevles
 *
 */
public class AnyValues implements IQueryElement, IQueryValues
{
	private final Object[] values;

	public AnyValues(Object[] values)
	{
		this.values = values;
	}

	public Object[] getValues()
	{
		return values;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.values == null ? 0 : BaseAbstractBaseQuery.arrayHashcode(this.values));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final AnyValues other = (AnyValues)obj;
		return BaseAbstractBaseQuery.arrayEquals(this.values, other.values);
	}

	@Override
	public String toString()
	{
		return new StringBuilder("ANY[").append(values == null ? "NO" : values.length).append(" values]").toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { values });
	}

	public AnyValues(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		values = (Object[])members[0];
	}
}