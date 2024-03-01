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
package com.servoy.j2db.util.visitor;

/**
 * Visitor class to for replacing all occurrences of one object with another one in a IVisitable object.
 *
 * @see IVisitable
 * @author rgansevles
 *
 */
public class ReplaceVisitor implements IVisitor
{
	private final Object org;
	private final Object repl;
	private final boolean useEquals;

	/**
	 * @param org
	 * @param repl
	 * @param useEquals
	 */
	public ReplaceVisitor(Object org, Object repl, boolean useEquals)
	{
		this.org = org;
		this.repl = repl;
		this.useEquals = useEquals;
	}

	public Object visit(Object o)
	{
		boolean match;
		if (org == null)
		{
			match = o == null;
		}
		else
		{
			match = (org == o || (useEquals && org.equals(o)));
		}
		if (match)
		{
			return repl;
		}
		return o;
	}
}
