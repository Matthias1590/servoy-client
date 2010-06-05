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


import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.server.annotations.TerracottaAutolockRead;
import com.servoy.j2db.server.annotations.TerracottaAutolockWrite;
import com.servoy.j2db.server.annotations.TerracottaInstrumentedClass;
import com.servoy.j2db.server.annotations.TerracottaTransient;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SerializableObject;

/**
 * @author sebster
 */
@TerracottaInstrumentedClass
public final class ClientInfo implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final int SERVER = 1;
	public static final int CLIENT = 2; //smart, rich
	public static final int HEADLESS_CLIENT = 4;
	public static final int WEB_CLIENT = 5;
	public static final int RUNTIME = 6;
	public static final int OFFLINE = 7;
	public static final int TYPES_COUNT = 8;

	private String clientId;
	private String hostIdentifier;
	private String hostName;
	private String hostAddress;
	private int hostPort = -1;
	private int applicationType;

	private String specialClientIndentifier;

	private String userUid;
	private String userName;

	// last successful authentication
	private String authenticatorType;
	private String authenticatorMethod;
	private String jsCredentials;


	@TerracottaTransient
	private TimeZone timeZone;

	private transient long loginTimestamp = 0;
	private transient long openSolutionTimestamp = 0;
	private transient long idleTimestamp = 0;
	private transient String[] groups;

	private int openSolutionId = -1;
	private List<String> infos = new ArrayList<String>();//to make it possible for developer to give a client a meaning full name/description in the admin page

	private boolean dontBlockDuringMaintenance;

	/**
	 * A private lock to synchronize read/write to the long valued properties. Since this lock is PRIVATE, all deadlock related issues are local to this class.
	 * Since the methods consist of 1 statement, the lock is always given up immediately and no deadlock on this lock can occur. The lock must be a serializable
	 * object because ClientInfo must be serializable, and the lock must always exist (and cannot be transient).
	 * 
	 * The lock is also used for Terracotta (you need to lock on stuff that changes if you want changes to be broadcasted in the cluster).
	 */
	private final SerializableObject lock = new SerializableObject();

	public ClientInfo(ClientState state)
	{
		//security check: only allow ClientInfo to be made with valid ClientState instances
		if (state == null)
		{
			throw new IllegalStateException();
		}
		initHostInfo();
	}

	@TerracottaAutolockWrite
	public void addInfo(String info)
	{
		synchronized (lock)
		{
			if (!infos.contains(info)) infos.add(info);
		}
	}

	@TerracottaAutolockWrite
	public void removeAllInfo()
	{
		synchronized (lock)
		{
			infos.clear();
		}
	}

	@TerracottaAutolockWrite
	public void setInfos(List<String> infos)
	{
		synchronized (lock)
		{
			this.infos = infos;
		}
	}

	@TerracottaAutolockRead
	public List<String> getInfos()
	{
		synchronized (lock)
		{
			return infos;
		}
	}

	@TerracottaAutolockWrite
	public void setClientId(String clientId)
	{
		synchronized (lock)
		{
			this.clientId = clientId;
		}
	}

	@TerracottaAutolockRead
	public String getClientId()
	{
		synchronized (lock)
		{
			return clientId;
		}
	}

	@TerracottaAutolockWrite
	public void setHostIdentifier(String hostIdentifier)
	{
		synchronized (lock)
		{
			this.hostIdentifier = hostIdentifier;
		}
	}

	@TerracottaAutolockRead
	public String getHostIdentifier()
	{
		synchronized (lock)
		{
			return hostIdentifier;
		}
	}

	@TerracottaAutolockWrite
	public void setUserUid(String userUid)
	{
		synchronized (lock)
		{
			this.userUid = userUid;
		}
	}

	@TerracottaAutolockRead
	public String getUserUid()
	{
		synchronized (lock)
		{
			return userUid;
		}
	}

	/**
	 * Returns the hostName.
	 * 
	 * @return String
	 */
	@TerracottaAutolockRead
	public String getHostName()
	{
		synchronized (lock)
		{
			return hostName;
		}
	}

	/**
	 * Returns the hostAddress.
	 * 
	 * @return String
	 */
	@TerracottaAutolockRead
	public String getHostAddress()
	{
		synchronized (lock)
		{
			return hostAddress;
		}
	}

	/**
	 * Sets the hostName.
	 * 
	 * @param hostName The hostName to set
	 */
	@TerracottaAutolockWrite
	public void setHostName(String hostname)
	{
		synchronized (lock)
		{
			this.hostName = hostname;
		}
	}

	/**
	 * Sets the hostAddress.
	 * 
	 * @param hostAddress The hostAddress to set
	 */
	@TerracottaAutolockWrite
	public void setHostAddress(String ipAddress)
	{
		synchronized (lock)
		{
			this.hostAddress = ipAddress;
		}
	}

	/**
	 * @return the timeZone
	 */
	public TimeZone getTimeZone()
	{
		return timeZone;
	}

	/**
	 * @param timeZone the timeZone to set
	 */
	public void setTimeZone(TimeZone timeZone)
	{
		this.timeZone = timeZone;
	}

	/**
	 * Returns the userName.
	 * 
	 * @return String
	 */
	@TerracottaAutolockRead
	public String getUserName()
	{
		synchronized (lock)
		{
			return userName;
		}
	}

	/**
	 * Sets the userName.
	 * 
	 * @param userName The userName to set
	 */
	@TerracottaAutolockWrite
	public void setUserName(String userName)
	{
		synchronized (lock)
		{
			this.userName = userName;
		}
	}

	@TerracottaAutolockRead
	public boolean isDontBlockDuringMaintenance()
	{
		synchronized (lock)
		{
			return dontBlockDuringMaintenance;
		}
	}

	@TerracottaAutolockWrite
	public void setDontBlockDuringMaintenance(boolean dontBlockDuringMaintenance)
	{
		synchronized (lock)
		{
			this.dontBlockDuringMaintenance = dontBlockDuringMaintenance;
		}
	}

	@TerracottaAutolockRead
	public int getOpenSolutionId()
	{
		synchronized (lock)
		{
			return openSolutionId;
		}
	}

	@TerracottaAutolockWrite
	public void setOpenSolutionId(int openSolutionId)
	{
		synchronized (lock)
		{
			this.openSolutionId = openSolutionId;
		}
	}

	@TerracottaAutolockWrite
	public void setLoginTimestamp(long loginTimestamp)
	{
		synchronized (lock)
		{
			this.loginTimestamp = loginTimestamp;
		}
	}

	@TerracottaAutolockRead
	public long getLoginTimestamp()
	{
		synchronized (lock)
		{
			return loginTimestamp;
		}
	}

	@TerracottaAutolockWrite
	public void setOpenSolutionTimestamp(long openSolutionTimestamp)
	{
		synchronized (lock)
		{
			this.openSolutionTimestamp = openSolutionTimestamp;
		}
	}

	@TerracottaAutolockRead
	public long getOpenSolutionTimestamp()
	{
		synchronized (lock)
		{
			return openSolutionTimestamp;
		}
	}

	@TerracottaAutolockWrite
	public void setIdleTimestamp(long idleTimestamp)
	{
		synchronized (lock)
		{
			this.idleTimestamp = idleTimestamp;
		}
	}

	@TerracottaAutolockRead
	public long getIdleTimestamp()
	{
		synchronized (lock)
		{
			return idleTimestamp;
		}
	}

	@Override
	@TerracottaAutolockRead
	public String toString()
	{
		synchronized (lock)
		{
			StringBuffer buffer = new StringBuffer();

			// User part
			if (userName != null)
			{
				buffer.append(userName);
				buffer.append('@');
			}
			else if (userUid != null)
			{
				buffer.append('[');
				buffer.append(userUid);
				buffer.append(']');
				buffer.append('@');
			}

			// Host part
			if (hostName != null)
			{
				buffer.append(hostName);
				if (hostAddress != null)
				{
					buffer.append('[');
					buffer.append(hostAddress);
					if (hostPort != -1)
					{
						buffer.append(':');
						buffer.append(hostPort);
					}
					buffer.append(']');
				}
			}
			else if (hostAddress != null)
			{
				buffer.append(hostAddress);
			}
			else
			{
				buffer.append("unknown"); //$NON-NLS-1$
			}

			return buffer.toString();
		}
	}

	@TerracottaAutolockWrite
	public void initHostInfo()
	{
		timeZone = TimeZone.getDefault();
		synchronized (lock)
		{
			// TODO check a webclient never sets the timezone
			// so a webclient can always use the default of the server..
			try
			{
				InetAddress inetAddress = InetAddress.getLocalHost();
				hostName = inetAddress.getHostName();
				hostAddress = inetAddress.getHostAddress();
				hostIdentifier = hostName + '_' + hostAddress;
			}
			catch (UnknownHostException e)
			{
				hostIdentifier = "Failed " + new Date().getTime(); //$NON-NLS-1$
				Debug.log("Could not resolve local host: " + e.getMessage()); //$NON-NLS-1$
			}
			catch (Exception e)
			{
				hostIdentifier = "Failed " + new Date().getTime(); //$NON-NLS-1$
				Debug.error(e);
			}
		}
	}

	@TerracottaAutolockRead
	public String[] getUserGroups()
	{
		synchronized (lock)
		{
			return groups;
		}
	}

	@TerracottaAutolockWrite
	public void setUserGroups(String[] g)
	{
		synchronized (lock)
		{
			groups = g;
		}
	}

	/**
	 * 
	 */
	@TerracottaAutolockWrite
	public void clearUserInfo()
	{
		synchronized (lock)
		{
			userUid = null;
			groups = null;
			userName = null;
			authenticatorType = null;
			authenticatorMethod = null;
			jsCredentials = null;
		}
	}

	@TerracottaAutolockRead
	public int getApplicationType()
	{
		synchronized (lock)
		{
			return this.applicationType;
		}
	}

	@TerracottaAutolockWrite
	public void setApplicationType(int applicationType)
	{
		synchronized (lock)
		{
			this.applicationType = applicationType;
		}
	}

	/**
	 * @param specialClientIndentifier the specialClientIndentifier to set
	 */
	@TerracottaAutolockWrite
	public void setSpecialClientIndentifier(String specialClientIndentifier)
	{
		synchronized (lock)
		{
			this.specialClientIndentifier = specialClientIndentifier;
		}
	}

	/**
	 * @return the specialClientIndentifier
	 */
	@TerracottaAutolockRead
	public String getSpecialClientIndentifier()
	{
		synchronized (lock)
		{
			return specialClientIndentifier;
		}
	}

	/**
	 * @param port
	 */
	@TerracottaAutolockWrite
	public void setHostPort(int port)
	{
		synchronized (lock)
		{
			this.hostPort = port;
		}
	}

	public void setLastAuthentication(String authenticatorType, String method, String jsCredentials)
	{
		synchronized (lock)
		{
			this.authenticatorType = authenticatorType;
			this.authenticatorMethod = method;
			this.jsCredentials = jsCredentials;
		}
	}

	public String getAuthenticatorType()
	{
		synchronized (lock)
		{
			return authenticatorType;
		}
	}

	public String getAuthenticatorMethod()
	{
		synchronized (lock)
		{
			return authenticatorMethod;
		}
	}

	public String getJsCredentials()
	{
		synchronized (lock)
		{
			return jsCredentials;
		}
	}

}
