package be.re.xml.stax;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Flushes each event immediately, which is useful for debugging filter chains
 * in which exceptions are thrown.
 * @author Werner Donn\u00e9
 */

public class FlushEventWriter extends EventWriterDelegate

{

  public
  FlushEventWriter()
  {
  }



  public
  FlushEventWriter(XMLEventWriter writer)
  {
    super(writer);
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    super.add(event);
    flush();
  }

} // FlushEventWriter
