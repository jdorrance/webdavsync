package be.re.cache;

import java.util.Enumeration;
import java.util.Hashtable;



public class SACache implements Cache, Ways

{

  private int		capacity = 0;
  private CacheToMedium	cacheToMedium = null;
  private Hashtable	hashtable = new Hashtable();
  private int		ways = 0;



  public
  SACache()
  {
  }



  public
  SACache(CacheToMedium cacheToMedium, int capacity, int ways)
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

    setWays(ways);
  }



  public void
  delete(Object key)
  {
    HashEntry	newEntry = new HashEntry(key, capacity, ways);
    HashEntry	entry = (HashEntry) hashtable.remove(newEntry);

    if (entry != null)
    {
      int	slot = entry.findSlot(key);

      if (slot < ways)
      {
        if (cacheToMedium.isDirtyMustWrite(entry.slots[slot]))
        {
          cacheToMedium.write(entry.slots[slot]);
        }

        cacheToMedium.dispose(entry.slots[slot]); // We don't negotiate.
      }
    }
  }



  public synchronized void
  flush()
  {
    Enumeration	elements = hashtable.elements();
    int		size = hashtable.size();

    for (int i = 0; i < size; ++i)
    {
      HashEntry	entry = (HashEntry) elements.nextElement();

      for (int j = 0; j < ways; ++j)
      {
        if (entry.slots[j] != null)
        {
          if (cacheToMedium.isDirtyMustWrite(entry.slots[j]))
          {
            cacheToMedium.write(entry.slots[j]);
          }

          cacheToMedium.dispose(entry.slots[j]); // We don't negotiate.
        }
      }
    }

    hashtable.clear();
  }



  public synchronized Object
  get(Object key) throws CacheFullException
  {
    if (capacity == 0)
    {
      return cacheToMedium.read(key);
    }

    HashEntry	newEntry = new HashEntry(key, capacity, ways);
    HashEntry	entry = (HashEntry) hashtable.get(newEntry);

    if (entry == null)
    {
      hashtable.put(newEntry, newEntry);
      newEntry.slots[0] = cacheToMedium.read(key);

      return newEntry.slots[0];
    }

    int	i = entry.findSlot(key);

    if (i < ways)
    {
      if (cacheToMedium.isDirtyMustRead(entry.slots[i]))
      {
        entry.slots[i] = cacheToMedium.read(key);
      }

      return entry.slots[i];
    }

    entry.slots[makeRoom(entry)] = cacheToMedium.read(key);

    return entry.slots[i];
  }



  public int
  getCapacity()
  {
    return capacity;
  }



  public CacheToMedium
  getCacheToMedium()
  {
    return cacheToMedium;
  }



  public int
  getWays()
  {
    return ways;
  }



  private int
  makeRoom(HashEntry entry)
  {
    int	i;

    for (i = 0; i < ways && entry.slots[i] != null; ++i);

    if (i < ways)
    {
      return i;
    }

    // A set associative cache can not negotiate about disposal because it
    // would be stuck quickly.

    if (cacheToMedium.isDirtyMustWrite(entry.slots[entry.toDispose]))
    {
      cacheToMedium.write(entry.slots[entry.toDispose]);
    }

    cacheToMedium.dispose(entry.slots[entry.toDispose]);
    i = entry.toDispose++;

    if (entry.toDispose == ways) // See comment in Ways.java.
    {
      entry.toDispose = 0;
    }

    return i;
  }



  public synchronized void
  put(Object key, Object value) throws CacheFullException
  {
    HashEntry	newEntry = new HashEntry(key, capacity, ways);
    HashEntry	entry = (HashEntry) hashtable.get(newEntry);

    if (entry != null)
    {
      int	slot = entry.findSlot(key);

      if (slot < ways)
      {
        entry.slots[slot] = value;
      }
      else
      {
        entry.slots[makeRoom(entry)] = value;
      }
    }
    else
    {
      hashtable.put(newEntry, newEntry);
      newEntry.slots[0] = value;
    }

    if (cacheToMedium.isDirtyMustWrite(value))
    {
      cacheToMedium.write(value);
    }
  }



  private void
  rehash(int newCapacity, int newWays)
  {
    Enumeration	elements = hashtable.elements();
    Hashtable	newHashtable = new Hashtable();
    int		size = hashtable.size();

    for (int i = 0; i < size; ++i)
    {
      HashEntry	entry = (HashEntry) elements.nextElement();

      for (int j = 0; j < ways; ++j)
      {
        HashEntry	key = new HashEntry(entry.key, newCapacity, newWays);
        HashEntry	newEntry = (HashEntry) newHashtable.get(key);

        if (newEntry == null)
        {
          newHashtable.put(key, key);
          newEntry = key;
        }

        int	k;

        for (k = 0; k < newWays && newEntry.slots[k] != null; ++k);

        if (k < newWays)
        {
          newEntry.slots[k] = entry.slots[j];
        }
        else
        {
          if (cacheToMedium.isDirtyMustWrite(entry.slots[j]))
          {
            cacheToMedium.write(entry.slots[j]);
          }

          cacheToMedium.dispose(entry.slots[j]);
        }
      }
    }

    hashtable = newHashtable;
    capacity = newCapacity;
    ways = newWays;
  }



  public synchronized void
  setCapacity(int value) throws CacheFullException
  {
    rehash(value, ways);
  }



  public synchronized void
  setCacheToMedium(CacheToMedium value)
  {
    cacheToMedium = value;
  }



  public synchronized void
  setWays(int value)
  {
    rehash(capacity, value);
  }



  class HashEntry

  {

    private int		capacity;
    public Object	key;
    public Object	slots[];
    private int		toDispose = 0;



    public
    HashEntry(Object key, int capacity, int ways)
    {
      this.key = key;
      slots = new Object[ways];
      this.capacity = capacity;
    }



    public int
    findSlot(Object key)
    {
      int	i;

      for
      (
        i = 0;
        i < slots.length && (slots[i] == null || !slots[i].equals(key));
        ++i
      );

      return i;
    }



    public int
    hashCode()
    {
      return key.hashCode() % capacity;
    }

  } // HashEntry

} // SACache
