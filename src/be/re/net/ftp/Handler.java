package be.re.net.ftp;

import be.re.net.ProxyManager;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;



public class Handler extends URLStreamHandler

{

  protected URLConnection
  openConnection(URL url) throws IOException
  {
    URL	proxy = ProxyManager.getProxy(url.getHost(), url.getProtocol());

    return
      proxy != null && proxy.getProtocol().equals("http") ?
        (URLConnection) new be.re.net.http.Connection(url) :
        (URLConnection) new Connection(url);
  }

} // Handler
