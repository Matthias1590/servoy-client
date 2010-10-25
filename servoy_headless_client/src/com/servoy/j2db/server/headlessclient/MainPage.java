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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.border.Border;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IPageMap;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.PageMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.version.undo.Change;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormManager.History;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.info.WEBCONSTANTS;
import com.servoy.j2db.server.headlessclient.dataui.AbstractServoyDefaultAjaxBehavior;
import com.servoy.j2db.server.headlessclient.dataui.FormLayoutProviderFactory;
import com.servoy.j2db.server.headlessclient.dataui.IFormLayoutProvider;
import com.servoy.j2db.server.headlessclient.dataui.ISupportWebTabSeq;
import com.servoy.j2db.server.headlessclient.dataui.StartEditOnFocusGainedEventBehavior;
import com.servoy.j2db.server.headlessclient.dataui.StyleAppendingModifier;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.headlessclient.dataui.WebEventExecutor;
import com.servoy.j2db.server.headlessclient.dataui.WebSplitPane;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.OrientationApplier;

/**
 * Main page being a main container.
 */
public class MainPage extends WebPage implements IMainContainer, IEventCallback, IAjaxIndicatorAware
{
	private static final long serialVersionUID = 1L;

	private static final String DIV_DIALOG_REPEATER_ID = "divdialogs";
	private static final String DIV_DIALOGS_REPEATER_PARENT_ID = "divdialogsparent";
	private static final String FILE_UPLOAD_DIALOG_ID = "fileuploaddialog";

	private final static ResourceReference servoy_js = new JavascriptResourceReference(MainPage.class, "servoy.js"); //$NON-NLS-1$


	private int inputNameIds;


	private WebClient client;
	private Label title;
	private PageContributor pageContributor;
	private WebMarkupContainer body;
	private ListView<IFormUIInternal< ? >> listview;
	private List<IFormUIInternal< ? >> webForms;
	private WebForm main;

	private WebMarkupContainer divDialogsParent;
	private RepeatingView divDialogRepeater;
	private final ServoyDivDialogActionBuffer divDialogActionBuffer = new ServoyDivDialogActionBuffer();
	private final HashMap<String, ServoyDivDialog> divDialogs = new HashMap<String, ServoyDivDialog>();

	private MainPage callingContainer;

	private String modifyNonDivWindowsScript;

	public ResourceReference serveResourceReference = new ResourceReference("resources"); //$NON-NLS-1$

	private FormController currentForm;

	private History history;

	private Component componentToFocus;

	private Component focusedComponent;

	private boolean mustFocusNull;

	private transient boolean mainFormSwitched;

	private final IValueMap bodyAttributes = new ValueMap();

	private boolean showingInDialog;

	private String statusText;
	private SetStatusBehavior setStatusBehavior = null;

	private boolean showPageInDialogDelayed = false;

	private ServoyDivDialog fileUploadWindow;
	private IMediaUploadCallback mediaUploadCallback;

	private ShowUrlInfo showUrlInfo;

	private boolean useAJAX;

	private boolean mediaUploadMultiSelect;

	private class SetStatusBehavior extends AbstractBehavior
	{
		private String text;

		public void setStatusText(String text)
		{
			this.text = text;
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			String jsCall = "setStatusText('" + text + "');"; //$NON-NLS-1$ //$NON-NLS-2$
			response.renderOnLoadJavascript(jsCall);
		}
	}

	private class TriggerUpdateAjaxBehavior extends AbstractServoyDefaultAjaxBehavior
	{

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			response.renderJavascript("function triggerAjaxUpdate() {setTimeout(\"" + getCallbackScript(true) + "\", 0);}", "triggerAjaxUpdate"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			ServoyForm form = main.findParent(ServoyForm.class);
			if (form != null)
			{
				form.processDelayedActions();
			}

			WebEventExecutor.generateResponse(target, getPage());
		}

	}


	public MainPage(PageParameters pp)
	{
		super();
		String solution = pp.getString("solution"); //$NON-NLS-1$
		if (solution != null)
		{
			throw new RestartResponseException(SolutionLoader.class, pp);
		}
		else
		{
			throw new RestartResponseException(SelectSolution.class);
		}
	}

	public MainPage(WebClient sc)
	{
		super();
		init(sc);
	}

	public MainPage(WebClient sc, IPageMap pagemap)
	{
		super(pagemap);
		init(sc);
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return "indicator"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getContainerName()
	 */
	public String getContainerName()
	{
		return getPageMap().getName();
	}

	@SuppressWarnings("nls")
	private void init(WebClient sc)
	{
		setStatelessHint(false);
		client = sc;
		webForms = new ArrayList<IFormUIInternal< ? >>();

		title = new Label("title", new Model<String>("Servoy Web Client")); //$NON-NLS-1$ //$NON-NLS-2$
		add(title);

		useAJAX = Utils.getAsBoolean(client.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		int dataNotifyFrequency = Utils.getAsInteger(sc.getSettings().getProperty("servoy.webclient.datanotify.frequency", "5")); //$NON-NLS-1$  //$NON-NLS-2$
		if (dataNotifyFrequency > 0 && useAJAX)
		{
			add(new AbstractAjaxTimerBehavior(Duration.seconds(dataNotifyFrequency))
			{
				private static final long serialVersionUID = 1L;

				/**
				 * @see wicket.ajax.AbstractAjaxTimerBehavior#onTimer(wicket.ajax.AjaxRequestTarget)
				 */
				@Override
				protected void onTimer(AjaxRequestTarget target)
				{
					if (callingContainer != null && modifyNonDivWindowsScript != null && !callingContainer.isNonModalWindowShown())
					{
						// this means non modal was closed from parent, do not try to close it again
						modifyNonDivWindowsScript = null;
					}
					WebEventExecutor.generateResponse(target, MainPage.this);
				}

				@Override
				public void renderHead(IHeaderResponse response)
				{
					super.renderHead(response);

					String jsTimerScript = getJsTimeoutCall(getUpdateInterval());
					response.renderJavascript("function restartTimer() {" + jsTimerScript + "}", "restartTimer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}


				@Override
				protected CharSequence getPreconditionScript()
				{
					return "onAjaxCall();return !Servoy.DD.isDragging;"; //$NON-NLS-1$
				}

				/**
				 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getFailureScript()
				 */
				@Override
				protected CharSequence getFailureScript()
				{
					return "onAjaxError();"; //$NON-NLS-1$
				}

				@Override
				protected CharSequence getCallbackScript()
				{
					return generateCallbackScript("wicketAjaxGet('" //$NON-NLS-1$
						+
						getCallbackUrl(onlyTargetActivePage()) + "&ignoremp=true'"); //$NON-NLS-1$
				}

				/**
				 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#findIndicatorId()
				 */
				@Override
				protected String findIndicatorId()
				{
					return null; // main page defines it and the timer shouldnt show it
				}

				/**
				 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
				 */
				@Override
				public boolean isEnabled(Component component)
				{
					// data notify is disabled when in design mode
					return !client.getFlattenedSolution().isInDesign(null) && getController() != null && getController().isFormVisible();
				}
			});
		}

		add(new AbstractServoyDefaultAjaxBehavior()
		{
			@Override
			protected void respond(AjaxRequestTarget target)
			{
				for (ServoyDivDialog divDialog : divDialogs.values())
				{
					if (!divDialog.isShown())
					{
						// this means a refresh was probably triggered and we must re-show these... because we do not keep closed dialogs in divDialogs
						divDialog.show(target);
					}
				}
				WebEventExecutor.generateResponse(target, MainPage.this);
			}

			@Override
			public void renderHead(IHeaderResponse response)
			{
				super.renderHead(response);
				response.renderOnDomReadyJavascript(getCallbackScript(true).toString());
			}

			@Override
			public boolean isEnabled(Component component)
			{
				return divDialogs.size() > 0 && super.isEnabled(component);
			}
		});

		add(new TriggerUpdateAjaxBehavior()); // for when another page needs to trigger an ajax update on this page using js (see media upload) 

		body = new WebMarkupContainer("servoy_page") //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see org.apache.wicket.Component#onComponentTag(org.apache.wicket.markup.ComponentTag)
			 */
			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.putAll(bodyAttributes);
			}
		};
		body.add(new AttributeModifier("dir", true, new AbstractReadOnlyModel<String>() //$NON-NLS-1$
			{

				@Override
				public String getObject()
				{
					String value = AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE;
					Locale l = client.getLocale();
					Solution solution = client.getSolution();

					if (solution != null && l != null)
					{
						value = OrientationApplier.getHTMLContainerOrientation(l, solution.getTextOrientation());
					}
					return value;
				}
			}));

		add(body);
		pageContributor = new PageContributor(client, "contribution");
		body.add(pageContributor);

		Label loadingIndicator = new Label("loading_indicator", sc.getI18NMessage("servoy.general.loading"));
		loadingIndicator.add(new SimpleAttributeModifier("style", "display:none;"));

		body.add(loadingIndicator);
		divDialogsParent = new WebMarkupContainer(DIV_DIALOGS_REPEATER_PARENT_ID);
		divDialogsParent.setOutputMarkupPlaceholderTag(true);
		divDialogsParent.setVisible(false);
		body.add(divDialogsParent);

		if (useAJAX)
		{
			divDialogRepeater = new RepeatingView(DIV_DIALOG_REPEATER_ID);
			divDialogsParent.add(divDialogRepeater);

			fileUploadWindow = new ServoyDivDialog(FILE_UPLOAD_DIALOG_ID, isShowingInDialog());
			body.add(fileUploadWindow);
			fileUploadWindow.setModal(true);
			fileUploadWindow.setPageMapName(null);
			fileUploadWindow.setCookieName("dialog_fileupload");
			fileUploadWindow.setResizable(true);
			fileUploadWindow.setInitialHeight(150);
			fileUploadWindow.setInitialWidth(400);
			fileUploadWindow.setMinimalHeight(130);
			fileUploadWindow.setMinimalWidth(400);
			fileUploadWindow.setUseInitialHeight(true); // no effect, can be removed
			fileUploadWindow.setPageCreator(new ModalWindow.PageCreator()
			{
				private static final long serialVersionUID = 1L;

				public Page createPage()
				{
					return new MediaUploadPage(PageMap.forName("fileupload"), mediaUploadCallback, mediaUploadMultiSelect);
				}
			});
			fileUploadWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
			{
				private static final long serialVersionUID = 1L;

				public void onClose(AjaxRequestTarget target)
				{
					fileUploadWindow.setPageMapName(null);
					fileUploadWindow.remove(fileUploadWindow.getContentId());
					WebEventExecutor.generateResponse(target, findPage());
				}
			});
		}
		else
		{
			divDialogsParent.add(new Label("divdialogs"));
			body.add(new Label("fileuploaddialog")); //$NON-NLS-1$
		}

		modifyNonDivWindowsScript = null;

		IModel<String> styleHrefModel = new AbstractReadOnlyModel<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				if (main != null)
				{
					return "/servoy-webclient/templates/" + client.getUIProperty(WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR) + "/servoy_web_client_default.css"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return null;
			}
		};
		Label main_form_style = new Label("main_form_style"); //$NON-NLS-1$
		main_form_style.add(new AttributeModifier("href", true, styleHrefModel)); //$NON-NLS-1$
		add(main_form_style);

		IModel<List<IFormUIInternal< ? >>> loopModel = new AbstractReadOnlyModel<List<IFormUIInternal< ? >>>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public List<IFormUIInternal< ? >> getObject()
			{
				return webForms;
			}
		};

		listview = new ListView<IFormUIInternal< ? >>("forms", loopModel) //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<IFormUIInternal< ? >> item)
			{
				final WebForm form = (WebForm)item.getModelObject();
				if (form.getParent() != null)
				{
					form.remove();
				}
				item.add(form);

				IFormLayoutProvider layoutProvider = FormLayoutProviderFactory.getFormLayoutProvider(client, client.getSolution(),
					form.getController().getForm(), form.getController().getName());

				TextualStyle styleToReturn = null;

				if ((navigator != null) && (form == navigator.getFormUI()))
				{
					styleToReturn = layoutProvider.getLayoutForForm(navigator.getForm().getSize().width, true, false);
				}
				else if (form == main)
				{
					int customNavWidth = 0;
					if (navigator != null) customNavWidth = navigator.getForm().getSize().width;
					styleToReturn = layoutProvider.getLayoutForForm(customNavWidth, false, false);
				}
				if (styleToReturn != null)
				{
					form.add(new StyleAppendingModifier(styleToReturn)
					{
						@Override
						public boolean isEnabled(Component component)
						{
							return (component.findParent(WebTabPanel.class) == null) && (component.findParent(WebSplitPane.class) == null);
						}
					});
				}
				TabIndexHelper.setUpTabIndexAttributeModifier(item, ISupportWebTabSeq.SKIP);
			}

			/**
			 * @see org.apache.wicket.markup.html.list.ListView#onBeforeRender()
			 */
			@Override
			protected void onBeforeRender()
			{
				super.onBeforeRender();
				// now first initialize all the tabs so that data from
				// tab x doesn't change anymore (so that it could alter data in tab y)
				// don't know if this still does anything because we need to do it 
				// in the onBeforeRender of WebTabPanel itself, else tableviews don't have there models yet..
				visitChildren(WebTabPanel.class, new IVisitor<WebTabPanel>()
				{
					public Object component(WebTabPanel wtp)
					{
						wtp.initalizeFirstTab();
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});

				addWebAnchoringInfoIfNeeded(false);
				if (useAJAX) pageContributor.addFocusEventListeners(MainPage.this);
			}
		};
		listview.setReuseItems(true);
		// if versioning is disabled then table views can go wrong (don't rollback on a submit)
		//listview.setVersioned(false);

		Form form = new ServoyForm("servoy_dataform"); //$NON-NLS-1$

		form.add(new SimpleAttributeModifier("autocomplete", "off")); //$NON-NLS-1$ //$NON-NLS-2$

		form.add(listview);
		WebMarkupContainer defaultButton = new WebMarkupContainer("defaultsubmitbutton", new Model()); //$NON-NLS-1$
		defaultButton.setVisible(!useAJAX);
		form.add(defaultButton);
		body.add(form);
	}

	private ServoyDivDialog createDivDialog(String name)
	{
		final ServoyDivDialog divDialog = new ServoyDivDialog(divDialogRepeater.newChildId(), isShowingInDialog());
		divDialog.setPageMapName(null);
		divDialog.setCookieName("dialog_" + name);
		divDialog.setModal(true);
		divDialog.setPageCreator(new ModalWindow.PageCreator()
		{
			private static final long serialVersionUID = 1L;

			public Page createPage()
			{
				MainPage page = (MainPage)((FormManager)client.getFormManager()).getOrCreateMainContainer(divDialog.getPageMapName());
				page.setShowingInDialog(true);
				return page;
			}
		});
		divDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
		{
			private static final long serialVersionUID = 1L;

			public void onClose(AjaxRequestTarget target)
			{
				divDialogRepeater.remove(divDialog);
				divDialogActionBuffer.remove(divDialog, divDialogsParent);
				divDialog.setPageMapName(null);
				WebEventExecutor.generateResponse(target, findPage());
			}
		});
		divDialog.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
		{
			private static final long serialVersionUID = 1L;

			public boolean onCloseButtonClicked(AjaxRequestTarget target)
			{
				if (!divDialog.isShown())
				{
					return true; // double clicked?
				}

				FormManager fm = ((FormManager)client.getFormManager());
				IMainContainer divDialogContainer = fm.getMainContainer(divDialog.getPageMapName());
				IMainContainer currentContainer = fm.getCurrentContainer();
				// get a lock on the dialog container
				if (divDialogContainer instanceof MainPage && RequestCycle.get() != null)
				{
					Session.get().getPage(divDialog.getPageMapName(), ((MainPage)divDialogContainer).getPath(), LATEST_VERSION);
				}

				// temporary set the dialog container as the current container (the close event is processed by the main container, not the dialog)
				fm.setCurrentContainer(divDialogContainer, divDialogContainer.getContainerName());
				client.getJSWindowManager().closeFormInWindow(divDialog.getPageMapName(), divDialog.getCloseAll());

				// reset current container again
				fm.setCurrentContainer(currentContainer, currentContainer.getContainerName());
				if (divDialogContainer instanceof MainPage)
				{
					// commented out because this condition is unlikely to become true; also, at the time this window.focus() was added in 2007, closePopup would have been
					// set indeed, but now after a series of refactorings and changes, closePopup is set on parent window; commented out to keep similar behavior...
//					if (((MainPage)divDialogContainer).closePopup)
//					{
//						target.appendJavascript("window.focus()"); //$NON-NLS-1$
//					}
// repaint the modal window (the contents may have changed)
					target.addComponent(divDialog);
					WebEventExecutor.generateResponse(target, (MainPage)currentContainer);
				}

				return false;
			}
		});
		divDialogRepeater.add(divDialog);
		divDialogsParent.setVisible(true);
		divDialogActionBuffer.add(divDialog, divDialogsParent);
		divDialogs.put(name, divDialog);
		return divDialog;
	}

	private final java.util.Set<WebForm> formsForFullAnchorRendering = new HashSet<WebForm>();

	public void addFormForFullAnchorRendering(WebForm form)
	{
		formsForFullAnchorRendering.add(form);
	}

	public void addWebAnchoringInfoIfNeeded(final boolean onlyChanged)
	{
		if (getController() != null)
		{

			boolean webAnchorsEnabled = Utils.getAsBoolean(getController().getApplication().getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$ 
			if (webAnchorsEnabled)
			{
				// test if there is a form in design
				Object isInDesign = visitChildren(IFormUIInternal.class, new IVisitor<Component>()
				{
					public Object component(Component component)
					{
						if (((IFormUIInternal< ? >)component).isDesignMode())
						{
							return Boolean.TRUE;
						}
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});
				if (isInDesign instanceof Boolean)
				{
					webAnchorsEnabled = !((Boolean)isInDesign).booleanValue();
				}
			}
			if (webAnchorsEnabled)
			{
				final SortedSet<FormAnchorInfo> formAnchorInfo = new TreeSet<FormAnchorInfo>();
				visitChildren(WebForm.class, new IVisitor<WebForm>()
				{
					public Object component(WebForm form)
					{
						if (form.isVisibleInHierarchy())
						{
							boolean getOnlyChangedControls = onlyChanged;
							if (formsForFullAnchorRendering.contains(form)) getOnlyChangedControls = false;
							FormAnchorInfo fai = form.getFormAnchorInfo(getOnlyChangedControls);
							if (fai != null)
							{
								if (form.equals(main))
								{
									fai.isTopLevelForm = true;
								}
								else
								{
									if (navigator != null)
									{
										if (form.equals(navigator.getFormUI()))
										{
											fai.isTopLevelNavigator = true;
										}
									}
								}
								formAnchorInfo.add(fai);
							}
							return IVisitor.CONTINUE_TRAVERSAL;
						}
						else
						{
							return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
					}
				});
				boolean isAjaxRequest = getRequestCycle().getRequestTarget() instanceof AjaxRequestTarget;
				if (!isAjaxRequest)
				{
					pageContributor.setFormAchorInfos(null, onlyChanged); // reset formAnchorInfo
				}
				pageContributor.setFormAchorInfos(formAnchorInfo, onlyChanged);
				formsForFullAnchorRendering.clear();
			}
			else
			{
				pageContributor.setFormAchorInfos(null, onlyChanged);
			}
		}
	}

	/**
	 * @param b
	 */
	protected void setShowingInDialog(boolean shownInDialog)
	{
		this.showingInDialog = shownInDialog;
	}

	/**
	 * @return
	 */
	public boolean isShowingInDialog()
	{
		return showingInDialog;
	}

	public IPageContributorInternal getPageContributor()
	{
		return pageContributor;
	}

	public MarkupContainer getBody()
	{
		return body;
	}

	public void addBodyAttributes(IValueMap map)
	{
		bodyAttributes.putAll(map);
	}

	/**
	 * @see wicket.Component#renderHead(wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@Override
	public void renderHead(HtmlHeaderContainer container)
	{
		super.renderHead(container);

		IHeaderResponse response = container.getHeaderResponse();

		String showUrl = getShowUrlScript();
		if (showUrl != null)
		{
			response.renderOnLoadJavascript(showUrl);
		}

		if (modifyNonDivWindowsScript != null)
		{
			response.renderOnLoadJavascript(modifyNonDivWindowsScript);
			modifyNonDivWindowsScript = null;
		}

		response.renderJavascriptReference(servoy_js);
		YUILoader.renderDragNDrop(response);
	}

	/**
	 * @see wicket.Page#configureResponse()
	 */
	@Override
	protected void configureResponse()
	{
		super.configureResponse();

		if (getWebRequestCycle().getResponse() instanceof WebResponse)
		{
			final WebResponse response = getWebRequestCycle().getWebResponse();
			HTTPUtils.setNoCacheHeaders(response.getHttpServletResponse());
//			response.setHeader("Pragma", "no-cache"); //$NON-NLS-1$//$NON-NLS-2$
//			response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store"); //$NON-NLS-1$//$NON-NLS-2$
		}

		final RequestCycle cycle = getRequestCycle();
		final Response response = cycle.getResponse();

		if (main != null)
		{
			final MarkupStream markupStream = main.getAssociatedMarkupStream(false);
			if (markupStream != null)
			{
				markupStream.setCurrentIndex(0);//not sure if this is needed
				MarkupElement m = markupStream.get();
				String docType = m.toString().trim();
				if (m != null && docType.toUpperCase().startsWith("<!DOCTYPE")) //$NON-NLS-1$
				{
					int index = docType.indexOf(">"); //$NON-NLS-1$
					if (index != -1)
					{
						response.write(docType.substring(0, index + 1));//delegate form doctype to be the mainpage doctype
					}
					else
					{
						response.write(docType);//delegate form doctype to be the mainpage doctype
					}
				}
			}
		}
	}

	public void flushCachedItems()
	{
		main = null;
		currentForm = null;
		navigator = null;
		componentToFocus = null;
		// both can't be set to null, else a login solution with a dialog wont close that dialog.
//		callingContainer = null;
//		closePopup = false;
		showingInDialog = false;
		showUrlInfo = null;
		mainFormSwitched = false;

		webForms.clear();
		if (RequestCycle.get() != null)
		{
			listview.removeAll();
		}
		if (history != null)
		{
			history.clear();
		}
	}

	public void show(String name)
	{
		//ignore, add does all the work
	}

	public void showBlankPanel()
	{
		//not needed in webclient
	}

	public void showSolutionLoading(boolean b)
	{
		//not needed in webclient
	}

	/**
	 * @see wicket.Component#onAttach()
	 */
	@Override
	protected void onBeforeRender()
	{
		if (main != null && (main.getParent() == null || main.getParent().getParent() != listview))
		{
			// the main page is removed underneath from the page.
			// lets regenerate the listview.
			listview.removeAll();
		}
		super.onBeforeRender();
	}

	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();

		mainFormSwitched = false;

		// make sure that all IProviderStylePropertyChanges are set to rendered on a full page render.
		visitChildren(IProviderStylePropertyChanges.class, new IVisitor<Component>()
		{
			@SuppressWarnings("nls")
			public Object component(Component component)
			{
				if (((IProviderStylePropertyChanges)component).getStylePropertyChanges().isChanged())
				{
					if (Debug.tracing())
					{
						if (component.isVisible())
						{
							Debug.trace("Component " + component + " is changed but is not rendered, deleted from template?");
						}
						else
						{
							Debug.trace("Component " + component + " is changed but is not rendered because it is not visible");
						}
					}
					((IProviderStylePropertyChanges)component).getStylePropertyChanges().setRendered();
				}
				return component.isVisible() ? IVisitor.CONTINUE_TRAVERSAL : IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});
	}


	/**
	 * @see wicket.markup.html.WebPage#onDetach()
	 */
	@Override
	protected void onDetach()
	{
		super.onDetach();

		// between requests a page is not versionable
		setVersioned(false);
	}

	public void add(final IComponent c, final String name)
	{
		// a new main form is added clear everything
		webForms.clear();
		if (navigator != null)
		{
			webForms.add(navigator.getFormUI());
		}

		WebMarkupContainer container = (WebMarkupContainer)c;
		if (!"webform".equals(container.getId())) //$NON-NLS-1$
		{
			throw new RuntimeException("only webforms with the name webform can be added to this mainpage"); //$NON-NLS-1$
		}

		if (main != null)
		{
			addStateChange(new Change()
			{
				private static final long serialVersionUID = 1L;

				final String formName = main.getController().getName();

				@Override
				public void undo()
				{
					((FormManager)client.getFormManager()).showFormInMainPanel(formName);
				}
			});
		}
		listview.removeAll();

		main = (WebForm)container;
		webForms.add((WebForm)container);

		/*
		 * if (navigator != null) { calculateFormAndNavigatorSizes(); }
		 */
	}

	public void remove(IComponent c)
	{
		webForms.remove(c);
		listview.removeAll();
	}

	public void setComponentVisible(boolean b)
	{
	}

	public void setBackground(Color cbg)
	{
	}

	public void setBorder(Border b)
	{
	}

	public void setCursor(Cursor predefinedCursor)
	{
	}

	public void setFont(Font f)
	{
	}

	public void setForeground(Color cfg)
	{
	}

	public void setLocation(Point loc)
	{
	}

	public void setName(String name)
	{
		// ignore, can only be set through constructor (as id)
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	public String getName()
	{
		return getId();
	}


	public void setOpaque(boolean b)
	{
	}

	public void setSize(Dimension size)
	{
	}

	public void setToolTipText(String tooltip)
	{
	}

	private FormController navigator;

	public FormController getNavigator()
	{
		return navigator;
	}

	public FormController setNavigator(FormController c)
	{
		if (c == navigator) return c;
		if (c == null && navigator != null)
		{
			webForms.remove(navigator.getFormUI());
			listview.removeAll();
			navigator = null;
			return null;
		}
		else if (navigator != null)
		{
			webForms.remove(navigator.getFormUI());
			listview.removeAll();
		}

		FormController retval = navigator;
		navigator = c;
		WebForm form = (WebForm)c.getFormUI();
		if (!webForms.contains(form))
		{
			webForms.add(form);
			listview.removeAll();
		}
		/*
		 * if (navigator != null) { calculateFormAndNavigatorSizes(); } else { dynamicCSSContent = ""; //$NON-NLS-1$ }
		 */

		return retval;
	}

	/**
	 * @return
	 */
	public IFormUIInternal<Component> getMainWebForm()
	{
		return main;
	}

	public void setTitle(String name)
	{
		Solution solution = this.client.getSolution();
		String titleString = ""; //$NON-NLS-1$
		String solutionTitle = solution.getTitleText();

		if (solutionTitle == null)
		{
			titleString = solution.getName();
		}
		else if (!solutionTitle.equals("<empty>")) //$NON-NLS-1$
		{
			titleString = solutionTitle;
		}

		titleString = client.getI18NMessageIfPrefixed(titleString);

		if (name != null && !name.trim().equals("") && !"<empty>".equals(name) && main != null) //$NON-NLS-1$ //$NON-NLS-2$
		{
			String nameString = client.getI18NMessageIfPrefixed(name);
			FormController formController = main.getController();
			if (formController != null)
			{
				String name2 = Text.processTags(nameString, formController.getTagResolver());
				if (name2 != null) nameString = name2;

			}
			else
			{
				String name2 = Text.processTags(nameString, TagResolver.createResolver(new PrototypeState(null)));
				if (name2 != null) nameString = name2;
			}
			if (!nameString.trim().equals("")) //$NON-NLS-1$
			{
				if ("".equals(titleString)) //$NON-NLS-1$
				{
					titleString += nameString;
				}
				else
				{
					titleString += " - " + nameString; //$NON-NLS-1$
				}
			}
		}
		String appName = "Servoy Web Client"; //$NON-NLS-1$
		boolean branding = Utils.getAsBoolean(client.getSettings().getProperty("servoy.branding", "false")); //$NON-NLS-1$ //$NON-NLS-2$
		String appTitle = client.getSettings().getProperty("servoy.branding.windowtitle"); //$NON-NLS-1$
		if (branding && appTitle != null)
		{
			appName = appTitle;
		}
		if (titleString.equals("")) //$NON-NLS-1$
		{
			titleString = appName;
		}
		else
		{
			titleString += " - " + appName; //$NON-NLS-1$
		}
		this.title.setDefaultModelObject(titleString);
	}

	public String nextInputNameId()
	{
		return Integer.toString(inputNameIds++);
	}

	public void setShowURLCMD(String url, String target, String target_options, int timeout)
	{
		showUrlInfo = new ShowUrlInfo(url, target, target_options, timeout);
	}

	/**
	 * @return
	 */
	public ShowUrlInfo getShowUrlInfo()
	{
		return showUrlInfo;
	}

	public static String getShowUrlScript(ShowUrlInfo showUrlInfo)
	{
		if (showUrlInfo != null)
		{
			if (showUrlInfo.target.equalsIgnoreCase("_close")) //$NON-NLS-1$
			{
				return "window.close();window.opener.location.reload(true)"; //$NON-NLS-1$
			}
			else if (showUrlInfo.target.equalsIgnoreCase("_self")) //$NON-NLS-1$
			{
				return "showurl('" + showUrlInfo.url + "'," + showUrlInfo.timeout + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			else if (showUrlInfo.target.equalsIgnoreCase("_top")) //$NON-NLS-1$
			{
				String script = "window.top.location.href='" + showUrlInfo.url + "';"; //$NON-NLS-1$ //$NON-NLS-2$
				return "window.setTimeout(\"" + script + "\"," + showUrlInfo.timeout + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			else
			{
				StringBuilder script = new StringBuilder();
				if (!"_blank".equals(showUrlInfo.target)) //$NON-NLS-1$
				{
					script.append("if (top.window.frames['" + showUrlInfo.target + "'])"); //$NON-NLS-1$ //$NON-NLS-2$
					script.append("{top.window.frames['" + showUrlInfo.target + "'].document.location.href = '" + showUrlInfo.url + "';}else{"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				if (showUrlInfo.target_options != null)
				{
					script.append("window.open('" + showUrlInfo.url + "','" + showUrlInfo.target + "','" + showUrlInfo.target_options + "');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				else
				{
					script.append("window.open('" + showUrlInfo.url + "','" + showUrlInfo.target + "');"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				if (!"_blank".equals(showUrlInfo.target)) //$NON-NLS-1$
				{
					script.append("}"); //$NON-NLS-1$
				}
				if (showUrlInfo.timeout != 0)
				{
					return "window.setTimeout(\"" + script + "\"," + showUrlInfo.timeout + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return script.toString();
			}
		}
		return null;
	}

	public String getShowUrlScript()
	{
		if (showUrlInfo != null)
		{
			try
			{
				return getShowUrlScript(showUrlInfo);
			}
			finally
			{
				showUrlInfo = null;
			}
		}
		return null;
	}

	/**
	 * @param name
	 * @param bs
	 */
	public String serveResource(String fname, byte[] bs, String mimetype)
	{
		WebClientSession session = (WebClientSession)getSession();
		session.serveResource(fname, bs, mimetype);
		return urlFor(serveResourceReference).toString();
	}

	@SuppressWarnings("nls")
	public void showOpenFileDialog(final IMediaUploadCallback callback, boolean multiSelect, String title)
	{
		this.mediaUploadMultiSelect = multiSelect;
		this.mediaUploadCallback = new IMediaUploadCallback()
		{
			boolean uploaded = false;

			public void uploadComplete(IUploadData[] fu)
			{
				uploaded = true;
				mediaUploadCallback = null;
				callback.uploadComplete(fu);
				divDialogActionBuffer.close(fileUploadWindow);
			}

			public void onSubmit()
			{
				if (!uploaded)
				{
					mediaUploadCallback = null;
					fileUploadWindow.setPageMapName(null);
					fileUploadWindow.remove(fileUploadWindow.getContentId());
					divDialogActionBuffer.close(fileUploadWindow);
				}
			}
		};

		fileUploadWindow.setPageMapName("fileupload");
		if (title == null)
		{
			fileUploadWindow.setTitle(Messages.getString("servoy.filechooser.title"));
		}
		else if (!"".equals(title))
		{
			fileUploadWindow.setTitle(title);
		}
		divDialogActionBuffer.show(fileUploadWindow);
	}

	public Color getBackground()
	{
		return null;
	}

	public Border getBorder()
	{
		return null;
	}

	public Font getFont()
	{
		return null;
	}

	public Color getForeground()
	{
		return null;
	}

	public Point getLocation()
	{
		return null;
	}

	public Dimension getSize()
	{
		return null;
	}

	public boolean isOpaque()
	{
		return false;
	}

	public void setComponentEnabled(boolean enabled)
	{
	}

	public Iterator<IComponent> getComponentIterator()
	{
		return null;
	}

	public void setFormController(FormController f)
	{
		if (currentForm != f)
		{
			mainFormSwitched = true;
			this.currentForm = f;
		}
		if (currentForm == null)
		{
			if (navigator != null)
			{
				webForms.remove(navigator.getFormUI());
			}
			navigator = null;
		}
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getController()
	 */
	public FormController getController()
	{
		return currentForm;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getHistory()
	 */
	public History getHistory()
	{
		if (history == null)
		{
			history = new FormManager.History(client, this);
		}
		return history;
	}

	/**
	 * @see org.apache.wicket.Component#onAttach()
	 */
	@Override
	public void onPageAttached()
	{
		// between requests a page is not versionable
		setVersioned(true);
		String ignore = RequestCycle.get().getRequest().getParameter("ignoremp"); //$NON-NLS-1$
		if (!"true".equalsIgnoreCase(ignore)) //$NON-NLS-1$
		{
			FormManager fm = (FormManager)client.getFormManager();
			if (fm != null)
			{
				if (isShowingInDialog() && fm.getCurrentContainer() != MainPage.this && callingContainer != null)
				{
					ServoyDivDialog dialog = callingContainer.divDialogs.get(getPageMapName());
					// bring the dialog on top of other possible non-modals
					if (dialog != null && !dialog.isModal()) divDialogActionBuffer.toFront(dialog); // not using callingContainer.divDialogActionBuffer here because the DivWindow JS allows it to be executed from both parent frame and dialog frame, and we are currently handling a request in the dialog frame
				}
				fm.setCurrentContainer(MainPage.this, MainPage.this.getPageMap().getName());
			}
		}
		super.onPageAttached();
	}

	@Override
	public void onNewBrowserWindow()
	{
		final IPageMap map = getSession().createAutoPageMap();
		FormManager fm = (FormManager)client.getFormManager();
		MainPage page = (MainPage)fm.getOrCreateMainContainer(map.getName());
		fm.setCurrentContainer(page, map.getName());
		fm.showFormInCurrentContainer(getController().getName());
		if (page.getController() == null)
		{
			// form switch did not happen, login form?
			page.add(getController().getFormUI(), getController().getName());
		}
		page.setNavigator(getNavigator());
		setResponsePage(page);
	}

	@SuppressWarnings("nls")
	public void showPopupWindow(MainPage windowContainer, String titleString, Rectangle r2, boolean resizeable, boolean closeAll)
	{
		StringBuilder sb = new StringBuilder(100);
		sb.append(windowContainer.getPageMapName());
		sb.append("=window.open('");
		sb.append(RequestCycle.get().urlFor(windowContainer));
		sb.append("','");
		sb.append(windowContainer.getPageMap().getName());
		sb.append("','scrollbars=yes,menubar=no");
		if (FormManager.FULL_SCREEN.equals(r2))
		{
			sb.append(",fullscreen=yes"); // IE
			sb.append(",height='+(screen.height-30)+'"); // FF
			sb.append(",width='+(screen.width-5)+'"); // FF
			sb.append(",top=0,left=0");
		}
		else
		{
			sb.append(",height=").append(r2.height);
			sb.append(",width=").append(r2.width);
			sb.append(",top='+");
			sb.append("((window.screenTop | window.screenY)+");
			if (r2.y == -1) sb.append("((document.documentElement.clientHeight | document.body.clientHeight)-" + r2.height + ")/2");
			else sb.append(r2.y);
			sb.append(")");
			sb.append("+',left='+");
			sb.append("((window.screenLeft | window.screenX)+");
			if (r2.x == -1) sb.append("((document.documentElement.clientWidth | document.body.clientWidth)-" + r2.width + ")/2)");
			else sb.append(r2.x + ")");
			sb.append("+'");
		}
		sb.append(",resizable=").append(resizeable ? "yes" : "no");
		sb.append(",toolbar=no,location=no,status=no');");
		if (((WebClientInfo)getSession().getClientInfo()).getProperties().isBrowserSafari())
		{
			// safari doesn't tell you that a popup was blocked
			sb.append("if (");
			sb.append(windowContainer.getPageMapName());
			sb.append(" == null || typeof(");
			sb.append(windowContainer.getPageMapName());
			sb.append(") == \"undefined\") alert('Pop-up page could not be opened, please disable your pop-up blocker.');");
		}
		modifyNonDivWindowsScript = sb.toString();
		mustFocusNullIfNoComponentToFocus();
		nonModalWindowShown = true;

		windowContainer.modifyNonDivWindowsScript = null; // to avoid a bug when you would open/close a window from scripting inside opener window; the opened window would be closed by JS in the opener, while scheduling a self.close() also; next time it was opened, it would automatically close itself
		windowContainer.callingContainer = this;
	}

	public void showPopupDiv(MainPage dialogContainer, String titleString, Rectangle r2, boolean resizeable, boolean closeAll, boolean modal, boolean firstShow)
	{
		if (useAJAX)
		{
			String windowName = dialogContainer.getPageMap().getName();
			ServoyDivDialog divDialog = divDialogs.get(windowName);
			if (divDialog == null)
			{
				divDialog = createDivDialog(windowName);
			}
			divDialog.setPageMapName(windowName);
			divDialog.setResizable(resizeable);
			divDialog.setUseInitialHeight(true);
			divDialog.setModal(modal);
			Rectangle bounds;
			if (FormManager.FULL_SCREEN.equals(r2))
			{
				firstShow = true; // always apply initial bounds if full-screen (to keep it the same as in SC)
				// get the size of the browser window (that will contain the div window)
				MainPage browserWindow = this;
				while (browserWindow.callingContainer != null && browserWindow.isShowingInDialog())
				{
					browserWindow = browserWindow.callingContainer;
				}
				Rectangle windowBounds = ((WebClient)getController().getApplication()).getWindowBounds(browserWindow.getContainerName());
				bounds = new Rectangle(windowBounds.x, windowBounds.y, windowBounds.width, windowBounds.height - 45); // it is a bit too high, why? Because windowBounds is size of what the div should occupy, while modalWindow.setInitialHeight() is only applied to the contents (without frame)  
			}
			else
			{
				bounds = r2;
			}
			if (firstShow)
			{
				divDialog.setCookieBoundsOnShow(bounds);
				divDialog.setInitialHeight(bounds.height);
				divDialog.setInitialWidth(bounds.width);
			}
			divDialog.setCloseAll(closeAll);

			FormController fp = dialogContainer.getController();
			String titleStr = titleString;
			if (titleStr == null) titleStr = fp.getForm().getTitleText();
			if (titleStr == null) titleStr = fp.getName();
			titleStr = client.getI18NMessageIfPrefixed(titleStr);

			if (titleStr != null)
			{
				String name2 = Text.processTags(titleStr, fp.getTagResolver());
				if (name2 != null) titleStr = name2;
			}

			divDialog.setTitle(titleStr);
			divDialogActionBuffer.show(divDialog);
		}

		dialogContainer.callingContainer = this;
	}

	private void closePopup(String popupName)
	{
		// first touch this page so that it is locked if this is a normal request
		if (RequestCycle.get() != null)
		{
			Session.get().getPage(getPageMapName(), getPath(), LATEST_VERSION);
		}

		ServoyDivDialog divDialog = divDialogs.remove(popupName);
		if (divDialog != null)
		{
			//divDialog.setPageMapName(null);
			divDialogActionBuffer.close(divDialog);
			if (divDialogs.size() == 0) divDialogsParent.setVisible(false);
		}

		// set new current container
		FormManager formManager = ((FormManager)client.getFormManager());
		String mpmn = getModalPageMapName();
		if (mpmn != null)
		{
			// there is a modal div window opened; it is the one that should become the new current container
			formManager.setCurrentContainer(formManager.getMainContainer(mpmn), mpmn);
		}
		else
		{
			formManager.setCurrentContainer(this, getPageMap().getName());
		}

		if (isNonModalWindowShown())
		{
			String script = "if (" + popupName + " && !" + popupName + ".closed) " + popupName + ".close();"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (modifyNonDivWindowsScript != null)
			{
				modifyNonDivWindowsScript = modifyNonDivWindowsScript + script;
			}
			else
			{
				modifyNonDivWindowsScript = script;
			}
		}
		nonModalWindowShown = false;
	}

	private String getModalPageMapName()
	{
		Iterator<ServoyDivDialog> it = divDialogs.values().iterator();
		while (it.hasNext())
		{
			ServoyDivDialog dw = it.next();
			if (dw.isModal() && dw.isShown())
			{
				return dw.getPageMapName();
			}
		}
		return null;
	}

	public void close()
	{
		// first touch this page so that it is locked if this is a normal request
		if (RequestCycle.get() != null)
		{
			Session.get().getPage(getPageMapName(), getPath(), LATEST_VERSION);
		}
		client.setWindowBounds(getPageMapName(), null);
		setShowPageInDialogDelayed(false);
		pageContributor.showNoDialog();

		setShowingInDialog(false);
		if (callingContainer != null)
		{
			if (callingContainer.isNonModalWindowShown())
			{
				// this means we have a non modal popup
				modifyNonDivWindowsScript = "if (!self.closed) self.close();"; //$NON-NLS-1$
			}
			callingContainer.closePopup(getPageMapName());
		}
	}


	public void setFocusedComponent(Component component)
	{
		focusedComponent = component;
	}

	public Component getFocusedComponent()
	{
		return focusedComponent;
	}

	/**
	 * @param webDataField
	 */
	public void componentToFocus(Component component)
	{
		if (component instanceof IDelegate< ? >)
		{
			Object delegate = ((IDelegate< ? >)component).getDelegate();
			if (delegate instanceof Component)
			{
				componentToFocus = (Component)delegate;
				return;
			}
		}
		componentToFocus = component;
	}

	/**
	 * 
	 */
	public Component getAndResetToFocusComponent()
	{
		Component c = componentToFocus;
		componentToFocus = null;
		return c;
	}

	/**
	 * Sets the must-focus-null flag. This means than, if no other component is to be focused, then wicket should not call focus again for the previously
	 * focused component. This is needed for example when showing a non-modal dialog in IE7 (or otherwise the new window would be displayed in the background).
	 */
	public void mustFocusNullIfNoComponentToFocus()
	{
		mustFocusNull = true;
	}

	/**
	 * Says whether or not the focus component should be set to null for the ajax request target if no other component is to be focused
	 * (getAndResetToFocusComponent() returns null).
	 * 
	 * @return true if the focus component should be set to null for the ajax request target if no other component is to be focused
	 *         (getAndResetToFocusComponent() returns null).
	 */
	public boolean getAndResetMustFocusNull()
	{
		boolean value = mustFocusNull;
		mustFocusNull = false;
		return value;
	}

	/**
	 * @return
	 */
	public boolean isMainFormSwitched()
	{
		return mainFormSwitched;
	}

	public boolean isShowPageInDialogDelayed()
	{
		return showPageInDialogDelayed;
	}

	public void setShowPageInDialogDelayed(boolean showDelayed)
	{
		this.showPageInDialogDelayed = showDelayed;
	}

	public void renderJavascriptChanges(final AjaxRequestTarget target)
	{
		divDialogActionBuffer.apply(target);
		if (modifyNonDivWindowsScript != null)
		{
			target.appendJavascript(modifyNonDivWindowsScript);
			modifyNonDivWindowsScript = null;
		}

		// commented out because you would end up rendering JS for one MainPage into the target of another MainPage (basically executing JS in the wrong window context)
//		if (callingContainer != null)
//		{
//			// callingContainer.renderJavascriptChanges(target);
//		}

		// trigger an ajax update in parent window in case something related to dialogs was scheduled to run in that window by current page JS;
		// TODO this doesn't optimize all the cases when other non-modal windows/dialogs run JS that is supposed to modify other dialogs/windows - when you would still need to wait for the timer(or next) ajax call for them to be applied
//		if (isShowingInDialog() && callingContainer.divDialogActionBuffer.hasActions()) target.appendJavascript("window.parent.triggerAjaxUpdate()");
	}

	private boolean nonModalWindowShown = false;

	public boolean isNonModalWindowShown()
	{
		return nonModalWindowShown;
	}

	public static class ShowUrlInfo implements Serializable
	{

		private final String url;
		private final String target;
		private final String target_options;
		private final int timeout;

		/**
		 * @param url
		 * @param target
		 * @param target_options
		 * @param timeout
		 */
		public ShowUrlInfo(String url, String target, String target_options, int timeout)
		{
			this.url = url;
			this.target = target == null ? "_blank" : target; //$NON-NLS-1$
			this.target_options = target_options;
			this.timeout = timeout * 1000;
		}

		/**
		 * @return
		 */
		public String getUrl()
		{
			return url;
		}

	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return null;
	}

	public String getAdminInfo()
	{
		return client.getAdminInfo();
	}

	public String getStatusText()
	{
		return statusText;
	}

	public void setStatusText(String statusText)
	{
		this.statusText = statusText;
		if (setStatusBehavior == null)
		{
			setStatusBehavior = new SetStatusBehavior();
			add(setStatusBehavior);
		}
		setStatusBehavior.setStatusText(statusText);
	}

	public void requestFocus()
	{
	}

	public boolean isApplicationShutDown()
	{
		return client.getPluginManager() == null;
	}

	/**
	 * Respond to focus/blur events.
	 */
	@SuppressWarnings("nls")
	public void respond(AjaxRequestTarget target, String event, final String markupId)
	{
		Component component = (Component)visitChildren(IComponent.class, new IVisitor<Component>()
		{
			public Object component(Component c)
			{
				if (c.getMarkupId().equals(markupId))
				{
					return c;
				}
				return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		if (component == null)
		{
			Debug.log("Component not found markupId " + markupId); //$NON-NLS-1$
			return;
		}

		IFormUIInternal< ? > formui = component.findParent(IFormUIInternal.class);
		if (formui != null && formui.isDesignMode())
		{
			Debug.log("Event ignored because of design mode"); //$NON-NLS-1$
			return;
		}

		if (component instanceof IFieldComponent)
		{
			WebEventExecutor eventExecutor = (WebEventExecutor)((IFieldComponent)component).getEventExecutor();
			if (eventExecutor != null)
			{
				if ("focus".equals(event))
				{
					int webModifier = Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER));
					StartEditOnFocusGainedEventBehavior.startEditing(component, WebEventExecutor.convertModifiers(webModifier), target);
					eventExecutor.onEvent(JSEvent.EventType.focusGained, target, component, webModifier);
				}
				else if ("blur".equals(event))
				{
					// test if the data is really posted by looking up the key.
					if (component instanceof FormComponent< ? > && RequestCycle.get().getRequest().getParameter("nopostdata") == null)
					{
						// changed data is posted
						((FormComponent< ? >)component).processInput();
					}
					eventExecutor.onEvent(JSEvent.EventType.focusLost, target, component, IEventExecutor.MODIFIERS_UNSPECIFIED);
				}
				else
				{
					Debug.trace("Ignored event " + event);
				}
			}
			else
			{
				Debug.trace("Ignored event, no eventExecutor");
			}
		}
		else
		{
			// other non-field components like WebLabel
			ServoyForm form = component.findParent(ServoyForm.class);
			if (form != null)
			{
				Page page = form.getPage(); // JS might change the page this form belongs to... so remember it now
				form.processDelayedActions();

				int webModifier = Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER));
				WebEventExecutor.setSelectedIndex(component, target, WebEventExecutor.convertModifiers(webModifier), true);
				WebEventExecutor.generateResponse(target, page);
			}
		}
	}

	/**
	 * 
	 */
	public void setMainPageSwitched()
	{
		mainFormSwitched = true;
	}

	public void addCallingContainers(ArrayList<IMainContainer> al)
	{
		if (!al.contains(this))
		{
			if (callingContainer != null)
			{
				callingContainer.addCallingContainers(al);
			}
			al.add(this);
		}
	}

	public void toBack()
	{
		if (isShowingInDialog() && callingContainer != null)
		{
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			if (divDialog != null)
			{
				divDialogActionBuffer.toBack(divDialog);
			}
		}
		else
		{
			if (modifyNonDivWindowsScript == null) modifyNonDivWindowsScript = "window.blur();";
			else modifyNonDivWindowsScript += "window.blur();";
		}
	}

	public void toFront()
	{
		if (isShowingInDialog() && callingContainer != null)
		{
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			if (divDialog != null)
			{
				divDialogActionBuffer.toFront(divDialog);
			}
		}
		else
		{
			if (modifyNonDivWindowsScript == null) modifyNonDivWindowsScript = "window.focus();";
			else modifyNonDivWindowsScript += "window.focus();";
		}
	}
}
