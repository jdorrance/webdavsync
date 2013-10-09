package be.re.gui.util;

import be.re.gui.util.Frame;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;



/**
 * A dialog to show messages.
 * @author Werner Donn\u00e9
 */

public class MessageDialog extends JDialog

{

  private static JFrame	defaultOwner;
  private Action	okAction =
    new AbstractAction()
    {
      public void
      actionPerformed(ActionEvent e)
      {
        dispose();
      }
    };



  public
  MessageDialog(String title, JComponent component)
  {
    this(defaultOwner != null ? defaultOwner : new JFrame(), title, component);
  }



  public
  MessageDialog(JFrame frame, String title, JComponent component)
  {
    super(frame, title);
    init(component);
    pack();

    setLocation
    (
      (int) (getToolkit().getScreenSize().getWidth() / 2) -
        (int) (getSize().getWidth() / 2),
      (int) (getToolkit().getScreenSize().getHeight() / 2) -
        (int) (getSize().getHeight() / 2)
    );
  }



  public void
  addNotify()
  {
    super.addNotify();

    if (Frame.getDefaultIcon() != null)
    {
      be.re.gui.util.Util.getFrame(this).
        setIconImage(Frame.getDefaultIcon().getImage());
    }
  }



  public static JFrame
  getDefaultOwner()
  {
    return defaultOwner;
  }



  private void
  init(JComponent component)
  {
    JPanel 	buttonPanel = new JPanel();
    JButton 	okButton = new JButton();

    okButton.setActionCommand("ok");
    okButton.setText(be.re.gui.util.Util.getResource("ok"));
    okButton.addActionListener(okAction);
    okButton.setDefaultCapable(true);
    getRootPane().getActionMap().put("ok", okAction);

    getRootPane().setDefaultButton(okButton);
    getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
      put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ok");
    component.setFocusable(false);

    getContentPane().
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(component);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 5)));
    getContentPane().add(buttonPanel);
    getContentPane().add(Box.createRigidArea(new Dimension(0, 10)));

    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(okButton);
  }



  public void
  open()
  {
    if (!isModal())
    {
      SwingUtilities.invokeLater
      (
        new Runnable()
        {
          public void
          run()
          {
            setVisible(true);
          }
        }
      );

      synchronized (this)
      {
        try
        {
          wait();
        }

        catch (InterruptedException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
    else
    {
      setVisible(true);
    }
  }



  public void
  removeNotify()
  {
    super.removeNotify();

    synchronized (this)
    {
      notifyAll();
    }
  }



  public static void
  setDefaultOwner(JFrame frame)
  {
    defaultOwner = frame;
  }

} // MessageDialog
