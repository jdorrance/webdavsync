package be.re.cls;

public class Util

{

  private static final String[][]	basicTypes =
    {
      {"B", "byte"},
      {"C", "char"},
      {"D", "double"},
      {"F", "float"},
      {"I", "int"},
      {"J", "long"},
      {"S", "short"},
      {"V", "void"},
      {"Z", "boolean"}
    };



  private static String
  getBasicType(String descriptor)
  {
    for (int i = 0; i < basicTypes.length; ++i)
    {
      if (basicTypes[i][0].equals(descriptor))
      {
        return basicTypes[i][1];
      }
    }

    return null;
  }



  public static String
  getType(String descriptor)
  {
    return
      descriptor.charAt(0) == 'L' &&
        descriptor.charAt(descriptor.length() - 1) == ';' ?
        getType(descriptor.substring(1, descriptor.length() - 1)) :
        (
          descriptor.charAt(0) == '[' ?
            (getType(descriptor.substring(1)) + "[]") :
            (
              getBasicType(descriptor) != null ?
                getBasicType(descriptor) : descriptor.replace('/', '.')
            )
        );
  }

} // Util
