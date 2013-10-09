package be.re.gui.util;

import be.re.net.AbortException;
import be.re.net.Authenticate;
import be.re.net.BasicUser;
import be.re.net.User;
import be.re.util.DispatchableEvent;
import be.re.util.EventMulticaster;
import java.util.EventListener;
import javax.swing.SwingUtilities;



/**
 * @author Werner Donn\u00e9
 */

public class InteractiveAuthenticator implements Authenticate

{

  private boolean		enableRemember;
  private EventMulticaster	rememberListeners = new EventMulticaster();



  public
  InteractiveAuthenticator()
  {
    this(false);
  }



  public
  InteractiveAuthenticator(boolean enableRemember)
  {
    this.enableRemember = enableRemember;
  }



  public void
  addRememberListener(RememberListener listener)
  {
    rememberListeners.add(listener);
  }



  public void
  badUser(String resource, String protocol, User user)
  {
  }



  public String
  getPassword(String resource, String protocol, String username)
  {
    User	user = new BasicUser(username, null);

    openDialog(resource, protocol, user, true);

    return user.getPassword();
  }



  public User
  getUser(String resource, String protocol, User defaultUser)
  {
    User	user = defaultUser != null ? defaultUser : new BasicUser();

    openDialog(resource, protocol, user, false);

    return
      user.getUsername() == null || user.getPassword() == null ? null : user;
  }



  public User
  getUser(String resource, String protocol)
  {
    return getUser(resource, protocol, null);
  }



  private void
  openDialog(String resource, String protocol, User user, boolean lockUsername)
  {
    final AuthenticateDialog	dialog =
      new AuthenticateDialog(resource, user, lockUsername, enableRemember);

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

    if (user.getPassword() == null)
    {
      throw new AbortException(Util.getResource("msg_aborted"));
    }

    if (dialog.shouldRemember())
    {
      rememberListeners.
        dispatch(new RememberEvent(this, resource, protocol, user));
    }
  }



  public void
  removeRememberListener(RememberListener listener)
  {
    rememberListeners.remove(listener);
  }



  public User
  usedPreviously(String resource, String protocol)
  {
    return null;
  }



  public class RememberEvent extends DispatchableEvent

  {

    private String	protocol;
    private String	resource;
    private User	user;



    private
    RememberEvent(Object source, String resource, String protocol, User user)
    {
      super(source);
      this.resource = resource;
      this.protocol = protocol;
      this.user = user;
    }



    public void
    dispatch(EventListener listener)
    {
      ((RememberListener) listener).remember(this);
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

  } // RememberEvent



  public interface RememberListener extends EventListener

  {

    public void	remember	(RememberEvent e);

  } // RememberListener

} // InteractiveAuthenticator
