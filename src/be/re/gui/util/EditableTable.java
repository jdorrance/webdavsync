package be.re.gui.util;

import be.re.util.EventMulticaster;
import java.awt.Component;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.CellEditor;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;



/**
 * @author Werner Donn\u00e9
 */

public class EditableTable extends JTable

{

  private static final int      APPEND = 0;
  private static final int      COPY = 2;
  private static final int      CUT = 3;
  private static final int      DOWN = 6;
  private static final int      INSERT = 1;
  private static final int      PASTE = 4;
  private static final int      UP = 5;

  private ActionSpace[]		actions =
    new ActionSpace[]
    {
      new ActionSpace("append"),
      new ActionSpace("insert"),
      new ActionSpace("copy"),
      new ActionSpace("cut"),
      new ActionSpace("paste"),
      new ActionSpace("up"),
      new ActionSpace("down")
    };
  private TableCellEditor	cellEditor =
    new TableCellEditor(new JTextField());
  private List			clipboard = new ArrayList();
  private JPopupMenu		popupMenu = new JPopupMenu();



  /**
   * Uses <code>javax.swing.table.DefaultTableModel</code>.
   */

  public
  EditableTable()
  {
    this(new ActionSpace[0], new int[0]);
  }



  /**
   * Uses <code>javax.swing.table.DefaultTableModel</code>.
   */

  public
  EditableTable(ActionSpace[] extraActions, int[] extraKeys)
  {
    this(new DefaultTableModel(), extraActions, extraKeys);
  }



  public
  EditableTable
  (
    DefaultTableModel	model,
    ActionSpace[]	extraActions,
    int[]		extraKeys
  )
  {
    super(model);

    Util.setStyle(this);
    setShowGrid(false);
    setRowHeight((int) (1.5 * getRowHeight()));
    cellEditor.setDefaultEditor(this);
    setDefaultRenderer(Object.class, new AlternatingTableCellRenderer());

    actions[APPEND].setText(Util.getResource("label_append"));
    actions[COPY].setText(Util.getResource("label_copy"));
    actions[CUT].setText(Util.getResource("label_cut"));
    actions[INSERT].setText(Util.getResource("label_insert"));
    actions[PASTE].setText(Util.getResource("label_paste"));
    actions[UP].setText(Util.getResource("label_up"));
    actions[DOWN].setText(Util.getResource("label_down"));

    setAppendActionHandler(actions[APPEND]);
    setCopyActionHandler(actions[COPY]);
    setCutActionHandler(actions[CUT]);
    setInsertActionHandler(actions[INSERT]);
    setPasteActionHandler(actions[PASTE]);
    setUpActionHandler(actions[UP]);
    setDownActionHandler(actions[DOWN]);

    actions[APPEND].setEnabled(true);
    actions[COPY].setEnabled(false);
    actions[CUT].setEnabled(false);
    actions[INSERT].setEnabled(true);
    actions[PASTE].setEnabled(false);
    actions[UP].setEnabled(false);
    actions[DOWN].setEnabled(false);

    setSelectionListener();
    preparePopupMenu(extraActions, extraKeys);
    prepareDeleteAction();
    setToolTipText(Util.getResource("msg_edit_table"));
  }



  public void
  appendAction()
  {
    appendRow(this);

    processKeyEvent
    (
      new KeyEvent
      (
        this,
        KeyEvent.KEY_PRESSED,
        System.currentTimeMillis(),
        0,
        KeyEvent.VK_DOWN,
        KeyEvent.CHAR_UNDEFINED
      )
    );
  }



  static void
  appendRow(JTable table)
  {
    int	row = table.getSelectedRow() + 1;

    if (row == 0 || row == table.getRowCount())
    {
      ((DefaultTableModel) table.getModel()).addRow(createEmptyRow(table));
      row = table.getRowCount() - 1;
    }
    else
    {
      ((DefaultTableModel) table.getModel()).
        insertRow(row, createEmptyRow(table));
    }

    table.scrollRectToVisible(table.getCellRect(row, 0, true));
    table.getSelectionModel().setSelectionInterval(row, row);
  }



  /**
   * DefaultTableModel.moveRow seems to set the cell in edit mode for
   * absolutely no reason.
   */

  private void
  cancelUnnecessaryEditing()
  {
    try
    {
      CellEditor	editor =
        getCellEditor(getEditingRow(), getEditingColumn());

      if (editor != null)
      {
        editor.cancelCellEditing();
      }
    }

    catch (Throwable e)
    {
      // Future Swing version may not do this anymore so there would be no
      // editing row nor column.
    }
  }



  private void
  checkActions()
  {
    actions[APPEND].setEnabled(getSelectedRowCount() <= 1);
    actions[INSERT].setEnabled(getSelectedRowCount() <= 1);
    actions[COPY].setEnabled(getRowCount() > 0 && getSelectedRowCount() > 0);
    actions[CUT].setEnabled(getRowCount() > 0 && getSelectedRowCount() > 0);

    actions[PASTE].setEnabled
    (
      (getRowCount() == 0 || getSelectedRowCount() == 1) && clipboard.size() > 0
    );

    actions[UP].setEnabled
    (
      getRowCount() > 1 && Util.isRowBlockSelected(this) &&
        getSelectionModel().getMinSelectionIndex() > 0
    );

    actions[DOWN].setEnabled
    (
      getRowCount() > 1 && Util.isRowBlockSelected(this) &&
        getSelectionModel().getMaxSelectionIndex() < getRowCount() - 1
    );
  }



  private void
  collectMenuItems(List items, ActionSpace[] actions, int[] keys)
  {
    for (int i = 0; i < actions.length; ++i)
    {
      JMenuItem	item = new JMenuItem();

      item.setAccelerator
      (
        KeyStroke.getKeyStroke(keys[i],
        be.re.util.Util.isMac() ? Event.META_MASK : Event.CTRL_MASK)
      );

      actions[i].add(item);
      getInputMap().put(item.getAccelerator(), actions[i].getName());
      getActionMap().put(actions[i].getName(), actions[i]);
      items.add(item);
    }
  }



  public void
  copyAction()
  {
    clipboard.clear();

    for (int i = getRowCount() - 1; i >= 0; --i)
    {
      if (getSelectionModel().isSelectedIndex(i))
      {
        clipboard.
          add(0, ((DefaultTableModel) getModel()).getDataVector().get(i));
      }
    }

    checkActions();
  }



  private Vector
  copyRow(Vector row)
  {
    Vector      copy = new Vector();

    for (int i = 0; i < getColumnCount(); ++i)
    {
      copy.add(row.get(i));
    }

    return copy;
  }



  private static String[]
  createEmptyRow(JTable table)
  {
    String[]	data = new String[table.getColumnCount()];

    Arrays.fill(data, "");

    return data;
  }



  public void
  cutAction()
  {
    clipboard.clear();

    for (int i = getRowCount() - 1; i >= 0; --i)
    {
      if (getSelectionModel().isSelectedIndex(i))
      {
        clipboard.add
        (
          0,
          ((DefaultTableModel) getModel()).getDataVector().get(i)
        );
      }
    }

    removeRows(this);
    checkActions();
  }



  public void
  downAction()
  {
    rowsDown(this);
    cancelUnnecessaryEditing();
  }



  public JPopupMenu
  getPopupMenu()
  {
    return popupMenu;
  }



  public void
  insertAction()
  {
    int	row = Math.max(0, getSelectedRow());

    ((DefaultTableModel) getModel()).insertRow(row, createEmptyRow(this));
    setRowSelectionInterval(row, row);
    scrollRectToVisible(getCellRect(row, 0, true));
  }



  public void
  pasteAction()
  {
    int	row = getSelectedRow() + 1;

    for (int i = clipboard.size() - 1; i >= 0; --i)
    {
      ((DefaultTableModel) getModel()).
        insertRow(row, copyRow((Vector) clipboard.get(i)));
    }

    checkActions();
    setRowSelectionInterval(row, row);
  }



  private void
  prepareDeleteAction()
  {
    addKeyListener
    (
      new KeyAdapter()
      {
        public void
        keyPressed(KeyEvent e)
        {
          if
          (
            e.getKeyCode() == KeyEvent.VK_BACK_SPACE	||
            e.getKeyCode() == KeyEvent.VK_DELETE
          )
          {
            removeRows(EditableTable.this);
          }
        }
      }
    );
  }



  private void
  preparePopupMenu(ActionSpace[] extraActions, int[] extraKeys)
  {
    List	items = new ArrayList();

    collectMenuItems
    (
      items,
      actions,
      new int[]
      {
        KeyEvent.VK_A,
        KeyEvent.VK_I,
        KeyEvent.VK_C,
        KeyEvent.VK_X,
        KeyEvent.VK_V,
        KeyEvent.VK_U,
        KeyEvent.VK_D
      }
    );

    collectMenuItems(items, extraActions, extraKeys);

    for (int i = 0; i < items.size(); ++i)
    {
      popupMenu.add((JMenuItem) items.get(i));
    }

    addMouseListener
    (
      new MouseAdapter()
      {
        public void
        mousePressed(MouseEvent e)
        {
          if (e.isPopupTrigger())
          {
            popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
          }
        }

        public void
        mouseReleased(MouseEvent e)
        {
          if (e.isPopupTrigger())
          {
            popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
          }
        }
      }
    );
  }



  static void
  removeRows(JTable table)
  {
    int	selected = table.getSelectedRow();

    Util.removeSelectedRows(table);

    if (table.getRowCount() == 0)
    {
      ((DefaultTableModel) table.getModel()).addRow(createEmptyRow(table));
      table.selectAll();
    }
    else
    {
      if (selected >= 0)
      {
        if (selected >= table.getRowCount())
        {
          selected = table.getRowCount() - 1;
        }

        table.getSelectionModel().setSelectionInterval(selected, selected);
      }
    }
  }



  static void
  rowsDown(JTable table)
  {
    int	first = table.getSelectionModel().getMinSelectionIndex();
    int	last = table.getSelectionModel().getMaxSelectionIndex();

    ((DefaultTableModel) table.getModel()).moveRow(first, last, first + 1);
    table.setRowSelectionInterval(first + 1, last + 1);
    table.scrollRectToVisible(table.getCellRect(first + 1, 0, true));
  }



  static void
  rowsUp(JTable table)
  {
    int	first = table.getSelectionModel().getMinSelectionIndex();
    int	last = table.getSelectionModel().getMaxSelectionIndex();

    ((DefaultTableModel) table.getModel()).moveRow(first, last, first - 1);
    table.setRowSelectionInterval(first - 1, last - 1);
    table.scrollRectToVisible(table.getCellRect(first - 1, 0, true));
  }



  protected void
  setAppendActionHandler(ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          appendAction();
        }
      }
    );
  }



  protected void
  setCopyActionHandler(ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          copyAction();
        }
      }
    );
  }



  protected void
  setCutActionHandler(ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          cutAction();
        }
      }
    );
  }



  public void
  setDefaultRenderer(Class columnClass, TableCellRenderer renderer)
  {
    super.setDefaultRenderer(columnClass, cellEditor.getCellRenderer(renderer));
  }



  protected void
  setDownActionHandler(ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          downAction();
        }
      }
    );
  }



  protected void
  setInsertActionHandler(ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          insertAction();
        }
      }
    );
  }



  public void
  setModel(TableModel dataModel)
  {
    if (!(dataModel instanceof DefaultTableModel))
    {
      throw new IllegalArgumentException("Wrong table model type.");
    }

    super.setModel(dataModel);
  }



  protected void
  setPasteActionHandler(final ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          pasteAction();
        }
      }
    );
  }



  private void
  setSelectionListener()
  {
    getSelectionModel().addListSelectionListener
    (
      new ListSelectionListener()
      {
        public void
        valueChanged(ListSelectionEvent e)
        {
          checkActions();
        }
      }
    );
  }



  protected void
  setUpActionHandler(final ActionSpace action)
  {
    action.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          upAction();
        }
      }
    );
  }



  public void
  upAction()
  {
    rowsUp(this);
    cancelUnnecessaryEditing();
  }

} // EditableTable
