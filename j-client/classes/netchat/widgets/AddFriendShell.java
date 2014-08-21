package netchat.widgets;

import netchat.module.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;

public class AddFriendShell
{

	private String smallLogoFile = "/icons/nc.png";
	private String logoFile = "/icons/buddy-50.png";
	
	private Shell friendShell = null;
	private Composite titleComposite = null;
	private Composite mainComposite = null;
	private Composite buttonComposite = null;
	private Label imageLabel = null;
	private Label titleLabel = null;
	private Button okButton = null;
	private Button cancelButton = null;
	private Label usernameLabel = null;
	private Text usernameText = null;
	private ChatModule chatMod;

	public AddFriendShell(ChatModule chatMod)
	{
		this.chatMod = chatMod;
		
		createFriendShell();
		
		friendShell.open();
	}
	
	private void createFriendShell()
	{
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		
		friendShell = new Shell(SWT.CLOSE | SWT.MIN);
		friendShell.setText("Add a Friend...");
		friendShell.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(smallLogoFile)));
		friendShell.setLayout(gridLayout);
		friendShell.setSize(new Point(300, 190));
		friendShell.setLocation(300, 300);
		
		createTitleComposite();
		createMainComposite();
		createButtonComposite();
		
		friendShell.setDefaultButton(okButton);
	}

	private void createTitleComposite() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		gridLayout1.marginHeight = 10;
		gridLayout1.marginWidth = 10;
		gridLayout1.horizontalSpacing = 10;
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		titleComposite = new Composite(friendShell, SWT.BORDER);
		titleComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		titleComposite.setLayout(gridLayout1);
		titleComposite.setLayoutData(gridData);
		titleComposite.setBackgroundMode(SWT.INHERIT_FORCE);
		
		imageLabel = new Label(titleComposite, SWT.NONE);
		imageLabel.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(logoFile)));
		//imageLabel.setImage(new Image(Display.getCurrent(), "C:/Documents and Settings/ANDY/workspace/Netchat/icons/messenger-50.png"));
		titleLabel = new Label(titleComposite, SWT.NONE);
		titleLabel.setText("Add a Friend...");
		titleLabel.setFont(new Font(Display.getDefault(), "Tahoma", 18, SWT.NORMAL));
	}

	private void createMainComposite() {
		GridData gridData5 = new GridData();
		gridData5.grabExcessHorizontalSpace = true;
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 2;
		gridLayout3.horizontalSpacing = 7;
		gridLayout3.marginWidth = 35;
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		
		mainComposite = new Composite(friendShell, SWT.NONE);
		mainComposite.setLayoutData(gridData1);
		mainComposite.setLayout(gridLayout3);
		usernameLabel = new Label(mainComposite, SWT.NONE);
		usernameLabel.setText("Username:");
		usernameText = new Text(mainComposite, SWT.BORDER);
		usernameText.setLayoutData(gridData5);
	}

	private void createButtonComposite() {
		GridData gridData4 = new GridData();
		gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		gridData4.grabExcessHorizontalSpace = true;
		GridData gridData3 = new GridData();
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.numColumns = 2;
		gridLayout2.marginHeight = 10;
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.END;
		
		buttonComposite = new Composite(friendShell, SWT.NONE);
		buttonComposite.setLayoutData(gridData2);
		buttonComposite.setLayout(gridLayout2);
		okButton = new Button(buttonComposite, SWT.NONE);
		okButton.setText("Ok");
		okButton.setLayoutData(gridData4);
		okButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				chatMod.addFriend(usernameText.getText());
				friendShell.close();
			}
		});
		
		cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(gridData3);
		cancelButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				friendShell.close();
			}
		});
		
		gridData3.widthHint = gridData4.widthHint = cancelButton.computeSize(-1, -1).x + 8;
		buttonComposite.layout();
	}

}
