package be.re.xml.sax;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



public class DevNullErrorHandler implements org.xml.sax.ErrorHandler

{

  public void
  error(SAXParseException exception) throws SAXException
  {
  }



  public void
  fatalError(SAXParseException exception) throws SAXException
  {
  }



  private String
  getMessage(SAXParseException e)
  {
    return null;
  }



  public void
  warning(SAXParseException exception) throws SAXException
  {
  }

} // DevNullErrorHandler
