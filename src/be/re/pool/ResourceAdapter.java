package be.re.pool;

import java.io.IOException;



/**
 * The connection pool and a resource communicate through this interface.
 * @author Werner Donn\u00e9
 */

public interface ResourceAdapter
{
  /**
   * The pool notifies the resource it has been given to a client.
   */

  public void		acquired	();

  /**
   * The pool calls this method to get rid of the resource permanently.
   */

  public void		close		() throws ResourceException;

  /**
   * Returns the actual connection, which is made available to the client
   * through the <code>Resource</code> interface. This method should not
   * have to access physical resources. This should be done during the
   * creation of the adapter. Therefore the <code>ResourceException</code> is
   * not declared here.
   * @see Resource
   */

  public Object		getConnection	();

  /**
   * The pool uses this method to test the resource. If the resource can no
   * longer be used the method must return <code>false</code>. In that case
   * it is removed from the pool.
   */

  public boolean	isAlive		();

  /**
   * The resource can tell the pool with this method that it must be closed
   * and removed from the pool.
   */

  public boolean	mustClose	();

  /**
   * The pool notifies the resource it has been returned to the pool by a
   * client.
   */

  public void		released	();
}
