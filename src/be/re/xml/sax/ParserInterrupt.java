package be.re.xml.sax;

import org.xml.sax.SAXException;



/**
 * Can be used to distinguish a parsing error and a wanted interruption of the
 * parser.
 * @author Werner Donn\u00e9
 */

public class ParserInterrupt extends SAXException

{

  public
  ParserInterrupt(Exception e)
  {
    super(e);
  }



  public
  ParserInterrupt(String message)
  {
    super(message);
  }



  public
  ParserInterrupt(String message, Exception e)
  {
    super(message, e);
  }

} // ParserInterrupt
