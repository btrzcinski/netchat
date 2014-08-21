package netchat.widgets;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;

public class AboutShell{

	private Label logoLabel = null;
	private StyledText aboutText = null;
	private Button closeButton = null;
	Shell s = null;
	
	public AboutShell(Display display, int style) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 25;
		gridLayout.verticalSpacing = 10;
		
		s = new Shell(display, style);
		s.setLayout(gridLayout);
		s.setSize(new Point(300, 350));
		s.setText("About NetChat");
		s.setLocation(450, 250);
		
		initialize();
		
		s.open();
	}
	
	private void initialize() {
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		
		logoLabel = new Label(s, SWT.NONE);
		logoLabel.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/netchat-75.png")));
		logoLabel.setLayoutData(gridData);
		aboutText = new StyledText(s, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		aboutText.setEditable(false);
		aboutText.setLayoutData(gridData1);
		aboutText.setText("    NetChat was developed by Andy Street, Barnett Trzcinski and Steven Fuqua, \u00a9 2007\n\n    Visit http://netchat.tjhsst.edu/trac/netchat for more information.");
		closeButton = new Button(s, SWT.NONE);
		closeButton.setText("  Close  ");
		closeButton.setLayoutData(gridData2);
		
		closeButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				s.dispose();
			}
		});
	}
}
