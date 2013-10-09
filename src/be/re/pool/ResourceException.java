package be.re.pool;

import java.io.PrintStream;
import java.io.PrintWriter;



/**
 * This exception class supports proper exception chaining, i.e. it doesn't
 * mask the stack trace.
 * @author Werner Donn\u00e9
 */

public class ResourceException extends Exception

{

  private Throwable	e;



  public
  ResourceException()
  {
  }



  public
  ResourceException(String message)
  {
    super(message);
  }



  public
  ResourceException(Throwable e)
  {
    this.e = e != null ? e : new Exception("null");
  }



  public Throwable
  getException()
  {
    return e;
  }



  public String
  getLocalizedMessage()
  {
    return e != null ? e.getLocalizedMessage() : super.getLocalizedMessage();
  }



  public String
  getMessage()
  {
    return e != null ? e.getMessage() : super.getMessage();
  }



  public void
  printStackTrace()
  {
    if (e != null)
    {
      e.printStackTrace();
    }
    else
    {
      super.printStackTrace();
    }
  }



  public void
  printStackTrace(PrintStream s)
  {
    if (e != null)
    {
      e.printStackTrace(s);
    }
    else
    {
      super.printStackTrace(s);
    }
  }



  public void
  printStackTrace(PrintWriter s)
  {
    if (e != null)
    {
      e.printStackTrace(s);
    }
    else
    {
      super.printStackTrace(s);
    }
  }



  public String
  toString()
  {
    return e != null ? e.toString() : super.toString();
  }

} // ResourceException
