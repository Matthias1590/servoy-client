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
package com.servoy.j2db.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;


public class RoundHalfUpDecimalFormat extends DecimalFormat
{
	private static ConcurrentHashMap<Locale, DecimalFormatSymbols> decimalFormatSymbols = new ConcurrentHashMap<Locale, DecimalFormatSymbols>();

	@SuppressWarnings("nls")
	public static DecimalFormatSymbols getDecimalFormatSymbols(Locale locale)
	{
		DecimalFormatSymbols dfs = decimalFormatSymbols.get(locale);
		if (dfs == null)
		{
			Locale loc = locale;
			if (locale.getCountry().length() == 0)
			{
				// without a country the currency can't be get, make the locale with a currency
				if (locale.getLanguage().equals("en")) loc = new Locale(loc.getLanguage(), "US", loc.getDisplayVariant());
				else loc = new Locale(loc.getLanguage(), loc.getLanguage().toUpperCase(), loc.getDisplayVariant());
			}
			dfs = new DecimalFormatSymbols(loc);
			decimalFormatSymbols.put(locale, dfs);
		}
		return dfs;
	}

	private boolean minusAtBack = false;

	public RoundHalfUpDecimalFormat(Locale locale)
	{
		this("#,##0.###", locale); //$NON-NLS-1$
	}

	public RoundHalfUpDecimalFormat(String format, Locale locale)
	{
		super(checkPattern(format, locale), getDecimalFormatSymbols(locale));
		setGroupingUsed(true);
		format = checkPattern(format, locale);
		if (format.endsWith("-")) //$NON-NLS-1$
		{
			applyPattern(format.substring(0, format.length() - 1));
			minusAtBack = true;
		}
		setParseBigDecimal(true);
		setRoundingMode(RoundingMode.HALF_UP);
	}

	@Override
	public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
	{
		if (minusAtBack && number < 0 && !("%".equals(getPositiveSuffix()) || "%".equals(getPositivePrefix())))
		{
			StringBuffer sb = super.format(Math.abs(number), result, fieldPosition);
			sb.append('-');
			return sb;
		}
		double toParse = number;
		// add a digit after the max fraction digits of the format so that 0.0245 are half upped.
		if (!Double.isNaN(toParse) && !Double.isInfinite(toParse))
		{
			int max = getMaximumFractionDigits();
			BigDecimal db = BigDecimal.valueOf(number);
			if (db.scale() <= max + 1)
			{
				if (number < 0)
				{
					toParse -= 1d / Math.pow(10, max + 4);
				}
				else
				{
					toParse += 1d / Math.pow(10, max + 4);
				}
			}
		}
		return super.format(toParse, result, fieldPosition);
	}


	@Override
	public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition)
	{
		if (minusAtBack && number < 0)
		{
			StringBuffer sb = super.format(Math.abs(number), result, fieldPosition);
			sb.append('-');
			return sb;
		}
		else
		{
			return super.format(number, result, fieldPosition);
		}
	}

	/**
	 *
	 */

	@Override
	public Number parse(String source, ParsePosition pos)
	{
		if (minusAtBack && source.endsWith("-")) //$NON-NLS-1$
		{
			Number o = super.parse(source.substring(0, source.length() - 1), pos);
			if (o instanceof Double)
			{
				o = new Double(-((Double)o).doubleValue());
			}
			else if (o instanceof Long)
			{
				o = new Long(-((Long)o).longValue());
			}
			else if (o instanceof BigDecimal)
			{
				return ((BigDecimal)o).negate();
			}
			return o;
		}
		else if ("".equals(source)) //$NON-NLS-1$
		{
			return null;
		}
		else
		{
			return super.parse(source, pos);
		}
	}

	private static String checkPattern(String format, Locale locale)
	{
		try
		{
			new DecimalFormat(format, getDecimalFormatSymbols(locale));
			return format;
		}
		catch (Exception ex)
		{
			Debug.error("Invalid number pattern : '" + format + "', continue using default pattern ...", ex); //$NON-NLS-1$ //$NON-NLS-2$
			return new DecimalFormat().toPattern();
		}
	}
}