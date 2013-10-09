package be.re.xml.stax;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;



/**
 * An XMLEventReader wrapper around an XMLStreamReader.
 * @author Werner Donn\u00e9
 */

public class StreamEventReader implements XMLEventReader

{

  private List			buffer = new ArrayList();
  private Stack			namespaces = new Stack();
  private XMLStreamReader	reader;



  public
  StreamEventReader(XMLStreamReader reader)
  {
    this.reader = reader;
  }



  public void
  close() throws XMLStreamException
  {
    reader.close();
  }



  private void
  createAttribute() throws XMLStreamException
  {
    for (int i = 0; i < reader.getAttributeCount(); ++i)
    {
      buffer.add
      (
        Util.eventFactory.createAttribute
        (
          reader.getAttributeName(i),
          reader.getAttributeValue(i)
        )
      );
    }
  }



  private void
  createCData() throws XMLStreamException
  {
    buffer.add(Util.eventFactory.createCData(reader.getText()));
  }



  private void
  createCharacters() throws XMLStreamException
  {
    buffer.add(Util.eventFactory.createCharacters(reader.getText()));
  }



  private void
  createComment() throws XMLStreamException
  {
    buffer.add(Util.eventFactory.createComment(reader.getText()));
  }



  private void
  createDTD() throws XMLStreamException
  {
    buffer.add(Util.eventFactory.createDTD(reader.getText()));
  }



  private void
  createEndDocument() throws XMLStreamException
  {
    buffer.add(Util.eventFactory.createEndDocument());
  }



  private void
  createEndElement() throws XMLStreamException
  {
    buffer.add
    (
      Util.eventFactory.createEndElement(reader.getName(),
      ((List) namespaces.pop()).iterator())
    );
  }



  private void
  createEntityReference() throws XMLStreamException
  {
    buffer.add
    (
      Util.eventFactory.createEntityReference
      (
        reader.getLocalName(),
        new InternalEntityDeclaration(reader.getLocalName(), reader.getText())
      )
    );
  }



  private void
  createEvent(int type) throws XMLStreamException
  {
    switch (type)
    {
      case XMLStreamConstants.ATTRIBUTE:
        createAttribute();
        break;
      case XMLStreamConstants.CDATA:
        createCData();
        break;
      case XMLStreamConstants.CHARACTERS:
        createCharacters();
        break;
      case XMLStreamConstants.COMMENT:
        createComment();
        break;
      case XMLStreamConstants.DTD:
        createDTD();
        break;
      case XMLStreamConstants.END_DOCUMENT:
        createEndDocument();
        break;
      case XMLStreamConstants.END_ELEMENT:
        createEndElement();
        break;
      case XMLStreamConstants.ENTITY_REFERENCE:
        createEntityReference();
        break;
      case XMLStreamConstants.NAMESPACE: createNamespace();
        break;
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
        createProcessingInstruction();
        break;
      case XMLStreamConstants.SPACE: createSpace();
        break;
      case XMLStreamConstants.START_DOCUMENT:
        createStartDocument();
        break;
      case XMLStreamConstants.START_ELEMENT:
        createStartElement();
        break;
    }
  }



  private void
  createNamespace() throws XMLStreamException
  {
    List	inScope = (List) namespaces.peek();

    for (int i = 0; i < reader.getNamespaceCount(); ++i)
    {
      Namespace	namespace =
        Util.eventFactory.createNamespace
        (
          reader.getNamespacePrefix(i),
          reader.getNamespaceURI(i)
        );

      buffer.add(namespace);
      inScope.add(namespace);
    }
  }



  private void
  createProcessingInstruction() throws XMLStreamException
  {
    buffer.add
    (
      Util.eventFactory.
        createProcessingInstruction(reader.getPITarget(), reader.getPIData())
    );
  }



  private void
  createSpace() throws XMLStreamException
  {
    buffer.add(Util.eventFactory.createSpace(reader.getText()));
  }



  private void
  createStartDocument() throws XMLStreamException
  {
    buffer.add
    (
      Util.eventFactory.createStartDocument
      (
        reader.getEncoding(),
        reader.getVersion(),
        reader.isStandalone()
      )
    );
  }



  private void
  createStartElement() throws XMLStreamException
  {
    buffer.
      add(Util.eventFactory.createStartElement(reader.getName(), null, null));
    namespaces.push(new ArrayList());
    createAttribute();
    createNamespace();
  }



  public String
  getElementText() throws XMLStreamException
  {
    return reader.getElementText();
  }



  public Object
  getProperty(String name)
  {
    return reader.getProperty(name);
  }



  public boolean
  hasNext()
  {
    try
    {
      return buffer.size() > 0 || reader.hasNext();
    }

    catch (XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }



  public Object
  next()
  {
    try
    {
      return nextEvent();
    }

    catch (XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    XMLEvent	result = peek();

    buffer.remove(0);

    return result;
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    return Util.nextTag(this);
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    if (buffer.size() == 0)
    {
      createEvent(reader.next());
    }

    return (XMLEvent) buffer.get(0);
  }



  public void
  remove()
  {
  }

} // StreamEventReader
