public class bittest
{
	public static void main(String[] a)
	{
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		String hex = "";
		byte[] b = {(byte)2, (byte)255, (byte)98, (byte)54};

		for(int i = 0; i < b.length; i++)
		{
			hex += hexChars[(int)((b[i] & 0xf0) >>> 4)];
			hex += hexChars[(int)(b[i] & 0x0f)];
		}
		
		System.out.println(hex);
	}
}
