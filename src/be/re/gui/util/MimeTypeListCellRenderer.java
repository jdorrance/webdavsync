package be.re.gui.util;

import be.re.util.MimeType;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;



/**
 * Shows icon for a MIME type if it is available.
 * @author Werner Donn\u00e9
 */

public class MimeTypeListCellRenderer extends DefaultListCellRenderer

{

  private static ImageIcon	emptyIcon;



  static
  {
    try
    {
      emptyIcon =
        new ImageIcon
        (
          MimeTypeListCellRenderer.class.getResource("res/empty_icon.png")
        );
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public Component
  getListCellRendererComponent
  (
    JList	list,
    Object	value,
    int		index,
    boolean	isSelected,
    boolean	cellHasFocus
  )
  {
    JLabel	result =
      (JLabel)
        super.getListCellRendererComponent
        (
          list,
          value,
          index,
          isSelected,
          cellHasFocus
        );

    if (value instanceof MimeType.LabeledMimeType || value instanceof String)
    {
      ImageIcon	icon =
        MimeType.getMimeTypeIcon
        (
          value instanceof MimeType.LabeledMimeType ?
            ((MimeType.LabeledMimeType) value).getMimeTypes()[0] :
            (String) value
        );

      result.setIcon(icon != null ? icon : emptyIcon);

      if (value instanceof String)
      {
        result.setText(MimeType.getMimeTypeLabel((String) value));
      }
    }

    return result;
  }

} // MimeTypeListCellRenderer
