package be.re.xml.stax;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;



/**
 * Writes nothing.
 * @author Werner Donn\u00e9
 */

public class DevNullStreamWriter implements XMLStreamWriter

{

  public void
  close() throws XMLStreamException
  {
  }



  public void
  flush() throws XMLStreamException
  {
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



  public Object
  getProperty(String name)
  {
    return null;
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
  }



  public void
  setNamespaceContext(NamespaceContext context)
  {
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
  }



  public void
  writeAttribute(String localName, String value) throws XMLStreamException
  {
  }



  public void
  writeAttribute(String namespaceURI, String localName, String value)
    throws XMLStreamException
  {
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
  }



  public void
  writeCData(String data) throws XMLStreamException
  {
  }



  public void
  writeCharacters(char[] text, int start, int len) throws XMLStreamException
  {
  }



  public void
  writeCharacters(String text) throws XMLStreamException
  {
  }



  public void
  writeComment(String data) throws XMLStreamException
  {
  }



  public void
  writeDefaultNamespace(String namespaceURI) throws XMLStreamException
  {
  }



  public void
  writeDTD(String dtd) throws XMLStreamException
  {
  }



  public void
  writeEmptyElement(String localName) throws XMLStreamException
  {
  }



  public void
  writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
  }



  public void
  writeEmptyElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
  }



  public void
  writeEndDocument() throws XMLStreamException
  {
  }



  public void
  writeEndElement()
  {
  }



  public void
  writeEntityRef(String name) throws XMLStreamException
  {
  }



  public void
  writeNamespace(String prefix, String namespaceURI) throws XMLStreamException
  {
  }



  public void
  writeProcessingInstruction(String target) throws XMLStreamException
  {
  }



  public void
  writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
  }



  public void
  writeStartDocument() throws XMLStreamException
  {
  }



  public void
  writeStartDocument(String version) throws XMLStreamException
  {
  }



  public void
  writeStartDocument(String encoding, String version) throws XMLStreamException
  {
  }



  public void
  writeStartElement(String localName) throws XMLStreamException
  {
  }



  public void
  writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
  }



  public void
  writeStartElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
  }

} // DevNullStreamWriter
