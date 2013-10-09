package be.re.webdav;

import be.re.net.HTTPClient;
import be.re.net.ProtocolException;
import be.re.xml.ExpandedName;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.net.URL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * Gets and sets dead WebDAV properties (see section 3 of <a
 * href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>) with a name/value
 * structure. The properties <code>properties</code> and <code>meta</code> in
 * the respective namespaces <code>urn:be-re:dav-cmd</code> and
 * <code>urn:be-re:dav</code> are supported. The former is for activities and
 * views, a Pincette-specific type of resource. The latter is for document meta
 * data.
 * @author Werner Donn\u00e9
 */

public class Properties

{

  /**
   * Duplicate properties will not be returned.
   */

  public static Property[]
  get(URL url, Client client) throws IOException, ProtocolException
  {
    return
      (Property[])
        get
        (
          url,
          client,
          Util.isActivity(url) || Util.isView(url) ?
            new ExpandedName(be.re.webdav.cmd.Constants.URI, "meta") :
            new ExpandedName(Constants.URI, "meta")
        ).toArray(new Property[0]);
  }



  private static Set
  get(URL url, Client client, ExpandedName element)
    throws IOException, ProtocolException
  {
    Client.Response	response = null;

    try
    {
      response = client.propfindSpecific(url, new ExpandedName[]{element}, "0");

      if (response.getStatusCode() != 207)
      {
        Util.throwException(response);
      }

      Document	document = response.createDocument();

      if (document == null)
      {
        return new HashSet();
      }

      Util.checkPropstatStatus(url, HTTPClient.PROPFIND, document);

      Element[]	elements =
        be.re.xml.Util.selectElements
        (
          document.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            element,
            new ExpandedName(element.namespaceURI, "prop")
          }
        );
      Set	result = new HashSet();

      for (int i = 0; i < elements.length; ++i)
      {
        Node[]	name =
          be.re.xml.Util.
            selectChildren(elements[i], element.namespaceURI, "name");
        Node[]	value =
          be.re.xml.Util.
            selectChildren(elements[i], element.namespaceURI, "value");

        if (name.length == 1 && value.length == 1)
        {
          result.add
          (
            new Property
            (
              be.re.xml.Util.getText(name[0]),
              be.re.xml.Util.getText(value[0])
            )
          );
        }
      }

      return result;
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }
    }
  }



  /**
   * Duplicate properties will not be stored.
   */

  public static void
  set(URL url, Client client, Property[] properties)
    throws IOException, ProtocolException
  {
    set
    (
      url,
      client,
      new HashSet(Arrays.asList(properties)),
      Util.isActivity(url) || Util.isView(url) ?
        new ExpandedName(be.re.webdav.cmd.Constants.URI, "meta") :
        new ExpandedName(Constants.URI, "meta")
    );
  }



  private static void
  set
  (
    URL			url,
    Client		client,
    Set			properties,
    ExpandedName	element
  ) throws IOException, ProtocolException
  {
    Document	document = Util.createDAVDocument("propertyupdate");
    Node	props =
      document.getDocumentElement().appendChild
      (
        document.createElementNS(Constants.DAV_URI, "set")
      ).appendChild(document.createElementNS(Constants.DAV_URI, "prop")).
        appendChild
        (
          document.createElementNS(element.namespaceURI, element.localName)
        );

    ((Element) props).setAttribute("xmlns", element.namespaceURI);

    for (Iterator i = properties.iterator(); i.hasNext();)
    {
      Property	property = (Property) i.next();
      Node	prop =
        props.
          appendChild(document.createElementNS(element.namespaceURI, "prop"));

      prop.
        appendChild(document.createElementNS(element.namespaceURI, "name")).
        appendChild(document.createTextNode(property.name));
      prop.
        appendChild(document.createElementNS(element.namespaceURI, "value")).
        appendChild(document.createTextNode(property.value));
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
   * Represents one property.
   */

  public static class Property

  {

    public String	name;
    public String	value;



    public
    Property(String name, String value)
    {
      this.name = name;
      this.value = value;
    }



    /**
     * A property equals another if the names and values are equal.
     */

    public boolean
    equals(Object o)
    {
      return
        o instanceof Property && ((Property) o).name.equals(name) &&
          ((Property) o).value.equals(value);
    }



    public int
    hashCode()
    {
      return name.hashCode() + value.hashCode();
    }

  } // Property

} // Properties
