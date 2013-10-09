package be.re.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



/**
 * A stream with a writing and a reading end. It is meant for situations where
 * chunks of data are written and read in one thread, which avoids the
 * accumulation of everything in one byte array stream. Neither side blocks,
 * which means the writer should stay ahead of the reader in order to avoid a
 * premature end of stream for the reader.
 * @author Werner Donn\u00e9
 */

public class QueueIOStream

{

  private byte[]	buffer;
  private boolean	closed;
  private InputStream	in = new In();
  private OutputStream	out = new Out();
  private int		readPosition;
  private int		writePosition;



  /**
   * Always returns the same internal InputStream.
   */

  public InputStream
  getInputStream()
  {
    return in;
  }



  /**
   * Always returns the same internal OutputStream.
   */

  public OutputStream
  getOutputStream()
  {
    return out;
  }



  public class In extends InputStream

  {

    public int
    available() throws IOException
    {
      return writePosition - readPosition;
    }



    public void
    close() throws IOException
    {
      closed = true;
    }



    public int
    read() throws IOException
    {
      return
        readPosition == writePosition ?
          -1 : ((int) 255 & buffer[readPosition++]);
    }



    public int
    read(byte b[]) throws IOException
    {
      return read(b, 0, b.length);
    }



    public int
    read(byte b[], int off, int len) throws IOException
    {
      int	i;

      for
      (
        i = 0;
        i < len && readPosition < writePosition && off + i < b.length;
        ++i, ++readPosition
      )
      {
        b[off + i] = buffer[readPosition];
      }

      return i == 0 ? -1 : i;
    }

  } // In



  public class Out extends OutputStream

  {

    public void
    close() throws IOException
    {
      closed = true;
    }



    public void
    flush() throws IOException
    {
    }



    public void
    write(byte[] b) throws IOException
    {
      write(b, 0, b.length);
    }



    public void
    write(byte[] b, int off, int len) throws IOException
    {
      if (closed)
      {
        throw new IOException("The stream is closed.");
      }

      if (buffer == null || writePosition + len >= buffer.length)
      {
        byte[]	newBuffer =
          new byte[Math.max(2 * (writePosition - readPosition + len), 0x10000)];
          // Avoid it to grow by one byte once it is over 64K.

        if (buffer == null)
        {
          writePosition = 0;
        }
        else
        {
          System.arraycopy
          (
            buffer,
            readPosition,
            newBuffer,
            0,
            writePosition - readPosition
          );

          writePosition -= readPosition;
        }

        buffer = newBuffer;
        readPosition = 0;
      }

      for (int i = 0; i < len && off + i < b.length; ++i, ++writePosition)
      {
        buffer[writePosition] = b[off + i];
      }
    }



    public void
    write(int b) throws IOException
    {
      write(new byte[]{(byte) b});
    }

  } // Out

} // QueueIOStream
