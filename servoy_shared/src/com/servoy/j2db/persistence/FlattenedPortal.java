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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;

/**
 * @author lvostinar
 *
 */
public class FlattenedPortal extends Portal
{
	private final Portal portal;

	public FlattenedPortal(Portal portal)
	{
		super(portal.getParent(), portal.getID(), portal.getUUID());
		this.portal = portal;
		fill();
	}

	private void fill()
	{
		internalClearAllObjects();
		List<Portal> portals = new ArrayList<Portal>();
		List<Integer> existingIDs = new ArrayList<Integer>();
		Portal currentPortal = portal;
		while (currentPortal != null && !portals.contains(currentPortal))
		{
			portals.add(currentPortal);
			currentPortal = (Portal)currentPortal.getSuperPersist();
		}
		for (Portal temp : portals)
		{
			for (IPersist child : temp.getAllObjectsAsList())
			{
				if (!existingIDs.contains(child.getID()) && !existingIDs.contains(new Integer(((AbstractBase)child).getExtendsID())))
				{
					if (((AbstractBase)child).isOverrideOrphanElement())
					{
						continue;
					}
					internalAddChild(child);
				}
				if (((AbstractBase)child).getExtendsID() > 0 && !existingIDs.contains(((AbstractBase)child).getExtendsID()))
				{
					existingIDs.add(((AbstractBase)child).getExtendsID());
				}
			}
		}
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		portal.internalRemoveChild(obj);
		fill();
	}

	@Override
	public Field createNewField(Point location) throws RepositoryException
	{
		return portal.createNewField(location);
	}

	@Override
	public GraphicalComponent createNewGraphicalComponent(Point location) throws RepositoryException
	{
		return portal.createNewGraphicalComponent(location);
	}

	@Override
	@Deprecated
	public RectShape createNewRectangle(Point location) throws RepositoryException
	{
		return portal.createNewRectangle(location);
	}

	@Override
	public Shape createNewShape(Point location) throws RepositoryException
	{
		return portal.createNewShape(location);
	}

	@Override
	public void addChild(IPersist obj)
	{
		portal.addChild(obj);
		fill();
	}

	@Override
	<T> T getTypedProperty(TypedProperty<T> property)
	{
		return portal.getTypedProperty(property);
	}

	@Override
	<T> void setTypedProperty(TypedProperty<T> property, T value)
	{
		portal.setTypedProperty(property, value);
	}

	@Override
	public boolean equals(Object obj)
	{
		return portal.equals(obj);
	}

	@Override
	public int hashCode()
	{
		// just to be more explicit, id is the same
		return portal.hashCode();
	}
}
