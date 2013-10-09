package be.re.net;

import java.net.MalformedURLException;
import java.net.URL;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;



public class JMSConnection

{

  private final static SharedConnections	connections =
    new SharedConnections(new Adapter());



  private static String
  canonic(String url)
  {
    return 
      url.charAt(url.length() - 1) == '/' ? 
        url.substring(0, url.length() - 1) : url;
  }



  /**
   * @param factoryJNDIUrl it is an URL of the form
   * jndi:<provider_url>!/<name_path>.
   */

  public static synchronized QueueConnection
  getQueueConnection(String factoryJNDIUrl)
    throws JMSException, MalformedURLException, NamingException
  {
    try
    {
      return (QueueConnection) connections.get(canonic(factoryJNDIUrl));
    }

    catch (Exception e)
    {
      if (e instanceof JMSException)
      {
        throw (JMSException) e;
      }

      if (e instanceof MalformedURLException)
      {
        throw (MalformedURLException) e;
      }

      if (e instanceof NamingException)
      {
        throw (NamingException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public static synchronized void
  release(Connection connection, boolean isDirty) throws JMSException
  {
    connections.release(connection, isDirty);
  }



  private static class Adapter implements SharedConnections.Adapter

  {

    public void
    close(Object connection)
    {
      try
      {
        ((Connection) connection).close();
      }

      catch (JMSException e)
      {
        throw new RuntimeException(e);
      }
    }



    public Object
    open(Object key) throws Exception
    {
      if (!"jndi".equals(new URL((String) key).getProtocol()))
      {
        throw new MalformedURLException(Util.getResource("url_protocol_error"));
      }

      Context	context = null;
      boolean	isDirty = false;

      try
      {
        context =
          JNDIInitialContext.get(Util.extractSubUrl((String) key).toString());

        QueueConnection	connection =
          (
            (QueueConnectionFactory)
              context.lookup(Util.extractComposedUrlEntry((String) key))
          ).createQueueConnection();

        connection.start();

        return connection;
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

  } // Adapter

} // JMSConnection
