/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.util.HTTPUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.servoy.base.util.I18NProvider;
import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
@SuppressWarnings("nls")
public class StatelessLoginHandler
{
	private static final String SVYLOGIN_PATH = "svylogin";
	public static final String PASSWORD = "password";
	public static final String ID_TOKEN = "id_token";
	public static final String PERMISSIONS = "permissions";
	public static final String USERNAME = "username";
	public static final String REMEMBER = "remember";
	public static final String UID = "uid";
	public static final String LAST_LOGIN = "last_login";
	private static final String JWT_Password = "servoy.jwt.logintoken.password";
	private static final int TOKEN_AGE_IN_SECONDS = 2 * 3600;

	private static final String BASE_CLOUD_URL = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu");
	private static final String CLOUD_REST_API_GET = BASE_CLOUD_URL + "/servoy-service/rest_ws/api/auth_endpoint/getEndpointUI/";
	private static final String CLOUD_REST_API_POST = BASE_CLOUD_URL + "/servoy-service/rest_ws/api/auth_endpoint/submitForm/";
	public static final String CLOUD_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/validateAuthUser";
	public static final String REFRESH_TOKEN_CLOUD_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/refreshPermissions";

	@SuppressWarnings({ "boxing" })
	public static Pair<Boolean, String> mustAuthenticate(HttpServletRequest request, HttpServletResponse reponse, String solutionName)
		throws ServletException
	{
		Pair<Boolean, String> needToLogin = new Pair<>(Boolean.FALSE, null);
		String requestURI = request.getRequestURI();
		if (requestURI.contains("/designer")) return needToLogin;

		if (solutionName != null && (requestURI.endsWith("/") ||
			requestURI.endsWith("/" + solutionName) || requestURI.toLowerCase().endsWith("/index.html")))
		{
			Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, request, reponse);
			FlattenedSolution fs = _fs.getLeft();
			if (fs == null) return needToLogin;
			try
			{
				AUTHENTICATOR_TYPE authenticator = fs.getSolution().getAuthenticator();
				needToLogin.setLeft(authenticator != AUTHENTICATOR_TYPE.NONE && fs.getSolution().getLoginFormID() <= 0 &&
					fs.getSolution().getLoginSolutionName() == null);
				if (needToLogin.getLeft())
				{
					String user = request.getParameter(USERNAME);
					String password = request.getParameter(PASSWORD);
					if (!Utils.stringIsEmpty(user) && !Utils.stringIsEmpty(password))
					{
						checkUser(user, password, needToLogin, fs.getSolution(), null, "on".equals(request.getParameter(REMEMBER)));
						if (!needToLogin.getLeft()) return needToLogin;
					}

					String id_token = request.getParameter(ID_TOKEN) != null ? request.getParameter(ID_TOKEN)
						: (String)request.getSession().getAttribute(ID_TOKEN);
					if (!Utils.stringIsEmpty(id_token))
					{
						Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
						JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(settings.getProperty(JWT_Password)))
							.build();
						try
						{
							jwtVerifier.verify(id_token);
							needToLogin.setLeft(Boolean.FALSE);
							needToLogin.setRight(id_token);
						}
						catch (JWTVerificationException ex)
						{
							if (ex instanceof TokenExpiredException)
							{
								DecodedJWT decodedJWT = JWT.decode(id_token);
								if (decodedJWT.getClaims().containsKey(USERNAME) && decodedJWT.getClaims().containsKey(UID) &&
									decodedJWT.getClaims().containsKey(PERMISSIONS))
								{
									String _user = decodedJWT.getClaim(USERNAME).asString();
									Boolean rememberUser = decodedJWT.getClaims().containsKey(REMEMBER) ? //
										decodedJWT.getClaim(REMEMBER).asBoolean() : Boolean.FALSE;
									try
									{
										checkUser(_user, null, needToLogin, fs.getSolution(), decodedJWT, rememberUser);
									}
									catch (Exception e)
									{
										throw new ServletException(e.getMessage());
									}
								}
							}
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				throw new ServletException(e);
			}
		}
		return needToLogin;
	}

	public static boolean handlePossibleCloudRequest(HttpServletRequest request, HttpServletResponse response, String solutionName) throws ServletException
	{
		Path path = Paths.get(request.getRequestURI()).normalize();
		if (solutionName != null && path.getNameCount() > 2 && SVYLOGIN_PATH.equals(path.getName(2).toString()))
		{
			Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, request, response);
			FlattenedSolution fs = _fs.getLeft();
			try
			{
				if (fs.getSolution().getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
				{
					try (CloseableHttpClient httpclient = HttpClients.createDefault())
					{
						Solution solution = fs.getSolution();
						Pair<Integer, JSONObject> res = null;
						String[] endpoints = getCloudRestApiEndpoints(request.getServletContext(), httpclient, solution);
						if (endpoints != null)
						{
							String endpoint = path.getName(path.getNameCount() - 1).toString().replace(".html", "");
							if (Arrays.asList(endpoints).contains(endpoint))
							{
								if ("POST".equalsIgnoreCase(request.getMethod()))
								{
									res = executeCloudPostRequest(httpclient, solution, endpoint, request);
								}
								else
								{
									res = executeCloudGetRequest(httpclient, solution, endpoint, request);
								}

								if (res != null)
								{
									writeResponse(request, response, solution, res);
									return true;
								}
							}
						}
					}
					catch (IOException e)
					{
						Debug.error("Can't access the Servoy Cloud api", e);
					}
				}
			}
			catch (Exception e)
			{
				throw new ServletException(e.getMessage());
			}
		}
		return false;

	}

	private static void writeResponse(HttpServletRequest request, HttpServletResponse response, Solution solution, Pair<Integer, JSONObject> res)
		throws IOException, UnsupportedEncodingException
	{
		String html = null;
		int status = res.getLeft().intValue();
		JSONObject json = res.getRight();
		if (json != null)
		{
			if (status == HttpStatus.SC_OK && json.has("html"))
			{
				html = json.getString("html");
			}
			else if (json.has("error"))
			{
				String error = json.optString("error", "");
				if (error.startsWith("<html>"))
				{
					html = error;
				}
				else
				{
					try (InputStream rs = StatelessLoginHandler.class.getResourceAsStream("error.html"))
					{
						html = IOUtils.toString(rs, Charset.forName("UTF-8"));
					}
					if (solution != null)
					{
						Solution sol = solution;
						I18NTagResolver i18nProvider = new I18NTagResolver(request.getLocale(), sol);
						html = TagParser.processTags(html, new ITagResolver()
						{
							@Override
							public String getStringValue(String name)
							{
								if ("solutionTitle".equals(name))
								{
									String titleText = sol.getTitleText();
									if (titleText == null) titleText = sol.getName();
									return i18nProvider.getI18NMessageIfPrefixed(titleText);
								}
								if ("error".equals(name))
								{
									return i18nProvider.getI18NMessageIfPrefixed(json.getString("error"));
								}
								return name;
							}
						}, null);
					}
				}
			}

			if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
			StringBuilder sb = new StringBuilder();
			sb.append("<base href=\"");
			sb.append(getPath(request));
			sb.append("\">");
			html = html.replace("<base href=\"/\">", sb.toString());

			String requestLanguage = request.getHeader("accept-language");
			if (requestLanguage != null)
			{
				html = html.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
			}

			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			response.setContentLengthLong(html.length());
			response.getWriter().write(html);
		}
	}

	private static Pair<Integer, JSONObject> executeCloudPostRequest(CloseableHttpClient httpclient, Solution solution, String endpoint,
		HttpServletRequest request)
	{
		HttpPost httppost = new HttpPost(CLOUD_REST_API_POST + endpoint);
		httppost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httppost.addHeader("uuid", solution.getUUID().toString());
		List<NameValuePair> postParameters = new ArrayList<>();
		Map<String, String[]> parameters = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : parameters.entrySet())
		{
			String[] values = entry.getValue();
			for (String value : values)
			{
				postParameters.add(new BasicNameValuePair(entry.getKey(), value));
			}
		}
		postParameters.add(new BasicNameValuePair("serverUrl", getServerURL(request))); //TODO param or header?
		httppost.setEntity(new UrlEncodedFormEntity(postParameters));

		try
		{
			return httpclient.execute(httppost, new ResponseHandler(endpoint));
		}
		catch (IOException e)
		{
			Debug.error("Can't get the rest api endpoints", e);
		}
		return null;
	}

	private static String getServerURL(HttpServletRequest req)
	{
		String scheme = req.getScheme();
		String serverName = req.getServerName();
		int serverPort = req.getServerPort();
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if (serverPort != 80 && serverPort != 443)
		{
			url.append(":").append(serverPort);
		}
		url.append(getPath(req));
		return url.toString();
	}

	private static String[] getCloudRestApiEndpoints(ServletContext servletContext, CloseableHttpClient httpclient, Solution solution)
	{
		String[] endpoints = (String[])servletContext.getAttribute("endpoints");
		if (endpoints != null)
		{
			long expire = Utils.getAsLong(servletContext.getAttribute("endpoints_expire"));
			if (expire < System.currentTimeMillis())
			{
				endpoints = null;
			}
		}
		if (endpoints == null)
		{
			HttpGet httpget = new HttpGet(CLOUD_REST_API_GET);
			httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
			httpget.addHeader("uuid", solution.getUUID().toString());
			try
			{
				endpoints = getArrayProperty(httpclient, httpget, "endpoints",
					"Error when getting the endpoints from the servoycloud: ");
				if (endpoints != null)
				{
					servletContext.setAttribute("endpoints", endpoints);
					servletContext.setAttribute("endpoints_expire", Long.valueOf(System.currentTimeMillis() + 10 * 60 * 1000));
				}
			}
			catch (IOException e)
			{
				Debug.error("Can't get the rest api endpoints", e);
				servletContext.setAttribute("endpoints", null);
			}
		}

		return endpoints;
	}

	private static Pair<Integer, JSONObject> executeCloudGetRequest(CloseableHttpClient httpclient, Solution solution, String endpoint,
		HttpServletRequest request)
	{
		try
		{
			URIBuilder uriBuilder = new URIBuilder(CLOUD_REST_API_GET + endpoint);
			if (request != null)
			{
				Map<String, String[]> parameters = request.getParameterMap();
				for (Map.Entry<String, String[]> entry : parameters.entrySet())
				{
					String[] values = entry.getValue();
					for (String value : values)
					{
						uriBuilder.setParameter(entry.getKey(), value);
					}
				}
			}
			HttpGet httpget = new HttpGet(uriBuilder.build());
			httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
			httpget.addHeader("uuid", solution.getUUID().toString());

			return httpclient.execute(httpget, new ResponseHandler(endpoint));
		}
		catch (Exception e)
		{
			Debug.error("Can't execute cloud get request", e);
		}
		return null;
	}

	private static void checkUser(String username, String password, Pair<Boolean, String> needToLogin, Solution solution, DecodedJWT oldToken,
		Boolean rememberUser)
	{
		boolean verified = false;
		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			verified = checkCloudPermissions(username, password, needToLogin, solution, oldToken, rememberUser);
		}
		else if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.AUTHENTICATOR)
		{
			verified = checkAuthenticatorPermssions(username, password, needToLogin, solution, oldToken, rememberUser);
		}
		else
		{
			verified = checkDefaultLoginPermissions(username, password, needToLogin, oldToken, rememberUser);
		}
		if (!verified)
		{
			needToLogin.setLeft(Boolean.TRUE);
			needToLogin.setRight(null);
		}
	}

	private static boolean checkDefaultLoginPermissions(String username, String password, Pair<Boolean, String> needToLogin, DecodedJWT oldToken,
		Boolean rememberUser)
	{
		try
		{
			String clientid = ApplicationServerRegistry.get().getClientId();
			if (oldToken != null)
			{
				long passwordLastChagedTime = ApplicationServerRegistry.get().getUserManager().getPasswordLastSet(clientid,
					oldToken.getClaim(UID).asString());
				if (passwordLastChagedTime > oldToken.getClaim(LAST_LOGIN).asLong().longValue())
				{
					needToLogin.setLeft(Boolean.TRUE);
					needToLogin.setRight(null);
					return false;
				}
			}

			String uid = oldToken != null ? oldToken.getClaim(UID).asString()
				: ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(username, password);
			if (uid != null)
			{

				String[] permissions = ApplicationServerRegistry.get().getUserManager().getUserGroups(clientid, uid);
				if (permissions.length > 0 && (oldToken == null || Arrays.equals(oldToken.getClaim(PERMISSIONS).asArray(String.class), permissions)))
				{
					String token = createToken(username, uid, permissions, Long.valueOf(System.currentTimeMillis()), rememberUser);
					needToLogin.setLeft(Boolean.FALSE);
					needToLogin.setRight(token);
					return true;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	private static boolean checkAuthenticatorPermssions(String username, String password, Pair<Boolean, String> needToLogin, Solution solution,
		DecodedJWT oldToken, Boolean rememberUser)
	{
		String modulesNames = solution.getModulesNames();
		IRepository localRepository = ApplicationServerRegistry.get().getLocalRepository();
		Solution authenticator = null;
		for (String moduleName : Utils.getTokenElements(modulesNames, ",", true))
		{
			try
			{
				Solution module = (Solution)localRepository.getActiveRootObject(moduleName, IRepository.SOLUTIONS);
				if (module.getSolutionType() == SolutionMetaData.AUTHENTICATOR)
				{
					authenticator = module;
					break;
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		if (authenticator != null)
		{
			JSONObject json = new JSONObject();
			json.put(USERNAME, username);
			json.put(PASSWORD, password);
			if (oldToken != null)
			{
				String payload = new String(java.util.Base64.getUrlDecoder().decode(oldToken.getPayload()));
				JSONObject token = new JSONObject(payload);
				json.put(LAST_LOGIN, token);
			}

			Credentials credentials = new Credentials(null, authenticator.getName(), null, json.toString());

			IApplicationServer applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
			try
			{
				ClientLogin login = applicationServer.login(credentials);
				if (login != null)
				{
					String token = createToken(login.getUserName(), login.getUserUid(), login.getUserGroups(), Long.valueOf(System.currentTimeMillis()),
						rememberUser);
					needToLogin.setLeft(Boolean.FALSE);
					needToLogin.setRight(token);
					return true;
				}
			}
			catch (RemoteException | RepositoryException e)
			{
				Debug.error(e);
			}
		}
		else
		{
			Debug.error("Trying to login in solution " + solution.getName() +
				" with using an AUTHENCATOR solution, but the main solution doesn't have that as a module");
		}
		return false;
	}

	private static boolean checkCloudPermissions(String username, String password, Pair<Boolean, String> needToLogin, Solution solution, DecodedJWT oldToken,
		Boolean rememberUser)
	{
		HttpGet httpget = new HttpGet(oldToken != null ? REFRESH_TOKEN_CLOUD_URL : CLOUD_URL);

		if (oldToken == null)
		{
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
			String authHeader = "Basic " + new String(encodedAuth);
			httpget.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
		}
		else
		{
			httpget.addHeader(USERNAME, oldToken.getClaim(USERNAME).asString());
			httpget.addHeader(LAST_LOGIN, oldToken.getClaim(LAST_LOGIN).asString());
		}
		httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpget.addHeader("uuid", solution.getUUID().toString());

		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			Pair<Integer, JSONObject> res = httpclient.execute(httpget, new ResponseHandler("login_auth"));
			if (res.getLeft().intValue() == HttpStatus.SC_OK)
			{
				JSONObject loginTokenJSON = res.getRight();
				if (loginTokenJSON != null && loginTokenJSON.has("permissions"))
				{
					String[] permissions = null;
					JSONArray permissionsArray = loginTokenJSON.getJSONArray("permissions");
					if (permissionsArray != null)
					{
						permissions = new String[permissionsArray.length()];
						for (int i = 0; i < permissions.length; i++)
						{
							permissions[i] = permissionsArray.getString(i);
						}
					}
					if (permissions != null)
					{
						String token = createToken(username, username, permissions, loginTokenJSON.optString("lastLogin"), rememberUser);
						needToLogin.setLeft(Boolean.FALSE);
						needToLogin.setRight(token);
						return true;
					}
				}
			}
		}
		catch (IOException e)
		{
			Debug.error("Can't validate user with the Servoy Cloud", e);
		}
		return false;
	}

	private static String[] getArrayProperty(CloseableHttpClient httpclient, HttpGet httpget, String property, String error) throws IOException
	{
		String[] permissions = httpclient.execute(httpget, new HttpClientResponseHandler<String[]>()
		{

			@Override
			public String[] handleResponse(ClassicHttpResponse response) throws HttpException, IOException
			{
				HttpEntity responseEntity = response.getEntity();
				String responseString = EntityUtils.toString(responseEntity);
				if (response.getCode() == HttpStatus.SC_OK)
				{
					JSONObject loginTokenJSON = new JSONObject(responseString);
					JSONArray permissionsArray = loginTokenJSON.getJSONArray(property);
					if (permissionsArray != null)
					{
						String[] prmsns = new String[permissionsArray.length()];
						for (int i = 0; i < prmsns.length; i++)
						{
							prmsns[i] = permissionsArray.getString(i);
						}
						return prmsns;
					}
					return null;
				}
				else
				{
					Debug.error(error + response.getCode() + " " +
						response.getReasonPhrase());
					return null;
				}
			}
		});
		return permissions;
	}


	public static String createToken(String username, String uid, String[] groups, Object lastLogin, Boolean rememberUser)
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		Algorithm algorithm = Algorithm.HMAC256(settings.getProperty(JWT_Password));
		Builder builder = JWT.create()
			.withIssuer("svy")
			.withClaim(UID, uid)
			.withClaim(USERNAME, username)
			.withArrayClaim(PERMISSIONS, groups)
			.withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_AGE_IN_SECONDS * 1000));
		if (lastLogin instanceof String)
		{
			builder = builder.withClaim(LAST_LOGIN, (String)lastLogin);
		}
		if (lastLogin instanceof Long)
		{
			builder = builder.withClaim(LAST_LOGIN, (Long)lastLogin);
		}
		if (Boolean.TRUE.equals(rememberUser))
		{
			builder = builder.withClaim(REMEMBER, rememberUser);
		}
		return builder.sign(algorithm);
	}

	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws IOException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		HTTPUtils.setNoCacheHeaders(response);
		Solution solution = null;
		try
		{
			solution = (Solution)ApplicationServerRegistry.get().getLocalRepository().getActiveRootObject(solutionName, IRepository.SOLUTIONS);
		}
		catch (RepositoryException e)
		{
			Debug.error("Can't load solution " + solutionName, e);
		}
		String loginHtml = null;
		if (solution != null && solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			try (CloseableHttpClient httpClient = HttpClients.createDefault())
			{
				Pair<Integer, JSONObject> result = executeCloudGetRequest(httpClient, solution, "login", null);
				if (result != null)
				{
					int status = result.getLeft().intValue();
					JSONObject res = result.getRight();
					if (status == HttpStatus.SC_OK && res != null)
					{
						loginHtml = res.optString("html", null);
					}
				}
			}
		}
		if (solution != null && loginHtml == null)
		{
			Media media = solution.getMedia("login.html");
			if (media != null) loginHtml = new String(media.getMediaData(), Charset.forName("UTF-8"));
		}
		if (loginHtml == null)
		{
			try (InputStream rs = StatelessLoginHandler.class.getResourceAsStream("login.html"))
			{
				loginHtml = IOUtils.toString(rs, Charset.forName("UTF-8"));
			}
		}
		if (solution != null)
		{
			Solution sol = solution;
			I18NTagResolver i18nProvider = new I18NTagResolver(request.getLocale(), sol);
			loginHtml = TagParser.processTags(loginHtml, new ITagResolver()
			{
				@Override
				public String getStringValue(String name)
				{
					if ("solutionTitle".equals(name))
					{
						String titleText = sol.getTitleText();
						if (titleText == null) titleText = sol.getName();
						return i18nProvider.getI18NMessageIfPrefixed(titleText);
					}
					return name;
				}
			}, i18nProvider);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(getPath(request));
		sb.append("\">");
		if (request.getParameter(ID_TOKEN) == null && request.getParameter(USERNAME) == null)
		{
			//we check the local storage for the token or username only once (if both are null)
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     if (window.localStorage.getItem('servoy_id_token')) { ");
			sb.append("\n    	document.body.style.display = 'none'; ");
			sb.append("\n  	    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_id_token'));  ");
			sb.append("\n    	document.login_form.remember.checked = true;  ");
			sb.append("\n    	document.login_form.submit(); ");
			sb.append("\n     } ");
			sb.append("\n     if (window.localStorage.getItem('servoy_username')) { ");
			sb.append("\n  	    document.login_form.username.value = JSON.parse(window.localStorage.getItem('servoy_username'));  ");
			sb.append("\n     } ");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");

		}
		else if (!Utils.stringIsEmpty(request.getParameter(ID_TOKEN) != null ? request.getParameter(ID_TOKEN)
			: (String)request.getSession().getAttribute(ID_TOKEN)))
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     window.localStorage.removeItem('servoy_id_token');");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (request.getParameter(USERNAME) != null)
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n  	    document.login_form.username.value = '");
			sb.append(StringEscapeUtils.escapeEcmaScript(request.getParameter(USERNAME)));
			sb.append("'");
			sb.append("\n  	    if (document.getElementById('errorlabel')) document.getElementById('errorlabel').style.display='block';");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}

		loginHtml = loginHtml.replace("<base href=\"/\">", sb.toString());

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			loginHtml = loginHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
		return;
	}

	private static String getPath(HttpServletRequest request)
	{
		String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
		Path p = Paths.get(request.getServletPath()).normalize();
		int i = 0;
		while (i < p.getNameCount() - 1 && !SVYLOGIN_PATH.equals(p.getName(i).toString()))
		{
			path += p.getName(i) + "/";
			i++;
		}
		return path;
	}

	/**
	 *
	 */
	public static void init()
	{
		Settings settings = Settings.getInstance();
		if (settings.getProperty(JWT_Password) == null)
		{
			Debug.warn("A servoy property '" + JWT_Password + //$NON-NLS-1$
				"' is added the the servoy properties file, this needs to be the same over redeploys, so make sure to add this in the servoy.properties that is used to deploy the WAR"); //$NON-NLS-1$
			settings.put(JWT_Password, "pwd" + Math.random());
			try
			{
				settings.save();
			}
			catch (Exception e)
			{
				Debug.error("Error saving the settings class to store the JWT_Password", e); //$NON-NLS-1$
			}
		}
	}


	/**
	 * @author emera
	 */
	public static class ResponseHandler implements HttpClientResponseHandler<Pair<Integer, JSONObject>>
	{
		private final String endpoint;

		public ResponseHandler(String endpoint)
		{
			this.endpoint = endpoint;
		}

		@Override
		public Pair<Integer, JSONObject> handleResponse(ClassicHttpResponse response) throws HttpException, IOException
		{
			HttpEntity responseEntity = response.getEntity();
			if (responseEntity != null)
			{
				Pair<Integer, JSONObject> pair = new Pair<>(Integer.valueOf(response.getCode()), null);
				String responseString = EntityUtils.toString(responseEntity);
				if (responseString.startsWith("{"))
				{
					pair.setRight(new JSONObject(responseString));
				}
				return pair;
			}
			else
			{
				Debug.error("Could not access rest api endpoint " + endpoint + " " + response.getCode() + " " +
					response.getReasonPhrase());
			}
			return null;
		}
	}


	/**
	 * @author jcompagner
	 *
	 */
	private static final class I18NTagResolver implements I18NProvider
	{
		private final Locale locale;
		private final Solution sol;

		/**
		 * @param request
		 * @param sol
		 */
		private I18NTagResolver(Locale locale, Solution sol)
		{
			this.locale = locale;
			this.sol = sol;
		}

		@Override
		public String getI18NMessage(String i18nKey)
		{
			return AngularIndexPageWriter.getSolutionDefaultMessage(sol, locale, i18nKey);
		}

		@Override
		public String getI18NMessage(String i18nKey, String language, String country)
		{
			return getI18NMessage(i18nKey);
		}

		@Override
		public String getI18NMessage(String i18nKey, Object[] array)
		{
			return getI18NMessage(i18nKey);
		}

		@Override
		public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
		{
			return getI18NMessage(i18nKey);
		}

		@Override
		public String getI18NMessageIfPrefixed(String key)
		{
			if (key != null && key.startsWith("i18n:")) //$NON-NLS-1$
			{
				return getI18NMessage(key.substring(5), null);
			}
			return key;
		}

		@Override
		public void setI18NMessage(String i18nKey, String value)
		{
		}
	}

}