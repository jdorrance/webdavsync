package be.re.xml.stax;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Doesn't let the document events through, which enables inlining of
 * documents. It also removes all events around the document events.
 * @author Werner Donn\u00e9
 */

public class GobbleDocumentEventsWriter extends EventWriterDelegate

{

  private boolean	documentOpen = false;



  public
  GobbleDocumentEventsWriter()
  {
  }



  public
  GobbleDocumentEventsWriter(XMLEventWriter writer)
  {
    super(writer);
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    if (!documentOpen && event.isStartElement())
    {
      documentOpen = true;
    }
    else
    {
      if (event.isEndDocument())
      {
        documentOpen = false;
      }
    }

    if (documentOpen)
    {
      super.add(event);
    }
  }

} // GobbleDocumentEventsWriter
