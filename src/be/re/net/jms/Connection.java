package be.re.net.jms;

import be.re.io.URLInputStream;
import be.re.io.URLOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;



public class Connection extends URLConnection

{

  public
  Connection(URL url)
  {
    super(url);
  }



  public void
  addRequestProperty(String key, String value)
  {
  }



  public void
  connect() throws IOException
  {
  }



  public String
  getContentEncoding()
  {
    return null;
  }



  public int
  getContentLength()
  {
    return -1;
  }



  public String
  getContentType()
  {
    return null;
  }



  public long
  getDate()
  {
    return 0;
  }



  public long
  getExpiration()
  {
    return 0;
  }



  public String
  getHeaderField(int n)
  {
    return null;
  }



  public String
  getHeaderField(String name)
  {
    return null;
  }



  public long
  getHeaderFieldDate(String name, long def)
  {
    return def;
  }



  public int
  getHeaderFieldInt(String name, int def)
  {
    return def;
  }



  public String
  getHeaderFieldKey(int n)
  {
    return null;
  }



  public Map
  getHeaderFields()
  {
    return new HashMap();
  }



  public InputStream
  getInputStream() throws IOException
  {
    return new URLInputStream(getURL());
  }



  public long
  getLastModified()
  {
    return -1;
  }



  public OutputStream
  getOutputStream() throws IOException
  {
    return new URLOutputStream(getURL());
  }



  public Map
  getRequestProperties()
  {
    return new HashMap();
  }



  public String
  getRequestProperty(String key)
  {
    return null;
  }



  public boolean
  getUseCaches()
  {
    return false;
  }



  public void
  setRequestProperty(String key, String value)
  {
  }

} // Connection
