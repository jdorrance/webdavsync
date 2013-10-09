package be.re.xml.stax;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;



/**
 * @author Werner Donn\u00e9
 */

public class Util

{

  static final DocumentBuilder	documentBuilder = newDocumentBuilder();
  static final XMLEventFactory	eventFactory = newEventFactory();
  private static Class		eventFactoryClass;
  static final XMLInputFactory	inputFactory = newInputFactory(false, true);
  private static Class		inputFactoryClass;
  static final XMLOutputFactory	outputFactory = newOutputFactory();
  private static Class		outputFactoryClass;



  public static Element
  accumulate(XMLEventReader reader, StartElement currentEvent)
    throws XMLStreamException
  {
    Document		document = documentBuilder.newDocument();
    DOMEventWriter	writer = new DOMEventWriter(document);

    writer.add(eventFactory.createStartDocument());
    addElement(reader, writer, currentEvent);
    writer.add(eventFactory.createEndDocument());
    writer.close();

    return document.getDocumentElement();
  }



  /**
   * Adds the complete element that started with <code>currentEvent</code> to
   * <code>writer</code>.
   */

  public static void
  addElement
  (
    XMLEventReader	reader,
    XMLEventWriter	writer,
    StartElement	currentEvent
  ) throws XMLStreamException
  {
    Stack	elements = new Stack();

    elements.push(currentEvent);
    writer.add(currentEvent);

    while (reader.hasNext())
    {
      XMLEvent	event = reader.nextEvent();

      writer.add(event);

      if (event.isStartElement())
      {
        elements.push(event);
      }
      else
      {
        if (event.isEndElement())
        {
          elements.pop();
        }
      }

      if (elements.isEmpty())
      {
        return;
      }
    }
  }



  public static void
  addElement(Element element, XMLEventWriter writer) throws XMLStreamException
  {
    new GobbleDocumentEventsWriter(writer).
      add(inputFactory.createXMLEventReader(new DOMSource(element)));
  }



  public static void
  addEmptyElement(QName name, XMLEventWriter writer) throws XMLStreamException
  {
    writer.add(eventFactory.createStartElement(name, null, null));
    writer.add(eventFactory.createEndElement(name, null));
  }



  public static void
  addTextElement(QName name, String text, XMLEventWriter writer)
    throws XMLStreamException
  {
    writer.add(eventFactory.createStartElement(name, null, null));
    writer.add(eventFactory.createCharacters(text));
    writer.add(eventFactory.createEndElement(name, null));
  }



  /**
   * This method is much less memory intensive for large PCDATA sections.
   * @param entityDeclarations a map from entity names to replacement texts.
   */

  private static String
  coalesceText(List events, Map entityDeclarations)
  {
    int	length = 0;

    for (Iterator i = events.iterator(); i.hasNext();)
    {
      XMLEvent	e = (XMLEvent) i.next();

      length +=
        e.isCharacters() ?
          e.asCharacters().getData().length() :
          (
            entityDeclarations != null &&
              entityDeclarations.get(((EntityReference) e).getName()) != null ?
              (
                (String) entityDeclarations.get(((EntityReference) e).getName())
              ).length() : (((EntityReference) e).getName().length() + 2)
          );
    }

    int		position = 0;
    char[]	result = new char[length];

    for (Iterator i = events.iterator(); i.hasNext();)
    {
      XMLEvent	e = (XMLEvent) i.next();
      String	text =
        e.isCharacters() ?
          e.asCharacters().getData() :
          (
            entityDeclarations != null &&
              entityDeclarations.get(((EntityReference) e).getName()) != null ?
              (String) entityDeclarations.get(((EntityReference) e).getName()) :
              ("&" + ((EntityReference) e).getName() + ";")
          );

      System.arraycopy(text.toCharArray(), 0, result, position, text.length());
    }

    return new String(result);
  }



  public static Attribute
  createAttribute(Attr attribute) throws XMLStreamException
  {
    return
      eventFactory.createAttribute
      (
        be.re.xml.Util.getQName(attribute),
        attribute.getValue()
      );
  }



  public static StartElement
  createStartElement(Element element) throws XMLStreamException
  {
    List		attributes = new ArrayList();
    NamedNodeMap	list = element.getAttributes();
    List		namespaces = new ArrayList();

    for (int i = 0; i < list.getLength(); ++i)
    {
      Attr	attribute = (Attr) list.item(i);

      if
      (
        XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())
      )
      {
        namespaces.add(createAttribute(attribute));
      }
      else
      {
        attributes.add(createAttribute(attribute));
      }
    }

    return
      eventFactory.createStartElement
      (
        be.re.xml.Util.getQName(element),
        attributes.iterator(),
        namespaces.iterator()
      );
  }



  public static void
  discardElement(XMLEventReader reader, StartElement currentEvent)
    throws XMLStreamException
  {
    addElement(reader, new DevNullEventWriter(), currentEvent);
  }



  public static String
  getAttribute(StartElement event, QName name)
  {
    Attribute	attribute = event.getAttributeByName(name);

    return attribute != null ? attribute.getValue() : null;
  }



  public static String
  getDocumentElementNamespaceURI(InputStream in) throws XMLStreamException
  {
    XMLEventReader	reader = null;

    try
    {
      reader = inputFactory.createXMLEventReader(in);

      while (reader.hasNext())
      {
        XMLEvent	event = reader.nextEvent();

        if (event.isStartElement())
        {
          return event.asStartElement().getName().getNamespaceURI();
        }
      }

      return null;
    }

    catch (XMLStreamException e)
    {
      throw e;
    }

    catch (Exception e)
    {
      throw new XMLStreamException(e);
    }

    finally
    {
      if (reader != null)
      {
        reader.close();
      }
    }
  }



  public static String
  getElementText
  (
    XMLEventReader	reader,
    XMLEvent		currentEvent,
    Map			entityDeclarations
  ) throws XMLStreamException
  {
    if (!currentEvent.isStartElement())
    {
      throw new XMLStreamException("Unexpected event type");
    }

    List	events = new ArrayList();

    for
    (
      XMLEvent e = reader.nextEvent();
      !e.isEndElement();
      e = reader.nextEvent()
    )
    {
      if (e.isCharacters() || e.isEntityReference())
      {
        events.add(e);
      }
      else
      {
        if
        (
          !e.isProcessingInstruction()				&&
          e.getEventType() != XMLStreamConstants.COMMENT
        )
        {
          throw new XMLStreamException("Unexpected event type");
        }
      }
    }

    return coalesceText(events, entityDeclarations);
  }



  public static boolean
  isEnd(XMLEvent event, String namespaceURI, String localName)
  {
    return
      event.isEndElement() &&
        (
          namespaceURI == null ||
            namespaceURI.
              equals(event.asEndElement().getName().getNamespaceURI())
        ) &&
        localName.equals(event.asEndElement().getName().getLocalPart());
  }



  public static boolean
  isStart(XMLEvent event, String namespaceURI, String localName)
  {
    return
      event.isStartElement() &&
        (
          namespaceURI == null ||
            namespaceURI.
              equals(event.asStartElement().getName().getNamespaceURI())
        ) &&
        localName.equals(event.asStartElement().getName().getLocalPart());
  }



  private static DocumentBuilder
  newDocumentBuilder()
  {
    try
    {
      return
        be.re.xml.Util.newDocumentBuilderFactory(false).newInstance().
          newDocumentBuilder();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static XMLEventFactory
  newEventFactory()
  {
    try
    {
      if (eventFactoryClass == null)
      {
        String	className =
          be.re.util.Util.
            getReSystemProperty("javax.xml.stream.XMLEventFactory");

        eventFactoryClass =
          className != null ?
            Class.forName(className) : XMLEventFactory.newInstance().getClass();
      }

      return (XMLEventFactory) eventFactoryClass.newInstance();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static XMLInputFactory
  newInputFactory()
  {
    try
    {
      if (inputFactoryClass == null)
      {
        String	className =
          be.re.util.Util.
            getReSystemProperty("javax.xml.stream.XMLInputFactory");

        inputFactoryClass =
          className != null ?
            Class.forName(className) : XMLInputFactory.newInstance().getClass();
      }

      return (XMLInputFactory) inputFactoryClass.newInstance();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static XMLInputFactory
  newInputFactory(boolean validating, boolean expanding)
  {
    XMLInputFactory	result = newInputFactory();

    result.setProperty("com.ctc.wstx.cacheDTDsByPublicId", new Boolean(true));
      // Prevent hostname lookup in Woodstox.
    result.setProperty(XMLInputFactory.SUPPORT_DTD, new Boolean(true));
    result.setProperty(XMLInputFactory.IS_VALIDATING, new Boolean(validating));
    result.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, new Boolean(true));

    result.setProperty
    (
      XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
      new Boolean(expanding)
    );

    result.setProperty
    (
      XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
      new Boolean(true)
    );

    return result;
  }



  public static XMLOutputFactory
  newOutputFactory()
  {
    try
    {
      if (outputFactoryClass == null)
      {
        String	className =
          be.re.util.Util.
            getReSystemProperty("javax.xml.stream.XMLOutputFactory");

        outputFactoryClass =
          className != null ?
            Class.forName(className) :
            XMLOutputFactory.newInstance().getClass();
      }

      return (XMLOutputFactory) outputFactoryClass.newInstance();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static XMLEvent
  nextTag(XMLEventReader reader) throws XMLStreamException
  {
    XMLEvent	e;

    for
    (
      e = reader.nextEvent();
      (
        e.isCharacters() &&
          (
            e.asCharacters().isIgnorableWhiteSpace() ||
              e.asCharacters().isWhiteSpace()
          )
      ) || e.isProcessingInstruction() ||
        e.getEventType() == XMLStreamConstants.COMMENT;
      e = reader.nextEvent()
    );

    if (!e.isStartElement() && !e.isEndElement())
    {
      throw new XMLStreamException("Unexpected event type");
    }

    return e;
  }



  public static StartElement
  removeAttribute(StartElement event, QName name)
  {
    List	attributes = new ArrayList();

    for (Iterator i = event.getAttributes(); i.hasNext();)
    {
      Attribute	attribute = (Attribute) i.next();

      if (!name.equals(attribute.getName()))
      {
        attributes.add(attribute);
      }
    }

    return
      eventFactory.createStartElement
      (
        event.getName(),
        attributes.iterator(),
        event.getNamespaces()
      );
  }



  public static StartElement
  setAttribute(StartElement event, QName name, String value)
  {
    return setAttribute(event, eventFactory.createAttribute(name, value));
  }



  public static StartElement
  setAttribute(StartElement event, Attribute attribute)
  {
    List	attributes = new ArrayList();

    for (Iterator i = event.getAttributes(); i.hasNext();)
    {
      Attribute	a = (Attribute) i.next();

      if (!attribute.getName().equals(a.getName()))
      {
        attributes.add(a);
      }
    }

    attributes.add(attribute);

    return
      eventFactory.createStartElement
      (
        event.getName(),
        attributes.iterator(),
        event.getNamespaces()
      );
  }



  public static StartElement
  setNamespace(StartElement event, String prefix, String namespaceURI)
  {
    return
      setNamespace
      (
        event,
        prefix == null ?
          eventFactory.createNamespace(namespaceURI) :
          eventFactory.createNamespace(prefix, namespaceURI)
      );
  }



  public static StartElement
  setNamespace(StartElement event, Namespace namespace)
  {
    boolean	alreadyPresent = false;
    List	namespaces = new ArrayList();

    for (Iterator i = event.getNamespaces(); i.hasNext() && !alreadyPresent;)
    {
      Namespace	n = (Namespace) i.next();

      if
      (
        namespace.getPrefix().equals(n.getPrefix())		&&
        namespace.getNamespaceURI().equals(n.getNamespaceURI())
      )
      {
        alreadyPresent = true;
      }
      else
      {
        namespaces.add(n);
      }
    }

    if (alreadyPresent)
    {
      return event;
    }

    namespaces.add(namespace);

    return
      eventFactory.createStartElement
      (
        event.getName(),
        event.getAttributes(),
        namespaces.iterator()
      );
  }

} // Util
