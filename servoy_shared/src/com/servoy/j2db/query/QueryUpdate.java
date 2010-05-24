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
import java.util.List;

import com.servoy.j2db.util.IVisitor;
import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Query update statement.
 * 
 * @author rob
 * 
 */
public class QueryUpdate extends AbstractBaseQuery implements ISQLUpdate
{
	private QueryTable table;
	private List columns = new ArrayList();
	private List values = new ArrayList();
	private AndCondition condition;
	private List joins; // joins in update statements are not supported by hibernate.


	public QueryUpdate(QueryTable table)
	{
		this.table = table;
	}

	public void addValue(QueryColumn column, Object value)
	{
		columns.add(column);
		values.add(value);
	}

	public boolean hasValues()
	{
		return (values.size() > 0);
	}

	public List getColumns()
	{
		return columns;
	}

	public List getValues()
	{
		return values;
	}

	public void setCondition(ISQLCondition c)
	{
		if (c == null || c instanceof AndCondition)
		{
			condition = (AndCondition)c;
		}
		else
		{
			condition = new AndCondition();
			condition.addCondition(c);
		}
	}


	public void addCondition(ISQLCondition c)
	{
		if (c == null)
		{
			return;
		}
		if (condition == null)
		{
			condition = new AndCondition();
		}
		condition.addCondition(c);
	}

//	public void setJoins(List jns)
//	{
//		int i;
//		for (i = 0; jns != null && i < jns.size(); i++)
//		{
//			Object join = jns.get(i);
//			if (!(join instanceof SQLJoin))
//			{
//				throw new IllegalArgumentException("Unknown join class "+join.getClass().getName()); //$NON-NLS-1$
//			}
//		}
//		joins = i == 0 ? null : jns;
//	}

//	public void addJoin(SQLJoin join)
//	{
//		if (joins == null)
//		{
//			joins = new ArrayList();
//		}
//		joins.add(join);
//	}

	public AndCondition getCondition()
	{
		return condition;
	}

	public AndCondition getConditionClone()
	{
		return deepClone(condition);
	}

//	public List getJoins()
//	{
//		return joins;
//	}

	public QueryTable getTable()
	{
		return table;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		table = (QueryTable)AbstractBaseQuery.acceptVisitor(table, visitor);
		columns = (List)AbstractBaseQuery.acceptVisitor(columns, visitor);
		values = (List)AbstractBaseQuery.acceptVisitor(values, visitor);
		condition = (AndCondition)AbstractBaseQuery.acceptVisitor(condition, visitor);
		joins = (List)AbstractBaseQuery.acceptVisitor(joins, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.columns == null) ? 0 : this.columns.hashCode());
		result = PRIME * result + ((this.condition == null) ? 0 : this.condition.hashCode());
		result = PRIME * result + ((this.joins == null) ? 0 : this.joins.hashCode());
		result = PRIME * result + ((this.table == null) ? 0 : this.table.hashCode());
		result = PRIME * result + ((this.values == null) ? 0 : this.values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryUpdate other = (QueryUpdate)obj;
		if (this.columns == null)
		{
			if (other.columns != null) return false;
		}
		else if (!this.columns.equals(other.columns)) return false;
		if (this.condition == null)
		{
			if (other.condition != null) return false;
		}
		else if (!this.condition.equals(other.condition)) return false;
		if (this.joins == null)
		{
			if (other.joins != null) return false;
		}
		else if (!this.joins.equals(other.joins)) return false;
		if (this.table == null)
		{
			if (other.table != null) return false;
		}
		else if (!this.table.equals(other.table)) return false;
		if (this.values == null)
		{
			if (other.values != null) return false;
		}
		else if (!this.values.equals(other.values)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("UPDATE ").append(table.toString()).append(" SET ("); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < columns.size(); i++)
		{
			if (i > 0)
			{
				sb.append('|');
			}
			sb.append(columns.get(i).toString());
		}
		sb.append(") = ("); //$NON-NLS-1$


		for (int i = 0; i < values.size(); i++)
		{
			if (i > 0)
			{
				sb.append('|');
			}
			sb.append(AbstractBaseQuery.toString(values.get(i)));
		}
		sb.append(')');
		if (condition != null)
		{
			sb.append(" WHERE ").append(condition); //$NON-NLS-1$
		}
		for (int i = 0; joins != null && i < joins.size(); i++)
		{
			sb.append(' ').append(joins.get(i).toString());
		}

		return sb.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, columns, values, condition, joins });
	}

	public QueryUpdate(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.table = (QueryTable)members[i++];
		this.columns = (List)members[i++];
		this.values = (List)members[i++];
		this.condition = (AndCondition)members[i++];
		this.joins = (List)members[i++];
	}

}
