package be.re.cls;

import java.util.ArrayList;
import java.util.List;



public class Method

{

  Attribute[]	attributes;
  String	className;
  Code		code;
  String	descriptor;
  String[]	exceptions;
  boolean	isDeprecated;
  boolean	isSynthetic;
  int		modifiers;
  String	name;



  public Attribute[]
  getAttribtutes()
  {
    return attributes;
  }



  public Code
  getCode()
  {
    return code;
  }



  public String
  getDescriptor()
  {
    return descriptor;
  }



  public String[]
  getExceptionNames()
  {
    return exceptions;
  }



  public String[]
  getExceptionTypes()
  {
    String[]	result = new String[exceptions.length];

    for (int i = 0; i < exceptions.length; ++i)
    {
      result[i] = Util.getType(exceptions[i]);
    }

    return result;
  }



  public int
  getModifiers()
  {
    return modifiers;
  }



  public String
  getName()
  {
    return
      isConstructor() ?
        Util.getType(className).
          substring(Util.getType(className).lastIndexOf('.') + 1) :
        name;
  }



  public String[]
  getParameterTypes()
  {
    boolean     array = false;
    List	result = new ArrayList();

    for (int i = 1; i < descriptor.length() && descriptor.charAt(i) != ')';)
    {
      if (descriptor.charAt(i) != '[')
      {
        int	currentPos = i;

        i =
          descriptor.charAt(i) == 'L' ?
            (descriptor.indexOf(';', i + 1) + 1) : (i + 1);

        result.add
        (
          Util.getType(descriptor.substring(currentPos, i)) +
            (array ? "[]" : "")
        );

        array = false;
      }
      else
      {
        ++i;
        array = true;
      }
    }

    return (String[]) result.toArray(new String[result.size()]);
  }



  public String
  getReturnType()
  {
    return Util.getType(descriptor.substring(descriptor.lastIndexOf(')') + 1));
  }



  public boolean
  isConstructor()
  {
    return name.equals("<init>");
  }



  public boolean
  isDeprecated()
  {
    return isDeprecated;
  }



  public boolean
  isInitializer()
  {
    return name.equals("<clinit>");
  }



  public boolean
  isSynthetic()
  {
    return isSynthetic;
  }

} // Method
