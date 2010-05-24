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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.UIManager;

/**
 * Helper class
 * 
 * @author jblok
 */
public class PersistHelper
{
	private PersistHelper()
	{
	}

	public static Object[] transVectorToArray(Vector vector)
	{
		Object[] fieldsToSave = new Object[vector.size()];
		vector.copyInto(fieldsToSave);
		return fieldsToSave;
	}

	public static Object[] transHashTableToArray(Hashtable table)
	{
		int x = 0;
		Object[] fieldsToSave = new Object[table.size()];
		Enumeration e = table.elements();
		while (e.hasMoreElements())
		{
			fieldsToSave[x] = e.nextElement();
			x++;
		}
		return fieldsToSave;
	}

	public static String createPageFormatString(PageFormat format)
	{
		if (format == null)
		{
			return null;
		}
		Paper paper = format.getPaper();
		StringBuffer sb = new StringBuffer();
		sb.append(format.getOrientation());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getWidth());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getHeight());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableX());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableY());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableWidth());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableHeight());
		sb.append(";"); //$NON-NLS-1$
		return sb.toString();
	}

	public static PageFormat createPageFormat(String value)
	{
		PageFormat format = null;
		if (value != null)
		{
			//orientation;width;height;ImageableX;ImageableY,ImageableWidth;ImageableHeight
			StringTokenizer tk = new StringTokenizer(value, ";"); //$NON-NLS-1$
			if (tk.hasMoreTokens())
			{
				int orientation = Utils.getAsInteger(tk.nextToken());
				double width = Utils.getAsDouble(tk.nextToken());
				double height = Utils.getAsDouble(tk.nextToken());
				double imageableX = Utils.getAsDouble(tk.nextToken());
				double imageableY = Utils.getAsDouble(tk.nextToken());
				double imageableWidth = Utils.getAsDouble(tk.nextToken());
				double imageableHeight = Utils.getAsDouble(tk.nextToken());

				format = new PageFormat();
				format.setOrientation(orientation);
				Paper paper = new Paper();
				paper.setSize(width, height);
				paper.setImageableArea(imageableX, imageableY, imageableWidth, imageableHeight);
				format.setPaper(paper);
			}
		}
		else
		{
			format = null;
		}
		return format;
	}

	public static String createDimensionString(Dimension d)
	{
		if (d == null) return null;
		StringBuffer retval = new StringBuffer();
		retval.append(d.width);
		retval.append(","); //$NON-NLS-1$
		retval.append(d.height);
		return retval.toString();
	}

	public static Dimension createDimension(String d)
	{
		if (d == null) return null;
		int w = 0;
		int h = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(d, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) w = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) h = Utils.getAsInteger(tk.nextToken());
		}
		catch (NumberFormatException ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Dimension(w, h);
	}

	public static String createPointString(Point d)
	{
		if (d == null) return null;
		StringBuffer retval = new StringBuffer();
		retval.append(d.x);
		retval.append(","); //$NON-NLS-1$
		retval.append(d.y);
		return retval.toString();
	}

	public static Point createPoint(String d)
	{
		if (d == null) return null;
		int x = 0;
		int y = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(d, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) x = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) y = Utils.getAsInteger(tk.nextToken());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Point(x, y);
	}

	public static String createLocaleString(Locale loc)
	{
		if (loc == null) return null;
		StringBuffer retval = new StringBuffer();
		retval.append(loc.getLanguage());
		retval.append(","); //$NON-NLS-1$
		retval.append(loc.getCountry());
		retval.append(","); //$NON-NLS-1$
		retval.append(loc.getVariant());
		return retval.toString();
	}

	public static Locale createLocale(String d)
	{
		if (d == null) return null;
		String lang = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$
		try
		{
			StringTokenizer tk = new StringTokenizer(d, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) lang = tk.nextToken();
			if (tk.hasMoreTokens()) country = tk.nextToken();
			if (tk.hasMoreTokens()) variant = tk.nextToken();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Locale(lang, country, variant);
	}

	public static String createInsetsString(Insets ins)
	{
		if (ins == null) return null;
		StringBuffer retval = new StringBuffer();
		retval.append(ins.top);
		retval.append(","); //$NON-NLS-1$
		retval.append(ins.left);
		retval.append(","); //$NON-NLS-1$
		retval.append(ins.bottom);
		retval.append(","); //$NON-NLS-1$
		retval.append(ins.right);
		return retval.toString();
	}

	public static Insets createInsets(String s)
	{
		if (s == null) return null;
		int top = 0;
		int left = 0;
		int bottom = 0;
		int right = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(s, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) top = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) left = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) bottom = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) right = Utils.getAsInteger(tk.nextToken());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Insets(top, left, bottom, right);
	}

	public static String createRectangleString(Rectangle rect)
	{
		if (rect == null) return null;
		StringBuffer retval = new StringBuffer();
		Point p = rect.getLocation();
		retval.append(createPointString(p));
		retval.append(","); //$NON-NLS-1$
		Dimension d = rect.getSize();
		retval.append(createDimensionString(d));
		return retval.toString();
	}

	public static Rectangle createRectangle(String a_rect)
	{
		if (a_rect == null) return null;
		int x = 0;
		int y = 0;
		int w = 0;
		int h = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(a_rect, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) x = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) y = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) w = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) h = Utils.getAsInteger(tk.nextToken());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Rectangle(x, y, w, h);
	}

	public static byte[] transStringToUTF8Bytes(String data) throws java.io.IOException
	{
		if (data == null) return null;
		byte[] retval = null;
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(baos, "UTF8"); //$NON-NLS-1$
		osw.write(data);
		osw.flush();
		baos.flush();
		retval = baos.toByteArray();
		osw.close();
		baos.close();
		return retval;
	}

	//TODO change this mechanism (readline leaves out the newlines)!
	public static String transUTF8BytesToString(byte[] data) throws java.io.IOException
	{
		if (data == null) return null;
		StringBuffer retval = new StringBuffer();
		java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
		java.io.InputStreamReader isr = new java.io.InputStreamReader(bais, "UTF8"); //$NON-NLS-1$
		java.io.BufferedReader br = new java.io.BufferedReader(isr);
		String line = br.readLine();
		while (line != null)
		{
			retval.append(line);
			retval.append("\n"); //$NON-NLS-1$
			line = br.readLine();
		}
		br.close();
		isr.close();
		bais.close();
		return retval.toString();
	}

	public static Color createColor(String s)
	{
		Color retval = null;
		if (s != null && s.length() == 7)
		{
			try
			{
				retval = Color.decode(s);
			}
			catch (NumberFormatException e)
			{
				//ignore;
			}
		}
		if (s != null && retval == null)
		{
			try
			{
				Field field = Color.class.getField(s);
				return (Color)field.get(null);
			}
			catch (Exception e)
			{
				// ignore
			}
		}
		return retval;
	}

	public static String createColorString(Color c)
	{
		String retval = null;
		if (c != null)
		{
			String r = Integer.toHexString(c.getRed());
			if (r.length() == 1) r = "0" + r; //$NON-NLS-1$
			String g = Integer.toHexString(c.getGreen());
			if (g.length() == 1) g = "0" + g; //$NON-NLS-1$
			String b = Integer.toHexString(c.getBlue());
			if (b.length() == 1) b = "0" + b; //$NON-NLS-1$
			retval = "#" + r + g + b; //$NON-NLS-1$
		}
		return retval;
	}

	private static void main(String[] args)
	{
		Font f = createFont("TimesNewRoamanPS-BoldItalicMT,0,12"); //$NON-NLS-1$
		System.out.println(f.getFamily());
		System.out.println(f.getName());
		System.out.println(f.getFontName());

		f = createFont("CourierNewPS-BoldItalicMT,0,12"); //$NON-NLS-1$
		System.out.println(f.getFamily());
		System.out.println(f.getName());
		System.out.println(f.getFontName());
	}

	private static Map allFonts = new HashMap();

	private synchronized static void initFonts() // it can be called by multiple concurrent threads in case of web-client startup; because HashMap is not synchronized it can end up (and it did) in a situation where iterators.next() and map.put() will do infinite loops => 100% CPU
	{
		if (allFonts.size() == 0)
		{
			Font[] allFontsArray = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
			for (Font element : allFontsArray)
			{
				allFonts.put(Utils.stringReplace(element.getName(), " ", "").toLowerCase(), element); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private static Map allCreatedFonts = new HashMap();

	public static Font createFont(String name, int style, int size)
	{
		if (name == null) return null;

		if (size <= 0) size = 1;


		Font retval = new Font(name, style, size);
		String specialName = Utils.stringReplace(name, " ", "").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
		initFonts();
		if (!allFonts.containsKey(specialName))
		{
			Font guess = guessFont(name);
			if (guess == null)
			{
				//fallback to LAFDefault font normally used on label, and we derive to right size and type
				guess = UIManager.getDefaults().getFont("Label.font"); //$NON-NLS-1$
			}
			if (guess != null)
			{
				retval = guess.deriveFont(style, size);//also make compatible with designer
			}
			else
			{
				retval = null;
			}
		}
		return retval;
	}

	public static Font createFont(String s)
	{
		if (s == null) return null;

		if (allCreatedFonts.containsKey(s))
		{
			return (Font)allCreatedFonts.get(s);//also nulls are returned
		}

		StringTokenizer tk = new StringTokenizer(s, ","); //$NON-NLS-1$
		if (tk.countTokens() >= 3)
		{
			String name = tk.nextToken();
			String style = tk.nextToken();
			String size = tk.nextToken();
//			String family = null;
//			if (tk.hasMoreTokens())
//			{
//				family = tk.nextToken();
//			}
			int istyle = Utils.getAsInteger(style);
			int isize = Utils.getAsInteger(size);
			Font retval = createFont(name, istyle, isize);
			allCreatedFonts.put(s, retval);
			return retval;
		}
		else
		{
			return null;
		}
	}

	private static Font guessFont(String aName)
	{
		if (aName == null) return null;
		if (aName.length() == 0) return null;
		String formatedName = stringFormat(aName);
		Font guessFont = null;
		int maxPieces = 0, minMissingPieces = 999;
		initFonts();
		Iterator it = allFonts.values().iterator();
		while (it.hasNext())
		{
			Font f = (Font)it.next();

			String fontName = f.getFontName();
			if (fontName.equalsIgnoreCase(aName)) return f;
			if (fontName.equalsIgnoreCase(formatedName)) return f;
			fontName = stringFormat(fontName);
			if (fontName.equalsIgnoreCase(aName)) return f;
			if (fontName.equalsIgnoreCase(formatedName)) return f;

			List pieceList = new ArrayList();
			StringTokenizer tk = new StringTokenizer(fontName, " "); //$NON-NLS-1$
			while (tk.hasMoreElements())
			{
				String element = (String)tk.nextElement();
				pieceList.add(element);
			}
			boolean substantial = false;
			int pieces = 0;
			int missingPieces = 0;
			for (int j = 0; j < pieceList.size(); j++)
			{
				String piece = (String)pieceList.get(j);
				if (piece.length() <= 2) continue; // do not match 'a' or 'in'
				if (aName.indexOf(piece) >= 0)
				{
					pieces++;
					if (!isStyle(piece)) substantial = true;
				}
				missingPieces++;
			}
			if (substantial && (pieces > maxPieces || (pieces == maxPieces && missingPieces < minMissingPieces)))
			{
				guessFont = f;
				maxPieces = pieces;
				minMissingPieces = missingPieces;
			}
		}
		return guessFont;
	}

	private static boolean isStyle(String value)
	{
		String[] styles = { "bold", //$NON-NLS-1$
		"italic", //$NON-NLS-1$
		"regular", //$NON-NLS-1$
		"medium", //$NON-NLS-1$
		"oblique", //$NON-NLS-1$
		"semi", //$NON-NLS-1$
		"narrow", //$NON-NLS-1$
		"black", //$NON-NLS-1$
		"light", //$NON-NLS-1$
		"extra", //$NON-NLS-1$
		"condensed", //$NON-NLS-1$
		"cond", //$NON-NLS-1$
		"ultra", //$NON-NLS-1$
		"demi", //$NON-NLS-1$
		"thin", //$NON-NLS-1$
		"wide", //$NON-NLS-1$
		"rounded" }; //$NON-NLS-1$
		for (String element : styles)
		{
			if (element.equalsIgnoreCase(value)) return true;
		}
		return false;
	}

	private static String stringFormat(String fn)
	{
		if (fn == null) return null;
		if (fn.length() == 0) return fn;

		fn = Utils.stringReplace(fn, "MT", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "MS", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "LT", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "PS", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "-", " "); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer stringResult = new StringBuffer();
		for (int i = 0; i < fn.length(); i++)
		{
			char ch1 = fn.charAt(i);
			stringResult.append(ch1);
			if (Character.getType(ch1) == Character.LOWERCASE_LETTER && i < (fn.length() - 1))
			{
				char ch2 = fn.charAt(i + 1);
				if (Character.getType(ch2) == Character.UPPERCASE_LETTER)
				{
					stringResult.append(" "); //$NON-NLS-1$
				}
			}
		}
		return stringResult.toString();
	}

	public static String createFontString(Font f)
	{
		if (f == null) return null;
		StringBuffer sb = new StringBuffer();
		sb.append(f.getName());
		sb.append(","); //$NON-NLS-1$
		sb.append(f.getStyle());
		sb.append(","); //$NON-NLS-1$
		sb.append(f.getSize());
//		sb.append(",");
//		sb.append(f.getFamily());
		return sb.toString();
	}

	public static String createFontCssString(Font f)
	{
		if (f == null) return null;
		StringBuffer sb = new StringBuffer();
		if (f.isBold())
		{
			sb.append("bold ");
		}
		if (f.isItalic())
		{
			sb.append("italic ");
		}
		sb.append(f.getSize());
		sb.append("pt ");
		sb.append("\"");
		sb.append(f.getName());
		sb.append("\"");
		return sb.toString();
	}

	public static Pair<String, String>[] createFontCSSProperties(String fontType)
	{
		if (fontType == null) return null;

		StringTokenizer tk = new StringTokenizer(fontType, ","); //$NON-NLS-1$
		if (tk.countTokens() >= 3)
		{
			String name = tk.nextToken();
			int istyle = Utils.getAsInteger(tk.nextToken());
			int isize = Utils.getAsInteger(tk.nextToken());
			Pair<String, String> fam = new Pair<String, String>("font-family", name + ", Verdana, Arial"); //$NON-NLS-1$ //$NON-NLS-2$ 
			Pair<String, String> size = new Pair<String, String>("font-size", isize + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			Pair<String, String> italic = null;
			Pair<String, String> bold = null;
			if (istyle == Font.PLAIN)
			{
				italic = new Pair<String, String>("font-style", "normal"); //$NON-NLS-1$ //$NON-NLS-2$
				bold = new Pair<String, String>("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$

			}
			else
			{
				italic = (Font.ITALIC == (Font.ITALIC & istyle) ? new Pair<String, String>("font-style", "italic") : null); //$NON-NLS-1$ //$NON-NLS-2$
				bold = (Font.BOLD == (Font.BOLD & istyle) ? new Pair<String, String>("font-weight", "bold") : null); //$NON-NLS-1$ //$NON-NLS-2$

			}
			return new Pair[] { fam, size, italic, bold };
		}
		return null;
	}
}
