package netchat.event;

import java.util.*;

public class MailEvent {

	public long time;
	public long id;
	public String subject, from, message, cc, bcc = null;
	public HashMap<Integer, String> attachments = null;
	public boolean doit = true;
	
	public MailEvent()
	{
		this.time = System.nanoTime();
		attachments = new HashMap<Integer, String>();
	}
	
}
