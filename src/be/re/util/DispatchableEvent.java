package be.re.util;

import java.util.EventListener;
import java.util.EventObject;



public class DispatchableEvent extends EventObject

{

  public
  DispatchableEvent(Object source)
  {
    super(source);
  }



  public void
  dispatch(EventListener listener)
  {
  }

} // DispatchableEvent
