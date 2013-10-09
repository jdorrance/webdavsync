package be.re.xml.stax;

import be.re.xml.stax.EventReaderDelegateBase;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Wraps an EventWriterDelegate in a EventReaderDelegate for using existing
 * filters in an input chain.
 * @author Werner Donn\u00e9
 */

public class EventWriterEventReaderDelegate extends EventReaderDelegateBase

{

  private List			buffer = new ArrayList();
  private EventWriterDelegate	writer;



  public
  EventWriterEventReaderDelegate(EventWriterDelegate writer)
  {
    this(writer, null);
  }



  public
  EventWriterEventReaderDelegate
  (
    EventWriterDelegate	writer,
    XMLEventReader	reader
  )
  {
    super(reader);
    this.writer = writer;

    writer.setParent
    (
      new DevNullEventWriter()
      {
        public void
        add(XMLEvent event) throws XMLStreamException
        {
          buffer.add(event);
        }
      }
    );
  }



  public boolean
  hasNext()
  {
    try
    {
      return buffer.size() > 0 || readNext();
    }

    catch (XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    if (!hasNext())
    {
      throw new NoSuchElementException();
    }

    XMLEvent	event = (XMLEvent) buffer.remove(0);

    setCurrentEvent(event);

    return event;
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return !hasNext() ? null : (XMLEvent) buffer.get(0);
  }



  private boolean
  readNext() throws XMLStreamException
  {
    while (buffer.size() == 0)
    {
      if (!getParent().hasNext())
      {
        return false;
      }

      writer.add(getParent().nextEvent());
    }

    return true;
  }

} // EventWriterEventReaderDelegate
