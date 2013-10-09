package be.re.io;

import be.re.io.PipedInputStream;
import be.re.io.PipedOutputStream;
import be.re.net.FTPClient;
import be.re.net.JNDIInitialContext;
import be.re.net.ProxyManager;
import be.re.net.URLManager;
import be.re.net.URLManagerJMS;
import be.re.webdav.Client;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Properties;
import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.NamingException;



/**
 * This is a convenience class for updating URLs. Its supports the schemes
 * <code>file</code>, <code>ftp</code>, <code>jar</code>, <code>jms</code>,
 * <code>http</code> and <code>https</code>.
 * @author Werner Donn\u00e9
 */

public class URLOutputStream extends OutputStream

{

  private ProtocolHandler[]	handlers;
  private OutputStream		out;



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  URLOutputStream(URL url) throws IOException, ProtocolException
  {
    this(url, null, null, null);
  }



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  URLOutputStream(URL url, String username, String password)
    throws IOException, ProtocolException
  {
    this(url, username, password, null);
  }



  /**
   * Opens the URL for writing.
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
  URLOutputStream(URL url, String username, String password, String onBehalfOf)
    throws IOException, ProtocolException
  {
    handlers =
      new ProtocolHandler[]
      {
        new FileHandler("file"),
        new FtpHandler("ftp", username, password),
        new JarHandler("jar"),
        new JmsHandler("jms"),
        new WebDAVHandler("http", username, password, onBehalfOf),
        new WebDAVHandler("https", username, password, onBehalfOf)
      };

    out = getProtocolHandler(url).getOutputStream(url);
  }



  public void
  close() throws IOException, ProtocolException
  {
    out.close();
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
  flush() throws IOException
  {
    out.flush();
  }



  public void
  write(int b) throws IOException
  {
    out.write(b);
  }



  public void
  write(byte[] b) throws IOException
  {
    out.write(b);
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
    out.write(b, off, len);
  }



  private interface ProtocolHandler

  {

    public OutputStream	getOutputStream	(URL url)
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



    public OutputStream
    getOutputStream(URL url) throws IOException, ProtocolException
    {
      try
      {
        return new FileOutputStream(be.re.net.Util.urlToFile(url));
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



    public OutputStream
    getOutputStream(URL url) throws IOException, ProtocolException
    {
      return
        FTPClient.connect(url.toString(), username, password).
          store(be.re.net.Util.getFTPFilename(url));
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



    private
    JarHandler(String name)
    {
      this.name = name;
    }



    public OutputStream
    getOutputStream(URL url) throws IOException, ProtocolException
    {
      final String	entry =
        be.re.net.Util.
          unescapeUriSpecials(be.re.net.Util.extractComposedUrlEntry(url));
      final File	zipFile =
        be.re.net.Util.urlToFile(be.re.net.Util.extractSubUrl(url));
      final File	tmpFile = Util.createTempFile("zip.", ".tmp");

      try
      {
        return
          new FileOutputStream(tmpFile)
          {
            public void
            close() throws IOException
            {
              try
              {
                super.close();

                be.re.util.Zip.put
                (
                  zipFile,
                  new String[]{entry},
                  new URL[]{be.re.net.Util.fileToUrl(tmpFile)},
                  false
                );
              }

              finally
              {
                tmpFile.delete();
              }
            }
          };
      }

      catch (IOException e)
      {
        tmpFile.delete();
        throw e;
      }

      catch (Exception e)
      {
        tmpFile.delete();
        throw new RuntimeException(e);
      }
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



    public OutputStream
    getOutputStream(URL url) throws IOException, ProtocolException
    {
      URLManagerJMS.checkUrl(url, false);

      if (URLManagerJMS.isMessage(url))
      {
        throw
          new MalformedURLException
          (
            MessageFormat.format
            (
              Util.getResource("jms_property_error"),
              new Object[] {"JMSMessageID"}
            )
          );
      }

      QueueConnection	connection = null;
      QueueSession	session = null;
  
      try
      {
        connection = URLManagerJMS.connect(url);
        session = URLManagerJMS.createSession(connection);
  
        BytesMessage	message = session.createBytesMessage();
        QueueSender	sender =
          session.createSender(URLManagerJMS.getQueue(url));

        setProperties
        (
          url,
          sender,
          message,
          URLManagerJMS.getUrlProperties(url)
        );

        return new MessageOutputStream(connection, session, sender, message);
      }
  
      catch (Exception e)
      {
        be.re.util.Util.printStackTrace(e);

        try
        {
          URLManagerJMS.cleanUp(connection, session, false);
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



    private void
    setProperties
    (
      URL		url,
      QueueSender	sender,
      Message		message,
      Properties	properties
    ) throws JMSException, MalformedURLException, NamingException
    {
      Enumeration	enumer = properties.keys();

      while (enumer.hasMoreElements())
      {
        String	key = (String) enumer.nextElement();

        if (key.equals("JMSMessageID") || key.equals("selector"))
        {
          throw
            new MalformedURLException
            (
              MessageFormat.format
              (
                Util.getResource("jms_property_error"),
                new Object[] {key}
              )
            );
        }

        if (key.equals("JMSCorrelationID"))
        {
          message.setJMSCorrelationID(properties.getProperty(key));
        }
        else
        {
           if (key.equals("JMSDeliveryMode"))
           {
             sender.setDeliveryMode
             (
               "NON_PERSISTENT".equals(properties.getProperty(key)) ?
                 DeliveryMode.NON_PERSISTENT : DeliveryMode.PERSISTENT
             );
           }
           else
           {
             if (key.equals("JMSTimeToLive"))
             {
               sender.setTimeToLive
               (
                 new Long(properties.getProperty(key)).longValue()
               );
             }
             else
             {
               if (key.equals("JMSPriority"))
               {
                 sender.setPriority
                 (
                   new Integer(properties.getProperty(key)).intValue()
                 );
               }
               else
               {
                 if (key.equals("JMSReplyTo"))
                 {
                   setReplyTo(url, message, properties.getProperty(key));
                 }
                 else
                 {
                   if (key.equals("JMSType"))
                   {
                     message.setJMSType(properties.getProperty(key));
                   }
                   else
                   {
                     message.
                       setStringProperty(key, properties.getProperty(key));
                   }
                 }
               }
             }
           }
        }
      }
    }



    private void
    setReplyTo(URL url, Message message, String destination)
      throws JMSException, MalformedURLException, NamingException
    {
      Context	context = null;
      boolean	isDirty = false;

      try
      {
        context = JNDIInitialContext.get(URLManagerJMS.getProviderUrl(url));
        message.setJMSReplyTo((Queue) context.lookup(destination));
      }

      catch (NamingException e)
      {
        isDirty = true;
        throw e;
      }

      finally
      {
        if (context != null)
        {
          JNDIInitialContext.release(context, isDirty);
        }
      }
    }



    private class MessageOutputStream extends OutputStream

    {

      private boolean		cleanedUp = false;
      private QueueConnection	connection;
      private boolean		isDirty = false;
      private BytesMessage	message;
      private QueueSender	sender;
      private QueueSession	session;



      private
      MessageOutputStream
      (
        QueueConnection	connection, 
        QueueSession	session,
        QueueSender	sender,
        BytesMessage	message
      )
      {
        this.connection = connection;
        this.session = session;
        this.sender = sender;
        this.message = message;
      }



      public void
      close() throws IOException
      {
        try
        {
          if (!isDirty)
          {
            sender.send(message);
          }
        }

        catch (JMSException e)
        {
          be.re.util.Util.printStackTrace(e);
          isDirty = true;
          throw new IOException(e.getMessage());
        }

        finally
        {
          try
          {
            URLManagerJMS.cleanUp(connection, session, isDirty);
          }

          catch (Exception e)
          {
            throw new IOException(e.getMessage());
          }

          cleanedUp = true;
        }
      }


 
      protected void
      finalize() throws Throwable
      {
        if (!cleanedUp)
        {
          URLManagerJMS.cleanUp(connection, session, isDirty);
        }
      }



      public void
      write(byte[] b) throws IOException
      {
        try
        {
          message.writeBytes(b);
        }

        catch (JMSException e)
        {
          isDirty = true;
          throw new IOException(e.getMessage());
        }
      }



      public void
      write(byte[] b, int off, int len) throws IOException
      {
        try
        {
          message.writeBytes(b, off, len);
        }

        catch (JMSException e)
        {
          isDirty = true;
          throw new IOException(e.getMessage());
        }
      }



      public void
      write(int b) throws IOException
      {
        try
        {
          message.writeByte((byte) b);
        }

        catch (JMSException e)
        {
          isDirty = true;
          throw new IOException(e.getMessage());
        }
      }

    } // MessageOutputStream

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



    public OutputStream
    getOutputStream(final URL url) throws IOException, ProtocolException
    {
      PipedOutputStream		out = new PipedOutputStream();
      final PipedInputStream	in = new PipedInputStream(out);

      new Thread
      (
        new Runnable()
        {
          public void
          run()
          {
            try
            {
              Client		client = new Client();
              URLManager	manager = new URLManager();

              if (!manager.exists(url))
              {
                manager.create(url, false);
              }

              Client.Response	response =
                client.put
                (
                  url,
                  in,
                  client.getSimpleLiveProperty(url, "getcontenttype"),
                  null
                );

              be.re.webdav.cmd.Util.report(url, response);
            }

            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      ).start();

      return out;
    }



    public String
    getName()
    {
      return name;
    }

  } // WebDAVHandler

} // URLOutputStream
