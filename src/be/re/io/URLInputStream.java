package be.re.io;

import be.re.net.FTPClient;
import be.re.net.JMSInputStream;
import be.re.net.ProxyManager;
import be.re.net.URLManagerJMS;
import be.re.webdav.Client;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream; // for bug 4351422
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;



/**
 * This is a convenience class for reading URLs. Its supports the schemes
 * <code>file</code>, <code>ftp</code>, <code>jar</code>, <code>jms</code>,
 * <code>http</code> and <code>https</code>.
 * @author Werner Donn\u00e9
 */

public class URLInputStream extends InputStream

{

  private ProtocolHandler[]	handlers;
  private InputStream		in;



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  URLInputStream(URL url) throws IOException, ProtocolException
  {
    this(url, null, null, null);
  }



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  URLInputStream(URL url, String username, String password)
    throws IOException, ProtocolException
  {
    this(url, username, password, null);
  }



  /**
   * Opens the URL for reading.
   * @param url the URL to be opened. It may contain a username and password in
   * its authority, which are overridden by the <code>username</code> and
   * <code>password</code> parameters if not equal to <code>null</code>.
   * @param username the username to be used for the connection. This overrides
   * information in the authority of the URL.
   * @param password the password to be used for the connection. This overrides
   * information in the authority of the URL.
   * @param onBehalfOf sets the HTTP extension header
   * <code>X-be.re.On-Behalf-Of</code> if not equal to <code>null</code>.
   * @see be.re.webdav.Client
   */

  public
  URLInputStream(URL url, String username, String password, String onBehalfOf)
    throws IOException, ProtocolException
  {
    handlers =
      new ProtocolHandler[]
      {
        new FileHandler("file"),
        new FtpHandler("ftp", username, password),
        new JarHandler("jar", username, password),
        new JmsHandler("jms"),
        new WebDAVHandler("http", username, password, onBehalfOf),
        new WebDAVHandler("https", username, password, onBehalfOf),
        new WebDAVHandler("feed", username, password, onBehalfOf)
      };

    in = getProtocolHandler(url).getInputStream(url);
  }



  public int
  available() throws IOException
  {
    return in.available();
  }



  public void
  close() throws IOException, ProtocolException
  {
    in.close();
  }



  private ProtocolHandler
  getProtocolHandler(URL url) throws ProtocolException
  {
    int	i;
    URL	proxy = ProxyManager.getProxy(url.getHost(), url.getProtocol());

    String	protocol =
      proxy != null ? proxy.getProtocol() : url.getProtocol();

    for
    (
      i = 0;
      i < handlers.length && !handlers[i].getName().equals(protocol);
      ++i
    );

    if (i == handlers.length)
    {
      throw
        new ProtocolException
        (
          MessageFormat.format
          (
            Util.getResource("unsupported_protocol_error"),
            new Object[] {protocol}
          )
        );
    }

    return handlers[i];
  }



  public void
  mark(int readLimit)
  {
    in.mark(readLimit);
  }



  public boolean
  markSupported()
  {
    return in.markSupported();
  }



  public int
  read() throws IOException
  {
    return in.read();
  }



  public int
  read(byte[] b) throws IOException
  {
    return in.read(b);
  }



  public int
  read(byte[] b, int off, int len) throws IOException
  {
    return in.read(b, off, len);
  }



  public void
  reset() throws IOException
  {
    in.reset();
  }



  public long
  skip(long n) throws IOException
  {
    return in.skip(n);
  }



  private interface ProtocolHandler

  {

    public InputStream	getInputStream	(URL url)
					  throws IOException, ProtocolException;
    public String	getName		();

  } // ProtocolHandler



  private class FileHandler implements ProtocolHandler

  {

    private String	name;



    private
    FileHandler(String name)
    {
      this.name = name;
    }



    public InputStream
    getInputStream(URL url) throws IOException, ProtocolException
    {
      try
      {
        return new FileInputStream(be.re.net.Util.urlToFile(url));
      }

      catch (FileNotFoundException e)
      {
        throw new ProtocolException(e.getMessage());
      }
    }



    public String
    getName()
    {
      return name;
    }

  } // FileHandler



  private class FtpHandler implements ProtocolHandler

  {

    private String	name;
    private String	password;
    private String	username;



    private
    FtpHandler(String name, String username, String password)
    {
      this.name = name;
      this.username = username;
      this.password = password;
    }



    public InputStream
    getInputStream(URL url) throws IOException, ProtocolException
    {
      return
        FTPClient.connect(url.toString(), username, password).
          retrieve(be.re.net.Util.getFTPFilename(url));
    }



    public String
    getName()
    {
      return name;
    }

  } // FtpHandler



  private class JarHandler implements ProtocolHandler

  {

    private String	name;
    private String	password;
    private String	username;



    private
    JarHandler(String name, String username, String password)
    {
      this.name = name;
      this.username = username;
      this.password = password;
    }



    public InputStream
    getInputStream(URL url) throws IOException, ProtocolException
    {
      String	entry =
        be.re.net.Util.
          unescapeUriSpecials(be.re.net.Util.extractComposedUrlEntry(url));

      if (entry == null)
      {
        return
          new URLInputStream
          (
            be.re.net.Util.extractSubUrl(url),
            username,
            password
          );
      }

      final ZipFile	zipFile =
        new ZipFile
        (
          be.re.net.Util.urlToFile(be.re.net.Util.extractSubUrl(url))
        );
      ZipEntry		e = zipFile.getEntry(entry);

      if (e == null)
      {
        throw
          new ProtocolException
          (
            MessageFormat.
              format(Util.getResource("jar_entry"), new Object[] {entry})
          );
      }

      //return zipFile.getInputStream(e);

      // Workaround for bug 4351422
      return
        new FilterInputStream(zipFile.getInputStream(e))
        {
          public void
          close() throws IOException
          {
            super.close();
            zipFile.close();
          }
        };
    }



    public String
    getName()
    {
      return name;
    }

  } // JarHandler



  private class JmsHandler implements ProtocolHandler

  {

    private String	name;



    private
    JmsHandler(String name)
    {
      this.name = name;
    }



    public InputStream
    getInputStream(URL url) throws IOException, ProtocolException
    {
      URLManagerJMS.checkUrl(url, true);

      QueueConnection	connection = null;
      QueueSession	session = null;
  
      try
      {
        connection = URLManagerJMS.connect(url);
        session = URLManagerJMS.createSession(connection);
  
        return
          new JMSInputStream
          (
            (Message) session.createBrowser
            (
              URLManagerJMS.getQueue(url),
              "JMSMessageID='" +
                URLManagerJMS.getUrlProperties(url).
                  getProperty("JMSMessageID") + "'"
            ).getEnumeration().nextElement(),
            connection,
            session
          );
      }
  
      catch (Exception e)
      {
        be.re.util.Util.printStackTrace(e);

        try
        {
          URLManagerJMS.cleanUp(connection, session, true);
        }

        catch (Exception ex)
        {
          // Give precedence to the original exception.
        }

        throw new ProtocolException(e.getMessage());
      }
    }



    public String
    getName()
    {
      return name;
    }

  } // JmsHandler



  private class WebDAVHandler implements ProtocolHandler

  {

    private Client	client;
    private String	name;



    private
    WebDAVHandler
    (
      String	name,
      String	username,
      String	password,
      String	onBehalfOf
    )
    {
      this.name = name;
      client = new Client(username, password, onBehalfOf);
    }



    public InputStream
    getInputStream(URL url) throws IOException, ProtocolException
    {
      Client.Response	response =
        client.get(url, new String[0], new String[0], null);

      if (response.getStatusCode() != 200)
      {
        be.re.webdav.Util.throwException(response);
      }

      return response.getBody();
    }



    public String
    getName()
    {
      return name;
    }

  } // WebDAVHandler

} // URLInputStream
