package be.re.webdav;

import be.re.io.DevNullInputStream;
import be.re.io.DevNullOutputStream;
import be.re.io.StreamConnector;
import be.re.net.BasicUser;
import be.re.net.HTTPClient;
import be.re.net.HTTPProtocolException;
import be.re.net.Headers;
import be.re.net.ProtocolException;
import be.re.net.User;
import be.re.util.Array;
import be.re.xml.ExpandedName;
import be.re.xml.sax.DevNullErrorHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;



/**
 * A client API for accessing a WebDAV server.
 * @author Werner Donn\u00e9
 */

public class Client

{

  private static final Set<String>	HTTP_METHODS =
    new HashSet<String>
    (
      Arrays.asList
      (
        new String[]
        {
          HTTPClient.DELETE,
          HTTPClient.GET,
          HTTPClient.HEAD,
          HTTPClient.OPTIONS,
          HTTPClient.POST,
          HTTPClient.TRACE
        }
      )
    );



  private Locale			locale = Locale.getDefault();
  private String			onBehalfOf;
  private String			password;
  private static TransformerFactory	transformerFactory;
  private static boolean		tunnel =
    System.getProperty("be.re.webdav.tunnel") != null;
  private String			userAgent;
  private String			username;
  private Object			webDavContext;



  static
  {
    try
    {
      transformerFactory = be.re.xml.Util.newTransformerFactory();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  /**
   * Calls the full constructor with all parameters set to <code>null</code>.
   */

  public
  Client()
  {
    this(null, null, null, null, null);
  }



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  Client(String username, String password)
  {
    this(username, password, null, null, null);
  }



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  Client(String username, String password, String onBehalfOf)
  {
    this(username, password, onBehalfOf, null, null);
  }



  /**
   * @param username if set it will be used for basic HTTP authentication.
   * @param password if set it will be used for basic HTTP authentication.
   * @param onBehalfOf if set it will be used as the value for the
   * <code>X-be.re.On-Behalf-Of</code> header. This feature is specific for
   * Pincette, where the <q>system</q> user honours the header to perform an
   * operation on behalf of another user. This makes it possible to use
   * Pincette as a resource in an application, in the same way database systems
   * can be used for example.
   * @param locale the default locale that will be assigned to the
   * <code>Accept-Language</code> header if it is not yet present.
   * @param userAgent if set it will override the <code>User-Agent</code>
   * header.
   */

  public
  Client
  (
    String	username,
    String	password,
    String	onBehalfOf,
    Locale	locale,
    String	userAgent
  )
  {
    this.username = username;
    this.password = password;
    this.onBehalfOf = onBehalfOf;
    this.locale = locale != null ? locale : Locale.getDefault();
    this.userAgent = userAgent;
  }



  /**
   * This is the constructor for local WebDAV-calls from Pincette modules.
   * @param username the user as who the call is performed.
   * @param context the WebDAV-context.
   */

  public
  Client(String username, Object context)
  {
    this(username, null, null, null, null);
    this.webDavContext = context;
  }



  /**
   * Sets the Access Control List for the resource. The Access Control Entries
   * are saved in the order they are provided.
   * @param url the resource.
   * @param aces the Access Control Entries.
   */

  public void
  acl(URL url, ACE[] aces) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    headersIn.set("Content-Type", "text/xml");

    Response	response =
      operation
      (
        HTTPClient.ACL,
        url,
        headersIn,
        prepareMessage(createAclBody(aces))
      );

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Adds a label to the resource. The resource should be checked in. The same
   * label on other versions of the resource will remain.
   * @param url the resource.
   * @param label the label that is to be added.
   */

  public void
  addLabel(URL url, String label) throws IOException, ProtocolException
  {
    label(url, "add", label, null, null);
  }



  /**
   * Adds a label to the resource. The resource should be checked in. The same
   * label on other versions of the resource will remain.
   * @param url the resource.
   * @param label the label that is to be added.
   * @param selectionLabel an optional label that overrides the default
   * version selection. The version carrying <code>selectionLabel</code> will
   * be the subject of the operation.
   * @param depth the scope of application of the label. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  addLabel(URL url, String label, String selectionLabel, String depth)
    throws IOException, ProtocolException
  {
    return addLabel(url, label, selectionLabel, depth, null);
  }



  /**
   * Adds a label to the resource. The resource should be checked in.
   * @param url the resource.
   * @param label the label that is to be added.
   * @param selectionLabel an optional label that overrides the default
   * version selection. The version carrying <code>selectionLabel</code> will
   * be the subject of the operation.
   * @param depth the scope of application of the label. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  addLabel
  (
    URL		url,
    String	label,
    String	selectionLabel,
    String	depth,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    return label(url, "add", label, selectionLabel, depth, lockToken);
  }



  private static Node
  appendForeign(Node node, ExpandedName name)
  {
    Element	element =
      (Element)
        node.appendChild
        (
          node.getOwnerDocument().
            createElementNS(name.namespaceURI, name.localName)
        );

    if (!node.getNamespaceURI().equals(name.namespaceURI))
    {
      element.
        setAttribute("xmlns", name.noNamespace() ? "" : name.namespaceURI);
    }

    return element;
  }



  private static Node
  appendTextElement(Node node, String name, String value)
  {
    return
      node.appendChild
      (
        node.getOwnerDocument().createElementNS(node.getNamespaceURI(), name)
      ).appendChild(node.getOwnerDocument().createTextNode(value));
  }



  /**
   * Returns the audit trail for a given resource. This method is specific for
   * Pincette.
   * @param url the repository URL.
   * @param path the absolute path of a resource within the repository. The
   * wildcard <code>*</code> is allowed. It may be <code>null</code>.
   * @param operations limits the audit trail to events with the given
   * operations. Operations are defined in <code>Constants.EVENT_*</code>. It
   * may be <code>null</code>.
   * @param username limits the audit trail to events involving the given user.
   * It may be <code>null</code>.
   * @param before limits the audit trail to events before the given timestamp
   * in milliseconds since 1 January 1970 UTC. It may be -1.
   * @param after limits the audit trail to events after the given timestamp
   * in milliseconds since 1 January 1970 UTC. It may be -1.
   * @param additionalInfo limits the audit trail to events having the given
   * additional information. The wildcard <code>*</code> is allowed. It may be
   * <code>null</code>.
   * @param handler an interface to which the events are passed one by one.
   * @see Constants
   */

  public void
  audit
  (
    URL		url,
    String	path,
    String[]	operations,
    String	username,
    long	before,
    long	after,
    String	additionalInfo,
    AuditTrail	handler
  ) throws IOException, ProtocolException
  {
    Response		response =
      operation
      (
        HTTPClient.AUDIT,
        url,
        new Headers(),
        prepareMessage
        (
          createAuditBody
          (
            path,
            operations,
            username,
            before,
            after,
            additionalInfo
          )
        )
      );
    InputStream		body = response.getBody();
    XMLEventReader	reader = null;

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }

      reader = Util.inputFactory.createXMLEventReader(body);
      collectEvents(url, reader, handler);
    }

    catch (IOException e)
    {
      throw e;
    }

    catch (Exception e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      response.close();

      try
      {
        if (reader != null)
        {
          reader.close();
        }
      }

      catch (Exception e)
      {
        throw new be.re.io.IOException(e);
      }

      body.close();
    }
  }



  /**
   * Creates an additional binding to an existing resource. Afterwards both
   * URLs denote the same resource.
   * @param url the existing resource
   * @param newBinding the new URL for the resource.
   * @param overwrite if set to <code>true</code> and there already is a
   * resource denoted by <code>newBinding</code>, then overwrite it.
   */

  public void
  bind(URL url, URL newBinding, boolean overwrite)
    throws IOException, ProtocolException
  {
    bind(url, newBinding, overwrite, null);
  }



  /**
   * Creates an additional binding to an existing resource. Afterwards both
   * URLs denote the same resource.
   * @param url the existing resource
   * @param newBinding the new URL for the resource.
   * @param overwrite if set to <code>true</code> and there already is a
   * resource denoted by <code>newBinding</code>, then overwrite it.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  bind(URL url, URL newBinding, boolean overwrite, String lockToken)
    throws IOException, ProtocolException
  {
    bindOrRebind(url, newBinding, true, overwrite, lockToken);
  }



  private void
  bindOrRebind
  (
    URL		url,
    URL		newBinding,
    boolean	bind,
    boolean	overwrite,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    headersIn.set("Overwrite", overwrite ? "T" : "F");

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response =
      operation
      (
        bind ? HTTPClient.BIND : HTTPClient.REBIND,
        Util.getParent(newBinding),
        headersIn,
        prepareMessage
        (
          createBindBody
          (
            bind ? "bind" : "rebind",
            getLastPathSegment(newBinding),
            url.toString()
          )
        )
      );

    try
    {
      if
      (
        response.getStatusCode() != 200	&&
        response.getStatusCode() != 201	&&
        response.getStatusCode() != 204
      )
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Checks in a resource. The parameters <code>keepCheckedOut</code> and
   * <code>forkOK</code> are set to <code>false</code> and
   * <code>lockToken</code> is set to <code>null</code>.
   * @param url the resource.
   * @return The newly created version.
   */

  public String
  checkin(URL url) throws IOException, ProtocolException
  {
    return checkin(url, false, false, null);
  }



  /**
   * Checks in a resource.
   * @param url the resource.
   * @param keepCheckedOut will automatically check out the resource after the
   * check-in.
   * @param forkOK when set to <code>true</code> forking a line of descent is
   * allowed.
   * @return The newly created version.
   */

  public String
  checkin(URL url, boolean keepCheckedOut, boolean forkOK)
    throws IOException, ProtocolException
  {
    return checkin(url, keepCheckedOut, forkOK, null);
  }



  /**
   * Checks in a resource.
   * @param url the resource.
   * @param keepCheckedOut will automatically check out the resource after the
   * check-in.
   * @param forkOK when set to <code>true</code> forking a line of descent is
   * allowed.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The newly created version.
   */

  public String
  checkin(URL url, boolean keepCheckedOut, boolean forkOK, String lockToken)
    throws IOException, ProtocolException
  {
    InputStream	body = null;
    Headers	headersIn = new Headers();

    if (keepCheckedOut || forkOK)
    {
      headersIn.set("Content-Type", "text/xml");
      body = prepareMessage(createCheckinBody(keepCheckedOut, forkOK));
    }

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response = operation(HTTPClient.CHECKIN, url, headersIn, body);

    try
    {
      if (response.getStatusCode() != 201)
      {
        Util.throwException(response);
      }

      return
        response.getHeaders().get("Location").length > 0 ?
          response.getHeaders().get("Location")[0] : null;
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Checks out a resource. The <code>activity</code> parameter is set to
   * <code>null</code>, <code>forkOK</code> to <code>false</code> and
   * <code>lockToken</code> to <code>null</code>.
   * @param url the resource.
   */

  public void
  checkout(URL url) throws IOException, ProtocolException
  {
    checkout(url, null, false, null);
  }



  /**
   * Checks out a resource.
   * @param url the resource.
   * @param activity the activity in which the resource should be checked out.
   * @param forkOK when set to <code>true</code> forking a line of descent is
   * allowed.
   */

  public void
  checkout(URL url, URL activity, boolean forkOK)
    throws IOException, ProtocolException
  {
    checkout(url, activity, forkOK, null);
  }



  /**
   * Checks out a resource.
   * @param url the resource.
   * @param activity the activity in which the resource should be checked out.
   * @param forkOK when set to <code>true</code> forking a line of descent is
   * allowed.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  checkout(URL url, URL activity, boolean forkOK, String lockToken)
    throws IOException, ProtocolException
  {
    InputStream	body = null;
    Headers	headersIn = new Headers();

    if (activity != null || forkOK)
    {
      headersIn.set("Content-Type", "text/xml");
      body =
        prepareMessage
        (
          createCheckoutBody(be.re.net.Util.stripUserInfo(activity), forkOK)
        );
    }

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response = operation(HTTPClient.CHECKOUT, url, headersIn, body);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  public void
  collapse(URL url, boolean noVersionControl, String depth)
    throws IOException, ProtocolException
  {
    testDepth(depth);

    Headers	headersIn = new Headers();

    headersIn.set(Constants.DEPTH_HEADER, depth);

    headersIn.set
    (
      Constants.NO_VERSION_CONTROL_HEADER,
      String.valueOf(noVersionControl)
    );

    Response	response = operation(HTTPClient.COLLAPSE, url, headersIn, null);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  private static void
  collectEvents(URL url, XMLEventReader reader, AuditTrail handler)
    throws Exception
  {
    for
    (
      Event e = readEvent(url, reader);
      e != null;
      e = readEvent(url, reader)
    )
    {
      if (!handler.handle(e))
      {
        return;
      }
    }
  }



  /**
   * Copies the resource denoted by <code>from</code> to the resource denoted
   * by <code>to</code>.
   * @param from the resource that is to be copied.
   * @param to the resource that is to be copied to.
   * @param label an optional label that overrides the default
   * version selection. The version carrying <code>label</code> will
   * be the subject of the operation.
   * @param overwrite if set the target will be overwritten if it already
   * exists.
   * @param depth the scope of the copy operation. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status codes 201 and 204 indicate success. See section
   * 9.8.5 of <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a> for
   * the complete specification of the status codes.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  copy(URL from, URL to, String label, boolean overwrite, String depth)
    throws IOException, ProtocolException
  {
    testDepth(depth);

    Headers	headersIn = new Headers();

    headersIn.set("Destination", be.re.net.Util.stripUserInfo(to).toString());
    headersIn.set("Overwrite", overwrite ? "T" : "F");
    headersIn.set(Constants.DEPTH_HEADER, depth);

    if (label != null)
    {
      headersIn.set
      (
        Constants.LABEL_HEADER,
        be.re.net.Util.escapeUriPathSegment(label)
      );
    }

    return operation(HTTPClient.COPY, from, headersIn, null);
  }



  private static Document
  createAclBody(ACE[] aces)
  {
    Document    result = Util.createDAVDocument("acl");

    for (int i = 0; i < aces.length; ++i)
    {
      Node	ace =
        result.getDocumentElement().
          appendChild(result.createElementNS(Constants.DAV_URI, "ace"));
      Node	principal =
        aces[i].invert ?
          ace.appendChild(result.createElementNS(Constants.DAV_URI, "invert")).
            appendChild
            (
              result.createElementNS(Constants.DAV_URI, "principal")
            ) :
            ace.appendChild
            (
              result.createElementNS(Constants.DAV_URI, "principal")
            );

      if (aces[i].principal != null)
      {
        if (aces[i].principal instanceof URL)
        {
          appendTextElement
          (
            principal,
            "href",
            be.re.net.Util.stripUserInfo((URL) aces[i].principal).toString()
          );
        }
        else
        {
          if (aces[i].principal instanceof String)
          {
            principal.appendChild
            (
              result.
                createElementNS(Constants.DAV_URI, aces[i].principal.toString())
            );
          }
          else
          {
            appendForeign
            (
              principal.appendChild
              (
                result.createElementNS(Constants.DAV_URI, "property")
              ),
              (ExpandedName) aces[i].principal
            );
          }
        }
      }

      Node	grantOrDeny =
        ace.appendChild
        (
          result.
            createElementNS(Constants.DAV_URI, aces[i].grant ? "grant" : "deny")
        );
      Node	privilege =
        grantOrDeny.
          appendChild(result.createElementNS(Constants.DAV_URI, "privilege"));

      if (aces[i].privilege != null)
      {
        appendForeign(privilege, aces[i].privilege);
      }

      if (aces[i].isProtected)
      {
        ace.appendChild(result.createElementNS(Constants.DAV_URI, "protected"));
      }
    }

    return result;
  }



  private static Document
  createAuditBody
  (
    String	path,
    String[]	operations,
    String	user,
    long	before,
    long	after,
    String	additionalInfo
  )
  {
    Document	result = Util.createReDocument("audit");

    if (path != null && !"".equals(path))
    {
      appendTextElement(result.getDocumentElement(), "path", path);
    }

    if (operations != null)
    {
      for (int i = 0; i < operations.length; ++i)
      {
        if (operations[i] != null && !"".equals(operations[i]))
        {
          appendTextElement
          (
            result.getDocumentElement(),
            "operation",
            operations[i]
          );
        }
      }
    }

    if (user != null && !"".equals(user))
    {
      appendTextElement(result.getDocumentElement(), "user", user);
    }

    if (before != -1)
    {
      appendTextElement
      (
        result.getDocumentElement(),
        "before",
        be.re.util.Util.createTimestamp(before)
      );
    }

    if (after != -1)
    {
      appendTextElement
      (
        result.getDocumentElement(),
        "after",
        be.re.util.Util.createTimestamp(after)
      );
    }

    if (additionalInfo != null && !"".equals(additionalInfo))
    {
      appendTextElement
      (
        result.getDocumentElement(),
        "additional-info",
        additionalInfo
      );
    }

    return result;
  }



  private static Document
  createBindBody(String name, String segment, String href) throws IOException
  {
    Document    result = Util.createDAVDocument(name);

    if (segment != null)
    {
      appendTextElement(result.getDocumentElement(), "segment", segment);
    }

    if (href != null)
    {
      appendTextElement
      (
        result.getDocumentElement(),
        "href",
        be.re.net.Util.stripUserInfo(new URL(href)).toString()
      );
    }

    return result;
  }



  private static Document
  createCheckinBody(boolean keepCheckedOut, boolean forkOK)
  {
    Document    result = Util.createDAVDocument("check-in");

    if (keepCheckedOut)
    {
      result.getDocumentElement().appendChild
      (
        result.createElementNS(Constants.DAV_URI, "keep-checked-out")
      );
    }

    if (forkOK)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "fork-ok"));
    }

    return result;
  }



  private static Document
  createCheckoutBody(URL activity, boolean forkOK)
  {
    Document    result = Util.createDAVDocument("check-out");

    if (activity != null)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "activity-set")).
        appendChild(result.createElementNS(Constants.DAV_URI, "href")).
        appendChild(result.createTextNode(activity.toString()));
    }

    if (forkOK)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "fork-ok"));
    }

    return result;
  }



  private static Document
  createLabelBody(String action, String label)
  {
    Document    result = Util.createDAVDocument("label");

    result.getDocumentElement().appendChild
    (
      result.createElementNS(Constants.DAV_URI, action)
    ).appendChild(result.createElementNS(Constants.DAV_URI, "label-name")).
      appendChild(result.createTextNode(label));

    return result;
  }



  private Document
  createLockBody(URL url)
  {
    Document    result = Util.createDAVDocument("lockinfo");

    result.getDocumentElement().
      appendChild(result.createElementNS(Constants.DAV_URI, "lockscope")).
      appendChild(result.createElementNS(Constants.DAV_URI, "exclusive"));
    result.getDocumentElement().
      appendChild(result.createElementNS(Constants.DAV_URI, "locktype")).
      appendChild(result.createElementNS(Constants.DAV_URI, "write"));

    User	user = getUser(url);

    if (user != null && user.getUsername() != null)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "owner")).
        appendChild(result.createTextNode(user.getUsername()));
    }

    return result;
  }



  private static Document
  createMergeBody
  (
    URL[]	sources,
    boolean	autoMerge,
    boolean	checkOut,
    URL		checkOutActivity,
    boolean	forkOK
  ) throws MalformedURLException
  {
    Document    result = Util.createDAVDocument("merge");
    Node	element =
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "source"));

    for (int i = 0; i < sources.length; ++i)
    {
      appendTextElement(element, "href", sources[i].toString());
    }

    if (!autoMerge)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "no-auto-merge"));
    }

    if (!checkOut)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "no-checkout"));
    }

    if (checkOutActivity != null)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "activity-set")).
        appendChild(result.createElementNS(Constants.DAV_URI, "href")).
        appendChild(result.createTextNode(checkOutActivity.toString()));
    }

    if (forkOK)
    {
      result.getDocumentElement().
        appendChild(result.createElementNS(Constants.DAV_URI, "fork-ok"));
    }

    return result;
  }



  private static Document
  createOptionsBody
  (
    boolean	activityCollections,
    boolean	metaCollections,
    boolean	versionHistoryCollections,
    boolean	viewCollections
  )
  {
    Document    result = Util.createDAVDocument("options");

    if (activityCollections)
    {
      result.getDocumentElement().appendChild
      (
        result.createElementNS(Constants.DAV_URI, "activity-collection-set")
      );
    }

    if (metaCollections)
    {
      appendForeign
      (
        result.getDocumentElement(),
        new ExpandedName(Constants.URI, "meta-collection-set")
      );
    }

    if (versionHistoryCollections)
    {
      result.getDocumentElement().appendChild
      (
        result.
          createElementNS(Constants.DAV_URI, "version-history-collection-set")
      );
    }

    if (viewCollections)
    {
      appendForeign
      (
        result.getDocumentElement(),
        new ExpandedName(Constants.URI, "view-collection-set")
      );
    }

    return result;
  }



  private static Document
  createPropfindBody(boolean all, boolean names, ExpandedName[] properties)
  {
    Document    result = Util.createDAVDocument("propfind");

    if (all)
    {
      result.getDocumentElement().appendChild
      (
        result.createElementNS(Constants.DAV_URI, "allprop")
      );
    }
    else
    {
      if (names)
      {
        result.getDocumentElement().
          appendChild(result.createElementNS(Constants.DAV_URI, "propname"));
      }
      else
      {
        Node	prop =
          result.getDocumentElement().
            appendChild(result.createElementNS(Constants.DAV_URI, "prop"));

        for (int i = 0; i < properties.length; ++i)
        {
          appendForeign(prop, properties[i]);
        }
      }
    }

    return result;
  }



  private static Document
  createProppatchBody(SimpleProperty[] properties)
  {
    String[]	withElement =
      new String[]{"auto-version", "checkin-fork", "checkout-fork"};
    Document    result = Util.createDAVDocument("propertyupdate");

    for (int i = 0; i < properties.length; ++i)
    {
      appendForeign
      (
        result.getDocumentElement().
          appendChild(result.createElementNS(Constants.DAV_URI, "set")).
          appendChild(result.createElementNS(Constants.DAV_URI, "prop")),
        properties[i].name
      ).appendChild
        (
          properties[i].value.length() > 0 &&
            Array.inArray(withElement, properties[i].name.localName) ?
            (Node)
              result.createElementNS(Constants.DAV_URI, properties[i].value) :
            (Node) result.createTextNode(properties[i].value)
        );
    }

    return result;
  }



  private static String
  createQValue(String[] value)
  {
    String	result = "";

    for (int i = 0; i < value.length; ++i)
    {
      result +=
        (result.equals("") ? "" : ", ") + value[i] + ";q=" +
          (((int) ((value.length - i) * 100 / value.length)) / 100);
    }

    return result;
  }



  /**
   * Deletes a resource.
   * @param url the resource.
   */

  public void
  delete(URL url) throws IOException, ProtocolException
  {
    delete(url, null);
  }



  /**
   * Deletes a resource.
   * @param url the resource.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  delete(URL url, String lockToken) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response = operation(HTTPClient.DELETE, url, headersIn, null);

    try
    {
      if (response.getStatusCode() != 204 && response.getStatusCode() != 200)
                                             // Some servers return this.
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Dispatches to a local WebDAV-handler.
   */

  private InputStream
  dispatchLocally
  (
    String	method,
    URL		url,
    InputStream	body,
    String	username,
    Headers	headersIn,
    Headers	headersOut
  ) throws IOException, ProtocolException
  {
    try
    {
      // Use reflection in order to avoid compiling in server-side classes for
      // remote clients.

      return
        (InputStream)
          Class.forName("be.re.repo.LocalDispatch").getDeclaredMethod
          (
            "dispatch",
            new Class[]
            {
              String.class, URL.class, InputStream.class, String.class,
                Headers.class, Headers.class, Object.class
            }
          ).invoke
            (
              null,
              new Object[]
              {
                method, url, body, username, headersIn, headersOut,
                  webDavContext
              }
            );
    }

    catch (InvocationTargetException e)
    {
      if (headersOut.get("Status-Code").length > 0)
      {
        throw new ProtocolException(e.getCause());
      }

      if (e.getCause() instanceof IOException)
      {
        throw (IOException) e.getCause();
      }

      throw new be.re.io.IOException(e.getCause());
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public boolean
  exists(URL url) throws IOException
  {
    try
    {
      head(url);

      return true;
    }

    catch (ProtocolException e)
    {
      return false;
    }
  }



  /**
   * Returns a resource.
   * @param url the URL of the resource to get. It should be either a version
   * controlled resource or a version resource.
   * @return The response should be closed.
   */

  public Response
  get(URL url) throws IOException, ProtocolException
  {
    return get(url, null, null, null);
  }



  /**
   * Returns a resource.
   * @param url the URL of the resource to get. It should be either a version
   * controlled resource or a version resource.
   * @param preferredMimeTypes the MIME types in which form the result is
   * desired, ordered from most to least preferred. It may be <code>null</code>
   * or empty.
   * @param preferredLanguages the languages in which form the result is
   * desired, ordered from most to least preferred. It may be <code>null</code>
   * or empty.
   * @param label an optional label that overrides the default
   * version selection. The version carrying <code>label</code> will
   * be the subject of the operation.
   * @return The response should be closed.
   */

  public Response
  get
  (
    URL		url,
    String[]	preferredMimeTypes,
    String[]	preferredLanguages,
    String	label
  ) throws IOException, ProtocolException
  {
    return get(url, preferredMimeTypes, preferredLanguages, label, -1, null);
  }



  /**
   * Returns a resource.
   * @param url the URL of the resource to get. It should be either a version
   * controlled resource or a version resource.
   * @param preferredMimeTypes the MIME types in which form the result is
   * desired, ordered from most to least preferred. It may be <code>null</code>
   * or empty.
   * @param preferredLanguages the languages in which form the result is
   * desired, ordered from most to least preferred. It may be <code>null</code>
   * or empty.
   * @param label an optional label that overrides the default
   * version selection. The version carrying <code>label</code> will
   * be the subject of the operation.
   * @param compareWith a version of the same version controlled resource. It
   * may be <code>null</code>. This parameter is specific for Pincette.
   * @return The response should be closed.
   */

  public Response
  get
  (
    URL		url,
    String[]	preferredMimeTypes,
    String[]	preferredLanguages,
    String	label,
    URL		compareWith
  ) throws IOException, ProtocolException
  {
    return
      get(url, preferredMimeTypes, preferredLanguages, label, -1, compareWith);
  }



  /**
   * Returns a resource.
   * @param url the URL of the resource to get. It should be either a version
   * controlled resource or a version resource.
   * @param preferredMimeTypes the MIME types in which form the result is
   * desired, ordered from most to least preferred. It may be <code>null</code>
   * or empty.
   * @param preferredLanguages the languages in which form the result is
   * desired, ordered from most to least preferred. It may be <code>null</code>
   * or empty.
   * @param label an optional label that overrides the default
   * version selection. The version carrying <code>label</code> will
   * be the subject of the operation.
   * @param modifiedSince the timestamp in milliseconds since 1 January 1970
   * UTC. It may be -1 if an unconditional GET is desired. Otherwise the
   * resource is only returned if it has changed since the given timestamp. If
   * it hasn't, the status code is set to 304.
   * @return The response should be closed.
   */

  public Response
  get
  (
    URL		url,
    String[]	preferredMimeTypes,
    String[]	preferredLanguages,
    String	label,
    long	modifiedSince
  ) throws IOException, ProtocolException
  {
    return
      get
      (
        url,
        preferredMimeTypes,
        preferredLanguages,
        label,
        modifiedSince,
        null
      );
  }



  private Response
  get
  (
    URL		url,
    String[]	preferredMimeTypes,
    String[]	preferredLanguages,
    String	label,
    long	modifiedSince,
    URL		compareWith
  ) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    if (label != null)
    {
      headersIn.
        set(Constants.LABEL_HEADER, be.re.net.Util.escapeUriPathSegment(label));
    }

    if (modifiedSince != -1)
    {
      headersIn.set
      (
        Constants.IF_MODIFIED_SINCE_HEADER,
        be.re.net.Util.httpDate(modifiedSince)
      );
    }

    if (compareWith != null)
    {
      headersIn.set
      (
        Constants.COMPARE_WITH_HEADER,
        compareWith.toString()
      );
    }

    if (preferredMimeTypes != null && preferredMimeTypes.length > 0)
    {
      headersIn.set("Accept", createQValue(preferredMimeTypes));
    }

    if (preferredLanguages != null && preferredLanguages.length > 0)
    {
      headersIn.set("Accept-Language", createQValue(preferredLanguages));
    }

    return operation(HTTPClient.GET, url, headersIn, null);
  }



  private static Element
  getAcl(Document document)
  {
    return
      be.re.xml.Util.selectElement
      (
        document.getDocumentElement(),
        new ExpandedName[]
        {
          new ExpandedName(Constants.DAV_URI, "response"),
          new ExpandedName(Constants.DAV_URI, "propstat"),
          new ExpandedName(Constants.DAV_URI, "prop"),
          new ExpandedName(Constants.DAV_URI, "acl")
        }
      );
  }



  /**
   * Returns the Access Control List of a resource.
   */

  public ACE[]
  getAcl(URL url) throws IOException, ProtocolException
  {
    Response	response =
      propfindSpecific
      (
        url,
        new ExpandedName[]
        {new ExpandedName(Constants.DAV_URI, "acl")},
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
        return new ACE[0];
      }

      Util.checkPropstatStatus(url, HTTPClient.PROPFIND, document);

      Element	acl = getAcl(document);

      return acl == null ? new ACE[0] : Util.getAces(url, acl);
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Returns the resource collections in the repository denoted by
   * <code>url</code> that contain activities.
   */

  public URL[]
  getActivityCollections(URL url) throws IOException, ProtocolException
  {
    return getCollections(url, Constants.DAV_URI, "activity-collection-set");
  }



  private URL[]
  getCollections(URL url, String namespaceURI, String localName)
    throws IOException, ProtocolException
  {
    Response	response =
      optionsInternal
      (
        url,
        "activity-collection-set".equals(localName),
        "meta-collection-set".equals(localName),
        "version-history-collection-set".equals(localName),
        "view-collection-set".equals(localName)
      );

    try
    {
      return
        getCollections
        (
          url,
          response,
          response.createDocument(),
          namespaceURI,
          localName
        );
    }

    finally
    {
      response.close();
    }
  }



  private URL[]
  getCollections
  (
    URL		url,
    Response	response,
    Document	body,
    String	namespaceURI,
    String	localName
  ) throws IOException, ProtocolException
  {
    if
    (
      (
        "OPTIONS".equals(response.getMethod())	&&
        response.getStatusCode() != 200
      )						||
      (
        "PROPFIND".equals(response.getMethod())	&&
        response.getStatusCode() != 207
      )
    )
    {
      Util.throwException(response);
    }

    if (body == null)
    {
      return new URL[0];
    }

    Node	node =
      "OPTIONS".equals(response.getMethod()) ?
        be.re.xml.Util.selectFirstChild
        (
          body.getDocumentElement(),
          namespaceURI,
          localName
        ) :
        be.re.xml.Util.selectElement
        (
          body.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "principal-collection-set")
          }
        );

    if (node == null)
    {
      return new URL[0];
    }

    Node[]	nodes =
      be.re.xml.Util.
        selectChildren(node, be.re.webdav.Constants.DAV_URI, "href");
    List	result = new ArrayList();
    String	userInfo = url.getUserInfo();

    for (int i = 0; i < nodes.length; ++i)
    {
      result.add
      (
        userInfo != null ?
          be.re.net.Util.setUserInfo
          (
            new URL(url, be.re.xml.Util.getText(nodes[i])),
            userInfo
          ) : new URL(url, be.re.xml.Util.getText(nodes[i]))
      );
    }

    return (URL[]) result.toArray(new URL[0]);
  }



  private static String
  getLastPathSegment(URL url)
  {
    String	segment = be.re.net.Util.getLastPathSegment(url);

    return
      segment.endsWith("/") ?
        segment.substring(0, segment.length() - 1) : segment;
  }



  /**
   * Returns the default locale that will be assigned to the
   * <code>Accept-Language</code> header if it is not yet present.
   */

  public Locale
  getLocale()
  {
    return locale;
  }



  /**
   * Returns all the lock tokens of a resource.
   */

  public String[]
  getLockTokens(URL url) throws IOException, ProtocolException
  {
    Lock[]	locks = getLocks(url);
    String[]	tokens = new String[locks.length];

    for (int i = 0; i < locks.length; ++i)
    {
      tokens[i] = locks[i].token;
    }

    return tokens;
  }



  /**
   * Returns all the locks of a resource.
   */

  public Lock[]
  getLocks(URL url) throws IOException, ProtocolException
  {
    Response	response =
      propfindSpecific
      (
        url,
        new ExpandedName[]
        {new ExpandedName(Constants.DAV_URI, "lockdiscovery")},
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
        return null;
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
            new ExpandedName(Constants.DAV_URI, "lockdiscovery"),
            new ExpandedName(Constants.DAV_URI, "activelock")
          }
        );

      if (elements.length == 0)
      {
        return new Lock[0];
      }

      int	statusCode =
        Util.getPropertyStatusCode
        (
          (Element) elements[0].getParentNode().getParentNode()
        );

      if (statusCode != 200)
      {
        Util.throwException(url, HTTPClient.PROPFIND, statusCode);
      }

      Lock[]	locks = new Lock[elements.length];

      for (int i = 0; i < locks.length; ++i)
      {
        locks[i] = readLock(elements[i]);
      }

      return locks;
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Returns the resource collections in the repository denoted by
   * <code>url</code> that contain functional meta collections. This method is
   * specific to Pincette.
   */

  public URL[]
  getMetaCollections(URL url) throws IOException, ProtocolException
  {
    return getCollections(url, Constants.URI, "meta-collection-set");
  }



  /**
   * Returns the default value for the <code>X-be.re.On-Behalf-Of</code> header.
   * A system user can use this to perform requests on behalf of another user.
   * This way a WebDAV server can be used as a resource by another service, much
   * like a database system.
   */

  public String
  getOnBehalfOf()
  {
    return onBehalfOf;
  }



  /**
   * Return the default password for basic HTTP authentication.
   */

  public String
  getPassword()
  {
    return password;
  }



  /**
   * Returns the resource collections in the repository denoted by
   * <code>url</code> that contain principals.
   */

  public URL[]
  getPrincipalCollections(URL url) throws IOException, ProtocolException
  {
    Response	response =
      propfindSpecific
      (
        url,
        new ExpandedName[]
        {new ExpandedName(Constants.DAV_URI, "principal-collection-set")},
        "0"
      );

    try
    {
      return
        getCollections
        (
          url,
          response,
          response.createDocument(),
          Constants.DAV_URI,
          "principal-collection-set"
        );
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Returns the value of a live property as a string if it has either one empty
   * element as a value, in which case the local name of the element is the
   * value, or only PCDATA, otherwise <code>null</code> is
   * returned.
   */

  public String
  getSimpleLiveProperty(URL url, ExpandedName property)
    throws IOException, ProtocolException
  {
    Response	response =
      propfindSpecific(url, new ExpandedName[]{property}, "0");

    try
    {
      if (response.getStatusCode() != 207)
      {
        Util.throwException(response);
      }

      Document	document = response.createDocument();

      if (document == null)
      {
        return null;
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
            property
          }
        );

      if (elements.length != 1)
      {
        return null;
      }

      int	statusCode = getSimpleLivePropertyStatusCode(elements[0]);

      if (statusCode == 404)
      {
        return null;
      }

      if (statusCode != 200)
      {
        Util.throwException(url, HTTPClient.PROPFIND, statusCode);
      }

      Element[]	children = be.re.xml.Util.selectElements(elements[0]);

      return
        children.length == 1 ?
          children[0].getLocalName() : be.re.xml.Util.getText(elements[0]);
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Returns the value of a live property in the <code>DAV:</code> namespace
   * as a string if it has one empty element as a value, in which case the local
   * name of the element is the value, or only PCDATA,
   * otherwise <code>null</code> is returned.
   */

  public String
  getSimpleLiveProperty(URL url, String property)
    throws IOException, ProtocolException
  {
    return
      getSimpleLiveProperty(url, new ExpandedName(Constants.DAV_URI, property));
  }



  private static Node
  getSimpleLivePropertyError(Element property)
  {
    Node	description =
      be.re.xml.Util.selectNextSibling
      (
        property.getParentNode(),
        Constants.DAV_URI,
        "responsedescription"
      );

    if (description == null)
    {
      return null;
    }

    return
      be.re.xml.Util.selectFirstChild(description, Constants.DAV_URI, "error");
  }



  private static int
  getSimpleLivePropertyStatusCode(Element property)
  {
    return Util.getPropertyStatusCode((Element) property.getParentNode());
  }



  /**
   * Returns all the live properties supported by the resource.
   */

  public ExpandedName[]
  getSupportedLiveProperties(URL url) throws IOException, ProtocolException
  {
    Response	response =
      propfindSpecific
      (
        url,
        new ExpandedName[]
        {new ExpandedName(Constants.DAV_URI, "supported-live-property-set")},
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
            new ExpandedName(Constants.DAV_URI, "supported-live-property-set"),
            new ExpandedName(Constants.DAV_URI, "supported-live-property"),
            new ExpandedName(Constants.DAV_URI, "prop")
          }
        );
      List	result = new ArrayList();

      for (int i = 0; i < elements.length; ++i)
      {
        Element[]	properties = be.re.xml.Util.selectElements(elements[i]);

        for (int j = 0; j < properties.length; ++j)
        {
          if (Constants.DAV_URI.equals(properties[j].getNamespaceURI()))
          {
            result.add
            (
              new ExpandedName
              (
                properties[j].getNamespaceURI(),
                properties[j].getLocalName()
              )
            );
          }
        }
      }

      return (ExpandedName[]) result.toArray(new ExpandedName[0]);
    }

    finally
    {
      response.close();
    }
  }



  private User
  getUser(URL url)
  {
    User	previousUser =
      HTTPClient.getAuthenticator() == null ?
        new BasicUser() :
        HTTPClient.getAuthenticator().
          usedPreviously(url.toString(), url.getProtocol());

    if (previousUser == null)
    {
      previousUser = new BasicUser();
    }

    if (username != null)
    {
      previousUser.setUsername(username);
    }

    if (password != null)
    {
      previousUser.setPassword(password);
    }

    return previousUser;
  }



  /**
   * Returns the default value for the <code>User-Agent</code> header.
   */

  public String
  getUserAgent()
  {
    return userAgent;
  }



  /**
   * Returns the default username for basic HTTP authentication.
   */

  public String
  getUsername()
  {
    return username;
  }



  /**
   * Returns the resource collections in the repository denoted by
   * <code>url</code> that contain version histories.
   */

  public URL[]
  getVersionHistoryCollections(URL url) throws IOException, ProtocolException
  {
    return
      getCollections(url, Constants.DAV_URI, "version-history-collection-set");
  }



  /**
   * Returns the resource collections in the repository denoted by
   * <code>url</code> that contain views. This method is specific to Pincette.
   */

  public URL[]
  getViewCollections(URL url) throws IOException, ProtocolException
  {
    return getCollections(url, Constants.URI, "view-collection-set");
  }



  /**
   * Returns the headers of a resource.
   */

  public Headers
  head(URL url) throws IOException, ProtocolException
  {
    Response	response = operation(HTTPClient.HEAD, url, new Headers(), null);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }

      return response.getHeaders();
    }

    finally
    {
      response.close();
    }
  }



  /**
   * This is an administration method that reindexes a WebDAV-collection in
   * Pincette.
   * @param url the collection.
   * @param depth the WebDAV <code>Depth</code> header.
   * @param withPreview when set to <code>true</code> the previews are also
   * re-extracted next to full-text indexing.
   */

  public void
  index(URL url, String depth, boolean withPreview)
    throws IOException, ProtocolException
  {
    testDepth(depth);

    Headers	headersIn = new Headers();

    headersIn.set(Constants.DEPTH_HEADER, depth);

    if (withPreview)
    {
      headersIn.set(Constants.WITH_PREVIEW_HEADER, "true");
    }

    Response	response = operation(HTTPClient.INDEX, url, headersIn, null);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  private void
  label
  (
    URL		url,
    String	action,
    String	label,
    String	selectionLabel,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    Response	response =
      label(url, action, label, selectionLabel, null, lockToken);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  private Response
  label
  (
    URL		url,
    String	action,
    String	label,
    String	selectionLabel,
    String	depth,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    testDepth(depth);

    Headers	headersIn = new Headers();

    headersIn.set("Content-Type", "text/xml");

    if (depth != null)
    {
      headersIn.set(Constants.DEPTH_HEADER, depth);
    }

    if (selectionLabel != null)
    {
      headersIn.set
      (
        Constants.LABEL_HEADER,
        be.re.net.Util.escapeUriPathSegment(selectionLabel)
      );
    }

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    return
      operation
      (
        HTTPClient.LABEL,
        url,
        headersIn,
        prepareMessage(createLabelBody(action, label))
      );
  }



  /**
   * Locks a resource.
   * @param url the resource to be locked.
   * @param timeoutSeconds the time in seconds the lock should last. If the
   * lock hasn't been refreshed before this time has passed, it will be
   * automatically released. The value -1 indicates an infinite timeout. Note
   * that a server may impose restrictions of its own.
   * @return The lock token.
   */

  public String
  lock(URL url, long timeoutSeconds) throws IOException, ProtocolException
  {
    return lock(url, timeoutSeconds, null);
  }



  /**
   * Locks a resource.
   * @param url the resource to be locked.
   * @param timeoutSeconds the time in seconds the lock should last. If the
   * lock hasn't been refreshed before this time has passed, it will be
   * automatically released. The value -1 indicates an infinite timeout. Note
   * that a server may impose restrictions of its own.
   * @param lockToken a lock token. It may be <code>null</code>. If it isn't it
   * denotes the lock to refresh.
   * @return The lock token.
   */

  public String
  lock(URL url, long timeoutSeconds, String lockToken)
    throws IOException, ProtocolException
  {
    InputStream	body = null;
    Headers	headersIn = new Headers();

    headersIn.set
    (
      "Timeout",
      timeoutSeconds == -1 ?
        "Infinite" : ("Second-" + String.valueOf(timeoutSeconds))
    );

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response =
      operation
      (
        HTTPClient.LOCK,
        url,
        headersIn,
        lockToken == null ? prepareMessage(createLockBody(url)) : null
      );

    try
    {
      if (response.getStatusCode() != 200 && response.getStatusCode() != 201)
      {
        Util.throwException(response);
      }

      return be.re.webdav.Util.getLockToken(response);
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Merges a number of resources into another one. When collections are
   * involved the merger will be recursive.
   * @param url the resource to merge into.
   * @param sources the resources that are to be merged.
   * @param label and optional label that overrides the default
   * version selection. For all the sources the version carrying
   * <code>label</code> will be the subject of the operation. This parameter is
   * specific to Pincette.
   * @param autoMerge allow the server to perform an automatic merge if it can.
   * Otherwise the merge is only registered and should be completed manually.
   * @param checkOut allow the server to check out a resource if needed.
   * @param checkOutActivity the activity in which the automatic check-out
   * should occur.
   * @param forkOK when set to <code>true</code> forking a line of descent is
   * allowed.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> to process the
   * response, where the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  merge
  (
    URL		url,
    URL[]	sources,
    String	label,
    boolean	autoMerge,
    boolean	checkOut,
    URL		checkOutActivity,
    boolean	forkOK
  ) throws IOException, ProtocolException
  {
    for (int i = 0; i < sources.length; ++i)
    {
      sources[i] = be.re.net.Util.stripUserInfo(sources[i]);
    }

    InputStream	body =
      prepareMessage
      (
        createMergeBody(sources, autoMerge, checkOut, checkOutActivity, forkOK)
      );
    Headers	headersIn = new Headers();

    headersIn.set("Content-Type", "text/xml");

    if (label != null)
    {
      headersIn.set
      (
        Constants.LABEL_HEADER,
        be.re.net.Util.escapeUriPathSegment(label)
      );
    }

    return operation(HTTPClient.MERGE, url, headersIn, body);
  }



  /**
   * Creates an activity.
   */

  public void
  mkactivity(URL url) throws IOException, ProtocolException
  {
    Response	response =
      operation(HTTPClient.MKACTIVITY, url, new Headers(), null);

    try
    {
      if (response.getStatusCode() != 201)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Creates a resource collection.
   */

  public void
  mkcol(URL url) throws IOException, ProtocolException
  {
    Response	response =
      operation(HTTPClient.MKCOL, url, new Headers(), null);

    try
    {
      if (response.getStatusCode() != 201)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Creates a view. This method is specific for Pincette.
   */

  public void
  mkview(URL url) throws IOException, ProtocolException
  {
    Response	response =
      operation(HTTPClient.MKVIEW, url, new Headers(), null);

    try
    {
      if (response.getStatusCode() != 201)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Moves one resource to another. Within a server this corresponds to a
   * rename.
   * @param from the resource to be moved.
   * @param to the new URL for the resource.
   * @param overwrite if set to <code>true</code> and there already is a
   * resource denoted by <code>to</code>, then overwrite it.
   */

  public void
  move(URL from, URL to, boolean overwrite)
    throws IOException, ProtocolException
  {
    move(from, to, overwrite, null);
  }



  /**
   * Moves one resource to another. Within a server this corresponds to a
   * rename.
   * @param from the resource to be moved.
   * @param to the new URL for the resource.
   * @param overwrite if set to <code>true</code> and there already is a
   * resource denoted by <code>to</code>, then overwrite it.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  move(URL from, URL to, boolean overwrite, String lockToken)
    throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    headersIn.set("Destination", be.re.net.Util.stripUserInfo(to).toString());
    headersIn.set("Overwrite", overwrite ? "T" : "F");

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response = operation(HTTPClient.MOVE, from, headersIn, null);

    try
    {
      if (response.getStatusCode() != 201 && response.getStatusCode() != 204)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * The low-level method that communicates with the server.
   * @param method the method.
   * @param url the URL of the resource.
   * @param headersIn the header that will be set on the request.
   * @param body the request body.
   * @return The response from the server.
   */

  public Response
  operation(String method, URL url, Headers headersIn, InputStream body)
    throws IOException, ProtocolException
  {
    if
    (
      !Array.inArray(new String[]{"http", "https", "local"}, url.getProtocol())
    )
    {
      throw
        new IllegalArgumentException
        (
          "Unsupported protocol \"" + url.getProtocol() + "\"."
        );
    }

    url = be.re.net.Util.escapedUrl(url.toString(), true);
      // Path segment parameters are not supported.

    Headers	headersOut = new Headers();
    User	user =
      "local".equals(url.getProtocol()) ?
        new BasicUser(username, password) : getUser(url);

    if (locale != null && headersIn.get("Accept-Language").length == 0)
    {
      headersIn.set("Accept-Language", be.re.util.Util.getLanguageTag(locale));
    }

    if (userAgent != null)
    {
      headersIn.set("User-Agent", userAgent);
    }

    if (onBehalfOf != null)
    {
      headersIn.set(Constants.ON_BEHALF_OF_HEADER, onBehalfOf);
    }

    try
    {
      return
        new Response
        (
          url,
          method,
          headersOut,
          "local".equals(url.getProtocol()) ?
            dispatchLocally
            (
              method,
              url,
              body,
              user != null ? user.getUsername() : null,
              headersIn,
              headersOut
            ) :
            request
            (
              method,
              url,
              body, 
              user != null ? user.getUsername() : null,
              user != null ? user.getPassword() : null,
              headersIn,
              headersOut
            )
        );
    }

    catch (ProtocolException e)
    {
      if (headersOut.get("Status-Code").length > 0)
      {
        return new Response(url, method, headersOut);
      }

      throw e;
    }
  }



  /**
   * Returns the options for a resource.
   * @param url the resource.
   * @param activityCollections if set to <code>true</code> the collections
   * that contain activities will be requested.
   * @param metaCollections if set to <code>true</code> the collections
   * that contain functional meta collections will be requested. This is
   * specific to Pincette.
   * @param versionHistoryCollections if set to <code>true</code> the
   * collections that contain version histories will be requested.
   * @param viewCollections if set to <code>true</code> the collections
   * that contain views will be requested. This is specific to Pincette.
   */

  public Options
  options
  (
    URL		url,
    boolean	activityCollections,
    boolean	metaCollections,
    boolean	versionHistoryCollections,
    boolean	viewCollections
  ) throws IOException, ProtocolException
  {
    Response	response =
      optionsInternal
      (
        url,
        activityCollections,
        metaCollections,
        versionHistoryCollections,
        viewCollections
      );

    try
    {
      Document	body = response.createDocument();
      Options	result = new Options();

      if (activityCollections)
      {
        result.activityCollectionSet =
          getCollections
          (
            url,
            response,
            body,
            Constants.DAV_URI,
            "activity-collection-set"
          );
      }

      if (metaCollections)
      {
        result.metaCollectionSet =
          getCollections
          (
            url,
            response,
            body,
            Constants.URI,
            "meta-collection-set"
          );
      }

      if (versionHistoryCollections)
      {
        result.versionHistoryCollectionSet =
          getCollections
          (
            url,
            response,
            body,
            Constants.DAV_URI,
            "version-history-collection-set"
          );
      }

      if (viewCollections)
      {
        result.viewCollectionSet =
          getCollections
          (
            url,
            response,
            body,
            Constants.URI,
            "view-collection-set"
          );
      }

      result.headers = response.getHeaders();
      result.allowedMethods = response.getHeaders().getValuesFromList("Allow");
      result.davFeatures = response.getHeaders().getValuesFromList("DAV");

      if (result.davFeatures.length > 1 && "2".equals(result.davFeatures[1]))
      {
        result.davVersion = 2;

        String[]	newArray = new String[result.davFeatures.length - 2];

        System.arraycopy(result.davFeatures, 2, newArray, 0, newArray.length);
        result.davFeatures = newArray;
      }
      else
      {
        if (result.davFeatures.length > 0 && "1".equals(result.davFeatures[0]))
        {
          result.davVersion = 1;

          String[]	newArray = new String[result.davFeatures.length - 1];

          System.arraycopy(result.davFeatures, 1, newArray, 0, newArray.length);
          result.davFeatures = newArray;
        }
      }

      return result;
    }

    finally
    {
      response.close();
    }
  }



  private Response
  optionsInternal
  (
    URL		url,
    boolean	activityCollections,
    boolean	metaCollections,
    boolean	versionHistoryCollections,
    boolean	viewCollections
  ) throws IOException, ProtocolException
  {
    InputStream	body = null;
    Headers	headersIn = new Headers();

    if
    (
      activityCollections	||
      metaCollections		||
      versionHistoryCollections	||
      viewCollections
    )
    {
      headersIn.set("Content-Type", "text/xml");
      body =
        prepareMessage
        (
          createOptionsBody
          (
            activityCollections,
            metaCollections,
            versionHistoryCollections,
            viewCollections
          )
        );
    }

    return operation(HTTPClient.OPTIONS, url, headersIn, body);
  }



  private static InputStream
  prepareMessage(Document document)
  {
    try
    {
      ByteArrayOutputStream	out = new ByteArrayOutputStream();
      Transformer 		transformer =
        transformerFactory.newTransformer();

      transformer.setOutputProperty("indent" , "yes");
      transformer.transform(new DOMSource(document), new StreamResult(out));

      return new ByteArrayInputStream(out.toByteArray());
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private Response
  propfind
  (
    URL			url,
    boolean		all,
    boolean		names,
    ExpandedName[]	properties,
    String		depth
  ) throws IOException, ProtocolException
  {
    testDepth(depth);

    InputStream	body = null;
    Headers	headersIn = new Headers();

    headersIn.set(Constants.DEPTH_HEADER, depth);

    if (all || names || properties != null)
    {
      headersIn.set("Content-Type", "text/xml");
      body = prepareMessage(createPropfindBody(all, names, properties));
    }

    return operation(HTTPClient.PROPFIND, url, headersIn, body);
  }



  /**
   * Returns all the properties of a resource.
   * @param url the resource.
   * @param depth the scope of the operation. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response is defined in section 9.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>. It should be
   * closed after consumption.
   */

  public Response
  propfindAll(URL url, String depth) throws IOException, ProtocolException
  {
    return propfind(url, true, false, null, depth);
  }



  /**
   * Returns all the property names of a resource.
   * @param url the resource.
   * @param depth the scope of the operation. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response is defined in section 9.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>. It should be
   * closed after consumption.
   */

  public Response
  propfindNames(URL url, String depth) throws IOException, ProtocolException
  {
    return propfind(url, false, true, null, depth);
  }



  /**
   * Returns all the property names of a resource.
   */

  public ExpandedName[]
  propfindNames(URL url) throws IOException, ProtocolException
  {
    Response	response = propfind(url, false, true, null, "0");

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
            new ExpandedName(Constants.DAV_URI, "prop")
          }
        );
      List	result = new ArrayList();

      for (int i = 0; i < elements.length; ++i)
      {
        Element[]	properties = be.re.xml.Util.selectElements(elements[i]);

        for (int j = 0; j < properties.length; ++j)
        {
          result.add
          (
            new ExpandedName
            (
              properties[j].getNamespaceURI(),
              properties[j].getLocalName()
            )
          );
        }
      }

      return (ExpandedName[]) result.toArray(new ExpandedName[0]);
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Returns the requested properties of a resource.
   * @param url the resource.
   * @param properties the requested properties.
   * @param depth the scope of the operation. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response is defined in section 9.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>. It should be
   * closed after consumption.
   */

  public Response
  propfindSpecific(URL url, ExpandedName[] properties, String depth)
    throws IOException, ProtocolException
  {
    return propfind(url, false, false, properties, depth);
  }



  /**
   * Changes properties of the resource.
   * @param url the resource.
   * @param in the request body. It is specified in section 9.2 of <a
   * href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> to process the
   * response. See section 9.2.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a> for the
   * specification of the status codes.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  proppatch(URL url, InputStream in) throws IOException, ProtocolException
  {
    return proppatch(url, in, null);
  }



  /**
   * Changes properties of the resource.
   * @param url the resource.
   * @param in the request body. It is specified in section 9.2 of <a
   * href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> to process the
   * response. See section 9.2.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a> for the
   * specification of the status codes.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  proppatch(URL url, InputStream in, String lockToken)
    throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    headersIn.set("Content-Type", "text/xml");

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    return operation(HTTPClient.PROPPATCH, url, headersIn, in);
  }



  /**
   * Changes properties of the resource.
   * @param url the resource.
   * @param document the request body. It is specified in section 9.2 of <a
   * href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a>.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> to process the
   * response. See section 9.2.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a> for the
   * specification of the status codes.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  proppatch(URL url, Document document) throws IOException, ProtocolException
  {
    return proppatch(url, document, null);
  }



  /**
   * Changes properties of the resource.
   * @param url the resource.
   * @param document the request body.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> to process the
   * response. See section 9.2.1 of
   * <a href="http://www.ietf.org/rfc/rfc4918.txt">RFC 4918</a> for the
   * specification of the status codes.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  proppatch(URL url, Document document, String lockToken)
    throws IOException, ProtocolException
  {
    return proppatch(url, prepareMessage(document), lockToken);
  }



  /**
   * Creates or updates a resource.
   * @param url the resource that will be updated. If it doesn't exist it will
   * be created.
   * @param in the request body. If it is <code>null</code> an empty request
   * body will be sent.
   * @param mimeType the MIME type of the request body. If it is
   * <code>null</code> the MIME type <code>application/octet-stream</code> is
   * used.
   * @param language the natural language of the request body. It may be
   * <code>null</code>.
   * @return The response should be closed.
   */

  public Response
  put(URL url, InputStream in, String mimeType, String language)
    throws IOException, ProtocolException
  {
    return put(url, in, mimeType, language, null);
  }



  /**
   * Creates or updates a resource.
   * @param url the resource that will be updated. If it doesn't exist it will
   * be created.
   * @param in the request body. If it is <code>null</code> an empty request
   * body will be sent.
   * @param mimeType the MIME type of the request body. If it is
   * <code>null</code> the MIME type <code>application/octet-stream</code> is
   * used.
   * @param language the natural language of the request body. It may be
   * <code>null</code>.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The response should be closed.
   */

  public Response
  put
  (
    URL			url,
    InputStream		in,
    String		mimeType,
    String		language,
    String		lockToken
  ) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    headersIn.set
    (
      "Content-Type",
      mimeType != null ? mimeType : "application/octet-stream"
    );

    if (language != null)
    {
      headersIn.set("Content-Language", language);
    }

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    return operation(HTTPClient.PUT, url, headersIn, in);
  }



  private static Event
  readEvent(URL url, XMLEventReader reader) throws Exception
  {
    Event	result = new Event();

    while (reader.hasNext())
    {
      XMLEvent	event = reader.nextEvent();

      if (Util.isEndReElement(event, "event"))
      {
        return result;
      }

      if (Util.isStartReElement(event, "path"))
      {
        result.path = reader.getElementText();
      }
      else
      {
        if (Util.isStartReElement(event, "operation"))
        {
          result.operation = reader.getElementText();
        }
        else
        {
          if (Util.isStartReElement(event, "occurrence"))
          {
            result.occurrence =
              be.re.util.Util.parseTimestamp(reader.getElementText());
          }
          else
          {
            if (Util.isStartReElement(event, "user"))
            {
              readUser(url, reader, result);
            }
            else
            {
              if (Util.isStartReElement(event, "additional-info"))
              {
                result.additionalInfo = reader.getElementText();
              }
            }
          }
        }
      }
    }

    return null;
  }



  private static InputStream
  readJSONP(byte[] b, String method, Headers headersOut) throws IOException
  {
    int	end = -1;
    int	start = -1;

    for (int i = 0; start == -1 && i < b.length; ++i)
    {
      if ((char) b[i] == '(')
      {
        start = i + 1;
      }
    }

    for (int i = b.length - 1; end == -1 && i >= 0; --i)
    {
      if ((char) b[i] == ')')
      {
        end = i;
      }
    }

    if (start == -1 || end == -1 || end <= start)
    {
      throw new IOException("Bad JSONP response.");
    }

    Object	result =
      be.re.json.Util.readJson(new ByteArrayInputStream(b, start, end - start));

    if (!(result instanceof Map) || ((Map) result).get("status") == null)
    {
      throw new IOException("Bad JSONP response.");
    }

    headersOut.set("Content-Type", "text/xml");
    headersOut.set("Status-Code", ((Map) result).get("status").toString());

    headersOut.set
    (
      "Status-Message",
      Util.getReasonPhrase
      (
        method,
        (int) ((Long) ((Map) result).get("status")).longValue()
      )
    );

    return
      ((Map) result).get("responseText") != null ?
        new ByteArrayInputStream
        (
          ((String) ((Map) result).get("responseText")).getBytes("UTF-8")
        ) : new DevNullInputStream();
  }



  private static Lock
  readLock(Node element)
  {
    Lock	lock = new Lock();

    lock.depth =
      be.re.xml.Util.getText
        (
          be.re.xml.Util.
            selectFirstChild(element, Constants.DAV_URI, "depth")
        );

    lock.exclusive =
      be.re.xml.Util.selectElement
      (
        element,
        new ExpandedName[]
        {
          new ExpandedName(Constants.DAV_URI, "lockscope"),
          new ExpandedName(Constants.DAV_URI, "exclusive")
        }
      ) != null;

    Node	owner =
      be.re.xml.Util.
        selectFirstChild(element, Constants.DAV_URI, "owner");

    if (owner != null)
    {
      lock.owner = be.re.xml.Util.getText(owner);
    }

    Node	root =
      be.re.xml.Util.
        selectFirstChild(element, Constants.DAV_URI, "lockroot");

    if (root != null) // Introduced in RFC 4918.
    {
      try
      {
        lock.root =
          new URL
          (
            be.re.xml.Util.getText
            (
              be.re.xml.Util.selectFirstChild(root, Constants.DAV_URI, "href")
            )
          );
      }

      catch (MalformedURLException e)
      {
      }
    }

    Node	timeout =
      be.re.xml.Util.
        selectFirstChild(element, Constants.DAV_URI, "timeout");

    if (timeout != null)
    {
      String	s = be.re.xml.Util.getText(timeout);

      if (s.equals("Infinity") || !s.startsWith("Second-"))
      {
        lock.timeoutSeconds = -1;
      }
      else
      {
        lock.timeoutSeconds = Integer.parseInt(s.substring("Second-".length()));
      }
    }

    Node	token =
      be.re.xml.Util.
        selectFirstChild(element, Constants.DAV_URI, "locktoken");

    if (token != null)
    {
      lock.token =
        be.re.xml.Util.getText
        (
          be.re.xml.Util.selectFirstChild(token, Constants.DAV_URI, "href")
        );

      lock.token =
        lock.token.startsWith("urn:uuid:") ?
          lock.token.substring("urn:uuid:".length()) :
          (
            lock.token.startsWith("opaquelocktoken:") ?
              lock.token.substring("opaquelocktoken:".length()) : lock.token
          );
    }

    lock.write =
      be.re.xml.Util.selectElement
      (
        element,
        new ExpandedName[]
        {
          new ExpandedName(Constants.DAV_URI, "locktype"),
          new ExpandedName(Constants.DAV_URI, "write")
        }
      ) != null;

    return lock;
  }



  private static void
  readUser(URL url, XMLEventReader reader, Event e) throws Exception
  {
    while (reader.hasNext())
    {
      XMLEvent	event = reader.nextEvent();

      if (Util.isEndReElement(event, "user"))
      {
        return;
      }

      if (Util.isStartReElement(event, "href"))
      {
        e.user = new URL(url, reader.getElementText());
      }
      else
      {
        if (Util.isStartReElement(event, "displayname"))
        {
          e.username = reader.getElementText();
        }
      }
    }
  }



  /**
   * Replaces a binding to an existing resource.
   * @param url the existing resource
   * @param newBinding the new URL for the resource.
   * @param overwrite if set to <code>true</code> and there already is a
   * resource denoted by <code>newBinding</code>, then overwrite it.
   */

  public void
  rebind(URL url, URL newBinding, boolean overwrite)
    throws IOException, ProtocolException
  {
    rebind(url, newBinding, overwrite, null);
  }



  /**
   * Replaces a binding to an existing resource.
   * @param url the existing resource
   * @param newBinding the new URL for the resource.
   * @param overwrite if set to <code>true</code> and there already is a
   * resource denoted by <code>newBinding</code>, then overwrite it.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  rebind(URL url, URL newBinding, boolean overwrite, String lockToken)
    throws IOException, ProtocolException
  {
    bindOrRebind(url, newBinding, false, overwrite, lockToken);
  }



  /**
   * Removes a label from the resource. The resource should be checked in.
   * @param url the resource
   * @param label the label that is to be removed.
   */

  public void
  removeLabel(URL url, String label) throws IOException, ProtocolException
  {
    label(url, "remove", label, null, null);
  }



  /**
   * Removes a label from the resource. The resource should be checked in.
   * @param url the resource.
   * @param label the label that is to be removed.
   * @param selectionLabel an optional label that overrides the default
   * version selection. The version carrying <code>selectionLabel</code> will
   * be the subject of the operation.
   * @param depth the scope of application of the label. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  removeLabel(URL url, String label, String selectionLabel, String depth)
    throws IOException, ProtocolException
  {
    return removeLabel(url, label, selectionLabel, depth, null);
  }



  /**
   * Removes a label from the resource. The resource should be checked in.
   * @param url the resource.
   * @param label the label that is to be removed.
   * @param selectionLabel an optional label that overrides the default
   * version selection. The version carrying <code>selectionLabel</code> will
   * be the subject of the operation.
   * @param depth the scope of application of the label. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  removeLabel
  (
    URL		url,
    String	label,
    String	selectionLabel,
    String	depth,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    return label(url, "remove", label, selectionLabel, depth, lockToken);
  }



  /**
   * <p>With this method more complex information can be retrieved about a
   * resource. The following WebDAV reports exist:</p>
   *
   * <ul>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3253.html#REPORT_version-tree">version-tree</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3253.html#REPORT_expand-property">expand-property</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3253.html#REPORT_locate-by-history">locate-by-history</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3253.html#rfc.section.11.3">merge-preview</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3253.html#rfc.section.12.7">compare-baseline</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3253.html#rfc.section.13.6">latest-activity-version</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3744.html#REPORT_acl-principal-prop-set">acl-principal-prop-set</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3744.html#REPORT_principal-match">principal-match</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3744.html#REPORT_principal-property-search">principal-property-search</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc3744.html#REPORT_principal-search-property-set">principal-search-property-set</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc4791.html#calendar-query">calendar-query</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc4791.html#calendar-multiget">calendar-multiget</a></li>
   *   <li><a
   *   href="http://www.webdav.org/specs/rfc4791.html#free-busy-query">free-busy-query</a></li>
   * </ul>
   *
   * <p>The reports that are supported by a resource can be discovered through
   * the <a
   * href="http://www.webdav.org/specs/rfc3253.html#PROPERTY_supported-report-set"><code>supported-report-set</code></a> property.</p>
   *
   * <p>Pincette has the extension report <code>simple-search</code> in the
   * namespace <code>urn:be-re:dav</code>. This is a report for general purpose
   * queries. It returns the list of resources that matches the query,
   * accompanied by properties if requested. The syntax is as follows:</p>
   *
   * <pre> &lt;!ELEMENT simple-search ((and|or|not|expr|(field,op,value)),(prop)?)>
   * &lt;!ELEMENT prop ANY>
   * &lt;!ELEMENT and (expr)+>
   * &lt;!ELEMENT or (expr)+>
   * &lt;!ELEMENT expr (and|or|not|expr|(field,op,value))>
   * &lt;!ELEMENT not (and|or|not|expr|(field,op,value))>
   * &lt;!ELEMENT field (#PCDATA)>
   * &lt;!ELEMENT op (#PCDATA)>
   * &lt;!ELEMENT value (#PCDATA)></pre>
   *
   * <p>The <code>prop</code> element should contain empty property name
   * elements. The following table lists the supported fields, their operators
   * and possible values:</p>
   *
   * <table rules="all" cellpadding="5">
   *   <thead>
   *     <tr><th>Field</th><th>Operators</th><th>Values</th></tr>
   *   </thead>
   *   <tbody>
   *     <tr><td>activity-set</td><td>contains</td><td>activity URL</td></tr>
   *     <tr><td>auto-merged</td><td>=</td><td>true or false</td></tr>
   *     <tr>
   *       <td>contains</td>
   *       <td>=</td>
   *       <td><a
   *       href="http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/queryParser/QueryParser.html">Lucene text query</a></td>
   *     </tr>
   *     <tr>
   *       <td>displayname</td>
   *       <td>=, !=</td>
   *       <td>string (wildcard "*")</td>
   *     </tr>
   *     <tr><td>checked-out</td><td>=</td><td>true or false</td></tr>
   *     <tr>
   *       <td>creation-date</td>
   *       <td>=, !=, &lt;, >, &lt;=, >=</td>
   *       <td>ISO 8601 timestamp</td>
   *     </tr>
   *     <tr>
   *       <td>creator-displayname</td>
   *       <td>=, !=</td>
   *       <td>string (wildcard "*")</td>
   *     </tr>
   *     <tr>
   *       <td>getcontentlanguage</td>
   *       <td>=, !=</td>
   *       <td>string (wildcard "*")</td>
   *     </tr>
   *     <tr>
   *       <td>getcontentlength</td>
   *       <td>=, !=, &lt;, >, &lt;=, >=</td>
   *       <td>natural number</td>
   *     </tr>
   *     <tr>
   *       <td>getcontenttype</td>
   *       <td>=, !=</td>
   *       <td>string (wildcard "*")</td>
   *     </tr>
   *     <tr>
   *       <td>getlastmodified</td>
   *       <td>=, !=, &lt;, >, &lt;=, >=</td>
   *       <td>ISO 8601 timestamp</td>
   *     </tr>
   *     <tr><td>label-name-set</td><td>contains</td><td>string</td></tr>
   *     <tr><td>not-label</td><td>=</td><td>string</td></tr>
   *     <tr><td>merge-incomplete</td><td>=</td><td>true or false</td></tr>
   *     <tr>
   *       <td>meta.&lt;property name></td>
   *       <td>=, !=</td>
   *       <td>string (wildcard "*")</td>
   *     </tr>
   *     <tr><td>owner</td><td>=, !=</td><td>string (wildcard "*")</td></tr>
   *   </tbody>
   * </table>
   *
   * @param url the resource for which the report is requested.
   * @param in the request document.
   * @param label an optional label that overrides the default
   * version selection. The version carrying <code>label</code> will
   * be the subject of the operation. This parameter is specific to Pincette.
   * @param depth the scope of the report. It can have the following values:
   * <ul>
   *  <li>0: apply the report only to the resource itself;</li>
   *  <li>1: apply the report to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the report to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   */

  public Response
  report(URL url, InputStream in, String label, String depth)
    throws IOException, ProtocolException
  {
    testDepth(depth);

    Headers	headersIn = new Headers();

    headersIn.set(Constants.DEPTH_HEADER, depth);

    if (label != null)
    {
      headersIn.set
      (
        Constants.LABEL_HEADER,
        be.re.net.Util.escapeUriPathSegment(label)
      );
    }

    return operation(HTTPClient.REPORT, url, headersIn, in);
  }



  /**
   * This is a variant of the other <code>report</code> method.
   */

  public Response
  report(URL url, Document document, String label, String depth)
    throws IOException, ProtocolException
  {
    return report(url, prepareMessage(document), label, depth);
  }



  private static InputStream
  request
  (
    String	method,
    URL		url,
    InputStream	body,
    String	username,
    String	password,
    Headers	headersIn,
    Headers	headersOut
  ) throws IOException
  {
    if (tunnel && !HTTP_METHODS.contains(method))
    {
      StringBuilder	s = new StringBuilder(url.toString());

      s.append(s.indexOf("?") == -1 ? "?" : "&");
      s.append("jsonp=true&webdav-method=");
      s.append(method);

      if (headersIn != null)
      {
        Headers.Header[]	headers = headersIn.getAll();

        for (int i = 0; i < headers.length; ++i)
        {
          s.append("&webdav-header-");
          s.append(headers[i].getName());
          s.append('=');
          s.append(be.re.net.Util.escapeUriQueryString(headers[i].getValue()));
        }
      }

      if (body != null)
      {
        ByteArrayOutputStream	out = new ByteArrayOutputStream();

        StreamConnector.copy(body, out);
        s.append("&body=");

        s.append
        (
          be.re.net.Util.
            escapeUriQueryString(new String(out.toByteArray(), "ASCII"))
        );
      }

      ByteArrayOutputStream	out = new ByteArrayOutputStream();

      StreamConnector.copy
      (
        HTTPClient.request
        (
          HTTPClient.GET,
          new URL(s.toString()),
          null,
          username,
          password,
          null,
          headersOut,
          null,
          null
        ),
        out
      );

      return readJSONP(out.toByteArray(), method, headersOut);
    }

    return
      HTTPClient.request
      (
        method,
        url,
        body,
        username,
        password,
        headersIn,
        headersOut,
        null,
        null
      );
  }



  /**
   * Sets a label on the resource. The resource should be checked in.
   * @param url the resource.
   * @param label the label that is to be set.
   */

  public void
  setLabel(URL url, String label) throws IOException, ProtocolException
  {
    label(url, "set", label, null, null);
  }



  /**
   * Sets a label on the resource. The resource should be checked in. If
   * another version of the resource already has the label, the label will be
   * moved.
   * @param url the resource.
   * @param label the label that is to be set.
   * @param selectionLabel an optional label that overrides the default
   * version selection. The version carrying <code>selectionLabel</code> will
   * be the subject of the operation.
   * @param depth the scope of application of the label. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  setLabel(URL url, String label, String selectionLabel, String depth)
    throws IOException, ProtocolException
  {
    return setLabel(url, label, selectionLabel, depth, null);
  }



  /**
   * Sets a label on the resource. The resource should be checked in. If
   * another version of the resource already has the label, the label will be
   * moved.
   * @param url the resource.
   * @param label the label that is to be set.
   * @param selectionLabel an optional label that overrides the default
   * version selection. The version carrying <code>selectionLabel</code> will
   * be the subject of the operation.
   * @param depth the scope of application of the label. It can have the
   * following values:
   * <ul>
   *  <li>0: apply the operation only to the resource itself;</li>
   *  <li>1: apply the operation to the resource itself and its immediate
   *  descendants;</li>
   *  <li>infinity: apply the operation to the resource itself and any of its
   * descendants no matter the level.</li>
   * </ul>
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @return The response should be closed after consumption. Use
   * <code>be.re.webdav.Util.reportMultistatus()</code> if the status code is
   * 207. Otherwise the status code 200 indicates success.
   * @see Util#reportMultistatus(Client.Response, Util.ReportMultistatus)
   */

  public Response
  setLabel
  (
    URL		url,
    String	label,
    String	selectionLabel,
    String	depth,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    return label(url, "set", label, selectionLabel, depth, lockToken);
  }



  /**
   * Sets the default locale that will be assigned to the
   * <code>Accept-Language</code> header if it is not yet present. It may be
   * <code>null</code>.
   */

  public void
  setLocale(Locale value)
  {
    locale = value;
  }



  /**
   * Sets the <code>X-be.re.On-Behalf-Of</code> header. A system user can use
   * this to perform requests on behalf of another user. This way a WebDAV
   * server can be used as a resource by another service, much like a database
   * system. It may be <code>null</code>.
   */

  public void
  setOnBehalfOf(String value)
  {
    onBehalfOf = value;
  }



  /**
   * Sets the default password for basic HTTP authentication. It may be
   * <code>null</code>.
   */

  public void
  setPassword(String value)
  {
    password = value;
  }



  /**
   * Sets the value of simple live properties.
   * @param url the resource that will be updated.
   * @param properties the properties.
   * @see Client#getSimpleLiveProperty(URL, ExpandedName)
   */

  public void
  setSimpleLiveProperties(URL url, SimpleProperty[] properties)
    throws IOException, ProtocolException
  {
    setSimpleLiveProperties(url, properties, null);
  }



  /**
   * Sets the value of a simple live properties.
   * @param url the resource that will be updated.
   * @param properties the properties.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @see Client#getSimpleLiveProperty(URL, ExpandedName)
   */

  public void
  setSimpleLiveProperties
  (
    URL			url,
    SimpleProperty[]	properties,
    String		lockToken
  ) throws IOException, ProtocolException
  {
    Response	response =
      proppatch
      (
        url,
        prepareMessage(createProppatchBody(properties)),
        lockToken
      );

    try
    {
      if (response.getStatusCode() == 200)
      {
        return;
      }

      if (response.getStatusCode() != 207)
      {
        Util.throwException(response);
      }

      Util.checkPropstatStatus
      (
        url,
        HTTPClient.PROPPATCH,
        response.createDocument()
      );
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Sets the value of a simple live property.
   * @param url the resource that will be updated.
   * @param property the name of the simple property.
   * @param value the value.
   * @see Client#getSimpleLiveProperty(URL, ExpandedName)
   */

  public void
  setSimpleLiveProperty(URL url, ExpandedName property, String value)
    throws IOException, ProtocolException
  {
    setSimpleLiveProperty(url, property, value, null);
  }



  /**
   * Sets the value of a simple live property.
   * @param url the resource that will be updated.
   * @param property the name of the simple property.
   * @param value the value.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @see Client#getSimpleLiveProperty(URL, ExpandedName)
   */

  public void
  setSimpleLiveProperty
  (
    URL			url,
    ExpandedName	property,
    String		value,
    String		lockToken
  ) throws IOException, ProtocolException
  {
    setSimpleLiveProperties
    (
      url,
      new SimpleProperty[]{new SimpleProperty(property, value)},
      lockToken
    );
  }



  /**
   * Sets the value of a simple live property.
   * @param url the resource that will be updated.
   * @param property the local name of the simple property. The property is
   * assumed to be in the <code>DAV:</code> namespace.
   * @param value the value.
   * @see Client#getSimpleLiveProperty(URL, ExpandedName)
   */

  public void
  setSimpleLiveProperty(URL url, String property, String value)
    throws IOException, ProtocolException
  {
    setSimpleLiveProperty(url, property, value, null);
  }



  /**
   * Sets the value of a simple live property.
   * @param url the resource that will be updated.
   * @param property the local name of the simple property. The property is
   * assumed to be in the <code>DAV:</code> namespace.
   * @param value the value.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   * @see Client#getSimpleLiveProperty(URL, ExpandedName)
   */

  public void
  setSimpleLiveProperty
  (
    URL		url,
    String	property,
    String	value,
    String	lockToken
  ) throws IOException, ProtocolException
  {
    setSimpleLiveProperty
    (
      url,
      new ExpandedName(Constants.DAV_URI, property),
      value,
      lockToken
    );
  }



  /**
   * Sets the default value for the <code>User-Agent</code> header. It may be
   * <code>null</code>.
   */

  public void
  setUserAgent(String userAgent)
  {
    this.userAgent = userAgent;
  }



  /**
   * Sets the default username for basic HTTP authentication. It may be
   * <code>null</code>.
   */

  public void
  setUsername(String value)
  {
    username = value;
  }



  private static void
  testDepth(String depth)
  {
    if (!"0".equals(depth) && !"1".equals(depth) && !"infinity".equals(depth))
    {
      throw new IllegalArgumentException("Illegal depth: " + depth);
    }
  }



  /**
   * Removes a binding to a resource. The resource will be deleted when there
   * no other bindings left.
   * @param url the resource.
   */

  public void
  unbind(URL url) throws IOException, ProtocolException
  {
    unbind(url, null);
  }



  /**
   * Removes a binding to a resource. The resource will be deleted when there
   * no other bindings left.
   * @param url the resource.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  unbind(URL url, String lockToken) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response =
      operation
      (
        HTTPClient.UNBIND,
        Util.getParent(url),
        headersIn,
        prepareMessage(createBindBody("unbind", getLastPathSegment(url), null))
      );

    try
    {
      if (response.getStatusCode() != 200 && response.getStatusCode() != 204)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Reverts the check-out of a resource.
   */

  public void
  uncheckout(URL url) throws IOException, ProtocolException
  {
    uncheckout(url, null);
  }



  /**
   * Reverts the check-out of a resource.
   * @param url the resource.
   * @param lockToken a lock token. It may be <code>null</code>. If the
   * resource is locked, the lock token should correspond to the lock.
   */

  public void
  uncheckout(URL url, String lockToken) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    if (lockToken != null)
    {
      headersIn.set("If", "(<opaquelocktoken:" + lockToken + ">)");
    }

    Response	response =
      operation(HTTPClient.UNCHECKOUT, url, headersIn, null);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Unlocks a resource.
   * @param url the resource.
   * @param lockToken a lock token. It must not be <code>null</code>.
   */

  public void
  unlock(URL url, String lockToken) throws IOException, ProtocolException
  {
    Headers	headersIn = new Headers();

    headersIn.set("Lock-Token", "<opaquelocktoken:" + lockToken + ">");

    Response	response = operation(HTTPClient.UNLOCK, url, headersIn, null);

    try
    {
      if (response.getStatusCode() != 204)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  public void
  versionControl(URL url) throws IOException, ProtocolException
  {
    Response	response =
      operation(HTTPClient.VERSION_CONTROL, url, new Headers(), null);

    try
    {
      if (response.getStatusCode() != 200)
      {
        Util.throwException(response);
      }
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Represents one Access Control Entry in an Access Control List.
   * @author Werner Donn\u00e9
   */

  public static class ACE

  {

    /**
     * Grant or deny.
     */

    public boolean	grant;

    /**
     * When set to <code>true</code> it means that the entry is valid for
     * principals that do not match.
     */

    public boolean	invert;

    /**
     * When set to <code>true</code> the entry can't be modified. An attempt to
     * set an Access Control List without it will fail.
     */

    public boolean	isProtected;

    /**
     * The principal can be a user or group URL, a value of type
     * <code>ExpandedName</code>, which is the name of a property the resource
     * should have with a value that matches the current user, or a symbolic
     * value from the following list:
     * <ul>
     *   <li><code>all</code>: the current user always matches.</li>
     *   <li><code>authenticated</code>: the current user matches only if
     * authenticated;</li>
     *   <li><code>unauthenticated</code>: the current user matches only if not
     *   authenticated.</li>
     *   <li><code>self</code>: the current user matches only if the resource
     * is the current user or a group of which the current user is a
     * member.</li>
     * </ul>
     */

    public Object	principal;

    /**
     * The standard privileges are defined in <a
     * href="http://www.webdav.org/specs/rfc3744.html#privileges">RFC 3744</a>.
     * Pincette adds the privileges <code>checkin</code>,
     * <code>checkout</code>, <code>delete-version</code>, <code>label</code>
     * and <code>uncheckout</code> in the namespace <code>urn:be-re:dav</code>.
     * It also adds the aggregated privilege <code>version</code> in the same
     * namespace. It implies the other ones.
     */

    public ExpandedName	privilege;



    public
    ACE()
    {
    }



    public
    ACE
    (
      URL		principal,
      boolean		invert,
      ExpandedName	privilege,
      boolean		grant,
      boolean		isProtected
    )
    {
      this.principal = principal;
      this.invert = invert;
      this.privilege = privilege;
      this.grant = grant;
      this.isProtected = isProtected;
    }



    public
    ACE
    (
      String		principal,
      boolean		invert,
      ExpandedName	privilege,
      boolean		grant,
      boolean		isProtected
    )
    {
      this.principal = principal;
      this.invert = invert;
      this.privilege = privilege;
      this.grant = grant;
      this.isProtected = isProtected;
    }



    public
    ACE
    (
      ExpandedName	principal,
      boolean		invert,
      ExpandedName	privilege,
      boolean		grant,
      boolean		isProtected
    )
    {
      this.principal = principal;
      this.invert = invert;
      this.privilege = privilege;
      this.grant = grant;
      this.isProtected = isProtected;
    }



    public
    ACE(ACE ace)
    {
      this.principal = ace.principal;
      this.invert = ace.invert;
      this.privilege = ace.privilege;
      this.grant = ace.grant;
      this.isProtected = ace.isProtected;
    }



    public boolean
    equals(Object o)
    {
      return
        o instanceof ACE && ((ACE) o).grant == grant &&
          ((ACE) o).invert == invert && ((ACE) o).isProtected == isProtected &&
          ((ACE) o).privilege.equals(privilege) &&
          ((ACE) o).principal.equals(principal);
    }



    public int
    hashCode()
    {
      return
        privilege.hashCode() + principal.hashCode() + (grant ? 1 : 0) +
          (invert ? 1 : 0) + (isProtected ? 1 : 0);
    }

  } // ACE



  /**
   * An interface to handle a stream of audit trail events.
   * @author Werner Donn\u00e9
   */

  public interface AuditTrail

  {

    /**
     * @param e the event to be handled.
     * @return <code>true</code> if event handling should continue and
     * <code>false</code> otherwise.
     */

    public boolean	handle	(Event e);

  } // AuditTrail



  /**
   * Represents one event line in an audit trail.
   * @author Werner Donn\u00e9
   */

  public static class Event

  {

    /**
     * Optional additional information about the event.
     */

    public String	additionalInfo;

    /**
     * The timestamp of the event in milliseconds since 1 January 1970 UTC. It
     * defaults to -1, which means the time is not available.
     */

    public long		occurrence = -1;

    /**
     * One of the operations defined in <code>Constants.EVENT_*</code>.
     * @see Constants
     */

    public String	operation;

    /**
     * An absolute path in the repository.
     */

    public String	path;

    /**
     * The URL denoting the user that performed the operation.
     */

    public URL		user;

    /**
     * The name of the user that performed the operation.
     */

    public String	username;

  } // Event



  /**
   * Represents a lock.
   * @author Werner Donn\u00e9
   */

  public static class Lock

  {

    /**
     * The depth of a lock. This can be "0", "1", or "infinity".
     */

    public String	depth;

    /**
     * The lock is exclusive when this field is <code>true</code>, otherwise it
     * is shared.
     */

    public boolean	exclusive;

    /**
     * The owner of a lock. This can be any string.
     */

    public String	owner;

    /**
     * The lock root, which is the URL through which the resource was 
     * addressed in the LOCK request.
     */

    public URL		root;

    /**
     * The number of seconds remaining before a lock expires. The value
     * <code>-1</code> indicates infinity.
     */

    public int		timeoutSeconds;

    /**
     * The lock token.
     */

    public String	token;

    /**
     * Indicates if this lock is a write lock.
     */

    public boolean	write;

  } // Lock



  /**
   * Contains the result of an options request.
   * @author Werner Donn\u00e9
   */

  public static class Options

  {

    /**
     * The set of collections that contain activities. If the set was not
     * requested the value will be <code>null</code>.
     */

    public URL[]	activityCollectionSet;

    /**
     * The allowed methods for a resource.
     */

    public String[]	allowedMethods;

    /**
     * The features published through the DAV header.
     */

    public String[]	davFeatures;

    /**
     * The DAV compliance level. The value can be 1 or 2.
     */

    public int		davVersion;

    /**
     * All the headers returned in the options response.
     */

    public Headers	headers;

    /**
     * The set of collections that contain functional meta data collections.
     * This is specific for Pincette. If the set was not requested the value
     * will be <code>null</code>.
     */

    public URL[]	metaCollectionSet;

    /**
     * The set of collections that contain version histories. If the set was
     * not requested the value will be <code>null</code>.
     */

    public URL[]	versionHistoryCollectionSet;

    /**
     * The set of collections that contain views. This is specific for
     * Pincette. If the set was not requested the value will be
     * <code>null</code>.
     */

    public URL[]	viewCollectionSet;

  } // Options



  public static class SimpleProperty

  {

    public ExpandedName	name;
    public String	value;



    public
    SimpleProperty(ExpandedName name, String value)
    {
      this.name = name;
      this.value = value;
    }



    public boolean
    equals(Object o)
    {
      return
        o instanceof SimpleProperty && ((SimpleProperty) o).name.equals(name) &&
          ((SimpleProperty) o).value.equals(value);
    }



    public int
    hashCode()
    {
      return name.hashCode() + value.hashCode();
    }

  } // SimpleProperty



  /**
   * Contains the response of a WebDAV operation.
   * @author Werner Donn\u00e9
   */

  public static class Response

  {

    private InputStream	body;
    private int		code;
    private Headers	headers;
    private String	method;
    private URL		url;



    public
    Response(URL url, String method, Headers headers) throws IOException
    {
      this(url, method, headers, null);
    }



    public
    Response(URL url, String method, Headers headers, InputStream body)
      throws IOException
    {
      this.url = url;
      this.method = method;
      this.headers = headers;
      this.body = body;

      try
      {
        code = Integer.parseInt(headers.get("Status-Code")[0]);
      }

      catch (Exception e)
      {
        throw new ProtocolException("No status code");
      }
    }



    /**
     * Frees the underlying resources. It should always be called afer
     * consumption.
     */

    public void
    close() throws IOException
    {
      InputStream	in = getBody();

      if (in != null)
      {
        if (be.re.net.Util.hasBody(headers))
        {
          StreamConnector.copy(in, new DevNullOutputStream(), false, true);
        }

        in.close();
      }
    }



    /**
     * Creates a DOM document from the response body. It can be called once.
     * Subsequent calls to this method as well as the <code>getBody</code>
     * method will return <code>null</code>.
     */

    public Document
    createDocument() throws IOException
    {
      InputStream	in = getBody();

      if
      (
        in == null							||
        (
          getHeaders().get("Content-Length").length > 0			&&
          Integer.parseInt(getHeaders().get("Content-Length")[0]) == 0
        )
      )
      {
        if (in != null)
        {
          in.close();
        }

        return null;
      }

      try
      {
        DocumentBuilder	parser =
          Util.nonValidatingDomFactory.newDocumentBuilder();

        parser.setErrorHandler(new DevNullErrorHandler());

        return parser.parse(in);
      }

      catch (ParserConfigurationException e)
      {
        throw new be.re.io.IOException(e);
      }

      catch (SAXException e)
      {
        return null; // Could be chunked.
      }
    }



    protected void
    finalize() throws Throwable
    {
      close();
    }



    /**
     * Returns the response body. It can be called once. Subsequent calls to
     * this method as well as the <code>createDocument</code> method will return
     * <code>null</code>.
     */

    public InputStream
    getBody()
    {
      InputStream	result = body;

      body = null;

      return result;
    }



    /**
     * Returns the response headers.
     */

    public Headers
    getHeaders()
    {
      return headers;
    }



    /**
     * Returns the request method.
     */

    public String
    getMethod()
    {
      return method;
    }



    /**
     * Returns the status code.
     */

    public int
    getStatusCode()
    {
      return code;
    }



    /**
     * Returns the status message in the status line of the response.
     */

    public String
    getStatusMessage()
    {
      String[]	message = headers.get("Status-Message");

      return message.length > 0 ? message[0] : "";
    }



    /**
     * Returns the request URL.
     */

    public URL
    getUrl()
    {
      return url;
    }

  } // Response

} // Client
