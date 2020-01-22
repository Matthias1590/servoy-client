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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.ISmartPropertyValue;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.ComponentTypeFormElementValue;
import com.servoy.j2db.server.ngclient.property.ComponentTypeSabloValue;

/**
 * @author jcompagner
 * @since 8.4
 */
public class FormComponentSabloValue implements ISmartPropertyValue
{
	private final ComponentTypeSabloValue[] components;
	private final Form form;
	private final FormComponentCache cache;
	private final String elementStartName;

	public FormComponentSabloValue(List<FormElement> elements, PropertyDescription pd, DataAdapterList dal, WebFormComponent component, Form form,
		FormComponentCache cache)
	{
		this.form = form;
		this.cache = cache;
		this.components = new ComponentTypeSabloValue[elements.size()];
		this.elementStartName = FormElementHelper.getStartElementName(component.getFormElement(), pd);
		PropertyPath path = new PropertyPath();
		path.add(component.getName());
		path.add("containedForm");
		path.add("childElements");
		JSONObject tags = new JSONObject();
		tags.put(ComponentTypeSabloValue.TAG_ADD_TO_ELEMENTS_SCOPE, true);
		PropertyDescription compPd = new PropertyDescriptionBuilder().withName(pd.getName()).withType(ComponentPropertyType.INSTANCE).withConfig(
			pd.getConfig()).withTags(tags).build();
		int j = 0;
		for (int i = 0; i < components.length; i++)
		{
			FormElement element = elements.get(i);
			path.add(j);
			ComponentTypeFormElementValue elementValue = ComponentPropertyType.INSTANCE.getFormElementValue(null, compPd, path, element,
				dal.getApplication().getFlattenedSolution());
			ComponentTypeSabloValue ctsv = ComponentPropertyType.INSTANCE.toSabloComponentValue(elementValue, compPd, element, component, dal);
			if (ctsv != null) components[j++] = ctsv; // if it is null then it is probably a child component that was blocked by security (visibility == false); in that case just ignore it (similar to what portal does through .spec setting on comp. array to ignore null values at runtime)
			path.backOneLevel();
		}
	}

	public FormComponentCache getCache()
	{
		return cache;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectContext)
	{
		for (ComponentTypeSabloValue componentTypeSabloValue : components)
		{
			componentTypeSabloValue.attachToBaseObject(changeMonitor, webObjectContext);
		}
	}

	@Override
	public void detach()
	{
		for (ComponentTypeSabloValue componentTypeSabloValue : components)
		{
			componentTypeSabloValue.detach();
		}
	}

	public void fullToJSON(JSONWriter writer, FormComponentPropertyType formComponentPropertyType, IBrowserConverterContext dataConverterContext)
	{
		writer.object();
		writer.key("uuid");
		writer.value(cache.getHtmlTemplateUUIDForAngular());
		writer.key("formHeight");
		writer.value(form.getSize().height);
		writer.key("formWidth");
		writer.value(form.getSize().width);
		writer.key("absoluteLayout");
		writer.value(!form.isResponsiveLayout());
		writer.key(IContentSpecConstants.PROPERTY_USE_CSS_POSITION);
		writer.value(form.getUseCssPosition());
		writer.key("startName");
		writer.value(elementStartName);
		writer.key("childElements");
		writer.array();
		for (ComponentTypeSabloValue component : components)
		{
			component.fullToJSON(writer, ComponentPropertyType.INSTANCE);
		}
		writer.endArray();
		writer.endObject();
	}

	public void browserUpdatesReceived(JSONArray array)
	{
		for (int i = 0; i < array.length(); i++)
		{
			Object comp = array.get(i);
			components[i].browserUpdatesReceived(comp);
		}
	}

	public void changesToJSON(JSONWriter writer, FormComponentPropertyType formComponentPropertyType)
	{
		writer.object();
		writer.key("childElements");
		writer.array();
		for (ComponentTypeSabloValue component : components)
		{
			component.changesToJSON(writer, ComponentPropertyType.INSTANCE);
		}
		writer.endArray();
		writer.endObject();
	}

}
