package be.re.xml.sax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * This class is able to handle abbreviated syntax XPath expressions which
 * contain only path elements consisting of a non-empty qualified name followed
 * by an optional position specification using numbers only. The expression may
 * be relative. The local name may be "*".
 * @author Werner Donn\u00e9
 */

public class SimpleXPath

{

  /**
   * The <code>namespaceURIMap</code> maps URIs to prefixes. The default
   * namespace, if any, must be mapped to the empty string. The map may be
   * <code>null</code>. If <code>relative</code> is <code>true</code> the
   * expression will start with "/".
   */

  public static String
  getExpression(PathElement[] path, Map namespaceURIMap, boolean relative)
  {
    String	result = "";

    for (int i = 0; i < path.length; ++i)
    {
      result +=
        (result.equals("") ? "" : "/") +
          (
            path[i].getNamespaceURI() != null &&
              !"".equals
              (
                (String) namespaceURIMap.get(path[i].getNamespaceURI())
              ) ?
                (
                  (String) namespaceURIMap.get(path[i].getNamespaceURI()) +
                    ":"
                ) : ""
          ) +
          path[i].getLocalName() +
          (
            path[i].getPosition() != -1 ?
              ("[" + String.valueOf(path[i].getPosition()) + "]") : ""
          );
    }

    return (relative ? "" : "/") + result;
  }



  public static Element[]
  getElementSet(Node contextNode, String expression, Map namespacePrefixMap)
  {
    return
      getElementSet
      (
        expression.charAt(0) == '/' ?
          contextNode.getOwnerDocument() : contextNode,
        getPath(expression, namespacePrefixMap)
      );
  }



  /**
   * The <code>path</code> is relative to the <code>contextNode</code>.
   */

  public static Element[]
  getElementSet(Node contextNode, PathElement[] path)
  {
    List	result = new ArrayList();

    getElementSet(contextNode, path, 0, result);

    return (Element[]) result.toArray(new Element[0]);
  }



  private static void
  getElementSet(Node contextNode, PathElement[] path, int offset, List list)
  {
    int		currentPosition = 0;

    for
    (
      Node n = contextNode.getFirstChild();
      n != null &&
        (
          path[offset].getPosition() == -1 ||
          currentPosition < path[offset].getPosition()
        );
      n = n.getNextSibling()
    )
    {
      if
      (
        n instanceof Element						&&
        (
          "*".equals(path[offset].getLocalName())			||
          path[offset].getLocalName().equals(n.getLocalName())
        )								&&
        (
          (
            path[offset].getNamespaceURI() == null			&&
            n.getNamespaceURI() == null
          )								||
          (
            path[offset].getNamespaceURI() != null			&&
            path[offset].getNamespaceURI().equals(n.getNamespaceURI())
          )
        )
      )
      {
        if
        (
          path[offset].getPosition() == -1			||
          ++currentPosition == path[offset].getPosition()
        )
        {
          if (offset == path.length - 1)
          {
            list.add((Element) n);
          }
          else
          {
            getElementSet(n, path, offset + 1, list);
          }
        }
      }
    }
  }



  /**
   * The <code>namespacePrefixMap</code> maps prefixes to URIs, which are
   * supposed to be strings. It may be <code>null</code>. The key of
   * the default namespace is the empty string.
   */

  public static PathElement[]
  getPath(String expression, Map namespacePrefixMap)
  {
    List		result = new ArrayList();
    StringTokenizer	tokenizer = new StringTokenizer(expression, "/");

    while (tokenizer.hasMoreTokens())
    {
      String	token = tokenizer.nextToken();
      int	localNameStart =
        token.indexOf(':') != -1 ? (token.indexOf(':') + 1) : 0;
      int	positionStart = token.indexOf('[');

      result.add
      (
        new PathElement
        (
          localNameStart > 0 ?
            (String)
              namespacePrefixMap.get(token.substring(0, localNameStart - 1)) :
            (String) namespacePrefixMap.get(""),
          token.substring
          (
            localNameStart,
            positionStart != -1 ? positionStart : token.length()
          ),
          positionStart != -1 ?
            Integer.
              parseInt(token.substring(positionStart + 1, token.length() - 1)) :
            -1
        )
      );
    }

    return (PathElement[]) result.toArray(new PathElement[0]);
  }



  public static PathElement[]
  getPath(Element element)
  {
    List	result = new ArrayList();

    getPath(element, result);

    return (PathElement[]) result.toArray(new PathElement[0]);
  }



  private static void
  getPath(Element element, List list)
  {
    list.add
    (
      0,
      new PathElement
      (
        element.getNamespaceURI(),
        element.getLocalName(),
        getPosition(element)
      )
    );

    if
    (
      element.getParentNode() != null			&&
      element.getParentNode() instanceof Element
    )
    {
      getPath((Element) element.getParentNode(), list);
    }
  }



  private static int
  getPosition(Element element)
  {
    return
      element == null ?
        0 :
        (
          getPosition
          (
            be.re.xml.Util.getPreviousSiblingElement
            (
              element.getPreviousSibling(),
              element.getNamespaceURI(),
              element.getLocalName()
            )
          ) + 1
        );
  }



  public static class PathElement

  {

    private String	localName;
    private String	namespaceURI;
    private int		position;



    /**
     * The <code>namespaceURI</code> may be <code>null</code>. The
     * <code>localName</code> must not be <code>null</code>. The position
     * must be positive or -1, indicating absence.
     */

    public
    PathElement(String namespaceURI, String localName, int position)
    {
      if (position < -1)
      {
        throw new IllegalArgumentException(String.valueOf(position));
      }

      if (localName == null)
      {
        throw new NullPointerException();
      }

      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.position = position;
    }



    public String
    getLocalName()
    {
      return localName;
    }



    public String
    getNamespaceURI()
    {
      return namespaceURI;
    }



    public int
    getPosition()
    {
      return position;
    }
  }

} // SimpleXPath
