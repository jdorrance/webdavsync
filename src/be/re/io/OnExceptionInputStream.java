package be.re.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * This class implements the repetitiveness of handling a failure on a
 * communication line in a general way.
 * @author Werner Donne\u00e9
 */

public class OnExceptionInputStream extends FilterInputStream

{

  private OnException	onException;



  public
  OnExceptionInputStream(InputStream in)
  {
    this(in, null);
  }



  public
  OnExceptionInputStream(InputStream in, OnException onException)
  {
    super(in);
    this.onException = onException;
  }



  public int
  available() throws IOException
  {
    try
    {
      return in.available();
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public void
  close() throws IOException
  {
    try
    {
      in.close();
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public int
  read() throws IOException
  {
    try
    {
      return in.read();
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public int
  read(byte[] b) throws IOException
  {
    try
    {
      return in.read(b);
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public int
  read(byte[] b, int off, int len) throws IOException
  {
    try
    {
      return in.read(b, off, len);
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public void
  reset() throws IOException
  {
    try
    {
      in.reset();
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }



  public void
  setOnException(OnException onException)
  {
    this.onException = onException;
  }



  public long
  skip(long n) throws IOException
  {
    try
    {
      return in.skip(n);
    }

    catch (Throwable e)
    {
      if (onException != null)
      {
        onException.handle(e);
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }
  }

} // OnExceptionInputStream
