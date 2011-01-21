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
package com.servoy.j2db.dataprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IPrepareForSave;
import com.servoy.j2db.dataprocessing.ValueFactory.BlobMarkerValue;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Keeps track of all the edited records and handles the save
 * 
 * @author jcompagner
 */
public class EditRecordList
{
	private final FoundSetManager fsm;

	private final List<IPrepareForSave> prepareForSaveListeners = new ArrayList<IPrepareForSave>(2);
	private final List<IGlobalEditListener> editListeners = new ArrayList<IGlobalEditListener>();

	private final Map<Table, Integer> accessMap = Collections.synchronizedMap(new HashMap<Table, Integer>());//per table (could be per column in future)
	private boolean autoSave = true;

	private ConcurrentMap<FoundSet, int[]> fsEventMap;

	private final ReentrantLock editRecordsLock = new ReentrantLock();

	private final List<IRecordInternal> editedRecords = Collections.synchronizedList(new ArrayList<IRecordInternal>(32));
	private final List<IRecordInternal> failedRecords = Collections.synchronizedList(new ArrayList<IRecordInternal>(2));
	private final List<IRecordInternal> recordTested = Collections.synchronizedList(new ArrayList<IRecordInternal>(2)); //tested for OnRecordSave event
	private boolean preparingForSave;

	private final boolean disableInsertsReorder;

	public EditRecordList(FoundSetManager fsm)
	{
		this.fsm = fsm;
		disableInsertsReorder = Utils.getAsBoolean(fsm.getApplication().getSettings().getProperty("servoy.disable.record.insert.reorder", "false")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IRecordInternal[] getFailedRecords()
	{
		editRecordsLock.lock();
		try
		{
			return failedRecords.toArray(new IRecordInternal[failedRecords.size()]);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @param record
	 * @return
	 */
	public boolean isEditing(IRecordInternal record)
	{
		editRecordsLock.lock();
		try
		{
			return editedRecords.contains(record);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public boolean isEditing()
	{
		editRecordsLock.lock();
		try
		{
			return editedRecords.size() > 0;
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @param set
	 * @return
	 */
	public IRecordInternal[] getEditedRecords(IFoundSetInternal set)
	{
		List<IRecordInternal> al = new ArrayList<IRecordInternal>();
		editRecordsLock.lock();
		try
		{
			for (int i = editedRecords.size(); --i >= 0;)
			{
				IRecordInternal record = editedRecords.get(i);
				if (record.getParentFoundSet() == set)
				{
					al.add(record);
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return al.toArray(new IRecordInternal[al.size()]);
	}

	public IRecordInternal[] getFailedRecords(IFoundSetInternal set)
	{
		List<IRecordInternal> al = new ArrayList<IRecordInternal>();
		editRecordsLock.lock();
		try
		{
			for (int i = failedRecords.size(); --i >= 0;)
			{
				IRecordInternal record = failedRecords.get(i);
				if (record.getParentFoundSet() == set)
				{
					al.add(record);
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return al.toArray(new IRecordInternal[al.size()]);
	}

	public boolean hasEditedRecords(IFoundSetInternal foundset)
	{
		removeUnChangedRecords(false, false);
		editRecordsLock.lock();
		try
		{
			List<IRecordInternal> allEditing = new ArrayList<IRecordInternal>();
			allEditing.addAll(editedRecords);
			allEditing.addAll(failedRecords);
			for (int i = allEditing.size(); --i >= 0;)
			{
				IRecordInternal record = allEditing.get(i);
				if (record.getParentFoundSet() == foundset)
				{
					return true;
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return false;
	}


	private boolean isSaving = false;
	private Exception lastStopEditingException;

	public int stopIfEditing(IFoundSetInternal fs)
	{
		if (hasEditedRecords(fs))
		{
			return stopEditing(false);
		}
		return ISaveConstants.STOPPED;
	}

	public int stopEditing(boolean javascriptStop)
	{
		return stopEditing(javascriptStop, (List<IRecordInternal>)null, 0);
	}

	/**
	 * stop/save
	 * 
	 * @param javascriptStop
	 * @param recordToSave null means all records
	 * @return IRowChangeListener static final
	 */
	public int stopEditing(boolean javascriptStop, IRecordInternal recordToSave)
	{
		return stopEditing(javascriptStop, Arrays.asList(new IRecordInternal[] { recordToSave }), 0);
	}

	/**
	 * stop/save
	 * 
	 * @param javascriptStop
	 * @param recordsToSave null means all records
	 * @return IRowChangeListener static final
	 */
	public int stopEditing(boolean javascriptStop, List<IRecordInternal> recordsToSave)
	{
		return stopEditing(javascriptStop, recordsToSave, 0);
	}

	private int stopEditing(final boolean javascriptStop, final List<IRecordInternal> recordsToSave, int recursionDepth)
	{
		if (recursionDepth > 50)
		{
			fsm.getApplication().reportJSError("stopEditing max recursion exceeded", new RuntimeException());
			return ISaveConstants.SAVE_FAILED;
		}

		//prevent recursive calls
		if (isSaving && !javascriptStop)
		{
			return ISaveConstants.STOPPED;
		}

		if (recordsToSave != null)
		{
			boolean hasEditedRecords = false;
			editRecordsLock.lock();
			try
			{
				for (IRecordInternal record : recordsToSave)
				{
					if (editedRecords.contains(record))
					{
						hasEditedRecords = true;
						break;
					}
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
			if (!hasEditedRecords) return ISaveConstants.STOPPED;
		}

		// here we can't have a test if editedRecords is empty (and return stop)
		// because for just globals or findstates (or deleted records)
		// we need to pass prepareForSave.

		if (!fsm.getApplication().isEventDispatchThread())
		{
			// only the event dispatch thread can stop an current edit.
			// this is a fix for innerworkings because background aggregate queries seems to also trigger saves
			Debug.trace("Stop edit postponend because it is not in the event dispatch thread: " + Thread.currentThread().getName()); //$NON-NLS-1$
			// calculations running from lazy table view loading threads may trigger stopEditing
			fsm.getApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					// do not stop if the user is editing something else.
					boolean stop;
					editRecordsLock.lock();
					try
					{
						stop = editedRecords.size() == 1 && recordsToSave != null && recordsToSave.size() == 1 && editedRecords.get(0) == recordsToSave.get(0);
					}
					finally
					{
						editRecordsLock.unlock();
					}
					if (stop)
					{
						stopEditing(javascriptStop, recordsToSave);
					}
					else
					{
						Debug.trace("Stop edit skipped because other records are being edited"); //$NON-NLS-1$
					}
				}

			});
			return ISaveConstants.AUTO_SAVE_BLOCKED;
		}

		int editedRecordsSize;
		try
		{
			isSaving = true;
			int p = prepareForSave(true);
			if (p != ISaveConstants.STOPPED)
			{
				return p;
			}

			//remove any non referenced failed records
			boolean fireChange = false;
			editRecordsLock.lock();
			try
			{
				if (failedRecords.size() != 0)
				{
					Iterator<IRecordInternal> it = failedRecords.iterator();
					while (it.hasNext())
					{
						IRecordInternal rec = it.next();
						if (rec != null)
						{
							if (rec.getParentFoundSet() == null)
							{
								it.remove();
							}
							else if (rec.getParentFoundSet().getRecordIndex(rec) == -1)
							{
								it.remove();
							}
						}
					}
					if (failedRecords.size() == 0)
					{
						fireChange = true;
					}
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
			if (fireChange) fireEditChange();

			// remove the unchanged, really calculate when it is a real stop (autosave = true or it is a javascript stop)
			removeUnChangedRecords(autoSave || javascriptStop, true);

			//check if anything left
			int editRecordListSize;
			editRecordsLock.lock();
			try
			{
				editRecordListSize = editedRecords.size();
				if (editRecordListSize == 0) return ISaveConstants.STOPPED;
			}
			finally
			{
				editRecordsLock.unlock();
			}

			//cannot stop, its blocked
			if (!autoSave && !javascriptStop)
			{
				return ISaveConstants.AUTO_SAVE_BLOCKED;
			}

			int failedCount = 0;
			lastStopEditingException = null;
			List<RowUpdateInfo> rowUpdates = new ArrayList<RowUpdateInfo>(editRecordListSize);
			editRecordsLock.lock();
			try
			{
				Iterator<IRecordInternal> it = new AllowListModificationIterator<IRecordInternal>(editedRecords);
				outer : while (it.hasNext())
				{
					IRecordInternal tmp = it.next();

					if (tmp instanceof Record)
					{
						Record record = (Record)tmp;

						if (recordsToSave != null && !recordsToSave.contains(record)) continue;

						//prevent multiple update for the same row (from multiple records)
						for (int j = 0; j < rowUpdates.size(); j++)
						{
							if (rowUpdates.get(j).getRow() == record.getRawData())
							{
								recordTested.remove(record);
								it.remove();
								continue outer;
							}
						}

						try
						{
							// test for table events; this may execute table events if the user attached JS methods to them;
							// the user might add/delete/edit records in the JS - thus invalidating a normal iterator (it)
							// - edited record list changes; this is why an AllowListModificationIterator is used

							// Note that the behaviour is different when trigger returns false or when it throws an exception.
							// when the trigger returns false, record must stay in editedRecords.
							//   this is needed because the trigger may be used as validation to keep the user in the record when autosave=true.
							// when the trigger throws an exception, the record must move from editedRecords to failedRecords so that in 
							//    scripting the failed records can be examined (the thrown value is retrieved via record.exception.getValue())
							editRecordsLock.unlock();
							try
							{
								if (!testTableEvents(record)) // throws ServoyException when trigger method throws exception
								{
									// just directly return if one returns false.
									return ISaveConstants.VALIDATION_FAILED;
								}
							}
							finally
							{
								editRecordsLock.lock();
							}

							RowUpdateInfo rowUpdateInfo = getRecordUpdateInfo(record);
							if (rowUpdateInfo != null)
							{
								rowUpdateInfo.setRecord(record);
								rowUpdates.add(rowUpdateInfo);
							}
							else
							{
								recordTested.remove(record);
								it.remove();
							}
						}
						catch (ServoyException e)
						{
							// trigger method threw exception
							lastStopEditingException = e;
							failedCount++;
							record.getRawData().setLastException(e); //set latest
							if (!failedRecords.contains(record))
							{
								failedRecords.add(record);
							}
							recordTested.remove(record);
							it.remove();
						}
						catch (Exception e)
						{
							Debug.error("Not a normal Servoy/Db Exception generated in saving record: " + record + " removing the record", e); //$NON-NLS-1$ //$NON-NLS-2$
							recordTested.remove(record);
							it.remove();
						}
					}
					else
					{
						// find state
						recordTested.remove(tmp);
						it.remove();
					}
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}

			if (failedCount > 0)
			{
				if (!(lastStopEditingException instanceof ServoyException))
				{
					lastStopEditingException = new ApplicationException(ServoyException.SAVE_FAILED, lastStopEditingException);
				}
				if (!javascriptStop) fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), //$NON-NLS-1$
					lastStopEditingException);
				return ISaveConstants.SAVE_FAILED;
			}

			if (rowUpdates.size() == 0)
			{
				fireEditChange();
				if (Debug.tracing())
				{
					Debug.trace("no records to update anymore, failed: " + failedRecords.size()); //$NON-NLS-1$
				}
				return ISaveConstants.STOPPED;
			}

			if (Debug.tracing())
			{
				Debug.trace("Updating/Inserting " + rowUpdates.size() + " records: " + rowUpdates.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			}

			RowUpdateInfo[] infos = rowUpdates.toArray(new RowUpdateInfo[rowUpdates.size()]);
			if (infos.length > 1 && !disableInsertsReorder)
			{
				// search if there are new row pks used that are 
				// used in records before this record and sort it based on that.
				boolean changed = false;
				List<RowUpdateInfo> al = new ArrayList<RowUpdateInfo>(Arrays.asList(infos));
				int prevI = -1;
				outer : for (int i = al.size(); --i > 0;)
				{
					Row row = al.get(i).getRow();
					// only test for new rows and its pks.
					if (row.existInDB()) continue;
					String[] pkColumns = row.getRowManager().getSQLSheet().getPKColumnDataProvidersAsArray();
					Object[] pk = row.getPK();
					for (int j = 0; j < pk.length; j++)
					{
						Object pkObject = pk[j];
						// special case if pk was db ident and that value was copied from another row.
						if (pkObject instanceof DbIdentValue && ((DbIdentValue)pkObject).getRow() != row) continue;
						for (int k = 0; k < i; k++)
						{
							RowUpdateInfo updateInfo = al.get(k);
							Object[] values = updateInfo.getRow().getRawColumnData();
							int[] pkIndexes = updateInfo.getFoundSet().getSQLSheet().getPKIndexes();
							IntHashMap<String> pks = new IntHashMap<String>(pkIndexes.length, 1);
							for (int pkIndex : pkIndexes)
							{
								pks.put(pkIndex, ""); //$NON-NLS-1$
							}
							for (int l = 0; l < values.length; l++)
							{
								// skip all pk column indexes (except from dbidents from other rows, this may need resort). Those shouldn't be resorted
								if (!(values[l] instanceof DbIdentValue && ((DbIdentValue)values[l]).getRow() != updateInfo.getRow()) && pks.containsKey(l)) continue;

								boolean same = values[l] == pkObject;
								if (!same && values[l] != null)
								{
									Column pkColumn = row.getRowManager().getSQLSheet().getTable().getColumn(pkColumns[j]);
									if (pkColumn.hasFlag(Column.UUID_COLUMN))
									{
										// same uuids are the same even if not the same object
										same = Utils.equalObjects(pkObject, values[l], 0, true);
									}
								}
								if (same)
								{
									al.add(k, al.remove(i));
									// watch out for endless loops when 2 records both with pk's point to each other...
									if (prevI != i)
									{
										prevI = i;
										i++;
									}
									changed = true;
									continue outer;
								}
							}
						}
					}
				}
				if (changed)
				{
					infos = al.toArray(infos);
				}
			}

			ISQLStatement[] statements = new ISQLStatement[infos.length];
			for (int i = 0; i < infos.length; i++)
			{
				statements[i] = infos[i].getISQLStatement();
			}

			// TODO if one statement fails in a transaction how do we know which one? and should we rollback all rows in these statements?
			Object[] idents = null;
			try
			{
				idents = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), statements);
			}
			catch (Exception e)
			{
				lastStopEditingException = e;
				if (!javascriptStop) fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), //$NON-NLS-1$
					new ApplicationException(ServoyException.SAVE_FAILED, lastStopEditingException));
				return ISaveConstants.SAVE_FAILED;
			}

			if (idents.length != infos.length)
			{
				Debug.error("Should be of same size!!"); //$NON-NLS-1$
			}

			List<RowUpdateInfo> infosToBePostProcessed = new ArrayList<RowUpdateInfo>();

			Map<FoundSet, List<Record>> foundsetToRecords = new HashMap<FoundSet, List<Record>>();
			Map<FoundSet, List<String>> foundsetToAggregateDeletes = new HashMap<FoundSet, List<String>>();
			List<Runnable> fires = new ArrayList<Runnable>(infos.length);
			// Walk in reverse over it, so that related rows are update in there row manger before they are required by there parents.
			for (int i = infos.length; --i >= 0;)
			{
				RowUpdateInfo rowUpdateInfo = infos[i];
				FoundSet foundSet = rowUpdateInfo.getFoundSet();
				Row row = rowUpdateInfo.getRow();

				String oldKey = row.getPKHashKey();
				Record record = rowUpdateInfo.getRecord();
				if (idents != null && idents.length != 0 && idents[i] != null)
				{
					Object retValue = idents[i];
					if (retValue instanceof Exception)
					{
						lastStopEditingException = (Exception)retValue;
						failedCount++;
						row.setLastException((Exception)retValue);
						markRecordAsFailed(record);
						continue;
					}
					else if (retValue instanceof Object[])
					{
						Object[] rowData = (Object[])retValue;
						Object[] oldRowData = row.getRawColumnData();
						if (oldRowData != null)
						{
							if (oldRowData.length == rowData.length)
							{
								for (int j = 0; j < rowData.length; j++)
								{
									if (rowData[j] instanceof BlobMarkerValue)
									{
										rowData[j] = oldRowData[j];
									}
									if (oldRowData[j] instanceof DbIdentValue)
									{
										row.setDbIdentValue(rowData[j]);
									}
								}
							}
							else
							{
								Debug.error("Requery data has different length from row data.");
							}
						}
						row.setRollbackData(rowData, false);
					}
					else if (!Boolean.TRUE.equals(retValue))
					{
						// is db ident, can only be one column
						row.setDbIdentValue(retValue);
					}
				}
				editRecordsLock.lock();
				try
				{
					editedRecords.remove(record);
					recordTested.remove(record);
				}
				finally
				{
					editRecordsLock.unlock();
				}

				if (!row.existInDB())
				{
					foundSet.updatePk(record);
				}
				try
				{
					row.getRowManager().rowUpdated(row, oldKey, foundSet, fires);
				}
				catch (Exception e)
				{
					lastStopEditingException = e;
					failedCount++;
					row.setLastException(e);
					editRecordsLock.lock();
					try
					{
						if (!failedRecords.contains(record))
						{
							failedRecords.add(record);
						}
					}
					finally
					{
						editRecordsLock.unlock();
					}
				}

				infosToBePostProcessed.add(infos[i]);

				List<Record> lst = foundsetToRecords.get(foundSet);
				if (lst == null)
				{
					lst = new ArrayList<Record>(3);
					foundsetToRecords.put(foundSet, lst);
				}
				lst.add(record);

				List<String> aggregates = foundsetToAggregateDeletes.get(foundSet);
				if (aggregates == null)
				{
					foundsetToAggregateDeletes.put(foundSet, rowUpdateInfo.getAggregatesToRemove());
				}
				else
				{
					List<String> toMerge = rowUpdateInfo.getAggregatesToRemove();
					for (int j = 0; j < toMerge.size(); j++)
					{
						String aggregate = toMerge.get(j);
						if (!aggregates.contains(aggregate))
						{
							aggregates.add(aggregate);
						}
					}
				}
			}

			// run rowmanager fires in reverse order (original order because info's were processed in reverse order) -> first inserted record is fired first
			for (int i = fires.size(); --i >= 0;)
			{
				fires.get(i).run();
			}

			// get the size of the edited records before the table events, so that we can look if those events did change records again.
			editedRecordsSize = editedRecords.size();
			for (RowUpdateInfo rowUpdateInfo : infosToBePostProcessed)
			{
				try
				{
					executeAfterUpdateOrInsertTrigger(rowUpdateInfo.getRecord(), rowUpdateInfo.getISQLStatement().getAction() == ISQLActionTypes.INSERT_ACTION);
				}
				catch (ServoyException e)
				{
					Debug.error("Failed to execute after update/insert trigger.", e); //$NON-NLS-1$
				}
			}

			Iterator<Map.Entry<FoundSet, List<Record>>> it = foundsetToRecords.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry<FoundSet, List<Record>> entry = it.next();
				FoundSet fs = entry.getKey();
				fs.recordsUpdated(entry.getValue(), foundsetToAggregateDeletes.get(fs));
			}
			boolean shouldFireEditChange;
			editRecordsLock.lock();
			try
			{
				shouldFireEditChange = editedRecords.size() == 0;
			}
			finally
			{
				editRecordsLock.unlock();
			}
			if (shouldFireEditChange)
			{
				fireEditChange();
			}
			if (failedCount > 0)
			{
				lastStopEditingException = new ApplicationException(ServoyException.SAVE_FAILED, lastStopEditingException);
				if (!javascriptStop) fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), //$NON-NLS-1$
					lastStopEditingException);
				else fsm.getApplication().reportJSError("save failed for 1 or more records", lastStopEditingException); //$NON-NLS-1$
				return ISaveConstants.SAVE_FAILED;
			}
		}
		catch (RuntimeException e)
		{
			if (e instanceof IllegalArgumentException)
			{
				fsm.getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));
				return ISaveConstants.SAVE_FAILED;
			}
			else if (e instanceof IllegalStateException)
			{
				fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), e); //$NON-NLS-1$
				return ISaveConstants.SAVE_FAILED;
			}
			else
			{
				Debug.error(e);
				throw e;
			}
		}
		finally
		{
			isSaving = false;
			fireEvents();
		}

		if (editedRecords.size() != editedRecordsSize && recordsToSave == null)
		{
			// records where changed by the after insert/update table events, call stop edit again if this was not a specific record save. 
			return stopEditing(javascriptStop, null, recursionDepth + 1);
		}

		return ISaveConstants.STOPPED;
	}

	/**
	 * Mark record as failed, move to failed records, remove from editedRecords if it was in there.
	 * @param record
	 */
	void markRecordAsFailed(IRecordInternal record)
	{
		editRecordsLock.lock();
		try
		{
			editedRecords.remove(record);
			if (!failedRecords.contains(record))
			{
				failedRecords.add(record);
			}
			recordTested.remove(record);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	void executeAfterUpdateOrInsertTrigger(Record record, boolean wasInsert) throws ServoyException
	{
		Table table = (Table)record.getParentFoundSet().getTable();
		FlattenedSolution solution = fsm.getApplication().getFlattenedSolution();
		Iterator<TableNode> tableNodes = solution.getTableNodes(table);
		while (tableNodes.hasNext())
		{
			TableNode tn = tableNodes.next();
			int methodId;
			String methodKey;
			if (!wasInsert)
			{
				methodId = tn.getOnAfterUpdateMethodID();
				methodKey = "onAfterUpdateMethodID"; //$NON-NLS-1$
			}
			else
			{
				methodId = tn.getOnAfterInsertMethodID();
				methodKey = "onAfterInsertMethodID"; //$NON-NLS-1$
			}
			if (methodId > 0)
			{
				ScriptMethod globalScriptMethod = solution.getScriptMethod(methodId);
				if (globalScriptMethod != null)
				{
					IExecutingEnviroment scriptEngine = fsm.getApplication().getScriptEngine();
					GlobalScope gscope = scriptEngine.getSolutionScope().getGlobalScope();
					Object function = gscope.get(globalScriptMethod.getName());
					if (function instanceof Function)
					{
						Object[] methodArgs = new Object[] { record };
						try
						{
							scriptEngine.executeFunction(((Function)function), gscope, gscope, Utils.arrayMerge(methodArgs,
								Utils.parseJSExpressions(tn.getInstanceMethodArguments(methodKey))), false, false);
						}
						catch (Exception e)
						{
							Debug.error(e);
							throw new ServoyException(ServoyException.SAVE_FAILED, new Object[] { e.getMessage() });
						}
					}
				}
			}
		}
	}

	public int prepareForSave(boolean looseFocus)
	{
		if (preparingForSave)
		{
			return ISaveConstants.STOPPED;
		}
		try
		{
			preparingForSave = true;
			//stop UI editing and fire onRecordEditStop Event 
			if (!firePrepareForSave(looseFocus))
			{
				Debug.trace("no stop editing because prepare for save stopped it"); //$NON-NLS-1$
				return ISaveConstants.VALIDATION_FAILED;
			}
			return ISaveConstants.STOPPED;
		}
		finally
		{
			preparingForSave = false;
		}
	}

	/**
	 * @param record
	 * @return
	 */
	private boolean testTableEvents(Record record) throws ServoyException
	{
		Table table = (Table)record.getParentFoundSet().getTable();
		FlattenedSolution solution = fsm.getApplication().getFlattenedSolution();
		Iterator<TableNode> tableNodes = solution.getTableNodes(table);
		while (tableNodes.hasNext())
		{
			TableNode tn = tableNodes.next();
			int methodId;
			String methodKey;
			if (record.existInDataSource())
			{
				methodId = tn.getOnUpdateMethodID();
				methodKey = "onUpdateMethodID"; //$NON-NLS-1$
			}
			else
			{
				methodId = tn.getOnInsertMethodID();
				methodKey = "onInsertMethodID"; //$NON-NLS-1$
			}
			if (methodId > 0)
			{
				ScriptMethod globalScriptMethod = solution.getScriptMethod(methodId);
				if (globalScriptMethod != null)
				{
					IExecutingEnviroment scriptEngine = fsm.getApplication().getScriptEngine();
					GlobalScope gscope = scriptEngine.getSolutionScope().getGlobalScope();
					Object function = gscope.get(globalScriptMethod.getName());
					if (function instanceof Function)
					{
						Object[] methodArgs = new Object[] { record };
						try
						{
							Object retval = scriptEngine.executeFunction(((Function)function), gscope, gscope, Utils.arrayMerge(methodArgs,
								Utils.parseJSExpressions(tn.getInstanceMethodArguments(methodKey))), false, true);
							if (Boolean.FALSE.equals(retval))
							{
								// update or insert method returned false. should block the save.
								return false;
							}
						}
						catch (JavaScriptException e)
						{
							// update or insert method threw exception.
							throw new DataException(ServoyException.RECORD_VALIDATION_FAILED, e.getValue());
						}
						catch (EcmaError e)
						{
							throw new ApplicationException(ServoyException.SAVE_FAILED, e);
						}
						catch (Exception e)
						{
							Debug.error(e);
							throw new ServoyException(ServoyException.SAVE_FAILED, new Object[] { e.getMessage() });
						}
					}
				}
			}
		}
		return true;
	}

	/*
	 * _____________________________________________________________ Methods for data manipulation
	 */
	private RowUpdateInfo getRecordUpdateInfo(IRecordInternal state) throws ServoyException
	{
		Table table = state.getParentFoundSet().getSQLSheet().getTable();
		RowManager rowManager = fsm.getRowManager(fsm.getDataSource(table));

		Row rowData = state.getRawData();
		boolean doesExistInDB = rowData.existInDB();
		if (doesExistInDB && !hasAccess(table, IRepository.UPDATE))
		{
			throw new ApplicationException(ServoyException.NO_MODIFY_ACCESS);
		}

		RowUpdateInfo rowUpdateInfo = rowManager.getRowUpdateInfo(rowData, hasAccess(table, IRepository.TRACKING));
		return rowUpdateInfo;
	}

	private boolean testIfRecordIsChanged(IRecordInternal record, boolean checkCalcValues)
	{
		Row rowData = record.getRawData();
		if (rowData == null) return false;

		if (checkCalcValues && record instanceof Record)
		{
			((Record)record).validateStoredCalculations();
		}

		return rowData.isChanged();
	}

	public void removeUnChangedRecords(boolean checkCalcValues, boolean doActualRemove)
	{
		if (preparingForSave) return;
		// Test the edited records if they are changed or not.
		Object[] editedRecordsArray = null;
		editRecordsLock.lock();
		try
		{
			editedRecordsArray = editedRecords.toArray();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		for (Object element : editedRecordsArray)
		{
			IRecordInternal record = (IRecordInternal)element;
			if (!testIfRecordIsChanged(record, checkCalcValues))
			{
				if (doActualRemove)
				{
					removeEditedRecord(record);
				}
				else
				{
					stopEditing(true, record);
				}
			}
		}
	}

	public void removeEditedRecord(IRecordInternal r)
	{
		int size;
		editRecordsLock.lock();
		try
		{
			editedRecords.remove(r);
			recordTested.remove(r);
			failedRecords.remove(r);
			size = editedRecords.size();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		if (size == 0)
		{
			fireEditChange();
		}
	}

	void removeEditedRecords(FoundSet set)
	{
		Object[] editedRecordsArray = null;
		editRecordsLock.lock();
		try
		{
			editedRecordsArray = editedRecords.toArray();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		for (Object element : editedRecordsArray)
		{
			IRecordInternal record = (IRecordInternal)element;
			if (record.getParentFoundSet() == set)
			{
				removeEditedRecord(record);
			}
		}
	}

	public boolean startEditing(IRecordInternal record, boolean mustFireEditRecordChange)
	{
		if (record == null)
		{
			throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.editNullRecord")); //$NON-NLS-1$
		}
		// only IRowListeners can go into edit.. (SubSummaryFoundSet records can't)
		if (!(record.getParentFoundSet() instanceof IRowListener)) return true;

		if (isEditing(record))
		{
			editRecordsLock.lock();
			try
			{
				recordTested.remove(record);
			}
			finally
			{
				editRecordsLock.unlock();
			}
			return true;
		}

		boolean isEditing = record instanceof FindState;
		if (!isEditing)
		{
			if (record.existInDataSource())
			{
				if (hasAccess(record.getParentFoundSet().getSQLSheet().getTable(), IRepository.UPDATE))
				{
					isEditing = !record.isLocked();// enable only if not locked by someone else
				}
				else
				{
					// TODO throw exception??
					//	Error handler ???
				}
			}
			else
			{
				if (hasAccess(record.getParentFoundSet().getSQLSheet().getTable(), IRepository.INSERT))
				{
					isEditing = true;
				}
				else
				{
					// TODO throw exception??
					// Error handler ???
				}
			}
		}
		if (isEditing)
		{
			if (mustFireEditRecordChange) isEditing = fireEditRecordStart(record);
			// find states also to the global foundset manager??
			if (isEditing)
			{
				int editRecordsSize = 0;
				editRecordsLock.lock();
				try
				{
					// editRecordStop should be called for this record to match the editRecordStop call
					recordTested.remove(record);

					// extra check if no other thread already added this record
					if (!editedRecords.contains(record))
					{
						editedRecords.add(record);
						editRecordsSize = editedRecords.size();
					}
					failedRecords.remove(record);
					// reset the exception so that it is tried again.
					record.getRawData().setLastException(null);
				}
				finally
				{
					editRecordsLock.unlock();
				}
				if (editRecordsSize == 1)
				{
					fireEditChange();
				}
			}
		}
		return isEditing;
	}

	/**
	 * 
	 */
	public void clearSecuritySettings()
	{
		accessMap.clear();
	}

	public boolean hasAccess(Table table, int flag)
	{
		if (table == null)
		{
			// no table, cannot insert/update/delete
			return (flag & (IRepository.INSERT | IRepository.UPDATE | IRepository.DELETE)) == 0;
		}
		Integer access = accessMap.get(table);
		if (access == null)
		{
			try
			{
				Iterator<String> it = table.getRowIdentColumnNames();
				if (it.hasNext())
				{
					String cname = it.next();
					//access is based on columns, but we don't yet use that fine grained level, its table only
					access = new Integer(fsm.getApplication().getFlattenedSolution().getSecurityAccess(
						Utils.getDotQualitfied(table.getServerName(), table.getName(), cname)));
				}
				else
				{
					access = new Integer(-2);
				}
				accessMap.put(table, access);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return false;
			}
		}
		if (access.intValue() == -1 && flag == IRepository.TRACKING) //deny tracking if security not is specified
		{
			return false;
		}
		return ((access.intValue() & flag) != 0);
	}

	protected boolean fireEditRecordStart(final IRecordInternal record)
	{
		Object[] array = prepareForSaveListeners.toArray();
		for (Object element : array)
		{
			IPrepareForSave listner = (IPrepareForSave)element;
			if (!listner.recordEditStart(record))
			{
				return false;
			}
		}
		return true;
	}

	protected void fireEditChange()
	{
		int editRecordsSize = 0;
		int failedRecordsSize = 0;
		editRecordsLock.lock();
		try
		{
			editRecordsSize = editedRecords.size();
			failedRecordsSize = failedRecords.size();
		}
		finally
		{
			editRecordsLock.unlock();
		}

		GlobalEditEvent e = new GlobalEditEvent(this, editRecordsSize > 0 || failedRecordsSize > 0);
		Object[] array = editListeners.toArray();
		for (Object element : array)
		{
			IGlobalEditListener listner = (IGlobalEditListener)element;
			listner.editChange(e);
		}

		if (editRecordsSize == 0)
		{
			fsm.performActionIfRequired();
		}
	}

	public synchronized Map<FoundSet, int[]> getFoundsetEventMap()
	{
		if (fsEventMap == null)
		{
			fsEventMap = new ConcurrentHashMap<FoundSet, int[]>();
		}
		return fsEventMap;
	}

	public void fireEvents()
	{
		Map<FoundSet, int[]> map = null;
		synchronized (this)
		{
			if (fsEventMap == null || isSaving) return;
			map = fsEventMap;
			fsEventMap = null;
		}
		Iterator<Entry<FoundSet, int[]>> it = map.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<FoundSet, int[]> entry = it.next();
			int[] indexen = entry.getValue();
			FoundSet fs = entry.getKey();
			fs.fireFoundSetEvent(indexen[0], indexen[1], FoundSetEvent.CHANGE_UPDATE);
		}
		// call until the map is null..
		fireEvents();
	}

	public void addEditListener(IGlobalEditListener editListener)
	{
		editListeners.add(editListener);
	}

	public void removeEditListener(IGlobalEditListener editListener)
	{
		editListeners.remove(editListener);
	}

	protected boolean firePrepareForSave(boolean looseFocus)
	{
		Object[] array = prepareForSaveListeners.toArray();
		for (Object element : array)
		{
			IPrepareForSave listner = (IPrepareForSave)element;
			if (!listner.prepareForSave(looseFocus))
			{
				return false;
			}
		}
		return true;
	}

	public void addPrepareForSave(IPrepareForSave prepareForSave)
	{
		prepareForSaveListeners.add(prepareForSave);
	}

	public void removePrepareForSave(IPrepareForSave prepareForSave)
	{
		prepareForSaveListeners.remove(prepareForSave);
	}

	/**
	 * @param autoSave
	 */
	public boolean setAutoSave(boolean autoSave)
	{
		if (this.autoSave != autoSave)
		{
			if (autoSave)
			{
				if (stopEditing(true) != ISaveConstants.STOPPED)
				{
					return false;
				}
			}
			this.autoSave = autoSave;
		}
		return true;
	}

	public boolean getAutoSave()
	{
		return autoSave;
	}

	public void rollbackRecords()
	{
		ArrayList<IRecordInternal> array = new ArrayList<IRecordInternal>();
		editRecordsLock.lock();
		try
		{
			recordTested.clear();
			array.addAll(failedRecords);
			array.addAll(editedRecords);
		}
		finally
		{
			editRecordsLock.unlock();
		}
		rollbackRecords(array);
	}

	public void rollbackRecords(List<IRecordInternal> records)
	{
		ArrayList<IRecordInternal> array = new ArrayList<IRecordInternal>();
		editRecordsLock.lock();
		try
		{
			for (IRecordInternal record : records)
			{
				recordTested.remove(record);
				if (failedRecords.remove(record)) array.add(record);
				if (editedRecords.contains(record)) array.add(record);
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		if (array.size() > 0)
		{
			// The "existsInDB" property sometimes changes while iterating over the array
			// below (for example when we have several Records that point to the same Row
			// and the Row is not yet stored in the database). So we memorize the initial
			// values of "existsInDB" and we use them while iterating over the array.
			boolean[] existsInDB = new boolean[array.size()];
			for (int i = 0; i < array.size(); i++)
			{
				existsInDB[i] = array.get(i).existInDataSource();
			}

			for (int i = 0; i < array.size(); i++)
			{
				IRecordInternal element = array.get(i);

				// TODO all fires in rollback should be accumulated and done here at once.

				element.getRawData().rollbackFromOldValues();//we also rollback !existsInDB records, since they can be held in variables 
				if (!existsInDB[i])
				{
					int index = element.getParentFoundSet().getRecordIndex(element);
					if (index != -1)
					{
						try
						{
							((FoundSet)element.getParentFoundSet()).deleteRecord(index);
						}
						catch (Exception e)
						{
							Debug.error("Error rollbacking new editting record", e); //$NON-NLS-1$
						}
					}
				}
			}
			editRecordsLock.lock();
			try
			{
				if (editedRecords.size() > 0)
				{
					editedRecords.removeAll(array);
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
			fireEditChange();
		}
	}

	public IRecordInternal[] getEditedRecords()
	{
		removeUnChangedRecords(true, false);
		editRecordsLock.lock();
		try
		{
			return editedRecords.toArray(new IRecordInternal[editedRecords.size()]);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @param set
	 * @return
	 */
	public IRecordInternal[] getUnmarkedEditedRecords(FoundSet set)
	{
		List<IRecordInternal> al = new ArrayList<IRecordInternal>();
		editRecordsLock.lock();
		try
		{
			for (int i = editedRecords.size(); --i >= 0;)
			{
				IRecordInternal record = editedRecords.get(i);
				if (record.getParentFoundSet() == set && !recordTested.contains(record))
				{
					al.add(record);
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return al.toArray(new IRecordInternal[al.size()]);
	}


	/**
	 * @param record
	 */
	public void markRecordTested(IRecordInternal record)
	{
		editRecordsLock.lock();
		try
		{
			if (!recordTested.contains(record))
			{
				recordTested.add(record);
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	public void init()
	{
		editRecordsLock.lock();
		try
		{
			accessMap.clear();//per table (could be per column in future)
			autoSave = true;
			preparingForSave = false;
			isSaving = false;

			editedRecords.clear();
			failedRecords.clear();
			recordTested.clear();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		fireEditChange();
	}

	/**
	 * This iterator will iterate over a list even if the list is modified during iteration. It is not thread-safe. <BR>
	 * <BR>
	 * The iterator will return every Object in the List (including null if contained) exactly one time (even if it is contained by more that one location in
	 * the list). If the list is modified during iteration, the iterator will also return the new objects in the list. <BR>
	 * <BR>
	 * All operations on this iterator run in linear time, roughly speaking.
	 * 
	 * @author acostescu
	 */
	private class AllowListModificationIterator<T> implements Iterator<T>
	{

		private final Map<T, Boolean> alreadyProcessed = new HashMap<T, Boolean>();
		private T elementReturnedByNext;
		private boolean nextReturned = false;
		private final List<T> list;

		/**
		 * Creates a new instance that iterates over the specified list.
		 * 
		 * @param list the list to be used for iteration.
		 */
		public AllowListModificationIterator(List<T> list)
		{
			this.list = list;
		}

		public boolean hasNext()
		{
			boolean has = false;
			int size = list.size();
			for (int i = 0; i < size; i++)
			{
				if (!alreadyProcessed.containsKey(list.get(i)))
				{
					has = true;
					break;
				}
			}

			return has;
		}

		public T next()
		{
			int size = list.size();
			for (int i = 0; i < size; i++)
			{
				elementReturnedByNext = list.get(i);
				if (!alreadyProcessed.containsKey(elementReturnedByNext))
				{
					alreadyProcessed.put(elementReturnedByNext, Boolean.TRUE);
					nextReturned = true;
					return elementReturnedByNext;
				}
			}

			nextReturned = false;
			throw new NoSuchElementException();
		}

		public void remove()
		{
			if (nextReturned)
			{
				list.remove(elementReturnedByNext);
			}
		}

	}

}