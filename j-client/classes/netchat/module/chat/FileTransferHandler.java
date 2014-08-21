package netchat.module.chat;

import netchat.event.*;
import netchat.module.*;
import netchat.util.*;
import netchat.widgets.FileTransferGUI;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;

import java.util.*;

public class FileTransferHandler {
	
	private ChatModule chatMod;
	private FileTransferModule fileMod;
	
	private HashMap<Long, FileTransferGUI> transferGUIs = null;
	
	public FileTransferHandler(ChatModule chatMod, FileTransferModule fileMod)
	{
		this.chatMod = chatMod;
		this.fileMod = fileMod;
		
		transferGUIs = new HashMap<Long, FileTransferGUI>();
		registerListeners();
	}
	
	private void registerListeners()
	{
		fileMod.addFileTransferListener(new FileTransferAdapter()
		{
			public void transferAccepted(FileTransferEvent e) {
			}
			public void chunkReceived(FileTransferEvent e) {
				FileTransferGUI gui;
				if((gui = transferGUIs.get(e.id)) == null)
				{
					gui = new FileTransferGUI(e);
					transferGUIs.put(e.id, gui);
				}
				
				gui.update(e);
			}
			public void chunkTransfer(FileTransferEvent e) {
				FileTransferGUI gui;
				if((gui = transferGUIs.get(e.id)) == null)
				{
					gui = new FileTransferGUI(e);
					transferGUIs.put(e.id, gui);
				}
				
				gui.update(e);
			}
			public void transferRejected(FileTransferEvent e) {
				if(e.sending)
				{
					MessageBox m = new MessageBox(chatMod.getFriendsList().getShell(), SWT.OK | SWT.ICON_ERROR);
					m.setText("Transfer Rejected");
					m.setMessage("User " + e.user + " rejected your file transfer of " + e.filename + "!");
					m.open();
				}
			}
			public void transferRequest(FileTransferEvent e) {
				MessageBox m = new MessageBox(chatMod.getFriendsList().getShell(), SWT.YES | SWT.NO | SWT.ICON_WORKING);
				m.setText("File Transfer");
				String message = "User " + e.user + " wants to share a file!";
				message += "\n\nFilename: " + e.filename;
				message += "\nSize: " + e.size + " KB";
				message += "\nComment: " + e.comment;
				message += "\n\nDo you accept?";
				m.setMessage(message);
				
				if(m.open() == SWT.YES)
					fileMod.acceptTransfer(e.id);
				else
					fileMod.rejectTransfer(e.id);
			}
			public void transferComplete(FileTransferEvent e) {
				FileTransferGUI gui;
				if((gui = transferGUIs.get(e.id)) != null)
				{
					gui.dispose();
				}
				else
				{
					Debug.printError("Reached null FileTransferGUI...");
				}
			}
		});
	}
}
