package be.re.xml.stax;

import java.util.Iterator;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;



/**
 * A ContentHandler wrapper.
 * @author Werner Donn\u00e9
 */

public class ContentHandlerEventWriter implements XMLEventWriter

{

  private Stack			elements = new Stack();
  private ContentHandler	handler;
  private NamespaceContext	namespaceContext;
  private boolean		pendingElement = false;



  public
  ContentHandlerEventWriter()
  {
  }



  public
  ContentHandlerEventWriter(ContentHandler handler)
  {
    setContentHandler(handler);
  }



  public void
  add(XMLEvent event) throws XMLStreamException 
  {
    switch (event.getEventType())
    {
      case XMLStreamConstants.ATTRIBUTE:
        writeAttribute((Attribute) event);
        break;
      case XMLStreamConstants.CDATA:
        writeCharacters((Characters) event);
        break;
      case XMLStreamConstants.CHARACTERS:
        writeCharacters((Characters) event);
        break;
      case XMLStreamConstants.COMMENT:
        writeComment((Comment) event);
        break;
      case XMLStreamConstants.DTD:
        break;
      case XMLStreamConstants.END_DOCUMENT:
        writeEndDocument((EndDocument) event);
        break;
      case XMLStreamConstants.END_ELEMENT:
        writeEndElement((EndElement) event);
        break;
      case XMLStreamConstants.ENTITY_DECLARATION:
        break;
      case XMLStreamConstants.ENTITY_REFERENCE:
        writeEntityReference((EntityReference) event);
        break;
      case XMLStreamConstants.NAMESPACE:
        writeNamespace((Namespace) event);
        break;
      case XMLStreamConstants.NOTATION_DECLARATION:
        break;
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
        writeProcessingInstruction((ProcessingInstruction) event);
        break;
      case XMLStreamConstants.SPACE:
        writeCharacters((Characters) event);
        break;
      case XMLStreamConstants.START_DOCUMENT:
        writeStartDocument((StartDocument) event);
        break;
      case XMLStreamConstants.START_ELEMENT:
        writeStartElement((StartElement) event);
        break;
    }
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
  getQName(QName name)
    throws XMLStreamException
  {
    return
      ("".equals(name.getPrefix()) ? "" : (name.getPrefix() + ":")) +
        name.getLocalPart();
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



  private void
  writeAttribute(Attribute event) throws XMLStreamException
  {
    ((Element) elements.peek()).attributes.addAttribute
    (
      event.getName().getNamespaceURI(),
      event.getName().getLocalPart(),
      getQName(event.getName()),
      event.getDTDType(),
      event.getValue()
    );
  }



  private void
  writeCharacters(Characters event) throws XMLStreamException
  {
    writeCharacters(event.getData());
  }



  private void
  writeCharacters(String s) throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      if (!elements.isEmpty())
      {
        handler.characters(s.toCharArray(), 0, s.length());
      }
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  private void
  writeComment(Comment event) throws XMLStreamException
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



  private void
  writeEndDocument(EndDocument event) throws XMLStreamException
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



  private void
  writeEndElement(EndElement event) throws XMLStreamException
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



  private void
  writeEntityReference(EntityReference event) throws XMLStreamException
  {
    writeCharacters("&" + event.getName() + ";");
  }



  private void
  writeNamespace(Namespace event) throws XMLStreamException
  {
    if
    (
      event.getPrefix() == null		||
      event.getPrefix().equals("")	||
      event.getPrefix().equals("xmlns")
    )
    {
      ((Element) elements.peek()).attributes.addAttribute
      (
        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        XMLConstants.XMLNS_ATTRIBUTE,
        XMLConstants.XMLNS_ATTRIBUTE,
        "CDATA",
        event.getNamespaceURI()
      );
    }
    else
    {
      ((Element) elements.peek()).attributes.addAttribute
      (
        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        event.getPrefix(),
        XMLConstants.XMLNS_ATTRIBUTE + ":" + event.getPrefix(),
        "CDATA",
        event.getNamespaceURI()
      );
    }
  }



  private void
  writeProcessingInstruction(ProcessingInstruction event)
    throws XMLStreamException
  {
    try
    {
      flushPendingElement();
      handler.processingInstruction(event.getTarget(), event.getData());
    }

    catch (SAXException e)
    {
      throw new XMLStreamException(e);
    }
  }



  private void
  writeStartDocument(StartDocument event) throws XMLStreamException
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



  private void
  writeStartElement(StartElement event) throws XMLStreamException
  {
    try
    {
      flushPendingElement();

      elements.push
      (
        new Element
        (
          event.getName().getNamespaceURI(),
          event.getName().getLocalPart(),
          getQName(event.getName())
        )
      );

      pendingElement = true;

      for (Iterator i = event.getNamespaces(); i.hasNext();)
      {
        writeNamespace((Namespace) i.next());
      }

      for (Iterator i = event.getAttributes(); i.hasNext();)
      {
        writeAttribute((Attribute) i.next());
      }
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

} // ContentHandlerEventWriter
