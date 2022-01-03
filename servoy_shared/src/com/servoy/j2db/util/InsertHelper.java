package com.servoy.j2db.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper class to insert stuff easily
 * @author jblok
 */
@SuppressWarnings("nls")
public class InsertHelper
{
	private final String tableName;
	private Map<String, Pair<Integer, Object>> columnData = new HashMap<String, Pair<Integer, Object>>();

	public InsertHelper(String a_tableName)
	{
		tableName = a_tableName;
	}

	/**
	 * @param connection
	 */
	public int insert(Connection connection) throws SQLException
	{
		int index = 1;
		String columns = createColumnsList();
		String questions = createQuestionsList();
		PreparedStatement st = connection.prepareStatement("insert into " + tableName + " (" + columns + ") values (" + questions + ")");
		Iterator<String> it = columnData.keySet().iterator();
		while (it.hasNext())
		{
			String name = it.next();
			Pair<Integer, Object> pair = columnData.get(name);
			st.setObject(index, pair.getRight(), (pair.getLeft()).intValue());
			index++;
		}
		int retval = st.executeUpdate();
		st.close();
		columnData = new HashMap<String, Pair<Integer, Object>>();
		return retval;
	}

	/**
	 * @param connection
	 */
	public Object insertReturnAutoGenKeys(Connection connection) throws SQLException
	{
		PreparedStatement st = null;
		ResultSet generatedKeys = null;
		try
		{
			int index = 1;
			String columns = createColumnsList();
			String questions = createQuestionsList();
			st = connection.prepareStatement("insert into " + tableName + " (" + columns + ") values (" + questions + ")",
				Statement.RETURN_GENERATED_KEYS);
			Iterator<String> it = columnData.keySet().iterator();
			while (it.hasNext())
			{
				String name = it.next();
				Pair<Integer, Object> pair = columnData.get(name);
				st.setObject(index, pair.getRight(), (pair.getLeft()).intValue());
				index++;
			}
			st.executeUpdate();
			generatedKeys = st.getGeneratedKeys();
			while (generatedKeys.next())
			{
				Object key = generatedKeys.getObject(1);
				// we expect to insert 1 row, so 1 generated key
				return key;
			}
		}
		finally
		{
			columnData = new HashMap<String, Pair<Integer, Object>>();
			if (st != null) st.close();
			if (generatedKeys != null) generatedKeys.close();
		}
		return null;
	}

	/**
	 * @return
	 */
	private String createQuestionsList()
	{
		StringBuffer sb = new StringBuffer();
		Iterator<String> it = columnData.keySet().iterator();
		while (it.hasNext())
		{
			it.next();
			sb.append("?");
			if (it.hasNext()) sb.append(",");
		}
		return sb.toString();
	}

	/**
	 * @return
	 */
	private String createColumnsList()
	{
		StringBuffer sb = new StringBuffer();
		Iterator<String> it = columnData.keySet().iterator();
		while (it.hasNext())
		{
			String name = it.next();
			sb.append(name);
			if (it.hasNext()) sb.append(",");
		}
		return sb.toString();
	}

	/**
	 * @param string
	 * @param class1
	 * @param communication_id
	 */
	public void addColumn(String cstring, int type, Object value)
	{
		columnData.put(cstring, new Pair<Integer, Object>(new Integer(type), value));
	}
}