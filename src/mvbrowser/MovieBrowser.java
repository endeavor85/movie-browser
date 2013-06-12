package mvbrowser;

public class MovieBrowser
{
	public static void main(String args[])
	{
		if(args.length > 0)
			new MovieBrowserFrame(args[0]);
		else
			new MovieBrowserFrame("");
	}
}
