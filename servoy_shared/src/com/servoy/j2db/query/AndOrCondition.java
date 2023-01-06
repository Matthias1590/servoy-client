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
package com.servoy.j2db.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import com.servoy.base.query.BaseAndOrCondition;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Base condition class for AndCondition and OrCondition.
 *
 * @author rgansevles
 *
 */
public abstract class AndOrCondition extends BaseAndOrCondition<ISQLCondition> implements ISQLCondition
{
	public AndOrCondition()
	{
	}

	public AndOrCondition(List<ISQLCondition> conditions)
	{
		super(conditions);
	}

	public AndOrCondition(HashMap<String, List<ISQLCondition>> conditions)
	{
		super(conditions);
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		conditions = AbstractBaseQuery.acceptVisitor(conditions, visitor);
	}

	static <T extends AndOrCondition> Collector<ISQLCondition, ArrayList<ISQLCondition>, T> collector(Function<ArrayList<ISQLCondition>, T> finisher)
	{
		Collector<ISQLCondition, ArrayList<ISQLCondition>, T> collector = Collector.of(
			ArrayList::new,
			ArrayList::add,
			(left, right) -> {
				left.addAll(right);
				return left;
			},
			finisher);
		return collector;
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), conditions);
	}

	public AndOrCondition(ReplacedObject s)
	{
		conditions = (HashMap<String, List<ISQLCondition>>)s.getObject(); // RAGTEST ouden
	}

//	public Object writeReplace()
//	{
//		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
//		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), name == null ? conditions : new Object[] { conditions, name });
//	}
//
//	public AndOrCondition(ReplacedObject s)
//	{
//		if (s.getObject() instanceof List)
//		{
//			conditions = (List<ISQLCondition>)s.getObject();
//		}
//		else
//		{
//			int i = 0;
//			Object[] members = (Object[])s.getObject();
//			conditions = (List<ISQLCondition>)members[i++];
//			name = (String)members[i++];
//		}
//	}
}