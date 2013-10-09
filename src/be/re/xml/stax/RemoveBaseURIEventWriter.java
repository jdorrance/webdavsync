package be.re.xml.stax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;



/**
 * Removes the xml:base attribute if <code>baseURI</code> is not
 * <code>null</code>.
 * @author Werner Donn\u00e9
 */

public class RemoveBaseURIEventWriter extends EventWriterDelegate

{

  private String	baseURI;



  public
  RemoveBaseURIEventWriter(String baseURI)
  {
    this(baseURI, null);
  }



  public
  RemoveBaseURIEventWriter(String baseURI, XMLEventWriter writer)
  {
    super(writer);
    this.baseURI = baseURI;
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    if (baseURI == null)
    {
      getParent().add(event);
    }
    else
    {
      if (event.isStartElement())
      {
        List	attributes = new ArrayList();

        for (Iterator i = event.asStartElement().getAttributes(); i.hasNext();)
        {
          Attribute	attribute = (Attribute) i.next();

          if (!shouldRemove(attribute, baseURI))
          {
            attributes.add(attribute);
          }
        }

        getParent().add
        (
          Util.eventFactory.createStartElement
          (
            event.asStartElement().getName(),
            attributes.iterator(),
            event.asStartElement().getNamespaces()
          )
        );
      }
      else
      {
        if (!event.isAttribute() || !shouldRemove((Attribute) event, baseURI))
        {
          getParent().add(event);
        }
      }
    }
  }



  private static boolean
  shouldRemove(Attribute event, String baseURI)
  {
    return
      ((Attribute) event).getName().
        equals(new QName(XMLConstants.XML_NS_URI, "base")) &&
        baseURI.equals(((Attribute) event).getValue());
  }

} // RemoveBaseURIEventWriter
