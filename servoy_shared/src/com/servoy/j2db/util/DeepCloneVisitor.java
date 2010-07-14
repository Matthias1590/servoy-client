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
package com.servoy.j2db.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.query.IQueryElement;

/**
 * Visitor class to for performing a deep clone that maintains tree structure.
 * 
 * @see IVisitable
 * @author rgansevles
 * 
 */
public class DeepCloneVisitor implements IVisitor
{
	private final Map<EqualityWrapper, Object> map = new HashMap<EqualityWrapper, Object>();

	public Object visit(Object o)
	{
		if (o == null || o instanceof Immutable)
		{
			// do not need to make a copy
			return new IVisitor.VistorResult(o, false);
		}

		EqualityWrapper wrapper = new EqualityWrapper(o);
		Object clone = map.get(wrapper);
		if (clone != null || map.containsKey(wrapper))
		{
			return new IVisitor.VistorResult(clone, false);
		}

		try
		{
			clone = getShallowClone(o);
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException("Clone not supported in class " + o.getClass(), e); //$NON-NLS-1$
		}
		map.put(wrapper, clone);
		return new IVisitor.VistorResult(clone, true);
	}

	static Object getShallowClone(Object o) throws CloneNotSupportedException
	{
		if (o instanceof IQueryElement)
		{
			return ((IQueryElement)o).shallowClone();
		}

		// don't know why we can't use the reflection method below for object arrays, it throws NoSuchMethodException
		if (o instanceof Object[])
		{
			return ((Object[])o).clone();
		}
		if (o instanceof byte[])
		{
			return ((byte[])o).clone();
		}
		if (o instanceof int[])
		{
			return ((int[])o).clone();
		}
		if (o instanceof short[])
		{
			return ((short[])o).clone();
		}
		if (o instanceof long[])
		{
			return ((long[])o).clone();
		}
		if (o instanceof float[])
		{
			return ((float[])o).clone();
		}
		if (o instanceof double[])
		{
			return ((double[])o).clone();
		}
		if (o instanceof char[])
		{
			return ((char[])o).clone();
		}
		if (o instanceof boolean[])
		{
			return ((boolean[])o).clone();
		}

		// something else than the known stuff, is there a clone method?
		if (o instanceof Cloneable)
		{
			try
			{
				// assume clone() is a shallow clone like it is for ArrayList, HashMap, Object[] etc.
				return o.getClass().getMethod("clone", (Class[])null).invoke(o, (Object[])null); //$NON-NLS-1$
			}
			catch (NoSuchMethodException e)
			{
				// Cloneable but no clone method???
				throw new RuntimeException("Cannot clone Cloneable class " + o.getClass()); //$NON-NLS-1$
			}
			catch (Exception e)
			{
				if (e instanceof InvocationTargetException && ((InvocationTargetException)e).getTargetException() instanceof CloneNotSupportedException)
				{
					throw (CloneNotSupportedException)((InvocationTargetException)e).getTargetException();
				}
				throw new CloneNotSupportedException(o.getClass().getName());
			}
		}

		// cannot clone, assume immutable primitives like Integer, String
		return o;
	}

	/**
	 * Wrapper around object that uses pointer equality on wrapped object for equals.
	 * 
	 * @author rgansevles
	 * 
	 */
	static class EqualityWrapper
	{
		final private Object o;

		EqualityWrapper(Object o)
		{
			this.o = o;
		}

		@Override
		public int hashCode()
		{
			if (o == null) return 0;
			return o.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			// only equals if wrapping the same object pointer
			return obj instanceof EqualityWrapper && ((EqualityWrapper)obj).o == o;
		}
	}
}
