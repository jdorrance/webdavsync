package be.re.util;

/**
 * With this class basic type classes can be converted to their object
 * counterpart and back. This can be interesting when doing generic
 * serialization operations.
 * @author Werner Donn\u00e9
 */

public class BasicTypeToClass

{

  /**
   * Returns the class object of the object version of a basic type.
   * If no such class object exists the parameter itself is returned.
   * @param the class object of the basic type.
   * @return the class object of the corresponding object class.
   */

  public static Class
  basicTypeToClass(Class classObject)
  {
    if (classObject == boolean.class)
    {
      return Boolean.class;
    }

    if (classObject == byte.class)
    {
      return Byte.class;
    }

    if (classObject == char.class)
    {
      return Character.class;
    }

    if (classObject == double.class)
    {
      return Double.class;
    }

    if (classObject == float.class)
    {
      return Float.class;
    }

    if (classObject == int.class)
    {
      return Integer.class;
    }

    if (classObject == long.class)
    {
      return Long.class;
    }

    if (classObject == short.class)
    {
      return Short.class;
    }

    if (classObject == void.class)
    {
      return Void.class;
    }

    return classObject;
  }



  /**
   * Returns the class object of the basic type corresponding to the object.
   * If no such class object exists the parameter itself is returned.
   * @param the class object of the object version.
   * @return the class object of the corresponding basic type.
   */

  public static Class
  classToBasicType(Class classObject)
  {
    if (classObject == Boolean.class)
    {
      return boolean.class;
    }

    if (classObject == Byte.class)
    {
      return byte.class;
    }

    if (classObject == Character.class)
    {
      return char.class;
    }

    if (classObject == Double.class)
    {
      return double.class;
    }

    if (classObject == Float.class)
    {
      return float.class;
    }

    if (classObject == Integer.class)
    {
      return int.class;
    }

    if (classObject == Long.class)
    {
      return long.class;
    }

    if (classObject == Short.class)
    {
      return short.class;
    }

    if (classObject == Void.class)
    {
      return void.class;
    }

    return classObject;
  }

} // BasicTypeToClass
