package netchat.widgets;

import netchat.*;
import netchat.module.ChatModule;
import netchat.module.chat.*;
import netchat.system.*;

import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;


public class MenuBar {
	
	private Menu menuBar = null;
	private Decorations parent = null;
	private FriendsList fl = null;
	int style;
	
	Menu fileMenu, friendsMenu, helpMenu = null;
	
	public MenuBar(Decorations parent, int style)
	{
		menuBar = new Menu(parent, SWT.BAR);
		
		this.parent = parent;
		this.style = style;
		
		initialize();
	}
	
	public MenuBar(Decorations parent, FriendsList fl, int style)
	{
		menuBar = new Menu(parent, SWT.BAR);
		
		this.parent = parent;
		this.fl = fl;
		this.style = style;
		
		initialize();
	}
	
	public void initialize()
	{
		//First
		if((style & NC.FILE) != 0)
			addFileMenu();
		
		if((style & NC.FRIENDS) != 0)
			addFriendsMenu();
		
		//Last
		if((style & NC.HELP) != 0)
			addHelpMenu();
	}
	
	public void addFileMenu()
	{
		MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");
		
		fileMenu = new Menu(parent, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);
		
		MenuItem newImItem = new MenuItem(fileMenu, SWT.PUSH);
		newImItem.setText("&New IM...");
		
		MenuItem closeItem = new MenuItem(fileMenu, SWT.PUSH);
		closeItem.setText("&Close");
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		MenuItem exitItem = new MenuItem(fileMenu, SWT.PUSH);
		exitItem.setText("&Exit NetChat");
		
		newImItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				//TODO
			}
		});
		
		closeItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				((Shell)parent).close();
			}
		});
		
		exitItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				Controller.getCurrent().shutDown();
				Controller.getCurrent().getDisplay().dispose();
				System.exit(0);
			}
		});
	}
	
	public void addFriendsMenu()
	{
		MenuItem friendsMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		friendsMenuHeader.setText("&Friends");
		
		friendsMenu = new Menu(parent, SWT.DROP_DOWN);
		friendsMenuHeader.setMenu(friendsMenu);
		
		addItem(friendsMenu, "&Add friend...").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				if(fl != null) fl.createAddFriendDialog();
			}
		});
		
		new MenuItem(friendsMenu, SWT.SEPARATOR);
		
		addItem(friendsMenu, "&Send File...").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				if(fl != null) fl.showFileTransferSelector();
			}
		});
		
		addItem(friendsMenu, "&Join Channel...").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				if(fl != null) fl.showChannelSelector();
			}
		});
	}
	
	public void addHelpMenu()
	{
		MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&Help");
		
		helpMenu = new Menu(parent, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);
		
		addItem(helpMenu, "&About NetChat").addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				displayAboutShell();
			}
		});
	}
	
	private MenuItem addItem(Menu parent, String name)
	{
		MenuItem mi = new MenuItem(parent, SWT.PUSH);
		mi.setText(name);
		
		return mi;
	}
	
	public Menu getMenuBar()
	{
		return menuBar;
	}
	
	public Menu getFileMenu()
	{
		return fileMenu;
	}
	
	public Menu getFriendsMenu()
	{
		return friendsMenu;
	}
	
	public Menu getHelpMenu()
	{
		return helpMenu;
	}
	
	public void displayAboutShell()
	{
		new AboutShell(Display.getCurrent(), SWT.CLOSE);
	}
}
