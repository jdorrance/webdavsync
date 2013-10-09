package be.re.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;



public class URLManagerFile implements URLManager.ProtocolHandler

{

  private URLManager	manager;



  public
  URLManagerFile(URLManager manager)
  {
    this.manager = manager;
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
    return destination.getProtocol().equals(source.getProtocol());
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

    if (container)
    {
      Util.urlToFile(url).mkdirs();
    }
    else
    {
      File	parent = Util.urlToFile(url).getParentFile();

      if (parent != null && !parent.exists())
      {
        parent.mkdirs();
      }

      Util.urlToFile(url).createNewFile();
    }

    return url;
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
    if (!Util.urlToFile(url).delete())
    {
      throw new ProtocolException(Util.getResource("file_destroy_error"));
    }
  }



  public boolean
  equal(URL url1, URL url2)
  {
    return Util.urlToFile(url1).equals(Util.urlToFile(url2));
  }



  public boolean
  exists(URL url) throws IOException, ProtocolException
  {
    return Util.urlToFile(url).exists();
  }



  /**
   * The URLs in the result which are containers will end with a slash.
   */

  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    if (Util.isArchive(url))
    {
      try
      {
        return manager.getContained(new URL("jar:" + url.toString() + "!/"));
      }

      catch (Throwable e)
      {
        // It wasn't a Jar file after all.
      }
    }

    try
    {
      File[]	files = Util.urlToFile(url).listFiles();

      if (files == null)
      {
        return new URL[0];
      }

      URL[]	result = new URL[files.length];

      for (int i = 0; i < result.length; ++i)
      {
        result[i] = Util.fileToUrl(files[i]);

        if (files[i].isDirectory())
        {
          String	s = result[i].toString();

          if (!s.endsWith("/"))
          {
            result[i] = new URL(result[i].toString() + "/");
          }
        }
      }

      return result;
    }

    catch (MalformedURLException e)
    {
      throw new ProtocolException(e);
    }
  }



  public URLManager.Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    return manager.genericContainedProperties(url);
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    return new Properties();
  }



  public URLManager.Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    File	file = Util.urlToFile(url);
    List	properties = new ArrayList();

    properties.add
    (
      new URLManager.Property
      (
        "name",
        Util.unescapeUriSpecials(Util.getLastPathSegment(url)),
        true,
        url
      )
    );

    properties.add
    (
      new URLManager.Property
      (
        "access",
        (file.canRead() ? "R" : "-") + (file.canWrite() ? "W" : "-"),
        url
      )
    );

    properties.add
    (
      new URLManager.Property("modified", new Date(file.lastModified()), url)
    );

    properties.
      add(new URLManager.Property("size", new Long(file.length()), url));

    return
      (URLManager.Property[])
        properties.toArray(new URLManager.Property[properties.size()]);
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    if (Util.isArchive(url))
    {
      try
      {
        return manager.isContainer(new URL("jar:" + url.toString() + "!/"));
      }

      catch (Throwable e)
      {
        // It wasn't a Jar file after all.
      }
    }

    return Util.urlToFile(url).isDirectory();
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
    if (!Util.urlToFile(source).renameTo(Util.urlToFile(destination)))
    {
      try
      {
        manager.copy(source, destination);
        manager.destroy(source);

        return true;
      }

      catch (Exception e)
      {
        throw new ProtocolException(Util.getResource("file_move_error"));
      }
    }

    return true;
  }



  public boolean
  useTrashBin(URL url)
  {
    return true;
  }

} // URLManagerFile
