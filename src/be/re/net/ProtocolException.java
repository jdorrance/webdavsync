package be.re.net;

import java.io.IOException;
import java.net.URL;



/**
 * An exception for protocol-oriented problems.
 * @author Werner Donn\u00e9
 */

public class ProtocolException extends IOException

{

  private int	code = -1;
  private URL	url;



  public
  ProtocolException()
  {
    this(null, -1, null);
  }



  public
  ProtocolException(String message)
  {
    this(null, -1, message);
  }



  public
  ProtocolException(int code, String message)
  {
    this(null, code, message);
  }



  /**
   * @param url the resource that triggered the exception.
   * @param code an error code. This is used by protocol implementations such
   * as HTTP and FTP.
   * @param message the exception message.
   */

  public
  ProtocolException(URL url, int code, String message)
  {
    super(message);
    this.url = url;
    this.code = code;
  }



  public
  ProtocolException(Throwable cause)
  {
    super();
    initCause(cause);
  }



  /**
   * An error code. This is used by protocol implementations such as HTTP and
   * FTP. If there is no code the value -1 will be returned.
   */

  public int
  getCode()
  {
    return code;
  }



  /**
   * Returns the resource that triggered the exception.
   */

  public URL
  getUrl()
  {
    return url;
  }

} // ProtocolException
