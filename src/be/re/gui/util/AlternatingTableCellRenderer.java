package be.re.gui.util;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;



/**
 * @author Werner Donn\u00e9
 */

public class AlternatingTableCellRenderer extends DefaultTableCellRenderer

{

  public Component
  getTableCellRendererComponent
  (
    JTable	table,
    Object	value,
    boolean	isSelected,
    boolean	hasFocus,
    int		row,
    int		column
  )
  {
    setBackground
    (
      row % 2 == 1 ? new Color(244, 247, 251) : table.getBackground()
    );

    Component	result =
      super.getTableCellRendererComponent
      (
        table,
        value,
        isSelected,
        hasFocus,
        row,
        column
      );

    ((JComponent) result).setBorder(new EmptyBorder(0, 5, 0, 5));

    return result;
  }

} // AlternatingTableCellRenderer
