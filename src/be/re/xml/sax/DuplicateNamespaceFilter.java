package be.re.xml.sax;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;



/**
 * Filters out namespace declarations that are already in scope and translates
 * namespace declarations in attribute form into prefix mapping events.
 * @author Werner Donn\u00e9
 */

public class DuplicateNamespaceFilter extends XMLFilterImpl

{

  private Stack	elements = new Stack();



  public
  DuplicateNamespaceFilter()
  {
  }



  public
  DuplicateNamespaceFilter(XMLReader parent)
  {
    super(parent);
  }



  public void
  endDocument() throws SAXException
  {
    elements.pop();
    super.endDocument();
  }



  public void
  endElement(String namespaceURI, String localName, String qName)
    throws SAXException
  {
    Map	syntheticPrefixMap = ((Element) elements.pop()).syntheticPrefixMap;

    super.endElement(namespaceURI, localName, qName);

    for (Iterator i = syntheticPrefixMap.keySet().iterator(); i.hasNext();)
    {
      endPrefixMapping((String) i.next());
    }
  }



  public void
  endPrefixMapping(String prefix) throws SAXException
  {
    if (((Element) elements.peek()).prefixMap.remove(prefix) != null)
    {
      super.endPrefixMapping(prefix);
    }
  }



  public void
  startDocument() throws SAXException
  {
    elements.push(new Element());
      // An extra level because prefix mapping events come around an element.
    super.startDocument();
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
    AttributesImpl	newAtts = new AttributesImpl(atts);

    for (int i = 0; i < newAtts.getLength(); ++i)
    {
      String	name = newAtts.getQName(i);

      if (name.startsWith("xmlns"))
      {
        String	prefix =
          name.indexOf(':') != -1 ? name.substring(name.indexOf(':') + 1) : "";

        startPrefixMapping(prefix, newAtts.getValue(i));
        ((Element) elements.peek()).syntheticPrefixMap.
          put(prefix, newAtts.getValue(i));
        newAtts.removeAttribute(i--);
      }
    }

    elements.push(new Element());
    super.startElement(namespaceURI, localName, qName, newAtts);
  }



  public void
  startPrefixMapping(String prefix, String uri) throws SAXException
  {
    for (int i = elements.size() - 1; i >= 0; --i)
    {
      if (uri.equals(((Element) elements.get(i)).prefixMap.get(prefix)))
      {
        return;
      }
    }

    ((Element) elements.peek()).prefixMap.put(prefix, uri);
    super.startPrefixMapping(prefix, uri);
  }



  private static class Element

  {

    private Map	prefixMap = new HashMap();
    private Map	syntheticPrefixMap = new HashMap();

  } // Element

} // DuplicateNamespaceFilter
