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
package com.servoy.j2db.smart.plugins;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.IBaseConverter;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.IColumnValidator;
import com.servoy.j2db.dataprocessing.IColumnValidatorManager;
import com.servoy.j2db.dataprocessing.IConverterManager;
import com.servoy.j2db.dataprocessing.IUIConverter;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IColumnConverterProvider;
import com.servoy.j2db.plugins.IColumnValidatorProvider;
import com.servoy.j2db.plugins.IPlugin;
import com.servoy.j2db.plugins.IPluginManagerInternal;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.plugins.IServerPlugin;
import com.servoy.j2db.plugins.IUIConverterProvider;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ExtendableURLClassLoader;
import com.servoy.j2db.util.JarManager;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.keyword.Ident;

/**
 * Manages plugins for the application.
 * 
 * @author jcompagner, jblok
 */
public class PluginManager extends JarManager implements IPluginManagerInternal, PropertyChangeListener
{
	private static final String PLUGIN_CL_SUFFIX = " plugins"; //$NON-NLS-1$

	private final File pluginDir;
	private static ExtendableURLClassLoader _pluginsClassLoader;
	private final static Map<URL, Pair<String, Long>> supportLibUrls = new HashMap<URL, Pair<String, Long>>();
	private final static Map<URL, Pair<String, Long>> pluginUrls = new LinkedHashMap<URL, Pair<String, Long>>();

	private static Extension[] clientPluginInfo;

	// ---instance vars
	protected final Object[] initLock = new Object[1];//when filled with an Object the init is completed

	// ---plugin instances
	protected Map<String, IClientPlugin> loadedClientPlugins; //contains all instances of client plugins, (name -> instance)
	protected List<IServerPlugin> loadedServerPlugins;//contains all instances of server plugins
	private final ClassLoader lafLoader;

	/**
	 * Loads plugins from the plugins directory.
	 */
	public PluginManager(Object prop_change_source, ClassLoader lafLoader)
	{
		this(lafLoader);
		J2DBGlobals.addPropertyChangeListener(prop_change_source, this);
	}

	public PluginManager(ClassLoader lafLoader)
	{
		super();
		this.lafLoader = lafLoader;
		pluginDir = new File(Settings.getInstance().getProperty(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY) + File.separator + "plugins"); //$NON-NLS-1$ 
		if (pluginUrls.size() == 0 && pluginDir.isDirectory())
		{
			readDir(pluginDir, pluginUrls, supportLibUrls, null, false);
		}
	}

	public PluginManager(Map<URL, Pair<String, Long>> pluginUrls, Map<URL, Pair<String, Long>> supportLibUrls, ClassLoader lafLoader)
	{
		super();
		this.lafLoader = lafLoader;
		pluginDir = null;
		PluginManager.pluginUrls.putAll(pluginUrls);
		PluginManager.supportLibUrls.putAll(supportLibUrls);
		List<URL> allUrls = new ArrayList<URL>(supportLibUrls.size() + pluginUrls.size());
		allUrls.addAll(supportLibUrls.keySet());
		allUrls.addAll(pluginUrls.keySet());
		URL[] urls = allUrls.toArray(new URL[allUrls.size()]);
		PluginManager._pluginsClassLoader = new ExtendableURLClassLoader(urls, lafLoader != null ? lafLoader : getClass().getClassLoader(), PLUGIN_CL_SUFFIX);
	}

	public PluginManager(String pluginDirAsString, ClassLoader lafLoader)
	{
		super();
		this.lafLoader = lafLoader;
		pluginDir = new File(pluginDirAsString);
		if (pluginUrls.size() == 0 && this.pluginDir.isDirectory())
		{
			readDir(pluginDir, pluginUrls, supportLibUrls, null, false);
		}
	}

	public File getPluginDir()
	{
		return pluginDir;
	}

	/**
	 * Unload all plugins.
	 */
	public synchronized void flushCachedItems()
	{
		if (loadedClientPlugins != null)
		{
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					plugin.unload();
				}
				catch (Throwable th)
				{
					Debug.error("Error occured unloading client plugin: " + plugin.getName(), th); //$NON-NLS-1$ 
				}
			}
			loadedClientPlugins = null;
		}

		if (loadedServerPlugins != null)
		{
			for (int j = 0; j < loadedServerPlugins.size(); j++)
			{
				IPlugin plugin = loadedServerPlugins.get(j);
				try
				{
					plugin.unload();
				}
				catch (Throwable th)
				{
					Debug.error("Error ocured unloading server plugin: " + plugin.getClass().getName(), th); //$NON-NLS-1$ 
				}
			}
			loadedServerPlugins = null;
		}
	}

	/**
	 * NOTE: To be called only from ApplicationServer.dispose()
	 */
	public static void dispose()
	{
		pluginUrls.clear();
		supportLibUrls.clear();
		_pluginsClassLoader = null;
		clientPluginInfo = null;
	}

	public Extension[] loadClientPluginDefs()
	{
		if (clientPluginInfo == null)
		{
			List<Extension> clientPlugins = getExtensionsForClass(IClientPlugin.class);
			clientPluginInfo = clientPlugins.toArray(new Extension[0]);
		}
		return clientPluginInfo;
	}

	private List<Extension> getExtensionsForClass(Class searchClass)
	{
		Map<URL, Pair<String, Long>> notProcessedMap = new HashMap<URL, Pair<String, Long>>();
		List<Extension> extensions = new ArrayList<Extension>();
		Iterator<Map.Entry<URL, Pair<String, Long>>> it = pluginUrls.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<URL, Pair<String, Long>> entry = it.next();
			boolean foundAttribute = false;
			try
			{
				URL url = entry.getKey();
				JarFile file = new JarFile(new File(new URI(url.toExternalForm())));
				Manifest mf = file.getManifest();
				if (mf != null)
				{
					List<String> pluginClasses = JarManager.getClassNamesForKey(mf, SERVOY_PLUGIN_ATTRIBUTE);
					if (pluginClasses != null && pluginClasses.size() > 0)
					{
						foundAttribute = true;
						for (String className : pluginClasses)
						{
							Class cls = null;
							try
							{
								cls = getClassLoader().loadClass(className);
							}
							catch (Throwable th)
							{
								Debug.trace(th.toString());
							}
							if (cls != null)
							{
								if (searchClass.isAssignableFrom(cls))
								{
									Extension ext = new Extension();
									ext.jarFileName = entry.getValue().getLeft();
									ext.jarFileModTime = entry.getValue().getRight().longValue();
									ext.jarUrl = url;
									ext.instanceClass = cls;
									ext.searchType = searchClass;
									extensions.add(ext);
								}
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				foundAttribute = false;
			}
			if (!foundAttribute)
			{
				notProcessedMap.put(entry.getKey(), entry.getValue());
			}
		}
		if (notProcessedMap.size() > 0)
		{
			Extension[] additionalExtensions = getExtensions((ExtendableURLClassLoader)getClassLoader(), searchClass, notProcessedMap);
			if (additionalExtensions != null)
			{
				extensions.addAll(Arrays.asList(additionalExtensions));
			}
		}
		return extensions;
	}

	protected void checkIfInitialized()
	{
		synchronized (initLock)
		{
			if (initLock[0] == null)
			{
				try
				{
					initLock.wait(5 * 1000);//max 5 sec, if not copleted then something else is wrong
				}
				catch (InterruptedException e)
				{
					Debug.error(e);
				}
			}
		}
	}

	private void flagInitialized()
	{
		initLock[0] = new Object();//done
		initLock.notifyAll();
	}

	/**
	 * Note: load clients first
	 * 
	 * @return
	 */
	private Class<IServerPlugin>[] loadServerPluginDefs()
	{
		try
		{
			List<Extension> serverPlugins = getExtensionsForClass(IServerPlugin.class);
			List<Class<IServerPlugin>> classes = new ArrayList<Class<IServerPlugin>>();
			for (Extension element : serverPlugins)
			{
				classes.add((Class<IServerPlugin>)element.instanceClass);
			}
			return classes.toArray(new Class[0]);
		}
		catch (Throwable th)
		{
			Debug.error("Error occured retrieving server plugins", th); //$NON-NLS-1$
			return null;
		}
	}

	public void init()
	{
		//ignore 
	}

	//should only be called by app server
	public void initServerPlugins(IServerAccess app)
	{
		synchronized (initLock)
		{
			if (loadedServerPlugins == null)
			{
				loadedServerPlugins = new ArrayList<IServerPlugin>();

				Class<IServerPlugin>[] classes = loadServerPluginDefs();
				for (Class<IServerPlugin> element : classes)
				{
					try
					{
						IServerPlugin plugin = loadServerPlugin(element);
						if (plugin != null)
						{
							long now = System.currentTimeMillis();
							plugin.initialize(app);
							Debug.trace("Plugin " + element.getName() + " initialised in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							loadedServerPlugins.add(plugin);
						}
					}
					catch (Throwable th)
					{
						Debug.error("Error occured loading server plugin class " + element.getName(), th); //$NON-NLS-1$ 
					}
				}

				flagInitialized();
			}
		}
	}

	/**
	 * Initialize plugins.
	 */
	public void initClientPlugins(IApplication application, IClientPluginAccess app)
	{
		synchronized (initLock)
		{
			loadClientPlugins(application);
			initClientPluginsEx(app);
		}
	}

	/**
	 * Only load client plugins; do not initialize them.
	 */
	public void loadClientPlugins(IApplication application)
	{
		synchronized (initLock)
		{
			loadedClientPlugins = new HashMap<String, IClientPlugin>();
			if (application == null || !application.isRunningRemote()) //Servoy developer loading
			{
				Extension[] exts = loadClientPluginDefs();
				if (exts != null)
				{
					for (Extension element : exts)
					{
						try
						{
							loadClientPlugin((Class<IClientPlugin>)element.instanceClass);
						}
						catch (Throwable th)
						{
							Debug.error("Error occured loading client plugin class " + element.instanceClass.getName(), th); //$NON-NLS-1$ 
						}
					}
				}
			}
			else
			//Servoy client loading
			{
				int count = 0;
				Settings settings = Settings.getInstance();
				String sCount = settings.getProperty("com.servoy.j2db.plugins.PluginManager.PluginCount"); //$NON-NLS-1$
				if (sCount != null)
				{
					count = new Integer(sCount).intValue();
				}
				Debug.trace("plugin count " + count); //$NON-NLS-1$

				for (int x = 0; x < count; x++)
				{
					String clazzName = settings.getProperty("com.servoy.j2db.plugins.PluginManager.Plugin." + x + ".className"); //$NON-NLS-1$ //$NON-NLS-2$
					if (clazzName != null && clazzName.length() != 0)
					{
						try
						{
							loadClientPlugin((Class<IClientPlugin>)Class.forName(clazzName.trim()));
						}
						catch (Throwable th)
						{
							Debug.error("Error occured loading client plugin class " + clazzName, th); //$NON-NLS-1$ 
						}
					}
				}
			}

			columnConverterManager = new ConverterManager<IColumnConverter>();
			uiConverterManager = new ConverterManager<IUIConverter>();
			columnValidatorManager = new ColumnValidatorManager();
			checkForConvertersAndValidators(columnConverterManager, uiConverterManager, columnValidatorManager);
		}
	}

	private ConverterManager<IColumnConverter> columnConverterManager;
	private ConverterManager<IUIConverter> uiConverterManager;
	private IColumnValidatorManager columnValidatorManager;

	public static class ConverterManager<T extends IBaseConverter> implements IConverterManager<T>
	{
		private final Map<String, T> converters = new HashMap<String, T>();//name -> converter

		public T getConverter(String name)
		{
			return converters.get(name);
		}

		public void registerConvertor(T converter)
		{
			Object obj = converters.put(converter.getName(), converter);
			if (obj != null) Debug.log("Duplicate converter found: " + converter.getName()); //$NON-NLS-1$
		}

		public Map<String, T> getConverters()
		{
			return converters;
		}
	}

	public static class ColumnValidatorManager implements IColumnValidatorManager
	{
		private final Map<String, IColumnValidator> validators = new HashMap<String, IColumnValidator>();//name -> validator

		public void registerValidator(IColumnValidator validator)
		{
			Object obj = validators.put(validator.getName(), validator);
			if (obj != null) Debug.log("Duplicate validator found: " + validator.getName()); //$NON-NLS-1$
		}

		public Map<String, IColumnValidator> getValidators()
		{
			return validators;
		}


		public IColumnValidator getValidator(String name)
		{
			return validators.get(name);
		}

	}

	public IConverterManager<IColumnConverter> getColumnConverterManager()
	{
		return columnConverterManager;
	}

	public IConverterManager<IUIConverter> getUIConverterManager()
	{
		return uiConverterManager;
	}

	public IColumnValidatorManager getColumnValidatorManager()
	{
		return columnValidatorManager;
	}

	private void initClientPluginsEx(IClientPluginAccess app)
	{
		synchronized (initLock)
		{
			// we assume that the plugins are already loaded
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					long now = System.currentTimeMillis();
					plugin.initialize(app);
//					if (plugin instanceof IAgent)
//					{
//						IApplication base = ((ClientPluginAccessProvider)app).getApplication();
//						if (base instanceof J2DBClient)
//						{
//							((J2DBClient)base).setAgent((IAgent)plugin);
//						}
//					}
					Debug.trace("Plugin " + plugin.getName() + " initialised in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				catch (Throwable th)
				{
					Debug.error("Error ocured initializing plugin: " + plugin.getName(), th); //$NON-NLS-1$
				}
			}

			flagInitialized();
		}
	}

	private void checkForConvertersAndValidators(IConverterManager<IColumnConverter> columnConverterMgr, IConverterManager<IUIConverter> uiConverterMgr,
		IColumnValidatorManager validatorManager)
	{
		synchronized (initLock)
		{
			// we assume that the plugins are already loaded
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					long now = System.currentTimeMillis();
					if (plugin instanceof IColumnConverterProvider && columnConverterMgr != null)
					{
						IColumnConverter[] cons = ((IColumnConverterProvider)plugin).getColumnConverters();
						if (cons != null)
						{
							for (IColumnConverter element2 : cons)
							{
								if (element2 != null)
								{
									columnConverterMgr.registerConvertor(element2);
								}
							}
						}
					}
					if (plugin instanceof IUIConverterProvider && uiConverterMgr != null)
					{
						IUIConverter[] cons = ((IUIConverterProvider)plugin).getUIConverters();
						if (cons != null)
						{
							for (IUIConverter element2 : cons)
							{
								if (element2 != null)
								{
									uiConverterMgr.registerConvertor(element2);
								}
							}
						}
					}
					if (plugin instanceof IColumnValidatorProvider && validatorManager != null)
					{
						IColumnValidator[] vals = ((IColumnValidatorProvider)plugin).getColumnValidators();
						if (vals != null)
						{
							for (IColumnValidator element2 : vals)
							{
								if (element2 != null)
								{
									validatorManager.registerValidator(element2);
								}
							}
						}
					}
					Debug.trace("Plugin " + plugin.getName() + " checked for converter/validator in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				catch (Throwable th)
				{
					Debug.error("Error ocured checking plugin: " + plugin.getName(), th); //$NON-NLS-1$
				}
			}
		}
	}

	private IClientPlugin loadClientPlugin(Class<IClientPlugin> pluginClass)
	{
		try
		{
			long now = System.currentTimeMillis();
			IClientPlugin plugin = pluginClass.newInstance();
			if (validatePlugin(plugin))
			{
				plugin.load();
				String name = plugin.getName();
				if (!Ident.checkIfKeyword(name))
				{
					loadedClientPlugins.put(name, plugin);
					Debug.trace("Plugin " + name + " loaded in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					return plugin;
				}
				else
				{
					Debug.error("Error occured loading client plugin, name is reserved word " + name); //$NON-NLS-1$
				}
			}
		}
		catch (Throwable th)
		{
			Debug.error("Error occured loading client class " + pluginClass.getName() + " from plugin.", th); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	private IServerPlugin loadServerPlugin(Class<IServerPlugin> pluginClass)
	{
		try
		{
			long now = System.currentTimeMillis();
			IServerPlugin plugin = pluginClass.newInstance();
			plugin.load();
			Debug.trace("Plugin " + pluginClass.getName() + " loaded in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return plugin;
		}
		catch (Throwable th)
		{
			Debug.error("Error occured loading server class " + pluginClass.getName() + " from plugin", th); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	public IClientPlugin getClientPlugin(String name)
	{
		checkIfInitialized();
		synchronized (initLock)
		{
			return loadedClientPlugins.get(name);
		}
	}

	private List< ? extends IPlugin> getClientPlugins()
	{
		checkIfInitialized();
		synchronized (initLock)
		{
			IClientPlugin[] list = loadedClientPlugins.values().toArray(new IClientPlugin[loadedClientPlugins.size()]);
			Arrays.sort(list, NameComparator.INSTANCE);
			return Arrays.asList(list);
		}
	}

	public List<IServerPlugin> getServerPlugins()
	{
		checkIfInitialized();
		synchronized (initLock)
		{
			return loadedServerPlugins;
		}
	}

	private boolean validatePlugin(IClientPlugin plugin)
	{
		String name = plugin.getName();
		if (name == null || name.trim().length() == 0)
		{
			Debug.error("Plugin " + plugin.getClass().getName() + " doesn't return a valid getName()"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}

		if (loadedClientPlugins.get(name) != null)
		{
			Debug.error("A Plugin with the internal name " + name + " has already been loaded"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		synchronized (initLock)
		{
			if (IPlugin.PROPERTY_SOLUTION.equals(evt.getPropertyName()) || IPlugin.PROPERTY_LOCALE.equals(evt.getPropertyName()) ||
				IPlugin.PROPERTY_CURRENT_WINDOW.equals(evt.getPropertyName()))
			{
				if (loadedClientPlugins != null)
				{
					Object[] list = loadedClientPlugins.values().toArray();
					for (Object element : list)
					{
						IClientPlugin plugin = (IClientPlugin)element;
						try
						{
							plugin.propertyChange(evt);
						}
						catch (Throwable e)//incase method is missing in old plugin or the plugin designer did something stupid
						{
							Debug.error("Error occured informing client plugin " + plugin.getName(), e); //$NON-NLS-1$ 
						}
					}
				}
			}
		}
	}

	/**
	 * Get the classloader (normally system classloader).
	 * 
	 * @return ClassLoader
	 */
	public synchronized ClassLoader getClassLoader()
	{
		if (_pluginsClassLoader == null)
		{
			try
			{
				if (pluginUrls.size() == 0 && pluginDir.isDirectory())
				{
					readDir(pluginDir, pluginUrls, supportLibUrls, null, false);
				}

				List<URL> allUrls = new ArrayList<URL>(supportLibUrls.size() + pluginUrls.size());
				allUrls.addAll(supportLibUrls.keySet());
				allUrls.addAll(pluginUrls.keySet());
				URL[] urls = allUrls.toArray(new URL[allUrls.size()]);
				_pluginsClassLoader = new ExtendableURLClassLoader(urls, lafLoader != null ? lafLoader : getClass().getClassLoader(), PLUGIN_CL_SUFFIX);
			}
			catch (Throwable th)
			{
				Debug.error("Error occured retrieving plugins. No plugins have been loaded", th); //$NON-NLS-1$
				return null;
			}
		}
		return _pluginsClassLoader;
	}

	/**
	 * Load plugins. Load all plugin jars into class loader.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public ClassLoader getPluginClassLoader()
	{
		return getClassLoader();
	}

	/**
	 * @return
	 */
	public PluginManager createEfficientCopy(Object prop_change_source)
	{
		PluginManager retval = new PluginManager(prop_change_source, lafLoader);
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.plugins.IPluginManager#getPlugin(java.lang.Class, java.lang.String)
	 */
	public <T extends IPlugin> T getPlugin(Class<T> pluginSubType, String name)
	{
		if (IClientPlugin.class.equals(pluginSubType))
		{
			return (T)getClientPlugin(name);
		}
		if (IServerPlugin.class.equals(pluginSubType))
		{
			return null;//nyi
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.plugins.IPluginManager#getPlugins(java.lang.Class)
	 */
	public <T extends IPlugin> List<T> getPlugins(Class<T> pluginSubType)
	{
		if (IClientPlugin.class.equals(pluginSubType))
		{
			return (List<T>)getClientPlugins();
		}
		if (IServerPlugin.class.equals(pluginSubType))
		{
			return (List<T>)getServerPlugins();
		}
		return null;
	}
}