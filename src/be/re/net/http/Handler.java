package be.re.net.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;



public class Handler extends URLStreamHandler

{

  protected int
  getDefaultPort()
  {
    return 80;
  }



  protected URLConnection
  openConnection(URL url) throws IOException
  {
    return new Connection(url);
  }

} // Handler
