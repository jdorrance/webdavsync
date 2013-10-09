package be.re.xml.stax;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;



/**
 * Removes the xml:base attribute if <code>baseURI</code> is not
 * <code>null</code>.
 * @author Werner Donn\u00e9
 */

public class RemoveBaseURIEventReader extends EventReaderDelegateBase

{

  private String	baseURI;
  private XMLEvent	peeked;



  public
  RemoveBaseURIEventReader(String baseURI)
  {
    this(baseURI, null);
  }



  public
  RemoveBaseURIEventReader(String baseURI, XMLEventReader reader)
  {
    super(reader);
    this.baseURI = baseURI;
  }



  public boolean
  hasNext()
  {
    return peeked != null || super.hasNext();
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    if (peeked != null)
    {
      XMLEvent	result = peeked;

      peeked = null;

      return result;
    }

    return process(super.nextEvent());
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    if (peeked == null)
    {
      peeked = process(super.nextEvent());
    }

    return peeked;
  }



  private XMLEvent
  process(XMLEvent event) throws XMLStreamException
  {
    if (baseURI != null && event.isStartElement())
    {
      Attribute	attribute =
        event.asStartElement().
          getAttributeByName(new QName(XMLConstants.XML_NS_URI, "base"));

      if (attribute != null && baseURI.equals(attribute.getValue()))
      {
        return
          Util.removeAttribute
          (
            event.asStartElement(),
            new QName(XMLConstants.XML_NS_URI, "base")
          );
      }
    }

    return event;
  }

} // RemoveBaseURIEventReader
