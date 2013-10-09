package be.re.xml.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;



/**
 * Replicates the output to additional writers.
 * @author Werner Donn\u00e9
 */

public class Tee extends EventWriterDelegate

{

  private XMLEventWriter[]	writers;



  public
  Tee(XMLEventWriter[] writers)
  {
    this(writers, null);
  }



  public
  Tee(XMLEventWriter[] writers, XMLEventWriter writer)
  {
    super(writer);
    this.writers = writers;
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    super.add(event);

    for (int i = 0; i < writers.length; ++i)
    {
      writers[i].add(event);
    }
  }



  public void
  close() throws XMLStreamException
  {
    super.close();

    for (int i = 0; i < writers.length; ++i)
    {
      writers[i].close();
    }
  }



  public void
  flush() throws XMLStreamException
  {
    super.flush();

    for (int i = 0; i < writers.length; ++i)
    {
      writers[i].flush();
    }
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    super.setDefaultNamespace(uri);

    for (int i = 0; i < writers.length; ++i)
    {
      writers[i].setDefaultNamespace(uri);
    }
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    super.setNamespaceContext(context);

    for (int i = 0; i < writers.length; ++i)
    {
      writers[i].setNamespaceContext(context);
    }
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    super.setPrefix(prefix, uri);

    for (int i = 0; i < writers.length; ++i)
    {
      writers[i].setPrefix(prefix, uri);
    }
  }

} // Tee
