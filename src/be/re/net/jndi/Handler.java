package be.re.net.jndi;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;



public class Handler extends URLStreamHandler

{

  protected URLConnection
  openConnection(URL url) throws IOException
  {
    throw new IOException("The JNDI handler doesn't support I/O");
  }

} // Handler
