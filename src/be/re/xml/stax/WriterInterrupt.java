package be.re.xml.stax;

import javax.xml.stream.XMLStreamException;



/**
 * Can be used to indicate a wanted interruption of the writer.
 * @author Werner Donn\u00e9
 */

public class WriterInterrupt extends XMLStreamException

{

  public
  WriterInterrupt(Exception e)
  {
    super(e);
  }



  public
  WriterInterrupt(String message)
  {
    super(message);
  }



  public
  WriterInterrupt(String message, Exception e)
  {
    super(message, e);
  }

} // WriterInterrupt
