package be.re.gui.util;

import be.re.util.EventMulticaster;
import be.re.util.Introspection;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventSetDescriptor;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.EventListener;
import javax.swing.AbstractAction;
import javax.swing.Icon;



/**
 * The class implements the <code>Action</code> interface. It can be
 * associated with a set of action aware components. Enabling and disabling
 * will happen in concert for these components. Text and icon properties are
 * directed as well through the action space. Associated components don't have
 * to implements these properties.
 * @see javax.swing.Action
 * @author Werner Donn\u00e9
 */

public class ActionSpace extends AbstractAction implements ActionListener

{

  private EventMulticaster	actionListeners = new EventMulticaster();
  private List			components = new ArrayList();
  private Icon			icon = null;
  private String		name;
  private String		text = null;



  public
  ActionSpace()
  {
    this(null);
  }



  public
  ActionSpace(String name)
  {
    this.name = name;
  }



  /**
   * If one of the associated components performs the action it is propagated
   * to the listeners if the action space object is enabled. The test is
   * necessary because an <code>ActionListener</code> can also be registered
   * with a <code>KeyStroke</code> in which case no component is involved.
   */

  public void
  actionPerformed(ActionEvent e)
  {
    if (isEnabled())
    {
      EventListener[]	listeners = actionListeners.getListeners();

      for (int i = 0; i < listeners.length; ++i)
      {
        ((ActionListener) listeners[i]).actionPerformed(e);
      }
    }
  }



  /**
   * Adds a component to the set if it is action aware, i.e. if it is capable
   * of firing actions. Components are added only once.
   */

  public void
  add(Component value)
  {
    EventSetDescriptor	event = Introspection.getEvent(value, "action");

    if (event == null)
    {
      return;
    }

    try
    {
      // ActionSpace is a listener, so it could already have been added to the
      // listeners list. We want to avoid double registration.
      event.getRemoveListenerMethod().invoke(value, new Object[] {this});
      event.getAddListenerMethod().invoke(value, new Object[] {this});
    }
      
    catch (Throwable e)
    {
      throw new RuntimeException(e);
    }

    synchronized (components)
    {
      if (components.contains(value))
      {
        return;
      }

      components.add(value);
    }

    setText(value);
    setIcon(value);
    value.setEnabled(isEnabled());
  }



  public void
  addActionListener(ActionListener listener)
  {
    actionListeners.add(listener);
  }



  /**
   * Returns all associated components.
   */

  public Component[]
  getComponents()
  {
    return (Component[]) components.toArray(new Component[components.size()]);
  }



  public Icon
  getIcon()
  {
    return icon;
  }



  public String
  getName()
  {
    return name;
  }



  public String
  getText()
  {
    return text;
  }



  /**
   * Removes a component from the set. A non-associated component is ignored.
   */

  public void
  remove(Component value)
  {
    if (components.remove(value))
    {
      EventSetDescriptor	event =
        Introspection.getEvent(value, "action");

      try
      {
        event.getRemoveListenerMethod().invoke(value, new Object[] {this});
      }
        
      catch (Throwable e)
      {
        throw new RuntimeException(e);
      }
    }
  }



  public void
  removeActionListener(ActionListener listener)
  {
    actionListeners.remove(listener);
  }



  /**
   * Changes the enable state. It is propagated to the associated components.
   */

  public void
  setEnabled(boolean value)
  {
    super.setEnabled(value);

    Iterator	iter = components.iterator();

    while (iter.hasNext())
    {
      ((Component) iter.next()).setEnabled(value);
    }
  }



  /**
   * Changes the icon. It is propagated to the associated components.
   */

  public void
  setIcon(Icon value)
  {
    icon = value;

    Iterator	iter = components.iterator();

    while (iter.hasNext())
    {
      setIcon((Component) iter.next());
    }
  }



  private void
  setIcon(Component component)
  {
    PropertyDescriptor	descriptor =
      Introspection.getProperty(component, "icon");

    if (descriptor != null)
    {
      try
      {
        descriptor.getWriteMethod().invoke(component, new Object[] {getIcon()});
      }

      catch (Throwable e)
      {
        throw new RuntimeException(e);
      }
    }
  }



  /**
   * Changes the text. It is propagated to the associated components.
   */

  public void
  setText(String value)
  {
    text = value;

    Iterator	iter = components.iterator();

    while (iter.hasNext())
    {
      setText((Component) iter.next());
    }
  }



  private void
  setText(Component component)
  {
    PropertyDescriptor	descriptor =
      Introspection.getProperty(component, "text");

    if (descriptor != null)
    {
      try
      {
        descriptor.getWriteMethod().invoke(component, new Object[] {getText()});
      }

      catch (Throwable e)
      {
        throw new RuntimeException(e);
      }
    }
  }

} // ActionSpace
