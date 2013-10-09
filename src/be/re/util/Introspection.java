package be.re.util;

import java.beans.EventSetDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;



public class Introspection

{

  public static EventSetDescriptor
  getEvent(Object object, String eventName)
  {
    EventSetDescriptor[]	descriptors;

    try
    {
      descriptors =
        Introspector.getBeanInfo(object.getClass()).getEventSetDescriptors();
    }

    catch (Throwable e)
    {
      return null;
    }

    for (int i = 0; i < descriptors.length; ++i)
    {
      if (descriptors[i].getName().equals(eventName))
      {
        return descriptors[i];
      }
    }

    return null;
  }



  public static PropertyDescriptor
  getProperty(Object object, String propertyName)
  {
    PropertyDescriptor[]	descriptors;

    try
    {
      descriptors =
        Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors();
    }

    catch (Throwable e)
    {
      return null;
    }

    for (int i = 0; i < descriptors.length; ++i)
    {
      if (descriptors[i].getName().equals(propertyName))
      {
        return descriptors[i];
      }
    }

    return null;
  }

} // Introspection
