package be.re.xml.stax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EntityDeclaration;



/**
 * An XMLStreamWriter wrapper around an XMLEventWriter.
 * @author Werner Donn\u00e9
 */

public class EventStreamWriter implements XMLStreamWriter

{

  private Stack			elements = new Stack();
  private Element		pendingElement;
  private XMLEventWriter	writer;



  public
  EventStreamWriter(XMLEventWriter writer)
  {
    this.writer = writer;
  }



  public void
  close() throws XMLStreamException
  {
    writer.close();
  }



  public void
  flush() throws XMLStreamException
  {
    writer.flush();
  }



  private void
  flushPendingStartElement() throws XMLStreamException
  {
    if (pendingElement != null)
    {
      writer.add
      (
        Util.eventFactory.createStartElement
        (
          pendingElement.name,
          pendingElement.attributes.iterator(),
          pendingElement.namespaces.iterator()
        )
      );

      if (pendingElement.empty)
      {
        writer.add
        (
          Util.eventFactory.createEndElement
          (
            pendingElement.name,
            pendingElement.namespaces.iterator()
          )
        );
      }

      pendingElement = null;
    }
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return writer.getNamespaceContext();
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return writer.getPrefix(uri);
  }



  public Object
  getProperty(String name)
  {
    return null;
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    writer.setDefaultNamespace(uri);
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    writer.setNamespaceContext(context);
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    writer.setPrefix(prefix, uri);
  }



  public void
  writeAttribute(String localName, String value) throws XMLStreamException
  {
    pendingElement.attributes.
      add(Util.eventFactory.createAttribute(localName, value));
  }



  public void
  writeAttribute(String namespaceURI, String localName, String value)
    throws XMLStreamException
  {
    pendingElement.attributes.add
    (
      Util.eventFactory.createAttribute("", namespaceURI, localName, value)
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
    pendingElement.attributes.add
    (
      Util.eventFactory.createAttribute(prefix, namespaceURI, localName, value)
    );
  }



  public void
  writeCData(String data) throws XMLStreamException
  {
    flushPendingStartElement();
    writer.add(Util.eventFactory.createCData(data));
  }



  public void
  writeCharacters(char[] text, int start, int len) throws XMLStreamException
  {
    flushPendingStartElement();
    writer.
      add(Util.eventFactory.createCharacters(new String(text, start, len)));
  }



  public void
  writeCharacters(String text) throws XMLStreamException
  {
    flushPendingStartElement();
    writer.add(Util.eventFactory.createCharacters(text));
  }



  public void
  writeComment(String data) throws XMLStreamException
  {
    flushPendingStartElement();
    writer.add(Util.eventFactory.createComment(data));
  }



  public void
  writeDefaultNamespace(String namespaceURI) throws XMLStreamException
  {
    pendingElement.namespaces.
      add(Util.eventFactory.createNamespace(namespaceURI));
  }



  public void
  writeDTD(String dtd) throws XMLStreamException
  {
    writer.add(Util.eventFactory.createDTD(dtd));
  }



  public void
  writeEmptyElement(String localName) throws XMLStreamException
  {
    flushPendingStartElement();
    pendingElement = new Element(new QName(localName));
    pendingElement.empty = true;
  }



  public void
  writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName));
    pendingElement.empty = true;
  }



  public void
  writeEmptyElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName, prefix));
    pendingElement.empty = true;
  }



  public void
  writeEndDocument() throws XMLStreamException
  {
    writer.add(Util.eventFactory.createEndDocument());
  }



  public void
  writeEndElement() throws XMLStreamException
  {
    flushPendingStartElement();

    Element	element = (Element) elements.pop();

    writer.add
    (
      Util.eventFactory.
        createEndElement(element.name, element.namespaces.iterator())
    );
  }



  public void
  writeEntityRef(String name) throws XMLStreamException
  {
    flushPendingStartElement();
    writer.add(Util.eventFactory.createEntityReference(name, null));
  }



  public void
  writeNamespace(String prefix, String namespaceURI) throws XMLStreamException
  {
    pendingElement.namespaces.
      add(Util.eventFactory.createNamespace(prefix, namespaceURI));
  }



  public void
  writeProcessingInstruction(String target) throws XMLStreamException
  {
    flushPendingStartElement();
    writer.add(Util.eventFactory.createProcessingInstruction(target, null));
  }



  public void
  writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    flushPendingStartElement();
    writer.add(Util.eventFactory.createProcessingInstruction(target, data));
  }



  public void
  writeStartDocument() throws XMLStreamException
  {
    writer.add(Util.eventFactory.createStartDocument());
  }



  public void
  writeStartDocument(String version) throws XMLStreamException
  {
    writer.add(Util.eventFactory.createStartDocument("UTF-8", version));
  }



  public void
  writeStartDocument(String encoding, String version) throws XMLStreamException
  {
    writer.add(Util.eventFactory.createStartDocument(encoding, version));
  }



  public void
  writeStartElement(String localName) throws XMLStreamException
  {
    flushPendingStartElement();
    pendingElement = new Element(new QName(localName));
    elements.push(pendingElement);
  }



  public void
  writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName));
    elements.push(pendingElement);
  }



  public void
  writeStartElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName, prefix));
    elements.push(pendingElement);
  }



  private static class Element

  {

    private boolean	empty;
    private QName	name;
    private List	attributes = new ArrayList();
    private List	namespaces = new ArrayList();



    private
    Element(QName name)
    {
      this.name = name;
    }

  } // Element

} // EventStreamWriter
