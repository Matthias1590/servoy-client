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

import java.io.CharArrayReader;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSMethod implements IJavaScriptType
{
	protected final IApplication application;
	protected final JSForm form;
	protected ScriptMethod sm;
	protected boolean isCopy;

	/**
	 * 
	 */
	JSMethod()
	{
		form = null;
		application = null;
		sm = null;
		isCopy = true;
	}

	public JSMethod(IApplication application, ScriptMethod sm, boolean isNew)
	{
		this.application = application;
		this.sm = sm;
		this.form = null;
		this.isCopy = isNew;
	}

	public JSMethod(IApplication application, JSForm form, ScriptMethod sm, boolean isNew)
	{
		this.sm = sm;
		this.application = application;
		this.form = form;
		this.isCopy = isNew;
	}

	void checkModification()
	{
		if (sm == null) return; // if a default constant

		if (form != null)
		{
			form.checkModification();
			// make copy if needed
			if (!isCopy)
			{
				// then get the replace the item with the item of the copied relation.
				sm = (ScriptMethod)form.getForm().getChild(sm.getUUID());
				isCopy = true;
			}
		}
		else if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			sm = application.getFlattenedSolution().createPersistCopy(sm);
			isCopy = true;
		}
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.AbstractScriptProvider#getDeclaration()
	 * 
	 * @sample
	 * var method = form.newFormMethod('function original() { application.output("Original function."); }');
	 * application.output('original method name: ' + method.getName());
	 * application.output('original method code: ' + method.code);
	 * method.code = 'function changed() { application.output("This is another function."); }';
	 * method.showInMenu = false;
	 * var button = form.newButton('Click me!', 10, 10, 100, 30, method);
	 */
	public String js_getCode()
	{
		if (sm == null) return null; // if a default constant
//		String code = sm.getDeclaration();
//
//		if (code != null && !code.startsWith("function")) //$NON-NLS-1$
//		{
//			code = "function " + sm.getName() + "()\n{\n" + code + "\n}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		}
		return sm.getDeclaration();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.AbstractScriptProvider#getName()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSMethod#js_getCode()
	 * 
	 * @return A String holding the name of this method.
	 */
	public String js_getName()
	{
		if (sm == null) return null; // if a default constant
		return sm.getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ScriptMethod#getShowInMenu()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSMethod#js_getCode()
	 */
	public boolean js_getShowInMenu()
	{
		if (sm == null) return false; // if a default constant
		return sm.getShowInMenu();
	}

	public void js_setCode(String content)
	{
		if (sm == null) return; // if a default constant
		checkModification();

		String name = parseName(content);
		if (!name.equals(sm.getName()))
		{
			try
			{
				sm.updateName(new ScriptNameValidator(form.getApplication().getFlattenedSolution()), name);
			}
			catch (RepositoryException e)
			{
				throw new RuntimeException("Error updating the name from " + sm.getName() + " to " + name, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		sm.setDeclaration(content);

		if (form == null)
		{
			application.getScriptEngine().getGlobalScope().put(sm, sm);
		}
	}

	/**
	 * gets the argument array for this method if that is set for the specific action this method is taken from.
	 * Will return null by default. This is only for reading, you can't alter the arguments through this array, 
	 * for that you need to create a new object through solutionModel.newMethodWithArguments(..) and assign it again.
	 * 
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var button = frm.getButton("button");
	 *  // get the arguments from the button.
	 * 	var arguments = button.onAction.getArguments();
	 *  if (arguments && arguments.length > 1 && arguments[1] == 10) { 
	 *    // change the value and assign it back to the onAction.
	 *    arguments[1] = 50;
	 *    button.onAction = solutionModel.newMethodWithArguments(button.onAction,arguments);
	 *  }
	 * 
	 * @return Array of the arguments, null if not specified.
	 */
	public Object[] js_getArguments()
	{
		return null;
	}

//	public void js_setName(String arg)
//	{
//		if (sm == null) return; // if a default constant
//		checkModification();
//		sm.setName(arg);
//	}

	public void js_setShowInMenu(boolean arg)
	{
		if (sm == null) return; // if a default constant
		checkModification();
		sm.setShowInMenu(arg);
	}

	ScriptMethod getScriptMethod()
	{
		return sm;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		if (sm.getParent() instanceof Form)
		{
			return "JSMethod[name:" + sm.getName() + ",form:" + ((Form)sm.getParent()).getName() + ']';
		}
		else if (sm.getParent() instanceof Solution)
		{
			return "JSMethod[name:" + sm.getName() + ",global, solution:" + ((Solution)sm.getParent()).getName() + ']';
		}
		return "JSMethod[name:" + sm.getName() + ']';
	}

	static String parseName(String content)
	{
		CompilerEnvirons cenv = new CompilerEnvirons();
		Parser parser = new Parser(cenv, new JSErrorReporter());
		try
		{
			ScriptOrFnNode parse = parser.parse(new CharArrayReader(content.toCharArray()), "", 0); //$NON-NLS-1$

			int functionCount = parse.getFunctionCount();
			if (functionCount != 1) throw new RuntimeException("Only 1 function is allowed, found: " + functionCount + " when setting code of a method"); //$NON-NLS-1$ //$NON-NLS-2$

			FunctionNode functionNode = parse.getFunctionNode(0);
			String name = functionNode.getFunctionName();
			return name;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error parsing " + content, e); //$NON-NLS-1$
		}
	}


	static class JSErrorReporter implements ErrorReporter
	{

		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset)
		{
			//do Nothing
		}

		public void error(String message, String sourceName, int lineNumber, String lineSource, int lineOffset)
		{
			throw new EcmaError("compileerror", message, sourceName, lineNumber, lineSource, lineOffset); //$NON-NLS-1$
		}

		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset)
		{
			return new EvaluatorException(message);
		}

	}
}
