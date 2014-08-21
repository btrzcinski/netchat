package netchat.module;

import netchat.system.*;

import java.util.*;

public class Module extends AbstractModule {

	private String name = "";
	private static String protocolVersion = "0.1a";
	private static String version = "0.1a";
	
	private Controller controller = null;
	
	public Module(Controller cont)
	{
		this.controller = cont;
	}

	public void cleanUp()
	{
	
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
