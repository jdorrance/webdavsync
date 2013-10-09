package be.re.webdav.cmd;

import be.re.gui.util.ConfirmationDialog;
import be.re.gui.util.InteractiveWriter;
import be.re.gui.util.YesOrNo;
import be.re.io.ReaderWriterConnector;
import be.re.io.StreamConnector;
import be.re.net.HTTPClient;
import be.re.net.Headers;
import be.re.net.ProtocolException;
import be.re.net.URLManager;
import be.re.util.Array;
import be.re.util.Diff;
import be.re.util.MimeType;
import be.re.webdav.Client;
import be.re.xml.ExpandedName;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 * This class can synchronize a local directory with a WebDAV collection, which
 * is useful in build scripts where the WebDAV server can't be mounted as a
 * file system.
 *
 * Synchronisation can be done from the server to a local directory, the other
 * way around or both ways.
 *
 * File or server resource moves are not detected and WebDAV bindings are not
 * supported.
 * @author Werner Donn\u00e9
 */

public class Sync

{

  public static enum ConflictType	{UPDATE, DELETE_LOCAL, DELETE_REMOTE};
  public static enum Direction		{BIDIRECTIONAL, DOWN, UP};

  private static final Pattern	VERSIONED_FILENAME =
    Pattern.compile(".*\\.\\d+");

  private Client	client = new Client();
  private boolean	daemonMode;
  private Node		defaultAcl;
  private File		database;
  private Direction	direction;
  private Pattern[]	exclusionPatterns = new Pattern[0];
  private String[]	exclusions = new String[0];
  private Filter	filter;
  private String	hiddenFolderName;
  private boolean	interactive;
  private Set<Listener>	listeners = new HashSet<Listener>();
  private File		local;
  private boolean	noRecursion;
  private boolean	recursive;
  private int		renameDepth = 10;
  private boolean	renameFiles;
  private URL		remote;
  private File		trash;



  public
  Sync()
  {
    this(getDatabase());
  }



  public
  Sync(File database)
  {
    this.database = database;

    if (!database.exists())
    {
      try
      {
        createDatabase(database);
      }

      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }



  public void
  addListener(Listener listener)
  {
    listeners.add(listener);
  }



  private boolean
  addToTree(Record tree, Record record)
  {
    if (record.path == null || record.lastModified == -1)
    {
      return false;
    }

    String	path = normalize(record.path);

    if (shouldExcludePath("/" + path))
    {
      return false;
    }

    Record	current = tree;
    String[]	segments = be.re.util.Util.getPathSegments(path);

    for (int i = 0; i < segments.length; ++i)
    {
      Record	entry = (Record) current.members.get(segments[i]);

      if (entry == null)
      {
        entry = new Record();
        current.members.put(segments[i], entry);
      }

      entry.parent = current;

      if (i < segments.length - 1)
      {
        current = entry;
      }
      else
      {
        entry.path = record.path;
        entry.contentType = record.contentType;
        entry.etag = record.etag;
        entry.lastModified = record.lastModified;
      }
    }

    return true;
  }



  private boolean
  checkConflicts(Record record, File member, Context context)
    throws IOException, SQLException
  {
    URL		url = getUrl(context.remote, record.path);
    boolean	result =
      context.direction == Direction.DOWN && member.isDirectory() ?
        deleteConflict(member) :
        (
          context.direction == Direction.UP && record.path.endsWith("/") ?
            deleteConflict(url) :
            (
              hasChanged(member, context) && hasChanged(record, context) ?
                (
                  context.direction == Direction.DOWN &&
                    hasChanged(member, context) ?
                    updateConflict(member) :
                    (
                      context.direction == Direction.UP &&
                        hasChanged(record, context) ?
                        updateConflict(url) : updateConflict(member, url)
                    )
                ) :
                (
                  context.lastSynchronization == -1 ? // First synchronization.
                    (
                      context.direction == Direction.DOWN &&
                        member.lastModified() > record.lastModified ?
                        updateConflict(member) :
                        (
                          context.direction == Direction.UP &&
                            record.lastModified > member.lastModified() ?
                            updateConflict(url) : true
                        )
                    ) : true
                )
            )
        );

    if (!result)
    {
      context.conflicts.add
      (
        new Conflict
        (
          context.remote,
          context.local,
          member,
          url,
          ConflictType.UPDATE
        )
      );
    }

    return result;
  }



  private boolean
  checkDirectoryConflict(Record record, File member, Context context)
    throws IOException
  {
    URL		url = getUrl(context.remote, record.path);
    boolean	result =
      context.direction == Direction.BIDIRECTIONAL &&
        (
          (record.path.endsWith("/") && !member.isDirectory()) ||
            (!record.path.endsWith("/") && member.isDirectory())
        ) ? updateConflict(member, url) : true;

    if (!result)
    {
      context.conflicts.add
      (
        new Conflict
        (
          context.remote,
          context.local,
          member,
          url,
          ConflictType.UPDATE
        )
      );
    }

    return result;
  }



  private void
  checkin(Context context)
  {
    if (context.checkedOut != null)
    {
      for (String url: context.checkedOut)
      {
        try
        {
          client.checkin(new URL(url), false, false);
        }

        catch (Exception e)
        {
          // Only an attempt.
        }
      }
    }
  }



  private void
  checkout(URL url, Context context)
  {
    if
    (
      context.checkedOut != null			&&
      !context.checkedOut.contains(url.toString())
    )
    {
      try
      {
        context.checkedOut.add(url.toString());
        client.checkout(url, null, false);
      }

      catch (Exception e)
      {
        // Only an attempt.
      }
    }
  }



  private static void
  cleanUpDirectories(File directory) throws IOException
  {
    if
    (
      be.re.io.Util.
        isDescendant(be.re.util.Util.getPackageStorage(Sync.class), directory)
    )
    {
      String[]	list = directory.list();

      if (list != null && list.length == 0)
      {
        directory.delete();
        cleanUpDirectories(directory.getParentFile());
      }
    }
  }



  private static void
  copyLastSynchronization(File file, Context context)
    throws IOException, SQLException
  {
    if (file.exists())
    {
      setSynchronization(file.lastModified(), context);
    }
  }



  private static void
  copyLocalFiles(File file, Context context) throws IOException, SQLException
  {
    if (file.exists())
    {
      String		line;
      BufferedReader	reader =
        new BufferedReader
        (
          new InputStreamReader(new FileInputStream(file), "UTF-8")
        );

      try
      {
        while ((line = reader.readLine()) != null)
        {
          String[]	values = splitLine(line);

          if (values.length > 1 && be.re.util.Util.isLong(values[1]))
          {
            writeFilename
            (
              new File(values[0]),
              Long.parseLong(values[1]),
              context
            );
          }
        }
      }

      finally
      {
        reader.close();
      }
    }
  }



  private static void
  copyRemoteFiles(File file, Context context) throws IOException, SQLException
  {
    if (file.exists())
    {
      String		line;
      BufferedReader	reader =
        new BufferedReader
        (
          new InputStreamReader(new FileInputStream(file), "UTF-8")
        );

      try
      {
        while ((line = reader.readLine()) != null)
        {
          String[]	values = splitLine(line);

          if (values.length >= 2 && be.re.util.Util.isLong(values[1]))
          {
            writeRemotePath
            (
              values[0],
              Long.parseLong(values[1]),
              values.length > 2 ? values[2] : null,
              context
            );
          }
        }
      }

      finally
      {
        reader.close();
      }
    }
  }



  private static void
  createDatabase(File database) throws IOException, SQLException
  {
    Connection		connection = getConnection(database, true);
    PreparedStatement	statement = null;

    try
    {
      StringWriter	out = new StringWriter();

      ReaderWriterConnector.copy
      (
        new InputStreamReader(Sync.class.getResourceAsStream("res/syncdb.sql")),
        out
      );

      String[]	statements = be.re.util.Util.split(out.toString(), ";");

      for (int i = 0; i < statements.length; ++i)
      {
        if (statements[i].trim().length() > 0)
        {
          statement = connection.prepareStatement(statements[i]);
          statement.execute();
          release(statement);
        }
      }
    }

    finally
    {
      connection.close();
    }
  }



  private static String
  createLocalPath(File file, File directory)
  {
    return
      (
        file.getParentFile() != null &&
          !file.getParentFile().equals(directory) ?
          createLocalPath(file.getParentFile(), directory) : ""
      ) + file.getName() + (file.isDirectory() ? "/" : "");
  }



  private static String
  createPath(URL descendant, URL url)
  {
    return
      descendant.getFile().startsWith(url.getFile()) ?
        be.re.net.Util.unescapeUriSpecials
        (
          descendant.getFile().substring(url.getFile().length())
        ) : null;
  }



  /**
   * An HTTP-date is only precise up to the second.
   */

  private static long
  cutMillis(long httpDate)
  {
    return ((long) (httpDate / 1000)) * 1000;
  }



  private boolean
  delete(File member, Context context) throws IOException, SQLException
  {
    if
    (
      !member.isDirectory()		&&
      hasChanged(member, context)	&&
      !deleteConflict(member)
    )
    {
      context.conflicts.add
      (
        new Conflict
        (
          context.remote,
          context.local,
          member,
          getUrl(context.remote, context.local, member),
          ConflictType.DELETE_LOCAL
        )
      );

      return false;
    }

    for (Listener listener: listeners)
    {
      listener.startDelete(member);
    }

    boolean	result;

    if (trash != null)
    {
      result = moveToTrash(member, context.local, trash, renameDepth);
    }
    else
    {
      if (hiddenFolderName != null)
      {
        result = moveToHiddenFolder(member, hiddenFolderName, renameDepth);
      }
      else
      {
        delete(member, interactive, true);
        result = true;
      }
    }

    deleteFilename(member, context);
    deleteRemotePath(getPath(context.local, member), context);

    for (Listener listener: listeners)
    {
      listener.endDelete(member);
    }

    return result;
  }



  private static void
  delete(File file, boolean interactive, boolean recursive) throws IOException
  {
    if (interactive)
    {
      URL	trashBin = getTrashBin(file);

      if (trashBin == null)
      {
        File	bin =
          new File(new File(System.getProperty("user.home")), ".Trash");

        if (!bin.exists())
        {
          if (!bin.mkdir())
          {
            throw new RuntimeException("No trash bin.");
          }

          System.err.
            println("Trash bin " + bin.getAbsolutePath() + " created.");
        }

        trashBin = be.re.net.Util.fileToUrl(bin);
      }

      new URLManager().move(be.re.net.Util.fileToUrl(file), trashBin);
    }
    else
    {
      if (file.isDirectory() && recursive)
      {
        be.re.io.Util.deleteDir(file);
      }
      else
      {
        file.delete();
      }
    }
  }



  private boolean
  delete(Record record, Context context) throws IOException, SQLException
  {
    URL	url = getUrl(context.remote, record.path);

    if
    (
      !record.path.endsWith("/")	&&
      hasChanged(record, context)	&&
      !deleteConflict(url)
    )
    {
      context.conflicts.add
      (
        new Conflict
        (
          context.remote,
          context.local,
          getFile(context.local, record.path),
          url,
          ConflictType.DELETE_REMOTE
        )
      );

      return false;
    }

    for (Listener listener: listeners)
    {
      listener.startDelete(url);
    }

    try
    {
      checkout(URLManager.getParent(url), context);

      if (hiddenFolderName != null)
      {
        moveToHiddenFolder(url, hiddenFolderName, renameDepth);
      }
      else
      {
        client.delete(url);
      }

      deleteFilename(getFile(context.local, record.path), context);
      deleteRemotePath(record.path, context);

      return true;
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.DELETE, e);
      }

      return false;
    }

    finally
    {
      for (Listener listener: listeners)
      {
        listener.endDelete(url);
      }
    }
  }



  private static void
  deleteAll(String table, Context context) throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "delete from " + table + " where local = ? and remote = ?"
        );

      statement.setString(1, context.local.getAbsolutePath());
      statement.setString(2, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  private static void
  deleteAllFiles(Context context) throws SQLException
  {
    deleteAll("file", context);
  }



  private static void
  deleteAllRemotePaths(Context context) throws SQLException
  {
    deleteAll("remote_path", context);
  }



  private boolean
  deleteConflict(File file)
  {
    boolean	objection = false;

    for (Listener listener: listeners)
    {
      objection |= !listener.deleteConflict(file);
    }

    return !objection;
  }



  private boolean
  deleteConflict(URL url)
  {
    boolean	objection = false;

    for (Listener listener: listeners)
    {
      objection |= !listener.deleteConflict(url);
    }

    return !objection;
  }



  public static void
  deleteFilename(URL remote, File local, File file, File database)
    throws IOException
  {
    Context	context = null;

    try
    {
      context = new Context(local, remote, database);
      deleteFilename(file, context);
      setSynchronization(context);
    }

    catch (SQLException e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      release(context);
    }
  }



  private static void
  deleteFilename(File file, Context context) throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "delete from file where filename = ? and local = ? and " +
            "remote = ?"
        );

      statement.setString(1, file.getAbsolutePath());
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  private static void
  deleteRemotePath(String path, Context context) throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "delete from remote_path where path = ? and local = ? and " +
            "remote = ?"
        );

      statement.setString(1, path);
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  private static void
  deleteSynchronization(Context context) throws SQLException
  {
    deleteAll("synchronization", context);
  }



  public static void
  deleteUrl(URL remote, File local, URL url, File database)
    throws IOException
  {
    Context	context = null;

    try
    {
      context = new Context(local, remote, database);
      deleteRemotePath(getPath(url, remote), context);
      setSynchronization(context);
    }

    catch (SQLException e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      release(context);
    }
  }



  private static void
  endBusy()
  {
    try
    {
      Class.forName("be.re.webdav.cmd.Uivi").getMethod("endBusy").
        invoke(null, new Object[0]);
    }

    catch (Exception e)
    {
    }
  }



  private boolean
  exchange(Record record, File member, Context context)
    throws IOException, SQLException
  {
    URL	url = getUrl(context.remote, record.path);

    if
    (
      (
        context.direction == Direction.DOWN			&&
        member.isDirectory()
      )								||
      (
        (
          context.lastSynchronization == -1			||
          hasChanged(record, context)
        )							&&
        (
          context.direction == Direction.DOWN			||
          context.direction == Direction.BIDIRECTIONAL
        )
      )
    )
    {
      boolean	result = get(url, member);

      if (result)
      {
        member.setLastModified(record.lastModified);
      }

      writeFilename(member, context);
      writeRemotePath(record, context);

      return result;
    }

    if
    (
      (
        context.direction == Direction.UP			&&
        record.path.endsWith("/")
      )								||
      (
        (
          context.lastSynchronization == -1			||
          hasChanged(member, context)
        )							&&
        (
          context.direction == Direction.UP			||
          context.direction == Direction.BIDIRECTIONAL
        )
      )
    )
    {
      long[]	time = new long[1];
      boolean	result = put(url, member, record.contentType, time, context);

      record.lastModified =
        Math.max
        (
          cutMillis(time[0]),
          cutMillis(be.re.net.HTTPClient.Util.getLastModified(url))
        ) + 2000;
        // Add a margin in case the update on the server is not visible
        // immediately.

      writeFilename(member, context);
      writeRemotePath(record, context);

      return result;
    }

    return false;
  }



  private boolean
  get(URL url, File member) throws IOException
  {
    boolean	create = false;

    if (member.isDirectory())
    {
      create = true;

      for (Listener listener: listeners)
      {
        listener.startDelete(member);
      }

      if (hiddenFolderName != null)
      {
        moveToHiddenFolder(member, hiddenFolderName, renameDepth);
      }
      else
      {
        delete(member, interactive, true);
      }

      for (Listener listener: listeners)
      {
        listener.endDelete(member);
      }
    }

    Client.Response	response = null;

    for (Listener listener: listeners)
    {
      if (create)
      {
        listener.startCreate(member);
      }
      else
      {
        listener.startUpdate(member);
      }
    }

    try
    {
      if (renameFiles && renameDepth > 0 && member.exists())
      {
        moveToHigherVersion(member, renameDepth);
      }

      response = client.get(url, new String[0], new String[0], null);

      if (response.getStatusCode() != 200)
      {
        for (Listener listener: listeners)
        {
          listener.error(response);
        }

        return false;
      }

      StreamConnector.copy(response.getBody(), new FileOutputStream(member));

      return true;
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.GET, e);
      }

      return false;
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }

      for (Listener listener: listeners)
      {
        if (create)
        {
          listener.endCreate(member);
        }
        else
        {
          listener.endUpdate(member);
        }
      }
    }
  }



  private Node
  getAcl(URL url) throws IOException
  {
    Client.Response	response = null;

    try
    {
      response =
        client.propfindSpecific
        (
          url,
          new ExpandedName[]{new ExpandedName(Constants.DAV_URI, "acl")},
          "0"
        );

      return
        be.re.xml.Util.selectElement
        (
          response.createDocument().getDocumentElement(),
          new ExpandedName[]
          {
            new ExpandedName(Constants.DAV_URI, "response"),
            new ExpandedName(Constants.DAV_URI, "propstat"),
            new ExpandedName(Constants.DAV_URI, "prop"),
            new ExpandedName(Constants.DAV_URI, "acl")
          }
        );
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.PROPFIND, e);
      }

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



  private static Connection
  getConnection(File database) throws IOException, SQLException
  {
    return getConnection(database, false);
  }



  private static Connection
  getConnection(File database, boolean create) throws IOException, SQLException
  {
    if (!database.exists() && !create)
    {
      createDatabase(database);
    }

    try
    {
      return
        DriverManager.getConnection
        (
          "jdbc:derby:" + database.getAbsolutePath() +
            (create ? ";create=true" : "")
        );
    }

    catch (SQLException e)
    {
      //System.exit(0); // Locked by another instance of DAVbox.
    }

    return null;
  }



  private static JFrame
  getCurrentFrame()
  {
    try
    {
      return
        (JFrame)
          Class.forName("be.re.app.uivi.Service").
            getMethod("getCurrentFrame", new Class[0]).
            invoke(null, new Object[0]);
    }

    catch (Exception e)
    {
      return null;
    }
  }



  /**
   * The default is <code>false</code>.
   */

  public boolean
  getDaemonMode()
  {
    return daemonMode;
  }



  private static File
  getDatabase()
  {
    return new File(be.re.util.Util.getPackageStorage(Sync.class), "syncdb");
  }



  public Node
  getDefaultAcl()
  {
    return defaultAcl;
  }



  public Direction
  getDirection()
  {
    return direction;
  }



  private static File
  getDirectory()
  {
    JFileChooser	chooser = new JFileChooser();

    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setMultiSelectionEnabled(false);

    return
      chooser.showOpenDialog(getCurrentFrame()) != JFileChooser.APPROVE_OPTION ?
        null : chooser.getSelectedFile();
  }



  private static String
  getETagFromRemotePath(String path, Context context) throws SQLException
  {
    PreparedStatement	statement = null;
    ResultSet		resultSet = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "select etag from remote_path " +
            "where path = ? and local = ? and remote = ? and etag != 'none'"
        );

      statement.setString(1, path);
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      resultSet = statement.executeQuery();

      return resultSet.next() ? resultSet.getString("etag") : null;
    }

    finally
    {
      release(resultSet);
      release(statement);
    }
  }



  public String[]
  getExclusions()
  {
    return exclusions;
  }



  public static File
  getFile(URL remote, File local, URL url) throws MalformedURLException
  {
    return getFile(local, getPath(url, remote));
  }



  private static File
  getFile(File local, String path)
  {
    File	result = local;
    String[]	segments = be.re.util.Util.getPathSegments(path);

    for (int i = 0; i < segments.length; ++i)
    {
      result = new File(result, segments[i]);
    }

    return result;
  }



  private static long
  getFileLastModified(File file, Context context) throws SQLException
  {
    PreparedStatement	statement = null;
    ResultSet		resultSet = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "select last_modified from file " +
            "where filename = ? and local = ? and remote = ?"
        );

      statement.setString(1, file.getAbsolutePath());
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      resultSet = statement.executeQuery();

      return resultSet.next() ? resultSet.getLong("last_modified") : -1;
    }

    finally
    {
      release(resultSet);
      release(statement);
    }
  }



  private String[]
  getFiles(File directory, Context context) throws IOException
  {
    File[]		files = directory.listFiles();
    List<String>	result = new ArrayList<String>();

    for (int i = 0; i < files.length; ++i)
    {
      if (!shouldExcludePath("/" + createLocalPath(files[i], context.local)))
      {
        result.add(files[i].getName());
      }
    }

    return result.toArray(new String[0]);
  }



  public Filter
  getFilter()
  {
    return filter;
  }



  private static Headers
  getHeaders(URL url) throws IOException
  {
    Headers	headers = new Headers();

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

    return headers;
  }



  public String
  getHiddenFolderName()
  {
    return hiddenFolderName;
  }



  /**
   * The default is <code>false</code>.
   */

  public boolean
  getInteractive()
  {
    return interactive;
  }



  public File
  getLocal()
  {
    return local;
  }



  public boolean
  getNoRecursion()
  {
    return noRecursion;
  }



  private static Client.Options
  getOptions(Client client, URL url) throws IOException
  {
    return client.options(url, false, false, false, false);
  }



  private static String
  getPath(File localDirectory, File member)
  {
    return
      localDirectory.equals(member) ?
        "" :
        (
          getPath(localDirectory, member.getParentFile()) + member.getName() +
            (member.isDirectory() ? "/" : "")
        );
  }



  private static String
  getPath(URL url, URL remote)
  {
    return
      be.re.net.Util.unescapeUriSpecials
      (
        url.toString().substring(remote.toString().length())
      );
  }



  private static Record
  getRecord(Record tree, String path)
  {
    path = normalize(path);

    Map		current = tree.members;
    String[]	segments = be.re.util.Util.getPathSegments(path);

    for (int i = 0; i < segments.length; ++i)
    {
      Record	entry = (Record) current.get(segments[i]);

      if (entry == null)
      {
        return null;
      }

      if (i == segments.length - 1)
      {
        return entry;
      }

      current = entry.members;
    }

    return null;
  }



  public boolean
  getRecursive()
  {
    return recursive;
  }



  public URL
  getRemote()
  {
    return remote;
  }



  private static String
  getRemotePathFromETag(String etag, Context context) throws SQLException
  {
    if (etag == null || etag.equals("none"))
    { 
      return null;
    }

    PreparedStatement	statement = null;
    ResultSet		resultSet = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "select path from remote_path " +
            "where etag = ? and local = ? and remote = ?"
        );

      statement.setString(1, etag);
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      resultSet = statement.executeQuery();

      return resultSet.next() ? resultSet.getString("path") : null;
    }

    finally
    {
      release(resultSet);
      release(statement);
    }
  }



  private static long
  getRemotePathLastModified(String path, Context context) throws SQLException
  {
    PreparedStatement	statement = null;
    ResultSet		resultSet = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "select last_modified from remote_path " +
            "where path = ? and local = ? and remote = ?"
        );

      statement.setString(1, path);
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      resultSet = statement.executeQuery();

      return resultSet.next() ? resultSet.getLong("last_modified") : -1;
    }

    finally
    {
      release(resultSet);
      release(statement);
    }
  }



  /**
   * The default is 10.
   */

  public int
  getRenameDepth()
  {
    return renameDepth;
  }



  /**
   * The default is <code>false</code>.
   */

  public boolean
  getRenameFiles()
  {
    return renameFiles;
  }



  private static File
  getStorage(URL remote, File local) throws IOException
  {
    File	dir =
      new File
      (
        getStorageDir
        (
          be.re.util.Util.getPackageStorage(Sync.class),
          new File(local.getAbsolutePath())
        ),
        "host_" + remote.getHost()
      );
    String[]	segments = be.re.util.Util.getPathSegments(remote.getPath());

    for (int i = 0; i < segments.length; ++i)
    {
      dir = new File(dir, segments[i]);
    }

    dir.mkdirs();

    return dir;
  }



  private static File
  getStorageDir(File storage, File directory) throws IOException
  {
    return
      new File
      (
        directory.getParentFile() == null ?
          storage : getStorageDir(storage, directory.getParentFile()),
        directory.getName()
      );
  }



  private static long
  getSynchronization(Context context) throws SQLException
  {
    PreparedStatement	statement = null;
    ResultSet		resultSet = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "select occurred from synchronization where local = ? and remote = ?"
        );

      statement.setString(1, context.local.getAbsolutePath());
      statement.setString(2, context.remote.toString());
      resultSet = statement.executeQuery();

      return resultSet.next() ? resultSet.getLong("occurred") : -1;
    }

    finally
    {
      release(resultSet);
      release(statement);
    }
  }



  public File
  getTrash()
  {
    return trash;
  }



  private static URL
  getTrashBin(File file)
  {
    try
    {
      return
        (URL)
          Class.forName("be.re.app.uivi.Service").
            getMethod("getTrashBin", new Class[]{String[].class}).invoke
            (
              null,
              new Object[]{be.re.net.Util.fileToUrl(file).toString()}
            );
    }

    catch (Exception e)
    {
      return null;
    }
  }



  private static File
  getTrashFolder(File member, File local, File trash)
  {
    String[]	segments =
      be.re.util.Util.split(getPath(local, member.getParentFile()), "/");
    File	result = trash;

    for (int i = 0; i < segments.length; ++i)
    {
      result = new File(result, segments[i]);
    }

    return result;
  }



  private Record
  getTree(final URL url, Record top, final Context context) throws IOException
  {
    final List<Record>	children = new ArrayList<Record>();
    final boolean[]	empty = new boolean[]{true};
    Client.Response	response = null;
    final Record	result = top != null ? top : new Record();

    try
    {
      response =
        client.propfindSpecific
        (
          url,
          new ExpandedName[]
            {
              new ExpandedName(Constants.DAV_URI, "getcontenttype"),
              new ExpandedName(Constants.DAV_URI, "getetag"),
              new ExpandedName(Constants.DAV_URI, "getlastmodified"),
              new ExpandedName(Constants.DAV_URI, "resourcetype")
            },
          context.depth
        );

      if (response.getStatusCode() == 403 && "infinity".equals(context.depth))
      {
        context.depth = "1";
        response.close();
        response = null;

        return getTree(url, result, context);
      }

      if (response.getStatusCode() != 207)
      {
        for (Listener listener: listeners)
        {
          listener.error(response);
        }

        return null;
      }

      result.path = url.getFile();

      be.re.webdav.Util.readPropertyList
      (
        url,
        response,
        new be.re.webdav.Util.PropertyHandler()
        {
          public boolean
          handle(URL href, Element[] properties, int statusCode)
          {
            if (statusCode == 200 && properties.length > 0)
            {
              Record	record = new Record();

              try
              {
                if (!href.getFile().endsWith("/") && isCollection(properties))
                {
                  href = new URL(href.toString() + "/");
                }

                record.path = createPath(href, context.remote);

                if
                (
                  !url.toString().equals(href.toString())	&&
                  getRecord(result, record.path) == null
                )
                {
                  setRecordProperties(record, properties, href, context);

                  if
                  (
                    addToTree(result, record)	&&
                    "1".equals(context.depth)	&&
                    record.path.endsWith("/")
                  )
                  {
                    children.add(record);
                  }
                }

                empty[0] = false;
              }

              catch (MalformedURLException e)
              {
                throw new RuntimeException(e); // Would be a bug.
              }
            }

            return true;
          }
        },
        false
      );
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.PROPFIND, e);
      }

      return null;
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }
    }

    if ("1".equals(context.depth))
    {
      for (Record record: children)
      {
        if
        (
          getTree
          (
            new URL
            (
              context.remote,
              be.re.net.Util.escapeUriPathSegments(record.path)
            ),
            result,
            context
          ) == null
        )
        {
          return null;
        }
      }
    }
    else
    {
      if (empty[0] && result.path.equals(url.getFile()))
      {
        context.depth = "1";

        return getTree(url, top, context);
      }
    }

    return result;
  }



  private static URL
  getUrl(URL url, String path) throws MalformedURLException
  {
    return new URL(url, be.re.net.Util.escapeUriPathSegments(path));
  }



  public static URL
  getUrl(URL remote, File local, File member) throws MalformedURLException
  {
    return
      new URL
      (
        remote,
        be.re.net.Util.escapeUriPathSegments(getPath(local, member))
      );
  }



  private static boolean
  hasChanged(File file, Context context) throws IOException, SQLException
  {
    long	lastCheck = getFileLastModified(file, context);

    return lastCheck != -1 && file.lastModified() > lastCheck;
  }



  private static boolean
  hasChanged(Record record, Context context) throws IOException, SQLException
  {
    long	lastCheck = getRemotePathLastModified(record.path, context);

    return lastCheck != -1 && record.lastModified > lastCheck;
  }



  private boolean
  insert(File member, Context context) throws IOException, SQLException
  {
    String	path = getPath(context.local, member);
    boolean	result = true;
    URL		url = getUrl(context.remote, context.local, member);

    checkout(URLManager.getParent(url), context);

    for (Listener listener: listeners)
    {
      listener.startCreate(url);
    }

    try
    {
      long[]	time = new long[1];

      if (member.isDirectory())
      {
        try
        {
          client.mkcol(url);
          writeFilename(member, context);
          writeUrl(url, context);
        }

        catch (Throwable e)
        {
          for (Listener listener: listeners)
          {
            listener.exception(url, HTTPClient.MKCOL, e);
          }

          return false;
        }

        Record	record = new Record();

        record.path = getPath(context.local, member);
        result = getNoRecursion() || sync(record, member, context);
      }
      else
      {
        try
        {
          put(url, member, null, time, context);
          writeFilename(member, context);
          writeUrl(url, time[0], context);
        }

        catch (Throwable e)
        {
          for (Listener listener: listeners)
          {
            listener.exception(url, HTTPClient.PUT, e);
          }

          result = false;
        }
      }

      if (result && getDefaultAcl() != null)
      {
        setAcl(url);
      }
    }

    finally
    {
      for (Listener listener: listeners)
      {
        listener.endCreate(url);
      }
    }

    return result;
  }



  private boolean
  insert(Record record, File member, Context context)
    throws IOException, SQLException
  {
    for (Listener listener: listeners)
    {
      listener.startCreate(member);
    }

    if (record.path.endsWith("/"))
    {
      member.mkdir();
      writeFilename(member, context);
      writeRemotePath(record, context);

      for (Listener listener: listeners)
      {
        listener.endCreate(member);
      }

      return getNoRecursion() || sync(record, member, context);
    }

    URL	url = getUrl(context.remote, record.path);

    try
    {
      get(url, member);
      member.setLastModified(record.lastModified);
      writeFilename(member, context);
      writeRemotePath(record, context);

      return true;
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.GET, e);
      }

      return false;
    }

    finally
    {
      for (Listener listener: listeners)
      {
        listener.endCreate(member);
      }
    }
  }



  private static boolean
  isCollection(Element[] properties)
  {
    for (int i = 0; i < properties.length; ++i)
    {
      // We should test for the namespace, but there seem to be servers such as
      // box.net that don't set the namespace.

      if ("resourcetype".equals(properties[i].getLocalName()))
      {
        return
          be.re.xml.Util.selectFirstChild
          (
            properties[i],
            Constants.DAV_URI,
            "collection"
          ) != null;
      }
    }

    return false;
  }



  public static void
  main(String[] args) throws Exception
  {
    boolean	bi = false;
    File	configFile = null;
    File	directory = null;
    boolean	down = false;
    String	excludes = null;
    List	extra = new ArrayList();
    String	hiddenFolderName = null;
    String	interval = null;
    boolean	noRecursion = false;
    boolean	recursive = false;
    int		renameDepth = 10;
    boolean	renameFiles = false;
    boolean	reset = false;
    boolean	showUsage = false;
    boolean	up = false;
    String	url = null;

    for (int i = 0; i < args.length; ++i)
    {
      if ("-bi".equals(args[i]))
      {
        bi = true;
      }
      else
      {
        if ("-down".equals(args[i]))
        {
          down = true;
        }
        else
        {
          if ("-up".equals(args[i]))
          {
            up = true;
          }
          else
          {
            if ("-u".equals(args[i]))
            {
              if (i == args.length - 1)
              {
                showUsage = true;
              }
              else
              {
                url = args[++i];
              }
            }
            else
            {
              if ("-d".equals(args[i]))
              {
                if (i == args.length - 1)
                {
                  showUsage = true;
                }
                else
                {
                  directory = new File(args[++i]);
                }
              }
              else
              {
                if ("-i".equals(args[i]))
                {
                  if (i == args.length - 1)
                  {
                    showUsage = true;
                  }
                  else
                  {
                    interval = args[++i];
                  }
                }
                else
                {
                  if ("-c".equals(args[i]))
                  {
                    if (i == args.length - 1)
                    {
                      showUsage = true;
                    }
                    else
                    {
                      configFile = new File(args[++i]);
                    }
                  }
                  else
                  {
                    if ("-h".equals(args[i]))
                    {
                      usage(0);
                    }
                    else
                    {
                      if ("-r".equals(args[i]))
                      {
                        recursive = true;
                      }
                      else
                      {
                        if ("-e".equals(args[i]))
                        {
                          excludes = args[++i];
                        }
                        else
                        {
                          if ("-rename".equals(args[i]))
                          {
                            renameFiles = true;
                          }
                          else
                          {
                            if ("-rename-depth".equals(args[i]))
                            {
                              if (i == args.length - 1)
                              {
                                showUsage = true;
                              }
                              else
                              {
                                renameDepth = Integer.parseInt(args[++i]);
                              }
                            }
                            else
                            {
                              if ("-hidden-folder".equals(args[i]))
                              {
                                if (i == args.length - 1)
                                {
                                  showUsage = true;
                                }
                                else
                                {
                                  hiddenFolderName = args[++i];
                                }
                              }
                              else
                              {
                                if ("-reset".equals(args[i]))
                                {
                                  reset = true;
                                }
                                else
                                {
                                  if ("-no-recursion".equals(args[i]))
                                  {
                                    noRecursion = true;
                                  }
                                  else
                                  {
                                    extra.add(args[i]);
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    boolean	interactive =
      (configFile == null && directory == null) ||
        System.getProperty("java.awt.headless") != null;

    if (!interactive)
    {
      System.setProperty("java.awt.headless", "true");
    }

    if
    (
      showUsage					||
      extra.size() > 0				||
      (
        interval != null			&&
        (
          !be.re.util.Util.isInteger(interval)	||
          Integer.parseInt(interval) < 0
        )
      )						||
      (
        configFile == null			&&
        (
          url == null				||
          !validateUrl(url)			||
          (
            !bi					&&
            !down				&&
            !up
          )					||
          (
            bi					&&
            down
          )					||
          (
            bi					&&
            up
          )					||
          (
            down				&&
            up
          )
        )
      )						||
      (
        configFile != null			&&
        (
          url != null				||
          directory != null			||
          excludes != null			||
          bi					||
          down					||
          up					||
          recursive				||
          noRecursion
        )
      )
    )
    {
      if (interactive && (url == null || extra.size() > 0))
      {
        Util.report(null, Util.getResource("err_one_folder"), true);

        return;
      }

      usage(1);
    }

    if (url != null && !url.endsWith("/"))
    {
      if (interactive)
      {
        Util.report(null, Util.getResource("err_one_folder"), true);

        return;
      }

      System.err.
        println("The URL should denote a collection (end with a \"/\").");
      //System.exit(1);
    }

    if (interactive && (directory = getDirectory()) == null)
    {
      return;
    }

    if
    (
      interactive							&&
      !new ConfirmationDialog(Util.getResource("title_synchronize")).
        confirm()
    )
    {
      return;
    }

    if (reset)
    {
      removeSynchronizationMark(new URL(url), directory, getDatabase());
    }

    boolean	end = false;

    while (!end)
    {
      try
      {
        if (configFile != null)
        {
          sync(configFile, System.err, interval != null);
        }
        else
        {
          if (interactive)
          {
            startBusy();
          }

          try
          {
            Sync	sync = new Sync();

            sync.setExclusions
            (
              excludes != null ?
                be.re.util.Util.split(excludes, " ,") : new String[0]
            );

            sync.setDirection
            (
              bi ?
                Direction.BIDIRECTIONAL :
                (down ? Direction.DOWN : Direction.UP)
            );

            sync.setLocal(directory);
            sync.setRemote(new URL(url));
            sync.setInteractive(interactive);
            sync.setDaemonMode(interval != null);
            sync.setRecursive(recursive);
            sync.setRenameFiles(renameFiles);
            sync.setRenameDepth(renameDepth);
            sync.setHiddenFolderName(hiddenFolderName);
            sync.setNoRecursion(noRecursion);
            sync.addListener(new Reporter(interactive ? null : System.out));

            sync.run();
          }

          finally
          {
            if (interactive)
            {
              endBusy();
            }
          }
        }
      }

      catch (Exception e)
      {
        if (e.getMessage() != null)
        {
          System.err.println(e.getMessage());
          be.re.util.Util.printStackTrace(e);
        }
        else
        {
          e.printStackTrace();
        }
      }

      if (interval == null)
      {
        end = true;
      }
      else
      {
        try
        {
          Thread.sleep(Integer.parseInt(interval) * 1000);
        }

        catch (InterruptedException e)
        {
        }
      }
    }
  }



  private static void
  monitorFile(File file)
  {
    while (System.currentTimeMillis() - file.lastModified() < 2000)
    {
      try
      {
        Thread.sleep(500);
      }

      catch (InterruptedException e)
      {
      }
    }
  }



  private static boolean
  moveExisting(URL url, int version, int renameDepth) throws IOException
  {
    String	s = url.toString();
    URL		alternative =
      new URL(s.endsWith("/") ? s.substring(0, s.length() - 1) : (s + "/"));

    return
      moveExistingAlternative(url, version, renameDepth) ||
        moveExistingAlternative(alternative, version, renameDepth);
  }



  private static boolean
  moveExistingAlternative(URL url, int version, int renameDepth)
    throws IOException
  {
    if (new URLManager().exists(url))
    {
      if (version < renameDepth)
      {
        moveToHigherVersion(url, version, renameDepth);
      }
      else
      {
        new Client().delete(url);
      }

      return true;
    }

    return false;
  }



  private void
  moveRecords() throws IOException, SQLException
  {
    Context	context = null;

    try
    {
      context = new Context(local, remote, database);
      context.connection.setAutoCommit(false);

      File	localFiles =
        new File(getStorage(remote, local), ".local_files");
      File	remoteFiles =
        new File(getStorage(remote, local), ".remote_files");
      File	lastSynchronization =
        new File(getStorage(remote, local), ".webdav_sync");

      if (getSynchronization(context) == -1)
      {
        copyLocalFiles(localFiles, context);
        copyRemoteFiles(remoteFiles, context);
        copyLastSynchronization(lastSynchronization, context);
      }

      context.connection.commit();
      localFiles.delete();
      remoteFiles.delete();
      lastSynchronization.delete();
      cleanUpDirectories(getStorage(remote, local));
    }

    catch (Exception e)
    {
      if (context != null)
      {
        context.connection.rollback();
      }
    }

    finally
    {
      release(context);
    }
  }



  public static boolean
  moveToHiddenFolder(File file, String hiddenFolderName, int renameDepth)
  {
    File	hiddenFolder = new File(file.getParentFile(), hiddenFolderName);
    File	saved = new File(hiddenFolder, file.getName());

    return
      (hiddenFolder.isDirectory() || hiddenFolder.mkdir()) &&
        (
          !saved.exists() || (renameDepth == 0 && saved.delete()) ||
            moveToHigherVersion(saved, renameDepth)
        ) && file.renameTo(saved);
  }



  public static void
  moveToHiddenFolder(URL url, String hiddenFolderName, int renameDepth)
    throws IOException
  {
    Client	client = new Client();
    URL		hiddenFolder =
      new URL
      (
        URLManager.getParent(url),
        be.re.net.Util.escapeUriPathSegment(hiddenFolderName) + "/"
      );
    URL		saved =
      new URL(hiddenFolder, be.re.net.Util.getLastPathSegment(url));

    if (!moveExisting(saved, 1, renameDepth))
    {
      try
      {
        client.mkcol(hiddenFolder);
      }

      catch (Exception e)
      {
        // Some servers don't allow fetching information from a hidden folder,
        // so just go ahead and try to create it.
      }
    }

    client.move(url, saved, false);
  }



  public static boolean
  moveToHigherVersion(File file, int renameDepth)
  {
    return moveToHigherVersion(file, 1, renameDepth);
  }



  private static boolean
  moveToHigherVersion(File file, int version, int renameDepth)
  {
    File	saved =
      new File
      (
        file.getParentFile(),
        (
          file.getName().endsWith("." + String.valueOf(version - 1)) ?
            file.getName().substring(0, file.getName().lastIndexOf('.')) :
            file.getName()
        ) + "." + String.valueOf(version)
      );

    return
      (
        !saved.exists() || (version == renameDepth && saved.delete()) ||
          moveToHigherVersion(saved, version + 1, renameDepth)
      ) && file.renameTo(saved);
  }



  public static void
  moveToHigherVersion(URL url, int renameDepth) throws IOException
  {
    moveToHigherVersion(url, 1, renameDepth);
  }



  private static void
  moveToHigherVersion(URL url, int version, int renameDepth) throws IOException
  {
    Client	client = new Client();
    String	name = be.re.net.Util.getLastPathSegment(url);
    String	prefix;
    String	suffix;

    if (name.endsWith("/"))
    {
      prefix = name.substring(0, name.length() - 1);
      suffix = "/";
    }
    else
    {
      prefix = name;
      suffix = "";
    }

    URL	saved =
      new URL
      (
        URLManager.getParent(url),
        (
          name.endsWith("." + String.valueOf(version - 1) + suffix) ?
            name.substring(0, name.lastIndexOf('.')) : prefix
        ) + "." + String.valueOf(version) + suffix
      );

    moveExisting(saved, version + 1, renameDepth);
    client.move(url, saved, false);
  }



  public static boolean
  moveToTrash(File file, File local, File trash, int renameDepth)
  {
    if (trash == null)
    {
      return false;
    }

    File	folder = getTrashFolder(file, local, trash);
    File	saved = new File(folder, file.getName());

    return
      (folder.isDirectory() || folder.mkdirs()) &&
        (
          !saved.exists() || (renameDepth == 0 && saved.delete()) ||
            moveToHigherVersion(saved, renameDepth)
        ) && file.renameTo(saved);
  }



  private static String
  normalize(String s)
  {
    return
      !Normalizer.isNormalized(s, Normalizer.Form.NFC) ?
        Normalizer.normalize(s, Normalizer.Form.NFC) : s;
  }



  private static String[]
  normalize(String[] s)
  {
    String[]	result = new String[s.length];

    for (int i = 0; i < s.length; ++i)
    {
      result[i] = normalize(s[i]);
    }

    return result;
  }



  private boolean
  notLocally(Record record, File member, Context context)
    throws IOException, SQLException
  {
    return
      context.direction == Direction.UP ?
        delete(record, context) :
        (
          context.direction == Direction.DOWN ?
            (
              tryRenamedFrom(record, member, context) ?
                onBothSides(record, member, context) :
                insert(record, member, context)
            ) :
            (
              tryRenamedFrom(record, member, context) ?
                onBothSides(record, member, context) :
                (
                  getFileLastModified(member, context) != -1 ?
                    delete(record, context) : insert(record, member, context)
                )
            )
        );
  }



  private boolean
  notOnServer(File member, Context context) throws IOException, SQLException
  {
    if (!member.exists())
    {
      return false;
        // It was already renamed and the path will never have a trailing slash,
        // because we can't know whether this was a directory or not. A rename
        // look-up could therefore fail.
    }

    Record	newRecord;
    String	path = getPath(context.local, member);

    return
      context.direction == Direction.DOWN ?
        (
          (newRecord = tryRenamedTo(path, member, context)) != null ?
            onBothSides
            (
              newRecord,
              getFile(context.local, newRecord.path),
              context
            ) : delete(member, context)
        ) :
        (
          context.direction == Direction.UP ?
            insert(member, context) :
            (
              (newRecord = tryRenamedTo(path, member, context)) != null ?
                onBothSides
                (
                  newRecord,
                  getFile(context.local, newRecord.path),
                  context
                ) :
                (
                  getRemotePathLastModified(path, context) != -1 ?
                    delete(member, context) : insert(member, context)
                )
            )
        );
  }



  private boolean
  onBothSides(Record record, File member, Context context)
    throws IOException, SQLException
  {
    if (!record.done)
    {
      record.done = true;
        // We may come here twice for a certain record because of a rename.

      boolean	result =
        checkDirectoryConflict(record, member, context) &&
          (
            (
              record.path.endsWith("/") &&
                (
                  context.direction == Direction.DOWN ||
                    context.direction == Direction.BIDIRECTIONAL
                )
            ) ||
              (
                member.isDirectory() &&
                  (
                    context.direction == Direction.UP ||
                      context.direction == Direction.BIDIRECTIONAL
                  )
              ) ?
              (getNoRecursion() || sync(record, member, context)) :
              (
                checkConflicts(record, member, context) &&
                  exchange(record, member, context)
              )
          );

      return result;
    }

    return false;
  }



  private static InputStream
  prepareMessage(Node node)
  {
    ByteArrayOutputStream	out = new ByteArrayOutputStream();

    try
    {
      Util.transformerFactory.newTransformer().
        transform(new DOMSource(node), new StreamResult(out));
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    return new ByteArrayInputStream(out.toByteArray());
  }



  private boolean
  put(URL url, File member, String mimeType, long[] time, Context context)
    throws IOException
  {
    if (getDaemonMode())
    {
      monitorFile(member);
    }

    boolean	create = false;

    if (new URLManager().isContainer(url))
    {
      create = true;
      checkout(URLManager.getParent(url), context);

      for (Listener listener: listeners)
      {
        listener.startDelete(url);
      }

      try
      {
        if (hiddenFolderName != null)
        {
          moveToHiddenFolder(url, hiddenFolderName, renameDepth);
        }
        else
        {
          client.delete(url);
        }
      }

      catch (Throwable e)
      {
        for (Listener listener: listeners)
        {
          listener.exception(url, HTTPClient.DELETE, e);
        }

        return false;
      }

      finally
      {
        for (Listener listener: listeners)
        {
          listener.endDelete(url);
        }
      }
    }

    for (Listener listener: listeners)
    {
      if (create)
      {
        listener.startCreate(url);
      }
      else
      {
        listener.startUpdate(url);
      }
    }

    try
    {
      if (renameFiles && renameDepth > 0 && new URLManager().exists(url))
      {
        checkout(URLManager.getParent(url), context);
        moveToHigherVersion(url, renameDepth);
      }
      else
      {
        checkout(url, context);
      }

      Headers	headersOut = new Headers();
      int	statusCode =
        put
        (
          url,
          member,
          mimeType != null ?
            mimeType : MimeType.getContentTypeFromName(member.getName()),
          headersOut
        );

      if (statusCode != 200 && statusCode != 201 && statusCode != 204)
      {
        for (Listener listener: listeners)
        {
          listener.error(url, HTTPClient.PUT, statusCode);
        }

        return false;
      }

      time[0] = be.re.net.Util.getTimeHeader(headersOut, "Date");

      return true;
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.PUT, e);
      }

      return false;
    }

    finally
    {
      for (Listener listener: listeners)
      {
        if (create)
        {
          listener.endCreate(url);
        }
        else
        {
          listener.endUpdate(url);
        }
      }
    }
  }



  public static int
  put(URL url, File file, String mimeType, Headers headersOut)
    throws IOException
  {
    boolean	canRetry = true;
    int		statusCode = -1;

    while (canRetry)
    {
      Headers	headersIn = new Headers();

      headersIn.set
      (
        "Content-Type",
        mimeType != null ? mimeType : "application/octet-stream"
      );

      HTTPClient.request
      (
        HTTPClient.PUT,
        url,
        new FileInputStream(file),
        null,
        null,
        headersIn,
        headersOut,
        null,
        null,
        HTTPClient.getExpect100(),
        file.length() >= 1024 * 1024 ? file.length() : -1
      ).close();

      statusCode = Integer.parseInt(headersOut.get("Status-Code")[0]);

      if (statusCode == 405) // Method not allowed.
      {
        HTTPClient.request(HTTPClient.DELETE, url);
      }
      else
      {
        canRetry = false;
      }
    }

    return statusCode;
  }



  private static Job[]
  readJobs(File configFile) throws Exception
  {
    Document	document =
      be.re.xml.Util.newDocumentBuilderFactory(false).newDocumentBuilder().
        parse(new FileInputStream(configFile));

    if (!"config".equals(document.getDocumentElement().getLocalName()))
    {
      throw new Exception("Not a configuration file.");
    }

    Client	client = new Client();
    Node[]	sync =
      be.re.xml.Util.selectChildren(document.getDocumentElement(), "sync");
    Job[]	result = new Job[sync.length];

    for (int i = 0; i < sync.length; ++i)
    {
      result[i] = new Job();

      Node	node = be.re.xml.Util.selectFirstChild(sync[i], "direction");

      if (node == null)
      {
        throw new Exception("Missing \"direction\" element.");
      }

      String	value = be.re.xml.Util.getText(node);

      if (!Array.inArray(new String[]{"down", "up", "bi"}, value))
      {
        throw new Exception("Wrong direction value \"" + value + "\".");
      }

      result[i].direction =
        "bi".equals(value) ?
          Direction.BIDIRECTIONAL :
          ("down".equals(value) ? Direction.DOWN : Direction.UP);
      node = be.re.xml.Util.selectFirstChild(sync[i], "directory");

      if (node == null)
      {
        throw new Exception("Missing \"directory\" element.");
      }

      value = be.re.xml.Util.getText(node);

      if (value == null)
      {
        throw new Exception("Wrong directory value \"" + value + "\".");
      }

      result[i].directory = new File(value);
      node = be.re.xml.Util.selectFirstChild(sync[i], "url");

      if (node == null)
      {
        throw new Exception("Missing \"url\" element.");
      }

      value = be.re.xml.Util.getText(node);

      if (value == null || !validateUrl(value))
      {
        throw new Exception("Wrong url value \"" + value + "\".");
      }

      result[i].url = new URL(value);

      if (supportsFeature(getOptions(client, result[i].url), "access-control"))
      {
        result[i].acl = be.re.xml.Util.selectFirstChild(sync[i], "acl");
      }

      Node[]	excludes = be.re.xml.Util.selectChildren(sync[i], "exclude");

      result[i].excludes = new String[excludes.length];

      for (int j = 0; j < excludes.length; ++j)
      {
        result[i].excludes[j] = be.re.xml.Util.getText(excludes[j]);
      }

      node = be.re.xml.Util.selectFirstChild(sync[i], "rename");

      if (node != null)
      {
        result[i].rename = true;
      }

      node = be.re.xml.Util.selectFirstChild(sync[i], "rename-depth");

      if (node != null)
      {
        value = be.re.xml.Util.getText(node);

        if (be.re.util.Util.isInteger(value) && Integer.parseInt(value) >= 0)
        {
          result[i].renameDepth = Integer.parseInt(value);
        }
      }

      node = be.re.xml.Util.selectFirstChild(sync[i], "hidden-folder-name");

      if (node != null)
      {
        value = be.re.xml.Util.getText(node);

        if (value.trim().length() > 0)
        {
          result[i].hiddenFolderName = value.trim();
        }
      }

      node = be.re.xml.Util.selectFirstChild(sync[i], "recursive");

      if (node != null)
      {
        result[i].recursive = true;
      }

      node = be.re.xml.Util.selectFirstChild(sync[i], "no-recursion");

      if (node != null)
      {
        result[i].noRecursion = true;
      }
    }

    return result;
  }



  private static void
  release(Context context) throws IOException
  {
    if (context != null)
    {
      try
      {
        context.close();
      }

      catch (SQLException e)
      {
        throw new be.re.io.IOException(e);
      }
    }
  }



  private static void
  release(ResultSet resultSet) throws SQLException
  {
    if (resultSet != null)
    {
      resultSet.close();
    }
  }



  private static void
  release(Statement statement) throws SQLException
  {
    if (statement != null)
    {
      statement.close();
    }
  }



  public void
  removeListener(Listener listener)
  {
    listeners.remove(listener);
  }



  public static void
  removeSynchronizationMark(URL url, File directory, File database)
    throws IOException
  {
    Context	context = null;

    try
    {
      context = new Context(directory, url, database);
      context.connection.setAutoCommit(false);
      deleteAllFiles(context);
      deleteAllRemotePaths(context);
      deleteSynchronization(context);
      context.connection.commit();
    }

    catch (SQLException e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      release(context);
    }
  }



  private static void
  renameFile(File oldFile, File newFile, Context context) throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          "update file set filename = ? " +
            "where filename = ? and local = ? and remote = ?"
        );

      statement.setString(1, newFile.getAbsolutePath());
      statement.setString(2, oldFile.getAbsolutePath());
      statement.setString(3, context.local.getAbsolutePath());
      statement.setString(4, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  private boolean
  replaceDirectory(File directory) throws IOException
  {
    if (!deleteConflict(directory))
    {
      return false;
    }

    for (Listener listener: listeners)
    {
      listener.startDelete(directory);
    }

    if (trash != null)
    {
      moveToTrash(directory, local, trash, renameDepth);
    }
    else
    {
      if (hiddenFolderName != null)
      {
        moveToHiddenFolder(directory, hiddenFolderName, renameDepth);
      }
      else
      {
        delete(directory, interactive, false);
      }
    }

    for (Listener listener: listeners)
    {
      listener.endDelete(directory);
      listener.startCreate(directory);
    }

    directory.mkdir();

    for (Listener listener: listeners)
    {
      listener.endCreate(directory);
    }

    return true;
  }



  private boolean
  replaceDirectory(URL url) throws IOException
  {
    if (!deleteConflict(url))
    {
      return false;
    }

    for (Listener listener: listeners)
    {
      listener.startDelete(url);
    }

    try
    {
      if (hiddenFolderName != null)
      {
        moveToHiddenFolder(url, hiddenFolderName, renameDepth);
      }
      else
      {
        client.delete(url);
      }

      for (Listener listener: listeners)
      {
        listener.endDelete(url);
        listener.startCreate(url);
      }

      client.mkcol(url);

      return true;
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.MKCOL, e);
      }

      return false;
    }

    finally
    {
      for (Listener listener: listeners)
      {
        listener.endCreate(url);
      }
    }
  }



  public Conflict[]
  run() throws IOException
  {
    for (Listener listener: listeners)
    {
      listener.start();
    }

    Context	context = null;

    try
    {
      moveRecords();

      context = new Context(local, remote, database);
      context.checkedOut =
        supportsFeature(getOptions(client, remote), "version-control") ?
          new HashSet<String>() : null;
      context.direction = direction;
      context.lastSynchronization = getSynchronization(context);
      context.depth = recursive ? "1" : "infinity";
      context.conflicts = new ArrayList<Conflict>();
      context.tree = getTree(remote, null, context);

      if (context.tree != null && sync(context.tree, local, context))
      {
        setSynchronization(context);
      }

      return context.conflicts.toArray(new Conflict[0]);
    }

    catch (SQLException e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      checkin(context);

      for (Listener listener: listeners)
      {
        listener.end();
      }

      release(context);
    }
  }



  private void
  setAcl(URL url) throws IOException
  {
    Node	current = getAcl(url);

    if (current == null)
    {
      current = getDefaultAcl();
    }
    else
    {
      Node[]	aces =
        be.re.xml.Util.
          selectChildren(getDefaultAcl(), Constants.DAV_URI, "ace");

      for (int i = 0; i < aces.length; ++i)
      {
        current.
          appendChild(current.getOwnerDocument().importNode(aces[i], true));
      }
    }

    Client.Response	response = null;

    try
    {
      response =
        client.operation
        (
          HTTPClient.ACL,
          url,
          new Headers(),
          prepareMessage(current)
        );

      if (response.getStatusCode() != 200)
      {
        for (Listener listener: listeners)
        {
          listener.error(response);
        }
      }
    }

    catch (Throwable e)
    {
      for (Listener listener: listeners)
      {
        listener.exception(url, HTTPClient.ACL, e);
      }
    }

    finally
    {
      if (response != null)
      {
        response.close();
      }
    }
  }



  public void
  setDaemonMode(boolean daemonMode)
  {
    this.daemonMode = daemonMode;
  }



  /**
   * The WebDAV ACL that will be set on new URLs.
   */

  public void
  setDefaultAcl(Node acl)
  {
    defaultAcl = acl;
  }



  /**
   * Sets the synchronization direction.
   */

  public void
  setDirection(Direction direction)
  {
    this.direction = direction;
  }



  /**
   * Setting this to <code>null</code> will save an empty array.
   */

  public void
  setExclusions(String[] exclusions)
  {
    if (exclusions == null)
    {
      this.exclusions = new String[0];
    }
    else
    {
      String[]	result = new String[exclusions.length];

      for (int i = 0; i < result.length; ++i)
      {
        result[i] = exclusions[i].replace('\\', '/').trim();
      }

      this.exclusions = result;
    }

    exclusionPatterns = toPattern(this.exclusions);
  }



  /**
   * A filter can exclude a particular synchronization based on a specific
   * condition.
   */

  public void
  setFilter(Filter filter)
  {
    this.filter = filter;
  }



  /**
   * When set deletions will be implemented as moves to the hidden folder, which
   * is created in the same folder as the file or folder that has to be deleted.
   * This works locally as well as on the server. The hidden folder is excluded
   * from synchronization.
   */

  public void
  setHiddenFolderName(String hiddenFolderName)
  {
    this.hiddenFolderName = hiddenFolderName;
  }



  public void
  setInteractive(boolean interactive)
  {
    this.interactive = interactive;
  }



  /**
   * Sets the local directory.
   */

  public void
  setLocal(File local)
  {
    if (local.exists() && !local.isDirectory())
    {
      throw
        new IllegalArgumentException
        (
          local.getAbsolutePath() + " exists but is not a directory."
        );
    }

    if (!local.exists())
    {
      local.mkdir();
    }

    this.local = local;
  }



  /**
   * This prohibits recursion into subfolders, whether the server supports it
   * or not.
   */

  public void
  setNoRecursion(boolean noRecursion)
  {
    this.noRecursion = noRecursion;
  }



  private static void
  setRecordProperties
  (
    Record	record,
    Element[]	properties,
    URL		url,
    Context	context
  )
  {
    for (int i = 0; i < properties.length; ++i)
    {
      // We should test for the namespace, but there seem to be servers such as
      // box.net that don't set the namespace.

      if ("getlastmodified".equals(properties[i].getLocalName()))
      {
        record.lastModified =
          cutMillis
          (
            be.re.net.Util.httpDate(be.re.xml.Util.getText(properties[i]))
          );

        if (record.lastModified == -1)
        {
          // Another one for box.net.

          record.lastModified =
            cutMillis
            (
              be.re.util.Util.
                parseTimestamp(be.re.xml.Util.getText(properties[i]))
            );
        }
      }
      else
      {
        if ("getetag".equals(properties[i].getLocalName()))
        {
          record.etag = be.re.xml.Util.getText(properties[i]);
          context.etagToPath.put(record.etag, record.path);
        }
        else
        {
          if ("getcontenttype".equals(properties[i].getLocalName()))
          {
            record.contentType = be.re.xml.Util.getText(properties[i]);
          }
        }
      }
    }

    // There are servers that don't return the getlastmodified property despite
    // the fact that they have the Last-Modified header.

    if (record.lastModified == -1)
    {
      record.lastModified =
        cutMillis(be.re.net.HTTPClient.Util.getLastModified(url));
    }
  }



  /**
   * This is useful for servers that don't support the value <q>infinity</q>
   * for the <code>Depth</code> header and implement it silently as <q>1</q>.
   * In that case this option will cause recursion into empty collections.
   */

  public void
  setRecursive(boolean recursive)
  {
    this.recursive = recursive;
  }



  /**
   * Sets the remote URL.
   */

  public void
  setRemote(URL remote)
  {
    try
    {
      this.remote =
        !remote.toString().endsWith("/") ?
          new URL(remote.toString() + "/") : remote;
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  /**
   * The number of versions that are kept when renaming files.
   */

  public void
  setRenameDepth(int renameDepth)
  {
    if (renameDepth < 0)
    {
      throw new IllegalArgumentException("The renameDepth should be positive.");
    }

    this.renameDepth = renameDepth;
  }



  /**
   * When set to <code>true</code> files that should be updated will be renamed
   * first in order to preserve the current version. Renaming is done by
   * appending a number after the filename. Numbering starts at 1. If there is
   * already a version with a number that version will be renamed with a higher
   * number in turn.
   */

  public void
  setRenameFiles(boolean renameFiles)
  {
    this.renameFiles = renameFiles;
  }



  private static void
  setSynchronization(Context context) throws SQLException
  {
    setSynchronization(-1, context);
  }



  private static void
  setSynchronization(long time, Context context) throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          getSynchronization(context) != -1 ?
            (
              "update synchronization set occurred = ? " +
                "where local = ? and remote = ? "
            ) :
            (
              "insert into synchronization (occurred, local, remote) " +
                "values (?, ?, ?)"
            )
        );

      statement.
        setLong(1, time != -1 ? time : System.currentTimeMillis() + 5000);
        // Add a small margin for clock differences.
      statement.setString(2, context.local.getAbsolutePath());
      statement.setString(3, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  public void
  setTrash(File trash)
  {
    this.trash = trash;
  }



  private boolean
  shouldExclude(String name)
  {
    if
    (
      name.equals(hiddenFolderName)					||
      (renameFiles && VERSIONED_FILENAME.matcher(name).matches())
    )
    {
      return true;
    }

    for (int i = 0; i < exclusionPatterns.length; ++i)
    {
      if (exclusionPatterns[i].matcher(name).matches())
      {
        return true;
      }
    }

    return false;
  }



  private boolean
  shouldExcludePath(String path)
  {
    if (shouldExcludePathOrParent(path))
    {
      return true;
    }

    String[]	segments = be.re.util.Util.getPathSegments(path);

    for (int i = 0; i < segments.length; ++i)
    {
      if (shouldExclude(segments[i]))
      {
        return true;
      }
    }

    return false;
  }



  private boolean
  shouldExcludePathOrParent(String path)
  {
    int	index =
      path.length() > 1 ? path.lastIndexOf('/', path.length() - 2) : -1;

    return
      shouldExclude(path) ||
        (
          index != -1 &&
            shouldExcludePathOrParent(path.substring(0, index + 1))
        );
  }



  private static String[]
  splitLine(String s)
  {
    int			position = 0;
    List<String>	result = new ArrayList<String>();

    for
    (
      int i = s.indexOf("||");
      i != -1;
      position = i + 2, i = s.indexOf("||", position)
    )
    {
      result.add(s.substring(position, i));
    }

    result.add(s.substring(position));

    return result.toArray(new String[0]);
  }



  private static void
  startBusy()
  {
    try
    {
      Class.forName("be.re.webdav.cmd.Uivi").getMethod("startBusy").
        invoke(null, new Object[0]);
    }

    catch (Exception e)
    {
    }
  }



  private static boolean
  supportsFeature(Client.Options options, String feature)
  {
    return Array.inArray(options.davFeatures, feature);
  }



  private boolean
  sync(Record record, File directory, Context context)
    throws IOException, SQLException
  {
    String[]	names =
      (String[]) record.members.keySet().toArray(new String[0]);
    String[]	list = getFiles(directory, context);
    URL		url = getUrl(context.remote, record.path);

    if (list == null)
    {
      list = new String[0];

      if (!directory.isDirectory() && !replaceDirectory(directory))
      {
        return false;
      }
    }
    else
    {
      list = normalize(list);
      Arrays.sort(names);
      Arrays.sort(list);
    }

    if (!record.path.endsWith("/") && !replaceDirectory(url))
    {
      return false;
    }

    Diff.Change[]	changes = Diff.diff(list, names);
    boolean		updates = false;

    for (int i = 0; i < changes.length; ++i)
    {
      File	member = new File(directory, (String) changes[i].subject);
      Record	rec = (Record) record.members.get(changes[i].subject);

      if
      (
        filter == null							||
        rec == null							||
        !filter.exclude(member, getUrl(context.remote, rec.path))
      )
      {
        switch (changes[i].operation)
        {
          case Diff.DELETE:
            updates |= notOnServer(member, context);
            break;

          case Diff.INSERT:
            updates |= notLocally(rec, member, context);
            break;

          case Diff.UNCHANGED:
            updates |= onBothSides(rec, member, context);
            break;
        }
      }
    }

    return updates;
  }



  private static void
  sync(File configFile, OutputStream out, boolean daemonMode) throws Exception
  {
    Job[]	jobs = readJobs(configFile);

    for (int i = 0; i < jobs.length; ++i)
    {
      Sync	sync = new Sync();

      sync.setLocal(jobs[i].directory);
      sync.setRemote(jobs[i].url);
      sync.setDirection(jobs[i].direction);
      sync.setDefaultAcl(jobs[i].acl);
      sync.setExclusions(jobs[i].excludes);
      sync.setDaemonMode(daemonMode);
      sync.setRenameFiles(jobs[i].rename);
      sync.setRenameDepth(jobs[i].renameDepth);
      sync.setHiddenFolderName(jobs[i].hiddenFolderName);
      sync.setRecursive(jobs[i].recursive);
      sync.setNoRecursion(jobs[i].noRecursion);
      sync.addListener(new Reporter(out));
      sync.run();
    }
  }



  private static Pattern[]
  toPattern(String[] patterns)
  {
    Pattern[]	result = new Pattern[patterns.length];

    for (int i = 0; i < patterns.length; ++i)
    {
      result[i] =
        Pattern.compile
        (
          patterns[i].startsWith("[") && patterns[i].endsWith("]") ?
            patterns[i].substring(1, patterns[i].length() - 1) :
            be.re.util.Util.patternToRegexp(patterns[i])
        );
    }

    return result;
  }



  private boolean
  tryRenamedFrom(Record record, File file, Context context) throws SQLException
  {
    String	oldPath = getRemotePathFromETag(record.etag, context);

    if (oldPath != null && getFileLastModified(file, context) == -1)
                           // Exists already because of multiple bindings.
    {
      File	oldFile = getFile(context.local, oldPath);

      if (file.exists() && !oldFile.exists())
      {
        return true; // It was renamed already.
      }

      boolean	result = oldFile.exists();

      if (result)
      {
        for (Listener listener: listeners)
        {
          listener.startRename(oldFile, file);
        }

        file.getParentFile().mkdirs();
        result = oldFile.renameTo(file);

        if (result)
        {
          renameFile(oldFile, file, context);

          for (Listener listener: listeners)
          {
            listener.endRename(oldFile, file);
          }
        }
      }

      return result;
    }

    return false;
  }



  private Record
  tryRenamedTo(String oldPath, File file, Context context) throws SQLException
  {
    String	etag = getETagFromRemotePath(oldPath, context);
    String	newPath = etag != null ? context.etagToPath.get(etag) : null;

    if (newPath != null)
    {
      File	newFile = getFile(context.local, newPath);

      if (getFileLastModified(newFile, context) != -1)
      {
        return null; // Exists already because of multiple bindings.
      }

      if (newFile.exists() && !file.exists())
      {
        return getRecord(context.tree, newPath); // It was already renamed.
      }

      boolean	result = file.exists();

      if (result)
      {
        for (Listener listener: listeners)
        {
          listener.startRename(file, newFile);
        }

        newFile.getParentFile().mkdirs();
        result = file.renameTo(newFile);

        if (result)
        {
          renameFile(file, newFile, context);

          for (Listener listener: listeners)
          {
            listener.endRename(file, newFile);
          }
        }
      }

      return result ? getRecord(context.tree, newPath) : null;
    }

    return null;
  }



  private boolean
  updateConflict(File file)
  {
    boolean	objection = false;

    for (Listener listener: listeners)
    {
      objection |= !listener.updateConflict(file);
    }

    return !objection;
  }



  private boolean
  updateConflict(URL url)
  {
    boolean	objection = false;

    for (Listener listener: listeners)
    {
      objection |= !listener.updateConflict(url);
    }

    return !objection;
  }



  private boolean
  updateConflict(File file, URL url)
  {
    for (Listener listener: listeners)
    {
      listener.updateConflict(file, url);
    }

    return false;
  }



  private static void
  usage(int exitCode)
  {
    System.err.println
    (
      "be.re.webdav.cmd.Sync [-h] [-i seconds] [-reset] " +
        "(-c config_file | [(-r|-no-recursion)] [-rename] " +
        "[-rename-depth number] [-hidden-folder] " +
        "[-e comma_separated_patterns] (-down|-up|-bi) -u URL -d directory)"
    );

    //System.exit(exitCode);
  }



  private static boolean
  validateUrl(String s)
  {
    try
    {
      return
        be.re.net.Util.isUrl(s) &&
          (
            "http".equals(new URL(s).getProtocol())	||
              "https".equals(new URL(s).getProtocol())
          );
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e);
    }
  }



  public static void
  writeFilename(URL remote, File local, File file, File database)
    throws IOException
  {
    Context	context = null;

    try
    {
      context = new Context(local, remote, database);
      writeFilename(file, context);
      writeUrl(getUrl(remote, local, file), context);
      setSynchronization(context);
    }

    catch (SQLException e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      release(context);
    }
  }



  private static void
  writeFilename(File file, Context context) throws SQLException
  {
    writeFilename(file, -1, context);
  }



  private static void
  writeFilename(File file, long time, Context context) throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          getFileLastModified(file, context) != -1 ?
            (
              "update file set last_modified = ? " +
                "where filename = ? and local = ? and remote = ? "
            ) :
            (
              "insert into file (last_modified, filename, local, remote) " +
                "values (?, ?, ?, ?)"
            )
        );

      statement.setLong(1, time != -1 ? time : file.lastModified());
      statement.setString(2, file.getAbsolutePath());
      statement.setString(3, context.local.getAbsolutePath());
      statement.setString(4, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  private static void
  writeRemotePath(Record record, Context context) throws SQLException
  {
    writeRemotePath(record.path, record.lastModified, record.etag, context);
  }



  private static void
  writeRemotePath(String path, long lastModified, String etag, Context context)
    throws SQLException
  {
    PreparedStatement	statement = null;

    try
    {
      statement =
        context.connection.prepareStatement
        (
          getRemotePathLastModified(path, context) != -1 ?
            (
              "update remote_path set last_modified = ?, etag = ? " +
                "where path = ? and local = ? and remote = ? "
            ) :
            (
              "insert into remote_path " +
                "(last_modified, etag, path, local, remote) " +
                "values (?, ?, ?, ?, ?)"
            )
        );

      statement.setLong(1, lastModified);
      statement.setString(2, etag != null ? etag : "none");
      statement.setString(3, path);
      statement.setString(4, context.local.getAbsolutePath());
      statement.setString(5, context.remote.toString());
      statement.execute();
    }

    finally
    {
      release(statement);
    }
  }



  public static void
  writeUrl(URL remote, File local, URL url, File database)
    throws IOException
  {
    writeUrl(remote, local, url, 0, database);
  }



  public static void
  writeUrl(URL remote, File local, URL url, long time, File database)
    throws IOException
  {
    Context	context = null;

    try
    {
      context = new Context(local, remote, database);
      writeFilename(getFile(remote, local, url), context);
      writeUrl(url, time, context);
      setSynchronization(context);
    }

    catch (SQLException e)
    {
      throw new be.re.io.IOException(e);
    }

    finally
    {
      release(context);
    }
  }



  private static void
  writeUrl(URL url, Context context) throws IOException, SQLException
  {
    writeUrl(url, 0, context);
  }



  private static void
  writeUrl(URL url, long time, Context context) throws IOException, SQLException
  {
    Headers	headers = getHeaders(url);

    // It may be that the Last-Modified header isn't yet updated, in which case
    // the time argument will win. The time argument corresponds to the Date
    // header of the PUT response.

    writeRemotePath
    (
      getPath(url, context.remote),
      Math.max
      (
        cutMillis(time),
        headers.get("Last-Modified").length > 0 ?
          cutMillis(be.re.net.Util.httpDate(headers.get("Last-Modified")[0])) :
          -1
      ),
      headers.get("ETag").length > 0 ? headers.get("ETag")[0] : null,
      context
    );
  }



  /**
   * When only one of <code>file</code> and <code>url</code> is set it means
   * that it should be deleted, otherwise both should be updated.
   */

  public static class Conflict

  {

    private File		file;
    private File		local;
    private URL			remote;
    private ConflictType	type;
    private URL			url;



    public
    Conflict(URL remote, File local, File file, URL url, ConflictType type)
    {
      this.remote = remote;
      this.local = local;
      this.file = file;
      this.url = url;
      this.type = type;
    }



    public boolean
    equals(Object o)
    {
      return
        o instanceof Conflict &&
          file.getAbsolutePath().equals(((Conflict) o).file.getAbsolutePath())
          && url.toString().equals(((Conflict) o).url.toString());
    }



    public File
    getFile()
    {
      return file;
    }



    public File
    getLocal()
    {
      return local;
    }



    public URL
    getRemote()
    {
      return remote;
    }



    public ConflictType
    getType()
    {
      return type;
    }



    public URL
    getUrl()
    {
      return url;
    }



    public int
    hashCode()
    {
      return file.getAbsolutePath().hashCode() + url.toString().hashCode();
    }

  } // Conflict



  private static class Context

  {

    private Set<String>		checkedOut;
    private List<Conflict>	conflicts = new ArrayList<Conflict>();
    private Connection		connection;
    private String		depth = "infinity";
    private Direction		direction;
    private Map<String,String>	etagToPath = new HashMap<String,String>();
    private long		lastSynchronization = -1;
    private File		local;
    private boolean		noRecursion;
    private URL			remote;
    private Record		tree;



    private
    Context(File local, URL remote, File database)
      throws IOException, SQLException
    {
      connection = getConnection(database);
      this.remote = remote;
      this.local = local;
    }



    private void
    close() throws SQLException
    {
      if (connection != null)
      {
        connection.close();
      }
    }

  } // Context



  public interface Filter

  {

    /**
     * Return <code>true</code> if the particular synchronization should be
     * excluded. This can be, for example, a transient condition.
     */

    public boolean	exclude	(File file, URL url);

  } // Filter



  private static class Job

  {

    private Node	acl;
    private Direction	direction;
    private File	directory;
    private String[]	excludes;
    private String	hiddenFolderName;
    private boolean	noRecursion;
    private boolean	recursive;
    private boolean	rename;
    private int		renameDepth = 10;
    private URL		url;

  } // Job



  public interface Listener

  {

    /**
     * Called when the local file should be deleted in a conflict.
     * Return <code>false</code> to object.
     */

    public boolean	deleteConflict	(File file);

    /**
     * Called when the URL should be deleted in a conflict.
     * Return <code>false</code> to object.
     */

    public boolean	deleteConflict	(URL url);

    /**
     * Called when the synchronization has ended.
     */

    public void		end		();

    /**
     * Called when the URL has been created.
     */

    public void		endCreate	(URL url);

    /**
     * Called when the local file has been created.
     */

    public void		endCreate	(File file);

    /**
     * Called when the URL has been deleted.
     */

    public void		endDelete	(URL url);

    /**
     * Called when the local file has been deleted.
     */

    public void		endDelete	(File file);

    /**
     * Called when the local file has been renamed.
     */

    public void		endRename	(File from, File to);

    /**
     * Called when the URL has been updated.
     */

    public void		endUpdate	(URL url);

    /**
     * Called when the local file has been updated.
     */

    public void		endUpdate	(File file);

    /**
     * Called when the server returns a response containing an error report.
     */

    public void		error		(Client.Response response)
					  throws IOException;

    /**
     * Called when the server returns a response containing an error report.
     */

    public void		error		(URL url, String method, int statusCode)
					  throws IOException;

    /**
     * Called when an exception occurred when accessing the URL.
     */

    public void		exception	(URL url, String method, Throwable e)
					  throws IOException;

    /**
     * Called when the synchronization is about to start.
     */

    public void		start		();

    /**
     * Called when the local file is going to be created.
     */

    public void		startCreate	(File file);

    /**
     * Called when the URL is going to be created.
     */

    public void		startCreate	(URL url);

    /**
     * Called when the local file is going to be deleted.
     */

    public void		startDelete	(File file);

    /**
     * Called when the URL is going to be deleted.
     */

    public void		startDelete	(URL url);

    /**
     * Called when the local file is going to be renamed.
     */

    public void		startRename	(File from, File to);

    /**
     * Called when the local file is going to be updated.
     */

    public void		startUpdate	(File file);

    /**
     * Called when the URL is going to be updated.
     */

    public void		startUpdate	(URL url);

    /**
     * Called when the local file should be updated in a conflict.
     * Return <code>false</code> to object.
     */

    public boolean	updateConflict	(File file);

    /**
     * Called when the URL should be updated in a conflict.
     * Return <code>false</code> to object.
     */

    public boolean	updateConflict	(URL url);

    /**
     * Called when both sides have been modified when a bidirectional
     * synchronization occurs.
     */

    public void		updateConflict	(File file, URL url);

  } // Listener



  /**
   * Does nothing but agreeing to all proposed actions.
   */

  public static class ListenerAdapter implements Listener

  {

    public boolean
    deleteConflict(File file)
    {
      return true;
    }



    public boolean
    deleteConflict(URL url)
    {
      return true;
    }



    public void
    end()
    {
    }



    public void
    endCreate(URL url)
    {
    }



    public void
    endCreate(File file)
    {
    }



    public void
    endDelete(URL url)
    {
    }



    public void
    endDelete(File file)
    {
    }



    public void
    endRename(File from, File to)
    {
    }



    public void
    endUpdate(URL url)
    {
    }



    public void
    endUpdate(File file)
    {
    }



    public void
    error(Client.Response response) throws IOException
    {
      be.re.webdav.Util.throwException(response);
    }



    public void
    error(URL url, String method, int statusCode) throws IOException
    {
      be.re.webdav.Util.throwException(url, method, statusCode);
    }



    public void
    exception(URL url, String method, Throwable e) throws IOException
    {
      throw new be.re.io.IOException(e);
    }



    public void
    start()
    {
    }



    public void
    startCreate(File file)
    {
    }



    public void
    startCreate(URL url)
    {
    }



    public void
    startDelete(File file)
    {
    }



    public void
    startDelete(URL url)
    {
    }



    public void
    startRename(File from, File to)
    {
    }



    public void
    startUpdate(File file)
    {
    }



    public void
    startUpdate(URL url)
    {
    }



    public boolean
    updateConflict(File file)
    {
      return true;
    }



    public boolean
    updateConflict(URL url)
    {
      return true;
    }



    public void
    updateConflict(File file, URL url)
    {
    }

  } // ListenerAdapter



  private static class Record

  {

    private String		contentType;
    private boolean		done;
    private String		etag;
    private long		lastModified;
    private Map<String,Record>	members = new TreeMap<String,Record>();
    private Record		parent;
    private String		path;

  } // Record



  public static class Reporter extends ListenerAdapter

  {

    private boolean	interactive;
    private PrintWriter	writer;



    /**
     * If <code>out</code> is <code>null</code> the reporter will be
     * interactive.
     */

    public
    Reporter(OutputStream out)
    {
      writer =
        new PrintWriter
        (
          out != null ?
            new OutputStreamWriter(out) :
            InteractiveWriter.open
            (
              be.re.gui.util.Util.getResource("title_execution_report")
            )
        );

      Util.setReportWriter(writer);
      interactive = out == null;
    }



    private boolean
    conflict(File file, String messageKey)
    {
      boolean	result =
        interactive ?
          new YesOrNo
          (
            MessageFormat.format
            (
              Util.getResource(messageKey),
              new Object[]{file.getAbsolutePath()}
            )
          ).ask() : false;

      if (!result)
      {
        report(file, Util.getResource("label_sync_conflict"));
      }

      return result;
    }



    private boolean
    conflict(URL url, String messageKey)
    {
      boolean	result =
        interactive ?
          new YesOrNo
          (
            MessageFormat.format
            (
              Util.getResource(messageKey),
              new Object[]{be.re.net.Util.stripUserInfo(url).toString()}
            )
          ).ask() : false;

      if (!result)
      {
        report(url, Util.getResource("label_sync_conflict"));
      }

      return result;
    }



    public boolean
    deleteConflict(File file)
    {
      return conflict(file, "msg_sync_delete");
    }



    public boolean
    deleteConflict(URL url)
    {
      return conflict(url, "msg_sync_delete");
    }



    public void
    end()
    {
      writer.println(Util.getResource("msg_done"));
      writer.flush();
    }



    private void
    report(File member, String operation)
    {
      writer.println(operation + ": " + member.getAbsolutePath());
      writer.flush();
    }



    private void
    report(URL url, String operation)
    {
      writer.
        println(operation + " " + be.re.net.Util.stripUserInfo(url).toString());
      writer.flush();
    }



    private void
    report(URL url, Exception e)
    {
      url = be.re.net.Util.stripUserInfo(url);

      if (interactive)
      {
        try
        {
          Util.report(url, e);
        }

        catch (IOException ex)
        {
          throw new RuntimeException(ex);
        }
      }
      else
      {
        writer.println
        (
          url.toString() + ": " + Util.getResource("ERROR") + ": " +
            e.getMessage()
        );
      }
    }



    public void
    startCreate(File file)
    {
      report(file, Util.getResource("label_sync_create"));
    }



    public void
    startCreate(URL url)
    {
      report(url, Util.getResource("label_sync_create"));
    }



    public void
    startDelete(File file)
    {
      report(file, Util.getResource("label_sync_delete"));
    }



    public void
    startDelete(URL url)
    {
      report(url, Util.getResource("label_sync_delete"));
    }



    public void
    startRename(File from, File to)
    {
      writer.println
      (
        Util.getResource("label_sync_rename") + ": " +
          from.getAbsolutePath() + " -> " + to.getAbsolutePath()
      );

      writer.flush();
    }



    public void
    startUpdate(File file)
    {
      report(file, Util.getResource("label_sync_update"));
    }



    public void
    startUpdate(URL url)
    {
      report(url, Util.getResource("label_sync_update"));
    }



    public boolean
    updateConflict(File file)
    {
      return conflict(file, "msg_sync_update_conflict");
    }



    public boolean
    updateConflict(URL url)
    {
      return conflict(url, "msg_sync_update_conflict");
    }



    public void
    updateConflict(File file, URL url)
    {
      report(file, Util.getResource("label_sync_conflict"));
    }

  } // Reporter

} // Sync
