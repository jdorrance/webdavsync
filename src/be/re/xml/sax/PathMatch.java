package be.re.xml.sax;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import be.re.xml.ExpandedName;



/**
 * @author Werner Donn\u00e9
 */

public class PathMatch

{

  private boolean	absolute;
  private Map		map;
  private Stack		pathElements = new Stack();



  public
  PathMatch(ExpandedName[][] paths, boolean absolute)
  {
    map = createPathMap(paths);
    this.absolute = absolute;
  }



  private static Map
  createPathMap(ExpandedName[][] paths)
  {
    Map	result = new HashMap(paths.length);

    for (int i = 0; i < paths.length; ++i)
    {
      Map	level = result;

      for (int j = paths[i].length - 1; j >= 0; --j)
      {
        Entry	entry = (Entry) level.get(paths[i][j]);

        if (entry == null)
        {
          entry = new Entry();

          entry.accept = (j == 0);
          level.put(paths[i][j], entry);
        }

        level = entry.map;
      }
    }

    return result;
  }



  /**
   * Returns <code>true</code> if any of the given paths match,
   * <code>false</code> otherwise.
   */

  public boolean
  match()
  {
    if (pathElements.empty())
    {
      return false;
    }

    Map	level = map;

    for (int i = pathElements.size() - 1; i >= 0; --i)
    {
      Entry	entry = (Entry) level.get(pathElements.get(i));

      if (entry == null)
      {
        return false;
      }

      if (entry.accept && (!absolute || i == 0))
      {
        return true;
      }

      level = entry.map;
    }

    return false;
  }



  public void
  pop()
  {
    pathElements.pop();
  }



  public void
  push(ExpandedName pathElement)
  {
    pathElements.push(pathElement);
  }



  private static class Entry

  {

    private boolean	accept;
    private Map		map = new HashMap(5);

  } // Entry

} // PathMatch
