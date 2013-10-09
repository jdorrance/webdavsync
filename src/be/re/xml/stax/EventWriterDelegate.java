package be.re.xml.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Writes nothing.
 * @author Werner Donn\u00e9
 */

public class EventWriterDelegate implements XMLEventWriter

{

  private XMLEventWriter	writer;



  public
  EventWriterDelegate()
  {
  }



  public
  EventWriterDelegate(XMLEventWriter writer)
  {
    setParent(writer);
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    writer.add(event);
  }



  public void
  add(XMLEventReader reader) throws XMLStreamException
  {
    while (reader.hasNext())
    {
      add(reader.nextEvent());
    }
  }



  public void
  close() throws XMLStreamException
  {
    writer.close();
  }



  public void
  flush() throws XMLStreamException
  {
    writer.flush();
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return writer.getNamespaceContext();
  }



  public XMLEventWriter
  getParent()
  {
    return writer;
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return writer.getPrefix(uri);
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    writer.setDefaultNamespace(uri);
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    writer.setNamespaceContext(context);
  }



  public void
  setParent(XMLEventWriter writer)
  {
    this.writer = writer;
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    writer.setPrefix(prefix, uri);
  }

} // EventWriterDelegate
