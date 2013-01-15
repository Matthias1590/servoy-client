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

package com.servoy.j2db.server.headlessclient.dataui;

import java.util.Locale;

import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IComponentInheritedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.scripting.RuntimeRadioButton;
import com.servoy.j2db.util.Utils;

/**
 * Represents a radiobutton field in the webbrowser.
 * 
 * @author lvostinar
 *
 */
public class WebDataRadioButton extends WebBaseSelectBox
{
	private final IConverter converter = new RadioButtonConverter();

	public WebDataRadioButton(IApplication application, RuntimeRadioButton scriptable, String id, String text, IValueList list)
	{
		this(application, scriptable, id, text);
		onValue = list;
	}

	public WebDataRadioButton(IApplication application, RuntimeRadioButton scriptable, String id, String text)
	{
		super(application, scriptable, id, text);
	}

	public final RuntimeRadioButton getScriptObject()
	{
		return (RuntimeRadioButton)scriptable;
	}

	@Override
	protected FormComponent getSelector(String id)
	{
		return new MyRadioButton(id);
	}


	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	@Override
	public String toString()
	{
		return getScriptObject().toString("value:" + getDefaultModelObject()); //$NON-NLS-1$ 
	}

	public final class MyRadioButton extends FormComponent<Boolean> implements IDisplayData
	{
		private static final long serialVersionUID = 1L;

		private MyRadioButton(String id)
		{
			super(id);
			setOutputMarkupPlaceholderTag(true);
			add(new AttributeModifier("disabled", true, new Model<String>() //$NON-NLS-1$
				{
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject()
					{
						return ((WebDataRadioButton.this.isEnabled() && !WebDataRadioButton.this.isReadOnly()) ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE
							: AttributeModifier.VALUELESS_ATTRIBUTE_ADD);
					}
				}));
			add(new StyleAppendingModifier(new Model<String>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return "width:14px;height:14px;";
				}
			}));
			setType(Object.class);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.wicket.Component#initModel()
		 */
		@Override
		protected IModel< ? > initModel()
		{
			// Search parents for CompoundPropertyModel
			for (Component current = getParent(); current != null; current = current.getParent())
			{
				// Get model
				IModel< ? > model = current.getDefaultModel();

				if (model instanceof IWrapModel< ? >)
				{
					model = ((IWrapModel< ? >)model).getWrappedModel();
				}

				if (model instanceof IComponentInheritedModel< ? >)
				{
					// we turn off versioning as we share the model with another
					// component that is the owner of the model (that component
					// has to decide whether to version or not
					setVersioned(false);

					// return the shared inherited
					return ((IComponentInheritedModel< ? >)model).wrapOnInheritance(WebDataRadioButton.this);
				}
			}

			// No model for this component!
			return null;
		}

		/**
		 * @see FormComponent#supportsPersistence()
		 */
		@Override
		protected final boolean supportsPersistence()
		{
			return true;
		}

		@Override
		public final IConverter getConverter(Class< ? > type)
		{
			return converter;
		}


		/**
		 * Processes the component tag.
		 * 
		 * @param tag
		 *            Tag to modify
		 * @see org.apache.wicket.Component#onComponentTag(ComponentTag)
		 */
		@Override
		protected void onComponentTag(final ComponentTag tag)
		{
			checkComponentTag(tag, "input");
			checkComponentTagAttribute(tag, "type", "radio");

			final String value = getValue();
			Object valuelistValue = null;
			if (onValue != null && onValue.getSize() >= 1)
			{
				if (onValue.hasRealValues())
				{
					valuelistValue = onValue.getRealElementAt(0);
				}
				else
				{
					valuelistValue = onValue.getElementAt(0);
				}
			}
			boolean checked = Utils.equalObjects(value, valuelistValue);

			if (checked)
			{
				tag.put("checked", "checked");
			}
			else
			{
				// In case the attribute was added at design time
				tag.remove("checked");
			}

			// remove value attribute, because it overrides the browser's submitted value, eg a [input
			// type="checkbox" value=""] will always submit as false
			tag.remove("value");

			super.onComponentTag(tag);
		}

		/**
		 * @see wicket.Component#isEnabled()
		 */
		@Override
		public boolean isEnabled()
		{
			return WebDataRadioButton.this.isEnabled();
		}

		/**
		 * @see wicket.markup.html.form.FormComponent#getInputName()
		 */
		@Override
		public String getInputName()
		{
			if (inputId == null)
			{
				Page page = findPage();
				if (page instanceof MainPage)
				{
					inputId = ((MainPage)page).nextInputNameId();
				}
				else
				{
					return super.getInputName();
				}
			}
			return inputId;
		}


		public void setTagResolver(ITagResolver resolver)
		{
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValueObject()
		 */
		public Object getValueObject()
		{
			return WebDataRadioButton.this.getValueObject();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setValueObject(java.lang.Object)
		 */
		public void setValueObject(Object data)
		{
			WebDataRadioButton.this.setValueObject(data);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListener()
		 */
		public boolean needEditListener()
		{
			return WebDataRadioButton.this.needEditListener();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
		 */
		public boolean needEntireState()
		{
			return WebDataRadioButton.this.needEntireState();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setNeedEntireState(boolean)
		 */
		public void setNeedEntireState(boolean needEntireState)
		{
			WebDataRadioButton.this.setNeedEntireState(needEntireState);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#addEditListener(com.servoy.j2db.dataprocessing.IEditListener)
		 */
		public void addEditListener(IEditListener editListener)
		{
			WebDataRadioButton.this.addEditListener(editListener);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDataProviderID()
		 */
		public String getDataProviderID()
		{
			return WebDataRadioButton.this.getDataProviderID();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setDataProviderID(java.lang.String)
		 */
		public void setDataProviderID(String dataProviderID)
		{
			WebDataRadioButton.this.setDataProviderID(dataProviderID);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
		 */
		public Document getDocument()
		{
			return WebDataRadioButton.this.getDocument();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#notifyLastNewValueWasChange(java.lang.Object, java.lang.Object)
		 */
		public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
		{
			WebDataRadioButton.this.notifyLastNewValueWasChange(oldVal, newVal);
		}

		public boolean isValueValid()
		{
			return WebDataRadioButton.this.isValueValid();
		}

		public void setValueValid(boolean valid, Object oldVal)
		{
			WebDataRadioButton.this.setValueValid(valid, oldVal);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#setValidationEnabled(boolean)
		 */
		public void setValidationEnabled(boolean b)
		{
			WebDataRadioButton.this.setValidationEnabled(b);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#stopUIEditing(boolean)
		 */
		public boolean stopUIEditing(boolean looseFocus)
		{
			return WebDataRadioButton.this.stopUIEditing(looseFocus);
		}

		public boolean isReadOnly()
		{
			return WebDataRadioButton.this.isReadOnly();
		}

		@Override
		protected void onRender(final MarkupStream markupStream)
		{
			super.onRender(markupStream);

			IModel model = WebDataRadioButton.this.getInnermostModel();

			if (model instanceof RecordItemModel)
			{
				((RecordItemModel)model).updateRenderedValue(WebDataRadioButton.this);
			}
		}
	}

	public class RadioButtonConverter implements IConverter
	{
		private static final long serialVersionUID = 1L;

		/**
		 * Constructor
		 */
		private RadioButtonConverter()
		{

		}

		/**
		 * @see org.apache.wicket.util.convert.IConverter#convertToObject(java.lang.String,
		 *      java.util.Locale)
		 */
		public Object convertToObject(String value, Locale locale)
		{
			if ("on".equals(value))
			{
				if (onValue != null && onValue.getSize() >= 1)
				{
					if (onValue.hasRealValues())
					{
						return onValue.getRealElementAt(0);
					}
					else
					{
						return onValue.getElementAt(0);
					}
				}
			}
			return value;
		}

		/**
		 * @see org.apache.wicket.util.convert.IConverter#convertToString(java.lang.Object,
		 *      java.util.Locale)
		 */
		public String convertToString(Object value, Locale locale)
		{
			return value != null ? value.toString() : null;
		}
	}
}