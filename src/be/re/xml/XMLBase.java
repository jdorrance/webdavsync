package be.re.xml;

/**
 * XML filters can implement this interface if they want to be notified about
 * the base URI of the document that is to be processed.
 * @author Werner Donn\u00e9
 */

public interface XMLBase

{

  public void	setBaseURI	(String baseURI);

} // XMLBase
