package be.re.xml.stax;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;



/**
 * This class is a base class for filters. It makes sure that all event logic
 * goes through its <code>nextEvent</code> method. This way only this method
 * should be overridden.
 * @author Werner Donn\u00e9
 */

public class EventReaderDelegateBase extends EventReaderDelegate

{

  private XMLEvent	currentEvent;
  private Map		entityDeclarations = new HashMap();



  public
  EventReaderDelegateBase()
  {
  }



  public
  EventReaderDelegateBase(XMLEventReader reader)
  {
    super(reader);
  }



  public String
  getElementText() throws XMLStreamException
  {
    return Util.getElementText(this, currentEvent, entityDeclarations);
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    return Util.nextTag(this);
  }



  /**
   * A subclass should call this method in its <code>nextEvent</code>.
   */

  protected void
  setCurrentEvent(XMLEvent event)
  {
    currentEvent = event;

    if (event.getEventType() == XMLStreamConstants.ENTITY_DECLARATION)
    {
      entityDeclarations.put
      (
        ((EntityDeclaration) event).getName(),
        ((EntityDeclaration) event).getReplacementText()
      );
    }
  }

} // EventReaderDelegateBase
