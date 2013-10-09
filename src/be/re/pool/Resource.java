package be.re.pool;

import java.io.IOException;



/**
 * This is what the connection pool and the client exchange.
 * @author Werner Donn\u00e9
 */

public interface Resource
{
  /**
   * Returns the actual connection. which must not be saved.
   */

  public Object	getConnection	();

  /**
   * Returns the resource to the connection pool.
   */

  public void	release		() throws ResourceException;
}
