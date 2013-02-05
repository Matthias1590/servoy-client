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
package com.servoy.j2db.server.headlessclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * A keeper of JS (and other) actions to be applied on a page on an AJAX request or header response.
 * It keeps JS actions ordered as added in each set except for triggerAjaxUpdate that will be added at the end (see SVY-1328), no matter when it's called.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class PageJSActionBuffer
{

	private final List<PageAction> buffer1 = new ArrayList<PageAction>();
	private final List<TriggerAjaxUpdateAction> buffer2 = new ArrayList<TriggerAjaxUpdateAction>();

	private static int batchID = 1;

	private static String newBatchID()
	{
		return "dd_ab_" + (batchID++);
	}

	public static interface PageAction
	{

		/**
		 * Applies this page action to the given target, on a ajax request from given page.
		 * @param target
		 * @param childFrameBatchId null if the request came from the page who's buffer contains this action. An unique ID if some other page's request is trying to execute the action.
		 * @return true if the action (was completed successfully and) should be removed from the action buffer. If this action cannot be executed on an ajax request target, returns false.
		 */
		boolean apply(AjaxRequestTarget target, String childFrameBatchId);

		/**
		 * Applies this page action to the given header response (for example onLoad javascript) of it's own page.
		 * @param response the header response.
		 * @return true if the action (was completed successfully and) should be removed from the action buffer. If this action cannot be executed on a header response returns false.
		 */
		boolean apply(IHeaderResponse response);

	}

	/**
	 * Page action that just needs to render some javascript.
	 * It can perform the action on the onLoad of a page as well, not just by ajax request target.
	 */
	public static class JSChangeAction implements PageAction
	{

		private final String js;

		public JSChangeAction(String js)
		{
			this.js = js;
		}

		public boolean apply(AjaxRequestTarget target, String childFrameBatchId)
		{
			boolean applied = false;
			if (childFrameBatchId == null)
			{
				applied = true;
				target.appendJavascript(js); // only if req. comes from current page
			}
			return applied;
		}

		public boolean apply(IHeaderResponse response)
		{
			response.renderOnLoadJavascript(js);
			return true;
		}

	}

	public static class DivDialogAction implements PageAction
	{

		public static final int OP_SHOW = 1;
		public static final int OP_CLOSE = 2;
		public static final int OP_DIALOG_ADDED_OR_REMOVED = 3;
		public static final int OP_TO_FRONT = 4;
		public static final int OP_TO_BACK = 5;
		public static final int OP_SET_BOUNDS = 6;
		public static final int OP_SAVE_BOUNDS = 7;
		public static final int OP_RESET_BOUNDS = 8;
		private static final int OP_REATTACH_BEHAVIORS_ON_CORRECT_PAGE = 101;

		private final ServoyDivDialog divDialog;
		private final int operation;
		private final Object[] parameters;

		public DivDialogAction(ServoyDivDialog divDialog, int operation)
		{
			this(divDialog, operation, null);
		}

		public DivDialogAction(ServoyDivDialog divDialog, int operation, Object[] parameters)
		{
			this.divDialog = divDialog;
			this.operation = operation;
			this.parameters = parameters;
		}

		public boolean apply(AjaxRequestTarget target, String childFrameBatchId)
		{
			// pageName == null if this is executed
			boolean applied = true;
			switch (operation)
			{
				case DivDialogAction.OP_SHOW :
					if (!divDialog.isShown())
					{
						divDialog.setPageMapName((String)parameters[0]);
						divDialog.show(target, childFrameBatchId);

						// if show is called from a child iFrame request, all callback scripts will point to incorrect page;
						// we need to schedule a reattach for them from the main/parent iframe.
						if (childFrameBatchId != null)
						{
							((MainPage)divDialog.getPage()).addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_REATTACH_BEHAVIORS_ON_CORRECT_PAGE,
								new Object[] { childFrameBatchId }));
						}
					}
					break;
				case DivDialogAction.OP_REATTACH_BEHAVIORS_ON_CORRECT_PAGE :
					applied = (childFrameBatchId == null);
					if (applied && divDialog.isShown())
					{
						divDialog.reAttachBehaviorsAfterShow(target, (String)parameters[0]);
					}
					break;
				case DivDialogAction.OP_CLOSE :
					if (divDialog.isShown())
					{
						divDialog.close(target, childFrameBatchId);
					}
					break;
				case DivDialogAction.OP_TO_FRONT :
					if (divDialog.getPageMapName() != null && divDialog.isShown())
					{
						divDialog.toFront(target, childFrameBatchId);
					}
					break;
				case DivDialogAction.OP_TO_BACK :
					if (divDialog.getPageMapName() != null && divDialog.isShown())
					{
						divDialog.toBack(target, childFrameBatchId);
					}
					break;
				case DivDialogAction.OP_DIALOG_ADDED_OR_REMOVED :
					// this should only happen when the request comes from the root iframe itself, otherwise it has no effect (the component cannot be rendered on a response from another page);
					// even though show is permitted to be called from another (iframe) page's response, the behaviors defined on this component should still work serverside when called from JS, even though it's
					// not yet rendered client side.
					applied = (childFrameBatchId == null);
					if (applied) target.addComponent((WebMarkupContainer)parameters[0]);
					break;
				case DivDialogAction.OP_SET_BOUNDS :
					divDialog.setBounds(target, (Integer)(parameters[0]), (Integer)parameters[1], (Integer)parameters[2], (Integer)parameters[3],
						childFrameBatchId);
					break;
				case DivDialogAction.OP_SAVE_BOUNDS :
					divDialog.saveBounds(target, childFrameBatchId);
					break;
				case DivDialogAction.OP_RESET_BOUNDS :
					DivWindow.deleteStoredBounds(target, (String)parameters[0]);
			}
			onAfterApply();
			return applied;
		}

		/**
		 * Does nothing; gets called after apply executes.
		 * Can be overriden.
		 */
		protected void onAfterApply()
		{
			// can be overridden to do stuff after apply
		}

		public boolean apply(IHeaderResponse response)
		{
			return false;
		}

	}

	public static class TriggerAjaxUpdateAction extends JSChangeAction
	{

		private final MainPage page;

		public TriggerAjaxUpdateAction(MainPage page, String triggerScript)
		{
			super(triggerScript);
			this.page = page;
		}

		public MainPage getPage()
		{
			return page;
		}

	}

	public synchronized void addAction(PageAction a)
	{
		buffer1.add(a);
	}

	public void apply(AjaxRequestTarget target)
	{
		apply(target, null);
	}

	public synchronized void apply(AjaxRequestTarget target, PageJSActionBuffer toBeAppliedAsWell)
	{
		Iterator<PageAction> it = buffer1.iterator();
		while (it.hasNext())
		{
			if (it.next().apply(target, null)) it.remove();
		}

		Iterator<TriggerAjaxUpdateAction> it1 = buffer2.iterator();
		while (it1.hasNext())
		{
			if (it1.next().apply(target, null)) it1.remove();
		}

		// what follows here is possibly running div window operations that are queued in the root iframe;
		// do this after the triggers above because one of the following operations might be a close on the current request's page and we
		// want to limit the chance that js is still trying to execute in a disposed page
		if (toBeAppliedAsWell != null)
		{
			if (toBeAppliedAsWell.buffer1.size() > 0)
			{
				String bID = newBatchID();
				DivWindow.beginActionBatch(target, bID);
				try
				{
					// not using iterators in here cause divWindow show action can add an item
					// to this buffer while iterating (reAttachBehaviors)
					for (int i = 0; i < toBeAppliedAsWell.buffer1.size();)
					{
						if (toBeAppliedAsWell.buffer1.get(i).apply(target, bID))
						{
							toBeAppliedAsWell.buffer1.remove(i);
						}
						else i++;
					}
				}
				finally
				{
					DivWindow.actionBatchComplete(target, bID);
				}
			}
		}
	}

	/**
	 * @return true if all scheduled actions were applied. False if more actions remain to be executed.
	 */
	public synchronized boolean apply(IHeaderResponse headerResponse)
	{
		Iterator<PageAction> it = buffer1.iterator();
		while (it.hasNext())
		{
			if (it.next().apply(headerResponse)) it.remove();
		}

		Iterator<TriggerAjaxUpdateAction> it1 = buffer2.iterator();
		while (it1.hasNext())
		{
			if (it1.next().apply(headerResponse)) it1.remove();
		}
		return (buffer1.size() == 0) && (buffer2.size() == 0);
	}

	public synchronized void clear()
	{
		buffer1.clear();
		buffer2.clear();
	}

	public synchronized void triggerAjaxUpdate(MainPage mainPage, String triggerScript)
	{
		if (!hasAjaxUpdateTrigger(mainPage)) buffer2.add(new TriggerAjaxUpdateAction(mainPage, triggerScript));
	}

	public synchronized boolean hasAjaxUpdateTrigger(MainPage mainPage)
	{
		boolean alreadyThere = false;
		for (TriggerAjaxUpdateAction a : buffer2)
		{
			if (a.getPage() == mainPage)
			{
				alreadyThere = true;
				break;
			}
		}
		return alreadyThere;
	}

}