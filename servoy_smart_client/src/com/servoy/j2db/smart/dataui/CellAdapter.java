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
package com.servoy.j2db.smart.dataui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.Style;
import javax.swing.text.html.CSS;
import javax.swing.text.html.StyleSheet;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ServoyBeanState;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.DisplaysAdapter;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.ListView;
import com.servoy.j2db.smart.TableView;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * Tableview renderer/editor wrapper (for use in swing)
 * @author jblok
 */
public class CellAdapter extends TableColumn implements TableCellEditor, TableCellRenderer, IDataAdapter, ItemListener, ActionListener, IEditListener
{
	private static final long serialVersionUID = 1L;

	private static final Object NONE = new Object();

	/** row to dataprovider map that holds a temp value to test lazy loading */
	private final Map<String, String> rowAndDataprovider = new HashMap<String, String>();

	//holds list of all displays
	private List<CellAdapter> displays;

	private final IApplication application;
	private final Component renderer;
	private final Component editor;
	private int editorX;
	private int editorWidth;
	private final String dataProviderID;
	private IRecordInternal currentEditingState;
	private DataAdapterList dal;
	private final TableView table;
	private boolean opaque = true;

	private Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	protected static JComponent empty = new JLabel();

	// We need a place to store the color the JLabel should be returned
	// to after its foreground and background colors have been set
	// to the selection background color.
	// These ivars will be made protected when their names are finalized.
	private Color unselectedForeground;
	private Color unselectedBackground;

	private Font unselectedFont;

	private Color lastEditorBgColor;
	private Color lastEditorFgColor;

	private Font lastEditorFont;

	private boolean adjusting = false;
	private Object lastInvalidValue = NONE;

	public CellAdapter(IApplication app, final TableView table, final int index, int width, String name, String title, String dataProviderID,
		Component renderer, Component editor)
	{
		super(index, width);
		this.application = app;
		this.table = table;
		this.dataProviderID = dataProviderID;
		this.renderer = renderer;
		if (renderer != null)
		{
			renderer.addComponentListener(new ComponentListener()
			{
				public void componentShown(ComponentEvent e)
				{
					int i = table.getColumnModel().getColumnCount();
					table.getColumnModel().addColumn(CellAdapter.this);

					// make sure it appears back in it's design position
					int realColumnIndex = getRealColumnIndex(table.getColumnModel().getColumns(), index);
					if (realColumnIndex < i)
					{
						table.getColumnModel().moveColumn(i, realColumnIndex);
					}
				}

				private int getRealColumnIndex(Enumeration<TableColumn> columns, int idx)
				{
					// some other columns (with indexes < index) might be hidden (removed from the table)
					// - so in order to correctly position this column we must see the real column index in the
					// table where is should appear - based on index and the indexes of other columns (hidden & shown);
					// visible columns are still added to the table column model
					int realIndex = 0;

					while (columns.hasMoreElements())
					{
						TableColumn ca = columns.nextElement();
						if (ca.getModelIndex() < idx)
						{
							realIndex++;
						}
					}

					return realIndex;
				}

				public void componentHidden(ComponentEvent e)
				{
					table.getColumnModel().removeColumn(CellAdapter.this);
				}

				public void componentResized(ComponentEvent e)
				{
				}

				public void componentMoved(ComponentEvent e)
				{
				}
			});
			opaque = renderer.isOpaque();
			if (renderer instanceof JComponent)
			{
				Border rBorder = ((JComponent)renderer).getBorder();
				noFocusBorder = rBorder;
			}
		}
		this.editor = editor;
		if (editor != null)
		{
			updateEditorX();
			updateEditorWidth();

			editor.addComponentListener(new ComponentListener()
			{
				public void componentHidden(ComponentEvent e)
				{
				}

				public void componentMoved(ComponentEvent e)
				{
					int sourceX = (e.getComponent() instanceof ISupportCachedLocationAndSize)
						? ((ISupportCachedLocationAndSize)e.getComponent()).getCachedLocation().x : e.getComponent().getLocation().x;
					if (editorX != sourceX) // see also updateEditorX and updateEditorWidth call hierarchy to understand this better
					{
						onEditorChanged();
						editorX = sourceX; // just to make sure
					}
				}

				public void componentResized(ComponentEvent e)
				{
					int sourceWidth = (e.getComponent() instanceof ISupportCachedLocationAndSize)
						? ((ISupportCachedLocationAndSize)e.getComponent()).getCachedSize().width : e.getComponent().getSize().width;
					if (editorWidth != sourceWidth) // see also updateEditorX and updateEditorWidth call hierarchy to understand this better
					{
						onEditorChanged();
						editorWidth = sourceWidth; // just to make sure
					}
				}

				public void componentShown(ComponentEvent e)
				{
				}

			});
		}

		super.setCellRenderer(this);
		super.setCellEditor(this);

		if (title == null || title.length() == 0)
		{
			setHeaderValue(" "); //$NON-NLS-1$
		}
		else
		{
			setHeaderValue(title);
		}

		setIdentifier(editor != null ? editor.getName() : name);

		if (editor instanceof DataCheckBox)
		{
			((DataCheckBox)editor).addItemListener(this);
			((DataCheckBox)editor).setText(""); //$NON-NLS-1$
			((DataCheckBox)renderer).setText(""); //$NON-NLS-1$
		}
		else if (editor instanceof DataComboBox)
		{
			((DataComboBox)editor).addItemListener(this);
		}
		else if (editor instanceof DataCalendar)
		{
			((DataCalendar)editor).addActionListner(this);
			((DataCalendar)editor).addEditListener(this);
		}
		else if (editor instanceof DataField)
		{
			((DataField)editor).addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					if (!e.isTemporary())
					{
						application.invokeLater(new Runnable()
						{
							public void run()
							{
								// if it already has focus again, igore this.
								if (CellAdapter.this.editor.hasFocus()) return;
								// if the tableview itself has focus also ignore this
								if (CellAdapter.this.table.hasFocus()) return;
								// only stop the cell editor if it is still this one.
								TableCellEditor tableCellEditor = table.getCellEditor();
								if (tableCellEditor == CellAdapter.this) tableCellEditor.stopCellEditing();
							}
						});
					}
				}
			});
			((DataField)editor).addEditListener(this);
		}
		else if (editor instanceof IDisplayData)
		{
			((IDisplayData)editor).addEditListener(this);
		}

		// editor is never transparent
		// if the editor must be transparent, then make sure that the getBackGround call done below 
		// is done on a none transparent field (and reset the transparency)
		if (!editor.isOpaque() && editor instanceof JComponent)
		{
			((JComponent)editor).setOpaque(true);
		}

		unselectedBackground = editor.getBackground();
		unselectedForeground = editor.getForeground();
		unselectedFont = editor.getFont();

	}

	public void setDataAdapterList(DataAdapterList dal)
	{
		this.dal = dal;
		if (editor instanceof IDisplayData)
		{
			((IDisplayData)editor).setTagResolver(dal);
		}
	}

	private boolean findMode = false;

	public void setFindMode(boolean b)
	{
		findMode = b;
		if (editor instanceof IDisplayData)
		{
			IDisplayData dt = (IDisplayData)editor;
			if (!(dal.getFormScope() != null && dt.getDataProviderID() != null && dal.getFormScope().get(dt.getDataProviderID()) != Scriptable.NOT_FOUND)) // skip for form variables
			{
				dt.setValidationEnabled(!b);
			}
		}
		if (renderer instanceof IDisplayData)
		{
			IDisplayData dt = (IDisplayData)renderer;
			if (!(dal.getFormScope() != null && dt.getDataProviderID() != null && dal.getFormScope().get(dt.getDataProviderID()) != Scriptable.NOT_FOUND)) // skip for form variables
			{
				dt.setValidationEnabled(!b);
			}
		}
		if (displays != null)
		{
			for (int d = 0; d < displays.size(); d++)
			{
				displays.get(d).setFindMode(b);
			}
		}
		if (table != null && !table.isEditing()) currentEditingState = null;
	}

	/*
	 * @see TableCellEditor#getTableCellEditorComponent(JTable, Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable jtable, Object value, boolean isSelected, int row, int column)
	{
		Color bgColor = getBgColor(jtable, isSelected, row, true);
		Color fgColor = getFgColor(jtable, isSelected, row);
		Font font = getFont(jtable, isSelected, row);

		Component cellEditorComp = getTableCellEditorComponentEx(jtable, value, isSelected, row, column, bgColor, fgColor, font);
		if (cellEditorComp instanceof ISupportOnRenderCallback)
		{
			RenderEventExecutor renderEventExecutor = ((ISupportOnRenderCallback)cellEditorComp).getRenderEventExecutor();
			if (renderEventExecutor != null && renderEventExecutor.hasRenderCallback())
			{
				ISwingFoundSet foundset = (ISwingFoundSet)jtable.getModel();
				IRecordInternal record = foundset != null ? foundset.getRecord(row) : null;
				renderEventExecutor.setRenderState(record, row, isSelected);
			}
		}
		return cellEditorComp;
	}

	private Component getTableCellEditorComponentEx(JTable jtable, Object value, boolean isSelected, int row, int column, Color bgColorParam,
		Color fgColorParam, Font fontParam)
	{
		if (editor == null || !isVisible(editor) || !(jtable.getModel() instanceof IFoundSetInternal))
		{
			return empty;
		}

		IRecordInternal newRec = ((IFoundSetInternal)jtable.getModel()).getRecord(row);

		if (isSelected)
		{
			Color bgColor = bgColorParam;
			if (bgColor == null)
			{
				bgColor = unselectedBackground; // unselected background is the default background color of the editor.
			}
			lastEditorBgColor = bgColor;
			editor.setBackground(bgColor);


			Color fgColor = fgColorParam;
			if (fgColor == null)
			{
				fgColor = unselectedForeground; // unselected foreground is the default foreground color of the editor.
			}
			lastEditorFgColor = fgColor;
			editor.setForeground(fgColor);

			Font font = fontParam;
			if (font == null)
			{
				font = unselectedFont; // unselected font is the default font of the editor.
			}
			lastEditorFont = font;
			editor.setFont(font);
		}

//		try
//		{
//			if (currentEditingState != null && newRec != currentEditingState && currentEditingState.isEditing())
//			{
//				currentEditingState.stopEditing();
//			}
//		}
//		catch (Exception e)
//		{
//			Debug.error(e);
//		}
		currentEditingState = newRec;

		if (currentEditingState != null)
		{
			// if not enabled or not editable do not start the edit
			if (editor instanceof IDisplay && ((IDisplay)editor).isEnabled() && !((IDisplay)editor).isReadOnly())
			{
				DisplaysAdapter.startEdit(dal, (IDisplay)editor, currentEditingState, findMode);
			}


			if (editor instanceof IDisplayRelatedData)
			{
				IDisplayRelatedData drd = (IDisplayRelatedData)editor;

				IRecordInternal state = ((IFoundSetInternal)jtable.getModel()).getRecord(row);
				if (state != null)
				{
					drd.setRecord(state, true);
				}
			}
			if (editor instanceof IDisplayData)// && dataProviderID != null)
			{
				try
				{
					Object data = dal.getValueObject(currentEditingState, dataProviderID);
					if (data instanceof DbIdentValue)
					{
						data = ((DbIdentValue)data).getPkValue();
					}
					((IDisplayData)editor).setValueObject(data);
				}
				catch (IllegalArgumentException iae)
				{
					Debug.error(iae);
				}
			}
			if (editor instanceof IServoyAwareBean)
			{
				((IServoyAwareBean)editor).setSelectedRecord(new ServoyBeanState(currentEditingState, dal.getFormScope()));
			}
		}
		return editor;
	}

	/*
	 * @see TableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
	 */

	public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected, boolean hasFocus, final int row, final int column)
	{
		Color bgColor = getBgColor(jtable, isSelected, row, false);
		Color fgColor = getFgColor(jtable, isSelected, row);
		Font font = getFont(jtable, isSelected, row);

		Component cellRendererComp = getTableCellRendererComponentEx(jtable, value, isSelected, hasFocus, row, column, bgColor, fgColor, font);
		if (cellRendererComp instanceof ISupportOnRenderCallback)
		{
			RenderEventExecutor renderEventExecutor = ((ISupportOnRenderCallback)cellRendererComp).getRenderEventExecutor();
			if (renderEventExecutor != null && renderEventExecutor.hasRenderCallback())
			{
				ISwingFoundSet foundset = (ISwingFoundSet)jtable.getModel();
				IRecordInternal record = foundset != null ? foundset.getRecord(row) : null;
				renderEventExecutor.setRenderState(record, row, isSelected);
			}
		}
		return cellRendererComp;
	}

	private boolean isVisible(Component comp)
	{
		boolean isVisible = false;
		if (comp != null)
		{
			if (comp instanceof ISupportOnRenderCallback && ((ISupportOnRenderCallback)comp).getRenderEventExecutor().hasRenderCallback() &&
				((ISupportOnRenderCallback)comp).getRenderEventExecutor().isComponentHiddenOnRender())
			{
				isVisible = true;
			}
			else
			{
				isVisible = comp.isVisible();
			}
		}

		return isVisible;
	}

	private Component getTableCellRendererComponentEx(JTable jtable, Object value, boolean isSelected, boolean hasFocus, final int row, final int column,
		Color bgColor, Color fgColor, Font font)
	{
		if (renderer == null || !isVisible(renderer) || !(jtable.getModel() instanceof IFoundSetInternal))
		{
			return empty;
		}

		final ISwingFoundSet foundset = (ISwingFoundSet)jtable.getModel();
		final IRecordInternal state;
		try
		{
			state = foundset.getRecord(row);
		}
		catch (RuntimeException re)
		{
			Debug.error("Error getting row ", re); //$NON-NLS-1$
			return empty;
		}

		// set the sizes of the to render component also in the editor if the editor is not used.
		// so that getLocation and getWidth in scripting on tableviews do work.
		if (editor != null && editor.getParent() == null)
		{
			Rectangle cellRect = jtable.getCellRect(row, column, false);
			editor.setLocation(cellRect.x, cellRect.y);
			editor.setSize(cellRect.width, cellRect.height);
		}

		if (isSelected)
		{
			Color tableSelectionColor = jtable.getSelectionForeground();
			if (bgColor != null)
			{
				int red = Math.abs(tableSelectionColor.getRed() - bgColor.getRed());
				int blue = Math.abs(tableSelectionColor.getBlue() - bgColor.getBlue());
				int green = Math.abs(tableSelectionColor.getGreen() - bgColor.getBlue());

				if (red < 128 && blue < 128 && green < 128)
				{
					red = Math.abs(tableSelectionColor.getRed() - 255);
					blue = Math.abs(tableSelectionColor.getBlue() - 255);
					green = Math.abs(tableSelectionColor.getGreen() - 255);
					tableSelectionColor = new Color(red, blue, green);
				}
			}

			renderer.setForeground(fgColor != null ? fgColor : tableSelectionColor);
			renderer.setBackground((bgColor != null ? bgColor : jtable.getSelectionBackground()));
			if (!renderer.isOpaque() && renderer instanceof JComponent)
			{
				((JComponent)renderer).setOpaque(true);
			}
			if (font != null) renderer.setFont(font);
		}
		else
		{
			// now get the editors background. if we don't do that then scripting doesn't show up
			Color background = editor.getBackground();
			if (background != null && !background.equals(lastEditorBgColor))
			{
				unselectedBackground = background;
			}
			Color foreground = editor.getForeground();
			if (foreground != null && !foreground.equals(lastEditorFgColor))
			{
				unselectedForeground = foreground;
			}
			Font editorFont = editor.getFont();
			if (editorFont != null && !editorFont.equals(lastEditorFont))
			{
				unselectedFont = editorFont;
			}

			if (editor instanceof IDisplayData && ((IDisplayData)editor).isValueValid() || !(editor instanceof IDisplayData))
			{
				Color currentForeground = (fgColor != null ? fgColor : (unselectedForeground != null) ? unselectedForeground : jtable.getForeground());
				renderer.setForeground(currentForeground);
			}
			Color currentColor = (bgColor != null ? bgColor : (unselectedBackground != null) ? unselectedBackground : jtable.getBackground());
			renderer.setBackground(currentColor);
			boolean currentOpaque = opaque || !(currentColor == null || currentColor instanceof ColorUIResource);
			if (renderer.isOpaque() != currentOpaque && renderer instanceof JComponent)
			{
				((JComponent)renderer).setOpaque(currentOpaque);
			}

			Font currentFont = (font != null ? font : (unselectedFont != null) ? unselectedFont : jtable.getFont());
			renderer.setFont(currentFont);
		}

		if (renderer instanceof JComponent)
		{
			JComponent borderOwner = (JComponent)renderer;
			Border adjustedBorder = null;
			if (!hasFocus) adjustedBorder = noFocusBorder;
			else
			{
				adjustedBorder = UIManager.getBorder("Table.focusCellHighlightBorder"); //$NON-NLS-1$
				if (noFocusBorder != null)
				{
					if (noFocusBorder instanceof CompoundBorder)
					{
						Border insideBorder = ((CompoundBorder)noFocusBorder).getInsideBorder();
						Border outsideBorder = ((CompoundBorder)noFocusBorder).getOutsideBorder();
						if (outsideBorder instanceof SpecialMatteBorder) adjustedBorder = new CompoundBorder(adjustedBorder, noFocusBorder);
						else adjustedBorder = new CompoundBorder(adjustedBorder, insideBorder);
					}
					else if (noFocusBorder instanceof SpecialMatteBorder)
					{
						adjustedBorder = new CompoundBorder(adjustedBorder, noFocusBorder);
					}
					else
					{
						// keep the renderer content at the same position,
						// create a focus border with the same border insets
						Insets noFocusBorderInsets = noFocusBorder.getBorderInsets(borderOwner);
						Insets adjustedBorderInsets = adjustedBorder.getBorderInsets(borderOwner);
						EmptyBorder emptyInsideBorder = new EmptyBorder(Math.max(0, noFocusBorderInsets.top - adjustedBorderInsets.top), Math.max(0,
							noFocusBorderInsets.left - adjustedBorderInsets.left), Math.max(0, noFocusBorderInsets.bottom - adjustedBorderInsets.bottom),
							Math.max(0, noFocusBorderInsets.right - adjustedBorderInsets.right));

						adjustedBorder = new CompoundBorder(adjustedBorder, emptyInsideBorder);
					}
				}
			}
			borderOwner.setBorder(adjustedBorder);
		}

		boolean printing = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
		if (renderer instanceof IDisplayRelatedData)
		{
			IDisplayRelatedData drd = (IDisplayRelatedData)renderer;
			String relationName = drd.getSelectedRelationName();
			if (state != null)
			{
				if (relationName != null)
				{
					if (!printing && !state.isRelatedFoundSetLoaded(relationName, null))
					{
						IApplication app = dal.getApplication();
						((IDisplayData)renderer).setValueObject(null);
						String key = row + "_" + relationName + "_" + null; //$NON-NLS-1$ //$NON-NLS-2$
						if (!rowAndDataprovider.containsKey(key))
						{
							rowAndDataprovider.put(key, key);
							Runnable r = new ASynchonizedCellLoad(app, jtable, foundset, row, jtable.convertColumnIndexToModel(column), relationName,
								drd.getDefaultSort(), null);
							app.getScheduledExecutor().execute(r);
						}
						return renderer;
					}
				}
				drd.setRecord(state, true);
			}
		}
		if (renderer instanceof IDisplayData)
		{
			if (state != null)
			{
				Object data = null;
				if (dataProviderID != null)
				{
					int index = -1;
					if (!printing && (index = dataProviderID.indexOf('.')) > 0)
					{
						String partName = dataProviderID.substring(0, index);
						final String restName = dataProviderID.substring(index + 1);
						if (!partName.equals(ScriptVariable.GLOBAL_PREFIX))
						{
							String relationName = partName;
							if (relationName != null && !(state.isRelatedFoundSetLoaded(relationName, restName)))
							{
								IApplication app = dal.getApplication();
								((IDisplayData)renderer).setValueObject(null);
								String key = row + "_" + relationName + "_" + restName; //$NON-NLS-1$ //$NON-NLS-2$
								if (!rowAndDataprovider.containsKey(key))
								{
									rowAndDataprovider.put(key, key);
									Runnable r = new ASynchonizedCellLoad(app, jtable, foundset, row, jtable.convertColumnIndexToModel(column), relationName,
										null, restName);
									app.getScheduledExecutor().execute(r);
								}
								return renderer;
							}
							IFoundSetInternal rfs = state.getRelatedFoundSet(relationName, null);
							if (rfs != null)
							{
								int selected = rfs.getSelectedIndex();
								// in printing selected row will be -1, but aggregates
								// should still go through record 0
								if (selected == -1 && rfs.getSize() > 0)
								{
									selected = 0;
								}
								final IRecordInternal relState = rfs.getRecord(selected);
								if (testCalc(restName, relState, row, jtable.convertColumnIndexToModel(column), foundset)) return renderer;
							}
						}
					}
					if (!((IDisplayData)renderer).needEntireState() && !printing &&
						testCalc(dataProviderID, state, row, jtable.convertColumnIndexToModel(column), foundset))
					{
						return renderer;
					}
					try
					{
						data = dal.getValueObject(state, dataProviderID);
					}
					catch (IllegalArgumentException iae)
					{
						Debug.error(iae);
						data = "<conversion error>"; //$NON-NLS-1$
					}
				}
				((IDisplayData)renderer).setTagResolver(new ITagResolver()
				{
					public String getStringValue(String name)
					{
						return TagResolver.formatObject(dal.getValueObject(state, name), dal.getApplication().getSettings());
					}
				});
				if (data instanceof DbIdentValue)
				{
					data = ((DbIdentValue)data).getPkValue();
				}
				((IDisplayData)renderer).setValueObject(data);
			}
		}
		if (renderer instanceof IServoyAwareBean && state != null)
		{
			((IServoyAwareBean)renderer).setSelectedRecord(new ServoyBeanState(state, dal.getFormScope()));
		}

		return renderer;
	}

	private Object getStyleAttributeForRow(JTable jtable, boolean isSelected, int row, ISupportRowStyling.ATTRIBUTE rowStyleAttribute)
	{
		Object rowStyleAttrValue = null;
		IRecordInternal state = ((IFoundSetInternal)jtable.getModel()).getRecord(row);
		boolean specialStateCase = (state instanceof PrototypeState || state instanceof FindState);
		if (/* !(renderer instanceof JButton) && */!specialStateCase)
		{
			if (jtable instanceof ISupportRowStyling)
			{
				ISupportRowStyling oddEvenStyling = (ISupportRowStyling)jtable;

				StyleSheet ss = oddEvenStyling.getRowStyleSheet();
				Style style = isSelected ? oddEvenStyling.getRowSelectedStyle() : null;
				if (style != null && style.getAttributeCount() == 0) style = null;
				if (style == null)
				{
					style = (row % 2 == 0) ? oddEvenStyling.getRowOddStyle() : oddEvenStyling.getRowEvenStyle(); // because index = 0 means record = 1	
				}

				if (ss != null && style != null)
				{
					switch (rowStyleAttribute)
					{
						case BGCOLOR :
							rowStyleAttrValue = style.getAttribute(CSS.Attribute.BACKGROUND_COLOR) != null ? ss.getBackground(style) : null;
							break;
						case FGCOLOR :
							rowStyleAttrValue = style.getAttribute(CSS.Attribute.COLOR) != null ? ss.getForeground(style) : null;
							break;
						case FONT :
							rowStyleAttrValue = style.getAttribute(CSS.Attribute.FONT) != null || style.getAttribute(CSS.Attribute.FONT_FAMILY) != null ||
								style.getAttribute(CSS.Attribute.FONT_SIZE) != null || style.getAttribute(CSS.Attribute.FONT_STYLE) != null ||
								style.getAttribute(CSS.Attribute.FONT_VARIANT) != null || style.getAttribute(CSS.Attribute.FONT_WEIGHT) != null
								? ss.getFont(style) : null;
					}
				}
			}
		}

		return rowStyleAttrValue;
	}

	private Color getFgColor(JTable jtable, boolean isSelected, int row)
	{
		return (Color)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.FGCOLOR);
	}

	private Font getFont(JTable jtable, boolean isSelected, int row)
	{
		return (Font)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.FONT);
	}

	private Color getBgColor(JTable jtable, boolean isSelected, int row, boolean isEdited)
	{
		Color bgColor = null;
		IRecordInternal state = ((IFoundSetInternal)jtable.getModel()).getRecord(row);
		boolean specialStateCase = (state instanceof PrototypeState || state instanceof FindState);
		if (/* !(renderer instanceof JButton) && */!specialStateCase)
		{
			ISwingFoundSet foundset = (ISwingFoundSet)jtable.getModel();
			bgColor = (Color)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.BGCOLOR);

			String strRowBGColorProvider = null;
			List<Object> rowBGColorArgs = null;

			if (jtable instanceof IView)
			{
				strRowBGColorProvider = ((IView)jtable).getRowBGColorScript();
				rowBGColorArgs = ((IView)jtable).getRowBGColorArgs();
			}
			if (strRowBGColorProvider == null) strRowBGColorProvider = "servoy_row_bgcolor"; //$NON-NLS-1$

			boolean isRowBGColorCalculation = state.getRawData().containsCalculation(strRowBGColorProvider);
			if (!isRowBGColorCalculation && strRowBGColorProvider.equals("servoy_row_bgcolor")) //$NON-NLS-1$
			{
				strRowBGColorProvider = ""; //$NON-NLS-1$
			}
			if (strRowBGColorProvider != null && !"".equals(strRowBGColorProvider)) //$NON-NLS-1$
			{
				Object bg_color = null;
				// TODO this should be done better....
				Record.VALIDATE_CALCS.set(Boolean.FALSE);
				try
				{
					String type = (editor instanceof IScriptableProvider && ((IScriptableProvider)editor).getScriptObject() instanceof IScriptBaseMethods)
						? ((IScriptBaseMethods)((IScriptableProvider)editor).getScriptObject()).js_getElementType() : null;
					String name = (editor instanceof IDisplayData) ? ((IDisplayData)editor).getDataProviderID() : null;
					if (isRowBGColorCalculation)
					{
						bg_color = foundset.getCalculationValue(
							state,
							strRowBGColorProvider,
							Utils.arrayMerge((new Object[] { new Integer(row), new Boolean(isSelected), type, name, new Boolean(isEdited) }),
								Utils.parseJSExpressions(rowBGColorArgs)), null);
					}
					else
					{
						try
						{
							FormController currentForm = dal.getFormController();
							bg_color = currentForm.executeFunction(strRowBGColorProvider, Utils.arrayMerge((new Object[] { new Integer(row), new Boolean(
								isSelected), type, name, currentForm.getName(), state, new Boolean(isEdited) }), Utils.parseJSExpressions(rowBGColorArgs)),
								false, null, true, null);
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
				}
				finally
				{
					Record.VALIDATE_CALCS.set(null);
				}

				if (bg_color != null && !(bg_color.toString().trim().length() == 0) && !(bg_color instanceof Undefined))
				{
					bgColor = PersistHelper.createColor(bg_color.toString());
				}
			}
		}
		return bgColor;
	}

	private boolean testCalc(final String possibleCalcDataprovider, final IRecordInternal state, final int row, final int column, final ISwingFoundSet foundset)
	{
		if (state != null && !(state instanceof PrototypeState || state instanceof FindState) &&
			state.getRawData().containsCalculation(possibleCalcDataprovider) && state.getRawData().mustRecalculate(possibleCalcDataprovider, true))
		{
			IApplication app = dal.getApplication();
			((IDisplayData)renderer).setValueObject(state.getRawData().getValue(possibleCalcDataprovider));

			final String key = row + "_" + possibleCalcDataprovider; //$NON-NLS-1$
			if (!rowAndDataprovider.containsKey(key))
			{
				rowAndDataprovider.put(key, key);
				app.getScheduledExecutor().execute(new Runnable()
				{

					public void run()
					{
						state.getValue(possibleCalcDataprovider);
						application.invokeLater(new Runnable()
						{
							public void run()
							{
								rowAndDataprovider.remove(key);
								foundset.fireTableModelEvent(row, row, column, TableModelEvent.UPDATE);
								Container parent = table.getParent();
								while (parent != null && !(parent instanceof ListView))
								{
									parent = parent.getParent();
								}
								if (parent instanceof ListView)
								{
									((ListView)parent).repaint();
								}
							}
						});
					}
				});
			}
			return true;
		}
		return false;
	}

	@Override
	public Object getHeaderValue()
	{
		Object value = super.getHeaderValue();
		return Text.processTags((String)value, dal);
	}

	class ASynchonizedCellLoad implements Runnable
	{
		private final IApplication app;
		private final ISwingFoundSet foundset;
		private final int row;
		private final int column;
		private final String relationName;
		private final List<SortColumn> sort;
		private final String restName;
		private final JTable jtable;

		ASynchonizedCellLoad(IApplication app, JTable jtable, ISwingFoundSet foundset, int row, int column, String relationName, List<SortColumn> sort,
			String restName)
		{
			this.app = app;
			this.foundset = foundset;
			this.row = row;
			this.column = column;
			this.relationName = relationName;
			this.sort = sort;

			this.restName = restName;

			this.jtable = jtable;
		}

		public void run()
		{
			try
			{
				final IRecordInternal state = foundset.getRecord(row);
				if (state != null)
				{
					// only retrieve
					if (!((J2DBClient)app).isConnected())
					{
						if (Debug.tracing())
						{
							Debug.trace("Client not connected, rescheduling it with a timeout of 5 seconds, clientid: " + app.getClientID()); //$NON-NLS-1$
						}
						app.getScheduledExecutor().schedule(this, 5, TimeUnit.SECONDS);
					}
					else
					{
						IFoundSetInternal fs = state.getRelatedFoundSet(relationName, sort);
						// this triggers an update of related foundset if mustQueryForUpdates is true
						// needed when foundset is not touched but still needs to be up to date
						if (fs != null) fs.getSize();
						if (fs == null && !((J2DBClient)app).isConnected())
						{
							if (Debug.tracing())
							{
								Debug.trace("Client got disconnected, rescheduling it with a timeout of 5 seconds, clientid: " + app.getClientID()); //$NON-NLS-1$
							}
							app.getScheduledExecutor().schedule(this, 5, TimeUnit.SECONDS);
						}
						else
						{
							if (fs != null && restName != null)
							{
								fs.getDataProviderValue(restName);// only do lookup for aggregate
							}
							app.invokeLater(new Runnable()
							{
								public void run()
								{
									rowAndDataprovider.remove(row + "_" + relationName + "_" + restName); //$NON-NLS-1$ //$NON-NLS-2$
									foundset.fireTableModelEvent(row, row, column, TableModelEvent.UPDATE);
									Container parent = jtable.getParent();
									while (parent != null && !(parent instanceof ListView))
									{
										parent = parent.getParent();
									}
									if (parent instanceof ListView)
									{
										((ListView)parent).repaint();
									}
								}
							});
						}
					}
				}
			}
			catch (RuntimeException re)
			{
				if (Debug.tracing())
				{
					Debug.trace("Exception in asyn load, rescheduling it with a timeout of 5 seconds, clientid: " + app.getClientID()); //$NON-NLS-1$
				}
				app.getScheduledExecutor().schedule(this, 5, TimeUnit.SECONDS);
			}
		}
	}

	/*
	 * @see IDataAdapter#setState(State)
	 */
	public void setRecord(IRecordInternal state)
	{
		// this is called but never do anything with this value, the renderer and
		// editer have to lookup there values them selfs
	}

	/*
	 * @see IDataAdapter#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/*
	 * _____________________________________________________________ DataListener
	 */
	private final List<IDataAdapter> listeners = new ArrayList<IDataAdapter>();

	public void addDataListener(IDataAdapter l)
	{
		if (!listeners.contains(l) && l != this) listeners.add(l);
	}

	public void removeDataListener(IDataAdapter listener)
	{
		listeners.remove(listener);
	}

	private void fireModificationEvent(IRecord record)
	{
		// Also notify the table about changes, there seems no other way to do this...
		TableModel parent = table.getModel();
		if (parent instanceof ISwingFoundSet)
		{
			int index = ((ISwingFoundSet)parent).getRecordIndex(record);
			if (index != -1) ((ISwingFoundSet)parent).fireTableModelEvent(index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
		}
	}

	private boolean gettingEditorValue = false;

	public Object getCellEditorValue()
	{
		try
		{
			// test if currentEditing state isn't deleted already
			if (currentEditingState == null || dataProviderID == null || (currentEditingState != null && currentEditingState.getParentFoundSet() == null)) return null;

			Object comp = editor;

			if ((comp instanceof IDisplay && ((IDisplay)comp).isReadOnly()) || gettingEditorValue)
			{
				return currentEditingState.getValue(getDataProviderID());
			}

			gettingEditorValue = true;

			if (comp instanceof IDelegate< ? >)
			{
				comp = ((IDelegate< ? >)comp).getDelegate();
			}

			//HACK:needed for commit value copied from other hack 'processfocus' in DataField
			if (comp instanceof DataField && ((DataField)comp).isEditable())
			{
				DataField edit = (DataField)comp;
				boolean needEntireState = edit.needEntireState();
				try
				{
					edit.setNeedEntireState(false);
					int fb = edit.getFocusLostBehavior();
					if (fb == JFormattedTextField.COMMIT || fb == JFormattedTextField.COMMIT_OR_REVERT)
					{
						try
						{
							edit.commitEdit();
							// Give it a chance to reformat.
							edit.setValueObject(edit.getValue());
						}
						catch (ParseException pe)
						{
							return null;
						}
					}
					else if (fb == JFormattedTextField.REVERT)
					{
						edit.setValueObject(edit.getValue());
					}
				}
				finally
				{
					edit.setNeedEntireState(needEntireState);
				}

			}

			Object obj = null;
			if (editor instanceof IDisplayData)
			{
				IDisplayData displayData = (IDisplayData)editor;
				obj = displayData.getValueObject();
				// if the editor is not enable or is readonly dont try to set any value.
				if (!displayData.isEnabled() || displayData.isReadOnly()) return obj;
				// then make sure the current state is in edit, if not, try to start it else just return.
				// this can happen when toggling with readonly. case 233226 or 232188 
				if (!currentEditingState.isEditing() && !currentEditingState.startEditing()) return obj;

				try
				{
					if (currentEditingState != null && (obj == null || "".equals(obj)) && currentEditingState.getValue(dataProviderID) == null) //$NON-NLS-1$
					{
						return null;
					}
				}
				catch (IllegalArgumentException iae)
				{
					Debug.error(iae);
				}
				if (currentEditingState instanceof FindState)
				{
					((FindState)currentEditingState).setFormat(dataProviderID, displayData.getFormat());
					currentEditingState.setValue(dataProviderID, obj);
				}
				else
				{

					if (!displayData.isValueValid() && Utils.equalObjects(lastInvalidValue, obj))
					{
						// already validated
						return obj;
					}
					try
					{
						adjusting = true;
						Object oldVal = null;
						try
						{
							oldVal = currentEditingState.getValue(dataProviderID);
						}
						catch (IllegalArgumentException iae)
						{
							Debug.error("Error getting the previous value", iae); //$NON-NLS-1$
						}
						try
						{
							if (oldVal == Scriptable.NOT_FOUND && dal.getFormScope().has(dataProviderID, dal.getFormScope()))
							{
								oldVal = dal.getFormScope().get(dataProviderID);
								dal.getFormScope().put(dataProviderID, obj);
								IFoundSetInternal foundset = currentEditingState.getParentFoundSet();
								if (foundset instanceof FoundSet) ((FoundSet)foundset).fireFoundSetChanged();
							}
							else currentEditingState.setValue(dataProviderID, obj);
						}
						catch (IllegalArgumentException e)
						{
							Debug.trace(e);
							displayData.setValueValid(false, oldVal);
							application.handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));

							Object stateValue = null;
							try
							{
								stateValue = dal.getValueObject(currentEditingState, dataProviderID);
							}
							catch (IllegalArgumentException iae)
							{
								Debug.error(iae);
							}
							Object displayValue;
							if (Utils.equalObjects(oldVal, stateValue))
							{
								// reset display to typed value
								displayValue = obj;
							}
							else
							{
								// reset display to changed value in validator method
								displayValue = stateValue;
							}
							displayData.setValueObject(displayValue);
							return displayValue;
						}

						if (!Utils.equalObjects(oldVal, obj))
						{
							fireModificationEvent(currentEditingState);
							displayData.notifyLastNewValueWasChange(oldVal, obj);
							obj = dal.getValueObject(currentEditingState, dataProviderID);
							displayData.setValueObject(obj);// we also want to reset the value in the current display if changed by script
						}
						else if (!displayData.isValueValid())
						{
							displayData.notifyLastNewValueWasChange(null, obj);
						}
						else
						{
							displayData.setValueValid(true, null);
						}
					}
					finally
					{
						adjusting = false;
						if (displayData.isValueValid())
						{
							lastInvalidValue = NONE;
						}
						else
						{
							lastInvalidValue = obj;
						}
					}
				}

			}
			return obj;
		}
		finally
		{
			gettingEditorValue = false;
		}
	}

	/*
	 * @see CellEditor#isCellEditable(EventObject)
	 */
	public boolean isCellEditable(EventObject anEvent)
	{
		if (editor.isEnabled())
		{
			// if we enable this the onAction is not fired and it is not possible
			// to copy text from non editable field
			// if (editor instanceof JTextComponent)
			// {
			// return ((JTextComponent)editor).isEditable();
			// }
			return true;
		}
		return false;
	}

	/*
	 * @see CellEditor#shouldSelectCell(EventObject)
	 */
	public boolean shouldSelectCell(EventObject anEvent)
	{
		return true;
	}

	private boolean isStopping = false;// we are not completly following the swing model which can couse repetive calls by jtable due to fire of tableContentChange

	public boolean stopCellEditing()
	{
		if (!isStopping)
		{
			try
			{
				isStopping = true;

				// Get the current celleditor value for testing notify changed.
				getCellEditorValue();

				// if the notify changed failed the mustTestLastValue is set and we shouldn't allow stopping:
				if (editor instanceof IDisplayData && !((IDisplayData)editor).isValueValid())
				{
					return false;
				}
				CellEditorListener l = listner;
				if (l != null)
				{
					l.editingStopped(new ChangeEvent(this));
				}
				return true;
			}
			finally
			{
				isStopping = false;
			}
		}
		else
		{
			return true;
		}
	}

	/*
	 * @see CellEditor#cancelCellEditing()
	 */
	public void cancelCellEditing()
	{
		if (listner != null) listner.editingCanceled(new ChangeEvent(this));
	}

	private CellEditorListener listner = null; // allow only one

	/*
	 * @see CellEditor#addCellEditorListener(CellEditorListener)
	 */
	public void addCellEditorListener(CellEditorListener l)
	{
		listner = l;
	}

	/*
	 * @see CellEditor#removeCellEditorListener(CellEditorListener)
	 */
	public void removeCellEditorListener(CellEditorListener l)
	{
		listner = null;
	}

	public void displayValueChanged(ModificationEvent event)
	{
		valueChanged(event);
	}

	/*
	 * @see JSModificationListner#valueChanged(ModificationEvent)
	 */
	public void valueChanged(ModificationEvent e)
	{
		if (adjusting) return;

		try
		{
			adjusting = true;
			// ignore globals in a cell adapter, will be handled by the row manager
			if (!table.isEditing() && e.getName().startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				// test if it is a related 
				if (dataProviderID != null && dataProviderID.indexOf('.') != -1)
				{
					TableModel parent = table.getModel();
					if (parent instanceof ISwingFoundSet)
					{
						// it could be based on that global so fire a table event.
						((ISwingFoundSet)parent).fireTableModelEvent(0, parent.getRowCount() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
					}
				}
				return;
			}

			// refresh value
			IRecord s = e.getRecord();
			if (s == null)
			{
				TableModel tm = table.getModel();
				if (tm instanceof IFoundSetInternal)
				{
					IFoundSetInternal fs = (IFoundSetInternal)tm;
					int selRow = fs.getSelectedIndex();
					if (selRow != -1)
					{
						s = fs.getRecord(selRow);
					}
				}
			}
			if (s != null)
			{
				Object obj = e.getValue();
				if (e.getName().equals(dataProviderID))
				{
					fireModificationEvent(s);// make sure the change is seen and pushed to display by jtable
				}
				else
				{
					obj = dal.getValueObject(s, dataProviderID);
					if (obj == Scriptable.NOT_FOUND)
					{
						obj = null;
					}
				}
				if (s == currentEditingState && table.getEditorComponent() == editor && editor instanceof IDisplayData)
				{
					((IDisplayData)editor).setValueObject(obj);
				}
			}
		}
		finally
		{
			adjusting = false;
		}
	}

	@Override
	public String toString()
	{
		return "CellAdapter " + dataProviderID + ",  hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the editor.
	 * 
	 * @return Component
	 */
	public Component getEditor()
	{
		return editor;
	}

	/**
	 * Returns the renderer.
	 * 
	 * @return Component
	 */
	public Component getRenderer()
	{
		return renderer;
	}

	private void onEditorChanged()
	{
		try
		{
			table.setLayoutChangingViaJavascript(true);
			ArrayList<CellAdapter> cellAdaptersList = new ArrayList<CellAdapter>();
			Enumeration<TableColumn> columnsEnum = table.getColumnModel().getColumns();
			TableColumn column;
			while (columnsEnum.hasMoreElements())
			{
				column = columnsEnum.nextElement();
				cellAdaptersList.add((CellAdapter)column);
			}
			for (CellAdapter ca : cellAdaptersList)
				table.getColumnModel().removeColumn(ca);

			Collections.sort(cellAdaptersList, new Comparator<CellAdapter>()
			{
				public int compare(CellAdapter o1, CellAdapter o2)
				{
					Component editor1 = o1.getEditor();
					Component editor2 = o2.getEditor();

					Point p1 = ((editor1 instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor1).getCachedLocation()
						: editor1.getLocation());
					Point p2 = ((editor2 instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor2).getCachedLocation()
						: editor2.getLocation());
					return PositionComparator.comparePoint(true, p1, p2);
				}
			});

			for (CellAdapter cellAdapter : cellAdaptersList)
			{
				cellAdapter.updateEditorX();
				cellAdapter.updateEditorWidth();
				cellAdapter.resetEditorSize();
				table.getColumnModel().addColumn(cellAdapter);
			}
		}
		finally
		{
			table.setLayoutChangingViaJavascript(false);
		}
	}

	public void updateEditorX()
	{
		this.editorX = (editor instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor).getCachedLocation().x
			: editor.getLocation().x;
	}

	public void updateEditorWidth()
	{
		this.editorWidth = (editor instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor).getCachedSize().width
			: editor.getSize().width;
	}

	private void resetEditorSize()
	{
		Dimension size;
		if (editor instanceof ISupportCachedLocationAndSize)
		{
			size = ((ISupportCachedLocationAndSize)editor).getCachedSize();
		}
		else
		{
			size = editor.getSize();
		}
		CellAdapter.this.setPreferredWidth(size != null ? size.width + table.getColumnModel().getColumnMargin() : 0);
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() instanceof DataComboBox && e.getStateChange() == ItemEvent.DESELECTED)
		{
			return;
		}
		stopCellEditing();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (currentEditingState != null) currentEditingState.startEditing();
	}

	public void addDisplay(CellAdapter ca)
	{
		if (displays == null) displays = new ArrayList<CellAdapter>();
		displays.add(ca);
	}

	public void commitEdit(IDisplayData e)
	{
		getCellEditorValue();
	}

	public void startEdit(IDisplayData e)
	{
		// ignore
	}
}
