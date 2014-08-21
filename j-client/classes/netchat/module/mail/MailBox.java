package netchat.module.mail;

import java.util.*;

public class MailBox {

	private MailBox parent = null;
	private String name = null;
	private ArrayList<Message> messages = null;
	
	public MailBox(String name)
	{
		this(null, name);
	}
	public MailBox(MailBox parent, String name)
	{
		this.parent = parent;
		this.name = name;
		
		messages = new ArrayList<Message>();
	}
	
	public void add(Message msg)
	{
		messages.add(msg);
	}
	public void remove(Message msg)
	{
		messages.remove(msg);
	}
	public void remove(int index)
	{
		messages.remove(index);
	}
	
	public MailBox getParent()
	{
		return parent;
	}
	public String getName()
	{
		return name;
	}
}
