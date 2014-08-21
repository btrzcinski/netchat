package netchat.util;

import netchat.event.*;

import org.eclipse.swt.widgets.*;

public class Timer
{
	private int timestep = 0;
	private boolean isRunning = false;
	private ActionListener listener = null;
	private Display display = null;
	private Runnable runner = null;
	
	private int numRunning = 0;
	
	public Timer(Display d, int ts, ActionListener al)
	{
		display = d;
		timestep = ts;
		listener = al;
		runner = new Runnable()
		{
			public void run()
			{
				if(isRunning)
				{
					listener.actionPerformed();
					display.timerExec(timestep, this);
				}
			}
		};
		
	}
	
	public void start()
	{
		isRunning = true;
		numRunning++;
		
		display.syncExec(new Runnable()
		{
			public void run()
			{
				display.timerExec(timestep, runner);
			}
		});
	}
	
	public void stop()
	{
		if(numRunning > 0)
			numRunning--;
		
		isRunning = false;
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
	
	/**
	 * Returns how many times the Timer has been started and not stopped.
	 * @return number of running timers
	 */
	public int getNumRunning()
	{
		return numRunning;
	}
	
	public void setActionListener(ActionListener al)
	{
		listener = al;
	}
}