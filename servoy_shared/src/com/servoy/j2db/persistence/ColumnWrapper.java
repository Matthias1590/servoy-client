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
package com.servoy.j2db.persistence;


import java.io.Serializable;
import java.util.Arrays;

/**
 * Binds column and relations together in one entity
 * 
 * @author jblok
 */
public class ColumnWrapper implements IDataProvider, Serializable
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final Relation[] relations;
	private final IColumn column;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public ColumnWrapper(IColumn column)
	{
		this(column, (Relation[])null);
	}

	public ColumnWrapper(IColumn column, Relation relation)
	{
		this(column, relation == null ? ((Relation[])null) : new Relation[] { relation });
	}

	public ColumnWrapper(IColumn column, Relation[] relations)
	{
		this.column = column;
		this.relations = (relations == null || relations.length == 0 || relations[0] == null) ? null : relations;
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */

/*
 * _____________________________________________________________ The methods below belong to this class
 */

	public Relation[] getRelations()
	{
		return relations;
	}

	public IColumn getColumn()
	{
		return column;
	}

	public String getName()
	{
		return prefixRelation(column.getName());
	}

	@Override
	public String toString()
	{
		return getName();
	}


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((column == null) ? 0 : column.hashCode());
		result = prime * result + Arrays.hashCode(relations);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ColumnWrapper other = (ColumnWrapper)obj;
		if (column == null)
		{
			if (other.column != null) return false;
		}
		else if (!column.equals(other.column)) return false;
		if (!Arrays.equals(relations, other.relations)) return false;
		return true;
	}

	public String getDataProviderID()//get the id
	{
		return prefixRelation(column.getDataProviderID());
	}

	protected String prefixRelation(String s)//get the id
	{
		if (relations == null)
		{
			return s;
		}

		StringBuilder sb = new StringBuilder(relations.length * 45);
		for (Relation relation : relations)
		{
			sb.append(relation.getName());
			sb.append('.');
		}
		sb.append(s);
		return sb.toString();
	}

	public int getDataProviderType()
	{
		return column.getDataProviderType();
	}

	public ColumnWrapper getColumnWrapper()
	{
		return this;
	}

	public int getLength()
	{
		return column.getLength();
	}

	public boolean isEditable()
	{
		return column.isEditable();
	}

	public int getFlags()
	{
		return column.getFlags();
	}

}
