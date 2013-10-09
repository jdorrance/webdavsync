package be.re.xml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;



/**
 * This class copes with the fact that the same namespace declarations can be
 * nested and that prefixes can be redeclared for other URIs.
 * @author Werner Donn\u00e9
 */

public class NamespacePrefixMap implements NamespaceContext

{

  private Map	namespaceMap = new HashMap();
  private Map	prefixMap = new HashMap();



  public void
  endPrefixMapping(String prefix)
  {
    Stack	stack = (Stack) prefixMap.get(prefix);

    if (stack != null)
    {
      unmap(namespaceMap, (String) stack.peek());
      unmap(prefixMap, prefix);
    }
  }



  public Map
  getCurrentPrefixMap()
  {
    Map	result = new HashMap();

    for (Iterator i = prefixMap.keySet().iterator(); i.hasNext();)
    {
      String	prefix = (String) i.next();

      result.put(prefix, ((Stack) prefixMap.get(prefix)).peek());
    }

    return result;
  }



  /**
   * Returns the most recent namespace prefix which is associated with
   * <code>uri</code> or <code>null</code> if there isn't any.
   */

  public String
  getNamespacePrefix(String uri)
  {
    if (uri == null || "".equals(uri))
    {
      throw new IllegalArgumentException();
    }

    return
      XMLConstants.XML_NS_URI.equals(uri) ?
        XMLConstants.XML_NS_PREFIX :
        (
          XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri) ?
            XMLConstants.XMLNS_ATTRIBUTE :
            (
              namespaceMap.get(uri) != null ?
                (String) ((Stack) namespaceMap.get(uri)).peek() : null
            )
        );
  }



  /**
   * Returns <code>null</code> if there is no mapping.
   */

  public String
  getNamespaceURI(String prefix)
  {
    return
      XMLConstants.XML_NS_PREFIX.equals(prefix) ?
        XMLConstants.XML_NS_URI :
        (
          XMLConstants.XMLNS_ATTRIBUTE.equals(prefix) ?
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI :
            (
              prefixMap.get(prefix) != null ?
                (String) ((Stack) prefixMap.get(prefix)).peek() : null
            )
        );
  }



  public String
  getPrefix(String namespaceURI)
  {
    return getNamespacePrefix(namespaceURI);
  }



  public Iterator
  getPrefixes(String namespaceURI)
  {
    if (namespaceURI == null || "".equals(namespaceURI))
    {
      throw new IllegalArgumentException();
    }

    final Iterator	i =
      XMLConstants.XML_NS_URI.equals(namespaceURI) ?
        Arrays.asList(new Object[]{XMLConstants.XML_NS_PREFIX}).iterator() :
        (
          XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI) ?
            Arrays.asList(new Object[]{XMLConstants.XMLNS_ATTRIBUTE}).
              iterator() :
            (
              namespaceMap.get(namespaceURI) != null ?
                ((Stack) namespaceMap.get(namespaceURI)).iterator() :
                Arrays.asList(new Object[0]).iterator()
            )
        );

    return
      new Iterator()
      {
        public boolean
        hasNext()
        {
          return i.hasNext();
        }

        public Object
        next()
        {
          return i.next();
        }

        public void
        remove()
        {
          throw new UnsupportedOperationException();
        }
      };
  }



  private static void
  map(Map mapping, String from, String to)
  {
    Stack	stack = (Stack) mapping.get(from);

    if (stack == null)
    {
      stack = new Stack();
      mapping.put(from, stack);
    }

    stack.push(to);
  }



  public void
  startPrefixMapping(String prefix, String uri)
  {
    map(prefixMap, prefix, uri);
    map(namespaceMap, uri, prefix);
  }



  private static void
  unmap(Map mapping, String from)
  {
    Stack	stack = (Stack) mapping.get(from);

    if (stack != null)
    {
      stack.pop();

      if (stack.isEmpty())
      {
        mapping.remove(from);
      }
    }
  }

} // NamespacePrefixMap
