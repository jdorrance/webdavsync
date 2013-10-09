package be.re.net;

import be.re.cache.BasicMedium;
import be.re.cache.Cache;
import be.re.cache.LRUCache;
import be.re.io.StreamConnector;
import be.re.io.URLInputStream;
import be.re.util.Array;
import be.re.util.Zip;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;



public class URLManagerJar implements URLManager.ProtocolHandler

{

  private URLManager    	manager;
  private final static Cache	treeCache =
    new LRUCache
    (
      new BasicMedium()
      {
        public boolean
        isDirtyMustRead(Object value)
        {
          return
            ((Tree) value).dirty ||
              ((Tree) value).file.lastModified() != ((Tree) value).lastModified;
        }

        public Object
        read(Object key)
        {
          try
          {
            return buildTree((File) key);
          }

          catch (Exception e)
          {
            throw new RuntimeException(e);
          }
        }
      },
      5
    );



  public
  URLManagerJar(URLManager manager)
  {
    this.manager = manager;
  }



  private static Tree
  buildTree(File file) throws IOException
  {
    ZipFile	zipFile = new ZipFile(file);

    try
    {
      ZipEntry[]	entries = getEntries(zipFile);
      TreeElement	tree = new TreeElement();

      tree.contained = new Hashtable(51);
      tree.isDirectory = true;

      for (int i = 0; i < entries.length; ++i)
      {
        StringTokenizer	tokenizer =
          new StringTokenizer(entries[i].getName(), "/");

        for (Hashtable j = tree.contained; tokenizer.hasMoreTokens();)
        {
          String	segment = tokenizer.nextToken();

          TreeElement	element = (TreeElement) j.get(segment);

          if (element == null)
          {
            element = new TreeElement();
            element.contained = new Hashtable(51);
            element.isDirectory = entries[i].isDirectory();
            j.put(segment, element);
          }

          j = element.contained;
        }
      }

      Tree	result = new Tree(file, false);

      result.root = tree;

      return result;
    }

    finally
    {
      zipFile.close();
    }
  }



  public boolean
  canCopy(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return
      "jar".equals(destination.getProtocol()) &&
        (
          manager.isPureContainer(destination) ||
            (source.length == 1 && !manager.isPureContainer(source[0]))
        );
  }



  public boolean
  canCopy(URL source, URL destination) throws IOException, ProtocolException
  {
    return
      "jar".equals(destination.getProtocol()) &&
        (
          manager.isPureContainer(destination) ||
            !manager.isPureContainer(source)
        );
  }



  public boolean
  canDestroy(URL[] urls) throws IOException, ProtocolException
  {
    return inSameArchive(urls);
  }



  public boolean
  canMove(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return
      canCopy(source, destination) ||
        (!"jar".equals(destination.getProtocol()) && inSameArchive(source));
  }



  public boolean
  canMove(URL source, URL destination) throws IOException, ProtocolException
  {
    return
      destination.getProtocol().equals(source.getProtocol()) &&
        Util.extractSubUrl(source).equals(Util.extractSubUrl(destination));
  }



  public boolean
  canPreserve()
  {
    return false;
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
    copyOrMove(source, destination, false);
  }



  public boolean
  copy(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    copyOrMove(new URL[]{source}, destination, false);

    return true; // Errors will throw exceptions.
  }



  private void
  copyOrMove(URL[] source, URL destination, boolean canMove)
    throws IOException, ProtocolException
  {
    String	destinationEntry = Util.getJarEntry(destination);
    List	entries = new ArrayList();
    List	expandedUrls = new ArrayList();
    URL[][]	urls = expandUrls(source);

    for (int i = 0; i < urls.length; ++i)
    {
      String	s =
        "ftp".equals(source[i].getProtocol()) ?
          FTPClient.removeType(source[i].toString()) : source[i].toString();
      String	segment = Util.getLastPathSegment(s);

      for (int j = 0; j < urls[i].length; ++j)
      {
        if (!manager.isPureContainer(urls[i][j]))
        {
          String	remainder =
            "ftp".equals(urls[i][j].getProtocol()) ?
              FTPClient.removeType(urls[i][j].toString()).
                substring(s.length()) :
              urls[i][j].toString().substring(s.length());
          String	entry =
            destinationEntry +
              (
                manager.isContainer(destination) ?
                  Util.unescapeUriSpecials
                  (
                    remainder.equals("") ?
                      segment :
                      (
                        segment +
                          (
                            segment.charAt(segment.length() - 1) == '/' ?
                              "" : "/"
                          ) + remainder
                      )
                  ) : ""
              );

          entries.add(entry);
          expandedUrls.add(urls[i][j]);
        }
      }
    }

    Zip.put
    (
      Util.urlToFile(Util.extractSubUrl(destination)),
      (String[]) entries.toArray(new String[entries.size()]),
      (URL[]) expandedUrls.toArray(new URL[expandedUrls.size()]),
      canMove
    );

    if (canMove) // Zip.put gets only the non-containers.
    {
      manager.destroy
      (
        selectNotInArchive
        (
          source,
          new URL("jar:" + Util.extractSubUrl(destination) + "!/")
        )
      );

      manager.destroy(selectContainers(source));
        // Only those in the archive will be left.
    }

    markDirty(destination);
  }



  public URL
  create(URL url, boolean container) throws IOException, ProtocolException
  {
    Zip.put
    (
      Util.urlToFile(Util.extractSubUrl(url)),
      new String[] {Util.getJarEntry(url)},
      new URL[] {null},
      false
    );

    markDirty(url);

    return url;
  }



  public void
  destroy(URL[] urls, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    if (urls.length == 0)
    {
      return;
    }

    List	entries = new ArrayList();
    URL[][]	expandedUrls = expandUrls(urls);

    for (int i = 0; i < expandedUrls.length; ++i)
    {
      for (int j = 0; j < expandedUrls[i].length; ++j)
      {
        entries.add(Util.getJarEntry(expandedUrls[i][j]));
      }
    }

    Zip.delete
    (
      Util.urlToFile(Util.extractSubUrl(urls[0])),
      (String[]) entries.toArray(new String[entries.size()])
    );

    markDirty(urls[0]);
  }



  public void
  destroy(URL url) throws IOException, ProtocolException
  {
    if (!Util.getJarEntry(url).equals(""))
    {
      destroy(new URL[] {url}, null);
    }
    else
    {
      manager.destroy(Util.extractSubUrl(url));
    }
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
    String	filename = Util.getJarEntry(url);
    ZipFile	zipFile = getZipFile(url);

    try
    {
      return
        filename.equals("") ?
          manager.exists(Util.extractSubUrl(url)) :
          (
            zipFile.getEntry(filename) != null ||
              Array.indexOf(getContained(manager.getParent(url)), url) != -1
          );
    }

    finally
    {
      zipFile.close();
    }
  }



  private void
  expandUrl(URL url, List expanded) throws IOException, ProtocolException
  {
    if (manager.isPureContainer(url))
    {
      URL[]	contained = manager.getContained(url);

      for (int i = 0; i < contained.length; ++i)
      {
        expandUrl(contained[i], expanded);
      }
    }

    expanded.add(url);
  }



  private URL[][]
  expandUrls(URL[] urls) throws IOException, ProtocolException
  {
    List	result = new ArrayList();

    for (int i = 0; i < urls.length; ++i)
    {
      List	expanded = new ArrayList();

      expandUrl(urls[i], expanded);
      result.add((URL[]) expanded.toArray(new URL[expanded.size()]));
    }

    return (URL[][]) result.toArray(new URL[result.size()][]);
  }



  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    TreeElement	element =
      getContained(getTree(url).root, Util.getJarEntry(url));

    if (element == null)
    {
      return new URL[0];
    }

    String	s = url.toString();

    if (!s.endsWith("/"))
    {
      s += "/";
    }

    Enumeration	enumer = element.contained.keys();
    List	result = new ArrayList();

    try
    {
      while (enumer.hasMoreElements())
      {
        String		key = (String) enumer.nextElement();
        TreeElement	subcontained = (TreeElement) element.contained.get(key);

        result.add
        (
          Util.escapedUrl
          (
            s + key +
              (
                (subcontained != null && subcontained.contained.size() > 0) ||
                  subcontained.isDirectory ?
                  "/" : ""
              )
          )
        );
      }
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }

    return (URL[]) result.toArray(new URL[result.size()]);
  }



  private static TreeElement
  getContained(TreeElement tree, String name)
  {
    TreeElement		result;
    StringTokenizer	tokenizer = new StringTokenizer(name, "/");

    for
    (
      result = tree;
      tokenizer.hasMoreTokens();
      result = (TreeElement) result.contained.get(tokenizer.nextToken())
    );

    return result;
  }



  public URLManager.Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    return manager.genericContainedProperties(url);
  }



  private static ZipEntry[]
  getEntries(ZipFile file) throws IOException
  {
    List	entries = new ArrayList();
    Enumeration	enumer = file.entries();

    while (enumer.hasMoreElements())
    {
      entries.add(enumer.nextElement());
    }

    return (ZipEntry[]) entries.toArray(new ZipEntry[entries.size()]);
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    return new Properties();
  }



  /**
   * Returns the same properties as for the file scheme.
   */

  public URLManager.Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    String	filename = Util.getJarEntry(url);

    if (filename.equals(""))
    {
      return manager.getProperties(Util.extractSubUrl(url));
    }

    ZipFile	zipFile = getZipFile(url);

    try
    {
      ZipEntry	entry = zipFile.getEntry(filename);
      List	properties = new ArrayList();

      properties.add
      (
        new URLManager.Property("name", Util.getLastPathSegment(url), true, url)
      );

      properties.add(new URLManager.Property("access", "RW", url));

      properties.add
      (
        new URLManager.Property
        (
          "modified",
          entry == null || entry.getTime() == -1 ?
            null : new Date(entry.getTime()),
          url
        )
      );

      properties.add
      (
        new URLManager.Property
        (
          "size",
          entry == null ? null : new Long(entry.getSize()),
          url
        )
      );

      properties.add
      (
        new URLManager.Property
        (
          "compressed_size",
          entry == null ? null : new Long(entry.getCompressedSize()),
          url
        )
      );

      properties.add
      (
        new URLManager.Property
        (
          "compression",
          entry == null ?
            null :
            (
              entry.getMethod() == ZipEntry.DEFLATED ?
                "DEFLATED" :
                (entry.getMethod() == ZipEntry.STORED ? "STORED" : "UNKNOWN")
            ),
          url
        )
      );

      properties.add
      (
        new URLManager.Property
        (
          "comment",
          entry == null ? null : entry.getComment(),
          url
        )
      );

      return
        (URLManager.Property[])
          properties.toArray(new URLManager.Property[properties.size()]);
    }

    finally
    {
      zipFile.close();
    }
  }



  private static Tree
  getTree(URL url) throws IOException
  {
    return (Tree) treeCache.get(Util.urlToFile(Util.extractSubUrl(url)));
  }



  private ZipFile
  getZipFile(URL url) throws IOException
  {
    return new ZipFile(Util.urlToFile(Util.extractSubUrl(url)));
  }



  private boolean
  inSameArchive(URL[] source) throws MalformedURLException
  {
    if (source.length == 0 || !"jar".equals(source[0].getProtocol()))
    {
      return false;
    }

    for (int i = 1; i < source.length; ++i)
    {
      if
      (
        !source[i].getProtocol().equals(source[0].getProtocol())	||
        manager.equal
        (
          Util.extractSubUrl(source[i]),
          Util.extractSubUrl(source[0])
        )
      )
      {
        return false;
      }
    }

    return true;
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    return url.toString().endsWith("/");
  }



  public boolean
  link(URL url, URL newBinding) throws IOException, ProtocolException
  {
    return false;
  }



  private static void
  markDirty(URL url)
  {
    try
    {
      treeCache.put
      (
        Util.urlToFile(Util.extractSubUrl(url)),
        new Tree(Util.urlToFile(Util.extractSubUrl(url)), true)
      );
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  public boolean
  move(URL[] source, URL destination, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    if ("jar".equals(destination.getProtocol()))
    {
      copyOrMove(source, destination, true);
    }
    else
    {
      manager.copy(source, destination, resume);
      manager.destroy(source, resume);
    }

    return true;
  }



  public boolean
  move(URL source, URL destination) throws IOException, ProtocolException
  {
    Zip.rename
    (
      Util.urlToFile(Util.extractSubUrl(source)),
      Util.getJarEntry(source),
      Util.getJarEntry(destination)
    );

    markDirty(source);

    return true;
  }



  private URL[]
  selectContainers(URL[] urls) throws IOException
  {
    List	result = new ArrayList();

    for (int i = 0; i < urls.length; ++i)
    {
      if (manager.isPureContainer(urls[i]))
      {
        result.add(urls[i]);
      }
    }

    return (URL[]) result.toArray(new URL[result.size()]);
  }



  private URL[]
  selectNotInArchive(URL[] urls, URL archive)
  {
    List	result = new ArrayList();

    for (int i = 0; i < urls.length; ++i)
    {
      if (!be.re.net.Util.isAncestor(archive, urls[i]))
      {
        result.add(urls[i]);
      }
    }

    return (URL[]) result.toArray(new URL[result.size()]);
  }



  public boolean
  useTrashBin(URL url)
  {
    return true;
  }



  private static class Tree

  {

    private File	file;
    private boolean	dirty;
    private long	lastModified;
    private TreeElement	root;



    private
    Tree(File file, boolean dirty)
    {
      this.file = file;
      this.dirty = dirty;
      this.lastModified = file.lastModified();
    }

  } // Tree



  private static class TreeElement

  {

    private boolean	isDirectory;
    private Hashtable	contained;

  } // TreeElement

} // URLManagerJar
