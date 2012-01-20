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

import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeValuelistComponent;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Utils;

/**
 * Represents a checkbox field in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataCheckBox extends WebBaseSelectBox
{

	public WebDataCheckBox(IApplication application, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, String id, String text, IValueList list)
	{
		this(application, scriptable, id, text);
		onValue = list;
	}


	public WebDataCheckBox(IApplication application, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, String id, String text)
	{
		super(application, scriptable, id, text);
	}


	@Override
	protected FormComponent getSelector(String id)
	{
		return new MyCheckBox(id);
	}


	// if this check box is linked to a integer non-null table column,
	// we must change null to "0" by default (as the user sees the check-box
	// unchecked - and when he tries to save he will not have null content problems)
	private void changeNullTo0IfNeeded()
	{
		IRecordInternal record = null;
		IModel model = getInnermostModel();
		if (model instanceof RecordItemModel)
		{
			record = ((RecordItemModel)model).getRecord();
		}
		if ((!allowNull) && record != null && !(record instanceof FindState) && record.getValue(dataProviderID) == null && dataType == IColumnTypes.INTEGER &&
			record.startEditing())
		{
			record.setValue(dataProviderID, new Integer(0));
		}
	}

	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		changeNullTo0IfNeeded(); // for record view
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

	private final class MyCheckBox extends CheckBox implements IResolveObject, IDisplayData
	{
		private static final long serialVersionUID = 1L;
		protected ITagResolver resolver;

		private MyCheckBox(String id)
		{
			super(id);
			setOutputMarkupPlaceholderTag(true);
			add(new AttributeModifier("disabled", true, new Model<String>() //$NON-NLS-1$
				{
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject()
					{
						return ((WebDataCheckBox.this.isEnabled() && !WebDataCheckBox.this.isReadOnly()) ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE
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
		}

		/**
		 * @see wicket.Component#isEnabled()
		 */
		@Override
		public boolean isEnabled()
		{
			IFormUIInternal< ? > formui = findParent(IFormUIInternal.class);
			if (formui != null && formui.isDesignMode())
			{
				return false;
			}
			return WebDataCheckBox.this.isEnabled();
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

		/*
		 * _____________________________________________________________ Methods for model object resolve
		 */
		public Object resolveDisplayValue(Object realVal)
		{
			if (onValue != null && onValue.getSize() >= 1)
			{
				Object real = onValue.getRealElementAt(0);
				if (real == null)
				{
					return new Boolean(realVal == null);
				}
				else
				{
					return new Boolean(real.equals(realVal));
				}
			}
			else
			{
				if (realVal instanceof Number)
				{
					return new Boolean(((Number)realVal).intValue() >= 1);
				}
				else if (realVal == null)
				{
					return new Boolean(false);
				}
				else
				{
					return new Boolean("1".equals(realVal.toString()));
				}
			}
		}

		@Override
		protected void onBeforeRender()
		{
			super.onBeforeRender();
			changeNullTo0IfNeeded(); // for table/list view
		}

		public void setTagResolver(ITagResolver resolver)
		{
			this.resolver = resolver;
		}

		public Object resolveRealValue(Object displayVal)
		{
			if (onValue != null && onValue.getSize() >= 1)
			{
				return (Utils.getAsBoolean(displayVal) ? onValue.getRealElementAt(0) : null);
			}
			else
			{
//		TODO this seems not possible in web and we don't have the previousRealValue			
//					// if value == null and still nothing selected return null (no data change)
//					if (previousRealValue == null && !Utils.getAsBoolean(displayVal))
//					{
//						return null;
//					}
				return new Integer((Utils.getAsBoolean(displayVal) ? 1 : 0));
			}
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValueObject()
		 */
		public Object getValueObject()
		{
			return WebDataCheckBox.this.getValueObject();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setValueObject(java.lang.Object)
		 */
		public void setValueObject(Object data)
		{
			WebDataCheckBox.this.setValueObject(data);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListener()
		 */
		public boolean needEditListener()
		{
			return WebDataCheckBox.this.needEditListener();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
		 */
		public boolean needEntireState()
		{
			return WebDataCheckBox.this.needEntireState();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setNeedEntireState(boolean)
		 */
		public void setNeedEntireState(boolean needEntireState)
		{
			WebDataCheckBox.this.setNeedEntireState(needEntireState);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#addEditListener(com.servoy.j2db.dataprocessing.IEditListener)
		 */
		public void addEditListener(IEditListener editListener)
		{
			WebDataCheckBox.this.addEditListener(editListener);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDataProviderID()
		 */
		public String getDataProviderID()
		{
			return WebDataCheckBox.this.getDataProviderID();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setDataProviderID(java.lang.String)
		 */
		public void setDataProviderID(String dataProviderID)
		{
			WebDataCheckBox.this.setDataProviderID(dataProviderID);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
		 */
		public Document getDocument()
		{
			return WebDataCheckBox.this.getDocument();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getFormat()
		 */
		public String getFormat()
		{
			return WebDataCheckBox.this.getFormat();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#notifyLastNewValueWasChange(java.lang.Object, java.lang.Object)
		 */
		public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
		{
			WebDataCheckBox.this.notifyLastNewValueWasChange(oldVal, newVal);
		}

		public boolean isValueValid()
		{
			return WebDataCheckBox.this.isValueValid();
		}

		public void setValueValid(boolean valid, Object oldVal)
		{
			WebDataCheckBox.this.setValueValid(valid, oldVal);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#setValidationEnabled(boolean)
		 */
		public void setValidationEnabled(boolean b)
		{
			WebDataCheckBox.this.setValidationEnabled(b);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#stopUIEditing(boolean)
		 */
		public boolean stopUIEditing(boolean looseFocus)
		{
			return WebDataCheckBox.this.stopUIEditing(looseFocus);
		}

		public boolean isReadOnly()
		{
			return WebDataCheckBox.this.isReadOnly();
		}

		@Override
		protected void onRender(final MarkupStream markupStream)
		{
			super.onRender(markupStream);

			IModel model = WebDataCheckBox.this.getInnermostModel();

			if (model instanceof RecordItemModel)
			{
				((RecordItemModel)model).updateRenderedValue(this);
			}
		}
	}

}
