package be.re.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventObject;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;



/**
 * A table cell editor that doesn't start editing after a double click, but
 * after one mouse click on a selected cell.
 * @author Werner Donn\u00e9
 */

public class TableCellEditor extends DefaultCellEditor

{

  private int			column = -1;
  private KeyListener		keyListener =
    new KeyAdapter()
    {
      public void
      keyTyped(KeyEvent e)
      {
        processKey(e);
      }
    };
  private long			lastClick = -1;
  private int			row = -1;
  private Boolean		savedAutoStartsEdit;
  private Timer			timer =
    new Timer
    (
      Constants.EDIT_DELAY,
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          if (row != -1 && column != -1)
          {
            table.editCellAt(row, column);
            getComponent().requestFocus();
          }
        }
      }
    );
  private JTable		table;



  public
  TableCellEditor(JCheckBox checkBox)
  {
    super(checkBox);
    timer.setRepeats(false);
  }



  public
  TableCellEditor(JComboBox comboBox)
  {
    super(comboBox);
    timer.setRepeats(false);
  }



  public
  TableCellEditor(JTextField textField)
  {
    super(textField);
    timer.setRepeats(false);
  }



  public TableCellRenderer
  getCellRenderer(final TableCellRenderer renderer)
  {
    return
      (TableCellRenderer)
        Proxy.newProxyInstance
        (
          getClass().getClassLoader(),
          new Class[]{TableCellRenderer.class},
          new InvocationHandler()
          {
            public Object
            invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
              Object	result = method.invoke(renderer, args);

              if
              (
                result instanceof Component				 &&
                "getTableCellRendererComponent".equals(method.getName()) &&
                ((Boolean) args[3]).booleanValue() // hasFocus
              )
              {
                table.setEditingRow(((Integer) args[4]).intValue());
                table.setEditingColumn(((Integer) args[5]).intValue());

                if (result instanceof JComponent)
                {
                  ((JComponent) result).setBorder
                  (
                    new CompoundBorder
                    (
                      new AbstractBorder()
                      {
                        public void
                        paintBorder
                        (
                          Component	c,
                          Graphics	g,
                          int		x,
                          int		y,
                          int		width,
                          int		height
                        )
                        {
                          g.setColor(Color.BLACK);
                          g.drawRect(x, y, width - 1, height - 1);
                        }
                      },
                      ((JComponent) result).getBorder()
                    )
                  );
                }
              }

              return result;
            }
          }
        );
  }



  public JTable
  getTable()
  {
    return table;
  }



  public boolean
  isCellEditable(EventObject e)
  {
    if (!(e instanceof MouseEvent) && !(e instanceof KeyEvent))
    {
      return super.isCellEditable(e);
    }

    if (e instanceof MouseEvent)
    {
      if
      (
        ((MouseEvent) e).getClickCount() >= 2				   ||
        (
          lastClick != -1						   &&
          System.currentTimeMillis() - lastClick <= Constants.DOUBLE_CLICK
        )
      )
      {
        timer.stop();
        lastClick = -1;
        row = -1;
        column = -1;
      }
      else
      {
        if (table != null)
        {
          row = table.rowAtPoint(((MouseEvent) e).getPoint());
          column = table.columnAtPoint(((MouseEvent) e).getPoint());
          lastClick = System.currentTimeMillis();
          timer.start();
        }
      }

      return false;
    }

    return ((KeyEvent) e).getKeyChar() == ' ';
  }



  private void
  processKey(KeyEvent e)
  {
    if
    (
      isCellEditable(e)			&&
      table.getEditingRow() != -1	&&
      table.getEditingColumn() != -1
    )
    {
      table.editCellAt(table.getEditingRow(), table.getEditingColumn());

      javax.swing.table.TableCellEditor	editor =
        table.getCellEditor(table.getEditingRow(), table.getEditingColumn());

      if (editor instanceof DefaultCellEditor)
      {
        Component	component = ((DefaultCellEditor) editor).getComponent();

        if (component instanceof JComboBox)
        {
          ((JComboBox) component).showPopup();
        }

        component.requestFocus();
      }
    }
  }



  /**
   * The default cell renderer should already have been set.
   */

  public void
  setDefaultEditor(JTable table)
  {
    if (this.table != null)
    {
      this.table.removeKeyListener(keyListener);

      if (savedAutoStartsEdit != null)
      {
        this.table.
          putClientProperty("JTable.autoStartsEdit", savedAutoStartsEdit);
      }
    }

    this.table = table;

    if (table != null)
    {
      table.setDefaultEditor(Object.class, this);
      table.addKeyListener(keyListener);
      savedAutoStartsEdit =
        (Boolean) table.getClientProperty("JTable.autoStartsEdit");
      table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

      table.setDefaultRenderer
      (
        Object.class,
        getCellRenderer(new DefaultTableCellRenderer())
      );
    }
  }



  public void
  setTable(JTable table)
  {
    this.table = table;
  }

} // TableCellEditor
