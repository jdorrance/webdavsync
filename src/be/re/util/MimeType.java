package be.re.util;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.ImageIcon;



/**
 * Some MIME type utilities.
 *
 * For the MIME type mappings it searches for files in the following order:
 * <ol>
 *   <li>The file <code>.mime.types</code> in the user's home directory.</li>
 *   <li>The file <i>&lt;java.home><code>/lib/mime.types</code>.</li>
 *   <li>The resource named <code>/META-INF/mime.types</code>.</li>
 *   <li>The resource named <code>/META-INF/mimetypes.default</code>.</li>
 *   <li>The file <code>/etc/mime.types</code>.</li>
 *   <li>The resource named <code>/be/re/util/res/mime_types.map</code>.</li>
 * </ol>
 * @author Werner Donn\u00e9
 */

public class MimeType

{

  private static Map			extensions = null;
  private static Map			icons = null;
  private static LabeledMimeType[]	knownLabeledMimeTypes = null;
  private static String[]		knownMimeTypes = null;
  private static Map			labels = null;
  private static Map			mimeTypes = null;



  public static String
  adjustExtension(String filename, String mimeType)
  {
    int	index = filename.lastIndexOf('.');

    if
    (
      index != -1							   &&
      mimeType.
        equals(getContentTypeFromExtension(filename.substring(index + 1)))
    )
    {
      return filename;
    }

    String[]	extensions = getExtensionsFromMimeType(mimeType);

    if (extensions.length == 0)
    {
      return filename;
    }

    return
      (index == -1 ? filename : filename.substring(0, index)) + "." +
        extensions[0];
  }



  /**
   * Returns the MIME type in lower case and the parameters in alphabetical
   * order, which facilitates comparison.
   */

  public static String
  canonical(String mimeType)
  {
    SortedSet	parameterStrings = new TreeSet();
    Map		parameters = getParameters(mimeType);
    String	result = stripParameters(mimeType);

    for (Iterator i = parameters.keySet().iterator(); i.hasNext();)
    {
      String	key = (String) i.next();

      parameterStrings.add(key + "=" + (String) parameters.get(key));
    }

    for (Iterator i = parameterStrings.iterator(); i.hasNext();)
    {
      result += ";" + (String) i.next();
    }

    return result;
  }



  /**
   * Tries to detect the MIME type through magic numbers. If the type is not
   * detected application/octet-stream is returned.
   */

  public static String
  detectType(byte[] b)
  {
    return
      b.length > 5 &&
        Array.equal(b, new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}, 6) ?
        "image/gif" :
        (
          b.length > 3 && b[0] == 0xff && b[1] == 0xd8 &&
            b[b.length - 2] == 0xff && b[b.length - 1] == 0xd9 ?
            "image/jpeg" :
            (
              b.length > 7 &&
                Array.equal
                (
                  b,
                  new byte[]
                  {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
                  8
                ) ? "image/png" : "application/octet-stream"
            )
        );
  }



  /**
   * The default is application/octet-stream.
   */

  public static String
  getContentTypeFromExtension(String extension)
  {
    if (mimeTypes == null)
    {
      loadMimeTypeMap();
    }

    String	result = (String) extensions.get(extension);

    return result != null ? result : "application/octet-stream";
  }



  /**
   * The default is application/octet-stream.
   */

  public static String
  getContentTypeFromName(String name)
  {
    int	index = name.lastIndexOf('.');

    return
      index != -1 ?
        getContentTypeFromExtension(name.substring(index + 1)) :
        "application/octet-stream";
  }



  public static String[]
  getExtensionsFromMimeType(String mimeType)
  {
    if (extensions == null)
    {
      loadMimeTypeMap();
    }

    List	result = (List) mimeTypes.get(stripParameters(mimeType));

    return
      result != null ? (String[]) result.toArray(new String[0]) : new String[0];
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



  /**
   * Returns the known MIME types sorted by label.
   */

  public static LabeledMimeType[]
  getKnownLabeledMimeTypes()
  {
    if (knownLabeledMimeTypes == null)
    {
      String[]	mimeTypes = getKnownMimeTypes();
      Map	result = new HashMap();

      for (int i = 0; i < mimeTypes.length; ++i)
      {
        try
        {
          String		label =
            Util.getResource("label_mime_" + mimeTypes[i]);
          LabeledMimeType	mimeType =
            (LabeledMimeType) result.get(new LabeledMimeType(null, label));

          if (mimeType == null)
          {
            mimeType = new LabeledMimeType(new String[]{mimeTypes[i]}, label);
            result.put(mimeType, mimeType);
          }
          else
          {
            mimeType.mimeTypes =
              (String[]) Array.append(mimeType.mimeTypes, mimeTypes[i]);
          }
        }

        catch (MissingResourceException e)
        {
          result.put
          (
            new LabeledMimeType(null, mimeTypes[i]),
            new LabeledMimeType(new String[]{mimeTypes[i]}, mimeTypes[i])
          );
        }
      }

      knownLabeledMimeTypes =
        (LabeledMimeType[]) result.values().toArray(new LabeledMimeType[0]);

      Arrays.sort
      (
        knownLabeledMimeTypes,
        new Comparator()
        {
          public int
          compare(Object o1, Object o2)
          {
            return o1.toString().compareTo(o2.toString());
          }
        }
      );
    }

    return knownLabeledMimeTypes;
  }



  public static String[]
  getKnownMimeTypes()
  {
    try
    {
      if (knownMimeTypes == null)
      {
        knownMimeTypes =
          Util.readLineConfig
          (
            new BufferedReader
            (
              new InputStreamReader
              (
                MimeType.class.getResourceAsStream("res/mime_types")
              )
            )
          );
      }

      return knownMimeTypes;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  /**
   * Returns the media type in lower case.
   */

  public static String
  getMediaType(String mimeType)
  {
    int	index = mimeType.indexOf('/');

    return
      index == -1 ?
        mimeType.toLowerCase() : mimeType.substring(0, index).toLowerCase();
  }



  public static ImageIcon
  getMimeTypeIcon(String mimeType)
  {
    if (icons == null)
    {
      Properties	properties = new Properties();

      try
      {
        properties.load(MimeType.class.getResourceAsStream("res/icons.map"));
      }

      catch (IOException e)
      {
        throw new RuntimeException(e);
      }

      icons = new HashMap();

      for (Iterator i = properties.keySet().iterator(); i.hasNext();)
      {
        String		key = (String) i.next();
        ImageIcon	icon =
          new ImageIcon
          (
            MimeType.class.getResource(properties.getProperty(key))
          );

        icon.setImage
        (
          icon.getImage().getScaledInstance(16, -1, Image.SCALE_SMOOTH)
        );

        icons.put(key, icon);
      }
    }

    return (ImageIcon) icons.get(mimeType);
  }



  public static String
  getMimeTypeLabel(String mimeType)
  {
    if (labels == null)
    {
      labels = new HashMap();

      LabeledMimeType[]	labeled = getKnownLabeledMimeTypes();

      for (int i = 0; i < labeled.length; ++i)
      {
        for (int j = 0; j < labeled[i].mimeTypes.length; ++j)
        {
          labels.put(labeled[i].mimeTypes[j], labeled[i].label);
        }
      }
    }

    return (String) labels.get(mimeType);
  }



  public static String
  getParameter(String mimeType, String name)
  {
    StringTokenizer	tokenizer = new StringTokenizer(mimeType, ";");

    tokenizer.nextToken();

    while (tokenizer.hasMoreTokens())
    {
      String	parameter = tokenizer.nextToken().trim();
      int	index = parameter.indexOf('=');

      if
      (
        index != -1							&&
        (name.equalsIgnoreCase(parameter.substring(0, index).trim()))
      )
      {
        StringTokenizer	value =
          new StringTokenizer(parameter.substring(index + 1), " \"'");

        if (value.countTokens() == 1)
        {
          return value.nextToken();
        }
      }
    }

    return null;
  }



  /**
   * The parameter names in lower case are the keys. The values are copied
   * literally.
   */

  public static Map
  getParameters(String mimeType)
  {
    Map	result = new HashMap();

    StringTokenizer	tokenizer = new StringTokenizer(mimeType, ";");

    tokenizer.nextToken();

    while (tokenizer.hasMoreTokens())
    {
      String	parameter = tokenizer.nextToken().trim();
      int	index = parameter.indexOf('=');

      if (index != -1)
      {
        StringTokenizer	value =
          new StringTokenizer(parameter.substring(index + 1), " \"'");

        if (value.countTokens() == 1)
        {
          result.put
          (
            parameter.substring(0, index).trim().toLowerCase(),
            value.nextToken()
          );
        }
      }
    }

    return result;
  }



  /**
   * Returns the subtype in lower case or <code>null</code> if there is no
   * subtype.
   */

  public static String
  getSubtype(String mimeType)
  {
    int	index1 = mimeType.indexOf('/');
    int	index2 = mimeType.indexOf(';');

    return
      index1 == -1 ?
        "*" :
        (
          index2 == -1 ?
            mimeType.substring(index1 + 1).toLowerCase() :
            mimeType.substring(index1 + 1, index2).trim().toLowerCase()
        );
  }



  private static void
  loadMimeTypeMap()
  {
    mimeTypes = new HashMap();
    extensions = new HashMap();

    try
    {
      parse
      (
        getFileInputStream(System.getProperty("user.home") + "/.mime.types")
      );

      parse
      (
        getFileInputStream(System.getProperty("java.home") + "/lib/mime.types")
      );

      parse
      (
        MimeType.class.getResourceAsStream("/META-INF/mime.types")
      );

      parse
      (
        MimeType.class.getResourceAsStream("/META-INF/mimetypes.default")
      );

      parse(getFileInputStream("/etc/mime.types"));

      parse
      (
        MimeType.class.getResourceAsStream("res/mime_types.map")
      );
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  private static void
  loadMimeTypeMap(String[] entries)
  {
    for (int i = 0; i < entries.length; ++i)
    {
      String		mimeType = null;
      StringTokenizer	tokenizer = new StringTokenizer(entries[i], " \t");

      while (tokenizer.hasMoreTokens())
      {
        if (mimeType == null)
        {
          mimeType = tokenizer.nextToken().toLowerCase();
        }
        else
        {
          String	extension = tokenizer.nextToken();

          if (extensions.get(extension) == null)
          {
            extensions.put(extension, mimeType);
          }

          List	list = (List) mimeTypes.get(mimeType);

          if (list == null)
          {
            list = new ArrayList();
            mimeTypes.put(mimeType, list);
          }

          if (!list.contains(extension))
          {
            list.add(extension);
          }
        }
      }
    }
  }



  private static void
  parse(InputStream in) throws IOException
  {
    if (in != null)
    {
      loadMimeTypeMap(Util.readLineConfig(in));
    }
  }



  public static String
  setParameter(String mimeType, String name, String value)
  {
    StringTokenizer	tokenizer = new StringTokenizer(mimeType, ";");
    boolean		replaced = false;
    String		result = tokenizer.nextToken().trim();

    while (tokenizer.hasMoreTokens())
    {
      String	parameter = tokenizer.nextToken().trim();
      int	index = parameter.indexOf('=');

      if
      (
        index != -1							&&
        (name.equalsIgnoreCase(parameter.substring(0, index).trim()))
      )
      {
        result +=
          ";" + parameter.substring(0, index).trim() + "=\"" + value + "\"";
        replaced = true;
      }
      else
      {
        result += ";" + parameter;
      }
    }

    if (!replaced)
    {
      result += ";" + name + "=\"" + value + "\"";
    }

    return result;
  }



  /**
   * Returns the MIME type in lower case without its parameters if it has any.
   */

  public static String
  stripParameters(String mimeType)
  {
    int	index = mimeType.indexOf(';');

    return
      index != -1 ?
        mimeType.substring(0, index).trim().toLowerCase() :
        mimeType.trim().toLowerCase();
  }



  /**
   * A representation of MIME types that can be displayed. There is a
   * one-to-many relationship between labels and MIME types, because for
   * certain document formats there are several MIME types in use.
   * @author Werner Donn\u00e9
   */

  public static class LabeledMimeType

  {

    private String	label;
    private String[]	mimeTypes;



    public
    LabeledMimeType(String[] mimeTypes, String label)
    {
      this.mimeTypes = mimeTypes;
      this.label = label;
    }



    /**
     * Returns <code>true</code> if the labels are equal.
     */

    public boolean
    equals(Object o)
    {
      return
        o instanceof LabeledMimeType &&
          label.equals(((LabeledMimeType) o).label);
    }



    public String
    getLabel()
    {
      return label;
    }



    public String[]
    getMimeTypes()
    {
      return mimeTypes;
    }



    public int
    hashCode()
    {
      return label.hashCode();
    }



    /**
     * Calls <code>getLabel()</code>.
     */

    public String
    toString()
    {
      return getLabel();
    }

  } // LabeledMimeType

} // MimeType
