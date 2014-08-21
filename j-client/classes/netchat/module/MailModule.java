package netchat.module;

import netchat.xml.*;
import netchat.util.*;
import netchat.event.*;
import netchat.system.*;
import netchat.module.mail.*;

import java.util.ArrayList;

import java.util.*;

public class MailModule extends AbstractModule {

	private String name = "mail";
	private static String protocolVersion = "0.1a";
	private static String version = "0.1a";
	
	private Controller controller = null;
	
	private ArrayList<MailListener> mailListeners = null;
	private HashMap<String, MailBox> mailBoxes = null;
	
	public MailModule(Controller cont)
	{
		this.controller = cont;
		
		mailListeners = new ArrayList<MailListener>();
		mailBoxes = new HashMap<String, MailBox>();
		
		requestFolderList();
	}

	public void requestFolderList()
	{
		sendMessage(controller, getName(), "folder_list_request", null);
	}
	public void retrieveMessageRequest(long id)
	{
		Element content = new Element("content");
		new Element(content, "message").getAttributes().put("id", "" + id);
		
		sendMessage(controller, getName(), "retrieve_message_request", content);
	}
	private void initFolder(MailBox parent, Element folderTree)
	{
		String name = folderTree.getAttributes().get("name");
		MailBox mb = new MailBox(parent, name);
		mailBoxes.put(name, mb);
		requestMail(mb);
		
		for(Element e : folderTree.getElements())
			initFolder(mb, e);
	}
	private void requestMail(MailBox mb)
	{
		Element content = new Element("content");
		new Element(content, "folder").getAttributes().put("name", mb.getName());
		
		sendMessage(controller, getName(), "get_mail_for_folder_request", content);
	}
	
	public void addMailListener(MailListener ml)
	{
		mailListeners.add(ml);
	}
	public void removeMailListener(MailListener ml)
	{
		mailListeners.remove(ml);
	}
	
	public void moduleMessage_folder_list(Element content)
	{
		for(Element e : content.getElements())
			initFolder(null, e);
	}
	public void moduleMessage_mail_for_folder(Element content)
	{
		Element folder = content.get("folder");
		String name;
		MailBox mb;
		
		if((mb = mailBoxes.get(name = folder.getAttributes().get("name"))) == null)
		{
			Debug.printWarning("Received extraneous \"mail_for_folder\" message for nonexistent MailBox " + name + ".  Ignoring...");
			return;
		}
		
		for(Element msg : folder.getElements())
		{
			Attributes a = msg.getAttributes();
			mb.add(new Message(Long.parseLong(a.get("id")), a.get("from"), a.get("subject"), a.get("cc"), a.get("bcc")));
		}
	}
	public void moduleMessage_message_data(Element content)
	{
		Element message = content.get("message");
		Attributes a = message.getAttributes();
		
		MailEvent e = new MailEvent();
		e.id = Long.parseLong(a.get("id"));
		e.from = a.get("from");
		e.cc = a.get("cc");
		
		for(Element attachment : message.getElements("attachment"))
			e.attachments.put(Integer.parseInt(attachment.getAttributes().get("id")), attachment.getAttributes().get("name"));
		
		e.message = message.get("body").toString();
		
		for(MailListener ml : mailListeners)
			ml.messageReceived(e);
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
		ArrayList<String> deps = new ArrayList<String>();
		deps.add("login");
		deps.add("tray");
		deps.add("filetransfer");
		
		return deps;
	}
}
