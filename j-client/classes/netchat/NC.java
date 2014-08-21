package netchat;

public class NC {
	
	public static final int NULL = 0;
	public static final int NONE = 0;
	
	//Modules
	public static final int FAIL = 1 << 5;
	public static final int RESUME = 1 << 6;
	
	//MenuBar
	public static final int FILE = 1 << 1;
	public static final int FRIENDS = 1 << 2;
	public static final int HELP = 1 << 3;
	
	//ContextMenu
	public static final int ADD_FRIEND = 1 << 6;
	public static final int REMOVE_FRIEND = 1 << 7;
	public static final int PRIVATE_CHAT = 1 << 8;
	public static final int FILE_TRANSFER = 1 << 9;
	
	//Message Types
	public static final int CHAT = 1 << 1;
	public static final int WARNING = 1 << 2;
	public static final int BACKLOG = 1 << 3;
	public static final int INFO = 1 << 4;
	
	//Add Friend Returns (SUCCESS is for modules as well)
	public static final int SUCCESS = 1 << 1;
	public static final int ERROR = 1 << 2;
	public static final int NOTIFY = 1 << 3;
	
	//Friend Status codes
	public static final int ONLINE = 1 << 1;
	public static final int OFFLINE = 1 << 2;
	public static final int AWAY = 1 << 3;
	public static final int BUSY = 1 << 4;
	public static final int GROUP_CHAT = 1 << 5;
	
	//File Transfer
	public static final int SENDING = 1 << 5;
	public static final int RECEIVING = 1 << 6;
}
