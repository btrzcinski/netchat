package netchat.widgets;

import netchat.*;
import netchat.event.*;
import netchat.util.*;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

public class FileTransferGUI {

	private Shell fileShell = null;

	private int width = 275;
	private int height = 140;

	private CLabel infoLabel = null;
	private Label chunkLabel = null;
	private ProgressBar progressBar = null;
	private Label sizeLabel = null;
		
	private boolean isSending = false;

	private String transString = "Sending";
	
	private String user = "";
	private String filename = "";
	private long fileSize;
	private long id;
	private int numChunks;
	
	public FileTransferGUI(FileTransferEvent e)
	{
		user = e.user;
		filename = e.filename;
		fileSize = e.size;
		id = e.id;
		numChunks = e.numChunks;
		
		isSending = e.sending;
		
		createFileShell();
		
		fileShell.open();
	}
	
	private void createFileShell() {
		GridData gridData11 = new GridData();
		gridData11.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData11.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData11.grabExcessHorizontalSpace = true;
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 3;
		gridLayout.marginWidth = 10;
		
		GridData gridData2 = new GridData();
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData2.grabExcessVerticalSpace = true;
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData1.grabExcessHorizontalSpace = true;
		
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		
		fileShell = new Shell(SWT.BORDER | SWT.TITLE);
		fileShell.setText("File Transfer - " + id);
		fileShell.setLayout(gridLayout);
		fileShell.setSize(new Point(width, height));
		
		fileShell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e) {
				e.doit = false;
			}
		});
		
		infoLabel = new CLabel(fileShell, SWT.CENTER);
		
		if(isSending)
			infoLabel.setText("Transfering " + filename + " to " + user + "...");
		else
			infoLabel.setText("Receiving " + filename + " from " + user + "...");
		
		
		
		infoLabel.setFont(new Font(Display.getDefault(), "Tahoma", 10, SWT.NORMAL));
		infoLabel.setLayoutData(gridData);
		
		transString = isSending ? "Sending" : "Receiving";
		
		chunkLabel = new Label(fileShell, SWT.CENTER);
		chunkLabel.setText(transString + " chunk 1 of ?...");
		chunkLabel.setLayoutData(gridData1);
		
		sizeLabel = new Label(fileShell, SWT.CENTER);
		sizeLabel.setText("0/" + fileSize + " Bytes");
		sizeLabel.setLayoutData(gridData11);
		
		progressBar = new ProgressBar(fileShell, SWT.NONE);
		progressBar.setLayoutData(gridData2);
	}
	public void update(FileTransferEvent e)
	{
		chunkLabel.setText(transString + " chunk " + e.chunk + " of " + numChunks + "...");
		
		double ratio = e.chunk / (numChunks * 1.0);
		
		sizeLabel.setText((int)(ratio * fileSize) + "/" + fileSize + " Bytes");
		progressBar.setSelection((int)(ratio * 100));
	}
	public void dispose()
	{
		fileShell.dispose();
	}

}
