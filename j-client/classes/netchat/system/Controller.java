package netchat.system;

import netchat.*;
import netchat.module.*;
import netchat.util.*;
import netchat.xml.*;

import org.eclipse.swt.widgets.Display;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.awt.event.*;

import javax.swing.JOptionPane;

/**
 * The Controller is the main backbone for the J-Client framework in that it not only houses all the server commands
 * and maps of loaded and unloaded modules, but also routes the majority of client traffic coming from the socket.
 * Incoming messages, after being parsed by the XMLParser into a XMLData object, are then sent to be handled by
 * the Controller, which ultimately decides what to do with the message.  If a message is a module message, the Controller
 * will find the module in the map of loaded modules, and route it to that module.  If it is a server message, the Controller
 * deals with it right there, using reflections to call the appropriate method for the message type.  For example, refer to
 * a mock authorize_module NetChat Protocol (NCP) message shown below:
 *<code>
 * <pre> &lt;message&gt;
 *   &lt;header&gt;
 *
 *      &lt;global&gt;
 *        &lt;properties type="servermessage"/&gt;
 *      &lt;/global&gt;
 *
 *      &lt;servermessage&gt;
 *        &lt;properties type="authorize_module"/&gt;
 *      &lt;/servermessage&gt;
 *
 *    &lt;/header&gt;
 *
 *    &lt;content&gt;
 *      &lt;name&gt;chat&lt;/name&gt;
 *      &lt;version&gt;0.1a&lt;/version&gt;
 *    &lt;/content&gt;
 * &lt;/message&gt;</pre>
 * </code>
 * <p>The Controller will look at header-global-properties and, by the fact that it's a server message, will use reflections on
 * iself (Controller class) to dynamically invoke the method serverCommand_authorize_module, passing it a stripped down message
 * consisting of the content tree.  The method then retrieves the name and version NCXMLElements and uses them to load the
 * module.
 *
 * <p>The Controller also houses the heartbeat monitor that monitors the connection by sending ping messages every 30 seconds
 * and awaiting the pongs in response [this may be moved to the ClientConnector at some point].  Overall, the Controller is
 * the main communications hub in the client.  All messages, whether from a module or from the socket, must pass through it,
 * and it gives access to all major componenets of the system.
 *
 * <p>By using reflections to load modules and intra-client route traffic, people can develop for the client and add their own
 * modules without ever having to change any code for the Controller or any other piece of the framework, making the client
 * very extensible.
 *
 * @author Andy Street, 2007
 */

public class Controller {
	
	static Controller currentController = null;
	
	//Basic NC components
	ClientConnector con;
	String username = null;
		
	//Command line args
	String hostname = null;
	int port = -1;
	boolean withSSL = true;
	boolean withXML = true;
	
	//Configuration 						//SHOULD THIS BE MOVED TO A CONF FILE? :D
	String confDir = "conf.d";
	String modulesConfFile = "modules.conf";
	String autoloaderConfFile = "autoloader.conf";
	String serversConfFile = "servers.conf";
	
	//Server and module maps
	HashMap<String, AbstractModule> modules = null;
	HashMap<String, HashMap<String, String[]>> serverMessageTemplates = null;
	HashMap<String, Class<AbstractModule>> moduleClassnameMap = null;
	HashMap<String, Boolean> moduleAuthorizationMap = null;
	HashMap<String, ArrayList<String>> modulesAwaitingDependencies = null;
	HashMap<String, ArrayList<String>> partialDependencies = null;
	
	//Autoloader
	ArrayList<String> autoloader = null;
	
	//Module authorization
	ArrayList<String> modulesAwaitingAuth = null;
	
	//Module Listeners
	ArrayList<ModuleListener> moduleListeners = null;
	
	//Server Queue
	ArrayList<String> servers = null;
	
	//Heartbeat
	javax.swing.Timer heartbeatMonitor = null;
	boolean initiatesBeat = true;
	boolean heartbeatAlive = true;
	boolean skippedBeat = false;
	double secondsBetweenPulses = 30;
	
	//Shutdown
	boolean shuttingDown = false;
	
	//Event Thread
	Display display = null;
	Thread readAndDispatchThread = null;
	
	//Properties
	private Properties properties;
	private String propsFile = ".ncprops";
	
	private boolean firstLogin = true;
	
	/**
	 * Default constructor.
	 * @param hn the hostname the client is to connect to
	 * @param p the port the client is to connect to
	 * @param ssl whether or not ssl is being used
	 * @param xml whether or not xml is being used
	 */
	public Controller(String hn, int p, boolean ssl, boolean xml)
	{
		Debug.println("Creating Controller... Welcome to NetChat!", 2);
		hostname = hn;
		port = p;
		withSSL = ssl;
		withXML = xml;
		
		currentController = this;
		
		loadProperties();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread()));
		
		modules = new HashMap<String, AbstractModule>();
		serverMessageTemplates = new HashMap<String, HashMap<String, String[]>>();
		autoloader = new ArrayList<String>();
		moduleClassnameMap = new HashMap<String, Class<AbstractModule>>();
		modulesAwaitingAuth = new ArrayList<String>();
		servers = new ArrayList<String>();
		moduleListeners = new ArrayList<ModuleListener>();
		moduleAuthorizationMap = new HashMap<String, Boolean>();
		modulesAwaitingDependencies = new HashMap<String, ArrayList<String>>();
		partialDependencies = new HashMap<String, ArrayList<String>>();
		
		Debug.println("Initiating SWT Event Thread...", 2);
		readAndDispatchThread = new Thread(new DisplayReadAndDispatchRunnable());
		readAndDispatchThread.start();
		
		populateServersQueue();
		populateServerMessageTemplates();
		populateModuleClassnameMap();
		populateAutoloader();
		
		Debug.println("Creating heartbeat monitor...", 2);
		heartbeatMonitor = new javax.swing.Timer((int)(secondsBetweenPulses * 1000), new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(heartbeatAlive)
					heartbeatAlive = false;
				else
				{
					if(skippedBeat)
					{
						connectionLost();
					}
					else
					{
						Debug.println("Server skipped a beat...", 0);
						skippedBeat = true;
					}
				}
				
				if(initiatesBeat)
					sendPing();
			}
		});
	}
	
	/**
	 * Initializes the ClientConnector associated with the Controller and calls startup
	 * @see #startup()
	 */
	void init()
	{
		initClientConnector();
	}
	
	/**
	 * Initializes the ClientConnector associated with the Controller.
	 * @see ClientConnector
	 */
	private void initClientConnector()
	{
		Debug.println("Initializing Connector...", 2);
		
		con = new ClientConnector(this);
		con.init(hostname, servers, port, withSSL, withXML);
		
		Debug.println("Connector successfully initiated.", 2);
	}
	
	void connectionCreated()
	{
		Debug.println("Starting heartbeat monitor...", 2);
		heartbeatMonitor.start();
		startup();
	}
	
	/**
	 * Loads NetChat properties
	 */
	private void loadProperties()
	{
		properties = new Properties();
		
		try
		{
			FileInputStream in = new FileInputStream(propsFile);
			properties.load(in);
			in.close();
		}
		catch(FileNotFoundException e)
		{
			Debug.println("Could not find properties file.  A new one will be created on shutdown.", 3);
			Debug.println("Properties not loaded.", 2);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Saves NetChat properties
	 * @param comment the comment to store with the file.  If null, it will default to the current time.
	 */
	public void saveProperties(String comment)
	{
		if(comment == null)
			comment = new Date(System.currentTimeMillis()).toString();
		
		Debug.println("Saving NetChat properties...", 2);
		
		try
		{
			FileOutputStream out = new FileOutputStream(propsFile);
			properties.store(out, comment);
			out.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Called when the heartbeat monitor detects that the connection has died.  Calls ClientConnector.cleanUp()
	 * to dispose of the socket.
	 */
	private void connectionLost()
	{
		Debug.println("Connection lost!", 0);
		
		Debug.println("Stopping heartbeat monitor...", 1);
		heartbeatMonitor.stop();
		
		con.cleanUp();
		
		disassociate(false);
		
		JOptionPane.showMessageDialog(null,
				"NetChat has lost its connection to the server.  It will now try to reconnect...", "Connection Lost", JOptionPane.ERROR_MESSAGE);
		
		Debug.println("Attempting to reconnect...", 2);
		con.findHost();
	}
	
	/**
	 * Sends a server message to the server to signout and unloads all modules
	 */
	public void disassociate(boolean isConnected)
	{
		Debug.println("Disassociating from server...", 0);
		
		unloadModules();
		
		if(isConnected)
			sendMessageToSocket(generateModuleXML("login", "logout", null));
	}
	
	/**
	 * Unloads all modules
	 */
	public void unloadModules()
	{
		for(AbstractModule m : new Vector<AbstractModule>(modules.values()))
		{	
				Debug.println("Cleaning up " + m.getClass() + "...", 2);
				unloadModule(m);	
		}
		
		partialDependencies = new HashMap<String, ArrayList<String>>();
		modulesAwaitingDependencies = new HashMap<String, ArrayList<String>>();
	}
	
	/**
	 * Sends a server message to the server to signout, unloads all modules, and starts up the login module.
	 */
	public void signOut()
	{
		Debug.println("Signing out...", 0);
		
		firstLogin = false;
		disassociate(true);
		
		Debug.println("Restarting...", 0);
		
		startup();
	}
	
	/**
	 * Called when the shutdown hook detects a shutdown, unloads all modules, cleans up the socket and exits.
	 */
	public void shutDown()
	{
		Debug.println("Received shutdown signal, shutting down...", 0);
		
		shuttingDown = true;
		
		disassociate(true);
		
		if(con != null)
			con.cleanUp();
		
		saveProperties(null);
		
		Debug.println("Exiting...", 0);
	}
	
	/**
	 * Loads server names from the servers configuration file and command line args to create a queue the ClientConnector
	 * will go through while trying to find a server to connect to.  See [NETCHATHOME]/etc/servers.conf for more information.
	 */
	private void populateServersQueue()
	{
		Debug.println("Populating servers queue...", 2);
		
		Scanner in = null;
		
		try
		{
			in = new Scanner(new File(confDir + "/" + serversConfFile));
		}
		catch (FileNotFoundException e)
		{
			Debug.println("Servers conf file is missing or moved: ", 0);
			Debug.println("\tExpecting file to be at " + confDir + "/" + serversConfFile, 0);
			
			System.exit(-1);
		}
		
		while(in.hasNext())
		{
			String s = in.nextLine();
			
			int poundIndex = s.indexOf('#');
			if(poundIndex != -1)
				s = s.substring(0, poundIndex);
			
			if(s.equals(""))
				continue;
			
			Debug.println("Adding " + s + " to servers queue...", 2);
			
			servers.add(s);
		}
	}
	
	/**
	 * Populates a template database of server messages that can be sent to allow easy construction of the messages later.
	 */
	private void populateServerMessageTemplates()
	{
		Debug.println("Populating server message template database...", 2);
		
		Debug.println("Adding ping template...", 2);
		
		HashMap<String, String[]> pingTemplate = new HashMap<String, String[]>(); //ping Template
		pingTemplate.put("globalproperties", new String[] {"type=\"servermessage\""});
		pingTemplate.put("messageproperties", new String[] {"type=\"ping\""});
		
		serverMessageTemplates.put("ping", pingTemplate);
		
		Debug.println("Adding pong template...", 2);
		
		HashMap<String, String[]> pongTemplate = new HashMap<String, String[]>(); //pong Template
		pongTemplate.put("globalproperties", new String[] {"type=\"servermessage\""});
		pongTemplate.put("messageproperties", new String[] {"type=\"pong\""});
		
		serverMessageTemplates.put("pong", pongTemplate);
		
		Debug.println("Adding module_request template...", 2);
		
		HashMap<String, String[]> request_moduleTemplate = new HashMap<String, String[]>(); //mod_req Template
		request_moduleTemplate.put("globalproperties", new String[] {"type=\"servermessage\""});
		request_moduleTemplate.put("messageproperties", new String[] {"type=\"module_request\""});
		
		serverMessageTemplates.put("module_request", request_moduleTemplate);
		
		Debug.println("Adding echo template...", 2);
		
		HashMap<String, String[]> echoTemplate = new HashMap<String, String[]>(); //echo Template
		echoTemplate.put("globalproperties", new String[] {"type=\"servermessage\""});
		echoTemplate.put("messageproperties", new String[] {"type=\"echo\""});
		
		serverMessageTemplates.put("echo", echoTemplate);
		
		Debug.println("Adding module_unload template...", 2);
		
		HashMap<String, String[]> module_unloadTemplate = new HashMap<String, String[]>(); //module_unload Template
		module_unloadTemplate.put("globalproperties", new String[] {"type=\"servermessage\""});
		module_unloadTemplate.put("messageproperties", new String[] {"type=\"module_unload\""});
		
		serverMessageTemplates.put("module_unload", module_unloadTemplate);
	}
	
	/**
	 * Loads module names and classnames into a HashMap fron the modules configuration file.  When the client then tries
	 * to load a module based on name, it will look up the classname for it and instantiate it via reflections.
	 * See [NETCHATHOME]/etc/modules.conf for more information.
	 */
	private void populateModuleClassnameMap()
	{
		Debug.println("Populating module classname database...", 2);
		
		Scanner in = null;
		
		try
		{
			in = new Scanner(new File(confDir + "/" + modulesConfFile));
		}
		catch (FileNotFoundException e)
		{
			Debug.println("Module conf file is missing or moved: ", 0);
			Debug.println("\tExpecting file to be at " + confDir + "/" + modulesConfFile, 0);
			
			System.exit(-1);
		}
		
		while(in.hasNext())
		{
			String s = in.nextLine();
			
			int poundIndex = s.indexOf('#');
			if(poundIndex != -1)
				s = s.substring(0, poundIndex);
			
			if(s.equals(""))
				continue;
			
			String[] args = s.split("\\s+", 0);
			
			if(args.length != 3)
			{
				Debug.println("Reached invalid conf statement: \"" + s + "." + "\"  Ignoring...", 0);
				continue;
			}
			
			
			
			Debug.println("Adding " + args[0] + ":" + args[1] + ":" + args[2] + " to module classname map...", 2);
			
			try
			{
				Class modClass = Class.forName(args[1]);
				
				moduleClassnameMap.put(args[0], (Class<AbstractModule>)modClass);
				moduleAuthorizationMap.put(args[0], Boolean.parseBoolean(args[2]));
			}
			catch (ClassNotFoundException e)
			{
				Debug.printError("WARNING: Invalid class name " + args[1] + ".  Ignoring...");
			}
			catch (Exception e)
			{
				Debug.printError("Bad configuration statement in " + confDir + "/" + modulesConfFile + ". Ignoring...");
			}
		}
	}
	
	/**
	 * Adds the names of modules that should be loaded on startup into queue from the autoloader configuration file.
	 * See [NETCHATHOME]/etc/autoloader.conf for more information.
	 */
	private void populateAutoloader()
	{
		Debug.println("Populating autoloader database...", 2);
		
		Scanner in = null;
		
		try
		{
			in = new Scanner(new File(confDir + "/" + autoloaderConfFile));
		}
		catch (FileNotFoundException e)
		{
			Debug.println("Autoloader conf file is missing or moved: ", 0);
			Debug.println("\tExpecting file to be at " + confDir + "/" + modulesConfFile, 0);
			
			System.exit(-1);
		}
		
		while(in.hasNext())
		{
			String s = in.nextLine();
			
			int poundIndex = s.indexOf('#');
			if(poundIndex != -1)
				s = s.substring(0, poundIndex);
			
			if(s.equals(""))
				continue;
			
			String[] args = s.split("\\s+", 0);
			
			if(args.length != 1)
			{
				Debug.println("Reached invalid autoloader conf statement: \"" + s + "\"  Ignoring...", 0);
				Debug.println("Autoloader module names are limited to one per line.", 0);
				continue;
			}
			
			Debug.println("Adding " + args[0] + " to autoloader list...", 2);
			
			autoloader.add(args[0]);
		}
	}
	
	/**
	 * Initializes client activity, specifically by trying to load the login module.
	 */
	private void startup()
	{
		requestLoadModule("login");
	}
	
	/**
	 * Performs any automatic post-login operations, such as calling loadPostLoginModules()
	 */
	private void postLogin()
	{
		loadPostLoginModules();
	}
	
	/**
	 * Sets the username and calls postLogin()
	 */
	public void acceptCredentials(String uname)
	{
		Debug.println("Credentials accepted, welcome " + uname + "!", 0);
		
		username = uname;
		
		postLogin();
	}
	
	/**
	 * Goes through the autoloader module queue and tries to load all modules found there.
	 * @see #populateAutoloader()
	 */
	private void loadPostLoginModules()
	{
		for(int i = 0; i < autoloader.size(); i++)
			requestLoadModule(autoloader.get(i));
	}
	
	private void failDependants(String modName)
	{
		ArrayList<String> dependants;
		if((dependants = modulesAwaitingDependencies.get(modName)) != null)
		{
			for(String dependant : dependants)
			{
				Debug.printError("Dependant to module " + modName + ", " + dependant + ", cannot be loaded because " + modName + " failed to load.");
				modulesAwaitingDependencies.remove(modName);
			}
		}
	}
	
	/**
	 * Retrieves the protocol version and version numbers from the requested module, then requests to load the module from the
	 * server via the request_module NCP.  If the module is not found in the module classname map, the method notifies
	 * Debug and exits.
	 * @param modName the name of the module to be requested.  It must match the module type name found in modules.conf
	 */
	public void requestLoadModule(String modName)
	{
		Class<AbstractModule> modClass;
		if((modClass = moduleClassnameMap.get(modName)) == null)
		{
			Debug.println("Module " + modName + " is undefined in module classname map.", 0);
			failDependants(modName);
			return;
		}
		
		if(moduleAuthorizationMap.get(modName)) //Requires authorization
			requestLoadAuthorizableModule(modClass, modName);
		else //Does not require authorization
		{
			loadModule(modName);
		}
	}
	
	private void requestLoadAuthorizableModule(Class<AbstractModule> modClass, String modName)
	{	
		String protocolVersion = null;
		try
		{
			Method protMethod = modClass.getMethod("getProtocolVersion");
			protocolVersion = (String)(protMethod.invoke(null, new Object[] {}));
		}
		catch (NoSuchMethodException e)
		{
			Debug.println("The getProtocolVersion method does not occur in module " + modName + ".  Aborting request...", 0);
			failDependants(modName);
			return;
		}
		catch (IllegalAccessException e)
		{
			Debug.println("IllegalAccessException while requesting protocolVersion for module " + modName + ".  Aborting request...", 0);
			failDependants(modName);
			return;
		}
		catch (InvocationTargetException e)
		{
			e.getTargetException().printStackTrace();
			Debug.println("Aborting...", 0);
			failDependants(modName);
			return;
		}
		
		String content = "<name>" + modName + "</name><protocol-version>" + protocolVersion + "</protocol-version>";
		
		Debug.println("Requesting module class " + modName + ", protocol version " + protocolVersion + "...", 2);
		
		modulesAwaitingAuth.add(modName);
		sendMessageToSocket(generateServerXML("module_request", content));
	}
	
	/**
	 * Sends a server message 'echo' to the server
	 * @param s the string to be echoed
	 */
	public void sendEchoMessageToSocket(String s)
	{
		if(!withXML)
			sendMessageToSocket(s);
		else
		{
			Debug.println("Creating XML from echo template...", 2);
			String echoXML = generateServerXML("echo", s);
			
			Debug.println("Sending XML to socket..", 2);
			sendMessageToSocket(echoXML);
		}
	}
	
	/**
	 * Sends a String to the ClientConnector to send to the server.
	 * @see #netchat.system.NCClientConnector.receiveMessage(String)
	 */
	public void sendMessageToSocket(String s)
	{
		Debug.println("Controller sending message to socket...", 1);
		Debug.println("Message: " + s, 2);
		
		con.receiveMessage(s + "\n");
	}
	
	/**
	 * Routes an XML message to be handled by wither a module or a client.
	 * As the main routing mechanism of the framework, handleXMLData first determines if it is a module or server message.
	 * <p>If it is module message, it will look up the module in the database of loaded modules and call handle() on it.
	 * If the module is not loaded, it will print out debug and exit.
	 * <p>If it is a server message, it will use reflections to locate the method in the Controller and call it as
	 * 'serverMessage_[Server Message Type]'.  If the method does not exist, debug is output and the method exits.
	 * @param d the XMLData representation of the message's XML tree
	 * @see #netchat.module.NCAbstractModule.handle(XMLData)
	 */
	public void handleXMLData(XMLData d)
	{
		HashMap data = d.h;
		String type = ((String)(data.get("type"))).toLowerCase();
		
		Debug.println("Handling XML message...", 2);
		
		if(type.equals("modulemessage"))
		{
			AbstractModule m;
			String name;
			
			if((m = modules.get((name = ((HashMap<String,String>)(data.get("properties"))).get("name")))) != null)
			{
				Debug.println("Handling " + type + " for module " + name + "...", 2);
				
				m.handle(d, this, m.moduleCommandsRequireEventThreadAccess());
			}
			else
				Debug.println("Module " + name + " is not loaded.", 0);
		}
		else if(type.equals("servermessage"))
		{
			String msgType = ((HashMap<String,String>)(data.get("properties"))).get("type").toLowerCase();
			
			Debug.println("Handling " + type + " " + msgType, 4);
			
			try
			{
				String method = "serverCommand_" + msgType;
				Method servMethod = Controller.class.getMethod(method,
						new Class[] {Element.class});
				servMethod.invoke(this, new Object[] {d.getContent()});
			}
			catch (NoSuchMethodException e)
			{
				Debug.println("Server command " + msgType + " not found, ignoring...", 0);
			}
			catch (IllegalAccessException e)
			{
				Debug.println("Illegal access while parsing server command" + msgType + ", aborting...", 0);
				
				e.printStackTrace();
				Debug.printError("The client encountered an error and must close.");
				System.exit(-1);
			}
			catch (InvocationTargetException e)
			{
				Debug.println("An error occured trying to invoke serverCommand_"+msgType+", aborting...", 0);
				e.getTargetException().printStackTrace();
				
				Debug.printError("The client encountered an error and must close.");
				System.exit(-1);
			}
			catch (Exception e)
			{
				Debug.println("There was an error processing the server command " + msgType, 0);
				
				e.printStackTrace();
				Debug.printError("The client encountered an error and must close.");
				System.exit(-1);
			}
		}
	}
	
	/**
	 * Adds an authorized module to the loaded module database.
	 */
	private void registerModule(AbstractModule m)
	{
		if(m == null)
		{
			Debug.printError("Trying to load null module!");
			return;
		}
		
		modules.put(m.getName(), m);
		for(ModuleListener ml : moduleListeners)
			ml.moduleAdded(new ModuleEvent(m.getName(), m.getVersion(), m.getProtocolVersion()));
	}
	
	/**
	 * Unloads a module by calling its cleanUp method, notifies listeners, and sends an unload message to the server.
	 * @param m the module to unload
	 */
	public void unloadModule(final AbstractModule m) //TODO: Check for dependency problems
	{
		if(m == null)
		{
			Debug.printError("Tried to unload a null module...");
			return;
		}
		
		m.cleanUp();
		
		modules.remove(m.getName());
		
		for(ModuleListener ml : moduleListeners)
			ml.moduleRemoved(new ModuleEvent(m.getName(), m.getVersion(), m.getProtocolVersion()));
		
		sendMessageToSocket(generateServerXML("module_unload", "<name>" + m.getName() + "</name><protocol-version>" + m.getProtocolVersion() + "</protocol-version>"));
	}
	
	/**
	 * Generates an XML tree in String form that reflects the current NCP for module messages.
	 * See http://netchat.tjhsst.edu/trac/netchat/wiki/NCP for more information.
	 * @param modName the name of the module the message is being sent to
	 * @param comName the type/command of the module message to be sent
	 * @param content a String representation of the XML content tree to be sent.  This can be null if there is no content.
	 * @return a String representation of the full module message XML tree
	 */
	public String generateModuleXML(String modName, String comName, String content)
	{
		Debug.println("Creating module message: ", 2);
		Debug.println("\tModule: " + modName, 2);
		Debug.println("\tCommand: " + comName, 2);
		Debug.println("\tContent: " + content, 2);
		
		String xmlMsg = "<?xml version=\"1.0\"?><message><header><global>";
		xmlMsg += "<properties type=\"modulemessage\"/>";
		xmlMsg += "</global><modulemessage>";
		xmlMsg += "<properties name=\"" + modName + "\" type=\"" + comName + "\"/>";
		xmlMsg += "</modulemessage></header>";
		
		if(content == null || content.equals(""))
			xmlMsg += "<content/>";
		else
			xmlMsg += "<content>" + content + "</content>";
		
		xmlMsg += "</message>";
		
		return xmlMsg;
	}
	
	/**
	 * Generates an XML tree in String form that reflects the current NCP for module messages.
	 * See http://netchat.tjhsst.edu/trac/netchat/wiki/NCP for more information.
	 * @param modName the name of the module the message is being sent to
	 * @param comName the type/command of the module message to be sent
	 * @param content an Element that is the root of the content tree to be sent.  This can be null if there is no content.
	 * @return a String representation of the full module message XML tree
	 */
	public String getModuleXML(String modName, String comName, Element content)
	{
		Debug.println("Creating module message: ", 2);
		Debug.println("\tModule: " + modName, 2);
		Debug.println("\tCommand: " + comName, 2);
		Debug.println("\tContent: " + content, 2);
		
		String xmlMsg = "<?xml version=\"1.0\"?><message><header><global>";
		xmlMsg += "<properties type=\"modulemessage\"/>";
		xmlMsg += "</global><modulemessage>";
		xmlMsg += "<properties name=\"" + modName + "\" type=\"" + comName + "\"/>";
		xmlMsg += "</modulemessage></header>";
		
		if(content == null)
			xmlMsg += "<content/>";
		else
			xmlMsg += content.toString();
		
		xmlMsg += "</message>";
		
		return xmlMsg;
	}
	
	/**
	 * Generates an XML tree in String form that reflects the current NCP for server messages based on a template.
	 * @param template the name of the template to be loaded from
	 * @param content a String representation of the XML content tree to be sent.  This can be null if there is no content.
	 * @return a String representation of the full server message XML tree
	 */
	public String generateServerXML(String template, String content)
	{
		if(serverMessageTemplates.get(template) == null)
			Debug.println("Trying to send unrecognized server message type, aborting...", 0);
		
		String xmlMsg = "<?xml version=\"1.0\"?>";
		xmlMsg += "<message>";
		xmlMsg += "<header>";
		xmlMsg += "<global>";
		
		String[] globalProps = serverMessageTemplates.get(template).get("globalproperties");
		
		xmlMsg += "<properties";
		for(int i = 0; i < globalProps.length; i++)
			xmlMsg += " " + globalProps[i];
		xmlMsg += "/>";
		
		xmlMsg += "</global>";
		xmlMsg += "<servermessage>";
		
		String[] msgProps = serverMessageTemplates.get(template).get("messageproperties");
		
		xmlMsg += "<properties";
		for(int i = 0; i < msgProps.length; i++)
			xmlMsg += " " + msgProps[i];
		xmlMsg += "/>";
		
		xmlMsg += "</servermessage>";
		xmlMsg += "</header>";
		
		if(content.equals(""))
			xmlMsg += "<content/>";
		else
		{
			xmlMsg += "<content>";
			xmlMsg += content;
			xmlMsg += "</content>";
		}
		
		xmlMsg += "</message>";
		
		Debug.println("Constucted server message \"" + template + "\":\n" + xmlMsg, 4);
		
		return xmlMsg;
	}
	
	/**
	 * Sends the server message 'ping' via the ping template.
	 */
	public void sendPing()
	{
		Debug.println("Ping...", 2);
		
		sendMessageToSocket(generateServerXML("ping", ""));
	}
	
	/**
	 * Sends the server message 'pong' via the pong template.
	 */
	public void sendPong()
	{
		Debug.println("Pong...", 2);
		
		sendMessageToSocket(generateServerXML("pong", ""));
	}
	
	/**
	 * Adds a ModuleListener that is notified on module events.
	 * @param the ModuleListener to add
	 */
	public void addModuleListener(ModuleListener ml)
	{
		moduleListeners.add(ml);
	}
	
	/**
	 * Removes a ModuleListener from the ArrayList of ModuleListeners that are notified on module events.
	 * @param the ModuleListener to remove
	 */
	public void removeModuleListener(ModuleListener ml)
	{
		moduleListeners.remove(ml);
	}
	
	private int processModuleDependencies(Class<AbstractModule> modClass, String modName)
	{
		ArrayList<String> dependencies;
		if((dependencies = partialDependencies.get(modName)) == null)
			try
			{	
				dependencies = (ArrayList<String>)(modClass.getMethod("getDependencies").invoke(null, new Object[] {}));
			}
			catch (NoSuchMethodException e)
			{
				Debug.println("The getDependencies method does not occur in module " + modName + ".  Aborting request...", 0);
				return NC.FAIL;
			}
			catch (IllegalAccessException e)
			{
				Debug.println("IllegalAccessException while requesting getDependencies for module " + modName + ".  Aborting request...", 0);
				return NC.FAIL;
			}
			catch (InvocationTargetException e)
			{
				e.getTargetException().printStackTrace();
				Debug.println("Aborting...", 0);
				return NC.FAIL;
			}
		
		if(dependencies == null || dependencies.size() == 0)
			return NC.SUCCESS;
		
		for(int i = 0; i < dependencies.size(); i++)
		{
			String mod = dependencies.get(i);
			
			if(modules.get(mod) != null)
			{
				Debug.println("Module " + mod + " is already loaded.  Continuing...", 2);
				dependencies.remove(mod);
				i--;
				continue;
			}
			
			Debug.println("Loading dependency " + mod + "...", 2);
			
			ArrayList<String> dependants;
			if((dependants = modulesAwaitingDependencies.get(mod)) != null)
				dependants.add(modName);
			else
			{
				dependants = new ArrayList<String>();
				dependants.add(modName);
				modulesAwaitingDependencies.put(mod, dependants);
			}
			partialDependencies.put(modName, dependencies);
			
			requestLoadModule(mod);
			return NC.RESUME;
		}
		
		partialDependencies.remove(dependencies);
		
		return NC.SUCCESS;
	}
	
	public int loadModule(String modName)
	{	
		if(modules.get(modName) != null)
		{
			Debug.println("Module " + modName + " already loaded, aborting...", 0);
			return NC.SUCCESS;
		}
			
		final Class<AbstractModule> modClass = moduleClassnameMap.get(modName);
		if(modClass == null)
		{
			Debug.println("ERROR:  Module " + modName + " is not defined!  Aborting...", 0);
			failDependants(modName);
			return NC.FAIL;
		}
		
		int returnCode = processModuleDependencies(modClass, modName);
		
		if(returnCode == NC.FAIL)
		{
			Debug.printError("Could not fulfill dependencies of module " + modName + "!  Not loading module...");
			failDependants(modName);
			return NC.FAIL;
		}
		else if(returnCode == NC.RESUME)
		{
			return NC.RESUME;
		}
		
		Debug.println("Creating instance of " + modClass + "...", 2);
		
		boolean requiresEventThreadAccess = false;
		
		try {
			Method threadAccessMethod = modClass.getMethod("constructionRequiresEventThreadAccess");
			requiresEventThreadAccess = ((Boolean)(threadAccessMethod.invoke(null, new Object[] {}))).booleanValue();
		}
		catch (NoSuchMethodException e)
		{
			Debug.printError("Could not find method constructionRequiresEventThreadAccess in class " + modClass);
			failDependants(modName);
		}
		catch (InvocationTargetException e)
		{
			Debug.printError("There was an error invoking constructionRequiresEventThreadAccess for class " + modClass);
			failDependants(modName);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		final String finalModName = modName;
		
		if(requiresEventThreadAccess && Display.findDisplay(Thread.currentThread()) == null)
			display.syncExec(new Runnable(){
				public void run()
				{
					reflectModule(modClass, finalModName);
				}
			});
		else
			reflectModule(modClass, modName);
		
		//TODO: This does not always mean success though...
		return NC.SUCCESS;
	}
	
	public AbstractModule getModule(String modName)
	{
		return modules.get(modName);
	}
	
	/**
	 * Called upon receiving the server message 'authorize_module,' uses reflections to create an instance of the module
	 * and loads it.  If the module cannot be found, outputs debug and exits.
	 * @param content the XMLElement representation of the content XML tree
	 */
	public void serverCommand_authorize_module(Element content) //Called upon receiving "authorize_module"
	{
		final String modName = content.get("name").text();
		
		if(modules.get(modName) != null)
		{
			Debug.println("Module " + modName + " already loaded, aborting...", 0);
			return;
		}
		
		boolean foundMod = false;
		for(int i = 0; i < modulesAwaitingAuth.size(); i++)
			if(modName.equals(modulesAwaitingAuth.get(i)))
			{
				foundMod = true;
				modulesAwaitingAuth.remove(i);
				break;
			}
		
		if(!foundMod)
		{
			Debug.println("ERROR: Module " + modName + " not awaiting authorization!  Aborting...", 0);
			return;
		}
			
		loadModule(modName);
	}
	
	private void reflectModule(Class<AbstractModule> modClass, String modName)
	{
		try
		{
			Class<Controller> cont = ((Class<Controller>)(getClass())); 
			Constructor c = modClass.getConstructor(new Class[] {cont});
			AbstractModule mod = (AbstractModule)(c.newInstance(new Object[] {this}));
			
			Debug.println("Loading module " + modName + "...", 2);
		
			registerModule(mod);
		}
		catch (IllegalAccessException e)
		{
			Debug.println("Illegal access while loading module " + modName + ".  Aborting...", 0);

			e.printStackTrace();
			Debug.printError("The client encountered an error and must close.");
			System.exit(-1);
		}
		catch (NoSuchMethodException e)
		{
			Debug.println("Error creating the default module constructor for " + modName + ".  Aborting...", 0);
			failDependants(modName);
			return;
		}
		catch (InvocationTargetException e)
		{
			e.getTargetException().printStackTrace();

			e.printStackTrace();
			Debug.printError("The client encountered an error and must close.");
			System.exit(-1);
		}
		catch (InstantiationException e)
		{
			Debug.println("Error instantiating module " + modName + ".  Aborting...", 0);

			e.printStackTrace();
			Debug.printError("The client encountered an error and must close.");
			System.exit(-1);
		}
		
		ArrayList<String> dependants;
		if((dependants = modulesAwaitingDependencies.get(modName)) != null)
		{
			for(String dependant : dependants)
			{
				modulesAwaitingDependencies.remove(modName);
				loadModule(dependant);
			}
		}
	}
	
	/**
	 * Called when the server message 'ping' is received, notifies the heartbeat monitor that the connection is still alive
	 * before replying with a pong message.
	 * @param content the XMLElement representation of the content XML tree
	 */
	public void serverCommand_ping(Element content)
	{
		Debug.println("Ping!", 4);
		
		heartbeatAlive = true;
		skippedBeat = false;
		sendPong();
	}
	
	/**
	 * Called when the server message 'pong' is received, notifies the heartbeat monitor that the connection is still alive.
	 * @param content the XMLElement representation of the content XML tree
	 */
	public void serverCommand_pong(Element content)
	{
		Debug.println("Pong!", 4);
		
		heartbeatAlive = true;
		skippedBeat = false;
	}
	
	/**
	 * Called when the server message 'deny_module' is received, notifies the user that the module has been denied and
	 * removes the module from the modulesAwaitingAuth queue.
	 * @param content the XMLElement representation of the content XML tree
	 */
	public void serverCommand_deny_module(Element content)
	{
		String modName = content.get("name").text();
		String version = content.get("version").text();
		
		boolean foundMod = false;
		for(int i = 0; i < modulesAwaitingAuth.size(); i++)
			if(modName.equals(modulesAwaitingAuth.get(i)))
			{
				foundMod = true;
				modulesAwaitingAuth.remove(i);
				break;
			}
		
		if(!foundMod)
		{
			Debug.println("ERROR: Module " + modName + " not awaiting authorization!  Aborting...", 0);
			return;
		}
		
		Debug.println("Module " + modName + ", version " + version + " was denied by the server...", 0);
		
		if(modName.equals("login"))
		{
			Debug.println("FATAL: Server denied login module!", 0);
			System.exit(0);
		}
	}
	
	/**
	 * Returns the SWT Display associated with the Controller
	 */
	public Display getDisplay()
	{
		return display;
	}
	
	/**
	 * Returns whether or not the client is shutting down.
	 */
	public boolean isShuttingDown()
	{
		return shuttingDown;
	}
	
	/**
	 * Returns the username String is logged in or null if not.
	 */
	public String getUsername()
	{
		return username;
	}
	
	/**
	 * Returns the NetChat System Properties map
	 */
	public Properties getProperties()
	{
		return properties;
	}
	
	/**
	 * Returns whether this is the first login
	 * @return true if first login, else false
	 */
	public boolean isFirstLogin()
	{
		return firstLogin;
	}
	
	/**
	 * Returns the current Controller
	 * @return the current controller
	 */
	public static synchronized Controller getCurrent()
	{
		return currentController;
	}
	
	/**
	 * Returns the ClientConnector.
	 */
	public ClientConnector getClientConnector()
	{
		return con;
	}
	public class ShutdownThread implements Runnable
	{
		public void run()
		{
			if(!isShuttingDown())
				shutDown();
		}
	}
	
	private class DisplayReadAndDispatchRunnable implements Runnable
	{
		public void run()
		{
			display = new Display();
			while(true)
			{
				if(!display.readAndDispatch())
					display.sleep();
			}
		}
	}
}
