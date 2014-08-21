package netchat.xml;

import netchat.util.*;
import netchat.xml.exception.*;

import java.util.*;
import org.xml.sax.Attributes;

/**
 * Whenever the XMLParser parses the message, it creates an XMLData object to house all the data stored in the Strgin XML tree 
 * in an easier to access form.  An XMLData object contains a HashMap of properties such as type (modulemessage or servermessage),
 * and an XMLElement that represents the XML tree within and including the content tags.  The XMLData object is only used to
 * route messages by the Controller and the handle() method in AbstractModule, to check systax of incoming messages, and provide
 * some debug information.  A typical XMLData object would look like this:<br>
 * Main Dictionary:<br>
 *       "type":"modulemessage"<br>
 *       "properties":{"name":"mail", "type":"receive_message"}<br><br>
 *
 * XMLElement content:
 * <code>
 * <pre>   &lt;content&gt;
 *      &lt;from&gt;foo@bar.com&lt;/from&gt;
 *      &lt;subject&gt;NetChat&lt;/subject&gt;
 *      &lt;message&gt;Hey, NetChat is great!&lt;/message&gt; 
 *    &lt;/content&gt;</pre>
 * <code>
 *
 * See XMLElement for more information.
 * @author Andy Street, 2007
 */

public class XMLData
{
	static final long serialVersionUID = 0L;
	
	/**
	 * Contains the XML tree along with all message attributes.
	 */
	public HashMap h;
	
	/**
	 * The default constructor.
	 */
	public XMLData()
	{
		h = new HashMap();
		h.put("type", null);
	}
	
	/**
	 * Sets the type of the message as either "modulemessage" or "servermessage".
	 * @throws XMLParseException If the type has already been set or if the type is not "modulemessage" or "servermessage".
	 */
	public void setType(String type) throws XMLParseException
	{
		if(h.get("type") != null)
			throw new XMLParseException("Trying to set type again...");
		else if(!(type.equals("modulemessage") || type.equals("servermessage")))
			throw new XMLParseException("Trying to set an invalid NCP message type.");
		else
			h.put("type", type);
	}
	
	/**
	 * Adds module properties to the properties map.
	 * @param key the attribute key from the properties element in the original message
	 * @param val the attribute value from the properties element in the original message
	 * @throws XMLServerModuleMismatchException If "type" has not been set or if type does not equal "modulemessage".
	 */
	public void addModuleProperties(String key, String val) throws XMLServerModuleMismatchException
	{
		if(h.get("type") == null)
		{
			throw new XMLServerModuleMismatchException("Type has not been set.");
		}
		if(!(h.get("type").equals("modulemessage")))
		{	
			throw new XMLServerModuleMismatchException("Attempting to set module in a non-module message.");
		}
		
		
		if(h.get("properties") == null)
			h.put("properties", new HashMap<String, String>());
		
		((HashMap<String, String>)(h.get("properties"))).put(key, val);
	
	}
	
	/**
	 * Adds server properties to the properties map.
	 * @param key the attribute key from the properties element in the original message
	 * @param val the attribute value from the properties element in the original message
	 * @throws XMLServerModuleMismatchException If "type" has not been set or if type does not equal "servermessage".
	 */
	public void addServerProperties(String key, String val) throws XMLServerModuleMismatchException
	{
		if(h.get("type") == null)
		{
			throw new XMLServerModuleMismatchException("Type has not been set.");
		}
		if(!(h.get("type").equals("servermessage")))
		{
			throw new XMLServerModuleMismatchException("Attempting to set server in a non-server message.");
		}
		
		if(h.get("properties") == null)
			h.put("properties", new HashMap<String, String>());
		
		((HashMap<String, String>)(h.get("properties"))).put(key, val);
	}
	
	/**
	 * Sets the content Element.
	 * @param e the element to set as the root element of the content tree
	 */
	public void setContent(Element e)
	{
		if(!e.getName().equals("content"))
			Debug.printError("Setting content to a non content element!");
		
		h.put("content", e);
	}
	
	/**
	 * Returns the content element.
	 */
	public Element getContent()
	{
		return (Element)(h.get("content"));
	}
	
	/**
	 * Prints a String representation of the XML tree this XMLData object represents to standard out.
	 */
	public void printXMLData()
	{
		Iterator i = h.keySet().iterator();
		
		while(i.hasNext())
		{
			Object key = i.next();
			System.out.print(key + ":");
			
			Object val = h.get(key);
			
			if(val instanceof HashMap)
			{
				Iterator in = ((HashMap)val).keySet().iterator();
				
				while(in.hasNext())
				{
					String k = (String)(in.next());
					System.out.println("\n  " + k + ":" + ((HashMap)val).get(k));
				}
			}
			else if(val instanceof Element)
			{
				System.out.println();
				printElement((Element)(val), "\t\t");
			}
			else
				System.out.println("\t" + val);
		}
	}
	
	/**
	 * Prints an element along with its attributes to standard out.
	 * @param e the element to print out
	 * @param tabs the number of tabs to output before the element [used by printXMLData()]
	 */
	public void printElement(Element e, String tabs)
	{
		System.out.print(tabs + "<" + e.getName());
		
		Attributes a = e.getAttributes();
		for(int i = 0; i < a.getLength(); i++)
			System.out.print(" " + a.getLocalName(i) + "=\"" + a.getValue(i) + "\"");
		System.out.println(">");

		ListIterator lit = e.getSubObjects().listIterator();
		
		while(lit.hasNext())
		{
			Object o = lit.next();
			String className = o.getClass().getName();
			
			if(className.equals("XMLElement"))
				printElement(((Element)(o)), tabs + "\t");
			else
				System.out.println(tabs + "\t" + o);
		}
		
		System.out.println(tabs + "</" + e.getName() + ">");
	}
}
