package be.re.xml.stax;

import java.io.Writer;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;



/**
 * @author Werner Donn\u00e9
 */

public class XMLEventBase implements XMLEvent

{

  public Characters
  asCharacters()
  {
    throw new ClassCastException();
  }



  public EndElement
  asEndElement()
  {
    throw new ClassCastException();
  }



  public StartElement
  asStartElement()
  {
    throw new ClassCastException();
  }



  public int
  getEventType()
  {
    return -1;
  }



  public Location
  getLocation()
  {
    return null;
  }



  public QName
  getSchemaType()
  {
    return null;
  }



  public boolean
  isAttribute()
  {
    return false;
  }



  public boolean
  isCharacters()
  {
    return false;
  }



  public boolean
  isEndDocument()
  {
    return false;
  }



  public boolean
  isEndElement()
  {
    return false;
  }



  public boolean
  isEntityReference()
  {
    return false;
  }



  public boolean
  isNamespace()
  {
    return false;
  }



  public boolean
  isProcessingInstruction()
  {
    return false;
  }



  public boolean
  isStartDocument()
  {
    return false;
  }



  public boolean
  isStartElement()
  {
    return false;
  }



  public void
  writeAsEncodedUnicode(Writer writer) throws XMLStreamException
  {
  }

} // XMLEventBase
