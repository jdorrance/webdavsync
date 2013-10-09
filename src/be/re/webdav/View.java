package be.re.webdav;

import be.re.net.HTTPClient;
import be.re.net.ProtocolException;
import be.re.xml.ExpandedName;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * Utility to work with Pincette views.
 * @author Werner Donn\u00e9
 */

public class View

{

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



  /**
   * Returns the rules of view <code>url</code> in the order specified by the
   * view.
   */

  public static Rule[]
  getRules(URL url, Client client) throws IOException
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
        Util.throwException(response);
      }

      Document	document = response.createDocument();

      if (document == null)
      {
        return new Rule[0];
      }

      Util.checkPropstatStatus(url, HTTPClient.PROPFIND, document);

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
  isTimeView(URL view) throws IOException
  {
    try
    {
      Rule[]	rules = getRules(view, new Client());

      return
        rules.length == 1 && rules[0].time != null &&
          "latest".equals(rules[0].label);
    }

    catch (ProtocolException e)
    {
      if (e.getCode() == 404)
      {
        return false;
      }

      throw e;
    }
  }



  /**
   * Sets the rules of the view <code>url</code> in the provided order.
   */

  public static void
  setRules(URL url, Client client, Rule[] rules) throws Exception
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
      if (response.getStatusCode() != 200 && response.getStatusCode() != 207)
      {
        Util.throwException(response);
      }

      if (response.getStatusCode() == 207)
      {
        Util.checkPropstatStatus
        (
          url,
          HTTPClient.PROPPATCH,
          response.createDocument()
        );
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Represents one rule.
   */

  public static class Rule

  {

    /**
     * An activity is like a version branch. It is defined in
     * <a href="http://www.webdav.org/specs/rfc3253.html#activity.feature">RFC
     * 3253</a>.
     */

    public String	activity;

    /**
     * This is the activity that will be used when a resource is checked out.
     * This is how a branch can be created on another.
     */

    public String	checkOutActivity;

    /**
     * A label that can be set on a version of a resource. A view can use it to
     * select versions. Labels are defined in
     * <a href="http://www.webdav.org/specs/rfc3253.html#label.feature">RFC
     * 3253</a>.
     */

    public String	label;

    /**
     * An ISO timestamp that can be used to select versions. This should not be
     * combined with other selection means.
     */

    public String	time;

    /**
     * A collection starting from which this selection rule applies. Typically
     * more precise rules are placed before less precise ones, because the
     * rules of a view are evaluated in sequence.
     */

    public String	url;

    /**
     * The name of specific version, which can be useful when for some reason a
     * specific version of a resource is required. This would normally be a
     * temporary situation.
     */

    public String	version;



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
