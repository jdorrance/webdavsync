package be.re.xml.sax;

import java.io.IOException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;



/**
 * An <code>XMLReader</code> which produces SAX events from another source than
 * a parser.
 * @author Werner Donn\u00e9
 */

public class AlternateSourceReader implements XMLReader

{

  private ContentHandler	contentHandler;
  private DTDHandler		dtdHandler;
  private EntityResolver	entityResolver;
  private ErrorHandler		errorHandler;
  private Generate		generate;



  public
  AlternateSourceReader(Generate generate)
  {
    this.generate = generate;
  }



  public ContentHandler
  getContentHandler()
  {
    return contentHandler;
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
    throw new SAXNotSupportedException("Features are not supported.");
  }



  public Object
  getProperty(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    throw new SAXNotSupportedException("Properties are not supported.");
  }



  public void
  parse(InputSource input) throws IOException, SAXException
  {
    if (generate != null)
    {
      try
      {
        generate.generate(this);
      }

      catch (Exception e)
      {
        throw new SAXException(e);
      }
    }
  }



  public void
  parse(String systemId) throws IOException, SAXException
  {
    parse(new InputSource(systemId));
  }



  public void
  setContentHandler(ContentHandler contentHandler)
  {
    this.contentHandler = contentHandler;
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
    throw new SAXNotSupportedException("Features are not supported.");
  }



  public void
  setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    throw new SAXNotSupportedException("Properties are not supported.");
  }



  public interface Generate

  {

    public void	generate	(XMLReader reader) throws Exception;

  } // Generate

} // AlternateSourceReader
