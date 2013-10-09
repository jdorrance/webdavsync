package be.re.net;

import be.re.gui.util.InteractiveAuthenticator;
import be.re.util.PBEException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;



public class SystemAuthenticator extends Authenticator

{

  private static Authenticate	authenticator;



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



  protected PasswordAuthentication
  getPasswordAuthentication()
  {
    if
    (
      getRequestingScheme() != null			&&
      !getRequestingScheme().equals("")			&&
      !getRequestingScheme().equalsIgnoreCase("basic")
    )
    {
      throw
        new RuntimeException
        (
          new IOException
          (
            "Authentication scheme " + getRequestingScheme() + " not supported"
          )
        );
    }

    User	user =
      authenticator.getUser
      (
        Util.isUrl(getUrl(getResource(true))) ?
          getUrl(getResource(true)) : getResource(true),
        getRequestingProtocol()
      );

    if (user == null)
    {
      user =
        authenticator.getUser
        (
          Util.isUrl(getUrl(getResource(false))) ?
            getUrl(getResource(false)) : getResource(false),
          getRequestingProtocol()
        );
    }

    return
      user == null ?
        null :
        new PasswordAuthentication
        (
          user.getUsername(),
          user.getPassword().toCharArray()
        );
  }



  private String
  getResource(boolean hostname)
  {
    return
      (
        getRequestingSite() != null ?
          (
            hostname ?
              getRequestingSite().getHostName() :
              getRequestingSite().getHostAddress()
          ) : ""
      ) +
      (
        getRequestingPort() != -1 ?
          (":" + String.valueOf(getRequestingPort())) : ""
      );
  }



  private String
  getUrl(String resource)
  {
    return getRequestingProtocol() + "//" + resource + "/";
  }

} // SystemAuthenticator
