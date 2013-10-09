package be.re.util;

import be.re.io.Base64InputStream;
import be.re.io.Base64OutputStream;
import be.re.io.StreamConnector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;



public class Base64

{

  public static byte[]
  decode(byte[] b)
  {
    return decode(b, 0, b.length);
  }



  public static byte[]
  decode(byte[] b, int off, int len)
  {
    try
    {
      ByteArrayInputStream	in = new ByteArrayInputStream(b, off, len);
      ByteArrayOutputStream	out = new ByteArrayOutputStream();

      StreamConnector.copy(new Base64InputStream(in), out);

      return out.toByteArray();
    }

    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }



  public static String
  decode(String s, String encoding)
  {
    try
    {
      return new String(decode(s.getBytes("ASCII")), encoding);
    }

    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }



  public static byte[]
  encode(byte[] b)
  {
    return encode(b, 0, b.length);
  }



  public static byte[]
  encode(byte[] b, boolean oneLine)
  {
    return encode(b, 0, b.length, oneLine);
  }



  public static byte[]
  encode(byte[] b, int off, int len)
  {
    return encode(b, off, len, false);
  }



  public static byte[]
  encode(byte[] b, int off, int len, boolean oneLine)
  {
    try
    {
      ByteArrayInputStream	in = new ByteArrayInputStream(b, off, len);
      ByteArrayOutputStream	out = new ByteArrayOutputStream();

      StreamConnector.copy(in, new Base64OutputStream(out, oneLine));

      return out.toByteArray();
    }

    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }



  public static String
  encode(String s, String encoding)
  {
    return encode(s, encoding, false);
  }



  public static String
  encode(String s, String encoding, boolean oneLine)
  {
    try
    {
      return new String(encode(s.getBytes(encoding), oneLine), "ASCII");
    }

    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

} // Base64
