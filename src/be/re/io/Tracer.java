package be.re.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;



public class Tracer extends FilterOutputStream

{

  private static Tracer	defaultTracer = new Tracer(System.out);



  public
  Tracer(OutputStream out)
  {
    super(out);
  }



  public static Tracer
  getDefault()
  {
    return defaultTracer;
  }



  public static void
  setDefault(Tracer tracer)
  {
    defaultTracer = tracer;
  }



  public void
  write(int b) throws IOException
  {
    write(new byte[] {(byte) b}, 0, 1);
  }



  /**
   * Flushes at each call.
   */

  public synchronized void
  write(byte[] b, int off, int len) throws IOException
  {
    out.write(b, off, len);
    out.flush();
  }

} // Tracer
