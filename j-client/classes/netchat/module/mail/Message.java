package netchat.module.mail;

public class Message {

	private String from, subject, message = null;
	private String cc, bcc = "";
	private long id;
	
	public Message(long id, String from, String subject)
	{
		this.id = id;
		this.from = from;
		this.subject = subject;
	}
	public Message(long id, String from, String subject, String message)
	{
		this(id, from, subject);
		
		this.message = message;
	}
	public Message(long id, String from, String subject, String cc, String bcc)
	{
		this(id, from, subject);
		
		this.cc = cc;
		this.bcc = bcc;
	}
	public Message(long id, String from, String subject, String message, String cc, String bcc)
	{
		this(id, from, subject, cc, bcc);
		
		this.message = message;
	}
	
	public String getFrom()
	{
		return from;
	}
	public String getSubject()
	{
		return subject;
	}
	public String getMessage()
	{
		return message;
	}
	public String getCC()
	{
		return cc;
	}
	public String getBCC()
	{
		return bcc;
	}
}
