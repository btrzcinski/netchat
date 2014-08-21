import java.io.*;

public class ParserTester
{
	public static void main(String[] a) throws IOException
	{
		NCXMLParser p = new NCXMLParser();
		
		p.parse(new FileInputStream(new File("testmsg.xml")));
	}
}
