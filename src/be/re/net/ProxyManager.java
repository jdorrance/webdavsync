package be.re.net;

import be.re.io.StreamConnector;
import be.re.util.Compare;
import be.re.util.Sort;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;



public class ProxyManager

{

  private static HashMap	proxies = loadProxies();



  public static synchronized Entry[]
  getProxies()
  {
    Set		keys = proxies.keySet();
    Iterator	iter = keys.iterator();
    Entry[]	result = new Entry[keys.size()];

    for (int i = 0; i < result.length; ++i)
    {
      String		key = (String) iter.next();
      StringTokenizer	tokenizer = new StringTokenizer(key, "#");
      Object		value = proxies.get(key);

      result[i] =
        new Entry
        (
          tokenizer.nextToken(),
          tokenizer.nextToken(),
          value instanceof URL ? (URL) value : null
        );
    }

    return result;
  }



  /**
   * The method for a proxy URL using the <code>host</code>. If nothing was
   * found the IP address is tried. If that still doesn't yield a result
   * the domain part of the <code>host</code> is used up to the highest level.
   * Then the network addresses are used for the search up to the highest
   * level.
   * @return the proxy URL or <code>null</code> if none exists.
   */

  public static synchronized URL
  getProxy(String host, String protocol)
  {
    host = host.toLowerCase();
    protocol = protocol.toLowerCase();

    boolean	hasAddress = true;
    Object	result = proxies.get(host + "#" + protocol);

    if (result == null)
    {
      try
      {
        result =
          proxies.
            get(InetAddress.getByName(host).getHostAddress() + "#" + protocol);
      }

      catch (UnknownHostException e)
      {
        hasAddress = false;
      }
    }

    if (result == null)
    {
      result = searchHierarchy(host, protocol, true);
    }

    if (result == null && hasAddress)
    {
      try
      {
        result =
          searchHierarchy
          (
            InetAddress.getByName(host).getHostAddress(),
            protocol,
            false
          );
      }

      catch (UnknownHostException e)
      {
        // We can't get here due to hasAddress.
      }
    }

    if (result == null)
    {
      result = proxies.get("default#" + protocol.toLowerCase());
    }

    return result instanceof URL ? (URL) result : null;
  }



  private static File
  getStorage(String filename)
  {
    File	directory =
      new File
      (
        new File(new File(System.getProperty("user.home"), ".be"), "re"),
        "net"
      );

    if (!directory.exists())
    {
      directory.mkdirs();
    }

    return new File(directory, filename);
  }



  private static Object
  getUrl(String url) throws MalformedURLException
  {
    return url.equals("none") ? (Object) "none" : (Object) new URL(url);
  }



  private static HashMap
  loadProxies()
  {
    BufferedReader	in = null;

    try
    {
      File	file = getStorage("ProxyManager");

      if (!file.exists())
      {
        StreamConnector.copy
        (
          ProxyManager.class.getResourceAsStream("res/ProxyManager"),
          new FileOutputStream(file)
        );
      }

      in = new BufferedReader(new FileReader(file));

      return readEntries(in);
    }

    catch (Throwable e)
    {
      throw new RuntimeException(e);
    }

    finally
    {
      if (in != null)
      {
        try
        {
          in.close();
        }

        catch (Throwable e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }



  private static HashMap
  readEntries(BufferedReader in) throws IOException
  {
    HashMap	map = new HashMap();
    String	s;

    while ((s = in.readLine()) != null)
    {
      if (s.indexOf('#') != -1)
      {
        s = s.substring(0, s.indexOf('#'));
      }

      s = s.trim();

      if (s.length() > 0)
      {
        StringTokenizer	tokenizer = new StringTokenizer(s, ";");

        if (tokenizer.countTokens() != 3)
        {
          System.err.println("ProxyManager: syntax error: " + s);
        }
        else
        {
          map.put
          (
            tokenizer.nextToken().toLowerCase() + "#" +
              tokenizer.nextToken().toLowerCase(),
            getUrl(tokenizer.nextToken())
          );
        }
      }
    }

    return map;
  }



  public static void
  removeProxy(String host, String protocol)
  {
    proxies.remove(host.toLowerCase() + "#" + protocol.toLowerCase());
  }



  /**
   * Saves all entries in durable storage, where they were loaded from.
   */

  public static synchronized void
  save() throws IOException
  {
    File	saveFile = null;
    PrintWriter	writer = null;

    try
    {
      saveFile =
        be.re.io.Util.
          createTempFile("proxy", null, getStorage("dummy").getParentFile());
      writer = new PrintWriter(new FileOutputStream(saveFile));
      writeEntries(writer);
      writer.close();
      writer = null;
      getStorage("ProxyManager.bak").delete();
      getStorage("ProxyManager").renameTo(getStorage("ProxyManager.bak"));
      saveFile.renameTo(getStorage("ProxyManager"));
    }

    catch (Throwable e)
    {
      if (saveFile != null)
      {
        saveFile.delete();
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }

    finally
    {
      if (writer != null)
      {
        try
        {
          writer.close();
        }

        catch (Throwable e2)
        {
        }
      }
    }
  }



  private static Object
  searchHierarchy(String name, String protocol, boolean forward)
  {
    Object	result = null;

    while (result == null)
    {
      result = proxies.get(name + "#" + protocol);

      if (name.indexOf('.') == -1)
      {
        break;
      }

      name =
        forward ?
          (
            name.indexOf('.') == name.length() - 1 ?
              "" : name.substring(name.indexOf('.') + 1)
          ) :
          name.substring(0, name.lastIndexOf('.'));
    }

    return result;
  }



  public static synchronized void
  setProxies(Entry[] entries)
  {
    proxies.clear();

    for (int i = 0; i < entries.length; ++i)
    {
      setProxy(entries[i].host, entries[i].protocol, entries[i].url);
    }
  }



  /**
   * Set the proxy URL for some host/protocol combination, both being case-
   * insensitive. If <code>host</code> has the value "default", the associated
   * <code>url</code> will be returned in the <code>getProxy</code> method if
   * case no other entry matches. If <code>url</code> is <code>null</code>,
   * "none" will be inserted as the value. With this value one can express
   * no proxy must be used for a certain host.
   */

  public static synchronized void
  setProxy(String host, String protocol, URL url)
  {
    proxies.put
    (
      host.toLowerCase() + "#" + protocol.toLowerCase(),
      url != null ? (Object) url : (Object) "none"
    );
  }



  private static void
  writeEntries(PrintWriter writer) throws IOException
  {
    Entry[]	entries =
      (Entry[]) Sort.qsort
      (
        getProxies(),
        new Compare()
        {
          public int
          compare(Object object1, Object object2)
          {
            int	result =
              ((Entry) object1).host.compareTo(((Entry) object2).host);

            return
              result == 0 ?
                ((Entry) object1).protocol.
                  compareTo(((Entry) object2).protocol) :
                result;
          }
        }
      );

    for (int i = 0; i < entries.length; ++i)
    {
      writer.write(entries[i].host + ";" + entries[i].protocol + ";");

      writer.println
      (
        entries[i].url != null ? entries[i].url.toString() : "none"
      );
    }
  }



  public static class Entry

  {

    private String	host;
    private String	protocol;
    private URL		url;



    public
    Entry(String host, String protocol, URL url)
    {
      this.host = host;
      this.protocol = protocol;
      this.url = url;
    }



    public String
    getHost()
    {
      return host;
    }



    public String
    getProtocol()
    {
      return protocol;
    }



    public URL
    getUrl()
    {
      return url;
    }

  } // Entry

} // ProxyManager
