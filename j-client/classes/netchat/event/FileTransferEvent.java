package netchat.event;

import java.io.*;

public class FileTransferEvent {
	public long time;
	public String filename = null;
	public long size;
	public String user = null;
	public String comment = null;
	public int numChunks;
	public int chunk;
	public File file = null;
	public long id;
	public boolean doit = true;
	public String encodedFile = null;
	public boolean sending = true;
	
	public FileTransferEvent()
	{
		
	}
	
	public FileTransferEvent(long time, String filename, String user, long id)
	{
		this.time = time;
		this.filename = filename;
		this.user = user;
		this.id = id;
	}
	
	public FileTransferEvent(String filename, String user, long id)
	{
		this(System.nanoTime(), filename, user, id);
	}
}
