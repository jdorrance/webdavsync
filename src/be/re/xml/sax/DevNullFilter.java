package be.re.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;



/**
 * Blocks all ContentHandler events except the document events.
 * @author Werner Donn\u00e9
 */

public class DevNullFilter extends XMLFilterImpl

{

  public void
  characters(char[] ch, int start, int length) throws SAXException
  {
  }



  public void
  endElement(String namespaceURI, String localName, String qName)
    throws SAXException
  {
  }



  public void
  endPrefixMapping(String prefix) throws SAXException
  {
  }



  public void
  ignorableWhitespace(char[] ch, int start, int length) throws SAXException
  {
  }



  public void
  processingInstruction(String target, String data) throws SAXException
  {
  }



  public void
  skippedEntity(String name) throws SAXException
  {
  }



  public void
  startElement
  (
    String	namespaceURI,
    String	localName,
    String	qName,
    Attributes	atts
  ) throws SAXException
  {
  }



  public void
  startPrefixMapping(String prefix, String uri) throws SAXException
  {
  }

} // DevNullFilter
