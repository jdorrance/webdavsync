package be.re.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;



/**
 * This class handles URLs with of the form
 * jms:<factory_url>!/<queue_name>[/properties], where factory_url is of the
 * form <jndi_provider_url>/<factory_name>.
 * @author Werner Donn\u00e9
 */

public class URLManagerJMS implements URLManager.ProtocolHandler

{

  private final static int	FACTORY = 1;
  private final static int	PROVIDER_URL = 0;
  private final static int	QUEUE = 2;



  private static Message
  browseMessage
  (
    URL			url,
    QueueConnection	connection,
    QueueSession	session,
    Queue		queue
  ) throws JMSException, MalformedURLException
  {
    Enumeration	enumer =
      session.createBrowser
      (
        queue,
        "JMSMessageID='" + getUrlProperties(url).getProperty("JMSMessageID") +
          "'"
      ).getEnumeration();

    return enumer.hasMoreElements() ? (Message) enumer.nextElement() : null;
  }



  public boolean
  canCopy(URL[] source, URL destination) throws IOException, ProtocolException
  {
    if (!"jms".equals(destination.getProtocol()))
    {
      return false;
    }

    for (int i = 0; i < source.length; ++i)
    {
      if (!"jms".equals(source[i].getProtocol()))
      {
        return false;
      }
    }

    return true;
  }



  public boolean
  canCopy(URL source, URL destination) throws IOException, ProtocolException
  {
    return
      "jms".equals(source.getProtocol()) &&
        "jms".equals(destination.getProtocol());
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
  
  
  
  public static void
  checkUrl(URL url, boolean isMessage) throws MalformedURLException
  {
    if (!Util.isComposedUrl(url))
    {
      throw new MalformedURLException(Util.getResource("jms_url_error"));
    }

    if (isMessage && !isMessage(url))
    {
      throw new MalformedURLException(Util.getResource("jms_msg_error"));
    }
  }



  public static void
  cleanUp(Connection connection, Session session, boolean isDirty)
    throws ProtocolException
  {
    Exception	exception = null;

    if (session != null)
    {
      try
      {
        session.close();
      }

      catch (JMSException e)
      {
        exception = e;
      }
    }

    if (connection != null)
    {
      try
      {
        JMSConnection.release(connection, isDirty || exception != null);
      }

      catch (JMSException e)
      {
        throw new ProtocolException(e);
      }
    }

    if (exception != null)
    {
      throw new ProtocolException(exception);
    }
  }



  public static QueueConnection
  connect(URL url) throws JMSException, MalformedURLException, NamingException
  {
    String[]	connectionElements = extractConnectionElements(url);

    return
      JMSConnection.getQueueConnection
      (
        "jndi:" + connectionElements[PROVIDER_URL] + "!" +
          connectionElements[FACTORY]
      );
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
    QueueConnection	connection = null;
    boolean		dirty = false;
    QueueSession	session = null;

    try
    {
      connection = connect(destination);
      session = createSession(connection);

      QueueSender	sender = session.createSender(getQueue(destination));

      for (int i = 0; i < source.length; ++i)
      {
        if (resume != null)
        {
          try
          {
            copy(source[i], session, sender);
          }

          catch (Exception e)
          {
            resume.handle(source[i], e);
            dirty = true;
          }
        }
        else
        {
          copy(source[i], session, sender);
        }
      }
    }

    catch (Exception e)
    {
      dirty = true;

      if (e instanceof ProtocolException)
      {
        throw (ProtocolException) e;
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }

    finally
    {
      cleanUp(connection, session, dirty);
    }
  }



  public boolean
  copy(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    copy(new URL[]{source}, destination, overwrite, null);

    return true; // Errors will throw exceptions.
  }



  private static void
  copy(URL source, QueueSession destination, QueueSender sender)
    throws
      JMSException, MalformedURLException, NamingException, ProtocolException
  {
    QueueConnection	connection = null;
    boolean		dirty = false;
    QueueSession	session = null;

    try
    {
      connection = connect(source);
      session = createSession(connection);

      Message	result =
        copy
        (
          (Message) session.createBrowser
          (
            getQueue(source),
            "JMSMessageID='" +
              getUrlProperties(source).getProperty("JMSMessageID") + "'"
          ).getEnumeration().nextElement(),
          destination
        );

      if (result != null)
      {
        sender.send(result);
      }
    }

    catch (JMSException e)
    {
      dirty = true;
      throw e;
    }

    finally
    {
      cleanUp(connection, session, dirty);
    }
  }



  private static Message
  copy(Message message, QueueSession session)
    throws JMSException
  {
    return
      copyHeadersAndProperties
      (
        message,
        message instanceof BytesMessage ?
          copyBody((BytesMessage) message, session.createBytesMessage()) :
          (
            message instanceof MapMessage ?
              copyBody((MapMessage) message, session.createMapMessage()) : 
              (
                message instanceof ObjectMessage ?
                  session.
                    createObjectMessage(((ObjectMessage) message).getObject()) :
                  (
                    message instanceof StreamMessage ?
                      copyBody
                      (
                        (StreamMessage) message,
                        session.createStreamMessage()
                      ) :
                      (
                        message instanceof TextMessage ?
                          session.createTextMessage
                          (
                            ((TextMessage) message).getText()
                          ) : null
                      )
                  )
              )
           )
        );
  }



  private static Message
  copyBody(BytesMessage source, BytesMessage target) throws JMSException
  {
    byte[]	buffer = new byte[0x10000];
    int		len;

    while ((len = source.readBytes(buffer)) != -1)
    {
      target.writeBytes(buffer, 0, len);
    }

    return target;
  }



  private static Message
  copyBody(MapMessage source, MapMessage target) throws JMSException
  {
    Enumeration	enumer = source.getMapNames();

    while (enumer.hasMoreElements())
    {
      String	name = (String) enumer.nextElement();

      target.setObject(name, source.getObject(name));
    }

    return target;
  }



  private static Message
  copyBody(StreamMessage source, StreamMessage target) throws JMSException
  {
    byte[]	buffer = new byte[0x10000];
    int		len;

    while ((len = source.readBytes(buffer)) != -1)
    {
      target.writeBytes(buffer, 0, len);
    }

    return target;
  }



  private static Message
  copyHeadersAndProperties(Message source, Message target) throws JMSException
  {
    if (source.getJMSCorrelationID() != null)
    {
      target.setJMSCorrelationID(source.getJMSCorrelationID());
    }

    target.setJMSDeliveryMode(source.getJMSDeliveryMode());
    target.setJMSExpiration(source.getJMSExpiration());
    target.setJMSPriority(source.getJMSPriority());
    target.setJMSType(source.getJMSType());

    if (source.getJMSReplyTo() != null)
    {
      target.setJMSReplyTo(source.getJMSReplyTo());
    }

    Enumeration	enumer = source.getPropertyNames();

    while (enumer.hasMoreElements())
    {
      String	name = (String) enumer.nextElement();

      target.setObjectProperty(name, source.getObjectProperty(name));
    }

    return target;
  }



  public URL
  create(URL url, boolean container) throws IOException, ProtocolException
  {
    throw new ProtocolException(Util.getResource("jms_create_error"));
  }



  public static QueueSession
  createSession(QueueConnection connection) throws JMSException
  {
    return connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
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
    checkUrl(url, true);

    QueueConnection	connection = null;
    boolean		isDirty = false;
    QueueSession	session = null;

    try
    {
      Properties	properties = getUrlProperties(url);

      connection = connect(url);
      session = createSession(connection);

      session.createReceiver
      (
        getQueue(url),
        "JMSMessageID='" + properties.getProperty("JMSMessageID") + "'"
      ).receive();
    }

    catch (Exception e)
    {
      isDirty = true;
      throw new ProtocolException(e);
    }

    catch (Throwable e)
    {
      isDirty = true;
      throw new RuntimeException(e);
    }

    finally
    {
      cleanUp(connection, session, isDirty);
    }
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
    checkUrl(url, false);

    QueueConnection	connection = null;
    boolean		isDirty = false;
    QueueSession	session = null;

    try
    {
      Queue	queue = getQueue(url);

      connection = connect(url);

      return
        connection != null && queue != null &&
          (
            !isMessage(url) ||
              browseMessage
              (
                url,
                connection,
                session = createSession(connection),
                queue
              ) != null
          );
    }

    catch (Throwable e)
    {
      isDirty = true;
      return false;
    }

    finally
    {
      cleanUp(connection, session, isDirty);
    }
  }



  private static String[]
  extractConnectionElements(URL url) throws MalformedURLException
  {
    String[]	result = new String[3];
    URL		factoryUrl = Util.extractSubUrl(url);

    result[PROVIDER_URL] =
      factoryUrl.getProtocol() + "://" + factoryUrl.getAuthority();
    result[FACTORY] = Util.unescapeUriSpecials(factoryUrl.getPath());
    result[QUEUE] =
      Util.unescapeUriSpecials
      (
        Util.extractComposedUrlEntry(stripUrlProperties(url))
      );

    return result;
  }



  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    checkUrl(url, false);

    QueueConnection	connection = null;
    boolean		isDirty = false;
    QueueSession	session = null;

    try
    {
      connection = connect(url);
      session = createSession(connection);

      Enumeration	enumer =
        session.createBrowser
        (
          getQueue(url),
          getUrlProperties(url).getProperty("selector")
        ).getEnumeration();
      List		result = new ArrayList();

      while (enumer.hasMoreElements())
      {
        result.add(getUrl(url, (Message) enumer.nextElement()));
      }

      return (URL[]) result.toArray(new URL[result.size()]);
    }

    catch (Exception e)
    {
      isDirty = true;
      throw new ProtocolException(e);
    }

    catch (Throwable e)
    {
      isDirty = true;
      throw new RuntimeException(e);
    }

    finally
    {
      cleanUp(connection, session, isDirty);
    }
  }



  public URLManager.Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    checkUrl(url, false);

    QueueConnection	connection = null;
    boolean		isDirty = false;
    QueueSession	session = null;

    try
    {
      connection = connect(url);
      session = createSession(connection);

      Enumeration	enumer =
        session.createBrowser
        (
          getQueue(url),
          getUrlProperties(url).getProperty("selector")
        ).getEnumeration();
      List		result = new ArrayList();

      while (enumer.hasMoreElements())
      {
        result.add(getMessageProperties((Message) enumer.nextElement(), url));
      }

      return
        (URLManager.Property[][])
          result.toArray(new URLManager.Property[result.size()][]);
    }

    catch (Exception e)
    {
      isDirty = true;
      throw new ProtocolException(e);
    }

    catch (Throwable e)
    {
      isDirty = true;
      throw new RuntimeException(e);
    }

    finally
    {
      cleanUp(connection, session, isDirty);
    }
  }



  public static String
  getFactory(URL url) throws MalformedURLException
  {
    return extractConnectionElements(url)[FACTORY];
  }



  private static String
  getFreeProperties(Message message) throws JMSException
  {
    Enumeration	enumer = message.getPropertyNames();
    SortedMap	properties = new TreeMap();

    while (enumer.hasMoreElements())
    {
      String    name = (String) enumer.nextElement();
      Object	value = message.getObjectProperty(name);

      properties.put(name, value == null ? "" : value.toString());
    }

    String	result = "";
    Iterator	iter = properties.keySet().iterator();

    while (iter.hasNext())
    {
      String	key = (String) iter.next();

      result +=
        (result.equals("") ? "" : ";") + key + "=" +
          (String) properties.get(key);
    }

    return result;
  }



  private static URLManager.Property[]
  getMessageProperties(Message message, URL url)
    throws IOException, JMSException
  {
    URL		messageUrl = getUrl(url, message);
    List	result = new ArrayList();
    Object	value = message.getJMSMessageID();

    result.add
    (
      new IDProperty
      (
        "JMSMessageID",
        value == null ? "" : value.toString(),
        true,
        messageUrl
      )
    );

    value = message.getJMSCorrelationID();

    result.add
    (
      new URLManager.Property
      (
        "JMSCorrelationID",
        value == null ? "" : value.toString(),
        messageUrl
      )
    );

    result.add
    (
      new URLManager.Property
      (
        "JMSTimestamp",
        new Date(message.getJMSTimestamp()),
        messageUrl
      )
    );

    result.add
    (
      new URLManager.Property
      (
        "JMSExpiration",
        new Date(message.getJMSExpiration()),
        messageUrl
      )
    );

    result.add
    (
      new URLManager.Property
      (
        "JMSPriority",
        new Integer(message.getJMSPriority()),
        messageUrl
      )
    );

    value = message.getJMSReplyTo();

    result.add
    (
      new URLManager.Property
      (
        "JMSReplyTo",
        value == null ? "" : value.toString(),
        messageUrl
      )
    );

    int	deliveryMode = message.getJMSDeliveryMode();

    result.add
    (
      new URLManager.Property
      (
        "JMSDeliveryMode",
        deliveryMode == DeliveryMode.PERSISTENT ?
          "PERSISTENT" :
          (deliveryMode == DeliveryMode.NON_PERSISTENT ? "NON_PERSISTENT" : ""),
        messageUrl
      )
    );

    result.add
    (
      new URLManager.Property
      (
        "JMSRedelivered",
        new Boolean(message.getJMSRedelivered()),
        messageUrl
      )
    );

    value = message.getJMSType();

    result.add
    (
      new URLManager.Property
      (
        "JMSType",
        value == null ? "" : value.toString(),
        messageUrl
      )
    );

    result.add
    (
      new URLManager.Property
      (
        "properties",
        getFreeProperties(message),
        messageUrl
      )
    );

    return
      (URLManager.Property[])
        result.toArray(new URLManager.Property[result.size()]);
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    return new Properties();
  }



  public URLManager.Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    checkUrl(url, true);

    QueueConnection	connection = null;
    boolean		isDirty = false;
    QueueSession	session = null;

    try
    {
      connection = connect(url);
      session = createSession(connection);

      return
        getMessageProperties
        (
          browseMessage(url, connection, session, getQueue(url)),
          url
        );
    }

    catch (Exception e)
    {
      isDirty = true;
      throw new ProtocolException(e);
    }

    catch (Throwable e)
    {
      isDirty = true;
      throw new RuntimeException(e);
    }

    finally
    {
      cleanUp(connection, session, isDirty);
    }
  }



  public static String
  getProviderUrl(URL url) throws MalformedURLException
  {
    return extractConnectionElements(url)[PROVIDER_URL];
  }



  public static Queue
  getQueue(URL url) throws MalformedURLException, ProtocolException
  {
    Context	context = null;
    boolean	isDirty = false;

    try
    {
      context = JNDIInitialContext.get(getProviderUrl(url));

      return (Queue) context.lookup(getQueueName(url));
    }

    catch (NamingException e)
    {
      isDirty = true;
      throw new ProtocolException(e);
    }

    finally
    {
      if (context != null)
      {
        JNDIInitialContext.release(context, isDirty);
      }
    }
  }



  public static String
  getQueueName(URL url) throws MalformedURLException
  {
    return extractConnectionElements(url)[QUEUE];
  }



  private static URL
  getUrl(URL url, Message message) throws IOException, JMSException
  {
    return
      Util.escapedUrl
      (
        stripUrlProperties(url).toString() + "/JMSMessageID=" +
          message.getJMSMessageID()
      );
  }



  public static Properties
  getUrlProperties(URL url) throws MalformedURLException
  {
    Properties		properties = new Properties();

    if (!hasProperties(url))
    {
      return properties;
    }

    StringTokenizer	tokenizer =
      new StringTokenizer
      (
        Util.unescapeUriSpecials(Util.getLastPathSegment(url)),
        ";"
      );
    int			count = tokenizer.countTokens();

    while (tokenizer.hasMoreTokens())
    {
      String	token = tokenizer.nextToken();

      if (token.indexOf('=') == -1)
      {
        throw
          new MalformedURLException
          (
            MessageFormat.format(Util.getResource("jms_property_error"),
            new Object[] {token})
          );
      }

      properties.setProperty
      (
        token.substring(0, token.indexOf('=')).trim(),
        token.substring(token.indexOf('=') + 1).trim()
      );
    }

    return properties;
  }



  public static boolean
  hasProperties(URL url)
  {
    return Util.getLastPathSegment(url).indexOf('=') != -1;
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    checkUrl(url, false);

    Properties	properties = getUrlProperties(url);

    return
      !isMessage(url) &&
        (
          (
            properties.size() == 1 &&
              properties.getProperty("selector") != null
          ) || properties.size() == 0
        ) && exists(url);
  }



  public static boolean
  isMessage(URL url) throws MalformedURLException
  {
    Properties	properties = getUrlProperties(url);

    return
      properties.size() == 1 && properties.getProperty("JMSMessageID") != null;
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
    throw new ProtocolException(Util.getResource("jms_move_error"));
  }



  private static URL
  stripUrlProperties(URL url) throws MalformedURLException
  {
    String	s = url.toString();

    return
      hasProperties(url) ?
        new URL(s.substring(0, s.lastIndexOf('/', s.length() - 2))) : url;
  }



  public boolean
  useTrashBin(URL url)
  {
    return true;
  }



  public static class IDProperty extends URLManager.Property

  {

    public
    IDProperty(String name, Object value, boolean identifier, URL url)
    {
      super(name, value, identifier, url);
    }



    public String
    getIdentifier()
    {
      return "JMSMessageID=" + (value == null ? "" : value.toString());
    }



    public String
    getIdentifier(String s)
    {
      return "JMSMessageID=" + s;
    }

  } // IDProperty

} // URLManagerJMS
