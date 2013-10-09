package be.re.net;

/**
 * Make it possible to wrap a cause.
 * @author Werner Donn\u00e9
 */

public class MalformedURLException extends java.net.MalformedURLException

{

  public
  MalformedURLException()
  {
  }



  public
  MalformedURLException(String s)
  {
    super(s);
  }



  public
  MalformedURLException(String message, Throwable cause)
  {
    super(message);
    initCause(cause);
  }



  public
  MalformedURLException(Throwable cause)
  {
    super();
    initCause(cause);
  }

} // MalformedURLException
