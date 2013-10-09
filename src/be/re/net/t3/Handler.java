package be.re.net.t3;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;



public class Handler extends URLStreamHandler

{

  protected URLConnection
  openConnection(URL url) throws IOException
  {
    throw new IOException("The T3 handler doesn't support I/O");
  }

} // Handler
