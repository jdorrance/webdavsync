package be.re.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



/**
 * @author Werner Donn\u00e9
 */

public class URLManagerFTP implements URLManager.ProtocolHandler

{

  private String	password;
  private String	username;



  public
  URLManagerFTP()
  {
    this(null, null);
  }



  public
  URLManagerFTP(String username, String password)
  {
    this.username = username;
    this.password = password;
  }



  public boolean
  canCopy(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canCopy(URL source, URL destination) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canDestroy(URL[] urls) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canMove(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canMove(URL source, URL destination) throws IOException, ProtocolException
  {
    return
      destination.getProtocol().equals(source.getProtocol()) &&
        destination.getAuthority().equals(source.getAuthority());
  }



  public boolean
  canPreserve()
  {
    return true;
  }



  public void
  copy
  (
    URL[]		source,
    URL			destination,
    boolean		overwrite,
    URLManager.Resume	resume
  ) throws IOException, ProtocolException
  {
    return;
  }



  public boolean
  copy(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    return true;
  }



  public URL
  create(URL url, boolean container) throws IOException, ProtocolException
  {
    if (exists(url))
    {
      throw
        new ProtocolException
        (
          Util.getResource("file_already_exists_error")
        );
    }

    FTPClient	client = FTPClient.connect(url.toString(), username, password);

    try
    {
      if (container)
      {
        client.makeDirectory(Util.getFTPFilename(url));
      }
      else
      {
        client.store(Util.getFTPFilename(url)).close();
      }

      return url;
    }

    finally
    {
      client.close();
    }
  }



  public void
  destroy(URL[] urls, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    return;
  }



  public void
  destroy(URL url) throws IOException, ProtocolException
  {
    FTPClient	client = FTPClient.connect(url.toString(), username, password);

    try
    {
      if (isContainer(url))
      {
        client.removeDirectory(Util.getFTPFilename(url));
      }
      else
      {
        client.delete(Util.getFTPFilename(url));
      }
    }

    finally
    {
      client.close();
    }
  }



  public boolean
  equal(URL url1, URL url2)
  {
    return
      Util.unescapeUriSpecials(removeType(url1).toString()).
        equals(Util.unescapeUriSpecials(removeType(url2).toString()));
  }



  public boolean
  exists(URL url) throws IOException, ProtocolException
  {
    FTPClient	client = FTPClient.connect(url.toString(), username, password);

    try
    {
      return client.exists(Util.getFTPFilename(url));
    }

    finally
    {
      client.close();
    }
  }



  /**
   * The URLs in the result which are containers will end with a slash.
   */

  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    FTPClient	client = null;

    try
    {
      client = FTPClient.connect(url.toString(), username, password);

      String[]	files = client.nameList(Util.getFTPFilename(url));

      URL[]	result = new URL[files.length];

      for (int i = 0; i < result.length; ++i)
      {
        result[i] =
          inheritType
          (
            url,
            new URL
            (
              "ftp://" + url.getAuthority() +
                Util.escapeUriPathSegments(files[i].replace('\\', '/')) +
                  (client.isDirectory(files[i]) ? "/" : "")
            )
          );
      }

      return result;
    }

    catch (MalformedURLException e)
    {
      throw new ProtocolException(e);
    }

    finally
    {
      if (client != null)
      {
        client.close();
      }
    }
  }



  public URLManager.Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    FTPClient	client = FTPClient.connect(url.toString(), username, password);

    try
    {
      String[][]        	list =
        client.nameListWithDetails(Util.getFTPFilename(url));
      URLManager.Property[][]	properties =
        new URLManager.Property[list.length][2];

      for (int i = 0; i < list.length; ++i)
      {
        String	name =
          Util.getLastPathSegment(list[i][0].replace('\\', '/')) +
            (client.isDirectory(list[i][0]) ? "/" : "");

        properties[i][0] =
          new URLManager.Property("name", name, true, new URL(url, name));
        properties[i][1] =
          new URLManager.Property("details", list[i][1], new URL(url, name));
      }

      return properties;
    }

    finally
    {
      client.close();
    }
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    return new Properties();
  }



  public URLManager.Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    List	properties = new ArrayList();

    properties.add
    (
      new URLManager.Property("name", Util.getLastPathSegment(url), true, url)
    );

    if (!isContainer(url))
    {
      FTPClient	client = FTPClient.connect(url.toString(), username, password);

      try
      {
        String[]	details = client.status(Util.getFTPFilename(url));

        properties.add
        (
          new URLManager.Property
          (
            "details",
            details.length == 1 ? details[0] : "",
            url
          )
        );
      }

      finally
      {
        client.close();
      }
    }
    else
    {
      properties.add(new URLManager.Property("details", "", url));
    }

    return
      (URLManager.Property[])
        properties.toArray(new URLManager.Property[properties.size()]);
  }



  private static String
  getType(URL url)
  {
    String	s = url.toString();
    int		i = s.lastIndexOf(";type=");

    return i != -1 ? s.substring(i + 6, i + 7) : null;
  }



  private static URL
  inheritType(URL parent, URL child)
  {
    return getType(parent) != null ? setType(child, getType(parent)) : child;
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    FTPClient	client = FTPClient.connect(url.toString(), username, password);

    try
    {
      return client.isDirectory(Util.getFTPFilename(url));
    }

    finally
    {
      client.close();
    }
  }



  public boolean
  link(URL url, URL newBinding) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  move(URL[] source, URL destination, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  move(URL source, URL destination) throws IOException, ProtocolException
  {
    if (source.getHost().equals(destination.getHost()))
    {
      FTPClient	client =
        FTPClient.connect(source.toString(), username, password);

      try
      {
        client.rename
        (
          Util.getFTPFilename(source),
          Util.getFTPFilename(destination)
        );

        return true;
      }

      finally
      {
        client.close();
      }
    }

    return false;
  }



  private static URL
  removeType(URL url)
  {
    try
    {
      return new URL(url.toString().replaceAll(";type=[aid]", ""));
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  private static URL
  setType(URL url, String type)
  {
    try
    {
      String    s = url.toString();

      return
        new URL
        (
          s.endsWith("/") ?
            (
              s.substring(0, s.length() - 1) + ";type=" + type + "/"
            ) :
            (s + ";type=" + type)
        );
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be bug.
    }
  }



  public boolean
  useTrashBin(URL url)
  {
    return true;
  }

} // URLManagerFTP
