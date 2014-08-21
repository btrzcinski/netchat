package netchat.xml.exception;

/**
 * The default parse exception, thrown whenever a message does not conform to NetChat Protocol (NCP).
 * @author Andy Street, 2007
 */

public class XMLParseException extends Exception
{
	/**
	 * Creates an XMLParseException with the specified message.
	 * @param msg the message to be included with the exception
	 */
	public XMLParseException(String msg)
	{
		super(msg);
	}
}
