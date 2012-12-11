/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.scripting.api.solutionmodel;

import com.servoy.j2db.persistence.constants.IFieldConstants;
import com.servoy.j2db.scripting.annotations.ServoyMobile;
import com.servoy.j2db.scripting.annotations.ServoyMobileFilterOut;


/**
 * Solution model field component (for mobile as well as other clients).
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyMobile
// TODO ac filter-out unsupported field types from servoy mobile
public interface IBaseSMField extends IBaseSMComponent
{

	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to text field. The field will show regular text on a single line.
	 * 
	 * @sample
	 * var tfield = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 460, 100, 20);
	 */
	public static final int TEXT_FIELD = IFieldConstants.TEXT_FIELD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to text area. The field will show text on multiple lines.
	 * 
	 * @sample
	 * var tarea = form.newField('my_table_text', JSField.TEXT_AREA, 10, 400, 100, 50);
	 */
	public static final int TEXT_AREA = IFieldConstants.TEXT_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to combobox. 
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public static final int COMBOBOX = IFieldConstants.COMBOBOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to radio buttons. The field will show a radio button, or a list of them if 
	 * the valuelist property is also set.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var radio = form.newField('my_table_options', JSField.RADIOS, 10, 280, 100, 50);
	 * radio.valuelist = vlist;
	 */
	public static final int RADIOS = IFieldConstants.RADIOS;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to checkbox. The field will show a checkbox, or a list of checkboxes if the valuelist
	 * property is also set.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var chk = form.newField('my_table_options', JSField.CHECKS, 10, 40, 100, 50);
	 * chk.valuelist = vlist;
	 */
	public static final int CHECKS = IFieldConstants.CHECKS;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the 
	 * field to calendar. The field will show a formatted date and will have a button which
	 * pops up a calendar for date selection.
	 *
	 * @sample 
	 * var cal = form.newField('my_table_date', JSField.CALENDAR, 10, 10, 100, 20);
	 */
	public static final int CALENDAR = IFieldConstants.CALENDAR;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * fiels to password. The field will allow the user to enter passwords, masking the typed
	 * characters.
	 * 
	 * @sample
	 * 	var pwd = form.newField('my_table_text', JSField.PASSWORD, 10, 250, 100, 20);
	 */
	public static final int PASSWORD = IFieldConstants.PASSWORD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the 
	 * field to RTF area. The field will display formatted RTF content.
	 * 
	 * @sample
	 * 	var rtf = form.newField('my_table_rtf', JSField.RTF_AREA, 10, 340, 100, 50);
	 */
	@ServoyMobileFilterOut
	public static final int RTF_AREA = IFieldConstants.RTF_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to HTML area. The field will display formatted HTML content.
	 * 
	 * @sample
	 * var html = form.newField('my_table_html', JSField.HTML_AREA, 10, 130, 100, 50);
	 */
	@ServoyMobileFilterOut
	public static final int HTML_AREA = IFieldConstants.HTML_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to image. The field will display images.
	 * 
	 * @sample
	 * var img = form.newField('my_table_image', JSField.IMAGE_MEDIA, 10, 190, 100, 50);
	 */
	public static final int IMAGE_MEDIA = IFieldConstants.IMAGE_MEDIA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to type ahead. The field will show regular text, but will have type ahead 
	 * capabilities.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var tahead = form.newField('my_table_text', JSField.TYPE_AHEAD, 10, 490, 100, 20);
	 * tahead.valuelist = vlist;
	 */
	@ServoyMobileFilterOut
	public static final int TYPE_AHEAD = IFieldConstants.TYPE_AHEAD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with single choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_list', JSField.LIST_BOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 */
	@Deprecated
	public static final int LIST_BOX = IFieldConstants.LIST_BOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with single choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_list', JSField.LISTBOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 */
	@ServoyMobileFilterOut
	public static final int LISTBOX = IFieldConstants.LIST_BOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with multiple choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_options', JSField.MULTISELECT_LISTBOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 */
	@ServoyMobileFilterOut
	public static final int MULTISELECT_LISTBOX = IFieldConstants.MULTISELECT_LISTBOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to spinner. The field will show a spinner.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var spinner = form.newField('my_spinner', JSField.SPINNER, 10, 460, 100, 20);
	 * spinner.valuelist = vlist;
	 */
	@ServoyMobileFilterOut
	public static final int SPINNER = IFieldConstants.SPINNER;

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getDataProviderID()
	 */
	public String getDataProviderID();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getDataProviderID()
	 */
	public boolean getDisplaysTags();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getDisplayType()
	 *
	 * @sample 
	 * // The display type is specified when the field is created.
	 * var cal = form.newField('my_table_date', JSField.CALENDAR, 10, 10, 100, 20);
	 * // But it can be changed if needed.
	 * cal.dataProviderID = 'my_table_text';
	 * cal.displayType = JSField.TEXT_FIELD;
	 */
	public int getDisplayType();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getValuelistID()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public IBaseSMValueList getValuelist();

	public void setDataProviderID(String arg);

	public void setDisplaysTags(boolean arg);

	public void setDisplayType(int arg);

	public void setValuelist(IBaseSMValueList valuelist);

	public void setOnAction(IBaseSMMethod method);

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getOnAction()
	 */
	public IBaseSMMethod getOnAction();

	public void setOnDataChange(IBaseSMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnDataChangeMethodID()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var onDataChangeMethod = form.newMethod('function onDataChange(oldValue, newValue, event) { application.output("Data changed from " + oldValue + " to " + newValue + " at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onDataChange = onDataChangeMethod;
	 * forms['someForm'].controller.show();
	 */
	public IBaseSMMethod getOnDataChange();

	public void setOnFocusGained(IBaseSMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusGainedMethodID()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var onFocusLostMethod = form.newMethod('function onFocusLost(event) { application.output("Focus lost at " + event.getTimestamp()); }');
	 * var onFocusGainedMethod = form.newMethod('function onFocusGained(event) { application.output("Focus gained at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onFocusGained = onFocusGainedMethod;
	 * field.onFocusLost = onFocusLostMethod;
	 * forms['someForm'].controller.show()
	 */
	public IBaseSMMethod getOnFocusGained();

	public void setOnFocusLost(IBaseSMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusLostMethodID()
	 * 
	 * @sampleas getOnFocusGained()
	 */
	public IBaseSMMethod getOnFocusLost();

}