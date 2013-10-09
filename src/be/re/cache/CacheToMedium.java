package be.re.cache;

/**
 * The interface defines the way a cache is supposed to interact with the two
 * media it connects.
 * In fact it describes the actual semantics of the cache.
 * @see Cache
 * @author Werner Donn\u00e9
 */

public interface CacheToMedium
{
  /**
   * The method asks an object of the expensive medium wether it is allowed to
   * get rid of it.
   * This is just a negotiation. A cache is allowed to ignore the wishes of an
   * object. If it does so it must clearly state this and in what situations
   * it can occur.
   * @param value the object belonging to the expensive medium.
   * @return The wishes of the object with regard to disposal.
   * <code>true</code> means it doesn't mind, <code>false</code> means it does.
   */

  public boolean	canDispose		(Object value);



  /**
   * This is a notification for an object from the expensive medium that it is
   * to be thrown out of the cache.
   * An implementation is supposed to call the method
   * <code>isDirtyMustWrite</code> first and perform a write if necessary.
   * @param value the object belonging to the expensive medium.
   */

  public void		dispose			(Object value);



  /**
   * With this method the cache can check if an object comming from the
   * expensive medium is still valid before returning it. If it isn't, the cache
   * should reread it.
   * The initiative to mark an object this way is normally taken by the
   * expensive medium or a representative of it.
   *
   * With this method an expensive medium could be surrounded by several
   * caches and take actions to preserve cache consistancy (SMP principle).
   * If some cheaper medium writes the object back to it, it can mark it to
   * cause a refetch by the caches.
   * @param value the object belonging to the expensive medium.
   * @return the validity of the object. <code>true</code> means it was
   * modified, <code>false</code> means it is still valid.
   */

  public boolean	isDirtyMustRead		(Object value);



  /**
   * With this method the cache can check if an object comming from the
   * expensive medium was modified by the cheaper medium. If it was, the cache
   * should write it before disposing of it.
   *
   * With this method an expensive medium could be surrounded by several
   * caches and take actions to preserve cache consistancy (SMP principle).
   * A cheaper medium can notify the others through the expensive medium that
   * the object was modified. The expensive medium must arbitrate in this
   * matter.
   * @param value the object belonging to the expensive medium.
   * @return the validity of the object. <code>true</code> means it was
   * modified, <code>false</code> means it is still valid.
   */

  public boolean	isDirtyMustWrite	(Object value);



  /**
   * A read is performed by the cache when it doesn't find the object or when
   * it is marked dirty by the expensive medium.
   * @param key the identification of the object as it is known by the cheaper
   * medium. The expensive medium is responsible to agree upon the dialog with
   * all the cheaper media it deals with. It must recognize their keys.
   * @return the object belonging to the expensive medium.
   */

  public Object		read			(Object key);



  /**
   * A write is performed by the cache when it wants to dispose the object and
   * discovers it is marked dirty by the cheaper medium. The cache is supposed
   * to it as well when its put method is called and if the object is dirty.
   * This way a cheaper medium can use the cache as a write through cache. It
   * has the choice to do this systematically or on an object by object basis.
   * @param value the object belonging to the expensive medium.
   */

  public void		write			(Object value);
}
