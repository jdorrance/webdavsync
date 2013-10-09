package be.re.cls;

public class LocalVariable

{

  String	descriptor;
  short		index;
  short		length;
  String	name;
  short		startPC;



  public String
  getDescriptor()
  {
    return descriptor;
  }



  public short
  getIndex()
  {
    return index;
  }



  public short
  getLength()
  {
    return length;
  }



  public String
  getName()
  {
    return name;
  }



  public int
  getStartProgramCounter()
  {
    return startPC;
  }



  public String
  getType()
  {
    return Util.getType(descriptor);
  }

} // LocalVariable
