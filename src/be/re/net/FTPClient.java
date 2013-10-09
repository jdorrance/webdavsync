package be.re.net;

import be.re.cache.BasicMedium;
import be.re.cache.Cache;
import be.re.cache.LRUCache;
import be.re.gui.util.InteractiveAuthenticator;
import be.re.io.Tracer;
import be.re.pool.ConnectionPool;
import be.re.pool.Resource;
import be.re.pool.ResourceAdapter;
import be.re.pool.ResourceException;
import be.re.pool.ResourceFactory;
import be.re.util.Array;
import be.re.util.Equal;
import be.re.util.PBEException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;



/**
 * This class implements a FTP connection. Connections should be obtained by
 * calling <code>connect</code>.
 * @author Werner Donn\u00e9
 */

public class FTPClient

{

  public static final String	ASCII = "A N";
  public static final String	ASCII_ASA = "A C";
  public static final String	ASCII_NON_PRINT = "A N";
  public static final String	ASCII_TELNET = "A T";
  public static final String	BLOCK = "B";
  public static final String	COMPRESSED = "C";
  public static final String	EBCDIC = "E N";
  public static final String	EBCDIC_ASA = "E C";
  public static final String	EBCDIC_NON_PRINT = "E N";
  public static final String	EBCDIC_TELNET = "E T";
  public static final String	IMAGE = "I";
  public static final String	LOCAL = "L";
  public static final String	STREAM = "S";

  private static final int	CACHE_SIZE = 1000;
  private static final int	EQUAL_CONNECTIONS = 5;
  private static final int	POOL_SIZE = 50;
  private static final long	TIME_DELTA = 60000;

  private static Equal		equivalenceClass =
    new Equal()
    {
      public boolean
      equal(Object object, Object refData)
      {
        return ((Connection) object).type.equals((ConnectionType) refData);
      }
    };
  private static ConnectionPool	pool =
    new ConnectionPool
    (
      new ResourceFactory()
      {
        public ResourceAdapter
        newInstance(Object criterion) throws ResourceException
        {
          try
          {
            return new Connection((ConnectionType) criterion);
          }

          catch (IOException e)
          {
            throw new ResourceException(e);
          }
        }
      },
      POOL_SIZE,
      equivalenceClass,
      EQUAL_CONNECTIONS,
      true
    );
  private static Authenticate	authenticator;
  private Resource		resourceFromPool;
  private static Tracer		tracer = initTrace();



  static
  {
    try
    {
      authenticator =
        new URLAuthenticator
        (
          new BasicAuthenticator(new InteractiveAuthenticator())
        );
    }

    catch (PBEException e)
    {
      throw new RuntimeException(e);
    }
  }



  private
  FTPClient()
  {
  }



  public OutputStream
  append(String pathName) throws IOException, ProtocolException
  {
    getConnection().client = this; // Automatic release moves to stream.

    return getConnection().append(pathName);
  }



  private static String
  canonicalPathName(String pathName)
  {
    return
      pathName.length() > 1 && pathName.charAt(pathName.length() - 1) == '/' ?
        pathName.substring(0, pathName.length() - 1) : pathName;
  }



  public void
  close() throws IOException
  {
    if (resourceFromPool != null)
    {
      try
      {
        resourceFromPool.release();
        resourceFromPool = null;
      }

      catch (ResourceException e)
      {
        if (e.getCause() instanceof IOException)
        {
          throw (IOException) e.getException();
        }

        throw new RuntimeException(e);
      }
    }
  }



  public static FTPClient
  connect(String resource) throws IOException, ProtocolException
  {
    return connect(resource, null, null);
  }



  public static FTPClient
  connect(String resource, String username)
    throws IOException, ProtocolException
  {
    return connect(resource, username, null);
  }



  public static FTPClient
  connect(String resource, String username, String password)
    throws IOException, ProtocolException
  {
    if (username == null || password == null)
    {
      Authenticate	auth = getAuthenticator();

      if (auth != null)
      {
        if (username ==  null)
        {
          User	user =
            auth.getUser
            (
              removeType(resource),
              "ftp",
              new BasicUser("anonymous", "here@there.com")
            );

          if (user != null)
          {
            username = user.getUsername();
            password = user.getPassword();
          }
        }
        else
        {
          auth.getPassword(resource, "ftp", username);
        }
      }
    }

    try
    {
      FTPClient	client = new FTPClient();
      Resource	resourceFromPool =
        pool.get
        (
          client,
          equivalenceClass,
          new ConnectionType(resource, username, password)
        );

      // Make sure it isn't collected and hence released. We manage release
      // ourselves.
      client.resourceFromPool = resourceFromPool;

      return client;
    }

    catch (ResourceException e)
    {
      throwException(e);

      return null;
    }
  }



  private static String[]
  createChildPathNames(String pathName, String[] nameList)
  {
    List	pathNames = new ArrayList();

    for (int i = 0; i < nameList.length; ++i)
    {
      if (!pathName.equals(nameList[i])) // Avoid false recursion.
      {
        pathNames.add
        (
          nameList[i].length() == 0 || nameList[i].charAt(0) != '/' ?
            (
              pathName +
                (
                  pathName.length() > 0 &&
                    pathName.charAt(pathName.length() - 1) == '/' ?
                    "" : "/"
                ) + nameList[i]
            ) : nameList[i]
        );
      }
    }

    return (String[]) pathNames.toArray(new String[pathNames.size()]);
  }



  public void
  delete(String pathName) throws IOException, ProtocolException
  {
    getConnection().delete(pathName);
  }



  private static void
  endStream(FTPClient client) throws IOException
  {
    try
    {
      client.getConnection().mustClose = true;
      pool.release(client, client.getConnection());
    }

    catch (ResourceException e)
    {
      if (e.getException() instanceof IOException)
      {
        throw (IOException) e.getException();
      }

      throw new RuntimeException(e);
    }
  }



  public boolean
  exists(String pathName) throws IOException
  {
    return getConnection().exists(pathName);
  }



  private static String[]
  extractPathNames(String s)
  {
    StringTokenizer	tokenizer = new StringTokenizer(s, " ");
    int			count = tokenizer.countTokens();

    for (int i = 0; i < count - 3; ++i)
    {
      tokenizer.nextToken();
    }

    String[]	result = new String[2];

    result[0] = tokenizer.nextToken();
    tokenizer.nextToken();
    result[1] = tokenizer.nextToken();

    return result;
  }



  protected void
  finalize() throws Throwable
  {
    close();
  }



  public static Authenticate
  getAuthenticator()
  {
    return authenticator;
  }



  private Connection
  getConnection()
  {
    return (Connection) resourceFromPool.getConnection();
  }



  public static Tracer
  getTracer()
  {
    return tracer;
  }



  private static HashMap
  hashListLines(String[] lines)
  {
    HashMap	result = new HashMap(1001);

    for (int i = 0; i < lines.length; ++i)
    {
      String    name = lines[i].substring(lines[i].lastIndexOf(' ') + 1);

      result.put(name.substring(name.lastIndexOf('/') + 1), lines[i]);
    }

    return result;
  }



  private static Tracer
  initTrace()
  {
    return
      System.getProperty("be.re.ftp.trace") != null ?
        Tracer.getDefault() : null;
  }



  private static boolean
  isBadCode(String code, String[] expectedCodes)
  {
    return
      (expectedCodes != null && !Array.inArray(expectedCodes, code)) ||
        (
          expectedCodes == null &&
            (code.charAt(0) == '4' || code.charAt(0) == '5')
        );
  }



  private static boolean
  isDecimalDigit(char c)
  {
    return
      Character.getNumericValue(c) >= 0 &&
        Character.getNumericValue(c) <= 9;
  }



  public boolean
  isDirectory(String pathName) throws IOException, ProtocolException
  {
    return getConnection().isDirectory(pathName);
  }



  private static boolean
  isMultiLineReply(String reply)
  {
    return
      reply.length() >= 4 &&
        isDecimalDigit(reply.charAt(0)) && isDecimalDigit(reply.charAt(1)) &&
        isDecimalDigit(reply.charAt(2)) && reply.charAt(3) == '-';
  }



  public void
  makeDirectory(String pathName) throws IOException, ProtocolException
  {
    getConnection().makeDirectory(pathName);
  }



  public long
  modifiedTime(String pathName) throws IOException, ProtocolException
  {
    return getConnection().modifiedTime(pathName);
  }



  public String[]
  nameList(String pathName) throws IOException, ProtocolException
  {
    return getConnection().nameList(pathName);
  }



  /**
   * @return the first element in a row is the path name, the second is the
   * detail string.
   */

  public String[][]
  nameListWithDetails(String pathName) throws IOException, ProtocolException
  {
    return getConnection().nameListWithDetails(pathName);
  }



  public void
  removeDirectory(String pathName) throws IOException, ProtocolException
  {
    getConnection().removeDirectory(pathName);
  }



  public static String
  removeType(String pathName)
  {
    return pathName.replaceAll(";type=[aid]", "");
  }



  public void
  rename(String pathNameFrom, String pathNameTo)
    throws IOException, ProtocolException
  {
    getConnection().rename(pathNameFrom, pathNameTo);
  }



  public InputStream
  retrieve(String pathName) throws IOException, ProtocolException
  {
    getConnection().client = this; // Automatic release moves to stream.

    return getConnection().retrieve(pathName);
  }



  private static boolean
  sameHosts(String r1, String r2)
  {
    try
    {
      return
        (Util.isUrl(r1) ? new URL(r1).getHost() : r1).
          equals(Util.isUrl(r2) ? new URL(r2).getHost() : r2);
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  public static void
  setAuthenticator(Authenticate value)
  {
    authenticator = value;
  }



  public void
  setMode(String mode) throws IOException, ProtocolException
  {
    getConnection().setMode(mode);
  }



  public static void
  setTracer(Tracer value)
  {
    tracer = value;
  }



  public void
  setType(String type) throws IOException, ProtocolException
  {
    getConnection().setType(type);
  }



  public int
  size(String pathName) throws IOException, ProtocolException
  {
    return getConnection().size(pathName);
  }



  public String[]
  status(String pathName) throws IOException, ProtocolException
  {
    return getConnection().status(pathName);
  }



  public OutputStream
  store(String pathName) throws IOException, ProtocolException
  {
    getConnection().client = this; // Automatic release moves to stream.

    return getConnection().store(pathName);
  }



  private static void
  trace(Connection connection, String s) throws IOException
  {
    Tracer	tr = getTracer();

    if (tr != null)
    {
      tr.write((connection.toString() + ": " + s + "\n").getBytes());
    }
  }



  private static void
  throwException(Exception e) throws IOException, ProtocolException
  {
    if (e instanceof ProtocolException)
    {
      throw (ProtocolException) e;
    }

    if (e instanceof IOException)
    {
      throw (IOException) e;
    }

    if (e.getCause() instanceof ProtocolException)
    {
      throw (ProtocolException) e.getCause();
    }

    if (e.getCause() instanceof IOException)
    {
      throw (IOException) e.getCause();
    }

    throw new RuntimeException(e);
  }



  public static class Connection implements ResourceAdapter

  {

    private Socket		data;
    private FTPClient		client = null;
    private Socket		control;
    private BufferedReader	controlIn;
    private Writer		controlOut;
    private String		currentTransferType;
    private Cache		existsCache = null;
    private Cache		isDirectoryCache = null;
    private String		mode = STREAM;
    private boolean		mustClose = false;
    private long		timeStamp;
    private ConnectionType	type;



    private
    Connection(ConnectionType type)
      throws IOException, MalformedURLException, ProtocolException
    {
      this.type = type;

      control =
        Util.isUrl(type.resource) ?
          new Socket
          (
            new URL(type.resource).getHost(),
            new URL(type.resource).getPort() != -1 ?
              new URL(type.resource).getPort() : 21
          ) :
          new Socket(type.resource, 21);

      controlIn =
        new BufferedReader
        (
          new InputStreamReader(control.getInputStream(), "ASCII")
        );

      controlOut = new OutputStreamWriter(control.getOutputStream());
      checkReply(new String[] {"220"});
      login();

      try
      {
        setMode(BLOCK);
      }

      catch (ProtocolException e1)
      {
        // Some servers don't implement MODE. S is assumed.
      }

      setType(IMAGE);
      prepareCaches();
      timeStamp = new Date().getTime();
    }



    public void
    acquired()
    {
    }



    private OutputStream
    append(String pathName)
      throws IOException, ProtocolException
    {
      return write(pathName, "APPE");
    }



    private void
    checkDataConnection(String reply) throws IOException
    {
      String	code;

      if
      (
        data != null							   &&
        (
          mode.equals(STREAM)						   ||
          (
            reply != null						   &&
            (
              "226".
                equals(code = new StringTokenizer(reply, " ").nextToken()) ||
              "250".equals(code)
            )
          )
        )
      )
      {
        data.close();
        data = null;
      }
    }



    private String
    checkReply(String[] expectedCodes) throws IOException, ProtocolException
    {
      return checkReply(expectedCodes, null);
    }



    private String
    checkReply(String[] expectedCodes, List lines)
      throws IOException, ProtocolException
    {
      String	reply = controlIn.readLine();

      if (reply == null)
      {
        throw
          new ProtocolException
          (
            Util.getResource("ftp_connection_closed_error")
          );
      }

      trace(this, reply);

      if (isMultiLineReply(reply))
      {
        reply = readUntilRealReply(reply, lines);
      }

      StringTokenizer	tokenizer = new StringTokenizer(reply, " ");

      if (!tokenizer.hasMoreTokens())
      {
        throw
          new ProtocolException(Util.getResource("ftp_unknown_reply_error"));
      }

      String	token = tokenizer.nextToken();

      if (isBadCode(token, expectedCodes))
      {
        trace(this, "Bad token " + token);

        throw new ProtocolException(Integer.parseInt(token), reply);
      }

      return reply;
    }



    public void
    close() throws ResourceException
    {
      try
      {
        control.close();
        control = null;

        if (data != null)
        {
          data.close();
          data = null;
        }
      }

      catch (IOException e)
      {
        throw new ResourceException(e);
      }
    }



    private void
    controlWrite(String s) throws IOException
    {
      controlOut.write(s + "\r\n");
      controlOut.flush();
      trace(this, s);
    }



    private void
    delete(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        simpleCommand(pathName, "DELE", new String[] {"250"});
        existsCache.put(canonicalPathName(pathName), new Boolean(false));
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);
      }
    }



    private boolean
    doesExist(String pathName) throws IOException
    {
      try
      {
        // Avoid actual transmission of name list in case of directories.

        if (isDirectory(pathName))
        {
          return true;
        }

        String[]	names = listQuery(pathName, "NLST");

        return names.length > 0 && names[0].equals(pathName);
      }

      catch (ProtocolException e)
      {
        if (e.getCode() == 450 || e.getCode() == 550)
        {
          return false;
        }

        throw e;
      }
    }



    private void
    destroyIfBad(Throwable e) throws IOException
    {
      if (e instanceof ProtocolException)
      {
        int	code = ((ProtocolException) e).getCode();

        if (code == 550)
        {
          existsCache.flush();
          isDirectoryCache.flush();
        }

        int[]	badCodes = {332, 421, 425, 426, 500, 501, 503, 530, 532};

        for (int i = 0; !mustClose && i < badCodes.length; ++i)
        {
          if (code == badCodes[i])
          {
            mustClose = true;
          }
        }
      }
      else
      {
        mustClose = true;
      }
    }



    private boolean
    exists(String pathName) throws IOException
    {
      pathName = removeType(pathName);

      try
      {
        return
          ((Boolean) existsCache.get(canonicalPathName(pathName))).
            booleanValue();
      }

      catch (Exception e)
      {
        destroyIfBad(e);

        if (e instanceof IOException)
        {
          throw (IOException) e;
        }

        throw new RuntimeException(e);
      }
    }



    private String[]
    findList(String pathName) throws IOException, ProtocolException
    {
      String[]	list = listQuery(pathName, "LIST");

      // Follow soft links.

      if (list.length == 1 && list[0].length() > 1 && list[0].charAt(0) == 'l')
      {
        String[]	pathNames = extractPathNames(list[0]);

        return
          findList
          (
            pathNames[1].charAt(0) == '/' ?
              pathNames[1] :
              (
                pathNames[0].substring
                (
                  0,
                  pathNames[0].charAt(pathNames[0].length() - 1) == '/' ?
                    pathNames[0].substring(0, pathNames[0].length() - 1).
                      lastIndexOf('/') + 1:
                    pathNames[0].lastIndexOf('/') + 1
                ) + pathNames[1]
              )
          );
      }

      return list;
    }



    public Object
    getConnection()
    {
      return this;
    }



    private boolean
    isActuallyDirectory(String pathName) throws IOException, ProtocolException
    {
      try
      {
        simpleCommand(pathName, "CWD", new String[] {"250"});
        return true;
      }

      catch (ProtocolException e)
      {
        if (e.getCode() == 550)
        {
          return false;
        }

        throw e;
      }
    }



    public boolean
    isAlive()
    {
      long	newTimeStamp = new Date().getTime();

      if (newTimeStamp - timeStamp <= TIME_DELTA)
      {
        timeStamp = newTimeStamp;
        return true;
      }

      try
      {
        controlWrite("NOOP");
        checkReply(new String[] {"200"});
        timeStamp = new Date().getTime();
        return true;
      }

      catch (Exception e)
      {
        return false;
      }
    }



    private boolean
    isDirectory(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        return
          ((Boolean) isDirectoryCache.get(canonicalPathName(pathName))).
            booleanValue();
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return false;
      }
    }



    private String[]
    listQuery(String pathName, String command)
      throws IOException, ProtocolException
    {
      String    reply = null;

      setDataConnection();

      try
      {
        controlWrite(command + " " + pathName);
        checkReply(new String[] {"125", "150"});

        List		names = new ArrayList();
        BufferedReader	reader =
          new BufferedReader
          (
            new InputStreamReader
            (
              mode.equals(STREAM) ?
                data.getInputStream() :
                new FTPBlockModeInputStream(data.getInputStream()),
              "ASCII"
            )
          );
        String		s;

        while ((s = reader.readLine()) != null)
        {
          if (!".".equals(s) && !"..".equals(s)) // Some servers return that.
          {
            names.add(s);
          }
        }

        if (mode.equals(STREAM))
        {
          data.close();
          data = null;
        }

        reply = checkReply(new String[] {"226", "250"});

        return (String[]) names.toArray(new String[0]);
      }

      catch (ProtocolException e)
      {
        if (e.getCode() == 550 && isDirectory(pathName))
        {
          return new String[0];
        }

        throw e;
      }

      finally
      {
        checkDataConnection(reply);
      }
    }



    private void
    login() throws IOException, ProtocolException
    {
      if (type.username == null || type.password == null)
      {
        throw new ProtocolException(530, Util.getResource("ftp_530"));
      }

      try
      {
        controlWrite("USER " + type.username);
        checkReply(new String[] {"230", "331", "332"});
        controlWrite("PASS " + type.password);
        checkReply(new String[] {"230"});
      }

      catch (ProtocolException e)
      {
        if (e.getCode() == 530 && getAuthenticator() != null)
        {
          getAuthenticator().badUser
          (
            type.resource,
            "ftp",
            new BasicUser(type.username, type.password)
          );

          User	user = getAuthenticator().getUser(type.resource, "ftp");

          if
          (
            user != null		&&
            user.getUsername() != null	&&
            user.getPassword() != null
          )
          {
            type.username = user.getUsername();
            type.password = user.getPassword();
            login();

            return;
          }
        }

        throw e;
      }
    }



    private void
    makeDirectory(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        simpleCommand(pathName, "MKD", new String[] {"257"});
        existsCache.put(canonicalPathName(pathName), new Boolean(true));
        isDirectoryCache.put(canonicalPathName(pathName), new Boolean(true));
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);
      }
    }



    private long
    modifiedTime(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        StringTokenizer	tokenizer =
          new StringTokenizer
          (
            simpleCommand(pathName, "MDTM", new String[] {"213"}),
            " "
          );
  
        tokenizer.nextToken();

        return
          tokenizer.hasMoreTokens() ?
            Long.parseLong(tokenizer.nextToken()) : -1;
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return -1;
      }
    }



    public boolean
    mustClose()
    {
      return mustClose;
    }



    private String[]
    nameList(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        String[][]	list = nameList(pathName, false);
        String[]	result = new String[list.length];
  
        for (int i = 0; i < result.length; ++i)
        {
          result[i] = list[i][0];
        }

        return result;
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return null;
      }
    }



    private String[][]
    nameList(String pathName, boolean details)
      throws IOException, ProtocolException
    {
      // The NLST output is the reference. The other list has no standard format
      // and is used to try to pre-fill the directory cache.

      HashMap	list = hashListLines(findList(pathName));
      String[]	nameList = listQuery(pathName, "NLST");
      String[]	pathNames = createChildPathNames(pathName, nameList);
      String[]	detailList = details ? new String[pathNames.length] : null;

      for (int i = 0; i < pathNames.length; ++i)
      {
        existsCache.put(canonicalPathName(pathNames[i]), new Boolean(true));

        String    line =
          (String) list.
            get(pathNames[i].substring(pathNames[i].lastIndexOf('/') + 1));

        if (details)
        {
          detailList[i] = (line != null ? line : "");
        }

        if (line != null && line.length() > 0)
        {
          if (line.charAt(0) == 'd')
          {
            isDirectoryCache.
              put(canonicalPathName(pathNames[i]), new Boolean(true));
          }
          else
          {
            if (line.charAt(0) == '-')
            {
              isDirectoryCache.
                put(canonicalPathName(pathNames[i]), new Boolean(false));
            }
          }
        }
      }

      String[][]	result = new String[pathNames.length][2];

      for (int i = 0; i < result.length; ++i)
      {
        result[i][0] = pathNames[i];
        result[i][1] = details ? detailList[i] : null;
      }

      return result;
    }



    private String[][]
    nameListWithDetails(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        return nameList(pathName, true);
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return null;
      }
    }



    private void
    prepareCaches()
    {
      existsCache =
        new LRUCache
        (
          new BasicMedium()
          {
            public Object
            read(Object key)
            {
              try
              {
                return new Boolean(doesExist((String) key));
              }

              catch (Exception e)
              {
                throw new RuntimeException(e);
              }
            }
          },
          CACHE_SIZE
        );

      isDirectoryCache =
        new LRUCache
        (
          new BasicMedium()
          {
            public Object
            read(Object key)
            {
              try
              {
                return new Boolean(isActuallyDirectory((String) key));
              }

              catch (Exception e)
              {
                throw new RuntimeException(e);
              }
            }
          },
          CACHE_SIZE
        );
    }



    private String
    readUntilRealReply(String reply, List lines)
      throws IOException, ProtocolException
    {
      String	expected = reply.substring(0, 3) + " ";
      String	s;

      while
      (
        !(
          (s = controlIn.readLine()) != null	&&
          s.length() >= 4			&&
          expected.equals(s.substring(0, 4))
        )
      )
      {
        if (s == null)
        {
          throw
            new ProtocolException
            (
              Util.getResource("ftp_connection_closed_error")
            );
        }

        if (lines != null)
        {
          lines.add(s);
        }
      }

      return s;
    }



    private void
    removeDirectory(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        simpleCommand(pathName, "RMD", new String[] {"250"});
        existsCache.put(canonicalPathName(pathName), new Boolean(false));
        isDirectoryCache.put(canonicalPathName(pathName), new Boolean(false));
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);
      }
    }



    private void
    rename(String pathNameFrom, String pathNameTo)
      throws IOException, ProtocolException
    {
      pathNameFrom = removeType(pathNameFrom);
      pathNameTo = removeType(pathNameTo);

      try
      {
        controlWrite("RNFR " + pathNameFrom);
        checkReply(new String[] {"350"});
        controlWrite("RNTO " + pathNameTo);
        checkReply(new String[] {"250"});

        existsCache.put(canonicalPathName(pathNameFrom), new Boolean(false));
        existsCache.put(canonicalPathName(pathNameTo), new Boolean(true));

        if (isDirectory(pathNameFrom))
        {
          isDirectoryCache.
            put(canonicalPathName(pathNameFrom), new Boolean(false));
          isDirectoryCache.
            put(canonicalPathName(pathNameTo), new Boolean(true));
        }
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);
      }
    }



    private InputStream
    retrieve(String pathName) throws IOException, ProtocolException
    {
      setDataConnection();

      try
      {
        String	saveType = currentTransferType;

        setTypeForPathName(pathName);
        pathName = removeType(pathName);
        controlWrite("RETR " + pathName);
        checkReply(new String[] {"125", "150"});

        return
          new RetrieveInputStream
          (
            mode.equals(STREAM) ?
              data.getInputStream() :
              new FTPBlockModeInputStream(data.getInputStream()),
            client,
            saveType
          );
      }
  
      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return null;
      }
    }



    public void
    released()
    {
    }



    private void
    setDataConnection() throws IOException, ProtocolException
    {
      if (data != null)
      {
        return;
      }

      controlWrite("PASV");

      String	reply = checkReply(new String[] {"227"});

      StringTokenizer	tokenizer =
        new StringTokenizer(reply.substring(reply.indexOf('(') + 1), ",)");

      data =
        new Socket
        (
          tokenizer.nextToken() + "." + tokenizer.nextToken() + "." +
            tokenizer.nextToken() + "." + tokenizer.nextToken(),
          (int) (Integer.parseInt(tokenizer.nextToken()) * Math.pow(2, 8)) +
            Integer.parseInt(tokenizer.nextToken())
        );
    }



    private void
    setMode(String mode) throws IOException, ProtocolException
    {
      if (mode.equals(COMPRESSED))
      {
        throw new ProtocolException("Not implemented.");
      }

      try
      {
        controlWrite("MODE " + mode);
        checkReply(new String[] {"200"});
        this.mode = mode;
      }

      catch (Exception e)
      {
        throwException(e);
      }
    }



    private void
    setType(String type) throws IOException, ProtocolException
    {
      try
      {
        controlWrite("TYPE " + type);
        checkReply(new String[] {"200"});
        currentTransferType = type;
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);
      }
    }



    private void
    setTypeForPathName(String pathName) throws IOException, ProtocolException
    {
      if (pathName.endsWith(";type=i") && !currentTransferType.equals(IMAGE))
      {
        setType(IMAGE);
      }
      else
      {
        if (pathName.endsWith(";type=a") && !currentTransferType.equals(ASCII))
        {
          setType(ASCII);
        }
      }
    }



    private String
    simpleCommand(String pathName, String command, String[] expected)
      throws IOException, ProtocolException
    {
      controlWrite(command + " " + pathName);

      return checkReply(expected);
    }



    private int
    size(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        StringTokenizer	tokenizer =
          new StringTokenizer
          (
            simpleCommand(pathName, "SIZE", new String[] {"213"}),
            " "
          );

        tokenizer.nextToken();

        return
          tokenizer.hasMoreTokens() ?
            Integer.parseInt(tokenizer.nextToken()) : -1;
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return -1;
      }
    }



    private String[]
    status(String pathName) throws IOException, ProtocolException
    {
      pathName = removeType(pathName);

      try
      {
        List	lines = new ArrayList();

        controlWrite
        (
          pathName == null || pathName.length() == 0 ?
            "STAT" : ("STAT " + pathName)
        );

        checkReply(new String[] {"211", "212", "213"}, lines);

        return (String[]) lines.toArray(new String[lines.size()]);
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return null;
      }
    }



    private OutputStream
    store(String pathName) throws IOException, ProtocolException
    {
      return write(pathName, "STOR");
    }



    private OutputStream
    write(String pathName, String command) throws IOException, ProtocolException
    {
      setDataConnection();

      try
      {
        String	saveType = currentTransferType;

        setTypeForPathName(pathName);
        pathName = removeType(pathName);
        controlWrite(command + " " + pathName);
        checkReply(new String[] {"125", "150"});
        existsCache.put(canonicalPathName(pathName), new Boolean(true));
        isDirectoryCache.put(canonicalPathName(pathName), new Boolean(false));

        return
          new StoreOutputStream
          (
            mode.equals(STREAM) ?
              data.getOutputStream() :
              new FTPBlockModeOutputStream(data.getOutputStream()),
            client,
            saveType
          );
      }

      catch (Exception e)
      {
        destroyIfBad(e);
        throwException(e);

        return null;
      }
    }

  } // Connection



  private static class ConnectionType

  {

    private String	password;
    private String	resource;
    private String	username;



    private
    ConnectionType(String resource, String username, String password)
    {
      this.resource = resource;
      this.username = username;
      this.password = password;
    }



    public boolean
    equals(Object object)
    {
      return
        object instanceof ConnectionType &&
          sameHosts(((ConnectionType) object).resource, resource) &&
          username.equals(((ConnectionType) object).username);
    }

  } // ConnectionType



  public static class RetrieveInputStream extends FilterInputStream

  {

    private FTPClient	client;
    private boolean     closed = false;
    private String	savedType;



    private
    RetrieveInputStream(InputStream in, FTPClient client, String savedType)
    {
      super(in);
      this.client = client;
      this.savedType = savedType;
    }



    public int
    available() throws IOException
    {
      try
      {
        return super.available();
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;

        try
        {
          super.close();

          client.getConnection().checkDataConnection
          (
            client.getConnection().checkReply(new String[] {"226", "250"})
          );

          if (!savedType.equals(client.getConnection().currentTransferType))
          {
            client.getConnection().setType(savedType);
          }
        }

        finally
        {
          try
          {
            pool.release(client, client.getConnection());
          }

          catch (ResourceException e)
          {
            if (e.getException() instanceof IOException)
            {
              throw (IOException) e.getException();
            }

            throw new RuntimeException(e);
          }
        }
      }
    }



    protected void
    finalize() throws Throwable
    {
      close();
    }



    public int
    read() throws IOException
    {
      int	result;

      try
      {
        result = super.read();
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }

      return result;
    }



    public int
    read(byte[] b, int off, int len) throws IOException
    {
      int	result;

      try
      {
        result = super.read(b, off, len);
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }

      return result;
    }



    public void
    reset(long n) throws IOException
    {
      try
      {
        super.reset();
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }
    }



    public long
    skip(long n) throws IOException
    {
      try
      {
        return super.skip(n);
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }
    }

  } // RetrieveInputStream



  public static class StoreOutputStream extends FilterOutputStream

  {

    private FTPClient	client;
    private boolean     closed = false;
    private String	savedType;



    private
    StoreOutputStream(OutputStream out, FTPClient client, String savedType)
    {
      super(new BufferedOutputStream(out, 0x10000));
      this.client = client;
      this.savedType = savedType;
    }



    public void
    close() throws IOException
    {
      if (!closed)
      {
        closed = true;

        try
        {
          super.close();

          client.getConnection().checkDataConnection
          (
            client.getConnection().checkReply(new String[] {"226", "250"})
          );

          if (!savedType.equals(client.getConnection().currentTransferType))
          {
            client.getConnection().setType(savedType);
          }
        }

        finally
        {
          try
          {
            pool.release(client, client.getConnection());
          }

          catch (ResourceException e)
          {
            if (e.getException() instanceof IOException)
            {
              throw (IOException) e.getException();
            }

            throw new RuntimeException(e);
          }
        }
      }
    }



    protected void
    finalize() throws Throwable
    {
      close();
    }



    public void
    flush() throws IOException
    {
      try
      {
        super.flush();
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }
    }



    public void
    write(int b) throws IOException
    {
      try
      {
        super.write(b);
      }

      catch (IOException e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }
    }



    public void
    write(byte[] b, int off, int len) throws IOException
    {
      try
      {
        super.write(b, off, len);
      }

      catch (IOException e)
      {
        endStream(client);
        throw e;
      }

      catch (Exception e)
      {
        endStream(client);
        throw new RuntimeException(e);
      }
    }

  } // StoreOutputStream

} // FTPClient
