package be.re.xml.sax;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;



/**
 * This SAX handler writes out the events.
 * @author Werner Donn\u00e9
 */

public class Tracer extends XMLFilterImpl

{

  private PrintWriter	out;
  private int		indent = 0;



  public
  Tracer(OutputStream out)
  {
    this.out = new PrintWriter(out);
  }



  public void
  characters(char[] ch, int start, int length) throws SAXException
  {
    out.println(getIndent() + "characters: " + new String(ch, start, length));
  }



  public void
  endDocument() throws SAXException
  {
    indent -= 2;
    out.println(getIndent() + "endDocument");
  }



  public void
  endElement(String namespaceURI, String localName, String qName)
    throws SAXException
  {
    indent -= 2;

    out.println
    (
      getIndent() + "endElement: " + namespaceURI + ", " + localName + ", " +
        qName
    );
  }



  public void
  endPrefixMapping(String prefix) throws SAXException
  {
    out.println(getIndent() + "endPrefixMapping: " + prefix);
  }



  private String
  getIndent()
  {
    char[]	buffer = new char[indent];

    Arrays.fill(buffer, ' ');

    return new String(buffer);
  }



  public void
  ignorableWhitespace(char[] ch, int start, int length) throws SAXException
  {
    out.println
    (
      getIndent() + "ignorableWhitespace: " + new String(ch, start, length)
    );
  }



  public void
  processingInstruction(String target, String data) throws SAXException
  {
    out.println(getIndent() + "processingInstruction: " + target + ", " + data);
  }



  public void
  setDocumentLocator(Locator locator)
  {
    out.println
    (
      getIndent() + "setDocumentLocator: " + locator.getPublicId() + ", " +
        locator.getSystemId() + ", " + String.valueOf(locator.getLineNumber()) +
        ", " + String.valueOf(locator.getColumnNumber())
    );
  }



  public void
  skippedEntity(String name) throws SAXException
  {
    out.println(getIndent() + "skippedEntity: " + name);
  }



  public void
  startDocument() throws SAXException
  {
    out.println(getIndent() + "startDocument");
    indent += 2;
  }



  public void
  startElement
  (
    String	namespaceURI,
    String	localName,
    String	qName,
    Attributes	atts
  ) throws SAXException
  {
    out.print
    (
      getIndent() + "startElement: " + namespaceURI + ", " + localName + ", " +
        qName + ", atts:"
    );

    for (int i = 0; i < atts.getLength(); ++i)
    {
      out.print(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");
    }

    out.println();
    indent += 2;
  }



  public void
  startPrefixMapping(String prefix, String uri) throws SAXException
  {
    out.println(getIndent() + "startPrefixMapping: " + prefix + ", " + uri);
  }

} // Tracer
