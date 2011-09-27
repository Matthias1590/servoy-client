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

package com.servoy.j2db.server.headlessclient;

import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * An implementation of {@link IExecuteEvent} that executes {@link Function} when {@link #execute()} is called.
 * Will set and reset all the wicket thread locals from the creation thread (the http thread) to the execution thread.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 */
public class FunctionEvent implements IExecuteEvent
{
	private final IFunctionExecutor functionExecutor;
	private final Function function;
	private final Scriptable scope;
	private final Scriptable thisObject;
	private final Object[] args;
	private final boolean focusEvent;
	private final boolean throwException;

	private final RequestCycle requestCycle;
	private final Session session;
	private final Application application;

	private volatile Object returnValue;
	private volatile Exception exception;
	private volatile boolean executed;
	private final List<IClusterable> dirtyObjectsList;
	private final List<Page> touchedPages;
	private final Thread currentThread;

	/**
	 * @param f
	 * @param scope
	 * @param thisObject
	 * @param args
	 * @param focusEvent
	 * @param throwException
	 * @param scriptEngine TODO
	 */
	public FunctionEvent(IFunctionExecutor functionExecutor, Function function, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent,
		boolean throwException)
	{
		this.functionExecutor = functionExecutor;
		this.function = function;
		this.scope = scope;
		this.thisObject = thisObject;
		this.args = args;
		this.focusEvent = focusEvent;
		this.throwException = throwException;

		requestCycle = RequestCycle.get();
		session = Session.get();
		application = Application.get();
		dirtyObjectsList = session.getDirtyObjectsList();
		touchedPages = session.getTouchedPages();
		currentThread = Thread.currentThread();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.IExecuteEvent#execute()
	 */
	public void execute()
	{
		try
		{
			ServoyRequestCycle.set(requestCycle);
			Session.set(session);
			Application.set(application);
			session.moveUsedPage(currentThread, Thread.currentThread());

			returnValue = functionExecutor.execute(function, scope, thisObject, args, focusEvent, throwException);
		}
		catch (Exception e)
		{
			exception = e;
		}
		finally
		{
			cleanup();
		}
	}

	/**
	 * 
	 */
	private void cleanup()
	{
		executed = true;

		List<IClusterable> lst = session.getDirtyObjectsList();
		for (IClusterable dirtyObject : lst)
		{
			if (!dirtyObjectsList.contains(dirtyObject))
			{
				dirtyObjectsList.add(dirtyObject);
			}
		}

		List<Page> pages = session.getTouchedPages();
		for (Page page : pages)
		{
			if (!touchedPages.contains(page))
			{
				touchedPages.add(page);
			}
		}

		session.moveUsedPage(Thread.currentThread(), currentThread);

		ServoyRequestCycle.set(null);
		Session.unset();
		Application.unset();
	}

	/**
	 * @return the returnValue
	 */
	public Object getReturnValue()
	{
		return returnValue;
	}

	/**
	 * @return the exception
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 * @return the executed
	 */
	public boolean isExecuted()
	{
		return executed;
	}

	/**
	 * 
	 */
	public void willSuspend()
	{
		cleanup();
	}

	/**
	 * 
	 */
	public void willResume()
	{
		executed = false;
		if (RequestCycle.get() == null) ServoyRequestCycle.set(requestCycle);
		if (Session.exists()) Session.set(session);
		if (Application.exists()) Application.set(application);
	}

}