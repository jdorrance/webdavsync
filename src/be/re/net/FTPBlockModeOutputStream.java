package be.re.net;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;



public class FTPBlockModeOutputStream extends FilterOutputStream

{

  private static final short	BLOCK_SIZE = Short.MAX_VALUE;
  private boolean		propagateClose;



  public
  FTPBlockModeOutputStream(OutputStream out)
  {
    this(out, false);
  }



  public
  FTPBlockModeOutputStream(OutputStream out, boolean propagateClose)
  {
    super(new DataOutputStream(out));
    this.propagateClose = propagateClose;
  }



  public void
  close() throws IOException
  {
    ((DataOutputStream) out).writeByte(0x40);
    ((DataOutputStream) out).writeShort(0);

    if (propagateClose)
    {
      out.close();
    }
  }



  public void
  write(int b) throws IOException
  {
    write(new byte[] {(byte) b}, 0, 1);
  }



  public void
  write(byte[] b, int off, int len) throws IOException
  {
    ((DataOutputStream) out).writeByte(0);
    ((DataOutputStream) out).writeShort(Math.min(len, BLOCK_SIZE));
    out.write(b, off, Math.min(len, BLOCK_SIZE));

    if (len > BLOCK_SIZE)
    {
      write(b, off + BLOCK_SIZE, len - BLOCK_SIZE);
    }
  }

} // FTPBlockModeOutputStream
