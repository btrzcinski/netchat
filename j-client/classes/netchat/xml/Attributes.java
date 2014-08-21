package netchat.xml;

import org.xml.sax.ext.Attributes2Impl;

public class Attributes extends Attributes2Impl {
	public Attributes()
	{
		super();
	}
	public Attributes(org.xml.sax.Attributes a)
	{
		super(a);
	}
	
	public void put(String key, String value)
	{
		addAttribute("", key, key, "CDATA", value);
	}
	public String get(String key)
	{
		return getValue(key);
	}
}
