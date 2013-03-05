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
	public static final String BEANS = "beans"; //$NON-NLS-1$
	public static final String RUNTIME = "runtime"; //$NON-NLS-1$
	public static final String DESIGNTIME = "designtime"; //$NON-NLS-1$
	public static final String JSLIB = "jslib"; //$NON-NLS-1$

	public static final String MEMBER_KIND_EVENT = "event"; //$NON-NLS-1$
	public static final String MEMBER_KIND_COMMAND = "command"; //$NON-NLS-1$
	public static final String MEMBER_KIND_PROPERY = "property"; //$NON-NLS-1$

	String category() default PLUGINS;

	/**
	 * This is the name that appears in wiki documentation - in the left-hand-side tree. If scriptingName is not
	 * defined then publicName is used instead even for scriptingName.
	 */
	String publicName() default "";

	/**
	 * Scripting name is the "JS type". It is what will be listed in wiki pages/solex/jseditor tooltips
	 * as a parameter type or as a return type. If scriptingName is null, the "JS type" is what publicName defines.<BR>
	 * If neither publicName or scriptingName are defined, then the "JS type" will be the class simple name.
	 */
	String scriptingName() default "";

	/**
	 * The super-component.
	 */
	String extendsComponent() default "";

	/**
	 * Override for member kind derived from method name.
	 * event, command, property
	 */
	String memberKind() default "";

	/**
	 * See graphical component as button
	 */
	boolean isButton() default false;

	/**
	 * Field display type
	 */
	int displayType() default 0;

	/**
	 * for documenting class, which class is the real class
	 */
	Class< ? > realClass() default Object.class;
}
