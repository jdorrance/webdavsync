package be.re.cls;

public class Code

{

  Attribute[]		attributes;
  byte[]		code;
  ExceptionHandler[]	exceptions;
  LocalVariable[]	localVariables;
  short			maxLocals;
  short			maxStack;



  public Attribute[]
  getAttributes()
  {
    return attributes;
  }



  public byte[]
  getCode()
  {
    return code;
  }



  public ExceptionHandler[]
  getExceptionHandler()
  {
    return exceptions;
  }



  public LocalVariable[]
  getLocalVariables()
  {
    return localVariables;
  }



  public short
  getMaxLocals()
  {
    return maxLocals;
  }



  public short
  getMaxStack()
  {
    return maxStack;
  }

} // Code
