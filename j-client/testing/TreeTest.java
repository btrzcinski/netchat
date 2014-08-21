
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

public class TreeTest {

	private static Shell sShell = null;
	private static Tree tree = null;

	/**
	 * This method initializes sShell
	 */
	public static void main(String[] args)
	{
		createSShell();
		
		sShell.open();

		while(!sShell.isDisposed())
			while(!Display.getCurrent().readAndDispatch())
				Display.getCurrent().sleep();
	}
	
	private static void createSShell() {
		GridData gridData = new GridData();
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		sShell = new Shell();
		sShell.setText("Shell");
		sShell.setSize(new Point(300, 200));
		sShell.setLayout(new GridLayout());
		tree = new Tree(sShell, SWT.NONE);
		tree.setLayoutData(gridData);
		
		TreeItem t = new TreeItem(tree, SWT.NONE);
		t.setText("Parent");

		for(int i = 0; i < 5; i++)
		{
			TreeItem t2 = new TreeItem(t, SWT.NONE);
			t2.setText("SubObject");
		}
	}

}
