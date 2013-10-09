package be.re.cls;

import be.re.util.Compare;
import be.re.util.Sort;
import java.io.IOException;
import java.lang.reflect.Modifier;



public class Writer

{

  private static String
  getConstructors(Method[] methods)
  {
    String	result = "";

    for (int i = 0; i < methods.length; ++i)
    {
      if (methods[i].isConstructor() && !methods[i].isSynthetic())
      {
        result +=
          "  " + getModifiers(methods[i].getModifiers()) + "\n  " +
            methods[i].getName() +
            getParameters(methods[i]) +
            (
              (methods[i].getExceptionTypes().length > 0) ?
                ("\n    " + getThrows(methods[i].getExceptionTypes())) : ""
            ) + "\n  {\n  }\n\n";
      }
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
      if (!fields[i].isSynthetic())
      {
        result +=
          "  " + getModifiers(fields[i].getModifiers()) + fields[i].getType() +
            " " + fields[i].getName() + ";\n";
      }
    }

    return result;
  }



  private static String
  getMethods(Method[] methods, boolean isInterface)
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
      if
      (
        !methods[i].isConstructor()	&&
        !methods[i].isInitializer()	&&
        !methods[i].isSynthetic()
      )
      {
        result +=
          "  " + getModifiers(methods[i].getModifiers()) +
            methods[i].getReturnType() + "\n  " +
            methods[i].getName() +
            getParameters(methods[i]) +
            (
              (methods[i].getExceptionTypes().length > 0) ?
                ("\n    " + getThrows(methods[i].getExceptionTypes())) : ""
            ) +
            (
              isInterface || Modifier.isNative(methods[i].getModifiers()) ?
                ";\n\n" : "\n  {\n  }\n\n"
            );
      }
    }

    return result;
  }



  private static String
  getModifiers(int modifiers)
  {
    return modifiers == 0 ? "" : (Modifier.toString(modifiers) + " ");
  }



  private static String
  getParameters(Method method)
  {
    String	result = "(";
    String[]	types = method.getParameterTypes();

    for (int i = 0; i < types.length; ++i)
    {
      result +=
        (i > 0 ? ", " : "") + types[i] + " " +
          (
            method.getCode() != null &&
              method.getCode().getLocalVariables().length >= types.length ?
              (
                method.getCode().getLocalVariables()
                  [i + (Modifier.isStatic(method.getModifiers()) ? 0 : 1)].
                  getName()
              ) : ("p" + String.valueOf(i))
          );
    }

    return result + ")";
  }



  private static String
  getThrows(String[] types)
  {
    if (types.length == 0)
    {
      return "";
    }

    String	result = "throws ";

    for (int i = 0; i < types.length; ++i)
    {
      result += (i > 0 ? ", " : "") + types[i];
    }

    return result;
  }



  public static void
  write(java.io.Writer out, ClassFile c) throws IOException
  {
    if (c.getType().indexOf('.') != -1)
    {
      out.write
      (
        "package " + c.getType().substring(0, c.getType().lastIndexOf('.')) +
          ";\n\n"
      );
    }

    int	modifiers = c.getModifiers() & ~Modifier.SYNCHRONIZED;
      // This also indicated ACC_SUPER.

    out.write
    (
      getModifiers(modifiers) + (c.isInterface() ? "" : "class ") +
        c.getType().substring(c.getType().lastIndexOf('.') + 1) + "\n"
    );

    if
    (
      c.getSuperClassName() != null			&&
      !c.getSuperClassType().equals("java.lang.Object")
    )
    {
      out.write("  extends " + c.getSuperClassType() + "\n");
    }

    String[]	intfs = c.getInterfaceTypes();

    if (intfs.length > 0)
    {
      out.write("  " + (c.isInterface() ? "extends" : "implements") + "\n");
    }

    for (int i = 0; i < intfs.length; ++i)
    {
      out.write("    " + intfs[i] + (i < intfs.length - 1 ? ",\n" : "\n"));
    }

    out.write
    (
      "{\n" + getFields(c.getFields()) + "\n" +
        getConstructors(c.getMethods()) + "\n" +
        getMethods(c.getMethods(), c.isInterface()) + "} // " +
        c.getType().substring(c.getType().lastIndexOf('.') + 1) + "\n"
    );
  }

} // Writer
