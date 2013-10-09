package be.re.webdav;

import be.re.net.HTTPClient;
import be.re.net.ProtocolException;
import be.re.util.Array;
import be.re.xml.ExpandedName;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * A collection of utility methods for working with a WebDAV server. Some
 * methods return localized names or messages. If you want to add translations
 * for this, you have to add the file
 * <code>Res_&lt;language_tag>.properties</code> to the package
 * <code>be.re.webdav.res</code>.
 *
 * @author Werner Donn\u00e9
 */

public class Util

{

  private static final ExpandedName[]	PRIVILEGES =
    new ExpandedName[]
    {
      new ExpandedName(Constants.URI, "audit"),
      new ExpandedName(Constants.DAV_URI, "all"),
      new ExpandedName(Constants.DAV_URI, "bind"),
      new ExpandedName(Constants.URI, "checkin"),
      new ExpandedName(Constants.URI, "checkout"),
      new ExpandedName(Constants.URI, "delete"),
      new ExpandedName(Constants.URI, "label"),
      new ExpandedName(Constants.DAV_URI, "read"),
      new ExpandedName(Constants.DAV_URI, "read-acl"),
      new ExpandedName(Constants.DAV_URI, "read-current-user-privilege-set"),
      new ExpandedName(Constants.DAV_URI, "unbind"),
      new ExpandedName(Constants.URI, "uncheckout"),
      new ExpandedName(Constants.DAV_URI, "unlock"),
      new ExpandedName(Constants.URI, "version"),
      new ExpandedName(Constants.DAV_URI, "write"),
      new ExpandedName(Constants.DAV_URI, "write-acl"),
      new ExpandedName(Constants.DAV_URI, "write-content"),
      new ExpandedName(Constants.URI, "write-documents"),
      new ExpandedName(Constants.DAV_URI, "write-properties")
    };

  private static ResourceBundle		bundle = null;
  static DocumentBuilderFactory		nonValidatingDomFactory;
  static XMLInputFactory		inputFactory;



  static
  {
    try
    {
      inputFactory = be.re.xml.stax.Util.newInputFactory(false, true);
      nonValidatingDomFactory = be.re.xml.Util.newDocumentBuilderFactory(false);
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private
  Util()
  {
  }



  /**
   * Throws an exception if the status code of the property is not 200.
   */

  static void
  checkPropstatStatus(URL url, String method, Document multiStatusResponse)
    throws IOException
  {
    Node	status =
      be.re.xml.Util.selectElement
      (
        multiStatusResponse.getDocumentElement(),
        new ExpandedName[]
        {
          new ExpandedName(Constants.DAV_URI, "response"),
          new ExpandedName(Constants.DAV_URI, "status")
        }
      );

    if (status != null)
    {
      int	code =
        be.re.net.Util.httpStatusCode(be.re.xml.Util.getText(status).trim());

      if (code != 200)
      {
        Element	error =
          be.re.xml.Util.selectElement
          (
            multiStatusResponse.getDocumentElement(),
            new ExpandedName[]
            {
              new ExpandedName(Constants.DAV_URI, "response"),
              new ExpandedName(Constants.DAV_URI, "responsedescription"),
              new ExpandedName(Constants.DAV_URI, "error")
            }
          );

        throw
          new ProtocolException
          (
            code,
            error != null ?
              getPreconditionMessage(error) : getReasonPhrase(method, code)
          );
      }
    }

    Element[]	propstats =
      be.re.xml.Util.selectElements
      (
        multiStatusResponse.getDocumentElement(),
        new ExpandedName[]
        {
          new ExpandedName(Constants.DAV_URI, "response"),
          new ExpandedName(Constants.DAV_URI, "propstat")
        }
      );

    for (int i = 0; i < propstats.length; ++i)
    {
      status =
        be.re.xml.Util.
          selectFirstChild(propstats[i], Constants.DAV_URI, "status");

      if (status != null)
      {
        int	code =
          be.re.net.Util.httpStatusCode(be.re.xml.Util.getText(status).trim());

        if (code != 200)
        {
          Element	error =
            be.re.xml.Util.selectElement
            (
              propstats[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "responsedescription"),
                new ExpandedName(Constants.DAV_URI, "error")
              }
            );

          throw
            new ProtocolException
            (
              code,
              error != null ?
                getPreconditionMessage(error) : getReasonPhrase(method, code)
            );
        }
      }
    }
  }



  static Document
  createDAVDocument(String name)
  {
    return createDocument(name, Constants.DAV_URI);
  }



  static Document
  createDocument(String name, String namespaceURI)
  {
    try
    {
      Document	result =
        be.re.xml.Util.getDocumentBuilder(nonValidatingDomFactory, null).
          newDocument();
      Element	element = result.createElementNS(namespaceURI, name);

      element.setAttribute("xmlns", namespaceURI);
      result.appendChild(element);

      return result;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  static Document
  createReDocument(String name)
  {
    return createDocument(name, Constants.URI);
  }



  /**
   * Extracts the Access Control Entries from a <code>DAV:acl</code> property
   * element.
   * @param baseUrl the URL to resolve relative URLs in the
   * <code>aclProperty</code> against.
   * @param aclProperty a <code>DAV:acl</code> property element.
   * @return The Acces Control Entries in the order of appearance.
   */

  public static Client.ACE[]
  getAces(URL baseUrl, Element aclProperty)
  {
    Element[]	aces =
      be.re.xml.Util.selectElements
      (
        aclProperty,
        new ExpandedName[]{new ExpandedName(Constants.DAV_URI, "ace")}
      );
    List	result = new ArrayList();

    for (int i = 0; i < aces.length; ++i)
    {
      Client.ACE	ace = new Client.ACE();
      Node		principal =
        be.re.xml.Util.
          selectFirstChild(aces[i], Constants.DAV_URI, "principal");
      Element[]		privileges =
        be.re.xml.Util.selectElements
        (
          aces[i],
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "grant"),
            new ExpandedName(Constants.DAV_URI, "privilege")
          }
        );

      ace.invert = (principal == null);
      ace.isProtected =
        be.re.xml.Util.
          selectFirstChild(aces[i], Constants.DAV_URI, "protected") != null;
      ace.grant = privileges.length > 0;

      if (principal == null)
      {
        principal =
          be.re.xml.Util.selectElement
          (
            aces[i],
            new ExpandedName[]
            {
              new ExpandedName(Constants.DAV_URI, "invert"),
              new ExpandedName(Constants.DAV_URI, "principal")
            }
          );
      }

      ace.principal = getPrincipal(baseUrl, principal);

      if (privileges.length == 0)
      {
        privileges =
          be.re.xml.Util.selectElements
          (
            aces[i],
            new ExpandedName[]
            {
              new ExpandedName(Constants.DAV_URI, "deny"),
              new ExpandedName(Constants.DAV_URI, "privilege")
            }
          );
      }

      for (int j = 0; j < privileges.length; ++j)
      {
        Element[]	elements =
          be.re.xml.Util.selectElements(privileges[j]);

        if (elements.length == 1)
        {
          Client.ACE	copy = new Client.ACE(ace);

          copy.privilege = new ExpandedName(elements[0]);
          result.add(copy);
        }
      }
    }

    return (Client.ACE[]) result.toArray(new Client.ACE[0]);
  }



  /**
   * Returns the <code>DAV:auto-version</code> property.
   * @param url the URL of the resource.
   * @return The value of the <code>DAV:auto-version</code> property, which may
   * be <code>null</code>.
   */

  public static String
  getAutoVersion(URL url) throws IOException
  {
    return getAutoVersion(url, new Client());
  }



  /**
   * Returns the <code>DAV:auto-version</code> property.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return The value of the <code>DAV:auto-version</code> property, which may
   * be <code>null</code>.
   */

  public static String
  getAutoVersion(URL url, Client client) throws IOException
  {
    return client.getSimpleLiveProperty(url, "auto-version");
  }



  /**
   * Returns the localized display name for the value of the
   * <code>DAV:auto-version</code> property.
   * @param value the value of the <code>DAV:auto-version</code> property.
   * @return The localized display name.
   */

  public static String
  getAutoVersionValueDisplayName(String value)
  {
    try
    {
      return getResource("property_auto-version_" + value);
    }

    catch (MissingResourceException e)
    {
      return value;
    }
  }



  /**
   * Returns the home folder of a user.
   * @param user the URL for the user.
   * @return The URL of the home folder or <code>null</code>.
   */

  public static URL
  getHome(URL user) throws IOException
  {
    return getHome(user, new Client());
  }



  /**
   * Returns the home folder of a user.
   * @param user the URL for the user.
   * @param client the <code>Client</code> object used for the connection.
   * @return The URL of the home folder or <code>null</code>.
   */

  public static URL
  getHome(URL user, Client client) throws IOException
  {
    String	result =
      client.getSimpleLiveProperty
      (
        user,
        new ExpandedName(be.re.webdav.Constants.URI, "home")
      );

    return result != null ? new URL(result) : null;
  }



  public static URL
  getHref(URL url, Element response)
  {
    String	result =
      be.re.xml.Util.getText
      (
        be.re.xml.Util.selectFirstChild(response, Constants.DAV_URI, "href")
      );

    if (result == null)
    {
      return null;
    }

    try
    {
      // There are servers that don't URI-encode their URLs.
      result =
        be.re.net.Util.isUrl(result) ?
          be.re.net.Util.escapedUrl(result).toString() :
          be.re.net.Util.escapeUriPathSegments(result);

      url = be.re.net.Util.stripUserInfo(url);

      return
        be.re.net.Util.resolvePath
          // Some servers return URLs with ../ in the path.
        (
          url.toString().equals(result) || url.toString().equals(result + "/") ?
            url :
            (
              be.re.net.Util.isUrl(result) ?
                new URL(result) : new URL(url, result)
            )
        );
    }

    catch (MalformedURLException e)
    {
      return null;
    }
  }



  static String
  getLockToken(Client.Response response)
  {
    try
    {
      Document	document = response.createDocument();

      return
        document == null ? null : getLockToken(document.getDocumentElement());
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  static String
  getLockToken(Element prop)
  {
    String	s =
      be.re.xml.Util.getText
      (
        be.re.xml.Util.selectElement
        (
          prop,
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "lockdiscovery"),
            new ExpandedName(Constants.DAV_URI, "activelock"),
            new ExpandedName(Constants.DAV_URI, "locktoken"),
            new ExpandedName(Constants.DAV_URI, "href"),
          }
        )
      );

    return
      s == null ?
        null :
        (
          s.startsWith("opaquelocktoken:") ?
            s.substring("opaquelocktoken:".length()) :
            (
              s.startsWith("urn:uuid:") ?
                s.substring("urn:uuid:".length()) : null
            )
        );
  }



  /**
   * Returns the owner of a resource.
   * @param url the URL of the resource.
   * @return The URL of the owner or <code>null</code>.
   */

  public static URL
  getOwner(URL url) throws IOException
  {
    return getOwner(url, new Client());
  }



  /**
   * Returns the owner of a resource.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return The URL of the owner or <code>null</code>.
   */

  public static URL
  getOwner(URL url, Client client) throws IOException
  {
    return new URL(client.getSimpleLiveProperty(url, "owner"));
  }



  public static URL
  getParent(URL url)
  {
    try
    {
      String	s = url.toString();

      return
        url.getFile() == "/" ?
          null :
          new URL
          (
            s.substring
            (
              0,
              s.lastIndexOf('/', s.length() - (s.endsWith("/") ? 2 : 1)) + 1
            )
          );
    }

    catch (MalformedURLException e)
    {
      return null;
    }
  }



  /**
   * Returns the password of a user.
   * @param user the URL of the user.
   * @return The password or <code>null</code>.
   */

  public static String
  getPassword(URL user) throws IOException
  {
    return getPassword(user, new Client());
  }



  /**
   * Returns the password of a user.
   * @param user the URL of the user.
   * @param client the <code>Client</code> object used for the connection.
   * @return The password or <code>null</code>.
   */

  public static String
  getPassword(URL user, Client client) throws IOException
  {
    return
      client.getSimpleLiveProperty
      (
        user,
        new ExpandedName(be.re.webdav.Constants.URI, "password")
      );
  }



  /**
   * Extracts the status code from a property element.
   * @param propElement the property element.
   * @return The status code or <code>-1</code>.
   */

  public static int
  getPropertyStatusCode(Node propElement)
  {
    Node	status =
      be.re.xml.Util.selectFirstChild
      (
        propElement.getParentNode(),
        Constants.DAV_URI,
        "status"
      );

    if (status == null)
    {
      return -1;
    }

    return be.re.net.Util.httpStatusCode(be.re.xml.Util.getText(status));
  }



  /**
   * Returns the available number of bytes for a user.
   * @param user the URL of the user.
   * @return The number of bytes or <code>-1</code>.
   */

  public static long
  getQuotaAvailableBytes(URL user) throws IOException
  {
    return getQuotaAvailableBytes(user, new Client());
  }



  /**
   * Returns the available number of bytes for a user.
   * @param user the URL of the user.
   * @param client the <code>Client</code> object used for the connection.
   * @return The number of bytes or <code>-1</code>.
   */

  public static long
  getQuotaAvailableBytes(URL user, Client client) throws IOException
  {
    return getQuotaBytes(user, client, "quota-available-bytes");
  }



  private static long
  getQuotaBytes(URL user, Client client, String property) throws IOException
  {
    String	bytes =
      client.getSimpleLiveProperty
      (
        user,
        new ExpandedName(be.re.webdav.Constants.URI, property)
      );

    return bytes != null ? Long.parseLong(bytes) : -1;
  }



  /**
   * Returns the total number of bytes a user can use.
   * @param user the URL of the user.
   * @return The number of bytes or <code>-1</code>.
   */

  public static long
  getQuotaTotalBytes(URL user) throws IOException
  {
    return getQuotaTotalBytes(user, new Client());
  }



  /**
   * Returns the total number of bytes a user can use.
   * @param user the URL of the user.
   * @param client the <code>Client</code> object used for the connection.
   * @return The number of bytes or <code>-1</code>.
   */

  public static long
  getQuotaTotalBytes(URL user, Client client) throws IOException
  {
    return getQuotaBytes(user, client, "quota-total-bytes");
  }



  /**
   * Returns the number of bytes a user is using.
   * @param user the URL of the user.
   * @return The number of bytes or <code>-1</code>.
   */

  public static long
  getQuotaUsedBytes(URL user) throws IOException
  {
    return getQuotaUsedBytes(user, new Client());
  }



  /**
   * Returns the number of bytes a user is using.
   * @param user the URL of the user.
   * @param client the <code>Client</code> object used for the connection.
   * @return The number of bytes or <code>-1</code>.
   */

  public static long
  getQuotaUsedBytes(URL user, Client client) throws IOException
  {
    return getQuotaBytes(user, client, "quota-used-bytes");
  }



  /**
   * Returns the localized message that corresponds to the precondition or the
   * empty string if there is no such message.
   * @param precondition the local name of the precondition element.
   * @return The localized message or the empty string.
   */

  public static String
  getPreconditionMessage(String precondition)
  {
    try
    {
      return getResource("dav_pc_" + precondition);
    }

    catch (MissingResourceException e)
    {
      return "";
    }
  }



  /**
   * Returns the localized message that corresponds to the preconditions in the
   * response.
   * @param response the response from the server.
   * @return The localized message or the empty string.
   */

  public static String
  getPreconditionMessage(Client.Response response)
  {
    try
    {
      Document	document = response.createDocument();

      return
        document == null ?
          getReasonPhrase(response) :
          getPreconditionMessage(document.getDocumentElement());
    }

    catch (Exception e)
    {
      return "";
    }
  }



  /**
   * Extracts the localized message that corresponds to the preconditions in
   * the <code>DAV:error</code> element.
   * @param error the error element.
   * @return The localized message or the empty string.
   */

  public static String
  getPreconditionMessage(Node error)
  {
    Element[]	children = be.re.xml.Util.selectElements(error);
    String	message = "";

    for (int i = 0; i < children.length; ++i)
    {
      message +=
        (message.equals("") ? "" : " ") +
          getPreconditionMessage(children[i].getLocalName());

      if ("need-privileges".equals(children[i].getLocalName()))
      {
        Node[]	resources =
          be.re.xml.Util.
            selectChildren(children[i], Constants.DAV_URI, "resource");

        for (int j = 0; j < resources.length; ++j)
        {
          Element[]	privilege =
            be.re.xml.Util.selectElements
            (
              be.re.xml.Util.
                selectFirstChild(resources[j], Constants.DAV_URI, "privilege")
            );

          message += "\n    " +
            be.re.xml.Util.getText
            (
              be.re.xml.Util.
                selectFirstChild(resources[j], Constants.DAV_URI, "href")
            ) + ": " +
              (
                privilege.length == 1 ?
                  getPrivilegeDescription(privilege[0]) : "unknown"
              );
        }
      }
    }

    return message;
  }



  private static Object
  getPrincipal(URL url, Node principal)
  {
    Element[]	elements = be.re.xml.Util.selectElements(principal);

    if
    (
      elements.length == 0					||
      !Constants.DAV_URI.equals(elements[0].getNamespaceURI())
    )
    {
      return null;
    }

    if ("href".equals(elements[0].getLocalName()))
    {
      String	text = be.re.xml.Util.getText(elements[0]);

      try
      {
        return new URL(url, text);
      }

      catch (MalformedURLException e)
      {
        return null;
      }
    }

    if ("property".equals(elements[0].getLocalName()))
    {
      Element[]	properties = be.re.xml.Util.selectElements(elements[0]);

      return properties.length > 0 ? new ExpandedName(properties[0]) : null;
    }

    return elements[0].getLocalName();
  }



  /**
   * Returns the localized description of a privilege.
   * @param privilege the privilege element.
   * @return The localized privilege desciption or <code>null</code>.
   */

  public static String
  getPrivilegeDescription(Node privilege)
  {
    return getPrivilegeDescription(privilege.getLocalName());
  }



  /**
   * Returns the localized description of a privilege.
   * @param privilege the qualified name of the privilege.
   * @return The localized privilege desciption or <code>null</code>.
   */

  public static String
  getPrivilegeDescription(ExpandedName privilege)
  {
    return getPrivilegeDescription(privilege.localName);
  }



  /**
   * Returns the localized description of a privilege using the local name.
   * @param privilege the local name of the privilege.
   * @return The localized privilege desciption or <code>null</code>.
   */

  public static String
  getPrivilegeDescription(String privilege)
  {
    try
    {
      return getResource("privilege_" + privilege);
    }

    catch (MissingResourceException e)
    {
      return null;
    }
  }



  /**
   * Returns all known privileges. The result shouldn't be altered.
   */

  public static ExpandedName[]
  getPrivileges()
  {
    return PRIVILEGES;
  }



  /**
   * Returns the localized display name of a property.
   * @param property the property.
   * @return The localized display name or the property itself if it doesn't
   * exist.
   */

  public static String
  getPropertyDisplayName(String property)
  {
    try
    {
      return getResource("property_" + property);
    }

    catch (MissingResourceException e)
    {
      return property;
    }
  }



  /**
   * Returns the localized reason message that corresponds to the response.
   * @param response the response from the server.
   * @return The localized message.
   */

  public static String
  getReasonPhrase(Client.Response response)
  {
    return
      response.getStatusCode() == 500 ?
        response.getStatusMessage() :
        getReasonPhrase(response.getMethod(), response.getStatusCode());
  }



  /**
   * Returns the localized reason message that corresponds to the method/status
   * code combination.
   * @param method the method.
   * @param statusCode the status code.
   * @return The localized message.
   */

  public static String
  getReasonPhrase(String method, int statusCode)
  {
    try
    {
      return
        getResource
        (
          "http_" + String.valueOf(statusCode) + "_" + method.toLowerCase()
        );
    }

    catch (MissingResourceException e)
    {
      return be.re.net.Util.getHttpReasonPhrase(statusCode);
    }
  }



  /**
   * A low-level method to retrieve localized strings from the
   * <code>be.re.webdav.res.Res</code> bundle.
   * @param key the key.
   * @return The localized string.
   */

  public static String
  getResource(String key)
  {
    if (bundle == null)
    {
      bundle = ResourceBundle.getBundle("be.re.webdav.res.Res");
    }

    return bundle.getString(key);
  }



  /**
   * Returns all the resource types a resource has.
   * @param url the URL of the resource.
   * @return An array of the qualified names of the resource types.
   */

  public static ExpandedName[]
  getResourceTypes(URL url) throws IOException
  {
    return getResourceTypes(url, new Client());
  }



  /**
   * Returns all the resource types a resource has.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return An array of the qualified names of the resource types. The array
   * will be empty when no resource types were returned.
   */

  public static ExpandedName[]
  getResourceTypes(URL url, Client client) throws IOException
  {
    Client.Response	response =
      client.propfindSpecific
      (
        url,
        new ExpandedName[]{new ExpandedName(Constants.DAV_URI, "resourcetype")},
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
        return new ExpandedName[0];
      }

      Element[]	elements =
        be.re.xml.Util.selectElements
        (
          document.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "resourcetype")
          }
        );

      if (elements.length != 1)
      {
        return new ExpandedName[0];
      }

      int	statusCode = getPropertyStatusCode(elements[0].getParentNode());

      if (statusCode != 200)
      {
        if (statusCode == 404)
        {
          return new ExpandedName[0];
        }

        Util.throwException(url, HTTPClient.PROPFIND, statusCode);
      }

      Element[]		children = be.re.xml.Util.selectElements(elements[0]);
      ExpandedName[]	result = new ExpandedName[children.length];

      for (int i = 0; i < children.length; ++i)
      {
        result[i] = new ExpandedName(children[i]);
      }

      return result;
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Extracts the status code from a multistatus reponse entry.
   * @param response the response element.
   * @return The status code or <code>-1</code>.
   */

  public static int
  getStatusCode(Element response)
  {
    String	statusCode =
      be.re.xml.Util.getText
      (
        be.re.xml.Util.selectFirstChild(response, Constants.DAV_URI, "status")
      );

    return
      statusCode == null || "".equals(statusCode) ?
        -1 : be.re.net.Util.httpStatusCode(statusCode.trim());
  }



  /**
   * Tests if the resource is an activity.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is an activity, <code>false</code>
   * otherwise.
   */

  public static boolean
  isActivity(URL url) throws IOException
  {
    return isActivity(url, new Client());
  }



  /**
   * Tests if the resource is an activity.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is an activity, <code>false</code>
   * otherwise.
   */

  public static boolean
  isActivity(URL url, Client client) throws IOException
  {
    try
    {
      return
        Array.inArray
        (
          getResourceTypes(url, client),
          new ExpandedName(Constants.DAV_URI, "activity")
        );
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
   * Returns the checked-out state.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is checked out, <code>false</code>
   * otherwise.
   */

  public static boolean
  isCheckedOut(URL url) throws IOException
  {
    return isCheckedOut(url, new Client());
  }



  /**
   * Returns the checked-out state.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is checked out, <code>false</code>
   * otherwise.
   */

  public static boolean
  isCheckedOut(URL url, Client client) throws IOException
  {
    Client.Response	response = null;
    final boolean[]	result = new boolean[1];

    try
    {
      response =
        client.propfindSpecific
        (
          url,
          new ExpandedName[]
          {new ExpandedName(Constants.DAV_URI, "checked-out")},
          "0"
        );

      readPropertyList
      (
        url,
        response,
        new PropertyHandler()
        {
          public boolean
          handle(URL href, Element[] properties, int statusCode)
            throws IOException
          {
            if (statusCode == 200 && properties.length == 1)
            {
              result[0] = true;
            }

            return true;
          }
        },
        true
      );
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }
    }

    return result[0];
  }



  /**
   * Tests if the resource is a collection.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is a collection, <code>false</code>
   * otherwise.
   */

  public static boolean
  isCollection(URL url) throws IOException
  {
    return isCollection(url, new Client());
  }



  /**
   * Tests if the resource is a collection.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is a collection, <code>false</code>
   * otherwise.
   */

  public static boolean
  isCollection(URL url, Client client) throws IOException
  {
    try
    {
      return
        Array.inArray
        (
          getResourceTypes(url, client),
          new ExpandedName(Constants.DAV_URI, "collection")
        );
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



  private static boolean
  isContainer(URL url)
  {
    return url.getPath().endsWith("/");
  }



  static boolean
  isEndDavElement(XMLEvent event, String localName)
  {
    return isEndElement(event, Constants.DAV_URI, localName);
  }



  static boolean
  isEndElement(XMLEvent event, String namespaceURI, String localName)
  {
    return
      event.isEndElement() &&
        event.asEndElement().getName().
          equals(new QName(namespaceURI, localName));
  }



  static boolean
  isEndReElement(XMLEvent event, String localName)
  {
    return isEndElement(event, Constants.URI, localName);
  }



  /**
   * Tests if the resource is a group.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is a group, <code>false</code>
   * otherwise.
   */

  public static boolean
  isGroup(URL url) throws IOException
  {
    return isGroup(url, new Client());
  }



  /**
   * Tests if the resource is a group.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is a group, <code>false</code>
   * otherwise.
   */

  public static boolean
  isGroup(URL url, Client client) throws IOException
  {
    try
    {
      ExpandedName[]	types = getResourceTypes(url, client);

      return
        Array.inArray
        (
          types,
          new ExpandedName(Constants.DAV_URI, "principal")
        ) &&
          Array.inArray
          (
            types,
            new ExpandedName(Constants.DAV_URI, "collection")
          );
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
   * Tests if user owns the resource.
   * @param url the URL of the resource.
   * @param user the URL of the user.
   * @return <code>true</code> if the user owns it, <code>false</code>
   * otherwise.
   */

  public static boolean
  isOwner(URL url, URL user) throws IOException
  {
    return isOwner(url, user, new Client());
  }



  /**
   * Tests if user owns the resource.
   * @param url the URL of the resource.
   * @param user the URL of the user.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if the user owns it, <code>false</code>
   * otherwise.
   */

  public static boolean
  isOwner(URL url, URL user, Client client) throws IOException
  {
    return
      getOwner(url, client).toString().
        equals(be.re.net.Util.stripUserInfo(user).toString());
  }



  /**
   * Tests if the resource is a principal.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is a principal, <code>false</code>
   * otherwise.
   */

  public static boolean
  isPrincipal(URL url) throws IOException
  {
    return isPrincipal(url, new Client());
  }



  /**
   * Tests if the resource is a principal.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is a principal, <code>false</code>
   * otherwise.
   */

  public static boolean
  isPrincipal(URL url, Client client) throws IOException
  {
    try
    {
      return
        Array.inArray
        (
          getResourceTypes(url, client),
          new ExpandedName(Constants.DAV_URI, "principal")
        );
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



  private static boolean
  isRequestCollection(URL requestCollection, URL href) throws IOException
  {
    if (isContainer(requestCollection))
    {
      if (!href.toString().endsWith("/"))
      {
        // There are servers that don't include the trailing slash.
        href = new URL(href + "/");
      }

      if (!requestCollection.getProtocol().equals(href.getProtocol()))
      {
        // There are servers that hard-code the protocol.
        href = be.re.net.Util.setScheme(href, requestCollection.getProtocol());
      }

      return
        be.re.net.Util.unescapeUriSpecials
        (
          be.re.net.Util.stripUserInfo(requestCollection).toString()
        ).equals
          (
            be.re.net.Util.unescapeUriSpecials
            (
              be.re.net.Util.stripUserInfo(href).toString()
            )
          );
    }

    return false;
  }



  static boolean
  isStartDavElement(XMLEvent event, String localName)
  {
    return isStartElement(event, Constants.DAV_URI, localName);
  }



  static boolean
  isStartElement(XMLEvent event, String namespaceURI, String localName)
  {
    return
      event.isStartElement() &&
        event.asStartElement().getName().
          equals(new QName(namespaceURI, localName));
  }



  static boolean
  isStartReElement(XMLEvent event, String localName)
  {
    return isStartElement(event, Constants.URI, localName);
  }



  /**
   * Tests if the resource is a user.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is a user, <code>false</code>
   * otherwise.
   */

  public static boolean
  isUser(URL url) throws IOException
  {
    return isUser(url, new Client());
  }



  /**
   * Tests if the resource is a user.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is a user, <code>false</code>
   * otherwise.
   */

  public static boolean
  isUser(URL url, Client client) throws IOException
  {
    try
    {
      ExpandedName[]	types = getResourceTypes(url, client);

      return
        Array.inArray
        (
          types,
          new ExpandedName(Constants.DAV_URI, "principal")
        ) &&
          !Array.inArray
          (
            types,
            new ExpandedName(Constants.DAV_URI, "collection")
          );
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
   * Tests if the resource is a version.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is a version, <code>false</code>
   * otherwise.
   */

  public static boolean
  isVersion(URL url) throws IOException
  {
    return isVersion(url, new Client());
  }



  /**
   * Tests if the resource is a version.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is a version, <code>false</code>
   * otherwise.
   */

  public static boolean
  isVersion(URL url, Client client) throws IOException
  {
    try
    {
      return client.getSimpleLiveProperty(url, "version-name") != null;
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
   * Tests if the resource is a view. This is specific to Pincette.
   * @param url the URL of the resource.
   * @return <code>true</code> if it is a view, <code>false</code>
   * otherwise.
   */

  public static boolean
  isView(URL url) throws IOException
  {
    return isView(url, new Client());
  }



  /**
   * Tests if the resource is a view. This is specific to Pincette.
   * @param url the URL of the resource.
   * @param client the <code>Client</code> object used for the connection.
   * @return <code>true</code> if it is a view, <code>false</code>
   * otherwise.
   */

  public static boolean
  isView(URL url, Client client) throws IOException
  {
    try
    {
      return
        Array.inArray
        (
          getResourceTypes(url, client),
          new ExpandedName(Constants.DAV_URI, "view")
        );
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
   * Creates the collection resource denoted by <code>url</code> and all of the
   * parent collection resources if they don't already exist.
   * @param url the URL of the new collection.
   */

  public static void
  mkcols(URL url) throws IOException
  {
    mkcols(url, new Client());
  }



  /**
   * Creates the collection resource denoted by <code>url</code> and all of the
   * parent collection resources if they don't already exist.
   * @param url the URL of the new collection.
   * @param manager the URL manager.
   * @param client the <code>Client</code> object used for the connection.
   */

  private static void
  mkcols(URL url, Client client) throws IOException, ProtocolException
  {
    if (!client.exists(url) && getParent(url) != null)
    {
      mkcols(getParent(url), client);

      boolean	ourCheckOut = true;

      try
      {
        client.checkout(getParent(url), null, false);
      }

      catch (ProtocolException e)
      {
        if (e.getCode() != 409)
        {
          throw e;
        }

        ourCheckOut = false;
      }

      client.mkcol(url);

      if (ourCheckOut)
      {
        client.checkin(getParent(url), false, false);
      }
    }
  }



  /**
   * Returns the values of the <code>DAV:displayname</code> properties of the
   * members of the <code>collection</code>. This is useful to show directory
   * listings or to extract values from functional collections such as those
   * that contain activities, principals, etc.
   * @param collection the URL of the resource.
   * @return The array of display names, which will be empty if the collection
   * is empty.
   */

  public static String[]
  readDisplayNameList(URL collection) throws IOException
  {
    final List		displayNames = new ArrayList();
    Client.Response	response =
      new Client().propfindSpecific
      (
        collection,
        new ExpandedName[]
        {new ExpandedName(Constants.DAV_URI, "displayname")},
        "1"
      );

    try
    {
      readDisplayNameList
      (
        collection,
        response,
        new be.re.webdav.Util.DisplayNameHandler()
        {
          public boolean
          handle(URL href, String displayName)
          {
            displayNames.add(displayName);

            return true;
          }
        },
        false
      );

      return (String[]) displayNames.toArray(new String[0]);
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Processes the values of the <code>DAV:displayname</code> properties in the
   * entries in a multistatus response.
   * @param url the request URL.
   * @param response the multistatus response.
   * @param handler the handler that is called for each occurrence of a
   * <code>DAV:displayname</code> property.
   * @param includeCollection if set to <code>true</code> the request URL will
   * also be considered in case it is a collection.
   */

  public static void
  readDisplayNameList
  (
    URL			url,
    Client.Response	response,
    DisplayNameHandler	handler,
    boolean		includeCollection
  ) throws IOException
  {
    InputStream	body = response.getBody();

    if (body == null)
    {
      return;
    }

    XMLEventReader	reader = null;

    try
    {
      boolean	stop = false;

      reader = inputFactory.createXMLEventReader(body);

      while (!stop && reader.hasNext())
      {
        XMLEvent	event = reader.nextEvent();

        if (isStartDavElement(event, "response"))
        {
          stop =
            !readDisplayNameResponse(url, reader, handler, includeCollection);
        }
      }
    }

    catch (Exception e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }

        catch (Exception e)
        {
          throw new be.re.io.IOException(e);
        }
      }

      body.close();
    }
  }



  private static boolean
  readDisplayNameResponse
  (
    URL			url,
    XMLEventReader	reader,
    DisplayNameHandler	handler,
    boolean		includeCollection
  ) throws IOException, XMLStreamException
  {
    String	displayName = null;
    String	href = null;

    while (reader.hasNext())
    {
      XMLEvent	event = reader.nextEvent();

      if (isEndDavElement(event, "response"))
      {
        try
        {
          if
          (
            href != null					&&
            displayName != null					&&
            (
              includeCollection					||
              !isRequestCollection(url, new URL(url, href))
            )
          )
          {
            return
              handler.handle
              (
                be.re.net.Util.
                  setUserInfo(new URL(url, href), url.getUserInfo()),
                displayName
              );
          }
        }

        catch (MalformedURLException e)
        {
        }

        return true;
      }

      if (isStartDavElement(event, "href"))
      {
        href = reader.getElementText();
      }
      else
      {
        if (isStartDavElement(event, "displayname"))
        {
          displayName = reader.getElementText();
        }
      }
    }

    return true;
  }



  /**
   * Processes all the properties in the entries in a multistatus response.
   * @param url the request URL.
   * @param response the multistatus response.
   * @param handler the handler that is called for each occurrence of the
   * <code>DAV:propstat</code> element. The handler will receive the properties
   * in it. If there are no properties at all for a response entry the handler
   * is called once with an empty property array.
   * @param includeCollection if set to <code>true</code> the request URL will
   * also be considered in case it is a collection.
   */

  public static void
  readPropertyList
  (
    URL			url,
    Client.Response	response,
    PropertyHandler	handler,
    boolean		includeCollection
  ) throws IOException
  {
    readPropertyList
    (
      url,
      response,
      handler,
      null,
      includeCollection
    );
  }



  /**
   * Processes all the properties in the entries in a multistatus response.
   * @param url the request URL.
   * @param response the multistatus response.
   * @param handler the handler that is called for each occurrence of the
   * <code>DAV:propstat</code> element. The handler will receive the properties
   * in it. If there no properties at all for a response entry the handler is
   * called once with an empty property array.
   * @param report the handler that is called when the status code for an entry
   * is present but not 200.
   * @param includeCollection if set to <code>true</code> the request URL will
   * also be considered in case it is a collection.
   */

  public static void
  readPropertyList
  (
    URL			url,
    Client.Response	response,
    PropertyHandler	handler,
    ReportMultistatus	report,
    boolean		includeCollection
  ) throws IOException
  {
    InputStream	body = response.getBody();

    if (body == null)
    {
      return;
    }

    XMLEventReader	reader = null;

    try
    {
      boolean	stop = false;

      reader = inputFactory.createXMLEventReader(body);

      while (!stop && reader.hasNext())
      {
        XMLEvent	event = reader.nextEvent();

        if (isStartDavElement(event, "response"))
        {
          Element	element =
            be.re.xml.stax.Util.accumulate(reader, event.asStartElement());
          int		statusCode = getStatusCode(element);

          stop = 
            statusCode == -1 || statusCode == 200 ?
              !readPropertyList(url, element, handler, includeCollection) :
              !reportResponse(url, element, report);
        }
      }
    }

    catch (Exception e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }

        catch (Exception e)
        {
          throw new be.re.io.IOException(e);
        }
      }

      body.close();
    }
  }



  private static boolean
  readPropertyList
  (
    URL			url,
    Element		response,
    PropertyHandler	handler,
    boolean		includeCollection
  ) throws IOException
  {
    URL	href = getHref(url, response);

    if
    (
      href == null			||
      (
        !includeCollection		&&
        isRequestCollection(url, href)
      )
    )
    {
      return true;
    }

    Element[]	propstat =
      be.re.xml.Util.selectElements
      (
        response,
        new ExpandedName[]{new ExpandedName(Constants.DAV_URI, "propstat")}
      );

    for (int i = 0; i < propstat.length; ++i)
    {
      Node	prop =
        be.re.xml.Util.
          selectFirstChild(propstat[i], Constants.DAV_URI, "prop");

      if (prop != null)
      {
        Node	status =
          be.re.xml.Util.
            selectFirstChild(propstat[i], Constants.DAV_URI, "status");

        if
        (
          !handler.handle
          (
            href,
            be.re.xml.Util.selectElements(prop),
            status != null ?
              be.re.net.Util.httpStatusCode(be.re.xml.Util.getText(status)) : -1
          )
        )
        {
          return false;
        }
      }
    }

    return propstat.length > 0 || handler.handle(href, new Element[0], -1);
  }



  /**
   * Processes the <code>DAV:href</code> element in the entries in a multistatus
   * response.
   * @param url the request URL.
   * @param response the multistatus response.
   * @param handler the handler that is called for each entry.
   */

  public static void
  readUrlList
  (
    URL			url,
    Client.Response	response,
    UrlHandler		handler
  ) throws IOException
  {
    InputStream	body = response.getBody();

    if (body == null)
    {
      return;
    }

    XMLEventReader	reader = null;

    try
    {
      boolean	stop = false;

      reader = inputFactory.createXMLEventReader(body);

      while (!stop && reader.hasNext())
      {
        XMLEvent	event = reader.nextEvent();

        if (isStartDavElement(event, "response"))
        {
          stop = !readUrlResponse(url, reader, handler);
        }
      }
    }

    catch (Exception e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }

        catch (Exception e)
        {
          throw new be.re.io.IOException(e);
        }
      }

      body.close();
    }
  }



  private static boolean
  readUrlResponse(URL url, XMLEventReader reader, UrlHandler handler)
    throws XMLStreamException
  {
    String	href = null;

    while (reader.hasNext())
    {
      XMLEvent	event = reader.nextEvent();

      if (isEndDavElement(event, "response"))
      {
        if (href != null)
        {
          try
          {
            return
              handler.handle
              (
                be.re.net.Util.
                  setUserInfo(new URL(url, href), url.getUserInfo())
              );
          }

          catch (MalformedURLException e)
          {
          }
        }

        return true;
      }

      if (isStartDavElement(event, "href"))
      {
        href = reader.getElementText();
      }
    }

    return true;
  }



  /**
   * Processes the entries in a multistatus response one by one.
   * @param response the multistatus response.
   * @param report the handler to process one entry.
   */

  public static void
  reportMultistatus(Client.Response response, ReportMultistatus report)
    throws IOException
  {
    InputStream	body = response.getBody();

    if (body == null)
    {
      return;
    }

    XMLEventReader	reader = null;

    try
    {
      boolean	stop = false;

      reader = inputFactory.createXMLEventReader(body);

      while (!stop && reader.hasNext())
      {
        XMLEvent	event = reader.nextEvent();

        if (isStartDavElement(event, "response"))
        {
          stop =
            !reportResponse
            (
              response.getUrl(),
              be.re.xml.stax.Util.accumulate(reader, event.asStartElement()),
              report
            );
        }
      }
    }

    catch (Exception e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }

        catch (Exception e)
        {
          throw new be.re.io.IOException(e);
        }
      }

      body.close();
    }
  }



  private static boolean
  reportMultistatusEntry(URL url, Element element, ReportMultistatus report)
    throws IOException
  {
    int	code = getStatusCode(element);

    if (code == -1)
    {
      return true;
    }

    Element	error =
      (Element)
        be.re.xml.Util.selectFirstChild(element, Constants.DAV_URI, "error");

    if (error == null)
    {
      error =
        be.re.xml.Util.selectElement
        (
          element,
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "responsedescription"),
            new ExpandedName(Constants.DAV_URI, "error")
          }
        );
    }

    try
    {
      return
        report == null ||
          report.report
          (
            url,
            code,
            error,
            error != null ?
              null :
              be.re.xml.Util.getText
              (
                be.re.xml.Util.selectFirstChild
                (
                  element,
                  Constants.DAV_URI,
                  "responsedescription"
                )
              )
          );
    }

    catch (MalformedURLException e)
    {
      // Invalid entry.
    }

    return true;
  }



  private static boolean
  reportResponse(URL url, Element response, ReportMultistatus report)
    throws IOException
  {
    String	href =
      be.re.xml.Util.getText
      (
        be.re.xml.Util.selectFirstChild(response, Constants.DAV_URI, "href")
      );

    if (href == null || "".equals(href))
    {
      return true;
    }

    if (!reportMultistatusEntry(new URL(url, href.trim()), response, report))
    {
      return false;
    }

    Element[]	propstats =
      be.re.xml.Util.selectElements
      (
        response,
        new ExpandedName[]{new ExpandedName(Constants.DAV_URI, "propstat")}
      );

    for (int i = 0; i < propstats.length; ++i)
    {
      if
      (
        !reportMultistatusEntry(new URL(url, href.trim()), propstats[i], report)
      )
      {
        return false;
      }
    }

    return true;
  }



  /**
   * Sets the auto-version mode for a resource.
   * @param url the URL of the resource.
   * @param mode see the WebDAV values for the "auto-version" property.
   */

  public static void
  setAutoVersion(URL url, String mode) throws IOException
  {
    setAutoVersion(url, mode, new Client());
  }



  /**
   * Sets the auto-version mode for a resource.
   * @param url the URL of the resource.
   * @param mode see the WebDAV values for the "auto-version" property.
   * @param client the <code>Client</code> object used for the connection.
   */

  public static void
  setAutoVersion(URL url, String mode, Client client) throws IOException
  {
    client.setSimpleLiveProperty(url, "auto-version", mode);
  }



  /**
   * Sets the home folder for a user.
   * @param user the URL of the user.
   * @param home the URL of the home folder.
   */

  public static void
  setHome(URL user, URL home) throws IOException
  {
    setHome(user, home, new Client());
  }



  /**
   * Sets the home folder for a user.
   * @param user the URL of the user.
   * @param home the URL of the home folder.
   * @param client the <code>Client</code> object used for the connection.
   */

  public static void
  setHome(URL user, URL home, Client client) throws IOException
  {
    client.setSimpleLiveProperty
    (
      user,
      new ExpandedName(be.re.webdav.Constants.URI, "home"),
      be.re.net.Util.stripUserInfo(home).toString()
    );
  }



  /**
   * Sets the owner of a resource.
   * @param url the URL of the resource.
   * @param owner the URL of owner.
   */

  public static void
  setOwner(URL url, URL owner) throws IOException
  {
    setOwner(url, owner, new Client());
  }



  /**
   * Sets the owner of a resource.
   * @param url the URL of the resource.
   * @param owner the URL of owner.
   * @param client the <code>Client</code> object used for the connection.
   */

  public static void
  setOwner(URL url, URL owner, Client client) throws IOException
  {
    client.setSimpleLiveProperty
    (
      url,
      "owner",
      be.re.net.Util.stripUserInfo(owner).toString()
    );
  }



  /**
   * Sets the password for a user.
   * @param user the URL of the user.
   * @param password the password.
   */

  public static void
  setPassword(URL user, String password) throws IOException
  {
    setPassword(user, password, new Client());
  }



  /**
   * Sets the password for a user.
   * @param user the URL of the user.
   * @param password the password.
   * @param client the <code>Client</code> object used for the connection.
   */

  public static void
  setPassword(URL user, String password, Client client) throws IOException
  {
    client.setSimpleLiveProperty
    (
      user,
      new ExpandedName(be.re.webdav.Constants.URI, "password"),
      password
    );
  }



  /**
   * Sets the total number of bytes a user can use.
   * @param user the URL of the user.
   * @param total the number of logical bytes.
   */

  public static void
  setQuotaTotalBytes(URL user, long total) throws IOException
  {
    setQuotaTotalBytes(user, total, new Client());
  }



  /**
   * Sets the total number of bytes a user can use.
   * @param user the URL of the user.
   * @param total the number of logical bytes.
   * @param client the <code>Client</code> object used for the connection.
   */

  public static void
  setQuotaTotalBytes(URL user, long total, Client client) throws IOException
  {
    client.setSimpleLiveProperty
    (
      user,
      new ExpandedName(be.re.webdav.Constants.URI, "quota-total-bytes"),
      String.valueOf(total)
    );
  }



  /**
   * Throw an exception with a localized message that is extracted from the
   * response.
   * @param response the response from the server.
   */

  public static void
  throwException(Client.Response response) throws IOException, ProtocolException
  {
    if (response.getStatusCode() >= 500)
    {
      throwException
      (
        response.getUrl(),
        response.getMethod(),
        response.getStatusCode()
      );
    }

    throwPrecondition(response);
  }



  /**
   * Throw an exception with a localized message that corresponds to the
   * method/status code combination.
   * @param url the URL of the resource.
   * @param method the method.
   * @param statusCode the status code.
   */

  public static void
  throwException(URL url, String method, int statusCode)
    throws ProtocolException
  {
    throwException(url, method, statusCode, null);
  }



  /**
   * Throw an exception with a localized message that corresponds to the
   * method/status code combination. If an error element is provided the
   * message will be extracted from it.
   * @param url the URL of the resource.
   * @param method the method.
   * @param statusCode the status code.
   * @param error the error element. It may be <code>null</code>.
   */

  public static void
  throwException(URL url, String method, int statusCode, Node error)
    throws ProtocolException
  {
    throw
      new ProtocolException
      (
        url,
        statusCode,
        error != null ?
          getPreconditionMessage(error) : getReasonPhrase(method, statusCode)
      );
  }



  private static void
  throwPrecondition(Client.Response response)
    throws IOException, ProtocolException
  {
    String	message = getPreconditionMessage(response);

    if (message == null || message.equals(""))
    {
      throwException
      (
        response.getUrl(),
        response.getMethod(),
        response.getStatusCode()
      );
    }

    throw
      new ProtocolException
      (
        response.getUrl(),
        response.getStatusCode(),
        message
      );
  }



  /**
   * This is for handling <code>DAV:displayname</code> properties in a
   * multistatus response one by one.
   * @see Util#readDisplayNameList(URL, Client.Response, DisplayNameHandler,
   * boolean)
   * @author Werner Donn\u00e9
   */

  public interface DisplayNameHandler

  {

    /**
     * @param href the resource in the multistatus response entry.
     * @param displayName the display name in the multistatus response entry.
     * @return To stop processing <code>false</code> should be returned.
     */

    public boolean	handle	(URL href, String displayName);

  } // DisplayNameHandler



  /**
   * This is for handling all properties in a multistatus response one by one.
   * @see Util#readPropertyList
   * @author Werner Donn\u00e9
   */

  public interface PropertyHandler

  {

    /**
     * This method is called for property groups in the same multistatus
     * response entry with the same status code.
     * @param href the resource in the multistatus response entry.
     * @param properties the properties in the multistatus response entry. Each
     * element in the array is the contents of a <code>prop</code> element.
     * @param statusCode the status code for the given properties.
     * @return To stop processing <code>false</code> should be returned.
     */

    public boolean	handle	(
				  URL		href,
				  Element[]	properties,
				  int		statusCode
				) throws IOException;

  } // PropertyHandler



  /**
   * This is for handling state and error information in a multistatus response
   * per entry.
   * @see Util#readPropertyList
   * @see Util#reportMultistatus
   * @author Werner Donn\u00e9
   */

  public interface ReportMultistatus

  {

    /**
     * @param href the resource in the multistatus response entry.
     * @param code the status code.
     * @param error the <code>DAV:error</code> element. It may be
     * <code>null</code>.
     * @param description the error description. It may be <code>null</code>.
     * @return To stop processing <code>false</code> should be returned.
     */

    public boolean	report	(
				  URL		href,
				  int		code,
				  Element	error,
				  String	description
				) throws IOException;

  } // ReportMultistatus



  /**
   * This is for handling <code>DAV:href</code> elements in a multistatus
   * response one by one.
   * @see Util#readUrlList
   * @author Werner Donn\u00e9
   */

  public interface UrlHandler

  {

    /**
     * @param href the resource in the multistatus response entry.
     * @return To stop processing <code>false</code> should be returned.
     */

    public boolean	handle	(URL href);

  } // UrlHandler

} // Util
