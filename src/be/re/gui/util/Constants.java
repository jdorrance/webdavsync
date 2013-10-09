package be.re.gui.util;

import java.awt.Color;
import java.awt.Toolkit;



/**
 * Werner Donn\u00e9
 */

public interface Constants

{

  public static final Color	BACKGROUND = new Color(231, 226, 226);
  public static final int	DOUBLE_CLICK =
    (
      (Integer)
        Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval")
    ).intValue();
  public static final int	EDIT_DELAY = 500;

} // Constants
