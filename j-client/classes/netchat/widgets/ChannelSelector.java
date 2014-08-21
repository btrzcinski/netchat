package netchat.widgets;

import java.util.ArrayList;

import netchat.module.*;

import org.htmlparser.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Combo;

public class ChannelSelector
{

	private String smallLogoFile = "/icons/nc.png";
	private String logoFile = "/icons/messenger-50.png";
	
	private ChatModule chatMod = null;
	
	private Shell actionShell = null;
	private Composite titleComposite = null;
	private Composite mainComposite = null;
	private Composite buttonComposite = null;
	private Label imageLabel = null;
	private Label titleLabel = null;
	private Button okButton = null;
	private Button cancelButton = null;
	private Combo combo = null;
	private Label label = null;

	private int width = 300;
	private int height = 225;
	
	private int x = 350;
	private int y = 300;
	
	public ChannelSelector(ChatModule chatMod)
	{
		this.chatMod = chatMod;
		
		createActionShell();
		
		actionShell.open();
	}
	
	private void createActionShell()
	{
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		
		actionShell = new Shell(SWT.CLOSE | SWT.MIN);
		actionShell.setText("Select a Channel...");
		actionShell.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(smallLogoFile)));
		actionShell.setLayout(gridLayout);
		actionShell.setSize(new Point(width, height));
		actionShell.setLocation(x, y);
		
		createTitleComposite();
		createMainComposite();
		
		chatMod.setChannelSelector(this);
		chatMod.requestRoomList();
		
		createButtonComposite();
		actionShell.setDefaultButton(okButton);
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
		titleComposite = new Composite(actionShell, SWT.BORDER);
		titleComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		titleComposite.setLayout(gridLayout1);
		titleComposite.setLayoutData(gridData);
		titleComposite.setBackgroundMode(SWT.INHERIT_FORCE);
		
		imageLabel = new Label(titleComposite, SWT.NONE);
		imageLabel.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(logoFile)));
		titleLabel = new Label(titleComposite, SWT.NONE);
		titleLabel.setText("Join a Channel...");
		titleLabel.setFont(new Font(Display.getDefault(), "Tahoma", 18, SWT.NORMAL));
	}

	private void createMainComposite() {
		GridData gridData6 = new GridData();
		gridData6.grabExcessVerticalSpace = true;
		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 2;
		gridLayout3.marginWidth = 25;
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		
		mainComposite = new Composite(actionShell, SWT.NONE);
		label = new Label(mainComposite, SWT.NONE);
		label.setText("Channel:");
		label.setLayoutData(gridData6);
		createCombo();
		mainComposite.setLayout(gridLayout3);
		mainComposite.setLayoutData(gridData1);
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
		gridLayout2.marginHeight = 15;
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.END;
		
		buttonComposite = new Composite(actionShell, SWT.NONE);
		buttonComposite.setLayoutData(gridData2);
		buttonComposite.setLayout(gridLayout2);
		
		okButton = new Button(buttonComposite, SWT.NONE);
		okButton.setText("Ok");
		okButton.setLayoutData(gridData4);
		okButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				chatMod.subscribeGroupChat(Translate.encode(combo.getText()));
				actionShell.close();
			}
		});
		
		cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(gridData3);
		cancelButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				actionShell.close();
			}
		});
		
		gridData3.widthHint = gridData4.widthHint = cancelButton.computeSize(-1, -1).x + 8;
		buttonComposite.layout();
	}

	private void createCombo() {
		GridData gridData5 = new GridData();
		gridData5.grabExcessHorizontalSpace = true;
		gridData5.grabExcessVerticalSpace = true;
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		combo = new Combo(mainComposite, SWT.NONE);
		combo.setLayoutData(gridData5);
	}
	
	public void updateRooms(ArrayList<String> a)
	{
		for(String s : a)
			if(combo.indexOf(s) < 0)
				combo.add(s);
	}

}
