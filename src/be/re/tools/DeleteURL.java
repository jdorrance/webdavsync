package be.re.tools;

import be.re.net.URLManager;
import java.net.URL;



public class DeleteURL

{

  public static void
  main(String[] args) throws Exception
  {
    be.re.net.Handlers.addUrlHandlers();
    URLManager.setInteractive(false);

    for (int i = 0; i < args.length; ++i)
    {
      System.out.println(args[i]);

      try
      {
        new URLManager().destroy(new URL(args[i]));
      }

      catch (Exception e)
      {
        System.err.println(e.getMessage());
      }
    }
  }

} // DeleteURL
