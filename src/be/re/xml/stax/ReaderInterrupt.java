package be.re.xml.stax;

import javax.xml.stream.XMLStreamException;



/**
 * Can be used to distinguish a parsing error and a wanted interruption of the
 * reader.
 * @author Werner Donn\u00e9
 */

public class ReaderInterrupt extends XMLStreamException

{

  public
  ReaderInterrupt(Exception e)
  {
    super(e);
  }



  public
  ReaderInterrupt(String message)
  {
    super(message);
  }



  public
  ReaderInterrupt(String message, Exception e)
  {
    super(message, e);
  }

} // ReaderInterrupt
