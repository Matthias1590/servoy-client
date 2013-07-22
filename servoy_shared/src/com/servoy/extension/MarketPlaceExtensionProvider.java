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

package com.servoy.extension;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import com.servoy.extension.parser.ParseDependencyMetadata;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * This class provides extension info & data taken from the Servoy Marketplace
 * @author gboros
 */
public class MarketPlaceExtensionProvider extends CachingExtensionProvider
{
	public static final String MARKETPLACE_HOST;

	static
	{
		MARKETPLACE_HOST = System.getProperty("market_place_host_url", "https://crm.servoy.com"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static final String MARKETPLACE_WS = MARKETPLACE_HOST + "/servoy-service/rest_ws/marketplace/ws_extensions/"; //$NON-NLS-1$
	private static final String WS_ACTION_VERSIONS = "versions"; //$NON-NLS-1$
	private static final String WS_ACTION_EXP = "exp"; //$NON-NLS-1$
	private static final String WS_ACTION_PACKAGE_XML = "xml"; //$NON-NLS-1$
	private static final String TO_BE_INSTALLED_FOLDER = ".ws"; //$NON-NLS-1$

	private final File destinationDir;

	private final HashMap<String, String[]> availableVersionsMap = new HashMap<String, String[]>();
	private final HashMap<Pair<String, String>, File> expFileMap = new HashMap<Pair<String, String>, File>();
	private final MessageKeeper messages = new MessageKeeper();

	public MarketPlaceExtensionProvider(File installDir)
	{
		destinationDir = new File(new File(installDir, ExtensionUtils.EXPFILES_FOLDER), TO_BE_INSTALLED_FOLDER);
	}

	public String[] getAvailableVersions(String extensionID)
	{
		if (!availableVersionsMap.containsKey(extensionID)) availableVersionsMap.put(extensionID, ws_getVersions(extensionID));
		return availableVersionsMap.get(extensionID);
	}

	public File getEXPFile(String extensionId, String version, IProgress progressMonitor)
	{
		Pair<String, String> extVersion = new Pair<String, String>(extensionId, version);
		if (!expFileMap.containsKey(extVersion))
		{
			expFileMap.put(extVersion, ws_getEXP(extensionId, version, progressMonitor));
			if (progressMonitor != null && progressMonitor.shouldCancelOperation())
			{
				expFileMap.remove(extVersion); // don't cache it then
			}
		}
		return expFileMap.get(extVersion);
	}

	@Override
	protected DependencyMetadata[] getDependencyMetadataImpl(ExtensionDependencyDeclaration extensionDependency)
	{
		ArrayList<DependencyMetadata> dmA = new ArrayList<DependencyMetadata>();
		for (String version : getAvailableVersions(extensionDependency.id))
		{
			if (VersionStringUtils.belongsToInterval(version, extensionDependency.minVersion, extensionDependency.maxVersion))
			{
				BufferedInputStream bis = null;
				try
				{
					URLConnection ws_connection = ws_getConnection(WS_ACTION_PACKAGE_XML, "application/binary", extensionDependency.id, version); //$NON-NLS-1$
					bis = new BufferedInputStream(ws_connection.getInputStream());
					ParseDependencyMetadata parseDM = new ParseDependencyMetadata(extensionDependency.id, messages);
					dmA.add(parseDM.runOnEntryInputStream(bis));
				}
				catch (Exception ex)
				{
					String msg = "Cannot get extension definition from marketplace. Error is : " + ex.getMessage(); //$NON-NLS-1$
					Debug.error(ex);
					messages.addError(msg);
				}
				finally
				{
					Utils.closeInputStream(bis);
				}
			}
		}

		return dmA.toArray(new DependencyMetadata[dmA.size()]);
	}

	public void dispose()
	{
		messages.clearMessages();
		availableVersionsMap.clear();
		flushCache();

		FileUtils.deleteQuietly(destinationDir);
		expFileMap.clear();
	}

	public Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clearMessages();
	}

	private String[] ws_getVersions(String extensionId)
	{
		ArrayList<String> versions = new ArrayList<String>();

		ByteArrayOutputStream bos = null;
		BufferedInputStream bis = null;
		try
		{
			URLConnection ws_connection = ws_getConnection(WS_ACTION_VERSIONS, "text/json", extensionId, null); //$NON-NLS-1$
			bos = new ByteArrayOutputStream();
			bis = new BufferedInputStream(ws_connection.getInputStream());
			byte[] buffer = new byte[1024];
			int len;
			while ((len = bis.read(buffer)) != -1)
				bos.write(buffer, 0, len);

			String encoding = getCharset(ws_connection.getContentType());
			if (encoding == null) encoding = "UTF-8"; //$NON-NLS-1$
			JSONArray jsonVersions = new JSONArray(new String(bos.toByteArray(), encoding));
			for (int i = 0; i < jsonVersions.length(); i++)
				versions.add(jsonVersions.getString(i));

		}
		catch (Exception ex)
		{
			String msg = "Cannot get extension versions from marketplace. Error is : " + ex.getMessage(); //$NON-NLS-1$
			Debug.error(ex);
			messages.addError(msg);
		}
		finally
		{
			Utils.closeInputStream(bis);
			Utils.closeOutputStream(bos);
		}

		return versions.toArray(new String[versions.size()]);
	}

	private File ws_getEXP(String extensionId, String version, IProgress progressMonitor)
	{
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		File outputFile = new File(destinationDir, extensionId + "_" + version + ".exp"); //$NON-NLS-1$ //$NON-NLS-2$
		try
		{
			destinationDir.mkdirs();
			URLConnection ws_connection = ws_getConnection(WS_ACTION_EXP, "application/binary", extensionId, version); //$NON-NLS-1$
			fos = new FileOutputStream(outputFile);
			bis = new BufferedInputStream(ws_connection.getInputStream());

			int total = ws_connection.getContentLength();
			if (progressMonitor != null)
			{
				progressMonitor.setStatusMessage("0 KB of " + getSizeString(total) + "..."); //$NON-NLS-1$//$NON-NLS-2$
				progressMonitor.start(total);
			}

			byte[] buffer = new byte[1091];
			int len;
			int downloaded = 0;
			long lastLabelUpdateTime = System.currentTimeMillis();
			while ((len = bis.read(buffer)) != -1)
			{
				fos.write(buffer, 0, len);
				if (progressMonitor != null)
				{
					downloaded += len;
					progressMonitor.worked(len);
					if (System.currentTimeMillis() - lastLabelUpdateTime > 500)
					{
						// do not update label faster then 0.5 sec - it looks strange
						progressMonitor.setStatusMessage(getSizeString(downloaded) + " of " + getSizeString(total) + "..."); //$NON-NLS-1$//$NON-NLS-2$
						lastLabelUpdateTime = System.currentTimeMillis();
					}
					if (progressMonitor.shouldCancelOperation()) break;
				}
			}
		}
		catch (Exception ex)
		{
			String msg = "Cannot get extension file from marketplace. Error is : " + ex.getMessage(); //$NON-NLS-1$
			Debug.error(ex);
			messages.addError(msg);
		}
		finally
		{
			Utils.closeInputStream(bis);
			Utils.closeOutputStream(fos);
		}

		if (progressMonitor != null && progressMonitor.shouldCancelOperation())
		{
			FileUtils.deleteQuietly(outputFile);
			outputFile = null;
		}

		return outputFile;
	}

	private String getCharset(String contentType)
	{
		String charset = null;
		if (contentType != null)
		{
			String[] split = contentType.split("; *"); //$NON-NLS-1$
			for (String element : split)
			{
				if (element.toLowerCase().startsWith("charset=")) //$NON-NLS-1$
				{
					charset = element.substring("charset=".length()); //$NON-NLS-1$
					if (charset.length() > 1 && charset.charAt(0) == '"' && charset.charAt(charset.length() - 1) == '"')
					{
						charset = charset.substring(1, charset.length() - 1);
					}
					return charset;
				}
			}
		}
		return charset;
	}

	private String getSizeString(int size)
	{
		String unit;
		double value;
		if (size > 1024 * 1024)
		{
			unit = " MB"; //$NON-NLS-1$
			value = ((double)size) / 1024 / 1024;
		}
		else
		{
			unit = " KB"; //$NON-NLS-1$
			value = ((double)size) / 1024;
		}

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);

		return bd.toPlainString() + unit;
	}

	private URLConnection ws_getConnection(String action, String acceptContentType, String extensionId, String version) throws Exception
	{
		String unescapedURL = MARKETPLACE_WS + action + "/" + extensionId + (version != null ? "/" + version : "");
		URL mpURL = new URI(null, null, unescapedURL, null).toURL(); // the URI should escape it correctly
		URLConnection urlConnection = mpURL.openConnection();

		urlConnection.addRequestProperty("accept", acceptContentType); //$NON-NLS-1$

		return urlConnection;
	}
}
