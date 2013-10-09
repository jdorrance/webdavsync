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

public class DevNullEventWriter implements XMLEventWriter

{

  public void
  add(XMLEvent event) throws XMLStreamException
  {
  }



  public void
  add(XMLEventReader reader) throws XMLStreamException
  {
    while(reader.hasNext())
    {
      add(reader.nextEvent());
    }
  }



  public void
  close() throws XMLStreamException
  {
  }



  public void
  flush() throws XMLStreamException
  {
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return null;
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return null;
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
  }

} // DevNullEventWriter
