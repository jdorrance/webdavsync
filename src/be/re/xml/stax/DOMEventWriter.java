package be.re.xml.stax;

import java.util.Iterator;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * Creates a DOM tree.
 * @author Werner Donn\u00e9
 */

public class DOMEventWriter implements XMLEventWriter

{

  private Document	document;
  private Stack		elements = new Stack();



  public
  DOMEventWriter()
  {
    this(null);
  }



  public
  DOMEventWriter(Document document)
  {
    this.document =
      document != null ? document : Util.documentBuilder.newDocument();
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    switch (event.getEventType())
    {
      case XMLEvent.ATTRIBUTE:
        addAttribute((Element) current(), (Attribute) event);
        break;

      case XMLEvent.CDATA:
        current().appendChild
        (
          document.createCDATASection(event.asCharacters().getData())
        );
        break;

      case XMLEvent.CHARACTERS:
      case XMLEvent.SPACE:
        if (current() instanceof Element)
        {
          current().appendChild
          (
            document.createTextNode(event.asCharacters().getData())
          );
        }

        break;

      case XMLEvent.COMMENT:
        current().
          appendChild(document.createComment(((Comment) event).getText()));
        break;

      case XMLEvent.END_DOCUMENT:
      case XMLEvent.END_ELEMENT:
       elements.pop();
       break;

      case XMLEvent.ENTITY_REFERENCE:
        current().appendChild
        (
          document.createEntityReference(((EntityReference) event).getName())
        );
        break;

      case XMLEvent.NAMESPACE:
        addNamespace((Element) current(), (Namespace) event);
        break;

      case XMLEvent.PROCESSING_INSTRUCTION:
        current().appendChild
        (
          document.createProcessingInstruction
          (
            ((ProcessingInstruction) event).getTarget(),
            ((ProcessingInstruction) event).getData()
          )
        );
        break;

      case XMLEvent.START_DOCUMENT:
        elements.push(document);
        break;

      case XMLEvent.START_ELEMENT:
        elements.push(addElement(current(), event.asStartElement()));
        break;
    }
  }



  public void
  add(XMLEventReader reader) throws XMLStreamException
  {
    while(reader.hasNext())
    {
      add(reader.nextEvent());
    }
  }



  private static void
  addAttribute(Element element, Attribute attribute)
  {
    element.setAttributeNS
    (
      "".equals(attribute.getName().getNamespaceURI()) ?
        null : attribute.getName().getNamespaceURI(),
      (
        attribute.getName().getPrefix() == null ||
          "".equals(attribute.getName().getPrefix()) ?
            "" : (attribute.getName().getPrefix() + ":")
      ) + attribute.getName().getLocalPart(),
      attribute.getValue()
    );
  }



  private Node
  addElement(Node node, StartElement element)
  {
    Element	result =
      document.createElementNS
      (
        "".equals(element.getName().getNamespaceURI()) ?
          null : element.getName().getNamespaceURI(),
        (
          "".equals(element.getName().getPrefix()) ?
            "" : (element.getName().getPrefix() + ":")
        ) + element.getName().getLocalPart()
      );

    for (Iterator i = element.getNamespaces(); i.hasNext();)
    {
      addNamespace(result, (Namespace) i.next());
    }

    for (Iterator i = element.getAttributes(); i.hasNext();)
    {
      addAttribute(result, (Attribute) i.next());
    }

    if (node != null)
    {
      node.appendChild(result);
    }
    else
    {
      document.appendChild(result);
    }

    return result;
  }



  private static void
  addNamespace(Element element, Namespace namespace)
  {
    element.setAttributeNS
    (
      XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
      XMLConstants.XMLNS_ATTRIBUTE +
        ("".equals(namespace.getPrefix()) ? "" : (":" + namespace.getPrefix())),
      namespace.getNamespaceURI()
    );
  }



  public void
  close() throws XMLStreamException
  {
  }



  public boolean
  complete()
  {
    return elements.isEmpty();
  }



  private Node
  current()
  {
    return elements.isEmpty() ? null : (Node) elements.peek();
  }



  public void
  flush() throws XMLStreamException
  {
  }



  public Document
  getDocument()
  {
    return document;
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

} // DOMEventWriter
