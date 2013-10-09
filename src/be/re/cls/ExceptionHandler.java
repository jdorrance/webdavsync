package be.re.cls;

public class ExceptionHandler

{

  short		endPC;
  short		handlerPC;
  short		startPC;
  String	type;



  public short
  getEndProgramCounter()
  {
    return endPC;
  }



  public short
  getHandlerProgramCounter()
  {
    return handlerPC;
  }



  public String
  getName()
  {
    return type;
  }



  public short
  getStartProgramHandler()
  {
    return startPC;
  }



  public String
  getType()
  {
    return Util.getType(type);
  }

} // ExceptionHandler
