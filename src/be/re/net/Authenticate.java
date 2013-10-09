package be.re.net;

/**
 * An interface for implementing a simple authentication service.
 * @author Werner Donn\u00e9
 */

public interface Authenticate
{
  /**
   * Invalidates the authentication information.
   */

  public void	badUser	    	(
				  String	resource,
				  String	protocol,
				  User		user
				);

  /**
   * Returns the password defined for a user for some resource. If none can be
   * found <code>null</code> is returned.
   */

  public String	getPassword	(
				  String	resource,
				  String	protocol,
				  String	username
				);

  /**
   * Picks the configured user among the users defined for this resource
   * according to a policy defined by the implementation. If none can be
   * found the <code>defaultUser</code> is returned.
   */

  public User	getUser		(
				  String	resource,
				  String	protocol,
				  User		defaultUser
			    	);

  /**
   * Picks the configured user among the users defined for this resource
   * according to a policy defined by the implementation. If none can be
   * found <code>null</code> is returned.
   */

  public User	getUser	    	(String resource, String protocol);

  /**
   * The implementation may return the user that was authenticated previously
   * for this resource.
   */

  public User	usedPreviously	(String resource, String protocol);
}
