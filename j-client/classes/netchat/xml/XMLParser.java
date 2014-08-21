package netchat.xml;

import netchat.system.*;
import netchat.util.*;
import netchat.xml.exception.*;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;
import org.xml.sax.ext.Attributes2Impl;
import org.apache.xerces.parsers.SAXParser;

/**
 * The NCParser implements ContentHandler to use the SAXParser contained in Apache's J-Xerces2 implementation.  The parser uses
 * callbacks to construct an XMLData object which contains message properties and an XMLElement representing the base of the
 * content XML tree.  See http://netchat.tjhsst.edu/trac/netchat/wiki/NCP for more information.
 * @author Andy Street, 2007
 */

public class XMLParser implements ContentHandler
{
	static final long serialVersionUID = 0L;

	String currentElement = null;
	SAXParser parser;
	XMLData d = null;
	ArrayList<Element> elements = null;
	ArrayList<String> elementNames = null;
	Controller con = null;
	boolean inContent = false;
	
	/**
	 * The default constructor.
	 */
	public XMLParser(Controller c)
	{
		Debug.println("Initializing base parser objects...", 3);
		con = c;
		
		Debug.println("Creating parser...", 3);
		parser = new SAXParser();
		
		Debug.println("Setting Handler...", 3);
		parser.setContentHandler(this);
	}
	/**
	 * Initiates message parsing.
	 * @param r the reader that buffers the message [usually a StringReader provided by the #netchat.system.ClientConnector]
	 */
	public void parse(Reader r)
	{
		try{
			parser.parse(new InputSource(r));
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	public void startDocument()
	{
		Debug.println("Initializing parser objects...", 4);
		elements = new ArrayList<Element>();
		elementNames = new ArrayList<String>();
		currentElement = null;
		inContent = false;
		
		d = new XMLData();
		Debug.println("Incoming XML message, starting parser...", 4);
	}
	public void endDocument()
	{
		Debug.println("XML message parsed.  Ending...", 4);
		
		
		if(!((HashMap<String, String>)(d.h.get("properties"))).get("type").equals("pong"))
		{
			Debug.println("XMLData stored:", 2);
			if(Debug.debugLevel() >= 2)
				d.printXMLData();
		}
		
		Debug.println("Sending XML data to connector...", 4);
		
		con.handleXMLData(d);
	}
	public void processingInstruction(String target, String data)
	{
	}
	public void startPrefixMapping(String prefix, String uri)
	{
	}
	public void endPrefixMapping(String prefix)
	{
	}
	public void startElement(String namespaceURI, String localName, String rawName, Attributes as)
	{	
		if(Debug.debugLevel() >= 5)
		{
			Debug.println("Start of element received:", 5);
			Debug.println("  Namespace URI: " + namespaceURI, 5);
			Debug.println("  Local Name: " + localName, 5);
			Debug.println("  Raw Name: " + rawName, 5);
			Debug.println("  Attributes:", 5);
			for(int i = 0; i < as.getLength(); i++)
			{
				Debug.println("    Namespace URI: " + as.getURI(i), 5);
				Debug.println("    Type: " + as.getType(i), 5);
				Debug.println("    Qualified (prefixed) Name: " + as.getQName(i), 5);
				Debug.println("    Local Name: " + as.getLocalName(i), 5);
				Debug.println("    Value: " + as.getValue(i), 5);
			}
		}
		
		if(inContent)
		{
			netchat.xml.Attributes a = new netchat.xml.Attributes();
			a.setAttributes(as);

			Element e = new Element(localName, a);
			
			elements.get(elements.size() - 1).addElement(e);
			elements.add(e);
		}
		else if(localName.equals("content"))
		{
			Debug.println("Reached content tag, now recording into content element...", 5);
				
			inContent = true;
			
			netchat.xml.Attributes a = new netchat.xml.Attributes();
			a.setAttributes(as);

			Element content = new Element(localName, a);
			d.setContent(content);
			
			elements.add(content);
		}
		else
		{
			boolean setData = false;
			
			for(int i = 0; i < as.getLength(); i++)
			{
				try{
					if(currentElement.equals("global"))			//Type
					{
						if(localName.equals("properties"))
							if(as.getLocalName(i).equals("type"))
							{
								d.setType(as.getValue(i));
								setData = true;
								
								Debug.println("Setting type = " + as.getValue(i), 5);
							}
					}
					else if(currentElement.equals("modulemessage"))		//Module
					{
						if(localName.equals("properties"))
						{
							d.addModuleProperties(as.getLocalName(i), as.getValue(i));
							setData = true;
							
							Debug.println("Adding module properties..." , 5);
						}
					}
					else if(currentElement.equals("servermessage"))		//Server
					{
						if(localName.equals("properties"))
						{
							d.addServerProperties(as.getLocalName(i), as.getValue(i));
							setData = true;
							
							Debug.println("Adding server properties...", 5);
						}
					}
					
					if(!setData)
						Debug.println("Unsupported options submitted.  Ignoring.", 0);
				}
				catch (XMLParseException e)
				{
					String errmsg = "There was an error in an XML message (server).  Ignoring message.";
					Debug.println(errmsg, 0);
					Debug.println(e.getMessage(), 0);
				}
			}
		}
		
		elementNames.add(localName);
		currentElement = localName;		
		
		Debug.println("End of start of element.", 5);
	}
	public void endElement(String namespaceURI, String localName, String rawName)
	{
		if(Debug.debugLevel() >= 5)
		{
			Debug.println("End of element received:", 5);
			Debug.println("  Namespace URI: " + namespaceURI, 5);
			Debug.println("  Local Name: " + localName, 5);
			Debug.println("  Raw Name: " + rawName, 5);
			Debug.println("End of end of element.", 5);
		}
		
		int nameLen = elementNames.size();
		int elemLen = elements.size();
		
		elementNames.remove(nameLen - 1);
		
		if(elemLen > 0)
			elements.remove(elemLen - 1);
		
		if(nameLen > 1)
			currentElement = ((String)(elementNames.get(nameLen - 2)));
	}
	public void characters(char[] ch, int s, int len)
	{
		String content = new String(ch, s, len);
		
		if(content.length() != 0)
		{
			if(Debug.debugLevel() >= 5)
			{
				Debug.println("Characters received:", 5);
				Debug.println("  Characters: " + content, 5);
				Debug.println("End of characters.", 5);
				Debug.println("Adding content...", 5);
			}
			
			if(inContent)
				elements.get(elements.size() - 1).addText(content);
			else
				Debug.println("received content outside of the content tags... Ignoring.", 0);
		}
	}
	
	public void error(SAXParseException e) //We use the default error and fatal error handlers
	{
	}
	public void fatalError(SAXParseException e)
	{
	}
	
	public void setDocumentLocator(Locator loc)
	{
		
	}
	public void ignorableWhitespace(char[] ch, int s, int len)
	{
		if(Debug.debugLevel() >= 5)
		{
			Debug.println("Ignorable whitespace:", 5);
			Debug.println("  Characters: " + new String(ch, s, len), 5);
			Debug.println("End of ignorable whitespace.", 5);
		}
	}
	public void skippedEntity(String entName)
	{
		Debug.println("Skipped entity: " + entName, 1);
	}
}
