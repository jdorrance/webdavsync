package be.re.cls;

public class Field

{

  Attribute[]	attributes;
  String	descriptor;
  boolean	isDeprecated;
  boolean	isSynthetic;
  int		modifiers;
  String	name;
  Object	value;



  public Attribute[]
  getAttribtutes()
  {
    return attributes;
  }



  public String
  getDescriptor()
  {
    return descriptor;
  }



  public int
  getModifiers()
  {
    return modifiers;
  }



  public String
  getName()
  {
    return name;
  }



  public String
  getType()
  {
    return Util.getType(descriptor);
  }



  public Object
  getValue()
  {
    return value;
  }



  public boolean
  isDeprecated()
  {
    return isDeprecated;
  }



  public boolean
  isSynthetic()
  {
    return isSynthetic;
  }

} // Field
