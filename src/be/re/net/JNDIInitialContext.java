package be.re.net;

import be.re.gui.util.InteractiveAuthenticator;
import be.re.util.PBEException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;



public class JNDIInitialContext

{

  private static Authenticate			authenticator;
  private final static SharedConnections	connections =
    new SharedConnections(new Adapter());



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



  private static String
  canonic(String url)
  {
    return
      url.charAt(url.length() - 1) == '/' ?
        url.substring(0, url.length() - 1) : url;
  }



  public static Context
  get(String providerUrl) throws MalformedURLException, NamingException
  {
    return get(providerUrl, null, null);
  }



  public static synchronized Context
  get(String providerUrl, String username, String password)
    throws MalformedURLException, NamingException
  {
    try
    {
      if (username != null && password != null)
      {
        providerUrl =
          Util.setUserInfo(new URL(providerUrl), username + ":" + password).
            toString();
      }
      else
      {
        if (getAuthenticator() != null)
        {
          User	user = null;

          if (username != null)
          {
            user = new BasicUser(username, null);

            user.setPassword
            (
              getAuthenticator().getPassword
              (
                providerUrl,
                new URL(providerUrl).getProtocol(),
                username
              )
            );
          }
          else
          {
            user =
              getAuthenticator().
                getUser(providerUrl, new URL(providerUrl).getProtocol());
          }

          if (user != null && user.getUsername() != null)
          {
            providerUrl =
              Util.setUserInfo
              (
                new URL(providerUrl),
                user.getUsername() +
                  (user.getPassword() != null ? (":" + user.getPassword()) : "")
              ).toString();
          }
        }
      }

      return (Context) connections.get(canonic(providerUrl));
    }

    catch (NamingException e)
    {
      throw e;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static Authenticate
  getAuthenticator()
  {
    return authenticator;
  }



  public static synchronized void
  release(Context connection, boolean isDirty)
  {
    connections.release(connection, isDirty);
  }



  public static void
  setAuthenticator(Authenticate value)
  {
    authenticator = value;
  }



  private static class Adapter implements SharedConnections.Adapter

  {

    public void
    close(Object connection)
    {
      try
      {
        ((Context) connection).close();
      }

      catch (NamingException e)
      {
        throw new RuntimeException(e);
      }
    }



    public Object
    open(Object key) throws Exception
    {
      Hashtable	properties = new Hashtable();
      String	initialContextFactory =
        be.re.util.Util.getSystemProperty("java.naming.factory.initial");

      if (initialContextFactory != null)
      {
        properties.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
      }

      properties.put
      (
        Context.PROVIDER_URL,
        Util.stripUserInfo(new URL((String) key)).toString()
      );

      String	password = null;
      String	userInfo = new URL((String) key).getUserInfo();
      String	username = null;

      if (userInfo != null && userInfo.indexOf(':') != -1)
      {
        username = userInfo.substring(0, userInfo.indexOf(':'));
        password = userInfo.substring(userInfo.indexOf(':') + 1);
        properties.put(Context.SECURITY_PRINCIPAL, username);
        properties.put(Context.SECURITY_CREDENTIALS, password);
      }

      try
      {
        Context	result =  new InitialContext(properties);

        result.list("/"); // Do a test for lazy evaluation providers.

        return result;
      }

      catch (NamingException e)
      {
        if (getAuthenticator() != null)
        {
          getAuthenticator().badUser
          (
            (String) key,
            new URL((String) key).getProtocol(),
            new BasicUser(username, password)
          );
        }

        throw e;
      }
    }

  } // Adapter

} // JNDIInitialContext
