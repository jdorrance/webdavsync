package be.re.gui.form;

import java.util.ResourceBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * @author Werner Donn\u00e9
 */

public class Util

{

  private static ResourceBundle	bundle = null;



  public static Option[]
  createOptions(Object[] values)
  {
    Option[]	result = new Option[values.length];

    for (int i = 0; i < values.length; ++i)
    {
      result[i] = new Option(values[i].toString(), null, false);
    }

    return result;
  }



  public static String
  getResource(String name)
  {
    if (bundle == null)
    {
      bundle = ResourceBundle.getBundle("be.re.gui.form.res.Res");
    }

    return bundle.getString(name);
  }



  public static void
  populateSelection(Document form, String field, Option[] options)
  {
    populateSelection
    (
      be.re.xml.Util.selectElements(form.getDocumentElement()),
      field,
      options
    );
  }



  private static void
  populateSelection(Element[] elements, String field, Option[] options)
  {
    for (int i = 0; i < elements.length; ++i)
    {
      if
      (
        "select".equals(elements[i].getLocalName())	&&
        field.equals(elements[i].getAttribute("name"))
      )
      {
        for (int j = 0; j < options.length; ++j)
        {
          Element	option =
            elements[i].getOwnerDocument().
              createElementNS("http://www.w3.org/1999/xhtml", "option");

          if (options[j].selected)
          {
            option.setAttribute("selected", "true");
          }

          elements[i].appendChild(option).
            appendChild
            (
              elements[i].getOwnerDocument().createTextNode
              (
                options[j].label != null ?
                  options[j].label : options[j].value.toString()
              )
            );

          option.setAttribute("value", options[j].value.toString());
        }
      }

      populateSelection
      (
        be.re.xml.Util.selectElements(elements[i]),
        field,
        options
      );
    }
  }



  public static void
  setField(Document form, String field, String value)
  {
    setField(form.getDocumentElement().getFirstChild(), field, value);
  }



  private static void
  setField(Node node, String field, String value)
  {
    if (node == null)
    {
      return;
    }

    if
    (
      node instanceof Element					&&
      field.equals(((Element) node).getAttribute("name"))
    )
    {
      if
      (
        "input".equals(node.getLocalName())			&&
        "radio".equals(((Element) node).getAttribute("type"))
      )
      {
        ((Element) node).setAttribute
        (
          "checked",
          value.equals(((Element) node).getAttribute("value")) ?
            "true" : "false"
        );
      }
      else
      {
        ((Element) node).setAttribute("value", value);
      }
    }

    setField(node.getFirstChild(), field, value);
    setField(node.getNextSibling(), field, value);
  }

} // Util
