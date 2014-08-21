package netchat.widgets;

import netchat.module.FileTransferModule;

import org.htmlparser.util.*;

import org.eclipse.swt.events.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;

public class FileTransferSelector {

	private FileTransferModule fileMod = null;
	
	private Shell fileShell = null;

	private int width = 400;
	private int height = 310;
	
	private Label nameLabel = null;
	private Label fileLabel = null;
	private Label commentLabel = null;
	private Text nameText = null;
	private Text fileText = null;
	private Button browseButton = null;
	private Text commentTextArea = null;
	private Composite titleComposite = null;
	private Label imageLabel = null;
	private Label titleLabel = null;
	private Composite buttonComposite = null;
	private Composite mainComposite = null;

	private Button okButton = null;

	private Button cancelButton = null;
	
	public FileTransferSelector(FileTransferModule fMod)
	{
		fileMod = fMod;
		createFileShell();
		
		fileShell.open();
	}
	public FileTransferSelector(FileTransferModule fMod, String user)
	{
		this(fMod);
		nameText.setText(user);
	}
	private void createFileShell() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		
		fileShell = new Shell(SWT.CLOSE | SWT.MIN);
		fileShell.setLayout(gridLayout);
		fileShell.setSize(new Point(width, height));
		fileShell.setLocation(400, 300);
		fileShell.setText("Initiate File Transfer");
		
		createTitleComposite();
		createMainComposite();
		createButtonComposite();
		
		fileShell.setDefaultButton(okButton);
	}

	private void createMainComposite()
	{
		GridData gridData41 = new GridData();
		GridData gridData31 = new GridData();
		gridData31.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		GridData gridData21 = new GridData();
		gridData21.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.verticalSpacing = 5;
		gridLayout2.marginWidth = 25;
		gridLayout2.numColumns = 3;
		
		GridData gridData11 = new GridData();
		gridData11.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData11.grabExcessHorizontalSpace = true;
		gridData11.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData11.grabExcessVerticalSpace = true;
		GridData gridData3 = new GridData();
		gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.grabExcessVerticalSpace = true;
		gridData2.grabExcessHorizontalSpace = true;
		GridData gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		
		mainComposite = new Composite(fileShell, SWT.NONE);
		mainComposite.setLayoutData(gridData11);
		mainComposite.setLayout(gridLayout2);
		nameLabel = new Label(mainComposite, SWT.NONE);
		nameLabel.setText("Username:");
		nameLabel.setLayoutData(gridData31);
		nameText = new Text(mainComposite, SWT.BORDER);
		nameText.setLayoutData(gridData);
		Label filler = new Label(mainComposite, SWT.NONE);
		fileLabel = new Label(mainComposite, SWT.NONE);
		fileLabel.setText("File:");
		fileLabel.setLayoutData(gridData21);
		fileText = new Text(mainComposite, SWT.BORDER);
		fileText.setLayoutData(gridData1);
		browseButton = new Button(mainComposite, SWT.NONE);
		browseButton.setText("Browse...");
		browseButton.setLayoutData(gridData41);
		
		browseButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				String s = new FileDialog(fileShell, SWT.OPEN).open();
				if(s != null)
					fileText.setText(s);
			}
		});
		
		commentLabel = new Label(mainComposite, SWT.NONE);
		commentLabel.setText("Comment:");
		commentLabel.setLayoutData(gridData3);
		commentTextArea = new Text(mainComposite, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		commentTextArea.setLayoutData(gridData2);
		Label filler1 = new Label(mainComposite, SWT.NONE);
	}
	private void createTitleComposite() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		gridLayout1.horizontalSpacing = 20;
		gridLayout1.marginHeight = 10;
		gridLayout1.marginWidth = 10;
		GridData gridData4 = new GridData();
		gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData4.grabExcessHorizontalSpace = true;
		gridData4.grabExcessVerticalSpace = false;
		gridData4.horizontalSpan = 3;
		gridData4.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		titleComposite = new Composite(fileShell, SWT.BORDER);
		titleComposite.setBackgroundMode(SWT.INHERIT_FORCE);
		titleComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		titleComposite.setLayoutData(gridData4);
		titleComposite.setLayout(gridLayout1);
		imageLabel = new Label(titleComposite, SWT.NONE);
		imageLabel.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/download-icon-75.png")));
		//imageLabel.setImage(new Image(Display.getCurrent(), "C:/Documents and Settings/ANDY/workspace/Netchat/icons/download-icon-75.png"));
		titleLabel = new Label(titleComposite, SWT.NONE);
		titleLabel.setText("Send a File...");
		titleLabel.setFont(new Font(Display.getDefault(), "Tahoma", 21, SWT.NORMAL));
	}

	private void createButtonComposite() {
		GridData gridData7 = new GridData();
		gridData7.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		gridData7.widthHint = 50;
		gridData7.grabExcessHorizontalSpace = true;
		GridData gridData6 = new GridData();
		gridData6.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData6.grabExcessHorizontalSpace = true;
		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 2;
		gridLayout3.marginHeight = 10;
		gridLayout3.verticalSpacing = 5;
		GridData gridData5 = new GridData();
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData5.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		buttonComposite = new Composite(fileShell, SWT.NONE);
		buttonComposite.setLayoutData(gridData5);
		buttonComposite.setLayout(gridLayout3);
		okButton = new Button(buttonComposite, SWT.NONE);
		okButton.setText("Send");
		okButton.setLayoutData(gridData7);

		okButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				fileMod.requestFileTransfer(Translate.encode(nameText.getText()), Translate.encode(fileText.getText()), Translate.encode(commentTextArea.getText()));
				fileShell.dispose();
			}
		});
		
		cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(gridData6);
		cancelButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				fileShell.dispose();
			}
		});
		
		gridData6.widthHint = gridData7.widthHint = cancelButton.computeSize(-1, -1).x + 8;
		buttonComposite.layout();
	}

}
