package be.re.xml.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;



/**
 * Sends all consumed events to an XMLEventWriter.
 * @author Werner Donn\u00e9
 */

public class EventReaderMonitor extends EventReaderDelegate

{

  private XMLEventWriter	writer;



  public
  EventReaderMonitor(XMLEventWriter writer)
  {
    this(writer, null);
  }



  public
  EventReaderMonitor(XMLEventWriter writer, XMLEventReader reader)
  {
    super(reader);
    this.writer = writer;
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    XMLEvent	event = getParent().nextEvent();

    writer.add(event);

    return event;
  }

} // EventReaderMonitor
