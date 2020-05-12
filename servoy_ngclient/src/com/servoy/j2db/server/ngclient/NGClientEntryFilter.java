package com.servoy.j2db.server.ngclient;

import static com.servoy.j2db.persistence.IRepository.SOLUTIONS;
import static com.servoy.j2db.server.ngclient.MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS;
import static com.servoy.j2db.server.ngclient.WebsocketSessionFactory.CLIENT_ENDPOINT;
import static com.servoy.j2db.util.Utils.getAsBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.sablo.IContributionEntryFilter;
import org.sablo.IndexPageEnhancer;
import org.sablo.WebEntry;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.sablo.services.template.ModifiablePropertiesGenerator;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.util.HTTPUtils;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.base.util.TagParser;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.MessagesResourceBundle;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.ngclient.template.DesignFormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator.DesignProperties;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.template.JSTemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

import ua_parser.Parser;
import ua_parser.UserAgent;

/**
 * Filter and entrypoint for webapp
 * @author jcompagner
 */
@WebFilter(urlPatterns = { "/solutions/*", "/spec/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
@SuppressWarnings("nls")
public class NGClientEntryFilter extends WebEntry
{
	private static final Pattern MAIN_JS_PATTERN = Pattern.compile("^.*/(main/[^/]*.js)$");
	public static final String SERVOY_CSS = "css/servoy.css";
	public static final String SVYGRP = "svygrp";
	public static final String SERVOY_CONTRIBUTIONS_SVYGRP = "servoy_contributions_svygrp";
	public static final String SERVOY_APP_SVYGRP = "servoy_app_svygrp";
	public static final String SERVOY_THIRDPARTY_SVYGRP = "servoy_thirdparty_svygrp";
	public static final String SERVOY_CSS_CONTRIBUTIONS_SVYGRP = "servoy_css_contributions_svygrp";
	public static final String SERVOY_CSS_THIRDPARTY_SVYGRP = "servoy_css_thirdparty_svygrp";
	public static final String SOLUTIONS_PATH = "/solutions/";
	public static final String FORMS_PATH = "/forms/";
	public static final String WAR_SERVOY_ADMIN_PATH = "/servoy-admin/";

	public static final String ANGULAR_JS = "js/angular.js";
	public static final String[][] ANGULAR_JS_MODULES = { //
		{ "angular-animate", "js/angular-modules/1.7.7/angular-animate.js" }, //
		{ "angular-aria", "js/angular-modules/1.7.7/angular-aria.js" }, //
		{ "angular-cookies", "js/angular-modules/1.7.7/angular-cookies.js" }, //
		{ "angular-message-format", "js/angular-modules/1.7.7/angular-message-format.js" }, //
		{ "angular-messages", "js/angular-modules/1.7.7/angular-messages.js" }, //
		{ "angular-resource", "js/angular-modules/1.7.7/angular-resource.js" }, //
		{ "angular-touch", "js/angular-modules/1.7.7/angular-touch.js" } };
	public static final String BOOTSTRAP_CSS = "css/bootstrap/css/bootstrap.css";

	public static final String[] INDEX_3RD_PARTY_CSS = { //
		"js/bootstrap-window/css/bootstrap-window.css" };
	public static final String[] INDEX_3RD_PARTY_JS = { //
		"js/jquery-3.3.1.js", //
		"js/jquery.maskedinput.js", //
		ANGULAR_JS, //
		"js/angular-sanitize.js", //
		"js/angular-translate.js", //
		"js/angular-webstorage.js", //
		"js/angularui/ui-bootstrap-tpls-2.4.0.js", //
		"js/numeral.js", //
		"js/locales.js", //
		"js/angular-file-upload/dist/ng-file-upload.js", //
		"js/bootstrap-window/js/Window.js", //
		"js/bootstrap-window/js/WindowManager.js" };
	private static final String[] INDEX_SABLO_JS = { //
		"sablo/lib/reconnecting-websocket.js", //
		"sablo/js/websocket.js", //
		"sablo/js/sablo_app.js", //
		"sablo/sabloService.js" };
	public static final String[] INDEX_SERVOY_JS = { //
		"js/servoy.js", //
		"js/servoyWindowManager.js", //
		"js/servoyformat.js", //
		"js/servoytooltip.js", //
		"js/fileupload.js", //
		"js/servoy-components.js", //
		"js/servoy_alltemplates.js", //
		"js/servoy_app.js" };

	private String[] locations;
	private String[] services;

	private String group_id;

	private final JSTemplateGenerator jsTemplateGenerator = new JSTemplateGenerator();

	private static final Parser USER_AGENT_PARSER;

	static
	{
		try
		{
			USER_AGENT_PARSER = new Parser();
		}
		catch (IOException e)
		{
			// should not happen, default config should always be found
			throw new RuntimeException(e);
		}
	}

	public NGClientEntryFilter()
	{
		super(CLIENT_ENDPOINT);
	}

	@Override
	public void init(final FilterConfig fc) throws ServletException
	{
		ApplicationServerRegistry.getServiceRegistry().registerService(IMessagesRecorder.class, new MessageRecorder());
		// when started in developer - init is done in the ResourceProvider filter
		if (!ApplicationServerRegistry.get().isDeveloperStartup())
		{
			try (InputStream is = fc.getServletContext().getResourceAsStream("/WEB-INF/components.properties"))
			{
				Properties properties = new Properties();
				properties.load(is);
				String loc = properties.getProperty("locations", "").trim();
				locations = "".equals(loc) ? new String[0] : loc.split(";");
			}
			catch (Exception e)
			{
				Debug.error("Exception during init components.properties reading", e);
			}
			try (InputStream is = fc.getServletContext().getResourceAsStream("/WEB-INF/services.properties"))
			{
				Properties properties = new Properties();
				properties.load(is);
				String loc = properties.getProperty("locations", "").trim();
				services = "".equals(loc) ? new String[0] : loc.split(";");
			}
			catch (Exception e)
			{
				Debug.error("Exception during init services.properties reading", e);
			}

			Types.getTypesInstance().registerTypes();

			if (getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.enableWebResourceOptimizer", "true")))
			{
				try
				{
					URL url = fc.getServletContext().getResource("/WEB-INF/groupid.properties");
					if (url != null)
					{
						try (InputStream is = url.openStream())
						{
							Properties properties = new Properties();
							properties.load(is);
							group_id = properties.getProperty("groupid");
							fc.getServletContext().setAttribute(SVYGRP, group_id);
						}
						catch (Exception e)
						{
							Debug.error("Exception during init groupid.properties reading", e);
						}
					}
					else
					{
						Debug.warn("Cannot use the optimized resources, the groupid.properties file was not found.");
					}
				}
				catch (MalformedURLException e)
				{
					Debug.error(e);
				}
			}

			super.init(fc);
		}
	}

	@Override
	public void initWebComponentSpecs(FilterConfig fc)
	{
		WebComponentSpecProvider.init(fc.getServletContext(), getWebComponentBundleNames(), DefaultComponentPropertiesProvider.instance);
	}

	@Override
	public String[] getWebComponentBundleNames()
	{
		return locations;
	}

	@Override
	public String[] getServiceBundleNames()
	{
		return services;
	}

	/**
	 * Get form script references, useful for debugging
	 * @param fs the flattened solution
	 * @return the form script contributions
	 */
	private Collection<String> getFormScriptReferences(FlattenedSolution fs)
	{
		List<String> formScripts = new ArrayList<>();
		if (Boolean.valueOf(System.getProperty("servoy.generateformscripts", "false")).booleanValue())
		{
			Iterator<Form> it = fs.getForms(false);
			while (it.hasNext())
			{
				Form form = it.next();
				Solution sol = (Solution)form.getAncestor(SOLUTIONS);
				formScripts.add("solutions/" + sol.getName() + "/forms/" + form.getName() + ".js");
			}
		}
		return formScripts;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		try
		{
			HttpServletRequest request = (HttpServletRequest)servletRequest;
			HttpServletResponse response = (HttpServletResponse)servletResponse;
			if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
			String uri = request.getRequestURI();

			if (handleShortSolutionRequest(request, response))
			{
				return;
			}

			if (handleDeeplink(request, response))
			{
				return;
			}

			if (handleRecording(request, response))
			{
				return;
			}


			if (uri != null && (uri.endsWith(".html") || uri.endsWith(".js")))
			{
				String solutionName = getSolutionNameFromURI(uri);
				if (solutionName != null)
				{
					String clientnr = getClientNr(uri, request);
					INGClientWebsocketSession wsSession = null;
					HttpSession httpSession = request.getSession(false);
					if (clientnr != null && httpSession != null)
					{
						wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(CLIENT_ENDPOINT, httpSession, Integer.parseInt(clientnr));
					}
					FlattenedSolution fs = null;
					boolean closeFS = false;
					if (wsSession != null)
					{
						fs = wsSession.getClient().getFlattenedSolution();
					}
					if (fs == null)
					{
						try
						{
							closeFS = true;
							IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
							if (applicationServerUnavailable(response, as))
							{
								return;
							}

							SolutionMetaData solutionMetaData = (SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(
								solutionName, SOLUTIONS);
							if (solutionMissing(response, solutionName, solutionMetaData))
							{
								return;
							}

							fs = new FlattenedSolution(solutionMetaData, new AbstractActiveSolutionHandler(as)
							{
								@Override
								public IRepository getRepository()
								{
									return ApplicationServerRegistry.get().getLocalRepository();
								}
							});
						}
						catch (Exception e)
						{
							Debug.error("error loading solution: " + solutionName + " for clientnr: " + clientnr, e);
						}
					}

					if (fs != null)
					{
						try
						{
							if (handleMainJs(request, response, fs))
							{
								return;
							}

							if (handleForm(request, response, wsSession, fs))
							{
								return;
							}

							if (handleMaintenanceMode(request, response, wsSession))
							{
								return;
							}

							// prepare for possible index.html lookup
							Map<String, Object> variableSubstitution = getSubstitutions(request, solutionName, clientnr, fs);

							List<String> extraMeta = new ArrayList<String>();
							addManifest(fs, extraMeta);
							addHeadIndexContributions(fs, extraMeta);

							super.doFilter(servletRequest, servletResponse, filterChain, asList(SERVOY_CSS), new ArrayList<String>(getFormScriptReferences(fs)),
								extraMeta, variableSubstitution, getContentSecurityPolicyConfig(request));
							return;
						}
						finally
						{
							if (closeFS)
							{
								fs.close(null);
							}
						}
					}
				}
			}

			if (!uri.contains(ModifiablePropertiesGenerator.PUSH_TO_SERVER_BINDINGS_LIST))
				Debug.log("No solution found for this request, calling the default filter: " + uri);
			super.doFilter(servletRequest, servletResponse, filterChain, null, null, null, null, null);
		}
		catch (RuntimeException | Error e)
		{
			Debug.error(e);
			throw e;
		}
	}

	/**
	 * Get the ContentSecurityPolicyConfig is it should be applied, otherwise return null;
	 *
	 * Only when configured and when the browser is a modern browser that supports Content-Security-Policy level 3.
	 */
	private ContentSecurityPolicyConfig getContentSecurityPolicyConfig(HttpServletRequest request)
	{
		Settings settings = Settings.getInstance();
		if (!getAsBoolean(settings.getProperty("servoy.ngclient.setContentSecurityPolicyHeader", "true")))
		{
			Debug.trace("ContentSecurityPolicyHeader is disabled by configuration");
			return null;
		}

		String userAgentHeader = request.getHeader("user-agent");
		UserAgent userAgent = USER_AGENT_PARSER.parseUserAgent(userAgentHeader);

		if (!supportsContentSecurityPolicyLevel3(userAgent))
		{
			if (Debug.tracing())
			{
				Debug.trace("ContentSecurityPolicyHeader is disabled, user agent '" + userAgent + "' does not support ContentSecurityPolicy level 3");
			}
			return null;
		}

		ContentSecurityPolicyConfig contentSecurityPolicyConfig = new ContentSecurityPolicyConfig(HTTPUtils.getNonce(request));

		// Overridable directives
		setDirectiveOverride(contentSecurityPolicyConfig, "frame-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "style-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "img-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "font-src", settings);

		return contentSecurityPolicyConfig;

	}

	private static void setDirectiveOverride(ContentSecurityPolicyConfig contentSecurityPolicyConfig, String directive, Settings settings)
	{
		String override = settings.getProperty("servoy.ngclient.contentSecurityPolicy." + directive);
		if (override != null && override.trim().length() > 0 && override.indexOf(';') < 0)
		{
			contentSecurityPolicyConfig.setDirective(directive, override);
		}
	}

	/** Does the CSP level 3, specifically strict-dynamic.
	 *
	 * @see <a href="https://caniuse.com/#feat=mdn-http_headers_csp_content-security-policy_strict-dynamic">caniuse.com</a>
	 */
	private boolean supportsContentSecurityPolicyLevel3(UserAgent userAgent)
	{
		if (("Chrome".equals(userAgent.family) || "Chromium".equals(userAgent.family)) && parseInt(userAgent.major) >= 52)
		{
			return true;
		}
		if ("Firefox".equals(userAgent.family) && parseInt(userAgent.major) >= 52)
		{
			return true;
		}
		if ("Edge".equals(userAgent.family) && parseInt(userAgent.major) >= 74) // Chromium-based
		{
			return true;
		}
		if ("Opera".equals(userAgent.family) && parseInt(userAgent.major) >= 39)
		{
			return true;
		}
		if ("Chrome Mobile WebView".equals(userAgent.family) && parseInt(userAgent.major) >= 76)
		{
			return true;
		}
		if ("Opera Mini".equals(userAgent.family) && parseInt(userAgent.major) >= 46)
		{
			return true;
		}
		if ("Samsung Internet".equals(userAgent.family) &&
			(parseInt(userAgent.major) > 6 || (parseInt(userAgent.major) == 6 && parseInt(userAgent.minor) >= 2)))
		{
			return true;
		}

		return false;
	}

	private void addManifest(FlattenedSolution fs, List<String> extraMeta)
	{
		if (fs.getMedia("manifest.json") != null)
		{
			String url = "resources/" + FLATTENED_SOLUTION_ACCESS + "/" + fs.getName() + "/manifest.json";
			extraMeta.add("<link rel=\"manifest\" href=\"" + url + "\">");
		}

	}

	private void addHeadIndexContributions(FlattenedSolution fs, List<String> extraMeta)
	{
		Media headExtension = fs.getMedia("head-index-contributions.html");
		if (headExtension != null)
		{
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(headExtension.getMediaData()), "UTF8")))
			{
				String line;
				for (int count = 0; count < 1000 && (line = reader.readLine()) != null; count++)
				{
					if (line.trim().startsWith("<meta") || line.trim().startsWith("<link"))
					{
						extraMeta.add(line);
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

	}

	private boolean handleMaintenanceMode(HttpServletRequest request, HttpServletResponse response, INGClientWebsocketSession wsSession) throws IOException
	{
		boolean maintenanceMode = wsSession == null //
			&& ApplicationServerRegistry.get().getDataServer().isInServerMaintenanceMode() //
			// when there is a http session, let the new client go through, otherwise another
			// client from the same browser may be killed by a load balancer
			&& request.getSession(false) == null;
		if (maintenanceMode)
		{
			response.getWriter().write("Server in maintenance mode");
			response.setStatus(SC_SERVICE_UNAVAILABLE);
			return true;
		}

		return false;
	}

	private boolean handleForm(HttpServletRequest request, HttpServletResponse response, INGClientWebsocketSession wsSession, FlattenedSolution fs)
		throws IOException
	{
		String uri = request.getRequestURI();
		String formName = getFormNameFromURI(uri);
		if (formName != null)
		{
			Form f = fs.getForm(formName);
			if (f == null && wsSession != null) f = wsSession.getClient().getFormManager().getPossibleForm(formName);
			Form form = (f != null ? fs.getFlattenedForm(f) : null);
			if (form != null)
			{
				PrintWriter writer = response.getWriter();
				if (request.getParameter("svy_designvalue") != null)
				{
					response.setContentType("text/html");
					DesignFormLayoutStructureGenerator.generateLayout(form, fs, writer);
					writer.flush();
					return true;

				}

				if (HTTPUtils.checkAndSetUnmodified(request, response, fs.getLastModifiedTime()))
				{
					return true;
				}

				HTTPUtils.setNoCacheHeaders(response);

				boolean design = getAsBoolean(request.getParameter("design"));
				if (uri.endsWith(".html"))
				{
					response.setContentType("text/html");
					if (form.isResponsiveLayout())
					{
						FormLayoutStructureGenerator.generateLayout(form, formName, fs, writer,
							design ? new DesignProperties(Utils.getAsInteger(request.getParameter("cont"))) : null);
					}
					else
					{
						FormLayoutGenerator.generateRecordViewForm(writer, form, formName,
							wsSession != null ? new ServoyDataConverterContext(wsSession.getClient()) : new ServoyDataConverterContext(fs), design);
					}
				}
				else if (uri.endsWith(".js"))
				{
					response.setContentType("text/javascript");
					new FormTemplateGenerator(wsSession != null ? new ServoyDataConverterContext(wsSession.getClient()) : new ServoyDataConverterContext(fs),
						false, design).generate(form, formName, "form_recordview_js.ftl", writer);
				}

				writer.flush();
				return true;
			}
		}

		return false;
	}

	private boolean handleMainJs(HttpServletRequest request, HttpServletResponse response, FlattenedSolution fs) throws ServletException, IOException
	{
		String uri = request.getRequestURI();
		String mainjs = getMainJs(uri);
		if (mainjs != null)
		{
			if (HTTPUtils.checkAndSetUnmodified(request, response, fs.getLastModifiedTime()))
			{
				return true;
			}
			HTTPUtils.setNoCacheHeaders(response);

			response.setContentType("text/javascript");
			PrintWriter writer = response.getWriter();

			String solutionName = getSolutionNameFromURI(uri);
			String clientnr = getClientNr(uri, request);

			Map<String, Object> variableSubstitution = getSubstitutions(request, solutionName, clientnr, fs);

			jsTemplateGenerator.generate(mainjs, variableSubstitution, writer);

			writer.flush();
			return true;
		}

		return false;
	}

	/**
	 * @param response
	 * @param solutionName
	 * @param solutionMetaData
	 * @throws IOException
	 */
	private boolean solutionMissing(HttpServletResponse response, String solutionName, SolutionMetaData solutionMetaData) throws IOException
	{
		if (solutionMetaData == null)
		{
			Debug.error("Solution '" + solutionName + "' was not found.");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			Writer w = response.getWriter();
			w.write(
				"<html><head><link rel=\"stylesheet\" href=\"/css/bootstrap/css/bootstrap.css\"/><link rel=\"stylesheet\" href=\"/css/servoy.css\"/></head><body><div style='padding:40px;'><div class=\"bs-callout bs-callout-danger\" ><h1>Page cannot be displayed</h1><p>Requested solution was not found.</p></div></div></body></html>");
			w.close();
			return true;
		}
		return false;
	}

	private boolean applicationServerUnavailable(HttpServletResponse response, IApplicationServer as) throws IOException
	{
		if (as == null)
		{
			response.setStatus(SC_SERVICE_UNAVAILABLE);
			Writer w = response.getWriter();
			w.write(
				"<html><head><link rel=\"stylesheet\" href=\"/css/bootstrap/css/bootstrap.css\"/><link rel=\"stylesheet\" href=\"/css/servoy.css\"/></head><body><div style='padding:20px;color:#fd7100'><div class=\"bs-callout bs-callout-danger\"><p>System is inaccessible. Please contact your system administrator.</p></div></div></body></html>");
			w.close();
			return true;
		}

		return false;
	}

	private boolean handleRecording(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String uri = request.getRequestURI();
		if (uri != null && uri.endsWith(".recording"))
		{
			IMessagesRecorder recorder = ApplicationServerRegistry.get().getService(IMessagesRecorder.class);
			int index = uri.lastIndexOf('/');
			CharSequence message = recorder.getMessage(uri.substring(index + 1, uri.length() - 10));
			if (message != null)
			{
				HTTPUtils.setNoCacheHeaders(response);
				response.setContentType("text/plain");
				response.getWriter().write(message.toString());
				return true;
			}

		}

		return false;
	}

	private boolean handleDeeplink(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String url = request.getRequestURL().toString();
		int index = url.indexOf(SOLUTIONS_PATH);
		if (index >= 0 && getMainJs(url) == null)
		{
			String solutionAndRest = url.substring(index + SOLUTIONS_PATH.length());
			int solutionEnd = solutionAndRest.indexOf('/');
			String rest = solutionAndRest.substring(solutionEnd + 1);
			if (rest.indexOf('/') != -1)
			{
				// it has deeplinks, need to rewrite url.
				StringBuffer redirectUrl = new StringBuffer(url.subSequence(0, index + SOLUTIONS_PATH.length() + solutionEnd));
				redirectUrl.append("/index.html");
				String queryString = request.getQueryString();
				String[] args = rest.split("/");

				if (args.length != 0 || queryString != null)
				{
					redirectUrl.append("?");
					if (queryString != null) redirectUrl.append(queryString);

					if (args.length % 2 == 0)
					{
						int i = 0;
						while (i < args.length - 1)
						{
							if (redirectUrl.indexOf("=") > 0) redirectUrl.append("&");
							redirectUrl.append(args[i] + "=" + args[i + 1]);
							i += 2;
						}
					}
					response.sendRedirect(redirectUrl.toString());
					return true;
				}
			}
		}
		return false;
	}

	private String getMainJs(String uri)
	{
		Matcher matcher = MAIN_JS_PATTERN.matcher(uri);
		if (matcher.find())
		{
			return matcher.group(1);
		}

		return null;
	}

	private boolean handleShortSolutionRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if (isShortSolutionRequest(request))
		{
			StringBuffer url = request.getRequestURL();
			if (!url.toString().endsWith("/")) url.append("/");
			url.append("index.html");
			String queryString = request.getQueryString();
			if (queryString != null) url.append("?").append(queryString);
			response.sendRedirect(url.toString());
			return true;
		}

		return false;
	}

	/**
	 * @param request
	 * @param solutionName
	 * @param clientnr
	 * @param fs
	 * @return
	 * @throws ServletException
	 */
	private Map<String, Object> getSubstitutions(HttpServletRequest request, String solutionName, String clientnr, FlattenedSolution fs) throws ServletException
	{
		Map<String, Object> variableSubstitution = new HashMap<>();

		variableSubstitution.put("solution", solutionName);
		variableSubstitution.put("clientnr", clientnr);

		variableSubstitution.put("contextPath", Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/'));
		variableSubstitution.put("pathname", request.getRequestURI());
		variableSubstitution.put("querystring", HTTPUtils.generateQueryString(request.getParameterMap(), request.getCharacterEncoding()));
		variableSubstitution.put("nonce", HTTPUtils.getNonce(request));
		String titleText = fs.getSolution().getTitleText();
		if (StringUtils.isBlank(titleText))
		{
			titleText = fs.getSolution().getName();
		}
		else if (titleText.equals("<empty>") || titleText.contains("i18n:") || titleText.contains(TagParser.TAGCHAR))
		{
			titleText = "";
		}
		variableSubstitution.put("solutionTitle", titleText);

		variableSubstitution.put("orientation", Integer.valueOf(fs.getSolution().getTextOrientation()));

		String ipaddr = request.getHeader("X-Forwarded-For"); // in case there is a forwarding proxy
		if (ipaddr == null)
		{
			ipaddr = request.getRemoteAddr();
		}
		variableSubstitution.put("ipaddr", ipaddr);
		variableSubstitution.put("hostaddr", request.getRemoteHost());

		// push some translations to the client, in case the client cannot connect back
		JSONObject defaultTranslations = new JSONObject();
		defaultTranslations.put("servoy.ngclient.reconnecting",
			getSolutionDefaultMessage(fs.getSolution(), request.getLocale(), "servoy.ngclient.reconnecting"));
		variableSubstitution.put("defaultTranslations", defaultTranslations.toString());

		return variableSubstitution;
	}

	// checks for short solution request, like "<context>/solutions/solution_name" or "<context>/solutions/solution_name/"
	private boolean isShortSolutionRequest(HttpServletRequest request)
	{
		String uri = request.getRequestURI();
		String contextPathWithSolutionPath = request.getContextPath() + SOLUTIONS_PATH;
		if (uri != null && uri.startsWith(contextPathWithSolutionPath))
		{
			String solutionName = uri.substring(contextPathWithSolutionPath.length());
			if (solutionName.length() > 0)
			{
				int firstSlashIdx = solutionName.indexOf('/');
				if (firstSlashIdx == -1 || (solutionName.length() > 1 && (firstSlashIdx == solutionName.length() - 1)))
				{
					return true;
				}
			}
		}
		return false;
	}

	private String getSolutionDefaultMessage(Solution solution, Locale locale, String key)
	{
		// removed the cache, if this gets called more often we may add it again
		return getSolutionDefaultMessageNotCached(solution.getID(), locale, key);
	}

	private String getSolutionDefaultMessageNotCached(int solutionId, Locale locale, String key)
	{
		MessagesResourceBundle messagesResourceBundle = new MessagesResourceBundle(null /* application */, locale == null ? Locale.ENGLISH : locale,
			null /* columnNameFilter */, null /* columnValueFilter */, solutionId);
		return messagesResourceBundle.getString(key);
	}

	/**
	 * Get the form name from url
	 * @param uri
	 * @return the name or null
	 */
	private String getFormNameFromURI(String uri)
	{
		int formIndex = uri.indexOf(FORMS_PATH);
		if (formIndex > 0)
		{
			String formName = uri.substring(formIndex + FORMS_PATH.length());
			formName = formName.replace(".html", "");
			formName = formName.replace(".js", "");
			return formName;
		}
		return null;
	}

	/**
	 * Get the solution name from an url
	 * @param uri
	 * @return the name or null
	 */
	private String getSolutionNameFromURI(String uri)
	{
		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		if (solutionIndex >= 0)
		{
			return uri.substring(solutionIndex + SOLUTIONS_PATH.length(), uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1));
		}
		return null;
	}

	/**
	 * Get the clientnr from parameter or an url /solutions/<solutionname>/<clientnr>/
	 *
	 */
	private String getClientNr(String uri, ServletRequest request)
	{
		String clientnr = request.getParameter("clientnr");
		if (clientnr != null)
		{
			return clientnr;
		}


		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		if (solutionIndex >= 0)
		{
			String[] parts = uri.substring(solutionIndex + SOLUTIONS_PATH.length()).split("/");
			if (parts.length >= 2 && parts[1].matches("[0-9]+"))
			{
				return parts[1];
			}
		}
		return null;
	}

	@Override
	protected IWebsocketSessionFactory createSessionFactory()
	{
		return new WebsocketSessionFactory();
	}

	@Override
	protected URL getIndexPageResource(HttpServletRequest request) throws IOException
	{
		String uri = request.getRequestURI();
		if (uri != null && uri.endsWith("index.html"))
		{
			return getClass().getResource("index.html");
		}
		return super.getIndexPageResource(request);
	}

	private Collection<String> appendGroupIdRequestParamToUrls(Collection<String> urls)
	{
		if (group_id != null)
		{
			ArrayList<String> groupIdRequestParamUrls = new ArrayList<>();
			String rp = "svy_gid=" + group_id;
			for (String url : urls)
			{
				try
				{
					URI uri = new URI(url);
					String newQuery = uri.getQuery();
					if (newQuery == null)
					{
						newQuery = rp;
					}
					else
					{
						newQuery += "&" + rp;
					}
					URI newUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
					groupIdRequestParamUrls.add(newUri.toString());
				}
				catch (URISyntaxException e)
				{
					Debug.error("Error appending svy_gid request param to " + url, e);
				}
			}
			return groupIdRequestParamUrls;
		}
		return urls;
	}

	@Override
	public List<String> filterCSSContributions(List<String> cssContributions)
	{
		ArrayList<String> allIndexCSS;
		if (getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.enableWebResourceOptimizer", "true")) && group_id != null)
		{
			allIndexCSS = new ArrayList<String>();
			allIndexCSS.add("wro/" + SERVOY_CSS_THIRDPARTY_SVYGRP + group_id + ".css");
			//get all css contributions which do not support grouping
			allIndexCSS.addAll(appendGroupIdRequestParamToUrls((Collection<String>)IndexPageEnhancer.getAllContributions(Boolean.FALSE, this)[0]));
			allIndexCSS.add("wro/" + SERVOY_CSS_CONTRIBUTIONS_SVYGRP + group_id + ".css");
		}
		else
		{
			allIndexCSS = new ArrayList<String>(appendGroupIdRequestParamToUrls(asList(INDEX_3RD_PARTY_CSS)));
			allIndexCSS.addAll(appendGroupIdRequestParamToUrls(cssContributions));
		}
		return allIndexCSS;
	}

	@Override
	public List<String> filterJSContributions(List<String> jsContributions)
	{
		ArrayList<String> allIndexJS;
		if (getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.enableWebResourceOptimizer", "true")) && group_id != null)
		{
			allIndexJS = new ArrayList<String>();
			allIndexJS.add("wro/" + SERVOY_THIRDPARTY_SVYGRP + group_id + ".js");
			allIndexJS.addAll(appendGroupIdRequestParamToUrls(asList(INDEX_SABLO_JS)));
			allIndexJS.add("wro/" + SERVOY_APP_SVYGRP + group_id + ".js");
			//get all contributions which do not support grouping
			allIndexJS.addAll(appendGroupIdRequestParamToUrls((Collection<String>)IndexPageEnhancer.getAllContributions(Boolean.FALSE, this)[1]));
			allIndexJS.add("wro/" + SERVOY_CONTRIBUTIONS_SVYGRP + group_id + ".js");
		}
		else
		{
			allIndexJS = new ArrayList<String>(appendGroupIdRequestParamToUrls(asList(INDEX_3RD_PARTY_JS)));
			allIndexJS.addAll(appendGroupIdRequestParamToUrls(asList(INDEX_SABLO_JS)));
			allIndexJS.addAll(appendGroupIdRequestParamToUrls(asList(INDEX_SERVOY_JS)));
			allIndexJS.addAll(appendGroupIdRequestParamToUrls(jsContributions));
		}
		if (ApplicationServerRegistry.get().isDeveloperStartup()) allIndexJS.add("js/debug.js");
		return allIndexJS;
	}

	public static final IContributionEntryFilter CONTRIBUTION_ENTRY_FILTER = new IContributionEntryFilter()
	{
		@Override
		public JSONObject filterContributionEntry(JSONObject contributionEntry)
		{
			String name = contributionEntry.optString("name");

			// replace angular module js with the one from Servoy, to ensure correct version is used
			if (name != null)
			{
				int firstDotIdx;
				if ((firstDotIdx = name.indexOf('.')) > 0)
				{
					name = name.substring(0, firstDotIdx);
				}

				for (String[] angularModules : ANGULAR_JS_MODULES)
				{
					if (angularModules[0].equals(name))
					{
						contributionEntry.put("url", angularModules[1]);
						break;
					}
				}

			}
			return contributionEntry;
		}
	};

	public JSONObject filterContributionEntry(JSONObject contributionEntry)
	{
		return CONTRIBUTION_ENTRY_FILTER.filterContributionEntry(contributionEntry);
	}

	@Override
	public void destroy()
	{
		super.destroy();
		FormElementHelper.INSTANCE.reload();
	}
}
