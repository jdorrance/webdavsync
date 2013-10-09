package be.re.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Implements Paul Heckel's algorithm in <q>A Technique for Isolating
 * Differences Between Files</q>, Communications of the ACM, April 1978.
 * @author Werner Donn\u00e9
 */

public class Diff

{

  public static final int	DELETE = 0;
  public static final int	INSERT = 1;
  public static final int	UNCHANGED = 2;



  private static void
  connectNeighboursAscending(Object[] oldArray, Object[] newArray)
  {
    for (int i = 0; i < newArray.length - 1; ++i)
    {
      if
      (
        newArray[i] instanceof Integer					    &&
        newArray[i + 1] == oldArray[((Integer) newArray[i]).intValue() + 1]
      )
      {
        newArray[i + 1] = new Integer(((Integer) newArray[i]).intValue() + 1);
        oldArray[((Integer) newArray[i]).intValue() + 1] = new Integer(i + 1);
      }
    }
  }



  private static void
  connectNeighboursDescending(Object[] oldArray, Object[] newArray)
  {
    for (int i = newArray.length - 1; i > 0; --i)
    {
      if
      (
        newArray[i] instanceof Integer					    &&
        newArray[i - 1] == oldArray[((Integer) newArray[i]).intValue() - 1]
      )
      {
        newArray[i - 1] = new Integer(((Integer) newArray[i]).intValue() - 1);
        oldArray[((Integer) newArray[i]).intValue() - 1] = new Integer(i - 1);
      }
    }
  }



  public static Change[]
  diff(Object[] oldVersion, Object[] newVersion)
  {
    Map		symbolTable = new HashMap();
    Object[]	newArray = fillSymbolTable(symbolTable, newVersion, false);
    Object[]	oldArray = fillSymbolTable(symbolTable, oldVersion, true);

    makeConnections(oldArray, newArray, symbolTable, newVersion);
    connectNeighboursAscending(oldArray, newArray);
    connectNeighboursDescending(oldArray, newArray);

    return encodeChanges(oldVersion, newVersion, newArray);
  }



  /**
   * The subjects in the result are <code>Character</code> objects.
   */

  public static Change[]
  diff(char[] oldVersion, char[] newVersion)
  {
    return
      diff
      (
        Util.toCharacterArray(oldVersion),
        Util.toCharacterArray(newVersion)
      );
  }



  /**
   * The subjects in the result are <code>Short</code> objects.
   */

  public static Change[]
  diff(short[] oldVersion, short[] newVersion)
  {
    return diff(Util.toShortArray(oldVersion), Util.toShortArray(newVersion));
  }



  /**
   * The subjects in the result are <code>Integer</code> objects.
   */

  public static Change[]
  diff(int[] oldVersion, int[] newVersion)
  {
    return
      diff(Util.toIntegerArray(oldVersion), Util.toIntegerArray(newVersion));
  }



  /**
   * The subjects in the result are <code>Long</code> objects.
   */

  public static Change[]
  diff(long[] oldVersion, long[] newVersion)
  {
    return diff(Util.toLongArray(oldVersion), Util.toLongArray(newVersion));
  }



  /**
   * The subjects in the result are <code>Float</code> objects.
   */

  public static Change[]
  diff(float[] oldVersion, float[] newVersion)
  {
    return diff(Util.toFloatArray(oldVersion), Util.toFloatArray(newVersion));
  }



  /**
   * The subjects in the result are <code>Double</code> objects.
   */

  public static Change[]
  diff(double[] oldVersion, double[] newVersion)
  {
    return diff(Util.toDoubleArray(oldVersion), Util.toDoubleArray(newVersion));
  }



  /**
   * The subjects in the result are <code>Character</code> objects.
   */

  public static Change[]
  diff(String oldVersion, String newVersion)
  {
    return diff(oldVersion.toCharArray(), newVersion.toCharArray());
  }



  /**
   * The subjects in the result are text lines.
   */

  public static Change[]
  diff(Reader oldVersion, Reader newVersion) throws IOException
  {
    return diff(read(oldVersion), read(newVersion));
  }



  private static Change[]
  encodeChanges(Object[] oldVersion, Object[] newVersion, Object[] newArray)
  {
    int		oldPosition = 1;
    List	result = new ArrayList();

    for (int i = 1; i < newArray.length - 1; ++i)
    {
      if (!(newArray[i] instanceof Integer))
      {
        result.add(new Change(INSERT, newVersion[i - 1]));
      }
      else
      {
        for (; oldPosition < ((Integer) newArray[i]).intValue(); ++oldPosition)
        {
          result.add(new Change(DELETE, oldVersion[oldPosition - 1]));
        }

        if (oldPosition == ((Integer) newArray[i]).intValue())
        {
          result.add
          (
            new Change
            (
              UNCHANGED,
              oldVersion[oldPosition++ - 1],
              newVersion[i - 1]
            )
          );
        }
        else
        {
          result.add(new Change(INSERT, newVersion[i - 1]));
        }
      }
    }

    for (int i = oldPosition - 1; i < oldVersion.length; ++i)
    {
      result.add(new Change(DELETE, oldVersion[i]));
    }

    return (Change[]) result.toArray(new Change[0]);
  }



  private static Object[]
  fillSymbolTable(Map symbolTable, Object[] version, boolean old)
  {
    Object[]	result = new Object[version.length + 2];

    for (int i = 0; i < version.length; ++i)
    {
      Entry	entry = (Entry) symbolTable.get(version[i]);

      if (entry == null)
      {
        entry = new Entry();
        symbolTable.put(version[i], entry);
      }

      if (old)
      {
        ++entry.oldCopies;
        entry.oldNumber = i;
      }
      else
      {
        ++entry.newCopies;
      }

      result[i + 1] = entry;
    }

    return result;
  }



  private static boolean
  isUnchanged(Object[] array, int position)
  {
    return
      position > 0 && position < array.length - 1 &&
        array[position] instanceof Integer;
  }



  private static void
  makeConnections
  (
    Object[]	oldArray,
    Object[]	newArray,
    Map		symbolTable,
    Object[]	newVersion
  )
  {
    oldArray[0] = new Integer(0);
    oldArray[oldArray.length - 1] = new Integer(newArray.length - 1);
    newArray[0] = new Integer(0);
    newArray[newArray.length - 1] = new Integer(oldArray.length - 1);

    for (int i = 0; i < newVersion.length; ++i)
    {
      Entry	entry = (Entry) symbolTable.get(newVersion[i]);

      if (entry.oldCopies == 1 && entry.newCopies == 1)
      {
        oldArray[entry.oldNumber + 1] = new Integer(i + 1);
        newArray[i + 1] = new Integer(entry.oldNumber + 1);
      }
    }
  }



  private static Object[]
  read(Reader in) throws IOException
  {
    List	result = new ArrayList();

    BufferedReader	buf = new BufferedReader(in);

    for (String line = buf.readLine(); line != null; line = buf.readLine())
    {
      result.add(line);
    }

    return (Object[]) result.toArray(new Object[0]);
  }



  public static class Change

  {

    public int		operation;
    public Object	peer;
    public Object	subject;



    public
    Change(int operation, Object subject)
    {
      this(operation, subject, null);
    }



    public
    Change(int operation, Object subject, Object peer)
    {
      this.operation = operation;
      this.subject = subject;
      this.peer = peer;
    }



    public boolean
    equals(Object object)
    {
      return
        object instanceof Change && operation == ((Change) object).operation &&
          subject.equals(((Change) object).subject);
    }



    public int
    hashCode()
    {
      return subject.hashCode() + operation;
    }



    /**
     * Converts the <code>subject</code> to a string, prefixed with a code
     * followed by a pipe sign. The code is <q>D</q> for
     * <code>DELETE</code>, <q>I</q> for <code>INSERT</code> and
     * <q>U</q> for <code>UNCHANGED</code>.
     */

    public String
    toString()
    {
      return
        (operation == DELETE ? "D" : (operation == INSERT ? "I" : "U")) + "|" +
          subject.toString();
    }

  } // Operation



  private static class Entry

  {

    private int	newCopies;
    private int	oldCopies;
    private int	oldNumber;

  } // Entry

} // Diff
