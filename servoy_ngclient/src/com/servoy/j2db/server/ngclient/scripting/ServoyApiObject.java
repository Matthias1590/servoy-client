/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ViewFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.NGClientWindow;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Provides utility methods for web object server side scripting to interact with the Servoy environment.
 * @author lvostinar
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ServoyApi", scriptingName = "servoyApi")
@ServoyClientSupport(sc = false, wc = false, ng = true)
public class ServoyApiObject
{
	private final INGApplication app;

	public ServoyApiObject(INGApplication app)
	{
		this.app = app;
	}

	@JSFunction
	/**
	 * Creates a view (read-only) foundset.
	 * @param name foundset name
	 * @param query query builder used to get the data for the foundset
	 * @return the view foundset
	 * @throws ServoyException
	 */
	public ViewFoundSet getViewFoundSet(String name, QBSelect query) throws ServoyException
	{
		if (!app.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
		return app.getFoundSetManager().getViewFoundSet(name, query, false);
	}

	/**
	 * Get select query for dataSource
	 * @param dataSource the dataSource
	 * @return QB select for the dataSource
	 * @throws ServoyException, RepositoryException
	 */
	@JSFunction
	public QBSelect getQuerySelect(String dataSource) throws ServoyException, RepositoryException
	{
		if (!app.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
		return (QBSelect)app.getFoundSetManager().getQueryFactory().createSelect(dataSource);
	}

	/**
	 * Hide a form directly on the server for instance when a tab will change on the client, so it won't need to do a round trip
	 * for hiding the form through the browser's component.
	 *
	 * @sample
	 * servoyApi.hideForm(formToHideName)
	 *
	 * @param formName the form to hide
	 * @return true if the form was hidden
	 */
	@JSFunction
	public boolean hideForm(String nameOrUUID)
	{
		String formName = nameOrUUID;
		Form form = app.getFlattenedSolution().getForm(nameOrUUID);
		if (form == null)
		{
			form = (Form)app.getFlattenedSolution().searchPersist(nameOrUUID);
			if (form != null)
			{
				formName = form.getName();
			}
		}
		IWebFormController formController = app.getFormManager().getForm(formName);
		if (formController != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ret = formController.notifyVisible(false, invokeLaterRunnables, true);
			if (ret)
			{
				formController.setParentFormController(null);
			}
			Utils.invokeAndWait(app, invokeLaterRunnables);
			return ret;
		}
		return false;
	}

	/**
	 * Show a form directly on the server for instance when a tab will change on the client, so it won't need to do a round trip
	 * for showing the form through the browser's component.
	 *
	 * @sample
	 * servoyApi.showForm(formToHideName)
	 *
	 * @param nameOrUUID the form to show
	 * @param parentForm the parent form
	 * @param parentComponent the parent container
	 * @param relationName the parent container
	 * @return true if the form was marked as visible
	 */
	@JSFunction
	public boolean showForm(String nameOrUUID, String parentForm, String parentComponent, String relationName)
	{
		String formName = nameOrUUID;
		Form form = app.getFlattenedSolution().getForm(nameOrUUID);
		if (form == null)
		{
			form = (Form)app.getFlattenedSolution().searchPersist(nameOrUUID);
			if (form != null)
			{
				formName = form.getName();
			}
		}
		IWebFormController formController = app.getFormManager().getForm(formName);
		IWebFormController parentFormController = null;
		WebFormComponent containerComponent = null;
		if (parentForm != null)
		{
			parentFormController = app.getFormManager().getForm(parentForm);
		}
		if (parentForm != null && parentComponent != null)
		{
			containerComponent = parentFormController.getFormUI().getWebComponent(parentComponent);
		}
		if (formController != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ret = formController.notifyVisible(true, invokeLaterRunnables, true);
			if (ret)
			{
				if (parentFormController != null)
				{
					parentFormController.getFormUI().getDataAdapterList().addVisibleChildForm(formController, relationName, true);
					if (containerComponent != null)
					{
						containerComponent.updateVisibleForm(parentFormController.getFormUI(), true, 0);
					}
				}

			}
			Utils.invokeAndWait(app, invokeLaterRunnables);

			if (ret)
			{
				NGClientWindow.getCurrentWindow().touchForm(app.getFlattenedSolution().getFlattenedForm(form), formName, true, true);
			}
			return ret;
		}
		return false;
	}

	/**
	 * Can be used to deep copy a custom value.
	 *
	 * @sample
	 * var eventSourceCopy = servoyApi.copyObject(eventSource);
	 *
	 * @param value the value to be copied
	 * @return a copy of the value object, the same as constructing the object in javascript from scratch
	 */
	@JSFunction
	public IdScriptableObject copyObject(Object value)
	{
		if (value instanceof NativeObject)
		{
			NativeObject nativeObject = new NativeObject();
			Object[] ids = ((NativeObject)value).getIds();
			for (Object id : ids)
			{
				Object objectValue = ((NativeObject)value).get(id.toString(), (NativeObject)value);
				if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
				{
					objectValue = copyObject(objectValue);
				}
				nativeObject.put(id.toString(), nativeObject, objectValue);
			}
			return nativeObject;
		}
		if (value instanceof NativeArray)
		{
			NativeArray arr = (NativeArray)value;
			Object[] values = new Object[arr.size()];
			for (int i = 0; i < arr.size(); i++)
			{
				Object objectValue = arr.get(i);
				if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
				{
					objectValue = copyObject(objectValue);
				}
				values[i] = objectValue;
			}
			return new NativeArray(values);
		}
		if (value instanceof RhinoMapOrArrayWrapper)
		{
			if (((RhinoMapOrArrayWrapper)value).getWrappedValue() instanceof Map)
			{
				NativeObject nativeObject = new NativeObject();
				Object[] ids = ((RhinoMapOrArrayWrapper)value).getIds();
				for (Object id : ids)
				{
					Object objectValue = ((RhinoMapOrArrayWrapper)value).get(id.toString(), (RhinoMapOrArrayWrapper)value);
					if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
					{
						objectValue = copyObject(objectValue);
					}
					nativeObject.put(id.toString(), nativeObject, objectValue);
				}
				return nativeObject;
			}
			else
			{
				Object[] ids = ((RhinoMapOrArrayWrapper)value).getIds();
				Object[] values = new Object[ids.length];
				for (int i = 0; i < ids.length; i++)
				{
					Object objectValue = ((RhinoMapOrArrayWrapper)value).get(i, (RhinoMapOrArrayWrapper)value);
					if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
					{
						objectValue = copyObject(objectValue);
					}
					values[i] = objectValue;
				}
				NativeArray nativeArray = new NativeArray(values);
				return nativeArray;
			}
		}
		Debug.error("cannot return object: " + value + " as NativeObject");
		return new NativeObject();
	}

	/**
	 * This will generate a url from a byte array so that the client can get the bytes from that url.
	 *
	 * @sample
	 * var url = servoyApi.getMediaUrl(bytes);
	 *
	 * @param bytes The value where an url should be created for
	 * @return the url where the bytes can be downloaded from
	 */
	@JSFunction
	public String getMediaUrl(byte[] bytes)
	{
		MediaResourcesServlet.MediaInfo mediaInfo = app.createMediaInfo(bytes);
		return mediaInfo.getURL(app.getWebsocketSession().getSessionKey().getClientnr());
	}


	/**
	 *	This will generate a list of primary keys names for the given data source.
	 *
	 * @sample
	 * var pkNames = servoyApi.getDatasourcePKs(datasource);
	 *
	 * @param datasource the data source
	 * @return a list of primary key names
	 * @throws ServoyException
	 */
	@JSFunction
	public String[] getDatasourcePKs(String datasource) throws ServoyException
	{
		if (!app.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
		List<String> listOfPrimaryKeyNames = new ArrayList<String>();
		ITable table = app.getFoundSetManager().getTable(datasource);
		if (table != null)
		{
			table.getRowIdentColumnNames().forEachRemaining(listOfPrimaryKeyNames::add);
		}

		return listOfPrimaryKeyNames.toArray(new String[0]);
	}

	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 * Will use any data filter defined on table.
	 *
	 * @sample
	 *  var dataset = servoyApi.getDataSetByQuery(qbselect, 10);
	 *
	 * @param query QBSelect query.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	@JSFunction
	public JSDataSet getDataSetByQuery(QBSelect query, Number max_returned_rows)
	{
		int _max_returned_rows = Utils.getAsInteger(max_returned_rows);

		String serverName = DataSourceUtils.getDataSourceServerName(query.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { query.getDataSource() }));
		QuerySelect select = query.build();

		try
		{
			return new JSDataSet(app, ((FoundSetManager)app.getFoundSetManager()).getDataSetByQuery(serverName, select,
				true, _max_returned_rows));
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * This is similar as calling foundset.js_addFoundSetFilterParam, but the main difference is that this
	 * works also on related foundsets.
	 *
	 * @param foundset The foundset to add the filter param/query to
	 * @param query The query repesenting the filter
	 * @param filterName a name given to this foundset filter
	 *
	 * @see Foundset.js_addFoundSetFilterParam
	 */
	@JSFunction
	public boolean addFoundSetFilterParam(FoundSet foundset, QBSelect query, String filterName)
	{
		return foundset.addFoundSetFilterParam(query, filterName);
	}
}
