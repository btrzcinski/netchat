package netchat.xml.exception;

/**
 * A XMLServerModuleMismatchException is thrown whenever a message contains some attributes of a server message, while at the
 * the same time claiming to be module message, and vice-versa.
 * @author Andy Street, 2007
 */

public class XMLServerModuleMismatchException extends XMLParseException
{
	/**
	 * Creates an XMLServerModuleMismatchException with the specified message.
	 * @param msg the message to be included with the exception
	 */
	public XMLServerModuleMismatchException(String msg)
	{
		super(msg);
	}
}
