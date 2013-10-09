package be.re.webdav.cmd;

import java.net.URL;



/**
 * An activity value that can be displayed.
 * @author Werner Donn\u00e9
 */

public class Activity implements Comparable

{

  public String	displayName;
  public URL	url;



  public
  Activity(URL url, String displayName)
  {
    this.url = url;
    this.displayName = displayName == null ? "" : displayName;
  }



  public int
  compareTo(Object o)
  {
    return toString().compareTo(o.toString());
  }



  public String
  toString()
  {
    return displayName;
  }

} // Activity
