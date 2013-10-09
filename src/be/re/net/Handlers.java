package be.re.net;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;



/**
 * Install central protocol handlers.
 * @author Werner Donn\u00e9
 */

public class Handlers

{

  public static void
  addUrlHandlers()
  {
    addUrlHandlers(new String[]{"be.re.net"});
  }



  public static synchronized void
  addUrlHandlers(String[] packageNames)
  {
    if (tryHandlerFactory(packageNames))
    {
      return;
    }

    for (int i = 0; i < packageNames.length; ++i)
    {
      addUrlHandlers(packageNames[i]);
    }
  }



  private static void
  addUrlHandlers(String packageName)
  {
    String	value = System.getProperty("java.protocol.handler.pkgs");

    if (value == null || value.indexOf(packageName) == -1)
    {
      System.setProperty
      (
        "java.protocol.handler.pkgs",
        packageName + (value == null || value.equals("") ? "" : ("|" + value))
      );

      value = System.getProperty("java.protocol.handler.pkgs");

      if (value == null || value.indexOf(packageName) == -1)
      {
        // Retry, because another thread came in between.
        addUrlHandlers(packageName);
      }
    }
  }



  private static boolean
  tryHandlerFactory(String[] packageNames)
  {
    try
    {
      URLManager	manager = new URLManager();
      final Map		protocols = new HashMap();

      for (int i = 0; i < packageNames.length; ++i)
      {
        URL[]	urls =
          manager.getContained
          (
            Util.class.getResource("/" + packageNames[i].replace('.', '/'))
          );

        for (int j = 0; j < urls.length; ++j)
        {
          String	segment = Util.getLastPathSegment(urls[j]);

          if (segment.endsWith("/"))
          {
            segment = segment.substring(0, segment.length() - 1);
          }

          if (protocols.get(segment) == null && manager.isContainer(urls[j]))
          {
            protocols.put(segment, packageNames[i]);
          }
        }
      }

      URL.setURLStreamHandlerFactory
      (
        new URLStreamHandlerFactory()
        {
          public URLStreamHandler
          createURLStreamHandler(String protocol)
          {
            String	packageName = (String) protocols.get(protocol);

            try
            {
              return
                packageName == null ?
                null :
                (URLStreamHandler)
                  Class.forName
                  (
                    packageName + "." + protocol + ".Handler"
                  ).newInstance();
            }

            catch (Exception e)
            {
              return null;
            }
          }
        }
      );

      return true;
    }

    catch (Throwable e)
    {
      return false;
    }
  }

} // Handlers
