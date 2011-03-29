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
package com.servoy.j2db.util.docvalidator;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.servoy.j2db.util.ValidatingDocument;
import com.servoy.j2db.util.ValidatingDocument.IDocumentValidator;

public class IdentDocumentValidator implements ValidatingDocument.IDocumentValidator
{
	private final int type;

	// Type of identifier determines the rules for the allowed name
	public static final int TYPE_SERVOY = 1;
	public static final int TYPE_SQL = 2;

	public IdentDocumentValidator(int type)
	{
		this.type = type;
	}


	// Returns true if s is a legal Java identifier.
	public static boolean isJavaIdentifier(String s)
	{
		return validateIdentifier(s, TYPE_SERVOY, true);
	}

	// Returns true if s is a legal SQL identifier.
	public static boolean isSQLIdentifier(String s)
	{
		return validateIdentifier(s, TYPE_SQL, true);
	}

	public String validateInsertString(Document document, int offs, String str, AttributeSet a) throws BadLocationException
	{
		if (validateIdentifier(str, type, offs == 0))
		{
			return str;
		}
		return null;
	}

	public String validateReplace(Document document, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
	{
		if (document.getLength() == 0 && text.length() == 0)
		{
			// no replace (this is triggered on the mac)
			return text;
		}
		if (offset == 0 && document.getLength() > 0)
		{
			// delete of start of string
			if ((text.length() == 0 && length == document.getLength()) /* allow make empty */
				|| validateIdentifier((text + document.getText(length, document.getLength() - length)), type, true))
			{
				return text;
			}
		}
		else if (validateIdentifier(text, type, document.getLength() == 0))
		{
			return text;
		}
		return null;
	}

	protected static boolean validateIdentifier(String str, int type, boolean isStart)
	{
		char[] source = str.toCharArray();
		if (isStart)
		{
			if (source.length == 0)
			{
				return false;
			}
			if (!Character.isJavaIdentifierStart(source[0]))
			{
				return false;
			}
			if (type == TYPE_SQL && source[0] == '_') // oracle does not like tables and columns to start with underscore
			{
				return false;
			}
		}

		for (int i = 0; i < source.length; i++)
		{
			if (!Character.isJavaIdentifierPart(source[i]))
			{
				return false;
			}
		}
		return true;
	}

}
