package netchat.system;

import netchat.util.*;
import netchat.module.*;

import javax.swing.*;

/**
 * The NCClient class is the main, runnable class in the NetChat system.  It manages the truststore via System properties, parses
 * command line arguments, prints licensing information, and initializes the Controller.  It is analogous to the bootloader section
 * of the harddrive in x86 systems.  It is necessary to get the system up and running, but doesn't do much on its own.
 * @author Andy Street, 2007
 */

public class NCClient
{
	static final long serialVersionUID = 0L;

	private static String hostname = null;
	private static int port = -1;
	private static boolean withSSL = true;
	private static boolean withXML = true;
	
	/**
	 * Initiates the NetChat J-Client.
	 */
	public static void main(String[] args)
	{
		System.setProperty("javax.net.ssl.trustStore", "./truststore/netcommcerts");
		System.setProperty("javax.net.ssl.trustStorePassword", "tankers");
		System.setProperty("javax.net.ssl.keyStore", "./truststore/netcommcerts");
		System.setProperty("javax.net.ssl.keyStorePassword", "tankers");
		
		parseCommandLineArgs(args);
		
		printLicensing();
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			Debug.println("Couldn't use native look and feel...", 0);
		}
		
		Controller c = new Controller(hostname, port, withSSL, withXML);
		c.init();
	}
	
	/**
	 * Parses command line arguments and sets appropriate variables/actions associated with them.
	 */
	public static void parseCommandLineArgs(String[] args)
	{
		boolean skipNext = false;
		
		for(int i = 0; i < args.length; i++)
		{
			if(!skipNext)
			{
				if(args[i].equals("-p") || args[i].equals("--port"))
				{
					skipNext = true;
					
					try{
						port = Integer.parseInt(args[i + 1]);
					}
					catch (Exception e){
						System.out.println("Incorrect usage.");
						System.out.println(usageText());
						System.exit(-1);
					}
				}
				else if(args[i].equals("-h") || args[i].equals("--hostname"))
				{
					skipNext = true;
					
					if(args.length < i + 2)
					{
						System.out.println("Incorrect usage.");
						System.out.println(usageText());
						System.exit(-1);
					}
					
					hostname = args[i + 1];
				}
				else if(args[i].equals("-s") || args[i].equals("--no-ssl"))
				{
					withSSL = false;
				}
				else if(args[i].equals("-x") || args[i].equals("--no-xml"))
				{
					withXML = false;
				}
				else if(args[i].equals("--help"))
				{
					displayHelp();
					System.exit(0);
				}
				else if(args[i].equals("-d") || args[i].equals("--debug-level"))
				{
					skipNext = true;
					
					try{
						int lvl = Integer.parseInt(args[i + 1]);
						Debug.setDebug(lvl);
					}
					catch (Exception e){
						System.out.println("Incorrect usage.");
						System.out.println(usageText());
						System.exit(-1);
					}
				}
				else if(args[i].equals("-v") || args[i].equals("--version"))
				{
					displayAboutText();
					System.exit(0);
				}
				else if(args[i].equals("--debug-to-err"))
				{
					Debug.setPrintToStd(false);
				}
				else
				{
					System.out.println("Incorrect usage.");
					System.out.println(usageText());
					System.exit(-1);
				}
			}
			else
				skipNext = false;
		}
	}
	
	/**
	 * Prints the usage text for how to run the client from the command line.
	 */
	public static String usageText()
	{
		return "Usage: java NCClient [-h hostname] [-p port] [-s] [-V] [-x]";
	}
	
	/**
	 * Displays the help text.  Invoked by passing the command line argument "--help".
	 */
	public static void displayHelp()
	{
		String help = usageText();
										//32 characters               !
		help += 	"\n\n\t-d, --debug-level LEVEL:                Outputs debug associated with";
		help +=		  "\n\t                                        debug level (0-2) and below,";
		help += 	  "\n\t                                        defaults to 0 (no debug)";
		help += 	  "\n\t-h, --hostname HOSTNAME:                Bind to a server on this host";
		help += 	  "\n\t-p, --port PORT:                        Bind to a server on this port";
		help += 	  "\n\t-s, --no-ssl:                           Connect without SSL";
		help += 	  "\n\t-x, --no-xml:                           Transmit data without XML";
		help += 	  "\n\t                                           (deprecated)";
		help += 	"\n\n\t--help:                                 Display this help text";
		help += 	  "\n\t--debug-to-err:                         Print all debug to System.err";
		help += 	  "\n\t-v, --version:                          Display version information";
		
		System.out.println(help);
	}
	
	/**
	 * Displays the about text.
	 */
	public static void displayAboutText()
	{
		String text = "";
		
		text += "\nNetChat was developed by Andy Street, Barnett Trzcinski, and Steven Fuqua";
		text += "\nServer version 0.9";
		text += "\nClient version 0.9";
		text += "\nProtocol version 0.1a";
		text += "\n";
		text += "\nEmail us at netcomm-devel@googlegroups.com";
		text += "\nAll rights reserved, Andy Street and the NetChat Group, 2006";
		text += "\n";
		text += "\nSubmit your suggestions to http://axuric.xelocy.com:8080/ncsuggestions/!";
		text += "\n";
		
		System.out.println(text);
	}
	
	/**
	 * Displays licensing information.
	 */
	public static void printLicensing()
	{
		displayAboutText();
	}
}
