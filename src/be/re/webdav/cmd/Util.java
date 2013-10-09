package be.re.webdav.cmd;

import be.re.gui.util.YesOrNo;
import be.re.io.StreamConnector;
import be.re.net.ProtocolException;
import be.re.net.URLManager;
import be.re.util.MimeType;
import be.re.webdav.Client;
import be.re.xml.ExpandedName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * @author Werner Donn\u00e9
 */

public class Util

{

  static final String	LOCKED = "locked";

  private static ResourceBundle		bundle = null;
  static XMLInputFactory		inputFactory;
  static DocumentBuilderFactory		nonValidatingDomFactory;
  private static Writer			reportWriter;
  static TransformerFactory		transformerFactory;



  static
  {
    try
    {
      inputFactory = be.re.xml.stax.Util.newInputFactory(false, true);
      nonValidatingDomFactory = be.re.xml.Util.newDocumentBuilderFactory(false);
      transformerFactory = be.re.xml.Util.newTransformerFactory();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  static URL
  asView(URL url) throws Exception
  {
    if (!url.toString().endsWith("/"))
    {
      return url; // No confusion possible.
    }

    URL	newUrl =
      new URL(url.toString().substring(0, url.toString().length() - 1));

    return
      be.re.webdav.Util.isView(newUrl) &&
        new YesOrNo(url.getFile() + ": " + Util.getResource("msg_as_view")).
          ask() ? newUrl : url;
  }



  static boolean
  checkPropstatStatus(URL url, Node propstat) throws IOException
  {
    Node	status =
      be.re.xml.Util.selectFirstChild(propstat, Constants.DAV_URI, "status");

    if (status == null)
    {
      return true; // No news is good news.
    }

    int	code =
      be.re.net.Util.httpStatusCode(be.re.xml.Util.getText(status).trim());

    if (code != 200)
    {
      Element	error =
        be.re.xml.Util.selectElement
        (
          propstat,
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "responsedescription"),
            new ExpandedName(Constants.DAV_URI, "error")
          }
        );

      report
      (
        url,
        error != null ?
          be.re.webdav.Util.getPreconditionMessage(error) :
          be.re.webdav.Util.getReasonPhrase("PROPFIND", code),
        true
      );

      return false;
    }

    return true;
  }



  private static String
  correctFilename(String s)
  {
    char[]	chars = s.toCharArray();

    for (int i = 0; i < chars.length; ++i)
    {
      if (!Character.isLetterOrDigit(chars[i]) && chars[i] != '.')
      {
        chars[i] = '_';
      }
    }

    return new String(chars);
  }



  static Document
  createDAVDocument(String name)
  {
    try
    {
      Document    result =
        be.re.xml.Util.getDocumentBuilder(nonValidatingDomFactory, null).
          newDocument();
      Element     element = result.createElementNS(Constants.DAV_URI, name);

      element.setAttribute("xmlns", Constants.DAV_URI);
      result.appendChild(element);

      return result;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  static File
  createTempFileFromRemote
  (
    String		url,
    Client.Response	response,
    String		mimeType
  ) throws Exception
  {
    String	filename =
      response.getHeaders().get("Content-Disposition").length > 0 ?
      extractFilename
      (
        response.getHeaders().get("Content-Disposition")[0],
        mimeType
      ) : null;

    return
      be.re.io.Util.createTempFile
      (
        "remote.",
        correctFilename
        (
          filename != null ?
            MimeType.adjustExtension(filename, mimeType) :
            be.re.net.Util.getLastPathSegment(url)
        )
      );
  }



  private static String
  extractFilename(String header, String mimeType)
  {
    StringTokenizer	tokenizer = new StringTokenizer(header, " ;");

    if
    (
      tokenizer.hasMoreTokens()				&&
      "inline".equalsIgnoreCase(tokenizer.nextToken())
    )
    {
      while (tokenizer.hasMoreTokens())
      {
        StringTokenizer	filenameTokenizer =
          new StringTokenizer(tokenizer.nextToken(), " ='\"");

        if
        (
          filenameTokenizer.countTokens() == 2				&&
          "filename".equalsIgnoreCase(filenameTokenizer.nextToken())
        )
        {
          String	token = filenameTokenizer.nextToken();

          if (!"".equals(token))
          {
            return
              mimeType != null ?
                MimeType.adjustExtension(token, mimeType) : token;
          }
        }
      }
    }

    return null;
  }



  private static void
  extractLabels(Node node, Collection labels)
  {
    if (node == null)
    {
      return;
    }

    if
    (
      be.re.webdav.Constants.DAV_URI.equals(node.getNamespaceURI())	&&
      "label-name".equals(node.getLocalName())
    )
    {
      labels.add(be.re.xml.Util.getText(node));
    }

    extractLabels(node.getFirstChild(), labels);
    extractLabels(node.getNextSibling(), labels);
  }



  static String
  findCommonPrefix(String s1, String s2)
  {
    int	i;

    for
    (
      i = 0;
      i < s1.length() && i < s2.length() && s1.charAt(i) == s2.charAt(i);
      ++i
    );

    return s1.substring(0, i);
  }



  public static Activity[]
  getActivities(Client client, URL url) throws Exception
  {
    URL[]	collections = client.getActivityCollections(url);
    List	result = new ArrayList();

    for (int i = 0; i < collections.length; ++i)
    {
      result.addAll
      (
        getDisplayablesFromCollection
        (
          client,
          collections[i],
          new CreateDisplayable()
          {
            public Object
            create(URL href, String displayName)
            {
              return new Activity(href, displayName);
            }
          }
        )
      );
    }

    return (Activity[]) result.toArray(new Activity[0]);
  }



  public static URL[]
  getAllActivities(Client client, URL url)
  {
    try
    {
      return getAllActivities(client, url, client.getActivityCollections(url));
    }

    catch (Exception e)
    {
      try
      {
        report(url, e);
      }

      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      return new URL[0];
    }
  }



  public static URL[]
  getAllActivities(Client client, URL url, URL[] collections)
  {
    try
    {
      return getAllCollectionMembers(collections);
    }

    catch (Exception e)
    {
      try
      {
        report(url, e);
      }

      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      return new URL[0];
    }
  }



  private static URL[]
  getAllCollectionMembers(URL[] collections) throws IOException
  {
    SortedSet	set =
      new TreeSet
      (
        new Comparator()
        {
          public int
          compare(Object o1, Object o2)
          {
            return o1.toString().compareTo(o2.toString());
          }
        }
      );
    URLManager	manager = new URLManager();

    for (int i = 0; i < collections.length; ++i)
    {
      set.addAll(Arrays.asList(manager.getContained(collections[i])));
    }

    return (URL[]) set.toArray(new URL[0]);
  }



  public static SortedSet
  getAllPrincipals(Client client, URL url) throws Exception
  {
    SortedSet	result = getAllEndUserPrincipals(client, url);

    result.add
    (
      new Principal
      (
        new ExpandedName(Constants.DAV_URI, "owner"),
        getResource("label_owner")
      )
    );

    result.add
    (
      new Principal
      (
        "unauthenticated",
        getResource("label_unauthenticated")
      )
    );

    result.add(new Principal("self", getResource("label_self")));

    return result;
  }



  /**
   * Includes the symbolic principals for selection by the end-user.
   */

  public static SortedSet
  getAllEndUserPrincipals(Client client, URL url) throws Exception
  {
    SortedSet	result = new TreeSet(Arrays.asList(getPrincipals(client, url)));

    result.add(new Principal("all", getResource("label_all")));
    result.
      add(new Principal("authenticated", getResource("label_authenticated")));

    for (Iterator i = result.iterator(); i.hasNext();)
    {
      Principal	principal = (Principal) i.next();

      if (principal.value instanceof URL)
      {
        principal.value = be.re.net.Util.stripUserInfo((URL) principal.value);
      }
    }

    return result;
  }



  public static URL[]
  getAllViews(Client client, URL url)
  {
    try
    {
      return getAllViews(client, url, client.getViewCollections(url));
    }

    catch (Exception e)
    {
      try
      {
        report(url, e);
      }

      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      return new URL[0];
    }
  }



  public static URL[]
  getAllViews(Client client, URL url, URL[] collections)
  {
    try
    {
      return getAllCollectionMembers(collections);
    }

    catch (Exception e)
    {
      try
      {
        report(url, e);
      }

      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      return new URL[0];
    }
  }



  private static List
  getDisplayablesFromCollection
  (
    Client			client,
    final URL			collection,
    final CreateDisplayable	create
  ) throws Exception
  {
    Client.Response	response = null;

    try
    {
      response =
        client.propfindSpecific
        (
          collection,
          new ExpandedName[]
          {new ExpandedName(Constants.DAV_URI, "displayname")},
          "1"
        );

      final List		result = new ArrayList();

      if (response.getStatusCode() != 207)
      {
        report(collection, response);

        return result;
      }

      be.re.webdav.Util.readDisplayNameList
      (
        collection,
        response,
        new be.re.webdav.Util.DisplayNameHandler()
        {
          public boolean
          handle(URL href, String displayName)
          {
            result.add
            (
              create.create(be.re.net.Util.stripUserInfo(href), displayName)
            );

            return true;
          }
        },
        false
      );

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
   * This method returns all the properties that exist in a Pincette server.
   */

  public static String[]
  getExistingMetaProperties(URL url) throws IOException
  {
    return getExistingMetaPropertySet(url, "properties/");
  }



  private static String[]
  getExistingMetaPropertySet(URL url, String path) throws IOException
  {
    URL[]	collections = new Client().getMetaCollections(url);
    Set		result = new HashSet();

    for (int i = 0; i < collections.length; ++i)
    {
      String[]	names =
        be.re.webdav.Util.readDisplayNameList(new URL(collections[0], path));

      for (int j = 0; j < names.length; ++j)
      {
        result.add(names[j]);
      }
    }

    return (String[]) result.toArray(new String[0]);
  }



  /**
   * This method returns all the values of a property that exist in a Pincette
   * server.
   */

  public static String[]
  getExistingMetaPropertyValues(URL url, String property) throws IOException
  {
    return getExistingMetaPropertySet(url, "properties/" + property + "/");
  }



  /**
   * The labels are sorted alphabetically.
   */

  static String[]
  getLabels(Client client, URL url) throws IOException
  {
    Client.Response	response = null;

    try
    {
      response =
        client.report
        (
          url,
          Util.class.getResourceAsStream("res/get_labels.xml"),
          null,
          "0"
        );

      if (response.getStatusCode() != 207)
      {
        report(url, response);

        return null;
      }

      Document	document = response.createDocument();

      if (document == null)
      {
        return null;
      }

      SortedSet	labels = new TreeSet();

      extractLabels(document.getDocumentElement().getFirstChild(), labels);

      return (String[]) labels.toArray(new String[0]);
    }

    catch (Exception e)
    {
      report(url, e);

      return null;
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }
    }
  }



  private static String
  getMessage(Throwable e)
  {
    String	message =
      e.getCause() != null ? getMessage(e.getCause()) : null;

    return message != null ? message : e.getMessage();
  }



  public static String
  getPrincipalName(Object principal)
  {
    return
      "all".equals(principal) ?
        Util.getResource("label_all") :
        (
          "authenticated".equals(principal) ?
            Util.getResource("label_authenticated") :
            (
              "unauthenticated".equals(principal) ?
                Util.getResource("label_unauthenticated") :
                (
                  "self".equals(principal) ?
                    Util.getResource("label_self") :
                    (
                      new ExpandedName(Constants.DAV_URI, "owner").
                        equals(principal) ?
                        Util.getResource("label_owner") :
                        (
                          principal instanceof URL ?
                            getPrincipalName((URL) principal) :
                            principal.toString()
                        )
                    )
                )
            )
        );
  }



  private static String
  getPrincipalName(URL principal)
  {
    String	result =
      be.re.net.Util.
        unescapeUriSpecials(be.re.net.Util.getLastPathSegment(principal));

    return
      result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
  }



  public static Principal[]
  getPrincipals(Client client, URL url) throws Exception
  {
    URL[]	collections = client.getPrincipalCollections(url);
    List	result = new ArrayList();

    for (int i = 0; i < collections.length; ++i)
    {
      result.addAll(getPrincipalsFromCollection(client, collections[i]));
    }

    return (Principal[]) result.toArray(new Principal[0]);
  }



  static Principal[]
  getPrincipals(Client client, URL url, Document document) throws Exception
  {
    Element[]	elements =
      be.re.xml.Util.selectElements
      (
        document.getDocumentElement(),
        new ExpandedName[]
        {
          new ExpandedName(Constants.DAV_URI, "response"),
          new ExpandedName(Constants.DAV_URI, "propstat"),
          new ExpandedName(Constants.DAV_URI, "prop"),
          new ExpandedName(Constants.DAV_URI, "principal-collection-set"),
          new ExpandedName(Constants.DAV_URI, "href")
        }
      );
    List	result = new ArrayList();

    for (int i = 0; i < elements.length; ++i)
    {
      URL	collection = new URL(url, be.re.xml.Util.getText(elements[i]));

      if (url.getUserInfo() != null)
      {
        collection = be.re.net.Util.setUserInfo(collection, url.getUserInfo());
      }

      result.addAll(getPrincipalsFromCollection(client, collection));
    }

    return (Principal[]) result.toArray(new Principal[0]);
  }



  private static List
  getPrincipalsFromCollection(Client client, final URL collection)
    throws Exception
  {
    Client.Response	response = null;

    try
    {
      response =
        client.propfindSpecific
        (
          collection,
          new ExpandedName[]
          {new ExpandedName(Constants.DAV_URI, "displayname")},
          "1"
        );

      final List	result = new ArrayList();

      if (response.getStatusCode() != 207)
      {
        report(collection, response);

        return result;
      }

      be.re.webdav.Util.readDisplayNameList
      (
        collection,
        response,
        new be.re.webdav.Util.DisplayNameHandler()
        {
          public boolean
          handle(URL href, String displayName)
          {
            result.add(new Principal(href, displayName));

            return true;
          }
        },
        false
      );

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



  public static String
  getResource(String key)
  {
    if (bundle == null)
    {
      bundle = ResourceBundle.getBundle("be.re.webdav.cmd.res.Res");
    }

    return bundle.getString(key);
  }



  static String[]
  getTexts(Element[] elements)
  {
    String[]	result = new String[elements.length];

    for (int i = 0; i < elements.length; ++i)
    {
      result[i] = be.re.xml.Util.getText(elements[i]);
    }

    return result;
  }



  static Map
  getVersionTree(URL url, Document document) throws Exception
  {
    return
      getVersionTree
      (
        url,
        be.re.xml.Util.selectElements
        (
          document.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "version-history"),
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "version-set"),
            new ExpandedName(Constants.DAV_URI, "response")
          }
        ),
        be.re.xml.Util.selectElements
        (
          document.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "checked-in"),
            new ExpandedName(Constants.DAV_URI, "href")
          }
        ),
        be.re.xml.Util.selectElements
        (
          document.getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "checked-out"),
            new ExpandedName(Constants.DAV_URI, "href")
          }
        )
      );
  }



  private static Map
  getVersionTree
  (
    URL		url,
    Element[]	responses,
    Element[]	checkedIn,
    Element[]	checkedOut
  ) throws Exception
  {
    String	checkedInVersion =
      checkedIn.length == 1 ?
        new URL(url, be.re.xml.Util.getText(checkedIn[0])).toString() : null;
    String	checkedOutVersion =
      checkedOut.length == 1 ?
        new URL(url, be.re.xml.Util.getText(checkedOut[0])).toString() : null;
    Map		result = new HashMap();

    for (int i = 0; i < responses.length; ++i)
    {
      String	version =
        new URL
        (
          url,
          be.re.xml.Util.getText
          (
            be.re.xml.Util.
              selectFirstChild(responses[i], Constants.DAV_URI, "href")
          )
        ).toString();

      result.put
      (
        version,
        new VersionEntry
        (
          url,
          be.re.xml.Util.getText
          (
            be.re.xml.Util.selectElement
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "version-name")
              }
            )
          ),
          version,
          getTexts
          (
            be.re.xml.Util.selectElements
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "activity-set"),
                new ExpandedName(Constants.DAV_URI, "href")
              }
            )
          ),
          getTexts
          (
            be.re.xml.Util.selectElements
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "successor-set"),
                new ExpandedName(Constants.DAV_URI, "href")
              }
            )
          ),
          getTexts
          (
            be.re.xml.Util.selectElements
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "checkout-set"),
                new ExpandedName(Constants.DAV_URI, "response"),
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "auto-merge-set"),
                new ExpandedName(Constants.DAV_URI, "href")
              }
            )
          ),
          getTexts
          (
            be.re.xml.Util.selectElements
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "checkout-set"),
                new ExpandedName(Constants.DAV_URI, "response"),
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "merge-set"),
                new ExpandedName(Constants.DAV_URI, "href")
              }
            )
          ),
          version.equals(checkedInVersion),
          version.equals(checkedOutVersion),
          be.re.xml.Util.selectElements
          (
            responses[i],
            new ExpandedName[]
            {
              new ExpandedName(Constants.DAV_URI, "propstat"),
              new ExpandedName(Constants.DAV_URI, "prop"),
              new ExpandedName(Constants.DAV_URI, "checkout-set"),
              new ExpandedName(Constants.DAV_URI, "response"),
              new ExpandedName(Constants.DAV_URI, "href")
            }
          ).length > 0,
          be.re.xml.Util.getText
          (
            be.re.xml.Util.selectElement
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "creationdate")
              }
            )
          ),
          be.re.xml.Util.getText
          (
            be.re.xml.Util.selectElement
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "creator-displayname")
              }
            )
          ),
          be.re.xml.Util.getText
          (
            be.re.xml.Util.selectElement
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "getlastmodified")
              }
            )
          ),
          be.re.xml.Util.getText
          (
            be.re.xml.Util.selectElement
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "getcontentlength")
              }
            )
          ),
          be.re.xml.Util.getText
          (
            be.re.xml.Util.selectElement
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "comment")
              }
            )
          ),
          getTexts
          (
            be.re.xml.Util.selectElements
            (
              responses[i],
              new ExpandedName[]
              {
                new ExpandedName(Constants.DAV_URI, "propstat"),
                new ExpandedName(Constants.DAV_URI, "prop"),
                new ExpandedName(Constants.DAV_URI, "label-name-set"),
                new ExpandedName(Constants.DAV_URI, "label-name")
              }
            )
          )
        )
      );
    }

    return result;
  }



  /**
   * Returns the view <code>url</code> is in or <code>null</code> if it isn't
   * in any.
   */

  public static URL
  getView(URL url, URL[] views)
  {
    String	s = url.toString();

    for (int i = 0; i < views.length; ++i)
    {
      if
      (
        s.startsWith
        (
          views[i].toString().endsWith("/") ?
            views[i].toString() : (views[i].toString() + "/")
        )
      ) // Avoid matching a view name that is a substring of it.
      {
        return views[i];
      }
    }

    return null;
  }



  public static URL[]
  getViewUrls(View[] views)
  {
    URL[]	result = new URL[views.length];

    for (int i = 0; i < views.length; ++i)
    {
      result[i] = views[i].url;
    }

    return result;
  }



  public static View[]
  getViews(Client client, URL url) throws Exception
  {
    URL[]	collections = client.getViewCollections(url);
    List	result = new ArrayList();

    for (int i = 0; i < collections.length; ++i)
    {
      result.addAll
      (
        getDisplayablesFromCollection
        (
          client,
          collections[i],
          new CreateDisplayable()
          {
            public Object
            create(URL href, String displayName)
            {
              return new View(href, displayName);
            }
          }
        )
      );
    }

    return (View[]) result.toArray(new View[0]);
  }



  public static boolean
  report(URL url, Client.Response response) throws IOException
  {
    return report(url, response, null, true);
  }



  public static boolean
  report
  (
    URL			url,
    Client.Response	response,
    boolean		reportAsError
  ) throws IOException
  {
    return report(url, response, null, reportAsError);
  }



  public static boolean
  report(URL url, Client.Response response, String subject) throws IOException
  {
    Reporter	reporter =
      reportWriter != null ? new Reporter(reportWriter) : new Reporter();

    try
    {
      return reporter.report(url, response, subject, true);
    }

    finally
    {
      reporter.close();
    }
  }



  public static boolean
  report
  (
    URL			url,
    Client.Response	response,
    String		subject,
    boolean		reportAsError
  ) throws IOException
  {
    Reporter	reporter =
      reportWriter != null ? new Reporter(reportWriter) : new Reporter();

    try
    {
      return reporter.report(url, response, subject, reportAsError);
    }

    finally
    {
      reporter.close();
    }
  }



  public static void
  report(URL url, Exception e) throws IOException
  {
    Reporter	reporter =
      reportWriter != null ? new Reporter(reportWriter) : new Reporter();

    try
    {
      reporter.report(url, getMessage(e), true);
    }

    finally
    {
      reporter.close();
    }
  }



  public static void
  report(URL url, String message, boolean error) throws IOException
  {
    Reporter	reporter =
      reportWriter != null ? new Reporter(reportWriter) : new Reporter();

    try
    {
      reporter.report(url, message, error);
    }

    finally
    {
      reporter.close();
    }
  }



  private static String[]
  resolveUrls(URL url, String[] urls) throws Exception
  {
    for (int i = 0; i < urls.length; ++i)
    {
      urls[i] = new URL(url, urls[i]).toString();
    }

    return urls;
  }



  public static void
  setReportWriter(Writer writer)
  {
    reportWriter = writer;
  }



  static boolean
  test(Client.Response response, int[] expected) throws Exception
  {
    try
    {
      boolean	found = false;

      for (int i = 0; i < expected.length && !found; ++i)
      {
        if (response.getStatusCode() == expected[i])
        {
          found = true;
        }
      }

      InputStream	body = response.getBody();

      if (body != null)
      {
        StreamConnector.copy(body, System.out);
      }

      if (!found)
      {
        System.err.println(String.valueOf(response.getStatusCode()));
      }

      return found;
    }

    finally
    {
      response.close();
    }
  }



  /**
   * Returns the lock token if the lock succeeds, <code>LOCKED</code> if it
   * doesn't and <code>null</code> if the method is not supported or if access
   * is denied.
   */

  static String
  tryToLock(URL url, Client client) throws Exception
  {
    try
    {
      return client.lock(url, -1);
    }

    catch (ProtocolException e)
    {
      if (e.getCode() == 423) // Locked.
      {
        Client.Lock[]	locks = client.getLocks(url);

        Util.report
        (
          url,
          (
            locks.length > 0 && locks[0].owner != null ?
              (Util.getResource("msg_locked_by") + " " + locks[0].owner) :
              Util.getResource("msg_locked")
          ) + ".",
          true
        );

        return LOCKED;
      }

      return null; // Not supported or no access.
    }
  }



  private interface CreateDisplayable

  {

    public Object	create	(URL href, String displayName);

  } // CreateDisplayable



  static class VersionEntry

  {

    String[]		activitySet;
    String[]		autoMergeSet;
    String		comment;
    String		creationDate;
    String		creator;
    boolean		hasCheckOutSet;
    String		lastModified;
    String[]		labels;
    String		length;
    String[]		mergeSet;
    String		name;
    boolean		selectedCheckedIn;
    boolean		selectedCheckedOut;
    String[]		successors;
    String		url;



    private
    VersionEntry
    (
      URL	url,
      String	name,
      String	versionUrl,
      String[]	activitySet,
      String[]	successors,
      String[]	autoMergeSet,
      String[]	mergeSet,
      boolean	selectedCheckedIn,
      boolean	selectedCheckedOut,
      boolean	hasCheckOutSet,
      String	creationDate,
      String	creator,
      String	lastModified,
      String	length,
      String	comment,
      String[]	labels
    ) throws Exception
    {
      this.name = name;
      this.url = versionUrl;
      this.selectedCheckedIn = selectedCheckedIn;
      this.selectedCheckedOut = selectedCheckedOut;
      this.hasCheckOutSet = hasCheckOutSet;
      this.creator = creator;
      this.comment = comment;
      this.labels = labels;
      this.length = length;
      this.activitySet = resolveUrls(url, activitySet);
      this.successors = resolveUrls(url, successors);
      this.autoMergeSet =
        (String[])
          new HashSet(Arrays.asList(resolveUrls(url, autoMergeSet))).
            toArray(new String[0]);
      this.mergeSet =
        (String[])
          new HashSet(Arrays.asList(resolveUrls(url, mergeSet))).
            toArray(new String[0]);

      if (creationDate != null)
      {
        long	time = be.re.util.Util.parseTimestamp(creationDate);

        if (time != -1)
        {
          this.creationDate = new SimpleDateFormat().format(new Date(time));
        }
      }

      if (lastModified != null)
      {
        long	time = be.re.net.Util.httpDate(lastModified);

        if (time != -1)
        {
          this.lastModified = new SimpleDateFormat().format(new Date(time));
        }
      }
    }

  } // VersionEntry

} // Util
