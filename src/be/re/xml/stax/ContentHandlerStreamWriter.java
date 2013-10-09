package be.re.xml.stax;

import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;



/**
 * A ContentHandler wrapper.
 * @author Werner Donn\u00e9
 */

public class ContentHandlerStreamWriter implements XMLStreamWriter

{

  private Stack			elements = new Stack();
  private ContentHandler	handler;
  private NamespaceContext	namespaceContext;
  private boolean		pendingElement = false;



  public
  ContentHandlerStreamWriter()
  {
  }



  public
  ContentHandlerStreamWriter(ContentHandler handler)
  {
    setContentHandler(handler);
  }



  public void
  close() throws XMLStreamException
  {
  }



  private void
  endElement() throws SAXException
  {
    Element	element = (Element) elements.pop();

    handler.endElement(element.namespaceURI, element.localName, element.qName);
  }



  public void
  flush() throws XMLStreamException
  {
  }



  private void
  flushPendingElement() throws SAXException
  {
    if (pendingElement && !elements.isEmpty())
    {
      pendingElement = false;

      Element	element = (Element) elements.peek();

      handler.startElement
      (
        element.namespaceURI,
        element.localName,
        element.qName,
        element.attributes
      );

      if (element.empty)
      {
        endElement();
      }
    }
  }



  public ContentHandler
  getContentHandler()
  {
    return handler;
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return namespaceContext;
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return null;
  }



  public Object
  getProperty(String name)
  {
    if (!"javax.xml.stream.isPrefixDefaulting".equals(name))
    {
      throw
        new IllegalArgumentException("Property " + name + " is not supported.");
    }

    return new Boolean(false);
  }



  private String
  getQName(String prefix, String namespaceURI, String localName)
    throws XMLStreamException
  {
    return (prefix.equals("") ? "" : (prefix + ":")) + localName;
  }



  public void
  setContentHandler(ContentHandler handler)
  {
    this.handler = handler;
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
  }



  public void
  setNamespaceContext(NamespaceContext context)
  {
    namespaceContext = context;
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
  }



  public void
  writeAttribute(String localName, String value) throws XMLStreamException
  {
    ((Element) elements.peek()).attributes.
      addAttribute("", localName, localName, "CDATA", value);
  }



  public void
  writeAttribute(String namespaceURI, String localName, String value)
    throws XMLStreamException
  {
    ((Element) elements.peek()).attributes.addAttribute
    (
      namespaceURI,
      localName,
      getQName("", namespaceURI, localName),
      "CDATA",
      value
    );
  }



  public void
  writeAttribute
  (
    String	prefix,
    String	namespaceURI,
    String	localName,
    String	value
  ) throws XMLStreamException
  {
    ((Element) elements.peek()).attributes.addAttribute
    (
      namespaceURI,
      localName,
      getQName(prefix, namespaceURI, localName),
      "CDATA",
      value
    );
  }



  public void
  writeCData(String data) throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      if (!elements.isEmpty())
      {
        handler.characters(data.toCharArray(), 0, data.length());
      }
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeCharacters(char[] text, int start, int len) throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      if (!elements.isEmpty())
      {
        handler.characters(text, start, len);
      }
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeCharacters(String text) throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      if (!elements.isEmpty())
      {
        handler.characters(text.toCharArray(), 0, text.length());
      }
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeComment(String data) throws XMLStreamException
  {
    try
    {
      flushPendingElement();
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeDefaultNamespace(String namespaceURI) throws XMLStreamException
  {
    ((Element) elements.peek()).attributes.addAttribute
    (
      XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
      XMLConstants.XMLNS_ATTRIBUTE,
      XMLConstants.XMLNS_ATTRIBUTE,
      "CDATA",
      namespaceURI
    );
  }



  public void
  writeDTD(String dtd) throws XMLStreamException
  {
  }



  public void
  writeEmptyElement(String localName) throws XMLStreamException
  {
    try
    {
      writeEmptySAXElement("", localName, localName);
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    try
    {
      writeEmptySAXElement
      (
        namespaceURI,
        localName,
        getQName("", namespaceURI, localName)
      );
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeEmptyElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    try
    {
      writeEmptySAXElement
      (
        namespaceURI,
        localName,
        getQName(prefix, namespaceURI, localName)
      );
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  private void
  writeEmptySAXElement(String namespaceURI, String localName, String qName)
    throws SAXException
  {
    flushPendingElement();
    handler.startElement(namespaceURI, localName, qName, new AttributesImpl());
    handler.endElement(namespaceURI, localName, qName);
  }



  public void
  writeEndDocument() throws XMLStreamException
  {
    try
    {
      handler.endDocument();
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeEndElement() throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      Element	element = (Element) elements.pop();

      handler.
        endElement(element.namespaceURI, element.localName, element.qName);
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeEntityRef(String name) throws XMLStreamException
  {
    writeCharacters("&" + name + ";");
  }



  public void
  writeNamespace(String prefix, String namespaceURI) throws XMLStreamException
  {
    if (prefix == null || prefix.equals("") || prefix.equals("xmlns"))
    {
      writeDefaultNamespace(namespaceURI);
    }
    else
    {
      ((Element) elements.peek()).attributes.addAttribute
      (
        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        prefix,
        XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix,
        "CDATA",
        namespaceURI
      );
    }
  }



  public void
  writeProcessingInstruction(String target) throws XMLStreamException
  {
    try
    {
      flushPendingElement();
      handler.processingInstruction(target, null);
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    try
    {
      flushPendingElement();
      handler.processingInstruction(target, data);
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeStartDocument() throws XMLStreamException
  {
    try
    {
      handler.startDocument();
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeStartDocument(String version) throws XMLStreamException
  {
    writeStartDocument();
  }



  public void
  writeStartDocument(String encoding, String version) throws XMLStreamException
  {
    writeStartDocument();
  }



  public void
  writeStartElement(String localName) throws XMLStreamException
  {
    try
    {
      flushPendingElement();
      elements.push(new Element("", localName, localName));
      pendingElement = true;
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      elements.push
      (
        new Element
        (
          namespaceURI,
          localName,
          getQName("", namespaceURI, localName)
        )
      );

      pendingElement = true;
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  public void
  writeStartElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      elements.push
      (
        new Element
        (
          namespaceURI,
          localName,
          getQName(prefix, namespaceURI, localName)
        )
      );

      pendingElement = true;
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  private static class Element

  {

    private AttributesImpl	attributes = new AttributesImpl();
    private boolean		empty= false;
    private String		localName;
    private String		namespaceURI;
    private String		qName;



    private
    Element(String namespaceURI, String localName, String qName)
    {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.qName = qName;
    }

  } // Element

} // ContentHandlerStreamWriter
