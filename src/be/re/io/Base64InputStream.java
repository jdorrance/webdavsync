package be.re.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;



/**
 * See also RFC 2045 section 6.8.
 * @author Werner Donn\u00e9
 */

public class Base64InputStream extends FilterInputStream

{

  byte[]		decodedBuffer = new byte[3];
    // The room for one decoded quantum.
  private byte[]	encodedBuffer = null;
  private int		encodedPosition = 0;
  private int		length = 0;
  private int		quantumPosition = 0;
  private static byte[]	values = createValues();



  public
  Base64InputStream(InputStream in)
  {
    super(in);
  }



  public int
  available() throws IOException
  {
    return 0;
      // This can't be predicted because non-alphabet bytes must be ignored and
      // we can't know up front how many there will be of them.
  }



  private static byte[]
  createValues()
  {
    byte	j = 0;
    byte[]	result = new byte[256];

    Arrays.fill(result, (byte) -1);

    for (int i = 'A'; i <= 'Z'; ++i, ++j)
    {
      result[i] = j;
    }

    for (int i = 'a'; i <= 'z'; ++i, ++j)
    {
      result[i] = j;
    }

    for (int i = '0'; i <= '9'; ++i, ++j)
    {
      result[i] = j;
    }

    result['+'] = j++;
    result['/'] = j;

    return result;
  }



  private boolean
  prepareBuffer(int len) throws IOException
  {
    if (length == -1)
    {
      return false;
    }

    if (encodedBuffer == null || encodedPosition == length)
    {
      encodedBuffer = new byte[Math.max(2 * len, 4)];
        // Four input bytes become three bytes, so this is large enough to hold
        // enough input for decoding.
      encodedPosition = 0;
      length = in.read(encodedBuffer);

      return length != -1;
    }

    return true;
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
    if (!prepareBuffer(len))
    {
      return -1;
    }

    int	i = 0;

    for (; encodedPosition < length && i < len; ++encodedPosition)
    {
      if
      (
        values[encodedBuffer[encodedPosition]] != -1 	||
        encodedBuffer[encodedPosition] == '='
      )
      {
        switch (quantumPosition)
        {
          case 0:
            decodedBuffer[0] =
              (byte) (values[encodedBuffer[encodedPosition]] << 2);
            break;

          case 1:
            decodedBuffer[0] |=
              (values[encodedBuffer[encodedPosition]] >> 4) & 0x03;
            decodedBuffer[1] =
              (byte) (values[encodedBuffer[encodedPosition]] << 4);
            b[i++ + off] = decodedBuffer[0];
            break;

          case 2:
            if (encodedBuffer[encodedPosition] != '=')
            {
              decodedBuffer[1] |=
                (values[encodedBuffer[encodedPosition]] >> 2) & 0x0f;
              decodedBuffer[2] =
                (byte) (values[encodedBuffer[encodedPosition]] << 6);
              b[i++ + off] = decodedBuffer[1];
            }

            break;

          case 3:
            if (encodedBuffer[encodedPosition] != '=')
            {
              decodedBuffer[2] |= values[encodedBuffer[encodedPosition]];
              b[i++ + off] = decodedBuffer[2];
            }

            break;
        }

        if (++quantumPosition == 4)
        {
          Arrays.fill(decodedBuffer, (byte) 0);
          quantumPosition = 0;
        }
      }
    }

    if (i < len)
    {
      int	first = i;

      i = read(b, off + first, len - first);

      return i == -1 ? (first == 0 ? -1 : first) : first + i;
    }

    return len;
  }



  public long
  skip(long n)
  {
    return 0; // See available method.
  }

} // Base64InputStream
