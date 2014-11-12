/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertySabloValue;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class PersistFieldInstanceTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("WebComponentTest.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("WebComponentTest-mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws RepositoryException
	{
		solution.createNewForm(validator, null, "test", null, false, new Dimension(600, 400));
		ValueList valuelist = solution.createNewValueList(validator, "test");
		valuelist.setValueListType(IValueListConstants.CUSTOM_VALUES);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.component.AbstractSoluionTest#setupData()
	 */
	@Override
	protected void setupData() throws ServoyException
	{
	}

	@Test
	public void testFieldWithValueList() throws RepositoryException
	{
		Form form = solution.getForm("test");
		DataAdapterList dataAdapterList = new DataAdapterList(new TestFormController(form, client))
		{
			@Override
			protected boolean isFormDataprovider(String dataprovider)
			{
				return false;
			}

			@Override
			protected boolean isGlobalDataprovider(String dataprovider)
			{
				return false;
			}
		};

		Assert.assertNotNull(form);
		ValueList vl = solution.getValueList("test");
		Assert.assertNotNull(vl);

		Field field = form.createNewField(new Point(0, 0));
		field.setDataProviderID("mycolumn");
		field.setFormat("#,###.00");
		field.setDisplayType(Field.TYPE_AHEAD);
		field.setValuelistID(vl.getID());

		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new ServoyDataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		WebFormComponent wc = ComponentFactory.createComponent(client, dataAdapterList, formElements.get(0), null);
		Object property = wc.getProperty("valuelistID");
		Assert.assertTrue(property != null ? property.getClass().getName() : "null", property instanceof ValueListPropertySabloValue &&
			((ValueListPropertySabloValue)property).getValueList() instanceof CustomValueList);
		Assert.assertEquals("#,###.00", ((CustomValueList)((ValueListPropertySabloValue)property).getValueList()).getFormat().getDisplayFormat());
	}

	@Test
	public void testTabPanelWithTabs() throws RepositoryException
	{
		Form form = solution.getForm("test");
		Assert.assertNotNull(form);

		Form tabForm = solution.createNewForm(validator, null, "tabform", null, false, new Dimension(600, 400));
		DataAdapterList dataAdapterList = new DataAdapterList(new TestFormController(tabForm, client));

		TabPanel tabpanel = form.createNewTabPanel("tabpanel");
		tabpanel.createNewTab("tab1", null, tabForm);
		tabpanel.createNewTab("tab2", null, tabForm);

		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new ServoyDataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		WebFormComponent wc = ComponentFactory.createComponent(client, dataAdapterList, formElements.get(0), null);
		List<Map<String, Object>> tabs = (List)wc.getProperty("tabs");
		Assert.assertEquals(2, tabs.size());
		Map<String, Object> map = tabs.get(1);
		Assert.assertSame(tabForm.getName(), map.get("containsFormId"));
	}

	@Test
	public void testSettingTextOfTabInTabpanel() throws RepositoryException, JSONException
	{
		Form form = solution.getForm("test");
		Assert.assertNotNull(form);
		DataAdapterList dataAdapterList = new DataAdapterList(new TestFormController(form, client));

		Form tabForm = solution.createNewForm(validator, null, "tabform", null, false, new Dimension(600, 400));

		TabPanel tabpanel = form.createNewTabPanel("tabpanel");
		tabpanel.createNewTab("tab1", null, tabForm);
		tabpanel.createNewTab("tab2", null, tabForm);

		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new ServoyDataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		WebFormComponent wc = ComponentFactory.createComponent(client, dataAdapterList, formElements.get(0), null);
		TypedData<Map<String, Object>> changes = wc.getChanges();
		Assert.assertEquals(0, changes.content.size());

		List<Map<String, Object>> tabs = (List)wc.getProperty("tabs");
		Assert.assertEquals(2, tabs.size());
		Map<String, Object> map = tabs.get(0);
		map.put("text", "a test");
		changes = wc.getChanges();

		Assert.assertEquals(1, changes.content.size());

		String json = JSONUtils.writeChangesWithConversions(changes.content, changes.contentType);

		Assert.assertEquals(
			"{\"tabs\":{\"vEr\":1,\"u\":[{\"i\":0,\"v\":{\"vEr\":1,\"u\":[{\"k\":\"text\",\"v\":\"a test\"}]}}],\"conversions\":{\"0\":{\"v\":\"JSON_obj\"}}},\"conversions\":{\"tabs\":\"JSON_arr\"}}",
			json);

	}

	@Test
	public void testCustomComponentWithI18NProperty() throws RepositoryException, JSONException
	{
		Form form = solution.getForm("test");
		Assert.assertNotNull(form);

		DataAdapterList dataAdapterList = new DataAdapterList(new TestFormController(form, client));

		Bean bean = form.createNewBean("mycustombean", "my-component");
		bean.setInnerHTML("{atype:{name:'name',text:'i18n:servoy.button.ok'}}");
		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new ServoyDataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		WebFormComponent wc = ComponentFactory.createComponent(client, dataAdapterList, formElements.get(0), null);

		Map<String, Object> type = (Map<String, Object>)wc.getProperty("atype");
		Assert.assertEquals("name", type.get("name"));
		Assert.assertEquals("i18n:servoy.button.ok", type.get("text"));

		Assert.assertEquals(0, wc.getChanges().content.size());

		TypedData<Map<String, Object>> props = wc.getProperties();

		String json = JSONUtils.writeDataWithConversions(props.content, props.contentType);
		Assert.assertEquals(
			"{\"atype\":{\"vEr\":2,\"v\":{\"text\":\"OK\",\"name\":\"name\"}},\"svyMarkupId\":\"b31e38a4634ea9d002a6cdbfcfc786d0\",\"conversions\":{\"atype\":\"JSON_obj\"}}",
			json);
	}

	@Test
	public void testCustomComponentWithFormProperty() throws RepositoryException, JSONException
	{
		// TODO this should become a test on form uuid in the inner html/bean xml instead of the form name..

		Form form = solution.getForm("test");
		Assert.assertNotNull(form);
		DataAdapterList dataAdapterList = new DataAdapterList(new TestFormController(form, client));

		Form tabForm = solution.createNewForm(validator, null, "tabform", null, false, new Dimension(600, 400));

		Bean bean = form.createNewBean("mycustombean", "my-component");
		bean.setInnerHTML("{atype:{name:'name',form:'tabform'}}");
		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new ServoyDataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		WebFormComponent wc = ComponentFactory.createComponent(client, dataAdapterList, formElements.get(0), null);

		Map<String, Object> type = (Map<String, Object>)wc.getProperty("atype");
		Assert.assertEquals("name", type.get("name"));
		Assert.assertEquals("tabform", type.get("form"));

		Assert.assertEquals(0, wc.getChanges().content.size());

		TypedData<Map<String, Object>> props = wc.getProperties();

		String json = JSONUtils.writeDataWithConversions(props.content, props.contentType);
		Assert.assertEquals("{\"svyMarkupId\":\"b31e38a4634ea9d002a6cdbfcfc786d0\"}", json);
	}
}
