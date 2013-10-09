package be.re.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;



/**
 * @author Werner Donn\u00e9
 */

public class URLManagerJNDI implements URLManager.ProtocolHandler

{

  private String	password;
  private String	username;



  public
  URLManagerJNDI()
  {
    this(null, null);
  }



  public URLManagerJNDI(String username, String password)
  {
    this.username = username;
    this.password = password;
  }



  private String
  adjustedName(Binding binding)
    throws MalformedURLException, NamingException
  {
    return
      !binding.getName().endsWith("/") &&
        binding.getObject() instanceof Context ?
          (binding.getName() + "/") : binding.getName();
  }



  private Enumeration
  bindings(URL url, boolean lenient)
    throws MalformedURLException, NamingException
  {
    Context	context = null;
    boolean	isDirty = false;

    try
    {
      context =
        JNDIInitialContext.
          get(Util.extractSubUrl(url).toString(), username, password);

      return
        context.listBindings
        (
          Util.unescapeUriSpecials(Util.extractComposedUrlEntry(url))
        );
    }

    catch (NamingException e)
    {
      isDirty = !lenient;
      throw e;
    }

    finally
    {
      if (context != null)
      {
        JNDIInitialContext.release(context, isDirty);
      }
    }
  }



  public boolean
  canCopy(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canCopy(URL source, URL destination) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canDestroy(URL[] urls) throws IOException, ProtocolException
  {
    return false;
  } 



  public boolean
  canMove(URL[] source, URL destination) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  canMove(URL source, URL destination) throws IOException, ProtocolException
  {
    return false;
  } 
  


  public boolean
  canPreserve()
  {
    return false;
  }
  
  

  private static void
  checkUrl(URL url) throws MalformedURLException
  {
    if (!Util.isComposedUrl(url))
    {
      throw new MalformedURLException(Util.getResource("jndi_url_error"));
    }
  }



  private String
  composedName(URL context, Binding binding)
    throws MalformedURLException, NamingException
  {
    return
      context.toString() + (context.toString().endsWith("/") ? "" : "/") +
        adjustedName(binding);
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
    return;
  }



  public boolean
  copy(URL source, URL destination, boolean overwrite)
    throws IOException, ProtocolException
  {
    return true;
  }



  public URL
  create(URL url, boolean container) throws IOException, ProtocolException
  {
    throw new ProtocolException(Util.getResource("jndi_create_error"));
  }



  public void
  destroy(URL[] urls, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    throw new ProtocolException(Util.getResource("jndi_destroy_error"));
  }



  public void
  destroy(URL url) throws IOException, ProtocolException
  {
    throw new ProtocolException(Util.getResource("jndi_destroy_error"));
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
    checkUrl(url);

    try
    {
      return lookup(url) != null;
    }

    catch (NamingException e)
    {
      return false;
    }
  }



  public URL[]
  getContained(URL url) throws IOException, ProtocolException
  {
    checkUrl(url);

    try
    {
      Enumeration	enumer = bindings(url, false);
      List		result = new ArrayList();

      while (enumer.hasMoreElements())
      {
        result.add
        (
          Util.escapedUrl
          (
            composedName(url, (Binding) enumer.nextElement())
          )
        );
      }

      return (URL[]) result.toArray(new URL[result.size()]);
    }

    catch (NamingException e)
    {
      throw new ProtocolException(e);
    }
  }



  public URLManager.Property[][]
  getContainedProperties(URL url) throws IOException, ProtocolException
  {
    checkUrl(url);

    try
    {
      Enumeration	enumer = bindings(url, false);
      List		result = new ArrayList();

      while (enumer.hasMoreElements())
      {
        Binding	binding = (Binding) enumer.nextElement();
        URL	child = new URL(url, adjustedName(binding));

        result.add
        (
          new URLManager.Property[]
          {
            new URLManager.Property("name", adjustedName(binding), true, child),
            new URLManager.Property("object", getObjectString(binding), child),
            new URLManager.Property("class", binding.getClassName(), child)
          }
        );
      }

      return
        (URLManager.Property[][])
          result.toArray(new URLManager.Property[result.size()][]);
    }

    catch (NamingException e)
    {
      throw new ProtocolException(e);
    }
  }



  private String
  getObjectString(Binding binding)
  {
    return
      binding.toString().substring(binding.toString().lastIndexOf(':') + 1);
  }



  public Properties
  getParameters(URL url) throws IOException, ProtocolException
  {
    return new Properties();
  }



  public URLManager.Property[]
  getProperties(URL url) throws IOException, ProtocolException
  {
    checkUrl(url);

    List	properties = new ArrayList();

    properties.add
    (
      new URLManager.Property("name", Util.getLastPathSegment(url), true, url)
    );

    Binding	binding = null;

    try
    {
      binding = lookup(url);
    }

    catch (NamingException e)
    {
      return
        (URLManager.Property[])
          properties.toArray(new URLManager.Property[properties.size()]);
    }

    properties.
      add(new URLManager.Property("object", getObjectString(binding), url));
    properties.
      add(new URLManager.Property("class", binding.getClassName(), url));

    return
      (URLManager.Property[])
        properties.toArray(new URLManager.Property[properties.size()]);
  }



  public boolean
  isContainer(URL url) throws IOException, ProtocolException
  {
    checkUrl(url);

    return url.toString().endsWith("/");
  }



  private Binding
  lookup(URL url) throws MalformedURLException, NamingException
  {
    Enumeration	enumer = bindings(URLManager.getParent(url), false);
    String	name = Util.unescapeUriSpecials(Util.getLastPathSegment(url));

    while (enumer.hasMoreElements())
    {
      Binding	binding = (Binding) enumer.nextElement();

      if (name.equals(binding.getName()))
      {
        return binding;
      }
    }

    return null;
  }



  public boolean
  link(URL url, URL newBinding) throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  move(URL[] source, URL destination, URLManager.Resume resume)
    throws IOException, ProtocolException
  {
    return false;
  }



  public boolean
  move(URL source, URL destination) throws IOException, ProtocolException
  {
    throw new ProtocolException(Util.getResource("jndi_move_error"));
  }



  public boolean
  useTrashBin(URL url)
  {
    return false;
  }

} // URLManagerJNDI
