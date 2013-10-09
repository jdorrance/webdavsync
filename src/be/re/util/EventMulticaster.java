package be.re.util;

import be.re.util.Array;
import java.util.EventListener;



/**
 * @author Werner Donn\u00e9
 */

public class EventMulticaster

{

  private EventListener[]	listeners = new EventListener[0];



  public synchronized void
  add(EventListener l)
  {
    listeners = (EventListener[]) Array.append(listeners, l);
  }



  public void
  dispatch(DispatchableEvent event)
  {
    EventListener[]	list = listeners;

    for (int i = 0; i < list.length; ++i)
    {
      event.dispatch((EventListener) list[i]);
    }
  }



  public EventListener[]
  getListeners()
  {
    return listeners;
  }



  public synchronized void
  remove(EventListener l)
  {
    int	i = Array.indexOf(listeners, l);

    if (i != -1)
    {
      listeners = (EventListener[]) Array.remove(listeners, i);
    }
  }

} // EventMulticaster
