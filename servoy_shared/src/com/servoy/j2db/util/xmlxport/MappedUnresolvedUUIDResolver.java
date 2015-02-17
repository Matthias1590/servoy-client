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
package com.servoy.j2db.util.xmlxport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.IUnresolvedUUIDResolver;
import com.servoy.j2db.util.IntArrayKey;
import com.servoy.j2db.util.UUID;

/**
 * @author sebster
 *
 */
public class MappedUnresolvedUUIDResolver implements IUnresolvedUUIDResolver, Serializable
{

	private final Map uuidMap;

	public MappedUnresolvedUUIDResolver()
	{
		uuidMap = new HashMap();
	}

	public void add(int elementId, int revision, int contentId, UUID uuid)
	{
		uuidMap.put(new IntArrayKey(new int[] { elementId, revision, contentId }), uuid);
	}

	public UUID resolve(int elementId, int revision, int contentId) throws RepositoryException
	{
		UUID uuid = (UUID)uuidMap.get(new IntArrayKey(new int[] { elementId, revision, contentId }));
		if (uuid != null)
		{
			return uuid;
		}
		return IRepository.UNRESOLVED_UUID;
	}

}
