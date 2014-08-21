package netchat.widgets;

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

import java.util.*;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.graphics.Point;

public class ToolTipComposite extends Composite {

	private String message = null;
	private String name = null;
	Image avatar = null;
	
	private Label nameLabel = null;
	private Label avatarLabel = null;
	private Composite infoComposite = null;
	private Label messageLabel = null;
	private Composite avatarComposite = null;
	private Label statusLabel = null;
	private Label statusImageLabel = null;
	
	public ToolTipComposite(Composite parent, String name, String message, Image i, int style) {
		super(parent, style);
		
		this.name = name;
		this.message = message;
		this.avatar = i;
		
		initialize();
	}

	private void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 3;
		gridLayout.marginWidth = 3;
		gridLayout.horizontalSpacing = 5;
		this.setLayout(gridLayout);
		createAvatarComposite();
		createInfoComposite();
	}

	private void createInfoComposite() {
		GridData gridData1 = new GridData();
		gridData1.grabExcessVerticalSpace = true;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		//gridData.heightHint = 50;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		
		GridData nameData = new GridData();
		nameData.horizontalSpan= 2;
		
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		infoComposite = new Composite(this, SWT.NONE);
		infoComposite.setLayout(gridLayout1);
		infoComposite.setLayoutData(gridData1);
		nameLabel = new Label(infoComposite, SWT.NONE);
		nameLabel.setText("User: " + name);
		nameLabel.setLayoutData(nameData);
		statusLabel = new Label(infoComposite, SWT.NONE);
		statusLabel.setText("Status:");
		statusImageLabel = new Label(infoComposite, SWT.NONE);
		statusImageLabel.setImage(avatar);
		messageLabel = new Label(infoComposite, SWT.WRAP);
		messageLabel.setText("Message: " + message);
		messageLabel.setLayoutData(gridData);
	}

	private void createAvatarComposite() {
		avatarComposite = new Composite(this, SWT.BORDER);
		avatarComposite.setLayout(new GridLayout());
		
		avatarLabel = new Label(avatarComposite, SWT.NONE);
		avatarLabel.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/anonymous-50.png")));
	}

}
