/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import java.awt.Dimension;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IWrapperType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType.MediaWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion2_FormElementValueToTemplateJSON;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class MediaPropertyType implements IWrapperType<Object, MediaWrapper>, ISupportsConversion2_FormElementValueToTemplateJSON<Object, Object>
{
	public static final MediaPropertyType INSTANCE = new MediaPropertyType();

	private MediaPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "media";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers) throws JSONException
	{
		// TODO currently we don't write anything here; we could make it implement ISupportsConversion1_FromDesignToFormElement, then it has access to solution we can use that to generate an url for many cases that don't need an application
		// JSONUtils.addKeyIfPresent(...);
		// writer.value(getMediaUrl(formElementValue.designValue, solution, null));

		return writer;
	}

	@Override
	public MediaWrapper fromJSON(Object newValue, MediaWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		return wrap(newValue, previousValue, dataConverterContext);
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, MediaWrapper object, DataConversion clientConversion) throws JSONException
	{
		if (object != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.value(object.mediaUrl);
		}
		return writer;
	}

	@Override
	public MediaWrapper defaultValue()
	{
		return null;
	}

	@Override
	public Object unwrap(MediaWrapper value)
	{
		return value != null ? value.mediaId : null;
	}

	@Override
	public MediaWrapper wrap(Object value, MediaWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		if (previousValue != null && Utils.equalObjects(value, previousValue.mediaUrl))
		{
			return previousValue;
		}
		IServoyDataConverterContext servoyDataConverterContext = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext();
		FlattenedSolution flattenedSolution = servoyDataConverterContext.getSolution();
		INGApplication application = servoyDataConverterContext.getApplication();

		String url = getMediaUrl(value, flattenedSolution, application);

		if (url != null) return new MediaWrapper(value, url);

		Debug.log("cannot convert media " + value + " using converter context " + servoyDataConverterContext);
		return null;
	}

	protected String getMediaUrl(Object value, FlattenedSolution flattenedSolution, INGApplication application)
	{
		String url = null;
		Media media = null;
		if (value instanceof Integer)
		{
			media = flattenedSolution.getMedia(((Integer)value).intValue());
		}
		else if (value instanceof String && ((String)value).toLowerCase().startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
		{
			media = flattenedSolution.getMedia(((String)value).substring(MediaURLStreamHandler.MEDIA_URL_DEF.length()));
		}
		if (media != null)
		{
			url = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" + media.getName();
			Dimension imageSize = ImageLoader.getSize(media.getMediaData());
			boolean paramsAdded = false;
			if (imageSize != null)
			{
				paramsAdded = true;
				url += "?imageWidth=" + imageSize.width + "&imageHeight=" + imageSize.height;
			}
			if (application != null)
			{
				Solution sc = flattenedSolution.getSolutionCopy(false);
				if (sc != null && sc.getMedia(media.getName()) != null)
				{
					if (paramsAdded) url += "&";
					else url += "?";
					url += "uuid=" + application.getWebsocketSession().getUuid() + "&lm:" + sc.getLastModifiedTime();
				}
			}
		}
		return url;
	}

	class MediaWrapper
	{
		Object mediaId;
		String mediaUrl;

		MediaWrapper(Object mediaId, String mediaUrl)
		{
			this.mediaId = mediaId;
			this.mediaUrl = mediaUrl;
		}
	}

}