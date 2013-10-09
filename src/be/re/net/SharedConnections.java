package be.re.net;

import be.re.cache.Cache;
import be.re.cache.LRUCache;
import be.re.cache.BasicMedium;
import be.re.io.Tracer;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;



/**
 * This class manages connections which may be shared by several threads. The
 * time-out is counted only when a connection is released by all clients.
 * @author Werner Donn\u00e9
 */

public class SharedConnections

{

  private Adapter	adapter;
  private Set		cached = new HashSet();
  private Cache		connectionCache;
  private Hashtable	counters = new Hashtable();
  private Set		dirty = new HashSet();
  private Hashtable	timers = new Hashtable();
  private long          timeOut;
  private static Tracer	tracer = initTrace();



  public
  SharedConnections(Adapter adapter)
  {
    this(adapter, 5, 300000);
  }



  public
  SharedConnections(Adapter adapter, int cacheSize, long timeOut)
  {
    this.adapter = adapter;
    connectionCache = new LRUCache(new Medium(), cacheSize);
    this.timeOut = timeOut;
  }



  private int
  addReference(Object connection)
  {
    Integer	references = (Integer) counters.get(connection);

    if (references == null)
    {
      counters.put(connection, new Integer(1));

      return 1;
    }

    counters.put(connection, new Integer(references.intValue() + 1));

    return references.intValue() + 1;
  }



  private void
  cancelTimer(Object connection)
  {
    Timer	timer = (Timer) timers.get(connection);

    if (timer != null)
    {
      trace("cancel timer", connection);
      timer.cancel();
      timers.remove(connection);
    }
  }



  public synchronized Object
  get(String key) throws Exception
  {
    try
    {
      Object	connection = connectionCache.get(key);

      trace("get", connection);
      trace("key", key);
      cancelTimer(connection);
      addReference(connection);

      return connection;
    }

    catch (RuntimeException e)
    {
      if (e.getCause() instanceof Exception)
      {
        throw (Exception) e.getCause();
      }

      throw e;
    }
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
      System.getProperty("be.re.shared.trace") != null ?
        Tracer.getDefault() : null;
  }



  public synchronized void
  release(Object connection, boolean isDirty)
  {
    trace("release", connection);

    if (cached.contains(connection) && isDirty)
    {
      dirty.add(connection);
    }

    if (removeReference(connection) == 0)
    {
      if (cached.contains(connection) && !dirty.contains(connection))
      {
        setTimer(connection);
      }
      else
      {
        trace("close", connection);
        adapter.close(connection);
      }
    }
  }



  private int
  removeReference(Object connection)
  {
    int	references = ((Integer) counters.get(connection)).intValue();

    if (--references == 0)
    {
      counters.remove(connection);
    }
    else
    {
      counters.put(connection, new Integer(references));
    }

    return references;
  }



  private void
  setTimer(final Object connection)
  {
    Timer	timer = new Timer(true);

    trace("start timer", connection);
    timers.put(connection, timer);

    timer.schedule
    (
      new TimerTask()
      {
        public void
        run()
        {
          trace("time-out", connection);
          timers.remove(connection);
          dirty.add(connection);
          trace("close", connection);
          adapter.close(connection);
        }
      },
      timeOut
    );
  }



  public static void
  setTracer(Tracer value)
  {
    tracer = value;
  }



  private static void
  trace(String s, Object o)
  {
    Tracer	tr = getTracer();

    if (tr != null)
    {
      try
      {
        tr.write
        (
          (s + (o != null ? (" " + o.toString()) : "") + "\n").getBytes()
        );
      }

      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }



  public interface Adapter

  {

    public void		close	(Object connection);
    public Object	open	(Object key) throws Exception;

  } // Adapter



  private class Medium extends BasicMedium

  {

    public void
    dispose(Object value)
    {
      trace("dispose", value);
      cached.remove(value);
      trace("close", value);
      adapter.close(value);
    }



    public boolean
    isDirtyMustRead(Object value)
    {
      if (dirty.contains(value))
      {
        trace("dirty", value);
        dirty.remove(value);

        return true;
      }

      return false;
    }



    public Object
    read(Object key)
    {
      try
      {
        Object  connection = adapter.open(key);

        trace("read", connection);
        cached.add(connection);

        return connection;
      }

      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }

  } // JMSMedium

} // SharedConnections
