package be.re.cache;

/**
 * The Cache interface describes a way of accessing an expensive medium from
 * within a cheaper medium in an efficient way.
 * @see CacheFullException
 * @see CacheToMedium
 * @see LRUCache
 * @see SACache
 * @author Werner Donn\u00e9
 */

public interface Cache

{

  /**
   * Removes an object from the cache.
   * @param key an identification of the object in the expensive medium. It
   * expresses the way the cheaper medium sees the object and is thus defined
   * by it.
   */

  public void		delete			(Object key);

  /**
   * The cache is flushed entirely.
   * The contract is that an implementaion
   * should check if entries are dirty and act accordingly. The method is not
   * supposed to negotiate about disposal from the cache though it can.
   * It interacts with the CacheToMedium interface to perform the
   * actual operations.
   * @see CacheToMedium
   */

  public void		flush			();

  /**
   * With this method an object can be retrieved from the expensive medium by
   * passing some identification.
   * The method interacts with the CacheToMedium interface to perform the
   * actual operations.
   * @see CacheToMedium
   * @param key an identification of the object in the expensive medium. It
   * expresses the way the cheaper medium sees the object and is thus defined
   * by it.
   * @return the object comming from the expensive medium.
   * @exception CacheFullException There is not enough room to keep more items.
   */

  public Object		get			(Object key)
						  throws CacheFullException;

  /**
   * Returns the interface of the cache with both media currently in use.
   */

  public CacheToMedium	getCacheToMedium	();

  /**
   * Returns the room the cache currently has in terms of the number of items.
   * An implementation can return anything indicating it has no fixed capacity.
   */

  public int		getCapacity		();

  /**
   * This method puts an object into the expensive medium accompanied by an
   * identification of the cheaper medium to allow the latter to retrieve it
   * later.
   * The method interacts with the CacheToMedium interface to perform the
   * actual operations.
   * If the caller wants the object to be written through it must mark it
   * as being dirty before calling this method.
   * @see CacheToMedium
   * @param key an identification of the object in the expensive medium. It
   * expresses the way the cheaper medium sees the object and is thus defined
   * by it.
   * @param value the object that must be passed to the expensive medium.
   * @exception CacheFullException There is not enough room to keep more items.
   */

  public void		put			(Object key, Object value)
						  throws CacheFullException;

  /**
   * Set the interface of the cache with both media to a new value.
   */

  public void		setCacheToMedium	(CacheToMedium value);

  /**
   * Changes the capacity of the cache in terms of the number of items. If the
   * cache ends up with less space for items it must dispose of the items
   * concerned in the normal way, i.e. as when a get or put is executed.
   * If an implementation has no fixed item capacity it can ignore a call to
   * this method.
   * If the capacity is zero, a call to <code>get()</code> must perform a
   * read and a call to <code>put()</code> must perform a write.
   * @exception CacheFullException There is not enough room to keep more items.
   */

  public void		setCapacity		(int value)
						  throws CacheFullException;

} // Cache
