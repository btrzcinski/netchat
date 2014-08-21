package netchat.module;

public class ModuleEvent {
	String name;
	String version;
	String protocolVersion;
	
	public ModuleEvent(String name, String version, String protocolVersion)
	{
		this.name = name;
		this.version = version;
		this.protocolVersion = protocolVersion;
	}
	
	public String getName()
	{
		return name;
	}
	public String getVersion()
	{
		return version;
	}
	public String getProtocolVersion()
	{
		return protocolVersion;
	}
}
