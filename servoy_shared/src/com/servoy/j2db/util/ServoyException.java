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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;

/**
 * IMPORTANT: The names are exposed to javascripting do not refactor names!
 * @author jblok 
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ServoyException", scriptingName = "ServoyException")
public class ServoyException extends Exception implements IReturnedTypesProvider, IConstantsObject
{
	private static final long serialVersionUID = 3598145362930457281L;

	// --------------------------------------------
	//db set 1xx
	// --------------------------------------------
	/**
	 * Exception code for UNKNOWN_DATABASE_EXCEPTION.
	 */
	public static final int UNKNOWN_DATABASE_EXCEPTION = 100;
	/**
	 * Exception code for DATA_INTEGRITY_VIOLATION.
	 */
	public static final int DATA_INTEGRITY_VIOLATION = 101;
	/**
	 * Exception code for BAD_SQL_SYNTAX.
	 */
	public static final int BAD_SQL_SYNTAX = 102;
	/**
	 * Exception code for PERMISSION_DENIED.
	 */
	public static final int PERMISSION_DENIED = 103;
	/**
	 * Exception code for DEADLOCK.
	 */
	public static final int DEADLOCK = 104;
	/**
	 * Exception code for DATA_ACCESS_RESOURCE_FAILURE.
	 */
	public static final int DATA_ACCESS_RESOURCE_FAILURE = 105;
	/**
	 * Exception code for ACQUIRE_LOCK_FAILURE.
	 */
	public static final int ACQUIRE_LOCK_FAILURE = 106;
	/**
	 * Exception code for INVALID_RESULTSET_ACCESS.
	 */
	public static final int INVALID_RESULTSET_ACCESS = 107;
	/**
	 * Exception code for UNEXPECTED_UPDATE_COUNT.
	 */
	public static final int UNEXPECTED_UPDATE_COUNT = 108;


	// --------------------------------------------
	//application error code should be in 300 range
	// --------------------------------------------
	/**
	 * Exception code for NO_LICENSE.
	 */
	public static final int NO_LICENSE = 307;
	/**
	 * Exception code for RECORD_LOCKED.
	 */
	public static final int RECORD_LOCKED = 308;
	/**
	 * Exception code for INVALID_INPUT_FORMAT.
	 */
	public static final int INVALID_INPUT_FORMAT = 309;
	/**
	 * Exception code for INVALID_INPUT.
	 */
	public static final int INVALID_INPUT = 310;
	/**
	 * Exception code for EXECUTE_PROGRAM_FAILED.
	 */
	public static final int EXECUTE_PROGRAM_FAILED = 311;
	/**
	 * Exception code for INCORRECT_LOGIN.
	 */
	public static final int INCORRECT_LOGIN = 312;
	/**
	 * Exception code for NO_MODIFY_ACCESS.
	 */
	public static final int NO_MODIFY_ACCESS = 319;
	/**
	 * Exception code for NO_ACCESS.
	 */
	public static final int NO_ACCESS = 320;
	/**
	 * Exception code for NO_DELETE_ACCESS.
	 */
	public static final int NO_DELETE_ACCESS = 322;
	/**
	 * Exception code for NO_CREATE_ACCESS.
	 */
	public static final int NO_CREATE_ACCESS = 323;
	/**
	 * Exception code for NO_RELATED_CREATE_ACCESS.
	 */
	public static final int NO_RELATED_CREATE_ACCESS = 324;
//	public static final int VALIDATOR_NOT_FOUND = 327;
//	public static final int CONVERTER_NOT_FOUND = 328;
	/**
	 * Exception code for SAVE_FAILED.
	 */
	public static final int SAVE_FAILED = 330;
	/**
	 * Exception code for NO_PARENT_DELETE_WITH_RELATED_RECORDS.
	 */
	public static final int NO_PARENT_DELETE_WITH_RELATED_RECORDS = 331;
	/**
	 * Exception code for DELETE_NOT_GRANTED.
	 */
	public static final int DELETE_NOT_GRANTED = 332;
	/**
	 * Exception code for MAINTENANCE_MODE.
	 */
	public static final int MAINTENANCE_MODE = 333;
	/**
	 * Exception code for ABSTRACT_FORM.
	 */
	public static final int ABSTRACT_FORM = 334;
	/**
	 * Exception code for RECORD_VALIDATION_FAILED.
	 */
	public static final int RECORD_VALIDATION_FAILED = 335;
	/**
	 * Exception code for CLIENT_NOT_AUTHORIZED.
	 */
	public static final int CLIENT_NOT_AUTHORIZED = 336;

	/**
	 * Error codes not available from java-script.
	 */
	public static class InternalCodes
	{
		public static final int UNKNOWN_EXCEPTION = 0;
		public static final int INTERNAL_ERROR = 1;

		// --------------------------------------------
		//repository set 2xx
		// --------------------------------------------
		public static final int ERROR_NO_REPOSITORY_IN_DB = 204;
		public static final int ERROR_OLD_REPOSITORY_IN_DB = 205;
		public static final int ERROR_TOO_NEW_REPOSITORY_IN_DB = 206;
		public static final int SERVER_NOT_FOUND = 213;
		public static final int TABLE_NOT_FOUND = 214;
		public static final int COLUMN_NOT_FOUND = 225;
		public static final int PRIMARY_KEY_NOT_FOUND = 221;
		public static final int ERROR_IN_TRANSACTION = 202;
		public static final int NO_TRANSACTION_ACTIVE = 215;
		public static final int INVALID_RMI_SERVER_CONNECTION = 216;
		public static final int CUSTOM_REPOSITORY_ERROR = 217;
		public static final int CHECKSUM_FAILURE = 218;
		public static final int ELEMENT_CHANGED_TYPE = 219; // an element (fixed uuid) changed object type between revisions
		public static final int INVALID_PROPERTY_VALUE = 220;
		public static final int INVALID_EXPORT = 226;
		public static final int CONNECTION_POOL_EXHAUSTED = 227;

		// --------------------------------------------
		//unknown set 4xx
		// --------------------------------------------
		public static final int CONNECTION_LOST = 401;
		public static final int OPERATION_CANCELLED = 403;
		public static final int JS_SCRIPT_ERROR = 410; //only use for js errors which halts the script
		public static final int CLIENT_NOT_REGISTERED = 420;
	}

	private int errorCode = 0;
	protected final Object[] tagValues;

	public ServoyException()
	{
		this(0, null);
	} // for scripting purposes

	public ServoyException(int errorCode)
	{
		this(errorCode, null);
	}

	public ServoyException(int errorCode, Object[] values)
	{
		super();
		this.errorCode = errorCode;
		tagValues = values;
	}

	/**
	 * Returns the errorCode.
	 * 
	 * @return int
	 */
	public int getErrorCode()
	{
		return errorCode;
	}

	/**
	 * Always true; it makes the distinction between ServoyException and DataException.
	 * @sampleas js_getErrorCode()
	 * @return true.
	 * @deprecated Use "typeof" operator instead.
	 */
	@Deprecated
	public boolean js_isServoyException()
	{
		return true;
	}

	@Override
	public String getMessage()
	{
		switch (errorCode)
		{
			case InternalCodes.CONNECTION_LOST :
				return Messages.getString("servoy.applicationException.connectionLost"); //$NON-NLS-1$

			case RECORD_LOCKED :
				return Messages.getString("servoy.foundSet.recordLocked"); //$NON-NLS-1$

			case InternalCodes.JS_SCRIPT_ERROR :
				return Messages.getString("servoy.applicationException.javascriptError"); //$NON-NLS-1$

			case NO_LICENSE :
				return Messages.getString("servoy.applicationException.noLicense"); //$NON-NLS-1$

			case EXECUTE_PROGRAM_FAILED :
				return Messages.getString("servoy.applicationException.execureProgramFailed"); //$NON-NLS-1$

			case INCORRECT_LOGIN :
				return Messages.getString("servoy.applicationException.incorrectLogin"); //$NON-NLS-1$

			case InternalCodes.SERVER_NOT_FOUND :
				return Messages.getString("servoy.exception.serverNotFound", tagValues); //$NON-NLS-1$

			case InternalCodes.TABLE_NOT_FOUND :
				return Messages.getString("servoy.sqlengine.error.tableMissing", tagValues); //$NON-NLS-1$

			case InternalCodes.COLUMN_NOT_FOUND :
				return Messages.getString("servoy.sqlengine.error.columnMissing", tagValues); //$NON-NLS-1$

			case InternalCodes.PRIMARY_KEY_NOT_FOUND :
				return Messages.getString("servoy.exception.primaryKeyNeeded", tagValues); //$NON-NLS-1$

			case InternalCodes.NO_TRANSACTION_ACTIVE :
				return Messages.getString("servoy.sqlengine.error.noTransactionActive", tagValues); //$NON-NLS-1$

			case InternalCodes.INVALID_RMI_SERVER_CONNECTION :
				return Messages.getString("servoy.exception.invalidServerConnection"); //$NON-NLS-1$

			case InternalCodes.ERROR_NO_REPOSITORY_IN_DB :
				return "No repository found in the database."; //$NON-NLS-1$

			case InternalCodes.ERROR_OLD_REPOSITORY_IN_DB :
				return "Old repository found in the database. Repository version: " + tagValues[0] + ", software version: " + tagValues[1] + //$NON-NLS-1$ //$NON-NLS-2$
					". Upgrade the repository first."; //$NON-NLS-1$

			case InternalCodes.ERROR_TOO_NEW_REPOSITORY_IN_DB :
				return "Repository found in database too new for this software version. Repository version: " + tagValues[0] + ", software version: " + //$NON-NLS-1$ //$NON-NLS-2$
					tagValues[1] + ". Upgrade Servoy first."; //$NON-NLS-1$

			case InternalCodes.CHECKSUM_FAILURE :
				return "Checksum failure"; //$NON-NLS-1$

			case InternalCodes.INVALID_EXPORT :
				return "Invalid export"; //$NON-NLS-1$

			case InternalCodes.CONNECTION_POOL_EXHAUSTED :
				return "Connection pool for server " + tagValues[0] + " exhausted"; //$NON-NLS-1$ //$NON-NLS-2$

			case InternalCodes.ERROR_IN_TRANSACTION :
				return "Error in transaction"; //$NON-NLS-1$

			case InternalCodes.CUSTOM_REPOSITORY_ERROR :
				return tagValues[0].toString();

			case NO_MODIFY_ACCESS :
				return Messages.getString("servoy.foundSet.error.noModifyAccess"); //$NON-NLS-1$

			case NO_ACCESS :
				return Messages.getString("servoy.foundSet.error.noAccess"); //$NON-NLS-1$

			case NO_DELETE_ACCESS :
				return Messages.getString("servoy.foundSet.error.noDeleteAccess"); //$NON-NLS-1$

			case NO_CREATE_ACCESS :
				return Messages.getString("servoy.foundSet.error.noCreateAccess"); //$NON-NLS-1$

			case NO_RELATED_CREATE_ACCESS :
				return Messages.getString("servoy.foundset.error.createRelatedRecordsNotAllowed", tagValues); //$NON-NLS-1$

			case RECORD_VALIDATION_FAILED :
				return Messages.getString("servoy.foundset.error.recordValidationFailed", tagValues); //$NON-NLS-1$

			case SAVE_FAILED :
				return Messages.getString("servoy.formPanel.error.saveFormData"); //$NON-NLS-1$

			case NO_PARENT_DELETE_WITH_RELATED_RECORDS :
				return Messages.getString("servoy.foundset.error.noParentDeleteWithRelatedrecords", tagValues); //$NON-NLS-1$

			case DELETE_NOT_GRANTED :
				return Messages.getString("servoy.foundset.error.deleteNotGranted"); //$NON-NLS-1$

			case MAINTENANCE_MODE :
				return Messages.getString("servoy.applicationException.maintenanceMode"); //$NON-NLS-1$

			case ABSTRACT_FORM :
				return Messages.getString("servoy.formPanel.error.cannotShowForm"); //$NON-NLS-1$

			case InternalCodes.OPERATION_CANCELLED :
				return "Operation cancelled"; //$NON-NLS-1$

			case INVALID_INPUT_FORMAT :
				return Messages.getString("servoy.applicationException.invalidInputFormat", tagValues); //$NON-NLS-1$

			case INVALID_INPUT :
				return Messages.getString("servoy.applicationException.invalidInput") + (getCause() != null ? ", " + getCause().getMessage() : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			case CLIENT_NOT_AUTHORIZED :
				return Messages.getString("servoy.client.notAuthorized"); //$NON-NLS-1$

			case InternalCodes.CLIENT_NOT_REGISTERED :
				return Messages.getString("servoy.sqlengine.error.notRegistered"); //$NON-NLS-1$

			case UNEXPECTED_UPDATE_COUNT :
				return "Update/insert failed, unexpected nr of records affected: expected " + tagValues[0] + ", actual " + tagValues[1]; //$NON-NLS-1$ //$NON-NLS-2$

			default :
			{
				if (errorCode == 0 && getCause() != null)
				{
					return super.getMessage();
				}
				else
				{
					return Messages.getString("servoy.applicationException.errorCode", new Object[] { new Integer(errorCode) }); //$NON-NLS-1$
				}
			}
		}
	}

	public int findErrorCode()
	{
		if (errorCode > 0)
		{
			return errorCode;
		}
		if (getCause() instanceof ServoyException)
		{
			return ((ServoyException)getCause()).findErrorCode();
		}
		return 0;
	}

	public boolean hasErrorCode(int code)
	{
		if (errorCode == code)
		{
			return true;
		}
		if (getCause() instanceof ServoyException)
		{
			return ((ServoyException)getCause()).hasErrorCode(code);
		}
		return false;
	}

	/**
	 * Returns the error code for this ServoyException. Can be one of the constants declared in ServoyException.
	 *
	 * @sample
	 * //this sample script should be attached to onError method handler in the solution settings
	 * application.output("Exception Object: "+ex)
	 * application.output("MSG: "+ex.getMessage())
	 * if (ex instanceof ServoyException)
	 * {
	 * 	application.output("is a ServoyException")
	 * 	application.output("Errorcode: "+ex.getErrorCode())
	 * 	if (ex.getErrorCode() == ServoyException.SAVE_FAILED)
	 * 	{
	 * 		plugins.dialogs.showErrorDialog( "Error",  "It seems you did not fill in a required field", 'OK');
	 * 		//Get the failed records after a save
	 * 		var array = databaseManager.getFailedRecords()
	 * 		for( var i = 0 ; i < array.length ; i++ )
	 * 		{
	 * 			var record = array[i];
	 * 			application.output(record.exception);
	 * 			if (record.exception instanceof DataException)
	 * 			{
	 * 				application.output("SQL: "+record.exception.getSQL())
	 * 				application.output("SQLState: "+record.exception.getSQLState())
	 * 				application.output("VendorErrorCode: "+record.exception.getVendorErrorCode())
	 * 			}
	 * 		}
	 * 		return false
	 * 	}
	 * }
	 * //if returns false or no return, error is not reported to client; if returns true error is reported
	 * //by default error report means logging the error, in smart client an error dialog will also show up
	 * return true
	 * @return the error code for this ServoyException. Can be one of the constants declared in ServoyException.
	 */
	public int js_getErrorCode()
	{
		return errorCode;
	}

	/**
	 * Returns the string message for this ServoyException. 
	 *
	 * @sampleas js_getErrorCode()
	 * @return the string message for this ServoyException. 
	 */
	public String js_getMessage()
	{
		return getMessage();
	}

	/**
	 * Returns the stack trace for this ServoyException. 
	 *
	 * @sampleas js_getErrorCode()
	 * @return the string stack trace for this ServoyException. 
	 */
	public String js_getStackTrace()
	{
		Writer result = new StringWriter();
		this.printStackTrace(new PrintWriter(result));
		return result.toString();
	}

	/**
	 * @see com.servoy.j2db.scripting.IScriptObject#getAllReturnedTypes()
	 */
	public Class< ? >[] getAllReturnedTypes()
	{
		return new Class[] { DataException.class };
	}

	/**
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString()
	{
		if (errorCode == 0)
		{
			return "ServoyException"; //$NON-NLS-1$
		}
		return super.toString();
	}
}
