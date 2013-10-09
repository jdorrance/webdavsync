package be.re.gui.util;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;



/**
 * This button can show a piece of online help. Make sure the component
 * hierarchy above the parent of the button is complete, because the short cuts
 * are added to the <code>JRootPane</code> ancestor in the
 * <code>addNotify</code> method.
 * @author Werner Donn\u00e9
 */

public class HelpButton extends JButton

{

  private Action	action;



  public
  HelpButton(final URL resource)
  {
    this(resource, null);
  }



  public
  HelpButton(final URL resource, final JMenuBar helpMenuBar)
  {
    super(new ImageIcon(HelpButton.class.getResource("res/help.png")));
    setContentAreaFilled(false);
    setBorderPainted(false);

    action =
      new AbstractAction()
      {
        public void
        actionPerformed(ActionEvent e)
        {
          Util.showHtml(resource, helpMenuBar, Util.getTop(HelpButton.this));
        }
      };

    setActionCommand("help");
    addActionListener(action);
  }



  public void
  addNotify()
  {
    super.addNotify();

    if (getRootPane() != null)
    {
      getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
        put
        (
          be.re.util.Util.isMac() ?
            KeyStroke.getKeyStroke(new Character('?'), Event.META_MASK) :
            KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
          "help"
        );

      getRootPane().getActionMap().put("help", action);
    }
  }

} // HelpButton
