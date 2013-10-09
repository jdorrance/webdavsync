package be.re.net;

import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;



public class FTPBlockModeInputStream extends FilterInputStream

{

  private byte[]	chunk = null;
  private int		chunkSize;
  private boolean	empty = false;
  private boolean	last = false;
  private int		offset;
  private boolean	propagateClose;



  public
  FTPBlockModeInputStream(InputStream in)
  {
    this(in, false);
  }



  public
  FTPBlockModeInputStream(InputStream in, boolean propagateClose)
  {
    super(new DataInputStream(in));
    this.propagateClose = propagateClose;
  }



  public int
  available() throws IOException
  {
    return 0; // Can't be predicted because of the chunk size fields.
  }



  public void
  close() throws IOException
  {
    if (propagateClose)
    {
      super.close();
    }
    else
    {
      chunk = null;
      empty = false;
      last = false;
    }
  }



  public void
  mark(int readLimit)
  {
  }



  public boolean
  markSupported()
  {
    return false;
  }



  public int
  read() throws IOException
  {
    byte[]	b = new byte[1];
    int		result = read(b, 0, 1);

    return result == -1 ? -1 : b[0];
  }



  public int
  read(byte[] b, int off, int len) throws IOException
  {
    if (empty || (chunk == null && !readChunk()))
    {
      empty = true;
      return -1;
    }

    System.arraycopy(chunk, offset, b, off, Math.min(len, chunkSize - offset));

    if (chunkSize - offset > len)
    {
      offset += len;
      return len;
    }

    int	first = chunkSize - offset;

    chunk = null;

    int	second = read(b, off + first, len - first);

    if (second == -1)
    {
      empty = true;
      return first;
    }

    return first + second;
  }



  private boolean
  readChunk() throws IOException
  {
    if (last)
    {
      return false;
    }

    last = (((DataInputStream) in).readByte() & 0x40) > 0;
    chunkSize = ((DataInputStream) in).readUnsignedShort();

    if (last && chunkSize == 0)
    {
      return false;
    }

    chunk = new byte[chunkSize];
    offset = 0;
    ((DataInputStream) in).readFully(chunk);

    return true;
  }



  public void
  reset() throws IOException
  {
  }



  public long
  skip(long n) throws IOException
  {
    return 0; // See the available method.
  }

} // FTPBlockModeInputStream
