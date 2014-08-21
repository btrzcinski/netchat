package netchat.module;

import netchat.xml.*;
import netchat.util.*;
import netchat.system.*;

import org.eclipse.swt.widgets.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * AbstractModule is the class that all user created modules must implement.  The AbstractModule class handles all message
 * routing and reflection, allowing users to just implement a method with the name moduleCommand_[Module Message Command Type]
 * and know that the method will be called and passed a content tree whenever a message with that module message command type
 * is received.
 * <p>Implementing subclasses must implement:<br>
 * * A constructor that is passed an Controller <br>
 * * The cleanUp() method, called when the module is being unloaded <br>
 * * The getName() method, which returns the generic name of the module [i.e. for LoginModule, the String "login", or whatever
 * is defined in etc/modules.conf] <br>
 * * The getProtocolVersion() method the returns the NetChat Protocol version number the module is using [currently 0.1a] <br>
 * * The getVersion() method that returns the version number of that particular module [used for versioning control and
 * automatic updates.] <br>
 * * The constructionRequiresEventThreadAccess() method that returns whether or not the module needs to be constructed with
 * * event thread access.
 * * The moduleCommandsRequireEventThreadAccess() method that returns whether or not module commands need to be called with
 * * event thread access.
 * * The getDependencies() method that returns an ArrayList&lt;String&gt; which contains the dependencies for this module.
 * @author Andy Street, 2007 
 */

public abstract class AbstractModule
{	
	/** 
	 * Default constuctor necessary for subclasses.  Does nothing.
	 */
	public AbstractModule()
	{
	}
	
	/**
	 * The constructor that must be implemented by all subclasses.
	 */
	public AbstractModule(Controller cont)
	{
	}
	
	/**
	 * Implements reflections in order to handle all incoming XML messages for implementing subclasses.
	 * This allows subclasses to just put moduleMessage_[NCP Message Type] in their classes and it will be invoked
	 * and passed the content XML tree when the module receives an XML module message with type [NCP Message Type].
	 * See http://netchat.tjhsst.edu/trac/netchat/wiki/NCP for more information about module messages.
	 *
	 * @param data the XMLData representation of the module message XML tree
	 */
	public void handle(final XMLData data, Controller cont, boolean requiresEventThreadAccess)
	{	
		final AbstractModule thisClass = this;
		final String msgType = ((HashMap<String, String>)(data.h.get("properties"))).get("type");
		Debug.println("Handling module message " + msgType, 2);
		String method = "moduleCommand_" + msgType;
		if(requiresEventThreadAccess && Display.findDisplay(Thread.currentThread()) == null)
		{
			try {
				final Method modMethod = getClass().getMethod(method,
						new Class[] {Element.class});

				cont.getDisplay().syncExec(new Runnable(){
					public void run()
					{
						invokeReflection(thisClass, modMethod, data, msgType);
					}
				});
			}
			catch (NoSuchMethodException e)
			{
				Debug.println("Module command " + msgType + " in " + getName() + " module not found, ignoring...", 0);
			}
		}
		else
		{
			try {
				invokeReflection(this, getClass().getMethod(method, new Class[] {Element.class}), data, msgType);
			}
			catch (NoSuchMethodException e)
			{
				Debug.println("Module command " + msgType + " in " + getName() + " module not found, ignoring...", 0);
			}
		}
	}
	
	private void invokeReflection(AbstractModule thisClass, Method modMethod, XMLData data, String msgType)
	{
		try {
			modMethod.invoke(thisClass, new Object[] {data.getContent()});
		}
		catch (IllegalAccessException e)
		{
			Debug.println("Illegal access while parsing module command" + msgType + " in " + getName() + " module, aborting...", 0);
			
			e.printStackTrace();
			Debug.printError("The client encountered an error and must close.");
			System.exit(-1);
		}
		catch (InvocationTargetException e)
		{
			Debug.println("An error occured while invoking moduleCommand_" + msgType + " in " + getName() + " module, aborting...", 0);

			e.printStackTrace();
			Debug.printError("The client encountered an error and must close.");
			System.exit(-1);
		}
	}
	
	/**
	 * 
	 * @param cont the #Controller that will handle the message
	 * @param modName name of the module
	 * @param comName the module command name
	 * @param content the message content tree as a String
	 */
	protected void sendXMLMessage(Controller cont, String modName, String comName, String content)
	{
		cont.sendMessageToSocket(cont.generateModuleXML(modName, comName, content));
	}
	
	/**
	 * 
	 * @param cont the #Controller that will handle the message
	 * @param modName name of the module
	 * @param comName the module command name
	 * @param content the message content tree as a #Element
	 */
	protected void sendMessage(Controller cont, String modName, String comName, Element content)
	{
		cont.sendMessageToSocket(cont.getModuleXML(modName, comName, content));
	}
	
	/**
	 * Called when a module is to be unloaded by the Controller
	 */
	public abstract void cleanUp();
	
	/**
	 * Returns the name of the module.  For example, LoginModule would return "login".
	 */
	public abstract String getName();
	
	/**
	 * Returns the protocol version the module implements.  Current protocol version is '0.1a'.  This must be implemented
	 * in all subclasses or else will return 'nil'.
	 */
	public static String getProtocolVersion()
	{
		return "nil";
	}
	
	/**
	 * Returns the protocol version the module implements.  This must be implemented
	 * in all subclasses or else will return 'nil'.
	 */
	public static String getVersion()
	{
		return "nil";
	}
	
	/**
	 * DO NOT USE, ONLY IMPLEMENT: returns whether or not the module needs to be accessed through the SWT event thread when constructed.  This must be implemented
	 * in all subclasses or else will return false.
	 */
	public static boolean constructionRequiresEventThreadAccess()
	{
		return false;
	}
	
	/**
	 * Returns whether or not the module needs module commands to be accessed through the SWT event thread.  This must be implemented
	 * in all subclasses.
	 */
	public abstract boolean moduleCommandsRequireEventThreadAccess();
	
	public static ArrayList<String> getDependencies()
	{
		return null;
	}
}
