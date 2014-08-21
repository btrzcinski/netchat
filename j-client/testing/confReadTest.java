import java.util.*;
import java.io.*;
public class confReadTest
{
	public static void main(String[] argss)
	{
		Scanner in = null;
		try { in = new Scanner(new File("etc/modules.conf")); }
		catch (FileNotFoundException e) {}
		
		while(in.hasNext())
		{
			String s = in.nextLine();
			
			int poundIndex = s.indexOf('#');
			if(poundIndex != -1)
				s = s.substring(0, poundIndex);
			
			if(s.equals(""))
				continue;
			
			String[] args = s.split("\\s+", 0);
			
			if(args.length != 2)
			{
				System.out.println("Reached invalid conf statement: " + s + "." + "Ignoring...");
				for(int i = 0; i < args.length; i++)
					System.out.println(args[i]);
				continue;
			}
			
			System.out.println("Adding " + args[0] + ":" + args[1] + " to module classname map...");
		}
	}
}
