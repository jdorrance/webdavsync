package be.re.xml.stax;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Sets the xml:base attribute on the first element it encounters if
 * <code>baseURI</code> is not <code>null</code>.
 * @author Werner Donn\u00e9
 */

public class SetBaseURIEventReader extends EventReaderDelegateBase

{

  private String	baseURI;
  private boolean	firstSeen;
  private XMLEvent	peeked;



  public
  SetBaseURIEventReader(String baseURI)
  {
    this(baseURI, null);
  }



  public
  SetBaseURIEventReader(String baseURI, XMLEventReader reader)
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
    if (event.isStartElement() && !firstSeen)
    {
      firstSeen = true;

      if (baseURI != null)
      {
        return
          Util.setAttribute
          (
            event.asStartElement(),
            new QName
            (
              XMLConstants.XML_NS_URI,
              "base",
              XMLConstants.XML_NS_PREFIX
            ),
            baseURI
          );
      }
    }

    return event;
  }

} // SetBaseURIEventReader
