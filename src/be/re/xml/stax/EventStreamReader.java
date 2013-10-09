package be.re.xml.stax;

import be.re.util.Array;
import be.re.xml.NamespacePrefixMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;



/**
 * An XMLStreamReader wrapper around an XMLEventReader.
 * @author Werner Donn\u00e9
 */

public class EventStreamReader implements XMLStreamReader

{

  private List			currentAttributes = new ArrayList();
  private XMLEvent		currentEvent;
  private List			currentNamespaces = new ArrayList();
  private Map			entityDeclarations = new HashMap();
  private NamespacePrefixMap	prefixMap = new NamespacePrefixMap();
  private XMLEventReader	reader;



  public
  EventStreamReader(XMLEventReader reader)
  {
    this.reader =
      new EventReaderDelegate(reader)
      {
        public XMLEvent
        nextEvent() throws XMLStreamException
        {
          currentEvent = getParent().nextEvent();

          return currentEvent;
        }
      };
  }



  private void
  checkAttributeState()
  {
    checkState
    (
      new int[]{XMLStreamConstants.ATTRIBUTE, XMLStreamConstants.START_ELEMENT}
    );
  }



  private void
  checkNamespaceState()
  {
    checkState
    (
      new int[]
      {
        XMLStreamConstants.NAMESPACE, XMLStreamConstants.START_ELEMENT,
          XMLStreamConstants.END_ELEMENT
      }
    );
  }



  private void
  checkState(int[] allowedTypes)
  {
    if
    (
      currentEvent == null					||
      Array.inArray(allowedTypes, currentEvent.getEventType())
    )
    {
      throw new IllegalStateException();
    }
  }



  private void
  checkTextState()
  {
    if (!hasText())
    {
      throw new IllegalStateException();
    }
  }



  private void
  clearNamespaces()
  {
    currentNamespaces.clear();

    for
    (
      Iterator i = currentEvent.asEndElement().getNamespaces(); i.hasNext();
    )
    {
      prefixMap.endPrefixMapping(((Namespace) i.next()).getPrefix());
    }
  }



  public void
  close() throws XMLStreamException
  {
    reader.close();
  }



  private Map
  createEntityDeclarations(List entities)
  {
    Map	result = new HashMap();

    for (Iterator i = entities.iterator(); i.hasNext();)
    {
      EntityDeclaration	declaration = (EntityDeclaration) i.next();

      result.put(declaration.getName(), declaration);
    }

    return result;
  }



  private Attribute
  getAttribute(int index)
  {
    return
      currentEvent.isAttribute() ?
        (Attribute) currentEvent : (Attribute) currentAttributes.get(index);
  }



  public int
  getAttributeCount()
  {
    checkAttributeState();

    return currentEvent.isAttribute() ? 1 : currentAttributes.size();
  }



  public String
  getAttributeLocalName(int index)
  {
    checkAttributeState();

    return getAttribute(index).getName().getLocalPart();
  }



  public QName
  getAttributeName(int index)
  {
    checkAttributeState();

    return getAttribute(index).getName();
  }



  public String
  getAttributeNamespace(int index)
  {
    checkAttributeState();

    return getAttribute(index).getName().getNamespaceURI();
  }



  public String
  getAttributePrefix(int index)
  {
    checkAttributeState();

    return getAttribute(index).getName().getPrefix();
  }



  public String
  getAttributeType(int index)
  {
    checkAttributeState();

    return getAttribute(index).getDTDType();
  }



  public String
  getAttributeValue(int index)
  {
    checkAttributeState();

    return getAttribute(index).getValue();
  }



  public String
  getAttributeValue(String namespaceURI, String localName)
  {
    checkAttributeState();

    QName	name =
      new QName(namespaceURI == null ? "" : namespaceURI, localName);

    if (currentEvent.isAttribute())
    {
      return
        name.equals(((Attribute) currentEvent).getName()) ?
          ((Attribute) currentEvent).getValue() : null;
    }

    for (Iterator i = currentAttributes.iterator(); i.hasNext();)
    {
      Attribute	attribute = (Attribute) i.next();

      if (name.equals(attribute.getName()))
      {
        return attribute.getValue();
      }
    }

    return null;
  }



  private void
  getAttributes()
  {
    currentAttributes.clear();

    for
    (
      Iterator i = currentEvent.asStartElement().getAttributes(); i.hasNext();
    )
    {
      currentAttributes.add(i.next());
    }
  }



  public String
  getCharacterEncodingScheme()
  {
    checkState(new int[]{XMLStreamConstants.START_DOCUMENT});

    return ((StartDocument) currentEvent).getCharacterEncodingScheme();
  }



  public String
  getElementText() throws XMLStreamException
  {
    return Util.getElementText(reader, currentEvent, entityDeclarations);
  }



  public String
  getEncoding()
  {
    checkState(new int[]{XMLStreamConstants.START_DOCUMENT});

    return ((StartDocument) currentEvent).getCharacterEncodingScheme();
  }



  public int
  getEventType()
  {
    return currentEvent.getEventType();
  }



  public String
  getLocalName()
  {
    checkState
    (
      new int[]
      {
        XMLStreamConstants.ENTITY_REFERENCE, XMLStreamConstants.START_ELEMENT,
          XMLStreamConstants.END_ELEMENT
      }
    );

    return
      currentEvent.isStartElement() ?
        currentEvent.asStartElement().getName().getLocalPart() :
        (
          currentEvent.isEndElement() ?
            currentEvent.asEndElement().getName().getLocalPart() :
            ((EntityReference) currentEvent).getName()
        );
  }



  public Location
  getLocation()
  {
    return currentEvent.getLocation();
  }



  public QName
  getName()
  {
    checkState
    (
      new int[]
      {XMLStreamConstants.END_ELEMENT, XMLStreamConstants.START_ELEMENT}
    );

    return
      currentEvent.isStartElement() ?
        currentEvent.asStartElement().getName() :
        currentEvent.asEndElement().getName();
  }



  private Namespace
  getNamespace(int index)
  {
    return
      currentEvent.isNamespace() ?
        (Namespace) currentEvent : (Namespace) currentNamespaces.get(index);
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return new PrefixMapContext();
  }



  public int
  getNamespaceCount()
  {
    checkNamespaceState();

    return currentEvent.isNamespace() ? 1 : currentNamespaces.size();
  }



  public String
  getNamespacePrefix(int index)
  {
    checkNamespaceState();

    return getNamespace(index).getPrefix();
  }



  public String
  getNamespaceURI()
  {
    return
      "".equals(getName().getNamespaceURI()) ?
        null : getName().getNamespaceURI();
  }



  public String
  getNamespaceURI(int index)
  {
    checkNamespaceState();

    return getNamespace(index).getNamespaceURI();
  }



  public String
  getNamespaceURI(String prefix)
  {
    return prefixMap.getNamespacePrefix(prefix);
  }



  private void
  getNamespaces()
  {
    currentNamespaces.clear();

    for
    (
      Iterator i = currentEvent.asStartElement().getNamespaces(); i.hasNext();
    )
    {
      Namespace	namespace = (Namespace) i.next();

      currentNamespaces.add(namespace);
      prefixMap.
        startPrefixMapping(namespace.getPrefix(), namespace.getNamespaceURI());
    }
  }



  public String
  getPIData()
  {
    checkState(new int[]{XMLStreamConstants.PROCESSING_INSTRUCTION});

    return ((ProcessingInstruction) currentEvent).getData();
  }



  public String
  getPITarget()
  {
    checkState(new int[]{XMLStreamConstants.PROCESSING_INSTRUCTION});

    return ((ProcessingInstruction) currentEvent).getTarget();
  }



  public String
  getPrefix()
  {
    return "".equals(getName().getPrefix()) ? null : getName().getPrefix();
  }



  public Object
  getProperty(String name)
  {
    if
    (
      currentEvent != null					&&
      currentEvent.getEventType() == XMLStreamConstants.DTD
    )
    {
      if ("javax.xml.stream.entities".equals(name))
      {
        List	result = ((DTD) currentEvent).getEntities();

        entityDeclarations = createEntityDeclarations(result);

        return result;
      }

      if ("javax.xml.stream.notations".equals(name))
      {
        return ((DTD) currentEvent).getNotations();
      }
    }

    return reader.getProperty(name);
  }



  public String
  getText()
  {
    checkTextState();

    return
      currentEvent.isCharacters() ?
        currentEvent.asCharacters().getData() :
        (
          currentEvent.getEventType() == XMLStreamConstants.COMMENT ?
            ((Comment) currentEvent).getText() :
            (
              currentEvent.getEventType() == XMLStreamConstants.DTD ?
                ((DTD) currentEvent).getDocumentTypeDeclaration() :
                ((EntityReference) currentEvent).getDeclaration().
                  getReplacementText()
            )
        );
  }



  public char[]
  getTextCharacters()
  {
    return getText().toCharArray();
  }



  public int
  getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
    throws XMLStreamException
  {
    checkState
    (
      new int[]
      {
        XMLStreamConstants.CDATA, XMLStreamConstants.CHARACTERS,
          XMLStreamConstants.SPACE
      }
    );

    char[]	chars = getTextCharacters();

    if
    (
      sourceStart < 0				||
      sourceStart > chars.length		||
      targetStart < 0				||
      targetStart >= target.length		||
      length < 0				||
      targetStart + length > target.length
    )
    {
      throw new IndexOutOfBoundsException();
    }

    int	result = Math.min(length, chars.length - sourceStart);

    System.arraycopy(chars, sourceStart, target, targetStart, result);

    return result;
  }



  public int
  getTextLength()
  {
    return getText().length();
  }



  public int
  getTextStart()
  {
    checkTextState();

    return 0;
  }



  public String
  getVersion()
  {
    checkState(new int[]{XMLStreamConstants.START_DOCUMENT});

    return ((StartDocument) currentEvent).getVersion();
  }



  public boolean
  hasName()
  {
    return currentEvent.isStartElement() || currentEvent.isEndElement();
  }



  public boolean
  hasNext() throws XMLStreamException
  {
    return reader.hasNext();
  }



  public boolean
  hasText()
  {
    return
      Array.inArray
      (
        new int[]
        {
          XMLStreamConstants.CDATA, XMLStreamConstants.CHARACTERS,
            XMLStreamConstants.COMMENT, XMLStreamConstants.DTD,
            XMLStreamConstants.ENTITY_REFERENCE, XMLStreamConstants.SPACE
        },
        currentEvent.getEventType()
      );
  }



  public boolean
  isAttributeSpecified(int index)
  {
    checkAttributeState();

    return getAttribute(index).isSpecified();
  }



  public boolean
  isCharacters()
  {
    return currentEvent != null && currentEvent.isCharacters();
  }



  public boolean
  isEndElement()
  {
    return currentEvent != null && currentEvent.isEndElement();
  }



  public boolean
  isStandalone()
  {
    checkState(new int[]{XMLStreamConstants.START_DOCUMENT});

    return ((StartDocument) currentEvent).isStandalone();
  }



  public boolean
  isStartElement()
  {
    return currentEvent != null && currentEvent.isStartElement();
  }



  public boolean
  isWhiteSpace()
  {
    return
      currentEvent != null && currentEvent.isCharacters() &&
        (
          currentEvent.asCharacters().isIgnorableWhiteSpace() ||
            currentEvent.asCharacters().isWhiteSpace()
        );
  }



  public int
  next() throws XMLStreamException
  {
    currentEvent = reader.nextEvent();

    if (currentEvent.isStartElement() || currentEvent.isEndElement())
    {
      getNamespaces();

      if (currentEvent.isStartElement())
      {
        getAttributes();
      }
      else
      {
        clearNamespaces();
      }
    }
    else
    {
      if (currentEvent.isNamespace())
      {
        prefixMap.startPrefixMapping
        (
          ((Namespace) currentEvent).getPrefix(),
          ((Namespace) currentEvent).getNamespaceURI()
        );
      }
    }

    return currentEvent.getEventType();
  }



  public int
  nextTag() throws XMLStreamException
  {
    currentEvent = Util.nextTag(reader);

    return currentEvent.getEventType();
  }



  public void
  require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    if
    (
      currentEvent == null			||
      type != currentEvent.getEventType()	||
      (
        namespaceURI != null			&&
        !namespaceURI.equals(getNamespaceURI())
      )						&&
      (
        localName != null			&&
        !localName.equals(getLocalName())
      )
    )
    {
      throw new XMLStreamException("Require type " + String.valueOf(type));
    }
  }



  public boolean
  standaloneSet()
  {
    return isStandalone();
  }



  private class PrefixMapContext implements NamespaceContext

  {

    public String
    getNamespaceURI(String prefix)
    {
      return prefixMap.getNamespaceURI(prefix);
    }



    public String
    getPrefix(String namespaceURI)
    {
      return prefixMap.getNamespacePrefix(namespaceURI);
    }



    public Iterator
    getPrefixes(String namespaceURI)
    {
      Map	map = prefixMap.getCurrentPrefixMap();
      List	result = new ArrayList();

      for (Iterator i = map.keySet().iterator(); i.hasNext();)
      {
        String	prefix = (String) i.next();

        if (namespaceURI.equals(map.get(prefix)))
        {
          result.add(prefix);
        }
      }

      return result.iterator();
    }

  } // PrefixMapContext

} // EventStreamReader
