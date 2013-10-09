package be.re.cache;

import be.re.util.List;
import java.util.Hashtable;



/**
 * This class implements the Cache interface applying a least referenced used
 * schema.
 * @see Cache
 * @author Werner Donn\u00e9
 */

public class LRUCache implements Cache

{

  private CacheToMedium	cacheToMedium = null;
  private int		capacity = 0;
  private Hashtable	hashtable = new Hashtable();
  private List		list = new List();
  private int		loaded = 0;



  /**
   * Creates an empty cache with no capacity and medium to work with.
   * The cache is unusable in this stage. At least the
   * <code>cacheToMedium</code> property must be set.
   */

  public
  LRUCache()
  {
  }



  /**
   * Creates an empty cache. Besides that it sets the
   * <code>cacheToMedium</code> and <code>capacity</code> properties.
   * @param cacheToMedium the medium the cache will work with. It can be
   * changed at any time.
   * @param capacity the capacity of the cache in terms of the number of items
   * it can hold at the time.
   */

  public
  LRUCache(CacheToMedium cacheToMedium, int capacity)
  {
    setCacheToMedium(cacheToMedium);

    try
    {
      setCapacity(capacity);
    }

    catch (CacheFullException e)
    {
      // This cache cannot be full because there nothing in it yet.
    }
  }



  public void
  delete(Object key)
  {
    HashEntry	entry = (HashEntry) hashtable.remove(key);

    if (entry != null)
    {
      delete(entry.position);
    }
  }



  private void
  delete(List.Element element)
  {
    Object	value = ((ListEntry) element.getValue()).value;

    if (cacheToMedium.isDirtyMustWrite(value))
    {
      cacheToMedium.write(value);
    }

    cacheToMedium.dispose(value);
      // We don't negotiate.
  }



  /**
   * The cache is flushed entirely.
   * It interacts with the CacheToMedium interface to perform the
   * actual operations.
   *
   * For each object in the cache the method
   * <code>CacheToMedium.isDirtyMustWrite</code> is
   * called. Any dirty object is written with
   * <code>CacheToMedium.write</code>. The
   * <code>CacheToMedium.dispose</code> is called for all objects.
   * This means the cache does not negotiate about disposal at this stage.
   * @see Cache
   * @see CacheToMedium
   */

  public synchronized void
  flush()
  {
    flush(list.getHead());
    list.clear();
    hashtable.clear();
    loaded = 0;
  }



  private void
  flush(List.Element element)
  {
    if (element == null)
    {
      return;
    }

    delete(element);
    flush(element.getNext());
  }



  /**
   * With this method an object can be retrieved from the expensive medium by
   * passing some identification.
   * The method interacts with the CacheToMedium interface to perform the
   * actual operations.
   *
   * If the capacity is 0 a read is performed immediately with the CacheToMedium
   * object and the function returns with the result. In this case there is no
   * real cache activity.
   *
   * If the cache has a capacity the key is used to lookup the corresponding
   * object. If it is found it is place at the front of the LRU list. Before
   * returning the object the CacheToMedium.isDirtyMustRead method is called.
   * In case the object is dirty a read is done with the CacheToMedium object.
   * If no object is found with the key a read is also executed with the
   * CacheToMedium object.
   *
   * If there is not enough room in the cache to hold another item the last
   * disposable object in the LRU list is removed, i.e. if the method
   * <code>CacheToMedium.CanDispose</code> returns <code>true</code>. Such
   * an object is first checked
   * with <code>CacheToMedium.isDirtyMustWrite</code> and written with
   * <code>CacheToMedium.write</code>
   * if necessary. The <code>CacheToMedium.dispose</code> method is then
   * called.
   *
   * @param key an identification of the object in the expensive medium. It
   * expresses the way the cheaper medium sees the object and is thus defined
   * by it.
   * @return the object comming from the expensive medium.
   * @exception CacheFullException There is not enough room to keep more items.
   *
   * @see Cache
   * @see CacheToMedium
   */

  public synchronized Object
  get(Object key) throws CacheFullException
  {
    if (capacity == 0)
    {
      return cacheToMedium.read(key);
    }

    HashEntry	entry = (HashEntry) hashtable.get(key);

    if (entry != null)
    {
      ListEntry	listEntry = (ListEntry) entry.position.getValue();

      if (cacheToMedium.isDirtyMustRead(listEntry.value))
      {
        listEntry.value = cacheToMedium.read(key);
      }

      entry.position.remove();

      if (listEntry.value != null)
      {
        entry.position.insert(list.getHead());
      }
      else
      {
        hashtable.remove(key);
      }

      return listEntry.value;
    }

    if (++loaded > capacity)
    {
      removeLast(null);
    }

    Object		value = cacheToMedium.read(key);

    if (value != null)
    {
      List.Element	position = list.new Element(new ListEntry(key, value));

      position.remove();
      position.insert(list.getHead());
      hashtable.put(key, new HashEntry(key, position));
    }

    return value;
  }



  /**
   * Returns the room the cache currently has in terms of the number of items.
   * An implementation can return anything indicating it has no fixed capacity.
   * @see Cache
   */

  public int
  getCapacity()
  {
    return capacity;
  }



  /**
   * Returns the interface of the cache with both media currently in use.
   * @see Cache
   */

  public CacheToMedium
  getCacheToMedium()
  {
    return cacheToMedium;
  }



  /**
   * This method puts an object into the expensive medium accompanied by an
   * identification of the cheaper medium to allow the latter to retrieve it
   * later.
   * The method interacts with the CacheToMedium interface to perform the
   * actual operations.
   * If the caller wants the object to be written through it must mark
   * as being dirty before calling this method.
   *
   * If an object already exists for this key it is replaced by the new object.
   * The function then returns.
   *
   * If the capacity is 0 a write is performed immediately with the
   * CacheToMedium object and the function returns. In this
   * case there is no real cache activity.
   *
   * When an object is added to the cache the
   * <code>CacheToMedium.isDirtyMustWrite</code>
   * method is called and a write performed if necessary.
   *
   * If there is not enough room in the cache to hold another item the last
   * disposable object in the LRU list is removed, i.e. if the method
   * <code>CacheToMedium.CanDispose</code> returns <code>true</code>.
   * Such an object is first checked
   * with <code>CacheToMedium.isDirtyMustWrite</code> and written with
   * <code>CacheToMedium.write</code> if necessary. The
   * <code>CacheToMedium.dispose</code> method is then called.
   *
   * @param key an identification of the object in the expensive medium. It
   * expresses the way the cheaper medium sees the object and is thus defined
   * by it.
   * @param value the object that must be passed to the expensive medium.
   * @exception CacheFullException There is not enough room to keep more items.
   *
   * @see Cache
   * @see CacheToMedium
   */

  public synchronized void
  put(Object key, Object value) throws CacheFullException
  {
    HashEntry	entry = (HashEntry) hashtable.get(key);

    if (entry != null)
    {
      ((ListEntry) entry.position.getValue()).value = value;
    }
    else
    {
      if (capacity == 0)
      {
        cacheToMedium.write(value);
        return;
      }

      if (++loaded > capacity)
      {
        removeLast(null);
      }

      List.Element	position = list.new Element(new ListEntry(key, value));

      position.remove();
      position.insert(list.getHead());
      hashtable.put(key, new HashEntry(key, position));
    }

    if (cacheToMedium.isDirtyMustWrite(value))
    {
      cacheToMedium.write(value);
    }
  }



  private void
  removeLast(ListEntry firstNondisposable) throws CacheFullException
  {
    if (list.getTail() == null)
    {
      return;
    }

    ListEntry	entry = (ListEntry) list.getTail().getValue();

    if (firstNondisposable == entry) // We have seen them all.
    {
      --loaded;
      throw new CacheFullException();
    }

    if (cacheToMedium.canDispose(entry.value))
    {
      list.getTail().remove();
      hashtable.remove(entry.key);
      --loaded;

      if (cacheToMedium.isDirtyMustWrite(entry.value))
      {
        cacheToMedium.write(entry.value);
      }

      cacheToMedium.dispose(entry.value);
    }
    else
    {
      List.Element	element = list.getTail();

      element.remove();
      element.insert(list.getHead());
      ((HashEntry) hashtable.get(entry.key)).position = list.getTail();
      removeLast(firstNondisposable != null ? firstNondisposable : entry);
    }
  }



  /**
   * Changes the capacity of the cache in terms of the number of items. If the
   * cache ends up with less space for items it disposes of the items
   * concerned in the normal way, i.e. as when a get or put is executed.
   * @exception CacheFullException There is not enough room to keep more items.
   * @see Cache
   */

  public synchronized void
  setCapacity(int value) throws CacheFullException
  {
    capacity = value;

    while (loaded > capacity)
    {
      removeLast(null);
    }
  }



  /**
   * Set the interface of the cache with both media to a new value.
   * @see Cache
   */

  public synchronized void
  setCacheToMedium(CacheToMedium value)
  {
    cacheToMedium = value;
  }



  private class HashEntry

  {

    private Object		key;
    private List.Element	position;



    private
    HashEntry(Object key, List.Element position)
    {
      this.key = key;
      this.position = position;
    }



    public int
    hashCode()
    {
      return key.hashCode();
    }

  } // HashEntry



  private class ListEntry

  {

    private Object	key;
    private Object	value;



    private
    ListEntry(Object key, Object value)
    {
      this.key = key;
      this.value = value;
    }



    public int
    hashCode()
    {
      return key.hashCode();
    }

  } // ListEntry

} // LRUCache
