package be.re.xml.stax;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.NotationDeclaration;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;



/**
 * Event utilities.
 * @author Werner Donn\u00e9
 */

public class Event

{

  private static boolean
  compareAttribute(Attribute e1, Attribute e2)
  {
    return
      e1.getDTDType().equals(e2.getDTDType()) &&
        e1.getName().equals(e2.getName()) &&
        e1.getValue().equals(e2.getValue());
  }



  /**
   * Attributes have no order.
   */

  private static boolean
  compareAttributes(Iterator i1, Iterator i2) throws Exception
  {
    return
      compareNamedOrderless
      (
        i1,
        i2,
        Attribute.class.getMethod("getName", new Class[0])
      );
  }



  private static boolean
  compareCharacters(Characters e1, Characters e2)
  {
    return e1.getData().equals(e2.getData());
  }



  private static boolean
  compareComment(Comment e1, Comment e2)
  {
    return e1.getText().equals(e2.getText());
  }



  private static boolean
  compareDTD(DTD e1, DTD e2) throws Exception
  {
    return
      e1.getDocumentTypeDeclaration().equals(e2.getDocumentTypeDeclaration()) &&
        compareNamedOrderless
        (
          e1.getEntities().iterator(),
          e2.getEntities().iterator(),
          EntityDeclaration.class.getMethod("getName", new Class[0])
        ) &&
        compareNamedOrderless
        (
          e1.getNotations().iterator(),
          e2.getNotations().iterator(),
          NotationDeclaration.class.getMethod("getName", new Class[0])
        );
  }



  private static boolean
  compareEndElement(EndElement e1, EndElement e2) throws Exception
  {
    return
      e1.getName().equals(e2.getName()) &&
        compareNamespaces(e1.getNamespaces(), e2.getNamespaces());
  }



  private static boolean
  compareEntityDeclaration(EntityDeclaration e1, EntityDeclaration e2)
  {
    // Don't compare base URI, because then one can't compare two documents.

    return
      e1.getName().equals(e2.getName()) &&
        compareWithNull(e1.getNotationName(), e2.getNotationName()) &&
        compareWithNull(e1.getPublicId(), e2.getPublicId()) &&
        compareWithNull(e1.getReplacementText(), e2.getReplacementText()) &&
        compareWithNull(e1.getSystemId(), e2.getSystemId());
  }



  private static boolean
  compareEntityReference(EntityReference e1, EntityReference e2)
  {
    return
      e1.getName().equals(e2.getName()) &&
        compareEntityDeclaration(e1.getDeclaration(), e2.getDeclaration());
  }



  private static boolean
  compareNamedOrderless(Iterator i1, Iterator i2, Method getName)
    throws Exception
  {
    Map	map = new HashMap();

    while (i1.hasNext())
    {
      XMLEvent	event = (XMLEvent) i1.next();

      map.put(getName.invoke(event, new Object[0]), event);
    }

    int	i;

    for (i = 0; i2.hasNext(); ++i)
    {
      XMLEvent	event = (XMLEvent) i2.next();
      XMLEvent	other =
        (XMLEvent) map.get(getName.invoke(event, new Object[0]));

      if (other == null || !equal(event, other))
      {
        return false;
      }
    }

    return i == map.size();
  }



  private static boolean
  compareNamespace(Namespace e1, Namespace e2)
  {
    return
      e1.getNamespaceURI().equals(e2.getNamespaceURI()) &&
        e1.getPrefix().equals(e2.getPrefix());
  }



  /**
   * Namespace declarations have no order.
   */

  private static boolean
  compareNamespaces(Iterator i1, Iterator i2) throws Exception
  {
    return
      compareNamedOrderless
      (
        i1,
        i2,
        Namespace.class.getMethod("getName", new Class[0])
      );
  }



  private static boolean
  compareNotationDeclaration(NotationDeclaration e1, NotationDeclaration e2)
  {
    return
      e1.getName().equals(e2.getName()) &&
        compareWithNull(e1.getPublicId(), e2.getPublicId()) &&
        compareWithNull(e1.getSystemId(), e2.getSystemId());
  }



  private static boolean
  compareProcessingInstruction
  (
    ProcessingInstruction	e1,
    ProcessingInstruction	e2
  )
  {
    return
      e1.getData().equals(e2.getData()) &&
        e1.getTarget().equals(e2.getTarget());
  }



  private static boolean
  compareStartElement(StartElement e1, StartElement e2) throws Exception
  {
    return
      e1.getName().equals(e2.getName()) &&
        compareAttributes(e1.getAttributes(), e2.getAttributes()) &&
        compareNamespaces(e1.getNamespaces(), e2.getNamespaces());
  }



  private static boolean
  compareWithNull(Object o1, Object o2)
  {
    return
      (o1 == null && o2 == null) || (o1 != null && o2 != null && o1.equals(o2));
  }



  public static boolean
  equal(XMLEvent e1, XMLEvent e2)
  {
    if (e1.getEventType() != e2.getEventType())
    {
      return false;
    }

    try
    {
      switch (e1.getEventType())
      {
        case XMLEvent.ATTRIBUTE:
          return compareAttribute((Attribute) e1, (Attribute) e2);

        case XMLEvent.CDATA: case XMLEvent.CHARACTERS: case XMLEvent.SPACE:
          return compareCharacters(e1.asCharacters(), e2.asCharacters());

        case XMLEvent.COMMENT:
          return compareComment((Comment) e1, (Comment) e2);

        case XMLEvent.DTD: return compareDTD((DTD) e1, (DTD) e2);

        case XMLEvent.END_ELEMENT:
          return compareEndElement(e1.asEndElement(), e2.asEndElement());

        case XMLEvent.ENTITY_DECLARATION:
          return
            compareEntityDeclaration
            (
              (EntityDeclaration) e1,
              (EntityDeclaration) e2
            );

        case XMLEvent.ENTITY_REFERENCE:
          return
            compareEntityReference
            (
              (EntityReference) e1,
              (EntityReference) e2
            );

        case XMLEvent.NAMESPACE:
          return compareNamespace((Namespace) e1, (Namespace) e2);

        case XMLEvent.NOTATION_DECLARATION:
          return
            compareNotationDeclaration
            (
              (NotationDeclaration) e1,
              (NotationDeclaration) e2
            );

        case XMLEvent.PROCESSING_INSTRUCTION:
          return
            compareProcessingInstruction
            (
              (ProcessingInstruction) e1,
              (ProcessingInstruction) e2
            );

        case XMLEvent.START_ELEMENT:
          return compareStartElement(e1.asStartElement(), e2.asStartElement());
      }

      return true;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

} // Event
