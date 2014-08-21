package netchat.module;

import netchat.*;
import netchat.widgets.ChannelSelector;
import netchat.xml.*;
import netchat.util.*;
import netchat.util.Timer;
import netchat.event.*;
import netchat.system.*;
import netchat.module.chat.*;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

import java.text.*;
import java.util.*;

import netchat.xml.Attributes;

/**
 * ChatModule is the default chat module of the NetChat system.
 * @author Andy Street, 2007
 */
 
public class ChatModule extends AbstractModule
{
	static String name = "chat";
	public static String version = "0.1a";
	public static String protocolVersion = "0.1a";
	
	ChatShell defaultChatShell = null;
	Controller controller = null;
	FriendsList friendsList = null;
	SystemTrayModule trayMod = null;
	FileTransferModule fileMod = null;
	
	Font boldFont = null;
	
	String username = null;
	
	TrayItem trayItem = null;
	ToolTip infoTip = null;
	boolean traySupported = false;
	
	HashMap<String, Integer> statusCodes = null;
	
	//Dates
	private Calendar defaultCalendar = null;
	private SimpleDateFormat dateFormat = null;
	private SimpleDateFormat timeFormat = null;
	private String dateFormatString = "yyyy-MM-dd HH:mm:ss";
	private String timeFormatString = "HH:mm:ss";
	
	private final int MAX_NOTIFY_MSG_LENGTH = 40; //Only 20 chars in a message notification
	
	int defaultXPos = 300;
	int defaultYPos = 300;
	int maxX = 600;
	int maxY = 800;
	
	ChannelSelector cs = null;
	
	
	FileTransferHandler ftHandler = null;
	
	HashMap<String, ChatComposite> chatPanes = null; // (username, chat pane)
	HashMap<ChatComposite, ChatShell> chatCompMap = null; // (composite, parent shell)
	
	HashMap<String, ChatComposite> groupChatPanes = null; // (username, chat pane)
	HashMap<ChatComposite, ChatShell> groupChatCompMap = null; // (composite, parent shell)
	
	public ChatModule(Controller con)
	{
		controller = con;
		username = controller.getUsername();
		
		chatPanes = new HashMap<String, ChatComposite>();
		chatCompMap = new HashMap<ChatComposite, ChatShell>();
		groupChatPanes = new HashMap<String, ChatComposite>();
		groupChatCompMap = new HashMap<ChatComposite, ChatShell>();
		statusCodes = new HashMap<String, Integer>();
		populateStatusCodeMap();
		
		defaultCalendar = Calendar.getInstance();
		
		dateFormat = new SimpleDateFormat(dateFormatString);
		timeFormat = new SimpleDateFormat(timeFormatString);
		
		FontData[] fontDatas = Display.getCurrent().getSystemFont().getFontData();
		FontData[] newFontDatas = new FontData[fontDatas.length];
		
		for(int i = 0; i < fontDatas.length; i++)
			newFontDatas[i] = new FontData(fontDatas[i].getName(), fontDatas[i].getHeight(), SWT.BOLD);
		
		boldFont = new Font(Display.getCurrent(), newFontDatas);
		
		friendsList = new FriendsList(this);
		defaultChatShell = new ChatShell(this, username);
		
		if((trayMod = (SystemTrayModule)(controller.getModule("tray"))) != null)
			if(traySupported = (trayItem = trayMod.getTrayItem()) != null)
				setUpTrayMenu();
		
		if((fileMod = (FileTransferModule)(controller.getModule("filetransfer"))) == null)
				Debug.printError("File transfer module could not be accessed!");
		else
			ftHandler = new FileTransferHandler(this, fileMod);
		
		loadFriendsList();
		requestBacklogs();
	}
	
	public void distributePrivateMessage(String msg, String user, int msgCode) //See NCChatGUI for msg codes
	{
		distributePrivateMessage(msg, user, null, msgCode);
	}
	public void distributePrivateMessage(String msg, String user, String date, int msgCode)
	{
		ChatComposite pane;
		
		pane = launchPrivateChatPane(user, false);
		
		pane.receiveMessage(msg, user, date, false, msgCode);
		
		if(!pane.getChatShell().getActivated())
			notifyBubble("NC Private Message : " + user, msg.length() > MAX_NOTIFY_MSG_LENGTH ? msg.substring(0, MAX_NOTIFY_MSG_LENGTH - 3) + " ..." : msg);
	}
	public void distributeGroupMessage(String msg, String user, String group, int msgCode) //See NCChatGUI for msg codes
	{
		launchGroupChatPane(group, false).receiveMessage(msg, user, null, false, msgCode);
	}
	public void populateStatusCodeMap()
	{
		statusCodes.put("online", NC.ONLINE);
		statusCodes.put("offline", NC.OFFLINE);
		statusCodes.put("away", NC.AWAY);
		statusCodes.put("busy", NC.BUSY);
	}
	public ChatComposite launchPrivateChatPane(String uname, boolean showMe)
	{
		ChatComposite pane;
		
		if((pane = chatPanes.get(uname)) == null)
		{
			pane = defaultChatShell.addChatPane(uname, false, showMe);
			chatPanes.put(uname, pane);
			chatCompMap.put(pane, defaultChatShell);
		}
		else
		{
			ChatShell shell = chatCompMap.get(pane);
			
			if(showMe)
				shell.showPane(pane);
			else	
				notifyPane(pane, shell, false);
		}
		
		return pane;
	}
	public ChatComposite launchGroupChatPane(String group, boolean showMe)
	{
		ChatComposite pane;
		
		if((pane = groupChatPanes.get(group)) == null)
		{
			pane = defaultChatShell.addChatPane(group, true, showMe);
			groupChatPanes.put(group, pane);
			groupChatCompMap.put(pane, defaultChatShell);
			
			Element content = new Element("content");
			new Element(content, "room").addText(group);
			sendMessage(controller, getName(), "room_subscribe", content);
			
			queryRoom(group);
		}
		else
		{
			ChatShell shell = groupChatCompMap.get(pane);
			
			if(showMe)
				shell.showPane(pane);
			else	
				notifyPane(pane, shell, true);
		}
		
		return pane;
	}
	public void notifyPane(ChatComposite pane, boolean isGroup)
	{
		ChatShell shell;
		if(isGroup)
			shell = groupChatCompMap.get(pane);
		else
			shell = chatCompMap.get(pane);
		
		notifyPane(pane, shell, isGroup);
	}
	public void notifyPane(ChatComposite pane, ChatShell shell, boolean isGroup)
	{	
		if(shell.getShell().isVisible())
		{
			CTabItem tabItem = pane.getTabItem();
			CTabItem selectedItem = shell.getTabFolder().getSelection();
			
			if(selectedItem != null && !tabItem.equals(selectedItem))
			{
				tabItem.setFont(boldFont);
			}
		}
		else
		{
			shell.show();
			CTabFolder folder = shell.getTabFolder();
			folder.setSelection(pane.getTabItem());
			pane.getMessageText().setFocus();
		}
	}
	public void removeChatPane(String uname)
	{
		ChatComposite pane = chatPanes.remove(uname);
		
		if(pane != null)
			chatCompMap.remove(pane);
		else
		{
			pane = groupChatPanes.remove(uname);
			if(pane != null)
				groupChatCompMap.remove(pane);
		}
	}
	public void loadFriendsList()
	{
		sendMessage(controller, "chat", "friends_list_request", null);
	}
	public void requestBacklogs()
	{
		sendMessage(controller, "chat", "backlog_request", null);
	}
	public void sendChatMessage(String s, String uname)
	{
		Element content = new Element("content");
		new Element(content, "properties").getAttributes().put("type", "private");
		new Element(content, "username").addText(uname);
		new Element(content, "message").addText(s);
		sendMessage(controller, "chat", "message", content);
	}
	public void sendGroupChatMessage(String s, String roomName)
	{
		Element content = new Element("content");
		new Element(content, "properties").getAttributes().put("type", "room");
		new Element(content, "room").addText(roomName);
		new Element(content, "message").addText(s);
		sendMessage(controller, "chat", "message", content);
	}
	public void addFriend(String name)
	{
		Element content = new Element("content");
		new Element(content, "username").addText(name);
		sendMessage(controller, "chat", "request_add_friend", content);
	}
	public void removeFriend(String name)
	{
		friendsList.removeFriend(name);
		
		Element content = new Element("content");
		new Element(content, "username").addText(name);
		sendMessage(controller, "chat", "remove_friend", content);
	}
	public void subscribeGroupChat(String roomName)
	{
		launchGroupChatPane(roomName, true);
	}
	public void unsubscribeGroupChat(String roomName)
	{
		Element content = new Element("content");
		new Element(content, "room").addText(roomName);
		sendMessage(controller, getName(), "room_unsubscribe", content);
	}
	public String getTime()
	{
		defaultCalendar.setTimeInMillis(System.currentTimeMillis());
		return timeFormat.format(defaultCalendar.getTime());
	}
	public void signOut()
	{
		controller.signOut();
	}
	private void setUpTrayMenu()
	{
		trayItem.addSelectionListener(new SelectionAdapter(){
			public void widgetDefaultSelected(SelectionEvent e) {
				friendsList.show();
			}
		});
		
		trayItem.setToolTipText("NetChat System - " + username);
		
		final Menu menu = new Menu(friendsList.getShell(), SWT.POP_UP);
		trayItem.addListener(SWT.MenuDetect, new Listener(){
			public void handleEvent(Event e) {
				menu.setVisible(true);
			}
		});
		
		addMenuItem(menu, "Show NetChat").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				friendsList.show();
			}
		});
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		addMenuItem(menu, "Sign Off").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				signOut();
			}
		});
		
		addMenuItem(menu, "Exit").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				controller.shutDown();
				controller.getDisplay().dispose();
				System.exit(0);
			}
		});
		
		infoTip = new ToolTip(friendsList.getShell(), SWT.BALLOON | SWT.ICON_INFORMATION);
		trayItem.setToolTip(infoTip);
	}
	public void notifyBubble(String title, String msg)
	{
		notifyBubble(title, msg, 5);
	}
	public void notifyBubble(String title, String msg, int duration)
	{
		if(!traySupported) return;
		
		infoTip.setText(title);
		infoTip.setMessage(msg);
		
		final Timer t = new Timer(Display.getCurrent(), duration * 1000, null);
		
		t.setActionListener(new ActionListener(){
			public void actionPerformed() {
				infoTip.setVisible(false);
				t.stop();
			}
		});
		
		infoTip.setVisible(true);
		t.start();
	}
	private MenuItem addMenuItem(Menu m, String text)
	{
		MenuItem mi = new MenuItem(m, SWT.PUSH);
		mi.setText(text);
		return mi;
	}
	public void disposeTray()
	{
		trayMod.disposeTray();
	}
	public boolean getTraySupported()
	{
		return traySupported;
	}
	public void requestRoomList()
	{
		sendMessage(controller, getName(), "room_list_request", null);
	}
	public void queryRoom(String name)
	{
		Element content = new Element("content");
		new Element(content, "room").addText(name);
		
		sendMessage(controller, getName(), "room_query", content);
	}
	public void sendTypingEvent(String name, boolean isTyping)
	{
		Element content = new Element("content");
		new Element(content, "to").addText(name);
		new Element(content, "from").addText(controller.getUsername());
		new Element(content, "is-typing").addText(isTyping);
		
		sendMessage(controller, getName(), "typing_event", content);
	}
	public void sendMessageUpdate(String message)
	{
		Element content = new Element("content");
		new Element(content, "username").addText(controller.getUsername());
		new Element(content, "message").addText(message);
		
		sendMessage(controller, getName(), "set_message", content);
	}
	
	public void moduleCommand_message(Element content)
	{
		boolean isGroup = content.get("properties").getAttributes().get("type").equals("room");
		String user = content.get("username").text();
		String msg = content.get("message").text();
		
		if(isGroup)
			distributeGroupMessage(msg, user, content.get("room").text(), NC.CHAT);
		else
			distributePrivateMessage(msg, user, NC.CHAT);
	}
	public void moduleCommand_typing_event(Element content)
	{
		ChatComposite chatPane;
		String from = content.get("from").text();
		if((chatPane = chatPanes.get(from)) != null)
			chatPane.typingEvent(Boolean.parseBoolean(content.get("is-typing").text()));
	}
	public void moduleCommand_room_list(Element content)
	{
		if(cs == null) return;
		
		ArrayList<String> rooms = new ArrayList<String>();
		
		for(Object o : content.getSubObjects())
			rooms.add(((Element)(o)).getAttributes().get("name"));
		
		cs.updateRooms(rooms);
		
		cs = null;
	}
	public void moduleCommand_room_info(Element content)
	{
		Element room = content.get("room");
		String roomName = room.getAttributes().get("name");
		
		ChatComposite comp;
		if((comp = groupChatPanes.get(roomName)) == null) return;
		
		ArrayList<String> users = new ArrayList<String>();
		for(Object o : room.getSubObjects())
			users.add(((Element)(o)).text());
		
		comp.setMembers(users);
	}
	public void moduleCommand_room_event(Element content)
	{
		String room = content.get("room").text();
		
		ChatComposite comp;
		if((comp = groupChatPanes.get(room)) == null) return;
		
		boolean isSubscribing = content.get("event").getAttributes().get("type").equals("subscribe");
		
		comp.updateMember(content.get("username").text(), isSubscribing);
	}
	public void moduleCommand_friends_list(Element content)
	{
		ArrayList<Element> friends = (ArrayList<Element>)(content.getSubObjects());
		Attributes attrs;
		String name;
		String status;
		String message;
		int statusCode;
		
		for(Element curr : friends)
		{
			attrs = curr.getAttributes();
			
			name = curr.text();
			status = attrs.get("online");
			
			if(status.toLowerCase().equals("true")) //Grrrrr....
				statusCode = statusCodes.get("online");
			else
				statusCode = statusCodes.get("offline");
			
			message = attrs.get("message");
			
			friendsList.addFriend(name, message, statusCode);
		}
		
		friendsList.expand("online");
	}
	public void moduleCommand_friend_status_update(Element content)
	{
		ArrayList<Element> friends = (ArrayList<Element>)(content.getSubObjects());
		Element curr;
		Attributes attrs;
		String name;
		String status;
		int statusCode;
		
		
		for(int i = 0; i < friends.size(); i++)
		{
			curr = friends.get(i);
			attrs = curr.getAttributes();
			
			name = (String)(curr.getSubObjects().get(0));
			status = attrs.getValue("online");
			
			if(status.toLowerCase().equals("true")) //Grrrrr....
				statusCode = statusCodes.get("online");
			else
				statusCode = statusCodes.get("offline");
			
			friendsList.updateFriendStatus(name, statusCode);
		}
	}
	public void moduleCommand_deny_friend(Element content)
	{
		String reason = (String)(content.get("reason").getSubObjects().get(0));
		String name = (String)(content.get("username").getSubObjects().get(0));
		
		Debug.println("Friend request for " + name + " denied.", 0);
		Debug.println("  Reason: " + reason, 0);
		
		friendsList.friendDenied(username, reason);
	}
	public void moduleCommand_authorize_friend(Element content)
	{
		int statusCode;
		
		Element curr = content.get("username");
		Attributes attrs = curr.getAttributes();
			
		String name = curr.text();
		String message = content.get("message").text();
		String status = attrs.getValue("online");
			
		if(status.toLowerCase().equals("true")) //Grrrrr....
			statusCode = statusCodes.get("online");
		else
			statusCode = statusCodes.get("offline");
		
		friendsList.friendApproved(name, message, statusCode);
	}
	public void moduleCommand_backlog(Element content)
	{
		ArrayList<Element> messages = (ArrayList<Element>)(content.getSubObjects());
		ArrayList<String> text;
		Element curr;
		Attributes attrs;
		String user;
		String msg;
		String date;
		
		for(int i = 0; i < messages.size(); i++)
		{
			msg = "";
			
			curr = messages.get(i);
			attrs = curr.getAttributes();
			
			user = attrs.getValue("src");
			date = attrs.getValue("sent");
			text = (ArrayList<String>)(curr.getSubObjects());
			
			for(int j = 0; j < text.size(); j++)
				msg += text.get(j);
			
			Debug.println("Distributing backlogged message...", 2);
			
			distributePrivateMessage(msg, user, date, NC.BACKLOG);
		}
	}
	public void moduleCommand_message_update(Element content)
	{
		String username = content.get("username").text();
		String message = content.get("message").text();
		
		friendsList.updateMessage(username, message);
	}
	public void moduleCommand_warning_message(Element content)
	{
		String msg = content.get("message").text();
		String user = content.get("username").text();
		
		distributePrivateMessage(msg, user, NC.WARNING);
	}
	public void moduleCommand_error_message(Element content)
	{
		String msg = content.get("message").text();
		
		Debug.printError(msg);
	}
	
	public ChatComposite getPrivateChatPane(String uname)
	{
		return chatPanes.get(uname);
	}
	public void setChannelSelector(ChannelSelector cs)
	{
		this.cs = cs;
	}
	public FileTransferModule getFileTransferModule()
	{
		return fileMod;
	}
	public FriendsList getFriendsList()
	{
		return friendsList;
	}
	public Controller getController()
	{
		return controller;
	}
	public String getBacklogInfoString()
	{
		return "The following messages were received while you were offline.";
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
		{
			final Iterator i = chatPanes.keySet().iterator();
			
			controller.getDisplay().syncExec(new Runnable(){
				public void run() {
					while(i.hasNext())
						chatPanes.get(i.next()).dispose();
				
					defaultChatShell.dispose();
					friendsList.dispose();
				}
			});
		}
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
