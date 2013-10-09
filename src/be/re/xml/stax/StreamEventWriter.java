package be.re.xml.stax;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.NotationDeclaration;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;



/**
 * An XMLEventWriter wrapper around an XMLStreamWriter.
 * @author Werner Donn\u00e9
 */

public class StreamEventWriter implements XMLEventWriter

{

  private XMLStreamWriter	stream;



  public
  StreamEventWriter(XMLStreamWriter stream)
  {
    this.stream = stream;
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
        writeDTD((DTD) event);
        break;
      case XMLStreamConstants.END_DOCUMENT:
        writeEndDocument((EndDocument) event);
        break;
      case XMLStreamConstants.END_ELEMENT:
        writeEndElement((EndElement) event);
        break;
      case XMLStreamConstants.ENTITY_DECLARATION:
        writeEntityDeclaration((EntityDeclaration) event);
        break;
      case XMLStreamConstants.ENTITY_REFERENCE:
        writeEntityReference((EntityReference) event);
        break;
      case XMLStreamConstants.NAMESPACE:
        writeNamespace((Namespace) event);
        break;
      case XMLStreamConstants.NOTATION_DECLARATION:
        writeNotationDeclaration((NotationDeclaration) event);
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
    while(reader.hasNext())
    {
      add(reader.nextEvent());
    }
  }



  public void
  close() throws XMLStreamException
  {
    stream.close();
  }



  public void
  flush() throws XMLStreamException
  {
    stream.flush();
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return stream.getNamespaceContext();
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return stream.getPrefix(uri);
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    stream.setDefaultNamespace(uri);
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    stream.setNamespaceContext(context);
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    stream.setPrefix(prefix, uri);
  }



  private void
  writeAttribute(Attribute event) throws XMLStreamException
  {
    stream.writeAttribute
    (
      event.getName().getPrefix(),
      event.getName().getNamespaceURI(),
      event.getName().getLocalPart(),
      event.getValue()
    );
  }



  private void
  writeCharacters(Characters event) throws XMLStreamException
  {
    if (event.isCData())
    {
      stream.writeCData(event.getData());
    }
    else
    {
      stream.writeCharacters(event.getData());
    }
  }



  private void
  writeComment(Comment event) throws XMLStreamException
  {
    stream.writeComment(event.getText());
  }



  private void
  writeDTD(DTD event) throws XMLStreamException
  {
    stream.writeDTD(event.getDocumentTypeDeclaration());
  }



  private void
  writeEndDocument(EndDocument event) throws XMLStreamException
  {
    stream.writeEndDocument();
  }



  private void
  writeEndElement(EndElement event) throws XMLStreamException
  {
    stream.writeEndElement();
  }



  private void
  writeEntityDeclaration(EntityDeclaration event) throws XMLStreamException
  {
  }



  private void
  writeEntityReference(EntityReference event) throws XMLStreamException
  {
    stream.writeEntityRef(event.getName());
  }



  private void
  writeNamespace(Namespace event) throws XMLStreamException
  {
    stream.writeNamespace(event.getPrefix(), event.getNamespaceURI());
  }



  private void
  writeNotationDeclaration(NotationDeclaration event) throws XMLStreamException
  {
  }



  private void
  writeProcessingInstruction(ProcessingInstruction event)
    throws XMLStreamException
  {
    stream.writeProcessingInstruction(event.getTarget(), event.getData());
  }



  private void
  writeStartDocument(StartDocument event) throws XMLStreamException
  {
    stream.writeStartDocument
    (
      event.getCharacterEncodingScheme(),
      event.getVersion()
    );
  }



  private void
  writeStartElement(StartElement event) throws XMLStreamException
  {
    stream.writeStartElement
    (
      event.getName().getNamespaceURI(),
      event.getName().getLocalPart()
    );

    for (Iterator i = event.getNamespaces(); i.hasNext();)
    {
      writeNamespace((Namespace) i.next());
    }

    for (Iterator i = event.getAttributes(); i.hasNext();)
    {
      writeAttribute((Attribute) i.next());
    }
  }

} // StreamEventWriter
