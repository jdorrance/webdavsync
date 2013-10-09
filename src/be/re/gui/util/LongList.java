package be.re.gui.util;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;



/**
 * A list and a text field combined. When the user types something in the text
 * field the list will be reduced to values that match.
 * @author Werner Donn\u00e9
 */

public class LongList extends JPanel

{

  private SortedSet<Object>	data =
    new TreeSet<Object>
    (
      new Comparator<Object>()
      {
        public int
        compare(Object o1, Object o2)
        {
          return o1.toString().compareToIgnoreCase(o2.toString());
        }
      }
    );
  private JTextField		field =
    new JTextField()
    {
      protected void
      processKeyEvent(KeyEvent e)
      {
        if
        (
          e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN
        )
        {
          list.requestFocus();

          if (list.getModel().getSize() > 0)
          {
            list.setSelectedIndex
            (
              e.getKeyCode() == KeyEvent.VK_UP ?
                list.getModel().getSize() - 1 : 0
            );
          }
        }
        else
        {
          super.processKeyEvent(e);
        }
      }
    };
  private boolean		internal;
  private JList			list = new JList(new ListModel());
  private JScrollPane		scrollPane = new JScrollPane(list);



  public
  LongList()
  {
    super(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);
    add(field, BorderLayout.SOUTH);
    field.setBorder(scrollPane.getBorder());

    field.addKeyListener
    (
      new KeyAdapter()
      {
        public void
        keyTyped(final KeyEvent e)
        {
          SwingUtilities.invokeLater
          (
            new Runnable()
            {
              public void
              run()
              {
                adjustData();
              }
            }
          );
        }
      }
    );
  }



  public
  LongList(Object[] data)
  {
    this();

    for (int i = 0; i < data.length; ++i)
    {
      ((DefaultListModel) list.getModel()).addElement(data[i]);
    }
  }



  public void
  addNotify()
  {
    super.addNotify();
    field.requestFocus();
  }



  private void
  adjustData()
  {
    String	value = reduceText(field.getText());

    internal = true;

    ((DefaultListModel) list.getModel()).clear();

    for (Object element: data)
    {
      if (reduceText(element.toString()).indexOf(value) != -1)
      {
        ((DefaultListModel) list.getModel()).addElement(element);
      }
    }

    internal = false;

    if (list.getModel().getSize() == 1)
    {
      list.setSelectedIndex(0);
    }
  }



  public JTextField
  getField()
  {
    return field;
  }



  public JList
  getList()
  {
    return list;
  }



  public JScrollPane
  getScrollPane()
  {
    return scrollPane;
  }



  private static String
  reduceText(String text)
  {
    return text.replaceAll("\\s+", "").toLowerCase();
  }



  private class ListModel extends DefaultListModel

  {

    public void
    add(int index, Object element)
    {
      super.add(index, element);

      if (!internal)
      {
        data.add(element);
      }
    }



    public void
    addElement(Object element)
    {
      super.addElement(element);

      if (!internal)
      {
        data.add(element);
      }
    }



    public void
    clear()
    {
      super.clear();

      if (!internal)
      {
        data.clear();
      }
    }



    public void
    insertElementAt(Object element, int index)
    {
      add(index, element);
    }



    public Object
    remove(int index)
    {
      Object	element = super.remove(index);

      if (!internal)
      {
        data.remove(element);
      }

      return element;
    }



    public void
    removeAllElements()
    {
      clear();
    }



    public boolean
    removeElement(Object element)
    {
      boolean	result = super.removeElement(element);

      if (!internal)
      {
        data.remove(element);
      }

      return result;
    }



    public void
    removeElementAt(int index)
    {
      remove(index);
    }



    public void
    removeRange(int fromIndex, int toIndex)
    {
      if (!internal)
      {
        for (int i = fromIndex; i <= toIndex; ++i)
        {
          data.remove(get(i));
        }
      }

      super.removeRange(fromIndex, toIndex);
    }



    public Object
    set(int index, Object element)
    {
      Object	old = super.set(index, element);

      if (!internal)
      {
        data.remove(old);
        data.add(element);
      }

      return old;
    }



    public void
    setElementAt(Object element, int index)
    {
      set(index, element);
    }

  } // ListModel

} // LongList
