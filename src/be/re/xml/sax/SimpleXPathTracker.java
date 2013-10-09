package be.re.xml.sax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;



/**
 * A helper for XML filters. It can give SimpleXPath at any position.
 * @author Werner Donn\u00e9
 */

public class SimpleXPathTracker

{

  private Stack				elements = new Stack();



  private static int
  countElements(Element element, List list)
  {
    int	result = 0;

    for (Iterator i = list.iterator(); i.hasNext();)
    {
      Element	e = (Element) i.next();

      if
      (
        element.namespaceURI.equals(e.namespaceURI)	&&
        element.localName.equals(e.localName)
      )
      {
        ++result;
      }
    }

    return result;
  }



  public SimpleXPath.PathElement[]
  getXPath()
  {
    List	result = new ArrayList();

    for (int i = elements.size() - 1; i > 0; --i)
    {
      Element	element = (Element) elements.get(i);

      result.add
      (
        0,
        new SimpleXPath.PathElement
        (
          element.namespaceURI,
          element.localName,
          countElements(element, ((Element) elements.get(i - 1)).children)
        )
      );
    }

    if (!elements.empty())
    {
      Element	root = (Element) elements.get(0);

      result.add
      (
        0,
        new SimpleXPath.PathElement(root.namespaceURI, root.localName, 1)
      );
    }

    return
      (SimpleXPath.PathElement[])
        result.toArray(new SimpleXPath.PathElement[0]);
  }



  public void
  pop()
  {
    elements.pop();
  }



  public void
  push(String namespaceURI, String localName)
  {
    Element	element = new Element(namespaceURI, localName);
    Element	parent = elements.empty() ? null : (Element) elements.peek();

    if (parent != null)
    {
      parent.children.add(element);
    }

    elements.push(element);
  }



  private static class Element

  {

    private List	children = new ArrayList();
    private String	localName;
    private String	namespaceURI;



    private
    Element(String namespaceURI, String localName)
    {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
    }

  } // Element

} // SimpleXPathTracker
