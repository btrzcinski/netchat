package netchat.module.chat;

import netchat.*;
import netchat.util.*;
import netchat.util.Timer;
import netchat.widgets.ContextMenu;
import netchat.event.*;
import netchat.module.*;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.layout.GridData;

import org.htmlparser.util.*;

import java.util.*;

public class ChatComposite extends Composite {

	private ChatModule chatMod = null;

	private CTabItem tabItem = null;
	private Image image = null;
	private Image typingImage = null;
	private String typingImageFile = "/icons/typing.png";
	
	private String myName = "";
	private String otherName = null;
	
	private GridData fillData = null;
	
	private SashForm sashForm = null;
	private Composite messageComposite = null;
	private StyledText convoText = null;
	private StyledText messageText = null;
	private Composite avatarComposite = null;
	private Label avatarLabel = null;
	
	private Color red = null;
	private Color blue = null;
	private Color gray = null;
	
	private ContextMenu cMenu = null;
	private Menu contextMenu = null;
	
	private ArrayList<Color> colors = null;
	private HashMap<String, Color> namesToColors = null;
	
	private int LIST_BUFFER = 5;
	
	private boolean firstMessage = true;
	private boolean hasBacklogged = false;

	private HashMap<String, TableItem> groupMembers = null;
	private ArrayList<String> members = null;
	private Table groupTable = null;
	private TableColumn groupColumn = null;
	private ChatShell parentShell = null;
	
	private Timer textChangeTimer = null;
	private boolean textChanged = false;
	private boolean isTyping = false;
	
	private boolean isGroupChat = false;
	private boolean enteredMessage = false;
	
	private int memNumber = 0;
	
	/**
	 * 
	 * @param parent parent of ChatComposite
	 * @param chatMod Chat module
	 * @param myName The current user's username
	 * @param otherName Name of other person in private chat, name of group in group chat
	 * @param style SWT style constant
	 * @param ncStyle NC style constant
	 */
	public ChatComposite(Composite parent, ChatModule chatMod, String myName, String otherName, CTabItem tabItem, ChatShell parentShell, int style, int ncStyle) {
		super(parent, style);
		
		this.chatMod = chatMod;
		this.myName = myName;
		this.otherName = otherName;
		this.tabItem = tabItem;
		this.image = tabItem.getImage();
		this.parentShell = parentShell;
		
		setBackgroundMode(SWT.INHERIT_FORCE);
		initialize();
		if((ncStyle & NC.GROUP_CHAT) != 0)
		{
			isGroupChat = true;
			groupMembers = new HashMap<String, TableItem>();
			members = new ArrayList<String>();
			namesToColors = new HashMap<String, Color>();
			initColors();
			createGroupList();
			initContextMenu();
		}
		else
		{
			initTextChangedTimer();
		}
	}
	
	private void initialize() {
		typingImage = new Image(Display.getCurrent(), getClass().getResourceAsStream(typingImageFile));
		
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		blue = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		gray = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
		
		fillData = new GridData();
		fillData.grabExcessHorizontalSpace = true;
		fillData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.grabExcessVerticalSpace = true;
		
		createSashForm();
		setLayout(gridLayout1);
		setSize(new Point(560, 300));
		layout();
		
		/*if(isGroupChat)
			addDisposeListener(new DisposeListener(){
				public void widgetDisposed(DisposeEvent e) {
					chatMod.unsubscribeGroupChat(otherName);
				}
			});*/
	}
	private void initTextChangedTimer()
	{	
		messageText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				if(enteredMessage)
					enteredMessage = false;
				else
					textChanged = true;
			}
		});
		
		textChangeTimer = new Timer(Display.getCurrent(), 500, new ActionListener(){
			public void actionPerformed() {
				if(isDisposed())
				{
					textChangeTimer.stop();
					return;
				}
				
				if(textChanged != isTyping)
				{
					isTyping = textChanged;
					chatMod.sendTypingEvent(otherName, textChanged);
				}
				
				textChanged = false;
				
				if(!isTyping && !isVisible())
				{
					textChangeTimer.stop();
				}
			}
		});
		
		textChangeTimer.start();
	}
	private void initColors()
	{
		Display d = Display.getCurrent();
		
		colors = new ArrayList<Color>();
		colors.add(red);
		colors.add(d.getSystemColor(SWT.COLOR_DARK_GREEN));
		//colors.add(new Color(Display.getCurrent(), 230, 236, 0)); //Yellow
		colors.add(new Color(Display.getCurrent(), 255, 128, 64)); //Orange
		colors.add(new Color(Display.getCurrent(), 0, 128, 192)); //Cyan
		colors.add(d.getSystemColor(SWT.COLOR_MAGENTA));
		colors.add(d.getSystemColor(SWT.COLOR_DARK_CYAN));
	}
	private void createGroupList()
	{
		GridData listGridData = new GridData();
		listGridData.grabExcessVerticalSpace = true;
		listGridData.widthHint = 80;
		listGridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		listGridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		
		groupTable = new Table(this, SWT.FULL_SELECTION | SWT.BORDER);
		groupColumn = new TableColumn(groupTable, SWT.NONE);
		groupTable.setLayoutData(listGridData);
		groupColumn.setWidth(listGridData.widthHint);
		groupTable.addControlListener(new ControlAdapter(){
			public void controlResized(ControlEvent e) {
				groupColumn.setWidth(groupTable.getClientArea().width);
			}
		});
		groupTable.addSelectionListener(new SelectionAdapter(){
			public void widgetDefaultSelected(SelectionEvent e) {
				chatMod.launchPrivateChatPane(((TableItem)(e.item)).getText(), true);
			}
		});
		groupTable.addListener(SWT.MeasureItem, new Listener(){
			public void handleEvent(Event e) {
				String text = ((TableItem)(e.item)).getText();
				if(text != null)
					e.height = e.gc.textExtent(text).y + LIST_BUFFER * 2;
			}
		});
		groupTable.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event e)
			{	
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
	}
	private void initContextMenu()
	{
		cMenu = new ContextMenu(getShell(), chatMod, NC.ADD_FRIEND | NC.PRIVATE_CHAT | NC.FILE_TRANSFER);
		contextMenu = cMenu.getContextMenu();
	
		groupTable.addListener(SWT.MenuDetect, new Listener(){
			public void handleEvent(Event e) {
				TableItem t = groupTable.getSelection()[0];
				
				String user = t.getText();
				contextMenu.setData(user);
				
				if(chatMod.getFriendsList().isFriend(user))
				{
					cMenu.getFileTransferItem().setEnabled(true);
					cMenu.getPrivateChatItem().setEnabled(true);
					cMenu.getAddFriendItem().setEnabled(false);
				}
				else
				{
					cMenu.getFileTransferItem().setEnabled(false);
					cMenu.getPrivateChatItem().setEnabled(false);
					cMenu.getAddFriendItem().setEnabled(true);
				}
				
				contextMenu.setVisible(true);
			}
		});
	}
	public void addGroupMember(String name)
	{
		TableItem t = new TableItem(groupTable, SWT.NONE);
		t.setText(name);
		groupMembers.put(name, t);
		members.add(name);
		
		if(name.equals(myName))
		{
			namesToColors.put(name, blue);
		}
		else
		{
			namesToColors.put(name, colors.get(memNumber));
			memNumber++;

			if(memNumber >= colors.size())
				memNumber = 0;
		}
		
		groupTable.redraw();
		groupTable.layout();
	}
	public void removeGroupMember(String name)
	{
		TableItem t;
		if((t = groupMembers.remove(name)) != null)
			t.dispose();
		members.remove(name);
		namesToColors.remove(name);
		
		groupTable.redraw();
		groupTable.layout();
	}
	
	private void createSashForm() {
		
		sashForm = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
		sashForm.setLayoutData(fillData);
		
		convoText = new StyledText(sashForm, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
		convoText.getCaret().setVisible(false);
		createMessageComposite();
	
		sashForm.setWeights(new int[] {65, 35});
	}

	private void createMessageComposite() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		
		messageComposite = new Composite(sashForm, SWT.NONE);
		messageComposite.setLayout(gridLayout);
		createAvatarComposite();
		messageText = new StyledText(messageComposite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		messageText.setLayoutData(fillData);
		
		messageText.addVerifyKeyListener(new VerifyKeyListener(){
			public void verifyKey(VerifyEvent e) {
				if((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) && e.stateMask != SWT.CTRL)
					e.doit = false;
			}
		});
		messageText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.character == SWT.CR || e.character == SWT.KEYPAD_CR)
				{
					if(e.stateMask == SWT.CTRL) return;
					
					//e.doit = false;
					
					String text = messageText.getText();
					//text = text.trim();
					
					if(!text.equals(""))
					{
						enteredMessage = true;
						messageText.setText("");
						
						if(isGroupChat)
							chatMod.sendGroupChatMessage(text, otherName);
						else
						{
							textChanged = false;
							isTyping = false;
							chatMod.sendChatMessage(Translate.encode(text), otherName);
							chatMod.sendTypingEvent(otherName, false);
						}
						
						receiveMessage(text, myName, true);
					}
				}
			}
		});
		
		messageText.setFocus();
	}

	private void createAvatarComposite() {
		avatarComposite = new Composite(messageComposite, SWT.NONE);
		avatarComposite.setLayout(new GridLayout());
		avatarLabel = new Label(avatarComposite, SWT.NONE);
		avatarLabel.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/anonymous-75.png")));
	}
	
	public void receiveMessage(String message, String user, boolean fromMe)
	{
		receiveMessage(message, user, null, fromMe, NC.NONE);
	}
	
	public void receiveMessage(String message, String user, boolean fromMe, boolean isGroup, int modifiers)
	{
		receiveMessage(message, user, null, fromMe, modifiers);
	}
	public void receiveWarning(String message)
	{
		receiveMessage(message, null, null, false, NC.WARNING);
	}
	public void receiveMessage(String message, String user, String date, boolean fromMe, int modifiers)
	{
		if((modifiers & NC.WARNING) != 0)
		{
			if(!firstMessage)
				message = "\r" + message;
			else
				firstMessage = false;
			
			StyleRange range = new StyleRange(convoText.getCharCount(), message.length(), gray, convoText.getBackground());
			range.fontStyle |= SWT.ITALIC;
			
			convoText.append(message);
			convoText.setStyleRange(range);
			
			convoText.setTopIndex(convoText.getLineCount() - 1);
			
			return;
		}
		
		if((modifiers & NC.INFO) != 0)
		{
			if(!firstMessage)
				message = "\r" + message;
			else
				firstMessage = false;
			
			StyleRange range = new StyleRange(convoText.getCharCount(), message.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY), convoText.getBackground());
			range.fontStyle |= SWT.ITALIC;
			
			convoText.append(message);
			convoText.setStyleRange(range);
		
			convoText.setTopIndex(convoText.getLineCount() - 1);
			
			return;
		}
		
		if(isGroupChat && !fromMe && members.indexOf(user) < 0) return;
		
		if((modifiers & NC.BACKLOG) != 0)
		{
			if(!hasBacklogged)
			{
				hasBacklogged = true;
				String backlogMessage = chatMod.getBacklogInfoString();
				
				if(!firstMessage)
					backlogMessage = "\r" + backlogMessage;
				else
					firstMessage = false;
				
				StyleRange range = new StyleRange(convoText.getCharCount(), backlogMessage.length(), gray, convoText.getBackground());
				range.fontStyle |= SWT.ITALIC;
				
				convoText.append(backlogMessage);
				convoText.setStyleRange(range);
			}
		}
		
		int end = convoText.getCharCount();
		
		if(date == null)
			date = chatMod.getTime();
		
		String header = user + " [" + date + "]";
		message = ": " + message;
		
		if(!firstMessage)
			header = "\r" + header;
		else
			firstMessage = false;
		
		convoText.append(header);
		
		StyleRange range = null;
		
		if(fromMe)
			range = new StyleRange(end, header.length(), blue, convoText.getBackground());
		else
			if(isGroupChat)
			{
				range = new StyleRange(end, header.length(), colors.get(members.indexOf(user) % colors.size()), convoText.getBackground());
			}
			else
				range = new StyleRange(end, header.length(), red, convoText.getBackground());
		
		convoText.setStyleRange(range);
		convoText.append(message);
		
		convoText.setTopIndex(convoText.getLineCount() - 1);
	}
	public void receiveInfo(String message)
	{
		receiveMessage(message, null, null, false, NC.INFO);
	}
	public void setMembers(ArrayList<String> mems)
	{
		for(TableItem t : groupMembers.values())
			t.dispose();
		
		groupMembers = new HashMap<String, TableItem>();
		members = new ArrayList<String>();
		namesToColors = new HashMap<String, Color>();
		
		for(String s : mems)
			addGroupMember(s);
	}
	public void updateMember(String member, boolean isSubscribing)
	{
		if(!isGroupChat) 
		{
			Debug.printError("Trying to update group members in a private chat!");
			return;
		}
		
		if(isSubscribing)
		{
			addGroupMember(member);
			receiveInfo(member + " joined " + otherName);
		}
		else
		{
			removeGroupMember(member);
			receiveInfo(member + " left " + otherName);
		}
	}
	public CTabItem getTabItem()
	{
		return tabItem;
	}
	public void typingEvent(boolean isTyping)
	{	
		if(isTyping)
			getTabItem().setImage(typingImage);
		else
			getTabItem().setImage(image);
	}
	
	public void setImage(Image i)
	{
		image = i;
		getTabItem().setImage(i);
	}
	
	public void restartTypingTimer()
	{
		if(isGroupChat) return;
		
		if(!textChangeTimer.isRunning())
		{
			textChangeTimer.start();
		}
	}
	
	public StyledText getMessageText()
	{
		return messageText;
	}
	
	public ChatShell getChatShell()
	{
		return parentShell;
	}
	public String getName()
	{
		return otherName;
	}
	public boolean isGroupChat()
	{
		return isGroupChat;
	}
	public void dispose()
	{
		super.dispose();
		image.dispose();
	}
}
