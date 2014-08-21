package netchat.system;

import netchat.util.*;
import netchat.xml.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.net.*;
import javax.net.ssl.*;

/**
 * The ClientConnector handles creating and closing the socket to the NetChat server.  In addition to creating and cleaning
 * up the socket, the ClientConnector is also responsible for the base sending of messages to the server.
 * @author Andy Street, 2007
 */

public class ClientConnector
{	
	private final int DEFAULTPORT = 45287;
	
	private int PORT = DEFAULTPORT;
	private String HOSTNAME = "";
	private ArrayList<String> servers = null;
	
	private Controller con = null;
	private XMLParser parser = null;
	
	private Socket clientSock = null;
	private InputStream sockIn = null;
	private PrintStream sockWriter = null;
	private Thread socketReadThread = null;
	
	private boolean withSSL = true;
	private boolean withXML = true;
	private boolean isConnected = false;
	private boolean isClosing = false;
	
	/**
	 * The default constructor.
	 */
	public ClientConnector(Controller c)
	{
		Debug.println("Creating client connector...", 2);
		con = c;
	}
	
	/**
	 * Cleans up the connection by closing the socket connection if it exists.
	 */
	public void cleanUp()
	{
		if(clientSock == null)
			return;
		
		isConnected = false;
		isClosing = true;
		
		Debug.println("Cleaning up socket...", 1);
		try{
			Debug.println("Preparing to close socket...", 2);
			if(!clientSock.isClosed())
				clientSock.close();
			Debug.println("Socket is closed.", 2);
		}
		catch (IOException e)
		{
			Debug.printError("\nThere was an error closing the socket.");
		}
		
		isClosing = false;
	}
	
	/**
	 * Initializes the ClientConnector by processing the server list and calling createConnection()
	 */
	public void init(String servername, ArrayList<String> serves, int port, boolean wSSL, boolean wXML)
	{
		servers = serves;
		
		if(servername != null)
		{
			if(servers.remove(servername))
				Debug.println("Reordering server queue...", 2);
			
			servers.add(0, servername);
		}
		
		if(port > 0)
			PORT = port;
		
		withSSL = wSSL;
		withXML = wXML;
		
		if(withSSL)
			Debug.println("Using SSL...", 1);
		else
			Debug.println("Not using SSL...", 1);
		
		if(withXML)
			Debug.println("Using XML...", 1);
		else
			Debug.println("Not using XML...", 1);
		
		findHost();
	}
	
	/**
	 * Goes through an ArrayList of servers created from command line args and configuration files until it finds a host
	 * that accepts a connection.
	 */
	public void findHost()
	{
		if(isConnected)
		{
			con.disassociate(true);
			cleanUp();
		}
		
		for(int i = 0; i < servers.size(); i++)
		{
			if(isConnected)
				break;
			
			createConnection(servers.get(i), PORT);
		}
		
		if(isConnected)
			makePrimaryServer(HOSTNAME);
		else
		{
			JOptionPane.showMessageDialog(null,
				"There was an error trying to connect to" +
				"\n remote hosts on port " + PORT + "." +
				"\nPossible causes are:" +
				"\n    - Another program is running on port " + PORT + "." +
				"\n    - All remote servers in queue are down." +
				"\n    - Port " + PORT + " on the remote servers" +
				"\n       is not accepting NetChat connections." +
				"\n    - You do not have a connection to the internet." +
				"\n    - There is a problem with SSL.", "Error", JOptionPane.ERROR_MESSAGE);
			
			Debug.println("Error creating socket, shutting down...", 0);
			System.exit(-1);
		}
	}
	
	/**
	 * Initializes the socket connection to a specified server on the default port, captures input/
	 * output streams, and initiates the socketReadThreads and XMLParser.
	 * @param host name of the host to connect to
	 */
	public void createConnection(String host)
	{
		createConnection(host, DEFAULTPORT);
	}
	
	/**
	 * Initializes the socket connection to a specified server on a specified port, captures input/
	 * output streams, and initiates the socketReadThreads and XMLParser.
	 * @param host name of the host to connect to
	 * @param port port to connect to the host through
	 */
	public void createConnection(String host, int port)
	{
		PORT = port;
		HOSTNAME = host;
		
		if(PORT < 1025 || PORT > 65536)
		{
			Debug.printError("Invalid port " + PORT + " specified.");
			System.exit(-1);
		}
		
		//Init socket
		Debug.println("Attempting to initialize connection to host " + HOSTNAME + " on port " + PORT + "...", 1);
			
		try{
			if(withSSL)
				clientSock = (SSLSocket)(SSLSocketFactory.getDefault().createSocket(HOSTNAME, PORT));
			else
				clientSock = new Socket(HOSTNAME, PORT);
		}
		catch (IOException e)
		{
			Debug.printError("Could not establish a connection to host " + HOSTNAME + "...");
			isConnected = false;
			return;
		}
		
		isConnected = true;
		
		Debug.println("Socket initialized.", 1);
		Debug.println("Connected to host " + HOSTNAME + ".", 0);
		
		//Get print stream from socket
		Debug.println("Attempting to capture output stream...", 2);
		try{
			sockWriter = new PrintStream(clientSock.getOutputStream());
		}
		catch (IOException e) { 
			Debug.printError("Could not capture socket OutputStream.  Exiting...");
			System.exit(-1);
		}
		Debug.println("Output stream successfully captured.", 2);
		
		//Get input stream from socket
		Debug.println("Attempting to capture input stream...", 2);
		try{
			sockIn = clientSock.getInputStream();
		}
		catch (IOException e) {
			Debug.printError("Could not capture Input Stream.  Exiting...");
			System.exit(1);
		}
		Debug.println("Input stream successfully captured.", 2);
		
		//Start listening for data from the server
		if(withXML)
			parser = new XMLParser(con);

		Debug.println("Starting read from socket...", 1);
		
		try
		{
			socketReadThread = new Thread(new SocketReadThread());
			socketReadThread.start();
		}
		catch (Exception e)
		{
			if(e.getMessage().equals("Socket closed") && clientSock.isClosed())
				Debug.println("Socket read Thread stopped.", 0);
			else
			{
				Debug.println("Read Thread encountered an error:", 0);
				e.printStackTrace();
			}
		}
		
		con.connectionCreated();
	}
	
	/**
	 * Prints a message to the socket.
	 * @param host the server to move to the front of the server list
	 */
	public void makePrimaryServer(String host)
	{
		servers.remove(host);
		servers.add(0, host);
	}
	/**
	 * Prints a message to the socket.
	 * @param s the message to be sent to the server
	 */
	public void receiveMessage(String s)
	{
		if(sockWriter != null)
			sockWriter.print(s);
		else
			Debug.printError("Trying to send message when socket is not initialized.");
	}
	
	/**
	 * Returns an ArrayList of servers populated from command line args and configuration files.
	 */
	public ArrayList<String> getServerList()
	{
		return servers;
	}
	
	/**
	 * Returns the server currently connected to or null if disconnected.
	 */
	public String getCurrentServer()
	{
		if(isConnected)
			return HOSTNAME;
		
		return null;
	}
	private class SocketReadThread implements Runnable
	{	
		public void run()
		{
			BufferedReader sockReader = new BufferedReader(new InputStreamReader(sockIn));
			String s = "";
			
			try{
				while((s = sockReader.readLine()) != null)
				{
					Debug.println("Received message...", 4);
					Debug.println("Sending message to client.", 4);
					Debug.println("Message: " + s, 3);
					
					try
					{
						if(withXML)
							parser.parse(new StringReader(s));
						else
							Debug.println("Non-XML messages are not supported.", 0);
					}
					catch(Exception e)
					{
						e.printStackTrace();
						Debug.printError("The client encountered an error and must close.");
						System.exit(-1);
					}
				}
			}
			catch (IOException e)
			{
				if(!isClosing && !clientSock.isClosed())
				{
					Debug.println("There was an error reading from the socket.", 0);
					//e.printStackTrace();
					//System.exit(-1);
					
				}
			}
		}
	}
}
