package netchat.event;

public class ChangeEvent {
	public String text;
	public String oldText;
	public long time;
	
	public ChangeEvent(String newText, String oldText)
	{
		this.text = newText;
		this.oldText = oldText;
		this.time = System.nanoTime();
	}
	
	public ChangeEvent(String newText)
	{
		this.text = newText;
		this.time = System.nanoTime();
	}
	
	public ChangeEvent()
	{
		this.time = System.nanoTime();
	}
}
