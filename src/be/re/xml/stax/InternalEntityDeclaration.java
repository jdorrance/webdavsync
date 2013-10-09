package be.re.xml.stax;

import java.io.IOException;
import java.io.Writer;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityDeclaration;



/**
 * @author Werner Donn\u00e9
 */

public class InternalEntityDeclaration extends XMLEventBase
  implements EntityDeclaration

{

  private String	name;
  private String	text;



  public
  InternalEntityDeclaration(String name, String text)
  {
    this.name = name;
    this.text = text;
  }



  private static String
  escapeDoubleQuotes(String s)
  {
    return s.replaceAll("\"", "&quot;");
  }



  public String
  getBaseURI()
  {
    return null;
  }



  public int
  getEventType()
  {
    return XMLStreamConstants.ENTITY_DECLARATION;
  }



  public String
  getName()
  {
    return name;
  }



  public String
  getNotationName()
  {
    return null;
  }



  public String
  getPublicId()
  {
    return null;
  }



  public String
  getReplacementText()
  {
    return text;
  }



  public String
  getSystemId()
  {
    return null;
  }



  public void
  writeAsEncodedUnicode(Writer writer) throws XMLStreamException
  {
    try
    {
      writer.write
      (
        "<!ENTITY " + getName() + " \"" +
          escapeDoubleQuotes(getReplacementText()) + "\">"
      );
    }

    catch (IOException e)
    {
      throw new XMLStreamException(e);
    }
  }

} // InternalEntityDeclaration
