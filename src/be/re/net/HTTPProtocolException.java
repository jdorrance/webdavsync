package be.re.net;

public class HTTPProtocolException extends ProtocolException

{

  private byte[]	data = new byte[0];



  public
  HTTPProtocolException()
  {
    super(-1, null);
  }



  public
  HTTPProtocolException(String message)
  {
    super(-1, message);
  }



  public
  HTTPProtocolException(int code, String message)
  {
    super(code, message);
  }



  public
  HTTPProtocolException(int code, String message, byte[] data)
  {
    super(code, message);
    this.data = data;
  }



  public byte[]
  getData()
  {
    return data;
  }

} // HTTPProtocolException
