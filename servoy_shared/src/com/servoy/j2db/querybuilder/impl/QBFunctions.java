/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.querybuilder.IQueryBuilderFunctions;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBFunctions extends QBPart implements IQueryBuilderFunctions
{
	QBFunctions(QBSelect parent)
	{
		super(parent, parent);
	}

	@Override
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#upper(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.upper.eq(query.functions.upper('Sample'))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction upper(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.upper, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#abs(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynum.abs.eq(query.functions.abs(myval))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction abs(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.abs, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#sqrt(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynum.sqrt.eq(query.functions.sqrt(myval))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction sqrt(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.sqrt, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#lower(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.lower.eq(query.functions.lower('Sample'))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction lower(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.lower, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#trim(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.trim.eq(query.functions.trim('Sample'))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction trim(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.trim, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#length(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.len.eq(query.functions.len('Sample'))) 
	 * foundset.loadRecords(query);
	 */
	public QBFunction js_len(Object value)
	{
		return length(value);
	}


	public QBFunction length(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.length, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#bit_length(Object)
	 * @param value 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.bit_length.eq(query.functions.bit_length('Sample'))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction bit_length(Object value)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.bit_length, new IQuerySelectValue[] { getRoot().createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#cast(Object, String)
	 * @param value object to cast
	 * @param type type see QUERY_COLUMN_TYPES
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.result.add(query.functions.cast("22",QUERY_COLUMN_TYPES.TYPE_INTEGER)).add(query.columns.amt_discount.cast(QUERY_COLUMN_TYPES.TYPE_STRING)); 
	 * application.output(databaseManager.getDataSetByQuery(query,1).getAsHTML())
	 */
	@JSFunction
	public QBFunction cast(Object value, String type)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.cast, new IQuerySelectValue[] { getRoot().createOperand(value), new QueryColumnValue(
			type, null, true) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#substring(Object, int)
	 * @param arg column name
	 * @param pos position
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.substring(3).eq(query.functions.substring('Sample', 3))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction substring(Object arg, int pos)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.substring,
			new IQuerySelectValue[] { getRoot().createOperand(arg), getRoot().createOperand(Integer.valueOf(pos)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#substring(Object, int, int)
	 * @param arg column name
	 * @param pos position
	 * @param len length
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.substring(3, 2).eq(query.functions.substring('Sample', 3, 2))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction substring(Object arg, int pos, int len)
	{
		return new QBFunction(
			getRoot(),
			getParent(),
			QueryFunctionType.substring,
			new IQuerySelectValue[] { getRoot().createOperand(arg), getRoot().createOperand(Integer.valueOf(pos)), getRoot().createOperand(Integer.valueOf(len)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#locate(Object, Object)
	 * @param string1 string to locate
	 * @param string2 string to search in
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.locate('amp').eq(query.functions.locate('Sample', 'amp'))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction locate(Object string1, Object string2)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.locate,
			new IQuerySelectValue[] { getRoot().createOperand(string1), getRoot().createOperand(string2), });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#locate(Object, Object, int)
	 * @param string1 string to locate
	 * @param string2 string to search in
	 * @param start start pos
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.locate('amp', 1).eq(query.functions.locate('Sample', 'amp', 1))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction locate(Object string1, Object string2, int start)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.locate,
			new IQuerySelectValue[] { getRoot().createOperand(string1), getRoot().createOperand(string2), getRoot().createOperand(Integer.valueOf(start)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#nullif(Object, Object)
	 * @param arg1 
	 * @param arg1 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.shipname.nullif('none').eq(query.functions.nullif('Sample', 'none'))) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction nullif(Object arg1, Object arg2)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.nullif,
			new IQuerySelectValue[] { getRoot().createOperand(arg1), getRoot().createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#mod(Object, Object)
	 * @param dividend 
	 * @param divisor 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.mod(2).eq(query.functions.mod(myvar, 2)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction mod(Object dividend, Object divisor)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.mod,
			new IQuerySelectValue[] { getRoot().createOperand(dividend), getRoot().createOperand(divisor) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#plus(Object, Object)
	 * @param arg1 
	 * @param arg2 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.plus(2).eq(query.functions.plus(myvar, 2)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction plus(Object arg1, Object arg2)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.plus, new IQuerySelectValue[] { getRoot().createOperand(arg1), getRoot().createOperand(
			arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#minus(Object, Object)
	 * @param arg1 
	 * @param arg2 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.minus(2).eq(query.functions.minus(myvar, 2)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction minus(Object arg1, Object arg2)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.minus,
			new IQuerySelectValue[] { getRoot().createOperand(arg1), getRoot().createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#multiply(Object, Object)
	 * @param arg1 
	 * @param arg2 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.multiply(2).eq(query.functions.multiply(myvar, 2)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction multiply(Object arg1, Object arg2)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.multiply,
			new IQuerySelectValue[] { getRoot().createOperand(arg1), getRoot().createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#divide(Object, Object)
	 * @param arg1 
	 * @param arg2 
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.divide(2).eq(query.functions.divide(myvar, 2)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction divide(Object arg1, Object arg2)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.divide,
			new IQuerySelectValue[] { getRoot().createOperand(arg1), getRoot().createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#floor(Object)
	 * @param arg number object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.floor.eq(query.functions.floor(myvar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction floor(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.floor, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#round(Object)
	 * @param arg number object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.round.eq(query.functions.round(myvar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction round(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.round, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#ceil(Object)
	 * @param arg number object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mynumcol.ceil.eq(query.functions.ceil(myvar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction ceil(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.ceil, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#second(Object)
	 * @param arg date object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mydatecol.second.eq(query.functions.second(mydatevar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction second(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.second, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#minute(Object)
	 * @param arg date object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mydatecol.minute.eq(query.functions.minute(mydatevar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction minute(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.minute, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#hour(Object)
	 * @param arg date object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mydatecol.hour.eq(query.functions.hour(mydatevar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction hour(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.hour, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#day(Object)
	 * @param arg date object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mydatecol.day.eq(query.functions.day(mydatevar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction day(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.day, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#month(Object)
	 * @param arg date object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mydatecol.month.eq(query.functions.month(mydatevar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction month(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.month, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#year(Object)
	 * @param arg date object
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mydatecol.year.eq(query.functions.year(mydatevar)) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction year(Object arg)
	{
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.year, new IQuerySelectValue[] { getRoot().createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#year(Object)
	 * @param args arguments to coalesce
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var query = databaseManager.createSelect('db:/example_data/orders') //$NON-NLS-1$
	 * query.where.add(query.columns.mycol.coalesce('defval').eq(query.functions.coalesce(myvar, 'defval')) 
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBFunction coalesce(Object... args)
	{
		List<IQuerySelectValue> list = new ArrayList<IQuerySelectValue>(args.length);
		for (Object arg : args)
		{
			list.add(getRoot().createOperand(arg));
		}
		return new QBFunction(getRoot(), getParent(), QueryFunctionType.coalesce, list.toArray(new IQuerySelectValue[list.size()]));
	}

}
