package netchat.event;

public interface FileTransferListener
{
	public void transferRequest(FileTransferEvent e);
	public void transferComplete(FileTransferEvent e);
	public void chunkTransfer(FileTransferEvent e);
	public void chunkReceived(FileTransferEvent e);
	public void transferAccepted(FileTransferEvent e);
	public void transferRejected(FileTransferEvent e);
}
