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
package com.servoy.j2db.scripting;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.dltk.rhino.dbgp.LazyInitScope;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.debug.IDebuggerWithWatchPoints;

import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.util.Debug;

/**
 * @author jblok
 */
public abstract class LazyCompilationScope extends DefaultScope implements LazyInitScope
{
	protected volatile IExecutingEnviroment scriptEngine;
	private final Map<Integer, String> idVars; //id -> name (not in the same so it will not be in runtime env)
	private volatile Scriptable functionParent;//default this
	private volatile ISupportScriptProviders scriptLookup;

	public LazyCompilationScope(Scriptable parent, IExecutingEnviroment scriptEngine, ISupportScriptProviders scriptLookup)
	{
		super(parent);
		this.scriptLookup = scriptLookup;
		this.scriptEngine = scriptEngine;
		idVars = new HashMap<Integer, String>();
		functionParent = this;
		createScriptProviders();
	}

	// final because called from constructor
	protected final void createScriptProviders()
	{
		Iterator< ? extends IScriptProvider> it = scriptLookup.getScriptMethods(false);
		while (it.hasNext())
		{
			IScriptProvider sm = it.next();
			put(sm, sm);
		}
	}


	/**
	 * @return the scriptLookup
	 */
	public ISupportScriptProviders getScriptLookup()
	{
		return scriptLookup;
	}

	public void setFunctionParentScriptable(Scriptable functionParent)
	{
		this.functionParent = functionParent;
	}

	public Scriptable getFunctionParentScriptable()
	{
		return functionParent;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#has(int, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public boolean has(int index, Scriptable start)
	{
		return idVars.containsKey(new Integer(index));
	}

	public void put(IScriptProvider sm, Object function)
	{
		remove(sm);
		Object removed = allVars.put(sm.getDataProviderID(), function);
		idVars.put(sm.getID(), sm.getDataProviderID());
	}

	public Object remove(IScriptProvider sm)
	{
		String sName = idVars.remove(sm.getID());
		if (sName != null)
		{
			Object o = allVars.remove(sName);
			return o instanceof Function ? (Function)o : null;
		}
		return null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object o = getImpl(name, start);
		if (o != Scriptable.NOT_FOUND)
		{
			Context currentContext = Context.getCurrentContext();
			if (currentContext != null)
			{
				Debugger debugger = currentContext.getDebugger();
				if (debugger instanceof IDebuggerWithWatchPoints)
				{
					IDebuggerWithWatchPoints wp = (IDebuggerWithWatchPoints)debugger;
					wp.access(name, this);
				}
			}
		}
		return o;
	}

	/**
	 * @param name
	 * @param start
	 * @return
	 */
	protected final Object getImpl(String name, Scriptable start)
	{
		Object o = super.get(name, start);
		if (o instanceof IScriptProvider)
		{
			IScriptProvider sp = (IScriptProvider)o;
			try
			{
				Scriptable compileScope = functionParent;
				Scriptable functionSuper = getFunctionSuper(sp);
				if (functionSuper != null)
				{
					compileScope = new DefaultScope(functionParent)
					{
						@Override
						public String getClassName()
						{
							return "RunScope"; //$NON-NLS-1$
						}
					};
					compileScope.setPrototype(functionParent);
					// _formname_ is used in a lot of plugins (getParent of a function object, see FunctionDef) to get the form name
					compileScope.put("_formname_", compileScope, functionParent.get("_formname_", functionParent)); //$NON-NLS-1$ //$NON-NLS-2$
					compileScope.put("_super", compileScope, functionSuper); //$NON-NLS-1$
				}
				o = scriptEngine.compileFunction(sp, compileScope);
				put(name, start, o);//replace to prevent more compiles
			}
			catch (Exception e)
			{
				o = null;
				Debug.error(e);
				o = null;
			}
			if (o == null) o = Scriptable.NOT_FOUND;
		}
		return o;
	}

	/**
	 * @param sp
	 * @return
	 */
	protected Scriptable getFunctionSuper(IScriptProvider sp)
	{
		return null;
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.LazyInitScope#getInitializedIds()
	 */
	public Object[] getInitializedIds()
	{
		ArrayList array = new ArrayList(allVars.size() + allIndex.size() + 2);
		array.add("allnames");
		array.add("length");

		for (Object element : allVars.keySet())
		{
			String name = (String)element;
			Object o = super.get(name, this);
			if (!(o instanceof IScriptProvider))
			{
				array.add(name);
			}
		}
//		for (Iterator iter = allIndex.keySet().iterator(); iter.hasNext();)
//		{
//			array.add(iter.next());
//		}
		return array.toArray();
	}

	public String getFunctionName(Integer id)
	{
		return idVars.get(id);
	}

	public Function getFunctionByName(String name)
	{
		if (name == null) return null;
		Object o = getImpl(name, this);
		return (o instanceof Function ? (Function)o : null);
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		boolean has = super.has(name, start);
		if (!has)
		{
			return idVars.containsKey(name);
		}
		return has;
	}

	/**
	 * @param persist
	 */
	public void reload()
	{
		Iterator it = allVars.values().iterator();
		while (it.hasNext())
		{
			Object object = it.next();
			if (object instanceof IScriptProvider || object instanceof Function)
			{
				it.remove();
			}
		}

		idVars.clear();

		Iterator< ? extends IScriptProvider> itSp = scriptLookup.getScriptMethods(false);
		while (itSp.hasNext())
		{
			IScriptProvider sm = itSp.next();
			put(sm, sm);
		}
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "LazyCompilationScope parent '" + (getParentScope() == this ? "this" : getParentScope()) + "', scriptLookup: " + scriptLookup; //$NON-NLS-1$ 
	}

	@Override
	public void destroy()
	{
		this.scriptEngine = null;
		this.idVars.clear();
		this.functionParent = null;
		this.scriptLookup = null;
		super.destroy();
	}

}
