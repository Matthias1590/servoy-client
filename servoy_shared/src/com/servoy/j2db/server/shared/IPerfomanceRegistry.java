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

package com.servoy.j2db.server.shared;

import java.util.Date;
import java.util.Map;


/**
 * @author gganea@servoy.com
 *
 */
public interface IPerfomanceRegistry
{

	public static final String MAX_ENTRIES_PER_CONTEXT_PROPERTY_NAME_PREFIX = "servoy.performanceStats.maxEntriesPerContext.";

	public static final int UNLIMITED_ENTRIES = -1;
	public static final int OFF = 0;

	PerformanceData getPerformanceData(String context);

	Date getLastCleared(String context);

	void clearPerformanceData(String context);

	Map<String, PerformanceTiming[]> getActiveTimings();

	String[] getPerformanceTimingContexts();

	PerformanceTimingAggregate[] getPerformanceTiming(String string);

	int getMaxNumberOfEntriesPerContext();

	void setMaxNumberOfEntriesPerContext(int maxNumberOfEntriesPerContext);

	String getId();

}
