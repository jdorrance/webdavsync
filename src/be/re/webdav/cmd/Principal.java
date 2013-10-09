package be.re.webdav.cmd;

/**
 * A principal value that can be displayed.
 * @author Werner Donn\u00e9
 */

public class Principal implements Comparable

{

  public String	displayName;
  public Object	value;



  public
  Principal(Object value, String displayName)
  {
    this.value = value;
    this.displayName = displayName == null ? "" : displayName;
  }



  public int
  compareTo(Object o)
  {
    return toString().compareToIgnoreCase(o.toString());
  }



  public String
  toString()
  {
    return displayName;
  }

} // Principal
