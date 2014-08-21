package netchat.module.chat;

import netchat.*;
import netchat.event.*;
import netchat.util.Debug;
import netchat.widgets.*;
import netchat.module.*;
import netchat.system.*;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.SWT;

import org.htmlparser.util.*;

import java.util.*;

public class FriendsList {

	private ChatModule chatMod = null;
	
	private String smallLogoFile = "/icons/nc.png";
	private String onlineSymbol = "/icons/nc.png";
	private String offlineSymbol = "/icons/nc-gray.png";
	private String unknownSymbol = "/icons/unknown.png";
	
	private GridData fillData = null;
	
	private Shell listShell = null;
	private Composite headComposite = null;
	private String logoIconFile = "netchat-75.png";
	private Tree listTree = null;
	private TreeColumn listColumn = null;
	
	private Shell toolTipShell = null;
	
	private HashMap<String, TreeItem> friends = null;
	
	private int width = 300;
	private int height = 550;
	
	final int ROW_BUFFER = 5;
	final int COL_BUFFER = 0;
	final int TEXT_OFFSET = 5;
	
	private int imageSize = 16;
	private Composite avatarComposite = null;
	private Label avatarLabel = null;
	
	private int avatarImageSize = 50;
	private String defaultAvatarFile = "/icons/anonymous-50.png";
	
	private Composite centeredComposite = null;
	private Composite infoComposite = null;
	private EditableLabel statusLabel = null;
	private EditableLabel messageLabel = null;
	
	private HashMap<String, String> messages = null;
	private HashMap<String, ArrayList<TreeItem>> groups = null;
	private HashMap<String, TreeItem> groupHeaders = null;
	private HashMap<Integer, String> groupStatus = null;
	private HashMap<String, String> friendsToGroups = null;
	private HashMap<TreeItem, Image> images = null;
	private HashMap<String, Image> imageRegistry = null;
	
	private Menu contextMenu = null;
	
	private boolean avatarBeforeText = true;
	
	public FriendsList(ChatModule chatMod)
	{
		this.chatMod = chatMod;
		
		messages = new HashMap<String, String>();
		groups = new HashMap<String, ArrayList<TreeItem>>();
		groupHeaders = new HashMap<String, TreeItem>();
		groupStatus = new HashMap<Integer, String>();
		friendsToGroups = new HashMap<String, String>();
		images = new HashMap<TreeItem, Image>();
		imageRegistry = new HashMap<String, Image>();
		
		initImageRegistry();
		createListShell();
		initContextMenu();
		run();
	}
	
	private void initImageRegistry()
	{
		addImage(defaultAvatarFile);
		addImage(onlineSymbol);
		addImage(offlineSymbol);
		addImage(unknownSymbol);
	}
	private void addImage(String imageName)
	{
		imageRegistry.put(imageName, new Image(Display.getCurrent(), getClass().getResourceAsStream(imageName)));
	}
	
	private void createAvatarComposite() {
		avatarComposite = new Composite(centeredComposite, SWT.BORDER);
		avatarComposite.setLayout(new GridLayout());
		avatarLabel = new Label(avatarComposite, SWT.NONE);
		avatarLabel.setImage(getScaledImage(imageRegistry.get(defaultAvatarFile), avatarImageSize, avatarImageSize));
	}

	private void createInfoComposite() {
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.marginHeight = 0;
		gridLayout2.marginWidth = 0;
		infoComposite = new Composite(centeredComposite, SWT.NONE);
		infoComposite.setLayoutData(fillData);
		infoComposite.setLayout(gridLayout2);
		createStatusLabel();
		createMessageLabel();
	}

	private void createStatusLabel() {
		statusLabel = new EditableLabel(infoComposite, SWT.BORDER);
		statusLabel.setLayoutData(fillData);
		statusLabel.setText(Controller.getCurrent().getUsername());
		statusLabel.setEnabled(false);
	}

	private void createMessageLabel() {

		messageLabel = new EditableLabel(infoComposite, SWT.BORDER);
		messageLabel.setLayoutData(fillData);
		messageLabel.setText("Click to set a Message");
		messageLabel.setEmptyText("Click to set a Message");
		messageLabel.addLabelChangeListener(new LabelChangeListener(){
			public void textChanged(ChangeEvent e) {
				if(!e.text.equals(e.oldText))
				{
					chatMod.sendMessageUpdate(Translate.encode(e.text));
					updateMessage(Controller.getCurrent().getUsername(), e.text);
				}
			}
		});
	}
	
	private void run()
	{
		friends = new HashMap<String, TreeItem>();
		
		groupHeaders.get("online").setExpanded(true);
		
		listShell.open();
		listShell.setActive();
	}
	
	private void createListShell() {
		fillData = new GridData();
		fillData.grabExcessHorizontalSpace = true;
		fillData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.grabExcessVerticalSpace = true;
		
		listShell = new Shell(SWT.SHELL_TRIM);
		listShell.setText("NetChat Friends List");
		listShell.setSize(new Point(width, height));
		listShell.setLayout(new GridLayout());
		listShell.setMenuBar(new MenuBar(listShell, this, NC.FILE | NC.FRIENDS | NC.HELP).getMenuBar());
		listShell.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(smallLogoFile)));
		
		listShell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e) {
				//Don't dispose me
				e.doit = false;
				
				hide();
			}
		});
		
		createHeadComposite();
		
		createListTree();
		
		
		//TODO: Height should be calc'ed by finding the size of 2-3 table rows.
		listShell.setMinimumSize(listShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		listShell.setSize(listShell.computeSize(-1, -1).x, height);
	}
	
	private void createListTree()
	{
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		
		listTree = new Tree(listShell, SWT.FULL_SELECTION | SWT.BORDER);
		listTree.setHeaderVisible(false);
		listTree.setLayoutData(gridData1);
		listTree.setLinesVisible(false);
		
		listColumn = new TreeColumn(listTree, SWT.NONE);
		
		listTree.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e)
			{
				listColumn.setWidth(listTree.getClientArea().width);
			}
		});
		
		listTree.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event e)
			{
				e.height = imageSize + ROW_BUFFER * 2;
			}
		});
		
		listTree.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event e)
			{
				//If it is a friend entry...
				//...we don't want the default foreground painting
				if(images.get((TreeItem)(e.item)) != null)
					e.detail &= ~SWT.FOREGROUND;
				
				//Only do stuff if we're doing a selection paint
				if((e.detail & SWT.SELECTED) == 0) return;
				
				GC gc = e.gc;
				Color oldForeground = gc.getForeground();
				Color oldBackground = gc.getBackground();
				
				gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION));
				gc.setBackground(new Color(Display.getCurrent(), 25, 65, 230));
				
				gc.fillGradientRectangle(e.x, e.y, e.width, e.height, true);
				
				gc.setBackground(oldBackground);
				gc.setForeground(oldForeground);
				
				//Remove paint selected from the todo list
				e.detail &= ~SWT.SELECTED;
			}
		});
		
		listTree.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event e) {
				GC gc = e.gc;
				TreeItem item = (TreeItem)(e.item);
				String text = item.getText();
				Image i = images.get(item);
				
				Font oldFont = null;
				Color oldForeground = null;
				
				//System.out.println("Item: " + e.item + "Distance: " + (e.x - listTree.getClientArea().x));
				
				Point p = gc.textExtent(text);
				int textHeight = p.y;
				int textWidth = p.x;
				
				if(i == null) return;
				
				int status = ((Integer)(item.getData())).intValue();
				if((status & NC.OFFLINE) != 0)
				{
					if(gc.getFont().getFontData().length > 1)
						Debug.println("WARNING: Ignoring layers of GC Font while drawing TreeItems", 0);

					oldFont = new Font(Display.getCurrent(), gc.getFont().getFontData());
					gc.getFont().getFontData()[0].setStyle(SWT.ITALIC);

					oldForeground = gc.getForeground();
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
				}
				
				gc.drawImage(i, avatarBeforeText ? e.x : e.x + textWidth + TEXT_OFFSET, imageSize > textHeight ? e.y + ROW_BUFFER : e.y + ((e.height - textHeight) / 2));
				gc.drawText(text, avatarBeforeText ? e.x + imageSize + TEXT_OFFSET : e.x, imageSize > textHeight ? e.y + ((e.height - textHeight) / 2) : e.y + ROW_BUFFER, true);
				
				if(oldFont != null)
					gc.setFont(oldFont);
				
				if(oldForeground != null)
					gc.setForeground(oldForeground);
					
			}
		});
		listTree.addSelectionListener(new SelectionAdapter(){
			public void widgetDefaultSelected(SelectionEvent e) {
				TreeItem t = (TreeItem)(e.item);
				
				if(t.getParentItem() != null)
					chatMod.launchPrivateChatPane(((TreeItem)(e.item)).getText(), true);
				else
					((TreeItem)(e.item)).setExpanded(!t.getExpanded());
			}
		});
		
		final Listener mouseToolListener = new Listener(){
			public void handleEvent(Event e) {
				Rectangle rect = ((Composite)(e.widget)).getClientArea();
				
				if((e.x < 0 || e.y < 0 || rect.width <= e.x || rect.height <= e.y) && toolTipShell != null && !toolTipShell.isDisposed())
				{
					toolTipShell.dispose();
					toolTipShell = null;
				}
			}
		};
		
		Listener toolTipListener = new Listener(){
			public void handleEvent(Event e) {
				switch(e.type){
					case SWT.KeyDown:
					case SWT.MouseMove:
					{
						if(toolTipShell != null && !toolTipShell.isDisposed())
						{
							toolTipShell.dispose();
							toolTipShell = null;
						}
						
						break;
					}
					case SWT.MouseHover:
					{
						if(toolTipShell != null) return;
						
						TreeItem t = listTree.getItem(new Point(e.x, e.y));

						if(t == null) return;
						if(t.getParentItem() == null) return;
						
						String name = t.getText();
						
						toolTipShell = new Shell(Display.getCurrent(), SWT.TOOL | SWT.ON_TOP);
						//toolTipShell.setSize(220, 85);
						Point displayLoc = listTree.toDisplay(e.x, e.y);
						toolTipShell.setLocation(displayLoc.x, displayLoc.y);
						toolTipShell.setLayout(new FillLayout());
						
						ToolTipComposite comp = new ToolTipComposite(toolTipShell, name, messages.get(name), imageRegistry.get(getStatusIcon(((Integer)t.getData()).intValue())), SWT.NONE);
						comp.addListener(SWT.MouseExit, mouseToolListener);
						comp.addListener(SWT.MouseDown, mouseToolListener);
						
						toolTipShell.pack();
						toolTipShell.setVisible(true);
					}
				}
			}
		};
		
		listTree.addListener(SWT.MouseMove, toolTipListener);
		listTree.addListener(SWT.KeyDown, toolTipListener);
		listTree.addListener(SWT.MouseHover, toolTipListener);
		
		initGroups();
	}
	private void initContextMenu()
	{
		contextMenu = new ContextMenu(listShell, chatMod, NC.REMOVE_FRIEND | NC.PRIVATE_CHAT | NC.FILE_TRANSFER).getContextMenu();
	
		listTree.addListener(SWT.MenuDetect, new Listener(){
			public void handleEvent(Event e) {
				TreeItem t = listTree.getSelection()[0];
				
				if(t.getParentItem() != null)
				{
					String user = t.getText();
					contextMenu.setData(user);
					contextMenu.setVisible(true);
				}
			}
		});
	}
	private void initGroups()
	{
		addGroup("online");
		addGroup("offline");
		
		groupStatus.put(NC.ONLINE, "online");
		groupStatus.put(NC.AWAY, "online");
		groupStatus.put(NC.BUSY, "online");
		groupStatus.put(NC.OFFLINE, "offline");
	}
	
	private void addGroup(String name)
	{
		TreeItem t = new TreeItem(listTree, SWT.NONE);
		t.setText(name.substring(0, 1).toUpperCase() + name.substring(1));
		groupHeaders.put(name, t);
	}
	
	private void createHeadComposite() {
		GridData gridData2 = new GridData();
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessVerticalSpace = true;
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		GridLayout gridLayout = new GridLayout();
		GridData gridData = new GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		
		headComposite = new Composite(listShell, SWT.BORDER);
		headComposite.setLayout(gridLayout);
		headComposite.setLayoutData(gridData);
		
		centeredComposite = new Composite(headComposite, SWT.NONE);
		centeredComposite.setLayout(gridLayout1);
		centeredComposite.setLayoutData(fillData);
		
		createAvatarComposite();
		createInfoComposite();
	}
	
	public void friendApproved(String name, String message, int status)
	{
		addFriend(name, message, status);
	}
	public void friendDenied(String user, String reason)
	{
		MessageBox m = new MessageBox(listShell, SWT.OK | SWT.ICON_ERROR);
		m.setText("Friend Request Denied");
		m.setMessage("Your friend request for user " + user + " was denied.\nReason: " + reason);
		m.open();
	}
	public void addFriend(String name, String message, int status)
	{
		Debug.println("Adding friend " + name + " with status " + status, 2);
				
		try
		{
			messages.put(name, message);
			
			String groupName = groupStatus.get(status);
			
			TreeItem t = new TreeItem(groupHeaders.get(groupName), SWT.NONE);
			t.setText(name);

			t.setData(new Integer(status));
			
			Image newImage = imageRegistry.get(getStatusIcon(status));
			images.put(t, newImage);
			
			ChatComposite chatPane;
			if((chatPane = chatMod.getPrivateChatPane(name)) != null)
				chatPane.setImage(newImage);
			
			ArrayList<TreeItem> groupFriends = groups.get(groupName);
			if(groupFriends == null)
			{
				groupFriends = new ArrayList<TreeItem>();
				groups.put(groupName, groupFriends);
			}
			
			groupFriends.add(t);
			friendsToGroups.put(name, groupName);
			friends.put(name, t);
		}
		catch(Exception e)
		{
			Debug.printError("Could not find group to associate with status " + status + ".");
			return;
		}
		
		listTree.layout();
	}
	public void updateFriendStatus(String friend, int statusCode)
	{
		TreeItem t = friends.get(friend);
		
		try
		{
			String groupName;
			if(!(groupName = groupStatus.get(statusCode)).equals(friendsToGroups.get(friend)))
			{
				t.dispose();
				t = new TreeItem(groupHeaders.get(groupName), SWT.NONE);
				t.setText(friend);
				friendsToGroups.put(friend, groupName);
				friends.put(friend, t);
			}
		}
		catch(Exception e)
		{
			Debug.printError("Could not find group to associate with status " + statusCode + " and user " + friend + ".");
			e.printStackTrace();
			return;
		}
		
		switch(statusCode)
		{
			case NC.ONLINE:
				chatMod.notifyBubble("NC Friend Alert", "Friend " + friend + " has signed on.");
				System.out.println("---> " + friend + " signed on.");
				break;
			
			case NC.OFFLINE:
				chatMod.notifyBubble("NC Friend Alert", "Friend " + friend + " has signed off.");
				System.out.println("---> " + friend + " signed off.");
				break;
			
			default: Debug.println("Unknown friend status in setAddToList...", 0); return;
		}
		
		Image i;
		Image newImage = imageRegistry.get(getStatusIcon(statusCode));
		
		ChatComposite chatPane;
		if((chatPane = chatMod.getPrivateChatPane(friend)) != null)
			chatPane.setImage(newImage);
		
		if((i = images.put(t, newImage)) != null)
			i.dispose();
		
		t.setData(new Integer(statusCode));
	}
	public void updateMessage(String username, String message)
	{	
		if(friends.get(username) == null && !username.equals(Controller.getCurrent().getUsername()))
		{
			Debug.printError("Trying to update message for non-friend!");
			return;
		}
		
		if(message == null) message = "";

		messages.put(username, message);
		
		if(username.equals(Controller.getCurrent().getUsername())) return;
		if(!message.equals(""))
		{
			chatMod.notifyBubble("NC Message Update : " + username, message);
		}
	}
	public void removeFriend(String friend)
	{
		TreeItem t;
		if((t = friends.remove(friend)) != null)
			t.dispose();
		
		friendsToGroups.remove(friend);
		friends.remove(friend);
	}
	
	public Image getScaledImage(Image i, int width, int height)
	{
		return new Image(Display.getCurrent(), i.getImageData().scaledTo(width, height));
	}
	
	public void createAddFriendDialog()
	{
		new AddFriendShell(chatMod);
	}
	public void showFileTransferSelector()
	{
		new FileTransferSelector(chatMod.getFileTransferModule());
	}
	public void showFileTransferSelector(String user)
	{
		new FileTransferSelector(chatMod.getFileTransferModule(), user);
	}
	public void showChannelSelector()
	{
		new ChannelSelector(chatMod);
	}
	
	public void show()
	{	
		listShell.setVisible(true);
		listShell.forceActive();
	}
	public void hide()
	{
		listShell.setVisible(false);
	}
	public void expand(String header)
	{
		groupHeaders.get(header).setExpanded(true);
	}
	public boolean isFriend(String friend)
	{
		return (friends.get(friend) != null);
	}
	public int getStatus(String user)
	{
		TreeItem t;
		if((t = friends.get(user)) == null) return NC.NULL;
		
		return ((Integer)(t.getData())).intValue();
		
	}
	public String getStatusIcon(int status)
	{
		switch(status)
		{
			case NC.OFFLINE: return offlineSymbol;
			case NC.AWAY:
			case NC.BUSY:
			case NC.ONLINE: return onlineSymbol;
			default: return unknownSymbol;
		}
	}
	
	public Shell getShell()
	{
		return listShell;
	}
	public void dispose()
	{
		listShell.dispose();
	}
}
