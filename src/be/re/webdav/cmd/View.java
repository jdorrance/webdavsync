package be.re.webdav.cmd;

import be.re.webdav.Client;
import be.re.xml.ExpandedName;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * A view that can be displayed.
 * @author Werner Donn\u00e9
 */

public class View implements Comparable

{

  public String	displayName;
  public URL	url;



  public
  View(URL url, String displayName)
  {
    this.url = url;
    this.displayName = displayName == null ? "" : displayName;
  }



  private static void
  appendValue(Node rule, String name, String value)
  {
    if (value != null && !"".equals(value.trim()))
    {
      rule.appendChild
      (
        rule.getOwnerDocument().
          createElementNS(be.re.webdav.Constants.URI, name)
      ).appendChild(rule.getOwnerDocument().createTextNode(value.trim()));
    }
  }



  public int
  compareTo(Object o)
  {
    return toString().compareTo(o.toString());
  }



  public static Rule[]
  getRules(URL url, Client client) throws Exception
  {
    Client.Response	response =
      client.propfindSpecific
      (
        url,
        new ExpandedName[]
        {new ExpandedName(be.re.webdav.Constants.URI, "rule-set")},
        "0"
      );

    try
    {
      if (response.getStatusCode() != 207)
      {
        Util.report(url, response);

        return null;
      }

      Document	document = response.createDocument();

      if (document == null)
      {
        return new Rule[0];
      }

      if
      (
        !Util.checkPropstatStatus
        (
          url,
          be.re.xml.Util.selectElement
          (
            document.getDocumentElement(),
            new ExpandedName[]
            {
              new ExpandedName(Constants.DAV_URI, "response"),
              new ExpandedName(Constants.DAV_URI, "propstat")
            }
          )
        )
      )
      {
        return null;
      }

      Element	element =
        be.re.xml.Util.selectElement
        (
          document.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(be.re.webdav.Constants.URI, "rule-set")
          }
        );

      return element == null ? new Rule[0] : getRules(element);
    }

    finally
    {
      response.close();
    }
  }



  public static Rule[]
  getRules(Element ruleSet)
  {
    Element[]	elements =
      be.re.xml.Util.selectElements
      (
        ruleSet,
        new ExpandedName[]{new ExpandedName(be.re.webdav.Constants.URI, "rule")}
      );
    List	result = new ArrayList();

    for (int i = 0; i < elements.length; ++i)
    {
      result.add
      (
        new Rule
        (
          getValue(elements[i], "url"),
          getValue(elements[i], "activity"),
          getValue(elements[i], "label"),
          getValue(elements[i], "version"),
          getValue(elements[i], "time"),
          getValue(elements[i], "check-out-activity")
        )
      );
    }

    return (Rule[]) result.toArray(new Rule[0]);
  }



  private static String
  getValue(Node rule, String name)
  {
    String	result =
      be.re.xml.Util.getText
      (
        be.re.xml.Util.selectFirstChild(rule, be.re.webdav.Constants.URI, name)
      );

    return result == null ? null : result.trim();
  }



  public static boolean
  setRules(URL url, Client client, Rule[] rules) throws Exception
  {
    try
    {
      Document	document = Util.createDAVDocument("propertyupdate");
      Node	props =
        document.getDocumentElement().appendChild
        (
          document.createElementNS(Constants.DAV_URI, "set")
        ).appendChild(document.createElementNS(Constants.DAV_URI, "prop")).
          appendChild
          (
            document.createElementNS(be.re.webdav.Constants.URI, "rule-set")
          );

      ((Element) props).setAttribute("xmlns", be.re.webdav.Constants.URI);

      for (int i = 0; i < rules.length; ++i)
      {
        Node	rule =
          props.appendChild
          (
            document.createElementNS(be.re.webdav.Constants.URI, "rule")
          );

        appendValue(rule, "url", rules[i].url);
        appendValue(rule, "activity", rules[i].activity);
        appendValue(rule, "label", rules[i].label);
        appendValue(rule, "version", rules[i].version);
        appendValue(rule, "time", rules[i].time);
        appendValue(rule, "check-out-activity", rules[i].checkOutActivity);
      }

      Client.Response	response = client.proppatch(url, document);

      try
      {
        if (response.getStatusCode() != 200)
        {
          Util.report(url, response);

          return false;
        }

        return true;
      }

      finally
      {
        response.close();
      }
    }

    catch (Exception e)
    {
      Util.report(url, e);

      return false;
    }
  }



  public String
  toString()
  {
    return displayName;
  }



  public static class Rule

  {

    String	activity;
    String	checkOutActivity;
    String	label;
    String	time;
    String	url;
    String	version;



    public
    Rule
    (
      String	url,
      String	activity,
      String	label,
      String	version,
      String	time,
      String	checkOutActivity
    )
    {
      this.url = url;
      this.activity = activity;
      this.label = label;
      this.version = version;
      this.time = time;
      this.checkOutActivity = checkOutActivity;
    }

  } // Rule

} // View
