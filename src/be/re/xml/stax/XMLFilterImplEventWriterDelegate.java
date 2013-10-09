package be.re.xml.stax;

import java.io.IOException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;



/**
 * An XMLEventWriter wrapper around an XMLFilterImpl. This implementation class
 * has been chosen because it both implements
 * <code>org.xml.sax.ContentHandler</code> and
 * <code>org.xml.sax.XMLReader</code>.
 * @author Werner Donn\u00e9
 */

public class XMLFilterImplEventWriterDelegate extends EventWriterDelegate

{

  private XMLFilterImpl		filter;
  private XMLEventWriter	wrapper;



  public
  XMLFilterImplEventWriterDelegate(XMLFilterImpl filter)
  {
    this(filter, null);
  }



  public
  XMLFilterImplEventWriterDelegate(XMLFilterImpl filter, XMLEventWriter writer)
  {
    this.filter = filter;
    setParent(writer);

    ContentHandlerStreamWriter	delegate = new ContentHandlerStreamWriter();

    wrapper = new StreamEventWriter(delegate);
    filter.setParent(new ReaderStub(delegate));
      // Give the filter a chance to do some set-up.

    if (delegate.getContentHandler() == null)
    {
      delegate.setContentHandler(filter);
    }
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
    super.setParent(writer);
    filter.setContentHandler(new EventWriterContentHandler(getParent()));
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    wrapper.setPrefix(prefix, uri);
  }



  private class ReaderStub implements XMLReader

  {

    private DTDHandler			dtdHandler;
    private EntityResolver		entityResolver;
    private ErrorHandler		errorHandler;
    private ContentHandlerStreamWriter	writer;



    public
    ReaderStub(ContentHandlerStreamWriter writer)
    {
      this.writer = writer;
    }



    public ContentHandler
    getContentHandler()
    {
      return writer.getContentHandler();
    }



    public DTDHandler
    getDTDHandler()
    {
      return dtdHandler;
    }



    public EntityResolver
    getEntityResolver()
    {
      return entityResolver;
    }



    public ErrorHandler
    getErrorHandler()
    {
      return errorHandler;
    }



    public boolean
    getFeature(String name)
      throws SAXNotRecognizedException, SAXNotSupportedException
    {
      return false;
    }



    public Object
    getProperty(String name)
      throws SAXNotRecognizedException, SAXNotSupportedException
    {
      return null;
    }



    public void
    parse(InputSource input) throws IOException, SAXException
    {
    }



    public void
    parse(String systemId) throws IOException, SAXException
    {
    }



    public void
    setContentHandler(ContentHandler contentHandler)
    {
      writer.setContentHandler(contentHandler);
    }



    public void
    setDTDHandler(DTDHandler dtdHandler)
    {
      this.dtdHandler = dtdHandler;
    }



    public void
    setEntityResolver(EntityResolver entityResolver)
    {
      this.entityResolver = entityResolver;
    }



    public void
    setErrorHandler(ErrorHandler errorHandler)
    {
      this.errorHandler = errorHandler;
    }



    public void
    setFeature(String name, boolean value)
      throws SAXNotRecognizedException, SAXNotSupportedException
    {
    }



    public void
    setProperty(String name, Object value)
      throws SAXNotRecognizedException, SAXNotSupportedException
    {
    }
  
  } // ReaderStub

} // XMLFilterImplEventWriterDelegate
