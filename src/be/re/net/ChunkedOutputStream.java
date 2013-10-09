package be.re.net;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;



public class ChunkedOutputStream extends FilterOutputStream

{

  public
  ChunkedOutputStream(OutputStream out)
  {
    super(out);
  }



  public void
  close() throws IOException
  {
    out.write("0\r\n".getBytes());
    super.close();
  }



  public void
  write(int b) throws IOException
  {
    write(new byte[] {(byte) b}, 0, 1);
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
    out.write((Integer.toString(len, 16) + "\r\n").getBytes());
    out.write(b, off, len);
    out.write("\r\n".getBytes());
  }

} // ChunkedOutputStream
