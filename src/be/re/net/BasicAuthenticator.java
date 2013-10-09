package be.re.net;

import be.re.io.StreamConnector;
import be.re.util.Array;
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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;



public class BasicAuthenticator implements Authenticate

{

  private Authenticate		delegate = null;
  private static boolean	encryption;
  private static Tuple[]	tuples = new Tuple[0];



  static
  {
    try
    {
      loadUsers();
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public
  BasicAuthenticator() throws PBEException
  {
    this(null);
  }



  public
  BasicAuthenticator(Authenticate delegate) throws PBEException
  {
    this.delegate = delegate;
  }



  public void
  badUser(String resource, String protocol, User user)
  {
    synchronized (tuples)
    {
      int	index =
        findAuthentication(resource, protocol, user.getUsername());

      if (index == -1 && !"default".equals(resource))
      {
        index = findAuthentication("default", protocol, null);
      }

      if (index != -1)
      {
        tuples = (Tuple[]) Array.remove(tuples, index);
      }
    }

    if (delegate != null)
    {
      delegate.badUser(resource, protocol, user);
    }
  }



  private static String
  decrypt(String s) throws PBEException
  {
    if (s.startsWith("^@e@^"))
    {
      encryption = true;

      return new String(PBE.decrypt(Base64.decode(s.substring(5).getBytes())));
    }

    return s;
  }



  public static synchronized void
  decryptPasswords() throws IOException, PBEException
  {
    encryption = false;
    save();
  }



  private static String
  encrypt(String s) throws PBEException
  {
    return "^@e@^" + new String(Base64.encode(PBE.encrypt(s.getBytes())));
  }



  public static synchronized void
  encryptPasswords() throws IOException, PBEException
  {
    encryption = true;
    save();
  }



  private static int
  findAuthentication(String resource, String protocol, String username)
  {
    List	toTry = new ArrayList();

    if (Util.isUrl(resource))
    {
      try
      {
        URL	url = new URL(resource);

        toTry.add
        (
          url.getHost() +
            (url.getPort() == -1 ? "" : (":" + String.valueOf(url.getPort())))
        );

        if (url.getPort() != -1)
        {
          toTry.add(url.getHost());
        }
      }

      catch (MalformedURLException e)
      {
        throw new RuntimeException(e); // Would be a bug.
      }
    }
    else
    {
      toTry.add(resource);
    }

    for (Iterator i = toTry.iterator(); i.hasNext();)
    {
      String	res = (String) i.next();

      for (int j = 0; j < tuples.length; ++j)
      {
        if
        (
          tuples[j].resource.equalsIgnoreCase(res)		&&
          tuples[j].protocol.equalsIgnoreCase(protocol)		&&
          (
            username == null					||
            tuples[j].user.getUsername().equals(username)
          )
        )
        {
          return j;
        }
      }
    }

    return -1;
  }



  /**
   * Gets persistent authentication entries.
   */

  public static synchronized Entry[]
  getAuthentications()
  {
    Tuple[]	tuples = select(true);
    Entry[]	entries = new Entry[tuples.length];

    for (int i = 0; i < tuples.length; ++i)
    {
      entries[i] =
        new Entry(tuples[i].resource, tuples[i].protocol, tuples[i].user);
    }

    return entries;
  }



  public String
  getPassword(String resource, String protocol, String username)
  {
    synchronized (tuples)
    {
      int	index = findAuthentication(resource, protocol, username);
      String	password =
        index != -1 && tuples[index].user.getPassword() != null ?
          tuples[index].user.getPassword() :
          (
            delegate != null ?
              delegate.getPassword(resource, protocol, username) : null
          );

      if (index == -1 && password != null)
      {
        setAuthentication
        (
          resource,
          protocol,
          new BasicUser(username, password),
          false
        );
      }

      return password;
    }
  }



  private static File
  getStorage(String filename)
  {
    File	directory =
      new File
      (
        new File(new File(System.getProperty("user.home"), ".be"), "re"),
        "net"
      );

    if (!directory.exists())
    {
      directory.mkdirs();
    }

    return new File(directory, filename);
  }



  public User
  getUser(String resource, String protocol)
  {
    return getUser(resource, protocol, null);
  }



  public User
  getUser(String resource, String protocol, User defaultUser)
  {
    synchronized (tuples)
    {
      int	index = findAuthentication(resource, protocol, null);

      if (index == -1 && !"default".equals(resource))
      {
        index = findAuthentication("default", protocol, null);
      }

      User	user =
        index != -1 && tuples[index].user.getUsername() != null ?
          tuples[index].user :
          (
            delegate != null ?
              delegate.getUser(resource, protocol, defaultUser) : defaultUser
          );

      if (index == -1 && user != null)
      {
        setAuthentication(resource, protocol, user, false);
      }
      else
      {
        if (user != null && user.getPassword() == null && delegate != null)
        {
          user.setPassword
          (
            delegate.getPassword(resource, protocol, user.getUsername())
          );
        }
      }

      return user;
    }
  }



  private static synchronized void
  loadUsers() throws IOException, PBEException
  {
    BufferedReader	in = null;

    try
    {
      File	directory =
        new File
        (
          new File(new File(System.getProperty("user.home"), ".be"), "re"),
          "net"
        );

      if (!directory.exists())
      {
        directory.mkdirs();
      }

      File	file = new File(directory, "BasicAuthenticator");

      if (!file.exists())
      {
        StreamConnector.copy
        (
          BasicAuthenticator.class.
            getResourceAsStream("res/BasicAuthenticator"),
          new FileOutputStream(file)
        );
      }

      in = new BufferedReader(new FileReader(file));
      readEntries(in);
    }

    finally
    {
      if (in != null)
      {
        in.close();
      }
    }
  }



  private static void
  processLine(String s) throws PBEException
  {
    StringTokenizer	tokenizer = new StringTokenizer(s, ";");

    if (tokenizer.countTokens() < 3 || tokenizer.countTokens() > 4)
    {
      System.err.println("BasicAuthenticator: syntax error: " + s);
    }
    else
    {
      setAuthentication
      (
        tokenizer.nextToken(),
        tokenizer.nextToken(),
        new BasicUser
        (
          tokenizer.nextToken(),
          tokenizer.hasMoreTokens() ? decrypt(tokenizer.nextToken()) : null
        )
      );
    }
  }



  private static void
  readEntries(BufferedReader in) throws IOException, PBEException
  {
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
        processLine(s);
      }
    }
  }



  public static synchronized void
  save() throws IOException, PBEException
  {
    File		saveFile = null;
    BufferedWriter	writer = null;

    try
    {
      saveFile =
        be.re.io.Util.createTempFile
        (
          "basic_auth",
          null,
          getStorage("dummy").getParentFile()
        );

      writer =
        new BufferedWriter
        (
          new OutputStreamWriter(new FileOutputStream(saveFile))
        );

      writeEntries(writer);
      writer.close();
      writer = null;
      getStorage("BasicAuthenticator.bak").delete();
      getStorage("BasicAuthenticator").
        renameTo(getStorage("BasicAuthenticator.bak"));
      saveFile.renameTo(getStorage("BasicAuthenticator"));
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



  private static Tuple[]
  select(boolean persistent)
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



  /**
   * Sets or adds the user for a resource/protocol combination and marks it as
   * persistent, meaning it will be saved along during the save operation.
   * @see save
   */

  public static synchronized void
  setAuthentication(String resource, String protocol, User user)
  {
    int	index = findAuthentication(resource, protocol, user.getUsername());

    if (index != -1)
    {
      tuples[index].user = user;
      tuples[index].persistent = true;
      tuples[index].passwordPersistent =
        user.getPassword() != null && user.getPassword().trim().length() > 0;
    }
    else
    {
      setAuthentication(resource, protocol, user, true);
    }
  }



  private static void
  setAuthentication
  (
    String	resource,
    String	protocol,
    User	user,
    boolean	persistent
  )
  {
    tuples =
      (Tuple[]) Array.insert
      (
        tuples,
        0,
        new Tuple(resource, protocol, user, persistent)
      );
  }



  /**
   * Sets persistent authentication entries.
   */

  public static synchronized void
  setAuthentications(Entry[] entries)
  { 
    // Persistent entries are given precedence because they will be inserted in
    // front of the non-persistent ones set below.

    tuples = select(false);

    for (int i = 0; i < entries.length; ++i)
    {
      setAuthentication
      (
        entries[i].resource,
        entries[i].protocol,
        entries[i].user
      );
    }
  }



  public User
  usedPreviously(String resource, String protocol)
  {
    synchronized (tuples)
    {
      int	index = findAuthentication(resource, protocol, null);

      if (index == -1 && !"default".equals(resource))
      {
        index = findAuthentication("default", protocol, null);
      }

      return
        index != -1 && tuples[index].user.getUsername() != null ?
          tuples[index].user : null;
    }
  }



  private static void
  writeEntries(BufferedWriter writer) throws IOException, PBEException
  {
    for (int i = 0; i < tuples.length; ++i)
    {
      if (tuples[i].persistent)
      {
        writer.write(tuples[i].resource + ";");
        writer.write(tuples[i].protocol + ";");

        if (tuples[i].user.getUsername() != null)
        {
          writer.write(tuples[i].user.getUsername() + ";");
        }

        if (tuples[i].passwordPersistent)
        {
          writer.write
          (
            encryption ?
              encrypt(tuples[i].user.getPassword()) :
              tuples[i].user.getPassword()
          );
        }

        writer.newLine();
      }
    }
  }



  public static class Entry

  {

    private String	protocol;
    private String	resource;
    private User	user;



    public
    Entry(String resource, String protocol, User user)
    {
      this.resource = resource;
      this.protocol = protocol;
      this.user = user;
    }



    public String
    getProtocol()
    {
      return protocol;
    }



    public String
    getResource()
    {
      return resource;
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
    private String	protocol;
    private String	resource;
    private User	user;



    private
    Tuple(String resource, String protocol, User user, boolean persistent)
    {
      this.resource = resource;
      this.protocol = protocol;
      this.user = user;
      this.persistent = persistent;

      if (user.getPassword() != null  && user.getPassword().trim().length() > 0)
      {
        passwordPersistent = true;
      }
    }

  } // Tuple

} // BasicAuthenticator
