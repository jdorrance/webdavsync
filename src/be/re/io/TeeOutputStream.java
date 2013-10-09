package be.re.io;

import java.io.IOException;
import java.io.OutputStream;



/**
 * Replicates the output to several OutputStreams in the order given in the
 * constructor.
 * @author Werner Donn\u00e9
 */

public class TeeOutputStream extends OutputStream

{

  private OutputStream[]	out;



  public
  TeeOutputStream(OutputStream[] out)
  {
    this.out = out;
  }



  public void
  close() throws IOException
  {
    for (int i = 0; i < out.length; ++i)
    {
      out[i].close();
    }
  }



  public void
  flush() throws IOException
  {
    for (int i = 0; i < out.length; ++i)
    {
      out[i].flush();
    }
  }



  public OutputStream[]
  getOutputStreams()
  {
    return out;
  }



  public void
  write(int b) throws IOException
  {
    for (int i = 0; i < out.length; ++i)
    {
      out[i].write(b);
    }
  }



  public void
  write(byte[] b) throws IOException
  {
    for (int i = 0; i < out.length; ++i)
    {
      out[i].write(b);
    }
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
    for (int i = 0; i < out.length; ++i)
    {
      out[i].write(b, off, len);
    }
  }

} // TeeOutputStream
