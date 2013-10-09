package be.re.net;

import be.re.io.DevNullInputStream;
import be.re.util.URLsFromHTML;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



public class URLManagerHTTP implements URLManager.ProtocolHandler

{

  private URLManager    manager;



  public
  URLManagerHTTP(URLManager manager)
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
    return false;
  }
  
  

  public boolean
  canPreserve()
  {
    return false;
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
    if (container && !isContainer(url))
    {
      url = new URL(url.toString() + "/");
    }

    if (exists(url))
    {
      throw new ProtocolException("URL already exists");
    }

    HTTPClient.request(HTTPClient.POST, url, new DevNullInputStream()).close();

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
    HTTPClient.request(HTTPClient.DELETE, url);
  }



  public boolean
  equal(URL url1, URL url2)
  {
    return
      Util.unescapeUriSpecials(url1.toString()).
        equals(Util.unescapeUriSpecials(url2.toString()));
  }



  public boolean
  exists(URL url) throws IOException, ProtocolException
  {
    if (url.getProtocol().equals("http"))
    {
      Headers	headers = new Headers();

      try
      {
        HTTPClient.
          request(HTTPClient.HEAD, url, null, headers, null, null, null);
      }

      catch (ProtocolException e)
      {
        if (e.getCode() == 404)
        {
          return false;
        }

        throw e;
      }

      return headers.size() > 0;
    }

    return true;
  }



  /**
   * The URLs in the result which are containers will end with a slash.
   */

  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    InputStream	in = HTTPClient.request(HTTPClient.GET, url);

    try
    {
      URL[]		links = URLsFromHTML.extract(in, url);
      URLManager	manager = new URLManager();
      List		urls = new ArrayList();

      for (int i = 0; i < links.length; ++i)
      {
        if (equal(url, manager.getParent(links[i])))
        {
          urls.add
          (
            url.getUserInfo() != null && !url.getUserInfo().equals("") ?
              Util.setUserInfo(links[i], url.getUserInfo()) : links[i]
          );
        }
      }

      return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    finally
    {
      in.close();
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
    Headers	headers = new Headers();
    List	properties = new ArrayList();

    properties.add
    (
      new URLManager.Property("name", Util.getLastPathSegment(url), true, url)
    );

    if (!url.getProtocol().equals("ftp"))
    {
      HTTPClient.
        request(HTTPClient.HEAD, url, null, null, headers, null, null);

      if (headers.get("Last-Modified").length > 0)
      {
        DateFormat	format = DateFormat.getInstance();

        format.setLenient(true);

        try
        {
          properties.add
          (
            new URLManager.Property
            (
              "modified",
              format.parse(headers.get("Last-Modified")[0]),
              url
            )
          );
        }

        catch (ParseException e)
        {
          properties.add
          (
            new
              URLManager.
                Property("modified", headers.get("Last-Modified")[0], url)
          );
        }
      }
      else
      {
        properties.add(new URLManager.Property("modified", null, url));
      }

      properties.add
      (
        new URLManager.Property
        (
          "size",
          headers.get("Content-Length").length > 0 ?
            new Long(headers.get("Content-Length")[0]) : null,
          url
        )
      );

      properties.add
      (
        new URLManager.Property
        (
          "type",
          headers.get("Content-Type").length > 0 ?
            headers.get("Content-Type")[0] : null,
          url
        )
      );
    }
    else
    {
      properties.add(new URLManager.Property("modified", null, url));
      properties.add(new URLManager.Property("size", null, url));
      properties.add(new URLManager.Property("type", null, url));
    }

    return
      (URLManager.Property[])
        properties.toArray(new URLManager.Property[properties.size()]);
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    return url.toString().endsWith("/");
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
    throw new ProtocolException(Util.getResource("http_move_error"));
  }



  public boolean
  useTrashBin(URL url)
  {
    return true;
  }

} // URLManagerHTTP
