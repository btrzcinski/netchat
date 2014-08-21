package netchat.event;

public interface MailListener {

	public void newMail(MailEvent e);
	public void messageReceived(MailEvent e);
	
}
