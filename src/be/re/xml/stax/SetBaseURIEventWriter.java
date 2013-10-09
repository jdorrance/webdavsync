package be.re.xml.stax;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Sets the xml:base attribute on the first element it encounters if
 * <code>baseURI</code> is not <code>null</code>.
 * @author Werner Donn\u00e9
 */

public class SetBaseURIEventWriter extends EventWriterDelegate

{

  private String	baseURI;
  private boolean	firstSeen;



  public
  SetBaseURIEventWriter(String baseURI)
  {
    this(baseURI, null);
  }



  public
  SetBaseURIEventWriter(String baseURI, XMLEventWriter writer)
  {
    super(writer);
    this.baseURI = baseURI;
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    if (event.isStartElement() && !firstSeen)
    {
      firstSeen = true;

      super.add
      (
        baseURI != null ?
          Util.setAttribute
          (
            event.asStartElement(),
            Util.eventFactory.createAttribute
            (
              XMLConstants.XML_NS_PREFIX, 
              XMLConstants.XML_NS_URI, 
              "base", 
              baseURI
            )
          ) : event
      );
    }
    else
    {
      super.add(event);
    }
  }

} // SetBaseURIEventWriter
