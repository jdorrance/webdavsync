package be.re.util;

import java.util.Comparator;



/**
 * Compares strings in a case-insensitive manner.
 * Werner Donn\u00e9
 */

public class CaseInsensitive implements Comparator

{

  public int
  compare(Object o1, Object o2)
  {
    return o1.toString().compareToIgnoreCase(o2.toString());
  }

} // CaseInsensitive
