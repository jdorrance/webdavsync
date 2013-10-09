package be.re.xml.stax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;



/**
 * A ContentHandler wrapper around an XMLEventWriter.
 * @author Werner Donn\u00e9
 */

public class EventWriterContentHandler implements ContentHandler

{

  private Stack			namespaces = new Stack();
  private List			pendingNamespaces = new ArrayList();
  private XMLEventWriter	writer;



  public
  EventWriterContentHandler(XMLEventWriter writer)
  {
    setWriter(writer);
  }



  public void
  characters(char[] ch, int start, int length) throws SAXException
  {
    try
    {
      writer.
        add(Util.eventFactory.createCharacters(new String(ch, start, length)));
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  endDocument() throws SAXException
  {
    try
    {
      writer.add(Util.eventFactory.createEndDocument());
      writer.flush();
      writer.close();
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  endElement(String namespaceURI, String localName, String qName)
    throws SAXException
  {
    try
    {
      writer.add
      (
        Util.eventFactory.createEndElement
        (
          qName.indexOf(':') != -1 ?
            qName.substring(0, qName.indexOf(':')) : "",
          namespaceURI,
          localName,
          ((Map) namespaces.pop()).values().iterator()
        )
      );
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  endPrefixMapping(String prefix) throws SAXException
  {
  }



  public XMLEventWriter
  getWriter()
  {
    return writer;
  }



  public void
  ignorableWhitespace(char[] ch, int start, int length) throws SAXException
  {
    try
    {
      writer.add
      (
        Util.eventFactory.createIgnorableSpace(new String(ch, start, length))
      );
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  processingInstruction(String target, String data) throws SAXException
  {
    try
    {
      writer.add(Util.eventFactory.createProcessingInstruction(target, data));
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  setDocumentLocator(Locator locator)
  {
  }



  public void
  setWriter(XMLEventWriter writer)
  {
    this.writer = writer;
  }



  public void
  skippedEntity(String name) throws SAXException
  {
  }



  public void
  startDocument() throws SAXException
  {
    try
    {
      writer.add(Util.eventFactory.createStartDocument());
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  startElement
  (
    String	namespaceURI,
    String	localName,
    String	qName,
    Attributes	atts
  ) throws SAXException
  {
    try
    {
      List	attributes = new ArrayList();
      Map	inScope = new HashMap();

      namespaces.push(inScope);

      for (int i = 0; i < atts.getLength(); ++i)
      {
        String		name = atts.getQName(i);

        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(atts.getURI(i)))
        {
          Namespace	namespace =
            name.indexOf(':') != -1 ?
              Util.eventFactory.createNamespace
              (
                name.substring(name.indexOf(':') + 1),
                atts.getValue(i)
              ) : Util.eventFactory.createNamespace(atts.getValue(i));

          inScope.put(namespace.getPrefix(), namespace);
        }
        else
        {
          attributes.add
          (
            Util.eventFactory.createAttribute
            (
              name.indexOf(':') != -1 ?
                name.substring(0, name.indexOf(':')) : "",
              atts.getURI(i),
              atts.getLocalName(i),
              atts.getValue(i)
            )
          );
        }
      }

      for (Iterator i = pendingNamespaces.iterator(); i.hasNext();)
      {
        Namespace	namespace = (Namespace) i.next();

        inScope.put(namespace.getPrefix(), namespace);
      }

      pendingNamespaces.clear();

      writer.add
      (
        Util.eventFactory.createStartElement
        (
          qName.indexOf(':') != -1 ?
            qName.substring(0, qName.indexOf(':')) : "",
          namespaceURI,
          localName,
          attributes.iterator(),
          inScope.values().iterator()
        )
      );
    }

    catch (XMLStreamException e)
    {
      throw new SAXException(e);
    }
  }



  public void
  startPrefixMapping(String prefix, String uri) throws SAXException
  {
    pendingNamespaces.add
    (
      "".equals(prefix) ?
        Util.eventFactory.createNamespace(uri) :
        Util.eventFactory.createNamespace(prefix, uri)
    );
  }

} // EventWriterContentHandler
