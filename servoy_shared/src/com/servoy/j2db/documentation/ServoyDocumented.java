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
package com.servoy.j2db.documentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation that is used to mark the classes that should participate
 * to the process of generating the documentation.
 * 
 * @author gerzse
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServoyDocumented
{
	public static final String PLUGINS = "plugins"; //$NON-NLS-1$
	public static final String RUNTIME = "runtime"; //$NON-NLS-1$
	public static final String DESIGNTIME = "designtime"; //$NON-NLS-1$
	public static final String JSLIB = "jslib"; //$NON-NLS-1$

	String category();

	String publicName() default "";

	String scriptingName() default "";
}
