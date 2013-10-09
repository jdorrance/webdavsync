package be.re.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;



/**
 * This stream passes an arbitrary byte stream to the underlying stream in a
 * Base64 encoded form. See also RFC 2045 section 6.8.
 * @author Werner Donn\u00e9
 */

public class Base64OutputStream extends FilterOutputStream

{

  private static byte[]	alphabet =
    {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
        'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
        'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
        'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
        '5', '6', '7', '8', '9', '+', '/'
    };

  private byte[]	buffer = new byte[3]; // One encoding quantum.
  private boolean	closeUnderlying;
  private int		lineSize = 0;
  private boolean	oneLine;
  private int		position = 0;



  public
  Base64OutputStream(OutputStream out)
  {
    this(out, false, true);
  }



  public
  Base64OutputStream(OutputStream out, boolean oneLine)
  {
    this(out, oneLine, true);
    this.oneLine = oneLine;
  }



  public
  Base64OutputStream(OutputStream out, boolean oneLine, boolean closeUnderlying)
  {
    super(out);
    this.oneLine = oneLine;
    this.closeUnderlying = closeUnderlying;
  }



  private int
  checkLine(byte[] b, int off)
  {
    if (++lineSize == 76)
    {
      lineSize = 0;
      b[off] = '\r';
      b[off + 1] = '\n';

      return 2;
    }

    return 0;
  }



  public void
  close() throws IOException
  {
    if (position > 0)
    {
      byte[]	output = new byte[6];
      int	len = encode(output, 0);

      out.write(output, 0, len);
    }

    out.flush();

    if (closeUnderlying)
    {
      out.close();
    }
  }



  private int
  encode(byte[] b, int off)
  {
    int	i = off;

    b[i++] = alphabet[(buffer[0] >> 2) & 0x3f];

    if (!oneLine)
    {
      i += checkLine(b, i);
    }

    b[i++] = alphabet[((buffer[0] << 4) & 0x3f) | ((buffer[1] >> 4) & 0x0f)];

    if (!oneLine)
    {
      i += checkLine(b, i);
    }

    // Only at the end of the stream the position can be left elsewhere than
    // at the start of the buffer. In that case padding must added.

    b[i++] =
      position > 1 ?
        alphabet[((buffer[1] << 2) & 0x3f) | ((buffer[2] >> 6) & 0x03)] :
        (byte) '=';

    if (!oneLine)
    {
      i += checkLine(b, i);
    }

    b[i++] = position > 2 ? alphabet[buffer[2] & 0x3f] : (byte) '=';

    if (!oneLine)
    {
      i += checkLine(b, i);
    }

    for (int j = 0; j < buffer.length; buffer[j++] = 0);
    position = 0;

    return i - off;
  }



  public void
  flush() throws IOException
  {
    super.flush();
  }



  public void
  write(int b) throws IOException
  {
    write(new byte[] {(byte) b}, 0, 1);
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
    int		j = 0;
    byte[]	output = new byte[Math.max(len * 2, 6)];
      // Reserve enough room for either the encoded result, which is 33
      // percent larger than the input, or the size of one encoded quantum plus
      // one possible \r\n combination, which is the least that can be written.

    for (int i = off; i < off + len && i < b.length; ++i)
    {
      buffer[position++] = b[i];

      if (position == buffer.length)
      {
        j += encode(output, j);
      }
    }

    out.write(output, 0, j);
  }

} // Base64OutputStream
