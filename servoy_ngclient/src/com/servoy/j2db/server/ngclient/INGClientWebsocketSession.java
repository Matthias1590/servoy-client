/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import java.util.Map;

import org.sablo.IChangeListener;
import org.sablo.websocket.IWebsocketSession;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;

/**
 * Interface for classes handling a websocket session based on a client.
 * @author rgansevles
 */
public interface INGClientWebsocketSession extends IWebsocketSession, IChangeListener
{
	INGApplication getClient();

	void startHandlingEvent();

	void stopHandlingEvent();

	void closeSession();

	/**
	 * @param form
	 * @param name
	 */
	void updateForm(Form form, String name);

	/**
	 * @param flattenedForm
	 * @param realInstanceName
	 */
	void touchForm(Form flattenedForm, String realInstanceName, boolean async);

	void solutionLoaded(Solution flattenedSolution);

	/**
	 * @param formData
	 */
	void sendChanges(Map<String, Map<String, Map<String, Object>>> formData) throws IOException;

	/**
	 * @param formName
	 */
	void formCreated(String formName);
}
