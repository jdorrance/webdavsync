package be.re.io;

import java.io.IOException;
import java.io.OutputStream;



/**
 * This output stream gobbles all output.
 * @author Werner Donn\u00e9
 */

public class DevNullOutputStream extends OutputStream

{

  public void
  close() throws IOException
  {
  }



  public void
  flush() throws IOException
  {
  }



  public void
  write(byte[] b) throws IOException
  {
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
  }



  public void
  write(int b) throws IOException
  {
  }

} // DevNullOutputStream
