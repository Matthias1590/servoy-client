/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

import java.util.Iterator;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderSort;
import com.servoy.j2db.querybuilder.IQueryBuilderSorts;
import com.servoy.j2db.querybuilder.internal.IQueryBuilderColumnSortInternal;

/**
 * @author rgansevles
 *
 */
public class QueryBuilderSorts extends AbstractQueryBuilderPart<QueryBuilder> implements IQueryBuilderSorts
{
	/**
	 * @param queryBuilder
	 */
	QueryBuilderSorts(QueryBuilder parent)
	{
		super(parent, parent);
	}

	@JSFunction
	public QueryBuilderSorts addPk() throws RepositoryException
	{
		Iterator<String> rowIdentColumnNames = getParent().getTable().getRowIdentColumnNames();
		while (rowIdentColumnNames.hasNext())
		{
			add(getParent().getColumn(rowIdentColumnNames.next()).asc());
		}
		return this;
	}

	public QueryBuilderSorts js_add(QueryBuilderSort sort) throws RepositoryException
	{
		return add(sort);
	}

	public QueryBuilderSorts add(IQueryBuilderSort sort) throws RepositoryException
	{
		getParent().getQuery().addSort(((IQueryBuilderColumnSortInternal)sort).getQueryQuerySort());
		return this;
	}

	public QueryBuilderSorts js_add(QueryBuilderColumn columnSortAsc) throws RepositoryException
	{
		return add(columnSortAsc);
	}

	public QueryBuilderSorts add(IQueryBuilderColumn columnSortAsc) throws RepositoryException
	{
		return add(columnSortAsc.asc());
	}

	@JSFunction
	public QueryBuilderSorts addValue(Object value) throws RepositoryException
	{
		getParent().getQuery().addColumn(new QueryColumnValue(value, null, value instanceof Integer));
		return this;
	}

}
