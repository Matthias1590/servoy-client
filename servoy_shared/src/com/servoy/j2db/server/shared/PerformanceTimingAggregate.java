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

package com.servoy.j2db.server.shared;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.servoy.j2db.dataprocessing.IDataServer;

/**
 * Timing of actions like queries in the server.
 *
 * @author jblok
 */
public class PerformanceTimingAggregate extends PerformanceAggregator
{
	private final String action;
	private final int type;
	private final AtomicLong min_ms = new AtomicLong();
	private final AtomicLong max_ms = new AtomicLong();
	private final AtomicLong s2 = new AtomicLong(); // used for running calculation of standard deviation
	private final AtomicLong xtotal_ms = new AtomicLong();
	private final AtomicLong total_interval_ms = new AtomicLong();
	private final AtomicInteger count = new AtomicInteger();

	private final PerformanceTimingAggregate totalSubActionTimes;

	public PerformanceTimingAggregate(String action, int type, int maxEntriesToKeep)
	{
		super(maxEntriesToKeep);
		this.action = action;
		this.type = type;
		totalSubActionTimes = new PerformanceTimingAggregate(action + " - subactions", getSubActionMaxEntries()); //$NON-NLS-1$
		totalSubActionTimes.count.set(count.get() - 1); // if only some of the calls (not first ones) call client side APIs, we still must average on all calls
	}

	public PerformanceTimingAggregate(PerformanceTimingAggregate copy)
	{
		super(copy);

		this.action = copy.getAction();
		this.type = copy.getType();
		this.min_ms.set(copy.getMinTimeMS());
		this.max_ms.set(copy.getMaxTimeMS());
		this.s2.set(copy.getS2());
		this.count.set(copy.getCount());
		this.xtotal_ms.set(copy.getTotalTimeMS());
		this.total_interval_ms.set(copy.getTotalIntervalTimeMS());
		if (copy.totalSubActionTimes != null)
		{
			totalSubActionTimes = new PerformanceTimingAggregate(copy.totalSubActionTimes);
		}
		else
		{
			totalSubActionTimes = null;
		}
	}

	private PerformanceTimingAggregate(String action, int maxEntriesToKeep)
	{
		super(maxEntriesToKeep);
		this.action = action;
		this.type = IDataServer.METHOD_CALL;
		totalSubActionTimes = null;
	}

	public void updateTime(long interval_ms, long running_ms, int nrecords)
	{
		total_interval_ms.addAndGet(interval_ms);
		xtotal_ms.addAndGet(running_ms);
		min_ms.set(count.get() == 0 ? running_ms : Math.min(min_ms.get(), running_ms));
		max_ms.set(count.get() == 0 ? running_ms : Math.max(max_ms.get(), running_ms));
		s2.addAndGet((running_ms * running_ms));
		count.addAndGet(nrecords);
	}

	public void updateSubActionTimes(Map<String, PerformanceTimingAggregate> newSubActionTimings, int nrecords)
	{
		long it = 0, rt = 0;
		if (newSubActionTimings != null)
		{
			for (Entry<String, PerformanceTimingAggregate> newE : newSubActionTimings.entrySet())
			{
				PerformanceTimingAggregate newSubTime = newE.getValue();
				addTiming(newE.getKey(), newSubTime.getTotalIntervalTimeMS(), newSubTime.getTotalTimeMS(), newSubTime.getType(), newSubTime.toMap(), nrecords);
				if (newSubTime.getType() != IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT)
				{
					it += newSubTime.getTotalIntervalTimeMS();
					rt += newSubTime.getTotalTimeMS();
				}
			}
		}

		if (it != 0 || rt != 0)
		{
			totalSubActionTimes.updateTime(it, rt, nrecords); // it can happen that if in one parent method execution there are no child API calls, min will become 0 - that is normal
		}
	}

	public PerformanceTimingAggregate getTotalSubActionTimes()
	{
		return totalSubActionTimes;
	}

	public void updateTime(long totalIntervalMs, long running_ms, long minMs, long maxMs, long s, int cnt)
	{
		this.total_interval_ms.addAndGet(totalIntervalMs);
		this.xtotal_ms.addAndGet(running_ms);
		this.min_ms.set(Math.min(this.min_ms.get(), minMs));
		this.max_ms.set(Math.max(this.max_ms.get(), maxMs));
		this.s2.addAndGet(s);
		this.count.addAndGet(cnt);
	}

	public String getAction()
	{
		return action;
	}

	public int getType()
	{
		return type;
	}

	public String getTypeString()
	{
		return PerformanceTiming.getTypeString(type);
	}

	public long getAverageIntervalTimeMS()
	{
		if (count.get() == 0) return total_interval_ms.get();
		return (total_interval_ms.get() / count.get());
	}

	public long getAverageTimeMS()
	{
		if (count.get() == 0) return xtotal_ms.get();
		return (xtotal_ms.get() / count.get());
	}

	public long getTotalIntervalTimeMS()
	{
		return total_interval_ms.get();
	}

	public long getTotalTimeMS()
	{
		return xtotal_ms.get();
	}

	public long getMinTimeMS()
	{
		return min_ms.get();
	}

	public long getMaxTimeMS()
	{
		return max_ms.get();
	}

	public double getStandardDeviation()
	{
		long cnt = count.get();
		if (cnt <= 1) return 0;

		// see http://en.wikipedia.org/wiki/Standard_deviation for calculating standard deviation
		// see http://easycalculation.com/statistics/standard-deviation.php for calculating stdev

		// Population Standard deviation
//		return Math.sqrt((count * s2) - (total_ms * total_ms)) / count;

		// Standard deviation
		long xtotal = xtotal_ms.get();
		return Math.sqrt((((double)((cnt * s2.get()) - (xtotal * xtotal)))) / (cnt * (cnt - 1)));
	}

	public int getCount()
	{
		return count.intValue();
	}

	public long getS2()
	{
		return s2.get();
	}

}
