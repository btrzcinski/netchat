package netchat.module.login;

import netchat.*;
import netchat.util.*;
import netchat.util.Timer;
import netchat.system.*;
import netchat.event.*;
import netchat.module.*;
import netchat.widgets.*;

import java.io.*;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

public class LoginGUI {

	private LoginModule mod = null;
	private ClientConnector connector = null;
	private Controller cont = null;
	
	private Display display = null;

	private String username = null;

	private Timer processingTimer = null;
	private Shell loginShell = null;
	private Label titleLabel = null;
	private Composite mainComposite = null;
	private Label statusLabel = null;
	private Label usernameLabel = null;
	private Text usernameText = null;
	private Label passwordLabel = null;
	private Text passwordText = null;
	private Composite saveComposite = null;
	private Button savePasswordCheckBox = null;
	private Button autoLoginCheckBox = null;
	private Composite buttonComposite = null;
	private Button signInButton = null;
	private Button quitButton = null;
	private Composite serverChooserComposite = null;
	private Label serverChooserLabel = null;
	private Combo serverChooser = null;
	private GridData gridData1 = null;

	private boolean autoLogin = false;
	private boolean open = false;
	
	private int width = 300;
	private int height = 460;
	private Label fillerLabel = null;

	private Color textForegroundColor = null;

	private boolean savedPassword = false;
	private String savedPassHash = null;

	public LoginGUI(LoginModule m, Display disp, ClientConnector con, Controller cont)
	{
		mod = m;
		display = disp;
		connector = con;
		this.cont = cont;
		
		if(System.getProperty("os.name").matches(".*Windows\\s*XP.*"))
		{
			Debug.println("Detected Windows XP operating system.", 1);
			height -= 25;
		}
		
		init();
	}

	private void init()
	{
		createLoginShell();

		processingTimer = new Timer(display, 80, new ActionListener(){
			public void actionPerformed()
			{
				String text = statusLabel.getText();
				int lastChar = (int)(text.charAt(text.length() - 1));
				text = text.substring(0, text.length() - 1);
				
				switch(lastChar)
				{
					case ((int)('-')): text += '\\'; break;
					case ((int)('\\')): text += '|'; break;
					case ((int)('|')): text += '/'; break;
					case ((int)('/')): text += '-'; break;
					/*case ((int)('-')): text = '\\' + text + '\\'; break;
					case ((int)('\\')): text = '|' + text + '|'; break;
					case ((int)('|')): text = '/' + text + '/'; break;
					case ((int)('/')): text = '-' + text + '-'; break;*/
				}
				
				statusLabel.setText(text);
			}
		});
		
		if(!retrievePassword())
			open = true;

		usernameText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e)
			{
				if(savedPassword)
				{
					savedPassword = false;
					passwordText.setText("");
				}
			}
		});

		passwordText.addFocusListener(new FocusAdapter(){
			public void focusGained(FocusEvent e)
			{
				if(savedPassword)
				{
					savedPassword = false;
					passwordText.setText("");
				}
			}
		});

		loginShell.setDefaultButton(signInButton);
		
		if(open)
		{
			loginShell.open();		
			usernameText.setFocus();
		}
	}

	private void createLoginShell() {
		GridData fillerLabel2GridData = new GridData();
		fillerLabel2GridData.heightHint = 23;

		GridData fillerLabelGridData = new GridData();
		fillerLabelGridData.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		fillerLabelGridData.heightHint = 5;

		GridData gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 10;

		loginShell = new Shell(SWT.SHELL_TRIM);
		loginShell.setText("NetChat Login");
		loginShell.setImage(new Image(display, getClass().getResourceAsStream("/icons/nc.png")));
		loginShell.setLayout(gridLayout);
		loginShell.setSize(new Point(width, height));
		loginShell.setLocation(350, 250);
		loginShell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				System.exit(0);
			}
		});
		
		loginShell.setMenuBar(new MenuBar(loginShell, NC.FILE | NC.HELP).getMenuBar());
		
		textForegroundColor = loginShell.getForeground();

		titleLabel = new Label(loginShell, SWT.CENTER);
		titleLabel.setImage(new Image(display, getClass().getResourceAsStream("/icons/netchat-60.png")));
		titleLabel.setLayoutData(gridData);

		fillerLabel = new Label(loginShell, SWT.NONE);
		fillerLabel.setLayoutData(fillerLabelGridData);

		createMainComposite();

		Label fillerLabel2 = new Label(loginShell, SWT.NONE);
		fillerLabel2.setLayoutData(fillerLabel2GridData);

		createButtonComposite();
	}

	private void createMainComposite() {
		GridData gridData5 = new GridData();
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData5.grabExcessHorizontalSpace = true;

		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData3.grabExcessHorizontalSpace = true;
		
		GridData gridData2 = new GridData();
		gridData2.horizontalSpan = 3;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.heightHint = 25;
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;

		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 3;
		gridLayout1.marginWidth = 0;

		gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = true;
		//gridData1.widthHint = titleLabel.getImage().getBounds().width + 25;
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;

		GridData gridData12 = new GridData();
		gridData12.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData12.grabExcessHorizontalSpace = true;

		mainComposite = new Composite(loginShell, SWT.NONE);
		mainComposite.setLayoutData(gridData1);
		mainComposite.setLayout(gridLayout1);

		//FontData fd = display.getSystemFont().getFontData()[0];
		//fd.setStyle(SWT.BOLD);
		//Font statusFont = new Font(display, fd);

		statusLabel = new Label(mainComposite, SWT.NONE);

		//statusLabel.setFont(statusFont);
		
		statusLabel.setText("Welcome!");
		statusLabel.setFont(new Font(Display.getDefault(), "Tahoma", 8, SWT.BOLD));
		statusLabel.setLayoutData(gridData2);
		statusLabel.setAlignment(SWT.CENTER);

		usernameLabel = new Label(mainComposite, SWT.NONE);
		usernameLabel.setText("Username:");

		@SuppressWarnings("unused")
		Label filler = new Label(mainComposite, SWT.NONE);

		usernameText = new Text(mainComposite, SWT.BORDER);
		usernameText.setLayoutData(gridData3);

		passwordLabel = new Label(mainComposite, SWT.NONE);
		passwordLabel.setText("Password:");

		@SuppressWarnings("unused")
		Label filler2 = new Label(mainComposite, SWT.NONE);

		passwordText = new Text(mainComposite, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(gridData5);

		@SuppressWarnings("unused")
		Label filler1 = new Label(mainComposite, SWT.NONE);

		@SuppressWarnings("unused")
		Label filler4 = new Label(mainComposite, SWT.NONE);

		createSaveComposite();

		serverChooserLabel = new Label(mainComposite, SWT.NONE);
		serverChooserLabel.setText("Server:");

		@SuppressWarnings("unused")
		Label filler5 = new Label(mainComposite, SWT.NONE);

		serverChooser = new Combo(mainComposite, SWT.BORDER | SWT.READ_ONLY);
		serverChooser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		serverChooser.setLayoutData(gridData12);
		serverChooser.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				String server = serverChooser.getItem(serverChooser.getSelectionIndex());
				if(!server.equals(connector.getCurrentServer()))
				{
					connector.makePrimaryServer(server);
					connector.findHost();
				}
			}
		});
		populateServerChooser();
	}

	private void populateServerChooser()
	{
		for(String s : connector.getServerList())
			serverChooser.add(s);
		serverChooser.select(0);
	}
	private void createSaveComposite() {

		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.marginWidth = 0;
		gridLayout2.verticalSpacing = 0;
		gridLayout2.marginHeight = 0;

		saveComposite = new Composite(mainComposite, SWT.NONE);
		saveComposite.setLayout(gridLayout2);

		savePasswordCheckBox = new Button(saveComposite, SWT.CHECK);
		savePasswordCheckBox.setText("Save Password");
		
		autoLoginCheckBox = new Button(saveComposite, SWT.CHECK);
		autoLoginCheckBox.setText("Auto-Login");
		
		new Label(saveComposite, SWT.NONE);
	}

	private void createButtonComposite() {
		GridData gridData10 = new GridData();
		gridData10.widthHint = 12;

		GridData gridData9 = new GridData();

		GridData gridData8 = new GridData();
		gridData8.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		gridData8.widthHint = 60;
		gridData8.heightHint = 30;

		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 3;

		GridData gridData7 = new GridData();
		gridData7.grabExcessHorizontalSpace = true;
		gridData7.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;

		buttonComposite = new Composite(loginShell, SWT.BORDER);
		buttonComposite.setLayoutData(gridData7);
		buttonComposite.setLayout(gridLayout3);

		signInButton = new Button(buttonComposite, SWT.NONE);
		signInButton.setText("Sign In");
		signInButton.setLayoutData(gridData8);
		signInButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				signIn();
			}
		});

		Label filler3 = new Label(buttonComposite, SWT.NONE);
		filler3.setLayoutData(gridData10);

		Point signInButtonSize = signInButton.computeSize(gridData8.widthHint, gridData8.heightHint, false);

		gridData9.widthHint = signInButtonSize.x;
		gridData9.heightHint = signInButtonSize.y;

		quitButton = new Button(buttonComposite, SWT.NONE);
		quitButton.setText("Quit");
		quitButton.setLayoutData(gridData9);
		quitButton.setSize(signInButton.computeSize(-1, -1, false));
		quitButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				System.exit(0);
			}
		});
	}

	private void signIn()
	{
		setProcessing();
		username = usernameText.getText();

		cont.getProperties().put("nc_auto_login", "" + autoLoginCheckBox.getSelection());
		
		if(savedPassword)
			mod.receiveSavedCredentials(username, savedPassHash, savePasswordCheckBox.getSelection());
		else
			mod.receiveCredentials(username, passwordText.getText().toCharArray(), savePasswordCheckBox.getSelection());

	}
	
	private boolean retrievePassword()
	{
		try
		{
			Properties p = cont.getProperties();
			String user = p.getProperty("nc_user");
			savedPassHash = p.getProperty("nc_hash");
			
			if(user == null || savedPassHash == null)
			{
				Debug.println("No password saved, skipping retrieval", 3);
				return false;
			}
			
			String aLogin = p.getProperty("nc_auto_login");
			if(aLogin != null)
				autoLogin = Boolean.parseBoolean(aLogin);
			
			usernameText.setText(user);
			passwordText.setText("*******");
			savePasswordCheckBox.setSelection(true);
			savedPassword = true;
			
			if(autoLogin)
			{
				autoLoginCheckBox.setSelection(true);
				if(cont.isFirstLogin())
				{
					signIn();
					return true;
				}
			}
		}
		catch (Exception e)
		{
			Debug.printError("Could not retrieve a saved password: " + e.getMessage());
		}
		
		return false;
	}

	public void setError(final String s)
	{
		statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
		statusLabel.setText(s);
	}
	public void setStatus(final String s)
	{
		statusLabel.setForeground(textForegroundColor);
		statusLabel.setText(s);
	}
	public void setProcessing()
	{
		setStatus("Processing... /");
		setEnabledAll(false);

		if(loginShell.isVisible())
			processingTimer.start();
	}
	public void acceptCredentials()
	{
		processingTimer.stop();
		loginShell.dispose();
	}
	public void rejectCredentials(String reason)
	{
		if(!loginShell.isVisible())
		{
			loginShell.open();		
			usernameText.setFocus();
		}
		
		processingTimer.stop();
		setError(reason);

		usernameText.setText("");
		passwordText.setText("");

		setEnabledAll(true);
	}
	public void setEnabledAll(boolean enab)
	{
		signInButton.setEnabled(enab);
		quitButton.setEnabled(enab);
		usernameText.setEnabled(enab);
		passwordText.setEnabled(enab);
	}
	public void dispose()
	{
		loginShell.dispose();
	}
}
