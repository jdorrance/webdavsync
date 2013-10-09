package be.re.net;

/**
 * An exception to indicate the user has aborted the operation in order to
 * avoid a retry.
 * @author Werner Donn\u00e9
 */

public class AbortException extends RuntimeException

{

  public
  AbortException()
  {
  }



  public
  AbortException(String message)
  {
    super(message);
  }



  public
  AbortException(String message, Throwable cause)
  {
    super(message, cause);
  }



  public
  AbortException(Throwable cause)
  {
    super(cause);
  }

} // AbortException
