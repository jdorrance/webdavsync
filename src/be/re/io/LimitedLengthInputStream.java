package be.re.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;



public class LimitedLengthInputStream extends FilterInputStream

{

  private long	bytesRead = 0;
  private long	length;



  public
  LimitedLengthInputStream(InputStream in, long length)
  {
    super(in);
    this.length = length;
  }



  public int
  available() throws IOException
  {
    return (int) Math.min((long) super.available(), length - bytesRead);
  }



  public int
  read() throws IOException
  {
    byte[]	b = new byte[1];

    return read(b, 0, b.length) == -1 ? -1 : ((int) 255 & b[0]);
  }



  public int
  read(byte[] b, int off, int len) throws IOException
  {
    if (bytesRead >= length)
    {
      return -1;
    }

    int	count = in.read(b, off, Math.min(len, (int) (length - bytesRead)));

    bytesRead += count;

    return
      count == -1 ?
        -1 : (int) (bytesRead < length ? count : (count - bytesRead + length));
  }



  public long
  skip(long n) throws IOException
  {
    long	result = in.skip(Math.min(n, length - bytesRead));

    bytesRead += result;

    return result;
  }

} // LimitedLengthInputStream
