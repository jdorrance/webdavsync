package be.re.net;

import be.re.gui.util.ConfirmationDialog;
import be.re.gui.util.MimeTypeListCellRenderer;
import be.re.io.DevNullInputStream;
import be.re.io.URLInputStream;
import be.re.net.HTTPClient;
import be.re.util.Array;
import be.re.util.MimeType;
import be.re.webdav.Client;
import be.re.xml.ExpandedName;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;



/**
 * @author Werner Donn\u00e9
 */

public class URLManagerDAV implements URLManager.ProtocolHandler

{

  private final static int		CANCEL = 2;
  private final static String		CANCEL_ALL = "cancel_all";
  private final static int		CHECKED_IN_POSITION = 1;
  private final static int		ERROR = 1;
  private final static int		OK = 0;
  private final static ExpandedName[]	ORDERED_PROPERTIES =
    new ExpandedName[]
    {
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "displayname"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "getlastmodified"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "getcontentlength"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "getcontenttype"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "owner"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "creator-displayname"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "creationdate"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "checked-in"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "comment"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "auto-version"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "quota-used-bytes"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "quota-available-bytes"),
      new ExpandedName(be.re.webdav.Constants.URI, "home")
    };
  private final static ExpandedName[]	SUPPORTED_PROPERTIES =
    new ExpandedName[]
    {
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "auto-version"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "checked-in"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "checked-out"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "comment"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "creationdate"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "creator-displayname"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "displayname"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "getcontentlength"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "getcontenttype"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "getlastmodified"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "lockdiscovery"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "owner"),
      new ExpandedName(be.re.webdav.Constants.DAV_URI, "version-history")
    };

  private Client			client;
  private static XMLInputFactory	inputFactory;
  private static String			lastChosenMimeType;
  private URLManager			manager;
  private static List			mimeTypes;



  static
  {
    try
    {
      inputFactory = be.re.xml.stax.Util.newInputFactory(false, true);

      MimeType.LabeledMimeType[]	types =
        MimeType.getKnownLabeledMimeTypes();

      Arrays.sort
      (
        types,
        new Comparator()
        {
          public int
          compare(Object o1, Object o2)
          {
            return
              ((MimeType.LabeledMimeType) o1).getLabel().
                compareToIgnoreCase(((MimeType.LabeledMimeType) o2).getLabel());
          }
        }
      );

      mimeTypes = new ArrayList(Arrays.asList(types));
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public
  URLManagerDAV(URLManager manager)
  {
    this(manager, null, null, null);
  }



  public
  URLManagerDAV
  (
    URLManager	manager,
    String	username,
    String	password,
    String	onBehalfOf
  )
  {
    this.manager = manager;
    this.client = new Client(username, password, onBehalfOf);
  }



  public boolean
  canCopy(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return
      (
        "http".equals(destination.getProtocol()) ||
          "https".equals(destination.getProtocol())
      ) && (manager.isContainer(destination) || source.length == 1);
  }



  public boolean
  canCopy(URL source, URL destination) throws IOException, ProtocolException
  {
    return
      "http".equals(destination.getProtocol()) ||
        "https".equals(destination.getProtocol());
  }



  public boolean
  canDestroy(URL[] urls) throws IOException, ProtocolException
  {
    for (int i = 0; i < urls.length; ++i)
    {
      if
      (
        !"http".equals(urls[i].getProtocol())	&&
        !"https".equals(urls[i].getProtocol())
      )
      {
        return false;
      }
    }

    return true;
  } 



  public boolean
  canMove(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return canCopy(source, destination);
  }



  public boolean
  canMove(URL source, URL destination) throws IOException, ProtocolException
  {
    return
      (
        (
          "http".equals(source.getProtocol()) &&
            "http".equals(destination.getProtocol())
        ) ||
          (
            "https".equals(source.getProtocol()) &&
              "https".equals(destination.getProtocol())
          )
      ) && source.getAuthority().equals(destination.getAuthority());
  } 
  


  public boolean
  canPreserve()
  {
    return false;
  }
  
  

  private static void
  checkin(URL url, Client client) throws IOException, ProtocolException
  {
    try
    {
      client.checkin(url, false, false);
    }

    catch (ProtocolException e)
    {
      if (e.getCode() != 405 && e.getCode() != 501) // Not implemented.
      {
        throw e;
      }
    }

    catch (IOException e)
    {
      throw e;
    }
  }



  private static void
  checkout(URL url, Client client) throws IOException, ProtocolException
  {
    try
    {
      client.checkout(url, null, false);
    }

    catch (ProtocolException e)
    {
      if (e.getCode() != 405 && e.getCode() != 501 && e.getCode() != 409)
        // Not implemented or conflict such as checked out already.
      {
        throw e;
      }
    }

    catch (IOException e)
    {
      throw e;
    }
  }



  private PropertyCollector
  collectProperties(URL url, ExpandedName[] properties, boolean contents)
    throws IOException, ProtocolException
  {
    PropertyCollector	propertyCollector =
      new PropertyCollector(properties, client.getPrincipalCollections(url));

    Client.Response	response = null;

    try
    {
      response = client.propfindSpecific(url, properties, contents ? "1" : "0");

      if (response.getStatusCode() != 207)
      {
        report(url, response);

        return propertyCollector;
      }

      XMLEventReader	reader =
        inputFactory.createXMLEventReader(response.getBody());

      while (reader.hasNext())
      {
        XMLEvent	event = reader.nextEvent();

        if (isStartDavElement(event, "response"))
        {
          propertyCollector.addEntry
          (
            url,
            be.re.xml.stax.Util.accumulate(reader, event.asStartElement()),
            contents
          );
        }
      }
    }

    catch (XMLStreamException e)
    {
      throw new ProtocolException(e);
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }
    }

    return propertyCollector;
  }



  public void
  copy
  (
    URL[]		source,
    URL			destination,
    boolean		overwrite,
    URLManager.Resume	resume
  ) throws IOException, ProtocolException
  {
    String[]	acceptAllMimeType = new String[1];
    String	autoVersion =
      isContainer(destination) ?
        be.re.webdav.Util.getAutoVersion(destination) : null;
    boolean	isPrincipal = isPrincipal(destination, false);
    int		result = OK;

    if
    (
      "checkout".equals(autoVersion)		||
      "checkout-checkin".equals(autoVersion)
    )
    {
      checkout(destination, client);
    }

    for (int i = 0; i < source.length && result != CANCEL; ++i)
    {
      try
      {
        result =
          copyToDav
          (
            source[i],
            destination,
            overwrite,
            acceptAllMimeType,
            isPrincipal
          );
      }

      catch (Throwable e)
      {
        if (resume != null)
        {
          resume.handle(source[i], e);
        }
        else
        {
          if (e instanceof IOException)
          {
            throw (IOException) e;
          }

          if (e instanceof ProtocolException)
          {
            throw (ProtocolException) e;
          }

          throw new RuntimeException(e);
        }
      }
    }

    if ("checkout-checkin".equals(autoVersion))
    {
      checkin(destination, client);
    }
  }



  public boolean
  copy(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    return
      copyToDav
      (
        source,
        destination,
        overwrite,
        new String[1],
        isPrincipal(destination, false)
      ) == OK;
  }



  private int
  copyContainer
  (
    URL		source,
    URL		destination,
    boolean	overwrite,
    String[]	acceptAllMimeType
  ) throws IOException, ProtocolException
  {
    try
    {
      // If we don't overwrite the destination isn't supposed to exist.

      if (!overwrite || !exists(destination))
      {
        client.mkcol(destination);

        report
        (
          destination,
          be.re.webdav.Util.getReasonPhrase(HTTPClient.MKCOL, 201)
        );
      }

      checkout(destination, client);

      int	result = OK;
      URL[]	urls = manager.getContained(source);

      for (int i = 0; i < urls.length && result != CANCEL; ++i)
      {
        result =
          copyToDav
          (
            Util.stripQuery(urls[i]),
            destination,
            overwrite,
            acceptAllMimeType,
            false
          );
      }

      checkin(destination, client);
    }

    catch (Exception e)
    {
      report(destination, e);

      return ERROR;
    }

    return OK;
  }



  private int
  copyToDav
  (
    URL		source,
    URL		destination,
    boolean	overwrite,
    String[]	acceptAllMimeType,
    boolean	isPrincipal
  ) throws IOException, ProtocolException
  {
    if (manager.isPureContainer(destination))
    {
      destination = new URL(URLManager.newContainedName(source, destination));
    }

    if (sameServer(source, destination))
    {
      Client.Response	response =
        client.copy(source, destination, null, true, "infinity");

      try
      {
        int	result =
          response.getStatusCode() == 201 || response.getStatusCode() == 204 ?
            OK : ERROR;

        if (result == OK)
        {
          report(destination, "OK");
        }
        else
        {
          report(destination, response);
        }

        return result;
      }

      finally
      {
        response.close();
      }
    }

    return
      manager.isPureContainer(source) ?
        copyContainer(source, destination, overwrite, acceptAllMimeType) :
        createLeafVCR
        (
          new URL[]{destination},
          source,
          acceptAllMimeType,
          isPrincipal,
          false
        );
  }



  public URL
  create(URL url, boolean container) throws IOException, ProtocolException
  {
    return container ? createContainer(url) : createLeaf(url);
  }



  private URL
  createContainer(URL url) throws IOException, ProtocolException
  {
    boolean	view = isView(url);

    if (view)
    {
      url =
        new URL(url.toString().substring(0, url.toString().length() - 1));
          // Remove the trailing slash.
      client.mkview(url);
    }
    else
    {
      client.mkcol(url);
    }

    report
    (
      url,
      be.re.webdav.Util.
        getReasonPhrase(view ? HTTPClient.MKVIEW : HTTPClient.MKCOL, 201)
    );

    return url;
  }



  private URL
  createLeaf(URL url) throws IOException, ProtocolException
  {
    try
    {
      if (isActivity(url))
      {
        client.mkactivity(url);

        report
        (
          url,
          be.re.webdav.Util.getReasonPhrase(HTTPClient.MKACTIVITY, 201)
        );

        return url;
      }

      if (isView(url))
      {
        client.mkview(url);

        report
        (
          url,
          be.re.webdav.Util.getReasonPhrase(HTTPClient.MKVIEW, 201)
        );

        return url;
      }

      if (isPrincipal(url, true))
      {
        Client.Response	response = client.put(url, null, null, null);

        try
        {
          report(url, response);

          return url;
        }

        finally
        {
          response.close();
        }
      }

      URL[]	u = new URL[]{url};

      createLeafVCR(u, null, new String[1], false, true);

      return u[0];
    }

    catch (IOException e)
    {
      if (!URLManager.getInteractive())
      {
        throw e;
      }

      report(url, e);

      return null;
    }
  }



  private int
  createLeafVCR
  (
    URL[]	url,
    URL		source,
    String[]	acceptAllMimeType,
    boolean	isPrincipal,
    boolean	isNew
  ) throws IOException, ProtocolException
  { 
    boolean[]	cancelAllMimeTypes = new boolean[1];
    String	mimeType =
      isPrincipal ?
        null :
        getContentType
        (
          source != null ?
            (
              !Util.extractComposedUrlEntry(source).equals("") ?
                Util.extractComposedUrlEntry(source) :
                (
                  "ftp".equals(source.getProtocol()) ?
                    FTPClient.removeType(source.getFile()) : source.getFile()
                )
            ) : null,
          source != null ? source : url[0],
          acceptAllMimeType,
          cancelAllMimeTypes
        );

    if (isPrincipal || mimeType != null)
    {
      if (isNew)
      {
        String[]	extensions =
          MimeType.getExtensionsFromMimeType(mimeType);

        if (extensions.length > 0 && !url[0].toString().endsWith(extensions[0]))
        {
          url[0] = new URL(url[0].toString() + "." + extensions[0]);
        }
      }

      Client.Response	response =
        client.put
        (
          url[0],
          !isPrincipal && source != null ?
            (InputStream) new URLInputStream(source) :
            (InputStream) new DevNullInputStream(),
          mimeType,
          null
        );

      try
      {
        report(url[0], response);

        return
          response.getStatusCode() == 200 || response.getStatusCode() == 201 ||
            response.getStatusCode() == 204 ? OK : ERROR;
      }

      finally
      {
        response.close();
      }
    }
    else
    {
      if (cancelAllMimeTypes[0])
      {
        return CANCEL;
      }
    }

    return OK;
  }



  public void
  destroy(URL[] urls, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    if
    (
      URLManager.getInteractive()					  &&
      !new ConfirmationDialog(Util.getResource("title_delete")).confirm()
    )
    {
      return;
    }

    // Sort backwards in case there are versions, which is likely to cause
    // the most recent versions to be deleted first, thus avoiding conflicts.
    // There is no guarantee however.

    URL[]	copy = new URL[urls.length];

    System.arraycopy(urls, 0, copy, 0, urls.length);

    Arrays.sort
    (
      copy,
      new Comparator()
      {
        public int
        compare(Object o1, Object o2)
        {
          return -1 * o1.toString().compareTo(o2.toString());
        }
      }
    );

    URL[]	parents = getParents(copy);
    String[]	autoVersion = new String[parents.length];

    for (int i = 0; i < parents.length; ++i)
    {
      try
      {
        autoVersion[i] = be.re.webdav.Util.getAutoVersion(parents[i]);
      }

      catch (ProtocolException e)
      {
        if (e.getCode() != 404) // A version doesn't have a parent.
        {
          throw e;
        }
      }

      if
      (
        "checkout".equals(autoVersion[i])		||
        "checkout-checkin".equals(autoVersion[i])
      )
      {
        checkout(parents[i], client);
      }
    }

    for (int i = 0; i < copy.length; ++i)
    {
      if (resume != null)
      {
        try
        {
          destroy(copy[i]);
        }

        catch (Throwable e)
        {
          resume.handle(copy[i], e);
        }
      }
      else
      {
        destroy(copy[i]);
      }
    }

    for (int i = 0; i < parents.length; ++i)
    {
      if ("checkout-checkin".equals(autoVersion[i]))
      {
        checkin(parents[i], client);
      }
    }
  }



  public void
  destroy(URL url) throws IOException, ProtocolException
  {
    client.delete(url);
    report(url, "OK");
  }



  public boolean
  equal(URL url1, URL url2)
  {
    return
      Util.unescapeUriSpecials(url1.toString()).
        equals(Util.unescapeUriSpecials(url2.toString()));
  }



  public boolean
  exists(URL url) throws IOException, ProtocolException
  {
    try
    {
      Headers     headers = new Headers();

      HTTPClient.request
      (
        HTTPClient.HEAD,
        url,
        null,
        null,
        headers,
        null,
        null
      ).close();

      return
        headers.get("Status-Code").length > 0 &&
          !headers.get("Status-Code")[0].equals("404");
    }

    catch (IOException e)
    {
      report(url, e);

      throw e;
    }

    catch (Exception e)
    {
      report(url, e);

      throw new RuntimeException(e);
    }
  }



  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    try
    {
      PropertyCollector	collector =
        collectProperties
        (
          url,
          new ExpandedName[]
          {
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "checked-out"),
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "lockdiscovery"),
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "resourcetype")
          },
          true
        );
      URL[]		urls = new URL[collector.entries.size()];

      for (int i = 0; i < collector.entries.size(); ++i)
      {
        urls[i] =
          be.re.net.Util.escapedUrl
          (
            collector.addDecoration
            (
              collector.entries.get(i).href,
              collector.entries.get(i)
            )
          );
      }

      return urls;
    }

    catch (Exception e)
    {
      report(url, e);

      return new URL[0];
    }
  }



  public URLManager.Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    try
    {
      return
        collectProperties
        (
          url,
          isPrincipalCollection(url) ?
            (ExpandedName[])
              Array.append
              (
                Array.append
                (
                  Array.append
                  (
                    Array.append
                    (
                      SUPPORTED_PROPERTIES,
                      new ExpandedName
                      (
                        be.re.webdav.Constants.DAV_URI,
                        "resourcetype"
                      )
                    ),
                    new ExpandedName
                    (
                      be.re.webdav.Constants.DAV_URI,
                      "quota-available-bytes"
                    )
                  ),
                  new ExpandedName
                  (
                    be.re.webdav.Constants.DAV_URI,
                    "quota-used-bytes"
                  )
                ),
                new ExpandedName(be.re.webdav.Constants.URI, "home")
             ) : SUPPORTED_PROPERTIES,
          true
        ).getProperties();
    }

    catch (ProtocolException e)
    {
      report(url, e);
      throw e;
    }

    catch (IOException e)
    {
      report(url, e);
      throw e;
    }

    catch (Exception e)
    {
      report(url, e);
      throw new RuntimeException(e);
    }
  }



  private String
  getContentType
  (
    String	sourceName,
    URL		url,
    String[]	acceptAllMimeType,
    boolean[]	cancelAllMimeTypes
  ) throws IOException
  {
    if (acceptAllMimeType[0] != null)
    {
      return acceptAllMimeType[0];
    }

    String	mimeType =
      sourceName == null ? null : MimeType.getContentTypeFromName(sourceName);

    if
    (
      mimeType != null					&&
      (
        !"application/octet-stream".equals(mimeType)	||
        !URLManager.getInteractive()
      )
    )
    {
      return mimeType;
    }

    if (mimeType == null && !URLManager.getInteractive())
    {
      return "application/octet-stream";
    }

    final ContentTypeDialog	dialog =
      new ContentTypeDialog
      (
        Util.getResource("dav_content_type_dialog_title"),
        url,
        lastChosenMimeType != null ?
          lastChosenMimeType : "application/octet-stream",
        acceptAllMimeType,
        cancelAllMimeTypes
      );

    SwingUtilities.invokeLater
    (
      new Runnable()
      {
        public void
        run()
        {
          dialog.setVisible(true);
        }
      }
    );

    synchronized (dialog)
    {
      try
      {
        dialog.wait();
      }

      catch (InterruptedException e)
      {
        throw new RuntimeException(e);
      }
    }

    return dialog.mimeType;
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    PropertyCollector	collector =
      collectProperties
      (
        url,
        new ExpandedName[]
        {
          new ExpandedName(be.re.webdav.Constants.DAV_URI, "checked-out"),
          new ExpandedName(be.re.webdav.Constants.DAV_URI, "lockdiscovery")
        },
        false
      );
    Properties		result = new Properties();

    if (collector.entries.size() == 1)
    {
      if
      (
        !"".equals
        (
          collector.getPropertyValue
          (
            collector.entries.get(0),
            new ExpandedName
            (
              be.re.webdav.Constants.DAV_URI,
              "checked-out"
            )
          )
        )
      )
      {
        result.setProperty("checkedout", "true");
      }

      if
      (
        "true".equals
        (
          collector.getPropertyValue
          (
            collector.entries.get(0),
            new ExpandedName
            (
              be.re.webdav.Constants.DAV_URI,
              "lockdiscovery"
            )
          )
        )
      )
      {
        result.setProperty("locked", "true");
      }
    }

    return result;
  }



  private static URL[]
  getParents(URL[] urls)
  {
    Set	set = new HashSet();

    for (int i = 0; i < urls.length; ++i)
    {
      set.add(URLManager.getParent(urls[i]).toString());
    }

    List	result = new ArrayList();

    try
    {
      for (Iterator i = set.iterator(); i.hasNext();)
      {
        result.add(new URL((String) i.next()));
      }
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }

    return (URL[]) result.toArray(new URL[0]);
  }



  public URLManager.Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    ExpandedName[]	properties =
      intersect(SUPPORTED_PROPERTIES, client.getSupportedLiveProperties(url));

    try
    {
      return
        properties.length == 0 ?
          new URLManager.Property[0] :
          collectProperties(url, properties, false).getProperties()[0];
    }

    catch (ProtocolException e)
    {
      report(url, e);
      throw e;
    }

    catch (IOException e)
    {
      report(url, e);
      throw e;
    }

    catch (Exception e)
    {
      report(url, e);
      throw new RuntimeException(e);
    }
  }



  private static int
  getPropertyPosition(ExpandedName property)
  {
    for (int i = 0; i < ORDERED_PROPERTIES.length; ++i)
    {
      if (ORDERED_PROPERTIES[i].equals(property))
      {
        return i;
      }
    }

    return -1;
  }



  private static String
  getText(Node node)
  {
    return
      node == null ?
        "" :
        (
          (
            node instanceof Text ?
              ((Text) node).getData() :
              (node instanceof Element ? getText(node.getFirstChild()) : "")
          ) + getText(node.getNextSibling())
        );
  }



  private static boolean
  hasPincetteAdmins(URL[] urls) throws IOException
  {
    for (int i = 0; i < urls.length; ++i)
    {
      String	parent = Util.getLastPathSegment(URLManager.getParent(urls[i]));

      if
      (
        (
          (
            "groups/".equals(parent)				||
            "users/".equals(parent)
          )							&&
          be.re.webdav.Util.isPrincipal(urls[i])
        )							||
        (
          "view/".equals(parent)				&&
          be.re.webdav.Util.isView
          (
            // Avoid asking for root in the view.
            urls[i].toString().endsWith("/") ?
              new URL
              (
                urls[i].toString().
                  substring(0, urls[i].toString().length() - 1)
              ) : urls[i]
          )
        )
      )
      {
        return true;
      }
    }

    return false;
  }



  private static boolean
  hasVersion(URL[] urls) throws IOException
  {
    for (int i = 0; i < urls.length; ++i)
    {
      if (be.re.webdav.Util.isVersion(urls[i]))
      {
        return true;
      }
    }

    return false;
  }



  private static ExpandedName[]
  intersect(ExpandedName[] s1, ExpandedName[] s2)
  {
    Set	result = new HashSet(Arrays.asList(s1));

    result.retainAll(Arrays.asList(s2));

    return (ExpandedName[]) result.toArray(new ExpandedName[0]);
  }



  private boolean
  isActivity(URL url) throws IOException
  {
    try
    {
      URL[]	urls = client.getActivityCollections(manager.getParent(url));

      for (int i = 0; i < urls.length; ++i)
      {
        if (manager.getParent(url).equals(urls[i]))
        {
          return true;
        }
      }
    }

    catch (ProtocolException e)
    {
    }

    return false;
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    return Util.stripQuery(url).toString().endsWith("/");
  }



  private boolean
  isPrincipal(URL url, boolean checkParentForCollections) throws IOException
  {
    try
    {
      URL[]	urls =
        client.getPrincipalCollections
        (
          checkParentForCollections ? manager.getParent(url) : url
        );

      for (int i = 0; i < urls.length; ++i)
      {
        if (manager.getParent(url).equals(urls[i]))
        {
          return true;
        }
      }
    }

    catch (ProtocolException e)
    {
    }

    return false;
  }



  private boolean
  isPrincipalCollection(URL url) throws IOException
  {
    try
    {
      URL[]	urls = client.getPrincipalCollections(url);

      for (int i = 0; i < urls.length; ++i)
      {
        if (manager.equal(urls[i], url))
        {
          return true;
        }
      }
    }

    catch (ProtocolException e)
    {
    }

    return false;
  }



  private static boolean
  isPropstatOK(Element propstat)
  {
    Node	status =
      be.re.xml.Util.
        selectFirstChild(propstat, be.re.webdav.Constants.DAV_URI, "status");

    return
      status == null ||
        be.re.xml.Util.getText(status).startsWith("HTTP/1.1 200 ") ||
        be.re.xml.Util.getText(status).startsWith("HTTP/1.0 200 ");
  }



  private static boolean
  isStartDavElement(XMLEvent event, String name)
  {
    return
      event.isStartElement() &&
        event.asStartElement().getName().
          equals(new QName(be.re.webdav.Constants.DAV_URI, name));
  }



  private boolean
  isView(URL url) throws IOException
  {
    try
    {
      URL[]	collections = client.getViewCollections(manager.getParent(url));

      for (int i = 0; i < collections.length; ++i)
      {
        if (manager.getParent(url).equals(collections[i]))
        {
          return true;
        }
      }
    }

    catch (ProtocolException e)
    {
    }

    return false;
  }



  public boolean
  link(URL url, URL newBinding) throws IOException, ProtocolException
  {
    try
    {
      client.bind(url, newBinding, false);
    }

    catch (ProtocolException e)
    {
      report(newBinding, e);

      return false;
    }

    return true;
  }



  public boolean
  move(URL[] source, URL destination, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    String[]	acceptAllMimeType = new String[1];
    String	autoVersion =
      isContainer(destination) ?
        be.re.webdav.Util.getAutoVersion(destination) : null;
    boolean	isPrincipal = isPrincipal(destination, false);
    URL[]	parents = getParents(source);
    String[]	autoVersionParents = new String[parents.length];

    if
    (
      "checkout".equals(autoVersion)		||
      "checkout-checkin".equals(autoVersion)
    )
    {
      checkout(destination, client);
    }

    for (int i = 0; i < parents.length; ++i)
    {
      if
      (
        "http".equals(parents[i].getProtocol())		||
        "https".equals(parents[i].getProtocol())
      )
      {
        try
        {
          autoVersionParents[i] = be.re.webdav.Util.getAutoVersion(parents[i]);
        }

        catch (ProtocolException e)
        {
          if (e.getCode() != 404) // A version doesn't have a parent.
          {
            throw e;
          }
        }

        if
        (
          "checkout".equals(autoVersionParents[i])		||
          "checkout-checkin".equals(autoVersionParents[i])
        )
        {
          checkout(parents[i], client);
        }
      }
    }

    for (int i = 0; i < source.length; ++i)
    {
      try
      {
        if (canMove(source[i], destination))
        {
          manager.move(source[i], destination); // Has container logic.
        }
        else
        {
          if
          (
            copyToDav
            (
              source[i],
              destination,
              true,
              acceptAllMimeType,
              isPrincipal
            ) == OK
          )
          {
            manager.destroy(source[i]);
          }
        }
      }

      catch (Throwable e)
      {
        if (resume != null)
        {
          resume.handle(source[i], e);
        }
        else
        {
          if (e instanceof IOException)
          {
            throw (IOException) e;
          }

          if (e instanceof ProtocolException)
          {
            throw (ProtocolException) e;
          }

          throw new RuntimeException(e);
        }
      }
    }

    if ("checkout-checkin".equals(autoVersion))
    {
      checkin(destination, client);
    }

    for (int i = 0; i < parents.length; ++i)
    {
      if ("checkout-checkin".equals(autoVersionParents[i]))
      {
        checkin(parents[i], client);
      }
    }

    return true;
  }



  /**
   * Only the view of the source is taken into account, because we can't be in
   * two views at the same time in WebDAV.
   */

  public boolean
  move(URL source, URL destination) throws IOException, ProtocolException
  {
    try
    {
      client.move(source, destination, false);
      report(destination, "OK");
    }

    catch (ProtocolException e)
    {
      report(source, e);

      return false;
    }

    return true;
  }



  private static void
  report(URL url, Exception e) throws IOException
  {
    if (URLManager.getInteractive())
    {
      be.re.webdav.cmd.Util.report(url, e);
    }
    else
    {
      be.re.util.Util.printStackTrace(e);
    }
  }



  private static void
  report(URL url, Client.Response response) throws IOException
  {
    if (URLManager.getInteractive())
    {
      be.re.webdav.cmd.Util.report(url, response);
    }
  }



  private static void
  report(URL url, String message) throws IOException
  {
    if (URLManager.getInteractive())
    {
      be.re.webdav.cmd.Util.report(url, message, false);
    }
  }



  private static boolean
  sameServer(URL url1, URL url2)
  {
    return
      url1.getProtocol().equals(url2.getProtocol()) &&
        url1.getHost().equals(url2.getHost()) &&
        url1.getPort() == url2.getPort();
  }



  private static Element[]
  selectOK(Element[] propstats)
  {
    List	result = new ArrayList();

    for (int i = 0; i < propstats.length; ++i)
    {
      if (isPropstatOK(propstats[i]))
      {
        result.add(propstats[i]);
      }
    }

    return (Element[]) result.toArray(new Element[0]);
  }



  public boolean
  useTrashBin(URL url)
  {
    return false;
  }



  public static class ContentTypeDialog extends JDialog

  {

    private String[]		acceptAllMimeType;
    private boolean		accepted = false;
    private boolean[]		cancelAllMimeTypes;
    private boolean		cancelled = false;
    private static JFrame	defaultOwner;
    private JComboBox		encodingField = new JComboBox();
    private String		mimeType = null;
    private JComboBox		mimeTypeField =
      new JComboBox(mimeTypes.toArray(new Object[0]));

    private Action		acceptAllAction =
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          acceptAll();
        }
      };

    private Action		cancelAction =
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          cancel(true);
        }
      };

    private Action		cancelAllAction =
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          cancelAll();
        }
      };

    private Action		okAction =
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          ok();
        }
      };



    private
    ContentTypeDialog
    (
      String	title,
      URL	target,
      String	proposition,
      String[]	acceptAllMimeType,
      boolean[]	cancelAllMimeTypes
    ) throws IOException
    {
      super(defaultOwner, title);
      mimeTypeField.setRenderer(new MimeTypeListCellRenderer());
      loadEncodings();
      this.acceptAllMimeType = acceptAllMimeType;
      this.cancelAllMimeTypes = cancelAllMimeTypes;

      init(target);
      pack();
      setSize(getWidth() + 5, getHeight());

      setLocation
      (
        (int) (getToolkit().getScreenSize().getWidth() / 2) -
          (int) (getSize().getWidth() / 2),
        (int) (getToolkit().getScreenSize().getHeight() / 2) -
          (int) (getSize().getHeight() / 2)
      );

      if (mimeType == null)
      {
        mimeType = proposition;
      }

      if (mimeType != null)
      {
        setMimeType(mimeType);
      }

      addWindowListener
      (
        new WindowAdapter()
        {
          public void
          windowClosing(WindowEvent e)
          {
            if (!cancelled && !accepted)
            {
              cancel(false);
            }
          }
        }
      );
    }



    private void
    acceptAll()
    {
      mimeType = getMimeType();
      acceptAllMimeType[0] = mimeType;
      accepted = true;
      dispose();

      synchronized (ContentTypeDialog.this)
      {
        ContentTypeDialog.this.notifyAll();
      }
    }



    public void
    addNotify()
    {
      super.addNotify();

      ((JFrame) be.re.gui.util.Util.getFrame(this)).setIconImage
      (
        new ImageIcon(be.re.gui.util.Util.class.getResource("res/rezw.png")).
          getImage()
      );
    }



    private void
    cancel(boolean closeWindow)
    {
      mimeType = null;
      cancelled = true;

      if (closeWindow)
      {
        dispose();
      }

      synchronized (ContentTypeDialog.this)
      {
        ContentTypeDialog.this.notifyAll();
      }
    }



    private void
    cancelAll()
    {
      mimeType = null;
      cancelAllMimeTypes[0] = true;
      cancelled = true;
      dispose();

      synchronized (ContentTypeDialog.this)
      {
        ContentTypeDialog.this.notifyAll();
      }
    }



    private boolean
    equals(String mimeType, Object item)
    {
      if (item instanceof String)
      {
        return mimeType.equals(item);
      }

      if (!(item instanceof MimeType.LabeledMimeType))
      {
        return false;
      }

      String[]	mimeTypes = ((MimeType.LabeledMimeType) item).getMimeTypes();

      for (int i = 0; i < mimeTypes.length; ++i)
      {
        if (mimeType.equals(mimeTypes[i]))
        {
          return true;
        }
      }

      return false;
    }



    public static JFrame
    getDefaultOwner()
    {
      return defaultOwner;
    }



    private String
    getMimeType()
    {
      String	encoding = (String) encodingField.getSelectedItem();

      return
        getMimeType(mimeTypeField) +
          (
            encoding == null || encoding.equals("") ?
              "" : ("; charset=\"" + encoding.toLowerCase() + "\"")
          );
    }



    private String
    getMimeType(JComboBox field)
    {
      return
        field.getSelectedItem() instanceof String ?
          (String) field.getSelectedItem() :
          (
            field.getSelectedItem() instanceof MimeType.LabeledMimeType ?
              ((MimeType.LabeledMimeType) field.getSelectedItem()).
                getMimeTypes()[0] :
            null
          );
    }



    private void
    init(URL target)
    {
      JButton 	acceptAllButton = new JButton();
      JPanel 	buttonPanel = new JPanel(new GridBagLayout());
      JButton 	cancelAllButton = new JButton();
      JButton 	cancelButton = new JButton();
      JPanel	mainPanel = new JPanel(new GridBagLayout());
      JButton 	okButton = new JButton();

      mimeTypeField.setPreferredSize
      (
        new Dimension(200, (int) mimeTypeField.getPreferredSize().getHeight())
      );

      mimeTypeField.setEditable(false);

      encodingField.setPreferredSize
      (
        new Dimension(200, (int) encodingField.getPreferredSize().getHeight())
      );

      encodingField.setEnabled(false);
      encodingField.setEditable(false);

      mimeTypeField.addItemListener
      (
        new ItemListener()
        {
          public void
          itemStateChanged(ItemEvent e)
          {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
              encodingField.setEnabled
              (
                "text".
                  equals(MimeType.getMediaType(getMimeType(mimeTypeField))) &&
                  !be.re.xml.Util.isXml(getMimeType(mimeTypeField))
              );
            }
          }
        }
      );

      cancelButton.setActionCommand("cancel");
      cancelButton.setText(be.re.gui.util.Util.getResource("cancel"));
      cancelButton.addActionListener(cancelAction);

      acceptAllButton.setActionCommand("accept_all");
      acceptAllButton.setText(be.re.gui.util.Util.getResource("accept_all"));
      acceptAllButton.addActionListener(acceptAllAction);

      cancelAllButton.setActionCommand("cancel_all");
      cancelAllButton.setText(be.re.gui.util.Util.getResource("cancel_all"));
      cancelAllButton.addActionListener(cancelAllAction);

      okButton.setActionCommand("ok");
      okButton.setText(be.re.gui.util.Util.getResource("ok"));
      okButton.addActionListener(okAction);
      okButton.setDefaultCapable(true);

      getRootPane().setDefaultButton(okButton);
      getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
        put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
      getRootPane().getActionMap().put("cancel", cancelAction);

      getContentPane().add(mainPanel, BorderLayout.CENTER);

      mainPanel.add
      (
        new JLabel(target.getFile()),
        new GridBagConstraints
        (
          0,
          0,
          2,
          1,
          0.0,
          0.0,
          GridBagConstraints.WEST,
          GridBagConstraints.NONE,
          new Insets(5, 15, 5, 15),
          0,
          0
        )
      );

      mainPanel.add
      (
        new JLabel(Util.getResource("dav_mime_type_label") + ": "),
        new GridBagConstraints
        (
          0,
          1,
          1,
          1,
          0.0,
          0.0,
          GridBagConstraints.WEST,
          GridBagConstraints.NONE,
          new Insets(5, 15, 5, 5),
          0,
          0
        )
      );

      mainPanel.add
      (
        mimeTypeField,
        new GridBagConstraints
        (
          1,
          1,
          1,
          1,
          1.0,
          1.0,
          GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL,
          new Insets(5, 0, 5, 15),
          0,
          0
        )
      );

      mainPanel.add
      (
        new JLabel(Util.getResource("dav_encoding_label") + ": "),
        new GridBagConstraints
        (
          0,
          2,
          1,
          1,
          0.0,
          0.0,
          GridBagConstraints.WEST,
          GridBagConstraints.NONE,
          new Insets(5, 15, 5, 5),
          0,
          0
        )
      );

      mainPanel.add
      (
        encodingField,
        new GridBagConstraints
        (
          1,
          2,
          1,
          1,
          1.0,
          1.0,
          GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL,
          new Insets(5, 0, 5, 15),
          0,
          0
        )
      );

      mainPanel.add
      (
        buttonPanel,
        new GridBagConstraints
        (
          0,
          3,
          2,
          1,
          1.0,
          1.0,
          GridBagConstraints.CENTER,
          GridBagConstraints.NONE,
          new Insets(10, 15, 10, 15), // Make it a bit wider for Mac OS X.
          0,
          0
        )
      );

      buttonPanel.add
      (
        okButton,
        new GridBagConstraints
        (
          0,
          0,
          1,
          1,
          1.0,
          1.0,
          GridBagConstraints.EAST,
          GridBagConstraints.NONE,
          new Insets(0, 2, 0, 2),
          0,
          0
        )
      );

      buttonPanel.add
      (
        cancelButton,
        new GridBagConstraints
        (
          1,
          0,
          1,
          1,
          1.0,
          1.0,
          GridBagConstraints.CENTER,
          GridBagConstraints.NONE,
          new Insets(0, 2, 0, 2),
          0,
          0
        )
      );

      buttonPanel.add
      (
        acceptAllButton,
        new GridBagConstraints
        (
          2,
          0,
          1,
          1,
          1.0,
          1.0,
          GridBagConstraints.CENTER,
          GridBagConstraints.NONE,
          new Insets(0, 2, 0, 2),
          0,
          0
        )
      );

      buttonPanel.add
      (
        cancelAllButton,
        new GridBagConstraints
        (
          3,
          0,
          1,
          1,
          1.0,
          1.0,
          GridBagConstraints.WEST,
          GridBagConstraints.NONE,
          new Insets(0, 2, 0, 2),
          0,
          0
        )
      );
    }



    private void
    loadEncodings()
    {
      new Thread
      (
        new Runnable()
        {
          public void
          run()
          {
            Map	charsets = Charset.availableCharsets();

            encodingField.addItem("");

            for (Iterator i = charsets.keySet().iterator(); i.hasNext();)
            {
              encodingField.addItem(i.next());
            }
          }
        }
      ).start();
    }



    private void
    ok()
    {
      mimeType = getMimeType();
      lastChosenMimeType = mimeType;
      accepted = true;
      dispose();

      synchronized (ContentTypeDialog.this)
      {
        ContentTypeDialog.this.notifyAll();
      }
    }



    public static void
    setDefaultOwner(JFrame frame)
    {
      defaultOwner = frame;
    }



    private void
    setMimeType(String mimeType)
    {
      int	i;

      for
      (
        i = 0;
        i < mimeTypeField.getItemCount() &&
          !equals(mimeType, mimeTypeField.getItemAt(i));
        ++i
      );

      if (i < mimeTypeField.getItemCount())
      {
        mimeTypeField.setSelectedIndex(i);
      }
      else
      {
        mimeTypeField.insertItemAt(mimeType, 0);
        mimeTypeField.setSelectedIndex(0);

        synchronized (mimeTypes)
        {
          mimeTypes.add(0, mimeType);
        }
      }
    }

  } // ContentTypeDialog



  public static class Property extends URLManager.Property

  {

    private String	identifierName;
    private Properties	parameters = new Properties();



    private
    Property(String name, Object value, boolean identifier, URL url)
    {
      super
      (
        name,
        identifier ?
          Util.
            unescapeUriSpecials(be.re.net.Util.stripQuery(value.toString())) :
          value,
        identifier,
        url
      );

      if (identifier)
      {
        int	query = value.toString().lastIndexOf('?');

        if (query != -1)
        {
          parameters =
            Util.getParameters(value.toString().substring(query + 1));
        }
      }
    }



    public Properties
    getParameters()
    {
      return parameters;
    }

  } // Property



  private static class PropertyCollector

  {

    private List<Entry>		entries = new ArrayList<Entry>();
    private Set<ExpandedName>	found = new HashSet<ExpandedName>();
    private String[]		principalCollections;
    private Set<ExpandedName>	supported;



    private
    PropertyCollector(ExpandedName[] supported, URL[] principalCollections)
    {
      this.supported = new HashSet<ExpandedName>(Arrays.asList(supported));
      this.principalCollections = new String[principalCollections.length];

      for (int i = 0; i < principalCollections.length; ++i)
      {
        this.principalCollections[i] =
          be.re.net.Util.stripUserInfo(principalCollections[i]).toString();
      }
    }



    private String
    addDecoration(String s, Entry entry)
    {
      boolean	checkedOut =
        !"".equals
        (
          getPropertyValue
          (
            entry,
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "checked-out")
          )
        );
      boolean	locked =
        "true".equals
        (
          getPropertyValue
          (
            entry,
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "lockdiscovery")
          )
        );

      return
        s +
          (
            (checkedOut || locked ? "?" : "") +
              (checkedOut ? "checkedout=true" : "") +
              (checkedOut && locked ? "&" : "") + (locked ? "locked=true" : "")
          );
    }



    private void
    addEntry(URL url, Element response, boolean contents)
      throws MalformedURLException
    {
      Entry	entry = new Entry();
      URL	href = be.re.webdav.Util.getHref(url, response);

      if (href == null)
      {
        return;
      }

      entry.href = href.toString();

      if
      (
        contents					&&
        new URLManager().equal
        (
          Util.stripUserInfo(url),
          Util.stripUserInfo(new URL(entry.href))
        )
      )
      {
        return;
      }

      entries.add(entry);

      Element[]	propstat =
        selectOK
        (
          be.re.xml.Util.selectElements
          (
            response,
            new ExpandedName[]
            {new ExpandedName(be.re.webdav.Constants.DAV_URI, "propstat")}
          )
        );

      for (int i = 0; i < propstat.length; ++i)
      {
        Node	prop =
          be.re.xml.Util.selectFirstChild
          (
            propstat[i],
            be.re.webdav.Constants.DAV_URI,
            "prop"
          );

        if (prop != null)
        {
          Element[]		elements = be.re.xml.Util.selectElements(prop);
          ExpandedName[]	names = new ExpandedName[elements.length];

          for (int j = 0; j < elements.length; ++j)
          {
            names[j] = new ExpandedName(elements[j]);

            if (supported.contains(names[j]))
            {
              entry.properties.put(names[j], getPropertyValue(elements[j]));
            }
          }

          // Second loop because of possible dependencies between properties.

          for (int j = 0; j < names.length; ++j)
          {
            if (!"".equals(getPropertyValue(entry, names[j])))
            {
              found.add(names[j]);
            }
          }
        }
      }

      correctCollection(entry);
    }



    private void
    correctCollection(Entry entry)
    {
      if
      (
        !entry.href.endsWith("/")					     &&
        "collection".equals
        (
          entry.properties.get
          (
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "resourcetype")
          )
        )
      )
      {
        entry.href += "/";

        String	displayName =
          entry.properties.get
          (
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "displayname")
          );

        if (displayName != null && !displayName.endsWith("/"))
        {
          entry.properties.put
          (
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "displayname"),
            displayName + "/"
          );
        }
      }
    }



    private String
    getLockValue(Element property)
    {
      return
        be.re.xml.Util.selectElement
        (
          property,
          new ExpandedName[]
          {
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "activelock"),
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "lockscope"),
            new ExpandedName(be.re.webdav.Constants.DAV_URI, "exclusive")
          }
        ) != null ? "true" : "false";
    }



    private String
    getPrincipalCollection(String owner)
    {
      if (owner == null)
      {
        return null;
      }

      for (int i = 0; i < principalCollections.length; ++i)
      {
        if (owner.startsWith(principalCollections[i]))
        {
          return principalCollections[i];
        }
      }

      return null;
    }



    private URLManager.Property[][]
    getProperties() throws IOException
    {
      URLManager.Property[][]	result =
        new URLManager.Property[entries.size()][];

      if (result.length == 0)
      {
        return result;
      }

      SortedSet	sorted =
        new TreeSet
        (
          new Comparator()
          {
            public int
            compare(Object o1, Object o2)
            {
              return
                getPropertyPosition((ExpandedName) o1) -
                  getPropertyPosition((ExpandedName) o2);
            }
          }
        );

      sorted.addAll
      (
        Arrays.asList
        (
          intersect(ORDERED_PROPERTIES, found.toArray(new ExpandedName[0]))
        )
      );

      for (int i = 0; i < result.length; ++i)
      {
        Iterator	it = sorted.iterator();

        result[i] = new URLManager.Property[sorted.size()];

        for (int j = 0; j < result[i].length; ++j)
        {
          ExpandedName	property = (ExpandedName) it.next();

          result[i][j] =
            new Property
            (
              be.re.webdav.Util.getPropertyDisplayName(property.localName),
              new ExpandedName(be.re.webdav.Constants.DAV_URI, "displayname").
                equals(property) ?
                addDecoration
                (
                  Util.getLastPathSegment(entries.get(i).href),
                  entries.get(i)
                ) : getPropertyValue(entries.get(i), property),
              new ExpandedName(be.re.webdav.Constants.DAV_URI, "displayname").
                equals(property),
              new URL(entries.get(i).href)
            );
        }
      }

      return result;
    }



    private String
    getPropertyValue(Element property)
    {
      if
      (
        new ExpandedName(be.re.webdav.Constants.DAV_URI, "lockdiscovery").
          equals(new ExpandedName(property))
      )
      {
        return getLockValue(property);
      }

      if
      (
        new ExpandedName(be.re.webdav.Constants.DAV_URI, "resourcetype").
          equals(new ExpandedName(property))
      )
      {
        return getResourceType(property);
      }

      Element[]	elements = be.re.xml.Util.selectElements(property);

      return
        elements.length == 1 ?
          (
            elements[0].getFirstChild() == null ?
              elements[0].getLocalName() :
              getText(elements[0].getFirstChild())
          ) :
          (
            elements.length == 0 ?
              getText(property.getFirstChild()).trim() : ""
          );
    }



    private Object
    getPropertyValue(Entry entry, ExpandedName property)
    {
      String	value = entry.properties.get(property);

      if ("checked-in".equals(property.localName))
      {
        if (value == null)
        {
          return "";
        }

        String	versionHistory =
          entry.properties.get
          (
            new ExpandedName
            (
              be.re.webdav.Constants.DAV_URI,
              "version-history"
            )
          );

        if (versionHistory != null && value.startsWith(versionHistory))
        {
          return value.substring(versionHistory.length());
        }

        return "";
      }

      if ("comment".equals(property.localName))
      {
        return
          value == null ?
            "" :
            (value.length() > 17 ? (value.substring(0, 17) + "...") : value);
      }

      if ("auto-version".equals(property.localName))
      {
        return
          value == null ?
            "" : be.re.webdav.Util.getAutoVersionValueDisplayName(value);
      }

      if ("getlastmodified".equals(property.localName))
      {
        return new Date(value == null ? 0 : be.re.net.Util.httpDate(value));
      }

      if ("creationdate".equals(property.localName))
      {
        return
          new Date(value == null ? 0 : be.re.util.Util.parseTimestamp(value));
      }

      if
      (
        "getcontentlength".equals(property.localName)	||
        "quota-used-bytes".equals(property.localName)
      )
      {
        return new Long(value == null || value.equals("") ? "0" : value);
      }

      if ("quota-available-bytes".equals(property.localName))
      {
        return
          value == null || value.equals("") ?
            Util.getResource("dav_unlimited") : new Long(value);
      }

      if ("displayname".equals(property.localName))
      {
        return
          value == null ? "" : (value + (entry.href.endsWith("/") ? "/" : ""));
      }

      if ("getcontenttype".equals(property.localName))
      {
        return
          value == null ?
            "" :
            (
              entry.href.endsWith("/") ?
                Util.getResource("label_folder") :
                (
                  MimeType.getMimeTypeLabel(value) != null ?
                    MimeType.getMimeTypeLabel(value) : value
                )
            );
      }

      if ("owner".equals(property.localName))
      {
        String	collection = getPrincipalCollection(value);

        if (collection != null)
        {
          return value.substring(collection.length());
        }
      }

      return value == null ? "" : value;
    }



    private String
    getResourceType(Element property)
    {
      Element[]	elements = be.re.xml.Util.selectElements(property);

      for (int i = 0; i < elements.length; ++i)
      {
        if
        (
          be.re.webdav.Constants.DAV_URI.
            equals(elements[i].getNamespaceURI())		&&
          "collection".equals(elements[i].getLocalName())
        )
        {
          return "collection";
        }
      }

      return "";
    }



    private URL[]
    getUrls() throws MalformedURLException
    {
      URL[]	result = new URL[entries.size()];

      for (int i = 0; i < result.length; ++i)
      {
        result[i] = be.re.net.Util.escapedUrl(entries.get(i).href);
      }

      return result;
    }



    private static class Entry

    {

      private String			href;
      private Map<ExpandedName,String>	properties =
        new HashMap<ExpandedName,String>();

    } // Entry

  } // PropertyCollector

} // URLManagerDAV
