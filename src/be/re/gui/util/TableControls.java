package be.re.gui.util;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;



/**
 * Has controls to add, remove and move rows in a table that uses the
 * <code>DefaultTableModel</code>.
 * @author Werner Donn\u00e9
 */

public class TableControls extends JPanel

{

  public
  TableControls(JTable table)
  {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    JButton	down =
      Util.createIconButton
      (
        new ImageIcon(TableControls.class.getResource("res/down_black.png")),
        true
      );
    JButton	minus =
      Util.createIconButton
      (
        new ImageIcon(TableControls.class.getResource("res/minus_black.png")),
        true
      );
    JButton	plus =
      Util.createIconButton
      (
        new ImageIcon(TableControls.class.getResource("res/plus_black.png")),
        true
      );
    JButton	up =
      Util.createIconButton
      (
        new ImageIcon(TableControls.class.getResource("res/up_black.png")),
        true
      );

    down.setDisabledIcon
    (
      new ImageIcon(TableControls.class.getResource("res/down_grey.png"))
    );

    minus.setDisabledIcon
    (
      new ImageIcon(TableControls.class.getResource("res/minus_grey.png"))
    );

    plus.setDisabledIcon
    (
      new ImageIcon(TableControls.class.getResource("res/plus_grey.png"))
    );

    up.setDisabledIcon
    (
      new ImageIcon(TableControls.class.getResource("res/up_grey.png"))
    );

    plus.setBorder(new MatteBorder(1, 1, 1, 0, new Color(0x6d, 0x6d, 0x6d)));
    minus.setBorder(new MatteBorder(1, 1, 1, 0, new Color(0x6d, 0x6d, 0x6d)));
    up.setBorder(new MatteBorder(1, 1, 1, 0, new Color(0x6d, 0x6d, 0x6d)));
    down.setBorder(new MatteBorder(1, 1, 1, 1, new Color(0x6d, 0x6d, 0x6d)));
    add(plus);
    add(minus);
    add(up);
    add(down);
    plus.setEnabled(table.getModel() instanceof DefaultTableModel);
    minus.setEnabled(table.getModel() instanceof DefaultTableModel);
    up.setEnabled(false);

    down.setEnabled
    (
      table.getModel() instanceof DefaultTableModel && table.getRowCount() > 1
    );

    setListeners(table, plus, minus, up, down);
  }



  private static void
  setListeners
  (
    final JTable	table,
    final JButton	plus,
    final JButton	minus,
    final JButton	up,
    final JButton	down
  )
  {
    table.getSelectionModel().addListSelectionListener
    (
      new ListSelectionListener()
      {
        public void
        valueChanged(ListSelectionEvent e)
        {
          plus.setEnabled
          (
            table.getModel() instanceof DefaultTableModel &&
              table.getSelectedRowCount() <= 1
          );

          minus.setEnabled
          (
            table.getModel() instanceof DefaultTableModel &&
              table.getSelectedRow() >= 0 &&
              table.getSelectedRow() < table.getRowCount()
          );

          up.setEnabled
          (
            table.getModel() instanceof DefaultTableModel &&
              table.getRowCount() > 1 && Util.isRowBlockSelected(table) &&
              table.getSelectionModel().getMinSelectionIndex() > 0
          );

          down.setEnabled
          (
            table.getModel() instanceof DefaultTableModel &&
              table.getRowCount() > 1 && Util.isRowBlockSelected(table) &&
              table.getSelectionModel().getMaxSelectionIndex() <
                table.getRowCount() - 1
          );
        }
      }
    );

    plus.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          EditableTable.appendRow(table);
        }
      }
    );

    minus.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          EditableTable.removeRows(table);
        }
      }
    );

    up.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          EditableTable.rowsUp(table);
        }
      }
    );

    down.addActionListener
    (
      new ActionListener()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          EditableTable.rowsDown(table);
        }
      }
    );
  }

} // TableControls
