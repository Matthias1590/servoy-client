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

package com.servoy.base.persistence.constants;

import com.servoy.base.solutionmodel.IBaseSMMethod;


/**
 * Constants used for alignment property.
 * 
 * @author acostescu
 */
public interface IDefaultSMConstants
{

	/**
	 * Constant used in various places to set properties to their default value.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 1200, 800);
	 * form.navigator = SM_DEFAULTS.DEFAULT; // Show the default navigator on the form.
	 */
	public static final int DEFAULT = 0;

	/**
	 * Constant used in various places to set properties to "none".
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 1200, 800);
	 * form.navigator = SM_DEFAULTS.NONE; // Hide the navigator on the form.
	 */
	public static final int NONE = -1;

	/**
	 * Constant used to remove a component from the tab sequence.
	 * 
	 * @sample
	 * // Create three fields. Based on how they are placed, by default they will come one
	 * // after another in the tab sequence.
	 * var fieldOne = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var fieldTwo = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * var fieldThree = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 70, 100, 20);
	 * // Set the third field come before the first in the tab sequence, and remove the 
	 * // second field from the tab sequence.
	 * fieldOne.tabSeq = 2;
	 * fieldTwo.tabSeq = SM_DEFAULTS.IGNORE;
	 * fieldThree.tabSeq = 1;
	 */
	public static final int IGNORE = -2;

//	/**
//	 * Constants used for setting commands to "default".
//	 * 
//	 * @sample
//	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 1200, 800);
//	 * form.onFindCmd = SM_DEFAULTS.COMMAND_DEFAULT; // This makes the find work like it does by default.
//	 */
//	public static final IBaseSMMethod COMMAND_DEFAULT = new ?();

	/**
	 * Constant used for setting commands to "none".
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 1200, 800);
	 * form.onFindCmd = SM_DEFAULTS.COMMAND_NONE; // This disables the find on the form.
	 */
	public static final IBaseSMMethod COMMAND_NONE = null;

}
