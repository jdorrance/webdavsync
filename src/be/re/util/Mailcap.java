package be.re.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



/**
 * Searches for external command-lines in mailcap files in the following order:
 * <ol>
 *   <li>The file <code>.mailcap</code> in the user's home directory.</li>
 *   <li>The file <i>&lt;java.home><code>/lib/mailcap</code>.</li>
 *   <li>The resource named <code>/META-INF/mailcap</code>.</li>
 *   <li>The resource named <code>/META-INF/mailcap.default</code>.</li>
 *   <li>The resource named <code>/META-INF/mailcap.Windows</code> if the
 *   host is a Windows system.</li>
 *   <li>The resource named <code>/META-INF/mailcap.MacOSX</code> if the
 *   host is a Mac OS X system.</li>
 *   <li>The file <code>/etc/mailcap</code> if the host is any other
 *   system.</li>
 * </ol>
 * @author Werner Donn\u00e9
 */

public class Mailcap

{

  private static final Map	map = load();



  private static String[]
  createCommandLineArray(String s)
  {
    List	result = new ArrayList();

    for (int i = 0; i < s.length(); ++i)
    {
      char	c = s.charAt(i);

      if (c == '"' || c == '\'')
      {
        int	index = findMatching(s, i, c);

        if (index == -1)
        {
          return new String[0];
        }

        result.add(s.substring(i + 1, index));
        i = index;
      }
      else
      {
        if (!Character.isWhitespace(c))
        {
          int	index = findWhitespace(s, i);

          if (index == -1)
          {
            index = s.length();
          }

          result.add(s.substring(i, index));
          i = index;
        }
      }
    }

    return (String[]) result.toArray(new String[0]);
  }



  public static Process
  exec(URL url)
  {
    return exec(url, null);
  }



  public static Process
  exec(URL url, String mimeType)
  {
    if (mimeType == null)
    {
      mimeType = MimeType.getContentTypeFromName(url.getFile());
    }

    if (mimeType == null)
    {
      mimeType = "*/*";
    }

    List	commandLines =
      getCommandLines(MimeType.stripParameters(mimeType));

    if (commandLines == null)
    {
      return null;
    }

    for (Iterator i = commandLines.iterator(); i.hasNext();)
    {
      List	command = new ArrayList();
      String[]	commandLine = (String[]) i.next();

      command.add(unescape(commandLine[0]));

      for (int j = 1; j < commandLine.length; ++j)
      {
        String	processed = processCommandLine(commandLine[j], url, mimeType);

        if (processed != null)
        {
          command.add(processed);
        }
      }

      try
      {
        return
          Runtime.getRuntime().exec((String[]) command.toArray(new String[0]));
      }

      catch (IOException e)
      {
        Util.printStackTrace(e);

        // Ignore and try the next one.
      }
    }

    return null;
  }



  private static int
  findMatching(String s, int position, char c)
  {
    int	index = s.indexOf(c, position + 1);

    return
      index == -1 ?
        -1 : (s.charAt(index - 1) != '\\' ? index : findMatching(s, index, c));
  }



  private static int
  findWhitespace(String s, int position)
  {
    for (int i = position; i < s.length(); ++i)
    {
      if (Character.isWhitespace(s.charAt(i)))
      {
        return i;
      }
    }

    return -1;
  }



  private static List
  getCommandLines(String mimeType)
  {
    List	result = (List) map.get(mimeType);

    if (result == null)
    {
      result = (List) map.get(MimeType.getMediaType(mimeType));
    }

    if (result == null)
    {
      result = (List) map.get(MimeType.getMediaType(mimeType) + "/*");
    }

    if (result == null)
    {
      result = (List) map.get("*");
    }

    if (result == null)
    {
      result = (List) map.get("*/*");
    }

    return result;
  }



  private static InputStream
  getFileInputStream(String filename)
  {
    try
    {
      return new FileInputStream(filename);
    }

    catch (FileNotFoundException e)
    {
      return null;
    }
  }



  private static Map
  load()
  {
    Map	result = new HashMap();

    try
    {
      parse
      (
        getFileInputStream(System.getProperty("user.home") + "/.mailcap"),
        result
      );

      parse
      (
        getFileInputStream(System.getProperty("java.home") + "/lib/mailcap"),
        result
      );

      parse
      (
        Mailcap.class.getResourceAsStream("/META-INF/mailcap"),
        result
      );

      parse
      (
        Mailcap.class.getResourceAsStream("/META-INF/mailcap.default"),
        result
      );

      if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
      {
        parse
        (
          Mailcap.class.getResourceAsStream("/META-INF/mailcap.Windows"),
          result
        );
      }
      else
      {
        if (System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
        {
          parse
          (
            Mailcap.class.getResourceAsStream("/META-INF/mailcap.MacOSX"),
            result
          );
        }
        else
        {
          parse(getFileInputStream("/etc/mailcap"), result);
        }
      }
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    return result;
  }



  private static void
  load(String[] entries, Map result)
  {
    for (int i = 0; i < entries.length; ++i)
    {
      int	index = entries[i].indexOf(';');

      if (index != -1)
      {
        int		matching = findMatching(entries[i], index, ';');
        String		type =
          entries[i].substring(0, index).trim().toLowerCase();

        if (result.get(type) == null)
        {
          String[]	commandLineArray =
            createCommandLineArray
            (
              entries[i].substring
              (
                index + 1,
                matching != -1 ? matching : entries[i].length()
              )
            );

          if (commandLineArray.length > 0)
          {
            List	commands = (List) result.get(type);

            if (commands == null)
            {
              commands = new ArrayList();
              result.put(type, commands);
            }

            commands.add(commandLineArray);
          }
        }
      }
    }
  }



  public static void
  main(String[] args) throws Exception
  {
    String	type = null;
    URL		url = null;

    for (int i = 0; i < args.length; ++i)
    {
      if ("-t".equals(args[i]))
      {
        if (i == args.length - 1)
        {
          usage();

          return;
        }

        type = args[++i];
      }
      else
      {
        if ("-h".equals(args[i]))
        {
          usage();

          return;
        }
        else
        {
          if (url != null)
          {
            usage();

            return;
          }

          url =
            be.re.net.Util.isUrl(args[i]) ?
              new URL(args[i]) : be.re.net.Util.fileToUrl(new File(args[i]));
        }
      }
    }

    if (url == null)
    {
      usage();

      return;
    }

    if (type == null && !"file".equals(url.getProtocol()))
    {
      try
      {
        type = url.openConnection().getContentType();
      }

      catch (Exception e)
      {
      }
    }

    Process	process = exec(url, type);

    if (process != null)
    {
      Util.waitFor(process);
    }
  }



  private static void
  parse(InputStream in, Map result) throws IOException
  {
    if (in != null)
    {
      load(Util.readLineConfig(in), result);
    }
  }



  private static String
  processCommandLine(String entry, URL url, String mimeType)
  {
    return
      "%s".equals(entry) ?
        (
          "file".equals(url.getProtocol()) ?
            be.re.net.Util.urlToFile(url).getAbsolutePath() : null
        ) :
        (
          "%u".equals(entry) ?
            url.toString() :
            (
              "%t".equals(entry) ?
                MimeType.stripParameters(mimeType) :
                (
                  entry.startsWith("%{") && entry.endsWith("}") &&
                    !entry.endsWith("\\}") ?
                    MimeType.getParameter
                    (
                      mimeType,
                      unescape(entry.substring(2, entry.length() - 1))
                    ) : unescape(entry)
                )
            )
        );
  }



  private static String
  unescape(String s)
  {
    return s.replaceAll("\\\\", "");
  }



  private static void
  usage()
  {
    System.err.
      println("Usage: be.re.util.Mailcap [-h] [-t mime_type] file_or_url");
  }

} // Mailcap
