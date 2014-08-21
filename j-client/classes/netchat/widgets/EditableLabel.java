package netchat.widgets;

import netchat.*;
import netchat.event.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.util.*;

public class EditableLabel extends Composite {

	EditableLabel thisClass = null;
	GridLayout gridLayout = null;
	GridData fillData = null;
	CLabel label = null;
	Text text = null;
	KeyListener keyListener = null;
	MouseListener mouseListener = null;
	FocusListener focusListener = null;
	
	private boolean isEnabled = true;
	private String emptyText = "";
	
	private ArrayList<LabelChangeListener> changeListeners = null;
	
	private String labelText = "";
	private String oldText = "";
	
	public EditableLabel(Composite parent, int style) {
		super(parent, style);
		
		changeListeners = new ArrayList<LabelChangeListener>();
		initialize();
	}

	private void initialize() {
		thisClass = this;
		
		fillData = new GridData();
		fillData.grabExcessHorizontalSpace = true;
		fillData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		fillData.grabExcessVerticalSpace = true;
		
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 3;
		setLayout(gridLayout);
		
		label = new CLabel(thisClass, SWT.NONE);
		
		keyListener = new KeyAdapter()
		{
			public void keyReleased(KeyEvent e) {
				if(!isEnabled) return;
				
				if(e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					labelText = text.getText();
					updateLabel();
				}
				else if(e.keyCode == SWT.ESC)
				{
					updateLabel();
				}
			}
			
		};
		
		mouseListener = new MouseAdapter()
		{
			public void mouseUp(MouseEvent e) {
				if(label.isDisposed()) return;
				if(!isEnabled) return;
				
				oldText = labelText;
				
				label.dispose();
				
				text = new Text(thisClass, SWT.BORDER);
				text.setLayoutData(fillData);
				text.addKeyListener(keyListener);
				text.setText(oldText);
				text.addFocusListener(focusListener);
				
				gridLayout.marginHeight = 3;
				
				layout();
				
				text.setFocus();
				text.selectAll();
			}
		};
		
		focusListener = new FocusAdapter()
		{
			public void focusLost(FocusEvent arg0) {
				if(!isEnabled) return;
				updateLabel();
			}
		};
		
		this.addMouseListener(mouseListener);
		label.addMouseListener(mouseListener);
	}
	
	public String getText()
	{
		return labelText;
	}
	
	public void setText(String s)
	{
		if(!label.isDisposed())
		{
			label.setText(s);
			notifyLabelChangeListeners(new ChangeEvent(s, oldText));
		}
		else if(!text.isDisposed())
			text.setText(s);
	}
	public void setEmptyText(String text)
	{
		emptyText = text;
	}
	
	private void updateLabel()
	{
		text.dispose();
		
		label = new CLabel(thisClass, SWT.NONE);
		
		if(labelText.equals(""))
			label.setText(emptyText);
		else
			label.setText(labelText);
		
		label.addMouseListener(mouseListener);
		
		gridLayout.marginHeight = 3;
		
		layout();
		
		notifyLabelChangeListeners(new ChangeEvent(labelText, oldText));
	}
	public void setEnabled(boolean b)
	{
		isEnabled = b;
	}
	public boolean getEnabled()
	{
		return isEnabled;
	}
	
	private void notifyLabelChangeListeners(ChangeEvent e)
	{
		for(LabelChangeListener l : changeListeners)
			l.textChanged(e);
	}
	
	public void addLabelChangeListener(LabelChangeListener l)
	{
		changeListeners.add(l);
	}
	
	public void removeLabelChangeListener(LabelChangeListener l)
	{
		changeListeners.remove(l);
	}

}
