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
package com.servoy.j2db.plugins;


import java.net.URL;
import java.util.List;

import com.servoy.j2db.IManager;

/**
 * Public interface for the plugin manager
 *
 * @author jblok
 */
public interface IPluginManager extends IManager
{
	/**
	 * Create a bean based from an classname.
	 *
	 * @param pluginSubType the class
	 * @param name the plugin name
	 * @return the instance
	 */
	public <T extends IPlugin> T getPlugin(Class<T> pluginSubType, String name);

	/**
	 * Get a list of all loaded plugins for a class type
	 *
	 * @param pluginSubType the class name
	 * @return instances
	 */
	public <T extends IPlugin> List<T> getPlugins(Class<T> pluginSubType);

	/**
	 * Get the plugin classloader
	 *
	 * @return ClassLoader
	 */
	public ClassLoader getClassLoader();

	/**
	 * Dynamically add a client plugin, only to be used from OSGi bundles
	 * @since 8.0
	 */
	public void addClientExtension(String clientPluginClassName, URL extension, URL[] supportLibs) throws PluginException;


	<T> T getPluginInstance(Class<T> pluginClass);
}
