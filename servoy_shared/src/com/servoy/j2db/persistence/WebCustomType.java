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


import java.beans.IntrospectionException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 *
 * @author gboros
 */
public class WebCustomType extends AbstractBase implements IChildWebObject
{

//	private static final long serialVersionUID = 1L; // this shouldn't get serialized anyway for now; parent WebComponent just serializes it's json

	protected static Set<String> purePersistPropertyNames;

	static
	{
		try
		{
			purePersistPropertyNames = RepositoryHelper.getSettersViaIntrospection(WebComponent.class).keySet();
		}
		catch (IntrospectionException e)
		{
			purePersistPropertyNames = new HashSet<String>();
			Debug.error(e);
		}
	}

	private transient final String jsonKey;
	private transient int index;
	protected transient final WebObjectImpl webObjectImpl;

	public WebCustomType(IBasicWebObject parentWebObject, Object propertyDescription, String jsonKey, int index, boolean isNew, int id, UUID uuid)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, id, uuid);
		webObjectImpl = new WebObjectImpl(this, propertyDescription);

		this.jsonKey = jsonKey;
		this.index = index;

		JSONObject fullJSONInFrmFile = WebObjectImpl.getFullJSONInFrmFile(this, isNew);
		if (fullJSONInFrmFile != null) webObjectImpl.setJsonInternal(fullJSONInFrmFile);
	}

	public PropertyDescription getPropertyDescription()
	{
		return webObjectImpl.getPropertyDescription();
	}

	@Override
	public void clearChanged()
	{
		super.clearChanged();
		for (IChildWebObject x : getAllPersistMappedProperties())
		{
			if (x.isChanged()) x.clearChanged();
		}
	}

	@Override
	public void updateJSON()
	{
		webObjectImpl.updatePersistMappedPropeties();
		getParent().updateJSON();
	}

	@Override
	public void setJsonSubproperty(String key, Object value)
	{
		webObjectImpl.setJsonSubproperty(key, value);
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (!webObjectImpl.setProperty(propertyName, val))
		{
			// see if it's not a direct persist property as well such as size, location, anchors... if it is set it here as well anyway so that they are in sync with spec properties
			if (purePersistPropertyNames.contains(propertyName)) super.setProperty(propertyName, val);
		}
		else super.setProperty(propertyName, val);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		if (!webObjectImpl.clearProperty(propertyName)) super.clearProperty(propertyName);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		Object value = null;
		if (webObjectImpl == null || purePersistPropertyNames.contains(propertyName)) value = super.getProperty(propertyName);
		if (value == null) value = webObjectImpl.getProperty(propertyName);
		return value;
	}

	/**
	 * Returns all direct child typed properties or array of such typed properties.
	 */
	public List<IChildWebObject> getAllPersistMappedProperties()
	{
		return webObjectImpl.getAllPersistMappedProperties();
	}

	// TODO is this still really needed? we now work with the property description based on specs...
	public void setTypeName(String arg)
	{
		webObjectImpl.setTypeName(arg);
	}

	public String getTypeName()
	{
		return webObjectImpl.getTypeName();
	}
	
	/**
	 * DO NOT USE this method! Use setProperty instead.
	 * @param arg
	 */
	public void setJson(JSONObject arg)
	{
		webObjectImpl.setJson(arg);
	}

	/**
	 * DO NOT USE this method! Use setProperty instead.
	 */
	public JSONObject getJson()
	{
		return webObjectImpl.getJson();
	}

	@Override
	public JSONObject getFlattenedJson()
	{
		return webObjectImpl.getJson();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + webObjectImpl.toString(); //$NON-NLS-1$
	}

	@Override
	public IBasicWebObject getParent()
	{
		return (IBasicWebObject)super.getParent();
	}

	public IBasicWebComponent getParentComponent()
	{
		return getParent().getParentComponent();
	}

	public int getIndex()
	{
		return index;
	}

//	public String getUUIDString()
//	{
//		String addIndex = "";
//		if (index >= 0) addIndex = "." + index;
//		return parent.getUUID() + "_" + jsonKey + addIndex + "_" + typeName;
//	}

//	public String getUUIDString()
//	{
//		String addIndex = "";
//		if (index >= 0) addIndex = "[" + index + "]";
//		return parentWebObject.getUUID() + "_" + jsonKey + addIndex + "_" + typeName;
//	}
//

//	@Override
//	public boolean equals(Object obj)
//	{
//		if (obj instanceof WebCustomType)
//		{
//			return ((WebCustomType)obj).getUUIDString().equals(this.getUUIDString());
//		}
//		return super.equals(obj);
//	}
//
	@Override
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	@Override
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	public String getJsonKey()
	{
		return jsonKey;
	}

	/**
	 * @param i the new index
	 */
	public void setIndex(int i)
	{
		index = i;
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		webObjectImpl.internalRemoveChild(obj);
	}

	@Override
	public void internalAddChild(IPersist obj)
	{
		webObjectImpl.internalAddChild(obj);
	}

	@Override
	public Iterator<IPersist> getAllObjects()
	{
		return webObjectImpl.getAllObjects();
	}

	@Override
	public List<IPersist> getAllObjectsAsList()
	{
		return Utils.asList(getAllObjects());
	}

	@Override
	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return webObjectImpl.getObjects(tp);
	}

	@Override
	public IPersist getChild(UUID childUuid)
	{
		return webObjectImpl.getChild(childUuid);
	}

	@Override
	public JSONObject getFullJsonInFrmFile()
	{
		return webObjectImpl.getJson();
	}

}
