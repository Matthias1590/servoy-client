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
package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSMedia implements IJavaScriptType
{

	private Media media;
	private boolean isCopy;

	private final FlattenedSolution fs;

	/**
	 * @param media
	 */
	public JSMedia(Media media, FlattenedSolution fs, boolean isCopy)
	{
		this.media = media;
		this.fs = fs;
		this.isCopy = isCopy;
	}

	private final void checkModification()
	{
		if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			media = fs.createPersistCopy(media);
			isCopy = true;
		}
	}

	/**
	 * @return the media
	 */
	Media getMedia()
	{
		return media;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Media#getMimeType()
	 * 
	 * @sampleas js_getBytes()
	 */
	public String js_getMimeType()
	{
		return media.getMimeType();
	}

	public void js_setMimeType(String type)
	{
		checkModification();
		media.setMimeType(type);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Media#getName()
	 * 
	 * @sampleas js_getBytes()
	 * 
	 * @return A String holding the name of this Media object.
	 */
	public String js_getName()
	{
		return media.getName();
	}

	/**
	 * A byte array holding the content of the Media object.
	 * 
	 * @sample
	 * var ballBytes = plugins.file.readFile('d:/ball.jpg');
	 * var mapBytes = plugins.file.readFile('d:/map.png');
	 * var ballImage = solutionModel.newMedia('ball.jpg', ballBytes);
	 * application.output('original image name: ' + ballImage.getName());
	 * ballImage.bytes = mapBytes;
	 * ballImage.mimeType = 'image/png';
	 * application.output('image name after change: ' + ballImage.getName()); // The name remains unchanged. Only the content (bytes) are changed.
	 * application.output('image mime type: ' + ballImage.mimeType);
	 * application.output('image size: ' + ballImage.bytes.length);
	 */
	public byte[] js_getBytes()
	{
		return media.getMediaData();
	}

	public void js_setBytes(byte[] bytes)
	{
		checkModification();
		media.setPermMediaData(bytes);

		if (bytes != null)
		{
			media.setMimeType(ImageLoader.getContentType(bytes));
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSMedia[name: " + media.getName() + ']';
	}

	/**
	 * Returns the UUID of this media
	 * 
	 * @sample
	 * var ballImg = plugins.file.readFile('d:/ball.jpg');
	 * application.output(ballImg.getUUID().toString());
	 */
	public UUID js_getUUID()
	{
		return media.getUUID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((media == null) ? 0 : media.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSMedia other = (JSMedia)obj;
		if (media == null)
		{
			if (other.media != null) return false;
		}
		else if (!media.getUUID().equals(other.media.getUUID())) return false;
		return true;
	}
}
