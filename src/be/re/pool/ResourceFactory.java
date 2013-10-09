package be.re.pool;

/**
 * The connection pool uses this interface to create new instances of a
 * resource when it needs them.
 * @author Werner Donn\u00e9
 */

public interface ResourceFactory
{
  public ResourceAdapter	newInstance	(Object criterion)
						  throws ResourceException;
}
