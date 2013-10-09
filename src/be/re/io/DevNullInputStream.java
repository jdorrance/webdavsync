package be.re.io;

import java.io.IOException;
import java.io.InputStream;



public class DevNullInputStream extends InputStream

{

  public int
  available() throws IOException
  {
    return 0;
  }



  public int
  read() throws IOException
  {
    return -1;
  }



  public int
  read(byte b[]) throws IOException
  {
    return -1;
  }



  public int
  read(byte b[], int off, int len) throws IOException
  {
    return -1;
  }

} // DevNullInputStream
