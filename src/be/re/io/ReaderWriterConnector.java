package be.re.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;



/**
 * This class reads data from reader and writes it to writer
 * in a separate thread. The thread stops when there is no more input
 * or when an exception occurs.
 * @author Werner Donn\u00e9
 */

public class ReaderWriterConnector

{

  private IOException	exception = null;
  private Thread	thread;



  public
  ReaderWriterConnector(Reader in, Writer out)
  {
    this(in, out, true, true);
  }



  public
  ReaderWriterConnector
  (
    final Reader	in,
    final Writer	out,
    final boolean	closeInput,
    final boolean	closeOutput
  )
  {
    this(in, out, 0x10000, closeInput, closeOutput);
  }



  public
  ReaderWriterConnector
  (
    final Reader	in,
    final Writer	out,
    final int		bufferSize,
    final boolean	closeInput,
    final boolean	closeOutput
  )
  {
    (
      thread =
        new Thread
        (
          new Runnable()
          {
            public void
            run()
            {
              try
              {
                copy(in, out, bufferSize, closeInput, closeOutput);
              }

              catch (IOException e)
              {
                exception = e;
              }
            }
          }
        )
    ).start();
  }



  public static void
  copy(Reader in, Writer out) throws IOException
  {
    copy(in, out, true, true);
  }



  public static void
  copy
  (
    Reader	in,
    Writer	out,
    boolean	closeInput,
    boolean	closeOutput
  ) throws IOException
  {
    copy(in, out, 0x10000, closeInput, closeOutput);
  }



  public static void
  copy
  (
    Reader	in,
    Writer	out,
    int		bufferSize,
    boolean	closeInput,
    boolean	closeOutput
  ) throws IOException
  {
    char[]	buffer = new char[bufferSize];
    int		len;

    while ((len = in.read(buffer)) != -1)
    {
      out.write(buffer, 0, len);
      out.flush();
    }

    if (closeInput)
    {
      in.close();
    }

    if (closeOutput)
    {
      out.close();
    }
    else
    {
      out.flush();
    }
  }



  /**
   * Breaks the connection. It doesn't touch the streams.
   */

  public void
  disconnect()
  {
    thread.interrupt();
  }



  /**
   * Joins with the thread implementing the io processing.
   */

  public void
  join() throws IOException, InterruptedException
  {
    thread.join();

    if (exception != null)
    {
      throw exception;
    }
  }

} // ReaderWriterConnector
