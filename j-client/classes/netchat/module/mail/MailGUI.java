package netchat.module.mail;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.browser.Browser;
import org.eclipse.jface.viewers.TableViewer;

public class MailGUI {

	private Shell mailShell = null;
	
	private GridData fillData = null;
	
	private int width = 1000;
	private int height = 600;

	private Composite mainComposite = null;

	private SashForm mainSashForm = null;

	private Composite leftMainComposite = null;

	private Composite rightMainComposite = null;

	private Tree folderTree = null;

	private SashForm rightSashForm = null;

	private Composite messagesComposite = null;

	private Composite displayComposite = null;

	private Table messagesTable = null;

	private Browser displayBrowser = null;

	private TableViewer tableViewer = null;
	
	private void createMailShell() {
		mailShell = new Shell();
		mailShell.setText("Shell");
		createMainComposite();
		mailShell.setSize(new Point(width, height));
		mailShell.setLayout(new GridLayout());
		
		mailShell.layout();
		mailShell.open();
	}

	private void createMainComposite() {
		fillData = new GridData();
		fillData.grabExcessHorizontalSpace = true;
		fillData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.grabExcessVerticalSpace = true;
		
		mainComposite = new Composite(mailShell, SWT.NONE);
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(fillData);
		
		createMainSashForm();
	}

	private void createMainSashForm() {
		mainSashForm = new SashForm(mainComposite, SWT.SMOOTH | SWT.HORIZONTAL);
		mainSashForm.setLayoutData(fillData);
		
		createLeftMainComposite();
		createRightMainComposite();
		
		mainSashForm.setWeights(new int[] {30, 70});
	}

	private void createLeftMainComposite() {
		leftMainComposite = new Composite(mainSashForm, SWT.NONE);
		leftMainComposite.setLayout(new GridLayout());
		leftMainComposite.setLayoutData(fillData);
		
		folderTree = new Tree(leftMainComposite, SWT.BORDER);
		folderTree.setLayoutData(fillData);
	}

	private void createRightMainComposite() {
		rightMainComposite = new Composite(mainSashForm, SWT.NONE);
		rightMainComposite.setLayout(new GridLayout());
		rightMainComposite.setLayoutData(fillData);
		
		createRightSashForm();
	}

	private void createRightSashForm() {
		rightSashForm = new SashForm(rightMainComposite, SWT.VERTICAL | SWT.SMOOTH);
		rightSashForm.setLayoutData(fillData);
		
		createMessagesComposite();
		createDisplayComposite();
		
		rightSashForm.setWeights(new int[] {25, 75});
	}

	private void createMessagesComposite() {
		messagesComposite = new Composite(rightSashForm, SWT.NONE);
		messagesComposite.setLayout(new GridLayout());
		messagesComposite.setLayoutData(fillData);
		
		messagesTable = new Table(messagesComposite, SWT.BORDER);
		messagesTable.setHeaderVisible(true);
		messagesTable.setLinesVisible(true);
		messagesTable.setLayoutData(fillData);
		
		tableViewer = new TableViewer(messagesTable);
	}

	private void createDisplayComposite() {
		displayComposite = new Composite(rightSashForm, SWT.NONE);
		displayComposite.setLayout(new GridLayout());
		createDisplayBrowser();
		displayComposite.setLayoutData(fillData);
	}

	private void createDisplayBrowser() {
		displayBrowser = new Browser(displayComposite, SWT.BORDER);
		displayBrowser.setLayoutData(fillData);
	}

}
