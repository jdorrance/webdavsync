package be.re.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;



public class ClassUtil

{

  public static String
  browse(Class c)
  {
    String	result =
      getModifiers(c.getModifiers()) + (c.isInterface() ? "" : "class ") +
        getClassName(c) + "\n";

    if (c.getSuperclass() != null)
    {
      result += "  extends " + getClassName(c.getSuperclass()) + "\n";
    }

    Class[]	intfs = c.getInterfaces();

    if (intfs.length > 0)
    {
      result += "  implements\n";
    }

    for (int i = 0; i < intfs.length; ++i)
    {
      result +=
        "    " + getClassName(intfs[i]) + (i < intfs.length - 1 ? ",\n" : "\n");
    }

    return
      result + "{\n" + getFields(c.getDeclaredFields()) + "\n" +
        getConstructors(c.getDeclaredConstructors()) + "\n" +
        getMethods(c.getDeclaredMethods()) + "}";
  }



  private static String
  getClassName(Class c)
  {
    return
      c.isArray() ? (getClassName(c.getComponentType()) + "[]") : c.getName();
  }



  private static String
  getConstructors(Constructor[] constructors)
  {
    String	result = "";

    for (int i = 0; i < constructors.length; ++i)
    {
      result +=
        "  " + getModifiers(constructors[i].getModifiers()) +
          constructors[i].getName() +
          getParameters(constructors[i].getParameterTypes()) +
          (
            (constructors[i].getExceptionTypes().length > 0) ?
              ("\n    " + getThrows(constructors[i].getExceptionTypes())) : ""
          ) + ";\n";
    }

    return result;
  }



  private static String
  getFields(Field[] fields)
  {
    Sort.qsort
    (
      fields,
      new Compare()
      {
        public int
        compare(Object object1, Object object2)
        {
          return
            ((Field) object1).getName().compareTo(((Field) object2).getName());
        }
      }
    );

    String	result = "";

    for (int i = 0; i < fields.length; ++i)
    {
      result +=
        "  " + getModifiers(fields[i].getModifiers()) +
          getClassName(fields[i].getType()) + " " + fields[i].getName() + ";\n";
    }

    return result;
  }



  private static String
  getMethods(Method[] methods)
  {
    Sort.qsort
    (
      methods,
      new Compare()
      {
        public int
        compare(Object object1, Object object2)
        {
          return
            ((Method) object1).getName().
              compareTo(((Method) object2).getName());
        }
      }
    );

    String	result = "";

    for (int i = 0; i < methods.length; ++i)
    {
      result +=
        "  " + getModifiers(methods[i].getModifiers()) +
          getClassName(methods[i].getReturnType()) + " " +
          methods[i].getName() +
          getParameters(methods[i].getParameterTypes()) +
          (
            (methods[i].getExceptionTypes().length > 0) ?
              ("\n    " + getThrows(methods[i].getExceptionTypes())) : ""
          ) + ";\n";
    }

    return result;
  }



  private static String
  getModifiers(int modifiers)
  {
    return modifiers == 0 ? "" : (Modifier.toString(modifiers) + " ");
  }



  private static String
  getParameters(Class[] types)
  {
    String	result = "(";

    for (int i = 0; i < types.length; ++i)
    {
      result += (i > 0 ? ", " : "") + getClassName(types[i]);
    }

    return result + ")";
  }



  private static String
  getThrows(Class[] types)
  {
    if (types.length == 0)
    {
      return "";
    }

    String	result = "throws ";

    for (int i = 0; i < types.length; ++i)
    {
      result += (i > 0 ? ", " : "") + getClassName(types[i]);
    }

    return result;
  }

} // ClassUtil
