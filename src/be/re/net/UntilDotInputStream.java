package be.re.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * The stream ends when a line containing only a dot is reached. It is
 * stateless.
 * @author Werner Donn\u00e9
 */

public class UntilDotInputStream extends FilterInputStream

{

  private boolean	dotSeen = false;
  private boolean	end = false;
  private boolean	newLineSeen = false;
  private boolean	propagateClose;



  public
  UntilDotInputStream(InputStream in)
  {
    this(in, true);
  }



  /**
   * @param propagateClose if set to <code>false</code> the underlying reader
   * is not closed when the <code>close</code> method is called.
   */

  public
  UntilDotInputStream(InputStream in, boolean propagateClose)
  {
    super(in);
    this.propagateClose = propagateClose;
  }



  public void
  close() throws IOException
  {
    if (propagateClose)
    {
      super.close();
    }
  }



  public void
  mark(int readAheadLimit)
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

    return read(b, 0, b.length) == -1 ? -1 : b[0];
  }



  public int
  read(byte[] b, int off, int len) throws IOException
  {
    if (end)
    {
      return -1;
    }

    int	i;

    for (i = off; i < len && !end; ++i)
    {
      int	c = in.read();

      if (c != -1)
      {
        switch (c)
        {
          case '.':
            if (newLineSeen)
            {
              dotSeen = true;
              newLineSeen = false;
            }
            else
            {
              dotSeen = false;
            }
            break;

          case '\r': break; // Is optional.

          case '\n':
            if (dotSeen)
            {
              end = true;
            }
            else
            {
              newLineSeen = true;
            }
            break;

          default:
            dotSeen = false;
            newLineSeen = false;
            break;
        }

        b[i] = (byte) c;
      }
      else
      {
        --i;
        end = true;
      }
    }

    return i - off;
  }



  public void
  reset() throws IOException
  {
  }



  public long
  skip(long n) throws IOException
  {
    return 0; // Don't allow to go beyond the ".".
  }

} // UntilDotInputStream
