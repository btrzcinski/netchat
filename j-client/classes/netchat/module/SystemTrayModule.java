package netchat.module;

import java.util.ArrayList;

import netchat.*;
import netchat.system.*;
import netchat.util.Debug;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;

public class SystemTrayModule extends AbstractModule {
	
	static String name = "tray";
	public static String version = "0.1a";
	public static String protocolVersion = "0.1a";
	
	String iconFile = "/icons/nc.png";
	
	Controller cont = null;
	
	Tray tray = null;
	TrayItem trayItem = null;
	
	public SystemTrayModule(Controller cont)
	{
		this.cont = cont;
		
		if((tray = Display.getCurrent().getSystemTray()) != null)
		{
			trayItem = new TrayItem(tray, SWT.NONE);
			trayItem.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(iconFile)));
		}
	}
	public Tray getSystemTray()
	{
		return tray;
	}
	public TrayItem getTrayItem()
	{
		return trayItem;
	}
	public void disposeTray()
	{
		if(trayItem != null)
			trayItem.dispose();
		
		if(tray != null)
			tray.dispose();
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
	public void cleanUp()
	{
		cont.getDisplay().asyncExec(new Runnable(){
			public void run()
			{
				disposeTray();
			}
		});
	}
	public String getName()
	{
		return name;
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
