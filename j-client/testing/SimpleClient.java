import java.net.*;
import java.io.*;

public class SimpleClient
{
	public static void main(String[] args)
	{
		Socket sock = null;
		
		PrintStream p = null;
		try{
			sock = new Socket("localhost", 45287);
			p = new PrintStream(sock.getOutputStream());
		}
		catch (Exception e)
		{}

		p.print("test\n");
		
		p.print("test\n");
	}
}
