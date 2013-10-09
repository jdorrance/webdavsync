package be.re.xml.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;



/**
 * An XMLEventWriter wrapper around an TransformerHandler.
 * @author Werner Donn\u00e9
 */

public class TransformerHandlerEventWriterDelegate extends EventWriterDelegate

{

  private TransformerHandler	handler;
  private XMLEventWriter	wrapper;



  public
  TransformerHandlerEventWriterDelegate(TransformerHandler handler)
  {
    this(handler, null);
  }



  public
  TransformerHandlerEventWriterDelegate
  (
    TransformerHandler	handler,
    XMLEventWriter	writer
  )
  {
    this.handler = handler;
    setParent(writer);
    wrapper = new ContentHandlerEventWriter(handler);
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    wrapper.add(event);
  }



  public void
  close() throws XMLStreamException
  {
    wrapper.close();
  }



  public void
  flush() throws XMLStreamException
  {
    wrapper.flush();
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return wrapper.getPrefix(uri);
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    wrapper.setDefaultNamespace(uri);
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    wrapper.setNamespaceContext(context);
  }



  public void
  setParent(XMLEventWriter writer)
  {
    if (writer != null)
    {
      super.setParent(writer);
      handler.
        setResult(new SAXResult(new EventWriterContentHandler(getParent())));
    }
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    wrapper.setPrefix(prefix, uri);
  }

} // TransformerHandlerEventWriterDelegate
