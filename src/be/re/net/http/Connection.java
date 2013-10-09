package be.re.net.http;

import be.re.io.PipedInputStream;
import be.re.io.PipedOutputStream;
import be.re.net.HTTPClient;
import be.re.net.Headers;
import be.re.net.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public class Connection extends URLConnection

{

  private InputStream	in = null;
  private OutputStream	out = null;
  private Headers	requestHeaders = new Headers();
  private Headers	responseHeaders = new Headers();



  public
  Connection(URL url)
  {
    super(url);
  }



  public void
  addRequestProperty(String key, String value)
  {
    requestHeaders.add(key, value);
  }



  public void
  connect() throws IOException
  {
    if (getIfModifiedSince() != -1)
    {
      requestHeaders.
        add("If-Modified-Since", new Date(getIfModifiedSince()).toString());
    }

    if (getDoOutput())
    {
      PipedOutputStream		outPipe = new PipedOutputStream();
      final PipedInputStream	inPipe = new PipedInputStream(outPipe);

      new Thread
      (
        new Runnable()
        {
          public void
          run()
          {
            try
            {
              in =
                HTTPClient.request
                (
                  HTTPClient.POST,
                  getURL(),
                  inPipe,
                  requestHeaders,
                  responseHeaders,
                  null,
                  responseHeaders
                );
            }

            catch (Throwable e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      ).start();

      out = outPipe;
    }
    else
    {
      in =
        HTTPClient.request
        (
          HTTPClient.GET,
          getURL(),
          null,
          requestHeaders,
          responseHeaders,
          null,
          responseHeaders
        );
    }
  }



  public String
  getContentEncoding()
  {
    return getResponseValue("Content-Encoding");
  }



  public int
  getContentLength()
  {
    return
      getResponseValue("Content-Length") == null ?
        -1 : Integer.parseInt(getResponseValue("Content-Length"));
  }



  public String
  getContentType()
  {
    return getResponseValue("Content-Type");
  }



  public long
  getDate()
  {
    return
      getResponseValue("Date") == null ? 0 : getDate(getResponseValue("Date"));
  }



  private long
  getDate(String s)
  {
    return Util.httpDate(s);
  }



  public long
  getExpiration()
  {
    return
      getResponseValue("Expires") == null ?
        0 : getDate(getResponseValue("Expires"));
  }



  public String
  getHeaderField(int n)
  {
    return
      n >= responseHeaders.getAll().length ?
        null : responseHeaders.getAll()[n].getValue();
  }



  public String
  getHeaderField(String name)
  {
    return getResponseValue(name);
  }



  public long
  getHeaderFieldDate(String name, long def)
  {
    return
      getResponseValue(name) == null ?
        def : getDate(getResponseValue(name));
  }



  public int
  getHeaderFieldInt(String name, int def)
  {
    return
      getResponseValue(name) == null ?
        def : Integer.parseInt(getResponseValue(name));
  }



  public String
  getHeaderFieldKey(int n)
  {
    return
      n >= responseHeaders.getAll().length ?
        null : responseHeaders.getAll()[n].getName();
  }



  public Map
  getHeaderFields()
  {
    return getHeaderMap(responseHeaders);
  }



  public Map
  getHeaderMap(Headers headers)
  {
    String[]	keys = getKeys(headers);
    Map		result = new HashMap();

    for (int i = 0; i < keys.length; ++i)
    {
      result.put(keys[i], getHeaderValue(headers, keys[i]));
    }

    return result;
  }



  private String
  getHeaderValue(Headers headers, String header)
  {
    String[]	value = headers.get(header);
    String	result = "";

    for (int i = 0; i < value.length; ++i)
    {
      result += result.equals("") ? value[i] : (", " + value[i]);
    }

    return result.equals("") ? null : result;
  }



  public InputStream
  getInputStream() throws IOException
  {
    connect();

    return in;
  }



  private String[]
  getKeys(Headers headers)
  {
    Headers.Header[]	all = headers.getAll();
    Set	        	result = new HashSet();

    for (int i = 0; i < all.length; ++i)
    {
      result.add(all[i].getName());
    }

    return (String[]) result.toArray(new String[result.size()]);
  }



  public long
  getLastModified()
  {
    return
      getResponseValue("Last-Modified") == null ?
        0 : getDate(getResponseValue("Last-Modified"));
  }



  public OutputStream
  getOutputStream() throws IOException
  {
    setDoOutput(true);
    connect();

    return out;
  }



  private String
  getRequestValue(String header)
  {
    return getHeaderValue(requestHeaders, header);
  }



  private String
  getResponseValue(String header)
  {
    return getHeaderValue(responseHeaders, header);
  }



  public Map
  getRequestProperties()
  {
    return getHeaderMap(requestHeaders);
  }



  public String
  getRequestProperty(String key)
  {
    return getRequestValue(key);
  }



  public boolean
  getUseCaches()
  {
    return false;
  }



  public void
  setRequestProperty(String key, String value)
  {
    requestHeaders.set(key, value);
  }

} // Connection
