package be.re.xml.stax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;



/**
 * Wraps an EventReaderDelegate in a EventWriterDelegate for using existing
 * filters in an output chain.
 * @author Werner Donn\u00e9
 */

public class EventReaderEventWriterDelegate extends EventWriterDelegate

{

  private List			buffer = new ArrayList();
  private Map			entityDeclarations = new HashMap();
  private EventReaderDelegate	reader;



  public
  EventReaderEventWriterDelegate(EventReaderDelegate reader)
  {
    this(reader, null);
  }



  public
  EventReaderEventWriterDelegate
  (
    EventReaderDelegate	reader,
    XMLEventWriter	writer
  )
  {
    super(writer);
    this.reader = reader;

    EventReaderDelegate	r;

    for
    (
      r = reader;
      r.getParent() != null && r.getParent() instanceof EventReaderDelegate;
      r = (EventReaderDelegate) r.getParent()
    );

    r.setParent(new BufferReader());
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    if
    (
      event.getEventType() == XMLStreamConstants.ENTITY_DECLARATION	&&
      ((EntityDeclaration) event).getReplacementText() != null
    )
    {
      entityDeclarations.put
      (
        ((EntityDeclaration) event).getName(),
        ((EntityDeclaration) event).getReplacementText()
      );
    }

    buffer.add(event);
    forward();
  }



  public void
  close() throws XMLStreamException
  {
    forward();
    reader.close();
    super.close();
  }



  public void
  flush() throws XMLStreamException
  {
    forward();
    super.flush();
  }



  private void
  forward() throws XMLStreamException
  {
    while (reader.hasNext())
    {
      getParent().add(reader.nextEvent());
    }
  }



  private class BufferReader implements XMLEventReader

  {

    public void
    close() throws XMLStreamException
    {
    }



    public String
    getElementText() throws XMLStreamException
    {
      return
        Util.getElementText(this, (XMLEvent) buffer.get(0), entityDeclarations);
    }



    public Object
    getProperty(String name)
    {
      throw new IllegalArgumentException();
    }



    public boolean
    hasNext()
    {
      return buffer.size() > 0;
    }



    public Object
    next()
    {
      return next();
    }



    public XMLEvent
    nextEvent() throws XMLStreamException
    {
      if (!hasNext())
      {
        throw new NoSuchElementException();
      }

      return (XMLEvent) buffer.remove(0);
    }



    public XMLEvent
    nextTag() throws XMLStreamException
    {
      return Util.nextTag(this);
    }



    public XMLEvent
    peek() throws XMLStreamException
    {
      return hasNext() ? (XMLEvent) buffer.get(0) : null;
    }



    public void
    remove()
    {
    }

  } // BufferReader

} // EventReaderEventWriterDelegate
