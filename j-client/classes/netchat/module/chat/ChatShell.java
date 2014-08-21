package netchat.module.chat;

import netchat.*;
import netchat.event.*;
import netchat.widgets.*;
import netchat.module.*;
import netchat.util.*;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;

public class ChatShell {

	private ChatModule chatMod = null;
	private FriendsList friendsList = null;
	
	private String myName = "";
	
	private String smallLogoFile = "/icons/nc.png";
	private String groupLogoFile = "/icons/group-chat.png";
	
	private Shell chatShell = null;
	private CTabFolder tabFolder = null;
	private int width = 550;
	private int height = 375;
	
	private boolean firstOpen = true;
	private boolean isActivated = false;
	
	public ChatShell(ChatModule chatMod, String myName)
	{
		this.chatMod = chatMod;
		this.myName = myName;
		this.friendsList = chatMod.getFriendsList();
		
		createChatShell();
		
		hide();
	}
	
	private void createChatShell() {
		chatShell = new Shell();
		chatShell.setText("Shell");
		createCTabFolder();
		chatShell.setSize(new Point(width, height));
		chatShell.setLayout(new FillLayout());
		
		chatShell.setText("NetChat Instant Messaging");
		chatShell.setBackgroundMode(SWT.INHERIT_FORCE);
		chatShell.setMenuBar(new MenuBar(chatShell, chatMod.getFriendsList(), NC.FILE | NC.FRIENDS | NC.HELP).getMenuBar());
		chatShell.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(smallLogoFile)));
		
		chatShell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e) {
				//Don't dispose me
				e.doit = false;
				
				hide();
			}
			public void shellActivated(ShellEvent e)
			{
				isActivated = true;
			}
			public void shellDeactivated(ShellEvent e)
			{
				isActivated = false;
			}
		});
		
		chatShell.setLocation(400, 300);
	}
	
	private void createCTabFolder() {
		Display display = Display.getCurrent();
		
		tabFolder = new CTabFolder(chatShell, SWT.BORDER | SWT.CLOSE | SWT.TOP);
		//cTabFolder.setMRUVisible(true);
		tabFolder.setSelectionBackground(new Color[] {
														new Color(display, 50, 150, 250),
														new Color(display, 75, 200, 255)
													}, new int[] { 75 }, true);
		
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter(){
			public void close(CTabFolderEvent e) {
				ChatComposite chatComp = (ChatComposite)(((CTabItem)(e.item)).getControl());
				String name = chatComp.getName();
				chatMod.removeChatPane(name);
				
				if(chatComp.isGroupChat())
					chatMod.unsubscribeGroupChat(name);
				
				if(tabFolder.getItems().length < 2)
					hide();
			}
		});
		tabFolder.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				CTabItem tabItem = (CTabItem)(e.item);
				ChatComposite chatComp = (ChatComposite)(tabItem.getControl());
				chatComp.restartTypingTimer();
				
				tabItem.setFont(null);
				
				if(chatComp.isGroupChat())
					chatShell.setText("NetChat Group - " + ((CTabItem)(e.item)).getText());
				else
					chatShell.setText("NetChat IM - " + myName + " : " + ((CTabItem)(e.item)).getText());
			}
		});
	}
	public void show()
	{
		if(firstOpen)
		{
			chatShell.open();
			chatShell.layout();
			firstOpen = false;
		}
		
		chatShell.setVisible(true);
	}
	public void hide()
	{
		chatShell.setVisible(false);
	}
	public ChatComposite addChatPane(String otherName, boolean isGroup, boolean showMe)
	{
		CTabItem chatTabItem = new CTabItem(tabFolder, SWT.NONE);
		
		if(isGroup)
		{
			chatTabItem.setText(otherName);
			chatTabItem.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(groupLogoFile)));
		}
		else
		{
			chatTabItem.setText(otherName);
			chatTabItem.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(friendsList.getStatusIcon(friendsList.getStatus(otherName)))));
		}
		
		ChatComposite chatComp;
		if(isGroup)
		{
			chatShell.setText("NetChat Group - " + otherName);
			chatComp = new ChatComposite(tabFolder, chatMod, myName, otherName, chatTabItem, this, SWT.NONE, NC.GROUP_CHAT);
		}
		else
		{
			chatShell.setText("NetChat IM - " + myName + " : " + otherName);
			chatComp = new ChatComposite(tabFolder, chatMod, myName, otherName, chatTabItem, this, SWT.NONE, NC.NONE);
		}
		
		chatTabItem.setControl(chatComp);
		
		if(showMe)
			showPane(chatComp);
		else
			chatMod.notifyPane(chatComp, this, isGroup);
		
		return chatComp;
	}
	public void showPane(ChatComposite chatComp)
	{
		if(chatComp == null) return;
		
		show();
		chatComp.getTabItem().setFont(null);
		tabFolder.setSelection(chatComp.getTabItem());
		chatComp.getMessageText().setFocus();
	}
	public CTabFolder getTabFolder()
	{
		return tabFolder;
	}
	public Shell getShell()
	{
		return chatShell;
	}
	public boolean getActivated()
	{
		return isActivated;
	}
	
	public void dispose()
	{
		chatShell.dispose();
	}
}
