package be.re.pool;

import be.re.io.Tracer;
import be.re.util.Equal;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * A generic connection pool.
 * @author Werner Donn\u00e9
 */

public class ConnectionPool

{

  private int			blockedRequests;
  private boolean	        blocking;
  private Equal		        equivalenceClass;
  private int		        equivalenceClassSize;
  private ResourceFactory       factory;
  private int		        poolSize;
  private List		        resources = new ArrayList();
  private static Tracer		tracer = initTrace();



  /**
   * Creates a blocking connection pool without equivalence classes.
   */

  public
  ConnectionPool(ResourceFactory factory, int poolSize)
  {
    this(factory, poolSize, null, 0, true);
  }



  /**
   * Creates a connection pool without equivalence classes.
   */

  public
  ConnectionPool(ResourceFactory factory, int poolSize, boolean blocking)
  {
    this(factory, poolSize, null, 0, blocking);
  }



  /**
   * Creates a connection pool.
   * @param factory to produce new resource instances.
   * @param poolSize the overall maximum number of resource instances.
   * @param equivalenceClass divides the pool in equivalence classes. The value
   * <code>null</code> indicates there are no classes.
   * @param equivalenceClassSize the maximum number of resource instances per
   * equivalence class. The first argument of its <code>equal</code> method
   * is always a resource adapter.
   * @param blocking indicates if the <code>get</method> blocks or not when no
   * resources are available.
   * equivalence class.
   */

  public
  ConnectionPool
  (
    ResourceFactory	factory,
    int			poolSize,
    Equal		equivalenceClass,
    int			equivalenceClassSize,
    boolean		blocking
  )
  {
    this.factory = factory;
    this.poolSize = poolSize;
    this.blocking = blocking;

    if (equivalenceClass != null)
    {
      this.equivalenceClass = equivalenceClass;
      this.equivalenceClassSize = equivalenceClassSize;
    }
    else
    {
      this.equivalenceClass =
        new Equal()
        {
          public boolean
          equal(Object object, Object refData)
          {
            return true;
          }
        };

      this.equivalenceClassSize = poolSize;
    }
  }



  /**
   * Any member of the pool qualifies.
   */

  public Resource
  get(Object client) throws ResourceException
  {
    return
      get
      (
        client,
        new Equal()
        {
          public boolean
          equal(Object object, Object refData)
          {
            return true;
          }
        },
        null
      );
  }



  /**
   * Retrieve a resource instance matching the criterion. If none can be found
   * a new one is created if the size constraints permit it. Otherwise the
   * method will return <code>null</code> or block until a resource is
   * available, depending on the mode set in the constructor.
   * @param client the object asking for the resource.
   * @param criterion executed on a resource adapter instance to check if it
   * qualifies.
   * @param refData second argument to <code>criterion</code>. If a match
   * couldn't be found, this argument is passed to the resource factory.
   * @return the returned <code>Resource</code> implementation releases the
   * resource when being finalized.
   * @exception ResourceException any problem caused by the resource.
   */

  public Resource
  get(Object client, Equal criterion, Object refData) throws ResourceException
  {
    trace("get from", this, client);

    synchronized (resources)
    {
      for (int i = 0; i < resources.size(); ++i)
      {
        Tuple	entry = (Tuple) resources.get(i);

        if (criterion.equal(entry.adapter, refData))
        {
          if (entry.occupied == null)
          {
            if (entry.adapter.isAlive())
            {
              trace("available", entry.adapter, client);
              entry.occupied = client;
              entry.adapter.acquired();

              return new ResourceCollector(client, entry.adapter);
            }

            trace("dead", entry.adapter, client);
            resources.remove(i--);
            entry.equivalenceClass.remove(entry.adapter);
          }
          else
          {
            trace("occupied", entry.adapter, client);
          }
        }
      }

      if (resources.size() >= poolSize)
      {
        return resourceWhenPoolFull(client, criterion, refData);
      }

      for (int i = 0; i < resources.size(); ++i)
      {
        Tuple	entry = (Tuple) resources.get(i);

        synchronized (entry.equivalenceClass)
        {
          if (equivalenceClass.equal(entry.adapter, refData))
          {
            if (entry.equivalenceClass.size() == equivalenceClassSize)
            {
              return
                resourceWhenClassFull
                (
                  client,
                  criterion,
                  refData,
                  entry.equivalenceClass
                );
            }
          }

          return newResource(client, refData, entry.equivalenceClass);
        }
      }

      return newResource(client, refData, null);
    }
  }



  public int
  getBlockedRequests()
  {
    return blockedRequests;
  }



  public int
  getOccupiedCount()
  {
    int	result = 0;

    for (int i = 0; i < resources.size(); ++i)
    {
      if (((Tuple) resources.get(i)).occupied != null)
      {
        ++result;
      }
    }

    return result;
  }



  public int
  getPoolSize()
  {
    return poolSize;
  }



  public int
  getResourceCount()
  {
    return resources.size();
  }



  public static Tracer
  getTracer()
  {
    return tracer;
  }



  private static Tracer
  initTrace()
  {
    return
      System.getProperty("be.re.pool.trace") != null ?
        Tracer.getDefault() : null;
  }



  private Resource
  newResource(Object client, Object refData, Set equivalenceClass)
    throws ResourceException
  {
    Tuple	entry = new Tuple();

    trace("new", entry.adapter, client);
    entry.adapter = factory.newInstance(refData);
    entry.equivalenceClass =
      equivalenceClass != null ? equivalenceClass : new HashSet();
    entry.occupied = client;
    entry.equivalenceClass.add(entry.adapter);
    resources.add(entry);
    entry.adapter.acquired();
    trace("created", entry.adapter, client);

    return new ResourceCollector(client, entry.adapter);
  }



  /**
   * Makes the <code>adapter</code> available again for selection from the
   * pool. Its <code>mustClose</code> method is called. If it returns
   * <code>true</code> the resource closed and removed from the pool
   * permanently.
   *
   * The method is idempotent. It is available to resource implementations
   * that have other schemes to release connections, such as at the end of
   * stream consumption in another thread.
   * @except ResourceException can be caused by the possible close.
   */

  public void
  release(Object client, ResourceAdapter adapter) throws ResourceException
  {
    synchronized (resources)
    {
      for (int i = 0; i < resources.size(); ++i)
      {
        Tuple	entry = (Tuple) resources.get(i);

        if (entry.adapter.equals(adapter) && client == entry.occupied)
                                             // Test reference to avoid races.
        {
          entry.occupied = null;

          if (resources.size() > poolSize || adapter.mustClose())
          {
            resources.remove(i);

            synchronized (entry.equivalenceClass)
            {
              entry.equivalenceClass.remove(entry.adapter);
            }

            trace("close", entry.adapter, client);
            adapter.close();
          }

          trace("notify", entry.adapter, client);

          synchronized (resources)
          {
            resources.notifyAll();
          }

          synchronized (entry.equivalenceClass)
          {
            entry.equivalenceClass.notifyAll();
          }

          trace("release", entry.adapter, client);
          entry.adapter.released();

          return;
        }
      }
    }
  }



  /**
   * Clears the current resources. This may be required when some global
   * properties of resource creation have changed.
   */

  public void
  reset()
  {
    synchronized (resources)
    {
      resources.clear();
    }
  }



  private Resource
  resourceWhenClassFull
  (
    Object	client,
    Equal	criterion,
    Object	refData,
    Set		equivalenceClass
  ) throws ResourceException
  {
    if (blocking)
    {
      try
      {
        synchronized (equivalenceClass)
        {
          trace("blocking on equivalence class", equivalenceClass, client);
          ++blockedRequests;
          equivalenceClass.wait();
          --blockedRequests;
        }
      }

      catch (InterruptedException e)
      {
        throw new ResourceException(e);
      }

      return get(client, criterion, refData);
    }

    return null;
  }



  private Resource
  resourceWhenPoolFull(Object client, Equal criterion, Object refData)
    throws ResourceException
  {
    if (blocking)
    {
      try
      {
        synchronized (resources)
        {
          trace("blocking on pool", this, client);
          ++blockedRequests;
          resources.wait();
          --blockedRequests;
        }
      }

      catch (InterruptedException e)
      {
        throw new ResourceException(e);
      }

      return get(client, criterion, refData);
    }

    return null;
  }



  public void
  setPoolSize(int value)
  {
    poolSize = value;
  }



  public static void
  setTracer(Tracer value)
  {
    tracer = value;
  }



  private static void
  trace(String s, Object o, Object client)
  {
    Tracer	tr = getTracer();

    if (tr != null)
    {
      try
      {
        tr.write
        (
          (
            s + (o != null ? (" " + o.toString()) : "") +
              (client != null ? (" (client: " + client.toString() + ")") : "") +
              "\n"
          ).getBytes()
        );
      }

      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }



  /**
   * The <code>get</code> method always returns an instance of this class. It
   * makes sure that when clients don't release the resource, the garbage
   * collector eventually will.
   */

  public class ResourceCollector implements Resource

  {

    private ResourceAdapter	adapter;
    private Object		client;
    private boolean		released = false;



    private
    ResourceCollector(Object client, ResourceAdapter adapter)
    {
      this.adapter = adapter;
      this.client = client;
    }



    protected void
    finalize() throws Throwable
    {
      release();
    }



    public Object
    getConnection()
    {
      return adapter.getConnection();
    }



    public void
    release() throws ResourceException
    {
      if (!released)
      {
        released = true;
        ConnectionPool.this.release(client, adapter);
      }
    }

  } // ResourceCollector



  private class Tuple

  {

    private ResourceAdapter	adapter;
    private Set			equivalenceClass;
    private Object		occupied = null;

  } // Tuple

} // ConnectionPool
