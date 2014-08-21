package netchat.module;

import netchat.*;
import netchat.system.*;
import netchat.util.*;
import netchat.xml.*;
import netchat.event.*;

import net.iharder.*;

import java.io.*;
import java.util.*;

public class FileTransferModule extends AbstractModule {
	
	static String name = "filetransfer";
	static String protocolVersion = "0.1a";
	static String version = "0.1a";
	
	private Controller controller;
	
	private ArrayList<FileTransferListener> transferListeners = null;
	
	private File downloadDir = null;
	private String downloadDirName = "MyDownloads";
	
	int chunkSize = 3073 / 7 * 8; //3KB
	private String transferString = "__NCTRANS__";
	
	private long currentID;
	
	private HashMap<Long, String[]> transfers = null;
	private HashMap<Long, File> transferFiles = null;
	private HashMap<Long, FileTransferEvent> requests = null;
	private HashMap<Long, String> encodedFiles = null;
	private HashMap<Long, String> receivedEncodedFiles = null;
	
	public FileTransferModule(Controller cont)
	{
		controller = cont;
		
		downloadDir = new File(downloadDirName);
		if(!downloadDir.exists())
			downloadDir.mkdir();
		
		transferListeners = new ArrayList<FileTransferListener>();
		requests = new HashMap<Long, FileTransferEvent>();
		transfers = new HashMap<Long, String[]>();
		transferFiles = new HashMap<Long, File>();
		encodedFiles = new HashMap<Long, String>();
		receivedEncodedFiles = new HashMap<Long, String>();
		
		requestNewTransferID();
		
		addFileTransferListener(new FileTransferListener(){
			public void transferRequest(FileTransferEvent e)
			{
				requests.put(e.id, e);
			}
			public void transferAccepted(FileTransferEvent e)
			{
				if(!e.sending)
				{
					Element content = new Element("content");
					new Element(content, "id").addText(e.id);
					Attributes routeAttrs = new Element(content, "route").getAttributes();
					routeAttrs.put("to", controller.getUsername());
					routeAttrs.put("from", e.user);
					new Element(content, "chunk").getAttributes().put("number", "" + 1);
					
					sendMessage(controller, getName(), "file_chunk_request", content);
				}
			}
			public void transferRejected(FileTransferEvent e)
			{
				if(e.doit)
				{
					transfers.remove(e.id);
					transferFiles.remove(e.id);
					encodedFiles.remove(e.id);
				}
			}
			public void chunkTransfer(FileTransferEvent e)
			{
				if(e.doit)
				{
					Element content = new Element("content");
					new Element(content, "id").addText("" + e.id);
					Element route = new Element(content, "route");
					Attributes routeAttrs = route.getAttributes();
					routeAttrs.put("from", controller.getUsername());
					routeAttrs.put("to", e.user);
					Element chunk = new Element(content, "chunk");
					Attributes chunkAttrs = chunk.getAttributes();
					chunkAttrs.put("number", "" + e.chunk);
					chunkAttrs.put("maxchunk", "" + (e.encodedFile.length() / chunkSize + 1));
					chunkAttrs.put("filename", e.filename);
					chunkAttrs.put("totalbsize", "" + ((int)(e.encodedFile.length() * 7.0 / 8)));
					
					if(e.chunk != e.numChunks)
						chunk.addText(e.encodedFile.substring(chunkSize * (e.chunk - 1), chunkSize * e.chunk).replace('\n', ' '));
					else
						chunk.addText(e.encodedFile.substring(chunkSize * (e.chunk - 1)).replace('\n', ' '));

					sendMessage(controller, getName(), "file_chunk", content);
				}
			}
			public void chunkReceived(FileTransferEvent e)
			{
				if(e.doit)
					if(e.chunk < e.numChunks)
					{
						Element content = new Element("content");
						new Element(content, "id").addText(e.id);
						Attributes routeAttrs = new Element(content, "route").getAttributes();
						routeAttrs.put("to", controller.getUsername());
						routeAttrs.put("from", e.user);
						new Element(content, "chunk").getAttributes().put("number", "" + (e.chunk + 1));
						
						sendMessage(controller, getName(), "file_chunk_request", content);
					}
					else
					{
						Element content = new Element("content");
						new Element(content, "id").addText(e.id);
						new Element(content, "from").addText(e.user);
						new Element(content, "to").addText(controller.getUsername());
						new Element(content, "success").addText("true");
						
						sendMessage(controller, getName(), "transfer_complete", content);
					}
			}
			public void transferComplete(FileTransferEvent e)
			{
				if(e.doit && !e.sending)
				{
					encodedFiles.remove(e.id);
					transferFiles.remove(e.id);
					transfers.remove(e.id);
					Debug.println("Finished download " + e.id, 2);
					
					Debug.println("Decoding downloaded file...", 2);
					String filenameAppend = "";
					int fileNum = 0;
					File f;
					while((f = new File(downloadDir, e.filename + filenameAppend)).exists())
					{
						fileNum++;
						filenameAppend = "" + fileNum;
					}
					
					Base64.decodeToFile(receivedEncodedFiles.get(e.id), f.getPath());
					
					Debug.println("Finished decoding file.  It is now located at " + f.getAbsolutePath(), 0);
				}
			}
		});
	}
	
	private void sendFileChunk(long id, int chunkNum)
	{
		String[] data = transfers.get(id);
		File f = transferFiles.get(id);
		String encodedFile = encodedFiles.get(id);
		
		String user = data[0];
		String filename = data[1];
		
		int maxChunk = (encodedFile.length() / chunkSize + 1);
		
		if(chunkNum > maxChunk)
		{
			Debug.printError("User " + user + " has requested a file chunk that does not exist!  Aborting!");
			return;
		}
		
		FileTransferEvent e = new FileTransferEvent(filename, user, id);
		e.chunk = chunkNum;
		e.numChunks = maxChunk;
		e.file = f;
		e.encodedFile = encodedFile;
		e.size = (int)(encodedFile.length() * 7.0 / 8);
		
		for(FileTransferListener l : transferListeners)
			l.chunkTransfer(e);
	}
	public void acceptTransfer(long id)
	{
		FileTransferEvent e;
		
		if((e = requests.remove(id)) == null)
			Debug.printError("Trying to accept a non-existent file transfer!");
		
		e.sending = false;
		
		Element content = new Element("content");
		new Element(content, "id").addText(e.id);
		new Element(content, "from").addText(e.user);
		new Element(content, "accept").addText("true");
		new Element(content, "filename").addText(e.filename);
		
		sendMessage(controller, getName(), "file_transfer_decision", content);
		
		for(FileTransferListener l : transferListeners)
			l.transferAccepted(e);
	}
	public void rejectTransfer(long id)
	{
		FileTransferEvent e;
		
		if((e = requests.remove(id)) == null)
			Debug.printError("Trying to reject a non-existent file transfer!");
		
		e.sending = false;
		
		for(FileTransferListener l : transferListeners)
			l.transferRejected(e);
		
		Element content = new Element("content");
		new Element(content, "id").addText(e.id);
		new Element(content, "from").addText(e.user);
		new Element(content, "accept").addText("false");
		new Element(content, "filename").addText(e.filename);
		
		sendMessage(controller, getName(), "file_transfer_decision", content);
	}
	public void addFileTransferListener(FileTransferListener l)
	{
		transferListeners.add(l);
	}
	public void requestNewTransferID()
	{
		sendMessage(controller, getName(), "file_transfer_id_request", null);
	}
	public int requestFileTransfer(String user, String filename, String comment)
	{
		long id = currentID;
		requestNewTransferID();
		
		File f = new File(filename);
		if(!f.exists())
		{
			Debug.printError("File " + filename + " was not found!");
			return NC.FAIL;
		}
		
		transfers.put(id, new String[] {user, f.getName()});
		transferFiles.put(id, f);
		encodedFiles.put(id, Base64.encodeFromFile(f.getPath()));
		
		Element content = new Element("content");
		new Element(content, "id").addText(id);
		new Element(content, "to").addText(user);
		new Element(content, "from").addText(controller.getUsername());
		Element filenameElement = new Element(content, "filename");
		filenameElement.addText(filename);
		filenameElement.getAttributes().put("bsize", "" + f.length());
		new Element(content, "comment").addText(comment);
		
		requests.put(id, new FileTransferEvent(filename, user, id));
		
		sendMessage(controller, getName(), "file_transfer_request", content);
		
		return NC.SUCCESS;
	}
	
	public void moduleCommand_file_transfer_request(Element content)
	{
		String from = content.get("from").text();
		Element filenameElement = content.get("filename");
		String filename = filenameElement.text();
		long bsize = Long.parseLong(filenameElement.getAttributes().getValue("bsize"));
		long id = Long.parseLong(content.get("id").text());
		String comment = content.get("comment").text();
		
		FileTransferEvent e = new FileTransferEvent(filename, from, id);
		e.size = bsize;
		e.comment = comment;
		
		for(FileTransferListener l : transferListeners)
			l.transferRequest(e);
	}
	public void moduleCommand_file_transfer_decision(Element content)
	{
		Element filenameElement = content.get("filename");
		String filename = filenameElement.text();
		long id = Long.parseLong(content.get("id").text());
		boolean accepted = Boolean.parseBoolean(content.get("accept").text());
		
		FileTransferEvent e = new FileTransferEvent(filename, requests.get(id).user, id);
		e.sending = true;
		
		if(accepted)
			for(FileTransferListener l : transferListeners)
				l.transferAccepted(e);
		else
			for(FileTransferListener l : transferListeners)
				l.transferRejected(e);
	}
	public void moduleCommand_file_chunk_request(Element content)
	{
		long id = Long.parseLong(content.get("id").text());
		int chunkNum = Integer.parseInt(content.get("chunk").getAttributes().getValue("number"));
		
		if(encodedFiles.get(id) == null)
			Debug.printError("Received extraneous file chunk request from user " + content.get("route").getAttributes().getValue("to") +".  Ignoring...");
		else
			sendFileChunk(id, chunkNum);
	}
	public void moduleCommand_file_chunk(Element content)
	{
		Element chunkElement = content.get("chunk");
		
		String from = content.get("route").getAttributes().getValue("from");
		String filename = chunkElement.getAttributes().getValue("filename");
		int chunkNumber = Integer.parseInt(chunkElement.getAttributes().getValue("number"));
		int totalChunks = Integer.parseInt(chunkElement.getAttributes().getValue("maxchunk"));
		long totalBsize = Long.parseLong(chunkElement.getAttributes().getValue("totalbsize"));
		long id = Long.parseLong(content.get("id").text());
		
		String s = receivedEncodedFiles.get(id);
		if(s == null)
			s = "";
		
		for(Object o : chunkElement.getSubObjects())
			s += o;
		
		receivedEncodedFiles.put(id, s);
		
		FileTransferEvent e = new FileTransferEvent();
		e.filename = filename;
		e.user = from;
		e.chunk = chunkNumber;
		e.numChunks = totalChunks;
		e.id = id;
		e.size = totalBsize;
		
		for(FileTransferListener l : transferListeners)
			l.chunkReceived(e);
		
		//TODO: Move
		if(chunkNumber == totalChunks)
		{
			FileTransferEvent e2 = new FileTransferEvent(filename, from, id);
			e2.size = totalBsize;
			e2.chunk = chunkNumber;
			e2.numChunks = totalChunks;
			e2.sending = false;
			
			for(FileTransferListener l : transferListeners)
				l.transferComplete(e2);
		}
	}
	public void moduleCommand_file_transfer_id(Element content)
	{
		currentID = Long.parseLong(content.get("id").text());
	}
	public void moduleCommand_transfer_complete(Element content)
	{
		FileTransferEvent e = new FileTransferEvent();
		e.sending = true;
		e.id = Long.parseLong(content.get("id").text());
		
		for(FileTransferListener l : transferListeners)
			l.transferComplete(e);
	}
	
	public void cleanUp()
	{
			
	}
	public String getName()
	{
		return name;
	}
	public static String getProtocolVersion()
	{
		return protocolVersion;
	}
	public static String getVersion()
	{
		return version;
	}
	public boolean moduleCommandsRequireEventThreadAccess()
	{
		return true;
	}
	public static ArrayList<String> getDependencies()
	{
		return null;
	}
}
