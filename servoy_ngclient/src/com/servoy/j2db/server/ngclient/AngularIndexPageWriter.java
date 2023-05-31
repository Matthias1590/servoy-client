/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import static com.servoy.j2db.persistence.IRepository.SOLUTIONS;
import static com.servoy.j2db.server.ngclient.MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS;
import static com.servoy.j2db.server.ngclient.WebsocketSessionFactory.CLIENT_ENDPOINT;
import static com.servoy.j2db.util.Utils.getAsBoolean;
import static java.util.stream.Collectors.joining;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.sablo.util.HTTPUtils;
import org.sablo.websocket.WebsocketSessionManager;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.MessagesResourceBundle;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.headlessclient.util.HCUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;

/**
 * @author jcompagner
 * @since 2021.03
 */
@SuppressWarnings("nls")
public class AngularIndexPageWriter
{
	public static final String SOLUTIONS_PATH = "/solution/";
	private static final String JWT_Password = "jwt";

	public static void writeStartupJs(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws IOException, ServletException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		String uri = request.getRequestURI();
		String clientnr = getClientNr(uri, request);
		Pair<FlattenedSolution, Boolean> pair = getFlattenedSolution(solutionName, clientnr, request, response);
		JSONObject json = new JSONObject();
		json.put("pathName", request.getRequestURI().replaceAll("[^/]*/[^/]*/startup.js$", "index.html"));
		json.put("querystring", HTTPUtils.generateQueryString(request.getParameterMap(), request.getCharacterEncoding()));
		String ipaddr = request.getHeader("X-Forwarded-For"); // in case there is a forwarding proxy
		if (ipaddr == null)
		{
			ipaddr = request.getRemoteAddr();
		}
		json.put("ipaddr", ipaddr);
		String remoteHost = request.getHeader("X-Forwarded-Host"); // in case there is a forwarding proxy
		if (remoteHost == null)
		{
			remoteHost = request.getRemoteHost();
		}
		json.put("hostaddr", remoteHost);
		if (pair.getLeft() != null)
		{
			Solution solution = pair.getLeft().getSolution();
			json.put("orientation", solution.getTextOrientation());
			JSONObject defaultTranslations = new JSONObject();
			defaultTranslations.put("servoy.ngclient.reconnecting",
				getSolutionDefaultMessage(solution, request.getLocale(), "servoy.ngclient.reconnecting"));
			json.put("defaultTranslations", defaultTranslations);

		}

		StringBuilder sb = new StringBuilder(256);

		sb.append("window.svyData=");
		sb.append(json.toString());

		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/javascript");
		response.setContentLengthLong(sb.length());
		response.getWriter().write(sb.toString());
		if (pair.getRight().booleanValue()) pair.getLeft().close(null);
	}


	public static void writeIndexPage(String page, HttpServletRequest request, HttpServletResponse response, String solutionName,
		String contentSecurityPolicyNonce)
		throws IOException, ServletException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		String uri = request.getRequestURI();
		String clientnr = getClientNr(uri, request);
		Pair<FlattenedSolution, Boolean> pair = getFlattenedSolution(solutionName, clientnr, request, response);
		FlattenedSolution fs = pair.getLeft();
		if (fs != null)
		{
			StringBuilder sb = new StringBuilder(756);

			String indexHtml = page;
			final String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
			sb.append("<base href=\"");
			sb.append(path);
			sb.append("\">");

			ContentSecurityPolicyConfig contentSecurityPolicyConfig = getContentSecurityPolicyConfig(request);
			if (contentSecurityPolicyNonce != null)
			{
				indexHtml = indexHtml.replace("<script ", "<script nonce='" + contentSecurityPolicyNonce + '\'');
			}

			String titleText = fs.getSolution().getTitleText();
			if (StringUtils.isBlank(titleText))
			{
				titleText = fs.getSolution().getName();
			}
			else if (titleText.equals("<empty>") || titleText.contains("i18n:") || titleText.contains(TagParser.TAGCHAR))
			{
				titleText = "";
			}
			sb.append("\n  <title>");
			sb.append(titleText);
			sb.append("</title>");

			if (fs.getMedia("manifest.json") != null)
			{
				String url = "resources/" + FLATTENED_SOLUTION_ACCESS + "/" + fs.getName() + "/manifest.json";
				sb.append("\n  <link rel=\"manifest\" href=\"");
				sb.append(url);
				sb.append("\">");
			}
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
							sb.append("\n  ");
							sb.append(line);
						}
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}

			sb.append("\n  <script ");
			if (contentSecurityPolicyConfig != null)
			{
				sb.append("nonce='");
				sb.append(contentSecurityPolicyConfig.getNonce());
				sb.append("' ");
			}
			sb.append("src=\"solution/");
			sb.append(solutionName);
			sb.append('/');
			sb.append(clientnr);
			sb.append("/main/startup.js?");
			sb.append(HTTPUtils.generateQueryString(request.getParameterMap(), request.getCharacterEncoding()));
			sb.append("\"></script>");
			indexHtml = indexHtml.replace("<base href=\"/\">", sb.toString());

			String requestLanguage = request.getHeader("accept-language");
			if (requestLanguage != null)
			{
				indexHtml = indexHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
			}

			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			response.setContentLengthLong(indexHtml.length());
			response.getWriter().write(indexHtml);

			if (pair.getRight().booleanValue()) fs.close(null);
		}
		return;
	}

	private static Pair<FlattenedSolution, Boolean> getFlattenedSolution(String solutionName, String clientnr, HttpServletRequest request,
		HttpServletResponse response)
	{
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
					return new Pair<FlattenedSolution, Boolean>(null, Boolean.FALSE);
				}

				SolutionMetaData solutionMetaData = (SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository()
					.getRootObjectMetaData(
						solutionName, SOLUTIONS);
				if (solutionMissing(response, solutionName, solutionMetaData))
				{
					return new Pair<FlattenedSolution, Boolean>(null, Boolean.FALSE);
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
		return new Pair<FlattenedSolution, Boolean>(fs, Boolean.valueOf(closeFS));
	}

	/**
	 * @param response
	 * @param solutionName
	 * @param solutionMetaData
	 * @throws IOException
	 */
	public static boolean solutionMissing(HttpServletResponse response, String solutionName, SolutionMetaData solutionMetaData) throws IOException
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

	public static boolean applicationServerUnavailable(HttpServletResponse response, IApplicationServer as) throws IOException
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

	/**
	 * Get the clientnr from parameter or an url /solutions/<solutionname>/<clientnr>/
	 *
	 */
	public static String getClientNr(String uri, ServletRequest request)
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

	public static String getSolutionDefaultMessage(Solution solution, Locale locale, String key)
	{
		// removed the cache, if this gets called more often we may add it again
		return getSolutionDefaultMessageNotCached(solution.getID(), locale, key);
	}

	public static String getSolutionDefaultMessageNotCached(int solutionId, Locale locale, String key)
	{
		MessagesResourceBundle messagesResourceBundle = new MessagesResourceBundle(null /* application */, locale == null ? Locale.ENGLISH : locale,
			null /* columnNameFilter */, null /* columnValueFilter */, solutionId);
		return messagesResourceBundle.getString(key);
	}

	public static ContentSecurityPolicyConfig addcontentSecurityPolicyHeader(HttpServletRequest request, HttpServletResponse response, boolean allowUnsafeEval)
	{
		ContentSecurityPolicyConfig contentSecurityPolicyConfig = getContentSecurityPolicyConfig(request);
		if (contentSecurityPolicyConfig != null)
		{
			response.addHeader("Content-Security-Policy", contentSecurityPolicyConfig.getDirectives().entrySet().stream()
				.map(entry -> {
					String value = entry.getValue();
					if (!allowUnsafeEval && "script-src".equals(entry.getKey()))
					{
						value = value.replace("'unsafe-eval'", "");
					}
					return entry.getKey() + ' ' + value;
				})
				.collect(joining("; ")));
		}
		return contentSecurityPolicyConfig;
	}

	/**
	 * Get the ContentSecurityPolicyConfig is it should be applied, otherwise return null;
	 *
	 * Only when configured and when the browser is a modern browser that supports Content-Security-Policy level 3.
	 */
	private static ContentSecurityPolicyConfig getContentSecurityPolicyConfig(HttpServletRequest request)
	{
		Settings settings = Settings.getInstance();
		if (!getAsBoolean(settings.getProperty("servoy.ngclient.setContentSecurityPolicyHeader", "true")))
		{
			Debug.trace("ContentSecurityPolicyHeader is disabled by configuration");
			return null;
		}

		String userAgentHeader = request.getHeader("user-agent");

		if (!HCUtils.supportsContentSecurityPolicyLevel3(userAgentHeader))
		{
			if (Debug.tracing())
			{
				Debug.trace("ContentSecurityPolicyHeader is disabled, user agent '" + userAgentHeader + "' does not support ContentSecurityPolicy level 3");
			}
			return null;
		}

		ContentSecurityPolicyConfig contentSecurityPolicyConfig = new ContentSecurityPolicyConfig(HTTPUtils.getNonce(request));

		// Overridable directives
		setDirectiveOverride(contentSecurityPolicyConfig, "frame-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "frame-ancestors", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "style-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "img-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "font-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "form-action", settings);

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

	public static boolean handleDeeplink(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String url = request.getRequestURL().toString();
		int index = url.indexOf(SOLUTIONS_PATH);
		if (index >= 0)
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

	protected static boolean handleMaintenanceMode(HttpServletRequest request, HttpServletResponse response, INGClientWebsocketSession wsSession)
		throws IOException
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

	public static boolean mustAuthenticate(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws ServletException, IOException
	{
		boolean needToLogin = false;
		String requestURI = request.getRequestURI();
		if (solutionName != null &&
			(!requestURI.contains("/designer") && (requestURI.endsWith("/") || requestURI.endsWith("/" + solutionName) ||
				requestURI.toLowerCase().endsWith("/index.html"))))
		{
			Pair<FlattenedSolution, Boolean> _fs = getFlattenedSolution(solutionName, null, request, null);
			FlattenedSolution fs = _fs.getLeft();

			try
			{
				needToLogin = fs.getMainSolutionMetaData().getMustAuthenticate() && fs.getSolution().getLoginFormID() == 0 &&
					fs.getSolution().getLoginSolutionName() == null;
			}
			catch (RepositoryException e)
			{
				throw new ServletException(e);
			}
		}
		if (needToLogin)
		{
			String user = request.getParameter("user");
			String password = request.getParameter("password");
			if (user != null && password != null)
			{
				needToLogin = !checkUser(request, response, solutionName, user, password);
				if (!needToLogin) return false;
			}

			String id_token = request.getParameter("id_token");
			Cookie idCookie = null;
			if (id_token == null)
			{
				idCookie = getCookie(request, "id_token");
				if (idCookie != null)
				{
					id_token = idCookie.getValue();
				}
			}
			if (id_token != null)
			{
				Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
				JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(settings.getProperty(JWT_Password)))
					.build();
				try
				{
					jwtVerifier.verify(id_token);
					needToLogin = false;
				}
				catch (JWTVerificationException ex)
				{
					//token verification failed, show login page
					if (idCookie != null)
					{
						idCookie = new Cookie("id_token", "");
						idCookie.setMaxAge(0);
						idCookie.setPath("/");
						idCookie.setDomain(request.getServerName());
						response.addCookie(idCookie);
					}

					if (ex instanceof TokenExpiredException && user == null && password == null)
					{
						Cookie u = getCookie(request, "user");
						if (u != null)
						{
							user = u.getValue();
						}
						Cookie p = getCookie(request, "password");
						if (p != null)
						{
							try
							{
								password = SecuritySupport.decrypt(Settings.getInstance(), p.getValue());
							}
							catch (Exception e)
							{
								throw new ServletException(e.getMessage());
							}
						}
						if (user != null && password != null)
						{
//							needToLogin = !checkUser(request, response, solutionName, user, password);
//							if (!needToLogin)
//							{
//								//TODO redirect to index with new token
//								return false;
//							}
						}
					}
				}
			}
			return needToLogin;
		}
		return false;
	}

	private static Cookie getCookie(HttpServletRequest request, String name)
	{
		return Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(name)).findAny().orElse(null);
	}

	private static boolean checkUser(ServletRequest servletRequest, ServletResponse servletResponse, String solutionName, String user, String password)
		throws IOException
	{
		String url = "/solution/" + solutionName + "/index.html";
		String uid;
		uid = ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(user, password);
		if (uid != null)
		{
			String token = null;
			try
			{
				String clientid = ApplicationServerRegistry.get().getClientId();
				String[] groups = ApplicationServerRegistry.get().getUserManager().getUserGroups(clientid, uid);
				Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
				if (settings.getProperty(JWT_Password) == null)
				{
					settings.put(JWT_Password, "pwd" + Math.random());
				}
				token = createToken(servletRequest, servletResponse, user, password, uid, groups, settings);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}

			if (token != null)
			{
				url += "?id_token=" + token;
				((HttpServletResponse)servletResponse).sendRedirect(url);
				return true;
			}
		}
		return false;
	}


	public static String createToken(ServletRequest servletRequest, ServletResponse servletResponse, String user, String password, String uid, String[] groups,
		Properties settings) throws Exception
	{
		String token;
		Algorithm algorithm = Algorithm.HMAC256(settings.getProperty(JWT_Password));
		token = JWT.create()
			.withIssuer("svy")
			.withClaim("uid", uid)
			.withClaim("user", user)
			.withArrayClaim("groups", groups)
			.withExpiresAt(new Date(System.currentTimeMillis() + 60000))
			.sign(algorithm);
		Cookie id_token = new Cookie("id_token", token);
		id_token.setMaxAge(60);
		id_token.setDomain(servletRequest.getServerName());
		HttpServletResponse response = (HttpServletResponse)servletResponse;
		response.addCookie(id_token);
		if ("on".equals(servletRequest.getParameter("remember")))
		{
			Cookie _user = new Cookie("user", user);
			_user.setMaxAge(3600);
			_user.setDomain(servletRequest.getServerName());
			response.addCookie(_user);
			Cookie _password = new Cookie("password", SecuritySupport.encrypt(Settings.getInstance(), password));
			_password.setMaxAge(3600);
			_password.setDomain(servletRequest.getServerName());
			response.addCookie(_password);
		}
		else
		{
			Cookie _user = new Cookie("user", "");
			_user.setMaxAge(0);
			_user.setDomain(servletRequest.getServerName());
			response.addCookie(_user);
			Cookie _password = new Cookie("password", "");
			_password.setMaxAge(0);
			_password.setDomain(servletRequest.getServerName());
			response.addCookie(_password);
		}
		return token;
	}

	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws IOException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		String loginHtml = null;
		try (InputStream rs = AngularIndexPageWriter.class.getResourceAsStream("login.html"))
		{
			if (rs != null)
			{
				loginHtml = IOUtils.toString(rs, Charset.forName("UTF-8"));
			}
		}
		final String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(path);
		sb.append("\">");
		sb.append("\n  <title>Login</title>");
		loginHtml = loginHtml.replace("<base href=\"/\">", sb.toString());

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			loginHtml = loginHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		loginHtml = loginHtml.replace("solutionName", solutionName);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
		return;
	}
}