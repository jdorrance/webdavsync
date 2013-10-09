package be.re.net;

import be.re.io.StreamConnector;
import be.re.util.Base64;
import be.re.util.PBE;
import be.re.util.PBEException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;



public class URLAuthenticator implements Authenticate

{

  private File			credentials;
  private Authenticate		delegate;
  private static boolean	globalEncryption;
  private static Tuple[]	globalTuples =
    loadCredentials(getStorage("URLAuthenticator"), null, true);
  private char[]		password;
  private Tuple[]		tuples = new Tuple[0];



  public
  URLAuthenticator()
  {
    this(null, null, null);
  }



  public
  URLAuthenticator(Authenticate delegate)
  {
    this(delegate, null, null);
  }



  /**
   * @param delegate if this authenticator doesn't find what is asked it
   * forwards the request to the delegate. It may be <code>null</code>.
   * @param credentials the file to store the credentials in. If it is
   * <code>null</code> the global credentials will be used.
   * @param password if this is not <code>null</code> the credentials will be
   * encrypted with it when stored.
   */

  public
  URLAuthenticator(Authenticate delegate, File credentials, char[] password)
  {
    this.delegate = delegate;
    this.credentials = credentials;
    tuples =
      credentials != null ?
        loadCredentials(credentials, password, false) : globalTuples;
    this.password = password;
  }



  public void
  badUser(String resource, String protocol, User user)
  {
    if (Util.isUrl(resource))
    {
      try
      {
        List	list = new ArrayList(Arrays.asList(tuples));
        int	index =
          findAuthentication
          (
            list,
            Util.resolvePath(new URL(resource)),
            user.getUsername()
          );

        if (index != -1)
        {
          list.remove(index);
        }

        tuples = (Tuple[]) list.toArray(new Tuple[0]);

        if (delegate != null)
        {
          User	userFromUrl = Util.userFromUrl(new URL(resource));

          if (userFromUrl.getUsername() != null)
          {
            resource =
              Util.setUserInfo
              (
                Util.stripUserInfo(new URL(resource)),
                userFromUrl.getUsername()
              ).toString();
          }

          delegate.badUser(canonic(resource), protocol, user);
        }
      }

      catch (MalformedURLException e)
      {
        throw new RuntimeException(e); // Would be a bug.
      }
    }
    else
    {
      if (delegate != null)
      {
        delegate.badUser(resource, protocol, user);
      }
    }
  }



  private static String
  canonic(String url)
  {
    return
      url.charAt(url.length() - 1) == '/' ?
        url.substring(0, url.length() - 1) : url;
  }



  public void
  clear()
  {
    tuples = new Tuple[0];
  }



  private static String
  decrypt(String s, char[] password, boolean global) throws PBEException
  {
    if (s.startsWith("^@e@^"))
    {
      if (password == null && global)
      {
        globalEncryption = true;
          // Reading the global credentials. Remember it so encryption will be
          // done when saving the list.
      }

      return
        new String
        (
          password != null ?
            PBE.decrypt(Base64.decode(s.substring(5).getBytes()), password) :
            PBE.decrypt(Base64.decode(s.substring(5).getBytes()))
        );
    }

    return s;
  }



  public static synchronized void
  decryptGlobalPasswords() throws IOException, PBEException
  {
    globalEncryption = false;
    saveGlobal();
  }



  private static String
  encrypt(String s, char[] password) throws PBEException
  {
    return
      "^@e@^" +
        new String
        (
          Base64.encode
          (
            password != null ?
              PBE.encrypt(s.getBytes(), password) : PBE.encrypt(s.getBytes())
          )
        );
  }



  public static synchronized void
  encryptGlobalPasswords() throws IOException, PBEException
  {
    globalEncryption = true;
    saveGlobal();
  }



  private static boolean
  equalUrls(URL url1, URL url2)
  {
    return
      url1.getProtocol().equalsIgnoreCase(url2.getProtocol()) &&
        url1.getHost().equalsIgnoreCase(url2.getHost()) &&
        url1.getPort() == url2.getPort() &&
        (
          url1.getFile().equals(url2.getFile()) ||
          url1.getFile().equals(url2.getFile() + "/") ||
          url2.getFile().equals(url1.getFile() + "/")
        );
  }



  /**
   * May modify the tuples list.
   */

  private static int
  findAuthentication(List tuples, URL url, String username)
  {
    // The most precise is found because the array is sorted.

    for (int i = 0; i < tuples.size(); ++i)
    {
      Tuple	tuple = (Tuple) tuples.get(i);

      if
      (
        (
          equalUrls(tuple.url, url)			||
          be.re.net.Util.isAncestor(tuple.url, url)
        )						&&
        (
          username == null				||
          username.equals(tuple.user.getUsername())
        )
      )
      {
        return i;
      }
    }

    // This can avoid that the user has to log in again for a sibling URL.

    int	result = findAuthenticationHost(tuples, url, username);

    if (result != -1)
    {
      Tuple	tuple = (Tuple) tuples.get(result);
      Tuple	copy = new Tuple(url, tuple.user, tuple.persistent);

      copy.passwordPersistent = tuple.passwordPersistent;
      copy.position = tuple.position;
      tuples.add(0, copy);

      return 0;
    }

    return -1;
  }



  private static int
  findAuthenticationHost(List tuples, URL url, String username)
  {
    // The least precise is found because the array is sorted.

    for (int i = tuples.size() - 1; i >= 0; --i)
    {
      Tuple	tuple = (Tuple) tuples.get(i);

      if
      (
        url.getHost().equals(tuple.url.getHost())	&&
        (
          username == null				||
          username.equals(tuple.user.getUsername())
        )
      )
      {
        return i;
      }
    }

    return -1;
  }



  /**
   * Gets persistent authentication entries.
   */

  public static synchronized Entry[]
  getGlobalAuthentications()
  {
    Tuple[]	tuples = select(globalTuples, true);
    Entry[]	entries = new Entry[tuples.length];

    for (int i = 0; i < tuples.length; ++i)
    {
      entries[i] = new Entry(tuples[i].url, tuples[i].user);
    }

    return entries;
  }



  public String
  getPassword(String resource, String protocol, String username)
  {
    if (!Util.isUrl(resource))
    {
      return
        delegate != null ?
          delegate.getPassword(resource, protocol, username) : null;
    }

    try
    {
      List	list = new ArrayList(Arrays.asList(tuples));
      URL	url = Util.resolvePath(new URL(resource));
      String	user =
        username != null ? username : Util.userFromUrl(url).getUsername();
      int	index = findAuthentication(list, url, user);
        // May modify the list because of host search.
      String	password =
        index != -1 && ((Tuple) list.get(index)).user.getPassword() != null ?
          ((Tuple) list.get(index)).user.getPassword() :
          (
            delegate != null ?
              delegate.getPassword(canonic(resource), protocol, user) : null
          );

      if (index == -1 && password != null)
      {
        setAuthentication(list, url, new BasicUser(user, password), false);
      }

      tuples = (Tuple[]) list.toArray(new Tuple[0]);

      return password;
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  private static File
  getStorage(String filename)
  {
    return
      new File
      (
        be.re.util.Util.getPackageStorage(URLAuthenticator.class),
        filename
      );
  }



  public User
  getUser(String resource, String protocol)
  {
    return getUser(resource, protocol, null);
  }



  public User
  getUser(String resource, String protocol, User defaultUser)
  {
    if (!Util.isUrl(resource))
    {
      return
        delegate != null ?
          delegate.getUser(resource, protocol, defaultUser) : defaultUser;
    }

    try
    {
      URL	url = Util.resolvePath(new URL(resource));
      User	urlUser = Util.userFromUrl(url);

      if (urlUser.getUsername() != null && urlUser.getPassword() != null)
      {
        return urlUser;
      }

      List	list = new ArrayList(Arrays.asList(tuples));
      int	index = findAuthentication(list, url, urlUser.getUsername());
      User	user =
        index != -1 && ((Tuple) list.get(index)).user.getUsername() != null ?
          ((Tuple) list.get(index)).user :
          (
            delegate != null ?
              (
                urlUser.getUsername() != null ?
                  new BasicUser
                  (
                    urlUser.getUsername(),
                    delegate.getPassword
                    (
                      canonic(resource),
                      protocol,
                      urlUser.getUsername()
                    )
                  ) :
                  delegate.getUser(canonic(resource), protocol, defaultUser)
              ) : defaultUser
          );

      if (index == -1 && user != null)
      {
        setAuthentication(list, url, user, false);
      }
      else
      {
        if (user != null && user.getPassword() == null && delegate != null)
        {
          user.setPassword
          (
            delegate.
              getPassword(canonic(resource), protocol, user.getUsername())
          );
        }
      }

      tuples = (Tuple[]) list.toArray(new Tuple[0]);

      return user;
    }

    catch (MalformedURLException e)
    {
      throw new RuntimeException(e); // Would be a bug.
    }
  }



  private static Tuple[]
  loadCredentials(File file, char[] password, boolean global)
  {
    BufferedReader	in = null;

    try
    {
      if (!file.exists())
      {
        StreamConnector.copy
        (
          URLAuthenticator.class.getResourceAsStream("res/URLAuthenticator"),
          new FileOutputStream(file)
        );
      }

      in = new BufferedReader(new FileReader(file));

      Tuple[]	tuples =  readEntries(in, password, global);

      sortTuples(tuples);

      return tuples;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    finally
    {
      if (in != null)
      {
        try
        {
          in.close();
        }

        catch (IOException ex)
        {
          throw new RuntimeException(ex);
        }
      }
    }
  }



  private static Tuple
  processLine(String s, char[] password, boolean global)
    throws MalformedURLException, PBEException
  {
    StringTokenizer	tokenizer = new StringTokenizer(s, ";");

    if (tokenizer.countTokens() < 2 || tokenizer.countTokens() > 3)
    {
      return null;
    }

    URL		url = Util.resolvePath(new URL(tokenizer.nextToken()));
    User	user =
      new BasicUser
      (
        tokenizer.nextToken(),
        tokenizer.hasMoreTokens() ?
          decrypt(tokenizer.nextToken(), password, global) : null
      );

    if (url.getUserInfo() != null)
    {
      StringTokenizer	userInfoTokenizer =
        new StringTokenizer(url.getUserInfo(), ":");
      String		username = userInfoTokenizer.nextToken();

      if (user.getUsername() == null)
      {
        user.setUsername(username);
      }

      if (user.getPassword() == null && userInfoTokenizer.hasMoreTokens())
      {
        user.setPassword(userInfoTokenizer.nextToken());
      }
    }

    return new Tuple(url, user, true);
  }



  private static Tuple[]
  readEntries(BufferedReader in, char[] password, boolean global)
    throws IOException, PBEException
  {
    List	result = new ArrayList();
    String	s;

    while ((s = in.readLine()) != null)
    {
      if (s.indexOf('#') != -1)
      {
        s = s.substring(0, s.indexOf('#'));
      }

      s = s.trim();

      if (s.length() > 0)
      {
        Tuple	tuple = processLine(s, password, global);

        if (tuple != null)
        {
          result.add(tuple);
        }
      }
    }

    return (Tuple[]) result.toArray(new Tuple[0]);
  }



  public void
  save() throws IOException, PBEException
  {
    if (credentials != null)
    {
      save(tuples, credentials, password);
    }
    else
    {
      saveGlobal();
    }
  }



  private static void
  save(Tuple[] tuples, File file, char[] password)
    throws IOException, PBEException
  {
    File		saveFile = null;
    BufferedWriter	writer = null;

    try
    {
      saveFile =
        be.re.io.Util.createTempFile("url_auth", null, file.getParentFile());

      writer =
        new BufferedWriter
        (
          new OutputStreamWriter(new FileOutputStream(saveFile))
        );

      writeEntries(writer, tuples, password);
      writer.close();
      writer = null;
      new File(file.getAbsolutePath() + ".bak").delete();
      file.renameTo(new File(file.getAbsolutePath() + ".bak"));
      saveFile.renameTo(file);
      file.setReadable(true, true);
    }

    catch (Throwable e)
    {
      if (saveFile != null)
      {
        saveFile.delete();
      }

      if (be.re.util.Util.getCause(e) instanceof AbortException)
      {
        return;
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      if (e instanceof PBEException)
      {
        throw (PBEException) e;
      }

      throw new RuntimeException(e);
    }

    finally
    {
      if (writer != null)
      {
        writer.close();
      }
    }
  }



  public static synchronized void
  saveGlobal() throws IOException, PBEException
  {
    save(globalTuples, getStorage("URLAuthenticator"), null);
  }



  private static Tuple[]
  select(Tuple[] tuples, boolean persistent)
  {
    List	result = new ArrayList();

    for (int i = 0; i < tuples.length; ++i)
    {
      if (persistent == tuples[i].persistent)
      {
        result.add(tuples[i]);
      }
    }

    return (Tuple[]) result.toArray(new Tuple[result.size()]);
  }



  public void
  setAuthentication(URL url, User user)
  {
    List	list = new ArrayList(Arrays.asList(tuples));

    setAuthentication(list, url, user);
    tuples = (Tuple[]) list.toArray(new Tuple[0]);
  }



  private static void
  setAuthentication(List tuples, URL url, User user)
  {
    int	index = findAuthentication(tuples, url, user.getUsername());

    if (index != -1)
    {
      Tuple	tuple = (Tuple) tuples.get(index);

      tuple.user = user;
      tuple.persistent = true;
      tuple.passwordPersistent =
        user.getPassword() != null && user.getPassword().trim().length() > 0;
    }
    else
    {
      setAuthentication(tuples, url, user, true);
    }
  }



  private static void
  setAuthentication(List tuples, URL url, User user, boolean persistent)
  {
    // The position expresses the fact that if URLs are equal and no user is
    // specified during a find, the last used user precedes.

    tuples.add(0, new Tuple(url, user, persistent));

    for (int i = 0; i < tuples.size(); ++i)
    {
      ((Tuple) tuples.get(i)).position = i;
    }

    Tuple[]	array = (Tuple[]) tuples.toArray(new Tuple[0]);

    sortTuples(array);
    tuples.clear();
    tuples.addAll(Arrays.asList(array));
  }



  /**
   * Sets or adds the user for a URL and marks it as persistent, meaning it
   * will be saved along during the save operation.
   * @see save
   */

  public static synchronized void
  setGlobalAuthentication(URL url, User user)
  {
    List	list = new ArrayList(Arrays.asList(globalTuples));

    setAuthentication(list, url, user);
    globalTuples = (Tuple[]) list.toArray(new Tuple[0]);
  }



  /**
   * Sets persistent authentication entries.
   */

  public static synchronized void
  setGlobalAuthentications(Entry[] entries)
  {
    // Persistent entries are given precedence because they will be inserted in
    // front of the non-persistent ones set below.

    List	list =
      new ArrayList(Arrays.asList(select(globalTuples, false)));

    for (int i = 0; i < entries.length; ++i)
    {
      setAuthentication(list, entries[i].url, entries[i].user);
    }

    globalTuples = (Tuple[]) list.toArray(new Tuple[0]);
  }



  private static void
  sortTuples(Tuple[] tuples)
  {
    Arrays.sort
    (
      tuples,
      new Comparator()
      {
        public int
        compare(Object object1, Object object2)
        {
          int result =
            new StringTokenizer(((Tuple) object2).url.getPath(), "/").
              countTokens() -
              new StringTokenizer(((Tuple) object1).url.getPath(), "/").
                countTokens();

          return
            result == 0 ?
              (((Tuple) object1).position - ((Tuple) object2).position) :
              result;
        }
      }
    );
  }



  public User
  usedPreviously(String resource, String protocol)
  {
    if (!Util.isUrl(resource))
    {
      return
        delegate != null ? delegate.usedPreviously(resource, protocol) : null;
    }

    try
    {
      URL	url = Util.resolvePath(new URL(resource));
      User	urlUser = Util.userFromUrl(url);

      if (urlUser.getUsername() != null && urlUser.getPassword() != null)
      {
        return urlUser;
      }

      List	list = new ArrayList(Arrays.asList(tuples));
      int	index = findAuthentication(list, url, urlUser.getUsername());
      User	user =
        index != -1 && ((Tuple) list.get(index)).user.getUsername() != null ?
          ((Tuple) list.get(index)).user : null;

      tuples = (Tuple[]) list.toArray(new Tuple[0]);

      return user;
    }

    catch (MalformedURLException e)
    {
      return null;
    }
  }



  private static void
  writeEntries(BufferedWriter writer, Tuple[] tuples, char[] password)
    throws IOException, PBEException
  {
    for (int i = 0; i < tuples.length; ++i)
    {
      if (tuples[i].persistent)
      {
        writer.write(tuples[i].url.toString() + ";");

        if (tuples[i].user.getUsername() != null)
        {
          writer.write(tuples[i].user.getUsername() + ";");
        }

        if (tuples[i].passwordPersistent)
        {
          writer.write
          (
            password != null || globalEncryption ?
              encrypt(tuples[i].user.getPassword(), password) :
              tuples[i].user.getPassword()
          );
        }

        writer.newLine();
      }
    }
  }



  public static class Entry

  {

    private URL		url;
    private User	user;



    public
    Entry(URL url, User user)
    {
      this.url = url;
      this.user = user;
    }



    public URL
    getUrl()
    {
      return url;
    }



    public User
    getUser()
    {
      return user;
    }

  } // Entry



  private static class Tuple

  {

    private boolean	passwordPersistent;
    private boolean	persistent;
    private int         position;
    private URL		url;
    private User	user;



    private
    Tuple(URL url, User user, boolean persistent)
    {
      this.url = url;
      this.user = user;
      this.persistent = persistent;

      if (user.getPassword() != null && user.getPassword().trim().length() > 0)
      {
        passwordPersistent = true;
      }
    }

  } // Tuple

} // URLAuthenticator
