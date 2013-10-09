package be.re.net;

import be.re.io.StreamConnector;
import be.re.io.URLInputStream;
import be.re.io.URLOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;



/**
 * <p>With this class, common operations such as copying, creating, moving,
 * deleting, etc. can be performed with URLs. The supported schemes are
 * <code>file</code>, <code>ftp</code>, <code>http</code>, <code>https</code>,
 * <code>jar</code>, <code>jms</code> and <code>jndi</code>.</p>
 *
 * <p>In this class URLs are divided in three categories. There are those that
 * refer to a container and those which don't. A container is a collection of
 * resources. It can be a file directory, a WebDAV collection, etc. Containers
 * are further subdivided in pure containers and archives. The latter are file
 * types such as ZIP or JAR files. In some situations they act like a file and
 * in others they are considered to be a container. Note that a container
 * within an archive is a pure container.</p>
 * @author Werner Donn\u00e9
 */

public class URLManager

{

  private Map	handlers = new HashMap();



  /**
   * Calls the full constructor with the parameters set to <code>null</code>.
   */

  public
  URLManager()
  {
    setProtocolHandler("file", new URLManagerFile(this));
    setProtocolHandler("ftp", new URLManagerFTP());
    setProtocolHandler("http", new URLManagerDAV(this));
    setProtocolHandler("https", new URLManagerDAV(this));
    setProtocolHandler("jar", new URLManagerJar(this));
    setProtocolHandler("jms", new URLManagerJMS());
    setProtocolHandler("jndi", new URLManagerJNDI());
  }



  /**
   * Calls the full constructor with the other parameters set to
   * <code>null</code>.
   */

  public
  URLManager(String username, String password)
  {
    this(username, password, null);
  }



  /**
   * @param username the username that will be used for all the operations.
   * @param password the password that will be used for all the operations.
   * @param onBehalfOf sets the HTTP extension header
   * <code>X-be.re.On-Behalf-Of</code> if not equal to <code>null</code>.
   * @see be.re.webdav.Client
   */

  public
  URLManager(String username, String password, String onBehalfOf)
  {
    setProtocolHandler("file", new URLManagerFile(this));
    setProtocolHandler("ftp", new URLManagerFTP(username, password));

    setProtocolHandler
    (
      "http",
      new URLManagerDAV(this, username, password, onBehalfOf)
    );

    setProtocolHandler
    (
      "https",
      new URLManagerDAV(this, username, password, onBehalfOf)
    );

    setProtocolHandler("jar", new URLManagerJar(this));
    setProtocolHandler("jms", new URLManagerJMS());
    setProtocolHandler("jndi", new URLManagerJNDI(username, password));
  }



  /**
   * Copies all resources in <code>source</code> to <code>destination</code>.
   * The latter should be a pure container. If the destinations exist they will
   * be preserved if possible.
   */

  public void
  copy(URL[] source, URL destination) throws IOException, ProtocolException
  {
    copy(source, destination, null, false);
  }



  /**
   * Copies all resources in <code>source</code> to <code>destination</code>.
   * The latter should be a pure container. If <code>overwrite</code> is
   * <code>false</code> and if the destinations exist they will be preserved if
   * possible.
   */

  public void
  copy(URL[] source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    copy(source, destination, null, overwrite);
  }



  /**
   * Copies all resources in <code>source</code> to <code>destination</code>.
   * The latter should be a pure container. When <code>resume</code> is not
   * <code>null</code>, it is called in case of exceptions. If the destinations
   * exist they will be preserved if possible.
   * @see be.re.net.URLManager.Resume
   */

  public void
  copy(URL[] source, URL destination, Resume resume)
    throws IOException, ProtocolException
  {
    copy(source, destination, resume, false);
  }



  /**
   * Copies all resources in <code>source</code> to <code>destination</code>.
   * The latter should be a pure container. When <code>resume</code> is not
   * <code>null</code>, it is called in case of exceptions. If
   * <code>overwrite</code> is <code>false</code> and if the destinations exist
   * they will be preserved if possible.
   * @see be.re.net.URLManager.Resume
   */

  public void
  copy(URL[] source, URL destination, Resume resume, boolean overwrite)
    throws IOException, ProtocolException
  {
    if (source.length > 1 && !isContainer(destination))
    {
      throw
        new ProtocolException(Util.getResource("url_copy_destination_error"));
    }

    if (getProtocolHandler(destination).canCopy(source, destination))
    {
      getProtocolHandler(destination).
        copy(source, destination, overwrite, resume);
    }
    else
    {
      if
      (
        Util.isArchive(destination)					&&
        getProtocolHandler("jar").canCopy
        (
          source,
          new URL("jar:" + destination.toString() + "!/")
        )
      )
      {
        getProtocolHandler("jar").copy
        (
          source,
          new URL("jar:" + destination.toString() + "!/"),
          true,
          resume
        );
      }
      else
      {
        for (int i = 0; i < source.length; ++i)
        {
          if (resume != null)
          {
            try
            {
              copy(source[i], destination, overwrite);
            }

            catch (Throwable e)
            {
              resume.handle(source[i], e);
            }
          }
          else
          {
            copy(source[i], destination, overwrite);
          }
        }
      }
    }
  }



  /**
   * <p>Copies <code>source</code> to <code>destination</code>. If the latter
   * already exists and if the scheme supports it, it will be preserved by
   * renaming it first, for example to the filename with the <q>.bak</q>
   * extension.</p>
   *
   * <p>If <code>destination</code> is a pure container the actual destination
   * will be created by appending the last path segment of <code>source</code>
   * to its path.</p>
   *
   * <p>If <code>source</code> is a pure container than <code>destination</code>
   * must also be one.</p>
   */

  public boolean
  copy(URL source, URL destination) throws IOException, ProtocolException
  {
    return copy(source, destination, false);
  }



  private boolean
  copy(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    if (!overwrite && equal(source, destination))
    {
      if (getProtocolHandler(destination).canPreserve())
      {
        preserve(destination);

        return copy(getPreserveUrl(destination), source, false);
      }

      return false;
    }

    return
      isPureContainer(source) ?
        copyContainer(source, destination, overwrite) :
        copyLeaf(source, destination, overwrite);
  }



  private boolean
  copyContainer(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    if (Util.isAncestor(source, destination))
    {
      throw new ProtocolException(Util.getResource("url_recursive_copy_error"));
    }

    boolean	exists = exists(destination);

    if (exists)
    {
      if (!isPureContainer(destination))
      {
        throw
          new ProtocolException(Util.getResource("url_container_copy_error"));
      }

      destination = new URL(newContainedName(source, destination));

      if (equal(source, destination))
      {
        return copy(source, destination, overwrite);
      }

      exists = exists(destination);

      if (exists && !overwrite)
      {
        preserve(destination);
        exists = false;
      }
    }

    if (!exists)
    {
      create(destination, true);
    }

    boolean	success = true;
    URL[]	urls = getContained(source);

    for (int i = 0; i < urls.length; ++i)
    {
      urls[i] = Util.stripQuery(urls[i]);
      success &=
        copy
        (
          urls[i],
          new URL(newContainedName(urls[i], destination)),
          overwrite
        );
    }

    return success;
  }



  private boolean
  copyLeaf(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    if (!overwrite && exists(destination) && !isPureContainer(destination))
    {
      preserve(destination);
    }

    if (isPureContainer(destination))
    {
      return
        copy
        (
          source,
          new URL(newContainedName(source, destination)),
          overwrite
        );
    }

    if (getProtocolHandler(destination).canCopy(source, destination))
    {
      return
        getProtocolHandler(destination).copy(source, destination, overwrite);
    }

    StreamConnector.copy
    (
      new URLInputStream(source),
      new URLOutputStream(destination)
    );

    return true;
  }



  /**
   * Creates a new resource. If <code>container</code> is set to
   * <code>true</code> a pure container will be created. The result is what is
   * actually created. An implementation may change the URL, for example by
   * adding an extension.
   */

  public URL
  create(URL url, boolean container) throws IOException, ProtocolException
  {
    return getProtocolHandler(url).create(url, container);
  }



  /**
   * Destroys all the resources.
   */

  public void
  destroy(URL[] urls) throws IOException, ProtocolException
  {
    destroy(urls, null);
  }



  /**
   * Destroys all the resources. When <code>resume</code> is not
   * <code>null</code>, it is called in case of exceptions.
   * @see be.re.net.URLManager.Resume
   */

  public void
  destroy(URL[] urls, Resume resume) throws IOException, ProtocolException
  {
    URL[][]	partitions = partition(urls);

    for (int i = 0; i < partitions.length; ++i)
    {
      if
      (
        getProtocolHandler(partitions[i][0]).canDestroy(partitions[i])
      )
      {
        getProtocolHandler(partitions[i][0]).destroy(partitions[i], resume);
      }
      else
      {
        for (int j = 0; j < partitions[i].length; ++j)
        {
          if (resume != null)
          {
            try
            {
              destroy(partitions[i][j]);
            }

            catch (Throwable e)
            {
              resume.handle(partitions[i][j], e);
            }
          }
          else
          {
            destroy(partitions[i][j]);
          }
        }
      }
    }
  }



  /**
   * Destroys the resource. If it is a pure container the destruction will be
   * recursive.
   */

  public void
  destroy(URL url) throws IOException, ProtocolException
  {
    if (getProtocolHandler(url).canDestroy(new URL[] {url}))
    {
      getProtocolHandler(url).destroy(new URL[] {url}, null);
    }
    else
    {
      if (isPureContainer(url))
      {
        URL[]	urls = getContained(url);

        for (int i = 0; i < urls.length; ++i)
        {
          destroy(urls[i]);
        }
      }

      getProtocolHandler(url).destroy(url);
    }
  }



  /**
   * Returns <code>true</code> if the schemes are the same and if the scheme
   * implementation says they are equal.
   */

  public boolean
  equal(URL url1, URL url2)
  {
    return
     url1.getProtocol() != null && url2.getProtocol() != null &&
       url1.getProtocol().equals(url2.getProtocol()) &&
       getProtocolHandler(url1.getProtocol()).equal(url1, url2);
  }



  /**
   * Tests if the resource exists.
   */

  public boolean
  exists(URL url) throws IOException, ProtocolException
  {
    return getProtocolHandler(url).exists(url);
  }



  private static String
  extractLowestName(URL url)
  {
    String	name = Util.getLastPathSegment(url);

    return "/".equals(name) ? "ANONYMOUS/" : name;
  }



  protected Property[][]
  genericContainedProperties(URL url) throws IOException, ProtocolException
  {
    URL[]		contained = getContained(url);
    Property[][]	properties = new Property[contained.length][];

    for (int i = 0; i < contained.length; ++i)
    {
      properties[i] = getProperties(contained[i]);
    }

    return properties;
  }



  /**
   * Returns the resources which are contained by the resource.
   * <code>url</code> should be a container.
   */

  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    URL[]	result = getProtocolHandler(url).getContained(url);
    String	userInfo = url.getUserInfo();

    if (userInfo != null)
    {
      for (int i = 0; i < result.length; ++i)
      {
        result[i] = Util.setUserInfo(result[i], userInfo);
      }
    }

    return result;
  }



  /**
   * Returns the display property sets of the resources which are contained by
   * the resource. <code>url</code> should be a container. The method is used
   * for display purposes.
   */

  public Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    Property[][]	result =
      getProtocolHandler(url).getContainedProperties(url);
    String		userInfo = url.getUserInfo();

    if (userInfo != null)
    {
      for (int i = 0; i < result.length; ++i)
      {
        for (int j = 0; j < result[i].length; ++j)
        {
          result[i][j].url = Util.setUserInfo(result[i][j].url, userInfo);
        }
      }
    }

    return result;
  }



  /**
   * Indicates if the user may be contacted. The default is <code>false</code>.
   */

  public static boolean
  getInteractive()
  {
    return HTTPClient.getInteractive();
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    return getProtocolHandler(url).getParameters(url);
  }



  /**
   * Returns the parent container URL. This method doesn't access the resource.
   */

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



  private static URL
  getPreserveUrl(URL url)
  {
    try
    {
      String	segment = be.re.util.Util.getLastPathSegment(url.getFile());
      int	index = segment.lastIndexOf('.');

      segment =
        index != -1 && be.re.util.Util.isInteger(segment.substring(index + 1)) ?
          (
            segment.substring(0, index + 1) +
              String.valueOf(Integer.parseInt(segment.substring(index + 1)) + 1)
          ) : (segment + ".2");

      return
        new URL
        (
          getParent(url),
          segment + (url.toString().endsWith("/") ? "/" : "")
        );
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e);
    }
  }



  /**
   * Returns the display property set of the resource. The method is used for
   * display purposes.
   */

  public Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    Property[]	result = getProtocolHandler(url).getProperties(url);
    String	userInfo = url.getUserInfo();

    if (userInfo != null)
    {
      for (int i = 0; i < result.length; ++i)
      {
        result[i].url = Util.setUserInfo(result[i].url, userInfo);
      }
    }

    return result;
  }



  ProtocolHandler
  getProtocolHandler(String protocol)
  {
    return (ProtocolHandler) handlers.get(protocol.toLowerCase());
  }



  private ProtocolHandler
  getProtocolHandler(URL url) throws ProtocolException
  {
    URL	proxy = ProxyManager.getProxy(url.getHost(), url.getProtocol());

    ProtocolHandler	handler =
      getProtocolHandler
      (
        proxy != null ? proxy.getProtocol() : url.getProtocol()
      );

    if (handler == null)
    {
      throw
        new ProtocolException
        (
          Util.getResource("url_protocol_error") + " " + url.getProtocol() + "."
        );
    }

    return handler;
  }



  /**
   * Tests if the resource is a container. This method may access the resource
   * or not depending on the scheme implementation.
   */

  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    return getProtocolHandler(url).isContainer(url);
  }



  private static boolean
  isHomogeneous(URL[] urls)
  {
    String	protocol = null;

    for (int i = 0; i < urls.length; ++i)
    {
      if (i > 0)
      {
        if (!urls[i].getProtocol().equals(protocol))
        {
          return false;
        }
      }

      protocol = urls[i].getProtocol();
    }

    return true;
  }



  /**
   * Tests if the resource is a pure container. This method may access the
   * resource or not depending on the scheme implementation.
   */

  public boolean
  isPureContainer(URL url) throws IOException, ProtocolException
  {
    return isContainer(url) && !Util.isArchive(url);
  }



  /**
   * Creates a new link <code>newBinding</code> to the existing
   * <code>url</code>. If the former already exists and if the scheme supports
   * it, it will be preserved by renaming it first, for example to the filename
   * with the <q>.bak</q> extension.</p>
   *
   * <p>If <code>newBinding</code> is a pure container the actual new binding
   * will be created by appending the last path segment of <code>url</code>
   * to its path.</p>
   *
   * <p>If <code>url</code> is a pure container then <code>newBinding</code>
   * must also be one.</p>
   */

  public boolean
  link(URL url, URL newBinding) throws IOException, ProtocolException
  {
    if (equal(url, newBinding))
    {
      return false;
    }

    if (exists(newBinding))
    {
      if (isPureContainer(newBinding))
      {
        newBinding = new URL(newContainedName(url, newBinding));

        if (equal(url, newBinding))
        {
          return false;
        }

        if (exists(newBinding))
        {
          preserve(newBinding);
        }
      }
      else
      {
        preserve(newBinding);
      }
    }

    return getProtocolHandler(url).link(url, newBinding);
  }



  /**
   * Moves all resources in <code>source</code> to <code>destination</code>. The
   * latter should be a pure container.
   */

  public boolean
  move(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return move(source, destination, null);
  }



  /**
   * Moves all resources in <code>source</code> to <code>destination</code>. The
   * latter should be a pure container. When <code>resume</code> is not
   * <code>null</code>, it is called in case of exceptions.
   * @see be.re.net.URLManager.Resume
   */

  public boolean
  move(URL[] source, URL destination, Resume resume)
    throws IOException, ProtocolException
  {
    if (source.length > 1 && !isContainer(destination))
    {
      throw
        new ProtocolException(Util.getResource("url_move_destination_error"));
    }

    if (getProtocolHandler(destination).canMove(source, destination))
    {
      return getProtocolHandler(destination).move(source, destination, resume);
    }

    if
    (
      source.length > 0							&&
      getProtocolHandler(source[0]).canMove(source, destination)
    )
    {
      return getProtocolHandler(source[0]).move(source, destination, resume);
    }

    if
    (
      Util.isArchive(destination)			&&
      getProtocolHandler("jar").canMove
      (
        source,
        new URL("jar:" + destination.toString() + "!/")
      )
    )
    {
      return
        getProtocolHandler("jar").move
        (
          source,
          new URL("jar:" + destination.toString() + "!/"),
          resume
        );
    }

    movePartitions(partition(source), destination, resume);

    return true;
  }



  /**
   * <p>Moves <code>source</code> to <code>destination</code>. If the latter
   * already exists and if the scheme supports it, it will be preserved by
   * renaming it first, for example to the filename with the <q>.bak</q>
   * extension.</p>
   *
   * <p>If <code>destination</code> is a pure container the actual destination
   * will be created by appending the last path segment of <code>source</code>
   * to its path.</p>
   *
   * <p>If <code>source</code> is a pure container then <code>destination</code>
   * must also be one.</p>
   */

  public boolean
  move(URL source, URL destination) throws IOException, ProtocolException
  {
    if (equal(source, destination))
    {
      return false;
    }

    if (Util.isAncestor(source, destination))
    {
      throw new ProtocolException("Recursive move");
    }

    if (getProtocolHandler(destination).canMove(source, destination))
    {
      if (exists(destination))
      {
        if (isPureContainer(destination))
        {
          destination = new URL(newContainedName(source, destination));

          if (equal(source, destination))
          {
            return false;
          }

          if (exists(destination))
          {
            preserve(destination);
          }
        }
        else
        {
          preserve(destination);
        }
      }

      return getProtocolHandler(destination).move(source, destination);
    }

    moveAcross(source, destination);

    return true;
  }



  private void
  moveAcross(URL source, URL destination) throws IOException, ProtocolException
  {
    if (copy(source, destination))
    {
      destroy(source);
    }
  }



  private void
  movePartitions(URL[][] partitions, URL destination, Resume resume)
    throws IOException, ProtocolException
  {
    for (int i = 0; i < partitions.length; ++i)
    {
      if
      (
        getProtocolHandler(destination).canMove(partitions[i], destination)
      )
      {
        getProtocolHandler(destination).
          move(partitions[i], destination, resume);
      }
      else
      {
        for (int j = 0; j < partitions[i].length; ++j)
        {
          if (resume != null)
          {
            try
            {
              move(partitions[i][j], destination);
            }

            catch (Throwable e)
            {
              resume.handle(partitions[i][j], e);
            }
          }
          else
          {
            move(partitions[i][j], destination);
          }
        }
      }
    }
  }



  static String
  newContainedName(URL source, URL destination)
  {
    String	lowest = extractLowestName(source);

    if
    (
      "ftp".equals(source.getProtocol())	&&
      !"ftp".equals(destination.getProtocol())
    )
      // Special case for FTP.
    {
      lowest = FTPClient.removeType(lowest);
    }
    else
    {
      if ("jms".equals(destination.getProtocol()) && lowest.indexOf('=') == -1)
        // Special case for JMS.
      {
        lowest = "Name=" + lowest;
      }
    }

    String      s = destination.toString();

    return s + (s.charAt(s.length() - 1) == '/' ? "" : "/") + lowest;
  }



  private static URL[][]
  partition(URL[] urls)
  {
    Map	partitions = new HashMap();

    for (int i = 0; i < urls.length; ++i)
    {
      try
      {
        String	key =
          Util.isComposedUrl(urls[i]) ?
            Util.extractSubUrl(urls[i]).toString() :
            (
              urls[i].getProtocol() + "://" +
                (urls[i].getAuthority() != null ? urls[i].getAuthority() : "") +
                "/"
            );
        List	partition = (List) partitions.get(key);

        if (partition == null)
        {
          partition = new ArrayList();
          partitions.put(key, partition);
        }

        partition.add(urls[i]);
      }

      catch (MalformedURLException e)
      {
        throw new RuntimeException(e); // Would be a bug.
      }
    }

    for (Iterator i = partitions.keySet().iterator(); i.hasNext();)
    {
      String	key = (String) i.next();
      List	partition = (List) partitions.get(key);

      partitions.put(key, partition.toArray(new URL[partition.size()]));
    }

    return (URL[][]) partitions.values().toArray(new URL[partitions.size()][]);
  }



  /**
   * If the scheme implementation supports it this method will save the
   * resource by moving it to another name, typically with the <q>.2</q>
   * extension.
   */

  public void
  preserve(URL url) throws IOException, ProtocolException
  {
    if (getProtocolHandler(url).canPreserve())
    {
      URL	copy = getPreserveUrl(url);

      if (exists(copy))
      {
        preserve(copy);
      }

      getProtocolHandler(url).move(url, copy);
    }
  }



  /**
   * Indicate if the user may be contacted.
   */

  public static void
  setInteractive(boolean value)
  {
    HTTPClient.setInteractive(value);
  }



  /**
   * Sets a new handler for a scheme.
   */

  public void
  setProtocolHandler(String protocol, ProtocolHandler handler)
  {
    handlers.put(protocol.toLowerCase(), handler);
  }



  /**
   * A client of this class can check with this method if the scheme
   * implementation supports some deletion protection of its own and if not, it
   * can copy the resource to a trash bin prior to the deletion.
   */

  public boolean
  useTrashBin(URL url) throws ProtocolException
  {
    return getProtocolHandler(url).useTrashBin(url);
  }



  /**
   * New URL scheme implementations can be added with this interface.
   * @author Werner Donn\u00e9
   */

  public interface ProtocolHandler

  {

    /**
     * An implementation should return <code>true</code> if it can optimize the
     * copy operation for a set of resources.
     */

    public boolean	canCopy		(URL[] source, URL destination)
					  throws IOException, ProtocolException;

    /**
     * An implementation should return <code>true</code> if it can optimize the
     * copy operation for a resource.
     */

    public boolean	canCopy		(URL source, URL destination)
					  throws IOException, ProtocolException;

    /**
     * An implementation should return <code>true</code> if it can optimize the
     * destroy operation for a set of resources.
     */

    public boolean	canDestroy	(URL[] urls)
					  throws IOException, ProtocolException;

    /**
     * An implementation should return <code>true</code> if it can optimize the
     * move operation for a set of resources.
     */

    public boolean	canMove		(URL[] source, URL destination)
					  throws IOException, ProtocolException;

    /**
     * An implementation should return <code>true</code> if it can optimize the
     * move operation for a resource.
     */

    public boolean	canMove		(URL source, URL destination)
					  throws IOException, ProtocolException;

    /**
     * An implementation should return <code>true</code> if it allows the
     * preservation of a resource when it is about to be overwritten by a copy
     * or move operation.
     */

    public boolean	canPreserve	();

    /**
     * Copy all the resources in <code>sources</code> to
     * <code>destination</code>. If <code>overwrite</code> is set to
     * <code>false</code>, the implementation should try to preserve the
     * <code>destination</code>.
     */

    public void		copy		(
					  URL[]		sources,
					  URL		destination,
					  boolean	overwrite,
					  Resume	resume
					) throws IOException, ProtocolException;

    /**
     * Copy the resource <code>source</code> to <code>destination</code>. If
     * <code>overwrite</code> is set to <code>false</code>, the
     * implementation should try to preserve the <code>destination</code>.
     */

    public boolean	copy		(
					  URL		source,
					  URL		destination,
					  boolean	overwrite
					) throws IOException, ProtocolException;

    /**
     * Create a resource. If <code>container</code> is set to
     * <code>true</code>, the resource should be a pure container. The result
     * is what is actually created. An implementation may change the URL, for
     * example by adding an extension.
     */

    public URL		create		(URL url, boolean container)
					  throws IOException, ProtocolException;

    /**
     * Destroy all resources.
     */

    public void		destroy		(URL[] urls, Resume resume)
					  throws IOException, ProtocolException;

    /**
     * Destroy the resource.
     */

    public void		destroy		(URL url)
					  throws IOException, ProtocolException;

    /**
     * Test if the URLs are equal.
     */

    public boolean	equal		(URL url1, URL url2);

    /**
     * Test is the resource exists.
     */

    public boolean	exists		(URL url)
					  throws IOException, ProtocolException;

    /**
     * Return the resources which are contained by the resource.
     */

    public URL[]	getContained	(URL url)
					  throws IOException, ProtocolException;

    /**
     * Return the display property sets of the resources which are contained by
     * the resource.
     */

    public Property[][]	getContainedProperties	(URL url)
					  throws IOException, ProtocolException;

    /**
     * Give the opportunity to the protocol handler to provide extra
     * information.
     */

    public Properties	getParameters	(URL url)
					  throws IOException, ProtocolException;

    /**
     * Return the display property set of the resource.
     */

    public Property[]	getProperties	(URL url)
					  throws IOException, ProtocolException;

    /**
     * Test if the resource is a pure container.
     */

    public boolean	isContainer	(URL url)
					  throws IOException, ProtocolException;

    /**
     * Creates a new binding <code>newBinding</code> to the existing
     * <code>url</code>.
     */

    public boolean	link		(URL url, URL newBinding)
					  throws IOException, ProtocolException;

    /**
     * Move all the resources in <code>sources</code> to
     * <code>destination</code>.
     */

    public boolean	move		(
					  URL[]		sources,
					  URL		destination,
					  Resume	resume
					) throws IOException, ProtocolException;

    /**
     * Move the resource <code>source</code> to <code>destination</code>.
     */

    public boolean	move		(URL source, URL destination)
					  throws IOException, ProtocolException;

    /**
     * An implementation should return <code>true</code> if it wishes to allow
     * a client of <code>be.re.net.URLManager</code> to use a trash bin
     * mechanism in order to protect resources against destruction.
     */

    public boolean	useTrashBin	(URL url);

  } // ProtocolHandler



  /**
   * This class is for displaying properties about URLs.
   * @author Werner Donn\u00e9
   */

  public static class Property

  {

    /**
     * Mark this property as the identifier of a set of  properties. A display
     * component can use this to organize a list of property sets from URLs.
     */

    public boolean	identifier;

    /**
     * The name of the property.
     */

    public String	name;

    /**
     * The URL this property is a description about.
     */

    public URL		url;

    /**
     * The value of the property. In <code>toString()</code> the value will be
     * transformed into a string.
     */

    public Object	value;



    /**
     * Calls the full constructor with <code>identifier</code> set to
     * <code>false</code>.
     */

    public
    Property(String name, Object value, URL url)
    {
      this(name, value, false, url);
    }



    /**
     * @param name the name of the property.
     * @param value the value of the property. In <code>toString()</code> the
     * value will be transformed into a string.
     * @param identifier mark this property as the identifier of a set of
     * properties. A display component can use this to organize a list of
     * property sets from URLs.
     */

    public
    Property(String name, Object value, boolean identifier, URL url)
    {
      this.name = name;
      this.value = value;
      this.identifier = identifier;
      this.url = url;
    }



    /**
     * Just calls <code>toString()</code>. Subclasses in protocol handlers can
     * use this to add something to the identifying value.
     */

    public String
    getIdentifier()
    {
      return toString();
    }



    /**
     * Just returns <code>s</code>. Subclasses in protocol handlers can use
     * this to combine the parameter with the property value in a way.
     */

    public String
    getIdentifier(String s)
    {
      return s;
    }



    /**
     * Returns the property value as a string.
     */

    public String
    toString()
    {
      String	s = value.toString();

      return
        value == null ?
          "" :
          (
            value instanceof Date ?
              DateFormat.
                getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).
                format((Date) value) :
              (
                identifier && s.endsWith("/") ?
                  s.substring(0, s.length() - 1) : s
              )
          );
    }

  } // Property



  /**
   * An implementation can decide what should happen when an operation on a set
   * of URLs is interrupted by an exception. The exception can be reported
   * without stopping the operation for example.
   * @author Werner Donn\u00e9
   */

  public interface Resume

  {

    public void	handle	(URL url, Throwable e);

  } // Resume

} // URLManager
