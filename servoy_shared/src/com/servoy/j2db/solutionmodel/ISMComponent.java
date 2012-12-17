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

package com.servoy.j2db.solutionmodel;

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMComponent;


/**
 * Solution model base component interface.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMComponent extends IBaseSMComponent, ISMHasUUID, ISMHasDesignTimeProperty
{

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getBackground()
	 * 
	 * @sample
	 * // This property can be used on all types of components.
	 * // Here it is illustrated only for labels and fields.
	 * var greenLabel = form.newLabel('Green',10,10,100,50);
	 * greenLabel.background = 'green'; // Use generic names for colors.	
	 * var redField = form.newField('parent_table_text',JSField.TEXT_FIELD,10,110,100,30);
	 * redField.background = '#FF0000'; // Use RGB codes for colors.
	 */
	public String getBackground();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getBorderType()
	 * 
	 * @sample
	 * //HINT: To know exactly the notation of this property set it in the designer and then read it once out through the solution model.
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.borderType = solutionModel.createBorder(1,'#ff0000');;
	 */
	public String getBorderType();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getFontType()
	 * 
	 * @sample
	 * var label = form.newLabel('Text here', 10, 50, 100, 20);
	 * label.fontType = solutionModel.createFont('Times New Roman',1,14);
	 */
	public String getFontType();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getForeground()
	 * 
	 * @sample 
	 * // This property can be used on all types of components.
	 * // Here it is illustrated only for labels and fields.
	 * var labelWithBlueText = form.newLabel('Blue text', 10, 10, 100, 30);
	 * labelWithBlueText.foreground = 'blue'; // Use generic names for colors.
	 * var fieldWithYellowText = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 50, 100, 20);
	 * fieldWithYellowText.foreground = '#FFFF00'; // Use RGB codes for colors.
	 */
	public String getForeground();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getPrintSliding()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var slidingLabel = form.newLabel('Some long text here', 10, 10, 5, 5);
	 * slidingLabel.printSliding = SM_PRINT_SLIDING.GROW_HEIGHT | SM_PRINT_SLIDING.GROW_WIDTH;
	 * slidingLabel.background = 'gray';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public int getPrintSliding();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getTransparent()
	 * 
	 * @sample
	 * // Load an image from disk an create a Media object based on it.
	 * var imageBytes = plugins.file.readFile('d:/ball.jpg');
	 * var media = solutionModel.newMedia('ball.jpg', imageBytes);
	 * // Put on the form a label with the image.
	 * var image = form.newLabel('', 10, 10, 100, 100);
	 * image.imageMedia = media;
	 * // Put two fields over the image. The second one will be transparent and the
	 * // image will shine through.
	 * var nonTransparentField = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 20, 100, 20);
	 * var transparentField = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 50, 100, 20);
	 * transparentField.transparent = true;
	 */
	public boolean getTransparent();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getAnchors()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var strechAllDirectionsLabel = form.newLabel('Strech all directions', 10, 10, 380, 280);
	 * strechAllDirectionsLabel.background = 'red';
	 * strechAllDirectionsLabel.anchors = SM_ANCHOR.ALL;	
	 * var strechVerticallyLabel = form.newLabel('Strech vertically', 10, 10, 190, 280);
	 * strechVerticallyLabel.background = 'green';
	 * strechVerticallyLabel.anchors = SM_ANCHOR.WEST | SM_ANCHOR.NORTH | SM_ANCHOR.SOUTH;
	 * var strechHorizontallyLabel = form.newLabel('Strech horizontally', 10, 10, 380, 140);
	 * strechHorizontallyLabel.background = 'blue';
	 * strechHorizontallyLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST | SM_ANCHOR.EAST;
	 * var stickToTopLeftCornerLabel = form.newLabel('Stick to top-left corner', 10, 10, 200, 100);
	 * stickToTopLeftCornerLabel.background = 'orange';
	 * stickToTopLeftCornerLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST; // This is equivalent to SM_ANCHOR.DEFAULT 
	 * var stickToBottomRightCornerLabel = form.newLabel('Stick to bottom-right corner', 190, 190, 200, 100);
	 * stickToBottomRightCornerLabel.background = 'pink';
	 * stickToBottomRightCornerLabel.anchors = SM_ANCHOR.SOUTH | SM_ANCHOR.EAST;
	 */
	public int getAnchors();

	/**
	 * The Z index of this component. If two components overlap,
	 * then the component with higher Z index is displayed above
	 * the component with lower Z index.
	 *
	 * @sample
	 * var labelBelow = form.newLabel('Green', 10, 10, 100, 50);
	 * labelBelow.background = 'green';	
	 * labelBelow.formIndex = 10;
	 * var fieldAbove = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 30);
	 * fieldAbove.background = '#FF0000';
	 * fieldAbove.formIndex = 20;
	 */
	public int getFormIndex();

	public void setFormIndex(int arg);

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getPrintable()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var printedField = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var notPrintedField = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * notPrintedField.printable = false; // This field won't show up in print preview and won't be printed.
	 * forms['printForm'].controller.showPrintPreview()
	 */
	public boolean getPrintable();

	public void setBackground(String arg);

	public void setBorderType(String arg);

	public void setFontType(String arg);

	public void setForeground(String arg);

	public void setPrintSliding(int i);

	public void setTransparent(boolean arg);

	public void setAnchors(int arg);

	public void setPrintable(boolean arg);

}