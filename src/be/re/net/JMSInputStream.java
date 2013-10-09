package be.re.net;

import be.re.io.DevNullInputStream;
import be.re.io.OnException;
import be.re.io.OnExceptionInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;



public class JMSInputStream extends InputStream

{

  private boolean		cleanedUp = false;
  private QueueConnection	connection;
  private InputStream		in;
  private boolean		isDirty = false;
  private QueueSession		session;



  /**
   * The connection and session are cleaned up and released when the stream is
   * closed or when an exception occurs.
   */

  public
  JMSInputStream
  (
    Message		message,
    QueueConnection	connection,
    QueueSession	session
  ) throws IOException
  {
    in = new OnExceptionInputStream(selectInputStream(message));

    ((OnExceptionInputStream) in).setOnException
    (
      new OnException()
      {
        public void
        handle(Throwable e)
        {
          isDirty = true;

          try
          {
            close();
          }

          catch (IOException ex)
          {
            throw new RuntimeException(ex);
          }
        }
      }
    );

    this.connection = connection;
    this.session = session;
  }



  public int
  available() throws IOException
  {
    return in.available();
  }



  public void
  close() throws IOException
  {
    if (!cleanedUp)
    {
      try
      {
        URLManagerJMS.cleanUp(connection, session, isDirty);
        cleanedUp = true;
      }

      catch (Exception e)
      {
        throw new IOException(e.getMessage());
      }
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
  mark(int readLimit)
  {
    in.mark(readLimit);
  }



  public boolean
  markSupported()
  {
    return in.markSupported();
  }



  private static byte[]
  objectToBytes(Object object) throws IOException
  {
    ByteArrayOutputStream	bytes = new ByteArrayOutputStream();
    ObjectOutputStream		out = new ObjectOutputStream(bytes);

    out.writeObject(object);
    out.close();

    return bytes.toByteArray();
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



  private InputStream
  selectInputStream(Message message) throws IOException
  {
    try
    {
      String	text;

      return
        message instanceof BytesMessage ||
          message instanceof StreamMessage ?
          new ByteInputStream(message) :
          (
            message instanceof TextMessage ?
              (
                (text = ((TextMessage) message).getText()) == null ?
                  (InputStream) new DevNullInputStream() :
                  (InputStream) new ByteArrayInputStream(text.getBytes())
              ) :
              (
                message instanceof ObjectMessage ?
                  (InputStream) new ByteArrayInputStream
                  (
                    objectToBytes(((ObjectMessage) message).getObject())
                  ) : (InputStream) new DevNullInputStream()
              )
          );
    }

    catch (JMSException e)
    {
      close();
      throw new IOException(e.getMessage());
    }
  }



  public long
  skip(long n) throws IOException
  {
    return in.skip(n);
  }



  private class ByteInputStream extends InputStream

  {

    private Message	message;
    private Method	readArray;



    private
    ByteInputStream(Message message) throws IOException
    {
      this.message = message;

      try
      {
        readArray =
          message.getClass().
            getMethod("readBytes", new Class[] {byte[].class});
      }

      catch (NoSuchMethodException e)
      {
        throw new IOException(e.getMessage());
      }
    }



    public int
    read() throws IOException
    {
      byte[]	b = new byte[1];

      return read(b) == -1 ? -1 : ((int) 255 & b[0]);
    }



    public int
    read(byte[] b) throws IOException
    {
      try
      {
        return
          ((Integer) readArray.invoke(message, new Object[] {b})).
            intValue();
      }

      catch (IllegalAccessException e)
      {
        throw new IOException(e.getMessage());
      }

      catch (InvocationTargetException e)
      {
        if (e.getTargetException() != null)
        {
          throw new IOException(e.getTargetException().getMessage());
        }

        throw new IOException(e.getMessage());
      }
    }



    public int
    read(byte[] b, int off, int len) throws IOException
    {
      byte[]	buffer = new byte[len];
      int	result = read(buffer);

      System.arraycopy(buffer, 0, b, off, len);

      return result;
    }

  } // ByteInputStream

} // JMSInputStream
