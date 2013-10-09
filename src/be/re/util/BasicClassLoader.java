package be.re.util;

import be.re.cls.ClassFile;
import be.re.cls.Field;
import be.re.cls.LocalVariable;
import be.re.cls.Method;
import be.re.io.StreamConnector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;



/**
 * The only added value of this class is having an concrete class loader.
 * The classes are actually loaded as resources in order to reuse the parent
 * class loader mechanisms for fetching the byte array.
 * @author Werner Donn\u00e9
 */

public class BasicClassLoader extends ClassLoader

{

  private File[]		classPath;
  private static Map		classesPerParent = new HashMap();
  private static String[]	defaultPrefixes =
    {
      "int", "char", "void", "long", "short", "double", "byte", "float",
        "boolean", "java.", "javax."
    };
  private static String[]	excludePrefixes =
    {"javax.xml.stream.", "javax.xml.namespace."};
  private Map			loadedClasses = new HashMap();
  private ClassLoader		parent;
  private Set			parentClasses = new HashSet();
  private String[]		prefixesForParent;
  private String[]		prefixesNotForParent;
  private static PrintStream	tracer;



  static
  {
    try
    {
      tracer =
        System.getProperty("be.re.classloader.trace") != null ?
          new PrintStream
          (
            System.getProperty("be.re.classloader.trace.file") != null ?
              new FileOutputStream
              (
                System.getProperty("be.re.classloader.trace.file")
              ) : System.out
          ) : null;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public
  BasicClassLoader()
  {
    this(new String[0], null);
  }



  public
  BasicClassLoader(ClassLoader parent)
  {
    this(new String[0], parent);
  }



  public
  BasicClassLoader(String[] parentClasses)
  {
    this(parentClasses, null);
  }



  public
  BasicClassLoader(String[] parentClasses, ClassLoader parent)
  {
    this(parentClasses, new String[0], new String[0], parent);
  }



  public
  BasicClassLoader
  (
    String[]	parentClasses,
    String[]	prefixesForParent,
    String[]	prefixesNotForParent,
    ClassLoader	parent
  )
  {
    this
    (
      parentClasses,
      prefixesForParent,
      prefixesNotForParent,
      parent,
      new File[0]
    );
  }



  /**
   * @param parentClasses classes which are to be loaded with the parent class
   * loader.
   * @param prefixesForParent classes that have one of the prefixes in their
   * name are loaded with the parent class loader unless they also have one of
   * the prefixes in <code>prefixesNotForParent</code>.
   * @param prefixesNotForParent see <code>prefixesForParent</code>.
   * @param parent this class loader is only used for loading the classes in
   * <code>parentClasses</code>, with their inferred classes, as well as
   * resources. Note that the <code>getParent</code> method will always return
   * <code>null</code>, no matter the value of <code>parent</code>.
   * @param classPath this path is searched for class files that are not for
   * the parent class loader and for resources. If nothing is found in the path
   * the parent class loader will be consulted. The parameter may contains
   * directories as well as JAR-files.
   */

  public
  BasicClassLoader
  (
    String[]	parentClasses,
    String[]	prefixesForParent,
    String[]	prefixesNotForParent,
    ClassLoader	parent,
    File[]	classPath
  )
  {
    super(null);
    this.parent = parent != null ? parent : ClassLoader.getSystemClassLoader();
    this.prefixesForParent =
      new String[defaultPrefixes.length + prefixesForParent.length];
    this.prefixesNotForParent =
      new String[excludePrefixes.length + prefixesNotForParent.length];
    this.classPath = classPath;

    System.arraycopy
    (
      defaultPrefixes,
      0,
      this.prefixesForParent,
      0,
      defaultPrefixes.length
    );

    System.arraycopy
    (
      prefixesForParent,
      0,
      this.prefixesForParent,
      defaultPrefixes.length,
      prefixesForParent.length
    );

    System.arraycopy
    (
      excludePrefixes,
      0,
      this.prefixesNotForParent,
      0,
      excludePrefixes.length
    );

    System.arraycopy
    (
      prefixesNotForParent,
      0,
      this.prefixesNotForParent,
      excludePrefixes.length,
      prefixesNotForParent.length
    );

    try
    {
      inferClasses(parentClasses);
    }

    catch (IOException e)
    {
      new RuntimeException(e);
    }
  }



  protected Class
  findClass(String className) throws ClassNotFoundException
  {
    if (!isNotForParent(className))
    {
      trace(className + ": parent classloader");

      return parent.loadClass(className);
    }

    Class	loaded = (Class) loadedClasses.get(className);

    if (loaded == null)
    {
      loaded = loadClassAsResource(className);
      loadedClasses.put(className, loaded);
    }

    return loaded;
  }



  private InputStream
  getClassStream(String name) throws IOException
  {
    Map		classes = (Map) classesPerParent.get(parent);

    if (classes == null)
    {
      classes = new HashMap();
      classesPerParent.put(parent, classes);
    }

    byte[]	byteCode = (byte[]) classes.get(name);

    if (byteCode != null)
    {
      return new ByteArrayInputStream(byteCode);
    }

    ByteArrayOutputStream	out = new ByteArrayOutputStream();
    InputStream			in =
      getResourceAsStream(name.replace('.', '/') + ".class");

    if (in == null)
    {
      return null;
    }

    StreamConnector.copy(in, out);
    byteCode = out.toByteArray();
    classes.put(name, byteCode);

    return new ByteArrayInputStream(byteCode);
  }



  public URL
  getResource(String name)
  {
    for (int i = 0; i < classPath.length; ++i)
    {
      if (classPath[i].isFile() && classPath[i].getName().endsWith(".jar"))
      {
        ZipFile	zip = null;

        try
        {
          zip = new ZipFile(classPath[i]);

          if (zip.getEntry(name) != null)
          {
            URL	url =
              new URL
              (
                "jar:" + be.re.net.Util.fileToUrl(classPath[i]) + "!/" + name
              );

            trace(name + ": " + url.toString());

            return url;
          }
        }

        catch (Exception e)
        {
        }

        finally
        {
          try
          {
            if (zip != null)
            {
              zip.close();
            }
          }

          catch (Exception ex)
          {
          }
        }
      }
      else
      {
        if (classPath[i].isDirectory() && new File(classPath[i], name).exists())
        {
          try
          {
            URL	url = be.re.net.Util.fileToUrl(new File(classPath[i], name));

            trace(name + ": " + url.toString());

            return url;
          }

          catch (Exception e)
          {
          }
        }
      }
    }

    trace(name + ": parent as resource");

    return parent.getResource(name);
  }



  public InputStream
  getResourceAsStream(String name)
  {
    for (int i = 0; i < classPath.length; ++i)
    {
      if (classPath[i].isFile() && classPath[i].getName().endsWith(".jar"))
      {
        try
        {
          final ZipFile	zip = new ZipFile(classPath[i]);
          ZipEntry	entry = zip.getEntry(name);

          if (zip.getEntry(name) != null)
          {
            trace
            (
              name + ": jar:" + be.re.net.Util.fileToUrl(classPath[i]) + "!/" +
                name
            );

            return
              new FilterInputStream(zip.getInputStream(entry))
              {
                public void
                close() throws IOException
                {
                  super.close();
                  zip.close();
                }
              };
          }
        }

        catch (Exception e)
        {
        }
      }
      else
      {
        if (classPath[i].isDirectory() && new File(classPath[i], name).exists())
        {
          try
          {
            trace
            (
              name + ": " +
                be.re.net.Util.fileToUrl(new File(classPath[i], name)).
                  toString()
            );

            return new FileInputStream(new File(classPath[i], name));
          }

          catch (Exception e)
          {
          }
        }
      }
    }

    trace(name + ": parent as resource");

    return parent.getResourceAsStream(name);
  }



  private static boolean
  hasPrefix(String s, String[] prefixes)
  {
    for (int i = 0; i < prefixes.length; ++i)
    {
      if (s.startsWith(prefixes[i]))
      {
        return true;
      }
    }

    return false;
  }



  private void
  inferClasses(String[] classes) throws IOException
  {
    for (int i = 0; i < classes.length; ++i)
    {
      String	s =
        classes[i].indexOf('[') != -1 ?
          classes[i].substring(0, classes[i].indexOf('[')) : classes[i];

      if (isNotForParent(s))
      {
        InputStream	in = getClassStream(s);

        if (in != null)
        {
          parentClasses.add(s);

          ClassFile	classFile = ClassFile.parse(in);

          if (classFile.getSuperClassType() != null)
          {
            inferClasses(new String[] {classFile.getSuperClassType()});
          }

          inferClasses(classFile.getInterfaceTypes());
          inferFieldClasses(classFile.getFields());
          inferMethodClasses(classFile.getMethods());
        }
      }
    }
  }



  private void
  inferFieldClasses(Field[] fields) throws IOException
  {
    for (int i = 0; i < fields.length; ++i)
    {
      inferClasses(new String[] {fields[i].getType()});
    }
  }



  private void
  inferMethodClasses(Method[] methods) throws IOException
  {
    for (int i = 0; i < methods.length; ++i)
    {
      inferClasses(methods[i].getExceptionTypes());
      inferClasses(methods[i].getParameterTypes());
      inferClasses(new String[] {methods[i].getReturnType()});

      if (methods[i].getCode() != null)
      {
        inferVariableClasses(methods[i].getCode().getLocalVariables());
      }
    }
  }



  private void
  inferVariableClasses(LocalVariable[] variables) throws IOException
  {
    for (int i = 0; i < variables.length; ++i)
    {
      inferClasses(new String[] {variables[i].getType()});
    }
  }



  private boolean
  isNotForParent(String className)
  {
    return
      !parentClasses.contains(className) &&
        (
          !hasPrefix(className, prefixesForParent) ||
            hasPrefix(className, prefixesNotForParent)
        );
  }



  public Class
  loadClass(String name, boolean resolve) throws ClassNotFoundException
  {
    Class	c = findClass(name);

    if (resolve)
    {
      resolveClass(c);
    }

    return c;
  }



  private Class
  loadClassAsResource(String className) throws ClassNotFoundException
  {
    InputStream	in =
      getResourceAsStream(className.replace('.', '/') + ".class");

    if (in == null)
    {
      throw new ClassNotFoundException(className);
    }

    ByteArrayOutputStream	out = new ByteArrayOutputStream();

    try
    {
      StreamConnector.copy(in, out);
    }

    catch (IOException e)
    {
      throw new ClassNotFoundException(className + ": " + e.getMessage());
    }

    int	index = className.lastIndexOf('.');

    if (index != -1)
    {
      String	name = className.substring(0, index);

      if (getPackage(name) == null)
      {
        definePackage(name, null, null, null, null, null, null, null);
      }
    }

    byte[]	b = out.toByteArray();

    return defineClass(className, b, 0, b.length);
  }



  private static void
  trace(String s)
  {
    if (tracer != null)
    {
      tracer.println(s);
      tracer.flush();
    }
  }

} // BasicClassLoader
