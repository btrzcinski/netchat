package netchat.module;

import netchat.xml.*;
import netchat.util.*;
import netchat.system.*;
import netchat.module.login.*;

import java.io.*;
import java.util.*;
import java.security.*;

/**
 * LoginModule is the default login module of the NetChat system.
 * @author Andy Street, 2007
 */

public class LoginModule extends AbstractModule
{
	Controller controller = null;
	LoginGUI gui = null;
	
	static String name = "login";
	public static String version = "0.1a";
	public static String protocolVersion = "0.1a";
	
	private String hashType = "SHA-512"; //Set to "none" for no hash
	private String lowerHash = null;
	private MessageDigest md = null;
	
	String uname = null;
	
	boolean savePassword = false;
	private String passHash = null;
	public String passFilename = ".ncsp";
	
	public LoginModule(Controller cont)
	{
		controller = cont;
		
		lowerHash = hashType.toLowerCase();
		
		try{
			if(lowerHash.equals("sha1") || lowerHash.equals("sha-1"))
				md = MessageDigest.getInstance("SHA-1");
			else if(lowerHash.equals("sha512") || lowerHash.equals("sha-512"))
				md = MessageDigest.getInstance("SHA-512");
			else if(!lowerHash.equals("none"))
			{		
				Debug.println("Hash type " + hashType + " is unrecognized.  Using SHA-512...", 0);
				md = MessageDigest.getInstance("SHA-512");
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			Debug.println("Algorithm for hash type " + hashType + " was not found.", 0);
			System.exit(-1);
		}
		
		gui = new LoginGUI(this, controller.getDisplay(), controller.getClientConnector(), controller);
	}
	public void receiveSavedCredentials(String username, String passHash, boolean savePassword)
	{
		this.passHash = passHash;
		this.savePassword = savePassword;
		
		sendCredentials(username, passHash);
		
		uname = username;
	}
	public void receiveCredentials(String username, char[] password, boolean savePassword)
	{	
		this.savePassword = savePassword;
		passHash = hashPassword(password);
		
		sendCredentials(username, passHash);
		
		uname = username;
	}
	public void sendCredentials(String username, String passHash)
	{
		String content = "<username>" + username + "</username>";
		content += "<password hash=\"" + hashType + "\">" + passHash + "</password>";
		
		sendXMLMessage(controller, name, "login_request", content);
	}
	public String hashPassword(char[] pw)
	{
		byte[] bytePw = new byte[pw.length];
		for(int i = 0; i < pw.length; i++)
			bytePw[i] = (byte)(pw[i]);
		
		if(lowerHash.equals("none"))
			return new String(pw);
		else
			return bytesToHex(md.digest(bytePw));
	}
	public String bytesToHex(byte[] b)
	{
		char[] hexChars = {'0', '1' ,'2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

		String hex = "";
		
		for(int i = 0; i < b.length; i++)
		{
			hex += hexChars[(int)((b[i] & 0xf0) >>> 4)];
			hex += hexChars[(int)(b[i] & 0x0f)];
		}

		return hex;
	}
	
	public void moduleCommand_accept_login(Element content)
	{
		try
		{
			Properties p = controller.getProperties();
			
			p.remove("nc_user");
			p.remove("nc_hash");
			p.remove("nc_hash_type");
		}
		catch(Exception e)
		{
			Debug.println("Unable to clear password: " + e.getClass(), 2);
		}
		
		if(savePassword)
		{
			try
			{
				Properties p = controller.getProperties();
				
				p.setProperty("nc_user", uname);
				p.setProperty("nc_hash", passHash);
				p.setProperty("nc_hash_type", hashType);
			}
			catch(Exception e)
			{
				Debug.printError("Unable to save password: " + e.getMessage());
			}
			
		}
		
		controller.acceptCredentials(uname);
		gui.acceptCredentials();
	}
	public void moduleCommand_reject_login(Element content)
	{
		ArrayList subObjects = content.getSubObjects();
		if(subObjects.size() > 0)
			gui.rejectCredentials((String)(content.getSubObjects().get(0)));
		else
			gui.rejectCredentials("Login denied.");
	}
	
	public String getName()
	{
		return name;
	}
	public static String getProtocolVersion()
	{
		return protocolVersion;
	}
	public static String getVersion()
	{
		return version;
	}
	public void cleanUp()
	{
		if(!controller.isShuttingDown())
			gui.dispose();
	}
	public static boolean constructionRequiresEventThreadAccess()
	{
		return true;
	}
	public boolean moduleCommandsRequireEventThreadAccess()
	{
		return true;
	}
	public static ArrayList<String> getDependencies()
	{
		return null;
	}
}
