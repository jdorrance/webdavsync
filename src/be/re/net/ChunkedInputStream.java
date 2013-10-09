package be.re.net;

import be.re.io.ReadLineInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;



public class ChunkedInputStream extends FilterInputStream

{

  private byte[]	chunk = null;
  private int		chunkSize;
  private boolean	empty = false;
  private int		offset;
  private Headers	trailer;



  public
  ChunkedInputStream(InputStream in)
  {
    this(in, null);
  }



  public
  ChunkedInputStream(InputStream in, Headers trailer)
  {
    super
    (
      in instanceof ReadLineInputStream ?
        (ReadLineInputStream) in : new ReadLineInputStream(in)
    );

    this.trailer = trailer;
  }



  public int
  available() throws IOException
  {
    return 0; // Can't be predicted because of the chunk size fields.
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

    return read(b, 0, 1) == -1 ? -1 : ((int) 255 & b[0]);
  }



  public int
  read(byte[] b, int off, int len) throws IOException
  {
    if (empty || (chunk == null && !readChunk()))
    {
      empty = true;
      return -1;
    }

    System.
      arraycopy(chunk, offset, b, off, Math.min(len, chunkSize - offset));

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
    byte[]	line = ((ReadLineInputStream) in).readLine();

    if (line == null)
    {
      return false;
    }

    chunkSize =
      Integer.
        parseInt(new StringTokenizer(new String(line), " ;").nextToken(), 16);

    if (chunkSize == 0)
    {
      if (trailer != null)
      {
        trailer.add(Util.readHeaders((ReadLineInputStream) in));
      }

      ((ReadLineInputStream) in).readLine(); // Closing empty line.

      return false;
    }

    chunk = new byte[chunkSize];
    offset = 0;

    int	allRead = 0;
    int	sizeRead;

    for
    (
      allRead = sizeRead = super.read(chunk, 0, chunkSize);
      sizeRead != -1 && allRead < chunkSize;
      sizeRead = super.read(chunk, allRead, chunkSize - allRead),
        allRead += sizeRead
    );

    if (allRead < chunkSize)
    {
      throw new IOException(Util.getResource("chunk_corrupt"));
    }

    ((ReadLineInputStream) in).readLine();

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

} // ChunkedInputStream
