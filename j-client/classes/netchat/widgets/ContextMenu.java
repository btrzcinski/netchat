package netchat.widgets;

import netchat.*;
import netchat.module.ChatModule;
import netchat.module.chat.*;
import netchat.system.*;

import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;


public class ContextMenu {
	
	private Menu contextMenu = null;
	
	MenuItem addFriendItem = null;
	MenuItem removeFriendItem = null;
	MenuItem privateChatItem = null;
	MenuItem fileTransferItem = null;
	
	private Decorations parent = null;
	private ChatModule chatMod = null;
	int style;
	
	public ContextMenu(Decorations parent, ChatModule chatMod, int style)
	{
		contextMenu = new Menu(parent, SWT.POP_UP);
		
		this.parent = parent;
		this.chatMod = chatMod;
		this.style = style;
		
		initialize();
	}
	
	public void initialize()
	{	
		//First
		if((style & NC.ADD_FRIEND) != 0)
		{
			addFriendItem = new MenuItem(contextMenu, SWT.PUSH);
			addFriendItem.setText("Add Friend");
			addFriendItem.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent arg0) {
					chatMod.addFriend((String)(contextMenu.getData()));
				}
			});
		}
		
		if((style & NC.PRIVATE_CHAT) != 0)
		{
			privateChatItem = new MenuItem(contextMenu, SWT.PUSH);
			privateChatItem.setText("Private Chat");
			privateChatItem.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent arg0) {
					chatMod.launchPrivateChatPane((String)(contextMenu.getData()), true);
				}
			});
		}
		
		if((style & NC.REMOVE_FRIEND) != 0)
		{
			removeFriendItem = new MenuItem(contextMenu, SWT.PUSH);
			removeFriendItem.setText("Remove Friend");
			removeFriendItem.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent arg0) {
					String message = "Are you sure you want to remove " + contextMenu.getData() + "?";
					MessageBox mb = new MessageBox(chatMod.getFriendsList().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					mb.setText("Remove Friend");
					mb.setMessage(message);
					
					if(mb.open() == SWT.YES)
						chatMod.removeFriend((String)(contextMenu.getData()));
				}
			});
		}
		
		//Last
		if((style & NC.FILE_TRANSFER) != 0)
		{
			new MenuItem(contextMenu, SWT.SEPARATOR);
			
			fileTransferItem = new MenuItem(contextMenu, SWT.PUSH);
			fileTransferItem.setText("Send File...");
			fileTransferItem.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent arg0) {
					chatMod.getFriendsList().showFileTransferSelector((String)(contextMenu.getData()));
				}
			});
		}
	}
	
	private MenuItem addItem(Menu parent, String name)
	{
		MenuItem mi = new MenuItem(parent, SWT.PUSH);
		mi.setText(name);
		
		return mi;
	}
	public Menu getContextMenu()
	{
		return contextMenu;
	}
	
	public MenuItem getAddFriendItem()
	{
		return addFriendItem;
	}
	
	public MenuItem getRemoveFriendItem()
	{
		return removeFriendItem;
	}
	
	public MenuItem getPrivateChatItem()
	{
		return privateChatItem;
	}
	
	public MenuItem getFileTransferItem()
	{
		return fileTransferItem;
	}
}
