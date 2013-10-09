package be.re.io;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;



/**
 * Buffers data until '\r', '\n' or "\r\n" are written, after which it flushes
 * the line. Intermediate flushes are possible.
 * @author Werner Donn\u00e9
 */

public class LineBufferedOutputStream extends FilterOutputStream

{

  private boolean	carriageReturnSeen = false;



  public
  LineBufferedOutputStream(OutputStream out)
  {
    super(new BufferedOutputStream(out));
  }



  public void
  write(int b) throws IOException
  {
    write(new byte[] {(byte) b}, 0, 1);
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
    out.write(b, off, len);

    for (int i = 0; i < len; ++i)
    {
      switch (b[off + i])
      {
        case '\n':
          carriageReturnSeen = false;
          flush();
          break;

        case '\r':
          if (carriageReturnSeen)
          {
            flush();
          }

          break;

        default:
          if (carriageReturnSeen)
          {
            flush();
          }

          carriageReturnSeen = false;
          break;
      }
    }
  }

} // LineBufferedOutputStream
