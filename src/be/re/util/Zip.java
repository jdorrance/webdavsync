package be.re.util;

import be.re.io.DevNullInputStream;
import be.re.io.StreamConnector;
import be.re.io.URLInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;



public class Zip

{

  private static void
  copyEntry(ZipEntry entry, InputStream in, ZipOutputStream out)
    throws IOException
  {
    out.putNextEntry(new ZipEntry(entry));
    StreamConnector.copy(in, out, false, false);
  }



  private static void
  copyZipFile(File zipFile, Action action) throws IOException
  {
    ZipInputStream	in = null;
    ZipOutputStream	out = null;
    FileOutputStream	outFile = null;
    File		tmpFile = null;

    try
    {
      int		count = 0;
      ZipEntry		entry;
      FileInputStream	inFile = new FileInputStream(zipFile);

      // Create temporary file next to the source file in order to make the
      // rename atomic.

      tmpFile =
        be.re.io.Util.
          createTempFile("be.re.util.Zip", ".zip", zipFile.getParentFile());
      in = new ZipInputStream(inFile);
      outFile = new FileOutputStream(tmpFile);
      out = new ZipOutputStream(outFile);

      while ((entry = in.getNextEntry()) != null)
      {
        count += action.handle(entry, in, out);
        in.closeEntry();
        out.closeEntry();
      }

      count += action.post(out);
      in.close();
      in = null;
      inFile.close();

      if (count > 0)
      {
        out.close();
        outFile.close();

        if (!zipFile.delete() || !tmpFile.renameTo(zipFile))
        {
          throw new IOException("Can't rename " + tmpFile + " to " + zipFile);
        }
      }
      else
      {
        outFile.close();
        outFile = null;
        tmpFile.delete();
        new FileOutputStream(zipFile).close();
      }
    }

    catch (Throwable e)
    {
      Util.printStackTrace(e);

      if (outFile != null)
      {
        outFile.close();
        tmpFile.delete();
      }

      if (e instanceof IOException)
      {
        throw (IOException) e;
      }

      throw new RuntimeException(e);
    }

    finally
    {
      if (in != null)
      {
        in.close();
      }
    }
  }



  private static Tuple[]
  createTuples(String[] entries, URL[] urls)
  {
    Tuple[]	tuples = new Tuple[entries.length];

    for (int i = 0; i < tuples.length; ++i)
    {
      tuples[i] = new Tuple(entries[i], urls[i]);
    }

    Arrays.sort
    (
      tuples,
      new Comparator()
      {
        public int
        compare(Object o1, Object o2)
        {
          return ((Tuple) o1).entry.compareTo(((Tuple) o2).entry);
        }
      }
    );

    return tuples;
  }



  public static void
  delete(File zipFile, String[] entries) throws IOException
  {
    final String[]	sortedEntries = new String[entries.length];

    System.arraycopy(entries, 0, sortedEntries, 0, entries.length);
    Arrays.sort(sortedEntries);

    copyZipFile
    (
      zipFile,
      new Action()
      {
        public int
        handle(ZipEntry entry, InputStream in, ZipOutputStream out)
          throws IOException
        {
          if (Arrays.binarySearch(sortedEntries, entry.getName()) < 0)
          {
            copyEntry(entry, in, out);

            return 1;
          }

          return 0;
        }

        public int
        post(ZipOutputStream out) throws IOException
        {
          return 0;
        }
      }
    );
  }



  private static int
  findEntry(Tuple[] tuples, String name)
  {
    return
      Arrays.binarySearch
      (
        tuples,
        name,
        new Comparator()
        {
          public int
          compare(Object o1, Object o2)
          {
            return ((Tuple) o1).entry.compareTo((String) o2);
          }
        }
      );
  }



  /**
   * Closes the streams after reading. The <code>entries</code> and
   * <code>urls</code> arguments should represent a one-to-one mapping.
   * The <code>canMove</code> parameter only affects entries withing the ZIP
   * file.
   */

  public static void
  put(File zipFile, String[] entries, URL[] urls, boolean canMove)
    throws IOException
  {
    final File		file = zipFile;
    final Map		movableUrls =
      canMove ? selectMovableUrls(zipFile, urls) : new HashMap();
    final Tuple[]	tuples = createTuples(entries, urls);

    copyZipFile
    (
      zipFile,
      new Action()
      {
        Set	handled = new HashSet();

        public int
        handle(ZipEntry entry, InputStream in, ZipOutputStream out)
          throws IOException
        {
          if (handled.contains(entry.getName()))
          {
            return 0;
          }

          handled.add(entry.getName());

          if (movableUrls.get(entry.getName()) != null)
          {
            int	i = ((Integer) movableUrls.get(entry.getName())).intValue();

            copyEntry
            (
              renamedEntry(entry, entry.getName(), tuples[i].entry),
              in,
              out
            );

            handled.add(tuples[i].entry);
          }
          else
          {
            int	i = findEntry(tuples, entry.getName());

            if (i >= 0)
            {
              ZipEntry	newEntry = new ZipEntry(entry);

              newEntry.setTime(System.currentTimeMillis());
              out.putNextEntry(newEntry);

              StreamConnector.copy
              (
                tuples[i].url != null ?
                  (InputStream) new URLInputStream(tuples[i].url) :
                  (InputStream) new DevNullInputStream(),
                out,
                true,
                false
              );
            }
            else
            {
              copyEntry(entry, in, out);
            }
          }

          return 1;
        }

        public int
        post(ZipOutputStream out) throws IOException
        {
          int	count = 0;

          for (int i = 0; i < tuples.length; ++i)
          {
            if (!handled.contains(tuples[i].entry)) // New entries.
            {
              ++count;
              out.putNextEntry(new ZipEntry(tuples[i].entry));

              StreamConnector.copy
              (
                tuples[i].url != null ?
                  (InputStream) new URLInputStream(tuples[i].url) :
                  (InputStream) new DevNullInputStream(),
                out,
                true,
                false
              );
            }
          }

          return count;
        }
      }
    );
  }



  public static void
  rename(File zipFile, String entryFrom, String entryTo)
    throws IOException
  {
    final String	localEntryFrom = entryFrom;
    final String	localEntryTo = entryTo;

    copyZipFile
    (
      zipFile,
      new Action()
      {
        public int
        handle(ZipEntry entry, InputStream in, ZipOutputStream out)
          throws IOException
        {
          if (entry.getName().startsWith(localEntryFrom))
          {
            copyEntry
            (
              renamedEntry(entry, localEntryFrom, localEntryTo),
              in,
              out
            );

            return 1;
          }

          if (!entry.getName().startsWith(localEntryTo))
          {
            copyEntry(entry, in, out);

            return 1;
          }

          return 0;
        }

        public int
        post(ZipOutputStream out) throws IOException
        {
          return 0;
        }
      }
    );
  }



  private static ZipEntry
  renamedEntry(ZipEntry entry, String oldName, String newName)
  {
    ZipEntry	newEntry =
      new ZipEntry(newName + entry.getName().substring(oldName.length()));

    if (entry.getComment() != null)
    {
      newEntry.setComment(entry.getComment());
    }

    if (entry.getCrc() != -1)
    {
      newEntry.setCrc(entry.getCrc());
    }

    if (entry.getMethod() != -1)
    {
      newEntry.setMethod(entry.getMethod());
    }

    if (entry.getExtra() != null)
    {
      newEntry.setExtra(entry.getExtra());
    }

    newEntry.setTime(System.currentTimeMillis());

    return newEntry;
  }



  private static Map
  selectMovableUrls(File zipFile, URL[] urls) throws MalformedURLException
  {
    Map	result = new HashMap();

    for (int i = 0; i < urls.length; ++i)
    {
      if
      (
        "jar".equals(urls[i].getProtocol())				 &&
        be.re.net.Util.urlToFile(be.re.net.Util.extractSubUrl(urls[i])).
          equals(zipFile)
      )
      {
        result.put(be.re.net.Util.getJarEntry(urls[i]), new Integer(i));
      }
    }

    return result;
  }



  private interface Action

  {

    public int	handle	(ZipEntry entry, InputStream in, ZipOutputStream out)
			  throws IOException;
    public int	post	(ZipOutputStream out) throws IOException;

  } // Action



  private static class Tuple

  {

    private String	entry;
    private URL		url;



    private
    Tuple(String entry, URL url)
    {
      this.entry = entry;
      this.url = url;
    }

  } // Tuple

} // Zip
