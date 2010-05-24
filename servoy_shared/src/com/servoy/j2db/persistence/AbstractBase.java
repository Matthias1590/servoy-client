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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;

import com.servoy.j2db.util.JSONWrapperMap;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;


/**
 * Abstract base class used by all IPersist classes If a sub class implements ISupportChild\IPersistConeble more methods are already provided
 * 
 * @author jblok
 */
public abstract class AbstractBase implements IPersist
{
	/*
	 * Attributes for IPersist
	 */
	protected UUID uuid;
	protected int element_id;
	protected int revision = 1;
	protected boolean isChanged = true;
	protected ISupportChilds parent;
	protected int type;

	/*
	 * All 1-n providers for this class
	 */
	private List<IPersist> allobjects = null;
	private transient Map<UUID, IPersist> allobjectsMap = null;

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	protected transient JSONWrapperMap jsonCustomProperties = null;
	protected String customProperties = null;


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public AbstractBase(int type, ISupportChilds parent, int element_id, UUID uuid)
	{
		this.type = type;
		this.parent = parent;
		this.element_id = element_id;
		this.uuid = uuid;
	}

	/*
	 * _____________________________________________________________ Methods from IPersist
	 */

	public Object acceptVisitor(IPersistVisitor visitor)
	{
		Object retval = visitor.visit(this);
		if (retval == IPersistVisitor.CONTINUE_TRAVERSAL && this instanceof ISupportChilds)
		{
			Iterator<IPersist> it = getAllObjects();
			while ((retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER) && it.hasNext())
			{
				retval = it.next().acceptVisitor(visitor);
			}
		}
		return (retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_UP)
			? null : retval;
	}

	public Object acceptVisitorDepthFirst(IPersistVisitor visitor) throws RepositoryException
	{
		Object retval = IPersistVisitor.CONTINUE_TRAVERSAL;
		if (this instanceof ISupportChilds)
		{
			Iterator<IPersist> it = getAllObjects();
			while (it.hasNext() &&
				(retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_UP))
			{
				IPersist visitee = it.next();
				retval = visitee.acceptVisitorDepthFirst(visitor);
			}
		}
		if (retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER)
		{
			retval = visitor.visit(this);
		}
		return (retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_UP)
			? null : retval;
	}

	void clearParent()
	{
		parent = null;
	}

	void setParent(ISupportChilds parent)
	{
		this.parent = parent;
	}

	public int getID()
	{
		return element_id;
	}

	/*
	 * only called when cloning to set the clone on a new id.
	 */
	public void setID(int id)
	{
		element_id = id;
	}

	public void setRevisionNumber(int revision)
	{
		this.revision = revision;
		clearChanged();
	}

	public int getRevisionNumber()
	{
		return revision;
	}

	public boolean isChanged()
	{
		return isChanged;
	}

	public void flagChanged()
	{
		this.isChanged = true;
	}

	public void clearChanged()
	{
		isChanged = false;
		setRuntimeProperty(NameChangeProperty, null);
	}

	public IRootObject getRootObject()
	{
		return parent.getRootObject();
	}

	public ISupportChilds getParent()
	{
		return parent;
	}

	/*
	 * _____________________________________________________________ Methods from ISupportChilds only visible when subclasses implement ISupportChilds
	 */
	public void removeChild(IPersist obj)
	{
		internalRemoveChild(obj);
		if (getRootObject().getChangeHandler() != null)
		{
			getRootObject().getChangeHandler().fireIPersistRemoved(obj);
		}
	}

	protected void internalRemoveChild(IPersist obj)
	{
		if (allobjects != null)
		{
			allobjects.remove(obj);
			if (allobjectsMap != null && obj != null)
			{
				allobjectsMap.remove(obj.getUUID());
			}
		}
	}

	public void addChild(IPersist obj)
	{
		internalAddChild(obj);
		if (getRootObject().getChangeHandler() != null)
		{
			getRootObject().getChangeHandler().fireIPersistCreated(obj);
		}
		if (obj instanceof AbstractBase && this instanceof ISupportChilds)
		{
			((AbstractBase)obj).setParent((ISupportChilds)this);
		}
	}

	public void internalAddChild(IPersist obj)
	{
		if (allobjects == null)
		{
			allobjects = new ArrayList<IPersist>(3);
		}
		allobjects.add(obj);
		if (allobjectsMap != null && obj != null)
		{
			allobjectsMap.put(obj.getUUID(), obj);
		}
	}

	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return new TypeIterator<T>(getAllObjectsAsList(), tp);
	}

	public Iterator<IPersist> getAllObjects()
	{
		return getAllObjectsAsList().iterator();
	}

	public final List<IPersist> getAllObjectsAsList()
	{
		return allobjects == null ? Collections.<IPersist> emptyList() : Collections.unmodifiableList(allobjects);
	}

	public void internalClearAllObjects()
	{
		allobjects = null;
		allobjectsMap = null;
	}

	private void flushAllObjectsMap()
	{
		allobjectsMap = null;
	}

	public IPersist getChild(UUID childUuid)
	{
		if (allobjectsMap == null && allobjects != null && allobjects.size() > 0)
		{
			allobjectsMap = new HashMap<UUID, IPersist>(allobjects.size());
			for (IPersist persist : allobjects)
			{
				if (persist != null)
				{
					allobjectsMap.put(persist.getUUID(), persist);
				}
			}
		}
		return allobjectsMap == null ? null : allobjectsMap.get(childUuid);
	}

	public Iterator<IPersist> getAllObjects(Comparator<IPersist> c)
	{
		return new SortedTypeIterator<IPersist>(getAllObjectsAsList(), -1, c);
	}

	/**
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj != null && obj.getClass() == getClass())
		{
			AbstractBase abstractBase = (AbstractBase)obj;
			if (abstractBase.getUUID().equals(uuid))
			{
				if (getParent() != null && abstractBase.getParent() != null)
				{
					return getParent().equals(abstractBase.getParent());
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return element_id;
	}

	protected void checkForChange(boolean oldValue, boolean newValue)
	{
		if (oldValue != newValue)
		{
			isChanged = true;
		}
	}

	protected void checkForChange(int oldValue, int newValue)
	{
		if (oldValue != newValue)
		{
			isChanged = true;
		}
	}

	protected void checkForNameChange(String oldValue, String newValue)
	{
		if (oldValue == null && newValue == null) return;
		checkForChange(oldValue, newValue);
		if ((oldValue == null && newValue != null) || !oldValue.equals(newValue))
		{
			if (getRuntimeProperty(NameChangeProperty) == null) //only do once, first time
			{
				if (oldValue == null)
				{
					setRuntimeProperty(NameChangeProperty, "");
				}
				else
				{
					setRuntimeProperty(NameChangeProperty, oldValue);
				}
			}
		}
	}

	public static final RuntimeProperty<String> NameChangeProperty = new RuntimeProperty<String>()
	{
		private static final long serialVersionUID = 1L;
	};


	public static final SerializableRuntimeProperty<HashMap<UUID, Integer>> UUIDToIDMapProperty = new SerializableRuntimeProperty<HashMap<UUID, Integer>>()
	{
		private static final long serialVersionUID = 1L;
	};

	public static final SerializableRuntimeProperty<HashMap<String, String>> UnresolvedPropertyToValueMapProperty = new SerializableRuntimeProperty<HashMap<String, String>>()
	{
		private static final long serialVersionUID = 1L;
	};

	protected void checkForChange(Object oldValue, Object newValue)
	{
		if (isChanged) return;//no need to check

		boolean retval = false;

		//null checks
		if (oldValue == null)
		{
			retval = (newValue != null);
		}
		else
		{
			retval = (!oldValue.equals(newValue));
		}

		if (retval) isChanged = true;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	public IPersist clonePersist()
	{
		try
		{
			AbstractBase cloned = (AbstractBase)super.clone();
			cloned.allobjectsMap = null;
			if (cloned.allobjects != null)
			{
				cloned.allobjects = new ArrayList<IPersist>(allobjects.size());
				for (IPersist persist : allobjects)
				{
					if (persist instanceof ICloneable)
					{
						IPersist clonePersist = ((ICloneable)persist).clonePersist();
						cloned.addChild(clonePersist);
//						cloned.allobjects.add(clonePersist);
					}
					else
					{
						cloned.allobjects.add(persist);
					}
				}
			}
			return cloned;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}


	/**
	 * Make a clone of the current obj (also makes new repository entry)
	 * 
	 * @return a clone from this object
	 */
	public IPersist cloneObj(ISupportChilds newParent, boolean deep, IValidateName validator, boolean changeName, boolean changeChildNames)
		throws RepositoryException
	{
		if (getRootObject().getChangeHandler() == null)
		{
			throw new RepositoryException("cannot clone/copy without change handler");
		}
		AbstractBase clone = (AbstractBase)getRootObject().getChangeHandler().cloneObj(this, newParent);
		if (changeName && clone instanceof ISupportUpdateableName && ((ISupportUpdateableName)clone).getName() != null)
		{
			int random = new Random().nextInt(1024);
			String newName = ((ISupportUpdateableName)clone).getName() + "_copy" + random; //$NON-NLS-1$
			((ISupportUpdateableName)clone).updateName(validator, newName);
		}
		if (clone instanceof ISupportChilds)//do deep clone
		{
			clone.allobjectsMap = null;
			if (deep && allobjects != null)
			{
				clone.allobjects = new ArrayList<IPersist>(allobjects.size());
				Iterator<IPersist> it = this.getAllObjects();
				while (it.hasNext())
				{
					IPersist element = it.next();
					if (element instanceof IPersistCloneable)
					{
						((IPersistCloneable)element).cloneObj((ISupportChilds)clone, deep, validator, changeChildNames, changeChildNames);
					}
				}
			}
			else
			{
				clone.allobjects = null;//clear so they are not shared due to native clone !
			}
		}
		return clone;
	}

	public int getTypeID()
	{
		return type;
	}

	public UUID getUUID()
	{
		return uuid;
	}

	public void resetUUID()
	{
		resetUUID(null);
	}

	public void resetUUID(UUID uuidParam)
	{
		if (parent instanceof AbstractBase)
		{
			((AbstractBase)parent).flushAllObjectsMap();
		}
		if (uuidParam == null) uuid = UUID.randomUUID();
		else uuid = uuidParam;
	}

	public IPersist getAncestor(int typeId)
	{
		if (getTypeID() == typeId)
		{
			return this;
		}
		if (parent == null)
		{
			return null;
		}
		return parent.getAncestor(typeId);
	}

	public MetaData getMetaData()
	{
		return null;
	}

	public static <T extends ISupportName> T selectByName(Iterator<T> iterator, String name)
	{
		if (name == null || name.trim().length() == 0) return null;

		while (iterator.hasNext())
		{
			T n = iterator.next();
			if (n != null && name.equals(n.getName()))
			{
				return n;
			}
		}
		return null;
	}

	public static <T extends IPersist> T selectById(Iterator<T> iterator, int id)
	{
		while (iterator.hasNext())
		{
			T p = iterator.next();
			if (p.getID() == id)
			{
				return p;
			}
		}
		return null;
	}


	/**
	 * Set the customProperties
	 * 
	 * <b>Note: this call is only for (de)serialisation, use putCustomProperty to set specific custom properties </b>
	 * 
	 * @param arg the customProperties
	 * @throws JSONException
	 */
	public void setCustomProperties(String arg)
	{
		// special checkForChange
		if (!isChanged)
		{
			isChanged = (customProperties == null && arg != null) || (customProperties != null && !customProperties.equals(arg));
		}

		customProperties = arg;
		jsonCustomProperties = null;
	}

	/**
	 * Get the customProperties
	 * 
	 * <b>Note: this call is only for (de)serialisation, use getCustomProperty to get specific custom properties </b>
	 * 
	 * @return the customProperties
	 */
	public String getCustomProperties()
	{
		if (jsonCustomProperties != null)
		{
			customProperties = jsonCustomProperties.toString();
		}
		return customProperties;
	}

	@SuppressWarnings("unchecked")
	public Object getCustomProperty(String[] path)
	{
		if (customProperties == null) return null;

		if (jsonCustomProperties == null)
		{
			jsonCustomProperties = new JSONWrapperMap(customProperties);
		}
		Map<String, Object> map = jsonCustomProperties;
		for (int i = 0; i < path.length; i++)
		{
			if (map == null || !map.containsKey(path[i]))
			{
				return null;
			}
			Object node = ServoyJSONObject.toJava(map.get(path[i]));
			if (i == path.length - 1)
			{
				// leaf node
				return node;
			}
			map = (Map)node;
		}
		return null;
	}

	public Object putCustomProperty(String[] path, Object value)
	{
		if (customProperties == null && value == null) return null;

		if (jsonCustomProperties == null)
		{
			if (customProperties != null)
			{
				jsonCustomProperties = new JSONWrapperMap(customProperties);
			}
			else
			{
				jsonCustomProperties = new JSONWrapperMap(new ServoyJSONObject());
			}
		}

		Map<String, Object> map = jsonCustomProperties;
		for (int i = 0; i < path.length - 1; i++)
		{
			if (!map.containsKey(path[i]))
			{
				if (value == null)
				{
					return null; // value not found
				}
				map.put(path[i], new ServoyJSONObject());
			}
			map = (Map)map.get(path[i]);

		}
		String leaf = path[path.length - 1];
		Object old = null;
		if (value == null)
		{
			old = map.remove(leaf);
		}
		else
		{
			old = map.put(leaf, value);
		}
		if (!isChanged && ((old == null && value != null) || (old != null && !old.equals(value))))
		{
			flagChanged();
		}
		customProperties = jsonCustomProperties.toString();
		return old;
	}

	public List<Object> getInstanceMethodArguments(String methodKey)
	{
		if (methodKey != null)
		{
			return (List<Object>)getCustomProperty(new String[] { "methods", methodKey, "arguments" });
		}
		return null;
	}

	public List<Object> putInstancMethodArguments(String methodKey, List<Object> args)
	{
		if (methodKey != null)
		{
			return (List<Object>)putCustomProperty(new String[] { "methods", methodKey, "arguments" }, args == null ? null : Collections.unmodifiableList(args));
		}
		return null;
	}

	/* Runtime properties */
	/** Application level meta data. */
	private PropertyEntry[] properties;

	public <T extends Serializable> T getSerializableRuntimeProperty(SerializableRuntimeProperty<T> property)
	{
		return property.get(properties);
	}

	public <T extends Serializable> void setSerializableRuntimeProperty(SerializableRuntimeProperty<T> property, T object)
	{
		properties = property.set(properties, object);
	}

	private transient PropertyEntry[] transient_properties;

	public <T extends Object> T getRuntimeProperty(RuntimeProperty<T> property)
	{
		return property.get(transient_properties);
	}

	public <T extends Object> void setRuntimeProperty(RuntimeProperty<T> property, T object)
	{
		transient_properties = property.set(transient_properties, object);
	}

}
