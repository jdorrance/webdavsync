package be.re.xml.stax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;



/**
 * An XMLEventReader wrapper around an XMLFilter.
 * @author Werner Donn\u00e9
 */

public class XMLFilterEventReaderDelegate extends EventReaderDelegate

{

  private List			buffer = new ArrayList();
  private XMLEventWriter	filterWriter;
  private XMLReader		nop =
    new XMLFilterImpl()
    {
      public void
      parse(InputSource input) throws IOException, SAXException
      {
      }

      public void
      parse(String systemId) throws IOException, SAXException
      {
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
    };



  public
  XMLFilterEventReaderDelegate(XMLFilter filter)
  {
    this(filter, null);
  }



  public
  XMLFilterEventReaderDelegate(XMLFilter filter, XMLEventReader reader)
  {
    super(reader);
    replaceParser(filter);

    try
    {
      filter.parse((String) null);
        // Make sure set-up is done in the filter chain.
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    filterWriter =
      new StreamEventWriter
      (
        new ContentHandlerStreamWriter(nop.getContentHandler())
      );

    filter.setContentHandler
    (
      new EventWriterContentHandler
      (
        new DevNullEventWriter()
        {
          public void
          add(XMLEvent event) throws XMLStreamException
          {
            buffer.add(event);
          }
        }
      )
    );
  }



  public String
  getElementText() throws XMLStreamException
  {
    return Util.getElementText(this, (XMLEvent) buffer.get(0), null);
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

      filterWriter.add(getParent().nextEvent());
    }

    return true;
  }



  private void
  replaceParser(XMLFilter filter)
  {
    XMLFilter	previous = null;

    for
    (
      XMLFilter i = filter;
      i.getParent() != null && i.getParent() instanceof XMLFilter;
      previous = i, i = (XMLFilter) i.getParent()
    );

    if (previous != null)
    {
      if (!(previous.getParent() instanceof XMLFilter))
      {
        previous.setParent(nop);
      }
      else
      {
        ((XMLFilter) previous.getParent()).setParent(nop);
      }
    }
    else
    {
      filter.setParent(nop);
    }
  }

} // XMLFilterEventReaderDelegate
